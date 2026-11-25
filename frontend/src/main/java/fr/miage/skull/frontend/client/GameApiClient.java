package fr.miage.skull.frontend.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Toutes les requêtes HTTP vers le game-service passent par cette classe.
 * Le jeton reste côté serveur : il est lu dans la session et envoyé dans l'en-tête Authorization.
 */
@Service
public class GameApiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<String>> STRINGS_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient client;

    public GameApiClient(@Qualifier("gameRestClient") RestClient gameRestClient) {
        this.client = gameRestClient;
    }

    /** GET /api/v1/games?status=... : parties ayant ce statut (OPEN, IN_PROGRESS, FINISHED). */
    public List<Map<String, Object>> listGames(String status, String token) {
        return call(() -> client.get()
                .uri("/api/v1/games?status={status}", status)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(LIST_TYPE));
    }

    /** GET /api/v1/games/{id} : la partie vue par le joueur connecté (disques des autres masqués). */
    public Map<String, Object> getGame(Long id, String token) {
        return call(() -> client.get()
                .uri("/api/v1/games/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(MAP_TYPE));
    }

    /** GET /api/v1/games/themes : motifs de disques autorisés. */
    public List<String> listThemes(String token) {
        return call(() -> client.get()
                .uri("/api/v1/games/themes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(STRINGS_TYPE));
    }

    /** POST /api/v1/games : crée la partie ; le créateur y est inscrit automatiquement. */
    public Map<String, Object> createGame(int maxPlayers, int aiCount, String flowerTheme, String skullTheme,
                                          String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("maxPlayers", maxPlayers);
        body.put("aiCount", aiCount);
        body.put("flowerTheme", flowerTheme);
        body.put("skullTheme", skullTheme);
        return call(() -> client.post()
                .uri("/api/v1/games")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(MAP_TYPE));
    }

    /** POST /api/v1/games/{id}/join : motifs absents = tirés au hasard par le serveur. */
    public Map<String, Object> joinGame(Long id, String flowerTheme, String skullTheme, String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("flowerTheme", flowerTheme);
        body.put("skullTheme", skullTheme);
        return call(() -> client.post()
                .uri("/api/v1/games/{id}/join", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(MAP_TYPE));
    }

    /** POST /api/v1/games/{id}/leave : renvoie null si la partie ouverte a été supprimée (204). */
    public Map<String, Object> leaveGame(Long id, String token) {
        return call(() -> client.post()
                .uri("/api/v1/games/{id}/leave", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(MAP_TYPE));
    }

    /** POST /api/v1/games/{id}/start : réservé au créateur, au moins 3 participants. */
    public Map<String, Object> startGame(Long id, String token) {
        return call(() -> client.post()
                .uri("/api/v1/games/{id}/start", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(MAP_TYPE));
    }

    /** POST /api/v1/games/{id}/moves : type PLACE_DISC, BID, PASS ou REVEAL et ses paramètres. */
    public Map<String, Object> playMove(Long id, Map<String, Object> move, String token) {
        return call(() -> client.post()
                .uri("/api/v1/games/{id}/moves", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(move)
                .retrieve()
                .body(MAP_TYPE));
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw toApiException(e);
        } catch (ResourceAccessException e) {
            throw new ApiException(503, "Service indisponible", Map.of());
        }
    }

    private ApiException toApiException(RestClientResponseException e) {
        Map<String, String> fields = new HashMap<>();
        String detail = null;
        try {
            Map<String, Object> body = e.getResponseBodyAs(MAP_TYPE);
            if (body != null) {
                Object d = body.get("detail");
                if (d != null) {
                    detail = d.toString();
                }
                if (body.get("invalidParams") instanceof Map<?, ?> params) {
                    params.forEach((k, v) -> fields.put(String.valueOf(k), String.valueOf(v)));
                }
            }
        } catch (Exception ignored) {
            // corps de réponse illisible : on garde seulement le code HTTP
        }
        return new ApiException(e.getStatusCode().value(), detail, fields);
    }
}
