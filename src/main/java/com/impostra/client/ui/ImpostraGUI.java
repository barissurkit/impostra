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

    // Ağ motorumuz
    private Client client;

    // --- İŞTE O KAYIP KUTU BURADA ---
    // Dinamik lobi kutumuzu en baştan, boş bir şekilde yaratıyoruz
    private VBox lobbyPlayerBox = new VBox(10);

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Platform.setImplicitExit(true); // Mac uyumluluk ayarı

        showLoginScreen();

        primaryStage.setTitle("Impostra: Digital Shift");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public void showLoginScreen() {
        // Ana katman (StackPane) - Arka plan için
        StackPane root = new StackPane();

        // --- ARKA PLAN RESMİ AYARI ---
        try {
            String imagePath = getClass().getResource("/assets/background.jpg").toExternalForm();
            root.setStyle("-fx-background-image: url('" + imagePath + "'); " +
                    "-fx-background-size: cover; " +
                    "-fx-background-position: center;");
        } catch (Exception e) {
            root.setStyle("-fx-background-color: #0a0a0c;");
            System.out.println("Resim yüklenemedi, klasör yolunu kontrol et! -> resources/assets/background.jpg");
        }

        // --- GİRİŞ PANELİ (Tam Ortada) ---
        VBox loginPanel = new VBox(25);
        loginPanel.setAlignment(Pos.CENTER);
        loginPanel.setMaxWidth(450);
        loginPanel.setMaxHeight(550);
        loginPanel.setOpacity(0); // Başlangıçta şeffaf (Fade-In için)

        // --- MAVİ-TURUNCU GRADYAN VE OKUNAKLI ARKA PLAN ---
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00FBFF", 0.7)),
                new Stop(1, Color.web("#FF8C00", 0.7))
        );
        loginPanel.setBackground(new Background(new BackgroundFill(gradient, new CornerRadii(20), Insets.EMPTY)));

        loginPanel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.7); " +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 50; " +
                        "-fx-border-color: #FF8C00; " +
                        "-fx-border-radius: 20; " +
                        "-fx-border-width: 2;"
        );

        // --- Başlık ve Parıltı Efekti ---
        Label titleLabel = new Label("IMPOSTRA");
        titleLabel.setTextFill(Color.web("#00FBFF"));
        titleLabel.setFont(Font.font("Consolas", 80));
        DropShadow glowEffect = new DropShadow(20, Color.web("#00FBFF"));
        titleLabel.setEffect(glowEffect);

        Label subTitle = new Label("> INITIALIZING DIGITAL SHIFT...");
        subTitle.setTextFill(Color.web("#00FBFF"));
        subTitle.setFont(Font.font("Consolas", 16));

        // --- Giriş Kutusu ---
        TextField nameField = new TextField();
        nameField.setPromptText("ACCESS_CODE (Nick)...");
        nameField.setMinWidth(300);
        nameField.setMinHeight(45);
        nameField.setStyle(
                "-fx-background-color: #1a1a1c; " +
                        "-fx-text-fill: #00FBFF; " +
                        "-fx-border-color: #FF8C00; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-font-family: 'Consolas';"
        );

        // --- Modern Buton ---
        Button connectBtn = new Button("ENTER THE GRID");
        connectBtn.setMinWidth(300);
        connectBtn.setMinHeight(55);
        connectBtn.setCursor(javafx.scene.Cursor.HAND);

        String baseButtonStyle =
                "-fx-background-color: #00FBFF; " +
                        "-fx-text-fill: #000000; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-size: 18px; " +
                        "-fx-background-radius: 15;";

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

        // --- ANİMASYONLAR ---
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), loginPanel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.0), titleLabel);
        pulse.setFromX(1.0); pulse.setFromY(1.0); pulse.setToX(1.05); pulse.setToY(1.05);
        pulse.setCycleCount(Animation.INDEFINITE); pulse.setAutoReverse(true);
        pulse.play();

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

        connectBtn.setOnMousePressed(e -> {
            buttonClickScale.setToX(0.95); buttonClickScale.setToY(0.95); buttonClickScale.playFromStart();
        });

        connectBtn.setOnMouseReleased(e -> {
            buttonClickScale.setToX(1.0); buttonClickScale.setToY(1.0); buttonClickScale.playFromStart();
        });

        // ==========================================
        // GERÇEK SUNUCU BAĞLANTISI VE LOBİ DİNLEME
        // ==========================================
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

                    // IP ADRESİ: Arkadaşınla oynarken burayı Hamachi IP yapacaksın!
                    client.connect(5000, "127.0.0.1", 54555, 54777);

                    client.addListener(new Listener() {
                        @Override
                        public void received(Connection connection, Object object) {

                            // 1. SUNUCU BİZİ KABUL ETTİYSE LOBİ EKRANINI AÇ
                            if (object instanceof Network.JoinResponse) {
                                Network.JoinResponse cevap = (Network.JoinResponse) object;
                                Platform.runLater(() -> {
                                    if (cevap.isAccepted) {
                                        showLobbyScreen(); // LOBİYE GEÇİŞ
                                    } else {
                                        statusLabel.setText("ACCESS DENIED: " + cevap.message);
                                        statusLabel.setTextFill(Color.RED);
                                        connectBtn.setDisable(false);
                                    }
                                });
                            }

                            // 2. SUNUCUDAN YENİ OYUNCU LİSTESİ GELDİYSE KUTUYU GÜNCELLE
                            if (object instanceof Network.LobbyUpdatePacket) {
                                Network.LobbyUpdatePacket lobiPaketi = (Network.LobbyUpdatePacket) object;
                                Platform.runLater(() -> {
                                    if (lobbyPlayerBox != null) {
                                        lobbyPlayerBox.getChildren().clear(); // Eski yazıları sil

                                        // Gelen güncel listedeki herkesi tek tek kutuya ekle
                                        for (String oyuncu : lobiPaketi.connectedPlayers) {
                                            Label lbl = new Label("> " + oyuncu + " (Bağlandı)");
                                            lbl.setTextFill(Color.web("#00FF41")); // Mavi/Yeşil neon
                                            lbl.setFont(Font.font("Consolas", 16));
                                            lobbyPlayerBox.getChildren().add(lbl);
                                        }
                                    }
                                });
                            }
                        }
                    });

                    // Bağlantı başarılıysa, ismimizi sunucuya gönder
                    Network.JoinRequest istek = new Network.JoinRequest();
                    istek.username = nick;
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

    // ==========================================
    // BEKLEME ODASI (LOBİ) EKRAN TASARIMI
    // ==========================================
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

        // En üstte tanımladığımız Dinamik Kutunun sadece stilini ayarlıyoruz
        lobbyPlayerBox.setAlignment(Pos.CENTER);
        lobbyPlayerBox.setStyle("-fx-background-color: #1a1a1c; -fx-padding: 20; -fx-border-color: #FF8C00; -fx-border-radius: 10; -fx-background-radius: 10;");
        lobbyPlayerBox.setMaxWidth(400);

        lobbyBox.getChildren().addAll(title, infoLabel, lobbyPlayerBox);
        lobbyRoot.getChildren().add(lobbyBox);

        Scene lobbyScene = new Scene(lobbyRoot, 1024, 768);
        primaryStage.setScene(lobbyScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}