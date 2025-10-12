package com.rivelbop.dossio;

import com.esotericsoftware.minlog.Log;
import com.rivelbop.dossio.networking.Network;
import com.rivelbop.dossio.scene.StartScene;
import java.awt.Taskbar;
import java.awt.Taskbar.Feature;
import java.awt.Toolkit;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** JavaFX GUI initial handler - additional properties (resolution, network, etc.). */
public final class Main extends Application {
  /** The JavaFX default window width. */
  public static final int WINDOW_WIDTH = 640;

  /** The JavaFX default window height. */
  public static final int WINDOW_HEIGHT = 480;

  private final Network network = new Network();

  private Stage primaryStage;

  /**
   * Alternative JavaFX launcher (not used for FAT Jar).
   *
   * @param args Java Program Arguments
   */
  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) {
    Log.set(Log.LEVEL_DEBUG); // TODO: Remove on release
    this.primaryStage = primaryStage;

    // TODO: Change test icon
    Image appIcon = new Image("/images/icon.png");
    primaryStage.getIcons().add(appIcon);

    if (Taskbar.isTaskbarSupported()) {
      Taskbar taskbar = Taskbar.getTaskbar();

      if (taskbar.isSupported(Feature.ICON_IMAGE)) {
        final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        var dockIcon = defaultToolkit.getImage(getClass().getResource("/images/icon.png"));
        taskbar.setIconImage(dockIcon);
      }
    }

    StartScene startScene = new StartScene(this);
    primaryStage.setScene(startScene);
    primaryStage.setMinWidth(WINDOW_WIDTH);
    primaryStage.setMinHeight(WINDOW_HEIGHT);
    primaryStage.setResizable(false);
    primaryStage.show();
  }

  @Override
  public void stop() throws Exception {
    super.stop();
    network.dispose();
  }

  public Network getNetwork() {
    return network;
  }

  public Stage getPrimaryStage() {
    return primaryStage;
  }
}
