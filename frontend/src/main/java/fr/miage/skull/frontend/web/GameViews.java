package fr.miage.skull.frontend.web;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Petites lectures dans la réponse du game-service (une Map JSON), communes aux pages du jeu.
 * Rappel : un siège (participantId) n'est pas un compte (playerId).
 */
public final class GameViews {

    public static final Map<String, String> THEME_LABELS = Map.of(
            "CLASSIC", "Classique",
            "MUERTOS", "Día de los Muertos",
            "CELTIQUE", "Celtique");

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Europe/Paris"));

    private GameViews() {
    }

    public static Long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }

    public static int asInt(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> players(Map<String, Object> game) {
        return game.get("players") instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    /** Le siège du joueur connecté, s'il joue encore lui-même (pas remplacé par une IA). */
    public static Map<String, Object> seatOf(Map<String, Object> game, Long playerId) {
        return players(game).stream()
                .filter(p -> !Boolean.TRUE.equals(p.get("ai")) && playerId != null && playerId.equals(asLong(p.get("playerId"))))
                .findFirst()
                .orElse(null);
    }

    /** Vrai si ce compte a occupé un siège de la partie, même s'il l'a quittée en cours de route. */
    public static boolean tookPart(Map<String, Object> game, Long playerId) {
        return players(game).stream().anyMatch(p -> playerId != null && playerId.equals(asLong(p.get("playerId"))));
    }

    public static Map<String, Object> seatOfAccount(Map<String, Object> game, Long playerId) {
        return players(game).stream()
                .filter(p -> playerId != null && playerId.equals(asLong(p.get("playerId"))))
                .findFirst()
                .orElse(null);
    }

    public static String creatorName(Map<String, Object> game) {
        Long creator = asLong(game.get("creatorId"));
        return players(game).stream()
                .filter(p -> creator != null && creator.equals(asLong(p.get("playerId"))))
                .map(p -> String.valueOf(p.get("displayName")))
                .findFirst()
                .orElse("?");
    }

    public static String date(Object isoInstant) {
        try {
            return DATE.format(Instant.parse(String.valueOf(isoInstant)));
        } catch (Exception e) {
            return "";
        }
    }
}
