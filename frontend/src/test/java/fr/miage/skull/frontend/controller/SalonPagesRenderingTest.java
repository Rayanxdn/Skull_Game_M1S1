package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.GameApiClient;
import fr.miage.skull.frontend.web.SessionKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static fr.miage.skull.frontend.controller.GameFixtures.ALICE;
import static fr.miage.skull.frontend.controller.GameFixtures.game;
import static fr.miage.skull.frontend.controller.GameFixtures.seat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Affiche vraiment les pages (templates Thymeleaf compris), avec un game-service simulé. */
@SpringBootTest
@AutoConfigureMockMvc
class SalonPagesRenderingTest {

    @Autowired MockMvc mvc;
    @MockitoBean GameApiClient games;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        session.setAttribute(SessionKeys.TOKEN, "tok");
        session.setAttribute(SessionKeys.PLAYER_ID, ALICE);
        session.setAttribute(SessionKeys.PSEUDO, "Alice");
        session.setAttribute(SessionKeys.ROLES, List.of("USER"));
        when(games.listThemes("tok")).thenReturn(List.of("CLASSIC", "MUERTOS", "CELTIQUE"));
    }

    @Test
    void tableauDeBord() throws Exception {
        Map<String, Object> finished = game("FINISHED");
        finished.put("winnerId", (int) ALICE);
        when(games.listGames("OPEN", "tok")).thenReturn(List.of(game("OPEN")));
        when(games.listGames("IN_PROGRESS", "tok")).thenReturn(List.of(game("IN_PROGRESS")));
        when(games.listGames("FINISHED", "tok")).thenReturn(List.of(finished));

        mvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Parties ouvertes")))
                .andExpect(content().string(containsString("Mes parties en cours")))
                .andExpect(content().string(containsString("Victoire")))
                .andExpect(content().string(not(containsString("données simulées"))));
    }

    @Test
    void formulaireDeCreation() throws Exception {
        mvc.perform(get("/games/new").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Créer la partie")))
                .andExpect(content().string(containsString("Día de los Muertos")));
    }

    @Test
    void salleDAttente_createur() throws Exception {
        when(games.getGame(5L, "tok")).thenReturn(game("OPEN", seat(3, 20, "Bob", false, 2)));

        mvc.perform(get("/games/5").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lancer la partie")))
                .andExpect(content().string(containsString("Quitter la partie")))
                .andExpect(content().string(containsString("Bob")));
    }

    @Test
    void salleDAttente_visiteur() throws Exception {
        session.setAttribute(SessionKeys.PLAYER_ID, 99L);
        when(games.getGame(5L, "tok")).thenReturn(game("OPEN"));

        mvc.perform(get("/games/5").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Rejoindre cette partie")))
                .andExpect(content().string(not(containsString("Lancer la partie"))));
    }

    @Test
    void lesNomsSontEchappes_pasDInjectionHtml() throws Exception {
        when(games.getGame(5L, "tok")).thenReturn(game("OPEN", seat(3, 20, "<script>alert(1)</script>", false, 2)));

        mvc.perform(get("/games/5").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    @Test
    void pageDeJeu() throws Exception {
        when(games.getGame(5L, "tok")).thenReturn(game("IN_PROGRESS"));

        mvc.perform(get("/games/5").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Partie")));
    }

    @Test
    void sansSession_lesPagesDuJeuRenvoientALaConnexion() throws Exception {
        mvc.perform(get("/games/new")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/games/5")).andExpect(redirectedUrl("/login"));
    }
}
