package com.example.cardgame.service;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.Game;
import com.example.cardgame.domain.Player;
import com.example.cardgame.repository.InMemoryGameRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

class GameServiceConcurrencyTest {

    @Test
    void concurrentDealsNeverDuplicateOrLoseCards() throws Exception {
        GameService service = new GameService(new InMemoryGameRepository());
        Game game = service.createGame();
        service.addDeck(game.id());            // 52 cards
        Player player = service.addPlayer(game.id(), "alice");

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Card> dealt = new ConcurrentLinkedQueue<>();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                start.await();
                Optional<Card> c;
                do {
                    c = service.dealCard(game.id(), player.id());
                    c.ifPresent(dealt::add);
                } while (c.isPresent());
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        List<Card> all = List.copyOf(dealt);
        assertThat(all).hasSize(52);
        assertThat(all).doesNotHaveDuplicates();
        assertThat(game.findPlayer(player.id()).orElseThrow().hand()).hasSize(52);
    }
}
