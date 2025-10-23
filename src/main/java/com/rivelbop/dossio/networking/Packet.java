package com.rivelbop.dossio.networking;

import java.util.Arrays;
import org.eclipse.jgit.diff.Edit;

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

  /** Alerts a client of upcoming edit packets for a file. */
  public static final class BeginEditPacket {
    public String fileName;
  }

  /** Stores the file edit data to send. */
  public static final class EditPacket {
    public String fileName;
    public Edit.Type type;
    public String[] lines;
    public int start;
    public int end;

    @Override
    public String toString() {
      return String.format(
          "EditPacket[%s]: Type[%s], Lines[%s], Start[%d], End[%d]",
          fileName, type, Arrays.toString(lines), start, end);
    }
  }

  /** Alerts a client when all edit packets are sent for a file. */
  public static final class EndEditPacket {
    public String fileName;
  }

  /** Alerts clients to delete a specific file. */
  public static final class DeleteFilePacket {
    public String fileName;
  }
}
