package com.rivelbop.dossio.io;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.esotericsoftware.minlog.Log;
import com.rivelbop.dossio.app.Main;
import com.rivelbop.dossio.networking.ClientHandler;
import com.rivelbop.dossio.networking.Packet.BeginEditPacket;
import com.rivelbop.dossio.networking.Packet.DeleteFilePacket;
import com.rivelbop.dossio.networking.Packet.EditPacket;
import com.rivelbop.dossio.networking.Packet.EndEditPacket;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.eclipse.jgit.diff.EditList;

/**
 * Core file handling, it handles file changes detected by the file watcher, filters them out, and
 * sends necessary packet updates.
 */
public final class FileHandler {
  private static final String LOG_TAG = "FileHandler";

  private final Path projectDirectoryPath;

  private final Path tempDirectoryPath;
  private final HashMap<Path, Path> cachedTempPaths = new HashMap<>();

  private final FileWatcher fileWatcher;
  private final FileFilter fileFilter;

  private final EditInterpreter editInterpreter = new EditInterpreter();

  private final ClientHandler clientHandler = Main.NETWORK.getClientHandler();

  /**
   * Creates a file handler (initializes the watcher and filter).
   *
   * @param projectDirectoryFile The base project directory to be shared and updated.
   * @param checkGitignore Whether a ".gitignore" file can be used to ignore certain file changes.
   * @throws IllegalArgumentException If the selected file is not a directory.
   * @throws RuntimeException If temporary directory, file watcher, or file filter fail to create.
   */
  public FileHandler(File projectDirectoryFile, boolean checkGitignore) {
    // Ensure the selected file is a directory
    if (!projectDirectoryFile.isDirectory()) {
      throw new IllegalArgumentException("Selected file must be a directory!");
    }

    // Ensure absolute file path
    projectDirectoryPath = projectDirectoryFile.toPath().toAbsolutePath();

    // Create a temporary directory (for all temporary files)
    try {
      tempDirectoryPath = Files.createTempDirectory(projectDirectoryPath, ".dosstemp");
    } catch (IOException e) {
      Log.error(LOG_TAG, "IO error occurred when creating temporary project directory!", e);
      throw new RuntimeException(e);
    }
    tempDirectoryPath.toFile().deleteOnExit(); // Remove temporary directory when JVM terminates

    // Initialize the file watcher and filter
    try {
      fileWatcher = new FileWatcher(this);
      fileFilter = new FileFilter(projectDirectoryPath, checkGitignore);
    } catch (IOException e) {
      Log.error(LOG_TAG, "The file watching and/or filter services have failed to initialize!", e);
      throw new RuntimeException(e);
    }

    registerProjectDirectory(); // Register all nested directories to file watcher
    fileWatcher.start(); // Start watching for file changes on a separate thread
  }

  /**
   * Checks if a file is a binary file (this helps indicate if a file is not a text file). It checks
   * for a NULL byte in the first 1KB read from the file.
   *
   * @param path The path of the file to check.
   * @return Whether the file contains binary or not.
   * @throws RuntimeException If fails to read file at path.
   */
  public static boolean isBinaryFile(Path path) {
    try (InputStream in = Files.newInputStream(path)) {
      byte[] buffer = new byte[1024]; // Read the first 1KB
      int n = in.read(buffer);
      for (int i = 0; i < n; i++) {
        if (buffer[i] == 0) { // Check for the NULL byte
          return true;
        }
      }
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to read buffer at binary file path!", e);
      throw new RuntimeException(e);
    }
    return false;
  }

  /**
   * Checks if a file contains text only (AKA if it is a text file). It checks if the file is not a
   * directory and the content type is text or not binary.
   *
   * @param path The path of the file to check.
   * @return If the file is a text file.
   * @throws RuntimeException If an IO error occurs when probing the content type of the file.
   */
  public static boolean isTextFile(Path path) {
    String fileType;
    try {
      fileType = Files.probeContentType(path);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to probe content type when checking if text file!", e);
      throw new RuntimeException(e);
    }
    return !Files.isDirectory(path)
        && ((fileType != null && fileType.startsWith("text/")) || !isBinaryFile(path));
  }

  /**
   * Called when file creation is detected.
   *
   * @param absoluteFilePath The absolute path of the created file.
   */
  public void onCreate(Path absoluteFilePath) {
    Path relativeFilePath = projectDirectoryPath.relativize(absoluteFilePath);
    if (fileFilter.isNotIgnored(relativeFilePath, absoluteFilePath)) {
      Log.info(LOG_TAG, "CREATED: " + absoluteFilePath);

      // Create a temporary file (if the path is to a text file)
      getTempPath(absoluteFilePath);

      // TODO: Complete this!
    }
  }

  /**
   * Called when file modification is detected.
   *
   * @param absoluteFilePath The absolute path of the modified file.
   * @throws RuntimeException If an IO error occurs when searching for mismatch.
   */
  public void onModify(Path absoluteFilePath) {
    Path relativeFilePath = projectDirectoryPath.relativize(absoluteFilePath);
    if (fileFilter.isNotIgnored(relativeFilePath, absoluteFilePath)) {
      Log.info(LOG_TAG, "MODIFIED: " + absoluteFilePath);

      // Get the temporary text file (if it exists) and check for mismatches
      Path tempFile = getTempPath(absoluteFilePath);
      if (tempFile != null) {
        try {
          // Get the file changes
          List<String> oldLines = Files.readAllLines(tempFile);
          List<String> newLines = Files.readAllLines(absoluteFilePath);
          EditList editList = FileComparer.compareText(oldLines, newLines);

          // If no differences were detected, don't proceed
          // This is useful when interpreting edit packet data (which results in modifying the file)
          if (editList.isEmpty()) {
            return;
          }

          // Convert the changes into packets
          String fileName = projectDirectoryPath.relativize(absoluteFilePath).toString();
          List<EditPacket> editPackets = EditSerializer.toEditPackets(fileName, newLines, editList);

          // Begin sending edit packets to the server
          BeginEditPacket beginPacket = new BeginEditPacket();
          beginPacket.fileName = fileName;
          clientHandler.sendTcp(beginPacket);

          // Send the edit packets
          for (EditPacket p : editPackets) {
            clientHandler.sendTcp(p);
          }

          // Finish sending the edit packets to the server
          EndEditPacket endPacket = new EndEditPacket();
          endPacket.fileName = fileName;
          clientHandler.sendTcp(endPacket);

          // Copy the new file's contents into the old temporary file (for future comparisons)
          Files.copy(absoluteFilePath, tempFile, REPLACE_EXISTING, COPY_ATTRIBUTES);
        } catch (IOException e) {
          Log.error(LOG_TAG, "Failed to compare files!", e);
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * Called when file deletion is detected.
   *
   * @param absoluteFilePath The absolute path of the deleted file.
   * @throws RuntimeException If IO error occurs when deleting the temporary text file.
   */
  public void onDelete(Path absoluteFilePath) {
    Path relativeFilePath = projectDirectoryPath.relativize(absoluteFilePath);
    if (fileFilter.isNotIgnored(relativeFilePath, absoluteFilePath)) {
      Log.info(LOG_TAG, "DELETED: " + absoluteFilePath);

      // Get the temporary text file (if it exists) and delete it
      Path tempFile = getTempPath(absoluteFilePath);
      if (tempFile != null) {
        try {
          Files.delete(tempFile);
        } catch (IOException e) {
          Log.error(LOG_TAG, "Failed to delete temporary text file!", e);
          throw new RuntimeException(e);
        }
      }

      // Send delete file packet to server
      String relativePathStr = relativeFilePath.toString();
      if (!clientHandler.getFilesMarkedForDeletion().remove(relativePathStr)) {
        DeleteFilePacket deletePacket = new DeleteFilePacket();
        deletePacket.fileName = relativePathStr;
        clientHandler.sendTcp(deletePacket);
      }
    }
  }

  /**
   * Interprets an edit packet received from the server and applies the changes to the local file.
   *
   * @param o The edit packet to interpret.
   * @throws RuntimeException If an IO error occurs when reading or writing the file.
   */
  public void interpretEdit(Object o) {
    if (o instanceof BeginEditPacket p) {
      editInterpreter.begin(p);
    } else if (o instanceof EditPacket p) {
      editInterpreter.insert(p);
    } else if (o instanceof EndEditPacket p) {
      Path absFilePath = projectDirectoryPath.resolve(p.fileName);
      List<String> lines;

      // Read file lines and apply the edits
      try {
        lines = Files.readAllLines(absFilePath);
      } catch (IOException e) {
        Log.error(LOG_TAG, "Failed to read lines from file when interpreting edit!", e);
        throw new RuntimeException(e);
      }
      editInterpreter.apply(editInterpreter.end(p), lines);

      // Write the updated lines to both the temporary and actual files
      try {
        Files.write(Objects.requireNonNull(getTempPath(absFilePath)), lines);
        Files.write(absFilePath, lines);
      } catch (IOException e) {
        Log.error(LOG_TAG, "Failed to write updated lines to file when interpreting edit!", e);
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Deletes a specified relative project file.
   *
   * @param fileName The name of the file to delete (relative to project directory).
   * @throws RuntimeException If an IO error occurs when deleting the file.
   */
  public void deleteFile(String fileName) {
    Path absFilePath = projectDirectoryPath.resolve(fileName);
    try {
      Files.deleteIfExists(absFilePath);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to delete file!", e);
      throw new RuntimeException(e);
    }
  }

  /** Ends the file watch update thread. */
  public void close() {
    fileWatcher.end();
  }

  /**
   * Returns the path to a text file's temporary file. If it doesn't exist, a temporary file for the
   * text file will be created.
   *
   * @param absolutePath The absolute path to the file.
   * @return The path to the file's temporary text file.
   * @throws RuntimeException If an IO error occurs when copying data from original file to temp.
   */
  @CheckForNull
  private Path getTempPath(Path absolutePath) {
    // Check if a temporary path was already cached
    Path tempPath = cachedTempPaths.get(absolutePath);
    if (tempPath != null) {
      return tempPath;
    }

    // Ensure the file exists and is a text file
    if (!Files.exists(absolutePath) || !isTextFile(absolutePath)) {
      return null;
    }

    // Get the name of the path relative to the project directory path
    String relativePathName = projectDirectoryPath.relativize(absolutePath).toString();

    // Create the temporary path if it wasn't already cached
    String tempPathName = relativePathName.replaceAll("/", "_").concat(".tmp");
    try {
      tempPath = Files.copy(absolutePath, tempDirectoryPath.resolve(tempPathName), COPY_ATTRIBUTES);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to copy data to temporary text file!", e);
      throw new RuntimeException(e);
    }
    tempPath.toFile().deleteOnExit();

    // Cache and return the newly created temporary path
    cachedTempPaths.put(absolutePath, tempPath);
    return tempPath;
  }

  /**
   * Registers nested directories of the project directory to the file watcher, ensuring it detects
   * all file changes. Additionally, it creates the temporary files for all text files.
   *
   * @throws RuntimeException If an IO error occurs when accessing the project directory.
   * @throws RuntimeException If an IO error occurs when registering a path to the file watcher.
   */
  private void registerProjectDirectory() {
    try (Stream<Path> stream = Files.walk(projectDirectoryPath)) {
      stream
          .filter(
              path ->
                  !path.equals(tempDirectoryPath) && !path.getParent().equals(tempDirectoryPath))
          .filter(path -> fileFilter.isNotIgnored(projectDirectoryPath.relativize(path), path))
          .forEach(
              path -> {
                if (Files.isDirectory(path)) {
                  // Register the directory to the file watcher
                  try {
                    fileWatcher.register(path);
                  } catch (IOException e) {
                    Log.error(LOG_TAG, "Failed to register directory to file watcher!", e);
                    throw new RuntimeException(e);
                  }
                } else {
                  // Create and copy existing text data to a temporary file
                  getTempPath(path);
                }
              });
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to access project directory to register nested directories!", e);
      throw new RuntimeException(e);
    }
  }
}
