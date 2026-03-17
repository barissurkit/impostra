package com.impostra.client;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.impostra.common.Network;

import java.io.IOException;

public class ClientApp {
    public static void main(String[] args) {
        System.out.println("--- Impostra Terminali Başlatılıyor ---");

        Client client = new Client();
        Network.register(client);
        client.start();

        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {

                if (object instanceof Network.JoinResponse) {
                    Network.JoinResponse cevap = (Network.JoinResponse) object;
                    System.out.println("\n[SİSTEM CEVABI]: " + cevap.message);
                }

                if (object instanceof Network.GameStartedPacket) {
                    Network.GameStartedPacket rolPaketi = (Network.GameStartedPacket) object;
                    System.out.println("\n=====================================");
                    System.out.println("🔌 SİSTEM UYKU MODUNA GEÇTİ (GECE) 🔌");
                    System.out.println("SENİN YETKİ SEVİYEN: " + rolPaketi.assignedRole);

                    if (rolPaketi.isEvil) {
                        System.out.println("Sen bir Rogue AI'sın. Gizli ağda ilerle ve sistem kullanıcılarını tek tek sil...");
                    } else {
                        System.out.println("Sen bir Sistem Kullanıcısısın. Ağdaki virüsleri bul ve silinmeden hayatta kal!");
                    }

                    System.out.println("\n--- AĞDAKİ AKTİF KULLANICILAR ---");
                    for (String isim : rolPaketi.playerList) {
                        System.out.println("- IP: " + isim);
                    }
                    System.out.println("=====================================\n");

                    if (rolPaketi.assignedRole.equals("Rogue AI") || rolPaketi.assignedRole.equals("Güvenlik Mühendisi")) {
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Network.NightActionPacket eylem = new Network.NightActionPacket();
                                eylem.targetPlayerName = rolPaketi.playerList[0];
                                client.sendTCP(eylem);
                                System.out.println("[EXECUTED] Gece eylemi ağa gönderildi! Hedef IP -> " + eylem.targetPlayerName);
                            } catch (InterruptedException e) { }
                        }).start();
                    }
                }

                if (object instanceof Network.MorningPacket) {
                    Network.MorningPacket sabahPaketi = (Network.MorningPacket) object;
                    System.out.println("\n=====================================");
                    System.out.println("🌐 SİSTEM UYANDI: " + sabahPaketi.morningMessage);
                    System.out.println("Ağ logları inceleniyor... Hangi kullanıcının aktivitesi şüpheli?");
                    System.out.println("=====================================\n");

                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                            Network.VotePacket oyPaketi = new Network.VotePacket();
                            oyPaketi.votedPlayerName = "Barış";
                            client.sendTCP(oyPaketi);
                            System.out.println("[EXECUTED] Şüpheli log bildirimi (Oy) yapıldı! Hedef -> " + oyPaketi.votedPlayerName);
                        } catch (InterruptedException e) { }
                    }).start();
                }

                if (object instanceof Network.VoteResultPacket) {
                    Network.VoteResultPacket sonucPaketi = (Network.VoteResultPacket) object;
                    System.out.println("\n=====================================");
                    System.out.println("⚖️ SİSTEM YÖNETİCİSİ KARARI: " + sonucPaketi.resultMessage);
                    System.out.println("Tekrar uyku moduna geçiliyor. Bağlantılar şifreleniyor...");
                    System.out.println("=====================================\n");
                }

                // --- 5. OYUN BİTTİ (FİNAL) MESAJI ---
                if (object instanceof Network.GameOverPacket) {
                    Network.GameOverPacket bitisPaketi = (Network.GameOverPacket) object;
                    System.out.println("\n=====================================");
                    System.out.println("      !!! SİMÜLASYON SONA ERDİ !!!   ");
                    System.out.println(bitisPaketi.winnerMessage);
                    System.out.println("=====================================\n");
                    System.out.println("Bağlantı kesiliyor...");
                    System.exit(0); // İstemci programını güvenlice kapat
                }
            }
        });

        try {
            client.connect(5000, "127.0.0.1", 54555, 54777);
            Network.JoinRequest istek = new Network.JoinRequest();
            istek.username = "Barış";
            client.sendTCP(istek);
        } catch (IOException e) {
            System.out.println("Sunucuya bağlanılamadı! Ağ kablolarını kontrol et.");
        }

        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
        }
    }
}