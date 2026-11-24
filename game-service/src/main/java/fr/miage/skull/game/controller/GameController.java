package fr.miage.skull.game.controller;

import fr.miage.skull.game.dto.CreateGameRequest;
import fr.miage.skull.game.dto.GameViewResponse;
import fr.miage.skull.game.dto.JoinGameRequest;
import fr.miage.skull.game.dto.MoveRequest;
import fr.miage.skull.game.entity.GameStatus;
import fr.miage.skull.game.security.AuthPlayer;
import fr.miage.skull.game.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST exposant les endpoints de création, de gestion et de déroulement des parties de Skull.
 * <p>
 * Toutes les routes sont accessibles sous le préfixe {@code /api/v1/games} et requièrent une authentification
 * via jeton JWT ({@link AuthPlayer}).
 */
@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "Game Service", description = "Gestion des parties")
@SecurityRequirement(name = "bearerAuth")
public class GameController {

    private final GameService gameService;

    /**
     * Construit le contrôleur avec son service métier dédié.
     *
     * @param gameService service orchestrant la logique des parties de Skull
     */
    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Crée une nouvelle partie ouverte ({@link GameStatus#OPEN}) et y inscrit automatiquement le joueur authentifié.
     *
     * @param request paramètres de configuration de la partie (nombre max de joueurs, nombre d'IA, thèmes)
     * @param player  joueur authentifié extrait du jeton JWT
     * @return une {@link ResponseEntity} avec le statut HTTP 201 (Created) et la vue filtrée de la partie créée
     */
    @PostMapping
    @Operation(summary = "Créer une partie")
    @ApiResponse(responseCode = "201", description = "Partie créée avec succès")
    @ApiResponse(responseCode = "400", description = "Paramètres de partie invalides")
    public ResponseEntity<GameViewResponse> createGame(
            @Valid @RequestBody CreateGameRequest request,
            @AuthenticationPrincipal AuthPlayer player
    ) {
        GameViewResponse created = gameService.createGame(request, player.id(), resolveDisplayName(player));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Liste toutes les parties correspondant au statut demandé ({@link GameStatus#OPEN} par défaut).
     *
     * @param status statut de filtrage des parties (optionnel, vaut {@code OPEN} par défaut)
     * @param player joueur authentifié consultant la liste
     * @return une {@link ResponseEntity} avec le statut HTTP 200 (OK) et la liste des vues filtrées
     */
    @GetMapping
    @Operation(summary = "Lister les parties")
    @ApiResponse(responseCode = "200", description = "Liste des parties récupérée avec succès")
    public ResponseEntity<List<GameViewResponse>> listGames(
            @RequestParam(required = false, defaultValue = "OPEN") GameStatus status,
            @AuthenticationPrincipal AuthPlayer player
    ) {
        List<GameViewResponse> games = gameService.listGames(status, player.id());
        return ResponseEntity.ok(games);
    }

    /**
     * Retourne l'état courant d'une partie filtré selon l'identité du joueur connecté
     * (les disques cachés des autres participants restent masqués).
     *
     * @param id     identifiant unique de la partie
     * @param player joueur authentifié demandant l'état de la partie
     * @return une {@link ResponseEntity} avec le statut HTTP 200 (OK) et la vue filtrée de la partie
     */
    @GetMapping("/{id}")
    @Operation(summary = "Afficher une partie")
    @ApiResponse(responseCode = "200", description = "État de la partie récupéré avec succès")
    @ApiResponse(responseCode = "404", description = "Partie non trouvée")
    public ResponseEntity<GameViewResponse> getGame(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPlayer player
    ) {
        GameViewResponse gameView = gameService.getGame(id, player.id());
        return ResponseEntity.ok(gameView);
    }

    /**
     * Inscrit le joueur authentifié dans une partie ouverte.
     *
     * @param id      identifiant unique de la partie à rejoindre
     * @param request choix optionnels de thèmes visuels pour les disques du joueur
     * @param player  joueur authentifié souhaitant rejoindre la table
     * @return une {@link ResponseEntity} avec le statut HTTP 200 (OK) et la vue mise à jour de la partie
     */
    @PostMapping("/{id}/join")
    @Operation(summary = "Rejoindre une partie")
    @ApiResponse(responseCode = "200", description = "Joueur inscrit dans la partie avec succès")
    @ApiResponse(responseCode = "404", description = "Partie non trouvée")
    @ApiResponse(responseCode = "409", description = "Partie complète ou déjà démarrée")
    public ResponseEntity<GameViewResponse> joinGame(
            @PathVariable Long id,
            @RequestBody(required = false) JoinGameRequest request,
            @AuthenticationPrincipal AuthPlayer player
    ) {
        GameViewResponse gameView = gameService.joinGame(id, request, player.id(), resolveDisplayName(player));
        return ResponseEntity.ok(gameView);
    }

    /**
     * Permet au joueur authentifié de quitter une partie avant ou pendant son déroulement.
     * <p>
     * Si la partie était ouverte et qu'il ne reste plus aucun joueur humain, la partie est supprimée
     * et une réponse vide 204 (No Content) est retournée.
     *
     * @param id     identifiant unique de la partie à quitter
     * @param player joueur authentifié quittant la table
     * @return une {@link ResponseEntity} 200 (OK) avec la vue mise à jour, ou 204 (No Content) si la partie a été supprimée
     */
    @PostMapping("/{id}/leave")
    @Operation(summary = "Quitter une partie")
    @ApiResponse(responseCode = "200", description = "Départ pris en compte (siège confié à une IA si partie en cours)")
    @ApiResponse(responseCode = "204", description = "Partie ouverte supprimée faute de joueurs humains restants")
    @ApiResponse(responseCode = "404", description = "Partie non trouvée")
    public ResponseEntity<GameViewResponse> leaveGame(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPlayer player
    ) {
        GameViewResponse updated = gameService.leaveGame(id, player.id());
        if (updated == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * Démarre une partie en attente (action réservée au créateur avec au moins 3 participants).
     *
     * @param id     identifiant unique de la partie à démarrer
     * @param player joueur authentifié demandant le lancement de la partie
     * @return une {@link ResponseEntity} avec le statut HTTP 200 (OK) et l'état initial de la partie lancée
     */
    @PostMapping("/{id}/start")
    @Operation(summary = "Démarrer une partie")
    @ApiResponse(responseCode = "200", description = "Partie démarrée avec succès")
    @ApiResponse(responseCode = "403", description = "Seul le créateur de la partie peut la démarrer")
    @ApiResponse(responseCode = "404", description = "Partie non trouvée")
    @ApiResponse(responseCode = "409", description = "Nombre de joueurs insuffisant ou partie déjà démarrée")
    public ResponseEntity<GameViewResponse> startGame(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthPlayer player
    ) {
        GameViewResponse gameView = gameService.startGame(id, player.id());
        return ResponseEntity.ok(gameView);
    }

    /**
     * Joue une action (pose de disque, enchère, retrait ou révélation) au tour du joueur authentifié,
     * puis enchaîne automatiquement les tours des éventuels joueurs IA suivants.
     *
     * @param id      identifiant unique de la partie
     * @param request détails du coup joué par le joueur
     * @param player  joueur authentifié effectuant l'action
     * @return une {@link ResponseEntity} avec le statut HTTP 200 (OK) et l'état mis à jour de la partie
     */
    @PostMapping("/{id}/moves")
    @Operation(summary = "Jouer un coup")
    @ApiResponse(responseCode = "200", description = "Coup appliqué avec succès")
    @ApiResponse(responseCode = "400", description = "Coup illégal ou hors tour")
    @ApiResponse(responseCode = "403", description = "Le joueur ne participe pas à cette partie")
    @ApiResponse(responseCode = "404", description = "Partie non trouvée")
    public ResponseEntity<GameViewResponse> playMove(
            @PathVariable Long id,
            @Valid @RequestBody MoveRequest request,
            @AuthenticationPrincipal AuthPlayer player
    ) {
        GameViewResponse gameView = gameService.playMove(id, request, player.id());
        return ResponseEntity.ok(gameView);
    }

    /**
     * Déduit un nom d'affichage lisible à partir de l'adresse e-mail contenue dans le jeton JWT,
     * ou génère un libellé par défaut basé sur l'identifiant du joueur.
     *
     * @param player joueur authentifié issu du contexte de sécurité
     * @return le pseudonyme déduit pour l'affichage autour de la table
     */
    private String resolveDisplayName(AuthPlayer player) {
        if (player.email() != null && player.email().contains("@")) {
            return player.email().substring(0, player.email().indexOf('@'));
        }
        return "Joueur #" + player.id();
    }
}