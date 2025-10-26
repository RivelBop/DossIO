package com.rivelbop.dossio.networking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.minlog.Log;
import com.rivelbop.dossio.io.EditSerializer;
import com.rivelbop.dossio.networking.Packet.BeginEditPacket;
import com.rivelbop.dossio.networking.Packet.ClientDataPacket;
import com.rivelbop.dossio.networking.Packet.CreateFilePacket;
import com.rivelbop.dossio.networking.Packet.DeleteFilePacket;
import com.rivelbop.dossio.networking.Packet.DisconnectClientPacket;
import com.rivelbop.dossio.networking.Packet.EditPacket;
import com.rivelbop.dossio.networking.Packet.EndEditPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.eclipse.jgit.diff.Edit;

/** Stores and maintains both {@link ServerHandler} and {@link ClientHandler}. */
public final class Network {
  /** The extended buffer size (65 KB) for both the client and server. */
  public static final int BUFFER_SIZE = 65536;

  /** The default IP address (sets if no IP address is provided). */
  public static final String DEFAULT_IP_ADDRESS;

  /** The maximum possible port number (max value of an unsigned short - 65535). */
  public static final int MAX_PORT = Short.MAX_VALUE * 2 + 1;

  /** The default port number (sets if no value provided or value is out of range). */
  public static final int DEFAULT_PORT = 54555;

  private static final String LOG_TAG = "Network";

  static {
    try {
      DEFAULT_IP_ADDRESS = getLocalhostLanAddress().toString().substring(1);
    } catch (UnknownHostException e) {
      Log.error(LOG_TAG, "Unable to get the localhost LAN address!", e);
      throw new RuntimeException(e);
    }
  }

  private final ServerHandler serverHandler = new ServerHandler();
  private final ClientHandler clientHandler = new ClientHandler();

  /** Registers the shared packet classes between the server and client. */
  public Network() {
    registerClasses(serverHandler.getKryo());
    registerClasses(clientHandler.getKryo());
  }

  /**
   * Checks the IP address to make sure it is valid (currently just checks if it is blank).
   *
   * <p>NOTE: If the IP address is invalid, the {@link #DEFAULT_IP_ADDRESS} will be returned.
   *
   * @param ipAddress The IP address to check and validate.
   * @return The validated IP address.
   */
  public static String validateIpAddress(String ipAddress) {
    if (ipAddress.isBlank()) {
      return DEFAULT_IP_ADDRESS;
    }
    return ipAddress;
  }

  /**
   * Checks the port to make sure it is valid.
   *
   * <p>NOTE: Ports outside the range (0-{@link #MAX_PORT}) are invalid and will automatically be
   * set to the {@link #DEFAULT_PORT}.
   *
   * @param port The port to check and validate.
   * @return The validated port.
   */
  public static int validatePort(int port) {
    if (port < 0 || port > MAX_PORT) {
      return DEFAULT_PORT;
    }
    return port;
  }

  /**
   * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN
   * IP address.
   *
   * <p>This method is intended for use as a replacement of JDK method <code>
   * InetAddress.getLocalHost</code>, because that method is ambiguous on Linux systems. Linux
   * systems enumerate the loopback network interface the same way as regular LAN network
   * interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not specify the
   * algorithm used to select the address returned under such circumstances, and will often return
   * the loopback address, which is not valid for network communication. Details <a
   * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
   *
   * <p>This method will scan all IP addresses on all network interfaces on the host machine to
   * determine the IP address most likely to be the machine's LAN address. If the machine has
   * multiple IP addresses, this method will prefer a site-local IP address (e.g. 192.168.x.x or
   * 10.10.x.x, usually IPv4) if the machine has one (and will return the first site-local address
   * if the machine has more than one), but if the machine does not hold a site-local address, this
   * method will return simply the first non-loopback address found (IPv4 or IPv6).
   *
   * <p>If this method cannot find a non-loopback address using this selection algorithm, it will
   * fall back to calling and returning the result of JDK method <code>InetAddress.getLocalHost
   * </code>.
   *
   * @throws UnknownHostException If the LAN address of the machine cannot be found.
   */
  public static InetAddress getLocalhostLanAddress() throws UnknownHostException {
    try {
      InetAddress candidateAddress = null;
      // Iterate all NICs (network interface cards)...
      for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
          ifaces.hasMoreElements(); ) {
        NetworkInterface iface = ifaces.nextElement();
        // Iterate all IP addresses assigned to each card...
        for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses();
            inetAddrs.hasMoreElements(); ) {
          InetAddress inetAddr = inetAddrs.nextElement();
          if (!inetAddr.isLoopbackAddress()) {
            if (inetAddr.isSiteLocalAddress()) {
              // Found non-loopback site-local address. Return it immediately...
              return inetAddr;
            } else if (candidateAddress == null) {
              // Found non-loopback address, but not necessarily site-local.
              // Store it as a candidate to be returned if site-local address is not subsequently
              // found...
              candidateAddress = inetAddr;
              // Note that we don't repeatedly assign non-loopback non-site-local addresses as
              // candidates,
              // only the first. For subsequent iterations, candidate will be non-null.
            }
          }
        }
      }
      if (candidateAddress != null) {
        // We did not find a site-local address, but we found some other non-loopback address.
        // Server might have a non-site-local address assigned to its NIC (or it might be running
        // IPv6 which deprecates the "site-local" concept).
        // Return this non-loopback candidate address...
        return candidateAddress;
      }
      // At this point, we did not find a non-loopback address.
      // Fall back to returning whatever InetAddress.getLocalHost() returns...
      InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
      if (jdkSuppliedAddress == null) {
        throw new UnknownHostException(
            "The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
      }
      return jdkSuppliedAddress;
    } catch (Exception e) {
      Log.error(LOG_TAG, "Failed to determine LAN address!", e);
      UnknownHostException unknownHostException = new UnknownHostException();
      unknownHostException.initCause(e);
      throw unknownHostException;
    }
  }

  /** Stop and dispose both the server and client handlers. */
  public void dispose() {
    serverHandler.stop();
    clientHandler.stop();

    serverHandler.dispose();
    clientHandler.dispose();
  }

  public ServerHandler getServerHandler() {
    return serverHandler;
  }

  public ClientHandler getClientHandler() {
    return clientHandler;
  }

  private void registerClasses(Kryo kryo) {
    // Client packets
    kryo.register(ClientDataPacket.class);
    kryo.register(DisconnectClientPacket.class);

    // File packets
    kryo.register(CreateFilePacket.class);
    kryo.register(DeleteFilePacket.class);

    // Edit packets
    kryo.register(BeginEditPacket.class);
    kryo.register(Edit.Type.class);
    kryo.register(String[].class);
    kryo.register(EditPacket.class, new EditSerializer());
    kryo.register(EndEditPacket.class);
  }
}
