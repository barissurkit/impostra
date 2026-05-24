package com.impostra.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.impostra.common.Network;
import com.impostra.common.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServerApp {
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
        Map<Integer, Player> connMap  = new HashMap<>();
        Set<Integer> readySet         = new HashSet<>();
        boolean[] gameStarted         = { false };
        Set<Integer> nightActors      = new HashSet<>();
        Set<Integer> voters           = new HashSet<>();
        int[] roundNumber             = { 1 };  // tur sayacı

        server.addListener(new Listener() {

            @Override
            public void connected(Connection connection) {
                System.out.println("[BAĞLANTI] Yeni bağlantı: " + connection.getID());
            }

            @Override
            public void disconnected(Connection connection) {
                // ================================================================
                //  BAĞLANTI KESİLDİ — oyuncuyu listeden kaldır, lobi güncelle
                // ================================================================
                int id = connection.getID();
                Player ayrilan = connMap.remove(id);
                readySet.remove(id);
                nightActors.remove(id);
                voters.remove(id);

                if (ayrilan != null) {
                    System.out.println("[AYRILDI] " + ayrilan.getUsername());
                    gameManager.getPlayers().remove(ayrilan);

                    // Lobi güncelle
                    broadcastLobbyUpdate(server, gameManager);
                    if (!gameStarted[0]) {
                        broadcastReadyStatus(server, gameManager, connMap, readySet);
                    }

                    // Oyun sırasında ayrılırsa kazanma kontrolü yap
                    if (gameStarted[0]) {
                        String win = gameManager.checkWinCondition();
                        if (win != null) sendGameOver(server, win);
                    }
                }
            }

            @Override
            public void received(Connection connection, Object object) {

                // ============================================================
                //  KATILMA
                // ============================================================
                if (object instanceof Network.JoinRequest) {
                    Network.JoinRequest req = (Network.JoinRequest) object;
                    if (gameStarted[0]) { sendReject(connection, "Oyun zaten başladı!"); return; }
                    if (gameManager.getPlayers().size() >= 14) { sendReject(connection, "Sistem dolu (Maks 14)!"); return; }

                    Player p = new Player(req.username);
                    gameManager.addPlayer(p);
                    connMap.put(connection.getID(), p);

                    Network.JoinResponse res = new Network.JoinResponse();
                    res.isAccepted = true;
                    res.message = "Bağlandın " + req.username + "! (" + gameManager.getPlayers().size() + "/14)";
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
                    }
                }

                // ============================================================
                //  YENİDEN BAŞLATMA İSTEĞİ
                // ============================================================
                if (object instanceof Network.RestartRequestPacket) {
                    System.out.println("[RESTART] Yeniden başlatma isteği alındı.");
                    gameStarted[0] = false;
                    roundNumber[0] = 1;
                    nightActors.clear();
                    voters.clear();
                    readySet.clear();
                    gameManager.reset();

                    // Herkese reset sinyali gönder → lobiye dön
                    server.sendToAllTCP(new Network.ResetPacket());

                    // Güncel lobi durumunu gönder
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
                    if (nightActors.contains(connection.getID())) return;
                    nightActors.add(connection.getID());

                    Player hedef = findPlayer(gameManager, aksiyon.targetPlayerName);
                    if (hedef == null) return;

                    String roleName = gonderen.getRole() != null ? gonderen.getRole().getName() : "";

                    if (roleName.equals("Rogue AI")) {
                        gameManager.setAITarget(hedef);
                    } else if (roleName.equals("Güvenlik Mühendisi")) {
                        gameManager.setEngineerTarget(hedef);
                    } else if (roleName.equals("Siber Analist")) {
                        boolean targetIsEvil = hedef.getRole() != null && hedef.getRole().isEvil();
                        Network.AnalystResultPacket result = new Network.AnalystResultPacket();
                        result.targetName = hedef.getUsername();
                        result.isEvil     = targetIsEvil;
                        connection.sendTCP(result);
                        System.out.println("[ANALİST] " + gonderen.getUsername() + " → " + hedef.getUsername() + " → " + (targetIsEvil ? "KÖTÜ" : "İYİ"));
                    }

                    System.out.println("[GECE] " + gonderen.getUsername() + " (" + roleName + ") → " + hedef.getUsername()
                            + " (" + nightActors.size() + "/" + gameManager.getAliveCount() + ")");

                    if (nightActors.size() >= gameManager.getAliveCount()) {
                        nightActors.clear();
                        String[] result = gameManager.endNight();

                        String win = gameManager.checkWinCondition();
                        if (win != null) { sendGameOver(server, win); return; }

                        gameManager.startVoting();

                        Network.MorningPacket sabah = new Network.MorningPacket();
                        sabah.morningMessage = result[0];
                        sabah.killedPlayer   = result[1];
                        sabah.roundNumber    = roundNumber[0];
                        server.sendToAllTCP(sabah);
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
                    voters.add(connection.getID());

                    Player hedef = findPlayer(gameManager, oyPaketi.votedPlayerName);
                    if (hedef == null) return;

                    gameManager.castVote(oyVeren, hedef);
                    System.out.println("[OY] " + oyVeren.getUsername() + " → " + hedef.getUsername()
                            + " (" + voters.size() + "/" + gameManager.getAliveCount() + ")");

                    if (voters.size() >= gameManager.getAliveCount()) {
                        voters.clear();
                        String[] result = gameManager.endVoting();
                        roundNumber[0]++;  // tur ilerle

                        String win = gameManager.checkWinCondition();

                        Network.VoteResultPacket sonuc = new Network.VoteResultPacket();
                        sonuc.resultMessage  = result[0];
                        sonuc.executedPlayer = result[1];
                        sonuc.roundNumber    = roundNumber[0];
                        server.sendToAllTCP(sonuc);

                        if (win != null) {
                            final String winMsg = win;
                            new Thread(() -> {
                                try { Thread.sleep(2500); } catch (Exception ignored) {}
                                sendGameOver(server, winMsg);
                            }).start();
                        }
                    }
                }
            }
        });
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

            if (p.getRole().isEvil()) {
                List<String> teammates = new ArrayList<>();
                for (String evil : evilList) if (!evil.equals(p.getUsername())) teammates.add(evil);
                pkt.evilTeammates = teammates.toArray(new String[0]);
            } else {
                pkt.evilTeammates = new String[0];
            }
            c.sendTCP(pkt);
        }
    }

    private static String[] buildEvilList(GameManager gm) {
        List<String> names = new ArrayList<>();
        for (Player p : gm.getPlayers()) if (p.getRole() != null && p.getRole().isEvil()) names.add(p.getUsername());
        return names.toArray(new String[0]);
    }

    private static void sendReject(Connection c, String msg) {
        Network.JoinResponse r = new Network.JoinResponse(); r.isAccepted = false; r.message = msg; c.sendTCP(r);
    }

    private static Player findPlayer(GameManager gm, String name) {
        for (Player p : gm.getPlayers()) if (p.getUsername().equals(name)) return p;
        return null;
    }

    private static void broadcastLobbyUpdate(Server server, GameManager gm) {
        String[] liste = new String[gm.getPlayers().size()];
        for (int i = 0; i < gm.getPlayers().size(); i++) liste[i] = gm.getPlayers().get(i).getUsername();
        Network.LobbyUpdatePacket p = new Network.LobbyUpdatePacket(); p.connectedPlayers = liste;
        server.sendToAllTCP(p);
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
        Network.GameOverPacket pkt = new Network.GameOverPacket(); pkt.winnerMessage = msg;
        server.sendToAllTCP(pkt);
        System.out.println("\n=== OYUN BİTTİ: " + msg + " ===");
    }
}