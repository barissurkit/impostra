package com.impostra.client;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.impostra.common.Network;

import java.io.IOException;

public class ClientApp {
    public static void main(String[] args) {
        System.out.println("--- Impostra Oyuncusu (Client) Başlatılıyor ---");

        Client client = new Client();

        // ÖNEMLİ: Kargo şirketimizi oyuncuya da tanıtıyoruz!
        Network.register(client);

        client.start();

        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {

                // --- 1. LOBİYE KATILMA CEVABI ---
                if (object instanceof Network.JoinResponse) {
                    Network.JoinResponse cevap = (Network.JoinResponse) object;
                    System.out.println("\n[SUNUCUDAN CEVAP]: " + cevap.message);
                }

                // --- 2. OYUN BAŞLADI VE ROL DAĞITILDI ---
                if (object instanceof Network.GameStartedPacket) {
                    Network.GameStartedPacket rolPaketi = (Network.GameStartedPacket) object;
                    System.out.println("\n=====================================");
                    System.out.println("🔥 OYUN BAŞLADI! EKRAN KARARDI (GECE OLDU) 🔥");
                    System.out.println("SENİN ROLÜN: " + rolPaketi.assignedRole);

                    if (rolPaketi.isEvil) {
                        System.out.println("Sen bir kötüsün! Köylüleri avlama vakti...");
                    } else {
                        System.out.println("Sen bir masumsun. Geceleri hayatta kalmaya çalış!");
                    }

                    System.out.println("\n--- KASABADAKİ OYUNCULAR ---");
                    for (String isim : rolPaketi.playerList) {
                        System.out.println("- " + isim);
                    }
                    System.out.println("=====================================\n");

                    // TEST: Eğer oyuncu Vampir veya Doktor ise 2 saniye sonra otomatik eylem yapsın
                    if (rolPaketi.assignedRole.equals("Vampir") || rolPaketi.assignedRole.equals("Doktor")) {
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Network.NightActionPacket eylem = new Network.NightActionPacket();
                                eylem.targetPlayerName = rolPaketi.playerList[0]; // Şimdilik test için listedeki ilk kişiyi seçiyor
                                client.sendTCP(eylem);
                                System.out.println("[SİSTEM] Gece eylemi sunucuya fırlatıldı! Hedef -> " + eylem.targetPlayerName);
                            } catch (InterruptedException e) { }
                        }).start();
                    }
                }

                // --- 3. SABAH OLDU MESAJI ---
                if (object instanceof Network.MorningPacket) {
                    Network.MorningPacket sabahPaketi = (Network.MorningPacket) object;
                    System.out.println("\n=====================================");
                    System.out.println("🌅 " + sabahPaketi.morningMessage);
                    System.out.println("Gündüz tartışması başladı. Kim şüpheli?");
                    System.out.println("=====================================\n");
                }
            }
        });

        try {
            client.connect(5000, "127.0.0.1", 54555, 54777);
            System.out.println("Sunucuya bağlantı başarılı! Giriş isteği gönderiliyor...\n");

            // Kargo paketimizi hazırlıyoruz
            Network.JoinRequest istek = new Network.JoinRequest();
            istek.username = "Barış"; // Oyuncunun adı

            // Paketi sunucuya fırlatıyoruz!
            client.sendTCP(istek);

        } catch (IOException e) {
            System.out.println("Sunucuya bağlanılamadı! Sunucunun açık olduğundan emin ol.");
            e.printStackTrace();
        }

        // Programın kapanmaması için döngü
        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
        }
    }
}