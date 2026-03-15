package com.impostra.common;

public class Vampire extends Role {

    public Vampire() {
        // İsmi "Vampir", isEvil durumu "true" (Kötü)
        super("Vampir", true);
    }

    @Override
    public void performNightAction() {
        // İleride buraya ağ üzerinden (server'a) "Şu oyuncuyu öldürmek istiyorum" mesajı gönderme kodunu yazacağız.
        System.out.println("Vampir avlanmaya çıktı! Bir kurban seçiliyor...");
    }
}