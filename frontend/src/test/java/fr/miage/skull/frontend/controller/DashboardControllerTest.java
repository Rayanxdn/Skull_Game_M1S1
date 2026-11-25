package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.ApiException;
import fr.miage.skull.frontend.client.GameApiClient;
import fr.miage.skull.frontend.web.SessionKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;
import java.util.Map;

import static fr.miage.skull.frontend.controller.GameFixtures.ALICE;
import static fr.miage.skull.frontend.controller.GameFixtures.game;
import static fr.miage.skull.frontend.controller.GameFixtures.seat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardControllerTest {

    private GameApiClient games;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        games = Mockito.mock(GameApiClient.class);
        mvc = MockMvcBuilders.standaloneSetup(new DashboardController(games))
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/", ".html"))
                .build();
    }

    @Test
    void lesPartiesOuvertesViennentDuGameService() throws Exception {
        when(games.listGames("OPEN", "tok")).thenReturn(List.of(game("OPEN")));
        when(games.listGames("IN_PROGRESS", "tok")).thenReturn(List.of());
        when(games.listGames("FINISHED", "tok")).thenReturn(List.of());

        mvc.perform(get("/dashboard").sessionAttr(SessionKeys.TOKEN, "tok").sessionAttr(SessionKeys.PLAYER_ID, 99L))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("games", hasSize(1)))
                .andExpect(model().attribute("myGames", hasSize(0)));
    }

    @Test
    void mesPartiesEnCoursEtMonHistoriqueSontFiltreesSurMonCompte() throws Exception {
        Map<String, Object> won = game("FINISHED");
        won.put("winnerId", (int) ALICE);
        Map<String, Object> notMine = game("FINISHED");
        GameFixtures.players(notMine).clear();
        GameFixtures.players(notMine).add(seat(7, 42, "Bob", false, 0));
        when(games.listGames("OPEN", "tok")).thenReturn(List.of());
        when(games.listGames("IN_PROGRESS", "tok")).thenReturn(List.of(game("IN_PROGRESS")));
        when(games.listGames("FINISHED", "tok")).thenReturn(List.of(won, notMine));

        mvc.perform(get("/dashboard").sessionAttr(SessionKeys.TOKEN, "tok").sessionAttr(SessionKeys.PLAYER_ID, ALICE))
                .andExpect(status().isOk())
                .andExpect(model().attribute("myGames", hasSize(1)))
                .andExpect(model().attribute("history", hasSize(1)))
                .andExpect(model().attribute("stats", Map.of("games", 1, "wins", 1L, "ratio", 100L)));
    }

    @Test
    void gameServiceInjoignable_messageEtPageQuandMeme() throws Exception {
        when(games.listGames("OPEN", "tok")).thenThrow(new ApiException(503, "Service indisponible", Map.of()));

        mvc.perform(get("/dashboard").sessionAttr(SessionKeys.TOKEN, "tok").sessionAttr(SessionKeys.PLAYER_ID, ALICE))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("games", hasSize(0)));
    }

    @Test
    void jetonExpire_retourALaConnexion() throws Exception {
        when(games.listGames("OPEN", "tok")).thenThrow(new ApiException(401, null, Map.of()));

        mvc.perform(get("/dashboard").sessionAttr(SessionKeys.TOKEN, "tok").sessionAttr(SessionKeys.PLAYER_ID, ALICE))
                .andExpect(redirectedUrl("/login?expired"));
    }
}
