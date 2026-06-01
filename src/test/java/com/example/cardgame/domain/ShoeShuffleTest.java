package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ShoeShuffleTest {

    @Test
    void shufflePreservesExactlyTheSameMultisetOfCards() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        List<Card> before = shoe.remaining();
        shoe.shuffle();
        List<Card> after = shoe.remaining();
        assertThat(after).hasSameSizeAs(before);
        assertThat(after).containsExactlyInAnyOrderElementsOf(before);
    }

    @Test
    void shuffleChangesOrderAtLeastOnceOverManyRuns() {
        // A correct shuffle will, with overwhelming probability, change the
        // order of 52 cards in at least one of several attempts.
        boolean changedAtLeastOnce = false;
        for (int attempt = 0; attempt < 5 && !changedAtLeastOnce; attempt++) {
            Shoe shoe = new Shoe();
            shoe.addDeck();
            List<Card> before = shoe.remaining();
            shoe.shuffle();
            if (!shoe.remaining().equals(before)) {
                changedAtLeastOnce = true;
            }
        }
        assertThat(changedAtLeastOnce).isTrue();
    }

    @Test
    void shuffleOfEmptyShoeIsNoOp() {
        Shoe shoe = new Shoe();
        shoe.shuffle();
        assertThat(shoe.size()).isZero();
    }
}
