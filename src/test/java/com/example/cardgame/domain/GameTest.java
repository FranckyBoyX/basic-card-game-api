package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class GameTest {

    @Test
    void newGameHasIdEmptyShoeAndNoPlayers() {
        Game game = new Game();
        assertThat(game.id()).isNotNull();
        assertThat(game.shoe().size()).isZero();
        assertThat(game.players()).isEmpty();
    }

    @Test
    void addPlayerAssignsIncrementingIdsStartingAtOne() {
        Game game = new Game();
        Player a = game.addPlayer("alice");
        Player b = game.addPlayer("bob");
        assertThat(a.id()).isEqualTo(1);
        assertThat(b.id()).isEqualTo(2);
        assertThat(game.players()).containsExactly(a, b);
    }

    @Test
    void playerIdsAreNotRecycledAfterRemoval() {
        Game game = new Game();
        game.addPlayer("alice");            // id 1
        Player bob = game.addPlayer("bob");  // id 2
        game.removePlayer(bob.id());
        Player carol = game.addPlayer("carol");
        assertThat(carol.id()).isEqualTo(3);
    }

    @Test
    void findPlayerReturnsEmptyForUnknownId() {
        Game game = new Game();
        assertThat(game.findPlayer(99)).isEqualTo(Optional.empty());
    }

    @Test
    void removePlayerReturnsFalseForUnknownId() {
        Game game = new Game();
        assertThat(game.removePlayer(99)).isFalse();
    }

    @Test
    void lockIsAvailableForServiceLayer() {
        assertThat(new Game().lock()).isNotNull();
    }
}
