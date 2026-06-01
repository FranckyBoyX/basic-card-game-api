# Basic Card-Game API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-quality Spring Boot REST API for a multi-deck "shoe" card game with thread-safe, in-memory state.

**Architecture:** Layered Spring MVC (controller → service → domain → repository) with a framework-free domain, in-memory repository behind an interface, and per-game locking owned by the service layer.

**Tech Stack:** Java 25, Spring Boot 4.0.6, Maven (via `spring-boot-starter-parent`), Spring Web (MVC), Lombok (`@Slf4j`/`@RequiredArgsConstructor` only), springdoc-openapi, Actuator, Jib, JUnit 5 + AssertJ + MockMvc. Toolchain (Java + Maven) pinned with **mise**.

**Spec:** `docs/superpowers/specs/2026-06-01-card-game-api-design.md`

**Conventions:** Base Java package `com.example.cardgame`. Group `com.example`, artifact `cardgame`. Commits never use `--no-verify`. Run tests with `mvn -q test` (Maven supplied by mise).

---

## File Structure

```
mise.toml                                            # Pins Java 25 + Maven
pom.xml                                              # spring-boot-starter-parent, deps, Jib
src/main/resources/application.yml                   # Server + actuator + springdoc config
src/main/java/com/example/cardgame/
  CardGameApplication.java                           # Spring Boot entry point
  domain/
    Suit.java                                        # enum, declared in sort order
    Rank.java                                         # enum carrying faceValue
    Card.java                                         # record(Suit, Rank)
    CardCount.java                                    # record(Card, int) for remaining-count view
    Shoe.java                                         # undealt cards; deal, Fisher-Yates, counts
    Player.java                                       # id, name, hand, totalValue
    Game.java                                         # UUID, Shoe, players, player-seq, lock
  repository/
    GameRepository.java                              # interface
    InMemoryGameRepository.java                      # ConcurrentHashMap impl
  service/
    GameService.java                                 # orchestration + per-game locking
    GameNotFoundException.java
    PlayerNotFoundException.java
  web/
    GameController.java                              # game lifecycle, decks, shuffle
    PlayerController.java                            # players, hands, deal
    ShoeController.java                              # remaining-count views
    GlobalExceptionHandler.java                     # RFC 9457 ProblemDetail
    dto/                                             # thin response records w/ static from(...)
      CreatePlayerRequest.java
      GameResponse.java
      PlayerResponse.java
      PlayerScoreResponse.java
      CardResponse.java
      CardCountResponse.java
src/test/java/com/example/cardgame/                  # mirrors main tree
```

---

## Task 1: Project scaffold (mise + Maven + Spring Boot)

**Files:**
- Create: `mise.toml`
- Create: `pom.xml`
- Create: `src/main/java/com/example/cardgame/CardGameApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `.gitignore`

- [ ] **Step 1: Create `.gitignore`**

```gitignore
target/
*.class
.idea/
*.iml
.vscode/
```

- [ ] **Step 2: Create `mise.toml`**

```toml
[tools]
java = "25"
maven = "3.9.9"
```

- [ ] **Step 3: Install the toolchain**

Run: `mise install`
Expected: mise installs Java 25 and Maven 3.9.9. Verify with `mise exec -- java -version` (reports 25) and `mise exec -- mvn -version`.

- [ ] **Step 4: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.6</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>cardgame</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>cardgame</name>
    <description>Basic deck-of-cards game REST API</description>

    <properties>
        <java.version>25</java.version>
        <springdoc.version>2.8.6</springdoc.version>
        <jib.version>3.4.4</jib.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>com.google.cloud.tools</groupId>
                <artifactId>jib-maven-plugin</artifactId>
                <version>${jib.version}</version>
                <configuration>
                    <to>
                        <image>cardgame:${project.version}</image>
                    </to>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

> Note: if the build reports an incompatible `springdoc-openapi` version for Spring Boot 4, bump `springdoc.version` to the latest `springdoc-openapi-starter-webmvc-ui` release and re-run. Same for `jib.version`.

- [ ] **Step 5: Create `CardGameApplication.java`**

```java
package com.example.cardgame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CardGameApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardGameApplication.class, args);
    }
}
```

- [ ] **Step 6: Create `application.yml`**

```yaml
spring:
  application:
    name: cardgame
server:
  port: 8080
management:
  endpoints:
    web:
      exposure:
        include: health,info
springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

- [ ] **Step 7: Verify the app context compiles**

Run: `mvn -q test`
Expected: BUILD SUCCESS (no tests yet, so this just compiles and runs zero tests).

- [ ] **Step 8: Commit**

```bash
git add mise.toml pom.xml .gitignore src
git commit -m "chore: scaffold Spring Boot project with mise toolchain"
```

---

## Task 2: Suit and Rank enums

**Files:**
- Create: `src/main/java/com/example/cardgame/domain/Suit.java`
- Create: `src/main/java/com/example/cardgame/domain/Rank.java`
- Test: `src/test/java/com/example/cardgame/domain/RankTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RankTest {

    @Test
    void aceIsWorthOneAndKingIsWorthThirteen() {
        assertThat(Rank.ACE.faceValue()).isEqualTo(1);
        assertThat(Rank.KING.faceValue()).isEqualTo(13);
    }

    @Test
    void faceValuesAreSequentialFromAceToKing() {
        assertThat(Rank.values())
            .extracting(Rank::faceValue)
            .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13);
    }

    @Test
    void suitsAreDeclaredInSortOrder() {
        assertThat(Suit.values())
            .containsExactly(Suit.HEARTS, Suit.SPADES, Suit.CLUBS, Suit.DIAMONDS);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=RankTest test`
Expected: COMPILATION FAILURE (`Suit`, `Rank` not defined).

- [ ] **Step 3: Create `Suit.java`**

```java
package com.example.cardgame.domain;

/** Card suits, declared in the order used to sort remaining cards. */
public enum Suit {
    HEARTS, SPADES, CLUBS, DIAMONDS
}
```

- [ ] **Step 4: Create `Rank.java`**

```java
package com.example.cardgame.domain;

/** Card ranks. {@code faceValue} drives both scoring (Ace=1..King=13)
 *  and the high-to-low remaining-card ordering. */
public enum Rank {
    ACE(1), TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7),
    EIGHT(8), NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13);

    private final int faceValue;

    Rank(int faceValue) {
        this.faceValue = faceValue;
    }

    public int faceValue() {
        return faceValue;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -Dtest=RankTest test`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/cardgame/domain src/test/java/com/example/cardgame/domain
git commit -m "feat: add Suit and Rank enums"
```

---

## Task 3: Card record

**Files:**
- Create: `src/main/java/com/example/cardgame/domain/Card.java`
- Test: `src/test/java/com/example/cardgame/domain/CardTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardTest {

    @Test
    void cardsWithSameSuitAndRankAreEqual() {
        assertThat(new Card(Suit.HEARTS, Rank.ACE))
            .isEqualTo(new Card(Suit.HEARTS, Rank.ACE));
    }

    @Test
    void rejectsNullSuitOrRank() {
        assertThatThrownBy(() -> new Card(null, Rank.ACE))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Card(Suit.HEARTS, null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=CardTest test`
Expected: COMPILATION FAILURE (`Card` not defined).

- [ ] **Step 3: Create `Card.java`**

```java
package com.example.cardgame.domain;

/** An immutable playing card. */
public record Card(Suit suit, Rank rank) {
    public Card {
        if (suit == null || rank == null) {
            throw new IllegalArgumentException("suit and rank are required");
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=CardTest test`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/domain/Card.java src/test/java/com/example/cardgame/domain/CardTest.java
git commit -m "feat: add Card record"
```

---

## Task 4: CardCount record

**Files:**
- Create: `src/main/java/com/example/cardgame/domain/CardCount.java`
- Test: `src/test/java/com/example/cardgame/domain/CardCountTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CardCountTest {

    @Test
    void exposesCardAndCount() {
        Card card = new Card(Suit.SPADES, Rank.KING);
        CardCount cc = new CardCount(card, 2);
        assertThat(cc.card()).isEqualTo(card);
        assertThat(cc.count()).isEqualTo(2);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=CardCountTest test`
Expected: COMPILATION FAILURE (`CardCount` not defined).

- [ ] **Step 3: Create `CardCount.java`**

```java
package com.example.cardgame.domain;

/** How many of a given card remain in the shoe. */
public record CardCount(Card card, int count) {
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=CardCountTest test`
Expected: PASS (1 test).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/domain/CardCount.java src/test/java/com/example/cardgame/domain/CardCountTest.java
git commit -m "feat: add CardCount record"
```

---

## Task 5: Shoe — construction, deal, counts

**Files:**
- Create: `src/main/java/com/example/cardgame/domain/Shoe.java`
- Test: `src/test/java/com/example/cardgame/domain/ShoeTest.java`

- [ ] **Step 1: Write the failing test (construction, deal, counts)**

```java
package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class ShoeTest {

    @Test
    void newShoeIsEmpty() {
        assertThat(new Shoe().size()).isZero();
    }

    @Test
    void addingOneDeckGivesFiftyTwoDistinctCards() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        assertThat(shoe.size()).isEqualTo(52);
        assertThat(shoe.remaining()).doesNotHaveDuplicates();
    }

    @Test
    void addingTwoDecksGivesOneHundredFourCards() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        shoe.addDeck();
        assertThat(shoe.size()).isEqualTo(104);
    }

    @Test
    void dealsAllFiftyTwoCardsThenReturnsEmpty() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        for (int i = 0; i < 52; i++) {
            assertThat(shoe.dealOne()).isPresent();
        }
        assertThat(shoe.dealOne()).isEqualTo(Optional.empty());
        assertThat(shoe.size()).isZero();
    }

    @Test
    void suitCountsIncludeAllFourSuitsAndSumToSize() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        assertThat(shoe.suitCounts())
            .containsKeys(Suit.HEARTS, Suit.SPADES, Suit.CLUBS, Suit.DIAMONDS)
            .containsValues(13, 13, 13, 13);
    }

    @Test
    void cardCountsAreSortedBySuitThenFaceValueDescending() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        var counts = shoe.cardCounts();
        assertThat(counts).hasSize(52);
        // first entry: HEARTS KING; 13th: HEARTS ACE; 14th: SPADES KING
        assertThat(counts.get(0).card()).isEqualTo(new Card(Suit.HEARTS, Rank.KING));
        assertThat(counts.get(12).card()).isEqualTo(new Card(Suit.HEARTS, Rank.ACE));
        assertThat(counts.get(13).card()).isEqualTo(new Card(Suit.SPADES, Rank.KING));
        assertThat(counts).allSatisfy(cc -> assertThat(cc.count()).isEqualTo(1));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ShoeTest test`
Expected: COMPILATION FAILURE (`Shoe` not defined).

- [ ] **Step 3: Create `Shoe.java` (shuffle added in Task 6)**

```java
package com.example.cardgame.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The game's shoe: the ordered collection of UNDEALT cards.
 * Not thread-safe by design — concurrency is managed by the service layer.
 */
public class Shoe {

    private static final Comparator<Card> REMAINING_ORDER =
        Comparator.comparingInt((Card c) -> c.suit().ordinal())
                  .thenComparing(Comparator.comparingInt((Card c) -> c.rank().faceValue()).reversed());

    private final Deque<Card> cards = new ArrayDeque<>();

    /** Adds a fresh standard 52-card deck to the bottom of the shoe. */
    public void addDeck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.addLast(new Card(suit, rank));
            }
        }
    }

    /** Deals one card from the top, or empty if the shoe is exhausted. */
    public Optional<Card> dealOne() {
        return Optional.ofNullable(cards.pollFirst());
    }

    public int size() {
        return cards.size();
    }

    /** Snapshot of remaining cards in current shoe order. */
    public List<Card> remaining() {
        return List.copyOf(cards);
    }

    /** Remaining count per suit; all four suits always present (0 if none). */
    public Map<Suit, Integer> suitCounts() {
        Map<Suit, Integer> counts = new EnumMap<>(Suit.class);
        for (Suit suit : Suit.values()) {
            counts.put(suit, 0);
        }
        for (Card card : cards) {
            counts.merge(card.suit(), 1, Integer::sum);
        }
        return counts;
    }

    /** Remaining cards with counts, sorted by suit then face value high-to-low. */
    public List<CardCount> cardCounts() {
        Map<Card, Integer> tally = new LinkedHashMap<>();
        for (Card card : cards) {
            tally.merge(card, 1, Integer::sum);
        }
        List<CardCount> result = new ArrayList<>();
        tally.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(REMAINING_ORDER))
            .forEach(e -> result.add(new CardCount(e.getKey(), e.getValue())));
        return result;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ShoeTest test`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/domain/Shoe.java src/test/java/com/example/cardgame/domain/ShoeTest.java
git commit -m "feat: add Shoe with deal and remaining-count views"
```

---

## Task 6: Shoe — hand-rolled Fisher-Yates shuffle

**Files:**
- Modify: `src/main/java/com/example/cardgame/domain/Shoe.java`
- Test: `src/test/java/com/example/cardgame/domain/ShoeShuffleTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ShoeShuffleTest {

    @Test
    void shufflePreservesExactlyTheSameMultisetOfCards() {
        Shoe shoe = new Shoe();
        shoe.addDeck();
        List<Card> before = shoe.remaining();
        shoe.shuffle();
        List<Card> after = shoe.remaining();
        assertThat(after).hasSameSizeAs(before);
        assertThat(after).containsExactlyInAnyOrderElementsOf(before);
    }

    @Test
    void shuffleChangesOrderAtLeastOnceOverManyRuns() {
        // A correct shuffle will, with overwhelming probability, change the
        // order of 52 cards in at least one of several attempts.
        boolean changedAtLeastOnce = false;
        for (int attempt = 0; attempt < 5 && !changedAtLeastOnce; attempt++) {
            Shoe shoe = new Shoe();
            shoe.addDeck();
            List<Card> before = shoe.remaining();
            shoe.shuffle();
            if (!shoe.remaining().equals(before)) {
                changedAtLeastOnce = true;
            }
        }
        assertThat(changedAtLeastOnce).isTrue();
    }

    @Test
    void shuffleOfEmptyShoeIsNoOp() {
        Shoe shoe = new Shoe();
        shoe.shuffle();
        assertThat(shoe.size()).isZero();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ShoeShuffleTest test`
Expected: COMPILATION FAILURE (`shuffle` not defined).

- [ ] **Step 3: Add `shuffle()` to `Shoe.java`**

Add these imports near the other `java.util` imports:

```java
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
```

Add this method to the `Shoe` class:

```java
    /**
     * Randomly permutes the shoe using a hand-rolled Fisher-Yates shuffle.
     * Uses a library RNG but NOT a library shuffle, per the assignment.
     */
    public void shuffle() {
        Card[] arr = cards.toArray(new Card[0]);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            Card tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        cards.clear();
        Collections.addAll(cards, arr);
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ShoeShuffleTest test`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/domain/Shoe.java src/test/java/com/example/cardgame/domain/ShoeShuffleTest.java
git commit -m "feat: add hand-rolled Fisher-Yates shuffle to Shoe"
```

---

## Task 7: Player

**Files:**
- Create: `src/main/java/com/example/cardgame/domain/Player.java`
- Test: `src/test/java/com/example/cardgame/domain/PlayerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlayerTest {

    @Test
    void newPlayerHasIdNameAndEmptyHand() {
        Player p = new Player(1, "alice");
        assertThat(p.id()).isEqualTo(1);
        assertThat(p.name()).isEqualTo("alice");
        assertThat(p.hand()).isEmpty();
        assertThat(p.totalValue()).isZero();
    }

    @Test
    void totalValueSumsFaceValues() {
        Player p = new Player(1, "alice");
        p.addCard(new Card(Suit.HEARTS, Rank.TEN));
        p.addCard(new Card(Suit.SPADES, Rank.KING));
        assertThat(p.totalValue()).isEqualTo(23); // 10 + 13
    }

    @Test
    void handPreservesDealOrderAndIsUnmodifiable() {
        Player p = new Player(1, "alice");
        Card first = new Card(Suit.HEARTS, Rank.TWO);
        Card second = new Card(Suit.CLUBS, Rank.NINE);
        p.addCard(first);
        p.addCard(second);
        assertThat(p.hand()).containsExactly(first, second);
        assertThatThrownBy(() -> p.hand().add(first))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=PlayerTest test`
Expected: COMPILATION FAILURE (`Player` not defined).

- [ ] **Step 3: Create `Player.java`**

```java
package com.example.cardgame.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A player in a game, holding an ordered hand of cards. Not thread-safe. */
public class Player {

    private final int id;
    private final String name;
    private final List<Card> hand = new ArrayList<>();

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void addCard(Card card) {
        hand.add(card);
    }

    /** Unmodifiable view of the hand in deal order. */
    public List<Card> hand() {
        return Collections.unmodifiableList(hand);
    }

    public int totalValue() {
        return hand.stream().mapToInt(c -> c.rank().faceValue()).sum();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=PlayerTest test`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/domain/Player.java src/test/java/com/example/cardgame/domain/PlayerTest.java
git commit -m "feat: add Player with hand and scoring"
```

---

## Task 8: Game

**Files:**
- Create: `src/main/java/com/example/cardgame/domain/Game.java`
- Test: `src/test/java/com/example/cardgame/domain/GameTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.domain;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class GameTest {

    @Test
    void newGameHasIdEmptyShoeAndNoPlayers() {
        Game game = new Game();
        assertThat(game.id()).isNotNull();
        assertThat(game.shoe().size()).isZero();
        assertThat(game.players()).isEmpty();
    }

    @Test
    void addPlayerAssignsIncrementingIdsStartingAtOne() {
        Game game = new Game();
        Player a = game.addPlayer("alice");
        Player b = game.addPlayer("bob");
        assertThat(a.id()).isEqualTo(1);
        assertThat(b.id()).isEqualTo(2);
        assertThat(game.players()).containsExactly(a, b);
    }

    @Test
    void playerIdsAreNotRecycledAfterRemoval() {
        Game game = new Game();
        game.addPlayer("alice");            // id 1
        Player bob = game.addPlayer("bob");  // id 2
        game.removePlayer(bob.id());
        Player carol = game.addPlayer("carol");
        assertThat(carol.id()).isEqualTo(3);
    }

    @Test
    void findPlayerReturnsEmptyForUnknownId() {
        Game game = new Game();
        assertThat(game.findPlayer(99)).isEqualTo(Optional.empty());
    }

    @Test
    void removePlayerReturnsFalseForUnknownId() {
        Game game = new Game();
        assertThat(game.removePlayer(99)).isFalse();
    }

    @Test
    void lockIsAvailableForServiceLayer() {
        assertThat(new Game().lock()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=GameTest test`
Expected: COMPILATION FAILURE (`Game` not defined).

- [ ] **Step 3: Create `Game.java`**

```java
package com.example.cardgame.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A game aggregate: one shoe plus its players.
 * Single-thread-correct; the {@link #lock()} is provided for the service
 * layer to enforce atomicity of compound operations. The domain never
 * acquires the lock itself.
 */
public class Game {

    private final UUID id = UUID.randomUUID();
    private final Shoe shoe = new Shoe();
    private final Map<Integer, Player> players = new LinkedHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private int nextPlayerId = 1;

    public UUID id() {
        return id;
    }

    public Shoe shoe() {
        return shoe;
    }

    public ReentrantLock lock() {
        return lock;
    }

    /** Players in join order. */
    public List<Player> players() {
        return new ArrayList<>(players.values());
    }

    public Player addPlayer(String name) {
        Player player = new Player(nextPlayerId++, name);
        players.put(player.id(), player);
        return player;
    }

    public boolean removePlayer(int playerId) {
        return players.remove(playerId) != null;
    }

    public Optional<Player> findPlayer(int playerId) {
        return Optional.ofNullable(players.get(playerId));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=GameTest test`
Expected: PASS (6 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/domain/Game.java src/test/java/com/example/cardgame/domain/GameTest.java
git commit -m "feat: add Game aggregate with player sequencing"
```

---

## Task 9: GameRepository + in-memory implementation

**Files:**
- Create: `src/main/java/com/example/cardgame/repository/GameRepository.java`
- Create: `src/main/java/com/example/cardgame/repository/InMemoryGameRepository.java`
- Test: `src/test/java/com/example/cardgame/repository/InMemoryGameRepositoryTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.repository;

import com.example.cardgame.domain.Game;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class InMemoryGameRepositoryTest {

    private final GameRepository repository = new InMemoryGameRepository();

    @Test
    void savesAndFindsGameById() {
        Game game = new Game();
        repository.save(game);
        assertThat(repository.findById(game.id())).contains(game);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID())).isEqualTo(Optional.empty());
    }

    @Test
    void deletesGameById() {
        Game game = new Game();
        repository.save(game);
        assertThat(repository.deleteById(game.id())).isTrue();
        assertThat(repository.findById(game.id())).isEmpty();
    }

    @Test
    void deleteReturnsFalseForUnknownId() {
        assertThat(repository.deleteById(UUID.randomUUID())).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=InMemoryGameRepositoryTest test`
Expected: COMPILATION FAILURE (`GameRepository` not defined).

- [ ] **Step 3: Create `GameRepository.java`**

```java
package com.example.cardgame.repository;

import com.example.cardgame.domain.Game;
import java.util.Optional;
import java.util.UUID;

/** Persistence seam for games. Implemented in-memory; swappable for a datastore. */
public interface GameRepository {
    Game save(Game game);
    Optional<Game> findById(UUID id);
    boolean deleteById(UUID id);
}
```

- [ ] **Step 4: Create `InMemoryGameRepository.java`**

```java
package com.example.cardgame.repository;

import com.example.cardgame.domain.Game;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryGameRepository implements GameRepository {

    private final ConcurrentMap<UUID, Game> games = new ConcurrentHashMap<>();

    @Override
    public Game save(Game game) {
        games.put(game.id(), game);
        return game;
    }

    @Override
    public Optional<Game> findById(UUID id) {
        return Optional.ofNullable(games.get(id));
    }

    @Override
    public boolean deleteById(UUID id) {
        return games.remove(id) != null;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q -Dtest=InMemoryGameRepositoryTest test`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/cardgame/repository src/test/java/com/example/cardgame/repository
git commit -m "feat: add in-memory game repository behind interface"
```

---

## Task 10: Service exceptions

**Files:**
- Create: `src/main/java/com/example/cardgame/service/GameNotFoundException.java`
- Create: `src/main/java/com/example/cardgame/service/PlayerNotFoundException.java`

- [ ] **Step 1: Create `GameNotFoundException.java`**

```java
package com.example.cardgame.service;

import java.util.UUID;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(UUID gameId) {
        super("Game not found: " + gameId);
    }
}
```

- [ ] **Step 2: Create `PlayerNotFoundException.java`**

```java
package com.example.cardgame.service;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(int playerId) {
        super("Player not found: " + playerId);
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `mvn -q test-compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/example/cardgame/service
git commit -m "feat: add service not-found exceptions"
```

---

## Task 11: GameService orchestration and locking

**Files:**
- Create: `src/main/java/com/example/cardgame/service/GameService.java`
- Test: `src/test/java/com/example/cardgame/service/GameServiceTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.service;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.Game;
import com.example.cardgame.domain.Player;
import com.example.cardgame.domain.Rank;
import com.example.cardgame.domain.Suit;
import com.example.cardgame.repository.InMemoryGameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameServiceTest {

    private GameService service;

    @BeforeEach
    void setUp() {
        service = new GameService(new InMemoryGameRepository());
    }

    @Test
    void createGameReturnsPersistedGame() {
        Game game = service.createGame();
        assertThat(service.getGame(game.id())).isEqualTo(game);
    }

    @Test
    void deleteUnknownGameThrows() {
        assertThatThrownBy(() -> service.deleteGame(UUID.randomUUID()))
            .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void getUnknownGameThrows() {
        assertThatThrownBy(() -> service.getGame(UUID.randomUUID()))
            .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void addDeckIncreasesShoeByFiftyTwo() {
        Game game = service.createGame();
        service.addDeck(game.id());
        assertThat(game.shoe().size()).isEqualTo(52);
    }

    @Test
    void addAndRemovePlayer() {
        Game game = service.createGame();
        Player p = service.addPlayer(game.id(), "alice");
        assertThat(p.id()).isEqualTo(1);
        service.removePlayer(game.id(), p.id());
        assertThat(game.findPlayer(p.id())).isEqualTo(Optional.empty());
    }

    @Test
    void removeUnknownPlayerThrows() {
        Game game = service.createGame();
        assertThatThrownBy(() -> service.removePlayer(game.id(), 99))
            .isInstanceOf(PlayerNotFoundException.class);
    }

    @Test
    void dealMovesOneCardFromShoeToPlayer() {
        Game game = service.createGame();
        service.addDeck(game.id());
        Player p = service.addPlayer(game.id(), "alice");
        Optional<Card> dealt = service.dealCard(game.id(), p.id());
        assertThat(dealt).isPresent();
        assertThat(game.shoe().size()).isEqualTo(51);
        assertThat(game.findPlayer(p.id()).orElseThrow().hand()).hasSize(1);
    }

    @Test
    void dealFromEmptyShoeReturnsEmptyAndDealsNothing() {
        Game game = service.createGame();
        service.addDeck(game.id());
        Player p = service.addPlayer(game.id(), "alice");
        for (int i = 0; i < 52; i++) {
            service.dealCard(game.id(), p.id());
        }
        assertThat(service.dealCard(game.id(), p.id())).isEqualTo(Optional.empty());
        assertThat(game.findPlayer(p.id()).orElseThrow().hand()).hasSize(52);
    }

    @Test
    void playersAreSortedByTotalValueDescending() {
        Game game = service.createGame();
        Player a = service.addPlayer(game.id(), "A");
        Player b = service.addPlayer(game.id(), "B");
        a.addCard(new Card(Suit.HEARTS, Rank.TEN));   // 10
        a.addCard(new Card(Suit.SPADES, Rank.KING));  // 13 -> 23
        b.addCard(new Card(Suit.CLUBS, Rank.SEVEN));  // 7
        b.addCard(new Card(Suit.DIAMONDS, Rank.QUEEN)); // 12 -> 19
        List<Player> ranked = service.playersByScore(game.id());
        assertThat(ranked).containsExactly(a, b);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=GameServiceTest test`
Expected: COMPILATION FAILURE (`GameService` not defined).

- [ ] **Step 3: Create `GameService.java`**

```java
package com.example.cardgame.service;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.CardCount;
import com.example.cardgame.domain.Game;
import com.example.cardgame.domain.Player;
import com.example.cardgame.domain.Suit;
import com.example.cardgame.repository.GameRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates game use-cases and owns the concurrency policy: every compound
 * mutation of a single game runs under that game's lock, so operations on the
 * same game serialize while different games run in parallel.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameService {

    private final GameRepository repository;

    public Game createGame() {
        Game game = repository.save(new Game());
        log.info("Created game {}", game.id());
        return game;
    }

    public Game getGame(UUID gameId) {
        return repository.findById(gameId)
            .orElseThrow(() -> new GameNotFoundException(gameId));
    }

    public void deleteGame(UUID gameId) {
        if (!repository.deleteById(gameId)) {
            throw new GameNotFoundException(gameId);
        }
        log.info("Deleted game {}", gameId);
    }

    public void addDeck(UUID gameId) {
        withGameLock(gameId, game -> {
            game.shoe().addDeck();
            return null;
        });
    }

    public Player addPlayer(UUID gameId, String name) {
        return withGameLock(gameId, game -> game.addPlayer(name));
    }

    public void removePlayer(UUID gameId, int playerId) {
        withGameLock(gameId, game -> {
            if (!game.removePlayer(playerId)) {
                throw new PlayerNotFoundException(playerId);
            }
            return null;
        });
    }

    public Optional<Card> dealCard(UUID gameId, int playerId) {
        return withGameLock(gameId, game -> {
            Player player = game.findPlayer(playerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId));
            Optional<Card> card = game.shoe().dealOne();
            card.ifPresent(player::addCard);
            return card;
        });
    }

    public void shuffle(UUID gameId) {
        withGameLock(gameId, game -> {
            game.shoe().shuffle();
            return null;
        });
    }

    public List<Player> playersByScore(UUID gameId) {
        return withGameLock(gameId, game -> game.players().stream()
            .sorted(Comparator.comparingInt(Player::totalValue).reversed())
            .toList());
    }

    public List<Card> playerCards(UUID gameId, int playerId) {
        return withGameLock(gameId, game -> game.findPlayer(playerId)
            .orElseThrow(() -> new PlayerNotFoundException(playerId))
            .hand());
    }

    public Map<Suit, Integer> suitCounts(UUID gameId) {
        return withGameLock(gameId, game -> game.shoe().suitCounts());
    }

    public List<CardCount> cardCounts(UUID gameId) {
        return withGameLock(gameId, game -> game.shoe().cardCounts());
    }

    /** Runs an action under the target game's lock, ensuring atomicity. */
    private <T> T withGameLock(UUID gameId, Function<Game, T> action) {
        Game game = getGame(gameId);
        ReentrantLock lock = game.lock();
        lock.lock();
        try {
            return action.apply(game);
        } finally {
            lock.unlock();
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=GameServiceTest test`
Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/service/GameService.java src/test/java/com/example/cardgame/service/GameServiceTest.java
git commit -m "feat: add GameService with per-game locking"
```

---

## Task 12: Thin response DTOs with static factories

No mapper class — each response record exposes a static `from(...)` that adapts a domain
object. `CreatePlayerRequest` is the one input DTO (carries validation). The domain stays
free of web/Jackson annotations.

**Files:**
- Create: `src/main/java/com/example/cardgame/web/dto/CreatePlayerRequest.java`
- Create: `src/main/java/com/example/cardgame/web/dto/GameResponse.java`
- Create: `src/main/java/com/example/cardgame/web/dto/PlayerResponse.java`
- Create: `src/main/java/com/example/cardgame/web/dto/PlayerScoreResponse.java`
- Create: `src/main/java/com/example/cardgame/web/dto/CardResponse.java`
- Create: `src/main/java/com/example/cardgame/web/dto/CardCountResponse.java`
- Test: `src/test/java/com/example/cardgame/web/dto/DtoMappingTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.CardCount;
import com.example.cardgame.domain.Player;
import com.example.cardgame.domain.Rank;
import com.example.cardgame.domain.Suit;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DtoMappingTest {

    @Test
    void cardResponseFromCardIncludesFaceValue() {
        CardResponse r = CardResponse.from(new Card(Suit.HEARTS, Rank.KING));
        assertThat(r.suit()).isEqualTo("HEARTS");
        assertThat(r.rank()).isEqualTo("KING");
        assertThat(r.value()).isEqualTo(13);
    }

    @Test
    void playerResponseFromPlayer() {
        PlayerResponse r = PlayerResponse.from(new Player(1, "alice"));
        assertThat(r.id()).isEqualTo(1);
        assertThat(r.name()).isEqualTo("alice");
    }

    @Test
    void playerScoreResponseFromPlayer() {
        Player p = new Player(1, "alice");
        p.addCard(new Card(Suit.HEARTS, Rank.TEN));
        p.addCard(new Card(Suit.SPADES, Rank.KING));
        PlayerScoreResponse r = PlayerScoreResponse.from(p);
        assertThat(r.id()).isEqualTo(1);
        assertThat(r.name()).isEqualTo("alice");
        assertThat(r.totalValue()).isEqualTo(23);
    }

    @Test
    void cardCountResponseFromCardCount() {
        CardCountResponse r = CardCountResponse.from(
            new CardCount(new Card(Suit.CLUBS, Rank.TWO), 3));
        assertThat(r.suit()).isEqualTo("CLUBS");
        assertThat(r.rank()).isEqualTo("TWO");
        assertThat(r.value()).isEqualTo(2);
        assertThat(r.count()).isEqualTo(3);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=DtoMappingTest test`
Expected: COMPILATION FAILURE (DTOs not defined).

- [ ] **Step 3: Create `CreatePlayerRequest.java`**

```java
package com.example.cardgame.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body to add a player. Name is a required game tag. */
public record CreatePlayerRequest(
    @NotBlank @Size(max = 50) String name
) {
}
```

- [ ] **Step 4: Create `GameResponse.java`**

```java
package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Game;
import java.util.UUID;

public record GameResponse(UUID gameId) {
    public static GameResponse from(Game game) {
        return new GameResponse(game.id());
    }
}
```

- [ ] **Step 5: Create `PlayerResponse.java`**

```java
package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Player;

public record PlayerResponse(int id, String name) {
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(player.id(), player.name());
    }
}
```

- [ ] **Step 6: Create `PlayerScoreResponse.java`**

```java
package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Player;

public record PlayerScoreResponse(int id, String name, int totalValue) {
    public static PlayerScoreResponse from(Player player) {
        return new PlayerScoreResponse(player.id(), player.name(), player.totalValue());
    }
}
```

- [ ] **Step 7: Create `CardResponse.java`**

```java
package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Card;

public record CardResponse(String suit, String rank, int value) {
    public static CardResponse from(Card card) {
        return new CardResponse(card.suit().name(), card.rank().name(), card.rank().faceValue());
    }
}
```

- [ ] **Step 8: Create `CardCountResponse.java`**

```java
package com.example.cardgame.web.dto;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.CardCount;

public record CardCountResponse(String suit, String rank, int value, int count) {
    public static CardCountResponse from(CardCount cc) {
        Card card = cc.card();
        return new CardCountResponse(
            card.suit().name(), card.rank().name(), card.rank().faceValue(), cc.count());
    }
}
```

- [ ] **Step 9: Run test to verify it passes**

Run: `mvn -q -Dtest=DtoMappingTest test`
Expected: PASS (4 tests).

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/example/cardgame/web/dto src/test/java/com/example/cardgame/web/dto
git commit -m "feat: add thin response DTOs with static factories"
```

---

## Task 13: GlobalExceptionHandler (RFC 9457 ProblemDetail)

**Files:**
- Create: `src/main/java/com/example/cardgame/web/GlobalExceptionHandler.java`
- Test: `src/test/java/com/example/cardgame/web/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write the failing test**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=GlobalExceptionHandlerTest test`
Expected: COMPILATION FAILURE (`GlobalExceptionHandler` not defined).

- [ ] **Step 3: Create `GlobalExceptionHandler.java`**

```java
package com.example.cardgame.web;

import com.example.cardgame.service.GameNotFoundException;
import com.example.cardgame.service.PlayerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Translates domain/web exceptions into RFC 9457 ProblemDetail responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GameNotFoundException.class)
    public ProblemDetail handleGameNotFound(GameNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Game not found");
        return pd;
    }

    @ExceptionHandler(PlayerNotFoundException.class)
    public ProblemDetail handlePlayerNotFound(PlayerNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Player not found");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Invalid request");
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
        pd.setTitle("Invalid request");
        return pd;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=GlobalExceptionHandlerTest test`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/web/GlobalExceptionHandler.java src/test/java/com/example/cardgame/web/GlobalExceptionHandlerTest.java
git commit -m "feat: add ProblemDetail exception handler"
```

---

## Task 14: GameController (game lifecycle, decks, shuffle)

**Files:**
- Create: `src/main/java/com/example/cardgame/web/GameController.java`
- Test: `src/test/java/com/example/cardgame/web/GameControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.web;

import com.example.cardgame.domain.Game;
import com.example.cardgame.service.GameNotFoundException;
import com.example.cardgame.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
class GameControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    GameService service;

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
```

> Note: if your Spring Boot version has removed `@MockBean`, replace the field annotation with `@org.springframework.test.context.bean.override.mockito.MockitoBean`. Both register a Mockito mock in the test context.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=GameControllerTest test`
Expected: COMPILATION FAILURE (`GameController` not defined).

- [ ] **Step 3: Create `GameController.java`**

```java
package com.example.cardgame.web;

import com.example.cardgame.domain.Game;
import com.example.cardgame.service.GameService;
import com.example.cardgame.web.dto.GameResponse;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService service;

    @PostMapping
    public ResponseEntity<GameResponse> createGame() {
        Game game = service.createGame();
        return ResponseEntity
            .created(URI.create("/api/v1/games/" + game.id()))
            .body(GameResponse.from(game));
    }

    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> deleteGame(@PathVariable UUID gameId) {
        service.deleteGame(gameId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gameId}/decks")
    public ResponseEntity<Void> addDeck(@PathVariable UUID gameId) {
        service.addDeck(gameId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gameId}/shuffle")
    public ResponseEntity<Void> shuffle(@PathVariable UUID gameId) {
        service.shuffle(gameId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=GameControllerTest test`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/web/GameController.java src/test/java/com/example/cardgame/web/GameControllerTest.java
git commit -m "feat: add GameController for lifecycle, decks, shuffle"
```

---

## Task 15: PlayerController (players, hands, deal)

**Files:**
- Create: `src/main/java/com/example/cardgame/web/PlayerController.java`
- Test: `src/test/java/com/example/cardgame/web/PlayerControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.web;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.Player;
import com.example.cardgame.domain.Rank;
import com.example.cardgame.domain.Suit;
import com.example.cardgame.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlayerController.class)
class PlayerControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    GameService service;

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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=PlayerControllerTest test`
Expected: COMPILATION FAILURE (`PlayerController` not defined).

- [ ] **Step 3: Create `PlayerController.java`**

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=PlayerControllerTest test`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/web/PlayerController.java src/test/java/com/example/cardgame/web/PlayerControllerTest.java
git commit -m "feat: add PlayerController for players, hands, deal"
```

---

## Task 16: ShoeController (remaining-count views)

**Files:**
- Create: `src/main/java/com/example/cardgame/web/ShoeController.java`
- Test: `src/test/java/com/example/cardgame/web/ShoeControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.example.cardgame.web;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.CardCount;
import com.example.cardgame.domain.Rank;
import com.example.cardgame.domain.Suit;
import com.example.cardgame.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ShoeController.class)
class ShoeControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    GameService service;

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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ShoeControllerTest test`
Expected: COMPILATION FAILURE (`ShoeController` not defined).

- [ ] **Step 3: Create `ShoeController.java`**

```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ShoeControllerTest test`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/cardgame/web/ShoeController.java src/test/java/com/example/cardgame/web/ShoeControllerTest.java
git commit -m "feat: add ShoeController for remaining-count views"
```

---

## Task 17: Integration test — full deal-and-score flow

**Files:**
- Test: `src/test/java/com/example/cardgame/CardGameIntegrationTest.java`

- [ ] **Step 1: Write the integration test**

```java
package com.example.cardgame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CardGameIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

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
```

- [ ] **Step 2: Run test to verify it passes**

Run: `mvn -q -Dtest=CardGameIntegrationTest test`
Expected: PASS (1 test). (Implementation already exists; this validates the wired stack.)

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/example/cardgame/CardGameIntegrationTest.java
git commit -m "test: add full-stack integration test"
```

---

## Task 18: Concurrency test — thread-safe dealing

**Files:**
- Test: `src/test/java/com/example/cardgame/service/GameServiceConcurrencyTest.java`

- [ ] **Step 1: Write the concurrency test**

```java
package com.example.cardgame.service;

import com.example.cardgame.domain.Card;
import com.example.cardgame.domain.Game;
import com.example.cardgame.domain.Player;
import com.example.cardgame.repository.InMemoryGameRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

class GameServiceConcurrencyTest {

    @Test
    void concurrentDealsNeverDuplicateOrLoseCards() throws Exception {
        GameService service = new GameService(new InMemoryGameRepository());
        Game game = service.createGame();
        service.addDeck(game.id());            // 52 cards
        Player player = service.addPlayer(game.id(), "alice");

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<Card> dealt = new ConcurrentLinkedQueue<>();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                start.await();
                Optional<Card> c;
                do {
                    c = service.dealCard(game.id(), player.id());
                    c.ifPresent(dealt::add);
                } while (c.isPresent());
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        List<Card> all = List.copyOf(dealt);
        assertThat(all).hasSize(52);
        assertThat(all).doesNotHaveDuplicates();
        assertThat(game.findPlayer(player.id()).orElseThrow().hand()).hasSize(52);
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `mvn -q -Dtest=GameServiceConcurrencyTest test`
Expected: PASS (1 test). If it fails intermittently, the per-game lock in `GameService.withGameLock` is not being applied — verify Task 11.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/example/cardgame/service/GameServiceConcurrencyTest.java
git commit -m "test: verify thread-safe concurrent dealing"
```

---

## Task 19: Full build, README, and container image

**Files:**
- Create: `README.md`

- [ ] **Step 1: Run the complete test suite**

Run: `mvn -q test`
Expected: BUILD SUCCESS, all tests green.

- [ ] **Step 2: Create `README.md`**

````markdown
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
   This provides Java 25 and Maven; all `mvn` commands below run against them.

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
````

- [ ] **Step 3: Verify the app boots**

Run: `mvn spring-boot:run` (then stop with Ctrl+C after confirming startup)
Expected: log line `Started CardGameApplication`. Optionally open `http://localhost:8080/swagger-ui.html`.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: add README with run, test, and container instructions"
```

---

## Self-Review

**Spec coverage:**
- Create/delete game → Task 14 ✓
- Create deck / add deck to shoe (Option 1, no standalone deck) → Task 5 (`addDeck`) + Task 14 endpoint ✓
- Add/remove players → Tasks 8, 11, 15 ✓
- Deal one card; 53rd deals nothing → Tasks 5, 11, 15, 17 ✓
- List a player's cards → Tasks 11, 15 ✓
- Players + summed face value, sorted descending → Tasks 7, 11, 15 ✓
- Count remaining per suit → Tasks 5, 16 ✓
- Count remaining per card, sorted suit then face value high→low → Tasks 5, 16 ✓
- Shuffle (hand-rolled Fisher-Yates, no library shuffle, callable anytime) → Tasks 6, 14 ✓
- REST tradeoffs (actions as POST sub-resources) → Tasks 14–16 ✓
- In-memory repo behind interface → Task 9 ✓
- Per-game locking / thread safety → Tasks 8, 11, 18 ✓
- Player id incrementing int per game, no recycle → Task 8 ✓
- ProblemDetail errors (404/400, no 409; 204 on empty deal) → Tasks 13, 14, 15 ✓
- Thin DTOs, framework-free domain → Task 12 ✓
- OpenAPI, Actuator, Jib, mise toolchain → Tasks 1, 19 ✓
- Testing strategy (unit, shuffle, controller, integration, concurrency) → Tasks 2–18 ✓

**Placeholder scan:** No TBD/TODO; every code step contains complete code.

**Type consistency:** Service methods (`addDeck`, `addPlayer`, `removePlayer`, `dealCard`, `shuffle`, `playersByScore`, `playerCards`, `suitCounts`, `cardCounts`); Player (`id()`, `name()`, `hand()`, `totalValue()`, `addCard()`); Game (`shoe()`, `lock()`, `findPlayer()`, `players()`, `addPlayer()`, `removePlayer()`); Shoe (`dealOne()`, `addDeck()`, `shuffle()`, `suitCounts()`, `cardCounts()`, `remaining()`, `size()`); DTO factories all named `from(...)` with accessors (`suit()`, `rank()`, `value()`, `count()`, `gameId()`, `id()`, `name()`, `totalValue()`) — consistent across mapper test and controllers.

**Known environment caveats flagged inline:** springdoc/Jib versions (Task 1), `@MockBean` vs `@MockitoBean` (Task 14).
