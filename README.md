# spring-gift

Spring Boot gift service for practicing production-like execution, automated verification, safe refactoring, and behavior changes with evidence.

## Current Status

- Java 21, Spring Boot 3.5, Gradle Kotlin DSL.
- Main code is organized by feature under `src/main/java/gift`.
- Flyway migrations are in `src/main/resources/db/migration`.
- Thymeleaf admin templates are in `src/main/resources/templates`.
- Docker Compose MySQL setup uses MySQL 8.4.9 LTS in `compose.yaml`.
- Baseline `./gradlew test`, `./gradlew serviceTest`, and `./gradlew apiTest` currently succeed.
- API tests now cover category deletion policy, admin product missing-category display, member registration/login behavior, and wish workflows.
- Category, Product, Wish, member registration, and order creation/listing now have UseCase/service extraction in progress; remaining controller logic still needs the same treatment.
- Member code is grouped under `gift.member`; auth/admin/controller/domain are separated, and service/usecase classes are further grouped into `auth` and `management` workflows.
- Wish is treated as a separate aggregate root and stores `memberId`/`productId` without direct `Member` or `Product` object references.
- Product is the aggregate root for option stock changes. Order creation updates option quantity through `Product.subtractOptionQuantity(...)`, and Product/Member optimistic versions guard stock and point concurrency.
- Option is not a separate aggregate root. Option creation/deletion goes through Product, only root repositories are used for writes, and read APIs use dedicated `JdbcTemplate` query objects.
- Order is treated as immutable history. It stores `productId`, `optionId`, and `memberId` values plus the product/option snapshot needed for order lists and Kakao messages.

## Implementation Strategy

- Keep each change small enough to explain from `git diff` in under 30 seconds.
- Separate structural refactors from behavior changes.
- Prefer Red -> Green -> Refactor for behavior work.
- Use Angular-style commit messages such as `docs:`, `test:`, `refactor:`, and `fix:`.
- Do not skip or disable tests to make a change pass.
- Verify behavior through observable results, not only absence of exceptions.
- Do not use `saveAndFlush`; use `save` and let method-level transaction boundaries flush changes.

## Refactoring Change Log

Detailed refactoring changes, policy changes, and object-specific TODOs are tracked in Korean in
`docs/refactoring-change-log.md`.

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
| `Product` | `options.product_id -> product.id`; `wish.product_id`, `orders.product_id`, and `orders.option_id` are value references only. | Wishes and orders can keep product or option ids whose rows were deleted. | Delete products without changing wishes or orders; wish lists hide missing products, and orders keep immutable ids plus creation-time snapshots. |
| `Option` | Owned by `Product`; no order FK after `V8__Store_order_product_and_option_ids.sql`. | Orders can keep an option id whose option row was deleted or later changed. | Ordered options can be deleted when Product aggregate rules allow it; order history keeps the original option id and option-name snapshot. |
| `Member` | `wish.member_id -> member.id`, `orders.member_id -> member.id` | Deleting a member with wishes or orders will fail at the database level. | Define whether wishes are cleaned up, but reject deletion when order history exists. |
| `Wish` | None currently identified. | Wish deletion is the lowest FK risk, but ownership validation must remain explicit. | Allow deletion only by the owning member. |
| `Order` | No current delete API. | No delete behavior has been defined. | Decide whether orders are immutable history. |

Order creation intentionally leaves the buyer's Wish rows unchanged. A wished product can be ordered repeatedly, such as a recurring monthly purchase, so Wish is not treated as a one-time cart item.

Option deletion is still constrained by the Product aggregate: every product must keep at least one option. Deleting the last option returns `400 Bad Request` with `옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.`.

### Order Stock And Point Concurrency

Order creation now runs through `CreateOrderService` with method-level `@Transactional`. The service loads the `Product` aggregate root by option id, updates option stock through the Product root, deducts member points, and saves an immutable order record containing `productId`, `optionId`, `memberId`, product name, option name, unit price, and product image URL as a creation-time snapshot.

Current policy:

- `Product.version` protects option stock changes at the Product aggregate boundary.
- `Product.update_dt` is refreshed when Product fields change or options are added, removed, or decremented.
- If order stock deduction and option deletion load the same Product version concurrently, exactly one transaction commits and the other fails through Product optimistic locking.
- `Member.version` protects point deduction.
- Order lists use the stored order snapshot, not the current Product/Option state.
- The service does not force `saveAndFlush`; Product and Member changes are flushed at the transaction boundary.
- Transaction-boundary concurrency failures are handled by the common API exception handler as `409 Conflict`.
- Kakao message sending is published as an order event and handled by an `AFTER_COMMIT` async listener through the `KakaoMessageSender` port, so it runs only after the order transaction succeeds without blocking the response path.
- Order list queries read the stored order snapshot through a `JdbcTemplate` query object, so Product/Option relationship changes do not change lookup performance or introduce N+1 behavior.
- Ordering a product does not remove matching Wish rows.

## Member Registration And Login

Member registration, member login, Kakao login URI creation, and Kakao callback login have been extracted to one-action UseCase services. Current API tests verify the observable behavior against the real MySQL test database.

Current behavior:

- `POST /api/members/register` returns `200 OK` with a JWT when a new email is registered.
- Sequential duplicate registration returns `400 Bad Request` with `Email is already registered.`.
- Concurrent duplicate registration also returns `400 Bad Request` with `Email is already registered.` for losing requests.
- Login with a registered email and matching password returns `200 OK` with a JWT.
- Login with a missing member or wrong password returns `400 Bad Request` with `Invalid email or password.`.
- Invalid email format on registration returns `400 Bad Request`.

Concurrent duplicate registration is handled at the service boundary. The database unique constraint keeps only one member row for an email, and persistence-level duplicate-email failures are converted to the same API contract as the sequential duplicate case.

Current policy:

- Keep the database unique constraint on `member.email`.
- Mark `Member.email` as unique in the JPA mapping.
- Convert duplicate-email persistence failures into `400 Bad Request` with `Email is already registered.`.

## Runtime Verification Notes

Earlier runtime verification on the local application confirmed that FK failures surfaced as `500 Internal Server Error` responses instead of domain-level API errors before the category policy changed:

| Request | Observed response | Runtime exception | FK constraint |
| --- | --- | --- | --- |
| `DELETE /api/categories/1` | `500 Internal Server Error` | `DataIntegrityViolationException` from `SQLIntegrityConstraintViolationException` | `product.category_id -> category.id` (`product_ibfk_1`) |
| `DELETE /api/products/1` | `500 Internal Server Error` | `DataIntegrityViolationException` from `SQLIntegrityConstraintViolationException` | `wish.product_id -> product.id` (`wish_ibfk_2`), later removed by `V4__Remove_wish_product_foreign_key.sql` |
| `DELETE /api/products/2/options/3` | `500 Internal Server Error` | `DataIntegrityViolationException` from `SQLIntegrityConstraintViolationException` | Historical: `orders.option_id -> options.id` (`orders_ibfk_1`), removed by `V8__Store_order_product_and_option_ids.sql` |
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

Kakao login uses `KakaoLoginClient` as an outer-service port. Tests replace it with a fake client so Kakao signup/login behavior is verified without real network calls.

## Implementation Checklist

Object-specific implementation checklist is tracked in `docs/refactoring-change-log.md`.

## Verification Log

- `docker compose config` - passed with MySQL 8.4.9 service.
- `docker compose up -d mysql` - passed; MySQL became healthy.
- `./gradlew bootRun` - passed; application started on port 8080.
- `curl http://localhost:8080/api/categories` - passed; returned seeded categories.
- `./gradlew test --rerun-tasks` - passed with current contract unit tests.
- `./gradlew serviceTest --rerun-tasks` - passed against Docker Compose MySQL test database.
- `./gradlew apiTest --rerun-tasks` - passed; includes member registration/login tests and concurrent duplicate-registration handling.
- `./gradlew test --tests "gift.wish.domain.WishContractTest" --rerun-tasks` - passed.
- `./gradlew serviceTest --tests "gift.wish.service.WishServiceTest" --rerun-tasks` - passed against Docker Compose MySQL test database.
- `./gradlew apiTest --tests "gift.wish.controller.WishApiTest" --rerun-tasks` - passed against Docker Compose MySQL test database.
- `./gradlew serviceTest --tests "gift.order.service.OrderServiceTest" --rerun-tasks` - passed against Docker Compose MySQL test database for order snapshot persistence, Kakao sender success/failure behavior, multi-option product lookup, ordered-option deletion, and Wish retention after order creation.
- `./gradlew serviceTest --tests "gift.order.service.OrderConcurrencyServiceTest" --rerun-tasks` - passed against Docker Compose MySQL test database after Product/Member transaction-boundary concurrency handling.
- Cucumber feature files added under `src/test/resources/features`; step definitions and runner are not configured yet.
- `./gradlew build --rerun-tasks` - passed.

## AI Usage Record

- Documentation reorganization: moved assignment instructions to ignored `homework.md`, converted `README.md` into the implementation plan, and added `homework.md` to `.gitignore`.
- Database setup: selected MySQL 8.4.9 LTS after checking MySQL lifecycle and Spring Boot-managed Connector/J compatibility.
- Black-box test design: organized current API behavior into Cucumber feature files for member, category, product, option, wish, and order workflows.
- Runtime environment setup: added `.env.example`, documented local `.env` usage, MySQL startup, and Kakao Login consent items.
- Member API behavior analysis: added tests for registration/login success and failure cases, then changed concurrent duplicate registration to return `400 Bad Request` with the duplicate-email message.
- Order modeling and concurrency analysis: added service-level stock/snapshot/point concurrency tests, introduced Product/Member optimistic versions, routed option stock changes through the Product aggregate root, removed forced flushes, moved Kakao messages to an after-commit async event listener, and verified event publication with a narrow Mockito unit test.
