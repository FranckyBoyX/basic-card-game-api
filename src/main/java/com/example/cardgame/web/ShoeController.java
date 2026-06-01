package com.example.cardgame.web;

import com.example.cardgame.domain.Suit;
import com.example.cardgame.service.GameService;
import com.example.cardgame.web.dto.CardCountResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games/{gameId}/shoe")
@RequiredArgsConstructor
public class ShoeController {

    private final GameService service;

    @GetMapping("/suit-counts")
    public Map<Suit, Integer> suitCounts(@PathVariable UUID gameId) {
        return service.suitCounts(gameId);
    }

    @GetMapping("/cards")
    public List<CardCountResponse> cardCounts(@PathVariable UUID gameId) {
        return service.cardCounts(gameId).stream()
            .map(CardCountResponse::from)
            .toList();
    }
}
