package com.example.cardgame.web;

import com.example.cardgame.service.GameNotFoundException;
import com.example.cardgame.service.PlayerNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void gameNotFoundMapsTo404() {
        ProblemDetail pd = handler.handleGameNotFound(new GameNotFoundException(UUID.randomUUID()));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Game not found");
    }

    @Test
    void playerNotFoundMapsTo404() {
        ProblemDetail pd = handler.handlePlayerNotFound(new PlayerNotFoundException(7));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("Player not found");
    }
}
