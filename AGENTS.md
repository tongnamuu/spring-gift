# Repository Guidelines

## Project Structure & Module Organization

This is a Spring Boot 3.5 application using Gradle Kotlin DSL and Java 21. Main code lives under `src/main/java/gift`, organized by feature package: `product`, `category`, `option`, `member`, `wish`, `order`, and `auth`. Kotlin source roots also exist at `src/main/kotlin/gift`. Thymeleaf admin views are in `src/main/resources/templates/{member,product}`, static assets belong in `src/main/resources/static`, and Flyway migrations are in `src/main/resources/db/migration`. Tests should mirror the package structure under `src/test/java/gift` or `src/test/kotlin/gift`.

## Build, Test, and Development Commands

Use the Gradle wrapper.

- `./gradlew build` compiles, runs ktlint checks, and executes tests.
- `./gradlew bootRun` starts the app locally on port `8080`.
- `./gradlew test` runs the JUnit Platform test suite.
- `./gradlew test --tests "gift.product.ProductControllerTest"` runs one test class.
- `./gradlew ktlintCheck` checks Kotlin style; `./gradlew ktlintFormat` fixes Kotlin formatting.
- `./gradlew flywayMigrate` applies database migrations.

## Coding Style & Naming Conventions

Keep code feature-oriented: controllers, repositories, DTOs, entities, and validators should stay in the owning domain package. Use 4-space indentation. Java classes use `PascalCase`; methods, fields, and variables use `camelCase`. Request/response DTOs should follow existing names such as `ProductRequest` and `ProductResponse`. JPA table names must match Flyway schema names, including `options` and `orders`. Add new schema changes as `V{n}__Description.sql`; do not edit existing migrations.

## Testing Guidelines

The project uses `spring-boot-starter-test`, JUnit 5, and Kotlin test support. Add focused tests for new behavior and regression tests for fixes. Name test classes after the subject, for example `OptionNameValidatorTest` or `OrderControllerTest`. For behavior changes, assert observable results such as persisted state or response payloads, not just the absence of exceptions. Run `./gradlew test` before submitting.

## Commit & Pull Request Guidelines

Follow the Angular-style convention already referenced by the project: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, and similar prefixes. Keep each commit to one purpose, and separate structural refactors from behavior changes. Before opening a pull request, update the README feature checklist when relevant, describe the change and verification performed, link related issues, and include screenshots for Thymeleaf UI changes.

## Security & Configuration Tips

`application.properties` reads secrets from `JWT_SECRET`, `JWT_EXPIRATION`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, and `KAKAO_REDIRECT_URI`. Set these locally; never commit real tokens, client secrets, or Kakao admin keys.
