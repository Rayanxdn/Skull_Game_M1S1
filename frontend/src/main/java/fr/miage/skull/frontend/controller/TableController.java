package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.ApiException;
import fr.miage.skull.frontend.client.GameApiClient;
import fr.miage.skull.frontend.client.PlayerApiClient;
import fr.miage.skull.frontend.web.GameErrors;
import fr.miage.skull.frontend.web.GameViews;
import fr.miage.skull.frontend.web.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Actions de la table de jeu : jouer un coup et signaler un joueur.
 * Les valeurs reçues du navigateur sont contrôlées ici (R18), puis le game-service applique les règles.
 */
@Controller
@RequestMapping("/games/{id}")
public class TableController {

    private static final Set<String> MOVES = Set.of("PLACE_DISC", "BID", "PASS", "REVEAL");
    private static final Set<String> DISCS = Set.of("FLOWER", "SKULL");
    static final int REASON_MAX = 300;

    private final GameApiClient games;
    private final PlayerApiClient players;

    public TableController(GameApiClient games, PlayerApiClient players) {
        this.games = games;
        this.players = players;
    }

    @PostMapping("/move")
    public String move(@PathVariable Long id,
                       @RequestParam String type,
                       @RequestParam(required = false) String discType,
                       @RequestParam(required = false) Integer bidAmount,
                       @RequestParam(required = false) Long targetPlayerId,
                       HttpSession session, RedirectAttributes redirect) {
        if (!MOVES.contains(type) || (discType != null && !DISCS.contains(discType))) {
            redirect.addFlashAttribute("error", "Coup inconnu.");
            return "redirect:/games/" + id;
        }
        Map<String, Object> move = new HashMap<>();
        move.put("type", type);
        move.put("discType", discType);
        move.put("bidAmount", bidAmount);
        move.put("targetPlayerId", targetPlayerId);
        try {
            games.playMove(id, move, token(session));
        } catch (ApiException e) {
            if (GameErrors.sessionExpired(e)) {
                return expired(session);
            }
            redirect.addFlashAttribute("error", GameErrors.message(e));
        }
        return "redirect:/games/" + id;
    }

    /** Le formulaire envoie un siège ; le compte à signaler est retrouvé dans la partie, jamais pris du navigateur. */
    @PostMapping("/report")
    public String report(@PathVariable Long id,
                         @RequestParam Long participantId,
                         @RequestParam(required = false) String reason,
                         HttpSession session, RedirectAttributes redirect) {
        String text = reason == null ? "" : reason.strip();
        if (text.isEmpty() || text.length() > REASON_MAX) {
            redirect.addFlashAttribute("error", "Indique un motif de signalement (" + REASON_MAX + " caractères au plus).");
            return "redirect:/games/" + id;
        }
        Long me = (Long) session.getAttribute(SessionKeys.PLAYER_ID);
        try {
            Map<String, Object> game = games.getGame(id, token(session));
            Map<String, Object> seat = GameViews.players(game).stream()
                    .filter(p -> participantId.equals(GameViews.asLong(p.get("participantId"))))
                    .findFirst()
                    .orElse(null);
            Long account = seat == null ? null : GameViews.asLong(seat.get("playerId"));
            if (seat == null || Boolean.TRUE.equals(seat.get("ai")) || account == null || account.equals(me)
                    || !GameViews.tookPart(game, me)) {
                redirect.addFlashAttribute("error", "Ce joueur ne peut pas être signalé.");
                return "redirect:/games/" + id;
            }
            players.reportPlayer(account, text, id, token(session));
            redirect.addFlashAttribute("success", "Signalement envoyé. Un administrateur va l'examiner ; la partie continue.");
        } catch (ApiException e) {
            if (GameErrors.sessionExpired(e)) {
                return expired(session);
            }
            redirect.addFlashAttribute("error", e.getStatus() == 409
                    ? "Tu as déjà signalé ce joueur pour cette partie."
                    : GameErrors.message(e));
        }
        return "redirect:/games/" + id;
    }

    private String token(HttpSession session) {
        return (String) session.getAttribute(SessionKeys.TOKEN);
    }

    private String expired(HttpSession session) {
        session.invalidate();
        return "redirect:/login?expired";
    }
}
