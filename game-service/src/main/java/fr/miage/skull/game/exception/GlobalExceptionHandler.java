package fr.miage.skull.game.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Intercepteur global des exceptions de {@code game-service}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gère les exceptions lorsqu'une entité demandée (ex. une partie) est introuvable en base de données.
     *
     * @param ex l'exception {@link EntityNotFoundException} levée
     * @return un objet {@link ProblemDetail} avec le statut HTTP 404 (Not Found)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Ressource Introuvable");
        problem.setType(URI.create("https://api.skull.miage.fr/errors/not-found"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Gère les exceptions lorsqu'un utilisateur authentifié tente une action qui ne lui est pas autorisée
     * (ex. démarrer une partie dont il n'est pas le créateur ou jouer dans une partie à laquelle il ne participe pas).
     *
     * @param ex l'exception {@link AccessDeniedException} levée
     * @return un objet {@link ProblemDetail} avec le statut HTTP 403 (Forbidden)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Accès Refusé");
        problem.setType(URI.create("https://api.skull.miage.fr/errors/forbidden"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Gère les exceptions liées à des coups contraires aux règles du jeu ou à des arguments métier invalides.
     *
     * @param ex l'exception {@link IllegalMoveException} ou {@link IllegalArgumentException} levée
     * @return un objet {@link ProblemDetail} avec le statut HTTP 400 (Bad Request)
     */
    @ExceptionHandler({
            IllegalMoveException.class,
            IllegalArgumentException.class
    })
    public ProblemDetail handleBadRequestException(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Action Invalide");
        problem.setType(URI.create("https://api.skull.miage.fr/errors/bad-request"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Gère les exceptions lorsqu'une opération est incompatible avec l'état courant de la partie
     * (ex. rejoindre une partie complète ou déjà lancée, démarrer avec moins de 3 joueurs).
     *
     * @param ex l'exception {@link IllegalStateException} levée
     * @return un objet {@link ProblemDetail} avec le statut HTTP 409 (Conflict)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleConflictStateException(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("État de Partie Incompatible");
        problem.setType(URI.create("https://api.skull.miage.fr/errors/conflict"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Gère les erreurs de validation des corps de requêtes annotés avec {@code @Valid}.
     *
     * @param ex l'exception {@link MethodArgumentNotValidException} contenant les erreurs par champ
     * @return un objet {@link ProblemDetail} avec le statut HTTP 400 (Bad Request) et la liste des champs invalides
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "La requête contient des champs invalides"
        );
        problem.setTitle("Erreur de validation");
        problem.setType(URI.create("https://api.skull.miage.fr/errors/validation"));

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        problem.setProperty("invalidParams", errors);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Filet de sécurité capturant toute exception technique non prévue afin d'éviter
     * d'exposer des traces internes au client.
     *
     * @param ex l'exception générique non interceptée
     * @return un objet {@link ProblemDetail} avec le statut HTTP 500 (Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUncaughtException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue sur le serveur"
        );
        problem.setTitle("Erreur Interne");
        problem.setType(URI.create("https://api.skull.miage.fr/errors/internal-server-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}