package com.impostra.common;
public class Witch extends Role {
    public Witch() { super("Cadı", false); }
    @Override public void performNightAction() {
        System.out.println("Cadı iksirlerini kullanıp kullanmayacağına karar veriyor...");
    }
}