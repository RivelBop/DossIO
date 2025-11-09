package com.rivelbop.dossio.app;

import com.esotericsoftware.minlog.Log;
import com.rivelbop.dossio.networking.Network;
import com.rivelbop.dossio.scene.StartScene;
import java.awt.Taskbar;
import java.awt.Taskbar.Feature;
import java.awt.Toolkit;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** JavaFX GUI initial handler - additional properties (resolution, network, etc.). */
public final class Main extends Application {
  /** The JavaFX default window width. */
  public static final int WINDOW_WIDTH = 640;

  /** The JavaFX default window height. */
  public static final int WINDOW_HEIGHT = 480;

  /** Access to server and client handlers. */
  public static final Network NETWORK = new Network();

  private Stage primaryStage;

  /**
   * Shows an alert dialog with the given parameters.
   *
   * @param type The type of alert.
   * @param title The title of the alert.
   * @param header The header text of the alert.
   * @param content The content text of the alert.
   */
  public static void showAlert(AlertType type, String title, String header, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.show();
  }

  /**
   * Shows an error alert dialog with the given parameters.
   *
   * @param title The title of the alert.
   * @param header The header text of the alert.
   * @param content The content text of the alert.
   */
  public static void showErrorAlert(String title, String header, String content) {
    showAlert(AlertType.ERROR, title, header, content);
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
    NETWORK.dispose();
    super.stop();
  }

  public Stage getPrimaryStage() {
    return primaryStage;
  }
}
