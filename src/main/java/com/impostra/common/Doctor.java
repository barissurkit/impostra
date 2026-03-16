package com.impostra.common;
public class Doctor extends Role {
    public Doctor() { super("Doktor", false); }
    @Override public void performNightAction() {
        System.out.println("Doktor bu gece koruyacağı kişiyi seçiyor...");
    }
}