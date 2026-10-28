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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private PlayerApiClient api;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        api = Mockito.mock(PlayerApiClient.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(api))
                .setViewResolvers(new InternalResourceViewResolver("/WEB-INF/", ".html"))
                .build();
    }

    @Test
    void login_withEmptyForm_showsFieldErrors() throws Exception {
        mvc.perform(post("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeHasFieldErrors("loginForm", "email", "password"));
        verify(api, never()).login(any(), any());
    }

    @Test
    void login_withWrongPassword_showsMessage() throws Exception {
        when(api.login("bob@test.fr", "secret1")).thenThrow(new ApiException(401, null, Map.of()));

        mvc.perform(post("/login").param("email", "bob@test.fr").param("password", "secret1"))
                .andExpect(view().name("login"))
                .andExpect(model().attribute("error", "Email ou mot de passe incorrect."));
    }

    @Test
    void login_whenBanned_showsBannedMessage() throws Exception {
        when(api.login("bob@test.fr", "secret1")).thenThrow(new ApiException(403, null, Map.of()));

        mvc.perform(post("/login").param("email", "bob@test.fr").param("password", "secret1"))
                .andExpect(model().attribute("error", "Ce compte a été banni."));
    }

    @Test
    void login_whenBackendDown_showsUnavailableMessage() throws Exception {
        when(api.login("bob@test.fr", "secret1")).thenThrow(new ApiException(503, "x", Map.of()));

        mvc.perform(post("/login").param("email", "bob@test.fr").param("password", "secret1"))
                .andExpect(model().attribute("error", "Le serveur est injoignable, réessaie plus tard."));
    }

    @Test
    void login_success_storesSessionAndRedirects() throws Exception {
        when(api.login("bob@test.fr", "secret1")).thenReturn(
                Map.of("token", "jwt", "id", 5, "pseudo", "Bob", "roles", List.of("USER")));

        mvc.perform(post("/login").param("email", "bob@test.fr").param("password", "secret1"))
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(request().sessionAttribute(SessionKeys.TOKEN, "jwt"))
                .andExpect(request().sessionAttribute(SessionKeys.PSEUDO, "Bob"));
    }

    @Test
    void register_withDifferentPasswords_isRejectedWithoutCallingTheApi() throws Exception {
        mvc.perform(post("/register").param("pseudo", "Bob").param("email", "bob@test.fr")
                        .param("password", "secret1").param("confirmPassword", "secret2"))
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "confirmPassword"));
        verify(api, never()).register(any(), any(), any());
    }

    @Test
    void register_withTakenEmail_showsErrorOnEmail() throws Exception {
        when(api.register("Bob", "bob@test.fr", "secret1")).thenThrow(new ApiException(409, null, Map.of()));

        mvc.perform(post("/register").param("pseudo", "Bob").param("email", "bob@test.fr")
                        .param("password", "secret1").param("confirmPassword", "secret1"))
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "email"));
    }

    @Test
    void register_success_redirectsToLogin() throws Exception {
        when(api.register("Bob", "bob@test.fr", "secret1")).thenReturn(Map.of("id", 1));

        mvc.perform(post("/register").param("pseudo", "Bob").param("email", "bob@test.fr")
                        .param("password", "secret1").param("confirmPassword", "secret1"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void logout_invalidatesSession() throws Exception {
        mvc.perform(post("/logout").sessionAttr(SessionKeys.TOKEN, "jwt"))
                .andExpect(redirectedUrl("/login?logout"));
    }
}
