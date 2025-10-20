package com.rivelbop.dossio.io;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.KryoBufferOverflowException;
import com.esotericsoftware.kryo.io.Output;
import com.rivelbop.dossio.networking.Network;
import com.rivelbop.dossio.networking.Packet.EditPacket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;

/** Serializes and interprets edits into edit packets to send over the network. */
public final class EditSerializer extends Serializer<EditPacket> {
  /**
   * Gets the byte size of a string (takes ASCII and UTF_8 into consideration).
   *
   * @param string The string to gather the bytes and count the length of.
   * @return The amount of bytes the string takes up.
   */
  public static int getStringByteSize(String string) {
    if (string.matches("\\A\\p{ASCII}*\\z")) {
      return string.getBytes(StandardCharsets.US_ASCII).length;
    }
    return string.getBytes(StandardCharsets.UTF_8).length;
  }

  /**
   * Converts a list of edits into a list of edit packets to send over the server.
   *
   * @param fileName The name of the file to put the edits into.
   * @param newFileLines The altered lines to gather new line data from.
   * @param edits The edits to convert into edit packets.
   * @return The resulting edit packets from edit data.
   */
  public static List<EditPacket> toEditPackets(
      String fileName, List<String> newFileLines, EditList edits) {
    ArrayList<EditPacket> packets = new ArrayList<>();
    for (Edit e : edits) {
      packets.addAll(toEditPackets(fileName, newFileLines, e));
    }
    return packets;
  }

  /**
   * Converts an edit into a list of edit packets (accounts for buffer overflow).
   *
   * @param fileName The name of the file to put the edits into.
   * @param newFileLines The altered lines to gather new line data from.
   * @param edit The edit to convert into edit packet(s).
   * @return The resulting edit packet(s) from edit.
   */
  public static List<EditPacket> toEditPackets(
      String fileName, List<String> newFileLines, Edit edit) {
    // Keep track of all the edit packets that need to be sent from the given edit
    ArrayList<EditPacket> packets = new ArrayList<>();

    // Split edit into multiple packets if INSERT or REPLACE
    Edit.Type type = edit.getType();
    switch (type) {
      case INSERT, REPLACE -> {
        // Store the ideal buffer size of an edit packet
        final int packetBufferSize = Network.BUFFER_SIZE / 2;

        // Keeps track of the starting index of each edit packet
        int packetBeginIndex = edit.getBeginB();

        // Keep track of the lines that will be sent with the current edit packet
        ArrayList<String> packetLines = new ArrayList<>();
        int packetLinesByteSize = 0;

        // Loop through the new line changes
        for (int i = packetBeginIndex; i < edit.getEndB(); i++) {
          String line = newFileLines.get(i);

          // Check if the current line is larger than the network buffer size
          int currLineByteSize = getStringByteSize(line);
          if (currLineByteSize >= Network.BUFFER_SIZE) {
            throw new KryoBufferOverflowException("Line [" + i + "] from new file is too large!");
          }

          // Keep track of the packet's total byte size
          packetLinesByteSize += currLineByteSize;

          // If the packet is over the recommended packet buffer size (it is time to send)
          if (packetLinesByteSize >= packetBufferSize) {
            boolean overflowsBuffer = packetLinesByteSize >= Network.BUFFER_SIZE;
            if (!overflowsBuffer) { // If the current line doesn't overflow the buffer
              packetLines.add(line); // Add the line to the packet's lines to send
            }
            packetLines.trimToSize(); // Trim the lines in the packet to ensure smaller array

            EditPacket packet = new EditPacket();
            packet.fileName = fileName;
            packet.type = type;

            // Convert the lines array list to a normal string array
            packet.lines = packetLines.toArray(new String[0]);

            int overflowIndex = overflowsBuffer ? 0 : 1;
            if (type == Edit.Type.REPLACE) {
              // Start of file A + offset
              packet.start = edit.getBeginA() + (packetBeginIndex - edit.getBeginB());

              // Ignore the current line if it causes overflow
              packet.end = packet.start + (i - packetBeginIndex + overflowIndex);
            } else {
              // The insert index is same for both start and end
              packet.start = packet.end = edit.getBeginA();
            }

            // Add the packet to the result packet list
            packets.add(packet);

            // Reset packet lines and move packet begin index to new position for next packet
            packetLinesByteSize = 0;
            packetBeginIndex = i + overflowIndex;
            packetLines.clear();

            // If the line overflowed the buffer, make sure not to skip it on the next loop
            if (overflowsBuffer) {
              i--;
            }
          } else {
            packetLines.add(line);
          }
        }

        // Send unsent packet lines (since they are under the buffer size)
        if (!packetLines.isEmpty()) {
          EditPacket packet = new EditPacket();
          packet.fileName = fileName;
          packet.type = type;
          packet.lines = packetLines.toArray(new String[0]);
          if (type == Edit.Type.REPLACE) {
            packet.start = edit.getBeginA() + (packetBeginIndex - edit.getBeginB());
            packet.end = edit.getEndA();
          } else {
            packet.start = packet.end = edit.getBeginA();
          }
          packets.add(packet);
        }
      }
      case DELETE -> {
        EditPacket packet = new EditPacket();
        packet.fileName = fileName;
        packet.type = edit.getType();
        packet.start = edit.getBeginA();
        packet.end = edit.getEndA();
        packets.add(packet);
      }
      default -> {}
    }
    return packets;
  }

  @Override
  public void write(Kryo kryo, Output output, EditPacket packet) {
    output.writeString(packet.fileName);
    kryo.writeObject(output, packet.type);
    kryo.writeObjectOrNull(output, packet.lines, String[].class);
    output.writeInt(packet.start, true);
    if (packet.type != Edit.Type.INSERT) {
      output.writeInt(packet.end, true);
    }
  }

  @Override
  public EditPacket read(Kryo kryo, Input input, Class<? extends EditPacket> type) {
    EditPacket packet = new EditPacket();
    packet.fileName = input.readString();
    packet.type = kryo.readObject(input, Edit.Type.class);
    packet.lines = kryo.readObjectOrNull(input, String[].class);
    packet.start = input.readInt(true);
    if (packet.type == Edit.Type.INSERT) {
      packet.end = packet.start;
    } else {
      packet.end = input.readInt(true);
    }
    return packet;
  }
}
