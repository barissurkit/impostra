package com.impostra.common;

public class RogueAI extends Role {

    public RogueAI() {
        // İsmi "Vampir", isEvil durumu "true" (Kötü)
        super("Rogue AI", true);
    }

    @Override
    public void performNightAction() {
        // İleride buraya ağ üzerinden (server'a) "Şu oyuncuyu öldürmek istiyorum" mesajı gönderme kodunu yazacağız.
        System.out.println("Yapay Zeka seçtiği bir sisteme saldırıyor...");
    }
}