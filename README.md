# Basic Deck-of-Cards Game API

A Spring Boot REST API for a multi-deck "shoe" card game with thread-safe,
in-memory state.

## Prerequisites

This project uses [mise](https://mise.jdx.dev) to pin the toolchain (Java 25 + Maven).

1. Install mise: https://mise.jdx.dev/getting-started.html#installing-mise-cli
2. From the project root, install the pinned tools:
   ```bash
   mise install
   ```
3. Activate mise in your shell so `java` and `mvn` are on your PATH:
   ```bash
   # bash / zsh — add this to your shell profile to make it permanent
   eval "$(mise activate bash)"   # or: eval "$(mise activate zsh)"
   ```
   Alternatively, prefix every command with `mise exec --` (e.g. `mise exec -- mvn test`).

## Run

```bash
mvn spring-boot:run
```

The API is served at `http://localhost:8080/api/v1`.

## API documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Health

- Actuator: `http://localhost:8080/actuator/health`

## Test

```bash
mvn test
```

## Container image (no Dockerfile, via Jib)

```bash
mvn jib:dockerBuild      # builds the image into the local Docker daemon
```

## Design

See `docs/superpowers/specs/2026-06-01-card-game-api-design.md` for architecture
and the tradeoffs behind the REST shape, concurrency model, persistence seam,
and shuffle implementation.

## Key endpoints

| Operation | Method & path |
|---|---|
| Create game | `POST /api/v1/games` |
| Delete game | `DELETE /api/v1/games/{gameId}` |
| Add deck to shoe | `POST /api/v1/games/{gameId}/decks` |
| Add player | `POST /api/v1/games/{gameId}/players` |
| Remove player | `DELETE /api/v1/games/{gameId}/players/{playerId}` |
| Players + scores (desc) | `GET /api/v1/games/{gameId}/players` |
| A player's cards | `GET /api/v1/games/{gameId}/players/{playerId}/cards` |
| Deal one card | `POST /api/v1/games/{gameId}/players/{playerId}/deal` |
| Shuffle shoe | `POST /api/v1/games/{gameId}/shuffle` |
| Remaining per suit | `GET /api/v1/games/{gameId}/shoe/suit-counts` |
| Remaining per card | `GET /api/v1/games/{gameId}/shoe/cards` |