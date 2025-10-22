package com.rivelbop.dossio.io;

import com.google.common.collect.ObjectArrays;
import com.rivelbop.dossio.networking.Packet.BeginEditPacket;
import com.rivelbop.dossio.networking.Packet.EditPacket;
import com.rivelbop.dossio.networking.Packet.EndEditPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Interprets and consolidates edit packets before applying them to a file's lines. */
public final class EditInterpreter {
  private final HashMap<String, List<EditPacket>> pendingEdits = new HashMap<>();

  /**
   * Begins tracking edits for a specific file.
   *
   * @param packet The begin edit packet containing the file name.
   */
  public void begin(BeginEditPacket packet) {
    pendingEdits.put(packet.fileName, new ArrayList<>());
  }

  /**
   * Inserts an edit packet into the pending edits for a specific file.
   *
   * @param edit The edit packet to insert.
   */
  public void insert(EditPacket edit) {
    List<EditPacket> edits = pendingEdits.get(edit.fileName);

    // Check if the previous edit can be merged with the current edit
    if (!edits.isEmpty()) {
      EditPacket prevEdit = edits.getLast();
      if (prevEdit.type == edit.type && prevEdit.end == edit.start) {
        prevEdit.lines = ObjectArrays.concat(prevEdit.lines, edit.lines, String.class);
        prevEdit.end = edit.end;
        return;
      }
    }
    // Otherwise, just add the edit normally
    edits.add(edit);
  }

  /**
   * Ends tracking edits for a specific file and returns the consolidated edits.
   *
   * @param packet The end edit packet containing the file name.
   * @return The list of consolidated edit packets for the file.
   */
  public List<EditPacket> end(EndEditPacket packet) {
    return pendingEdits.remove(packet.fileName).reversed();
  }

  /**
   * Applies a list of finalized edits to the given lines.
   *
   * @param finalizedEdits The list of finalized edit packets to apply.
   * @param lines The lines to apply the edits to.
   */
  public void apply(List<EditPacket> finalizedEdits, List<String> lines) {
    for (EditPacket edit : finalizedEdits) {
      switch (edit.type) {
        case REPLACE:
          // Delete the old lines to be replaced
          if (edit.end > edit.start) {
            lines.subList(edit.start, edit.end).clear();
          }
        // Fall through
        case INSERT:
          // Insert the new lines
          for (int i = edit.lines.length - 1; i > -1; i--) {
            lines.add(edit.start, edit.lines[i]);
          }
          break;
        case DELETE:
          if (edit.end > edit.start) {
            lines.subList(edit.start, edit.end).clear();
          }
          break;
        default:
          break;
      }
    }
  }
}
