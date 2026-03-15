package com.impostra.common;

// abstract (soyut) sınıf yapıyoruz çünkü kimse düz "Rol" olamaz.
// Herkes "Vampir", "Doktor" gibi alt rollere sahip olmalı.
public abstract class Role {

    protected String name;
    protected boolean isEvil; // Kötü mü? (Vampirler için true, Köylüler için false)

    // Constructor (Yapıcı Metot)
    public Role(String name, boolean isEvil) {
        this.name = name;
        this.isEvil = isEvil;
    }

    // Gece yapılacak yetenek. Her alt sınıf (Vampir, Doktor vs.) bunu kendine göre Override edecek (ezecek).
    public abstract void performNightAction();

    // Getter metotları
    public String getName() {
        return name;
    }

    public boolean isEvil() {
        return isEvil;
    }
}