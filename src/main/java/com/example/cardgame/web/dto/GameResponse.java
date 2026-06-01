package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Game;
import java.util.UUID;

public record GameResponse(UUID gameId) {
    public static GameResponse from(Game game) {
        return new GameResponse(game.id());
    }
}
