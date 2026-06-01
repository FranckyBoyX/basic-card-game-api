package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Card;

public record CardResponse(String suit, String rank, int value) {
    public static CardResponse from(Card card) {
        return new CardResponse(card.suit().name(), card.rank().name(), card.rank().faceValue());
    }
}
