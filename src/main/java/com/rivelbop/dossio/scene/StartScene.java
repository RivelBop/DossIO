package com.rivelbop.dossio.scene;

import com.rivelbop.dossio.Main;
import com.rivelbop.dossio.networking.ClientHandler;
import com.rivelbop.dossio.networking.Network;
import com.rivelbop.dossio.networking.ServerHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/** Start up scene - handles hosting, joining, etc. */
public final class StartScene extends Scene {
  private final Main main;
  private final GridPane grid;

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
    grid.setAlignment(Pos.CENTER);
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(25, 25, 25, 25));

    hostUiInit();
    joinUiInit();
  }

  /** Create UI that helps with hosting. */
  private void hostUiInit() {
    hostTextField.setPromptText(Network.DEFAULT_IP_ADDRESS + ":" + Network.DEFAULT_PORT);
    grid.add(hostTextField, 1, 1);

    Network network = main.getNetwork();
    ServerHandler serverHandler = network.getServerHandler();
    ClientHandler clientHandler = network.getClientHandler();

    hostButton.setText("Host");
    hostButton.setOnAction(
        event -> {
          serverHandler.setIpAddress(hostTextField.getText());
          serverHandler.start();

          clientHandler.setIpAddress(hostTextField.getText());
          clientHandler.connect();

          main.getPrimaryStage().setScene(new ProjectScene(main));
        });

    HBox hostBox = new HBox(10);
    hostBox.setAlignment(Pos.CENTER);
    hostBox.getChildren().add(hostButton);
    grid.add(hostBox, 1, 2);
  }

  /** Create UI that helps with connecting. */
  private void joinUiInit() {
    joinTextField.setPromptText(Network.DEFAULT_IP_ADDRESS + ":" + Network.DEFAULT_PORT);
    grid.add(joinTextField, 1, 3);

    ClientHandler clientHandler = main.getNetwork().getClientHandler();

    joinButton.setText("Join");
    joinButton.setOnAction(
        event -> {
          clientHandler.setIpAddress(joinTextField.getText());
          clientHandler.connect();

          main.getPrimaryStage().setScene(new ProjectScene(main));
        });

    HBox joinBox = new HBox(10);
    joinBox.setAlignment(Pos.CENTER);
    joinBox.getChildren().add(joinButton);
    grid.add(joinBox, 1, 4);
  }
}
