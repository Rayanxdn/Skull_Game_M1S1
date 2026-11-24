package fr.miage.skull.game.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtSecurityTest {

    @Autowired MockMvc mockMvc;
    @Value("${jwt.secret}") String secret;

    private String token(String withSecret, long durationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject("alice@test.local")
                .claim("id", 7)
                .claim("pseudo", "Alice")
                .claim("roles", List.of("USER"))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + durationMs))
                .signWith(Keys.hmacShaKeyFor(withSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void ping_isPublic() throws Exception {
        mockMvc.perform(get("/ping")).andExpect(status().isOk());
    }

    @Test
    void protectedRoute_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/whoami")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withGarbageToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/whoami").header("Authorization", "Bearer n-importe-quoi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withExpiredToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/whoami").header("Authorization", "Bearer " + token(secret, -60_000)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withTokenSignedByAnotherSecret_returns401() throws Exception {
        String forged = token("un-autre-secret-un-autre-secret-un-autre-secret-99", 60_000);
        mockMvc.perform(get("/api/v1/whoami").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_withValidToken_returnsIdentityFromToken() throws Exception {
        mockMvc.perform(get("/api/v1/whoami").header("Authorization", "Bearer " + token(secret, 60_000)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("alice@test.local")));
    }

    @Test
    void protectedRoute_withValidToken_readsPseudoFromToken() throws Exception {
        mockMvc.perform(get("/api/v1/whoami").header("Authorization", "Bearer " + token(secret, 60_000)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("pseudo=Alice")));
    }
}
