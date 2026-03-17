package com.impostra.client.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ImpostraGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Kutu Düzeni (Root Node) - Siberpunk siyahı bir arka plan yapalım
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #0a0a0c;"); // Çok koyu siber siyah

        // 2. Ortaya geçici bir neon yazı ekleyelim (Çalıştığını görmek için)
        Label baslik = new Label("SİSTEME BAĞLANILIYOR...");
        baslik.setTextFill(Color.web("#00FF41")); // Matrix yeşili
        baslik.setFont(Font.font("Consolas", 24)); // Hacker fontu

        // Yazıyı düzenin içine ekle
        root.getChildren().add(baslik);

        // 3. Dekoratı (Scene) oluşturuyoruz (Genişlik: 1024, Yükseklik: 768)
        Scene scene = new Scene(root, 1024, 768);

        // 4. Dekoratı Pencereye (Stage) ekleyip gösteriyoruz
        primaryStage.setTitle("Impostra: Cyber Warfare"); // Pencere başlığı
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // Şimdilik pencere boyutunu sabitleyelim, tasarım kaymasın
        primaryStage.show();
    }

    public static void main(String[] args) {
        // JavaFX motorunu ateşle!
        launch(args);
    }
}