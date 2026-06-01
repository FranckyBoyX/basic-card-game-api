package com.example.cardgame.service;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.Game;
import com.example.cardgame.domain.Player;
import com.example.cardgame.domain.Rank;
import com.example.cardgame.domain.Suit;
import com.example.cardgame.repository.InMemoryGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {

    private GameService service;

    @BeforeEach
    void setUp() {
        service = new GameService(new InMemoryGameRepository());
    }

    @Test
    void createGameReturnsPersistedGame() {
        Game game = service.createGame();
        assertThat(service.getGame(game.id())).isEqualTo(game);
    }

    @Test
    void deleteUnknownGameThrows() {
        assertThatThrownBy(() -> service.deleteGame(UUID.randomUUID()))
            .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void getUnknownGameThrows() {
        assertThatThrownBy(() -> service.getGame(UUID.randomUUID()))
            .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void addDeckIncreasesShoeByFiftyTwo() {
        Game game = service.createGame();
        service.addDeck(game.id());
        assertThat(game.shoe().size()).isEqualTo(52);
    }

    @Test
    void addAndRemovePlayer() {
        Game game = service.createGame();
        Player p = service.addPlayer(game.id(), "alice");
        assertThat(p.id()).isEqualTo(1);
        service.removePlayer(game.id(), p.id());
        assertThat(game.findPlayer(p.id())).isEqualTo(Optional.empty());
    }

    @Test
    void removeUnknownPlayerThrows() {
        Game game = service.createGame();
        assertThatThrownBy(() -> service.removePlayer(game.id(), 99))
            .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void dealMovesOneCardFromShoeToPlayer() {
        Game game = service.createGame();
        service.addDeck(game.id());
        Player p = service.addPlayer(game.id(), "alice");
        Optional<Card> dealt = service.dealCard(game.id(), p.id());
        assertThat(dealt).isPresent();
        assertThat(game.shoe().size()).isEqualTo(51);
        assertThat(game.findPlayer(p.id()).orElseThrow().hand()).hasSize(1);
    }

    @Test
    void dealFromEmptyShoeReturnsEmptyAndDealsNothing() {
        Game game = service.createGame();
        service.addDeck(game.id());
        Player p = service.addPlayer(game.id(), "alice");
        for (int i = 0; i < 52; i++) {
            service.dealCard(game.id(), p.id());
        }
        assertThat(service.dealCard(game.id(), p.id())).isEqualTo(Optional.empty());
        assertThat(game.findPlayer(p.id()).orElseThrow().hand()).hasSize(52);
    }

    @Test
    void playersAreSortedByTotalValueDescending() {
        Game game = service.createGame();
        Player a = service.addPlayer(game.id(), "A");
        Player b = service.addPlayer(game.id(), "B");
        a.addCard(new Card(Suit.HEARTS, Rank.TEN));   // 10
        a.addCard(new Card(Suit.SPADES, Rank.KING));  // 13 -> 23
        b.addCard(new Card(Suit.CLUBS, Rank.SEVEN));  // 7
        b.addCard(new Card(Suit.DIAMONDS, Rank.QUEEN)); // 12 -> 19
        List<Player> ranked = service.playersByScore(game.id());
        assertThat(ranked).containsExactly(a, b);
    }
}
