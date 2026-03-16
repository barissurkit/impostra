package com.impostra.common;
public class Detective extends Role {
    public Detective() { super("Dedektif", false); }
    @Override public void performNightAction() {
        System.out.println("Dedektif birinin kimliğini araştırıyor...");
    }
}