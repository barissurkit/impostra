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
    private void assignRoles() {
        List<Role> roleDeck = new ArrayList<>();
        int playerCount = players.size();

        // 1. Temel Rolleri ve Açtığın Özel Rolleri Desteye Ekle
        roleDeck.add(new Vampire());
        roleDeck.add(new Doctor());
        // Eğer 6+ kişi oynuyorsanız buraya Cursed, Witch vb. de eklenebilir.
        // roleDeck.add(new Detective());

        // 2. Destede kalan boşlukları Köylü ile doldur
        while (roleDeck.size() < playerCount) {
            roleDeck.add(new Villager());
        }

        // 3. Desteyi Karıştır
        Collections.shuffle(roleDeck);

        // 4. Karışmış destedeki rolleri oyunculara sırayla ver
        for (int i = 0; i < playerCount; i++) {
            Player p = players.get(i);
            Role assignedRole = roleDeck.get(i);
            p.assignRole(assignedRole);

            // Konsola kimin hangi rolü aldığını yazdırıyoruz (Test için)
            System.out.println("[GİZLİ SİSTEM BİLGİSİ] " + p.getUsername() + " => " + assignedRole.getName());
        }
    }

    public List<Player> getPlayers() { return players; }
    public GamePhase getCurrentPhase() { return currentPhase; }
}