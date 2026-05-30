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

Keep code feature-oriented: controllers, repositories, DTOs, entities, and validators should stay in the owning domain package. Use 4-space indentation. Java classes use `PascalCase`; methods, fields, and variables use `camelCase`. Request/response DTOs should follow existing names such as `ProductRequest` and `ProductResponse`. JPA table names must match Flyway schema names, including `options` and `orders`. Add new schema changes as `V{n}__Description.sql`; do not edit existing migrations. Never put `@Transactional` on a class; declare transaction boundaries explicitly on each method that needs them.

## Class & UseCase Design Rules

Before adding or changing a class, inspect the existing interface and neighboring classes in the same feature package. UseCase interfaces are intentionally small to prevent one class from accumulating many methods and responsibilities: one API action per interface, one `execute` method per interface. Implement one UseCase interface with one concrete class so each class has a narrow reason to change. Do not create a broad service class that implements multiple UseCase interfaces or mixes unrelated actions unless explicitly requested. Keep the implementation class in the owning domain package, and keep controller logic thin by delegating only to the relevant UseCase. Match the existing request/response DTOs and domain objects instead of inventing new boundary types.

## Testing Guidelines

The project uses `spring-boot-starter-test`, JUnit 5, and Kotlin test support. Add focused tests for new behavior and regression tests for fixes. Name test classes after the subject, for example `OptionNameValidatorTest` or `OrderControllerTest`. Never use mocks, Mockito, fake repositories, or stub collaborators in tests. Prefer real domain objects for pure domain tests, and use Spring integration tests with the real persistence setup for repository, UseCase, and controller behavior. If a test or verification needs a database, start MySQL with Docker Compose and run against the Flyway-managed schema rather than replacing it with mocks. For behavior changes, assert observable results such as persisted state or response payloads, not just the absence of exceptions. Run `./gradlew test` before submitting.

## Commit & Pull Request Guidelines

Follow the Angular-style convention already referenced by the project: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, and similar prefixes. Keep each commit to one purpose, and separate structural refactors from behavior changes. Before opening a pull request, update the README feature checklist when relevant, describe the change and verification performed, link related issues, and include screenshots for Thymeleaf UI changes.

## Security & Configuration Tips

`application.properties` reads secrets from `JWT_SECRET`, `JWT_EXPIRATION`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, and `KAKAO_REDIRECT_URI`. Set these locally; never commit real tokens, client secrets, or Kakao admin keys.
