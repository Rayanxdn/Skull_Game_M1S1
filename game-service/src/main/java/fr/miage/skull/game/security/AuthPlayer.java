package fr.miage.skull.game.security;

import java.util.List;

/** Le joueur connecté, reconstruit à partir des informations du jeton. */
public record AuthPlayer(Long id, String email, List<String> roles) {
}
