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

                    // --- MİNİMUM OYUNCU (6) İLE TEST İÇİN OTOMATİK BAŞLATMA ---
                    // Not: Arayüz (UI) yapıldığında burası bir "Host Başlat Butonu" ile değişecek.
                    if (gameManager.getPlayers().size() == 6) {
                        gameManager.startGame();

                        String[] tumOyuncular = new String[6]; // Dizi boyutunu da 6 yaptık
                        for (int i = 0; i < gameManager.getPlayers().size(); i++) {
                            tumOyuncular[i] = gameManager.getPlayers().get(i).getUsername();
                        }

                        for (Connection c : server.getConnections()) {
                            Player p = connectionPlayerMap.get(c.getID());
                            if (p != null) {
                                Network.GameStartedPacket rolPaketi = new Network.GameStartedPacket();
                                rolPaketi.assignedRole = p.getRole().getName();
                                rolPaketi.isEvil = p.getRole().isEvil();
                                rolPaketi.playerList = tumOyuncular;
                                c.sendTCP(rolPaketi);
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
                        if (gonderenOyuncu.getRole().getName().equals("Rogue AI")) {
                            gameManager.setAITarget(hedefOyuncu);
                        } else if (gonderenOyuncu.getRole().getName().equals("Güvenlik Mühendisi")) {
                            gameManager.setEngineerTarget(hedefOyuncu);
                        }

                        nightActionsReceived[0]++;
                        if (nightActionsReceived[0] == 2) {
                            gameManager.endNight();
                            Network.MorningPacket sabahPaketi = new Network.MorningPacket();
                            sabahPaketi.morningMessage = "Ağ taraması bitti. Sistem tekrar aktif!";
                            server.sendToAllTCP(sabahPaketi);
                            nightActionsReceived[0] = 0;
                            gameManager.startVoting(); // Oylamayı başlat

                            // --- FİNAL KONTROLÜ (GECE BİTİMİNDE BİRİ KAZANDI MI?) ---
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
                        gameManager.castVote(oyVerenOyuncu, hedefOyuncu);
                        votesReceived[0]++;

                        int hayattakiOyuncuSayisi = 0;
                        for (Player p : gameManager.getPlayers()) {
                            if (p.isAlive()) hayattakiOyuncuSayisi++;
                        }

                        if (votesReceived[0] == hayattakiOyuncuSayisi) {
                            gameManager.endVoting();
                            Network.VoteResultPacket sonucPaketi = new Network.VoteResultPacket();
                            sonucPaketi.resultMessage = "Ağ oylaması bitti. Loglar incelendi ve gereği yapıldı.";
                            server.sendToAllTCP(sonucPaketi);
                            votesReceived[0] = 0;

                            // --- FİNAL KONTROLÜ (OYLAMA BİTİMİNDE BİRİ KAZANDI MI?) ---
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
                        }
                    }
                }
            }
        });
    }
}