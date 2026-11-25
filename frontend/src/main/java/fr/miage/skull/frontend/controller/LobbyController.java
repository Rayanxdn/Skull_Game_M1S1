package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.ApiException;
import fr.miage.skull.frontend.client.GameApiClient;
import fr.miage.skull.frontend.form.CreateGameForm;
import fr.miage.skull.frontend.web.GameErrors;
import fr.miage.skull.frontend.web.GameTable;
import fr.miage.skull.frontend.web.GameViews;
import fr.miage.skull.frontend.web.SessionKeys;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Salon : créer une partie, la voir, la rejoindre, la quitter, la lancer.
 * Chaque action correspond à un endpoint du game-service, qui décide de tout (droits, règles) ;
 * les actions qui modifient une partie sont toujours des POST (R24).
 */
@Controller
@RequestMapping("/games")
public class LobbyController {

    private static final Set<String> FORM_FIELDS = Set.of("maxPlayers", "aiCount", "flowerTheme", "skullTheme");

    private final GameApiClient games;

    public LobbyController(GameApiClient games) {
        this.games = games;
    }

    private String token(HttpSession session) {
        return (String) session.getAttribute(SessionKeys.TOKEN);
    }

    private String expired(HttpSession session) {
        session.invalidate();
        return "redirect:/login?expired";
    }

    @GetMapping("/new")
    public String newGame(HttpSession session, Model model) {
        model.addAttribute("createGameForm", new CreateGameForm());
        return creationPage(session, model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("createGameForm") CreateGameForm form, BindingResult result,
                         HttpSession session, Model model) {
        if (form.getMaxPlayers() != null && form.getAiCount() != null && form.getAiCount() >= form.getMaxPlayers()) {
            result.rejectValue("aiCount", "tooMany", "Il faut laisser au moins une place à un joueur humain");
        }
        if (result.hasErrors()) {
            return creationPage(session, model);
        }
        try {
            Map<String, Object> game = games.createGame(form.getMaxPlayers(), form.getAiCount(),
                    blankToNull(form.getFlowerTheme()), blankToNull(form.getSkullTheme()), token(session));
            return "redirect:/games/" + GameViews.asLong(game.get("id"));
        } catch (ApiException e) {
            if (GameErrors.sessionExpired(e)) {
                return expired(session);
            }
            e.getFieldErrors().forEach((field, message) -> {
                if (FORM_FIELDS.contains(field)) {
                    result.rejectValue(field, "api", message);
                }
            });
            if (e.getFieldErrors().isEmpty()) {
                model.addAttribute("error", GameErrors.message(e));
            }
            return creationPage(session, model);
        }
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirect) {
        Map<String, Object> game;
        try {
            game = games.getGame(id, token(session));
        } catch (ApiException e) {
            if (GameErrors.sessionExpired(e)) {
                return expired(session);
            }
            redirect.addFlashAttribute("error", GameErrors.message(e));
            return "redirect:/dashboard";
        }
        Long me = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        Map<String, Object> mySeat = GameViews.seatOf(game, me);
        int seats = GameViews.players(game).size();

        model.addAttribute("game", game);
        model.addAttribute("mySeat", mySeat);
        model.addAttribute("isParticipant", mySeat != null);
        model.addAttribute("isCreator", me != null && me.equals(GameViews.asLong(game.get("creatorId"))));
        model.addAttribute("canStart", seats >= 3);
        model.addAttribute("isFull", seats >= GameViews.asInt(game.get("maxPlayers")));
        model.addAttribute("themeLabels", GameViews.THEME_LABELS);

        if ("OPEN".equals(game.get("status"))) {
            model.addAttribute("themes", themes(session));
            return "waiting-room";
        }
        model.addAllAttributes(GameTable.build(game, me));
        return "game";
    }

    @PostMapping("/{id}/join")
    public String join(@PathVariable Long id,
                       @RequestParam(required = false) String flowerTheme,
                       @RequestParam(required = false) String skullTheme,
                       HttpSession session, RedirectAttributes redirect) {
        try {
            games.joinGame(id, blankToNull(flowerTheme), blankToNull(skullTheme), token(session));
            redirect.addFlashAttribute("success", "Tu as rejoint la partie.");
        } catch (ApiException e) {
            if (GameErrors.sessionExpired(e)) {
                return expired(session);
            }
            redirect.addFlashAttribute("error", GameErrors.message(e));
        }
        return "redirect:/games/" + id;
    }

    @PostMapping("/{id}/leave")
    public String leave(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        try {
            Map<String, Object> after = games.leaveGame(id, token(session));
            boolean wasStarted = after != null && !"OPEN".equals(after.get("status"));
            redirect.addFlashAttribute("success", wasStarted
                    ? "Tu as quitté la partie : une IA prend ta place et la partie est comptée comme perdue."
                    : "Tu as quitté la partie.");
            return "redirect:/dashboard";
        } catch (ApiException e) {
            if (GameErrors.sessionExpired(e)) {
                return expired(session);
            }
            redirect.addFlashAttribute("error", GameErrors.message(e));
            return "redirect:/games/" + id;
        }
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id, HttpSession session, RedirectAttributes redirect) {
        try {
            games.startGame(id, token(session));
        } catch (ApiException e) {
            if (GameErrors.sessionExpired(e)) {
                return expired(session);
            }
            redirect.addFlashAttribute("error", GameErrors.message(e));
        }
        return "redirect:/games/" + id;
    }

    private String creationPage(HttpSession session, Model model) {
        model.addAttribute("themes", themes(session));
        model.addAttribute("themeLabels", GameViews.THEME_LABELS);
        return "game-new";
    }

    /** Sans la liste (service injoignable), le formulaire propose seulement « au hasard ». */
    private List<String> themes(HttpSession session) {
        try {
            return games.listThemes(token(session));
        } catch (ApiException e) {
            return List.of();
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
