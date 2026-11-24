package fr.miage.skull.game.engine;

/**
 * Ce qui vient de se passer à la table, sans jamais révéler un disque caché.
 * targetId : joueur dont la pile est retournée ; value : enchère, points ou numéro de manche.
 */
public record GameEvent(GameEventType type, String playerId, String targetId, Integer value) {

    public static GameEvent of(GameEventType type, String playerId) {
        return new GameEvent(type, playerId, null, null);
    }
}
