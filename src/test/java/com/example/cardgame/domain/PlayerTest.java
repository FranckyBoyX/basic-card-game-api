package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerTest {

    @Test
    void newPlayerHasIdNameAndEmptyHand() {
        Player p = new Player(1, "alice");
        assertThat(p.id()).isEqualTo(1);
        assertThat(p.name()).isEqualTo("alice");
        assertThat(p.hand()).isEmpty();
        assertThat(p.totalValue()).isZero();
    }

    @Test
    void totalValueSumsFaceValues() {
        Player p = new Player(1, "alice");
        p.addCard(new Card(Suit.HEARTS, Rank.TEN));
        p.addCard(new Card(Suit.SPADES, Rank.KING));
        assertThat(p.totalValue()).isEqualTo(23); // 10 + 13
    }

    @Test
    void handPreservesDealOrderAndIsUnmodifiable() {
        Player p = new Player(1, "alice");
        Card first = new Card(Suit.HEARTS, Rank.TWO);
        Card second = new Card(Suit.CLUBS, Rank.NINE);
        p.addCard(first);
        p.addCard(second);
        assertThat(p.hand()).containsExactly(first, second);
        assertThatThrownBy(() -> p.hand().add(first))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
