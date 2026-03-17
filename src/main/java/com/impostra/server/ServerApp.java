package com.impostra.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.impostra.common.Network;
import com.impostra.common.Player;

import java.io.IOException;

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
        java.util.Map<Integer, Player> connectionPlayerMap = new java.util.HashMap<>();

        int[] nightActionsReceived = {0};
        int[] votesReceived = {0};

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {

                if (object instanceof Network.JoinRequest) {
                    Network.JoinRequest istek = (Network.JoinRequest) object;

                    // --- MAKSİMUM OYUNCU (14) KONTROLÜ ---
                    if (gameManager.getPlayers().size() >= 14) {
                        Network.JoinResponse retCevabi = new Network.JoinResponse();
                        retCevabi.isAccepted = false;
                        retCevabi.message = "Bağlantı reddedildi! Sistem dolu (Maksimum 14 kullanıcı).";
                        connection.sendTCP(retCevabi);
                        return; // Metodu burada kes, oyuncuyu içeri alma
                    }

                    Player yeniOyuncu = new Player(istek.username);
                    gameManager.addPlayer(yeniOyuncu);
                    connectionPlayerMap.put(connection.getID(), yeniOyuncu);

                    Network.JoinResponse cevap = new Network.JoinResponse();
                    cevap.isAccepted = true;
                    cevap.message = "Ağa bağlandın " + istek.username + "! Aktif kullanıcı: " + gameManager.getPlayers().size() + "/14";
                    connection.sendTCP(cevap);

                    // Sunucudaki herkesin güncel ismini bir diziye topla
                    String[] guncelListe = new String[gameManager.getPlayers().size()];
                    for (int i = 0; i < gameManager.getPlayers().size(); i++) {
                        guncelListe[i] = gameManager.getPlayers().get(i).getUsername();
                    }

                    // Bu diziyi paketle ve İSTİSNASIZ HERKESE yolla
                    Network.LobbyUpdatePacket lobiPaketi = new Network.LobbyUpdatePacket();
                    lobiPaketi.connectedPlayers = guncelListe;
                    server.sendToAllTCP(lobiPaketi);

                    // --- 2 KİŞİ İLE TEST İÇİN MANUEL BAŞLATMA ---
                    if (gameManager.getPlayers().size() == 2) {

                        // DİKKAT: gameManager.startGame() BURADAN KALDIRILDI (Çökmeyi önlemek için)

                        String[] tumOyuncular = new String[2];
                        for (int i = 0; i < gameManager.getPlayers().size(); i++) {
                            tumOyuncular[i] = gameManager.getPlayers().get(i).getUsername();
                        }

                        // Test için birinize Virüs, diğerinize Güvenlikçi rolünü verelim
                        boolean virusMu = true;

                        for (Connection c : server.getConnections()) {
                            Player p = connectionPlayerMap.get(c.getID());
                            if (p != null) {
                                Network.GameStartedPacket rolPaketi = new Network.GameStartedPacket();
                                // GameManager'dan almak yerine sahte rol basıyoruz:
                                rolPaketi.assignedRole = virusMu ? "Rogue AI" : "Sistem Mühendisi";
                                rolPaketi.isEvil = virusMu;
                                rolPaketi.playerList = tumOyuncular;
                                c.sendTCP(rolPaketi);

                                virusMu = !virusMu; // Sonraki döngüde diğer oyuncuya zıt rolü ver
                            }
                        }
                    }
                }

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
                        // Not: Test aşamasında GameManager sahte rollerle çalıştığı için
                        // buradaki yetenek kullanımları konsola hata basabilir, şimdilik görmezden gelebiliriz.
                        if (gonderenOyuncu.getRole() != null && gonderenOyuncu.getRole().getName().equals("Rogue AI")) {
                            gameManager.setAITarget(hedefOyuncu);
                        } else if (gonderenOyuncu.getRole() != null && gonderenOyuncu.getRole().getName().equals("Güvenlik Mühendisi")) {
                            gameManager.setEngineerTarget(hedefOyuncu);
                        }

                        nightActionsReceived[0]++;
                        if (nightActionsReceived[0] == 2) {

                            // GÜNCELLEME: Çökmeyi önlemek için gameManager.endNight() yoruma alındı.
                            // gameManager.endNight();

                            Network.MorningPacket sabahPaketi = new Network.MorningPacket();
                            sabahPaketi.morningMessage = "AĞ TARAMASI BİTTİ! ŞÜPHELİYİ SİSTEMDEN ATMAK İÇİN OYLAMA BAŞLADI.";
                            server.sendToAllTCP(sabahPaketi);
                            nightActionsReceived[0] = 0;

                            // GÜNCELLEME: Çökmeyi önlemek için gameManager.startVoting() yoruma alındı.
                            // gameManager.startVoting();

                            // GÜNCELLEME: Çökmeyi önlemek için bitiş kontrolü yoruma alındı.
                            /*
                            String bitisMesaji = gameManager.checkWinCondition();
                            if (bitisMesaji != null) {
                                Network.GameOverPacket bitisPaketi = new Network.GameOverPacket();
                                bitisPaketi.winnerMessage = bitisMesaji;
                                server.sendToAllTCP(bitisPaketi);

                                System.out.println("\n[SİSTEM] OYUN BİTTİ. Sunucu 2 saniye içinde kapatılıyor...");
                                new Thread(() -> {
                                    try { Thread.sleep(2000); System.exit(0); } catch (Exception ignored) {}
                                }).start();
                            }
                            */
                        }
                    }
                }

                if (object instanceof Network.VotePacket) {
                    Network.VotePacket oyPaketi = (Network.VotePacket) object;
                    Player oyVerenOyuncu = connectionPlayerMap.get(connection.getID());

                    Player hedefOyuncu = null;
                    for (Player p : gameManager.getPlayers()) {
                        if (p.getUsername().equals(oyPaketi.votedPlayerName)) {
                            hedefOyuncu = p; break;
                        }
                    }

                    if (hedefOyuncu != null) {

                        // GÜNCELLEME: Çökmeyi önlemek için castVote yoruma alındı.
                        // gameManager.castVote(oyVerenOyuncu, hedefOyuncu);

                        votesReceived[0]++;

                        int hayattakiOyuncuSayisi = 0;
                        for (Player p : gameManager.getPlayers()) {
                            if (p.isAlive()) hayattakiOyuncuSayisi++;
                        }

                        // TEST İÇİN GÜNCELLEME: 2 kişi de oy verince oylamayı bitir
                        if (votesReceived[0] == 2) {

                            // GÜNCELLEME: Çökmeyi önlemek için endVoting yoruma alındı.
                            // gameManager.endVoting();

                            Network.VoteResultPacket sonucPaketi = new Network.VoteResultPacket();
                            sonucPaketi.resultMessage = "AĞ OYLAMASI BİTTİ. SİSTEM LOGLARI KAYDEDİLDİ.";
                            server.sendToAllTCP(sonucPaketi);
                            votesReceived[0] = 0;

                            // GÜNCELLEME: Çökmeyi önlemek için bitiş kontrolü yoruma alındı.
                            /*
                            String bitisMesaji = gameManager.checkWinCondition();
                            if (bitisMesaji != null) {
                                Network.GameOverPacket bitisPaketi = new Network.GameOverPacket();
                                bitisPaketi.winnerMessage = bitisMesaji;
                                server.sendToAllTCP(bitisPaketi);

                                System.out.println("\n[SİSTEM] OYUN BİTTİ. Sunucu 2 saniye içinde kapatılıyor...");
                                new Thread(() -> {
                                    try { Thread.sleep(2000); System.exit(0); } catch (Exception ignored) {}
                                }).start();
                            }
                            */
                        }
                    }
                }
            }
        });
    }
}