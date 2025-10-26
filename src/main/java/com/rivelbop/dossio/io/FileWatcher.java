package com.rivelbop.dossio.io;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.esotericsoftware.minlog.Log;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;

/** Watches for file changes in the shared project directory (added, edited, removed). */
public final class FileWatcher extends Thread {
  private static final String LOG_TAG = "FileWatcher";

  /**
   * How long to put the file watcher thread to sleep for after retrieving watch key (milliseconds).
   *
   * <p>NOTE: This prevents two separate entry modify events (for modification and timestamp).
   */
  private static final int TAKE_TIMEOUT = 50;

  private final WatchService watcher;
  private final FileHandler handler;

  private final AtomicBoolean isWatching = new AtomicBoolean();

  /**
   * Creates a file watch service.
   *
   * @param handler The file handler to report file updates to.
   * @throws IOException If the file system fails to create a new default watch service.
   */
  public FileWatcher(FileHandler handler) throws IOException {
    watcher = FileSystems.getDefault().newWatchService();
    this.handler = handler;
    this.setDaemon(true); // Does not prevent the JVM from exiting when the program finishes
  }

  /**
   * Register a file directory to the file watcher.
   *
   * @param filePath The file path directory to register.
   * @throws IOException If an IO error occurs when registering the file path to the watcher.
   */
  public void register(Path filePath) throws IOException {
    filePath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
  }

  /** Starts the file watcher thread. */
  @Override
  public void start() {
    super.start();
    isWatching.set(true);
  }

  /** Calls the continuous loop to check for file changes. */
  @Override
  public void run() {
    while (isWatching.get()) {
      // Wait for key to be signaled
      WatchKey key;
      try {
        key = watcher.take(); // Wait for next watch key
        Thread.sleep(TAKE_TIMEOUT); // Prevent receiving two separate ENTRY_MODIFY events
      } catch (InterruptedException e) {
        Log.error(LOG_TAG, "Interruption in watcher thread when waiting for next watch key!", e);
        isWatching.set(false);
        Thread.currentThread().interrupt();
        return;
      }

      Path directory = (Path) key.watchable();
      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();

        if (kind == OVERFLOW) {
          continue;
        }

        // The filePath is the context of the event
        @SuppressWarnings("unchecked")
        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path filePath = directory.resolve(ev.context()); // context() returns relative path

        // Let the file handler handle the file events
        if (event.kind() == ENTRY_CREATE) {
          handler.onCreate(filePath);
        } else if (event.kind() == ENTRY_MODIFY) {
          handler.onModify(filePath);
        } else if (event.kind() == ENTRY_DELETE) {
          handler.onDelete(filePath);
        }
      }

      // Reset the key to receive further watch events
      key.reset();
    }
  }

  /** Stops the file watcher loop. */
  public void end() {
    isWatching.set(false);
    try {
      watcher.close();
    } catch (IOException e) {
      Log.error(LOG_TAG, "Failed to close watch service!", e);
    }
  }
}
