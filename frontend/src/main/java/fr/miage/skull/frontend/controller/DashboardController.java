package fr.miage.skull.frontend.controller;

import fr.miage.skull.frontend.client.ApiException;
import fr.miage.skull.frontend.client.GameApiClient;
import fr.miage.skull.frontend.web.GameErrors;
import fr.miage.skull.frontend.web.GameViews;
import fr.miage.skull.frontend.web.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tableau de bord : parties ouvertes, mes parties en cours et mes parties terminées, lues dans le game-service.
 * Les statistiques sont calculées à partir des parties terminées (le classement complet arrive au Lot 3).
 */
@Controller
public class DashboardController {

    private final GameApiClient games;

    public DashboardController(GameApiClient games) {
        this.games = games;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String forbidden,
                            HttpSession session, Model model) {
        model.addAttribute("pseudo", session.getAttribute(SessionKeys.PSEUDO));
        if (forbidden != null) {
            model.addAttribute("error", "Cette page est réservée aux administrateurs.");
        }
        String token = (String) session.getAttribute(SessionKeys.TOKEN);
        Long me = (Long) session.getAttribute(SessionKeys.PLAYER_ID);

        List<Map<String, Object>> open = List.of();
        List<Map<String, Object>> mine = List.of();
        List<Map<String, Object>> finished = List.of();
        try {
            open = games.listGames("OPEN", token).stream().map(g -> openRow(g, me)).toList();
            mine = games.listGames("IN_PROGRESS", token).stream()
                    .filter(g -> GameViews.seatOf(g, me) != null)
                    .map(g -> openRow(g, me))
                    .toList();
            finished = games.listGames("FINISHED", token).stream()
                    .filter(g -> GameViews.tookPart(g, me))
                    .map(g -> historyRow(g, me))
                    .toList();
        } catch (ApiException e) {
            if (GameErrors.sessionExpired(e)) {
                session.invalidate();
                return "redirect:/login?expired";
            }
            model.addAttribute("error", "Impossible de charger les parties. " + GameErrors.message(e));
        }

        long wins = finished.stream().filter(h -> Boolean.TRUE.equals(h.get("won"))).count();
        model.addAttribute("games", open);
        model.addAttribute("myGames", mine);
        model.addAttribute("history", finished);
        model.addAttribute("stats", Map.of(
                "games", finished.size(),
                "wins", wins,
                "ratio", finished.isEmpty() ? 0 : Math.round(wins * 100.0 / finished.size())));
        return "dashboard";
    }

    private Map<String, Object> openRow(Map<String, Object> game, Long me) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", GameViews.asLong(game.get("id")));
        row.put("creator", GameViews.creatorName(game));
        row.put("players", GameViews.players(game).size());
        row.put("max", GameViews.asInt(game.get("maxPlayers")));
        row.put("ai", GameViews.asInt(game.get("aiCount")));
        row.put("joined", GameViews.seatOf(game, me) != null);
        return row;
    }

    private Map<String, Object> historyRow(Map<String, Object> game, Long me) {
        Map<String, Object> seat = GameViews.seatOfAccount(game, me);
        boolean forfeited = seat != null && Boolean.TRUE.equals(seat.get("forfeited"));
        boolean won = !forfeited && me.equals(GameViews.asLong(game.get("winnerId")));
        Map<String, Object> row = new HashMap<>();
        row.put("id", GameViews.asLong(game.get("id")));
        row.put("date", GameViews.date(game.get("createdAt")));
        row.put("won", won);
        row.put("result", won ? "Victoire" : forfeited ? "Abandon" : "Défaite");
        row.put("points", seat != null ? GameViews.asInt(seat.get("points")) : 0);
        return row;
    }
}
