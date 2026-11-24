package fr.miage.skull.game.engine;

import java.util.List;

/**
 * stack : disques encore face cachée (le dernier élément est le dessus de la pile).
 * revealed : disques de ce joueur retournés pendant le défi en cours, visibles de tous.
 */
public record PlayerState(
        String playerId,
        String displayName,
        boolean isAi,
        List<DiscType> hand,
        List<DiscType> stack,
        List<DiscType> revealed,
        int lostDiscsCount,
        int points,
        boolean passedInRound,
        boolean eliminated
) {
    public PlayerState {
        hand = hand == null ? List.of() : List.copyOf(hand);
        stack = stack == null ? List.of() : List.copyOf(stack);
        revealed = revealed == null ? List.of() : List.copyOf(revealed);
    }

    public int discCount() {
        return hand.size() + stack.size() + revealed.size();
    }

    public PlayerState controlledByAi(String newDisplayName) {
        return new PlayerState(playerId, newDisplayName, true, hand, stack, revealed,
                lostDiscsCount, points, passedInRound, eliminated);
    }
}
