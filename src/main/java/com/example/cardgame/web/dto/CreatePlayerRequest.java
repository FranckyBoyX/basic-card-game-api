package com.example.cardgame.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body to add a player. Name is a required game tag. */
public record CreatePlayerRequest(
    @NotBlank @Size(max = 50) String name
) {
}
