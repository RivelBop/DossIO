package com.rivelbop.dossio.io;

import com.esotericsoftware.minlog.Log;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Core of file handling, it handles file changes detected by the file watcher, filters them out,
 * and sends necessary packet updates.
 */
public final class FileHandler {
  private static final String LOG_TAG = "FileHandler";

  private final Path directoryPath;

  private final FileWatcher fileWatcher;
  private final FileFilter fileFilter;

  /**
   * Creates a file handler (initializes the watcher and filter).
   *
   * @param directoryFile The base project directory to be shared and updated.
   * @param checkGitIgnore Whether a ".gitignore" file can be used to ignore certain file changes.
   * @throws IllegalArgumentException If the selected file is not a directory.
   */
  public FileHandler(File directoryFile, boolean checkGitIgnore) {
    // Ensure the selected file is a directory
    if (!directoryFile.isDirectory()) {
      throw new IllegalArgumentException("Selected file must be a directory!");
    }

    // Ensure absolute file path
    directoryPath = directoryFile.toPath().toAbsolutePath();

    fileWatcher = new FileWatcher(this);
    try {
      fileFilter = new FileFilter(directoryPath, checkGitIgnore);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    registerDirectory(directoryPath); // Register all nested directories to file watcher
    fileWatcher.start(); // Start watching for file changes on a separate thread
  }

  /**
   * Called when file creation is detected.
   *
   * @param absoluteFilePath The absolute path of the created file.
   */
  public void onCreate(Path absoluteFilePath) {
    Path relativeFilePath = directoryPath.relativize(absoluteFilePath);
    if (fileFilter.isNotIgnored(relativeFilePath, absoluteFilePath)) {
      Log.info(LOG_TAG, "CREATED: " + absoluteFilePath);
      // TODO: Complete this!
    }
  }

  /**
   * Called when file modification is detected.
   *
   * @param absoluteFilePath The absolute path of the modified file.
   */
  public void onModify(Path absoluteFilePath) {
    Path relativeFilePath = directoryPath.relativize(absoluteFilePath);
    if (fileFilter.isNotIgnored(relativeFilePath, absoluteFilePath)) {
      Log.info(LOG_TAG, "MODIFIED: " + absoluteFilePath);
      // TODO: Complete this!
    }
  }

  /**
   * Called when file deletion is detected.
   *
   * @param absoluteFilePath The absolute path of the deleted file.
   */
  public void onDelete(Path absoluteFilePath) {
    Path relativeFilePath = directoryPath.relativize(absoluteFilePath);
    if (fileFilter.isNotIgnored(relativeFilePath, absoluteFilePath)) {
      Log.info(LOG_TAG, "DELETED: " + absoluteFilePath);
      // TODO: Complete this!
    }
  }

  /** Ends the file watch update thread. */
  public void close() {
    fileWatcher.end();
  }

  /**
   * This registers nested directories of a directory to the file watcher, ensuring it detects all
   * file changes.
   *
   * @param initialDirectory The path of the directory to watch.
   * @throws RuntimeException If an IO error occurs when accessing the initial directory.
   */
  private void registerDirectory(Path initialDirectory) {
    try (Stream<Path> stream = Files.walk(initialDirectory)) {
      stream
          .filter(Files::isDirectory)
          .filter(subDir -> fileFilter.isNotIgnored(initialDirectory.relativize(subDir), subDir))
          .forEach(fileWatcher::register);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
