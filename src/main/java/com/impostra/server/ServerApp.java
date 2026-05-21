package com.impostra.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.impostra.common.Network;
import com.impostra.common.Player;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
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
            System.out.println("[BAŞARILI] Ana sunucu 54555 portundan dinliyor.");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        GameManager gameManager = new GameManager();
        Map<Integer, Player> connectionPlayerMap = new HashMap<>();

        // ============================================================
        //  YENİ: Hazır sistemi — hangi connection ID'ler hazır?
        // ============================================================
        Set<Integer> readySet = new HashSet<>();
        boolean[] gameStarted = {false};  // Oyun bir kez başladıktan sonra tekrar başlatmayı engelle

        int[] nightActionsReceived = {0};
        int[] votesReceived = {0};

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {

                // ============================================================
                //  KATILMA İSTEĞİ (mevcut mantık korundu)
                // ============================================================
                if (object instanceof Network.JoinRequest) {
                    Network.JoinRequest istek = (Network.JoinRequest) object;

                    if (gameStarted[0]) {
                        Network.JoinResponse ret = new Network.JoinResponse();
                        ret.isAccepted = false;
                        ret.message = "Oyun zaten başladı! Yeni oyuncu kabul edilmiyor.";
                        connection.sendTCP(ret);
                        return;
                    }

                    if (gameManager.getPlayers().size() >= 14) {
                        Network.JoinResponse ret = new Network.JoinResponse();
                        ret.isAccepted = false;
                        ret.message = "Bağlantı reddedildi! Sistem dolu (Maksimum 14 kullanıcı).";
                        connection.sendTCP(ret);
                        return;
                    }

                    Player yeniOyuncu = new Player(istek.username);
                    gameManager.addPlayer(yeniOyuncu);
                    connectionPlayerMap.put(connection.getID(), yeniOyuncu);

                    Network.JoinResponse cevap = new Network.JoinResponse();
                    cevap.isAccepted = true;
                    cevap.message = "Ağa bağlandın " + istek.username + "! Aktif kullanıcı: " + gameManager.getPlayers().size() + "/14";
                    connection.sendTCP(cevap);

                    System.out.println("[LOBİ] " + istek.username + " bağlandı. Toplam: " + gameManager.getPlayers().size());

                    // Herkese güncel lobi listesini gönder
                    broadcastLobbyUpdate(server, gameManager);

                    // Yeni oyuncu gelince hazır durumunu da güncelle (o henüz hazır değil)
                    broadcastReadyStatus(server, gameManager, connectionPlayerMap, readySet);
                }

                // ============================================================
                //  YENİ: HAZIR PAKETİ
                // ============================================================
                if (object instanceof Network.ReadyPacket) {
                    if (gameStarted[0]) return;  // Oyun zaten başladıysa ignore

                    int connId = connection.getID();
                    Player p = connectionPlayerMap.get(connId);
                    if (p == null) return;

                    // Toggle: tekrar basarsa hazır durumunu geri al
                    if (readySet.contains(connId)) {
                        readySet.remove(connId);
                        System.out.println("[LOBİ] " + p.getUsername() + " hazır değil.");
                    } else {
                        readySet.add(connId);
                        System.out.println("[LOBİ] " + p.getUsername() + " HAZIR!");
                    }

                    // Herkese güncel hazır durumunu yayınla
                    broadcastReadyStatus(server, gameManager, connectionPlayerMap, readySet);

                    // Herkes hazır mı kontrol et (minimum 2 kişi)
                    int totalPlayers = gameManager.getPlayers().size();
                    if (totalPlayers >= 2 && readySet.size() == totalPlayers) {
                        gameStarted[0] = true;
                        System.out.println("\n[SİSTEM] HERKES HAZIR! Oyun başlatılıyor... (" + totalPlayers + " oyuncu)");
                        startGameForAll(server, gameManager, connectionPlayerMap);
                    }
                }

                // ============================================================
                //  GECE AKSİYONU (mevcut mantık korundu)
                // ============================================================
                if (object instanceof Network.NightActionPacket) {
                    Network.NightActionPacket aksiyon = (Network.NightActionPacket) object;
                    Player gonderenOyuncu = connectionPlayerMap.get(connection.getID());

                    Player hedefOyuncu = null;
                    for (Player p : gameManager.getPlayers()) {
                        if (p.getUsername().equals(aksiyon.targetPlayerName)) {
                            hedefOyuncu = p; break;
                        }
                    }

                    if (hedefOyuncu != null) {
                        if (gonderenOyuncu.getRole() != null && gonderenOyuncu.getRole().getName().equals("Rogue AI")) {
                            gameManager.setAITarget(hedefOyuncu);
                        } else if (gonderenOyuncu.getRole() != null && gonderenOyuncu.getRole().getName().equals("Güvenlik Mühendisi")) {
                            gameManager.setEngineerTarget(hedefOyuncu);
                        }

                        nightActionsReceived[0]++;

                        // Hayattaki oyuncu sayısı kadar aksiyon gelince sabah olsun
                        int alive = 0;
                        for (Player p : gameManager.getPlayers()) { if (p.isAlive()) alive++; }

                        if (nightActionsReceived[0] >= alive) {
                            Network.MorningPacket sabahPaketi = new Network.MorningPacket();
                            sabahPaketi.morningMessage = "AĞ TARAMASI BİTTİ! ŞÜPHELİYİ SİSTEMDEN ATMAK İÇİN OYLAMA BAŞLADI.";
                            server.sendToAllTCP(sabahPaketi);
                            nightActionsReceived[0] = 0;
                        }
                    }
                }

                // ============================================================
                //  OY KULLANMA (mevcut mantık korundu)
                // ============================================================
                if (object instanceof Network.VotePacket) {
                    Network.VotePacket oyPaketi = (Network.VotePacket) object;

                    Player hedefOyuncu = null;
                    for (Player p : gameManager.getPlayers()) {
                        if (p.getUsername().equals(oyPaketi.votedPlayerName)) {
                            hedefOyuncu = p; break;
                        }
                    }

                    if (hedefOyuncu != null) {
                        votesReceived[0]++;

                        int alive = 0;
                        for (Player p : gameManager.getPlayers()) { if (p.isAlive()) alive++; }

                        if (votesReceived[0] >= alive) {
                            Network.VoteResultPacket sonucPaketi = new Network.VoteResultPacket();
                            sonucPaketi.resultMessage = "AĞ OYLAMASI BİTTİ. SİSTEM LOGLARI KAYDEDİLDİ.";
                            server.sendToAllTCP(sonucPaketi);
                            votesReceived[0] = 0;
                        }
                    }
                }
            }
        });
    }

    // ================================================================
    //  YARDIMCI METOTLAR
    // ================================================================

    /** Herkese güncel oyuncu listesini gönder */
    private static void broadcastLobbyUpdate(Server server, GameManager gm) {
        String[] liste = new String[gm.getPlayers().size()];
        for (int i = 0; i < gm.getPlayers().size(); i++) {
            liste[i] = gm.getPlayers().get(i).getUsername();
        }
        Network.LobbyUpdatePacket paket = new Network.LobbyUpdatePacket();
        paket.connectedPlayers = liste;
        server.sendToAllTCP(paket);
    }

    /** Herkese güncel hazır durumunu gönder */
    private static void broadcastReadyStatus(Server server, GameManager gm,
                                             Map<Integer, Player> connMap, Set<Integer> readySet) {
        int n = gm.getPlayers().size();
        String[] names = new String[n];
        boolean[] flags = new boolean[n];

        for (int i = 0; i < n; i++) {
            Player p = gm.getPlayers().get(i);
            names[i] = p.getUsername();

            // Bu oyuncunun connection ID'sini bul
            for (Map.Entry<Integer, Player> entry : connMap.entrySet()) {
                if (entry.getValue() == p) {
                    flags[i] = readySet.contains(entry.getKey());
                    break;
                }
            }
        }

        Network.ReadyStatusPacket paket = new Network.ReadyStatusPacket();
        paket.connectedPlayers = names;
        paket.readyFlags = flags;
        server.sendToAllTCP(paket);
    }

    /**
     * Herkese rol dağıtıp oyunu başlat.
     * TEST MODU: Sahte roller — ilk oyuncuya Rogue AI, ikinciye Sistem Mühendisi,
     * gerisine sırayla Kullanıcı / Siber Analist vs.
     */
    private static void startGameForAll(Server server, GameManager gm, Map<Integer, Player> connMap) {
        int n = gm.getPlayers().size();
        String[] tumOyuncular = new String[n];
        for (int i = 0; i < n; i++) {
            tumOyuncular[i] = gm.getPlayers().get(i).getUsername();
        }

        // Sahte roller: ilk oyuncu kötü, geri kalan iyi
        // İleride gameManager.startGame() ile gerçek rol dağıtımı yapılacak
        String[] sahteRoller = {"Rogue AI", "Sistem Mühendisi", "Siber Analist", "Kullanıcı",
                "Güvenlik Mühendisi", "Log Okuyucu", "Root Yöneticisi",
                "Uyuyan Bot", "Senkronize Düğüm", "Kullanıcı",
                "Kullanıcı", "Kullanıcı", "Kullanıcı", "Kullanıcı"};

        int idx = 0;
        for (Connection c : server.getConnections()) {
            Player p = connMap.get(c.getID());
            if (p == null) continue;

            Network.GameStartedPacket rolPaketi = new Network.GameStartedPacket();
            rolPaketi.assignedRole = sahteRoller[idx % sahteRoller.length];
            rolPaketi.isEvil = rolPaketi.assignedRole.equals("Rogue AI");
            rolPaketi.playerList = tumOyuncular;
            c.sendTCP(rolPaketi);

            System.out.println("[ROL] " + p.getUsername() + " → " + rolPaketi.assignedRole +
                    (rolPaketi.isEvil ? " [KÖTÜ]" : " [İYİ]"));
            idx++;
        }
    }
}