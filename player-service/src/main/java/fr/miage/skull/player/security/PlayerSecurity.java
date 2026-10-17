package fr.miage.skull.player.security;

import fr.miage.skull.player.repository.PlayerRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Utilisé dans les annotations @PreAuthorize pour savoir si on agit sur son propre compte. */
@Component("playerSecurity")
public class PlayerSecurity {

    private final PlayerRepository playerRepository;

    public PlayerSecurity(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public boolean isOwner(Long playerId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return playerRepository.findById(playerId)
                .map(player -> player.getEmail().equals(authentication.getName()))
                .orElse(false);
    }
}
