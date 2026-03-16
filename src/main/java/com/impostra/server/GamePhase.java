package com.impostra.server;

// Oyunun içinde bulunabileceği tüm durumlar (Evreler)
public enum GamePhase {
    LOBBY,          // Oyuncuların bağlandığı ve beklediği an
    DAY_DISCUSSION, // Gündüz herkesin konuştuğu evre
    DAY_VOTING,     // Gündüz birini asmak için oy verilen evre
    NIGHT           // Gece (Vampirlerin, Doktorların gizli güçlerini kullandığı evre)
}