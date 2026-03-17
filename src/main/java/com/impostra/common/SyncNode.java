package com.impostra.common;

public class SyncNode extends Role {
    public SyncNode() {
        super("Senkronize Düğüm", false); // İyi karakter
    }

    @Override
    public void performNightAction() {
        System.out.println("Senkronize Düğüm, eşleştiği diğer IP adresiyle (Node) paket senkronizasyonu yapıyor...");
    }
}