# spring-gift

Spring Boot gift service for practicing production-like execution, automated verification, safe refactoring, and behavior changes with evidence.

## Current Status

- Java 21, Spring Boot 3.5, Gradle Kotlin DSL.
- Main code is organized by feature under `src/main/java/gift`.
- Flyway migrations are in `src/main/resources/db/migration`.
- Thymeleaf admin templates are in `src/main/resources/templates`.
- Docker Compose MySQL setup uses MySQL 8.4.9 LTS in `compose.yaml`.
- Baseline `./gradlew test` currently succeeds, but there are no real test classes yet.
- Controllers currently contain most business logic; service extraction is still pending.

## Implementation Strategy

- Keep each change small enough to explain from `git diff` in under 30 seconds.
- Separate structural refactors from behavior changes.
- Prefer Red -> Green -> Refactor for behavior work.
- Use Angular-style commit messages such as `docs:`, `test:`, `refactor:`, and `fix:`.
- Do not skip or disable tests to make a change pass.
- Verify behavior through observable results, not only absence of exceptions.

## Currently Identified Problems

### Category Delete Rule

`Category` and `Product` are separate aggregate roots. `Category` can exist without any product, and `Product` stores only the `categoryId` value instead of holding a direct `Category` object reference.

Category deletion is independent from product lifecycle. Deleting a category does not move or delete products that contain the deleted category id. Product listing code must resolve category names separately and display `미분류 카테고리` when the category row no longer exists.

Current policy:

- Delete a category regardless of product references.
- Keep existing products and their `categoryId` values unchanged.
- Display products with missing category rows as `미분류 카테고리` in product lists.
- `DELETE /api/categories/{id}` returns `204 No Content` even when products still contain that category id.

### Delete Behavior And FK Constraints

Most remaining Flyway foreign keys are defined without `ON DELETE CASCADE`. In MySQL this means parent rows cannot be deleted while child rows still reference them. Current delete use cases must therefore define explicit domain rules instead of letting `DataIntegrityViolationException` leak from the database.

| Target object | Direct FK dependencies | Current risk | Expected policy to define |
| --- | --- | --- | --- |
| `Category` | `Product.categoryId` value reference only; no DB FK after `V3__Remove_product_category_foreign_key.sql`. | Products can keep a category id whose category row was deleted. | Allow category deletion and display missing category rows as `미분류 카테고리`. |
| `Product` | `wish.product_id -> product.id`, `options.product_id -> product.id` | Wishes block product deletion. Options may be removed through the product aggregate, but ordered options are still blocked by orders. | Reject product deletion while wishes or orders exist for the product. |
| `Option` | `orders.option_id -> options.id` | Deleting an option that was ordered will fail at the database level. | Reject option deletion while orders reference it; also keep the existing rule that a product needs at least one option. |
| `Member` | `wish.member_id -> member.id`, `orders.member_id -> member.id` | Deleting a member with wishes or orders will fail at the database level. | Define whether wishes are cleaned up, but reject deletion when order history exists. |
| `Wish` | None currently identified. | Wish deletion is the lowest FK risk, but ownership validation must remain explicit. | Allow deletion only by the owning member. |
| `Order` | No current delete API. | No delete behavior has been defined. | Decide whether orders are immutable history. |

Related behavior gap: order creation currently has a documented intent to remove the ordered product from the buyer's wishes, but this still needs runtime verification and may leave wish rows that later block product or member deletion.

Earlier runtime verification on the local application confirmed that FK failures surfaced as `500 Internal Server Error` responses instead of domain-level API errors before the category policy changed:

| Request | Observed response | Runtime exception | FK constraint |
| --- | --- | --- | --- |
| `DELETE /api/categories/1` | `500 Internal Server Error` | `DataIntegrityViolationException` from `SQLIntegrityConstraintViolationException` | `product.category_id -> category.id` (`product_ibfk_1`) |
| `DELETE /api/products/1` | `500 Internal Server Error` | `DataIntegrityViolationException` from `SQLIntegrityConstraintViolationException` | `wish.product_id -> product.id` (`wish_ibfk_2`) |
| `DELETE /api/products/2/options/3` | `500 Internal Server Error` | `DataIntegrityViolationException` from `SQLIntegrityConstraintViolationException` | `orders.option_id -> options.id` (`orders_ibfk_1`) |
| `POST /admin/members/2/delete` | `500 Internal Server Error` | `DataIntegrityViolationException` from `SQLIntegrityConstraintViolationException` | `orders.member_id -> member.id` (`orders_ibfk_2`) |

Application startup itself succeeds against local MySQL. The remaining startup warnings are Flyway's MySQL 8.4 support warning and Spring's default `open-in-view` warning.

## Commit Prompt Hook

This checkout uses `scripts/git-hooks` as `core.hooksPath`. Before committing work done through AI prompts, record each user prompt with:

```bash
scripts/record-commit-prompt.sh "prompt text"
```

The `prepare-commit-msg` hook appends the recorded prompts to the commit body under `Codex Prompts:`. After a successful commit, `post-commit` archives the prompt log to `.git/codex-commit-prompts.last.md` and clears it for the next commit.

## Local Runtime Setup

Use `.env.example` as the template for local secrets and runtime values:

```bash
cp .env.example .env
```

Fill `.env` with local values. Do not commit `.env`; it is ignored by Git. The application imports `.env` through `application.yaml` and still falls back to defaults where safe.

Start MySQL first, then run the Spring Boot application locally:

```bash
docker compose up -d mysql
./gradlew bootRun
```

The MySQL service uses a named Docker volume, so data survives normal container restarts. Use `docker compose stop mysql` to stop the database without removing data. Use `docker compose down -v` only when you intentionally want to delete the local database volume.

For Kakao Login, create a Kakao Developers app and copy the REST API key and client secret into `.env`:

```env
KAKAO_CLIENT_ID=replace-with-kakao-rest-api-key
KAKAO_CLIENT_SECRET=replace-with-kakao-client-secret
KAKAO_REDIRECT_URI=http://localhost:8080/api/auth/kakao/callback
```

Register the same redirect URI in Kakao Developers:

```text
http://localhost:8080/api/auth/kakao/callback
```

Required Kakao consent items:

| Consent item | Why it is needed |
| --- | --- |
| `account_email` | Kakao login uses the Kakao account email to find or create a `Member`. |
| `talk_message` | Order creation can send a KakaoTalk message to the logged-in user. |

## Implementation Checklist

- [x] Configure local development to use a non-EOL MySQL LTS version.
- [x] Define black-box Cucumber feature specifications for API workflows.
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

- `docker compose config` - passed with MySQL 8.4.9 service.
- `docker compose up -d mysql` - passed; MySQL became healthy.
- `./gradlew bootRun` - passed; application started on port 8080.
- `curl http://localhost:8080/api/categories` - passed; returned seeded categories.
- `./gradlew test` - passed; no real test classes yet.
- Cucumber feature files added under `src/test/resources/features`; step definitions and runner are not configured yet.
- `./gradlew build` - pending before final handoff.

## AI Usage Record

- Documentation reorganization: moved assignment instructions to ignored `homework.md`, converted `README.md` into the implementation plan, and added `homework.md` to `.gitignore`.
- Database setup: selected MySQL 8.4.9 LTS after checking MySQL lifecycle and Spring Boot-managed Connector/J compatibility.
- Black-box test design: organized current API behavior into Cucumber feature files for member, category, product, option, wish, and order workflows.
- Runtime environment setup: added `.env.example`, documented local `.env` usage, MySQL startup, and Kakao Login consent items.
