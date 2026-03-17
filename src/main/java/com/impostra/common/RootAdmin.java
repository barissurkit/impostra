package com.impostra.common;

public class RootAdmin extends Role {
    public RootAdmin() {
        super("Root Yöneticisi", false); // İyi karakter
    }

    @Override
    public void performNightAction() {
        System.out.println("Root Yöneticisi, ağ üzerinde 'Kalıcı Silme' (rm -rf) veya 'Yedekten Dönme' (Restore) yetkisini kullanmayı düşünüyor...");
    }
}