package com.impostra.common;

public class CyberAnalyst extends Role {
    public CyberAnalyst() {
        super("Siber Analist", false); // İyi karakter
    }

    @Override
    public void performNightAction() {
        System.out.println("Siber Analist, hedef IP'nin kaynak kodlarını tarayıp yapay zeka olup olmadığını analiz ediyor...");
    }
}