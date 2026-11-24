package fr.miage.skull.game.engine;

import java.util.List;
import java.util.Random;

public interface GameEngine {
    record PlayerSetup(String playerId, String displayName, boolean isAi) {}

    GameState initGame(List<PlayerSetup> players);

    GameState applyMove(GameState state, String playerId, Move move, Random random);
}
