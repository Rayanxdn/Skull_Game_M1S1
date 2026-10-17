package fr.miage.skull.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String PASSWORD = "secret123";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ---------- helpers ----------

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    private long register(String pseudo, String email) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("pseudo", pseudo, "email", email, "password", PASSWORD));
        String response = mockMvc.perform(post("/api/v1/players/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private ResultActions loginRequest(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String loginToken(String email, String password) throws Exception {
        String response = loginRequest(email, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- accès public / sans token ----------

    @Test
    void ping_isPublic() throws Exception {
        mockMvc.perform(get("/ping")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/players/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/players/1").header("Authorization", "Bearer n-importe-quoi"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- connexion ----------

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        String email = uniqueEmail();
        register("Bob", email);
        loginRequest(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        register("Bob", email);
        loginRequest(email, "mauvais-mot-de-passe").andExpect(status().isUnauthorized());
    }

    // ---------- rôles ----------

    @Test
    void user_cannotListAllPlayers_returns403() throws Exception {
        String email = uniqueEmail();
        register("Bob", email);
        String token = loginToken(email, PASSWORD);

        mockMvc.perform(get("/api/v1/players").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_canListAllPlayers() throws Exception {
        String adminToken = loginToken("admin@test.local", "AdminTest123!");

        mockMvc.perform(get("/api/v1/players").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void user_cannotBanAnotherPlayer_returns403() throws Exception {
        long victimId = register("Victime", uniqueEmail());
        String attackerEmail = uniqueEmail();
        register("Attaquant", attackerEmail);
        String attackerToken = loginToken(attackerEmail, PASSWORD);

        mockMvc.perform(put("/api/v1/players/" + victimId + "/ban")
                        .param("banned", "true")
                        .header("Authorization", bearer(attackerToken)))
                .andExpect(status().isForbidden());
    }

    // ---------- propriété du compte ----------

    @Test
    void user_canModifyOwnAccount() throws Exception {
        String email = uniqueEmail();
        long id = register("Bob", email);
        String token = loginToken(email, PASSWORD);

        mockMvc.perform(put("/api/v1/players/" + id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pseudo\":\"Robert\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pseudo").value("Robert"));
    }

    @Test
    void user_cannotModifyAnotherAccount_returns403() throws Exception {
        long victimId = register("Victime", uniqueEmail());
        String attackerEmail = uniqueEmail();
        register("Attaquant", attackerEmail);
        String attackerToken = loginToken(attackerEmail, PASSWORD);

        mockMvc.perform(put("/api/v1/players/" + victimId)
                        .header("Authorization", bearer(attackerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pseudo\":\"hack\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- bannissement ----------

    @Test
    void bannedPlayer_cannotLogin_andOldTokenIsRejected() throws Exception {
        String email = uniqueEmail();
        long id = register("Tricheur", email);
        String oldToken = loginToken(email, PASSWORD);
        String adminToken = loginToken("admin@test.local", "AdminTest123!");

        mockMvc.perform(put("/api/v1/players/" + id + "/ban")
                        .param("banned", "true")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        // plus de nouvelle connexion
        loginRequest(email, PASSWORD).andExpect(status().isForbidden());

        // et l'ancien token, pourtant encore valide, est rejeté
        mockMvc.perform(get("/api/v1/players/" + id).header("Authorization", bearer(oldToken)))
                .andExpect(status().isUnauthorized());
    }
}
