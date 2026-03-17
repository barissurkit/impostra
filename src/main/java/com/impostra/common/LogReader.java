package com.impostra.common;

public class LogReader extends Role {
    public LogReader() {
        super("Log Okuyucu", false); // İyi karakter
    }

    @Override
    public void performNightAction() {
        System.out.println("Log Okuyucu: Sistemden karantinaya alınmış (silinmiş) kullanıcıların son log kayıtlarını (Verilerini) inceliyor...");
    }
}