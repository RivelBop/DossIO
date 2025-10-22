package com.rivelbop.dossio.scene;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.minlog.Log;
import com.rivelbop.dossio.app.Main;
import com.rivelbop.dossio.io.FileHandler;
import com.rivelbop.dossio.networking.ClientHandler;
import com.rivelbop.dossio.networking.ClientListener;
import com.rivelbop.dossio.networking.Packet.BeginEditPacket;
import com.rivelbop.dossio.networking.Packet.ClientDataPacket;
import com.rivelbop.dossio.networking.Packet.DisconnectClientPacket;
import com.rivelbop.dossio.networking.Packet.EditPacket;
import com.rivelbop.dossio.networking.Packet.EndEditPacket;
import com.rivelbop.dossio.networking.ServerHandler;
import java.io.File;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;

/** Shows clients and selection of directory to sync. */
public final class ProjectScene extends Scene {
  private static final String LOG_TAG = "ProjectScene";

  private final Main main;
  private final VBox verticalBox;

  private final MenuBar menuBar = new MenuBar();

  private final ListView<String> clientList = new ListView<>();

  // File Menu
  private final Menu fileMenu = new Menu("File");
  private final MenuItem openMenuItem = new MenuItem("Open");
  private final MenuItem closeMenuItem = new MenuItem("Close");

  // Help Menu
  private final Menu helpMenu = new Menu("Help");
  private final MenuItem keybindsMenuItem = new MenuItem("Keybinds");
  private final MenuItem docsMenuItem = new MenuItem("Documentation");
  private final MenuItem contributeMenuItem = new MenuItem("Contribute");

  // Directory selection
  private final Text enterText = new Text("Press Enter to Choose Path.");
  private final DirectoryChooser directoryChooser = new DirectoryChooser();
  private FileHandler fileHandler;
  private boolean directorySelected;

  /**
   * Creates UI elements and button event handlers.
   *
   * @param main Access to the main application.
   */
  public ProjectScene(Main main) {
    super(new VBox(10), Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);

    this.main = main;

    ServerHandler serverHandler = Main.NETWORK.getServerHandler();
    ClientHandler clientHandler = Main.NETWORK.getClientHandler();

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

    clientList.setPrefSize(200, 200);
    clientList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    clientList.setDisable(true);

    HBox clientsBox = new HBox(10);
    clientsBox.setAlignment(Pos.BOTTOM_CENTER);
    clientsBox.getChildren().add(clientList);
    verticalBox.getChildren().add(clientsBox);

    clientHandler.setClientListener(
        new ClientListener() {
          @Override
          public void connected(Connection connection) {
            // Add yourself to the client list
            clientList.getItems().add(clientHandler.getUsername() + "[" + connection.getID() + "]");
          }

          @Override
          public void received(Connection connection, Object object) {
            if (object instanceof ClientDataPacket p) {
              clientList.getItems().add(p.username + "[" + p.id + "]"); // Add client to list
            } else if (object instanceof DisconnectClientPacket p) {
              clientList.getItems().remove(p.id - 1); // Remove client from list
            } else if (object instanceof BeginEditPacket
                || object instanceof EditPacket
                || object instanceof EndEditPacket) {
              // Interpret received edit packet data
              if (fileHandler != null) {
                fileHandler.interpretEdit(object);
              }
            }
          }

          @Override
          public void disconnected(Connection connection) {
            // Intentionally left empty
          }
        });

    // Choose directory at start
    showFinder(true);
  }

  /** Create menu bar (file, help, etc.). */
  private void menuBarInit() {
    // File Menu
    openMenuItem.setOnAction(
        event -> {
          if (!directorySelected) {
            showFinder(true);
          }
        });
    closeMenuItem.setOnAction(event -> main.getPrimaryStage().close());
    fileMenu.getItems().addAll(openMenuItem, closeMenuItem);

    // Help Menu
    // TODO: Set proper action for keybinds
    docsMenuItem.setOnAction(
        event ->
            main.getHostServices()
                .showDocument("https://github.com/RivelBop/DossIO/blob/main/README.md#dossio"));
    keybindsMenuItem.setOnAction(
        event ->
            main.getHostServices()
                .showDocument(
                    "https://github.com/RivelBop/DossIO/blob/main/README.md#quick-start"));
    contributeMenuItem.setOnAction(
        event ->
            main.getHostServices()
                .showDocument(
                    "https://github.com/RivelBop/DossIO/blob/main/.github/CONTRIBUTING.md"));
    helpMenu.getItems().addAll(docsMenuItem, keybindsMenuItem, contributeMenuItem);

    menuBar.getMenus().addAll(fileMenu, helpMenu);
  }

  /** Handles shortcuts to do specific events. */
  private void input() {
    this.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.Q) {
            main.getPrimaryStage().close();
          }

          if (!directorySelected && event.getCode() == KeyCode.ENTER) {
            showFinder(true); // TODO: Allow to choose to check .gitignore
          }

          if (event.getCode() == KeyCode.I) {
            // Intentionally left empty
            // TODO: Make info window about networking like ping, amount of players in, and more
          }

          if (event.getCode() == KeyCode.H) {
            main.getHostServices().showDocument("https://github.com/RivelBop/DossIO");
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
      directorySelected = true;
      verticalBox.getChildren().remove(enterText);
      clientList.setDisable(false);
      openMenuItem.setDisable(true);

      fileHandler = new FileHandler(selectedFile, checkGitignore);
      Log.info(LOG_TAG, "Selected Project Directory: " + selectedFile);
    }
  }
}
