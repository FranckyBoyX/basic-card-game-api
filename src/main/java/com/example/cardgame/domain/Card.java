package com.example.cardgame.domain;

/** An immutable playing card. */
public record Card(Suit suit, Rank rank) {
    public Card {
        if (suit == null || rank == null) {
            throw new IllegalArgumentException("suit and rank are required");
        }
    }
}
