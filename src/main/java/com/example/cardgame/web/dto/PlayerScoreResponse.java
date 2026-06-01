package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Player;

public record PlayerScoreResponse(int id, String name, int totalValue) {
    public static PlayerScoreResponse from(Player player) {
        return new PlayerScoreResponse(player.id(), player.name(), player.totalValue());
    }
}
