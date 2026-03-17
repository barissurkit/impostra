package com.impostra.client.ui;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.impostra.common.Network;

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ImpostraGUI extends Application {

    private Stage primaryStage;
    private Label statusLabel;
    private Client client;
    private VBox lobbyPlayerBox = new VBox(10);

    // Hafıza Değişkenleri
    private String myUsername = "";
    private String[] currentPlayers;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Platform.setImplicitExit(true);

        showLoginScreen();

        primaryStage.setTitle("Impostra: Digital Shift");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public void showLoginScreen() {
        StackPane root = new StackPane();

        try {
            String imagePath = getClass().getResource("/assets/background.jpg").toExternalForm();
            root.setStyle("-fx-background-image: url('" + imagePath + "'); -fx-background-size: cover; -fx-background-position: center;");
        } catch (Exception e) {
            root.setStyle("-fx-background-color: #0a0a0c;");
        }

        VBox loginPanel = new VBox(25);
        loginPanel.setAlignment(Pos.CENTER);
        loginPanel.setMaxWidth(450);
        loginPanel.setMaxHeight(550);
        loginPanel.setOpacity(0);

        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00FBFF", 0.7)),
                new Stop(1, Color.web("#FF8C00", 0.7))
        );
        loginPanel.setBackground(new Background(new BackgroundFill(gradient, new CornerRadii(20), Insets.EMPTY)));

        loginPanel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.7); -fx-background-radius: 20; -fx-padding: 50; -fx-border-color: #FF8C00; -fx-border-radius: 20; -fx-border-width: 2;"
        );

        Label titleLabel = new Label("IMPOSTRA");
        titleLabel.setTextFill(Color.web("#00FBFF"));
        titleLabel.setFont(Font.font("Consolas", 80));
        DropShadow glowEffect = new DropShadow(20, Color.web("#00FBFF"));
        titleLabel.setEffect(glowEffect);

        Label subTitle = new Label("> INITIALIZING DIGITAL SHIFT...");
        subTitle.setTextFill(Color.web("#00FBFF"));
        subTitle.setFont(Font.font("Consolas", 16));

        TextField nameField = new TextField();
        nameField.setPromptText("ACCESS_CODE (Nick)...");
        nameField.setMinWidth(300);
        nameField.setMinHeight(45);
        nameField.setStyle(
                "-fx-background-color: #1a1a1c; -fx-text-fill: #00FBFF; -fx-border-color: #FF8C00; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-family: 'Consolas';"
        );

        Button connectBtn = new Button("ENTER THE GRID");
        connectBtn.setMinWidth(300);
        connectBtn.setMinHeight(55);
        connectBtn.setCursor(javafx.scene.Cursor.HAND);

        String baseButtonStyle = "-fx-background-color: #00FBFF; -fx-text-fill: #000000; -fx-font-weight: bold; -fx-font-family: 'Consolas'; -fx-font-size: 18px; -fx-background-radius: 15;";

        DropShadow buttonGlow = new DropShadow(15, Color.web("#00FBFF"));
        connectBtn.setEffect(buttonGlow);
        connectBtn.setStyle(baseButtonStyle);

        statusLabel = new Label("Waiting for authorization...");
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setFont(Font.font("Consolas", 14));

        loginPanel.getChildren().addAll(titleLabel, subTitle, nameField, connectBtn, statusLabel);
        root.getChildren().add(loginPanel);
        StackPane.setAlignment(loginPanel, Pos.CENTER);

        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setScene(scene);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), loginPanel);
        fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.0), titleLabel);
        pulse.setFromX(1.0); pulse.setFromY(1.0); pulse.setToX(1.05); pulse.setToY(1.05);
        pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true); pulse.play();

        ScaleTransition buttonHoverScale = new ScaleTransition(Duration.millis(150), connectBtn);
        ScaleTransition buttonClickScale = new ScaleTransition(Duration.millis(100), connectBtn);

        connectBtn.setOnMouseEntered(e -> {
            connectBtn.setStyle("-fx-background-color: #FF8C00; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-family: 'Consolas'; -fx-font-size: 18px;");
            buttonGlow.setColor(Color.web("#FF8C00")); buttonGlow.setRadius(25);
            buttonHoverScale.setToX(1.1); buttonHoverScale.setToY(1.1); buttonHoverScale.playFromStart();
        });

        connectBtn.setOnMouseExited(e -> {
            connectBtn.setStyle(baseButtonStyle);
            buttonGlow.setColor(Color.web("#00FBFF")); buttonGlow.setRadius(15);
            buttonHoverScale.setToX(1.0); buttonHoverScale.setToY(1.0); buttonHoverScale.playFromStart();
        });

        connectBtn.setOnMousePressed(e -> { buttonClickScale.setToX(0.95); buttonClickScale.setToY(0.95); buttonClickScale.playFromStart(); });
        connectBtn.setOnMouseReleased(e -> { buttonClickScale.setToX(1.0); buttonClickScale.setToY(1.0); buttonClickScale.playFromStart(); });

        connectBtn.setOnAction(e -> {
            String nick = nameField.getText().trim();
            if (nick.isEmpty()) {
                statusLabel.setText("ERROR: ACCESS_CODE CANNOT BE EMPTY!");
                statusLabel.setTextFill(Color.RED);
                return;
            }

            statusLabel.setText("CONNECTING TO SERVER...");
            statusLabel.setTextFill(Color.web("#00FBFF"));
            connectBtn.setDisable(true);

            new Thread(() -> {
                try {
                    if (client != null) client.stop();

                    client = new Client();
                    Network.register(client);
                    client.start();
                    client.connect(5000, "127.0.0.1", 54555, 54777);

                    client.addListener(new Listener() {
                        @Override
                        public void received(Connection connection, Object object) {
                            if (object instanceof Network.JoinResponse) {
                                Network.JoinResponse cevap = (Network.JoinResponse) object;
                                Platform.runLater(() -> {
                                    if (cevap.isAccepted) {
                                        showLobbyScreen();
                                    } else {
                                        statusLabel.setText("ACCESS DENIED: " + cevap.message);
                                        statusLabel.setTextFill(Color.RED);
                                        connectBtn.setDisable(false);
                                    }
                                });
                            }

                            if (object instanceof Network.LobbyUpdatePacket) {
                                Network.LobbyUpdatePacket lobiPaketi = (Network.LobbyUpdatePacket) object;
                                Platform.runLater(() -> {
                                    if (lobbyPlayerBox != null) {
                                        lobbyPlayerBox.getChildren().clear();
                                        for (String oyuncu : lobiPaketi.connectedPlayers) {
                                            Label lbl = new Label("> " + oyuncu + " (Bağlandı)");
                                            lbl.setTextFill(Color.web("#00FF41"));
                                            lbl.setFont(Font.font("Consolas", 16));
                                            lobbyPlayerBox.getChildren().add(lbl);
                                        }
                                    }
                                });
                            }

                            if (object instanceof Network.GameStartedPacket) {
                                Network.GameStartedPacket oyunPaketi = (Network.GameStartedPacket) object;
                                Platform.runLater(() -> {
                                    showGameScreen(oyunPaketi.assignedRole, oyunPaketi.isEvil, oyunPaketi.playerList);
                                });
                            }

                            // 4. SABAH PAKETİ GELDİYSE OYLAMA EKRANINI AÇ
                            if (object instanceof Network.MorningPacket) {
                                Network.MorningPacket sabahPaketi = (Network.MorningPacket) object;
                                Platform.runLater(() -> {
                                    showVotingScreen(sabahPaketi.morningMessage);
                                });
                            }
                        }
                    });

                    Network.JoinRequest istek = new Network.JoinRequest();
                    istek.username = nick;
                    myUsername = nick;
                    client.sendTCP(istek);

                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setText("CONNECTION FAILED: Sunucu Çevrimdışı!");
                        statusLabel.setTextFill(Color.RED);
                        connectBtn.setDisable(false);
                    });
                }
            }).start();
        });
    }

    public void showLobbyScreen() {
        StackPane lobbyRoot = new StackPane();
        lobbyRoot.setStyle("-fx-background-color: #0a0a0c;");

        VBox lobbyBox = new VBox(20);
        lobbyBox.setAlignment(Pos.CENTER);

        Label title = new Label("BEKLEME ODASI (LOBİ)");
        title.setTextFill(Color.web("#00FBFF"));
        title.setFont(Font.font("Consolas", 50));
        title.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,251,255,0.7), 10, 0, 0, 0);");

        Label infoLabel = new Label("Sisteme bağlanan diğer kullanıcılar bekleniyor...");
        infoLabel.setTextFill(Color.LIGHTGRAY);
        infoLabel.setFont(Font.font("Consolas", 18));

        lobbyPlayerBox.setAlignment(Pos.CENTER);
        lobbyPlayerBox.setStyle("-fx-background-color: #1a1a1c; -fx-padding: 20; -fx-border-color: #FF8C00; -fx-border-radius: 10; -fx-background-radius: 10;");
        lobbyPlayerBox.setMaxWidth(400);

        lobbyBox.getChildren().addAll(title, infoLabel, lobbyPlayerBox);
        lobbyRoot.getChildren().add(lobbyBox);

        Scene lobbyScene = new Scene(lobbyRoot, 1024, 768);
        primaryStage.setScene(lobbyScene);
    }

    public void showGameScreen(String role, boolean isEvil, String[] playerList) {
        this.currentPlayers = playerList; // Oylama ekranı için listeyi hafızaya al

        StackPane gameRoot = new StackPane();
        gameRoot.setStyle("-fx-background-color: #0a0a0c;");

        VBox gameBox = new VBox(20);
        gameBox.setAlignment(Pos.CENTER);

        Label title = new Label("SİSTEM AĞI AKTİF - GÖREV BAŞLADI");
        title.setTextFill(Color.web("#FF0055"));
        title.setFont(Font.font("Consolas", 40));
        title.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(255,0,85,0.7), 10, 0, 0, 0);");

        String takim = isEvil ? "[VİRÜS / HACKER]" : "[SİSTEM / GÜVENLİK]";
        Color rolRengi = isEvil ? Color.RED : Color.web("#00FBFF");

        Label roleLabel = new Label("GİZLİ KİMLİĞİN: " + role + " " + takim);
        roleLabel.setTextFill(rolRengi);
        roleLabel.setFont(Font.font("Consolas", 28));
        roleLabel.setStyle("-fx-font-weight: bold;");

        VBox playersBox = new VBox(10);
        playersBox.setAlignment(Pos.CENTER);
        playersBox.setStyle("-fx-background-color: #1a1a1c; -fx-padding: 20; -fx-border-color: #FF0055; -fx-border-radius: 10;");
        playersBox.setMaxWidth(450);

        Label listTitle = new Label("AĞDAKİ KULLANICILAR (HEDEFLER):");
        listTitle.setTextFill(Color.LIGHTGRAY);
        listTitle.setFont(Font.font("Consolas", 16));
        playersBox.getChildren().add(listTitle);

        for (String p : playerList) {
            HBox playerRow = new HBox(15);
            playerRow.setAlignment(Pos.CENTER);

            Label pLabel = new Label("> " + p);
            pLabel.setTextFill(Color.web("#00FF41"));
            pLabel.setFont(Font.font("Consolas", 18));

            if (!p.equals(myUsername)) {
                Button actionBtn = new Button("SİSTEMİNE SIZ / KORU");
                actionBtn.setStyle("-fx-background-color: #FF0055; -fx-text-fill: white; -fx-font-family: 'Consolas'; -fx-cursor: hand; -fx-font-weight: bold;");

                actionBtn.setOnAction(e -> {
                    Network.NightActionPacket aksiyon = new Network.NightActionPacket();
                    aksiyon.targetPlayerName = p;
                    client.sendTCP(aksiyon);

                    actionBtn.setText("İLETİLDİ (SİSTEM BEKLENİYOR)");
                    actionBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: #00FF41; -fx-font-family: 'Consolas';");
                    actionBtn.setDisable(true);
                });

                playerRow.getChildren().addAll(pLabel, actionBtn);
            } else {
                Label meLabel = new Label(" (BU SENSİN)");
                meLabel.setTextFill(Color.GRAY);
                meLabel.setFont(Font.font("Consolas", 14));
                playerRow.getChildren().addAll(pLabel, meLabel);
            }
            playersBox.getChildren().add(playerRow);
        }

        gameBox.getChildren().addAll(title, roleLabel, playersBox);
        gameRoot.getChildren().add(gameBox);

        Scene gameScene = new Scene(gameRoot, 1024, 768);
        primaryStage.setScene(gameScene);
    }

    // ==========================================
    // YEPYENİ: GÜNDÜZ EKRANI (OYLAMA FAZI)
    // ==========================================
    public void showVotingScreen(String message) {
        StackPane voteRoot = new StackPane();
        voteRoot.setStyle("-fx-background-color: #0a0a0c;");

        VBox voteBox = new VBox(20);
        voteBox.setAlignment(Pos.CENTER);

        Label title = new Label("GÜNDÜZ - AĞ OYLAMASI");
        title.setTextFill(Color.web("#FFD700")); // Altın/Sarı neon
        title.setFont(Font.font("Consolas", 40));
        title.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(255,215,0,0.7), 10, 0, 0, 0);");

        Label infoLabel = new Label("> " + message);
        infoLabel.setTextFill(Color.LIGHTGRAY);
        infoLabel.setFont(Font.font("Consolas", 20));

        VBox playersBox = new VBox(10);
        playersBox.setAlignment(Pos.CENTER);
        playersBox.setStyle("-fx-background-color: #1a1a1c; -fx-padding: 20; -fx-border-color: #FFD700; -fx-border-radius: 10;");
        playersBox.setMaxWidth(450);

        for (String p : currentPlayers) {
            HBox playerRow = new HBox(15);
            playerRow.setAlignment(Pos.CENTER);

            Label pLabel = new Label("> " + p);
            pLabel.setTextFill(Color.web("#00FF41"));
            pLabel.setFont(Font.font("Consolas", 18));

            if (!p.equals(myUsername)) {
                Button voteBtn = new Button("ŞÜPHELİ OLARAK OY VER");
                voteBtn.setStyle("-fx-background-color: #FFD700; -fx-text-fill: black; -fx-font-family: 'Consolas'; -fx-cursor: hand; -fx-font-weight: bold;");

                voteBtn.setOnAction(e -> {
                    Network.VotePacket oy = new Network.VotePacket();
                    oy.votedPlayerName = p;
                    client.sendTCP(oy);

                    voteBtn.setText("OY KULLANILDI");
                    voteBtn.setStyle("-fx-background-color: #555555; -fx-text-fill: #FFD700; -fx-font-family: 'Consolas';");
                    voteBtn.setDisable(true);
                });
                playerRow.getChildren().addAll(pLabel, voteBtn);
            } else {
                Label meLabel = new Label(" (BU SENSİN)");
                meLabel.setTextFill(Color.GRAY);
                meLabel.setFont(Font.font("Consolas", 14));
                playerRow.getChildren().addAll(pLabel, meLabel);
            }
            playersBox.getChildren().add(playerRow);
        }

        voteBox.getChildren().addAll(title, infoLabel, playersBox);
        voteRoot.getChildren().add(voteBox);

        Scene voteScene = new Scene(voteRoot, 1024, 768);
        primaryStage.setScene(voteScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}