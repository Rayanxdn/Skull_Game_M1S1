package fr.miage.skull.game.engine;

import java.util.List;
import java.util.Optional;

/**
 * challengerId : meilleur enchérisseur pendant les enchères, puis joueur qui tente le défi.
 * recentEvents : derniers événements de la partie (journal), du plus ancien au plus récent.
 */
public record GameState(
        GamePhase phase,
        List<PlayerState> players,
        String currentPlayerId,
        String challengerId,
        int currentBid,
        int revealedFlowersCount,
        String winnerId,
        int roundNumber,
        String roundStarterId,
        List<GameEvent> recentEvents
) {
    public GameState {
        players = players == null ? List.of() : List.copyOf(players);
        recentEvents = recentEvents == null ? List.of() : List.copyOf(recentEvents);
    }

    public Optional<PlayerState> player(String id) {
        return players.stream().filter(p -> p.playerId().equals(id)).findFirst();
    }

    public int discsOnTable() {
        return players.stream().mapToInt(p -> p.stack().size() + p.revealed().size()).sum();
    }

    public GameState withPlayers(List<PlayerState> newPlayers) {
        return new GameState(phase, newPlayers, currentPlayerId, challengerId, currentBid,
                revealedFlowersCount, winnerId, roundNumber, roundStarterId, recentEvents);
    }
}
