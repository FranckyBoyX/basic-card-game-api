package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RankTest {

    @Test
    void aceIsWorthOneAndKingIsWorthThirteen() {
        assertThat(Rank.ACE.faceValue()).isEqualTo(1);
        assertThat(Rank.KING.faceValue()).isEqualTo(13);
    }

    @Test
    void faceValuesAreSequentialFromAceToKing() {
        assertThat(Rank.values())
            .extracting(Rank::faceValue)
            .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
    }

    @Test
    void suitsAreDeclaredInSortOrder() {
        assertThat(Suit.values())
            .containsExactly(Suit.HEARTS, Suit.SPADES, Suit.CLUBS, Suit.DIAMONDS);
    }
}
