package com.impostra.common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class Network {

    // Hem Server hem Client bu metodu çağıracak ki "Aynı Dili" konuşabilsinler
    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();

        // Ağ üzerinden gönderilecek tüm kargo paketlerini (sınıfları) buraya kaydetmeliyiz
        kryo.register(JoinRequest.class);
        kryo.register(JoinResponse.class);
        kryo.register(GameStartedPacket.class);
    }

    // --- AĞ ÜZERİNDEN GÖNDERİLECEK KARGO PAKETLERİ (ŞABLONLAR) ---

    // 1. Oyuncu sunucuya bağlanmak istediğinde bu paketi yollar
    public static class JoinRequest {
        public String username; // Oyuncunun girdiği kullanıcı adı
    }

    // 2. Sunucu oyuncuya cevap olarak bu paketi yollar
    public static class JoinResponse {
        public boolean isAccepted; // Lobiye kabul edildi mi?
        public String message;     // "Hoş geldin" veya "Oyun çoktan başladı" mesajı
    }

    // 3. Oyun başladığında sunucu herkese KENDİ ROLÜNÜ bu paketle yollar
    public static class GameStartedPacket {
        public String assignedRole; // Oyuncuya düşen rolün adı (Vampir, Doktor vb.)
        public boolean isEvil;      // Oyuncu kötü takımda mı? (İleride arayüzü kırmızı yapmak için kullanacağız)
    }
}