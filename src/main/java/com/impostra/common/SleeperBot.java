package com.impostra.common;

public class SleeperBot extends Role {
    public SleeperBot() {
        super("Uyuyan Bot", false); // Başlangıçta iyi, hacklenirse true olacak
    }

    @Override
    public void performNightAction() {
        System.out.println("Uyuyan Bot: Sistemde sıradan bir kullanıcı gibi davranarak bekliyor (Hacklenene kadar aktifleşmez)...");
    }
}