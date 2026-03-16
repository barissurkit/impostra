package com.impostra.server;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.impostra.common.Network;
import com.impostra.common.Player;

import java.io.IOException;

public class ServerApp {
    public static void main(String[] args) {
        System.out.println("--- Impostra Sunucusu Başlatılıyor ---");

        Server server = new Server();
        // Gece gelen aksiyonları sayacağımız değişken (Bir dizi (array) mantığına girmeden basitçe sayıyoruz)
        int[] nightActionsReceived = {0};
        // Hangi bağlantı numarasının (ID), hangi Oyuncuya (Player) ait olduğunu tutan Kargo Takip Listesi
        java.util.Map<Integer, Player> connectionPlayerMap = new java.util.HashMap<>();

        // ÖNEMLİ: Kargo şirketimizi sunucuya tanıtıyoruz!
        Network.register(server);

        try {
            server.start();
            server.bind(54555, 54777);
            System.out.println("[BAŞARILI] Sunucu 54555 portundan dinlemeye başladı.");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Oyun Motorumuzu (Beynimizi) oluşturuyoruz
        GameManager gameManager = new GameManager();

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                // Eğer gelen paket bir "Oyuna Katılma İsteği" (JoinRequest) ise:
                if (object instanceof Network.JoinRequest) {
                    Network.JoinRequest istek = (Network.JoinRequest) object;
                    System.out.println("[SİSTEM] Yeni giriş isteği alındı: " + istek.username);

                    // Yeni bir Player objesi oluşturup GameManager'a (Lobiye) ekliyoruz
                    Player yeniOyuncu = new Player(istek.username);
                    gameManager.addPlayer(yeniOyuncu);
                    // Oyuncuyu bağlantı numarasıyla etiketleyip haritaya kaydediyoruz
                    connectionPlayerMap.put(connection.getID(), yeniOyuncu);

                    // Oyuncuya "Kabul edildin" mesajını (JoinResponse) geri yolluyoruz
                    Network.JoinResponse cevap = new Network.JoinResponse();
                    cevap.isAccepted = true;
                    cevap.message = "Lobiye hoş geldin " + istek.username + "! Şu an lobide " + gameManager.getPlayers().size() + " kişi var.";
                    connection.sendTCP(cevap);

                    // TEST İÇİN: Eğer lobideki kişi sayısı 4'e ulaşırsa oyunu otomatik başlat!
                    // TEST İÇİN: Eğer lobideki kişi sayısı 4'e ulaşırsa oyunu otomatik başlat!
                    if (gameManager.getPlayers().size() == 4) {
                        System.out.println("\n[SİSTEM] 4 kişiye ulaşıldı! OYUN BAŞLATILIYOR...");
                        gameManager.startGame();

                        // YENİ EKLENEN KISIM: Oyundaki herkesin ismini bir listeye (diziye) topluyoruz
                        String[] tumOyuncular = new String[4];
                        for (int i = 0; i < gameManager.getPlayers().size(); i++) {
                            tumOyuncular[i] = gameManager.getPlayers().get(i).getUsername();
                        }

                        // Şimdi herkese GİZLİCE kendi rolünü VE oyuncu listesini yolluyoruz
                        for (Connection c : server.getConnections()) {
                            Player p = connectionPlayerMap.get(c.getID());

                            if (p != null) {
                                Network.GameStartedPacket rolPaketi = new Network.GameStartedPacket();
                                rolPaketi.assignedRole = p.getRole().getName();
                                rolPaketi.isEvil = p.getRole().isEvil();
                                rolPaketi.playerList = tumOyuncular; // Listeyi kargo paketine koyduk!

                                c.sendTCP(rolPaketi);
                            }
                        }
                    }
                }

                // Eğer oyuncudan bir "Gece Eylemi" paketi gelirse:
                if (object instanceof Network.NightActionPacket) {
                    Network.NightActionPacket aksiyon = (Network.NightActionPacket) object;
                    Player gonderenOyuncu = connectionPlayerMap.get(connection.getID());

                    System.out.println("\n[GECE EYLEMİ] " + gonderenOyuncu.getUsername() + " eylemini gerçekleştirdi. Hedef: " + aksiyon.targetPlayerName);

                    // Hedef oyuncuyu GameManager'daki listeden ismine göre buluyoruz
                    Player hedefOyuncu = null;
                    for (Player p : gameManager.getPlayers()) {
                        if (p.getUsername().equals(aksiyon.targetPlayerName)) {
                            hedefOyuncu = p;
                            break;
                        }
                    }

                    // Gönderenin rolüne göre GameManager'a (Oyun Motoruna) emri iletiyoruz
                    if (hedefOyuncu != null) {
                        if (gonderenOyuncu.getRole().getName().equals("Vampir")) {
                            gameManager.setVampireTarget(hedefOyuncu);
                        } else if (gonderenOyuncu.getRole().getName().equals("Doktor")) {
                            gameManager.setDoctorTarget(hedefOyuncu);
                        }

                        // İleride buraya Cadı veya Dedektif eklendiğinde aynı mantıkla alt alta yazılır.
                    }

                    // Gelen eylemi say
                    nightActionsReceived[0]++;

                    // Eğer 2 eylem de geldiyse (Vampir ve Doktor) geceyi bitir!
                    if (nightActionsReceived[0] == 2) {
                        System.out.println("\n[SİSTEM] Tüm gece eylemleri alındı! Sabah oluyor...");

                        // Oyun motoruna hesaplamaları yap ve kimin öldüğünü bul emri veriyoruz
                        gameManager.endNight();

                        // Herkese sabah olduğunu haber veren kargoyu yolluyoruz
                        Network.MorningPacket sabahPaketi = new Network.MorningPacket();
                        sabahPaketi.morningMessage = "Güneş doğdu! Köy uyanıyor... Meydanda toplanma vakti.";

                        // sendToAllTCP ile paketi bağlı olan HERKESE aynı anda fırlatıyoruz!
                        server.sendToAllTCP(sabahPaketi);

                        // Bir sonraki gece için sayacı sıfırlıyoruz
                        nightActionsReceived[0] = 0;
                    }
                }
            }
        });
    }
}