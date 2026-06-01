package com.example.cardgame.repository;

import com.example.cardgame.domain.Game;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryGameRepositoryTest {

    private final GameRepository repository = new InMemoryGameRepository();

    @Test
    void savesAndFindsGameById() {
        Game game = new Game();
        repository.save(game);
        assertThat(repository.findById(game.id())).contains(game);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID())).isEqualTo(Optional.empty());
    }

    @Test
    void deletesGameById() {
        Game game = new Game();
        repository.save(game);
        assertThat(repository.deleteById(game.id())).isTrue();
        assertThat(repository.findById(game.id())).isEmpty();
    }

    @Test
    void deleteReturnsFalseForUnknownId() {
        assertThat(repository.deleteById(UUID.randomUUID())).isFalse();
    }
}
