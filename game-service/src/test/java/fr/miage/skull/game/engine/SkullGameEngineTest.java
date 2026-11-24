package fr.miage.skull.game.engine;

import fr.miage.skull.game.ai.AiAction;
import fr.miage.skull.game.ai.AiView;
import fr.miage.skull.game.ai.Disc;
import fr.miage.skull.game.ai.StandardAiStrategy;
import fr.miage.skull.game.exception.IllegalMoveException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static fr.miage.skull.game.engine.DiscType.FLOWER;
import static fr.miage.skull.game.engine.DiscType.SKULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkullGameEngineTest {

    private final SkullGameEngine engine = new SkullGameEngine();
    private final Random random = new Random(42);

    // --- outils ---

    private GameState newGame() {
        return engine.initGame(List.of(
                new GameEngine.PlayerSetup("1", "Alice", false),
                new GameEngine.PlayerSetup("2", "Bob", false),
                new GameEngine.PlayerSetup("3", "Chloé", false)));
    }

    private GameState play(GameState s, String id, Move move) {
        return engine.applyMove(s, id, move, random);
    }

    private static Move place(DiscType d) {
        return new Move(MoveType.PLACE_DISC, d, null, null);
    }

    private static Move bid(int amount) {
        return new Move(MoveType.BID, null, amount, null);
    }

    private static Move pass() {
        return new Move(MoveType.PASS, null, null, null);
    }

    private static Move reveal(String target) {
        return new Move(MoveType.REVEAL, null, null, target);
    }

    /** Chacun pose une fleur : la partie passe au tour de jeu, Alice commence. */
    private GameState afterInitialFlowers() {
        GameState s = newGame();
        s = play(s, "1", place(FLOWER));
        s = play(s, "2", place(FLOWER));
        return play(s, "3", place(FLOWER));
    }

    private static PlayerState player(String id, List<DiscType> hand, List<DiscType> stack, int lost, int points) {
        return new PlayerState(id, "J" + id, false, hand, stack, List.of(), lost, points, false, false);
    }

    private static GameState challengeState(String challenger, int bid, PlayerState... players) {
        return new GameState(GamePhase.CHALLENGE, List.of(players), challenger, challenger, bid, 0, null,
                1, challenger, List.of());
    }

    private static Random fixedIndex(int index) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return index;
            }
        };
    }

    // --- mise en place ---

    @Test
    void initGame_chaqueJoueurATroisFleursEtUnCrane() {
        GameState s = newGame();

        assertEquals(GamePhase.INITIAL_PLACEMENT, s.phase());
        assertEquals("1", s.currentPlayerId());
        assertEquals(1, s.roundNumber());
        for (PlayerState p : s.players()) {
            assertEquals(3, p.hand().stream().filter(d -> d == FLOWER).count());
            assertEquals(1, p.hand().stream().filter(d -> d == SKULL).count());
            assertTrue(p.stack().isEmpty());
        }
    }

    @Test
    void initGame_moinsDeTroisJoueurs_refuse() {
        List<GameEngine.PlayerSetup> two = List.of(
                new GameEngine.PlayerSetup("1", "A", false),
                new GameEngine.PlayerSetup("2", "B", false));

        assertThrows(IllegalMoveException.class, () -> engine.initGame(two));
    }

    // --- pose ---

    @Test
    void premierTour_enchereRefusee_laPoseEstObligatoire() {
        GameState s = newGame();

        assertThrows(IllegalMoveException.class, () -> play(s, "1", bid(1)));
    }

    @Test
    void poseInitiale_quandTousOntPose_leTourDeJeuCommenceParLePremierJoueur() {
        GameState s = newGame();
        s = play(s, "1", place(SKULL));
        assertEquals(GamePhase.INITIAL_PLACEMENT, s.phase());
        assertEquals("2", s.currentPlayerId());

        s = afterInitialFlowers();

        assertEquals(GamePhase.TURN_ACTION, s.phase());
        assertEquals("1", s.currentPlayerId());
        assertEquals(3, s.discsOnTable());
    }

    @Test
    void poser_retireLeDisqueDeLaMainEtLeMetSurLaPile() {
        GameState s = play(newGame(), "1", place(SKULL));

        PlayerState alice = s.player("1").orElseThrow();
        assertEquals(List.of(FLOWER, FLOWER, FLOWER), alice.hand());
        assertEquals(List.of(SKULL), alice.stack());
    }

    @Test
    void poserUnDisqueAbsentDeLaMain_refuse() {
        GameState s = afterInitialFlowers();
        GameState aliceSkull = play(play(play(s, "1", place(SKULL)), "2", place(FLOWER)), "3", place(FLOWER));

        assertThrows(IllegalMoveException.class, () -> play(aliceSkull, "1", place(SKULL)));
    }

    @Test
    void poserSansPreciserLeDisque_refuse() {
        assertThrows(IllegalMoveException.class, () -> play(newGame(), "1", place(null)));
    }

    @Test
    void sansDisqueEnMain_laPoseEstRefuseeEtLEnchereObligatoire() {
        GameState s = new GameState(GamePhase.TURN_ACTION, List.of(
                player("1", List.of(), List.of(FLOWER, FLOWER, FLOWER, SKULL), 0, 0),
                player("2", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0),
                player("3", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0)),
                "1", null, 0, 0, null, 1, "1", List.of());

        assertThrows(IllegalMoveException.class, () -> play(s, "1", place(FLOWER)));
        assertEquals(GamePhase.BIDDING, play(s, "1", bid(2)).phase());
    }

    // --- tour et enchères ---

    @Test
    void actionHorsTour_refusee() {
        GameState s = afterInitialFlowers();

        IllegalMoveException e = assertThrows(IllegalMoveException.class, () -> play(s, "2", place(FLOWER)));
        assertEquals("Ce n'est pas votre tour.", e.getMessage());
    }

    @Test
    void joueurInconnu_refuse() {
        assertThrows(IllegalMoveException.class, () -> play(newGame(), "99", place(FLOWER)));
    }

    @Test
    void retournerAvantLeDefi_refuse() {
        GameState s = afterInitialFlowers();

        assertThrows(IllegalMoveException.class, () -> play(s, "1", reveal("1")));
    }

    @Test
    void enchere_auDelaDuNombreDeDisquesSurLaTable_refusee() {
        GameState s = afterInitialFlowers();

        assertThrows(IllegalMoveException.class, () -> play(s, "1", bid(4)));
        assertThrows(IllegalMoveException.class, () -> play(s, "1", bid(0)));
    }

    @Test
    void enchere_lanceLesEncheresEtPasseAuJoueurSuivant() {
        GameState s = play(afterInitialFlowers(), "1", bid(1));

        assertEquals(GamePhase.BIDDING, s.phase());
        assertEquals(1, s.currentBid());
        assertEquals("1", s.challengerId());
        assertEquals("2", s.currentPlayerId());
    }

    @Test
    void enchereEgaleAuNombreDeDisques_lanceDirectementLeDefi() {
        GameState s = play(afterInitialFlowers(), "1", bid(3));

        assertEquals(GamePhase.CHALLENGE, s.phase());
        assertEquals("1", s.currentPlayerId());
        assertEquals("1", s.challengerId());
    }

    @Test
    void surenchereInferieureOuEgale_refusee() {
        GameState s = play(afterInitialFlowers(), "1", bid(2));

        assertThrows(IllegalMoveException.class, () -> play(s, "2", bid(2)));
        assertThrows(IllegalMoveException.class, () -> play(s, "2", bid(1)));
    }

    @Test
    void pendantLesEncheres_poserEstRefuse() {
        GameState s = play(afterInitialFlowers(), "1", bid(1));

        assertThrows(IllegalMoveException.class, () -> play(s, "2", place(FLOWER)));
    }

    @Test
    void lesJoueursRetiresSontSautes() {
        GameState s = play(afterInitialFlowers(), "1", bid(1));
        s = play(s, "2", pass());
        s = play(s, "3", bid(2));

        assertEquals("1", s.currentPlayerId());
        GameState afterAlice = play(s, "1", pass());
        assertEquals(GamePhase.CHALLENGE, afterAlice.phase());
        assertEquals("3", afterAlice.challengerId());
    }

    @Test
    void quandUnSeulJoueurResteDansLEnchere_ilTenteLeDefi() {
        GameState s = play(afterInitialFlowers(), "1", bid(1));
        s = play(s, "2", pass());
        s = play(s, "3", pass());

        assertEquals(GamePhase.CHALLENGE, s.phase());
        assertEquals("1", s.currentPlayerId());
        assertEquals(1, s.currentBid());
    }

    // --- défi ---

    @Test
    void defi_retournerUneAutrePileAvantLaSienne_refuse() {
        GameState s = challengeState("1", 2,
                player("1", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0),
                player("2", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0),
                player("3", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0));

        IllegalMoveException e = assertThrows(IllegalMoveException.class, () -> play(s, "1", reveal("2")));
        assertEquals("Vous devez d'abord retourner tous les disques de votre propre pile.", e.getMessage());
    }

    @Test
    void defi_onRetourneLeDessusDeLaPileUnDisqueALaFois() {
        GameState s = challengeState("1", 4,
                player("1", List.of(FLOWER, SKULL), List.of(FLOWER, FLOWER), 0, 0),
                player("2", List.of(FLOWER, FLOWER), List.of(SKULL, FLOWER), 0, 0),
                player("3", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0));

        s = play(s, "1", reveal("1"));
        s = play(s, "1", reveal("1"));
        s = play(s, "1", reveal("2"));

        PlayerState bob = s.player("2").orElseThrow();
        assertEquals(List.of(SKULL), bob.stack());
        assertEquals(List.of(FLOWER), bob.revealed());
        assertEquals(3, s.revealedFlowersCount());
        assertEquals(GamePhase.CHALLENGE, s.phase());
    }

    @Test
    void defi_pileVide_refuse() {
        GameState s = challengeState("1", 2,
                player("1", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0),
                player("2", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0),
                player("3", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0));
        GameState ownDone = play(s, "1", reveal("1"));

        assertThrows(IllegalMoveException.class, () -> play(ownDone, "1", reveal("1")));
        assertThrows(IllegalMoveException.class, () -> play(ownDone, "1", reveal(null)));
    }

    @Test
    void defiReussi_marqueUnPointEtLeChallengerOuvreLaManche() {
        GameState s = challengeState("2", 2,
                player("1", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0),
                player("2", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0),
                player("3", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0));

        s = play(s, "2", reveal("2"));
        s = play(s, "2", reveal("3"));

        assertEquals(1, s.player("2").orElseThrow().points());
        assertEquals(GamePhase.INITIAL_PLACEMENT, s.phase());
        assertEquals(2, s.roundNumber());
        assertEquals("2", s.currentPlayerId());
        assertEquals(0, s.currentBid());
        assertNull(s.challengerId());
        for (PlayerState p : s.players()) {
            assertEquals(4, p.hand().size());
            assertTrue(p.stack().isEmpty() && p.revealed().isEmpty());
        }
    }

    @Test
    void deuxiemeDefiReussi_gagneLaPartie() {
        GameState s = challengeState("1", 1,
                player("1", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 1),
                player("2", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0),
                player("3", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 1));

        GameState end = play(s, "1", reveal("1"));

        assertEquals(GamePhase.FINISHED, end.phase());
        assertEquals("1", end.winnerId());
        assertNull(end.currentPlayerId());
        assertThrows(IllegalMoveException.class, () -> play(end, "1", place(FLOWER)));
    }

    @Test
    void crane_defiRateEtUnDisqueTireAuHasardEstPerdu() {
        GameState s = challengeState("1", 2,
                player("1", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0),
                player("2", List.of(FLOWER, FLOWER, FLOWER), List.of(SKULL), 0, 0),
                player("3", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0));
        s = play(s, "1", reveal("1"));

        // disques d'Alice à ce moment : main [F, F, S] + retourné [F] ; l'indice 2 désigne son crâne
        GameState after = engine.applyMove(s, "1", reveal("2"), fixedIndex(2));

        PlayerState alice = after.player("1").orElseThrow();
        assertEquals(1, alice.lostDiscsCount());
        assertEquals(List.of(FLOWER, FLOWER, FLOWER), alice.hand());
        assertEquals(0, alice.points());
        assertEquals(GamePhase.INITIAL_PLACEMENT, after.phase());
        assertEquals("1", after.currentPlayerId());
        assertTrue(after.recentEvents().stream().anyMatch(e -> e.type() == GameEventType.SKULL_REVEALED));
    }

    @Test
    void lesDisquesDUnJoueurRetireRestentSurLaTableEtComptent() {
        GameState s = play(afterInitialFlowers(), "1", bid(1));
        s = play(s, "2", pass());
        s = play(s, "3", bid(3));

        assertEquals(GamePhase.CHALLENGE, s.phase());
        s = play(s, "3", reveal("3"));
        s = play(s, "3", reveal("2"));
        s = play(s, "3", reveal("1"));

        assertEquals(1, s.player("3").orElseThrow().points());
    }

    // --- élimination et fin ---

    @Test
    void dernierDisquePerdu_leJoueurEstEliminéEtLaPartieContinue() {
        GameState s = challengeState("1", 2,
                player("1", List.of(), List.of(FLOWER), 3, 0),
                player("2", List.of(FLOWER, FLOWER, FLOWER), List.of(SKULL), 0, 0),
                player("3", List.of(FLOWER, FLOWER, SKULL), List.of(FLOWER), 0, 0));
        s = play(s, "1", reveal("1"));
        GameState after = play(s, "1", reveal("2"));

        PlayerState alice = after.player("1").orElseThrow();
        assertTrue(alice.eliminated());
        assertEquals(0, alice.discCount());
        assertEquals(GamePhase.INITIAL_PLACEMENT, after.phase());
        assertEquals("2", after.currentPlayerId());
        assertThrows(IllegalMoveException.class, () -> play(after, "1", place(FLOWER)));
    }

    @Test
    void joueurEliminé_estSauteAuTourSuivant() {
        PlayerState out = new PlayerState("2", "J2", false, List.of(), List.of(), List.of(), 4, 0, false, true);
        GameState s = new GameState(GamePhase.INITIAL_PLACEMENT, List.of(
                player("1", List.of(FLOWER, FLOWER, FLOWER, SKULL), List.of(), 0, 0),
                out,
                player("3", List.of(FLOWER, FLOWER, FLOWER, SKULL), List.of(), 0, 0)),
                "1", null, 0, 0, null, 2, "1", List.of());

        s = play(s, "1", place(FLOWER));

        assertEquals("3", s.currentPlayerId());
    }

    @Test
    void dernierSurvivant_gagneLaPartie() {
        PlayerState out = new PlayerState("3", "J3", false, List.of(), List.of(), List.of(), 4, 0, false, true);
        GameState s = challengeState("1", 2,
                player("1", List.of(), List.of(FLOWER), 3, 1),
                player("2", List.of(FLOWER, FLOWER, FLOWER), List.of(SKULL), 0, 0),
                out);
        s = play(s, "1", reveal("1"));
        s = play(s, "1", reveal("2"));

        assertEquals(GamePhase.FINISHED, s.phase());
        assertEquals("2", s.winnerId());
    }

    @Test
    void lEtatRecuNEstJamaisModifie() {
        GameState before = afterInitialFlowers();
        String copy = before.toString();

        play(before, "1", place(SKULL));

        assertEquals(copy, before.toString());
    }

    @Test
    void leJournalGardeLesDerniersEvenements() {
        GameState s = afterInitialFlowers();

        GameEvent last = s.recentEvents().get(s.recentEvents().size() - 1);
        assertEquals(GameEventType.DISC_PLACED, last.type());
        assertEquals("3", last.playerId());
        assertTrue(s.recentEvents().size() <= SkullGameEngine.MAX_EVENTS);
    }

    // --- parties complètes jouées par l'IA du sujet ---

    @Test
    void partiesJoueesParDesIASeules_seTerminentToutes() {
        for (long seed = 0; seed < 300; seed++) {
            int players = 3 + (int) (seed % 4);
            GameState end = playAiGame(players, new Random(seed));

            assertEquals(GamePhase.FINISHED, end.phase(), "graine " + seed);
            assertFalse(end.player(end.winnerId()).orElseThrow().eliminated(), "graine " + seed);
        }
    }

    @Test
    void memeGraine_memePartie() {
        assertEquals(playAiGame(4, new Random(7)), playAiGame(4, new Random(7)));
    }

    private GameState playAiGame(int count, Random rnd) {
        List<GameEngine.PlayerSetup> setups = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            setups.add(new GameEngine.PlayerSetup(String.valueOf(i), "IA " + i, true));
        }
        GameState s = engine.initGame(setups);
        StandardAiStrategy ai = new StandardAiStrategy();
        for (int moves = 0; moves < 5000 && s.phase() != GamePhase.FINISHED; moves++) {
            String id = s.currentPlayerId();
            s = engine.applyMove(s, id, aiMove(s, s.player(id).orElseThrow(), ai, rnd), rnd);
        }
        return s;
    }

    /** Même traduction que GameService : pose initiale, défi, sinon décision de l'IA du sujet. */
    private static Move aiMove(GameState s, PlayerState me, StandardAiStrategy ai, Random rnd) {
        if (s.phase() == GamePhase.INITIAL_PLACEMENT) {
            return place(me.hand().get(rnd.nextInt(me.hand().size())));
        }
        if (s.phase() == GamePhase.CHALLENGE) {
            if (!me.stack().isEmpty()) {
                return reveal(me.playerId());
            }
            List<PlayerState> targets = s.players().stream()
                    .filter(p -> !p.playerId().equals(me.playerId()) && !p.stack().isEmpty())
                    .toList();
            return reveal(targets.get(rnd.nextInt(targets.size())).playerId());
        }
        int onTable = s.players().stream().mapToInt(p -> p.stack().size()).sum();
        AiView view = new AiView(toAi(me.hand()), toAi(me.stack()), s.currentBid(), onTable);
        AiAction action = ai.decide(view, rnd);
        return switch (action) {
            case AiAction.PlaceDisc p -> place(p.disc() == Disc.SKULL ? SKULL : FLOWER);
            case AiAction.Bid b -> bid(b.amount());
            case AiAction.Withdraw w -> pass();
        };
    }

    private static List<Disc> toAi(List<DiscType> discs) {
        return discs.stream().map(d -> d == SKULL ? Disc.SKULL : Disc.FLOWER).toList();
    }
}
