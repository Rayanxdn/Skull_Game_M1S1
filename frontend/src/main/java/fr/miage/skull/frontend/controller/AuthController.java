package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.ApiException;
import fr.miage.skull.frontend.client.PlayerApiClient;
import fr.miage.skull.frontend.form.LoginForm;
import fr.miage.skull.frontend.form.RegisterForm;
import fr.miage.skull.frontend.web.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class AuthController {

    private static final Set<String> REGISTER_FIELDS = Set.of("pseudo", "email", "password");

    private final PlayerApiClient api;

    public AuthController(PlayerApiClient api) {
        this.api = api;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String expired,
                            @RequestParam(required = false) String logout,
                            HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionKeys.TOKEN) != null) {
            return "redirect:/dashboard";
        }
        if (expired != null) {
            model.addAttribute("info", "Ta session a expiré, reconnecte-toi.");
        }
        if (logout != null) {
            model.addAttribute("success", "Tu es déconnecté.");
        }
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginForm form, BindingResult result,
                        HttpServletRequest request, Model model) {
        if (result.hasErrors()) {
            return "login";
        }
        try {
            Map<String, Object> res = api.login(form.getEmail(), form.getPassword());

            HttpSession session = request.getSession(true);
            request.changeSessionId(); // évite la fixation de session
            session.setAttribute(SessionKeys.TOKEN, String.valueOf(res.get("token")));
            session.setAttribute(SessionKeys.PLAYER_ID, ((Number) res.get("id")).longValue());
            session.setAttribute(SessionKeys.PSEUDO, String.valueOf(res.get("pseudo")));
            session.setAttribute(SessionKeys.ROLES, toStringList(res.get("roles")));
            return "redirect:/dashboard";
        } catch (ApiException e) {
            model.addAttribute("error", loginMessage(e.getStatus()));
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form, BindingResult result,
                           RedirectAttributes redirect, Model model) {
        if (form.getPassword() != null && form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Les mots de passe ne correspondent pas");
        }
        if (result.hasErrors()) {
            return "register";
        }
        try {
            api.register(form.getPseudo(), form.getEmail(), form.getPassword());
            redirect.addFlashAttribute("success", "Compte créé. Tu peux te connecter.");
            return "redirect:/login";
        } catch (ApiException e) {
            if (e.getStatus() == 409) {
                result.rejectValue("email", "taken", "Cet email est déjà utilisé");
            } else if (e.getStatus() == 400 && !e.getFieldErrors().isEmpty()) {
                e.getFieldErrors().forEach((field, message) -> {
                    if (REGISTER_FIELDS.contains(field)) {
                        result.rejectValue(field, "api", message);
                    }
                });
            } else if (e.getStatus() == 503) {
                model.addAttribute("error", "Le serveur est injoignable, réessaie plus tard.");
            } else {
                model.addAttribute("error", "Erreur inattendue (" + e.getStatus() + ").");
            }
            return "register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    private String loginMessage(int status) {
        return switch (status) {
            case 401 -> "Email ou mot de passe incorrect.";
            case 403 -> "Ce compte a été banni.";
            case 503 -> "Le serveur est injoignable, réessaie plus tard.";
            default -> "Erreur inattendue (" + status + ").";
        };
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
