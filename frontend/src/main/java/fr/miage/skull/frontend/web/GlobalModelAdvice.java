package fr.miage.skull.frontend.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/** Ajoute à toutes les pages l'état de connexion, utilisé par la barre de navigation. */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute
    public void connectionState(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        boolean loggedIn = session != null && session.getAttribute(SessionKeys.TOKEN) != null;
        model.addAttribute("loggedIn", loggedIn);
        if (loggedIn) {
            model.addAttribute("currentPseudo", session.getAttribute(SessionKeys.PSEUDO));
            Object roles = session.getAttribute(SessionKeys.ROLES);
            model.addAttribute("isAdmin", roles instanceof List<?> l && l.contains("ADMIN"));
        } else {
            model.addAttribute("isAdmin", false);
        }
    }
}
