package fr.miage.skull.game.ai;

import org.springframework.stereotype.Component;

import java.util.Optional;

/** Version par défaut : ne connaît aucun joueur. L'option la remplacera plus tard. */
@Component
public class NoOpPlayerStyleStore implements PlayerStyleStore {

    @Override
    public Optional<PlayerStyle> find(Long playerId) {
        return Optional.empty();
    }
}
