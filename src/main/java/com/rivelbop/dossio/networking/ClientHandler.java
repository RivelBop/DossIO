package com.rivelbop.dossio.networking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import java.io.IOException;

/** Handles Kryonet {@link Client} - connecting, stopping, sending/receiving packets. */
public final class ClientHandler {
  /** The timeout for connecting to a server (milliseconds). */
  private static final int TIMEOUT = 5000;

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
    client.addListener(
        new Listener() {
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
      client.connect(TIMEOUT, ipAddress, port, port);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  public Kryo getKryo() {
    return client.getKryo();
  }
}
