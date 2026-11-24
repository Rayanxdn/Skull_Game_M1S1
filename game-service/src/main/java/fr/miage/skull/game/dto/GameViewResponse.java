package fr.miage.skull.game.dto;

import fr.miage.skull.game.engine.GamePhase;
import fr.miage.skull.game.entity.GameStatus;
import java.time.Instant;
import java.util.List;

/**
 * Vue d'une partie pour un joueur. currentTurnPlayerId, highestBidderId et winnerParticipantId
 * sont des participantId (sièges) ; creatorId et winnerId sont des playerId (comptes).
 */
public record GameViewResponse(
        Long id,
        GameStatus status,
        GamePhase phase,
        int maxPlayers,
        int aiCount,
        Long creatorId,
        Long winnerId,
        Long currentTurnPlayerId,
        int currentBid,
        Long highestBidderId,
        Instant createdAt,
        List<PlayerViewDto> players,
        int roundNumber,
        int revealedFlowersCount,
        int discsOnTable,
        Long winnerParticipantId,
        List<GameEventView> recentEvents
) {}
