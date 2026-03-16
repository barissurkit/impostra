package com.impostra.common;
public class Dishonest extends Role {
    public Dishonest() { super("Sahtekar", true); } // Kötü taraftadır ama dedektife masum görünür (Bunu GameManager'da ayarlayacağız)
    @Override public void performNightAction() {
        System.out.println("Sahtekar ortalığı karıştırıyor...");
    }
}