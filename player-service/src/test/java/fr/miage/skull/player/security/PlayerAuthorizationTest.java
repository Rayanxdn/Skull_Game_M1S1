package fr.miage.skull.player.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlayerAuthorizationTest {
    private final PlayerAuthorization authorization =
            new PlayerAuthorization();

    @Test
    void anonymousUserIsNotOwner() {assertFalse(authorization.isOwner(1L, null));
    }

    @Test
    void authenticatedUserIsNotOwnerUntilIdentityIsConfigured() {

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                        "joueur@example.com",
                        "password",
                        List.of());

        assertFalse(authorization.isOwner(1L, authentication));
    }
}
