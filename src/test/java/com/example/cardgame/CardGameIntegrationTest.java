package com.example.cardgame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CardGameIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void createGameAddDeckShuffleDealAndScore() throws Exception {
        String body = mvc.perform(post("/api/v1/games"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String gameId = objectMapper.readTree(body).get("gameId").asText();

        mvc.perform(post("/api/v1/games/{g}/decks", gameId)).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/games/{g}/shuffle", gameId)).andExpect(status().isNoContent());

        String pBody = mvc.perform(post("/api/v1/games/{g}/players", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"alice\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        int playerId = objectMapper.readTree(pBody).get("id").asInt();

        for (int i = 0; i < 52; i++) {
            mvc.perform(post("/api/v1/games/{g}/players/{p}/deal", gameId, playerId))
                .andExpect(status().isOk());
        }
        mvc.perform(post("/api/v1/games/{g}/players/{p}/deal", gameId, playerId))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/games/{g}/shoe/suit-counts", gameId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.HEARTS").value(0))
            .andExpect(jsonPath("$.SPADES").value(0))
            .andExpect(jsonPath("$.CLUBS").value(0))
            .andExpect(jsonPath("$.DIAMONDS").value(0));

        String cards = mvc.perform(get("/api/v1/games/{g}/players/{p}/cards", gameId, playerId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        JsonNode arr = objectMapper.readTree(cards);
        assertThat(arr.size()).isEqualTo(52);

        // Total value = 4 suits * (1+2+...+13) = 4 * 91 = 364
        mvc.perform(get("/api/v1/games/{g}/players", gameId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalValue").value(364));
    }
}
