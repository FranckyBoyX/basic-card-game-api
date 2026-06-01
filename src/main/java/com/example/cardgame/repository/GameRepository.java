package com.example.cardgame.repository;

import com.example.cardgame.domain.Game;
import java.util.Optional;
import java.util.UUID;

/** Persistence seam for games. Implemented in-memory; swappable for a datastore. */
public interface GameRepository {
    Game save(Game game);
    Optional<Game> findById(UUID id);
    boolean deleteById(UUID id);
}
