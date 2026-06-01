package com.example.cardgame.web;

import com.example.cardgame.domain.Player;
import com.example.cardgame.service.GameService;
import com.example.cardgame.web.dto.CardResponse;
import com.example.cardgame.web.dto.CreatePlayerRequest;
import com.example.cardgame.web.dto.PlayerResponse;
import com.example.cardgame.web.dto.PlayerScoreResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games/{gameId}/players")
@RequiredArgsConstructor
public class PlayerController {

    private final GameService service;

    @PostMapping
    public ResponseEntity<PlayerResponse> addPlayer(
            @PathVariable UUID gameId,
            @Valid @RequestBody CreatePlayerRequest request) {
        Player player = service.addPlayer(gameId, request.name());
        return ResponseEntity
            .created(URI.create("/api/v1/games/" + gameId + "/players/" + player.id()))
            .body(PlayerResponse.from(player));
    }

    @DeleteMapping("/{playerId}")
    public ResponseEntity<Void> removePlayer(
            @PathVariable UUID gameId, @PathVariable int playerId) {
        service.removePlayer(gameId, playerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<PlayerScoreResponse> listPlayers(@PathVariable UUID gameId) {
        return service.playersByScore(gameId).stream()
            .map(PlayerScoreResponse::from)
            .toList();
    }

    @GetMapping("/{playerId}/cards")
    public List<CardResponse> playerCards(
            @PathVariable UUID gameId, @PathVariable int playerId) {
        return service.playerCards(gameId, playerId).stream()
            .map(CardResponse::from)
            .toList();
    }

    @PostMapping("/{playerId}/deal")
    public ResponseEntity<CardResponse> deal(
            @PathVariable UUID gameId, @PathVariable int playerId) {
        return service.dealCard(gameId, playerId)
            .map(card -> ResponseEntity.ok(CardResponse.from(card)))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
