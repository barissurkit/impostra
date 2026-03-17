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
        kryo.register(LobbyUpdatePacket.class); // Dinamik Lobi Paketi Eklendi
    }

    public static class JoinRequest { public String username; }
    public static class JoinResponse { public boolean isAccepted; public String message; }
    public static class LobbyUpdatePacket { public String[] connectedPlayers; } // Lobi Listesi
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
}