package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.ApiException;
import fr.miage.skull.frontend.client.PlayerApiClient;
import fr.miage.skull.frontend.web.SessionKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminControllerTest {

    private PlayerApiClient api;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        api = Mockito.mock(PlayerApiClient.class);
        mvc = MockMvcBuilders.standaloneSetup(new AdminController(api))
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/", ".html"))
                .build();
    }

    @Test
    void admin_seesThePlayerList() throws Exception {
        when(api.listPlayers("tok")).thenReturn(
                List.of(Map.of("id", 1, "pseudo", "Bob", "email", "bob@test.fr", "banned", false)));

        mvc.perform(get("/admin").sessionAttr(SessionKeys.ROLES, List.of("ADMIN"))
                        .sessionAttr(SessionKeys.TOKEN, "tok"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("players"));
    }

    @Test
    void normalUser_isRedirectedAndNeverCallsTheApi() throws Exception {
        mvc.perform(get("/admin").sessionAttr(SessionKeys.ROLES, List.of("USER"))
                        .sessionAttr(SessionKeys.TOKEN, "tok"))
                .andExpect(redirectedUrl("/dashboard?forbidden"));

        mvc.perform(post("/admin/players/1/ban").param("banned", "true")
                        .sessionAttr(SessionKeys.ROLES, List.of("USER"))
                        .sessionAttr(SessionKeys.TOKEN, "tok"))
                .andExpect(redirectedUrl("/dashboard?forbidden"));

        verify(api, never()).listPlayers(anyString());
        verify(api, never()).setBanned(anyLong(), anyBoolean(), anyString());
    }

    @Test
    void admin_canBanAPlayer() throws Exception {
        mvc.perform(post("/admin/players/7/ban").param("banned", "true")
                        .sessionAttr(SessionKeys.ROLES, List.of("ADMIN"))
                        .sessionAttr(SessionKeys.TOKEN, "tok"))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("success", "Joueur banni."));

        verify(api).setBanned(7L, true, "tok");
    }

    @Test
    void expiredToken_sendsBackToLogin() throws Exception {
        when(api.listPlayers("tok")).thenThrow(new ApiException(401, null, Map.of()));

        mvc.perform(get("/admin").sessionAttr(SessionKeys.ROLES, List.of("ADMIN"))
                        .sessionAttr(SessionKeys.TOKEN, "tok"))
                .andExpect(redirectedUrl("/login?expired"));
    }

    @Test
    void bannedAdmin_isLoggedOut() throws Exception {
        when(api.listPlayers("tok")).thenThrow(new ApiException(403, null, Map.of()));

        mvc.perform(get("/admin").sessionAttr(SessionKeys.ROLES, List.of("ADMIN"))
                        .sessionAttr(SessionKeys.TOKEN, "tok"))
                .andExpect(redirectedUrl("/login?expired"));
    }

    @Test
    void unknownPlayer_showsAMessage() throws Exception {
        when(api.setBanned(99L, true, "tok")).thenThrow(new ApiException(404, null, Map.of()));

        mvc.perform(post("/admin/players/99/ban").param("banned", "true")
                        .sessionAttr(SessionKeys.ROLES, List.of("ADMIN"))
                        .sessionAttr(SessionKeys.TOKEN, "tok"))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("error", "Joueur introuvable."));
    }
}
