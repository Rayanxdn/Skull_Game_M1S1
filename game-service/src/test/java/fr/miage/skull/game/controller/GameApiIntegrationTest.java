package fr.miage.skull.game.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.skull.game.service.Themes;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Parcours complet par l'API : sécurité, contrôleur, service, vrai moteur et base H2. */
@SpringBootTest
@AutoConfigureMockMvc
class GameApiIntegrationTest {

    private static final String THREE_SEATS_TWO_AI = "{\"maxPlayers\":3,\"aiCount\":2}";

    @Autowired MockMvc mockMvc;
    @Value("${jwt.secret}") String secret;

    private final ObjectMapper json = new ObjectMapper();

    private String bearer(long id, String pseudo) {
        Date now = new Date();
        return "Bearer " + Jwts.builder()
                .subject(pseudo.toLowerCase() + "@test.local")
                .claim("id", id)
                .claim("pseudo", pseudo)
                .claim("roles", List.of("USER"))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private JsonNode createGame(String token) throws Exception {
        String body = mockMvc.perform(post("/api/v1/games").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(THREE_SEATS_TWO_AI))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void creation_lePseudoVientDuJetonEtLesMotifsSontTiresDansLaListe() throws Exception {
        JsonNode creator = createGame(bearer(101, "Alice")).get("players").get(0);

        assertEquals("Alice", creator.get("displayName").asText());
        assertTrue(Themes.BASE.contains(creator.get("flowerTheme").asText()));
        assertTrue(Themes.BASE.contains(creator.get("skullTheme").asText()));
    }

    @Test
    void creation_motifHorsDeLaListe_400() throws Exception {
        mockMvc.perform(post("/api/v1/games").header("Authorization", bearer(102, "Bob"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxPlayers\":3,\"aiCount\":2,\"flowerTheme\":\"<script>\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creation_sansJeton_401() throws Exception {
        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON).content(THREE_SEATS_TWO_AI))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listeDesMotifsDisponibles() throws Exception {
        mockMvc.perform(get("/api/v1/games/themes").header("Authorization", bearer(103, "Chloe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(Themes.BASE.size()))
                .andExpect(jsonPath("$[0]").value("CLASSIC"));
    }

    @Test
    void partieAvecDeuxIA_lancementPuisPremierCoup_lesDisquesDesAutresRestentCaches() throws Exception {
        String token = bearer(104, "Dana");
        JsonNode game = createGame(token);
        long id = game.get("id").asLong();
        long me = game.get("players").get(0).get("participantId").asLong();

        mockMvc.perform(post("/api/v1/games/{id}/start", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.phase").value("INITIAL_PLACEMENT"))
                .andExpect(jsonPath("$.currentTurnPlayerId").value(me))
                .andExpect(jsonPath("$.players[0].myHand.length()").value(4));

        mockMvc.perform(post("/api/v1/games/{id}/moves", id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"BID\",\"bidAmount\":1}"))
                .andExpect(status().isBadRequest());

        // après la pose, les deux IA posent à leur tour puis la main revient au créateur
        mockMvc.perform(post("/api/v1/games/{id}/moves", id).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PLACE_DISC\",\"discType\":\"FLOWER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("TURN_ACTION"))
                .andExpect(jsonPath("$.currentTurnPlayerId").value(me))
                .andExpect(jsonPath("$.players[0].myStack[0]").value("FLOWER"))
                .andExpect(jsonPath("$.players[1].stackCount").value(1))
                .andExpect(jsonPath("$.players[1].myHand").doesNotExist())
                .andExpect(jsonPath("$.players[1].myStack").doesNotExist());
    }

    @Test
    void unJoueurAbsentDeLaPartieNePeutPasJouer_403() throws Exception {
        String owner = bearer(105, "Eve");
        long id = createGame(owner).get("id").asLong();
        mockMvc.perform(post("/api/v1/games/{id}/start", id).header("Authorization", owner))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/games/{id}/moves", id).header("Authorization", bearer(106, "Intrus"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"PLACE_DISC\",\"discType\":\"FLOWER\"}"))
                .andExpect(status().isForbidden());
    }
}
