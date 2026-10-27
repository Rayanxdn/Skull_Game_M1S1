package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.ApiException;
import fr.miage.skull.frontend.client.PlayerApiClient;
import fr.miage.skull.frontend.mock.MockData;
import fr.miage.skull.frontend.web.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private final PlayerApiClient api;

    public DashboardController(PlayerApiClient api) {
        this.api = api;
    }

    @GetMapping("/dashboard")
    @SuppressWarnings("unchecked")
    public String dashboard(HttpSession session, Model model) {
        String token = (String) session.getAttribute(SessionKeys.TOKEN);
        Long id = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        List<String> roles = (List<String>) session.getAttribute(SessionKeys.ROLES);

        try {
            model.addAttribute("profile", api.getPlayer(id, token)); // donnée réelle
        } catch (ApiException e) {
            if (e.getStatus() == 401 || e.getStatus() == 403) {
                session.invalidate(); // token expiré ou compte banni
                return "redirect:/login?expired";
            }
            model.addAttribute("error", "Impossible de charger ton profil pour le moment.");
        }

        model.addAttribute("pseudo", session.getAttribute(SessionKeys.PSEUDO));
        model.addAttribute("roles", roles);
        model.addAttribute("isAdmin", roles != null && roles.contains("ADMIN"));
        model.addAttribute("games", MockData.openGames());   // simulé
        model.addAttribute("history", MockData.history());   // simulé
        return "dashboard";
    }
}
