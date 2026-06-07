# Repository Guidelines

## Project Structure & Module Organization

This is a Spring Boot 3.5 application using Gradle Kotlin DSL and Java 21. Main code lives under `src/main/java/gift`, organized by feature package: `product`, `category`, `member`, `wish`, `order`, and `auth`. Option code belongs under the Product feature because Product owns Option. Kotlin source roots also exist at `src/main/kotlin/gift`. Thymeleaf admin views are in `src/main/resources/templates/{member,product}`, static assets belong in `src/main/resources/static`, and Flyway migrations are in `src/main/resources/db/migration`. Tests should mirror the package structure under `src/test/java/gift` or `src/test/kotlin/gift`.

## Build, Test, and Development Commands

Use the Gradle wrapper.

- `./gradlew build` compiles, runs ktlint checks, and executes tests.
- `./gradlew bootRun` starts the app locally on port `8080`.
- `./gradlew test` runs the JUnit Platform test suite.
- `./gradlew test --tests "gift.product.ProductControllerTest"` runs one test class.
- `./gradlew ktlintCheck` checks Kotlin style; `./gradlew ktlintFormat` fixes Kotlin formatting.
- `./gradlew flywayMigrate` applies database migrations.

## Coding Style & Naming Conventions

Keep code feature-oriented: controllers, repositories, DTOs, entities, and validators should stay in the owning domain package. Use 4-space indentation. Java classes use `PascalCase`; methods, fields, and variables use `camelCase`. Request/response DTOs should follow existing names such as `ProductRequest` and `ProductResponse`. JPA table names must match Flyway schema names, including `options` and `orders`. Add new schema changes as `V{n}__Description.sql`; do not edit existing migrations. Never put `@Transactional` on a class; declare transaction boundaries explicitly on each method that needs them. Do not use `saveAndFlush`; persist with `save` and let the method-level transaction boundary flush changes.

## Class & UseCase Design Rules

Before adding or changing a class, inspect the existing interface and neighboring classes in the same feature package. UseCase interfaces are intentionally small to prevent one class from accumulating many methods and responsibilities: one API action per interface, one `execute` method per interface. Implement one UseCase interface with one concrete class so each class has a narrow reason to change. Do not create a broad service class that implements multiple UseCase interfaces or mixes unrelated actions unless explicitly requested. Keep the implementation class in the owning domain package, and keep controller logic thin by delegating only to the relevant UseCase. Match the existing request/response DTOs and domain objects instead of inventing new boundary types.

Repositories should be created only for aggregate roots. Child entities such as `Option` must be changed through the owning aggregate root (`Product`) and saved through the root repository. Do not add a child repository just to bypass aggregate rules.

Read APIs should use a dedicated `JdbcTemplate` query object instead of JPA entity traversal or derived repository queries. Object and aggregate relationship changes must not change query performance, join shape, or introduce N+1 behavior. Keep write UseCases on domain objects and repositories, and keep read models explicit with SQL that returns the API response shape.

## Testing Guidelines

The project uses `spring-boot-starter-test`, JUnit 5, and Kotlin test support. Organize tests into three layers by purpose:

- Contract unit tests describe domain rules and small object contracts without Spring, a database, or external systems. Use real domain objects, and hand-written fake repositories when persistence is a collaborator rather than the behavior under test. Examples include validator rules, option stock subtraction, member point charging/deduction, and DTO conversion.
- Service tests verify actual UseCase behavior through Spring beans and real persistence. Use the real repository and the Flyway-managed schema when persistence behavior matters. If a database is needed, start MySQL with Docker Compose.
- API tests verify HTTP-level behavior: request validation, status codes, authentication, response payloads, and observable persisted state.

Do not use Mockito or dynamic mocks by default. Hand-written fakes are allowed in unit tests, and fake/stub implementations are allowed at outer service boundaries such as Kakao clients so tests remain deterministic without real network calls. If the user explicitly requests interaction verification for a critical event publication boundary, keep Mockito usage narrow to that unit test and still prefer fakes for service/API tests. Do not duplicate the same assertion at every layer: unit tests cover rules, service tests cover use case state changes, and API tests cover the external contract. Name test classes after the subject and layer, for example `OptionNameValidatorTest`, `CreateCategoryServiceTest`, or `CategoryApiTest`. For behavior changes, assert observable results such as persisted state or response payloads, not just the absence of exceptions. Run `./gradlew test` before submitting.

## Commit & Pull Request Guidelines

Follow the Angular-style convention already referenced by the project: `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, and similar prefixes. Keep each commit to one purpose, and separate structural refactors from behavior changes. Before opening a pull request, update the README feature checklist when relevant, describe the change and verification performed, link related issues, and include screenshots for Thymeleaf UI changes.

## Security & Configuration Tips

`application.properties` reads secrets from `JWT_SECRET`, `JWT_EXPIRATION`, `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, and `KAKAO_REDIRECT_URI`. Set these locally; never commit real tokens, client secrets, or Kakao admin keys.
