# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build / run / test (use the wrapper):

```bash
./gradlew build              # compile + ktlint + test
./gradlew bootRun            # run the Spring Boot app on :8080
./gradlew test               # run all tests
./gradlew test --tests "gift.product.ProductControllerTest"      # single test class
./gradlew test --tests "gift.product.ProductControllerTest.shouldX"  # single test method
./gradlew ktlintCheck        # Kotlin lint check
./gradlew ktlintFormat       # auto-fix Kotlin lint issues
./gradlew flywayMigrate      # apply DB migrations
```

Java toolchain is **21**; Gradle build script is Kotlin DSL.

## Required environment variables

`application.properties` reads these via `${VAR:default}`:

- `JWT_SECRET`, `JWT_EXPIRATION` — JWT signing
- `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI` — Kakao login
  (default redirect URI is `http://localhost:8080/api/auth/kakao/callback`)

Never commit secrets. Set these locally or via the run configuration.

## Architecture

Single Spring Boot 3.5 app, package root `gift.*`. The project mixes Java + Kotlin source roots (`src/main/java`, `src/main/kotlin`); Kotlin plugins (`plugin.spring`, `plugin.jpa`, `allOpen` on `@Entity`/`@MappedSuperclass`/`@Embeddable`) are configured so JPA entities can be Kotlin classes.

### Layering by feature package
Each domain (`product`, `category`, `option`, `member`, `wish`, `order`, `auth`) is a self-contained slice typically containing: `Entity`, `Repository` (Spring Data JPA), `Controller`, `Request`/`Response` DTOs, plus feature-specific validators (e.g. `ProductNameValidator`, `OptionNameValidator`). There is currently **no separate service layer** in most slices — extracting one is an explicit goal listed in `README.md` ("서비스 계층 추출"). Treat that as a structural refactor (no behavior change) when doing it.

### Domain relationships
`category 1—* product 1—* option 1—* order`, plus `member 1—* wish *—1 product` and `member 1—* order`. Schema is authoritative in `src/main/resources/db/migration/V1__Initialize_project_tables.sql`. Note the `options` and `orders` table names (reserved-word avoidance) — JPA `@Table` names must match.

### Auth flow
`gift.auth` implements Kakao OAuth: `KakaoAuthController` → `KakaoLoginClient` (talks to Kakao REST) → issues a JWT via `JwtProvider`. `KakaoLoginProperties` is a `@ConfigurationProperties` bean; `Application` enables `@ConfigurationPropertiesScan`. Authenticated controllers use `AuthenticationResolver` (a `HandlerMethodArgumentResolver`) to inject the current member from the JWT.

### Persistence & migrations
- DB is **MySQL in production** (`mysql-connector-j`), **H2 in tests/local** (runtime-only).
- **Flyway** owns the schema. Add changes as new `V{n}__*.sql` files under `src/main/resources/db/migration/` — never edit existing migrations. `V2__Insert_default_data.sql` seeds reference data.
- JPA is in use, but DDL is Flyway's job; do not rely on `ddl-auto`.

### Admin vs API controllers
Some domains expose two controllers: `XxxController` (REST/JSON for `/api/...`) and `AdminXxxController` (Thymeleaf-rendered admin pages backed by templates in `src/main/resources/templates/{member,product}/`).

### External integration
`KakaoMessageClient` (in `gift.order`) sends Kakao "send-to-me" messages on order creation. This is the place where order behavior crosses a process boundary — relevant to the README's "트랜잭션 경계 세우기" goal: the message send must not be inside the DB transaction.

## Process rules from README.md

The repo's `README.md` defines binding rules for changes — read it before making non-trivial edits. Highlights that affect how you work:

- **One purpose per commit.** Structural changes (rename, extract, move) and behavioral changes go in **separate commits**.
- **Make a feature checklist in `README.md` first**, commit per checklist item, use AngularJS commit message conventions (`feat:`, `fix:`, `refactor:`, …).
- **TDD**: Red → Green → Refactor. Never disable or skip tests to make a change pass.
- **Behavior changes need observable evidence** (state re-read, response assertion) — "no exception thrown" is not enough.
- Record AI usage (prompts, patterns, what was learned) in `README.md` under the AI 활용 기록 section.
