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
        kryo.register(ReadyPacket.class);
        kryo.register(ReadyStatusPacket.class);
        kryo.register(boolean[].class);
        kryo.register(AnalystResultPacket.class);
        kryo.register(ResetPacket.class);     // YENİ: sunucu → client, lobi sıfırla
        kryo.register(RestartRequestPacket.class); // YENİ: client → sunucu, yeniden başlat
    }

    public static class JoinRequest  { public String username; }
    public static class JoinResponse { public boolean isAccepted; public String message; }
    public static class LobbyUpdatePacket { public String[] connectedPlayers; }

    public static class GameStartedPacket {
        public String   assignedRole;
        public String   roleDescription;
        public boolean  isEvil;
        public String[] playerList;
        public String[] evilTeammates;
    }

    public static class NightActionPacket { public String targetPlayerName; }

    public static class MorningPacket {
        public String morningMessage;
        public String killedPlayer;
        public int    roundNumber;   // YENİ: kaçıncı tur
    }

    public static class VotePacket       { public String votedPlayerName; }

    public static class VoteResultPacket {
        public String resultMessage;
        public String executedPlayer;
        public int    roundNumber;   // YENİ: kaçıncı tur
    }

    public static class GameOverPacket   { public String winnerMessage; }
    public static class ReadyPacket      { }

    public static class ReadyStatusPacket {
        public String[]  connectedPlayers;
        public boolean[] readyFlags;
    }

    public static class AnalystResultPacket {
        public String  targetName;
        public boolean isEvil;
    }

    /** Sunucu → Tüm clientlar: oyun sıfırlandı, lobiye dön */
    public static class ResetPacket { }

    /** Client → Sunucu: oyunu yeniden başlatmak istiyorum */
    public static class RestartRequestPacket { }
}