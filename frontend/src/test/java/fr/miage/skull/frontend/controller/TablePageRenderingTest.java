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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static fr.miage.skull.frontend.controller.GameFixtures.ALICE;
import static fr.miage.skull.frontend.controller.TableFixtures.addEvent;
import static fr.miage.skull.frontend.controller.TableFixtures.event;
import static fr.miage.skull.frontend.controller.TableFixtures.seatOf;
import static fr.miage.skull.frontend.controller.TableFixtures.table;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Affiche vraiment la page de la table (template compris), avec un game-service simulé. */
@SpringBootTest
@AutoConfigureMockMvc
class TablePageRenderingTest {

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
    }

    @Test
    void monTour_lesBoutonsDeLaPhaseSansRafraichissement() throws Exception {
        when(games.getGame(5L, "tok")).thenReturn(table("INITIAL_PLACEMENT", 1, List.of("FLOWER", "SKULL")));

        mvc.perform(get("/games/5").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("À toi de jouer")))
                .andExpect(content().string(containsString("Poser une fleur")))
                .andExpect(content().string(containsString("Poser le crâne")))
                .andExpect(content().string(not(containsString("setInterval"))));
    }

    @Test
    void pasMonTour_laPageSeMetAJourToutSeule() throws Exception {
        when(games.getGame(5L, "tok")).thenReturn(table("TURN_ACTION", 3, List.of("FLOWER")));

        mvc.perform(get("/games/5").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Au tour de")))
                .andExpect(content().string(not(containsString("Poser une fleur"))))
                .andExpect(content().string(containsString("setInterval")));
    }

    @Test
    void defi_bandeauEtCibles() throws Exception {
        Map<String, Object> game = table("CHALLENGE", 1, List.of());
        game.put("currentBid", 3);
        game.put("revealedFlowersCount", 1);
        seatOf(game, 1).put("stackCount", 0);
        seatOf(game, 1).put("revealed", List.of("FLOWER"));
        seatOf(game, 3).put("stackCount", 2);
        when(games.getGame(5L, "tok")).thenReturn(game);

        mvc.perform(get("/games/5").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 / 3 fleurs trouvées")))
                .andExpect(content().string(containsString("Retourner : Bob (2)")));
    }

    @Test
    void boutonSignaler_seulementPourLesAutresHumains() throws Exception {
        when(games.getGame(5L, "tok")).thenReturn(table("TURN_ACTION", 3, List.of()));

        mvc.perform(get("/games/5").session(session))
                .andExpect(content().string(containsString("name=\"participantId\" value=\"3\"")))
                .andExpect(content().string(not(containsString("name=\"participantId\" value=\"2\""))))
                .andExpect(content().string(not(containsString("name=\"participantId\" value=\"1\""))));
    }

    @Test
    void lesDisquesCachesDesAutresNeSontJamaisAffiches() throws Exception {
        Map<String, Object> game = table("TURN_ACTION", 3, List.of("FLOWER"));
        seatOf(game, 3).put("myHand", List.of("SKULL"));
        seatOf(game, 3).put("myStack", List.of("SKULL"));
        when(games.getGame(5L, "tok")).thenReturn(game);

        String html = mvc.perform(get("/games/5").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Crâne : <b>0</b>")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        // seul le logo « SKULL. » de la barre de navigation contient ce mot
        assertFalse(html.replace("SKULL<span>.</span>", "").contains("SKULL"));
    }

    @Test
    void journal_echappe_pasDInjectionHtml() throws Exception {
        Map<String, Object> game = table("TURN_ACTION", 3, List.of());
        addEvent(game, event("BID", 3, "<script>alert(1)</script>", null, null, 2));
        when(games.getGame(5L, "tok")).thenReturn(game);

        mvc.perform(get("/games/5").session(session))
                .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt; annonce 2 fleurs.")))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))));
    }

    @Test
    void finDePartie_resultatEtRetour() throws Exception {
        Map<String, Object> game = table("FINISHED", 1, List.of("FLOWER"));
        game.put("status", "FINISHED");
        game.put("winnerParticipantId", 1);
        when(games.getGame(5L, "tok")).thenReturn(game);

        mvc.perform(get("/games/5").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tu as gagné !")))
                .andExpect(content().string(not(containsString("Poser une fleur"))))
                .andExpect(content().string(not(containsString("setInterval"))));
    }
}
