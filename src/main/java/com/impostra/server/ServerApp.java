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
                    if (gameManager.getPlayers().size() == 4) {
                        System.out.println("\n[SİSTEM] 4 kişiye ulaşıldı! OYUN BAŞLATILIYOR...");
                        gameManager.startGame(); // Senin yazdığın o efsane rol dağıtma motoru çalışır!

                        // Şimdi herkese GİZLİCE kendi rolünü yolluyoruz
                        for (Connection c : server.getConnections()) {
                            // Bu bağlantıya (c) ait olan oyuncuyu haritadan buluyoruz
                            Player p = connectionPlayerMap.get(c.getID());

                            if (p != null) {
                                // Oyuncuya özel kargo paketini hazırlıyoruz
                                Network.GameStartedPacket rolPaketi = new Network.GameStartedPacket();
                                rolPaketi.assignedRole = p.getRole().getName();
                                rolPaketi.isEvil = p.getRole().isEvil();

                                // Paketi sadece ona (c) gönderiyoruz!
                                c.sendTCP(rolPaketi);
                            }
                        }
                    }
                }
            }
        });
    }
}