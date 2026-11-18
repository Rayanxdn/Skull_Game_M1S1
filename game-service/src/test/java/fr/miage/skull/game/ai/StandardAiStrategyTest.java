package fr.miage.skull.game.ai;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class StandardAiStrategyTest {

    private final StandardAiStrategy ai = new StandardAiStrategy();
    private static final int TRIALS = 10_000;

    private static final List<Disc> HAND = List.of(Disc.FLOWER, Disc.FLOWER, Disc.SKULL);

    /** Compte combien de fois l'IA choisit une action d'un type donné sur TRIALS tirages. */
    private double frequency(AiView view, Class<? extends AiAction> type) {
        Random random = new Random(42);   // graine fixe : le test est reproductible
        int count = 0;
        for (int i = 0; i < TRIALS; i++) {
            if (type.isInstance(ai.decide(view, random))) count++;
        }
        return (double) count / TRIALS;
    }

    @Test
    void beforeBidding_places_aboutEightyPercent() {
        AiView view = new AiView(HAND, List.of(Disc.FLOWER), 0, 5);
        assertEquals(0.80, frequency(view, AiAction.PlaceDisc.class), 0.02);
    }

    @Test
    void beforeBidding_bids_aboutTwentyPercent_withItsNumberOfTiles() {
        AiView view = new AiView(HAND, List.of(Disc.FLOWER, Disc.FLOWER), 0, 6);
        assertEquals(0.20, frequency(view, AiAction.Bid.class), 0.02);

        Random random = new Random(1);
        for (int i = 0; i < 200; i++) {
            if (ai.decide(view, random) instanceof AiAction.Bid bid) {
                assertEquals(2, bid.amount());   // elle a posé 2 disques
            }
        }
    }

    @Test
    void withNoDiscInHand_mustBid() {
        AiView view = new AiView(List.of(), List.of(Disc.FLOWER, Disc.SKULL), 0, 4);
        Random random = new Random(7);
        for (int i = 0; i < 100; i++) {
            assertInstanceOf(AiAction.Bid.class, ai.decide(view, random));
        }
    }

    @Test
    void withNothingOnTable_alwaysPlaces() {
        AiView view = new AiView(HAND, List.of(), 0, 3);
        assertEquals(1.0, frequency(view, AiAction.PlaceDisc.class));
    }

    @Test
    void duringBidding_withOnlyFlowers_raises_aboutSeventyPercent() {
        AiView view = new AiView(HAND, List.of(Disc.FLOWER, Disc.FLOWER), 2, 8);
        assertEquals(0.70, frequency(view, AiAction.Bid.class), 0.02);
    }

    @Test
    void duringBidding_withSkullOnTable_raises_aboutTwentyPercent() {
        AiView view = new AiView(HAND, List.of(Disc.FLOWER, Disc.SKULL), 2, 8);
        assertEquals(0.20, frequency(view, AiAction.Bid.class), 0.02);
    }

    @Test
    void duringBidding_raisesByExactlyOne() {
        AiView view = new AiView(HAND, List.of(Disc.FLOWER), 3, 8);
        Random random = new Random(3);
        for (int i = 0; i < 200; i++) {
            if (ai.decide(view, random) instanceof AiAction.Bid bid) {
                assertEquals(4, bid.amount());
            }
        }
    }

    @Test
    void duringBidding_cannotRaiseAboveDiscsOnTable_withdraws() {
        AiView view = new AiView(HAND, List.of(Disc.FLOWER), 5, 5);
        assertEquals(1.0, frequency(view, AiAction.Withdraw.class));
    }

    @Test
    void sameSeed_sameDecisions() {
        AiView view = new AiView(HAND, List.of(Disc.FLOWER), 0, 5);
        assertEquals(ai.decide(view, new Random(99)), ai.decide(view, new Random(99)));
    }
}
