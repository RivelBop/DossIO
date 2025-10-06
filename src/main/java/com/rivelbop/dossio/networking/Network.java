package com.rivelbop.dossio.networking;

import com.esotericsoftware.kryo.Kryo;

/** Stores and maintains both {@link ServerHandler} and {@link ClientHandler}. */
public final class Network {
  /** The default IP address (sets if no IP address is provided). */
  public static final String DEFAULT_IP_ADDRESS = "0.0.0.0";

  /** The maximum possible port number (max value of an unsigned short). */
  public static final int MAX_PORT = 65535;

  /** The default port number (sets if no value provided or value is out of range). */
  public static final int DEFAULT_PORT = 54555;

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

  /** Stop and dispose both the server and client handlers. */
  public void dispose() {
    serverHandler.stop();
    serverHandler.dispose();

    clientHandler.stop();
    clientHandler.dispose();
  }

  public ServerHandler getServerHandler() {
    return serverHandler;
  }

  public ClientHandler getClientHandler() {
    return clientHandler;
  }

  private void registerClasses(Kryo kryo) {
    // Intentionally empty
  }
}
