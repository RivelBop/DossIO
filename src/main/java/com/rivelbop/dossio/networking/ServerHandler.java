package com.rivelbop.dossio.networking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.rivelbop.dossio.networking.Packet.ClientDataPacket;
import com.rivelbop.dossio.networking.Packet.DisconnectClientPacket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

/** Handles Kryonet {@link Server} - starting, stopping, sending/receiving packets. */
public final class ServerHandler {
  private static final String LOG_TAG = "ServerHandler";

  private final Server server = new Server(Network.BUFFER_SIZE, Network.BUFFER_SIZE);
  private final HashMap<Integer, ClientDataPacket> clients = new HashMap<>();

  private String ipAddress = Network.DEFAULT_IP_ADDRESS;
  private int port = Network.DEFAULT_PORT;

  /**
   * Sets the listener, binds the IP address, and starts the server. Throws exception if fails.
   *
   * @throws RuntimeException If the server fails to bind to IP address and port.
   */
  public void start() {
    // If the server was previously running, clear any remaining client data
    clients.clear();

    // Set listener
    server.addListener(
        new Listener() {
          @Override
          public void connected(Connection connection) {
            // Send all current server client's to the newly connected client
            int id = connection.getID();
            for (ClientDataPacket c : clients.values()) {
              if (c.id != id) {
                server.sendToTCP(id, c);
              }
            }
          }

          @Override
          public void received(Connection connection, Object object) {
            if (object instanceof ClientDataPacket p) {
              clients.put(p.id, p);
            }
            server.sendToAllExceptTCP(connection.getID(), object);
          }

          @Override
          public void disconnected(Connection connection) {
            clients.remove(connection.getID());

            DisconnectClientPacket disconnectClientPacket = new DisconnectClientPacket();
            disconnectClientPacket.id = connection.getID();
            server.sendToAllExceptTCP(connection.getID(), disconnectClientPacket);
          }
        });

    // Bind the server to a TCP and UDP address
    try {
      server.bind(new InetSocketAddress(ipAddress, port), new InetSocketAddress(ipAddress, port));
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to bind server to socket address!", e);
      throw new RuntimeException(e);
    }

    // Start
    server.start();
  }

  /**
   * Checks the server update thread to see if it is alive (therefore running).
   *
   * @return The server's online status.
   */
  public boolean isRunning() {
    Thread t = server.getUpdateThread();
    return t != null && t.isAlive();
  }

  /** Stops the Kryonet server from running. */
  public void stop() {
    server.stop();
  }

  /**
   * Calls the server's dispose method. Throws exception if fails.
   *
   * @throws RuntimeException If server fails to release resources.
   */
  public void dispose() {
    try {
      server.dispose();
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to free resources from the server!", e);
      throw new RuntimeException(e);
    }
  }

  public String getIpAddress() {
    return ipAddress;
  }

  /**
   * Validates and sets the server's IP address.
   *
   * @param ipAddress The IP address to set the server to.
   */
  public void setIpAddress(String ipAddress) {
    this.ipAddress = Network.validateIpAddress(ipAddress);
  }

  public int getPort() {
    return port;
  }

  /**
   * Validates and sets the server's port.
   *
   * @param port The port to set the server to.
   */
  public void setPort(int port) {
    this.port = Network.validatePort(port);
  }

  public Kryo getKryo() {
    return server.getKryo();
  }
}
