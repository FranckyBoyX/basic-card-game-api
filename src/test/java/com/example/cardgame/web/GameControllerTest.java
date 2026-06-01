package com.example.cardgame.web;

import com.example.cardgame.domain.Game;
import com.example.cardgame.service.GameNotFoundException;
import com.example.cardgame.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.UUID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GameControllerTest {

    MockMvc mvc;
    GameService service;

    @BeforeEach
    void setUp() {
        service = mock(GameService.class);
        mvc = MockMvcBuilders
            .standaloneSetup(new GameController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void createGameReturns201WithGameId() throws Exception {
        Game game = new Game();
        when(service.createGame()).thenReturn(game);
        mvc.perform(post("/api/v1/games"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.gameId").value(game.id().toString()));
    }

    @Test
    void deleteGameReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(delete("/api/v1/games/{id}", id))
            .andExpect(status().isNoContent());
        verify(service).deleteGame(id);
    }

    @Test
    void deleteUnknownGameReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new GameNotFoundException(id)).when(service).deleteGame(id);
        mvc.perform(delete("/api/v1/games/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Game not found"));
    }

    @Test
    void addDeckReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/api/v1/games/{id}/decks", id))
            .andExpect(status().isNoContent());
        verify(service).addDeck(id);
    }

    @Test
    void shuffleReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/api/v1/games/{id}/shuffle", id))
            .andExpect(status().isNoContent());
        verify(service).shuffle(id);
    }
}
