package fr.miage.skull.frontend.mock;

import java.util.List;
import java.util.Map;

/** Données simulées, en attendant le game-service et le ladder-service (lots suivants). */
public final class MockData {

    private MockData() {}

    public static List<Map<String, Object>> openGames() {
        return List.of(
                Map.of("name", "Partie de Bob", "players", 2, "max", 4, "ai", 1),
                Map.of("name", "Table des crânes", "players", 1, "max", 5, "ai", 2),
                Map.of("name", "Duel du soir", "players", 3, "max", 3, "ai", 0));
    }

    public static List<Map<String, Object>> history() {
        return List.of(
                Map.of("date", "20/10/2026", "result", "Victoire", "score", "2 - 1"),
                Map.of("date", "18/10/2026", "result", "Défaite", "score", "0 - 2"),
                Map.of("date", "15/10/2026", "result", "Victoire", "score", "2 - 0"));
    }

    public static Map<String, Object> stats() {
        return Map.of("games", 3, "wins", 2, "ratio", 67);
    }
}
