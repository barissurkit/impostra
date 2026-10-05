package com.impostra.server;

import static org.junit.jupiter.api.Assertions.*;

import com.impostra.common.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameManagerTest {

    private static Player player(String name, Role role) {
        Player p = new Player(name);
        p.assignRole(role);
        return p;
    }

    @Test
    void startsInLobbyAndAcceptsPlayers() {
        GameManager gm = new GameManager();
        assertEquals(GamePhase.LOBBY, gm.getCurrentPhase());

        gm.addPlayer(new Player("a"));
        gm.addPlayer(new Player("b"));

        assertEquals(2, gm.getPlayers().size());
    }

    @Test
    void startGameNeedsAtLeastTwoPlayers() {
        GameManager gm = new GameManager();
        gm.addPlayer(new Player("a"));

        gm.startGame();

        assertEquals(GamePhase.LOBBY, gm.getCurrentPhase());
    }

    @Test
    void startGameAssignsARoleToEveryPlayerAndMovesToNight() {
        GameManager gm = new GameManager();
        for (String n : List.of("a", "b", "c", "d")) gm.addPlayer(new Player(n));

        gm.startGame();

        assertEquals(GamePhase.NIGHT, gm.getCurrentPhase());
        for (Player p : gm.getPlayers()) assertNotNull(p.getRole());
        long evil = gm.getPlayers().stream().filter(p -> p.getRole().isEvil()).count();
        assertEquals(1, evil, "4 oyuncuda tek Rogue AI olmalı");
    }

    @Test
    void playersCannotJoinAfterGameStarted() {
        GameManager gm = new GameManager();
        gm.addPlayer(new Player("a"));
        gm.addPlayer(new Player("b"));
        gm.startGame();

        gm.addPlayer(new Player("late"));

        assertEquals(2, gm.getPlayers().size());
    }

    @Test
    void endNightKillsAiTargetWhenNotProtected() {
        GameManager gm = new GameManager();
        Player ai = player("ai", new RogueAI());
        Player victim = player("victim", new SystemUser());
        gm.addPlayer(ai);
        gm.addPlayer(victim);

        gm.setAITarget(victim);
        String[] result = gm.endNight();

        assertFalse(victim.isAlive());
        assertEquals("victim", result[1]);
        assertEquals(GamePhase.DAY_DISCUSSION, gm.getCurrentPhase());
    }

    @Test
    void endNightDoesNotKillWhenEngineerProtectsTarget() {
        GameManager gm = new GameManager();
        Player victim = player("victim", new SystemUser());
        gm.addPlayer(victim);

        gm.setAITarget(victim);
        gm.setEngineerTarget(victim);
        String[] result = gm.endNight();

        assertTrue(victim.isAlive());
        assertEquals("", result[1]);
    }

    @Test
    void endNightWithoutTargetKillsNobody() {
        GameManager gm = new GameManager();
        Player p = player("p", new SystemUser());
        gm.addPlayer(p);

        String[] result = gm.endNight();

        assertTrue(p.isAlive());
        assertEquals("", result[1]);
    }

    @Test
    void votingExecutesPlayerWithMostVotes() {
        GameManager gm = new GameManager();
        Player a = player("a", new SystemUser());
        Player b = player("b", new SystemUser());
        Player c = player("c", new RogueAI());
        gm.addPlayer(a);
        gm.addPlayer(b);
        gm.addPlayer(c);

        gm.startVoting();
        gm.castVote(a, c);
        gm.castVote(b, c);
        gm.castVote(c, a);
        String[] result = gm.endVoting();

        assertEquals("c", result[1]);
        assertFalse(c.isAlive());
        assertEquals(GamePhase.NIGHT, gm.getCurrentPhase());
    }

    @Test
    void votingTieExecutesNobody() {
        GameManager gm = new GameManager();
        Player a = player("a", new SystemUser());
        Player b = player("b", new SystemUser());
        gm.addPlayer(a);
        gm.addPlayer(b);

        gm.startVoting();
        gm.castVote(a, b);
        gm.castVote(b, a);
        String[] result = gm.endVoting();

        assertEquals("", result[1]);
        assertTrue(a.isAlive());
        assertTrue(b.isAlive());
    }

    @Test
    void deadPlayersCannotVoteAndCannotBeVoted() {
        GameManager gm = new GameManager();
        Player a = player("a", new SystemUser());
        Player dead = player("dead", new SystemUser());
        gm.addPlayer(a);
        gm.addPlayer(dead);
        dead.kill();

        gm.startVoting();
        gm.castVote(dead, a);
        gm.castVote(a, dead);
        String[] result = gm.endVoting();

        assertEquals("", result[1], "Geçersiz oylar sayılmamalı");
    }

    @Test
    void goodTeamWinsWhenNoEvilPlayerIsAlive() {
        GameManager gm = new GameManager();
        Player ai = player("ai", new RogueAI());
        gm.addPlayer(ai);
        gm.addPlayer(player("u1", new SystemUser()));
        gm.addPlayer(player("u2", new SystemUser()));
        ai.kill();

        String result = gm.checkWinCondition();

        assertNotNull(result);
        assertTrue(result.contains("İYİLER KAZANDI"));
    }

    @Test
    void evilTeamWinsWhenEvilCountReachesGoodCount() {
        GameManager gm = new GameManager();
        gm.addPlayer(player("ai", new RogueAI()));
        gm.addPlayer(player("u1", new SystemUser()));

        String result = gm.checkWinCondition();

        assertNotNull(result);
        assertTrue(result.contains("KÖTÜLER KAZANDI"));
    }

    @Test
    void gameContinuesWhileGoodOutnumberEvil() {
        GameManager gm = new GameManager();
        gm.addPlayer(player("ai", new RogueAI()));
        gm.addPlayer(player("u1", new SystemUser()));
        gm.addPlayer(player("u2", new SystemUser()));

        assertNull(gm.checkWinCondition());
    }

    @Test
    void resetReturnsToLobbyAndRevivesPlayers() {
        GameManager gm = new GameManager();
        Player p = player("p", new SystemUser());
        gm.addPlayer(p);
        p.kill();
        gm.setCurrentPhase(GamePhase.NIGHT);

        gm.reset();

        assertEquals(GamePhase.LOBBY, gm.getCurrentPhase());
        assertTrue(p.isAlive());
        assertNull(p.getRole());
    }

    @Test
    void aliveHelpersIgnoreDeadPlayers() {
        GameManager gm = new GameManager();
        Player a = player("a", new SystemUser());
        Player b = player("b", new SystemUser());
        gm.addPlayer(a);
        gm.addPlayer(b);
        b.kill();

        assertEquals(1, gm.getAliveCount());
        assertArrayEquals(new String[] {"a"}, gm.getAlivePlayerNames());
    }

    @Test
    void roleDescriptionFallsBackForUnknownRole() {
        assertEquals("Rolün hakkında bilgi bulunamadı.", GameManager.getRoleDescription("yok"));
        assertTrue(GameManager.getRoleDescription("Rogue AI").contains("sistemden sil"));
    }
}
