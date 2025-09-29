package com.rivelbop.dossio.networking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import java.io.IOException;
import java.net.InetSocketAddress;

/** Handles Kryonet {@link Server} - starting, stopping, sending/receiving packets. */
public final class ServerHandler {

  private final Server server = new Server();

  private String ipAddress = Network.DEFAULT_IP_ADDRESS;
  private int port = Network.DEFAULT_PORT;

  /**
   * Sets the listener, binds the IP address, and starts the server. Throws exception if fails.
   *
   * @throws RuntimeException If the server fails to bind to IP address and port.
   */
  public void start() {
    // Set listener
    server.addListener(
        new Listener() {
          @Override
          public void connected(Connection connection) {
            // Intentionally empty
          }

          @Override
          public void received(Connection connection, Object object) {
            // Intentionally empty
          }

          @Override
          public void disconnected(Connection connection) {
            // Intentionally empty
          }
        });

    // Bind the server to a TCP and UDP address
    try {
      server.bind(new InetSocketAddress(ipAddress, port), new InetSocketAddress(ipAddress, port));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Start
    server.start();
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
      throw new RuntimeException(e);
    }
  }

  public String getIpAddress() {
    return ipAddress;
  }

  /**
   * Sets the server's IP address, you MUST restart the server to apply change. Blank addresses will
   * automatically be set to {@link Network#DEFAULT_IP_ADDRESS}.
   *
   * @param ip The IP address to set the server to.
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
   * Sets the server's port, you MUST restart the server to apply change. Ports outside of range
   * (0-{@link Network#MAX_PORT}) will be automatically set to {@link Network#DEFAULT_PORT}.
   *
   * @param port The port to set the server to.
   */
  public void setPort(int port) {
    if (port < 0 || port > Network.MAX_PORT) {
      this.port = Network.DEFAULT_PORT;
      return;
    }
    this.port = port;
  }

  public Kryo getKryo() {
    return server.getKryo();
  }
}
