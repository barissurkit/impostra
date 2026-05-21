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
        kryo.register(NightActionPacket.class);
        kryo.register(MorningPacket.class);
        kryo.register(VotePacket.class);
        kryo.register(VoteResultPacket.class);
        kryo.register(GameOverPacket.class);
        kryo.register(LobbyUpdatePacket.class);
        kryo.register(ReadyPacket.class);        // YENİ: Oyuncu hazır bildirimi
        kryo.register(ReadyStatusPacket.class);   // YENİ: Sunucu → herkese hazır durumu
        kryo.register(boolean[].class);           // ReadyStatusPacket içinde kullanılıyor
    }

    // === Mevcut paketler (HİÇBİRİ DEĞİŞMEDİ) ===
    public static class JoinRequest { public String username; }
    public static class JoinResponse { public boolean isAccepted; public String message; }
    public static class LobbyUpdatePacket { public String[] connectedPlayers; }
    public static class GameStartedPacket {
        public String assignedRole;
        public boolean isEvil;
        public String[] playerList;
    }
    public static class NightActionPacket { public String targetPlayerName; }
    public static class MorningPacket { public String morningMessage; }
    public static class VotePacket { public String votedPlayerName; }
    public static class VoteResultPacket { public String resultMessage; }
    public static class GameOverPacket { public String winnerMessage; }

    // === YENİ PAKETLER ===

    /** Client → Server: Oyuncu hazır olduğunu bildirir */
    public static class ReadyPacket { }

    /**
     * Server → All Clients: Lobideki herkesin hazır durumunu yayınlar.
     * connectedPlayers[i] ile readyFlags[i] eşleşir.
     */
    public static class ReadyStatusPacket {
        public String[]  connectedPlayers;
        public boolean[] readyFlags;
    }
}