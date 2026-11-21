package fr.miage.skull.game.ai;

import java.util.Optional;

/** Donne le style d'un joueur. Rien n'est appris pour l'instant : l'IA standard ne s'en sert pas. */
public interface PlayerStyleStore {

    Optional<PlayerStyle> find(Long playerId);
}
