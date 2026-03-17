package com.impostra.client.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.impostra.common.Network;

public class ImpostraGUI extends Application {

    private Stage primaryStage;
    private Label statusLabel;

    private Client client;

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
            //resources/assets/background.jpg dosyasını yükle
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
        loginPanel.setMaxWidth(450); // Panel genişliğini kısıtla
        loginPanel.setMaxHeight(500); // Panel yüksekliğini kısıtla

        // --- OKUNAKLILIK İÇİN ŞEFFAF ARKA PLAN ---
        // Panele yarı şeffaf bir siyahlık ve köşelerine yuvarlaklık ekle
        loginPanel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.7); " +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 40; " +
                        "-fx-border-color: #00FBFF; " +
                        "-fx-border-radius: 20; " +
                        "-fx-border-width: 1;"
        );

        // Başlık
        Label titleLabel = new Label("IMPOSTRA");
        titleLabel.setTextFill(Color.web("#00FBFF")); // Mavi neon tonu
        titleLabel.setFont(Font.font("Consolas", 80));
        // Siberpunk parıltı efekti
        titleLabel.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,251,255,0.9), 15, 0, 0, 0);");

        Label subTitle = new Label("> INITIALIZING DIGITAL SHIFT...");
        subTitle.setTextFill(Color.web("#00FBFF"));
        subTitle.setFont(Font.font("Consolas", 16));

        // Giriş Kutusu (Daha okunaklı siyah arka plan)
        TextField nameField = new TextField();
        nameField.setPromptText("ACCESS_CODE (Nick)...");
        nameField.setMinWidth(300);
        nameField.setMinHeight(45);
        nameField.setStyle(
                "-fx-background-color: #1a1a1c; " + // Koyu okunaklı arka plan
                        "-fx-text-fill: #00FBFF; " +
                        "-fx-border-color: #008F11; " + // Yeşille çerçeve
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-font-family: 'Consolas';"
        );

        // Buton (Town of Salem tarzı yuvarlak köşeler ve parıltı)
        Button connectBtn = new Button("ENTER THE GRID");
        connectBtn.setMinWidth(300);
        connectBtn.setMinHeight(55);
        connectBtn.setCursor(javafx.scene.Cursor.HAND);
        connectBtn.setStyle(
                "-fx-background-color: #00FBFF; " +
                        "-fx-text-fill: #000000; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-size: 18px; " +
                        "-fx-background-radius: 15; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,251,255,0.7), 10, 0, 0, 0);"
        );
        // Hover (üzerine gelince) efekti
        connectBtn.setOnMouseEntered(e -> connectBtn.setStyle("-fx-background-color: #008F11; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-family: 'Consolas'; -fx-font-size: 18px;"));
        connectBtn.setOnMouseExited(e -> connectBtn.setStyle("-fx-background-color: #00FBFF; -fx-text-fill: #000000; -fx-background-radius: 15; -fx-font-family: 'Consolas'; -fx-font-size: 18px;"));

        statusLabel = new Label("Waiting for authorization...");
        statusLabel.setTextFill(Color.LIGHTGRAY);
        statusLabel.setFont(Font.font("Consolas", 14));

        loginPanel.getChildren().addAll(titleLabel, subTitle, nameField, connectBtn, statusLabel);

        // Paneli ana katmanın tam ortasına yerleştir
        root.getChildren().add(loginPanel);
        StackPane.setAlignment(loginPanel, Pos.CENTER);

        Scene scene = new Scene(root, 1024, 768);
        primaryStage.setScene(scene);

        // Buton Aksiyonu
        // Buton Aksiyonu: Gerçek Sunucu Bağlantısı
        connectBtn.setOnAction(e -> {
            String nick = nameField.getText().trim();
            if (nick.isEmpty()) {
                statusLabel.setText("ERROR: ACCESS_CODE CANNOT BE EMPTY!");
                statusLabel.setTextFill(Color.RED);
                return;
            }

            statusLabel.setText("CONNECTING TO SERVER...");
            statusLabel.setTextFill(Color.web("#00FBFF"));
            connectBtn.setDisable(true); // Tıklamayı geçici olarak kapat (Spam'i önle)

            // Ağ bağlantısı her zaman ayrı bir iş parçacığında (Thread) yapılmalı ki ekran donmasın
            new Thread(() -> {
                try {
                    if (client != null) client.stop(); // Eski bağlantı varsa temizle

                    client = new Client();
                    Network.register(client);
                    client.start();

                    // DİKKAT: Arkadaşınla oynayacağın zaman "127.0.0.1" yerine Hamachi IP'ni yazacaksın
                    client.connect(5000, "127.0.0.1", 54555, 54777);

                    // Sunucudan gelecek cevapları dinle
                    client.addListener(new Listener() {
                        @Override
                        public void received(Connection connection, Object object) {
                            if (object instanceof Network.JoinResponse) {
                                Network.JoinResponse cevap = (Network.JoinResponse) object;

                                // Arayüzü (JavaFX) değiştiren her şey Platform.runLater içinde olmalıdır!
                                Platform.runLater(() -> {
                                    if (cevap.isAccepted) {
                                        statusLabel.setText("ACCESS GRANTED! Lobiye giriliyor...");
                                        statusLabel.setTextFill(Color.web("#00FF41")); // Yeşil

                                        showLobbyScreen();

                                    } else {
                                        statusLabel.setText("ACCESS DENIED: " + cevap.message);
                                        statusLabel.setTextFill(Color.RED);
                                        connectBtn.setDisable(false); // Butonu tekrar aç
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
                    // Bağlantı çökürse arayüzü uyar
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
        // Lobi için yeni bir ana katman
        StackPane lobbyRoot = new StackPane();
        lobbyRoot.setStyle("-fx-background-color: #0a0a0c;"); // Koyu siber arka plan

        VBox lobbyBox = new VBox(20);
        lobbyBox.setAlignment(Pos.CENTER);

        Label title = new Label("BEKLEME ODASI (LOBİ)");
        title.setTextFill(Color.web("#00FBFF"));
        title.setFont(Font.font("Consolas", 50));
        title.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,251,255,0.7), 10, 0, 0, 0);");

        Label infoLabel = new Label("Sisteme bağlanan diğer kullanıcılar bekleniyor...");
        infoLabel.setTextFill(Color.LIGHTGRAY);
        infoLabel.setFont(Font.font("Consolas", 18));

        // Şimdilik temsili bir "Bağlı Oyuncular" kutusu
        VBox playerList = new VBox(10);
        playerList.setAlignment(Pos.CENTER);
        playerList.setStyle("-fx-background-color: #1a1a1c; -fx-padding: 20; -fx-border-color: #FF8C00; -fx-border-radius: 10; -fx-background-radius: 10;");
        playerList.setMaxWidth(400);

        Label player1 = new Label("> SEN BAĞLANDIN (Aktif)");
        player1.setTextFill(Color.web("#00FF41"));
        player1.setFont(Font.font("Consolas", 16));

        playerList.getChildren().add(player1);

        lobbyBox.getChildren().addAll(title, infoLabel, playerList);
        lobbyRoot.getChildren().add(lobbyBox);

        // Sahneyi (Scene) Lobi ile değiştir
        Scene lobbyScene = new Scene(lobbyRoot, 1024, 768);
        primaryStage.setScene(lobbyScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}