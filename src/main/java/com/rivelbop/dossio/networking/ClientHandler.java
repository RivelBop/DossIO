package com.rivelbop.dossio.networking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.rivelbop.dossio.networking.Packet.ClientDataPacket;
import com.rivelbop.dossio.networking.Packet.DisconnectClientPacket;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javafx.application.Platform;
import javax.annotation.CheckForNull;

/** Handles Kryonet {@link Client} - connecting, stopping, sending/receiving packets. */
public final class ClientHandler {
  private static final String LOG_TAG = "ClientHandler";

  /** The timeout for connecting to a server (milliseconds). */
  private static final int TIMEOUT = 5000;

  private final Client client = new Client(Network.BUFFER_SIZE, Network.BUFFER_SIZE);
  private final HashMap<Integer, ClientDataPacket> clients = new HashMap<>();

  private final Set<String> filesMarkedForCreation = Collections.synchronizedSet(new HashSet<>());
  private final Set<String> filesMarkedForDeletion = Collections.synchronizedSet(new HashSet<>());

  private String ipAddress = Network.DEFAULT_IP_ADDRESS;
  private int port = Network.DEFAULT_PORT;

  private String username = "CLIENT";

  @CheckForNull private ClientListener clientListener;

  /**
   * Sets the listener, starts, and connects the client. Throws exception if fails.
   *
   * @throws RuntimeException If the client fails to connect.
   */
  public void connect() {
    // Set listener
    client.addListener(
        new Listener() {
          @Override
          public void connected(Connection connection) {
            // Send current client's data to server
            ClientDataPacket clientDataPacket = new ClientDataPacket();
            clientDataPacket.id = connection.getID();
            clientDataPacket.username = username;
            client.sendTCP(clientDataPacket);

            if (clientListener != null) {
              Platform.runLater(() -> clientListener.connected(connection));
            }
          }

          @Override
          public void received(Connection connection, Object object) {
            if (object instanceof ClientDataPacket p) {
              clients.put(p.id, p);
            } else if (object instanceof DisconnectClientPacket p) {
              clients.remove(p.id);
            }

            if (clientListener != null) {
              Platform.runLater(
                  () -> {
                    if (object instanceof Packet.CreateFilePacket p) {
                      filesMarkedForCreation.add(p.fileName);
                    } else if (object instanceof Packet.DeleteFilePacket p) {
                      filesMarkedForDeletion.add(p.fileName);
                    }
                    clientListener.received(connection, object);
                  });
            }
          }

          @Override
          public void disconnected(Connection connection) {
            clients.clear();

            if (clientListener != null) {
              Platform.runLater(() -> clientListener.disconnected(connection));
            }
          }
        });

    // Start
    client.start();

    // Connect
    try {
      client.connect(TIMEOUT, ipAddress, port, port);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to connect client to server!", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends a TCP packet to the server.
   *
   * @param o The packet to send.
   */
  public void sendTcp(Object o) {
    client.sendTCP(o);
  }

  /** Stops the Kryonet client from running. */
  public void stop() {
    client.stop();
  }

  /**
   * Calls the client's dispose method. Throws exception if fails.
   *
   * @throws RuntimeException If client fails to release resources.
   */
  public void dispose() {
    try {
      client.dispose();
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to free resources from the client!", e);
      throw new RuntimeException(e);
    }
  }

  public String getIpAddress() {
    return ipAddress;
  }

  /**
   * Validates and sets the client's IP address.
   *
   * @param ipAddress The IP address to set the client to.
   */
  public void setIpAddress(String ipAddress) {
    this.ipAddress = Network.validateIpAddress(ipAddress);
  }

  public int getPort() {
    return port;
  }

  /**
   * Validates and sets the client's port.
   *
   * @param port The port to set the client to.
   */
  public void setPort(int port) {
    this.port = Network.validatePort(port);
  }

  public String getUsername() {
    return username;
  }

  /**
   * Checks if the input username is not blank and sets the client's username.
   *
   * @param username The username to set the client to.
   */
  public void setUsername(String username) {
    if (!username.isBlank()) {
      this.username = username;
    }
  }

  public int getId() {
    return client.getID();
  }

  public Map<Integer, ClientDataPacket> getClients() {
    return clients;
  }

  public Kryo getKryo() {
    return client.getKryo();
  }

  public void setClientListener(@CheckForNull ClientListener clientListener) {
    this.clientListener = clientListener;
  }

  public Set<String> getFilesMarkedForCreation() {
    return filesMarkedForCreation;
  }

  public Set<String> getFilesMarkedForDeletion() {
    return filesMarkedForDeletion;
  }
}
