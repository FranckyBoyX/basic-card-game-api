package com.example.cardgame.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The game's shoe: the ordered collection of UNDEALT cards.
 * Not thread-safe by design — concurrency is managed by the service layer.
 */
public class Shoe {

    private static final Comparator<Card> REMAINING_ORDER =
        Comparator.comparingInt((Card c) -> c.suit().ordinal())
                  .thenComparing(Comparator.comparingInt((Card c) -> c.rank().faceValue()).reversed());

    private final Deque<Card> cards = new ArrayDeque<>();

    /** Adds a fresh standard 52-card deck to the bottom of the shoe. */
    public void addDeck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.addLast(new Card(suit, rank));
            }
        }
    }

    /** Deals one card from the top, or empty if the shoe is exhausted. */
    public Optional<Card> dealOne() {
        return Optional.ofNullable(cards.pollFirst());
    }

    public int size() {
        return cards.size();
    }

    /** Snapshot of remaining cards in current shoe order. */
    public List<Card> remaining() {
        return List.copyOf(cards);
    }

    /** Remaining count per suit; all four suits always present (0 if none). */
    public Map<Suit, Integer> suitCounts() {
        Map<Suit, Integer> counts = new EnumMap<>(Suit.class);
        for (Suit suit : Suit.values()) {
            counts.put(suit, 0);
        }
        for (Card card : cards) {
            counts.merge(card.suit(), 1, Integer::sum);
        }
        return counts;
    }

    /** Remaining cards with counts, sorted by suit then face value high-to-low. */
    public List<CardCount> cardCounts() {
        Map<Card, Integer> tally = new LinkedHashMap<>();
        for (Card card : cards) {
            tally.merge(card, 1, Integer::sum);
        }
        List<CardCount> result = new ArrayList<>();
        tally.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(REMAINING_ORDER))
            .forEach(e -> result.add(new CardCount(e.getKey(), e.getValue())));
        return result;
    }

    /**
     * Randomly permutes the shoe using a hand-rolled Fisher-Yates shuffle.
     * Uses a library RNG but NOT a library shuffle, per the assignment.
     */
    public void shuffle() {
        Card[] arr = cards.toArray(new Card[0]);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            Card tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        cards.clear();
        Collections.addAll(cards, arr);
    }
}
