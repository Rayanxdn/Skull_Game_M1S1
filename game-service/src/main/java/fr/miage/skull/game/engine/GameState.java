package fr.miage.skull.game.engine;

import java.util.List;

public record GameState (
        GamePhase phase,
        List<PlayerState> players,
        String currentPlayerId,
        String challengerId,       // Joueur qui a remporté l'enchère et retourne les disques
        int currentBid,
        int revealedFlowersCount,  // Fleurs trouvées pendant le défi en cours
        String winnerId
) {}
