package com.example.cardgame.web;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.Player;
import com.example.cardgame.domain.Rank;
import com.example.cardgame.domain.Suit;
import com.example.cardgame.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlayerControllerTest {

    MockMvc mvc;
    GameService service;

    @BeforeEach
    void setUp() {
        service = mock(GameService.class);
        mvc = MockMvcBuilders
            .standaloneSetup(new PlayerController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void addPlayerReturns201() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(service.addPlayer(eq(gameId), eq("alice"))).thenReturn(new Player(1, "alice"));
        mvc.perform(post("/api/v1/games/{g}/players", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"alice\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("alice"));
    }

    @Test
    void addPlayerWithBlankNameReturns400() throws Exception {
        UUID gameId = UUID.randomUUID();
        mvc.perform(post("/api/v1/games/{g}/players", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void removePlayerReturns204() throws Exception {
        UUID gameId = UUID.randomUUID();
        mvc.perform(delete("/api/v1/games/{g}/players/{p}", gameId, 1))
            .andExpect(status().isNoContent());
    }

    @Test
    void listPlayersReturnsScoresDescending() throws Exception {
        UUID gameId = UUID.randomUUID();
        Player a = new Player(1, "A");
        a.addCard(new Card(Suit.HEARTS, Rank.TEN));
        a.addCard(new Card(Suit.SPADES, Rank.KING)); // 23
        Player b = new Player(2, "B");
        b.addCard(new Card(Suit.CLUBS, Rank.SEVEN));
        b.addCard(new Card(Suit.DIAMONDS, Rank.QUEEN)); // 19
        when(service.playersByScore(gameId)).thenReturn(List.of(a, b));
        mvc.perform(get("/api/v1/games/{g}/players", gameId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("A"))
            .andExpect(jsonPath("$[0].totalValue").value(23))
            .andExpect(jsonPath("$[1].totalValue").value(19));
    }

    @Test
    void getPlayerCardsReturnsHand() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(service.playerCards(gameId, 1))
            .thenReturn(List.of(new Card(Suit.HEARTS, Rank.ACE)));
        mvc.perform(get("/api/v1/games/{g}/players/{p}/cards", gameId, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].suit").value("HEARTS"))
            .andExpect(jsonPath("$[0].rank").value("ACE"))
            .andExpect(jsonPath("$[0].value").value(1));
    }

    @Test
    void dealReturns200WithCard() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(service.dealCard(gameId, 1))
            .thenReturn(Optional.of(new Card(Suit.SPADES, Rank.KING)));
        mvc.perform(post("/api/v1/games/{g}/players/{p}/deal", gameId, 1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.suit").value("SPADES"))
            .andExpect(jsonPath("$.value").value(13));
    }

    @Test
    void dealFromEmptyShoeReturns204() throws Exception {
        UUID gameId = UUID.randomUUID();
        when(service.dealCard(gameId, 1)).thenReturn(Optional.empty());
        mvc.perform(post("/api/v1/games/{g}/players/{p}/deal", gameId, 1))
            .andExpect(status().isNoContent());
    }
}
