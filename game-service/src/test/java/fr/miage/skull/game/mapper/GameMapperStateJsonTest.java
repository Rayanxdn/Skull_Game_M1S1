package fr.miage.skull.game.mapper;

import fr.miage.skull.game.engine.DiscType;
import fr.miage.skull.game.engine.GameEngine;
import fr.miage.skull.game.engine.GameState;
import fr.miage.skull.game.engine.Move;
import fr.miage.skull.game.engine.MoveType;
import fr.miage.skull.game.engine.SkullGameEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameMapperStateJsonTest {

    private final GameMapper mapper = new GameMapper();
    private final SkullGameEngine engine = new SkullGameEngine();

    @Test
    void lEtatDuMoteurSurvitAuPassageEnJson() {
        GameState state = engine.initGame(List.of(
                new GameEngine.PlayerSetup("1", "Alice", false),
                new GameEngine.PlayerSetup("2", "IA #1", true),
                new GameEngine.PlayerSetup("3", "IA #2", true)));
        state = engine.applyMove(state, "1", new Move(MoveType.PLACE_DISC, DiscType.SKULL, null, null), new Random(1));

        GameState copy = mapper.deserializeState(mapper.serializeState(state));

        assertEquals(state, copy);
    }
}
