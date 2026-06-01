package com.example.cardgame.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A player in a game, holding an ordered hand of cards. Not thread-safe. */
public class Player {

    private final int id;
    private final String name;
    private final List<Card> hand = new ArrayList<>();

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    /** Unmodifiable view of the hand in deal order. */
    public List<Card> hand() {
        return Collections.unmodifiableList(hand);
    }

    public int totalValue() {
        return hand.stream().mapToInt(c -> c.rank().faceValue()).sum();
    }
}
