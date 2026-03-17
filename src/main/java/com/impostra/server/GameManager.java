package com.impostra.server;

import com.impostra.common.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameManager {

    private List<Player> players;
    private GamePhase currentPhase;

    // Siber Eylem Hedefleri (Eski vampireTarget ve doctorTarget yerine)
    private Player aiTarget = null;
    private Player engineerTarget = null;

    // Oyuncuların aldıkları şikayetleri (oyları) sayacağımız log sandığı
    private Map<Player, Integer> voteCounts = new HashMap<>();

    public GameManager() {
        this.players = new ArrayList<>();
        this.currentPhase = GamePhase.LOBBY;
    }

    public void addPlayer(Player player) {
        if (currentPhase == GamePhase.LOBBY) {
            players.add(player);
            System.out.println("[LOBİ] " + player.getUsername() + " sisteme bağlandı. Aktif Bağlantı: " + players.size());
        }
    }

    public void startGame() {
        // En az 4 kişi olmadan oyun başlamasın
        if (players.size() >= 4) {
            System.out.println("\n--- SİSTEM BAŞLATILIYOR! Yetkiler dağıtılıyor... ---");

            assignRoles(); // Siber rol dağıtma fonksiyonumuzu çağırıyoruz

            currentPhase = GamePhase.NIGHT;
            System.out.println("Sistem evresi değişti: UYKU MODU (GECE)");
            System.out.println("Ağ trafiği şifrelendi... Kötücül yazılımlar harekete geçiyor.");
        } else {
            System.out.println("Sistemi başlatmak için yeterli kullanıcı yok! Şu an: " + players.size());
        }
    }

    // --- RASTGELE SİBER ROL DAĞITMA ALGORİTMASI ---
    private void assignRoles() {
        List<Role> roleDeck = new ArrayList<>();
        int playerCount = players.size();

        // 1. Kesin Olması Gereken Roller
        roleDeck.add(new RogueAI());
        roleDeck.add(new SecurityEngineer());
        roleDeck.add(new CyberAnalyst()); // Eski Detective

        // 2. Oyuncu sayısına göre ekstra (özel) rolleri desteye ekleme
        if (playerCount >= 7) {
            roleDeck.add(new SleeperBot()); // Eski Cursed
        }
        if (playerCount >= 8) {
            roleDeck.add(new RootAdmin()); // Eski Witch
        }
        if (playerCount >= 9) {
            roleDeck.add(new LogReader()); // Eski Medium
        }
        if (playerCount >= 10) {
            roleDeck.add(new RogueAI()); // 10 kişi olunca 2. Rogue AI eklenir (Denge için)
            roleDeck.add(new InsiderThreat()); // Eski InsiderThreat
        }
        if (playerCount >= 12) {
            // Senkronize Düğümler (Eski Aşıklar) 2 kişi olmalı
            roleDeck.add(new SyncNode());
            roleDeck.add(new SyncNode());
        }

        // 3. Destede hala boşluk varsa SystemUser (Sıradan Kullanıcı) ile doldur
        while (roleDeck.size() < playerCount) {
            roleDeck.add(new SystemUser());
        }

        // 4. Desteyi İyice Karıştır
        Collections.shuffle(roleDeck);

        // 5. Karışmış destedeki rolleri oyunculara sırayla ver
        for (int i = 0; i < playerCount; i++) {
            Player p = players.get(i);
            Role assignedRole = roleDeck.get(i);
            p.assignRole(assignedRole);

            System.out.println("[GİZLİ SİSTEM BİLGİSİ] IP: " + p.getUsername() + " => YETKİ: " + assignedRole.getName());
        }
    }

    public List<Player> getPlayers() { return players; }
    public GamePhase getCurrentPhase() { return currentPhase; }

    // Gece aksiyonlarını alan metotlar
    public void setAITarget(Player target) {
        if (currentPhase == GamePhase.NIGHT) {
            this.aiTarget = target;
            System.out.println("[SİSTEM LOG] Rogue AI hedef IP'yi kilitledi, silme işlemi başlatılıyor...");
        }
    }

    public void setEngineerTarget(Player target) {
        if (currentPhase == GamePhase.NIGHT) {
            this.engineerTarget = target;
            System.out.println("[SİSTEM LOG] Güvenlik Mühendisi hedef IP'ye ekstra Firewall (Güvenlik Duvarı) kurdu...");
        }
    }

    // Gece bitip sabah olduğunda çalışacak asıl hesaplama metodu
    public void endNight() {
        System.out.println("\n--- GECE BİTİYOR, AĞ TRAFİĞİ İNCELENİYOR ---");

        // Eğer Rogue AI birini seçtiyse hesaplama başlar
        if (aiTarget != null) {
            // Mühendisin hedefi ile AI'ın hedefi AYNI KİŞİ Mİ?
            if (aiTarget == engineerTarget) {
                System.out.println("🛡️ GÜVENLİK RAPORU: Rogue AI '" + aiTarget.getUsername() + "' adresine saldırdı ama Firewall saldırıyı blokladı!");
            } else {
                System.out.println("💥 KRİTİK HATA: Rogue AI '" + aiTarget.getUsername() + "' kullanıcısının bağlantısını kesti (Silindi)!");
                aiTarget.kill(); // Player sınıfındaki kill() metodunu çağırıp isAlive durumunu false yapıyoruz
            }
        } else {
            System.out.println("✅ GÜVENLİK RAPORU: Gece sakin geçti, anormallik yok.");
        }

        // Değişkenleri bir sonraki gece için sıfırlıyoruz ki eski hedefler hafızada kalmasın
        aiTarget = null;
        engineerTarget = null;

        // Sabah evresine geçiyoruz
        checkWinCondition(); // Her ölümden sonra oyunu biri kazandı mı diye kontrol et!

        currentPhase = GamePhase.DAY_DISCUSSION;
        System.out.println("Sistem evresi değişti: GÜNDÜZ (LOG İNCELEMESİ)");
        System.out.println("Sistem uyandı. Log kayıtları inceleniyor...");
    }

    // 1. Oylama aşamasını başlatır
    public void startVoting() {
        if (currentPhase == GamePhase.DAY_DISCUSSION) {
            currentPhase = GamePhase.DAY_VOTING;
            voteCounts.clear(); // Önceki günün oylarını temizle
            System.out.println("\n--- OYLAMA BAŞLADI! Hangi IP adresini karantinaya (silmeye) almak istiyorsunuz? ---");
        }
    }

    // 2. Bir kullanıcının diğerini şikayet etmesi (oy vermesi)
    public void castVote(Player voter, Player target) {
        if (currentPhase != GamePhase.DAY_VOTING) {
            System.out.println("[HATA] Şu an oylama (şikayet) aşamasında değiliz!");
            return;
        }
        if (!voter.isAlive() || !target.isAlive()) {
            System.out.println("[HATA] Silinmiş (ölü) kullanıcılar oy kullanamaz veya oy alamaz!");
            return;
        }

        // Hedefin mevcut oyunu bul, üzerine 1 ekle
        voteCounts.put(target, voteCounts.getOrDefault(target, 0) + 1);
        System.out.println("[SİSTEM] " + voter.getUsername() + ", şüpheli olarak " + target.getUsername() + " adresini işaretledi.");
    }

    // 3. Oylamayı bitir, oyları say ve silme işlemini gerçekleştir!
    public void endVoting() {
        if (currentPhase != GamePhase.DAY_VOTING) return;

        System.out.println("\n--- OYLAMA BİTTİ! Şikayet logları sayılıyor... ---");

        Player playerToExecute = null;
        int maxVotes = 0;
        boolean isTie = false; // Beraberlik durumu

        // Sandıktaki oyları tek tek sayıyoruz
        for (Map.Entry<Player, Integer> entry : voteCounts.entrySet()) {
            Player candidate = entry.getKey();
            int votes = entry.getValue();

            System.out.println("- IP: " + candidate.getUsername() + " : " + votes + " şikayet aldı.");

            if (votes > maxVotes) {
                maxVotes = votes;
                playerToExecute = candidate;
                isTie = false; // Beraberlik bozuldu
            } else if (votes == maxVotes && maxVotes > 0) {
                isTie = true; // İki kişi aynı sayıda en yüksek oyu aldı
            }
        }

        // Sonuçları açıklama anı
        if (maxVotes == 0) {
            System.out.println("Ağ kararsız kaldı, hiç şikayet çıkmadı. Kimse silinmiyor.");
        } else if (isTie) {
            System.out.println("⚖️ SİSTEM YÖNETİCİSİ: Oylamada BERABERLİK çıktı! Sistem kuralları gereği bugün kimse karantinaya alınmıyor.");
        } else {
            System.out.println("🔥 SİSTEM KARARINI VERDİ! En çok şikayet alan '" + playerToExecute.getUsername() + "' sistemden tamamen silindi!");
            playerToExecute.kill(); // Oyuncuyu siliyoruz
        }

        checkWinCondition(); // Her infazdan sonra oyunu biri kazandı mı diye kontrol et!

        // İnfaz bitti, ağ şifreleniyor ve tekrar Gece oluyor...
        currentPhase = GamePhase.NIGHT;
        System.out.println("\nSistem evresi değişti: UYKU MODU (GECE)");
        System.out.println("Ağ trafiği şifreleniyor, bağlantılar koparılıyor...");
    }

    // Oyunu bitiren şartların sağlanıp sağlanmadığını kontrol etme kısmı...

    public String checkWinCondition() {
        int evilCount = 0;
        int goodCount = 0;

        for(Player p: players) {
            if ( p.isAlive() ) {
                if ( p.getRole().isEvil()) {
                    evilCount++;
                } else {
                    goodCount++;
                }
            }
        }

        if ( evilCount == 0 ) {
            return "🎉 SİSTEM GÜVENDE! Bütün Rogue AI kodları temizlendi. İNSANLIK KAZANDI! 🎉";
        } else if (evilCount >= goodCount) {
            return "⚠️ SİSTEM ÇÖKTÜ! Rogue AI ağın kontrolünü ele geçirdi. YAPAY ZEKA KAZANDI! ⚠️";
        } else {
            System.out.println("[SİSTEM DURUMU] Kalan Zararlı Yazılım: " + evilCount + " | Kalan Güvenli Kullanıcı: " + goodCount);
            return null;
        }
    }
}