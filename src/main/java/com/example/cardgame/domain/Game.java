package com.example.cardgame.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A game aggregate: one shoe plus its players.
 * Single-thread-correct; the {@link #lock()} is provided for the service
 * layer to enforce atomicity of compound operations. The domain never
 * acquires the lock itself.
 */
public class Game {

    private final UUID id = UUID.randomUUID();
    private final Shoe shoe = new Shoe();
    private final Map<Integer, Player> players = new LinkedHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private int nextPlayerId = 1;

    public UUID id() {
        return id;
    }

    public Shoe shoe() {
        return shoe;
    }

    public ReentrantLock lock() {
        return lock;
    }

    /** Players in join order. */
    public List<Player> players() {
        return new ArrayList<>(players.values());
    }

    public Player addPlayer(String name) {
        Player player = new Player(nextPlayerId++, name);
        players.put(player.id(), player);
        return player;
    }

    public boolean removePlayer(int playerId) {
        return players.remove(playerId) != null;
    }

    public Optional<Player> findPlayer(int playerId) {
        return Optional.ofNullable(players.get(playerId));
    }
}
