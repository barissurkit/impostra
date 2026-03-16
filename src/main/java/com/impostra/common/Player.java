package com.impostra.common;

public class Player {
    private String username;
    private Role role;       // Oyuncunun rolü (Vampir, Köylü vs.)
    private boolean isAlive; // Yaşıyor mu?

    // Constructor: Oyuncu ilk oyuna girdiğinde sadece adı vardır, rolü sonradan dağıtılır ve başlarken hayattadır.
    public Player(String username) {
        this.username = username;
        this.isAlive = true;
        this.role = null; // Rol başlangıçta boş, oyun başlayınca GameManager dağıtacak.
    }

    // --- Oyuncunun Durumunu Değiştiren Metotlar ---

    // Oyuncuya rol atama (Oyun başlarken kullanılacak)
    public void assignRole(Role assignedRole) {
        this.role = assignedRole;
    }

    // Oyuncuyu öldürme (Vampir saldırdığında veya oylamada elendiğinde kullanılacak)
    public void kill() {
        this.isAlive = false;
        System.out.println(username + " öldü!");
    }

    // --- Bilgi Alma (Getter) Metotları ---

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAlive() {
        return isAlive;
    }
}