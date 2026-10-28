package fr.miage.skull.player.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("playerAuthorization")
public class PlayerAuthorization {
    public boolean isOwner(Long playerId,Authentication authentication) {
        if (playerId == null
                || authentication == null
                || !authentication.isAuthenticated()) {
            return false;
        }
        return false;

    }
}
