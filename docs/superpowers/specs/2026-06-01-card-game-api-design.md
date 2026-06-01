# Basic Deck-of-Cards Game API — Design

**Date:** 2026-06-01
**Status:** Approved design, pre-implementation
**Author:** François Pelletier (with AI-assisted brainstorming)

## 1. Problem

Implement a REST API representing a deck of poker-style playing cards and a very basic
multi-player game played from a "shoe" (one or more decks combined). The API must support
creating/deleting games, adding decks to a game's shoe, adding/removing players, dealing
cards, querying player hands and scores, querying remaining cards in the shoe, and shuffling
the shoe with a hand-rolled algorithm.

This is graded as the *foundation of a new product*: the decision-making, architecture, and
tradeoffs matter as much as correctness.

## 2. Technology choices

| Concern | Choice | Rationale |
|---|---|---|
| Language / runtime | Java 25 (LTS) | Records, modern language features; LTS support. |
| Framework | Spring Boot 4.0.6 | Familiar, mature REST stack. |
| Build | Maven | Simplicity, ubiquity. |
| Web stack | **Spring MVC** (not WebFlux) | Workload is in-memory and CPU/data-bound with zero blocking I/O; reactive's non-blocking/backpressure benefits don't apply and would only complicate thread-safety reasoning and debugging. |
| Persistence | **In-memory behind a repository interface** | Standalone service, no external infra required. A `GameRepository` interface with an `InMemoryGameRepository` (`ConcurrentHashMap`) keeps the door open to a real datastore without coupling. Migration path noted in §8. |
| Logging | Logback + SLF4J | Spring Boot default; structured, configurable. |
| Boilerplate | Lombok — `@Slf4j`, `@RequiredArgsConstructor` only | Records carry data; Lombok is not used for data classes (no `@Data`). |
| API docs | springdoc-openapi | Cheap, high-signal; live OpenAPI/Swagger UI. |
| Ops | Spring Boot Actuator | Health/info endpoints for production credibility. |
| Container image | **Jib** (Maven plugin) | Builds a reproducible, layered OCI image with no Dockerfile and no Docker daemon. |
| Testing | JUnit 5 + AssertJ + MockMvc | Fluent assertions; slice + integration tests. |

CI is intentionally out of scope for the time-box.

## 3. Architecture

Layered Spring MVC with dependencies pointing inward toward a framework-free domain:

```
controller  → REST endpoints, DTOs, request validation, domain ⇄ DTO mapping
service     → use-case orchestration, owns per-game locking policy
domain      → Card, Suit, Rank, Shoe, Game, Player — pure Java, no Spring/Jackson/JPA
repository  → GameRepository interface + InMemoryGameRepository (ConcurrentHashMap)
```

**Core principle:** the domain has zero framework annotations. All web concerns (DTOs,
validation, serialization) live in the controller layer; all concurrency policy lives in the
service layer. The domain is single-thread-correct and independently testable, and "doesn't
know" it sits behind REST or in memory. This is the central foundational-product argument.

## 4. Domain model

- **`Suit`** enum: `HEARTS, SPADES, CLUBS, DIAMONDS` — declared in this order, which drives the
  remaining-cards sort.
- **`Rank`** enum: `ACE(1), TWO(2) … TEN(10), JACK(11), QUEEN(12), KING(13)`. A single
  `faceValue` per rank serves both requirements: player scoring (Ace=1, King=13; e.g. 10+King=23)
  and the "high→low" remaining-cards ordering (descending `faceValue`: King…2, Ace).
- **`Card`** = `record(Suit suit, Rank rank)` — immutable, value equality for free.
- **`Shoe`** — the mutable aggregate. An ordered collection of *undealt* cards only. Owns
  `dealOne(): Optional<Card>` and the hand-rolled Fisher–Yates `shuffle()`. All shoe mutation
  funnels through here. Dealt cards leave the shoe and live in player hands.
- **`Player`** — `playerId` (`int`, see §5), display `name` (game tag), and an ordered
  `List<Card>` hand.
- **`Game`** — `gameId` (UUID), the `Shoe`, an ordered map of `Player`s, the per-game player-id
  counter, and the per-game lock (see §6).

## 5. Identifiers

- **Game id:** opaque `UUID`. The value is not meaningful, so no ordering information is implied.
- **Player id:** **per-game incrementing `int` starting at 1.** The number is meaningful — it
  records join order *within that game* (player 1 joined first). Each game has its own sequence
  (game A and game B both have a player 1). **Ids are monotonic and never recycled:** if player 2
  leaves, the next player is 3, preserving join-order meaning and avoiding stale-reference
  confusion. The counter lives on `Game` and is advanced under the per-game lock already held for
  player adds.

## 6. Concurrency

- **Game registry:** `ConcurrentHashMap<UUID, Game>` for game create/lookup/delete.
- **Per-game atomicity:** each `Game` carries its own `ReentrantLock`. The service acquires it
  around any compound shoe/player mutation — deal, shuffle, add deck, add/remove player — so these
  multi-step operations are atomic. Lock striping by game means operations on *different* games run
  fully in parallel; operations on the *same* game serialize.
- **No global lock.** A single lock would needlessly serialize unrelated games.
- The domain stays lock-free and single-thread-correct; the **service owns the locking policy**.
  The domain does not know about threads.

## 7. REST API

Base path `/api/v1`. Resources: games, their decks, players, and player hands. The three commands
(shuffle, deal) are modeled as `POST`s to action sub-resources — a deliberate, pragmatic tradeoff
against strict REST purism, justified by principle-of-least-surprise.

| Operation | Method & path | Notes |
|---|---|---|
| Create game | `POST /api/v1/games` | Empty body. `201` + `Location`. |
| Delete game | `DELETE /api/v1/games/{gameId}` | `204`; `404` if unknown. |
| Add a deck to the shoe | `POST /api/v1/games/{gameId}/decks` | Empty body; mints a fresh standard 52-card deck and merges it into the shoe. No standalone deck resource (see §9). Once added, cannot be removed. |
| Add player | `POST /api/v1/games/{gameId}/players` | Body `{ "name": "<tag>" }`, validated (see §10). `201` + `Location`. |
| Remove player | `DELETE /api/v1/games/{gameId}/players/{playerId}` | `204`; `404` if unknown. |
| List players + scores | `GET /api/v1/games/{gameId}/players` | Each player's total face value; sorted **descending** by total. |
| Get a player's cards | `GET /api/v1/games/{gameId}/players/{playerId}/cards` | Player's hand in deal order. |
| Deal one card | `POST /api/v1/games/{gameId}/players/{playerId}/deal` | Deals exactly one card from the top of the shoe to the player. Returns the dealt card; `204 No Content` when the shoe is empty (no card dealt — not an error). |
| Shuffle the shoe | `POST /api/v1/games/{gameId}/shuffle` | Hand-rolled permutation; returns no value (`204`). Callable any time. |
| Remaining count per suit | `GET /api/v1/games/{gameId}/shoe/suit-counts` | e.g. `{ hearts: 5, spades: 3, ... }`. |
| Remaining count per card | `GET /api/v1/games/{gameId}/shoe/cards` | Each (suit, value) count, sorted by suit (hearts, spades, clubs, diamonds) then face value high→low. |

### Dealing semantics
Dealing is **one card at a time** by design — no count parameter. A player needing multiple cards
calls `deal` repeatedly, mirroring how cards are dealt at a real table. This makes the atomic
primitive trivially "draw one from the top," sidesteps any partial-deal ambiguity, and satisfies
the spec's example (shuffle then 52 deals returns all 52 in random order; the 53rd deals nothing).

### Shuffle algorithm
Hand-rolled **Fisher–Yates** (unbiased, O(n)), using a library RNG (`ThreadLocalRandom` /
`SecureRandom`). No library `shuffle()` is used, per the assignment.

## 8. Persistence detail & migration path

`InMemoryGameRepository` stores games in a `ConcurrentHashMap<UUID, Game>`. If real persistence is
ever needed, the `GameRepository` interface is the seam to swap. A relational option (H2/JPA) is
viable but carries a known modeling wrinkle worth discussing: the shoe is an *ordered* sequence, so
relational storage needs an explicit position/index column to preserve card order — the in-memory
ordered collection avoids that friction.

## 9. Deck modeling decision

"Create a deck" is **not** a standalone resource. Adding a deck *is* its creation:
`POST /games/{id}/decks` mints a fresh 52-card deck and merges it into the shoe. This fits "once a
deck is added it cannot be removed" (no unattached-deck lifecycle to manage) and avoids questions
like where an orphan deck lives or whether it can be shared across games. The alternative
(first-class `Deck` resource with its own id) was rejected as added surface area for no benefit.

## 10. Validation & error handling

- **Validation:** the only request body with a field is `CreatePlayerRequest { name }`. The name is
  a required game tag with minimal validation — `@NotBlank` plus a `@Size(max=...)` cap. No
  first/last-name structure.
- **Error model:** `@RestControllerAdvice` producing RFC 9457 `ProblemDetail` responses
  (`type`, `title`, `status`, `detail`, offending id).
  - `404` — unknown game or player.
  - `400` — failed name validation, malformed JSON, or path-variable type mismatch (e.g. a player
    id that won't parse as `int`). The latter two come from the framework regardless of Bean
    Validation; handlers ensure they don't leak stack traces.
  - `204` — deal from an empty shoe (no card dealt; not an error).
  - No `409`: there is no natural conflict in this domain — no remove-deck endpoint, no player
    uniqueness constraint, and pessimistic per-game locking serializes concurrent operations rather
    than colliding.

## 11. Testing strategy

- **Domain unit tests (core):** Fisher–Yates yields a permutation containing exactly the original
  cards; deal exhaustion (52 deals succeed, 53rd returns empty); scoring + descending sort;
  suit-count and per-card-count values and ordering.
- **Shuffle tests:** one seeded-RNG test for determinism; a statistical test over many shuffles to
  demonstrate real randomization (not equal to identity / no position fixed beyond chance) without
  asserting an exact order.
- **Controller tests:** MockMvc / `@WebMvcTest` for status codes, payloads, and error mapping.
- **Integration:** `@SpringBootTest` happy-path through the full stack.
- **Concurrency test:** many threads dealing from one shoe → exactly the right number of cards
  dealt, no duplicates, no lost card.

## 12. Deliverables

- The running service: `mvn spring-boot:run`; container via `mvn jib:dockerBuild`.
- README with run instructions and the OpenAPI/Swagger URL.
- **AI-usage requirement** satisfied by: this design doc, the detailed implementation plan, and the
  exported AI chat transcript (JSONL session). No separate write-up needed — these together document
  how AI influenced the design and where the author overrode suggestions (e.g. pure one-at-a-time
  dealing chosen against the AI's count-parameter recommendation).
