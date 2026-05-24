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

    private Player aiTarget       = null;
    private Player engineerTarget = null;

    private Map<Player, Integer> voteCounts = new HashMap<>();

    // ============================================================
    //  ROL AÇIKLAMALARI
    // ============================================================
    public static String getRoleDescription(String roleName) {
        switch (roleName) {
            case "Rogue AI":
                return "Her gece bir oyuncuyu sistemden sil.\nTüm iyi oyuncular elenene kadar hayatta kal.\nDiğer kötü oyuncularla koordineli çalış.";
            case "İç Tehdit":
                return "Rogue AI ile aynı takımdasın.\nGündüzleri iyi biri gibi davran, şüpheyi\nbka oyuncuların üzerine çek.";
            case "Güvenlik Mühendisi":
                return "Her gece bir oyuncuyu koru.\nRogue AI'ın saldırısını engelleyebilirsin.\nKendini de koruyabilirsin.";
            case "Siber Analist":
                return "Her gece bir oyuncuyu sorgula.\nO oyuncunun iyi mi kötü mü olduğunu öğren.\nBu bilgiyi gündüzleri kullan.";
            case "Root Yöneticisi":
                return "Güçlü bir yetkiye sahipsin.\nGündüzleri oylamayı etkileyebilirsin.\nDoğru kişiyi oylamaya yönlendir.";
            case "Log Okuyucu":
                return "Gece ölen oyuncuların rolünü öğrenirsin.\nBu bilgiyle kötüleri tespit edebilirsin.\nÖlüler de sana ipucu verir.";
            case "Uyuyan Bot":
                return "Başlangıçta iyi bir oyuncusun.\nAma Rogue AI seni hackleyebilir!\nHacklenirsen kötü takımına geçersin.";
            case "Senkronize Düğüm":
                return "Başka bir Senkronize Düğüm ile eşleşirsin.\nEşin öldürülürse sen de ölürsün.\nBirlikte hayatta kalın.";
            case "Kullanıcı":
                return "Sıradan bir sistem kullanıcısısın.\nÖzel bir yetkin yok.\nGündüzleri dikkatli gözlemle ve\ndoğru kişiyi oyla.";
            default:
                return "Rolün hakkında bilgi bulunamadı.";
        }
    }

    public GameManager() {
        this.players = new ArrayList<>();
        this.currentPhase = GamePhase.LOBBY;
    }

    public void addPlayer(Player player) {
        if (currentPhase == GamePhase.LOBBY) {
            players.add(player);
            System.out.println("[LOBİ] " + player.getUsername() + " bağlandı. Toplam: " + players.size());
        }
    }

    /**
     * Oyunu tamamen sıfırlar — lobi durumuna döner.
     * Mevcut oyuncuları korur (bağlantıları kesülmeden sadece durum sıfırlanır).
     */
    public void reset() {
        // Her oyuncunun durumunu sıfırla (rolü ve hayat durumu)
        for (Player p : players) {
            p.resetForNewGame();
        }
        aiTarget       = null;
        engineerTarget = null;
        voteCounts.clear();
        currentPhase = GamePhase.LOBBY;
        System.out.println("\n=== OYUN SIFIRLANDI — LOBİYE DÖNÜLİYOR ===");
    }

    public void startGame() {
        if (players.size() >= 2 && players.size() <= 14) {
            System.out.println("\n=== OYUN BAŞLIYOR (" + players.size() + " oyuncu) ===");
            assignRoles();
            currentPhase = GamePhase.NIGHT;
        } else {
            System.out.println("[HATA] Oyuncu sayısı uygun değil: " + players.size());
        }
    }

    private void assignRoles() {
        List<Role> deck = new ArrayList<>();
        int n = players.size();

        int rogueCount = (n >= 12) ? 3 : (n >= 9) ? 2 : 1;
        for (int i = 0; i < rogueCount; i++) deck.add(new RogueAI());
        if (n >= 7) deck.add(new InsiderThreat());

        deck.add(new SecurityEngineer());
        if (n >= 4) deck.add(new CyberAnalyst());
        if (n >= 6) deck.add(new RootAdmin());
        if (n >= 8) deck.add(new LogReader());
        if (n >= 9) deck.add(new SleeperBot());
        if (n >= 12) { deck.add(new SyncNode()); deck.add(new SyncNode()); }

        while (deck.size() < n) deck.add(new SystemUser());

        Collections.shuffle(deck);
        for (int i = 0; i < n; i++) {
            players.get(i).assignRole(deck.get(i));
            System.out.println("[ROL] " + players.get(i).getUsername()
                    + " → " + deck.get(i).getName()
                    + (deck.get(i).isEvil() ? " [KÖTÜ]" : " [İYİ]"));
        }
    }

    public List<Player> getPlayers()         { return players; }
    public GamePhase getCurrentPhase()        { return currentPhase; }
    public void setCurrentPhase(GamePhase p)  { this.currentPhase = p; }

    public void setAITarget(Player target) {
        this.aiTarget = target;
        System.out.println("[GECE] Rogue AI hedefi: " + target.getUsername());
    }

    public void setEngineerTarget(Player target) {
        this.engineerTarget = target;
        System.out.println("[GECE] Güvenlik Mühendisi koruması: " + target.getUsername());
    }

    public String[] endNight() {
        String message, killed = "";
        if (aiTarget != null) {
            if (aiTarget == engineerTarget) {
                message = "Rogue AI '" + aiTarget.getUsername() + "' adresine saldırdı ama Firewall engelledi! Kimse ölmedi.";
            } else {
                killed  = aiTarget.getUsername();
                message = "'" + killed + "' gece Rogue AI tarafından sistemden silindi!";
                aiTarget.kill();
            }
        } else {
            message = "Gece sakin geçti, kimse saldırıya uğramadı.";
        }
        aiTarget = null; engineerTarget = null;
        currentPhase = GamePhase.DAY_DISCUSSION;
        return new String[]{ message, killed };
    }

    public void startVoting() {
        currentPhase = GamePhase.DAY_VOTING;
        voteCounts.clear();
    }

    public void castVote(Player voter, Player target) {
        if (!voter.isAlive() || !target.isAlive()) return;
        voteCounts.put(target, voteCounts.getOrDefault(target, 0) + 1);
        System.out.println("[OY] " + voter.getUsername() + " → " + target.getUsername());
    }

    public String[] endVoting() {
        Player toExecute = null; int maxVotes = 0; boolean isTie = false;
        for (Map.Entry<Player, Integer> e : voteCounts.entrySet()) {
            int v = e.getValue();
            if (v > maxVotes)                    { maxVotes = v; toExecute = e.getKey(); isTie = false; }
            else if (v == maxVotes && maxVotes > 0) isTie = true;
        }
        String message, executed = "";
        if (maxVotes == 0)  { message = "Hiç oy kullanılmadı. Kimse silinmedi."; }
        else if (isTie)     { message = "Oylamada beraberlik! Kimse silinmedi."; }
        else {
            executed = toExecute.getUsername();
            message  = "'" + executed + "' en çok oyu aldı ve sistemden silindi!";
            toExecute.kill();
        }
        currentPhase = GamePhase.NIGHT;
        return new String[]{ message, executed };
    }

    public String checkWinCondition() {
        int evil = 0, good = 0;
        for (Player p : players) {
            if (p.isAlive()) {
                if (p.getRole() != null && p.getRole().isEvil()) evil++; else good++;
            }
        }
        if (evil == 0)    return "SİSTEM GÜVENDE! Tüm Rogue AI'lar temizlendi. İYİLER KAZANDI!";
        if (evil >= good) return "SİSTEM ÇÖKTÜ! Rogue AI kontrolü ele geçirdi. KÖTÜLER KAZANDI!";
        System.out.println("[DURUM] Kötü: " + evil + " | İyi: " + good);
        return null;
    }

    public int      getAliveCount()       { int c=0; for (Player p:players) if(p.isAlive()) c++; return c; }
    public String[] getAlivePlayerNames() { List<String> l=new ArrayList<>(); for(Player p:players) if(p.isAlive()) l.add(p.getUsername()); return l.toArray(new String[0]); }
}