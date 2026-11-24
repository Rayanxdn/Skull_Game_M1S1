package fr.miage.skull.game.service;

import fr.miage.skull.game.ai.AiAction;
import fr.miage.skull.game.ai.AiStrategy;
import fr.miage.skull.game.ai.AiView;
import fr.miage.skull.game.ai.Disc;
import fr.miage.skull.game.ai.StandardAiStrategy;
import fr.miage.skull.game.dto.CreateGameRequest;
import fr.miage.skull.game.dto.GameViewResponse;
import fr.miage.skull.game.dto.JoinGameRequest;
import fr.miage.skull.game.dto.MoveRequest;
import fr.miage.skull.game.engine.DiscType;
import fr.miage.skull.game.engine.GameEngine;
import fr.miage.skull.game.engine.GamePhase;
import fr.miage.skull.game.engine.GameState;
import fr.miage.skull.game.exception.IllegalMoveException;
import fr.miage.skull.game.engine.Move;
import fr.miage.skull.game.engine.MoveType;
import fr.miage.skull.game.engine.PlayerState;
import fr.miage.skull.game.entity.Game;
import fr.miage.skull.game.entity.GameParticipant;
import fr.miage.skull.game.entity.GameStatus;
import fr.miage.skull.game.mapper.GameMapper;
import fr.miage.skull.game.repository.GameRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Service métier orchestrant le cycle de vie des parties de Skull.
 * <p>
 * Assure la coordination entre la persistance JPA ({@link GameRepository}),
 * le moteur de règles pur ({@link GameEngine}), la stratégie des joueurs artificiels
 * ({@link AiStrategy}) et le filtrage anti-triche des vues ({@link GameMapper}).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GameService {

    /** Thème visuel par défaut appliqué lorsqu'aucun motif n'est spécifié. */
    private static final String DEFAULT_THEME = "CLASSIC";

    /** Plafond de sécurité empêchant toute boucle infinie lors de l'enchaînement des tours d'IA. (Dans RunAiTurn) */
    private static final int MAX_AI_CHAIN_STEPS = 200;

    private final GameRepository gameRepository;
    private final GameEngine gameEngine;
    private final GameMapper gameMapper;

    private final AiStrategy aiStrategy = new StandardAiStrategy();
    private final Random random = new SecureRandom();

    /**
     * Crée une nouvelle partie en attente ({@link GameStatus#OPEN}), y inscrit automatiquement
     * le joueur créateur en première position puis ajoute les joueurs IA demandés.
     *
     * @param request            paramètres de création de la partie (nombre max de joueurs, nombre d'IA, thèmes)
     * @param creatorId          identifiant unique du joueur créateur extrait du jeton JWT
     * @param creatorDisplayName pseudonyme affiché du créateur
     * @return la vue filtrée de la partie nouvellement créée pour le créateur
     * @throws IllegalArgumentException si le nombre de joueurs ou d'IA ne respecte pas les règles (3 à 6 joueurs)
     */
    public GameViewResponse createGame(CreateGameRequest request, Long creatorId, String creatorDisplayName) {
        int maxPlayers = request.maxPlayers();
        int aiCount = request.aiCount();

        if (maxPlayers < 3 || maxPlayers > 6) {
            throw new IllegalArgumentException("Le nombre maximum de joueurs doit être compris entre 3 et 6.");
        }
        if (aiCount < 0 || aiCount >= maxPlayers) {
            throw new IllegalArgumentException("Le nombre d'IA doit être compris entre 0 et maxPlayers - 1.");
        }

        Game game = new Game();
        game.setStatus(GameStatus.OPEN);
        game.setMaxPlayers(maxPlayers);
        game.setAiCount(aiCount);
        game.setCreatorId(creatorId);

        GameParticipant creator = buildHumanParticipant(
                creatorId,
                creatorDisplayName != null ? creatorDisplayName : "Joueur #" + creatorId,
                request.flowerTheme(),
                request.skullTheme()
        );
        game.addParticipant(creator);

        for (int i = 0; i < aiCount; i++) {
            game.addParticipant(buildAiParticipant(i + 1));
        }

        Game saved = gameRepository.save(game);
        return gameMapper.toFilteredView(saved, creatorId);
    }

    /**
     * Récupère la liste des parties correspondant au statut demandé, triées de la plus récente à la plus ancienne.
     *
     * @param status         statut filtré ({@link GameStatus#OPEN} par défaut si {@code null})
     * @param viewerPlayerId identifiant du joueur consultant la liste
     * @return liste des vues filtrées des parties correspondantes
     */
    @Transactional(readOnly = true)
    public List<GameViewResponse> listGames(GameStatus status, Long viewerPlayerId) {
        GameStatus targetStatus = status != null ? status : GameStatus.OPEN;
        return gameRepository.findByStatusOrderByCreatedAtDesc(targetStatus).stream()
                .map(game -> gameMapper.toFilteredView(game, viewerPlayerId))
                .toList();
    }

    /**
     * Récupère l'état courant d'une partie filtré selon l'identité du joueur demandeur.
     *
     * @param gameId         identifiant de la partie
     * @param viewerPlayerId identifiant du joueur demandeur (seuls ses propres disques cachés sont visibles)
     * @return la vue filtrée de la partie
     * @throws EntityNotFoundException si aucune partie ne correspond à {@code gameId}
     */
    @Transactional(readOnly = true)
    public GameViewResponse getGame(Long gameId, Long viewerPlayerId) {
        Game game = findGameOrThrow(gameId);
        return gameMapper.toFilteredView(game, viewerPlayerId);
    }

    /**
     * Inscrit un joueur humain dans une partie ouverte ({@link GameStatus#OPEN}).
     * Si le joueur est déjà inscrit, retourne simplement l'état actuel sans doublon.
     *
     * @param gameId      identifiant de la partie à rejoindre
     * @param request     choix optionnels de thèmes visuels pour les fleurs et le crâne
     * @param playerId    identifiant du joueur souhaitant rejoindre
     * @param displayName pseudonyme affiché du joueur
     * @return la vue filtrée de la partie mise à jour
     * @throws EntityNotFoundException si la partie n'existe pas
     * @throws IllegalStateException   si la partie est déjà lancée, terminée ou complète
     */
    public GameViewResponse joinGame(Long gameId, JoinGameRequest request, Long playerId, String displayName) {
        Game game = findGameOrThrow(gameId);

        if (game.getStatus() != GameStatus.OPEN) {
            throw new IllegalStateException("Impossible de rejoindre une partie déjà lancée ou terminée.");
        }

        boolean alreadyJoined = game.getParticipants().stream()
                .anyMatch(p -> !p.isAi() && playerId.equals(p.getPlayerId()));
        if (alreadyJoined) {
            return gameMapper.toFilteredView(game, playerId);
        }

        if (game.getParticipants().size() >= game.getMaxPlayers()) {
            throw new IllegalStateException("La partie est complète.");
        }

        String flower = request != null ? request.flowerTheme() : null;
        String skull = request != null ? request.skullTheme() : null;

        GameParticipant participant = buildHumanParticipant(
                playerId,
                displayName != null ? displayName : "Joueur #" + playerId,
                flower,
                skull
        );
        game.addParticipant(participant);

        Game saved = gameRepository.save(game);
        return gameMapper.toFilteredView(saved, playerId);
    }

    /**
     * Gère le départ d'un joueur humain d'une partie :
     * <ul>
     *   <li>Avant le lancement ({@link GameStatus#OPEN}) : retire le joueur, transfère le rôle de créateur
     *       si nécessaire ou supprime la partie s'il ne reste aucun humain.</li>
     *   <li>En cours de partie ({@link GameStatus#IN_PROGRESS}) : marque le joueur comme ayant déclaré forfait,
     *       confie son siège et ses disques à une IA et déclenche le tour de l'IA si nécessaire.</li>
     * </ul>
     *
     * @param gameId   identifiant de la partie
     * @param playerId identifiant du joueur qui quitte la table
     * @return la vue filtrée mise à jour, ou {@code null} si la partie ouverte a été supprimée faute d'humains
     * @throws EntityNotFoundException  si la partie n'existe pas
     * @throws IllegalArgumentException si le joueur ne participe pas à cette partie
     * @throws IllegalStateException    si la partie est déjà terminée
     */
    public GameViewResponse leaveGame(Long gameId, Long playerId) {
        Game game = findGameOrThrow(gameId);

        GameParticipant leaving = game.getParticipants().stream()
                .filter(p -> !p.isAi() && playerId.equals(p.getPlayerId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Vous ne participez pas à cette partie."));

        if (game.getStatus() == GameStatus.OPEN) {
            game.getParticipants().remove(leaving);

            List<GameParticipant> remainingHumans = game.getParticipants().stream()
                    .filter(p -> !p.isAi())
                    .sorted(Comparator.comparingInt(GameParticipant::getSeatOrder))
                    .toList();

            if (remainingHumans.isEmpty()) {
                gameRepository.delete(game);
                return null;
            }

            if (playerId.equals(game.getCreatorId())) {
                game.setCreatorId(remainingHumans.get(0).getPlayerId());
            }

            reorderSeats(game.getParticipants());
            Game saved = gameRepository.save(game);
            return gameMapper.toFilteredView(saved, playerId);
        }

        if (game.getStatus() == GameStatus.IN_PROGRESS) {
            leaving.setAi(true);
            leaving.setForfeited(true);
            leaving.setDisplayName(leaving.getDisplayName() + " (IA)");

            GameState currentState = gameMapper.deserializeState(game.getStateJson());
            if (currentState != null) {
                String seatId = String.valueOf(leaving.getId());
                List<PlayerState> updatedPlayers = currentState.players().stream()
                        .map(ps -> ps.playerId().equals(seatId)
                                ? new PlayerState(
                                ps.playerId(),
                                leaving.getDisplayName(),
                                true,
                                ps.hand(),
                                ps.stack(),
                                ps.revealedCount(),
                                ps.lostDiscsCount(),
                                ps.points(),
                                ps.passedInRound(),
                                ps.eliminated()
                        )
                                : ps)
                        .toList();

                GameState updatedState = new GameState(
                        currentState.phase(),
                        updatedPlayers,
                        currentState.currentPlayerId(),
                        currentState.challengerId(),
                        currentState.currentBid(),
                        currentState.revealedFlowersCount(),
                        currentState.winnerId()
                );

                updatedState = runAiTurns(updatedState);
                syncGameWithState(game, updatedState);
            }

            Game saved = gameRepository.save(game);
            return gameMapper.toFilteredView(saved, playerId);
        }

        throw new IllegalStateException("La partie est déjà terminée.");
    }

    /**
     * Démarre une partie en attente, initialise l'état via le moteur de règles
     * et exécute les éventuels premiers tours des IA.
     *
     * @param gameId      identifiant de la partie à démarrer
     * @param requesterId identifiant du joueur demandant le démarrage (doit être le créateur)
     * @return la vue filtrée initiale de la partie démarrée
     * @throws EntityNotFoundException si la partie n'existe pas
     * @throws AccessDeniedException   si le demandeur n'est pas le créateur de la partie
     * @throws IllegalStateException   si la partie est déjà lancée ou compte moins de 3 participants
     */
    public GameViewResponse startGame(Long gameId, Long requesterId) {
        Game game = findGameOrThrow(gameId);

        if (game.getStatus() != GameStatus.OPEN) {
            throw new IllegalStateException("La partie a déjà été lancée.");
        }
        if (!requesterId.equals(game.getCreatorId())) {
            throw new AccessDeniedException("Seul le créateur de la partie peut la démarrer.");
        }
        if (game.getParticipants().size() < 3) {
            throw new IllegalStateException("Il faut au moins 3 participants (joueurs et IA) pour démarrer.");
        }

        List<GameParticipant> ordered = game.getParticipants().stream()
                .sorted(Comparator.comparingInt(GameParticipant::getSeatOrder))
                .toList();

        List<GameEngine.PlayerSetup> setups = ordered.stream()
                .map(p -> new GameEngine.PlayerSetup(
                        String.valueOf(p.getId()),
                        p.getDisplayName(),
                        p.isAi()
                ))
                .toList();

        GameState initialState = gameEngine.initGame(setups);
        game.setStatus(GameStatus.IN_PROGRESS);

        GameState afterAi = runAiTurns(initialState);
        syncGameWithState(game, afterAi);

        Game saved = gameRepository.save(game);
        return gameMapper.toFilteredView(saved, requesterId);
    }

    /**
     * Exécute le coup d'un joueur humain via le moteur de règles, puis enchaîne
     * automatiquement les tours des joueurs IA jusqu'au prochain joueur humain ou à la fin de la partie.
     *
     * @param gameId   identifiant de la partie
     * @param request  détails de l'action jouée (type de coup, disque, enchère ou cible)
     * @param playerId identifiant du joueur humain jouant le coup
     * @return la vue filtrée mise à jour après le coup du joueur et les réponses des IA
     * @throws EntityNotFoundException si la partie n'existe pas
     * @throws AccessDeniedException   si le joueur ne participe pas activement à la partie
     * @throws IllegalStateException   si la partie n'est pas en cours ou que son état est invalide
     * @throws IllegalMoveException    si ce n'est pas le tour du joueur ou si le coup viole les règles
     */
    public GameViewResponse playMove(Long gameId, MoveRequest request, Long playerId) {
        Game game = findGameOrThrow(gameId);

        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("La partie n'est pas en cours.");
        }

        GameParticipant participant = game.getParticipants().stream()
                .filter(p -> !p.isAi() && playerId.equals(p.getPlayerId()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Vous ne participez pas activement à cette partie."));

        GameState currentState = gameMapper.deserializeState(game.getStateJson());
        if (currentState == null) {
            throw new IllegalStateException("État de la partie introuvable.");
        }

        String seatId = String.valueOf(participant.getId());
        if (!seatId.equals(currentState.currentPlayerId())) {
            throw new IllegalMoveException("Ce n'est pas votre tour.");
        }

        Move move = new Move(
                request.type(),
                request.discType(),
                request.bidAmount(),
                request.targetPlayerId() != null ? String.valueOf(request.targetPlayerId()) : null
        );

        GameState nextState = gameEngine.applyMove(currentState, seatId, move, random);
        nextState = runAiTurns(nextState);
        syncGameWithState(game, nextState);

        Game saved = gameRepository.save(game);
        return gameMapper.toFilteredView(saved, playerId);
    }

    /**
     * Fait jouer automatiquement toutes les IA consécutives tant que la partie n'est pas terminée
     * et que le joueur actif est piloté par une IA.
     *
     * @param state état courant du moteur avant la séquence des IA
     * @return nouvel état du moteur une fois la main rendue à un joueur humain ou la partie terminée
     */
    private GameState runAiTurns(GameState state) {
        GameState current = state;
        int steps = 0;

        while (current != null
                && current.phase() != GamePhase.FINISHED
                && current.currentPlayerId() != null
                && steps < MAX_AI_CHAIN_STEPS) {

            String currentSeatId = current.currentPlayerId();
            PlayerState currentPlayer = current.players().stream()
                    .filter(p -> p.playerId().equals(currentSeatId))
                    .findFirst()
                    .orElse(null);

            if (currentPlayer == null || !currentPlayer.isAi()) {
                break;
            }

            Move aiMove = computeAiMove(current, currentPlayer);
            current = gameEngine.applyMove(current, currentSeatId, aiMove, random);
            steps++;
        }

        return current;
    }

    /**
     * Calcule le prochain coup d'une IA selon la phase de jeu en cours :
     * pose aléatoire lors du placement initial, dépilement de sa propre pile puis d'un adversaire
     * au hasard lors d'un défi, ou délégation à {@link AiStrategy} pour les tours et enchères.
     *
     * @param state    état courant de la partie
     * @param aiPlayer état du joueur IA dont c'est le tour
     * @return le {@link Move} à transmettre au moteur de règles
     */
    private Move computeAiMove(GameState state, PlayerState aiPlayer) {
        if (state.phase() == GamePhase.INITIAL_PLACEMENT) {
            DiscType chosen = aiPlayer.hand().get(random.nextInt(aiPlayer.hand().size()));
            return new Move(MoveType.PLACE_DISC, chosen, null, null);
        }

        if (state.phase() == GamePhase.CHALLENGE) {
            if (!aiPlayer.stack().isEmpty()) {
                return new Move(MoveType.REVEAL, null, null, aiPlayer.playerId());
            }
            List<PlayerState> targets = state.players().stream()
                    .filter(p -> !p.playerId().equals(aiPlayer.playerId()) && !p.stack().isEmpty())
                    .toList();
            if (targets.isEmpty()) {
                throw new IllegalStateException("Aucune pile disponible à retourner pendant le défi.");
            }
            PlayerState target = targets.get(random.nextInt(targets.size()));
            return new Move(MoveType.REVEAL, null, null, target.playerId());
        }

        int totalDiscsOnTable = state.players().stream()
                .mapToInt(p -> p.stack().size())
                .sum();

        List<Disc> aiHand = aiPlayer.hand().stream().map(this::toAiDisc).toList();
        List<Disc> aiStack = aiPlayer.stack().stream().map(this::toAiDisc).toList();

        AiView view = new AiView(aiHand, aiStack, state.currentBid(), totalDiscsOnTable);
        AiAction action = aiStrategy.decide(view, random);

        return switch (action) {
            case AiAction.PlaceDisc place -> new Move(MoveType.PLACE_DISC, toEngineDisc(place.disc()), null, null);
            case AiAction.Bid bid -> new Move(MoveType.BID, null, bid.amount(), null);
            case AiAction.Withdraw ignored -> new Move(MoveType.PASS, null, null, null);
        };
    }

    /**
     * Sérialise le nouvel état du moteur dans l'entité {@link Game} et met à jour son statut
     * ainsi que son vainqueur humain éventuel lorsque la partie est terminée.
     *
     * @param game  entité JPA de la partie à synchroniser
     * @param state état du moteur à persister
     */
    private void syncGameWithState(Game game, GameState state) {
        game.setStateJson(gameMapper.serializeState(state));

        if (state.phase() == GamePhase.FINISHED) {
            game.setStatus(GameStatus.FINISHED);

            if (state.winnerId() != null) {
                game.getParticipants().stream()
                        .filter(p -> String.valueOf(p.getId()).equals(state.winnerId()))
                        .findFirst()
                        .ifPresent(winner -> {
                            if (!winner.isAi() && !winner.isForfeited()) {
                                game.setWinnerId(winner.getPlayerId());
                            }
                        });
            }
        }
    }

    /**
     * Recherche une partie par son identifiant ou lève une exception si elle est introuvable.
     *
     * @param gameId identifiant de la partie recherchée
     * @return l'entité {@link Game} correspondante
     * @throws EntityNotFoundException si aucune partie n'existe avec cet identifiant
     */
    private Game findGameOrThrow(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new EntityNotFoundException("Partie introuvable : " + gameId));
    }

    /**
     * Construit un participant humain prêt à être ajouté à une partie.
     *
     * @param playerId    identifiant utilisateur du joueur
     * @param displayName pseudonyme affiché autour de la table
     * @param flowerTheme thème choisi pour les fleurs (ou défaut si vide)
     * @param skullTheme  thème choisi pour le crâne (ou défaut si vide)
     * @return une instance {@link GameParticipant} configurée pour un humain
     */
    private GameParticipant buildHumanParticipant(
            Long playerId,
            String displayName,
            String flowerTheme,
            String skullTheme
    ) {
        GameParticipant p = new GameParticipant();
        p.setPlayerId(playerId);
        p.setDisplayName(displayName);
        p.setAi(false);
        p.setForfeited(false);
        p.setFlowerTheme(isBlank(flowerTheme) ? DEFAULT_THEME : flowerTheme);
        p.setSkullTheme(isBlank(skullTheme) ? DEFAULT_THEME : skullTheme);
        return p;
    }

    /**
     * Construit un participant piloté par l'IA prêt à être ajouté à une partie.
     *
     * @param aiIndex numéro d'ordre de l'IA pour son nom d'affichage
     * @return une instance {@link GameParticipant} configurée pour une IA
     */
    private GameParticipant buildAiParticipant(int aiIndex) {
        GameParticipant p = new GameParticipant();
        p.setPlayerId(null);
        p.setDisplayName("IA #" + aiIndex);
        p.setAi(true);
        p.setForfeited(false);
        p.setFlowerTheme(DEFAULT_THEME);
        p.setSkullTheme(DEFAULT_THEME);
        return p;
    }

    /**
     * Réattribue séquentiellement les numéros de siège ({@code seatOrder}) de {@code 0} à {@code n - 1}.
     *
     * @param participants liste des participants à réordonner
     */
    private void reorderSeats(List<GameParticipant> participants) {
        for (int i = 0; i < participants.size(); i++) {
            participants.get(i).setSeatOrder(i);
        }
    }

    /**
     * Convertit un type de disque du moteur vers l'énumération utilisée par le module IA.
     *
     * @param type type de disque côté moteur
     * @return type de disque équivalent côté IA
     */
    private Disc toAiDisc(DiscType type) {
        return type == DiscType.SKULL ? Disc.SKULL : Disc.FLOWER;
    }

    /**
     * Convertit un type de disque du module IA vers l'énumération utilisée par le moteur.
     *
     * @param type type de disque côté IA
     * @return type de disque équivalent côté moteur
     */
    private DiscType toEngineDisc(Disc type) {
        return type == Disc.SKULL ? DiscType.SKULL : DiscType.FLOWER;
    }

    /**
     * Indique si une chaîne de caractères est nulle ou ne contient que des espaces.
     *
     * @param s chaîne à tester
     * @return {@code true} si {@code s} est nulle ou vide, {@code false} sinon
     */
    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}