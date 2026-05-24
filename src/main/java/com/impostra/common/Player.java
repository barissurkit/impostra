package com.impostra.common;

public class Player {
    private String  username;
    private Role    role;
    private boolean isAlive;

    public Player(String username) {
        this.username = username;
        this.isAlive  = true;
        this.role     = null;
    }

    public void assignRole(Role assignedRole) {
        this.role = assignedRole;
    }

    public void kill() {
        this.isAlive = false;
        System.out.println(username + " öldü!");
    }

    /**
     * Yeni oyun için oyuncuyu sıfırlar.
     * Kullanıcı adı korunur (bağlantı kesilmedi), rol ve hayat durumu sıfırlanır.
     */
    public void resetForNewGame() {
        this.role    = null;
        this.isAlive = true;
    }

    public String  getUsername() { return username; }
    public Role    getRole()     { return role; }
    public boolean isAlive()     { return isAlive; }
}