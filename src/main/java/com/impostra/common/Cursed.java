package com.impostra.common;
public class Cursed extends Role {
    public Cursed() { super("Lanetli", false); } // Başlangıçta köylü (kötü değil)
    @Override public void performNightAction() {
        // Lanetlinin gece kendi yapacağı bir eylem yoktur.
    }
}