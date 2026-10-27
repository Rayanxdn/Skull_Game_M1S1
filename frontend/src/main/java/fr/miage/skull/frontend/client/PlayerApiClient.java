package fr.miage.skull.frontend.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class PlayerApiClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient client;

    public PlayerApiClient(RestClient playerRestClient) {
        this.client = playerRestClient;
    }

    /** POST /api/v1/auth/login : renvoie token, id, pseudo, roles. */
    public Map<String, Object> login(String email, String password) {
        return call(() -> client.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password))
                .retrieve()
                .body(MAP_TYPE));
    }

    /** POST /api/v1/players/register : renvoie le joueur créé (201). */
    public Map<String, Object> register(String pseudo, String email, String password) {
        return call(() -> client.post()
                .uri("/api/v1/players/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("pseudo", pseudo, "email", email, "password", password))
                .retrieve()
                .body(MAP_TYPE));
    }

    /** GET /api/v1/players/{id} : route protégée, le token part dans le header Authorization. */
    public Map<String, Object> getPlayer(Long id, String token) {
        return call(() -> client.get()
                .uri("/api/v1/players/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
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
