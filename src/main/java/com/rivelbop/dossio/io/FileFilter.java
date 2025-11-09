package com.rivelbop.dossio.io;

import com.esotericsoftware.minlog.Log;
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
  private static final String LOG_TAG = "FileFilter";

  private final IgnoreNode ignoreNode = new IgnoreNode();
  private final Path tempDirectory;

  /**
   * Creates a file filter for the project directory using .dosshide and .gitignore files.
   *
   * @param projectDirectory The base project directory to filter.
   * @param tempDirectory The temporary file directory to ignore.
   * @param checkGitignore Whether .gitignore files should be checked.
   * @throws IOException If fails to create new ignore files or read ignore files (when parsing).
   */
  public FileFilter(Path projectDirectory, Path tempDirectory, boolean checkGitignore)
      throws IOException {
    // Parse the .dosshide file
    File dosshideFile = projectDirectory.resolve(".dosshide").toFile();
    dosshideFile.createNewFile(); // If never created, make an empty one
    try (InputStream in = new FileInputStream(dosshideFile)) {
      ignoreNode.parse(in);
    }

    // Parse the .gitignore file
    if (checkGitignore) {
      File gitignoreFile = projectDirectory.resolve(".gitignore").toFile();
      gitignoreFile.createNewFile(); // If never created, make an empty one
      try (InputStream in = new FileInputStream(gitignoreFile)) {
        ignoreNode.parse(in);
      }
    }

    // Store the temporary directory to help ignore temporary file events
    this.tempDirectory = tempDirectory;
  }

  /**
   * Checks if a file path is ignored based on .dosshide and/or .gitignore, or if it is temporary.
   *
   * @param relativeFilePath The relative path to the file.
   * @param absoluteFilePath The absolute path to the file.
   * @return Whether the file is ignored using the ignore node (parsed ignored files).
   */
  public boolean isIgnored(Path relativeFilePath, Path absoluteFilePath) {
    // If the file is temporary, ignore it
    if (absoluteFilePath.startsWith(tempDirectory)) {
      return true;
    }

    // If the file is considered hidden or a dotfile, don't send it
    boolean isHidden;
    try {
      isHidden = Files.isHidden(absoluteFilePath);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Unable to detect if file is hidden!", e);
      isHidden = false;
    }
    if (isHidden || relativeFilePath.getFileName().toString().startsWith(".")) {
      return true;
    }

    // Check if the file is ignored by the parsed ignore files
    Path relativeIterator = relativeFilePath;
    Path absoluteIterator = absoluteFilePath;
    while (relativeIterator != null) {
      if (ignoreNode.isIgnored(
              FileHandler.pathToNetworkString(relativeIterator),
              Files.isDirectory(absoluteIterator))
          == MatchResult.IGNORED) {
        return true;
      }

      relativeIterator = relativeIterator.getParent();
      absoluteIterator = absoluteIterator.getParent();
    }

    return false;
  }
}
