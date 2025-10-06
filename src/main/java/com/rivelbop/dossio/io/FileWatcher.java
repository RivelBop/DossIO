package com.rivelbop.dossio.io;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.atomic.AtomicBoolean;

/** Watches for file changes in the shared project directory (added, edited, removed). */
public final class FileWatcher extends Thread {
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
   * @throws RuntimeException If the file system fails to create a new default watch service.
   */
  public FileWatcher(FileHandler handler) {
    try {
      watcher = FileSystems.getDefault().newWatchService();
      this.handler = handler;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.setDaemon(true); // Does not prevent the JVM from exiting when the program finishes
  }

  /**
   * Register a file directory to the file watcher.
   *
   * @param filePath The file path directory to register.
   * @throws RuntimeException If an IO error occurs when registering the file path to the watcher.
   */
  public void register(Path filePath) {
    try {
      filePath.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
        isWatching.set(false);
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
        Path filePath = directory.resolve(ev.context()); // Context() returns relative path

        // Let the file handler handle the file events
        if (event.kind() == ENTRY_CREATE) {
          handler.onCreate(filePath);
        } else if (event.kind() == ENTRY_MODIFY) {
          handler.onModify(filePath);
        } else if (event.kind() == ENTRY_DELETE) {
          handler.onDelete(filePath);
        }
      }

      // Reset the key -- this step is critical if you want to
      // receive further watch events.  If the key is no longer valid,
      // the directory is inaccessible so exit the loop.
      boolean valid = key.reset();
      if (!valid) {
        isWatching.set(false);
        break;
      }
    }
  }

  /** Stops the file watcher loop. */
  public void end() {
    isWatching.set(false);
  }
}
