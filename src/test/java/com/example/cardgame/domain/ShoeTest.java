package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class ShoeTest {

    @Test
    void newShoeIsEmpty() {
        assertThat(new Shoe().size()).isZero();
    }

    @Test
    void addingOneDeckGivesFiftyTwoDistinctCards() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        assertThat(shoe.size()).isEqualTo(52);
        assertThat(shoe.remaining()).doesNotHaveDuplicates();
    }

    @Test
    void addingTwoDecksGivesOneHundredFourCards() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        shoe.addDeck();
        assertThat(shoe.size()).isEqualTo(104);
    }

    @Test
    void dealsAllFiftyTwoCardsThenReturnsEmpty() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        for (int i = 0; i < 52; i++) {
            assertThat(shoe.dealOne()).isPresent();
        }
        assertThat(shoe.dealOne()).isEqualTo(Optional.empty());
        assertThat(shoe.size()).isZero();
    }

    @Test
    void suitCountsIncludeAllFourSuitsAndSumToSize() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        assertThat(shoe.suitCounts())
            .containsKeys(Suit.HEARTS, Suit.SPADES, Suit.CLUBS, Suit.DIAMONDS)
            .containsValues(13, 13, 13, 13);
    }

    @Test
    void cardCountsAreSortedBySuitThenFaceValueDescending() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        var counts = shoe.cardCounts();
        assertThat(counts).hasSize(52);
        // first entry: HEARTS KING; 13th: HEARTS ACE; 14th: SPADES KING
        assertThat(counts.get(0).card()).isEqualTo(new Card(Suit.HEARTS, Rank.KING));
        assertThat(counts.get(12).card()).isEqualTo(new Card(Suit.HEARTS, Rank.ACE));
        assertThat(counts.get(13).card()).isEqualTo(new Card(Suit.SPADES, Rank.KING));
        assertThat(counts).allSatisfy(cc -> assertThat(cc.count()).isEqualTo(1));
    }
}
