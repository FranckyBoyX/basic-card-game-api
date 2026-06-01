package com.example.cardgame.service;

import java.util.UUID;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(UUID gameId) {
        super("Game not found: " + gameId);
    }
}
