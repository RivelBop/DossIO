package com.rivelbop.dossio.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.Sequence;
import org.eclipse.jgit.diff.SequenceComparator;

/** A utility class for efficient file comparisons. */
public final class FileComparer {
  private static final StringSequenceComparator STRING_COMPARATOR = new StringSequenceComparator();

  private FileComparer() {}

  /**
   * Compares the text between two groups of lines.
   *
   * @param linesA The lines from source A.
   * @param linesB The lines from source B.
   * @return The list of edits required to transform lines A to lines B.
   */
  public static EditList compareText(List<String> linesA, List<String> linesB) {
    // Create sequences from the lines
    StringListSequence seqA = new StringListSequence(linesA);
    StringListSequence seqB = new StringListSequence(linesB);

    // Get the list of edits using MyersDiff
    return MyersDiff.INSTANCE.diff(STRING_COMPARATOR, seqA, seqB);
  }

  /**
   * Compares the text of two files.
   *
   * @param absPathA The absolute path to file A.
   * @param absPathB The absolute path to file B.
   * @return The list of edits required to transform file A's content into file B's content.
   * @throws IOException IO error occurred when reading file A and/or B.
   */
  public static EditList compareText(Path absPathA, Path absPathB) throws IOException {
    // Read text from files A+B and compare text
    return compareText(Files.readAllLines(absPathA), Files.readAllLines(absPathB));
  }

  /** A string list sequence for MyersDiff algorithm. */
  public static final class StringListSequence extends Sequence {
    private final List<String> lines;

    /**
     * Stores a list of strings to serve as lines to compare.
     *
     * @param lines The "lines" of strings to serve as a sequence.
     */
    public StringListSequence(List<String> lines) {
      this.lines = lines;
    }

    @Override
    public int size() {
      return lines.size();
    }

    /**
     * Returns the string at the provided line index (from the string list).
     *
     * @param index The line index to retrieve the string from.
     * @return The string at indexed line.
     */
    public String get(int index) {
      return lines.get(index);
    }
  }

  /**
   * Used to compare portions of two string list sequences and discover the minimal edits required
   * to transform from one sequence to the other.
   */
  public static final class StringSequenceComparator
      extends SequenceComparator<StringListSequence> {
    @Override
    public boolean equals(StringListSequence a, int ai, StringListSequence b, int bi) {
      return a.get(ai).equals(b.get(bi));
    }

    @Override
    public int hash(StringListSequence seq, int i) {
      return seq.get(i).hashCode();
    }
  }
}
