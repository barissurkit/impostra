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
        kryo.register(ResetPacket.class);
        kryo.register(RestartRequestPacket.class);
        // Yeni paketler
        kryo.register(NightPhasePacket.class);
        kryo.register(NightResultPacket.class);
        kryo.register(RoleChangedPacket.class);
        kryo.register(LogReaderResultPacket.class);
        kryo.register(EvilTeamUpdatePacket.class);
        kryo.register(ChatMessagePacket.class);
        kryo.register(ChatBroadcastPacket.class);
        kryo.register(DiscussionPhasePacket.class);
        kryo.register(VotingPhaseStartPacket.class);
        kryo.register(LastWordsPacket.class);
    }

    // ============================================================
    //  MEVCUT PAKETLER
    // ============================================================

    public static class JoinRequest  { public String username; }
    public static class JoinResponse { public boolean isAccepted; public String message; }
    public static class LobbyUpdatePacket { public String[] connectedPlayers; }

    public static class GameStartedPacket {
        public String   assignedRole;
        public String   roleDescription;
        public boolean  isEvil;
        public String[] playerList;
        public String[] evilTeammates;
        public String   syncPartnerName;   // Senkronize Düğüm eşi
    }

    public static class NightActionPacket { public String targetPlayerName; }

    public static class MorningPacket {
        public String   morningMessage;
        public String   killedPlayer;
        public int      roundNumber;
        public String[] alivePlayerNames;  // Güncel hayatta listesi
    }

    public static class VotePacket       { public String votedPlayerName; }

    public static class VoteResultPacket {
        public String   resultMessage;
        public String   executedPlayer;
        public int      roundNumber;
        public String[] alivePlayerNames;  // Güncel hayatta listesi
    }

    public static class GameOverPacket {
        public String   winnerMessage;
        public String[] playerNames;   // Tüm oyuncular
        public String[] playerRoles;   // Aynı sıradaki rolleri
        public boolean[] playerEvil;   // Kötü müydü?
        public boolean[] playerAlive;  // Sonda hayatta mıydı?
    }
    public static class ReadyPacket           { }
    public static class ResetPacket           { }
    public static class RestartRequestPacket  { }

    public static class ReadyStatusPacket {
        public String[]  connectedPlayers;
        public boolean[] readyFlags;
    }

    public static class AnalystResultPacket {
        public String  targetName;
        public boolean isEvil;
    }

    // ============================================================
    //  YENİ PAKETLER
    // ============================================================

    /**
     * Sunucu → Client: Şu an hangi rol aksiyonunu yapıyor?
     * Gece sırası UI için kullanılır — zamanlayıcı ve "sıranı bekle" mesajı.
     */
    public static class NightPhasePacket {
        public String  activeRole;       // Şu an aksiyonu olan rol (örn. "Rogue AI")
        public int     timeoutSeconds;   // Kaç saniyesi var
        public boolean isYourTurn;       // Bu paketin gönderildiği oyuncunun sırası mı?
        public String[] targetOptions;   // Seçebileceği hedefler (hayatta olanlar / ölüler)
        public String  blockedTarget;    // Bu oyuncuyu seçemezsin (örn. Güvenlik Mühendisi'nin önceki hedefi)
    }

    /**
     * Sunucu → Client: Gece aksiyonu sonucu bildirimi.
     * Her aksiyondan sonra ilgili oyunculara özel sonuç gönderilir.
     */
    public static class NightResultPacket {
        public String resultType;   // "PROTECTED", "KILLED", "ANALYZED", "LOCKED", "RESTORED", "LOG_READ"
        public String targetName;
        public String message;
    }

    /**
     * Sunucu → Uyuyan Bot: Rol değişti bildirimi.
     * Sadece hacklendiğinde Uyuyan Bot'a gönderilir.
     */
    public static class RoleChangedPacket {
        public String newRole;
        public String message;
    }

    /**
     * Sunucu → Log Okuyucu: Ölen oyuncunun rolü.
     */
    public static class LogReaderResultPacket {
        public String targetName;
        public String roleName;
        public boolean wasEvil;
    }

    /**
     * Sunucu → Kötü takım: Takım listesi güncellendi (Uyuyan Bot hacklenince).
     */
    public static class EvilTeamUpdatePacket {
        public String[] evilTeammates;   // Alıcı hariç tüm kötü takım üyeleri
    }

    // ============================================================
    //  SOHBET PAKETLERİ
    // ============================================================

    /** Client → Sunucu: mesaj göndermek istiyorum */
    public static class ChatMessagePacket {
        public String message;
        public String channel;  // "DAY" (herkes), "DEAD" (ölüler), "EVIL" (kötüler gecesi)
    }

    /** Sunucu → Client(lar): bir mesaj geldi */
    public static class ChatBroadcastPacket {
        public String sender;
        public String message;
        public String channel;     // "DAY" / "DEAD" / "EVIL" / "SYSTEM"
        public boolean fromDead;   // ölü mü gönderdi (görsel için)
    }

    // ============================================================
    //  FAZ PAKETLERİ (tartışma & oylama)
    // ============================================================

    /** Sunucu → Tüm clientlar: gündüz tartışma fazı başladı */
    public static class DiscussionPhasePacket {
        public int    durationSeconds;
        public String morningMessage;   // Sabah olayları
        public String killedPlayer;
        public int    roundNumber;
    }

    /** Sunucu → Tüm clientlar: oylama fazı başladı (tartışma süresi bitti) */
    public static class VotingPhaseStartPacket {
        public int durationSeconds;     // 0 = süresiz
    }

    /**
     * Sunucu → Tüm clientlar: bir oyuncu öldürüldü, son sözler süresi başladı.
     * Bu süre boyunca o oyuncunun mesajları DAY kanalına gider, herkes görür.
     */
    public static class LastWordsPacket {
        public String playerName;
        public int    durationSeconds;
    }
}