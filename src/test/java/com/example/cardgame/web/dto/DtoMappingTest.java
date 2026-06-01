package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.CardCount;
import com.example.cardgame.domain.Player;
import com.example.cardgame.domain.Rank;
import com.example.cardgame.domain.Suit;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DtoMappingTest {

    @Test
    void cardResponseFromCardIncludesFaceValue() {
        CardResponse r = CardResponse.from(new Card(Suit.HEARTS, Rank.KING));
        assertThat(r.suit()).isEqualTo("HEARTS");
        assertThat(r.rank()).isEqualTo("KING");
        assertThat(r.value()).isEqualTo(13);
    }

    @Test
    void playerResponseFromPlayer() {
        PlayerResponse r = PlayerResponse.from(new Player(1, "alice"));
        assertThat(r.id()).isEqualTo(1);
        assertThat(r.name()).isEqualTo("alice");
    }

    @Test
    void playerScoreResponseFromPlayer() {
        Player p = new Player(1, "alice");
        p.addCard(new Card(Suit.HEARTS, Rank.TEN));
        p.addCard(new Card(Suit.SPADES, Rank.KING));
        PlayerScoreResponse r = PlayerScoreResponse.from(p);
        assertThat(r.id()).isEqualTo(1);
        assertThat(r.name()).isEqualTo("alice");
        assertThat(r.totalValue()).isEqualTo(23);
    }

    @Test
    void cardCountResponseFromCardCount() {
        CardCountResponse r = CardCountResponse.from(
            new CardCount(new Card(Suit.CLUBS, Rank.TWO), 3));
        assertThat(r.suit()).isEqualTo("CLUBS");
        assertThat(r.rank()).isEqualTo("TWO");
        assertThat(r.value()).isEqualTo(2);
        assertThat(r.count()).isEqualTo(3);
    }
}
