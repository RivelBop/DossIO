package com.rivelbop.dossio.scene;

import com.google.common.base.Splitter;
import com.rivelbop.dossio.Main;
import com.rivelbop.dossio.networking.ClientHandler;
import com.rivelbop.dossio.networking.Network;
import com.rivelbop.dossio.networking.ServerHandler;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/** Start up scene - handles hosting, joining, etc. */
public final class StartScene extends Scene {
  private final Main main;
  private final GridPane grid;

  private final TextField usernameTextField = new TextField();
  private final Text usernameText = new Text("Username");

  private final TextField hostTextField = new TextField();
  private final Button hostButton = new Button();

  private final TextField joinTextField = new TextField();
  private final Button joinButton = new Button();

  /**
   * Initializes UI elements.
   *
   * @param main Access to the main application.
   */
  public StartScene(Main main) {
    super(new GridPane(), Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT, Color.LIGHTGRAY);

    this.main = main;
    main.getPrimaryStage().setTitle("DossIO - Home");

    grid = (GridPane) this.getRoot();
    grid.setAlignment(Pos.TOP_CENTER);
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(25, 25, 25, 25));

    Text versionText = new Text(Main.class.getPackage().getImplementationVersion());
    versionText.setFont(Font.font("Tahoma", FontWeight.NORMAL, 12));
    versionText.setOpacity(0.1);

    HBox versionBox = new HBox(10);
    versionBox.setAlignment(Pos.CENTER);
    versionBox.getChildren().add(versionText);
    grid.add(versionBox, 1, 27);

    usernameUiInit();
    hostUiInit();
    joinUiInit();
  }

  /** Create UI that handles the username. */
  private void usernameUiInit() {
    usernameTextField.setPromptText("Set username");
    usernameText.setFont(Font.font("Tahoma", FontWeight.SEMI_BOLD, 20));
    usernameText.setOpacity(0.2);

    HBox usernameTextBox = new HBox(10);
    usernameTextBox.setAlignment(Pos.TOP_CENTER);
    usernameTextBox.getChildren().add(usernameText);
    grid.add(usernameTextBox, 1, 0);
    grid.add(usernameTextField, 1, 1);
  }

  /** Create UI that helps with hosting. */
  private void hostUiInit() {
    hostTextField.setPromptText(Network.DEFAULT_IP_ADDRESS + ":" + Network.DEFAULT_PORT);
    grid.add(hostTextField, 1, 6);

    Network network = main.getNetwork();
    ServerHandler serverHandler = network.getServerHandler();
    ClientHandler clientHandler = network.getClientHandler();

    hostButton.setText("Host");
    hostButton.setOnAction(
        event -> {
          List<String> ipText = Splitter.on(':').splitToList(hostTextField.getText());
          boolean hasPort = ipText.size() > 1;

          serverHandler.setIpAddress(ipText.get(0));
          if (hasPort) {
            serverHandler.setPort(Integer.parseInt(ipText.get(1)));
          }
          serverHandler.start();

          clientHandler.setIpAddress(ipText.get(0));
          clientHandler.setPort(serverHandler.getPort());
          clientHandler.setUsername(usernameTextField.getText());

          // Switch scene first to set client handler listener before connecting
          main.getPrimaryStage().setScene(new ProjectScene(main));
          clientHandler.connect();
        });

    HBox hostBox = new HBox(10);
    hostBox.setAlignment(Pos.CENTER);
    hostBox.getChildren().add(hostButton);
    grid.add(hostBox, 1, 7);
  }

  /** Create UI that helps with connecting. */
  private void joinUiInit() {
    joinTextField.setPromptText(Network.DEFAULT_IP_ADDRESS + ":" + Network.DEFAULT_PORT);
    grid.add(joinTextField, 1, 8);

    ClientHandler clientHandler = main.getNetwork().getClientHandler();

    joinButton.setText("Join");
    joinButton.setOnAction(
        event -> {
          List<String> ipText = Splitter.on(':').splitToList(joinTextField.getText());
          boolean hasPort = ipText.size() > 1;

          clientHandler.setIpAddress(ipText.get(0));
          if (hasPort) {
            clientHandler.setPort(Integer.parseInt(ipText.get(1)));
          }
          clientHandler.setUsername(usernameTextField.getText());

          // Switch scene first to set client handler listener before connecting
          main.getPrimaryStage().setScene(new ProjectScene(main));
          clientHandler.connect();
        });

    HBox joinBox = new HBox(10);
    joinBox.setAlignment(Pos.CENTER);
    joinBox.getChildren().add(joinButton);
    grid.add(joinBox, 1, 9);
  }
}
