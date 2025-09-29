package com.rivelbop.dossio;

import com.rivelbop.dossio.networking.Network;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** JavaFX UI handling. Handles properties (resolution, network, etc.). */
public final class Main extends Application {

  /** The JavaFX window default width. */
  public static final int WINDOW_WIDTH = 640;

  /** The JavaFX window default height. */
  public static final int WINDOW_HEIGHT = 480;

  /** Kryonet network access for the whole program. */
  public static final Network NETWORK = new Network();

  private final Button hostButton = new Button();
  private final Button joinButton = new Button();
  private final TextField hostTextField = new TextField();
  private final TextField joinTextField = new TextField();
  private final GridPane grid = new GridPane();

  public static void main(String[] args) {
    launch(args);
  }

  /**
   * Window and UI properties (positioning, functionality, etc.).
   *
   * <p>TODO: Split into multiple methods or classes.
   */
  @Override
  public void start(Stage primaryStage) {
    primaryStage.setTitle("DossIO Welcome!");

    grid.setAlignment(Pos.CENTER);
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(25, 25, 25, 25));

    // Host ui
    hostTextField.setPromptText(Network.DEFAULT_IP_ADDRESS + ":" + Network.DEFAULT_PORT);
    grid.add(hostTextField, 1, 1);

    hostButton.setText("Host");
    hostButton.setOnAction(
        event -> {
          NETWORK.getServerHandler().setIpAddress(hostTextField.getText());
          NETWORK.getClientHandler().setIpAddress(hostTextField.getText());
          NETWORK.getServerHandler().start();
          NETWORK.getClientHandler().connect();
        });

    HBox hBox = new HBox(10);
    hBox.setAlignment(Pos.CENTER);
    hBox.getChildren().add(hostButton);
    grid.add(hBox, 1, 2);

    // Join ui
    joinTextField.setPromptText(Network.DEFAULT_IP_ADDRESS + ":" + Network.DEFAULT_PORT);
    grid.add(joinTextField, 1, 3);

    joinButton.setText("Join");
    joinButton.setOnAction(
        event -> {
          NETWORK.getClientHandler().setIpAddress(joinTextField.getText());
          NETWORK.getClientHandler().connect();
        });

    HBox jBox = new HBox(10);
    jBox.setAlignment(Pos.CENTER);
    jBox.getChildren().add(joinButton);
    grid.add(jBox, 1, 4);

    // Primary stage properties
    primaryStage.setScene(new Scene(grid, WINDOW_WIDTH, WINDOW_HEIGHT, Color.LIGHTGRAY));
    primaryStage.setResizable(false);
    primaryStage.show();
  }

  @Override
  public void stop() throws Exception {
    super.stop();
    NETWORK.dispose();
  }
}
