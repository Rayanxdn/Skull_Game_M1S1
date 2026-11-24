package fr.miage.skull.game.dto;

import fr.miage.skull.game.engine.DiscType;
import java.util.List;

public record PlayerViewDto(
        Long participantId,
        Long playerId,
        String displayName,
        boolean ai,
        boolean forfeited,
        int seatOrder,
        String flowerTheme,
        String skullTheme,
        int points,
        boolean eliminated,
        boolean passed,
        int handCount,
        int stackCount,
        // Renseigné uniquement pour le joueur connecté (null pour les adversaires)
        List<DiscType> myHand,
        List<DiscType> myStack,
        // Public : disques de ce joueur déjà retournés pendant le défi en cours
        List<DiscType> revealed,
        int lostDiscsCount
) {}
