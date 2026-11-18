package fr.miage.skull.game.ai;

import java.util.List;
import java.util.Random;

/** L'IA décrite dans le sujet. */
public class StandardAiStrategy implements AiStrategy {

    static final double PLACE_PROBABILITY = 0.80;        // sinon, elle enchérit
    static final double RAISE_IF_ONLY_FLOWERS = 0.70;
    static final double RAISE_IF_SKULL = 0.20;

    @Override
    public String id() {
        return "standard";
    }

    @Override
    public AiAction decide(AiView view, Random random) {
        return view.biddingStarted() ? decideDuringBidding(view, random) : decideBeforeBidding(view, random);
    }

    /** Personne n'a encore enchéri : poser un disque (80 %) ou enchérir (20 %). */
    private AiAction decideBeforeBidding(AiView view, Random random) {
        boolean cannotPlace = view.hand().isEmpty();
        boolean nothingOnTable = view.ownStack().isEmpty();

        // elle doit enchérir si elle ne peut plus poser, et ne peut pas enchérir sans avoir posé
        if (cannotPlace) {
            return new AiAction.Bid(view.ownStack().size());
        }
        if (nothingOnTable || random.nextDouble() < PLACE_PROBABILITY) {
            return new AiAction.PlaceDisc(randomDisc(view.hand(), random));
        }
        // son enchère de départ est son nombre de tuiles posées
        return new AiAction.Bid(view.ownStack().size());
    }

    /** Une enchère est en cours : surenchérir de 1, ou se retirer. */
    private AiAction decideDuringBidding(AiView view, Random random) {
        int raise = view.currentBid() + 1;
        if (raise > view.totalDiscsOnTable()) {
            return new AiAction.Withdraw();   // impossible d'enchérir plus haut que le nombre de disques
        }
        double probability = view.ownStack().contains(Disc.SKULL) ? RAISE_IF_SKULL : RAISE_IF_ONLY_FLOWERS;
        return random.nextDouble() < probability ? new AiAction.Bid(raise) : new AiAction.Withdraw();
    }

    private Disc randomDisc(List<Disc> hand, Random random) {
        return hand.get(random.nextInt(hand.size()));
    }
}
