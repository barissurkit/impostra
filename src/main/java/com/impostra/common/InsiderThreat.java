package com.impostra.common;

public class InsiderThreat extends Role {
    public InsiderThreat() {
        super("İç Tehdit", true); // Kötü karakter
    }

    @Override
    public void performNightAction() {
        System.out.println("İç Tehdit: Yapay Zeka (AI) algoritmalarını korumak için ağda şaşırtmaca yapıyor...");
    }
}