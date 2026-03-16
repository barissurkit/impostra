package com.impostra.server;

import com.impostra.common.Player;

public class ServerApp {
    public static void main(String[] args) {
        System.out.println("--- Impostra Sunucusu Başlatılıyor ---\n");

        // 1. Oyun Motorumuzu (Beynimizi) çalıştırıyoruz
        GameManager gameManager = new GameManager();

        // 2. Lobiye sahte oyuncular ekliyoruz (8 kişilik bir test yapalım)
        gameManager.addPlayer(new Player("Barış"));
        gameManager.addPlayer(new Player("Ahmet"));
        gameManager.addPlayer(new Player("Ayşe"));
        gameManager.addPlayer(new Player("Mehmet"));
        gameManager.addPlayer(new Player("Fatma"));
        gameManager.addPlayer(new Player("Can"));
        gameManager.addPlayer(new Player("Elif"));
        gameManager.addPlayer(new Player("Burak"));

        // 3. Oyunu Başlatıyoruz! (Bu komut rolleri dağıtıp geceyi başlatacak)
        gameManager.startGame();

        System.out.println("\n--- Sistem Testi Tamamlandı ---");
    }
}