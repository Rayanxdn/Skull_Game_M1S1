package fr.miage.skull.frontend.web;

import fr.miage.skull.frontend.client.ApiException;

/** Message à afficher pour une erreur du game-service, sans aucun détail technique (R25). */
public final class GameErrors {

    private GameErrors() {
    }

    public static String message(ApiException e) {
        return switch (e.getStatus()) {
            case 404 -> "Partie introuvable.";
            case 503 -> "Le serveur de jeu est injoignable, réessaie plus tard.";
            // messages rédigés par le game-service lui-même (règles du jeu, partie pleine...)
            case 400, 403, 409 -> e.getMessage() != null ? e.getMessage() : "Action refusée.";
            default -> "Erreur inattendue (" + e.getStatus() + ").";
        };
    }

    /** Jeton expiré ou refusé : la session ne sert plus à rien. */
    public static boolean sessionExpired(ApiException e) {
        return e.getStatus() == 401;
    }
}
