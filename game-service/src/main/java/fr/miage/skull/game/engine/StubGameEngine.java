package fr.miage.skull.game.engine;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class StubGameEngine implements GameEngine {

    @Override
    public GameState initGame(List<PlayerSetup> players) {
        List<PlayerState> playerStates = players.stream()
                .map(p -> new PlayerState(
                        p.playerId(),
                        p.displayName(),
                        p.isAi(),
                        List.of(DiscType.FLOWER, DiscType.FLOWER, DiscType.FLOWER, DiscType.SKULL),
                        List.of(),
                        0, 0, 0, false, false
                ))
                .toList();

        return new GameState(
                GamePhase.INITIAL_PLACEMENT,
                playerStates,
                playerStates.get(0).playerId(),
                null,
                0,
                0,
                null
        );
    }

    @Override
    public GameState applyMove(GameState state, String playerId, Move move, Random random) {
        if (!state.currentPlayerId().equals(playerId)) {
            throw new IllegalMoveException("Ce n'est pas le tour de ce joueur.");
        }
        
        List<PlayerState> players = state.players();
        int currentIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).playerId().equals(playerId)) {
                currentIndex = i;
                break;
            }
        }
        String nextPlayerId = players.get((currentIndex + 1) % players.size()).playerId();

        return new GameState(
                GamePhase.TURN_ACTION,
                new ArrayList<>(players),
                nextPlayerId,
                state.challengerId(),
                state.currentBid(),
                state.revealedFlowersCount(),
                state.winnerId()
        );
    }
}
