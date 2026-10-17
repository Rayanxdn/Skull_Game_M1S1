package fr.miage.skull.player.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * AccessDeniedException est une RuntimeException : sans cette classe, le handler
 * RuntimeException de GlobalExceptionHandler la transformerait en 400 au lieu de 403.
 * HIGHEST_PRECEDENCE la fait passer en premier.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Vous n'avez pas le droit d'effectuer cette action");
        problem.setTitle("Accès refusé");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
