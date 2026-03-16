package com.impostra.server;

import com.impostra.common.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameManager {

    private List<Player> players;
    private GamePhase currentPhase;

    public GameManager() {
        this.players = new ArrayList<>();
        this.currentPhase = GamePhase.LOBBY;
    }

    public void addPlayer(Player player) {
        if (currentPhase == GamePhase.LOBBY) {
            players.add(player);
            System.out.println(player.getUsername() + " lobiye katıldı. Mevcut Oyuncu: " + players.size());
        }
    }

    public void startGame() {
        // En az 4 kişi olmadan oyun başlamasın (Test için sayıyı düşürebilirsin)
        if (players.size() >= 4) {
            System.out.println("\n--- Oyun Başlıyor! Rol dağıtımı yapılıyor... ---");

            assignRoles(); // Rol dağıtma fonksiyonumuzu çağırıyoruz

            currentPhase = GamePhase.NIGHT;
            System.out.println("Oyun evresi değişti: GECE");
            System.out.println("İlk gece çöktü... Herkes uykuya daldı.");
        } else {
            System.out.println("Oyunu başlatmak için yeterli oyuncu yok! Şu an: " + players.size());
        }
    }

    // --- RASTGELE ROL DAĞITMA ALGORİTMASI ---
    // --- RASTGELE ROL DAĞITMA ALGORİTMASI ---
    private void assignRoles() {
        List<Role> roleDeck = new ArrayList<>();
        int playerCount = players.size();

        // 1. Kesin Olması Gereken Roller (Vampir, Doktor ve Dedektif her oyunda olur)
        roleDeck.add(new Vampire());
        roleDeck.add(new Doctor());
        roleDeck.add(new Detective());

        // 2. Oyuncu sayısına göre ekstra (özel) rolleri desteye ekleme
        if (playerCount >= 7) {
            roleDeck.add(new Cursed()); // 7. kişi lanetli olur
        }
        if (playerCount >= 8) {
            roleDeck.add(new Witch()); // 8 kişiyseniz cadı da gelir
        }
        if (playerCount >= 9) {
            roleDeck.add(new Medium()); // 9 kişiyseniz medyum da gelir
        }
        if (playerCount >= 10) {
            roleDeck.add(new Vampire()); // 10 kişi olunca 2. Vampir eklenir (Denge için)
            roleDeck.add(new Dishonest()); // Sahtekar eklenir
        }
        if (playerCount >= 12) {
            // Aşıklar 2 kişi olmalı
            roleDeck.add(new Lover());
            roleDeck.add(new Lover());
        }

        // 3. Destede hala boşluk varsa (örneğin 6 kişiyiz, 3 rol atandı, 3 boşluk var)
        // Kalan boşlukları Köylü ile doldur
        while (roleDeck.size() < playerCount) {
            roleDeck.add(new Villager());
        }

        // 4. Desteyi İyice Karıştır
        Collections.shuffle(roleDeck);

        // 5. Karışmış destedeki rolleri oyunculara sırayla ver
        for (int i = 0; i < playerCount; i++) {
            Player p = players.get(i);
            Role assignedRole = roleDeck.get(i);
            p.assignRole(assignedRole);

            System.out.println("[GİZLİ SİSTEM BİLGİSİ] " + p.getUsername() + " => " + assignedRole.getName());
        }
    }

    public List<Player> getPlayers() { return players; }
    public GamePhase getCurrentPhase() { return currentPhase; }
}