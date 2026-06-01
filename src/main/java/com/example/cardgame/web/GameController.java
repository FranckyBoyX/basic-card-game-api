package com.example.cardgame.web;

import com.example.cardgame.domain.Game;
import com.example.cardgame.service.GameService;
import com.example.cardgame.web.dto.GameResponse;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService service;

    @PostMapping
    public ResponseEntity<GameResponse> createGame() {
        Game game = service.createGame();
        return ResponseEntity
            .created(URI.create("/api/v1/games/" + game.id()))
            .body(GameResponse.from(game));
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable UUID gameId) {
        service.deleteGame(gameId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gameId}/decks")
    public ResponseEntity<Void> addDeck(@PathVariable UUID gameId) {
        service.addDeck(gameId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gameId}/shuffle")
    public ResponseEntity<Void> shuffle(@PathVariable UUID gameId) {
        service.shuffle(gameId);
        return ResponseEntity.noContent().build();
    }
}
