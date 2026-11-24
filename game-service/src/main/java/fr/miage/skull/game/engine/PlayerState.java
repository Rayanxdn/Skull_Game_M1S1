package fr.miage.skull.game.engine;

import java.util.List;

public record PlayerState (
        String playerId,
        String displayName,
        boolean isAi,
        List<DiscType> hand,       // Disqeus en main
        List<DiscType> stack,      // Pile posée devant le joueur (le dernier élément = le sommet)
        int revealedCount,         // Nombre de disques déjà retournés pendant un défi
        int lostDiscsCount,        // Nombre de disques définitivement perdus
        int points,                // Défis réussis (victoire à 2)
        boolean passedInRound,     // S'est retiré des enchères en cours
        boolean eliminated         // N'a plus aucun disque (4 perdus)
){}
