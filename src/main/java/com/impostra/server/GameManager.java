package com.impostra.server;

import com.impostra.common.*;
import java.util.*;

public class GameManager {

    private List<Player> players;
    private GamePhase currentPhase;

    // Gece hedefleri
    private Player aiTarget          = null;
    private Player engineerTarget    = null;
    private Player insiderTarget     = null;  // İç Tehdit kilitleme hedefi
    private Player analystTarget     = null;
    private Player rootTarget        = null;  // Root Yöneticisi geri getirme hedefi
    private Player logTarget         = null;  // Log Okuyucu sorgulama hedefi

    // Oylama
    private Map<Player, Integer> voteCounts = new HashMap<>();

    // ============================================================
    //  ROL AÇIKLAMALARI (konuşmada onaylanan final versiyonlar)
    // ============================================================
    public static String getRoleDescription(String roleName) {
        switch (roleName) {
            case "Rogue AI":
                return "Sistemin kalbine yerleştin.\nHer gece sessizce bir kullanıcıyı sil — iz bırakma.\nGündüzleri masum görün, şüpheyi başkalarına yönlendir.\nTüm iyi kullanıcılar elendiğinde ağ senin.";
            case "İç Tehdit":
                return "Sistemin içine yerleşmiş bir ajan olarak\nher gece bir kullanıcıyı kilitle.\nKilitlenen oyuncu ertesi gün oy kullanamaz —\nsusturulan bir ses, kaybedilen bir oy.\nRogue AI ile aynı amacı paylaşıyorsun,\nama senin silahın sessizlik.";
            case "Güvenlik Mühendisi":
                return "Her gece bir kullanıcının sistemine Firewall kurarsın.\nO gece Rogue AI o kişiye saldırırsa saldırıyı engellersin.\nKendini de koruyabilirsin — ama dikkat,\naynı kişiyi arka arkaya koruyamazsın.\nDoğru kişiyi koru, sistemi ayakta tut.";
            case "Siber Analist":
                return "Her gece bir kullanıcının sistem loglarını tara.\nSabah o kişinin iyi mi kötü mü olduğunu öğrenirsin —\nbu bilgi sadece sende kalır.\nGündüzleri doğru kişiyi oylamaya yönlendirmek senin görevin.\nAma dikkatli ol: bulguların olmadan\nkimse sana inanmak zorunda değil.";
            case "Root Yöneticisi":
                return "Sistemin en yüksek yetkisine sahipsin —\nrm -rf de diyebilirsin, restore da.\nTüm oyun boyunca yalnızca bir kez,\ndaha önce öldürülmüş bir kullanıcıyı sisteme geri getirebilirsin.\nBu yetkiyi doğru anda kullan:\nyanlış kişiyi geri getirirsen ağı mahvedebilirsin,\ndoğru kişiyi getirirsen oyunu kurtarabilirsin.";
            case "Log Okuyucu":
                return "Silinen kullanıcılar iz bırakır.\nHer gece ölen oyunculardan birinin gizli rolünü okuyabilirsin —\nRogue AI mıydı, iyi kullanıcı mıydı?\nBu bilgiyi gündüzleri zekice kullan.\nAma dikkat: ölü yoksa gece aksiyonun çalışmaz.";
            case "Uyuyan Bot":
                return "Sistemde sıradan bir kullanıcı gibi görünüyorsun — şimdilik.\nEğer Rogue AI gece seni hedef seçerse ölmezsin, onun saflarına geçersin.\nArtık kötü takımın bir parçasısın ve bunu kimse bilmez.\nHacklenmeden önce iyi, sonra kötüsün —\nSiber Analist bile yanılabilir.";
            case "Senkronize Düğüm":
                return "Ağda seninle senkronize çalışan başka bir düğüm var —\nve sen onu biliyorsun.\nİkiniz birbirinize bağlısınız:\nbiri ölürse diğeri de çöker.\nGece aksiyonun yok ama bu bağ\nhem en büyük gücün hem en büyük zayıflığın.\nEşini koru, birlikte hayatta kalın.";
            case "Kullanıcı":
                return "Ağdaki sıradan bir kullanıcısın —\nözel yetkin yok, gece uyursun.\nAma bu seni güçsüz yapmaz.\nGündüzleri dikkatli gözlemle, konuş, ikna et\nve doğru kişiyi oyla.\nİyi takımın en kalabalık üyesisin —\nbirlikte hareket edersen Rogue AI'ı durdurabilirsin.";
            default:
                return "Rolün hakkında bilgi bulunamadı.";
        }
    }

    // ============================================================
    //  GECE SIRASI
    //  Sıra: Rogue AI + İç Tehdit → Güvenlik Mühendisi → Siber Analist
    //        → Root Yöneticisi → Log Okuyucu → diğerleri otomatik
    // ============================================================
    public static final String[] NIGHT_ORDER = {
            "Rogue AI",
            "İç Tehdit",
            "Güvenlik Mühendisi",
            "Siber Analist",
            "Root Yöneticisi",
            "Log Okuyucu"
    };

    public static final int[] NIGHT_TIMEOUTS = {
            15,  // Rogue AI
            15,  // İç Tehdit
            15,  // Güvenlik Mühendisi
            15,  // Siber Analist
            20,  // Root Yöneticisi
            15   // Log Okuyucu
    };

    // ============================================================
    //  CONSTRUCTOR
    // ============================================================
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

    public void reset() {
        for (Player p : players) p.resetForNewGame();
        aiTarget = null; engineerTarget = null; insiderTarget = null;
        analystTarget = null; rootTarget = null; logTarget = null;
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
        if (n >= 7)  deck.add(new InsiderThreat());

        deck.add(new SecurityEngineer());
        if (n >= 4)  deck.add(new CyberAnalyst());
        if (n >= 6)  deck.add(new RootAdmin());
        if (n >= 8)  deck.add(new LogReader());
        if (n >= 9)  deck.add(new SleeperBot());
        if (n >= 12) { deck.add(new SyncNode()); deck.add(new SyncNode()); }

        while (deck.size() < n) deck.add(new SystemUser());

        Collections.shuffle(deck);
        for (int i = 0; i < n; i++) {
            players.get(i).assignRole(deck.get(i));
            System.out.println("[ROL] " + players.get(i).getUsername()
                    + " → " + deck.get(i).getName()
                    + (deck.get(i).isEvil() ? " [KÖTÜ]" : " [İYİ]"));
        }

        // Senkronize Düğümleri eşleştir
        List<Player> syncNodes = new ArrayList<>();
        for (Player p : players) {
            if (p.getRole() != null && p.getRole().getName().equals("Senkronize Düğüm")) {
                syncNodes.add(p);
            }
        }
        if (syncNodes.size() == 2) {
            syncNodes.get(0).setSyncPartner(syncNodes.get(1));
            syncNodes.get(1).setSyncPartner(syncNodes.get(0));
            System.out.println("[SYNC] " + syncNodes.get(0).getUsername()
                    + " ↔ " + syncNodes.get(1).getUsername());
        }
    }

    // ============================================================
    //  GECE AKSİYONLARI
    // ============================================================

    public void setAITarget(Player target) {
        this.aiTarget = target;
        System.out.println("[GECE] Rogue AI hedefi: " + target.getUsername());
    }

    public void setEngineerTarget(Player target) {
        this.engineerTarget = target;
        System.out.println("[GECE] Güvenlik Mühendisi koruması: " + target.getUsername());
    }

    public void setInsiderTarget(Player target) {
        this.insiderTarget = target;
        System.out.println("[GECE] İç Tehdit kilitledi: " + target.getUsername());
    }

    public void setAnalystTarget(Player target) {
        this.analystTarget = target;
        System.out.println("[GECE] Siber Analist sorguladı: " + target.getUsername());
    }

    public void setRootTarget(Player target) {
        this.rootTarget = target;
        System.out.println("[GECE] Root Yöneticisi geri getiriyor: " + target.getUsername());
    }

    public void setLogTarget(Player target) {
        this.logTarget = target;
        System.out.println("[GECE] Log Okuyucu okuyor: " + target.getUsername());
    }

    /**
     * Gece biter, tüm aksiyonlar işlenir.
     * Döndürülen map anahtarları:
     *   "morning_message"  → sabah herkese duyurulacak mesaj
     *   "killed_player"    → ölen oyuncunun adı (yoksa "")
     *   "sync_killed"      → sync partner ölümüyle ölen oyuncunun adı (yoksa "")
     *   "analyst_target"   → analistin sorguladığı oyuncunun adı
     *   "analyst_evil"     → "true" / "false"
     *   "log_target"       → log okuyucunun baktığı oyuncunun adı
     *   "log_role"         → o oyuncunun rolü
     *   "log_evil"         → "true" / "false"
     *   "root_restored"    → root'un geri getirdiği oyuncunun adı (yoksa "")
     *   "insider_locked"   → kilitlenen oyuncunun adı (yoksa "")
     *   "sleeper_hacked"   → hacklenip kötüye geçen uyuyan botun adı (yoksa "")
     */
    public Map<String, String> endNight() {
        Map<String, String> result = new HashMap<>();
        result.put("killed_player",   "");
        result.put("sync_killed",     "");
        result.put("analyst_target",  "");
        result.put("analyst_evil",    "");
        result.put("log_target",      "");
        result.put("log_role",        "");
        result.put("log_evil",        "");
        result.put("root_restored",   "");
        result.put("insider_locked",  "");
        result.put("sleeper_hacked",  "");

        // 1. Güvenlik Mühendisi — aynı kişiyi arka arkaya koruyamaz
        if (engineerTarget != null && engineerTarget.wasLastProtected()) {
            System.out.println("[GECE] Güvenlik Mühendisi aynı kişiyi tekrar seçti — koruma geçersiz!");
            engineerTarget = null;
        }

        // Bu gece korunan oyuncuları işaretle, eski korumayı temizle
        for (Player p : players) p.clearLastProtected();
        if (engineerTarget != null) engineerTarget.markProtectedThisRound();

        // 2. Rogue AI saldırısı
        String morningMsg;
        if (aiTarget != null) {
            // Uyuyan Bot kontrolü: AI onu hedef seçtiyse hack et, öldürme
            if (aiTarget.getRole() != null && aiTarget.getRole().getName().equals("Uyuyan Bot") && !aiTarget.isHacked()) {
                aiTarget.setHacked(true);
                // Rolü kötüye çevir
                aiTarget.assignRole(new RogueAI());
                result.put("sleeper_hacked", aiTarget.getUsername());
                morningMsg = "Gece sakin geçti... ama ağda bir şeyler değişti.";
                System.out.println("[GECE] Uyuyan Bot hacklendi: " + aiTarget.getUsername());
            } else if (aiTarget == engineerTarget) {
                morningMsg = "Rogue AI '" + aiTarget.getUsername() + "' adresine saldırdı ama Firewall engelledi! Kimse ölmedi.";
                System.out.println("[GECE] Saldırı engellendi: " + aiTarget.getUsername());
            } else {
                String killed = aiTarget.getUsername();
                result.put("killed_player", killed);
                morningMsg = "'" + killed + "' gece Rogue AI tarafından sistemden silindi!";
                aiTarget.kill();

                // Senkronize Düğüm kontrolü
                Player syncPartner = aiTarget.getSyncPartner();
                if (syncPartner != null && syncPartner.isAlive()) {
                    syncPartner.kill();
                    result.put("sync_killed", syncPartner.getUsername());
                    morningMsg += "\n'" + syncPartner.getUsername() + "' senkronize eşi çöktüğü için sistemden silindi!";
                    System.out.println("[GECE] Sync partner da öldü: " + syncPartner.getUsername());
                }
            }
        } else {
            morningMsg = "Gece sakin geçti, kimse saldırıya uğramadı.";
        }

        // 3. Root Yöneticisi — ölüyü geri getir
        if (rootTarget != null && !rootTarget.isAlive()) {
            rootTarget.revive();
            result.put("root_restored", rootTarget.getUsername());
            morningMsg += "\n'" + rootTarget.getUsername() + "' Root Yöneticisi tarafından sisteme geri yüklendi!";
            System.out.println("[GECE] Root restore: " + rootTarget.getUsername());
        }

        // 4. İç Tehdit kilitleme
        if (insiderTarget != null && insiderTarget.isAlive()) {
            insiderTarget.setLocked(true);
            result.put("insider_locked", insiderTarget.getUsername());
            System.out.println("[GECE] İç Tehdit kilitledi: " + insiderTarget.getUsername());
        }

        // 5. Siber Analist sonucu
        if (analystTarget != null) {
            boolean isEvil = analystTarget.getRole() != null && analystTarget.getRole().isEvil();
            result.put("analyst_target", analystTarget.getUsername());
            result.put("analyst_evil",   String.valueOf(isEvil));
        }

        // 6. Log Okuyucu sonucu (ölü listesinden)
        if (logTarget != null) {
            boolean wasEvil = logTarget.getRole() != null && logTarget.getRole().isEvil();
            result.put("log_target", logTarget.getUsername());
            result.put("log_role",   logTarget.getRole() != null ? logTarget.getRole().getName() : "?");
            result.put("log_evil",   String.valueOf(wasEvil));
        }

        result.put("morning_message", morningMsg);

        // Temizle
        aiTarget = null; engineerTarget = null; insiderTarget = null;
        analystTarget = null; rootTarget = null; logTarget = null;

        // Her oyuncunun tur durumunu sıfırla
        for (Player p : players) p.resetForNewRound();

        currentPhase = GamePhase.DAY_DISCUSSION;
        return result;
    }

    // ============================================================
    //  OYLAMA
    // ============================================================

    public void startVoting() {
        currentPhase = GamePhase.DAY_VOTING;
        voteCounts.clear();
    }

    public void castVote(Player voter, Player target) {
        if (!voter.isAlive() || voter.isLocked()) {
            System.out.println("[OY] " + voter.getUsername() + " kilitli, oy kullanamaz!");
            return;
        }
        if (!target.isAlive()) return;
        voteCounts.put(target, voteCounts.getOrDefault(target, 0) + 1);
        System.out.println("[OY] " + voter.getUsername() + " → " + target.getUsername());
    }

    public String[] endVoting() {
        Player toExecute = null; int maxVotes = 0; boolean isTie = false;
        for (Map.Entry<Player, Integer> e : voteCounts.entrySet()) {
            int v = e.getValue();
            if (v > maxVotes)                     { maxVotes = v; toExecute = e.getKey(); isTie = false; }
            else if (v == maxVotes && maxVotes > 0)  isTie = true;
        }
        String message, executed = "";
        if (maxVotes == 0)  { message = "Hiç oy kullanılmadı. Kimse silinmedi."; }
        else if (isTie)     { message = "Oylamada beraberlik! Kimse silinmedi."; }
        else {
            executed = toExecute.getUsername();
            message  = "'" + executed + "' en çok oyu aldı ve sistemden silindi!";
            toExecute.kill();

            // Senkronize Düğüm kontrolü
            Player syncPartner = toExecute.getSyncPartner();
            if (syncPartner != null && syncPartner.isAlive()) {
                syncPartner.kill();
                message += "\n'" + syncPartner.getUsername() + "' senkronize eşi silindiği için sistemden çöktü!";
            }
        }

        // Oylama bitince kilitleri sıfırla (bir sonraki gece başında da sıfırlanır
        // ama burada da temizleyelim)
        for (Player p : players) p.setLocked(false);

        currentPhase = GamePhase.NIGHT;
        return new String[]{ message, executed };
    }

    // ============================================================
    //  KAZANMA KOŞULU
    // ============================================================

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

    // ============================================================
    //  YARDIMCI METOTLAR
    // ============================================================

    public List<Player>  getPlayers()            { return players; }
    public GamePhase     getCurrentPhase()        { return currentPhase; }
    public void          setCurrentPhase(GamePhase p) { this.currentPhase = p; }

    public int      getAliveCount() {
        int c = 0; for (Player p : players) if (p.isAlive()) c++; return c;
    }

    public String[] getAlivePlayerNames() {
        List<String> l = new ArrayList<>();
        for (Player p : players) if (p.isAlive()) l.add(p.getUsername());
        return l.toArray(new String[0]);
    }

    /** Ölü oyuncuların listesi (Log Okuyucu ve Root Yöneticisi için) */
    public List<Player> getDeadPlayers() {
        List<Player> dead = new ArrayList<>();
        for (Player p : players) if (!p.isAlive()) dead.add(p);
        return dead;
    }

    /** Belirli role sahip hayatta oyuncuları döndürür */
    public List<Player> getAlivePlayersWithRole(String roleName) {
        List<Player> result = new ArrayList<>();
        for (Player p : players)
            if (p.isAlive() && p.getRole() != null && p.getRole().getName().equals(roleName))
                result.add(p);
        return result;
    }
}