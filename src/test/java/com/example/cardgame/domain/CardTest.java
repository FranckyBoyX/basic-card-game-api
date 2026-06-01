package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardTest {

    @Test
    void cardsWithSameSuitAndRankAreEqual() {
        assertThat(new Card(Suit.HEARTS, Rank.ACE))
            .isEqualTo(new Card(Suit.HEARTS, Rank.ACE));
    }

    @Test
    void rejectsNullSuitOrRank() {
        assertThatThrownBy(() -> new Card(null, Rank.ACE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Card(Suit.HEARTS, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
