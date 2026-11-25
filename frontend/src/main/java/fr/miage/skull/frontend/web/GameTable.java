package fr.miage.skull.frontend.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.miage.skull.frontend.web.GameViews.asInt;
import static fr.miage.skull.frontend.web.GameViews.asLong;

/**
 * Prépare tout ce que la page de la table affiche, à partir de la vue renvoyée par le game-service.
 * Aucune règle n'est décidée ici : on montre seulement les actions utiles, le game-service vérifie chaque coup (R15).
 * La vue ne contient jamais les disques cachés des autres joueurs (R16) : on n'affiche que des nombres.
 */
public final class GameTable {

    public static final Map<String, String> PHASES = Map.of(
            "INITIAL_PLACEMENT", "Pose des disques",
            "TURN_ACTION", "Poser ou enchérir",
            "BIDDING", "Enchères",
            "CHALLENGE", "Défi",
            "FINISHED", "Partie terminée");

    private static final int JOURNAL_SIZE = 30;

    private GameTable() {
    }

    public static Map<String, Object> build(Map<String, Object> game, Long me) {
        Map<String, Object> mySeat = GameViews.seatOf(game, me);
        Long mySeatId = mySeat == null ? null : asLong(mySeat.get("participantId"));
        Long current = asLong(game.get("currentTurnPlayerId"));
        boolean inProgress = "IN_PROGRESS".equals(game.get("status"));
        boolean finished = "FINISHED".equals(game.get("status"));
        boolean myTurn = inProgress && mySeatId != null && mySeatId.equals(current);
        Long winnerSeat = asLong(game.get("winnerParticipantId"));

        Map<String, Object> actions = myTurn ? actions(game, mySeat) : Map.of();
        Map<String, Object> t = new HashMap<>();
        List<Map<String, Object>> seats = seats(game, mySeatId, current, GameViews.tookPart(game, me) && !finished, me,
                revealIds(actions));
        t.put("seats", seats);
        t.put("reportable", seats.stream().filter(s -> Boolean.TRUE.equals(s.get("canReport"))).toList());
        t.put("phaseLabel", PHASES.getOrDefault(String.valueOf(game.get("phase")), ""));
        t.put("round", asInt(game.get("roundNumber")));
        t.put("bid", asInt(game.get("currentBid")));
        t.put("onTable", asInt(game.get("discsOnTable")));
        t.put("flowersFound", asInt(game.get("revealedFlowersCount")));
        t.put("challenge", inProgress && "CHALLENGE".equals(game.get("phase")));
        t.put("leftGame", inProgress && mySeat == null && GameViews.tookPart(game, me));
        t.put("inProgress", inProgress);
        t.put("finished", finished);
        t.put("myTurn", myTurn);
        t.put("isPlayer", mySeat != null);
        t.put("meOut", mySeat != null && Boolean.TRUE.equals(mySeat.get("eliminated")));
        t.put("currentName", nameOf(game, current));
        t.put("winnerName", nameOf(game, winnerSeat));
        t.put("iWon", finished && mySeatId != null && mySeatId.equals(winnerSeat));
        t.put("winReason", finished ? winReason(game, winnerSeat) : "");
        t.put("hand", hand(mySeat));
        t.put("actions", actions);
        t.put("journal", journal(game));
        t.put("animEvents", animEvents(game));
        return t;
    }

    /** Sièges dans l'ordre du jeu, le joueur connecté en bas, puis dans le sens des aiguilles d'une montre. */
    static List<Map<String, Object>> seats(Map<String, Object> game, Long mySeatId, Long current,
                                           boolean canReport, Long me, List<Object> revealIds) {
        List<Map<String, Object>> players = new ArrayList<>(GameViews.players(game));
        players.sort((a, b) -> Integer.compare(asInt(a.get("seatOrder")), asInt(b.get("seatOrder"))));
        int start = 0;
        for (int i = 0; i < players.size(); i++) {
            if (mySeatId != null && mySeatId.equals(asLong(players.get(i).get("participantId")))) {
                start = i;
            }
        }
        Collections.rotate(players, -start);

        List<Map<String, Object>> seats = new ArrayList<>();
        int n = players.size();
        for (int i = 0; i < n; i++) {
            Map<String, Object> p = players.get(i);
            Long id = asLong(p.get("participantId"));
            Long account = asLong(p.get("playerId"));
            boolean ai = Boolean.TRUE.equals(p.get("ai"));
            double angle = Math.PI / 2 + 2 * Math.PI * i / n;

            Map<String, Object> s = new HashMap<>();
            s.put("id", id);
            s.put("name", String.valueOf(p.get("displayName")));
            s.put("initial", initial(String.valueOf(p.get("displayName"))));
            s.put("revealable", revealIds.contains(id));
            s.put("peek", id != null && id.equals(mySeatId) ? peek(p.get("myStack")) : "");
            s.put("ai", ai);
            s.put("forfeited", Boolean.TRUE.equals(p.get("forfeited")));
            s.put("me", id != null && id.equals(mySeatId));
            s.put("current", id != null && id.equals(current));
            s.put("eliminated", Boolean.TRUE.equals(p.get("eliminated")));
            s.put("passed", Boolean.TRUE.equals(p.get("passed")));
            s.put("points", asInt(p.get("points")));
            s.put("stackCount", asInt(p.get("stackCount")));
            s.put("handCount", asInt(p.get("handCount")));
            s.put("lost", asInt(p.get("lostDiscsCount")));
            s.put("revealed", p.get("revealed") instanceof List<?> r ? r : List.of());
            s.put("canReport", canReport && !ai && account != null && !account.equals(me));
            s.put("left", Math.round(50 + 38 * Math.cos(angle)));
            s.put("top", Math.round(50 + 38 * Math.sin(angle)));
            s.put("pileLeft", Math.round(50 + 17 * Math.cos(angle)));
            s.put("pileTop", Math.round(50 + 17 * Math.sin(angle)));
            seats.add(s);
        }
        return seats;
    }

    /** Sièges qu'on peut retourner maintenant (défi, à mon tour) : la pile devient cliquable. */
    private static List<Object> revealIds(Map<String, Object> actions) {
        List<Object> ids = new ArrayList<>();
        if (actions.get("reveal") instanceof List<?> targets) {
            for (Object target : targets) {
                if (target instanceof Map<?, ?> m) {
                    ids.add(m.get("id"));
                }
            }
        }
        return ids;
    }

    /** Ma propre pile, du bas vers le haut (F = fleur, C = crâne) : visible seulement par moi. */
    private static String peek(Object myStack) {
        List<String> letters = new ArrayList<>();
        if (myStack instanceof List<?> discs) {
            for (Object d : discs) {
                letters.add("SKULL".equals(d) ? "C" : "F");
            }
        }
        return String.join("·", letters);
    }

    private static final Map<String, String> ANIM_CODES = Map.of(
            "DISC_PLACED", "P", "BID", "B", "PASS", "X", "FLOWER_REVEALED", "F", "SKULL_REVEALED", "S",
            "CHALLENGE_WON", "W", "DISC_LOST", "L", "PLAYER_ELIMINATED", "E", "NEW_ROUND", "R", "GAME_WON", "G");

    /**
     * Les événements dans l'ordre, sous forme courte pour rejouer les coups à l'écran
     * (code, siège qui agit, siège visé, valeur). Uniquement des informations publiques :
     * un disque posé n'est jamais décrit, seul un disque retourné l'est.
     */
    static List<Map<String, Object>> animEvents(Map<String, Object> game) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (game.get("recentEvents") instanceof List<?> events) {
            for (Object e : events) {
                if (e instanceof Map<?, ?> event) {
                    Map<String, Object> a = new HashMap<>();
                    a.put("t", ANIM_CODES.getOrDefault(String.valueOf(event.get("type")), "O"));
                    a.put("p", asLong(event.get("participantId")));
                    a.put("q", asLong(event.get("targetParticipantId")));
                    a.put("v", asInt(event.get("value")));
                    list.add(a);
                }
            }
        }
        return list;
    }

    /** Comment la partie a été gagnée : 2 défis réussis, ou dernier joueur encore en jeu. */
    private static String winReason(Map<String, Object> game, Long winnerSeat) {
        return GameViews.players(game).stream()
                .filter(p -> winnerSeat != null && winnerSeat.equals(asLong(p.get("participantId"))))
                .findFirst()
                .map(p -> asInt(p.get("points")) >= 2 ? "2 défis réussis" : "dernier joueur encore en jeu (les autres sont éliminés)")
                .orElse("");
    }

    private static String initial(String name) {
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
    }

    static Map<String, Integer> hand(Map<String, Object> mySeat) {
        int flowers = 0;
        int skulls = 0;
        if (mySeat != null && mySeat.get("myHand") instanceof List<?> discs) {
            for (Object d : discs) {
                if ("FLOWER".equals(d)) {
                    flowers++;
                } else if ("SKULL".equals(d)) {
                    skulls++;
                }
            }
        }
        return Map.of("flowers", flowers, "skulls", skulls);
    }

    /** Les boutons à montrer pour la phase en cours, quand c'est le tour du joueur connecté. */
    static Map<String, Object> actions(Map<String, Object> game, Map<String, Object> mySeat) {
        String phase = String.valueOf(game.get("phase"));
        Map<String, Integer> hand = hand(mySeat);
        boolean hasDisc = hand.get("flowers") + hand.get("skulls") > 0;
        int onTable = asInt(game.get("discsOnTable"));
        int bid = asInt(game.get("currentBid"));

        Map<String, Object> a = new HashMap<>();
        boolean place = "INITIAL_PLACEMENT".equals(phase) || ("TURN_ACTION".equals(phase) && hasDisc);
        a.put("placeFlower", place && hand.get("flowers") > 0);
        a.put("placeSkull", place && hand.get("skulls") > 0);
        boolean canBid = ("TURN_ACTION".equals(phase) || "BIDDING".equals(phase)) && bid + 1 <= onTable;
        a.put("bid", canBid);
        a.put("bidMin", bid + 1);
        a.put("bidMax", onTable);
        a.put("pass", "BIDDING".equals(phase));
        a.put("reveal", "CHALLENGE".equals(phase) ? revealTargets(game, mySeat) : List.of());
        return a;
    }

    /** Pendant le défi : d'abord sa propre pile en entier, ensuite les piles des autres (règle du sujet). */
    static List<Map<String, Object>> revealTargets(Map<String, Object> game, Map<String, Object> mySeat) {
        Long mine = asLong(mySeat.get("participantId"));
        if (asInt(mySeat.get("stackCount")) > 0) {
            return List.of(Map.of("id", mine, "name", "ma pile", "count", asInt(mySeat.get("stackCount"))));
        }
        List<Map<String, Object>> targets = new ArrayList<>();
        for (Map<String, Object> p : GameViews.players(game)) {
            Long id = asLong(p.get("participantId"));
            int count = asInt(p.get("stackCount"));
            if (id != null && !id.equals(mine) && count > 0) {
                targets.add(Map.of("id", id, "name", String.valueOf(p.get("displayName")), "count", count));
            }
        }
        return targets;
    }

    /** Les derniers événements, du plus récent au plus ancien, en phrases. */
    static List<String> journal(Map<String, Object> game) {
        List<String> lines = new ArrayList<>();
        if (game.get("recentEvents") instanceof List<?> events) {
            for (Object e : events) {
                if (e instanceof Map<?, ?> event) {
                    lines.add(describe(event));
                }
            }
        }
        Collections.reverse(lines);
        return lines.size() > JOURNAL_SIZE ? lines.subList(0, JOURNAL_SIZE) : lines;
    }

    static String describe(Map<?, ?> e) {
        String actor = String.valueOf(e.get("actorName"));
        Object targetId = e.get("targetParticipantId");
        boolean ownPile = targetId != null && targetId.equals(e.get("participantId"));
        String target = ownPile ? "sa propre pile" : "la pile de " + e.get("targetName");
        int value = asInt(e.get("value"));
        return switch (String.valueOf(e.get("type"))) {
            case "DISC_PLACED" -> actor + " pose un disque.";
            case "BID" -> actor + " annonce " + value + (value > 1 ? " fleurs." : " fleur.");
            case "PASS" -> actor + " se retire des enchères.";
            case "FLOWER_REVEALED" -> actor + " retourne une fleur dans " + target + " (" + value + " trouvée"
                    + (value > 1 ? "s)." : ").");
            case "SKULL_REVEALED" -> actor + " tombe sur un crâne dans " + target + " !";
            case "CHALLENGE_WON" -> actor + " réussit son défi (" + value + (value > 1 ? " points)." : " point).");
            case "DISC_LOST" -> actor + " perd un disque.";
            case "PLAYER_ELIMINATED" -> actor + " est éliminé.";
            case "NEW_ROUND" -> "Manche " + value + ".";
            case "GAME_WON" -> actor + " remporte la partie !";
            default -> "";
        };
    }

    private static String nameOf(Map<String, Object> game, Long seat) {
        if (seat == null) {
            return "";
        }
        return GameViews.players(game).stream()
                .filter(p -> seat.equals(asLong(p.get("participantId"))))
                .map(p -> String.valueOf(p.get("displayName")))
                .findFirst()
                .orElse("");
    }
}
