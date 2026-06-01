package com.example.cardgame.service;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.CardCount;
import com.example.cardgame.domain.Game;
import com.example.cardgame.domain.Player;
import com.example.cardgame.domain.Suit;
import com.example.cardgame.repository.GameRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates game use-cases and owns the concurrency policy: every compound
 * mutation of a single game runs under that game's lock, so operations on the
 * same game serialize while different games run in parallel.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameService {

    private final GameRepository repository;

    public Game createGame() {
        Game game = repository.save(new Game());
        log.info("Created game {}", game.id());
        return game;
    }

    public Game getGame(UUID gameId) {
        return repository.findById(gameId)
            .orElseThrow(() -> new GameNotFoundException(gameId));
    }

    public void deleteGame(UUID gameId) {
        withGameLock(gameId, game -> {
            repository.deleteById(gameId);
            return null;
        });
        log.info("Deleted game {}", gameId);
    }

    public void addDeck(UUID gameId) {
        withGameLock(gameId, game -> {
            game.shoe().addDeck();
            return null;
        });
    }

    public Player addPlayer(UUID gameId, String name) {
        return withGameLock(gameId, game -> game.addPlayer(name));
    }

    public void removePlayer(UUID gameId, int playerId) {
        withGameLock(gameId, game -> {
            if (!game.removePlayer(playerId)) {
                throw new PlayerNotFoundException(playerId);
            }
            return null;
        });
    }

    public Optional<Card> dealCard(UUID gameId, int playerId) {
        return withGameLock(gameId, game -> {
            Player player = game.findPlayer(playerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));
            Optional<Card> card = game.shoe().dealOne();
            card.ifPresent(player::addCard);
            return card;
        });
    }

    public void shuffle(UUID gameId) {
        withGameLock(gameId, game -> {
            game.shoe().shuffle();
            return null;
        });
    }

    public List<Player> playersByScore(UUID gameId) {
        return withGameLock(gameId, game -> game.players().stream()
            .sorted(Comparator.comparingInt(Player::totalValue).reversed())
            .toList());
    }

    public List<Card> playerCards(UUID gameId, int playerId) {
        return withGameLock(gameId, game -> List.copyOf(game.findPlayer(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId))
            .hand()));
    }

    public Map<Suit, Integer> suitCounts(UUID gameId) {
        return withGameLock(gameId, game -> game.shoe().suitCounts());
    }

    public List<CardCount> cardCounts(UUID gameId) {
        return withGameLock(gameId, game -> game.shoe().cardCounts());
    }

    /** Runs an action under the target game's lock, ensuring atomicity. */
    private <T> T withGameLock(UUID gameId, Function<Game, T> action) {
        Game game = getGame(gameId);
        ReentrantLock lock = game.lock();
        lock.lock();
        try {
            return action.apply(game);
        } finally {
            lock.unlock();
        }
    }
}
