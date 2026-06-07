# 리팩터링 변경점 및 작업 현황

이 문서는 객체 모델링과 UseCase 리팩터링 과정에서 확인한 변경점, 정책 변경,
작업 완료 항목, 남은 TODO를 추적한다.

## 리팩터링 기준

현재 리팩터링의 목적은 컨트롤러에 모여 있는 비즈니스 로직을 UseCase 서비스로
옮겨서 트랜잭션 경계를 메서드 단위로 명확히 선언하는 것이다.

작업은 두 종류로 구분한다.

- 구조 해결: UseCase 도입, 서비스 추출, 패키지 분리처럼 코드 구조를 개선하는 작업.
- 문제 해결: FK 삭제 오류, 동시성 오류, 정책 누락, API 상태 코드 오류처럼 실제 동작 문제를 해결하는 작업.

새로운 동작 오류, 정책 공백, FK 위험, 런타임/API 실패가 발견되면 먼저 문제 해결
항목에 기록한 뒤, 필요한 경우 구조 해결 작업으로 이어간다.

코드 스타일 기준으로 `saveAndFlush`는 사용하지 않는다. 저장은 `save`로 수행하고
flush는 메서드 단위 트랜잭션 경계에서 발생하도록 둔다.

## 완료된 구조 해결

| 커밋 | 변경점 | 의미 |
| --- | --- | --- |
| `bf94bac` | API 워크플로우를 블랙박스 Cucumber feature로 정리했다. | 리팩터링 전 외부 동작 계약을 문서화했다. |
| `fd86235` | 초기 UseCase 인터페이스를 도출했다. | API 동작 단위를 컨트롤러 구현에서 분리하기 시작했다. |
| `0fe3d2d` | 테스트 계층 규칙을 정의했다. | 계약 단위 테스트, 서비스 테스트, API 테스트의 목적을 분리했다. |
| `7f7009f` | 현재 객체 모델을 문서화했다. | 구조 변경 전 Aggregate와 관계 기준선을 만들었다. |
| `8afaa99` | Category 계약 테스트를 추가했다. | 서비스 추출 전 Category 객체 규칙을 고정했다. |
| `e6c77a7` | Category UseCase 서비스를 추가했다. | Category 동작을 메서드 단위 트랜잭션 경계로 옮기기 시작했다. |
| `4b383f6` | Product 패키지를 재구성했다. | Product의 controller, entity, repository, service, usecase 경계를 분리했다. |
| `d4c7a7c` | Product UseCase 서비스를 추가했다. | Product/Option 동작을 컨트롤러 밖 서비스 메서드로 옮겼다. |
| `3e01cfa` | Product와 Category를 별도 Aggregate로 분리했다. | Product가 Category 객체를 직접 갖는 결합을 제거했다. |
| `9defb68` | Member 회원가입/로그인 API 테스트를 추가했다. | Member API 동작을 구조 변경 전에 고정했다. |
| `905d6b1` | Member 회원가입 UseCase/서비스 흐름을 완성했다. | 회원가입 로직을 컨트롤러에서 빼고 중복 이메일 저장 실패를 서비스 경계에서 처리했다. |
| `8ade19c` | Member 인증 Cucumber feature를 현재 API 계약에 맞게 갱신했다. | 블랙박스 Member 명세를 현재 동작과 맞췄다. |
| `2003461` | Category 패키지를 controller, domain, service, usecase로 분리했다. | Category 패키지 구조를 리팩터링 방향과 맞췄다. |
| `81ebc15` | Order 생성 UseCase 서비스를 도입했다. | 주문 생성 로직을 컨트롤러에서 서비스로 옮기고 포인트/재고 동시성 문제를 서비스 테스트로 드러냈다. |
| `현재 작업` | 옵션 재고 변경을 Product 루트 경유로 변경했다. | Product Aggregate root 기준으로 옵션 재고 변경과 낙관적 락 경계를 맞췄다. |
| `현재 작업` | Order가 Product/Option을 객체가 아니라 id 값과 주문 당시 스냅샷으로 보관하게 했다. | 주문 이력을 별도 루트로 두고 Product/Option 삭제/변경 정책과 주문 이력 표시를 분리했다. |
| `현재 작업` | Kakao 메시지를 주문 트랜잭션 afterCommit으로 이동했다. | 외부 부수효과가 DB 트랜잭션 성공 전 발생하지 않도록 했다. |

## 완료된 문제 해결

| 커밋 | 해결한 문제 | 결과 |
| --- | --- | --- |
| `1a756f6` | 삭제 요청에서 FK 오류가 `500 Internal Server Error`로 노출되는 실제 오류를 확인했다. | 삭제 실패와 FK 제약을 README에 기록했다. |
| `bb32929` | 상품이 걸린 Category 삭제 정책이 명시되지 않았다. | 처음에는 상품이 있는 Category 삭제를 거절하는 규칙을 추가했다. 이 규칙은 이후 미분류 정책으로 대체됐다. |
| `c72940d` | Category 생명주기가 Product에 과하게 묶여 있었다. | Category 삭제를 Product와 무관하게 허용하고, Category row가 없는 Product는 목록에서 `미분류 카테고리`로 표시하도록 했다. |
| `905d6b1` | 동시 중복 회원가입이 `500 Internal Server Error`로 드러날 수 있었다. | 중복 이메일 저장 실패를 `400 Bad Request`와 `Email is already registered.` 메시지로 변환했다. |
| `81ebc15` | 동시 주문에서 회원 포인트 차감 실패가 DB lock/deadlock 예외로 노출될 수 있었다. | `Member.version`을 추가하고 포인트 낙관적 락 충돌을 도메인 실패로 변환했다. |
| `현재 작업` | 동시 주문에서 같은 옵션 재고가 초과 판매될 수 있었다. | `Product.version`과 `update_dt`를 추가하고 옵션 재고 차감을 Product 루트 메서드로 수행하게 했다. |
| `현재 작업` | 주문 생성 서비스가 `saveAndFlush`로 중간 flush를 강제하고 낙관적 락 예외를 트랜잭션 내부에서 변환했다. | 중간 flush를 제거하고 트랜잭션 경계에서 발생한 동시성 실패를 공통 API 예외 처리에서 `409 Conflict`로 변환한다. |
| `현재 작업` | 주문 이력이 `Option` 엔티티와 DB FK에 묶여 있어 옵션 삭제 정책이 주문 FK에 의해 결정됐다. | `orders.product_id`를 추가하고 `orders.option_id` FK를 제거해 주문 이력은 생성 당시 id 값과 스냅샷을 보관하게 했다. |
| `현재 작업` | Order 목록이 현재 Product/Option을 조회하면 과거 주문의 상품명, 옵션명, 가격이 바뀌어 보일 수 있었다. | Order에 상품명, 옵션명, 단가, 이미지 URL 스냅샷을 저장하고 목록 응답은 이 값을 사용한다. |
| `현재 작업` | Kakao 메시지가 주문 트랜잭션 성공 전에 전송될 수 있었다. | 메시지에 필요한 값을 캡처한 뒤 afterCommit에서 best-effort로 전송한다. |

## 식별된 정책 변경

| 객체 | 이전 동작 또는 가정 | 식별한 문제 | 현재 정책 | 근거 |
| --- | --- | --- | --- | --- |
| `Category` | Product가 참조하는 Category는 DB FK 또는 명시적 존재 체크 때문에 삭제할 수 없다고 봤다. | Category 생명주기가 Product에 너무 강하게 묶였다. Product는 삭제된 Category id를 갖고도 목록에 표시될 수 있다. | Category 삭제는 Product와 독립적으로 허용한다. Product를 이동하거나 삭제하지 않고, Category row가 없으면 `미분류 카테고리`로 표시한다. | `c72940d`, `CategoryServiceTest`, `CategoryApiTest`, `AdminProductApiTest` |
| `Product` | Product가 Category 객체를 직접 갖거나 직접 의존한다고 봤다. | Product -> Category 객체 결합 때문에 Aggregate 경계가 흐려졌다. | Product는 `categoryId` 값만 저장한다. Product와 Category는 서로 다른 Aggregate root이다. | `3e01cfa`, `ProductContractTest` |
| `Product/Option` | 주문 생성 시 `OptionRepository`로 Option을 직접 조회해 재고를 차감했다. | Option은 Product Aggregate 내부 객체인데 주문 흐름이 루트를 우회했고, 동시 주문에서 같은 옵션 재고가 초과 판매될 수 있었다. | 옵션 재고 변경은 Product 루트의 `subtractOptionQuantity`로 수행한다. Product는 `version`과 `update_dt`를 갖고 옵션 추가/삭제/재고 차감 시 갱신된다. | `ProductContractTest`, `OrderConcurrencyServiceTest`, `V6__Add_product_version.sql`, `V7__Add_product_update_dt.sql` |
| `Wish` | Wish가 `Product` 객체를 직접 들고, 회원은 `memberId` 값으로만 참조했다. | Wish를 별도 Aggregate root로 삼을 때 Product 객체 참조가 Product Aggregate와의 경계를 흐렸다. | Wish는 `memberId`, `productId` 값만 보관한다. 상품 정보가 필요한 응답 조립은 Wish UseCase 서비스에서 `ProductRepository`로 조회한다. | `WishContractTest`, `WishServiceTest`, `WishApiTest` |
| `Product/Wish` | Product 삭제 시 `wish.product_id -> product.id` FK가 삭제를 막았다. | Product Aggregate 삭제 정책이 Wish Aggregate에 묶였다. | Product 삭제는 Wish를 수정/삭제하지 않는다. `wish.product_id` FK를 제거하고, Product가 없는 Wish는 목록에서 미노출한다. | `V4__Remove_wish_product_foreign_key.sql`, `ProductUseCaseServiceTest`, `WishServiceTest` |
| `Member` | 중복 회원가입은 `existsByEmail` 사전 체크로 충분하다고 봤다. | 동시 요청에서는 사전 체크 이후 저장 시점에 unique 제약 위반이 발생할 수 있다. | `member.email` unique 제약을 유지하고, 저장 단계의 중복 이메일 실패도 `400 Bad Request`로 변환한다. | `905d6b1`, `MemberServiceTest`, `MemberApiTest` |
| `Member/Order` | 주문 포인트 차감은 현재 포인트를 읽고 메모리에서 차감한 뒤 저장했다. | 동시 주문에서 같은 회원 포인트를 동시에 차감하면 optimistic lock 실패나 MySQL deadlock/lock 실패가 발생할 수 있다. | `Member.version`으로 포인트 차감 충돌을 감지하고, 트랜잭션 경계의 `ConcurrencyFailureException`은 공통 API 처리에서 `409 Conflict`로 변환한다. | `Member.version`, `OrderConcurrencyServiceTest`, `GlobalExceptionHandler`, `V5__Add_member_version.sql` |
| `Order` | Order가 `Option` 객체를 직접 참조했다. | 주문 이력이 Product/Option 생명주기와 DB FK에 묶이고, afterCommit 메시지 전송 시 lazy/entity 상태에 기대게 된다. | Order는 별도 이력 루트로 보고 `productId`, `optionId`, `memberId` 값과 주문 당시 상품명/옵션명/단가/이미지 URL 스냅샷만 저장한다. | `OrderContractTest`, `OrderServiceTest`, `CreateOrderService`, `V8__Store_order_product_and_option_ids.sql`, `V9__Add_order_snapshot_fields.sql` |

## 진행 중인 작업

| 분류 | 작업 | 현재 상태 |
| --- | --- | --- |
| 구조 해결 | 컨트롤러 로직을 하나의 API 동작당 하나의 UseCase 서비스로 계속 추출한다. | 로그인, 관리자 회원 기능, Order 목록, 남은 API/admin 흐름을 검토해야 한다. |
| 구조 해결 | 서비스/UseCase 메서드에 트랜잭션 경계를 추가한다. | 클래스 단위 `@Transactional`은 사용하지 않고 메서드 단위로만 선언한다. |
| 문제 해결 | FK 삭제 오류, 런타임 오류, 정책 공백을 계속 수집한다. | 새로 발견한 동작 문제는 구조 변경보다 먼저 문제 해결 항목에 기록한다. |

## 객체별 작업 상태

| 객체 | 계약 단위 테스트 | 서비스/API 테스트 | 구조 상태 | 문제 상태 |
| --- | --- | --- | --- | --- |
| `Category` | 완료: `CategoryContractTest` | 완료: `CategoryServiceTest`, `CategoryApiTest` | 완료: controller, domain, service, usecase 패키지 분리 | 삭제 정책 정의 완료: Product와 무관하게 삭제하고 누락된 Category는 `미분류 카테고리`로 표시한다. |
| `Product` | 완료: `ProductContractTest` | 완료: `ProductUseCaseServiceTest`, 관리자 상품 미분류 API 테스트, 주문 재고 동시성 서비스 테스트 | 진행 중: Product 패키지와 UseCase 서비스가 존재한다. | Wish 관련 삭제 정책과 주문 재고 동시성은 완료. Order는 Product id 값만 보관하므로 Product 삭제와 주문 이력은 분리됐다. |
| `Option` | 부분 완료: product/option 서비스 테스트로 일부 커버하지만 독립 계약 테스트는 아직 없다. | 완료: `ProductOptionUseCaseServiceTest`, `OrderConcurrencyServiceTest` | 진행 중: Option 동작은 Product usecase/service 흐름 아래에 있고, 재고 차감은 Product 루트 메서드로 수행한다. `OptionRepository`는 조회/관리 흐름에 유지 중이다. | Order는 Option id 값만 보관하므로 Option 삭제와 주문 이력은 분리됐다. |
| `Member` | 미완료: 포인트와 식별성 규칙 계약 테스트가 필요하다. | 완료: 회원가입/로그인 API, 회원가입 서비스 테스트, 주문 포인트 동시성 서비스 테스트 | 부분 완료: 회원가입 UseCase 서비스는 존재하고 로그인/관리자 회원 로직은 아직 컨트롤러에 남아 있다. | 중복 회원가입과 주문 포인트 차감 동시성은 해결됐고, Wish/Order가 있을 때의 Member 삭제 정책이 더 필요하다. |
| `Wish` | 완료: `WishContractTest` | 완료: `WishServiceTest`, `WishApiTest` | 완료: controller, domain, service, usecase 패키지 분리, 추가/목록/삭제 UseCase 서비스 추출, `Member`/`Product` 직접 객체 참조 제거, 목록 응답용 `wish`-`product` 조인 쿼리 분리 | 현재 API 정책은 정의됨: 인증 필요, 중복 추가는 기존 Wish 반환, 삭제는 소유자만 가능. Product가 없는 Wish는 목록에서 미노출한다. 동시 중복 추가와 Product/Member 삭제 정책은 남아 있다. |
| `Order` | 부분 완료: `OrderContractTest` | 부분 완료: `OrderServiceTest`, `OrderConcurrencyServiceTest` | 진행 중: 생성 UseCase 서비스가 존재하고 목록 UseCase 서비스는 아직 없다. Order는 `productId`, `optionId`, `memberId` 값과 주문 당시 스냅샷을 보관한다. | 재고/포인트 동시성, 주문 목록 스냅샷, Kakao afterCommit은 해결됐다. Wish 정리 동작은 아직 남아 있다. |

## 객체별 리팩터링 TODO

| 객체 | UseCase 식별 | 트랜잭션 경계 확인 | 루트 객체 확인 | 동시성 문제 확인 |
| --- | --- | --- | --- | --- |
| `Category` | 완료: 생성, 목록, 수정, 삭제 UseCase 존재 | 완료: Category 서비스는 메서드 단위 `@Transactional` 사용 | 완료: `Category`는 Product 없이 존재할 수 있는 Aggregate root | TODO: 중복 Category 이름, 동시 수정/삭제 확인 |
| `Product` | 현재 Product/Option 흐름은 완료, 관리자 Product UseCase 구현 검토 필요 | 부분 완료: Product 서비스는 메서드 단위 `@Transactional` 사용, 관리자 흐름 검토 필요 | 완료: `Product`는 Aggregate root이고 `categoryId`만 저장하며 `Option` 재고 변경을 루트 메서드로 수행한다 | 주문 재고 차감 동시성은 완료, TODO: Wish, Option, Order, 누락 Category와 동시 삭제/수정 경쟁 확인 |
| `Option` | 부분 완료: Product 패키지 아래에서 목록/생성/삭제 UseCase 존재 | 현재 Option 서비스는 완료, 이후 수정 흐름 추가 시 경계 필요 | 완료: `Option`은 별도 root가 아니라 `Product`에 소유된다 | 주문 재고 차감 동시성은 Product 루트 version으로 해결, TODO: 중복 Option 생성, Order 존재 중 삭제 확인 |
| `Member` | 부분 완료: 회원가입 UseCase 존재, 로그인/관리자 생성/수정/삭제/포인트 충전 UseCase 필요 | 부분 완료: 회원가입과 주문 포인트 차감 서비스 경계 존재, 로그인/관리자 기능 검토 필요 | TODO: Wish, Order, Point, Kakao access token을 기준으로 `Member` root 경계 확인 | 중복 회원가입과 주문 포인트 차감은 완료, TODO: 동시 포인트 충전과 Member 삭제 확인 |
| `Wish` | 완료: 추가, 목록, 삭제 UseCase 식별 및 서비스 구현 | 완료: Wish 서비스는 메서드 단위 `@Transactional` 사용 | 완료: `Wish`는 별도 루트이며 `memberId`, `productId` 값만 보관한다. 삭제 소유권은 `memberId`로 검증한다. | TODO: 동시 중복 Wish 추가와 소유권 기반 삭제 경쟁 확인 |
| `Order` | 부분 완료: 생성 UseCase 서비스 존재, 목록 UseCase 서비스 구현 필요 | 부분 완료: 생성 서비스는 메서드 단위 `@Transactional` 사용 | 완료: `Order`는 불변 이력 Aggregate root로 보고 Product/Option/Member를 id 값과 주문 당시 스냅샷으로 보관한다 | 재고/포인트 차감 동시성, 목록 스냅샷, Kakao afterCommit은 완료, TODO: Wish 정리 확인 |

## 예정 작업

| 분류 | 작업 | 이유 |
| --- | --- | --- |
| 문제 해결 | Product/Option 삭제 후 Order 이력 조회 정책 정의 | 주문은 Product/Option id 값과 주문 당시 스냅샷을 보관한다. 삭제/변경된 상품/옵션 이름은 Order 목록에 영향을 주지 않는다. |
| 문제 해결 | Wish와 주문 이력이 있는 Member 삭제 정책 정의 | Member 삭제는 Wish/Order FK 위험을 가진다. |
| 문제 해결 | 주문 생성 후 Wish 정리 동작 검증 및 구현 | 의도는 문서화되어 있지만 실제 동작 검증이 필요하다. |
| 문제 해결 | 동시성 실패 API 계약을 객체별로 세분화 | Order 생성은 공통 `409 Conflict` 처리로 정리했지만, 다른 흐름의 동시성 실패 응답은 아직 검토가 필요하다. |
| 구조 해결 | Wish와 Order 흐름의 서비스/UseCase 추출 | 명확한 트랜잭션 경계와 도메인 동작 검증을 가능하게 한다. |
| 구조 해결 | 필요할 때만 Cucumber step definition을 실행 가능한 검증 체계로 추가 | 현재 feature 파일은 실행 테스트가 아니라 블랙박스 명세 역할이다. |

## 객체별 구현 체크리스트

### 환경

- [x] 로컬 개발 환경을 EOL이 아닌 MySQL LTS 버전으로 구성한다.
- [x] API 워크플로우 블랙박스 Cucumber feature 명세를 작성한다.
- [x] Docker Compose MySQL 기반 서비스/API 테스트 환경을 추가한다.

### Category

- [x] Category 생명주기 규칙 계약 테스트를 추가한다.
- [x] Category 생성/목록/수정/삭제 서비스 테스트를 추가한다.
- [x] Category 삭제 정책 API 테스트를 추가한다.
- [x] 생성/목록/수정/삭제 UseCase 서비스를 추출한다.
- [x] Category controller, domain, service, usecase 패키지를 분리한다.
- [x] Category 서비스의 메서드 단위 트랜잭션 경계를 확인한다.
- [x] Product 없이 존재할 수 있는 `Category` Aggregate root를 확인한다.
- [ ] 중복 Category 이름과 동시 수정/삭제 동작을 검토한다.

### Product

- [x] Product의 `categoryId` 참조 규칙 계약 테스트를 추가한다.
- [x] 현재 Product 생성/목록/단건/수정/삭제 서비스 테스트를 추가한다.
- [x] 관리자 상품 목록의 미분류 Category 표시 API 테스트를 추가한다.
- [x] 현재 Product UseCase 서비스를 추출한다.
- [x] Product와 Category Aggregate를 분리한다.
- [ ] 관리자 Product UseCase 구현을 검토한다.
- [ ] 관리자 Product 흐름의 메서드 단위 트랜잭션 경계를 확인한다.
- [x] Wish가 Product를 참조할 때 삭제 정책을 정의한다: Product 삭제 시 Wish는 수정/삭제하지 않는다.
- [x] Order가 Product를 참조할 때 삭제 정책을 정의한다: Order는 product id 값과 상품 스냅샷을 보관한다.
- [x] 삭제/변경된 Product id를 가진 Order 목록 표시 정책을 정의한다: 주문 당시 상품 스냅샷을 표시한다.
- [ ] Wish, Option, Order, 누락 Category와 Product 동시 수정/삭제 경쟁을 검토한다.

### Option

- [x] 현재 Product Option 목록/생성/삭제 UseCase 서비스 테스트를 추가한다.
- [x] 현재 Option UseCase 서비스를 Product 패키지 아래에 추출한다.
- [x] `Option`은 별도 Aggregate root가 아니라 `Product` 소유 객체로 본다.
- [ ] Option 이름, 수량, 재고 규칙에 대한 독립 계약 테스트를 추가한다.
- [x] Order가 Option을 참조할 때 삭제 정책을 정의한다: Order는 option id 값과 옵션명 스냅샷을 보관한다.
- [x] 삭제/변경된 Option id를 가진 Order 목록 표시 정책을 정의한다: 주문 당시 옵션 스냅샷을 표시한다.
- [ ] 동시 재고 변경, 중복 Option 생성, Order 존재 중 삭제를 검토한다.

### Member

- [x] 회원가입, 로그인, 요청 DTO validation, 중복 회원가입 API 테스트를 추가한다.
- [x] 회원가입과 중복 이메일 동작 서비스 테스트를 추가한다.
- [x] Member 회원가입 UseCase 서비스를 추출한다.
- [x] 동시 중복 회원가입이 `400 Bad Request`를 반환하도록 수정한다.
- [ ] Member 포인트 충전/차감과 식별성 규칙 계약 테스트를 추가한다.
- [ ] 로그인 로직을 하나의 API 동작에 대응하는 UseCase 서비스로 추출한다.
- [ ] 관리자 회원 생성/수정/삭제/포인트 충전 동작을 각각 하나의 UseCase 서비스로 추출한다.
- [ ] Wish, Order, Point, Kakao access token 기준으로 `Member` Aggregate root 경계를 확인한다.
- [ ] Wish 또는 주문 이력이 있는 Member 삭제 정책을 정의한다.
- [ ] 동시 포인트 충전/차감과 Member 삭제 동작을 검토한다.

### Wish

- [x] 추가/목록/삭제 UseCase를 식별한다.
- [x] 소유권과 삭제 규칙 계약 테스트를 추가한다.
- [x] 추가/목록/삭제 서비스/API 테스트를 추가한다.
- [x] 컨트롤러 로직을 하나의 API 동작당 하나의 UseCase 서비스로 추출한다.
- [x] 서비스 추출 후 메서드 단위 트랜잭션 경계를 추가한다.
- [x] `Wish`를 별도 Aggregate root로 보고 `Member`/`Product` 직접 객체 참조를 제거한다.
- [x] 목록 응답 조립은 `JdbcTemplate` 기반 query 객체에서 `wish`와 `product`를 inner join해서 처리한다.
- [x] Wish row는 있지만 Product row가 없으면 목록에 노출하지 않는다.
- [x] Product 삭제 시 Wish는 수정/삭제하지 않고 그대로 둔다.
- [ ] 중복 Wish 추가와 소유권 기반 삭제 경쟁을 검토한다.

### Order

- [x] 생성 UseCase를 식별한다.
- [ ] 목록 UseCase 서비스를 구현한다.
- [x] 재고, 포인트 차감, 불변 주문 이력 규칙 계약 테스트를 추가한다.
- [ ] 주문 이력, Kakao 부수효과, Wish 정리 서비스/API 테스트를 추가한다.
- [ ] 컨트롤러 로직을 하나의 API 동작당 하나의 UseCase 서비스로 추출한다.
- [x] `Order`를 불변 이력 Aggregate root로 보고 Product/Option/Member 직접 객체 참조를 제거한다.
- [x] Order 목록과 Kakao 메시지에 필요한 상품명, 옵션명, 단가, 이미지 URL을 주문 당시 스냅샷으로 보관한다.
- [x] 생성 서비스 추출 후 메서드 단위 트랜잭션 경계를 추가한다.
- [x] Kakao 메시지 전송을 주문 트랜잭션 성공 이후로 이동한다.
- [ ] 주문 생성 후 구매자와 주문 상품의 Wish 정리를 구현한다.
- [x] 동시 재고 차감과 포인트 차감을 검토한다.

### 공통

- [ ] 각 객체의 계약 테스트를 먼저 세운 뒤 Option 재고와 Member 포인트 도메인 책임을 강화한다.
- [ ] 최종 확인 시 `./gradlew test`와 `./gradlew build`를 실행한다.
- [ ] AI 사용 내역과 검증 근거를 문서에 기록한다.
