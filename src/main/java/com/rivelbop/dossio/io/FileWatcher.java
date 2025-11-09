package com.rivelbop.dossio.io;

import com.esotericsoftware.minlog.Log;
import com.rivelbop.dossio.app.Main;
import io.methvin.watcher.DirectoryWatcher;
import io.methvin.watcher.visitor.DefaultFileTreeVisitor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Watches for file changes in the shared project directory (added, edited, removed). */
public final class FileWatcher {
  private static final String LOG_TAG = "FileWatcher";

  private final DirectoryWatcher watcher;
  private CompletableFuture<Void> watchFuture;

  /**
   * Creates a directory watcher (a superior alternative to Oracle's Watch Service).
   *
   * @param projectDirectory The project directory to register to the watcher.
   * @param handler The file handler to report file updates to.
   * @throws IOException If the file system fails to create a new directory watcher.
   */
  public FileWatcher(Path projectDirectory, FileHandler handler) throws IOException {
    // FIXME: Make a custom tree visitor that takes the ignore files into account
    watcher =
        DirectoryWatcher.builder()
            .path(projectDirectory)
            .listener(
                event -> {
                  switch (event.eventType()) {
                    case CREATE -> handler.onCreate(projectDirectory.resolve(event.path()));
                    case MODIFY -> handler.onModify(projectDirectory.resolve(event.path()));
                    case DELETE -> handler.onDelete(projectDirectory.resolve(event.path()));
                    default -> {}
                  }
                })
            .fileTreeVisitor(new DefaultFileTreeVisitor())
            .build();
  }

  /** Starts the file watcher thread. */
  public void start() {
    Log.info(LOG_TAG, "Starting file watcher...");
    watchFuture = watcher.watchAsync();
    watchFuture.whenComplete(
        (result, exception) -> {
          if (exception != null) {
            // The watcher stopped because of an error
            Log.error(LOG_TAG, "File watcher stopped unexpectedly!", exception);

            // Alert the user of the error
            Main.showErrorAlert(
                "File Watcher Error", "File Watcher Stopped Unexpectedly", exception.toString());
          }
        });
  }

  /** Stops the file watcher loop. */
  public void end() {
    Log.info(LOG_TAG, "Closing file watcher...");
    try {
      watcher.close();
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to close watch service!", e);
      Main.showErrorAlert("File Watcher Error", "Failed to Close File Watcher", e.toString());
    }
  }
}
