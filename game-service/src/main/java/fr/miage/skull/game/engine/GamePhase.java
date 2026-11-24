package fr.miage.skull.game.engine;

public enum GamePhase {
    INITIAL_PLACEMENT, // Chaque joueur pose son 1er disque obligatoire
    TURN_ACTION,       // Poser un disque ou lancer une enchère
    BIDDING,           // Surenchérir ou se retirer. Bidding = enchère
    CHALLENGE,         // Retourner les disques un par un
    FINISHED           // Partie terminée
}
