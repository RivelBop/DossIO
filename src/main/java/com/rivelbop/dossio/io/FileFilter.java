package com.rivelbop.dossio.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.ignore.IgnoreNode.MatchResult;

/** Filters files in a directory based on the provided .gitignore and .dosshide files. */
public final class FileFilter {
  private final IgnoreNode ignoreNode = new IgnoreNode();

  /**
   * Creates a file filter for a directory using .dosshide and .gitignore files.
   *
   * @param directory The base directory to filter.
   * @param checkGitignore Whether .gitignore files should be checked.
   * @throws IOException If fails to create new ignore files or read ignore files (when parsing).
   */
  public FileFilter(Path directory, boolean checkGitignore) throws IOException {
    // Parse the .dosshide file
    File dosshideFile = directory.resolve(".dosshide").toFile();
    dosshideFile.createNewFile(); // If never created, make an empty one
    try (InputStream in = new FileInputStream(dosshideFile)) {
      ignoreNode.parse(in);
    }

    // Parse the .gitignore file
    if (checkGitignore) {
      File gitignoreFile = directory.resolve(".gitignore").toFile();
      gitignoreFile.createNewFile(); // If never created, make an empty one
      try (InputStream in = new FileInputStream(gitignoreFile)) {
        ignoreNode.parse(in);
      }
    }
  }

  /**
   * Checks if a file path is not ignored based on .dosshide and/or .gitignore.
   *
   * @param relativeFilePath The relative path to the file.
   * @param absoluteFilePath The absolute path to the file.
   * @return Whether the file isn't ignored using the ignore node (parsed ignored files).
   */
  public boolean isNotIgnored(Path relativeFilePath, Path absoluteFilePath) {
    return ignoreNode.isIgnored(relativeFilePath.toString(), Files.isDirectory(absoluteFilePath))
        != MatchResult.IGNORED;
  }
}
