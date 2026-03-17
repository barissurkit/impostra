package com.impostra.common;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

public class Network {

    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();

        kryo.register(JoinRequest.class);
        kryo.register(JoinResponse.class);
        kryo.register(GameStartedPacket.class);
        kryo.register(String[].class);
        // Gece aksiyonu kargosu sisteme tanıtıldı
        kryo.register(NightActionPacket.class);
        kryo.register(MorningPacket.class);

        kryo.register(VotePacket.class);
        kryo.register(VoteResultPacket.class);

        kryo.register(GameOverPacket.class);
    }

    // --- KARGO PAKETLERİ ---

    public static class JoinRequest {
        public String username;
    }

    public static class JoinResponse {
        public boolean isAccepted;
        public String message;
    }

    public static class GameStartedPacket {
        public String assignedRole;
        public boolean isEvil;
        public String[] playerList;
    }

    // EKSİK OLAN VE HATA VERDİREN PAKET İŞTE BURADA:
    public static class NightActionPacket {
        public String targetPlayerName;
    }

    // 5. Gece bittiğinde sunucu herkese "Sabah Oldu" mesajını yollar
    public static class MorningPacket {
        public String morningMessage; // Gece ne olduğuyla ilgili genel bilgi
    }

    // 6. Oyuncu gündüz oylamasında birine oy verdiğinde bunu yollar
    public static class VotePacket {
        public String votedPlayerName; // İdam edilmesini istediği kişinin adı
    }

    // 7. Oylama bittiğinde sunucu sonucu herkese bu paketle bildirir
    public static class VoteResultPacket {
        public String resultMessage; // "Ahmet asıldı" veya "Beraberlik çıktı" mesajı
    }

    public static class GameOverPacket {
        public String winnerMessage;
    }
}