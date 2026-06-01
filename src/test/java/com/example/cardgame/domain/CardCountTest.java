package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CardCountTest {

    @Test
    void exposesCardAndCount() {
        Card card = new Card(Suit.SPADES, Rank.KING);
        CardCount cc = new CardCount(card, 2);
        assertThat(cc.card()).isEqualTo(card);
        assertThat(cc.count()).isEqualTo(2);
    }
}
