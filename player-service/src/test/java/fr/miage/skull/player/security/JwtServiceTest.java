package fr.miage.skull.player.security;

import fr.miage.skull.player.model.Player;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    @Test
    void leJetonContientLIdentifiantLEmailEtLePseudo() {
        Player player = new Player("alice@test.local", "mot-de-passe-hache", "Alice", List.of("USER"));
        player.setId(7L);

        String token = new JwtService(SECRET, 3_600_000).generateToken(player);
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("alice@test.local", claims.getSubject());
        assertEquals(7L, ((Number) claims.get("id")).longValue());
        assertEquals("Alice", claims.get("pseudo", String.class));
    }
}
