package com.example.cardgame.web;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.CardCount;
import com.example.cardgame.domain.Rank;
import com.example.cardgame.domain.Suit;
import com.example.cardgame.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ShoeControllerTest {

    MockMvc mvc;
    GameService service;

    @BeforeEach
    void setUp() {
        service = mock(GameService.class);
        mvc = MockMvcBuilders
            .standaloneSetup(new ShoeController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void suitCountsReturnsPerSuitMap() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(service.suitCounts(gameId)).thenReturn(Map.of(
            Suit.HEARTS, 5, Suit.SPADES, 3, Suit.CLUBS, 0, Suit.DIAMONDS, 1));
        mvc.perform(get("/api/v1/games/{g}/shoe/suit-counts", gameId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.HEARTS").value(5))
            .andExpect(jsonPath("$.SPADES").value(3));
    }

    @Test
    void cardCountsReturnsSortedList() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(service.cardCounts(gameId)).thenReturn(List.of(
            new CardCount(new Card(Suit.HEARTS, Rank.KING), 1),
            new CardCount(new Card(Suit.HEARTS, Rank.ACE), 1)));
        mvc.perform(get("/api/v1/games/{g}/shoe/cards", gameId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].suit").value("HEARTS"))
            .andExpect(jsonPath("$[0].rank").value("KING"))
            .andExpect(jsonPath("$[0].value").value(13))
            .andExpect(jsonPath("$[0].count").value(1))
            .andExpect(jsonPath("$[1].rank").value("ACE"));
    }
}
