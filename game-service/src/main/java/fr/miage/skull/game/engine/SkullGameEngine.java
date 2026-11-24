package fr.miage.skull.game.engine;

import fr.miage.skull.game.exception.IllegalMoveException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Règles de Skull. Classe sans état ni dépendance Spring (l'annotation sert seulement à l'injection) :
 * elle reçoit un état et un coup, et rend un nouvel état ou lève IllegalMoveException.
 */
@Component
public class SkullGameEngine implements GameEngine {

    static final int MIN_PLAYERS = 3;
    static final int POINTS_TO_WIN = 2;
    static final int MAX_EVENTS = 30;
    static final List<DiscType> STARTING_DISCS =
            List.of(DiscType.FLOWER, DiscType.FLOWER, DiscType.FLOWER, DiscType.SKULL);

    @Override
    public GameState initGame(List<PlayerSetup> setups) {
        if (setups == null || setups.size() < MIN_PLAYERS) {
            throw new IllegalMoveException("Il faut au moins " + MIN_PLAYERS + " joueurs pour lancer une partie.");
        }
        List<PlayerState> players = setups.stream()
                .map(s -> new PlayerState(s.playerId(), s.displayName(), s.isAi(),
                        STARTING_DISCS, List.of(), List.of(), 0, 0, false, false))
                .toList();
        String starter = players.get(0).playerId();
        return new GameState(GamePhase.INITIAL_PLACEMENT, players, starter, null, 0, 0, null,
                1, starter, List.of(new GameEvent(GameEventType.NEW_ROUND, null, null, 1)));
    }

    @Override
    public GameState applyMove(GameState state, String playerId, Move move, Random random) {
        if (state.phase() == GamePhase.FINISHED) {
            throw new IllegalMoveException("La partie est terminée.");
        }
        if (move == null || move.type() == null) {
            throw new IllegalMoveException("Le type de coup est obligatoire.");
        }
        PlayerState player = state.player(playerId)
                .orElseThrow(() -> new IllegalMoveException("Ce joueur ne participe pas à la partie."));
        if (player.eliminated()) {
            throw new IllegalMoveException("Ce joueur est éliminé.");
        }
        if (!playerId.equals(state.currentPlayerId())) {
            throw new IllegalMoveException("Ce n'est pas votre tour.");
        }

        Table table = new Table(state);
        switch (state.phase()) {
            case INITIAL_PLACEMENT -> initialPlacement(table, playerId, move);
            case TURN_ACTION -> turnAction(table, playerId, move);
            case BIDDING -> bidding(table, playerId, move);
            case CHALLENGE -> challenge(table, playerId, move, random);
            default -> throw new IllegalMoveException("Phase de jeu inconnue.");
        }
        return table.toState();
    }

    private void initialPlacement(Table t, String playerId, Move move) {
        if (move.type() != MoveType.PLACE_DISC) {
            throw new IllegalMoveException("En début de manche, chaque joueur doit d'abord poser un disque.");
        }
        placeDisc(t, playerId, move.discType());

        String next = t.nextActiveAfter(playerId);
        while (!next.equals(t.roundStarter) && !t.get(next).stack().isEmpty()) {
            next = t.nextActiveAfter(next);
        }
        if (next.equals(t.roundStarter)) {
            t.phase = GamePhase.TURN_ACTION;
        }
        t.current = next;
    }

    private void turnAction(Table t, String playerId, Move move) {
        switch (move.type()) {
            case PLACE_DISC -> {
                if (t.get(playerId).hand().isEmpty()) {
                    throw new IllegalMoveException("Vous n'avez plus de disque en main : vous devez lancer une enchère.");
                }
                placeDisc(t, playerId, move.discType());
                t.current = t.nextActiveAfter(playerId);
            }
            case BID -> placeBid(t, playerId, move.bidAmount(), 1);
            default -> throw new IllegalMoveException("À votre tour, vous devez poser un disque ou lancer une enchère.");
        }
    }

    private void bidding(Table t, String playerId, Move move) {
        switch (move.type()) {
            case BID -> placeBid(t, playerId, move.bidAmount(), t.bid + 1);
            case PASS -> {
                PlayerState p = t.get(playerId);
                t.set(new PlayerState(p.playerId(), p.displayName(), p.isAi(), p.hand(), p.stack(), p.revealed(),
                        p.lostDiscsCount(), p.points(), true, p.eliminated()));
                t.log(GameEvent.of(GameEventType.PASS, playerId));
                if (t.activeBidders() == 1) {
                    startChallenge(t);
                } else {
                    t.current = t.nextBidderAfter(playerId);
                }
            }
            default -> throw new IllegalMoveException("Pendant les enchères, vous pouvez seulement surenchérir ou vous retirer.");
        }
    }

    private void placeBid(Table t, String playerId, Integer amount, int minimum) {
        int maximum = t.discsOnTable();
        if (amount == null || amount < minimum || amount > maximum) {
            throw new IllegalMoveException("L'enchère doit être comprise entre " + minimum + " et " + maximum + ".");
        }
        t.bid = amount;
        t.challenger = playerId;
        t.log(new GameEvent(GameEventType.BID, playerId, null, amount));

        if (amount == maximum) {
            startChallenge(t);
        } else {
            t.phase = GamePhase.BIDDING;
            t.current = t.nextBidderAfter(playerId);
        }
    }

    private void startChallenge(Table t) {
        t.phase = GamePhase.CHALLENGE;
        t.current = t.challenger;
        t.flowers = 0;
    }

    private void challenge(Table t, String playerId, Move move, Random random) {
        if (move.type() != MoveType.REVEAL) {
            throw new IllegalMoveException("Pendant le défi, vous devez retourner un disque.");
        }
        String targetId = move.targetPlayerId();
        PlayerState target = targetId == null ? null : t.find(targetId);
        if (target == null) {
            throw new IllegalMoveException("Choisissez la pile d'un joueur de la table.");
        }
        if (!targetId.equals(playerId) && !t.get(playerId).stack().isEmpty()) {
            throw new IllegalMoveException("Vous devez d'abord retourner tous les disques de votre propre pile.");
        }
        if (target.stack().isEmpty()) {
            throw new IllegalMoveException("Cette pile n'a plus de disque face cachée.");
        }

        List<DiscType> stack = new ArrayList<>(target.stack());
        DiscType disc = stack.remove(stack.size() - 1);
        List<DiscType> revealed = new ArrayList<>(target.revealed());
        revealed.add(disc);
        t.set(new PlayerState(target.playerId(), target.displayName(), target.isAi(), target.hand(), stack, revealed,
                target.lostDiscsCount(), target.points(), target.passedInRound(), target.eliminated()));

        if (disc == DiscType.FLOWER) {
            t.flowers++;
            t.log(new GameEvent(GameEventType.FLOWER_REVEALED, playerId, targetId, t.flowers));
            if (t.flowers >= t.bid) {
                challengeWon(t, playerId);
            }
        } else {
            t.log(new GameEvent(GameEventType.SKULL_REVEALED, playerId, targetId, null));
            challengeLost(t, playerId, random);
        }
    }

    private void challengeWon(Table t, String playerId) {
        PlayerState p = t.get(playerId);
        int points = p.points() + 1;
        t.set(new PlayerState(p.playerId(), p.displayName(), p.isAi(), p.hand(), p.stack(), p.revealed(),
                p.lostDiscsCount(), points, p.passedInRound(), p.eliminated()));
        t.log(new GameEvent(GameEventType.CHALLENGE_WON, playerId, null, points));

        if (points >= POINTS_TO_WIN) {
            finish(t, playerId);
        } else {
            newRound(t, playerId);
        }
    }

    /** Le disque perdu est tiré parmi tous les disques du joueur ; son type reste secret. */
    private void challengeLost(Table t, String playerId, Random random) {
        PlayerState p = t.get(playerId);
        List<DiscType> discs = new ArrayList<>(p.hand());
        discs.addAll(p.stack());
        discs.addAll(p.revealed());
        discs.remove(random.nextInt(discs.size()));

        boolean eliminated = discs.isEmpty();
        t.set(new PlayerState(p.playerId(), p.displayName(), p.isAi(), discs, List.of(), List.of(),
                p.lostDiscsCount() + 1, p.points(), p.passedInRound(), eliminated));
        t.log(GameEvent.of(GameEventType.DISC_LOST, playerId));

        if (eliminated) {
            t.log(GameEvent.of(GameEventType.PLAYER_ELIMINATED, playerId));
            List<PlayerState> alive = t.players.stream().filter(pl -> !pl.eliminated()).toList();
            if (alive.size() == 1) {
                finish(t, alive.get(0).playerId());
                return;
            }
            newRound(t, t.nextActiveAfter(playerId));
        } else {
            newRound(t, playerId);
        }
    }

    private void newRound(Table t, String starter) {
        t.players.replaceAll(p -> {
            List<DiscType> discs = new ArrayList<>(p.hand());
            discs.addAll(p.stack());
            discs.addAll(p.revealed());
            return new PlayerState(p.playerId(), p.displayName(), p.isAi(), discs, List.of(), List.of(),
                    p.lostDiscsCount(), p.points(), false, p.eliminated());
        });
        t.phase = GamePhase.INITIAL_PLACEMENT;
        t.round++;
        t.roundStarter = starter;
        t.current = starter;
        t.challenger = null;
        t.bid = 0;
        t.flowers = 0;
        t.log(new GameEvent(GameEventType.NEW_ROUND, null, null, t.round));
    }

    private void finish(Table t, String winnerId) {
        t.phase = GamePhase.FINISHED;
        t.winner = winnerId;
        t.current = null;
        t.log(GameEvent.of(GameEventType.GAME_WON, winnerId));
    }

    private void placeDisc(Table t, String playerId, DiscType disc) {
        if (disc == null) {
            throw new IllegalMoveException("Précisez le disque à poser (FLOWER ou SKULL).");
        }
        PlayerState p = t.get(playerId);
        List<DiscType> hand = new ArrayList<>(p.hand());
        if (!hand.remove(disc)) {
            throw new IllegalMoveException("Vous n'avez pas ce disque en main.");
        }
        List<DiscType> stack = new ArrayList<>(p.stack());
        stack.add(disc);
        t.set(new PlayerState(p.playerId(), p.displayName(), p.isAi(), hand, stack, p.revealed(),
                p.lostDiscsCount(), p.points(), p.passedInRound(), p.eliminated()));
        t.log(GameEvent.of(GameEventType.DISC_PLACED, playerId));
    }

    /** Copie modifiable de l'état, le temps d'appliquer un seul coup. */
    private static final class Table {
        final List<PlayerState> players;
        final List<GameEvent> events;
        GamePhase phase;
        String current;
        String challenger;
        int bid;
        int flowers;
        String winner;
        int round;
        String roundStarter;

        Table(GameState s) {
            players = new ArrayList<>(s.players());
            events = new ArrayList<>(s.recentEvents());
            phase = s.phase();
            current = s.currentPlayerId();
            challenger = s.challengerId();
            bid = s.currentBid();
            flowers = s.revealedFlowersCount();
            winner = s.winnerId();
            round = s.roundNumber();
            roundStarter = s.roundStarterId();
        }

        GameState toState() {
            return new GameState(phase, players, current, challenger, bid, flowers, winner,
                    round, roundStarter, events);
        }

        PlayerState find(String id) {
            return players.stream().filter(p -> p.playerId().equals(id)).findFirst().orElse(null);
        }

        PlayerState get(String id) {
            return players.get(indexOf(id));
        }

        void set(PlayerState p) {
            players.set(indexOf(p.playerId()), p);
        }

        int indexOf(String id) {
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).playerId().equals(id)) {
                    return i;
                }
            }
            throw new IllegalStateException("Joueur absent de l'état : " + id);
        }

        void log(GameEvent event) {
            events.add(event);
            if (events.size() > MAX_EVENTS) {
                events.remove(0);
            }
        }

        int discsOnTable() {
            return players.stream().mapToInt(p -> p.stack().size() + p.revealed().size()).sum();
        }

        int activeBidders() {
            return (int) players.stream().filter(p -> !p.eliminated() && !p.passedInRound()).count();
        }

        /** Joueur suivant dans le sens horaire, en sautant les éliminés. */
        String nextActiveAfter(String id) {
            return nextAfter(id, false);
        }

        /** Joueur suivant qui peut encore enchérir (ni éliminé, ni retiré). */
        String nextBidderAfter(String id) {
            return nextAfter(id, true);
        }

        private String nextAfter(String id, boolean skipPassed) {
            int start = indexOf(id);
            for (int step = 1; step <= players.size(); step++) {
                PlayerState p = players.get((start + step) % players.size());
                if (!p.eliminated() && !(skipPassed && p.passedInRound())) {
                    return p.playerId();
                }
            }
            return id;
        }
    }
}
