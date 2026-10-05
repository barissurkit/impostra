package com.impostra.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void newPlayerIsAliveAndHasNoRole() {
        Player p = new Player("ali");
        assertEquals("ali", p.getUsername());
        assertTrue(p.isAlive());
        assertNull(p.getRole());
    }

    @Test
    void killMarksPlayerAsDead() {
        Player p = new Player("ali");
        p.kill();
        assertFalse(p.isAlive());
    }

    @Test
    void resetForNewGameRestoresStateButKeepsUsername() {
        Player p = new Player("ali");
        p.assignRole(new RogueAI());
        p.kill();

        p.resetForNewGame();

        assertEquals("ali", p.getUsername());
        assertTrue(p.isAlive());
        assertNull(p.getRole());
    }

    @Test
    void rogueAiAndInsiderThreatAreEvilOthersAreNot() {
        assertTrue(new RogueAI().isEvil());
        assertTrue(new InsiderThreat().isEvil());
        assertFalse(new SystemUser().isEvil());
        assertFalse(new SecurityEngineer().isEvil());
        assertFalse(new CyberAnalyst().isEvil());
    }

    @Test
    void roleNamesMatchTheNamesUsedByTheServer() {
        assertEquals("Rogue AI", new RogueAI().getName());
        assertEquals("Kullanıcı", new SystemUser().getName());
    }
}
