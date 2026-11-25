package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.ApiException;
import fr.miage.skull.frontend.client.GameApiClient;
import fr.miage.skull.frontend.client.PlayerApiClient;
import fr.miage.skull.frontend.web.SessionKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.miage.skull.frontend.controller.GameFixtures.ALICE;
import static fr.miage.skull.frontend.controller.TableFixtures.table;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

class TableControllerTest {

    private GameApiClient games;
    private PlayerApiClient players;
    private MockMvc mvc;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        games = Mockito.mock(GameApiClient.class);
        players = Mockito.mock(PlayerApiClient.class);
        mvc = MockMvcBuilders.standaloneSetup(new TableController(games, players)).build();
        session = new MockHttpSession();
        session.setAttribute(SessionKeys.TOKEN, "tok");
        session.setAttribute(SessionKeys.PLAYER_ID, ALICE);
        when(games.getGame(5L, "tok")).thenReturn(table("TURN_ACTION", 3, List.of("FLOWER")));
    }

    private static Map<String, Object> move(String type, String disc, Integer bid, Long target) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("discType", disc);
        m.put("bidAmount", bid);
        m.put("targetPlayerId", target);
        return m;
    }

    // --- jouer un coup ---

    @Test
    void poserUneFleur() throws Exception {
        mvc.perform(post("/games/5/move").session(session).param("type", "PLACE_DISC").param("discType", "FLOWER"))
                .andExpect(redirectedUrl("/games/5"));
        verify(games).playMove(5L, move("PLACE_DISC", "FLOWER", null, null), "tok");
    }

    @Test
    void encherir() throws Exception {
        mvc.perform(post("/games/5/move").session(session).param("type", "BID").param("bidAmount", "3"))
                .andExpect(redirectedUrl("/games/5"));
        verify(games).playMove(5L, move("BID", null, 3, null), "tok");
    }

    @Test
    void retournerUnePile() throws Exception {
        mvc.perform(post("/games/5/move").session(session).param("type", "REVEAL").param("targetPlayerId", "3"))
                .andExpect(redirectedUrl("/games/5"));
        verify(games).playMove(5L, move("REVEAL", null, null, 3L), "tok");
    }

    @Test
    void coupInconnu_rienNEstEnvoye() throws Exception {
        mvc.perform(post("/games/5/move").session(session).param("type", "CHEAT"))
                .andExpect(redirectedUrl("/games/5"))
                .andExpect(flash().attribute("error", "Coup inconnu."));
        mvc.perform(post("/games/5/move").session(session).param("type", "PLACE_DISC").param("discType", "GOLD"))
                .andExpect(flash().attribute("error", "Coup inconnu."));
        verify(games, never()).playMove(anyLong(), any(), anyString());
    }

    @Test
    void coupRefuseParLeServeur_sonMessageEstAffiche() throws Exception {
        when(games.playMove(anyLong(), any(), anyString()))
                .thenThrow(new ApiException(400, "Ce n'est pas votre tour.", Map.of()));

        mvc.perform(post("/games/5/move").session(session).param("type", "PASS"))
                .andExpect(redirectedUrl("/games/5"))
                .andExpect(flash().attribute("error", "Ce n'est pas votre tour."));
    }

    @Test
    void jetonExpire_sessionFermee() throws Exception {
        when(games.playMove(anyLong(), any(), anyString())).thenThrow(new ApiException(401, null, Map.of()));

        mvc.perform(post("/games/5/move").session(session).param("type", "PASS"))
                .andExpect(redirectedUrl("/login?expired"));
        assertTrue(session.isInvalid());
    }

    // --- signaler ---

    @Test
    void signalerUnAutreJoueur_leCompteEstRetrouveDansLaPartie() throws Exception {
        mvc.perform(post("/games/5/report").session(session).param("participantId", "3").param("reason", "  Insultes  "))
                .andExpect(redirectedUrl("/games/5"))
                .andExpect(flash().attributeExists("success"));
        verify(players).reportPlayer(20L, "Insultes", 5L, "tok");
    }

    @Test
    void signalerUneIA_refuse() throws Exception {
        mvc.perform(post("/games/5/report").session(session).param("participantId", "2").param("reason", "bug"))
                .andExpect(flash().attribute("error", "Ce joueur ne peut pas être signalé."));
        verify(players, never()).reportPlayer(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void seSignalerSoiMeme_refuse() throws Exception {
        mvc.perform(post("/games/5/report").session(session).param("participantId", "1").param("reason", "test"))
                .andExpect(flash().attribute("error", "Ce joueur ne peut pas être signalé."));
        verify(players, never()).reportPlayer(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void signalerSansMotif_refuseSansAppelerLesServices() throws Exception {
        mvc.perform(post("/games/5/report").session(session).param("participantId", "3").param("reason", "   "))
                .andExpect(redirectedUrl("/games/5"))
                .andExpect(flash().attributeExists("error"));
        mvc.perform(post("/games/5/report").session(session).param("participantId", "3").param("reason", "x".repeat(301)))
                .andExpect(flash().attributeExists("error"));
        verify(games, never()).getGame(anyLong(), anyString());
        verify(players, never()).reportPlayer(anyLong(), anyString(), anyLong(), anyString());
    }

    @Test
    void signalerDeuxFois_messageClair() throws Exception {
        when(players.reportPlayer(anyLong(), anyString(), anyLong(), anyString()))
                .thenThrow(new ApiException(409, "Vous avez déjà signalé ce joueur pour cette partie", Map.of()));

        mvc.perform(post("/games/5/report").session(session).param("participantId", "3").param("reason", "Triche"))
                .andExpect(flash().attribute("error", "Tu as déjà signalé ce joueur pour cette partie."));
    }
}
