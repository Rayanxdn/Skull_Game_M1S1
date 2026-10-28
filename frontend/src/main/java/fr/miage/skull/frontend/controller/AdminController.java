package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.ApiException;
import fr.miage.skull.frontend.client.PlayerApiClient;
import fr.miage.skull.frontend.web.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Interface d'administration : liste des joueurs, bannir / débannir.
 * Le contrôle ici sert à l'affichage ; la vraie protection est celle du player-service (403).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PlayerApiClient api;

    public AdminController(PlayerApiClient api) {
        this.api = api;
    }

    private boolean isAdmin(HttpSession session) {
        return session.getAttribute(SessionKeys.ROLES) instanceof List<?> roles && roles.contains("ADMIN");
    }

    private String token(HttpSession session) {
        return (String) session.getAttribute(SessionKeys.TOKEN);
    }

    private String message(ApiException e) {
        return switch (e.getStatus()) {
            case 404 -> "Joueur introuvable.";
            case 503 -> "Le serveur est injoignable, réessaie plus tard.";
            default -> e.getMessage() != null ? e.getMessage() : "Erreur inattendue (" + e.getStatus() + ").";
        };
    }

    @GetMapping
    public String players(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard?forbidden";
        }
        try {
            model.addAttribute("players", api.listPlayers(token(session)));
        } catch (ApiException e) {
            if (e.getStatus() == 401) {              // token expiré ou compte banni
                session.invalidate();
                return "redirect:/login?expired";
            }
            if (e.getStatus() == 403) {
                return "redirect:/dashboard?forbidden";
            }
            model.addAttribute("error", message(e));
        }
        model.addAttribute("selfId", session.getAttribute(SessionKeys.PLAYER_ID));
        return "admin";
    }

    @PostMapping("/players/{id}/ban")
    public String ban(@PathVariable Long id, @RequestParam boolean banned,
                      HttpSession session, RedirectAttributes redirect) {
        if (!isAdmin(session)) {
            return "redirect:/dashboard?forbidden";
        }
        try {
            api.setBanned(id, banned, token(session));
            redirect.addFlashAttribute("success", banned ? "Joueur banni." : "Joueur débanni.");
        } catch (ApiException e) {
            if (e.getStatus() == 401) {
                session.invalidate();
                return "redirect:/login?expired";
            }
            redirect.addFlashAttribute("error", e.getStatus() == 403
                    ? "Action refusée par le serveur." : message(e));
        }
        return "redirect:/admin";
    }
}
