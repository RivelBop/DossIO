package com.rivelbop.dossio.networking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import java.io.IOException;

/**
 * Handles Kryonet {@link Client} - connecting, stopping, sending/receiving packets.
 */
public final class ClientHandler {

  private static final int TIMEOUT = 5;

  private final Client client = new Client();

  private String ipAddress = Network.DEFAULT_IP_ADDRESS;
  private int port = Network.DEFAULT_PORT;

  /**
   * Sets the listener, starts, and connects the client. Throws exception if fails.
   *
   * @throws RuntimeException If the client fails to connect.
   */
  public void connect() {
    // Set listener
    client.addListener(new Listener() {
      @Override
      public void connected(Connection connection) {
        // Intentionally empty
      }

      @Override
      public void disconnected(Connection connection) {
        // Intentionally empty
      }

      @Override
      public void received(Connection connection, Object object) {
        // Intentionally empty
      }

      @Override
      public void idle(Connection connection) {
        // Intentionally empty
      }
    });

    // Start
    client.start();

    // Connect
    try {
      client.connect(TIMEOUT * 1000, ipAddress, port, port);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

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
      throw new RuntimeException(e);
    }
  }

  public String getIpAddress() {
    return ipAddress;
  }

  /**
   * Sets the client's IP address, you MUST restart the client to apply change. Blank addresses will
   * automatically be set to {@link Network#DEFAULT_IP_ADDRESS}.
   *
   * @param ip The IP address to set the client to.
   */
  public void setIpAddress(String ip) {
    if (ip.isBlank()) {
      ipAddress = Network.DEFAULT_IP_ADDRESS;
      return;
    }
    ipAddress = ip;
  }

  public int getPort() {
    return port;
  }

  /**
   * Sets the client's port, you MUST restart the client to apply change. Ports outside of range
   * (0-{@link Network#MAX_PORT}) will be automatically set to {@link Network#DEFAULT_PORT}.
   *
   * @param port The port to set the client to.
   */
  public void setPort(int port) {
    if (port < 0 || port > Network.MAX_PORT) {
      this.port = Network.DEFAULT_PORT;
      return;
    }
    this.port = port;
  }

  public Kryo getKryo() {
    return client.getKryo();
  }
}
