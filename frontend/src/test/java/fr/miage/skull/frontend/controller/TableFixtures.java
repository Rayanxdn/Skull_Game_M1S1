package fr.miage.skull.frontend.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.miage.skull.frontend.controller.GameFixtures.players;
import static fr.miage.skull.frontend.controller.GameFixtures.seat;

/**
 * Partie #5 en cours : Alice (siège 1, compte 10), IA #1 (siège 2), Bob (siège 3, compte 20).
 * Alice voit sa main ; les autres joueurs n'apparaissent qu'en nombres, comme dans la vraie réponse.
 */
final class TableFixtures {

    private TableFixtures() {
    }

    static Map<String, Object> table(String phase, int currentSeat, List<String> aliceHand) {
        Map<String, Object> g = GameFixtures.game("IN_PROGRESS", seat(3, 20, "Bob", false, 2));
        g.put("phase", phase);
        g.put("currentTurnPlayerId", currentSeat);
        g.put("currentBid", 0);
        g.put("highestBidderId", null);
        g.put("roundNumber", 1);
        g.put("revealedFlowersCount", 0);
        g.put("discsOnTable", 0);
        g.put("winnerParticipantId", null);
        g.put("recentEvents", new ArrayList<>());
        for (Map<String, Object> p : players(g)) {
            p.put("handCount", 4);
        }
        Map<String, Object> alice = players(g).get(0);
        alice.put("myHand", aliceHand);
        alice.put("myStack", List.of());
        alice.put("handCount", aliceHand.size());
        return g;
    }

    static Map<String, Object> seatOf(Map<String, Object> game, int participantId) {
        return players(game).stream()
                .filter(p -> Integer.valueOf(participantId).equals(p.get("participantId")))
                .findFirst()
                .orElseThrow();
    }

    static Map<String, Object> event(String type, Integer actor, String actorName, Integer target, String targetName,
                                     Integer value) {
        Map<String, Object> e = new HashMap<>();
        e.put("type", type);
        e.put("participantId", actor);
        e.put("actorName", actorName);
        e.put("targetParticipantId", target);
        e.put("targetName", targetName);
        e.put("value", value);
        return e;
    }

    @SuppressWarnings("unchecked")
    static void addEvent(Map<String, Object> game, Map<String, Object> event) {
        ((List<Map<String, Object>>) game.get("recentEvents")).add(event);
    }
}
