# spring-gift

Spring Boot gift service for practicing production-like execution, automated verification, safe refactoring, and behavior changes with evidence.

## Current Status

- Java 21, Spring Boot 3.5, Gradle Kotlin DSL.
- Main code is organized by feature under `src/main/java/gift`.
- Flyway migrations are in `src/main/resources/db/migration`.
- Thymeleaf admin templates are in `src/main/resources/templates`.
- Docker Compose MySQL setup exists in `compose.yaml`.
- Baseline `./gradlew test` currently succeeds, but there are no real test classes yet.
- Controllers currently contain most business logic; service extraction is still pending.

## Implementation Strategy

- Keep each change small enough to explain from `git diff` in under 30 seconds.
- Separate structural refactors from behavior changes.
- Prefer Red -> Green -> Refactor for behavior work.
- Use Angular-style commit messages such as `docs:`, `test:`, `refactor:`, and `fix:`.
- Do not skip or disable tests to make a change pass.
- Verify behavior through observable results, not only absence of exceptions.

## Implementation Checklist

- [ ] Add deterministic test configuration using H2 and Flyway.
- [ ] Add baseline domain tests for product/option validators, stock subtraction, and point deduction.
- [ ] Add API or service tests for product, category, option, member, wish, and order workflows.
- [ ] Extract service layer from controllers without changing behavior.
- [ ] Add service-level transaction boundaries for read and write use cases.
- [ ] Move Kakao message sending to after successful order commit.
- [ ] Implement order-created wish cleanup for the buyer and ordered product.
- [ ] Strengthen domain responsibility around option stock and member points.
- [ ] Run final verification with `./gradlew test` and `./gradlew build`.
- [ ] Record AI usage and verification evidence in this README.

## Planned Commit Order

1. `docs: separate homework instructions from plan`
2. `test: add baseline coverage for domain rules`
3. `refactor: extract service layer`
4. `refactor: add transaction boundaries`
5. `fix: clean up wish after order creation`
6. `fix: send kakao message after order commit`
7. `test: cover main api workflows`
8. `docs: record verification and ai usage`

## Verification Log

- `./gradlew test` - pending after test setup.
- `./gradlew build` - pending before final handoff.

## AI Usage Record

- Documentation reorganization: moved assignment instructions to ignored `homework.md`, converted `README.md` into the implementation plan, and added `homework.md` to `.gitignore`.
