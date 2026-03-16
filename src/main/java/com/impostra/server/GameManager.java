package com.impostra.server;

import com.impostra.common.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.HashMap;
import java.util.Map;

public class GameManager {

    private List<Player> players;
    private GamePhase currentPhase;
    // Oyuncuların aldıkları oyları sayacağımız sandık
    private Map<Player, Integer> voteCounts = new HashMap<>();

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
    // Gece hedeflerini akılda tutacağımız değişkenler
    private Player vampireTarget = null;
    private Player doctorTarget = null;

    // Vampirin hedefini sisteme kaydeder
    public void setVampireTarget(Player target) {
        if (currentPhase == GamePhase.NIGHT) {
            this.vampireTarget = target;
            System.out.println("[SİSTEM] Vampir hedefini kilitledi...");
        }
    }

    // Doktorun hedefini sisteme kaydeder
    public void setDoctorTarget(Player target) {
        if (currentPhase == GamePhase.NIGHT) {
            this.doctorTarget = target;
            System.out.println("[SİSTEM] Doktor hedefini kilitledi...");
        }
    }

    // Gece bitip sabah olduğunda çalışacak asıl hesaplama metodu
    public void endNight() {
        System.out.println("\n--- GECE BİTİYOR, HESAPLAMALAR YAPILIYOR ---");

        // Eğer vampir birini seçtiyse hesaplama başlar
        if (vampireTarget != null) {
            // Doktorun hedefi ile Vampirin hedefi AYNI KİŞİ Mİ?
            if (vampireTarget == doctorTarget) {
                System.out.println("🔥 SONUÇ: Vampir '" + vampireTarget.getUsername() + "' adlı oyuncuya saldırdı ama Doktor onu kurtardı!");
            } else {
                System.out.println("💀 SONUÇ: Vampir '" + vampireTarget.getUsername() + "' adlı oyuncuya saldırdı ve onu öldürdü!");
                vampireTarget.kill(); // Player sınıfındaki kill() metodunu çağırıp isAlive durumunu false yapıyoruz
            }
        } else {
            System.out.println("🕊️ SONUÇ: Gece sakin geçti, kimse saldırıya uğramadı.");
        }

        // Değişkenleri bir sonraki gece için sıfırlıyoruz ki eski hedefler hafızada kalmasın
        vampireTarget = null;
        doctorTarget = null;

        // Sabah evresine geçiyoruz
        currentPhase = GamePhase.DAY_DISCUSSION;
        System.out.println("Oyun evresi değişti: GÜNDÜZ TARTIŞMASI");
        System.out.println("Güneş doğdu. Köy uyanıyor...");
    }

    // 1. Oylama aşamasını başlatır
    public void startVoting() {
        if (currentPhase == GamePhase.DAY_DISCUSSION) {
            currentPhase = GamePhase.DAY_VOTING;
            voteCounts.clear(); // Önceki günün oylarını temizle (Sandığı boşalt)
            System.out.println("\n--- OYLAMA BAŞLADI! Kimi asmak istiyorsunuz? ---");
        }
    }

    // 2. Bir oyuncunun başka birine oy vermesi
    public void castVote(Player voter, Player target) {
        if (currentPhase != GamePhase.DAY_VOTING) {
            System.out.println("[HATA] Şu an oylama aşamasında değiliz!");
            return;
        }
        if (!voter.isAlive() || !target.isAlive()) {
            System.out.println("[HATA] Ölü oyuncular oy kullanamaz veya oy alamaz!");
            return;
        }

        // Hedefin mevcut oyunu bul, üzerine 1 ekle. (Daha önce hiç oy almadıysa 0 kabul edip 1 yapar)
        voteCounts.put(target, voteCounts.getOrDefault(target, 0) + 1);
        System.out.println("[SİSTEM] " + voter.getUsername() + ", oyunu " + target.getUsername() + " için kullandı.");
    }

    // 3. Oylamayı bitir, oyları say ve infazı gerçekleştir!
    public void endVoting() {
        if (currentPhase != GamePhase.DAY_VOTING) return;

        System.out.println("\n--- OYLAMA BİTTİ! Oylar sayılıyor... ---");

        Player playerToExecute = null;
        int maxVotes = 0;
        boolean isTie = false; // Beraberlik durumu

        // Sandıktaki oyları tek tek sayıyoruz
        for (Map.Entry<Player, Integer> entry : voteCounts.entrySet()) {
            Player candidate = entry.getKey();
            int votes = entry.getValue();

            System.out.println("- " + candidate.getUsername() + " : " + votes + " oy aldı.");

            // Eğer yeni sayılan oy, şu anki maksimum oydan büyükse yeni lider o olur
            if (votes > maxVotes) {
                maxVotes = votes;
                playerToExecute = candidate;
                isTie = false; // Beraberlik bozuldu
            } else if (votes == maxVotes && maxVotes > 0) {
                isTie = true; // Eyvah! İki kişi aynı sayıda en yüksek oyu aldı
            }
        }

        // Sonuçları açıklama anı
        if (maxVotes == 0) {
            System.out.println("Köy kararsız kaldı, sandıktan hiç oy çıkmadı. Kimse asılmıyor.");
        } else if (isTie) {
            System.out.println("⚖️ Oylamada BERABERLİK çıktı! Köy kuralları gereği bugün kimse asılmıyor.");
        } else {
            System.out.println("🔥 KÖY KARARINI VERDİ! En çok oyu alan '" + playerToExecute.getUsername() + "' darağacına gönderildi!");
            playerToExecute.kill(); // Oyuncuyu öldürüyoruz
        }

        // İnfaz bitti, güneş batıyor ve tekrar Gece oluyor...
        currentPhase = GamePhase.NIGHT;
        System.out.println("\nOyun evresi değişti: GECE");
        System.out.println("Güneş battı, herkes evlerine çekildi...");
    }

    // Oyunu bitiren şartların sağlanıp sağlanmadığını kontrol eder
    public void checkWinCondition() {
        int evilCount = 0;
        int goodCount = 0;

        // Hayatta olan oyuncuları sayıyoruz
        for (Player p : players) {
            if (p.isAlive()) {
                // Oyuncunun rolü "Kötü" mü diye soruyoruz (Role sınıfındaki isEvil mantığı)
                if (p.getRole().isEvil()) {
                    evilCount++;
                } else {
                    goodCount++;
                }
            }
        }

        // Kazanma Şartlarını Test Ediyoruz
        if (evilCount == 0) {
            System.out.println("\n🎉 OYUN BİTTİ! Bütün vampirler avlandı. KÖYLÜLER KAZANDI! 🎉");
            // Test aşamasında olduğumuz için programı durduruyoruz.
            // İleride buraya "Oyuncuları lobiye geri at" kodu yazacağız.
            System.exit(0);
        } else if (evilCount >= goodCount) {
            System.out.println("\n🦇 OYUN BİTTİ! Vampirlerin sayısı köylüleri geçti. VAMPİRLER KAZANDI! 🦇");
            System.exit(0);
        } else {
            // Oyun hala devam ediyorsa güncel durumu konsola yazdır
            System.out.println("[DURUM] Kalan Kötü: " + evilCount + " | Kalan İyi: " + goodCount);
        }
    }

}