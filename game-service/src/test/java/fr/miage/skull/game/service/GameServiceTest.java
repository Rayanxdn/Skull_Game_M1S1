
package fr.miage.skull.game.service;

import fr.miage.skull.game.dto.CreateGameRequest;
import fr.miage.skull.game.dto.GameViewResponse;
import fr.miage.skull.game.dto.JoinGameRequest;
import fr.miage.skull.game.dto.MoveRequest;
import fr.miage.skull.game.engine.DiscType;
import fr.miage.skull.game.engine.GameEngine;
import fr.miage.skull.game.engine.GameState;
import fr.miage.skull.game.engine.Move;
import fr.miage.skull.game.engine.MoveType;
import fr.miage.skull.game.entity.Game;
import fr.miage.skull.game.entity.GameParticipant;
import fr.miage.skull.game.entity.GameStatus;
import fr.miage.skull.game.exception.IllegalMoveException;
import fr.miage.skull.game.mapper.GameMapper;
import fr.miage.skull.game.repository.GameRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de {@link GameService}.
 * Le repository, le moteur de règles et le mapper sont simulés avec Mockito :
 * on teste uniquement la logique du service (règles de création, de participation, de départ,
 * de lancement et de contrôle des coups), sans base de données ni moteur réel.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameServiceTest {

    private static final Long GAME_ID = 1L;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameEngine gameEngine;

    @Mock
    private GameMapper gameMapper;

    @InjectMocks
    private GameService gameService;

    private GameViewResponse view;

    @BeforeEach
    void setUp() {
        view = mock(GameViewResponse.class);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gameMapper.toFilteredView(any(Game.class), any())).thenReturn(view);
    }

    // ------------------------------------------------------------------
    // Outils de construction
    // ------------------------------------------------------------------

    private GameParticipant human(Long participantId, Long playerId, String name) {
        return GameParticipant.builder()
                .id(participantId)
                .playerId(playerId)
                .displayName(name)
                .ai(false)
                .build();
    }

    private GameParticipant ai(Long participantId, String name) {
        return GameParticipant.builder()
                .id(participantId)
                .displayName(name)
                .ai(true)
                .build();
    }

    private Game game(GameStatus status, Long creatorId, int maxPlayers, GameParticipant... participants) {
        Game game = Game.builder()
                .id(GAME_ID)
                .status(status)
                .maxPlayers(maxPlayers)
                .creatorId(creatorId)
                .build();
        for (GameParticipant p : participants) {
            game.addParticipant(p);
        }
        return game;
    }

    private void gameExists(Game game) {
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
    }

    // ------------------------------------------------------------------
    // createGame
    // ------------------------------------------------------------------

    @Test
    void createGame_valide_creeUnePartieOuverteAvecLeCreateurEtLesIA() {
        CreateGameRequest request = new CreateGameRequest("Rania", 4, 2, null, null);

        GameViewResponse result = gameService.createGame(request, 10L, "Rania");

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());
        Game saved = captor.getValue();

        assertSame(view, result);
        assertEquals(GameStatus.OPEN, saved.getStatus());
        assertEquals(4, saved.getMaxPlayers());
        assertEquals(2, saved.getAiCount());
        assertEquals(10L, saved.getCreatorId());
        assertEquals(3, saved.getParticipants().size());

        GameParticipant creator = saved.getParticipants().get(0);
        assertEquals(10L, creator.getPlayerId());
        assertFalse(creator.isAi());
        assertEquals(0, creator.getSeatOrder());

        assertTrue(saved.getParticipants().get(1).isAi());
        assertTrue(saved.getParticipants().get(2).isAi());
        assertEquals("IA #1", saved.getParticipants().get(1).getDisplayName());
    }

    @Test
    void createGame_sansTheme_utiliseLeThemeParDefaut() {
        CreateGameRequest request = new CreateGameRequest("Rania", 3, 0, " ", null);

        gameService.createGame(request, 10L, "Rania");

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());
        GameParticipant creator = captor.getValue().getParticipants().get(0);
        assertEquals("CLASSIC", creator.getFlowerTheme());
        assertEquals("CLASSIC", creator.getSkullTheme());
    }

    @Test
    void createGame_pseudoNul_utiliseUnNomParDefaut() {
        CreateGameRequest request = new CreateGameRequest("x", 3, 0, null, null);

        gameService.createGame(request, 10L, null);

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());
        assertEquals("Joueur #10", captor.getValue().getParticipants().get(0).getDisplayName());
    }

    @Test
    void createGame_moinsDeTroisJoueursMax_refuse() {
        CreateGameRequest request = new CreateGameRequest("Rania", 2, 0, null, null);

        assertThrows(IllegalArgumentException.class, () -> gameService.createGame(request, 10L, "Rania"));
        verify(gameRepository, never()).save(any());
    }

    @Test
    void createGame_plusDeSixJoueurs_refuse() {
        CreateGameRequest request = new CreateGameRequest("Rania", 7, 0, null, null);

        assertThrows(IllegalArgumentException.class, () -> gameService.createGame(request, 10L, "Rania"));
        verify(gameRepository, never()).save(any());
    }

    @Test
    void createGame_autantDIAQueDeJoueursMax_refuse() {
        CreateGameRequest request = new CreateGameRequest("Rania", 4, 4, null, null);

        assertThrows(IllegalArgumentException.class, () -> gameService.createGame(request, 10L, "Rania"));
        verify(gameRepository, never()).save(any());
    }

    @Test
    void createGame_nombreDIANegatif_refuse() {
        CreateGameRequest request = new CreateGameRequest("Rania", 4, -1, null, null);

        assertThrows(IllegalArgumentException.class, () -> gameService.createGame(request, 10L, "Rania"));
    }

    // ------------------------------------------------------------------
    // listGames / getGame
    // ------------------------------------------------------------------

    @Test
    void listGames_sansStatut_listeLesPartiesOuvertes() {
        Game open = game(GameStatus.OPEN, 10L, 4, human(1L, 10L, "A"));
        when(gameRepository.findByStatusOrderByCreatedAtDesc(GameStatus.OPEN)).thenReturn(List.of(open));

        List<GameViewResponse> result = gameService.listGames(null, 10L);

        assertEquals(1, result.size());
        verify(gameRepository).findByStatusOrderByCreatedAtDesc(GameStatus.OPEN);
    }

    @Test
    void listGames_avecStatut_utiliseCeStatut() {
        when(gameRepository.findByStatusOrderByCreatedAtDesc(GameStatus.FINISHED)).thenReturn(List.of());

        List<GameViewResponse> result = gameService.listGames(GameStatus.FINISHED, 10L);

        assertTrue(result.isEmpty());
        verify(gameRepository).findByStatusOrderByCreatedAtDesc(GameStatus.FINISHED);
    }

    @Test
    void getGame_existante_retourneLaVueDuJoueur() {
        Game game = game(GameStatus.OPEN, 10L, 4, human(1L, 10L, "A"));
        gameExists(game);

        GameViewResponse result = gameService.getGame(GAME_ID, 10L);

        assertSame(view, result);
        verify(gameMapper).toFilteredView(game, 10L);
    }

    @Test
    void getGame_inexistante_lanceEntityNotFound() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> gameService.getGame(99L, 10L));
    }

    // ------------------------------------------------------------------
    // joinGame
    // ------------------------------------------------------------------

    @Test
    void joinGame_partieOuverte_ajouteLeJoueur() {
        Game game = game(GameStatus.OPEN, 10L, 4, human(1L, 10L, "A"));
        gameExists(game);
        JoinGameRequest request = new JoinGameRequest("B", "ROSE", "NOIR");

        GameViewResponse result = gameService.joinGame(GAME_ID, request, 20L, "B");

        assertSame(view, result);
        assertEquals(2, game.getParticipants().size());
        GameParticipant added = game.getParticipants().get(1);
        assertEquals(20L, added.getPlayerId());
        assertEquals("ROSE", added.getFlowerTheme());
        assertEquals("NOIR", added.getSkullTheme());
        verify(gameRepository).save(game);
    }

    @Test
    void joinGame_dejaInscrit_neCreePasDeDoublon() {
        Game game = game(GameStatus.OPEN, 10L, 4, human(1L, 10L, "A"));
        gameExists(game);

        GameViewResponse result = gameService.joinGame(GAME_ID, null, 10L, "A");

        assertSame(view, result);
        assertEquals(1, game.getParticipants().size());
        verify(gameRepository, never()).save(any());
    }

    @Test
    void joinGame_partieComplete_lanceIllegalState() {
        Game game = game(GameStatus.OPEN, 10L, 3,
                human(1L, 10L, "A"), human(2L, 20L, "B"), human(3L, 30L, "C"));
        gameExists(game);

        assertThrows(IllegalStateException.class,
                () -> gameService.joinGame(GAME_ID, null, 40L, "D"));
        verify(gameRepository, never()).save(any());
    }

    @Test
    void joinGame_partieDejaLancee_lanceIllegalState() {
        Game game = game(GameStatus.IN_PROGRESS, 10L, 4, human(1L, 10L, "A"));
        gameExists(game);

        assertThrows(IllegalStateException.class,
                () -> gameService.joinGame(GAME_ID, null, 20L, "B"));
    }

    @Test
    void joinGame_partieInexistante_lanceEntityNotFound() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> gameService.joinGame(99L, null, 20L, "B"));
    }

    // ------------------------------------------------------------------
    // leaveGame
    // ------------------------------------------------------------------

    @Test
    void leaveGame_joueurAbsent_lanceIllegalArgument() {
        Game game = game(GameStatus.OPEN, 10L, 4, human(1L, 10L, "A"));
        gameExists(game);

        assertThrows(IllegalArgumentException.class, () -> gameService.leaveGame(GAME_ID, 99L));
    }

    @Test
    void leaveGame_partieOuverte_dernierHumain_supprimeLaPartie() {
        Game game = game(GameStatus.OPEN, 10L, 4, human(1L, 10L, "A"), ai(2L, "IA #1"));
        gameExists(game);

        GameViewResponse result = gameService.leaveGame(GAME_ID, 10L);

        assertNull(result);
        verify(gameRepository).delete(game);
        verify(gameRepository, never()).save(any());
    }

    @Test
    void leaveGame_partieOuverte_leCreateurPart_unAutreHumainDevientCreateur() {
        GameParticipant first = human(1L, 10L, "A");
        GameParticipant second = human(2L, 20L, "B");
        Game game = game(GameStatus.OPEN, 10L, 4, first, second);
        gameExists(game);

        GameViewResponse result = gameService.leaveGame(GAME_ID, 10L);

        assertSame(view, result);
        assertEquals(20L, game.getCreatorId());
        assertEquals(1, game.getParticipants().size());
        assertEquals(0, second.getSeatOrder());
        verify(gameRepository).save(game);
    }

    @Test
    void leaveGame_partieOuverte_unJoueurQuiNEstPasCreateurPart_createurInchange() {
        GameParticipant first = human(1L, 10L, "A");
        GameParticipant second = human(2L, 20L, "B");
        Game game = game(GameStatus.OPEN, 10L, 4, first, second);
        gameExists(game);

        gameService.leaveGame(GAME_ID, 20L);

        assertEquals(10L, game.getCreatorId());
        assertEquals(1, game.getParticipants().size());
    }

    @Test
    void leaveGame_partieEnCours_leJoueurDevientUneIAForfait() {
        GameParticipant leaving = human(1L, 10L, "A");
        Game game = game(GameStatus.IN_PROGRESS, 10L, 4, leaving, human(2L, 20L, "B"), ai(3L, "IA #1"));
        gameExists(game);
        when(gameMapper.deserializeState(any())).thenReturn(null);

        GameViewResponse result = gameService.leaveGame(GAME_ID, 10L);

        assertSame(view, result);
        assertTrue(leaving.isAi());
        assertTrue(leaving.isForfeited());
        assertEquals("A (IA)", leaving.getDisplayName());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        verify(gameRepository).save(game);
    }

    @Test
    void leaveGame_partieTerminee_lanceIllegalState() {
        Game game = game(GameStatus.FINISHED, 10L, 4, human(1L, 10L, "A"));
        gameExists(game);

        assertThrows(IllegalStateException.class, () -> gameService.leaveGame(GAME_ID, 10L));
    }

    // ------------------------------------------------------------------
    // startGame
    // ------------------------------------------------------------------

    @Test
    void startGame_valide_lanceLaPartieViaLeMoteur() {
        Game game = game(GameStatus.OPEN, 10L, 4,
                human(1L, 10L, "A"), human(2L, 20L, "B"), ai(3L, "IA #1"));
        gameExists(game);
        GameState initial = mock(GameState.class);
        when(gameEngine.initGame(any())).thenReturn(initial);
        when(gameMapper.serializeState(initial)).thenReturn("{}");

        GameViewResponse result = gameService.startGame(GAME_ID, 10L);

        assertSame(view, result);
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals("{}", game.getStateJson());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameEngine.PlayerSetup>> captor = ArgumentCaptor.forClass(List.class);
        verify(gameEngine).initGame(captor.capture());
        assertEquals(3, captor.getValue().size());
        verify(gameRepository).save(game);
    }

    @Test
    void startGame_partieDejaLancee_lanceIllegalState() {
        Game game = game(GameStatus.IN_PROGRESS, 10L, 4,
                human(1L, 10L, "A"), human(2L, 20L, "B"), human(3L, 30L, "C"));
        gameExists(game);

        assertThrows(IllegalStateException.class, () -> gameService.startGame(GAME_ID, 10L));
        verify(gameEngine, never()).initGame(any());
    }

    @Test
    void startGame_pasLeCreateur_lanceAccessDenied() {
        Game game = game(GameStatus.OPEN, 10L, 4,
                human(1L, 10L, "A"), human(2L, 20L, "B"), human(3L, 30L, "C"));
        gameExists(game);

        assertThrows(AccessDeniedException.class, () -> gameService.startGame(GAME_ID, 20L));
        verify(gameEngine, never()).initGame(any());
    }

    @Test
    void startGame_moinsDeTroisParticipants_lanceIllegalState() {
        Game game = game(GameStatus.OPEN, 10L, 4, human(1L, 10L, "A"), human(2L, 20L, "B"));
        gameExists(game);

        assertThrows(IllegalStateException.class, () -> gameService.startGame(GAME_ID, 10L));
        verify(gameEngine, never()).initGame(any());
    }

    // ------------------------------------------------------------------
    // playMove
    // ------------------------------------------------------------------

    private MoveRequest placeFlower() {
        return new MoveRequest(MoveType.PLACE_DISC, DiscType.FLOWER, null, null);
    }

    @Test
    void playMove_partieNonEnCours_lanceIllegalState() {
        Game game = game(GameStatus.OPEN, 10L, 4, human(1L, 10L, "A"));
        gameExists(game);

        assertThrows(IllegalStateException.class,
                () -> gameService.playMove(GAME_ID, placeFlower(), 10L));
    }

    @Test
    void playMove_joueurNonParticipant_lanceAccessDenied() {
        Game game = game(GameStatus.IN_PROGRESS, 10L, 4, human(1L, 10L, "A"));
        gameExists(game);

        assertThrows(AccessDeniedException.class,
                () -> gameService.playMove(GAME_ID, placeFlower(), 99L));
    }

    @Test
    void playMove_etatIntrouvable_lanceIllegalState() {
        Game game = game(GameStatus.IN_PROGRESS, 10L, 4, human(1L, 10L, "A"));
        gameExists(game);
        when(gameMapper.deserializeState(any())).thenReturn(null);

        assertThrows(IllegalStateException.class,
                () -> gameService.playMove(GAME_ID, placeFlower(), 10L));
    }

    @Test
    void playMove_pasSonTour_lanceIllegalMove() {
        Game game = game(GameStatus.IN_PROGRESS, 10L, 4, human(1L, 10L, "A"), human(2L, 20L, "B"));
        gameExists(game);
        GameState state = mock(GameState.class);
        when(state.currentPlayerId()).thenReturn("2");
        when(gameMapper.deserializeState(any())).thenReturn(state);

        assertThrows(IllegalMoveException.class,
                () -> gameService.playMove(GAME_ID, placeFlower(), 10L));
        verify(gameEngine, never()).applyMove(any(), any(), any(), any());
    }

    @Test
    void playMove_sonTour_appliqueLeCoupViaLeMoteur() {
        Game game = game(GameStatus.IN_PROGRESS, 10L, 4, human(1L, 10L, "A"), human(2L, 20L, "B"));
        gameExists(game);
        GameState state = mock(GameState.class);
        GameState next = mock(GameState.class);
        when(state.currentPlayerId()).thenReturn("1");
        when(gameMapper.deserializeState(any())).thenReturn(state);
        when(gameEngine.applyMove(eq(state), eq("1"), any(Move.class), any(Random.class))).thenReturn(next);
        when(gameMapper.serializeState(next)).thenReturn("{\"etat\":1}");

        GameViewResponse result = gameService.playMove(GAME_ID, placeFlower(), 10L);

        assertSame(view, result);
        ArgumentCaptor<Move> moveCaptor = ArgumentCaptor.forClass(Move.class);
        verify(gameEngine).applyMove(eq(state), eq("1"), moveCaptor.capture(), any(Random.class));
        assertEquals(new Move(MoveType.PLACE_DISC, DiscType.FLOWER, null, null), moveCaptor.getValue());
        assertEquals("{\"etat\":1}", game.getStateJson());
        verify(gameRepository).save(game);
    }
}
