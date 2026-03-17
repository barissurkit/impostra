package com.impostra.client.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import com.esotericsoftware.kryonet.Client;
import com.impostra.common.Network;
import java.io.IOException;

public class ImpostraGUI extends Application {

    private Stage primaryStage;
    private Label statusLabel;
    private Client client; // Ağ istemcimiz burada yaşayacak

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // --- MAC UYUMLULUK AYARI ---
        // Mac'teki "NSInternalInconsistencyException" hatasını önlemek için
        // JavaFX uygulama döngüsünü daha stabil hale getirir.
        Platform.setImplicitExit(true);

        // İlk ekranı göster
        showLoginScreen();

        // Pencere Ayarları
        primaryStage.setTitle("Impostra: Cyber Warfare");
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Kullanıcının adını girip sunucuya bağlanacağı giriş ekranı
     */
    public void showLoginScreen() {
        // Ana kutu dizilimi
        VBox root = new VBox(25);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0a0a0c; -fx-padding: 50;");

        // 1. Oyun Logosu / Başlığı
        Label titleLabel = new Label("IMPOSTRA");
        titleLabel.setTextFill(Color.web("#00FF41")); // Matrix Yeşili
        titleLabel.setFont(Font.font("Consolas", 80));
        // Hafif bir parlama efekti (Gölge)
        titleLabel.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,255,65,0.8), 10, 0, 0, 0);");

        // 2. Alt Başlık
        Label subTitle = new Label("> SİSTEME ERİŞİM PANELİ");
        subTitle.setTextFill(Color.web("#008F11"));
        subTitle.setFont(Font.font("Consolas", 18));

        // 3. Kullanıcı Adı Giriş Alanı
        TextField nameField = new TextField();
        nameField.setPromptText("Kullanıcı Adı...");
        nameField.setMaxWidth(300);
        nameField.setMinHeight(40);
        nameField.setStyle(
                "-fx-background-color: #1a1a1c; " +
                        "-fx-text-fill: #00FF41; " +
                        "-fx-border-color: #008F11; " +
                        "-fx-border-width: 2; " +
                        "-fx-font-family: 'Consolas';"
        );

        // 4. Bağlan Butonu
        Button connectBtn = new Button("SİSTEME SIZ (CONNECT)");
        connectBtn.setMinWidth(300);
        connectBtn.setMinHeight(50);
        connectBtn.setCursor(javafx.scene.Cursor.HAND);
        connectBtn.setStyle(
                "-fx-background-color: #00FF41; " +
                        "-fx-text-fill: black; " +
                        "-fx-font-family: 'Consolas'; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 16px;"
        );

        // 5. Durum Mesajı
        statusLabel = new Label("Bağlantı bekleniyor...");
        statusLabel.setTextFill(Color.GRAY);
        statusLabel.setFont(Font.font("Consolas", 14));

        // Elemanları ekrana ekle
        root.getChildren().addAll(titleLabel, subTitle, nameField, connectBtn, statusLabel);

        // Sahneyi ayarla
        Scene loginScene = new Scene(root, 1024, 768);
        primaryStage.setScene(loginScene);

        // --- BUTON AKSİYONU ---
        connectBtn.setOnAction(e -> {
            String nick = nameField.getText().trim();
            if (nick.isEmpty()) {
                statusLabel.setText("HATA: Kullanıcı adı boş olamaz!");
            } else {
                try {
                    // Eğer zaten bir bağlantı varsa kapat (temiz kurulum)
                    if (client != null) client.stop();

                    client = new Client();
                    Network.register(client);
                    client.start();

                    // IP kısmına senin IP'ni yazmalısın. Şimdilik test için localhost:
                    client.connect(5000, "127.0.0.1", 54555, 54777);

                    // Sunucuya "Ben geldim" de
                    Network.JoinRequest istek = new Network.JoinRequest();
                    istek.username = nick;
                    client.sendTCP(istek);

                    statusLabel.setText("SİSTEME SIZILDI! Lobi bekleniyor...");
                    statusLabel.setTextFill(Color.web("#00FF41"));

                } catch (IOException ex) {
                    statusLabel.setText("BAĞLANTI HATASI: Sunucu kapalı!");
                    statusLabel.setTextFill(Color.RED);
                }
            }
        });
    }

    public static void main(String[] args) {
        // JavaFX uygulamasını başlat
        launch(args);
    }
}