package com.impostra.common;

public class Player {
    private String  username;
    private Role    role;
    private boolean isAlive;

    // İç Tehdit mekaniği: bu oyuncu kilitlendi mi? (oy kullanamaz)
    private boolean isLocked       = false;

    // Güvenlik Mühendisi mekaniği: geçen gece korundu mu? (arka arkaya koruma engeli)
    private boolean lastProtected  = false;

    // Senkronize Düğüm mekaniği: eşleştiği partner
    private Player  syncPartner    = null;

    // Root Yöneticisi mekaniği: kurtarma hakkını kullandı mı?
    private boolean rootUsed       = false;

    // Uyuyan Bot mekaniği: hacklendi mi? (isEvil role'e geçiş için)
    private boolean isHacked       = false;

    public Player(String username) {
        this.username      = username;
        this.isAlive       = true;
        this.role          = null;
    }

    public void assignRole(Role assignedRole) {
        this.role = assignedRole;
    }

    public void kill() {
        this.isAlive = false;
        System.out.println(username + " sistemden silindi!");
    }

    // Her gece başında çağrılır — kilit sıfırlanır
    public void resetForNewRound() {
        // Kilit her sabah oylama sonrasında sıfırlanır (zaten GameManager.endVoting'de yapılıyor)
        // Burası gece başlamadan önce güvence için
        this.isLocked = false;
    }

    /**
     * Güvenlik Mühendisi her gece sonunda çağırır — bu gece korunan oyuncuyu işaretler.
     * Bir sonraki gece bu oyuncu tekrar korunamaz.
     */
    public void markProtectedThisRound() {
        this.lastProtected = true;
    }

    /** Her gece başlangıcında çağrılır — koruma geçmişi temizlenir */
    public void clearLastProtected() {
        this.lastProtected = false;
    }

    // Oyun tamamen sıfırlandığında
    public void resetForNewGame() {
        this.role          = null;
        this.isAlive       = true;
        this.isLocked      = false;
        this.lastProtected = false;
        this.syncPartner   = null;
        this.rootUsed      = false;
        this.isHacked      = false;
    }

    // --- Getter / Setter ---

    public String  getUsername()    { return username; }
    public Role    getRole()        { return role; }
    public boolean isAlive()        { return isAlive; }

    /** Root Yöneticisi tarafından canlandırma için */
    public void revive() {
        this.isAlive = true;
        System.out.println(username + " sisteme geri yüklendi!");
    }

    public boolean isLocked()                   { return isLocked; }
    public void    setLocked(boolean v)          { this.isLocked = v; }

    public boolean wasLastProtected()            { return lastProtected; }
    public void    setLastProtected(boolean v)   { this.lastProtected = v; }

    public Player  getSyncPartner()              { return syncPartner; }
    public void    setSyncPartner(Player p)      { this.syncPartner = p; }

    public boolean isRootUsed()                  { return rootUsed; }
    public void    setRootUsed(boolean v)        { this.rootUsed = v; }

    public boolean isHacked()                    { return isHacked; }
    public void    setHacked(boolean v)          { this.isHacked = v; }
}