package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.ApiException;
import fr.miage.skull.frontend.client.GameApiClient;
import fr.miage.skull.frontend.web.SessionKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;
import java.util.Map;

import static fr.miage.skull.frontend.controller.GameFixtures.ALICE;
import static fr.miage.skull.frontend.controller.GameFixtures.game;
import static fr.miage.skull.frontend.controller.GameFixtures.seat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LobbyControllerTest {

    private GameApiClient games;
    private MockMvc mvc;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        games = Mockito.mock(GameApiClient.class);
        mvc = MockMvcBuilders.standaloneSetup(new LobbyController(games))
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/", ".html"))
                .build();
        session = new MockHttpSession();
        session.setAttribute(SessionKeys.TOKEN, "tok");
        session.setAttribute(SessionKeys.PLAYER_ID, ALICE);
        when(games.listThemes("tok")).thenReturn(List.of("CLASSIC", "MUERTOS", "CELTIQUE"));
    }

    // --- création ---

    @Test
    void formulaireDeCreation_avecLaListeDesMotifs() throws Exception {
        mvc.perform(get("/games/new").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("game-new"))
                .andExpect(model().attributeExists("createGameForm"))
                .andExpect(model().attribute("themes", List.of("CLASSIC", "MUERTOS", "CELTIQUE")));
    }

    @Test
    void creationValide_redirigeVersLaSalleDAttente() throws Exception {
        when(games.createGame(4, 2, "MUERTOS", null, "tok")).thenReturn(game("OPEN"));

        mvc.perform(post("/games").session(session)
                        .param("maxPlayers", "4").param("aiCount", "2")
                        .param("flowerTheme", "MUERTOS").param("skullTheme", ""))
                .andExpect(redirectedUrl("/games/5"));

        verify(games).createGame(4, 2, "MUERTOS", null, "tok");
    }

    @Test
    void creationInvalide_resteSurLeFormulaireSansAppelerLeServeur() throws Exception {
        mvc.perform(post("/games").session(session).param("maxPlayers", "2").param("aiCount", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("game-new"))
                .andExpect(model().attributeHasFieldErrors("createGameForm", "maxPlayers"));

        mvc.perform(post("/games").session(session).param("maxPlayers", "3").param("aiCount", "3"))
                .andExpect(model().attributeHasFieldErrors("createGameForm", "aiCount"));

        verify(games, never()).createGame(anyInt(), anyInt(), any(), any(), anyString());
    }

    @Test
    void creationRefuseeParLeServeur_messageSurLeChamp() throws Exception {
        when(games.createGame(4, 2, null, null, "tok"))
                .thenThrow(new ApiException(400, "La requête contient des champs invalides", Map.of("aiCount", "Trop d'IA")));

        mvc.perform(post("/games").session(session).param("maxPlayers", "4").param("aiCount", "2"))
                .andExpect(view().name("game-new"))
                .andExpect(model().attributeHasFieldErrors("createGameForm", "aiCount"));
    }

    // --- salle d'attente ---

    @Test
    void salleDAttente_vueParLeCreateur() throws Exception {
        when(games.getGame(5L, "tok")).thenReturn(game("OPEN", seat(3, 20, "Bob", false, 2)));

        mvc.perform(get("/games/5").session(session))
                .andExpect(view().name("waiting-room"))
                .andExpect(model().attribute("isParticipant", true))
                .andExpect(model().attribute("isCreator", true))
                .andExpect(model().attribute("canStart", true))
                .andExpect(model().attribute("isFull", false));
    }

    @Test
    void salleDAttente_vueParUnVisiteur_aDeuxSieges_nePeutPasLancer() throws Exception {
        session.setAttribute(SessionKeys.PLAYER_ID, 99L);
        when(games.getGame(5L, "tok")).thenReturn(game("OPEN"));

        mvc.perform(get("/games/5").session(session))
                .andExpect(model().attribute("isParticipant", false))
                .andExpect(model().attribute("isCreator", false))
                .andExpect(model().attribute("canStart", false));
    }

    @Test
    void partieLancee_afficheLaPageDeJeu() throws Exception {
        when(games.getGame(5L, "tok")).thenReturn(game("IN_PROGRESS"));

        mvc.perform(get("/games/5").session(session)).andExpect(view().name("game"));
    }

    @Test
    void partieIntrouvable_retourAuTableauDeBordAvecUnMessage() throws Exception {
        when(games.getGame(5L, "tok")).thenThrow(new ApiException(404, "Partie introuvable : 5", Map.of()));

        mvc.perform(get("/games/5").session(session))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attribute("error", "Partie introuvable."));
    }

    // --- actions ---

    @Test
    void rejoindre_motifsVidesEnvoyesCommeNull() throws Exception {
        mvc.perform(post("/games/5/join").session(session).param("flowerTheme", "").param("skullTheme", "CELTIQUE"))
                .andExpect(redirectedUrl("/games/5"));

        verify(games).joinGame(5L, null, "CELTIQUE", "tok");
    }

    @Test
    void rejoindreUnePartiePleine_messageDuServeur() throws Exception {
        when(games.joinGame(5L, null, null, "tok")).thenThrow(new ApiException(409, "La partie est complète.", Map.of()));

        mvc.perform(post("/games/5/join").session(session))
                .andExpect(redirectedUrl("/games/5"))
                .andExpect(flash().attribute("error", "La partie est complète."));
    }

    @Test
    void quitterAvantLeLancement() throws Exception {
        when(games.leaveGame(5L, "tok")).thenReturn(game("OPEN"));

        mvc.perform(post("/games/5/leave").session(session))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attribute("success", "Tu as quitté la partie."));
    }

    @Test
    void quitterPendantLaPartie_previentQueLaPartieEstPerdue() throws Exception {
        when(games.leaveGame(5L, "tok")).thenReturn(game("IN_PROGRESS"));

        mvc.perform(post("/games/5/leave").session(session))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attribute("success",
                        "Tu as quitté la partie : une IA prend ta place et la partie est comptée comme perdue."));
    }

    @Test
    void lancer_refusParLeServeur_messageAffiche() throws Exception {
        when(games.startGame(5L, "tok"))
                .thenThrow(new ApiException(409, "Il faut au moins 3 participants (joueurs et IA) pour démarrer.", Map.of()));

        mvc.perform(post("/games/5/start").session(session))
                .andExpect(redirectedUrl("/games/5"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void jetonExpire_sessionFermeeEtRetourALaConnexion() throws Exception {
        when(games.startGame(5L, "tok")).thenThrow(new ApiException(401, null, Map.of()));

        mvc.perform(post("/games/5/start").session(session))
                .andExpect(redirectedUrl("/login?expired"));

        assertTrue(session.isInvalid());
    }

    @Test
    void serviceInjoignable_messageClairSansDetailTechnique() throws Exception {
        when(games.joinGame(5L, null, null, "tok")).thenThrow(new ApiException(503, "Service indisponible", Map.of()));

        mvc.perform(post("/games/5/join").session(session))
                .andExpect(flash().attribute("error", "Le serveur de jeu est injoignable, réessaie plus tard."));
    }
}
