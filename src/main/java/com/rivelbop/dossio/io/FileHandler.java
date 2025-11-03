package com.rivelbop.dossio.io;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.esotericsoftware.minlog.Log;
import com.rivelbop.dossio.app.Main;
import com.rivelbop.dossio.networking.ClientHandler;
import com.rivelbop.dossio.networking.Packet.BeginEditPacket;
import com.rivelbop.dossio.networking.Packet.CreateFilePacket;
import com.rivelbop.dossio.networking.Packet.DeleteFilePacket;
import com.rivelbop.dossio.networking.Packet.EditPacket;
import com.rivelbop.dossio.networking.Packet.EndEditPacket;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.util.FileUtils;

/**
 * Core file handling, it handles file changes detected by the file watcher, filters them out, and
 * sends necessary packet updates.
 */
public final class FileHandler {
  private static final String LOG_TAG = "FileHandler";

  private final Path projectDirectoryPath;

  private final Path tempDirectoryPath;
  private final ConcurrentHashMap<Path, Path> cachedTempPaths = new ConcurrentHashMap<>();

  private final FileWatcher fileWatcher;
  private final FileFilter fileFilter;

  private final EditInterpreter editInterpreter = new EditInterpreter();

  private final ClientHandler clientHandler = Main.NETWORK.getClientHandler();
  private final Set<String> filesMarkedForCreation = Collections.synchronizedSet(new HashSet<>());
  private final Set<String> filesMarkedForModification =
      Collections.synchronizedSet(new HashSet<>());
  private final Set<String> filesMarkedForDeletion = Collections.synchronizedSet(new HashSet<>());

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

    registerDirectory(
        projectDirectoryPath, false); // Register nested project directories to file watcher
    fileWatcher.start(); // Start watching for file changes on a separate thread
  }

  /**
   * Checks if a file is a binary file (this helps indicate if a file is not a text file). It checks
   * for a NULL byte in the first 1KB read from the file.
   *
   * @param path The path of the file to check.
   * @return Whether the file contains binary or not.
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
      return false;
    }
    return false;
  }

  /**
   * Checks if a file contains text only (AKA if it is a text file). It checks if the file is not a
   * directory and the content type is text or not binary.
   *
   * @param path The path of the file to check.
   * @return If the file is a text file.
   */
  public static boolean isTextFile(Path path) {
    // Check if directory - therefore not text file
    if (Files.isDirectory(path)) {
      return false;
    }

    // Probe file content type
    String fileType;
    try {
      fileType = Files.probeContentType(path);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to probe content type when checking if text file!", e);
      return false;
    }

    // Check if the file is filled with text or is not a binary file
    return (fileType != null && fileType.startsWith("text/")) || !isBinaryFile(path);
  }

  /**
   * Converts a path to its Unix string representation to send over the network.
   *
   * <p>This is used to ensure cross-platform support, this string can then be converted to the
   * platform it is used for.
   *
   * @param path The path to represent as Unix string path.
   * @return The path's Unix-based network string representation.
   */
  public static String pathToNetworkString(Path path) {
    return path.toString().replace(File.separatorChar, '/');
  }

  /**
   * Converts a network path string to a local one by replacing the Unix-based file separator with
   * the system's file separator.
   *
   * @param pathStr The network path string to convert (should be Unix-based).
   * @return The local string representation of the network path string.
   */
  public static String networkPathStringToLocalPathString(String pathStr) {
    return pathStr.replace('/', File.separatorChar);
  }

  /**
   * Called when file creation is detected.
   *
   * @param absoluteFilePath The absolute path of the created file.
   */
  public void onCreate(Path absoluteFilePath) {
    Path relativeFilePath = projectDirectoryPath.relativize(absoluteFilePath);

    // Check if file is ignored
    if (fileFilter.isIgnored(relativeFilePath, absoluteFilePath)) {
      return;
    }

    Log.info(LOG_TAG, "CREATED: " + absoluteFilePath);

    // Create a temporary file (if the path is to a text file)
    boolean isTextFile = getTempPath(absoluteFilePath) != null;

    // Register directory to the file watcher and ensure it isn't sent to network
    // Most text files are typically expected so the first check is more efficient
    if (!isTextFile && Files.isDirectory(absoluteFilePath)) {
      registerDirectory(absoluteFilePath, true);
      return;
    }

    // Ensure no creation packet is sent if you received a packet to create this file
    String relativePathStr = pathToNetworkString(relativeFilePath);
    if (filesMarkedForCreation.remove(relativePathStr)) {
      return;
    }

    // Send creation packet
    CreateFilePacket createPacket = new CreateFilePacket();
    createPacket.fileName = relativePathStr;
    clientHandler.sendTcp(createPacket);

    // Don't read lines from a non-text file
    if (!isTextFile) {
      return;
    }

    // Read new lines from created file
    List<String> newLines;
    try {
      newLines = Files.readAllLines(absoluteFilePath);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to read created file lines!", e);
      return;
    }

    // If no text data was in the created file, don't send
    EditList editList = FileComparer.compareText(new ArrayList<>(), newLines);
    if (editList.isEmpty()) {
      return;
    }

    // Convert the lines into edit packets
    List<EditPacket> editPackets =
        EditSerializer.toEditPackets(relativePathStr, newLines, editList);

    // Begin sending edit packets to the server
    BeginEditPacket beginPacket = new BeginEditPacket();
    beginPacket.fileName = relativePathStr;
    clientHandler.sendTcp(beginPacket);

    // Send the edit packets
    for (EditPacket p : editPackets) {
      clientHandler.sendTcp(p);
    }

    // Finish sending the edit packets to the server
    EndEditPacket endPacket = new EndEditPacket();
    endPacket.fileName = relativePathStr;
    clientHandler.sendTcp(endPacket);
  }

  /**
   * Called when file modification is detected.
   *
   * @param absoluteFilePath The absolute path of the modified file.
   * @throws RuntimeException If error occurs when copying modified data to temporary file.
   */
  public void onModify(Path absoluteFilePath) {
    Path relativeFilePath = projectDirectoryPath.relativize(absoluteFilePath);
    String fileName = pathToNetworkString(relativeFilePath);

    // This avoids checking the changes in a modification from the network, while also avoiding
    // potential issues when a file is deleted before onModify is called
    if (filesMarkedForModification.remove(fileName) || !Files.exists(absoluteFilePath)) {
      return;
    }

    // Check if file is ignored
    if (fileFilter.isIgnored(relativeFilePath, absoluteFilePath)) {
      return;
    }

    Log.info(LOG_TAG, "MODIFIED: " + absoluteFilePath);

    // Get the temporary text file (if it exists) and check for mismatches
    Path tempFile = getTempPath(absoluteFilePath);
    if (tempFile == null) {
      return;
    }

    // Get the file changes
    List<String> oldLines;
    List<String> newLines;
    try {
      oldLines = Files.readAllLines(tempFile);
      newLines = Files.readAllLines(absoluteFilePath);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to read old and/or new modified file lines!", e);
      return;
    }
    EditList editList = FileComparer.compareText(oldLines, newLines);

    // If no differences were detected, don't proceed
    // This is useful when interpreting edit packet data (which results in modifying the file)
    if (editList.isEmpty()) {
      return;
    }

    // Convert the changes into packets
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
    try {
      Files.copy(absoluteFilePath, tempFile, REPLACE_EXISTING, COPY_ATTRIBUTES);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to copy modified data to temporary file!", e);
      throw new RuntimeException(e); // Prevent wrecking future modifications of this file
    }
  }

  /**
   * Called when file deletion is detected.
   *
   * @param absoluteFilePath The absolute path of the deleted file.
   */
  public void onDelete(Path absoluteFilePath) {
    Path relativeFilePath = projectDirectoryPath.relativize(absoluteFilePath);

    // Check if file is ignored
    if (fileFilter.isIgnored(relativeFilePath, absoluteFilePath)) {
      return;
    }

    Log.info(LOG_TAG, "DELETED: " + absoluteFilePath);

    // Get the temporary text file (if it exists)
    // Automatically deletes the file (if the file at absoluteFilePath doesn't exist)
    getTempPath(absoluteFilePath);

    // Send delete file packet to server
    String relativePathStr = pathToNetworkString(relativeFilePath);
    if (!filesMarkedForDeletion.remove(relativePathStr)
        && Files.exists(absoluteFilePath.getParent())) {
      DeleteFilePacket deletePacket = new DeleteFilePacket();
      deletePacket.fileName = relativePathStr;
      clientHandler.sendTcp(deletePacket);
    }
  }

  /**
   * Interprets an edit packet received from the server and applies the changes to the local file.
   *
   * @param o The edit packet to interpret.
   * @throws RuntimeException If an IO error occurs when writing the file.
   */
  public void interpretEdit(Object o) {
    if (o instanceof BeginEditPacket p) {
      editInterpreter.begin(p);
    } else if (o instanceof EditPacket p) {
      editInterpreter.insert(p);
    } else if (o instanceof EndEditPacket p) {
      Path absFilePath =
          projectDirectoryPath.resolve(networkPathStringToLocalPathString(p.fileName));
      List<String> lines;

      // Read file lines and apply the edits
      try {
        lines = Files.readAllLines(absFilePath);
      } catch (IOException e) {
        Log.error(LOG_TAG, "Failed to read lines from file when interpreting edit!", e);
        return;
      }
      editInterpreter.apply(editInterpreter.end(p), lines);

      filesMarkedForModification.add(p.fileName);

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
   * Creates a specified relative project file.
   *
   * <p>NOTE: This is supposed to be used to create a file from the network.
   *
   * @param fileName The name of the file to create (relative to project directory).
   */
  public void createFile(String fileName) {
    Path absFilePath = projectDirectoryPath.resolve(networkPathStringToLocalPathString(fileName));
    if (Files.exists(absFilePath)) {
      return;
    }

    filesMarkedForCreation.add(fileName);
    try {
      Files.createDirectories(absFilePath.getParent()); // Ensure parent directories exist
      Files.createFile(absFilePath);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to create file!", e);
    }
  }

  /**
   * Deletes a specified relative project file.
   *
   * <p>NOTE: This is supposed to be used to delete a file from the network.
   *
   * @param fileName The name of the file to delete (relative to project directory).
   */
  public void deleteFile(String fileName) {
    Path absFilePath = projectDirectoryPath.resolve(networkPathStringToLocalPathString(fileName));
    if (!Files.exists(absFilePath)) {
      return;
    }

    filesMarkedForDeletion.add(fileName);
    try {
      // Delete if directory
      FileUtils.delete(absFilePath.toFile(), FileUtils.RECURSIVE | FileUtils.SKIP_MISSING);

      // Delete if file (in the case that FileUtils didn't delete it)
      Files.deleteIfExists(absFilePath);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to delete file!", e);
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

    // Ensure the file exists and is a text file
    if (!Files.exists(absolutePath) || !isTextFile(absolutePath)) {
      // If it used to, remove its temp path
      if (tempPath != null) {
        cachedTempPaths.remove(tempPath);
        try {
          Files.delete(tempPath);
        } catch (IOException e) {
          Log.error(LOG_TAG, "Failed to delete temporary text file!", e);
          return null;
        }
      }
      return null;
    }

    // If the file is valid and the path is already cached
    if (tempPath != null) {
      return tempPath;
    }

    // Get the name of the path relative to the project directory path
    String relativePathName = projectDirectoryPath.relativize(absolutePath).toString();
    // Remove any "special" characters from the file name
    String safePathName =
        relativePathName.replace(File.separatorChar, '_').replaceAll("[^a-zA-Z0-9._-]", "_");
    // Ensure file name under 250 chars (Windows limit)
    // Go under 200 here to add the hash
    if (safePathName.length() > 200) {
      safePathName = safePathName.substring(0, 200);
    }

    // Create the temporary path if it wasn't already cached
    String tempPathName = safePathName + "_" + relativePathName.hashCode() + ".tmp";
    try {
      tempPath = Files.copy(absolutePath, tempDirectoryPath.resolve(tempPathName), COPY_ATTRIBUTES);
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to copy data to temporary text file!", e);
      throw new RuntimeException(e); // Can't just return null, may cause issues later
    }
    tempPath.toFile().deleteOnExit();

    // Cache and return the newly created temporary path
    cachedTempPaths.put(absolutePath, tempPath);
    return tempPath;
  }

  /**
   * Registers nested directories of the provided directory to the file watcher, ensuring it detects
   * all file changes. Additionally, it creates the temporary files for all text files.
   *
   * @param absDirectoryPath The absolute path of the directory to register.
   * @throws RuntimeException If an IO error occurs when accessing provided directory and/or
   *     registering a path to the file watcher.
   */
  private void registerDirectory(Path absDirectoryPath, boolean alertNetworkOfNestedFiles) {
    try (Stream<Path> stream = Files.walk(absDirectoryPath)) {
      stream
          .filter(path -> !Files.isSymbolicLink(path))
          .filter(path -> !path.startsWith(tempDirectoryPath))
          .filter(path -> !fileFilter.isIgnored(projectDirectoryPath.relativize(path), path))
          .forEach(
              path -> {
                if (Files.isDirectory(path)) {
                  // Register the directory to the file watcher
                  try {
                    fileWatcher.register(path);
                  } catch (IOException e) {
                    Log.error(LOG_TAG, "Failed to register directory to file watcher!", e);
                    throw new RuntimeException(e); // Can't continue if it possibly refers to root
                  }
                } else if (alertNetworkOfNestedFiles) {
                  // Alert the network of the created file
                  onCreate(path);
                } else {
                  // Create and copy existing text data to a temporary file
                  getTempPath(path);
                }
              });
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to access directory to register nested directories!", e);
      throw new RuntimeException(e);
    }
  }
}
