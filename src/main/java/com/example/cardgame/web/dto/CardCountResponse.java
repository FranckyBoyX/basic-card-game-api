package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.CardCount;

public record CardCountResponse(String suit, String rank, int value, int count) {
    public static CardCountResponse from(CardCount cc) {
        Card card = cc.card();
        return new CardCountResponse(
            card.suit().name(), card.rank().name(), card.rank().faceValue(), cc.count());
    }
}
