package com.example.cardgame.domain;

/** How many of a given card remain in the shoe. */
public record CardCount(Card card, int count) {
}
