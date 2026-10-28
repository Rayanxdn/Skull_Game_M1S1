package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.mock.MockData;
import fr.miage.skull.frontend.web.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tableau de bord. Le pseudo et les rôles viennent de la session (remplie au login) :
 * on n'appelle pas GET /players/{id}. Parties et historique sont simulés (lots suivants).
 */
@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String forbidden,
                            HttpSession session, Model model) {
        model.addAttribute("pseudo", session.getAttribute(SessionKeys.PSEUDO));
        if (forbidden != null) {
            model.addAttribute("error", "Cette page est réservée aux administrateurs.");
        }
        model.addAttribute("games", MockData.openGames());   // simulé
        model.addAttribute("history", MockData.history());   // simulé
        model.addAttribute("stats", MockData.stats());       // simulé
        return "dashboard";
    }
}
