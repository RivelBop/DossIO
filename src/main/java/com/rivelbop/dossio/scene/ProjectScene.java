package com.rivelbop.dossio.scene;

import com.esotericsoftware.minlog.Log;
import com.rivelbop.dossio.Main;
import com.rivelbop.dossio.io.FileHandler;
import com.rivelbop.dossio.networking.ClientHandler;
import com.rivelbop.dossio.networking.Network;
import com.rivelbop.dossio.networking.ServerHandler;
import java.io.File;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;

/** Shows clients and selection of directory to sync. */
public final class ProjectScene extends Scene {
  private final Main main;
  private final VBox verticalBox;

  private final MenuBar menuBar = new MenuBar();

  // File Menu
  private final Menu fileMenu = new Menu("File");
  private final MenuItem openMenuItem = new MenuItem("Open");
  private final MenuItem closeMenuItem = new MenuItem("Close");

  // Help Menu
  private final Menu helpMenu = new Menu("Help");
  private final MenuItem keybindsMenuItem = new MenuItem("Keybinds");
  private final MenuItem docsMenuItem = new MenuItem("Documentation");
  private final MenuItem noGuiMenuItem = new MenuItem("No GUI Setup");
  private final MenuItem contributeMenuItem = new MenuItem("Contribute");

  // Directory selection
  private final Text enterText = new Text("Press Enter to Choose Path.");
  private final DirectoryChooser directoryChooser = new DirectoryChooser();
  private FileHandler fileHandler;

  /**
   * Creates UI elements and button event handlers.
   *
   * @param main Access to the main application.
   */
  public ProjectScene(Main main) {
    super(new VBox(10), Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);

    this.main = main;

    Network network = main.getNetwork();
    ServerHandler serverHandler = network.getServerHandler();
    ClientHandler clientHandler = network.getClientHandler();

    String title;
    if (serverHandler.isRunning()) {
      title = "Hosting to " + serverHandler.getIpAddress();
    } else {
      title = "Joined " + clientHandler.getIpAddress();
    }
    main.getPrimaryStage().setTitle(title);

    directoryChooser.setTitle("Open Resource File");
    enterText.setFont(Font.font("Tahoma", FontWeight.EXTRA_BOLD, 20));
    enterText.setOpacity(0.2);

    menuBarInit();
    input();

    verticalBox = (VBox) this.getRoot();
    verticalBox.setAlignment(Pos.TOP_CENTER);
    verticalBox.getChildren().addAll(menuBar, enterText);
  }

  /** Create menu bar (file, help, etc.). */
  private void menuBarInit() {
    // File Menu
    openMenuItem.setOnAction(
        event -> {
          showFinder(true);
          verticalBox.getChildren().remove(enterText);
        });
    closeMenuItem.setOnAction(event -> main.getPrimaryStage().close());
    fileMenu.getItems().addAll(openMenuItem, closeMenuItem);

    // Help Menu
    // TODO: Set proper action for each
    keybindsMenuItem.setOnAction(
        event -> main.getHostServices().showDocument("https://github.com/RivelBop/DossIO"));
    docsMenuItem.setOnAction(
        event -> main.getHostServices().showDocument("https://github.com/RivelBop/DossIO"));
    noGuiMenuItem.setOnAction(
        event -> main.getHostServices().showDocument("https://github.com/RivelBop/DossIO"));
    contributeMenuItem.setOnAction(
        event -> main.getHostServices().showDocument("https://github.com/RivelBop/DossIO"));
    helpMenu.getItems().addAll(keybindsMenuItem, docsMenuItem, noGuiMenuItem, contributeMenuItem);

    menuBar.getMenus().addAll(fileMenu, helpMenu);
  }

  /** Handles shortcuts to do specific events. */
  private void input() {
    this.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.Q) {
            main.getPrimaryStage().close();
          }

          if (event.getCode() == KeyCode.ENTER) {
            showFinder(true); // TODO: Allow to choose to check .gitignore
            verticalBox.getChildren().remove(enterText);
          }
        });
  }

  /**
   * Show finder and allow directory selection.
   *
   * @param checkGitignore Whether .gitignore files can filter files (alongside .dosshide).
   */
  private void showFinder(boolean checkGitignore) {
    File selectedFile = directoryChooser.showDialog(main.getPrimaryStage());
    if (selectedFile != null) {
      if (fileHandler != null) {
        fileHandler.close();
      }
      fileHandler = new FileHandler(selectedFile, checkGitignore);
      Log.info("ProjectScene", "Selected file is " + selectedFile);
    }
  }
}
