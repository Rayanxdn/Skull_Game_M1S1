package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.web.GameTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static fr.miage.skull.frontend.controller.GameFixtures.ALICE;
import static fr.miage.skull.frontend.controller.TableFixtures.addEvent;
import static fr.miage.skull.frontend.controller.TableFixtures.event;
import static fr.miage.skull.frontend.controller.TableFixtures.seatOf;
import static fr.miage.skull.frontend.controller.TableFixtures.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ce que la page de table montre selon la phase : seulement des boutons utiles, aucune règle décidée ici. */
class GameTableTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> actions(Map<String, Object> view) {
        return (Map<String, Object>) view.get("actions");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> seats(Map<String, Object> view) {
        return (List<Map<String, Object>>) view.get("seats");
    }

    @Test
    void sieges_moiEnBas_puisDansLOrdreDuJeu() {
        Map<String, Object> view = GameTable.build(table("TURN_ACTION", 3, List.of("FLOWER")), ALICE);

        List<Map<String, Object>> seats = seats(view);
        assertEquals(List.of("Alice", "IA #1", "Bob"), seats.stream().map(s -> s.get("name")).toList());
        assertEquals(true, seats.get(0).get("me"));
        assertEquals(50L, seats.get(0).get("left"));
        assertEquals(88L, seats.get(0).get("top"));
        assertEquals(true, seats.get(2).get("current"));
        assertEquals("Bob", view.get("currentName"));
    }

    @Test
    void poseInitiale_seulementLesDisquesQueJAi() {
        Map<String, Object> view = GameTable.build(table("INITIAL_PLACEMENT", 1, List.of("FLOWER", "FLOWER")), ALICE);

        assertEquals(true, view.get("myTurn"));
        assertEquals(true, actions(view).get("placeFlower"));
        assertEquals(false, actions(view).get("placeSkull"));
        assertEquals(false, actions(view).get("bid"));
        assertEquals(false, actions(view).get("pass"));
        assertEquals(Map.of("flowers", 2, "skulls", 0), view.get("hand"));
    }

    @Test
    void tourAction_mainVide_seulementLEnchere() {
        Map<String, Object> game = table("TURN_ACTION", 1, List.of());
        game.put("discsOnTable", 4);

        Map<String, Object> a = actions(GameTable.build(game, ALICE));
        assertEquals(false, a.get("placeFlower"));
        assertEquals(false, a.get("placeSkull"));
        assertEquals(true, a.get("bid"));
        assertEquals(1, a.get("bidMin"));
        assertEquals(4, a.get("bidMax"));
    }

    @Test
    void encheres_surencherirOuSeRetirer() {
        Map<String, Object> game = table("BIDDING", 1, List.of("FLOWER", "SKULL"));
        game.put("currentBid", 2);
        game.put("discsOnTable", 5);

        Map<String, Object> a = actions(GameTable.build(game, ALICE));
        assertEquals(false, a.get("placeFlower"));
        assertEquals(true, a.get("bid"));
        assertEquals(3, a.get("bidMin"));
        assertEquals(5, a.get("bidMax"));
        assertEquals(true, a.get("pass"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void defi_dAbordSaProprePile() {
        Map<String, Object> game = table("CHALLENGE", 1, List.of("FLOWER"));
        seatOf(game, 1).put("stackCount", 2);
        seatOf(game, 3).put("stackCount", 1);

        List<Map<String, Object>> targets = (List<Map<String, Object>>) actions(GameTable.build(game, ALICE)).get("reveal");
        assertEquals(1, targets.size());
        assertEquals(1L, targets.get(0).get("id"));
        assertEquals("ma pile", targets.get(0).get("name"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void defi_ensuiteLesPilesNonVidesDesAutres() {
        Map<String, Object> game = table("CHALLENGE", 1, List.of("FLOWER"));
        seatOf(game, 1).put("stackCount", 0);
        seatOf(game, 2).put("stackCount", 0);
        seatOf(game, 3).put("stackCount", 2);

        List<Map<String, Object>> targets = (List<Map<String, Object>>) actions(GameTable.build(game, ALICE)).get("reveal");
        assertEquals(List.of(3L), targets.stream().map(t -> t.get("id")).toList());
    }

    @Test
    void pasMonTour_aucunBouton() {
        Map<String, Object> view = GameTable.build(table("TURN_ACTION", 3, List.of("FLOWER")), ALICE);

        assertEquals(false, view.get("myTurn"));
        assertTrue(actions(view).isEmpty());
    }

    @Test
    void signaler_seulementLesAutresJoueursHumains() {
        List<Map<String, Object>> seats = seats(GameTable.build(table("TURN_ACTION", 3, List.of()), ALICE));

        assertEquals(false, seats.get(0).get("canReport"));
        assertEquals(false, seats.get(1).get("canReport"));
        assertEquals(true, seats.get(2).get("canReport"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void journal_enPhrases_duPlusRecentAuPlusAncien() {
        Map<String, Object> game = table("CHALLENGE", 1, List.of());
        addEvent(game, event("BID", 3, "Bob", null, null, 2));
        addEvent(game, event("FLOWER_REVEALED", 3, "Bob", 3, "Bob", 1));
        addEvent(game, event("SKULL_REVEALED", 3, "Bob", 1, "Alice", null));

        List<String> journal = (List<String>) GameTable.build(game, ALICE).get("journal");
        assertEquals(List.of(
                "Bob tombe sur un crâne dans la pile de Alice !",
                "Bob retourne une fleur dans sa propre pile (1 trouvée).",
                "Bob annonce 2 fleurs."), journal);
    }

    @Test
    void finDePartie_vainqueurEtPlusDeBoutons() {
        Map<String, Object> game = table("FINISHED", 1, List.of("FLOWER"));
        game.put("status", "FINISHED");
        game.put("currentTurnPlayerId", null);
        game.put("winnerParticipantId", 3);

        Map<String, Object> view = GameTable.build(game, ALICE);
        assertEquals(true, view.get("finished"));
        assertEquals("Bob", view.get("winnerName"));
        assertEquals(false, view.get("iWon"));
        assertEquals(false, view.get("myTurn"));
        assertFalse(seats(view).stream().anyMatch(s -> Boolean.TRUE.equals(s.get("canReport"))));
    }
}
