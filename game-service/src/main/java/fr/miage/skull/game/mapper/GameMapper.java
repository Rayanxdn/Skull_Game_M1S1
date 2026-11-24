package fr.miage.skull.game.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.skull.game.dto.GameEventView;
import fr.miage.skull.game.dto.GameViewResponse;
import fr.miage.skull.game.dto.PlayerViewDto;
import fr.miage.skull.game.engine.DiscType;
import fr.miage.skull.game.engine.GameEvent;
import fr.miage.skull.game.engine.GameState;
import fr.miage.skull.game.engine.PlayerState;
import fr.miage.skull.game.entity.Game;
import fr.miage.skull.game.entity.GameParticipant;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Composant chargé de la conversion des données d'une partie entre la couche persistance
 * ({@link Game}, {@link GameState}) et la couche exposition REST ({@link GameViewResponse}).
 * <p>
 * Assure également le filtrage anti-triche en masquant le contenu des mains et des piles
 * des joueurs adverses.
 * </p>
 */
@Component
public class GameMapper {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    /**
     * Sérialise l'état interne du moteur de jeu en chaîne JSON pour stockage en base de données.
     *
     * @param state l'état courant de la partie ({@link GameState}), peut être {@code null}
     * @return la représentation JSON de l'état, ou {@code null} si {@code state} est {@code null}
     * @throws IllegalStateException si la sérialisation JSON échoue
     */
    public String serializeState(GameState state) {
        if (state == null) return null;
        try {
            return objectMapper.writeValueAsString(state);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erreur de sérialisation de l'état de la partie", e);
        }
    }

    /**
     * Désérialise la chaîne JSON stockée en base de données vers un objet {@link GameState}.
     *
     * @param json la chaîne JSON représentant l'état de la partie, peut être {@code null} ou vide
     * @return l'objet {@link GameState} reconstruit, ou {@code null} si {@code json} est vide ou {@code null}
     * @throws IllegalStateException si la désérialisation JSON échoue
     */
    public GameState deserializeState(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, GameState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erreur de désérialisation de l'état de la partie", e);
        }
    }

    /**
     * Construit une vue filtrée de la partie adaptée au joueur qui la consulte.
     * <p>
     * Les informations publiques (phase, enchères, scores, nombre de disques, disques déjà retournés,
     * disques perdus, journal) sont visibles par tous, tandis que le détail des disques en main et sur
     * la pile n'est inclus que pour le joueur connecté.
     * </p>
     *
     * @param game           l'entité JPA représentant la partie
     * @param viewerPlayerId l'identifiant du joueur authentifié consultant la partie (peut être {@code null})
     * @return un {@link GameViewResponse} sécurisé et prêt à être renvoyé par l'API REST
     */
    public GameViewResponse toFilteredView(Game game, Long viewerPlayerId) {
        GameState state = deserializeState(game.getStateJson());

        Map<String, PlayerState> stateByParticipantId = (state != null && state.players() != null)
                ? state.players().stream()
                .collect(Collectors.toMap(PlayerState::playerId, Function.identity()))
                : Map.of();

        List<PlayerViewDto> playerViews = game.getParticipants().stream()
                .map(p -> toPlayerView(p, stateByParticipantId.get(String.valueOf(p.getId())), viewerPlayerId))
                .toList();

        Map<String, String> namesBySeat = game.getParticipants().stream()
                .collect(Collectors.toMap(p -> String.valueOf(p.getId()), GameParticipant::getDisplayName));
        List<GameEventView> events = state != null
                ? state.recentEvents().stream().map(e -> toEventView(e, namesBySeat)).toList()
                : List.of();

        return new GameViewResponse(
                game.getId(),
                game.getStatus(),
                state != null ? state.phase() : null,
                game.getMaxPlayers(),
                game.getAiCount(),
                game.getCreatorId(),
                game.getWinnerId(),
                state != null ? parseLongOrNull(state.currentPlayerId()) : null,
                state != null ? state.currentBid() : 0,
                state != null ? parseLongOrNull(state.challengerId()) : null,
                game.getCreatedAt(),
                playerViews,
                state != null ? state.roundNumber() : 0,
                state != null ? state.revealedFlowersCount() : 0,
                state != null ? state.discsOnTable() : 0,
                state != null ? parseLongOrNull(state.winnerId()) : null,
                events
        );
    }

    /**
     * Transforme un participant et son état moteur associé en DTO de vue joueur ({@link PlayerViewDto}).
     * <p>
     * Applique la règle de confidentialité : les listes {@code myHand} et {@code myStack} ne sont
     * renseignées que si le participant est un humain dont le {@code playerId} correspond à {@code viewerPlayerId}.
     * </p>
     *
     * @param participant    le participant enregistré en base de données
     * @param pState         l'état dynamique du participant dans le moteur de jeu (peut être {@code null} avant le lancement)
     * @param viewerPlayerId l'identifiant du joueur qui effectue la requête
     * @return le {@link PlayerViewDto} filtré pour ce participant
     */
    private PlayerViewDto toPlayerView(GameParticipant participant, PlayerState pState, Long viewerPlayerId) {
        boolean isViewer = !participant.isAi()
                && participant.getPlayerId() != null
                && participant.getPlayerId().equals(viewerPlayerId);

        int points = pState != null ? pState.points() : 0;
        boolean eliminated = pState != null && pState.eliminated();
        boolean passed = pState != null && pState.passedInRound();
        int handCount = (pState != null && pState.hand() != null) ? pState.hand().size() : 0;
        int stackCount = (pState != null && pState.stack() != null) ? pState.stack().size() : 0;
        List<DiscType> revealed = pState != null ? pState.revealed() : List.of();
        int lostDiscs = pState != null ? pState.lostDiscsCount() : 0;

        return new PlayerViewDto(
                participant.getId(),
                participant.getPlayerId(),
                participant.getDisplayName(),
                participant.isAi(),
                participant.isForfeited(),
                participant.getSeatOrder(),
                participant.getFlowerTheme(),
                participant.getSkullTheme(),
                points,
                eliminated,
                passed,
                handCount,
                stackCount,
                isViewer && pState != null ? pState.hand() : null,
                isViewer && pState != null ? pState.stack() : null,
                revealed,
                lostDiscs
        );
    }

    /**
     * Traduit un événement du moteur pour l'affichage : les identifiants de siège deviennent des
     * nombres et sont accompagnés du nom affiché, pour que le front n'ait rien à recalculer.
     *
     * @param event       événement enregistré par le moteur
     * @param namesBySeat noms affichés indexés par identifiant de siège
     * @return l'événement prêt à afficher
     */
    private GameEventView toEventView(GameEvent event, Map<String, String> namesBySeat) {
        return new GameEventView(
                event.type().name(),
                parseLongOrNull(event.playerId()),
                event.playerId() != null ? namesBySeat.get(event.playerId()) : null,
                parseLongOrNull(event.targetId()),
                event.targetId() != null ? namesBySeat.get(event.targetId()) : null,
                event.value()
        );
    }

    /**
     * Convertit une chaîne de caractères en {@link Long} sans lever d'exception en cas de format invalide.
     *
     * @param value la chaîne à convertir
     * @return le {@link Long} correspondant, ou {@code null} si la chaîne est vide, {@code null} ou non numérique
     */
    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}