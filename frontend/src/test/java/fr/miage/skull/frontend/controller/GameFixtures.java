package fr.miage.skull.frontend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Réponses du game-service telles que Jackson les lit (nombres entiers, valeurs nulles possibles). */
final class GameFixtures {

    static final long ALICE = 10L;

    private GameFixtures() {
    }

    static Map<String, Object> seat(int participantId, Integer playerId, String name, boolean ai, int order) {
        Map<String, Object> p = new HashMap<>();
        p.put("participantId", participantId);
        p.put("playerId", playerId);
        p.put("displayName", name);
        p.put("ai", ai);
        p.put("forfeited", false);
        p.put("seatOrder", order);
        p.put("flowerTheme", "CLASSIC");
        p.put("skullTheme", "MUERTOS");
        p.put("points", 0);
        p.put("eliminated", false);
        p.put("passed", false);
        p.put("handCount", 0);
        p.put("stackCount", 0);
        p.put("myHand", null);
        p.put("myStack", null);
        p.put("revealed", List.of());
        p.put("lostDiscsCount", 0);
        return p;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> players(Map<String, Object> game) {
        return (List<Map<String, Object>>) game.get("players");
    }

    /** Partie #5 créée par Alice (compte 10), avec une IA ; d'autres sièges peuvent être ajoutés. */
    @SafeVarargs
    static Map<String, Object> game(String status, Map<String, Object>... extraSeats) {
        List<Map<String, Object>> players = new ArrayList<>();
        players.add(seat(1, (int) ALICE, "Alice", false, 0));
        players.add(seat(2, null, "IA #1", true, 1));
        players.addAll(List.of(extraSeats));
        Map<String, Object> g = new HashMap<>();
        g.put("id", 5);
        g.put("status", status);
        g.put("maxPlayers", 4);
        g.put("aiCount", 1);
        g.put("creatorId", (int) ALICE);
        g.put("winnerId", null);
        g.put("createdAt", "2026-11-24T19:00:00Z");
        g.put("players", players);
        return g;
    }
}
