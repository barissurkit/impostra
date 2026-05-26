package com.impostra.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.impostra.common.Network;
import com.impostra.common.Player;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ServerApp {

    // Süre sabitleri
    private static final int DISCUSSION_SECONDS  = 45;
    private static final int VOTING_SECONDS      = 30;
    private static final int LAST_WORDS_SECONDS  = 10;   // Öldürülen oyuncunun son sözler süresi

    // Zamanlayıcı
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> nightTimer = null;
    private static ScheduledFuture<?> phaseTimer = null;

    // Son sözler — geçici olarak DAY kanalına konuşabilen ölü oyuncu
    private static volatile String lastWordsPlayer = "";

    // Gece sırası takibi
    private static int  nightPhaseIndex = 0;   // NIGHT_ORDER'daki mevcut sıra
    private static final Set<String> completedNightRoles = new HashSet<>(); // Bu gece aksiyonunu tamamlayan roller

    public static void main(String[] args) {
        System.out.println("--- Impostra Ana Sunucusu Başlatılıyor ---");

        Server server = new Server();
        Network.register(server);

        try {
            server.start();
            server.bind(54555, 54777);
            System.out.println("[BAŞARILI] Sunucu 54555 portundan dinliyor.");
        } catch (IOException e) { e.printStackTrace(); return; }

        GameManager gameManager = new GameManager();
        Map<Integer, Player> connMap = new HashMap<>();
        Set<Integer> readySet        = new HashSet<>();
        boolean[] gameStarted        = { false };
        Set<Integer> voters          = new HashSet<>();
        int[] roundNumber            = { 1 };

        server.addListener(new Listener() {

            @Override
            public void connected(Connection connection) {
                System.out.println("[BAĞLANTI] Yeni: " + connection.getID());
            }

            @Override
            public void disconnected(Connection connection) {
                synchronized (gameManager) {
                    int id = connection.getID();
                    Player ayrilan = connMap.remove(id);
                    readySet.remove(id);
                    voters.remove(id);

                    if (ayrilan != null) {
                        System.out.println("[AYRILDI] " + ayrilan.getUsername());
                        gameManager.getPlayers().remove(ayrilan);
                        broadcastLobbyUpdate(server, gameManager);
                        if (!gameStarted[0]) broadcastReadyStatus(server, gameManager, connMap, readySet);
                        if (gameStarted[0]) {
                            // Ayrılan oyuncu o gece aksiyon yapmamışsa onu da tamamlanmış say
                            // ki gece sırası kilitlenmesin
                            if (ayrilan.getRole() != null) {
                                completedNightRoles.add(ayrilan.getRole().getName() + "_" + ayrilan.getUsername());
                            }
                            String win = gameManager.checkWinCondition();
                            if (win != null) sendGameOver(server, win, gameManager);
                        }
                    }
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                // Tüm received işlemleri tek tek serileştirilir (race condition koruması).
                // KryoNet farklı thread'lerden çağırabilir; aynı anda iki istek gelirse
                // voters.size() veya nightActors.size() yanlış hesaplanabilir.
                synchronized (gameManager) {
                    handleReceived(connection, object);
                }
            }

            private void handleReceived(Connection connection, Object object) {

                // ============================================================
                //  KATILMA
                // ============================================================
                if (object instanceof Network.JoinRequest) {
                    Network.JoinRequest req = (Network.JoinRequest) object;
                    if (gameStarted[0])                          { sendReject(connection, "Oyun zaten başladı!"); return; }
                    if (gameManager.getPlayers().size() >= 14)   { sendReject(connection, "Sistem dolu (Maks 14)!"); return; }

                    // Username validasyonu
                    String uname = req.username == null ? "" : req.username.trim();
                    // Kontrol karakterlerini ve newline'ları temizle
                    uname = uname.replaceAll("[\\p{Cntrl}]", "");
                    if (uname.isEmpty())            { sendReject(connection, "Kullanıcı adı boş olamaz!"); return; }
                    if (uname.length() > 20)        { sendReject(connection, "Kullanıcı adı çok uzun (maks 20 karakter)!"); return; }
                    if (uname.length() < 2)         { sendReject(connection, "Kullanıcı adı çok kısa (min 2 karakter)!"); return; }

                    // Aynı isim kontrolü (case-insensitive)
                    synchronized (gameManager) {
                        for (Player existing : gameManager.getPlayers()) {
                            if (existing.getUsername().equalsIgnoreCase(uname)) {
                                sendReject(connection, "Bu kullanıcı adı zaten kullanılıyor!");
                                return;
                            }
                        }
                    }

                    Player p = new Player(uname);
                    gameManager.addPlayer(p);
                    connMap.put(connection.getID(), p);

                    Network.JoinResponse res = new Network.JoinResponse();
                    res.isAccepted = true;
                    res.message = "Bağlandın " + uname + "! (" + gameManager.getPlayers().size() + "/14)";
                    connection.sendTCP(res);

                    broadcastLobbyUpdate(server, gameManager);
                    broadcastReadyStatus(server, gameManager, connMap, readySet);
                }

                // ============================================================
                //  HAZIR
                // ============================================================
                if (object instanceof Network.ReadyPacket) {
                    if (gameStarted[0]) return;
                    int id = connection.getID();
                    if (readySet.contains(id)) readySet.remove(id); else readySet.add(id);
                    broadcastReadyStatus(server, gameManager, connMap, readySet);

                    int total = gameManager.getPlayers().size();
                    if (total >= 2 && readySet.size() == total) {
                        gameStarted[0] = true;
                        roundNumber[0] = 1;
                        System.out.println("\n=== HERKES HAZIR — OYUN BAŞLIYOR (" + total + ") ===");
                        gameManager.startGame();
                        sendRolePackets(server, gameManager, connMap);

                        // İlk gece sırasını başlat
                        nightPhaseIndex = 0;
                        completedNightRoles.clear();
                        startNextNightPhase(server, gameManager, connMap, voters, roundNumber, gameStarted);
                    }
                }

                // ============================================================
                //  YENİDEN BAŞLATMA
                // ============================================================
                if (object instanceof Network.RestartRequestPacket) {
                    System.out.println("[RESTART] Yeniden başlatma isteği.");
                    if (nightTimer != null) { nightTimer.cancel(false); nightTimer = null; }
                    if (phaseTimer != null) { phaseTimer.cancel(false); phaseTimer = null; }
                    gameStarted[0] = false; roundNumber[0] = 1;
                    nightPhaseIndex = 0; completedNightRoles.clear();
                    voters.clear(); readySet.clear();
                    lastWordsPlayer = "";
                    gameManager.reset();
                    server.sendToAllTCP(new Network.ResetPacket());
                    broadcastLobbyUpdate(server, gameManager);
                    broadcastReadyStatus(server, gameManager, connMap, readySet);
                }

                // ============================================================
                //  GECE AKSİYONU
                // ============================================================
                if (object instanceof Network.NightActionPacket) {
                    Network.NightActionPacket aksiyon = (Network.NightActionPacket) object;
                    Player gonderen = connMap.get(connection.getID());
                    if (gonderen == null || !gonderen.isAlive()) return;

                    String roleName = gonderen.getRole() != null ? gonderen.getRole().getName() : "";

                    // Şu an bu rolün sırası mı kontrol et
                    if (!isCurrentNightRole(roleName)) {
                        System.out.println("[GECE] " + roleName + " sırası değil, görmezden geliniyor.");
                        return;
                    }

                    // Daha önce aksiyon yaptı mı?
                    if (completedNightRoles.contains(roleName + "_" + gonderen.getUsername())) return;
                    completedNightRoles.add(roleName + "_" + gonderen.getUsername());

                    Player hedef = findPlayer(gameManager, aksiyon.targetPlayerName);
                    if (hedef == null) return;

                    processNightAction(server, gameManager, connMap, gonderen, hedef, roleName);

                    // Bu rol grubundaki tüm oyuncular aksiyonunu yaptı mı?
                    if (isNightRoleGroupComplete(gameManager, roleName)) {
                        // Zamanlayıcıyı iptal et, bir sonraki faza geç
                        if (nightTimer != null) { nightTimer.cancel(false); nightTimer = null; }
                        nightPhaseIndex++;
                        startNextNightPhase(server, gameManager, connMap, voters, roundNumber, gameStarted);
                    }
                }

                // ============================================================
                //  OY KULLANMA
                // ============================================================
                if (object instanceof Network.VotePacket) {
                    Network.VotePacket oyPaketi = (Network.VotePacket) object;
                    Player oyVeren = connMap.get(connection.getID());
                    if (oyVeren == null || !oyVeren.isAlive()) return;
                    if (voters.contains(connection.getID())) return;

                    // Kilitli oyuncu oy kullanamaz - otomatik pas
                    if (oyVeren.isLocked()) {
                        System.out.println("[OY] " + oyVeren.getUsername() + " kilitli, otomatik pas geçti.");
                        voters.add(connection.getID());
                    } else if (oyPaketi.votedPlayerName == null || oyPaketi.votedPlayerName.isEmpty()) {
                        // Skip / pas geç
                        System.out.println("[OY] " + oyVeren.getUsername() + " pas geçti.");
                        voters.add(connection.getID());
                    } else {
                        voters.add(connection.getID());
                        Player hedef = findPlayer(gameManager, oyPaketi.votedPlayerName);
                        if (hedef != null) gameManager.castVote(oyVeren, hedef);
                    }

                    System.out.println("[OY] " + oyVeren.getUsername()
                            + " (" + voters.size() + "/" + gameManager.getAliveCount() + ")");

                    if (voters.size() >= gameManager.getAliveCount()) {
                        // Tüm oylar geldi, oylama timer'ını iptal et ve normal akışı sürdür
                        if (phaseTimer != null) { phaseTimer.cancel(false); phaseTimer = null; }

                        Runnable startNextRound = () -> {
                            synchronized (gameManager) {
                                nightPhaseIndex = 0;
                                completedNightRoles.clear();
                                broadcastSystemChat(server, "🌙 Gece bastırdı...");
                                startNextNightPhase(server, gameManager, connMap, voters, roundNumber, gameStarted);
                            }
                        };
                        forceEndVoting(server, gameManager, voters, roundNumber, startNextRound);
                    }
                }

                // ============================================================
                //  SOHBET MESAJI
                // ============================================================
                if (object instanceof Network.ChatMessagePacket) {
                    Network.ChatMessagePacket msg = (Network.ChatMessagePacket) object;
                    Player gonderen = connMap.get(connection.getID());
                    if (gonderen == null) return;
                    if (msg.message == null) return;

                    // Mesaj sanitize
                    String text = msg.message.trim().replaceAll("[\\p{Cntrl}]", "");
                    if (text.isEmpty()) return;
                    if (text.length() > 200) text = text.substring(0, 200);

                    String channel = msg.channel != null ? msg.channel : "DAY";
                    GamePhase phase = gameManager.getCurrentPhase();
                    boolean isDead = !gonderen.isAlive();
                    boolean isEvil = gonderen.getRole() != null && gonderen.getRole().isEvil();

                    // Kanal & faz validasyonu
                    if (isDead) {
                        // SON SÖZLER: ölü ama lastWordsPlayer ise DAY kanalına yaz
                        if (gonderen.getUsername().equals(lastWordsPlayer)) {
                            broadcastChat(server, connMap, gonderen.getUsername() + " 💀",
                                    text, "DAY", true);
                        } else {
                            // Normal ölü kanalı
                            broadcastChat(server, connMap, gonderen.getUsername(), text, "DEAD", true);
                        }
                    } else if (phase == GamePhase.NIGHT) {
                        if (isEvil) {
                            broadcastChat(server, connMap, gonderen.getUsername(), text, "EVIL", false);
                        }
                    } else {
                        broadcastChat(server, connMap, gonderen.getUsername(), text, "DAY", false);
                    }
                }
            }
        });
    }

    // ================================================================
    //  GECE SIRASI YÖNETİMİ
    // ================================================================

    private static void startNextNightPhase(Server server, GameManager gm,
                                            Map<Integer, Player> connMap,
                                            Set<Integer> voters, int[] roundNumber,
                                            boolean[] gameStarted) {
        // Gece sırasının sonuna geldik mi?
        if (nightPhaseIndex >= GameManager.NIGHT_ORDER.length) {
            // Tüm gece aksiyonları tamamlandı → sabahı işle
            processEndOfNight(server, gm, connMap, voters, roundNumber, gameStarted);
            return;
        }

        String currentRole = GameManager.NIGHT_ORDER[nightPhaseIndex];
        int timeout = GameManager.NIGHT_TIMEOUTS[nightPhaseIndex];

        // Bu rolde hayatta oyuncu var mı?
        List<Player> activePlayers = gm.getAlivePlayersWithRole(currentRole);

        // Root Yöneticisi: ölü yoksa otomatik geç
        if (currentRole.equals("Root Yöneticisi")) {
            List<Player> rootPlayers = gm.getAlivePlayersWithRole("Root Yöneticisi");
            boolean rootUsed = !rootPlayers.isEmpty() && rootPlayers.get(0).isRootUsed();
            if (gm.getDeadPlayers().isEmpty() || rootUsed) {
                System.out.println("[GECE] Root Yöneticisi atlanıyor (ölü yok veya hak kullanıldı).");
                nightPhaseIndex++;
                startNextNightPhase(server, gm, connMap, voters, roundNumber, gameStarted);
                return;
            }
        }

        // Log Okuyucu: ölü yoksa otomatik geç
        if (currentRole.equals("Log Okuyucu") && gm.getDeadPlayers().isEmpty()) {
            System.out.println("[GECE] Log Okuyucu atlanıyor (ölü yok).");
            nightPhaseIndex++;
            startNextNightPhase(server, gm, connMap, voters, roundNumber, gameStarted);
            return;
        }

        // Bu rolde oyuncu yoksa atla
        if (activePlayers.isEmpty()) {
            System.out.println("[GECE] " + currentRole + " oyuncusu yok, atlanıyor.");
            nightPhaseIndex++;
            startNextNightPhase(server, gm, connMap, voters, roundNumber, gameStarted);
            return;
        }

        System.out.println("[GECE SIRASI] " + currentRole + " (" + timeout + "sn)");

        // İlgili oyuncuya NightPhasePacket gönder
        for (Connection c : server.getConnections()) {
            Player p = connMap.get(c.getID());
            if (p == null) continue;
            String pRole = p.getRole() != null ? p.getRole().getName() : "";

            Network.NightPhasePacket pkt = new Network.NightPhasePacket();
            pkt.activeRole      = currentRole;
            pkt.timeoutSeconds  = timeout;
            pkt.isYourTurn      = isRoleMatch(pRole, currentRole) && p.isAlive();
            pkt.blockedTarget   = "";

            // Hedef seçenekleri belirle
            if (pkt.isYourTurn) {
                if (currentRole.equals("Root Yöneticisi")) {
                    List<Player> dead = gm.getDeadPlayers();
                    pkt.targetOptions = dead.stream().map(Player::getUsername).toArray(String[]::new);
                } else if (currentRole.equals("Log Okuyucu")) {
                    List<Player> dead = gm.getDeadPlayers();
                    pkt.targetOptions = dead.stream().map(Player::getUsername).toArray(String[]::new);
                } else {
                    pkt.targetOptions = gm.getAlivePlayerNames();

                    // Güvenlik Mühendisi için son korunan oyuncuyu bul
                    if (currentRole.equals("Güvenlik Mühendisi")) {
                        for (Player pl : gm.getPlayers()) {
                            if (pl.wasLastProtected()) {
                                pkt.blockedTarget = pl.getUsername();
                                break;
                            }
                        }
                    }
                }
            } else {
                pkt.targetOptions = new String[0];
            }

            c.sendTCP(pkt);
        }

        // Zamanlayıcı — süre dolunca otomatik geç
        final String frozenRole = currentRole;
        nightTimer = scheduler.schedule(() -> {
            System.out.println("[ZAMAN DOLDU] " + frozenRole + " aksiyonunu kaçırdı.");
            completedNightRoles.add(frozenRole + "_timeout");
            nightPhaseIndex++;
            startNextNightPhase(server, gm, connMap, voters, roundNumber, gameStarted);
        }, timeout, TimeUnit.SECONDS);
    }

    private static boolean isCurrentNightRole(String roleName) {
        if (nightPhaseIndex >= GameManager.NIGHT_ORDER.length) return false;
        return isRoleMatch(roleName, GameManager.NIGHT_ORDER[nightPhaseIndex]);
    }

    /** Rogue AI ve İç Tehdit aynı gruptadır (aynı anda oynarlar) */
    private static boolean isRoleMatch(String playerRole, String nightOrderRole) {
        if (nightOrderRole.equals("Rogue AI")) {
            return playerRole.equals("Rogue AI") || playerRole.equals("İç Tehdit");
        }
        return playerRole.equals(nightOrderRole);
    }

    private static boolean isNightRoleGroupComplete(GameManager gm, String roleName) {
        String groupRole = GameManager.NIGHT_ORDER[nightPhaseIndex];
        // Bu gruptaki tüm oyuncuların aksiyonunu tamamladığını kontrol et
        List<Player> groupPlayers = new ArrayList<>();
        groupPlayers.addAll(gm.getAlivePlayersWithRole(groupRole));
        if (groupRole.equals("Rogue AI")) {
            groupPlayers.addAll(gm.getAlivePlayersWithRole("İç Tehdit"));
        }

        for (Player p : groupPlayers) {
            String key = p.getRole().getName() + "_" + p.getUsername();
            if (!completedNightRoles.contains(key)) return false;
        }
        return true;
    }

    private static void processNightAction(Server server, GameManager gm,
                                           Map<Integer, Player> connMap,
                                           Player gonderen, Player hedef, String roleName) {
        switch (roleName) {
            case "Rogue AI":
                gm.setAITarget(hedef);
                break;

            case "İç Tehdit":
                gm.setInsiderTarget(hedef);
                // Kilitlenenin kendisine bildir (sabah görür)
                break;

            case "Güvenlik Mühendisi":
                gm.setEngineerTarget(hedef);
                // Koruma onayı gönder
                Network.NightResultPacket protect = new Network.NightResultPacket();
                protect.resultType = "PROTECTED";
                protect.targetName = hedef.getUsername();
                protect.message    = "'" + hedef.getUsername() + "' adresine Firewall kuruldu.";
                sendToPlayer(server, connMap, gonderen, protect);
                break;

            case "Siber Analist":
                gm.setAnalystTarget(hedef);
                boolean isEvil = hedef.getRole() != null && hedef.getRole().isEvil();
                Network.AnalystResultPacket analystResult = new Network.AnalystResultPacket();
                analystResult.targetName = hedef.getUsername();
                analystResult.isEvil     = isEvil;
                sendToPlayer(server, connMap, gonderen, analystResult);
                System.out.println("[ANALİST] " + gonderen.getUsername() + " → "
                        + hedef.getUsername() + " → " + (isEvil ? "KÖTÜ" : "İYİ"));
                break;

            case "Root Yöneticisi":
                if (!gonderen.isRootUsed() && !hedef.isAlive()) {
                    gm.setRootTarget(hedef);
                    gonderen.setRootUsed(true);
                    Network.NightResultPacket rootResult = new Network.NightResultPacket();
                    rootResult.resultType = "RESTORED";
                    rootResult.targetName = hedef.getUsername();
                    rootResult.message    = "'" + hedef.getUsername() + "' sisteme geri yükleniyor...";
                    sendToPlayer(server, connMap, gonderen, rootResult);
                }
                break;

            case "Log Okuyucu":
                if (!hedef.isAlive()) {
                    gm.setLogTarget(hedef);
                    boolean wasEvil = hedef.getRole() != null && hedef.getRole().isEvil();
                    Network.LogReaderResultPacket logResult = new Network.LogReaderResultPacket();
                    logResult.targetName = hedef.getUsername();
                    logResult.roleName   = hedef.getRole() != null ? hedef.getRole().getName() : "?";
                    logResult.wasEvil    = wasEvil;
                    sendToPlayer(server, connMap, gonderen, logResult);
                    System.out.println("[LOG] " + gonderen.getUsername() + " okudu: "
                            + hedef.getUsername() + " → " + logResult.roleName);
                }
                break;
        }
    }

    private static void processEndOfNight(Server server, GameManager gm,
                                          Map<Integer, Player> connMap,
                                          Set<Integer> voters, int[] roundNumber,
                                          boolean[] gameStarted) {
        Map<String, String> result = gm.endNight();

        // Kazanma kontrolü
        String win = gm.checkWinCondition();
        if (win != null) { sendGameOver(server, win, gm); return; }

        // Uyuyan Bot hacklenirse bildirim gönder
        String hacked = result.get("sleeper_hacked");
        if (!hacked.isEmpty()) {
            Player hackedPlayer = findPlayer(gm, hacked);
            if (hackedPlayer != null) {
                Network.RoleChangedPacket roleChanged = new Network.RoleChangedPacket();
                roleChanged.newRole = "Rogue AI";
                roleChanged.message = "SİSTEMİNE SIZMAK! Artık Rogue AI'ın saflarındasın. Kimse bilmez.";
                sendToPlayer(server, connMap, hackedPlayer, roleChanged);
                broadcastEvilTeamUpdate(server, gm, connMap);
            }
        }

        // GÜNDÜZ TARTIŞMA FAZI BAŞLAT
        gm.setCurrentPhase(GamePhase.DAY_DISCUSSION);

        // Tartışma paketini gönder
        Network.DiscussionPhasePacket disc = new Network.DiscussionPhasePacket();
        disc.durationSeconds = DISCUSSION_SECONDS;
        disc.morningMessage  = result.get("morning_message");
        disc.killedPlayer    = result.get("killed_player");
        disc.roundNumber     = roundNumber[0];
        server.sendToAllTCP(disc);

        broadcastSystemChat(server, "🌅 " + result.get("morning_message"));

        // Yeni tur başlatma runnable'ı — hem oylama tamamlanınca hem timeout'ta kullanılacak
        final Runnable startNextRound = () -> {
            synchronized (gm) {
                nightPhaseIndex = 0;
                completedNightRoles.clear();
                broadcastSystemChat(server, "🌙 Gece bastırdı...");
                startNextNightPhase(server, gm, connMap, voters, roundNumber, gameStarted);
            }
        };

        // Süre dolunca oylama fazına geç
        if (phaseTimer != null) phaseTimer.cancel(false);
        phaseTimer = scheduler.schedule(() -> {
            synchronized (gm) {
                startVotingPhase(server, gm, voters, roundNumber, startNextRound);
            }
        }, DISCUSSION_SECONDS, TimeUnit.SECONDS);

        // Kilitli oyuncuya özel bildirim (oylama başlayınca)
        String lockedName = result.get("insider_locked");
        if (!lockedName.isEmpty()) {
            Player lockedPlayer = findPlayer(gm, lockedName);
            if (lockedPlayer != null && lockedPlayer.isAlive()) {
                Network.NightResultPacket lockNotif = new Network.NightResultPacket();
                lockNotif.resultType = "LOCKED";
                lockNotif.targetName = lockedName;
                lockNotif.message    = "KİLİTLENDİN — bu tur oy kullanamazsın.";
                scheduler.schedule(() -> sendToPlayer(server, connMap, lockedPlayer, lockNotif),
                        DISCUSSION_SECONDS * 1000L + 500, TimeUnit.MILLISECONDS);
            }
        }

        String syncKilled = result.get("sync_killed");
        if (!syncKilled.isEmpty()) {
            System.out.println("[SYNC] " + syncKilled + " de öldü.");
        }
    }

    /** Tartışma bitti, oylama fazını başlat */
    private static void startVotingPhase(Server server, GameManager gm, Set<Integer> voters,
                                         int[] roundNumber, Runnable startNextRound) {
        gm.startVoting();
        voters.clear();

        Network.VotingPhaseStartPacket vp = new Network.VotingPhaseStartPacket();
        vp.durationSeconds = VOTING_SECONDS;
        server.sendToAllTCP(vp);

        broadcastSystemChat(server, "⚖ Oylama başladı — şüpheliyi seç (" + VOTING_SECONDS + "sn).");

        System.out.println("[FAZ] Oylama başladı (" + VOTING_SECONDS + "sn).");

        // Süre bitince zorla bitir
        if (phaseTimer != null) phaseTimer.cancel(false);
        phaseTimer = scheduler.schedule(() -> {
            synchronized (gm) {
                System.out.println("[FAZ] Oylama süresi doldu.");
                forceEndVoting(server, gm, voters, roundNumber, startNextRound);
            }
        }, VOTING_SECONDS, TimeUnit.SECONDS);
    }

    /** Oylama süresi dolduğunda zorla bitir (oy vermeyenler pas sayılır). */
    private static void forceEndVoting(Server server, GameManager gm, Set<Integer> voters,
                                       int[] roundNumber, Runnable startNextRound) {
        if (gm.getCurrentPhase() != GamePhase.DAY_VOTING) return;

        String[] result = gm.endVoting();
        roundNumber[0]++;
        String win = gm.checkWinCondition();

        Network.VoteResultPacket sonuc = new Network.VoteResultPacket();
        sonuc.resultMessage  = result[0];
        sonuc.executedPlayer = result[1];
        sonuc.roundNumber    = roundNumber[0];
        sonuc.alivePlayerNames = gm.getAlivePlayerNames();
        server.sendToAllTCP(sonuc);

        broadcastSystemChat(server, "⚖ " + result[0]);
        voters.clear();

        // Son sözler — öldürülen oyuncu varsa 10sn konuşma hakkı
        String executed = result[1];
        boolean hasLastWords = executed != null && !executed.isEmpty() && win == null;
        if (hasLastWords) {
            lastWordsPlayer = executed;
            Network.LastWordsPacket lw = new Network.LastWordsPacket();
            lw.playerName       = executed;
            lw.durationSeconds  = LAST_WORDS_SECONDS;
            server.sendToAllTCP(lw);
            broadcastSystemChat(server, "💬 " + executed + " için son sözler (" + LAST_WORDS_SECONDS + "sn)...");
            System.out.println("[LAST WORDS] " + executed + " konuşabilir.");

            // Son sözler süresi bittiğinde lastWordsPlayer'ı temizle, sonra geceyi başlat
            scheduler.schedule(() -> {
                synchronized (gm) {
                    if (lastWordsPlayer.equals(executed)) {
                        lastWordsPlayer = "";
                        broadcastSystemChat(server, "💬 Son sözler sona erdi.");
                    }
                }
            }, LAST_WORDS_SECONDS, TimeUnit.SECONDS);
        }

        if (win != null) {
            final String winMsg = win;
            scheduler.schedule(() -> sendGameOver(server, winMsg, gm), 2500, TimeUnit.MILLISECONDS);
        } else {
            // Son sözler varsa gece daha uzun gecikme ile başlasın
            int delay = hasLastWords ? (LAST_WORDS_SECONDS * 1000 + 1500) : 3000;
            scheduler.schedule(startNextRound, delay, TimeUnit.MILLISECONDS);
        }
    }

    // ================================================================
    //  YARDIMCI METOTLAR
    // ================================================================

    private static void sendRolePackets(Server server, GameManager gm, Map<Integer, Player> connMap) {
        int total = gm.getPlayers().size();
        String[] allNames = new String[total];
        for (int i = 0; i < total; i++) allNames[i] = gm.getPlayers().get(i).getUsername();
        String[] evilList = buildEvilList(gm);

        for (Connection c : server.getConnections()) {
            Player p = connMap.get(c.getID());
            if (p == null || p.getRole() == null) continue;

            Network.GameStartedPacket pkt = new Network.GameStartedPacket();
            pkt.assignedRole    = p.getRole().getName();
            pkt.roleDescription = GameManager.getRoleDescription(p.getRole().getName());
            pkt.isEvil          = p.getRole().isEvil();
            pkt.playerList      = allNames;

            // Kötü takım arkadaşları
            if (p.getRole().isEvil()) {
                List<String> teammates = new ArrayList<>();
                for (String evil : evilList) if (!evil.equals(p.getUsername())) teammates.add(evil);
                pkt.evilTeammates = teammates.toArray(new String[0]);
            } else {
                pkt.evilTeammates = new String[0];
            }

            // Senkronize Düğüm eşi
            if (p.getSyncPartner() != null) {
                pkt.syncPartnerName = p.getSyncPartner().getUsername();
            }

            c.sendTCP(pkt);
        }
    }

    private static void sendToPlayer(Server server, Map<Integer, Player> connMap,
                                     Player target, Object packet) {
        for (Connection c : server.getConnections()) {
            if (connMap.get(c.getID()) == target) {
                c.sendTCP(packet); return;
            }
        }
    }

    private static String[] buildEvilList(GameManager gm) {
        List<String> names = new ArrayList<>();
        for (Player p : gm.getPlayers())
            if (p.getRole() != null && p.getRole().isEvil()) names.add(p.getUsername());
        return names.toArray(new String[0]);
    }

    /** Kötü takım listesi değiştiğinde (Uyuyan Bot hacklenince) tüm kötülere güncel listeyi yollar. */
    private static void broadcastEvilTeamUpdate(Server server, GameManager gm, Map<Integer, Player> connMap) {
        String[] allEvil = buildEvilList(gm);
        for (Connection c : server.getConnections()) {
            Player p = connMap.get(c.getID());
            if (p == null || p.getRole() == null || !p.getRole().isEvil()) continue;

            List<String> teammates = new ArrayList<>();
            for (String evil : allEvil) if (!evil.equals(p.getUsername())) teammates.add(evil);

            Network.EvilTeamUpdatePacket pkt = new Network.EvilTeamUpdatePacket();
            pkt.evilTeammates = teammates.toArray(new String[0]);
            c.sendTCP(pkt);
        }
        System.out.println("[KÖTÜ TAKIM] Güncel liste broadcast edildi: " + String.join(", ", allEvil));
    }

    // ================================================================
    //  CHAT BROADCAST
    // ================================================================

    /** Belirli kanaldaki mesajı uygun alıcılara gönder.
     *  - DAY: tüm hayatta oyunculara
     *  - DEAD: tüm ölü oyunculara
     *  - EVIL: tüm hayatta kötü oyunculara
     */
    private static void broadcastChat(Server server, Map<Integer, Player> connMap,
                                      String sender, String text, String channel, boolean fromDead) {
        Network.ChatBroadcastPacket pkt = new Network.ChatBroadcastPacket();
        pkt.sender   = sender;
        pkt.message  = text;
        pkt.channel  = channel;
        pkt.fromDead = fromDead;

        for (Connection c : server.getConnections()) {
            Player p = connMap.get(c.getID());
            if (p == null) continue;

            boolean shouldReceive = false;
            switch (channel) {
                case "DAY":
                    // Hayatta olanlar görür; ölüler de izlerken görsün
                    shouldReceive = true;
                    break;
                case "DEAD":
                    shouldReceive = !p.isAlive();
                    break;
                case "EVIL":
                    shouldReceive = p.isAlive() && p.getRole() != null && p.getRole().isEvil();
                    break;
            }
            if (shouldReceive) c.sendTCP(pkt);
        }
        System.out.println("[CHAT/" + channel + "] " + sender + ": " + text);
    }

    /** Sistem mesajı — herkese */
    private static void broadcastSystemChat(Server server, String text) {
        Network.ChatBroadcastPacket pkt = new Network.ChatBroadcastPacket();
        pkt.sender   = "SİSTEM";
        pkt.message  = text;
        pkt.channel  = "SYSTEM";
        pkt.fromDead = false;
        server.sendToAllTCP(pkt);
    }

    private static void sendReject(Connection c, String msg) {
        Network.JoinResponse r = new Network.JoinResponse();
        r.isAccepted = false; r.message = msg; c.sendTCP(r);
    }

    private static Player findPlayer(GameManager gm, String name) {
        for (Player p : gm.getPlayers()) if (p.getUsername().equals(name)) return p;
        return null;
    }

    private static void broadcastLobbyUpdate(Server server, GameManager gm) {
        String[] liste = new String[gm.getPlayers().size()];
        for (int i = 0; i < gm.getPlayers().size(); i++) liste[i] = gm.getPlayers().get(i).getUsername();
        Network.LobbyUpdatePacket p = new Network.LobbyUpdatePacket();
        p.connectedPlayers = liste; server.sendToAllTCP(p);
    }

    private static void broadcastReadyStatus(Server server, GameManager gm,
                                             Map<Integer, Player> connMap, Set<Integer> readySet) {
        int n = gm.getPlayers().size();
        String[] names = new String[n]; boolean[] flags = new boolean[n];
        for (int i = 0; i < n; i++) {
            Player p = gm.getPlayers().get(i); names[i] = p.getUsername();
            for (Map.Entry<Integer, Player> e : connMap.entrySet())
                if (e.getValue() == p) { flags[i] = readySet.contains(e.getKey()); break; }
        }
        Network.ReadyStatusPacket pkt = new Network.ReadyStatusPacket();
        pkt.connectedPlayers = names; pkt.readyFlags = flags;
        server.sendToAllTCP(pkt);
    }

    private static void sendGameOver(Server server, String msg) {
        sendGameOver(server, msg, null);
    }

    private static void sendGameOver(Server server, String msg, GameManager gm) {
        Network.GameOverPacket pkt = new Network.GameOverPacket();
        pkt.winnerMessage = msg;

        if (gm != null) {
            int n = gm.getPlayers().size();
            pkt.playerNames = new String[n];
            pkt.playerRoles = new String[n];
            pkt.playerEvil  = new boolean[n];
            pkt.playerAlive = new boolean[n];
            for (int i = 0; i < n; i++) {
                Player p = gm.getPlayers().get(i);
                pkt.playerNames[i] = p.getUsername();
                pkt.playerRoles[i] = p.getRole() != null ? p.getRole().getName() : "?";
                pkt.playerEvil[i]  = p.getRole() != null && p.getRole().isEvil();
                pkt.playerAlive[i] = p.isAlive();
            }
        } else {
            pkt.playerNames = new String[0];
            pkt.playerRoles = new String[0];
            pkt.playerEvil  = new boolean[0];
            pkt.playerAlive = new boolean[0];
        }

        server.sendToAllTCP(pkt);
        System.out.println("\n=== OYUN BİTTİ: " + msg + " ===");
    }
}