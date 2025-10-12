package com.rivelbop.dossio.networking;

/** Stores all packets that will be handled through the network. */
public final class Packet {
  private Packet() {}

  /** Stores the username and ID of the client. */
  public static final class ClientDataPacket {
    public String username;
    public int id;
  }

  /** Stores the disconnecting client's ID. */
  public static final class DisconnectClientPacket {
    public int id;
  }
}
