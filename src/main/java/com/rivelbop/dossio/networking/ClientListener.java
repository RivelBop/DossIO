package com.rivelbop.dossio.networking;

import com.esotericsoftware.kryonet.Connection;

/** Handles what happens when a specific action over the network is done. */
public interface ClientListener {
  /**
   * Called when connected to a server.
   *
   * @param connection The TCP/UDP connection between the client and server.
   */
  void connected(Connection connection);

  /**
   * Called when a packet is received from the server.
   *
   * @param connection The TCP/UDP connection between the client and server.
   * @param object The packet received from the server.
   */
  void received(Connection connection, Object object);

  /**
   * Called when disconnected from a server.
   *
   * @param connection The TCP/UDP connection between the client and server.
   */
  void disconnected(Connection connection);
}
