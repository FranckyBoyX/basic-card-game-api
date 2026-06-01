package com.example.cardgame.service;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(int playerId) {
        super("Player not found: " + playerId);
    }
}
