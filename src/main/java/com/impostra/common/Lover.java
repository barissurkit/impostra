package com.impostra.common;
public class Lover extends Role {
    public Lover() { super("Aşık", false); }
    @Override public void performNightAction() {
        // Gece yapacakları eylem yoktur, sadece birbirlerini bilirler.
    }
}