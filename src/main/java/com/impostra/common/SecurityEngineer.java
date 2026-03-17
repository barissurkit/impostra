package com.impostra.common;

public class SecurityEngineer extends Role {
    public SecurityEngineer() {
        super("Güvenlik Mühendisi", false); // İyi karakter
    }

    @Override
    public void performNightAction() {
        System.out.println("Güvenlik Mühendisi: Hedef IP adresine geçici bir Firewall (Güvenlik Duvarı) kuruyor...");
    }
}