package fr.miage.skull.game.mapper;

import fr.miage.skull.game.dto.GameEventView;
import fr.miage.skull.game.dto.GameViewResponse;
import fr.miage.skull.game.dto.PlayerViewDto;
import fr.miage.skull.game.engine.GameEvent;
import fr.miage.skull.game.engine.GameEventType;
import fr.miage.skull.game.engine.GamePhase;
import fr.miage.skull.game.engine.GameState;
import fr.miage.skull.game.engine.PlayerState;
import fr.miage.skull.game.entity.Game;
import fr.miage.skull.game.entity.GameParticipant;
import fr.miage.skull.game.entity.GameStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static fr.miage.skull.game.engine.DiscType.FLOWER;
import static fr.miage.skull.game.engine.DiscType.SKULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameMapperViewTest {

    private static final long ALICE = 10L;

    private final GameMapper mapper = new GameMapper();

    /** Défi en cours : Alice a retourné sa fleur, les piles de l'IA et de Bob sont encore cachées. */
    private Game gameDuringChallenge() {
        Game game = Game.builder().id(1L).status(GameStatus.IN_PROGRESS).maxPlayers(3).creatorId(ALICE).build();
        game.addParticipant(GameParticipant.builder().id(1L).playerId(ALICE).displayName("Alice").build());
        game.addParticipant(GameParticipant.builder().id(2L).displayName("IA #1").ai(true).build());
        game.addParticipant(GameParticipant.builder().id(3L).playerId(20L).displayName("Bob").build());

        GameState state = new GameState(GamePhase.CHALLENGE, List.of(
                new PlayerState("1", "Alice", false, List.of(FLOWER, FLOWER, SKULL), List.of(), List.of(FLOWER), 0, 0, false, false),
                new PlayerState("2", "IA #1", true, List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), List.of(), 1, 0, true, false),
                new PlayerState("3", "Bob", false, List.of(FLOWER, FLOWER, FLOWER), List.of(SKULL), List.of(), 0, 1, false, false)),
                "1", "1", 2, 1, null, 3, "1", List.of(
                        new GameEvent(GameEventType.BID, "1", null, 2),
                        new GameEvent(GameEventType.FLOWER_REVEALED, "1", "1", 1)));
        game.setStateJson(mapper.serializeState(state));
        return game;
    }

    @Test
    void laVueContientLesInformationsPubliquesDuDefi() {
        GameViewResponse view = mapper.toFilteredView(gameDuringChallenge(), ALICE);

        assertEquals(3, view.roundNumber());
        assertEquals(1, view.revealedFlowersCount());
        assertEquals(2, view.currentBid());
        assertEquals(1L, view.highestBidderId());
        assertEquals(3, view.discsOnTable());
        assertNull(view.winnerParticipantId());

        PlayerViewDto alice = view.players().get(0);
        assertEquals(List.of(FLOWER), alice.revealed());
        assertEquals(List.of(), alice.myStack());

        PlayerViewDto ia = view.players().get(1);
        assertEquals(1, ia.lostDiscsCount());
        assertEquals(1, ia.stackCount());
        assertEquals(List.of(), ia.revealed());
    }

    @Test
    void lesDisquesCachesDesAutresNeSontJamaisDansLaVue() {
        GameViewResponse view = mapper.toFilteredView(gameDuringChallenge(), ALICE);

        for (PlayerViewDto other : view.players().subList(1, 3)) {
            assertNull(other.myHand());
            assertNull(other.myStack());
        }
        assertEquals(List.of(FLOWER, FLOWER, SKULL), view.players().get(0).myHand());
    }

    @Test
    void unSpectateurNeVoitAucuneMainNiAucunePile() {
        GameViewResponse view = mapper.toFilteredView(gameDuringChallenge(), 99L);

        for (PlayerViewDto p : view.players()) {
            assertNull(p.myHand());
            assertNull(p.myStack());
        }
    }

    @Test
    void leJournalEstTraduitAvecLesNomsDesJoueurs() {
        List<GameEventView> events = mapper.toFilteredView(gameDuringChallenge(), ALICE).recentEvents();

        assertEquals(2, events.size());
        assertEquals(new GameEventView("BID", 1L, "Alice", null, null, 2), events.get(0));
        assertEquals(new GameEventView("FLOWER_REVEALED", 1L, "Alice", 1L, "Alice", 1), events.get(1));
    }

    @Test
    void partieNonLancee_vueSansEtatDuMoteur() {
        Game game = Game.builder().id(2L).status(GameStatus.OPEN).maxPlayers(4).creatorId(ALICE).build();
        game.addParticipant(GameParticipant.builder().id(5L).playerId(ALICE).displayName("Alice").build());

        GameViewResponse view = mapper.toFilteredView(game, ALICE);

        assertNull(view.phase());
        assertEquals(0, view.roundNumber());
        assertEquals(List.of(), view.recentEvents());
        assertEquals(List.of(), view.players().get(0).revealed());
    }
}
