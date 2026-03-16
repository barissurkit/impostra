package com.impostra.common;
public class Medium extends Role {
    public Medium() { super("Medyum", false); }
    @Override public void performNightAction() {
        System.out.println("Medyum ölülerin ruhlarıyla iletişime geçiyor...");
    }
}