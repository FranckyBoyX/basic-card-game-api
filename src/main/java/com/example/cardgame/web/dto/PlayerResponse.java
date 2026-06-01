package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Player;

public record PlayerResponse(int id, String name) {
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(player.id(), player.name());
    }
}
