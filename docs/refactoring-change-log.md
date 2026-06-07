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

조회 API는 JPA 엔티티 탐색이나 derived repository query가 아니라 query UseCase
서비스와 `JdbcTemplate` 기반 query 객체로 구현한다. 객체와 Aggregate 관계를 바꾸는
구조 리팩터링이 조회 성능, join 형태, N+1 발생 여부에 영향을 주지 않도록 읽기 모델을
SQL로 명시한다. 조회 UseCase 구현체와 query 객체는 각 도메인의 `query` 패키지에
두고, command UseCase 서비스에서는 query 패키지 객체를 주입하거나 호출하지 않는다.

Repository는 Aggregate root에만 둔다. `Option`처럼 루트가 아닌 하위 객체는
소유 루트인 `Product` 메서드로 변경하고 `ProductRepository.save(product)`로
저장한다. 조회 API가 하위 객체 목록을 반환해야 하면 repository 대신 전용
`JdbcTemplate` query 객체를 사용한다.

테스트는 기본적으로 Mockito를 사용하지 않고 fake/stub을 둔다. 다만 사용자가 명시적으로
중요한 이벤트 발행 경계의 상호작용 검증을 요청한 경우에는 해당 단위 테스트에 한해
Mockito `verify(times/never)`를 사용한다.

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
| `3542836` | 옵션 재고 변경을 Product 루트 경유로 변경했다. | Product Aggregate root 기준으로 옵션 재고 변경과 낙관적 락 경계를 맞췄다. |
| `3542836` | Order가 Product/Option을 객체가 아니라 id 값과 주문 당시 스냅샷으로 보관하게 했다. | 주문 이력을 별도 루트로 두고 Product/Option 삭제/변경 정책과 주문 이력 표시를 분리했다. |
| `2ef5190` | Order 목록 조회를 `JdbcTemplate` query DAO와 `GetOrdersUseCase`로 분리했다. | 객체 관계 변경이 조회 성능이나 N+1 문제로 이어지지 않게 주문 조회를 명시적 SQL 읽기 모델로 고정했다. |
| `2ef5190` | Order 패키지를 controller, domain, service, usecase로 분리했다. | 주문 생성/목록, Order 이력 루트, Kakao 메시지 부수효과의 책임 위치를 명확히 했다. |
| `264bed7` | Kakao 메시지를 Spring 이벤트 기반 afterCommit 비동기 listener와 `KakaoMessageSender` 포트로 이동했다. | 외부 부수효과가 DB 트랜잭션 성공 전 발생하지 않고, API 응답 시간을 막지 않도록 했다. 이벤트 발행 여부는 Mockito `verify(times/never)`로, 외부 전송은 fake sender로 검증한다. |
| `9a0331d` | `OptionRepository`를 제거하고 옵션 쓰기 흐름을 Product 루트 저장으로 통일했다. | Aggregate root repository만 허용하는 기준을 Product/Option에 적용했다. |
| `9a0331d` | Product 단건/목록 조회 응답에 옵션 목록을 포함하고 `ProductQueryDao`/`OptionQueryDao`를 도입했다. | Product/Option 객체 관계 변경이 조회 API 성능이나 N+1 문제에 영향을 주지 않도록 SQL 읽기 모델을 분리했다. |
| `68154b9` | 조회 UseCase 구현체와 query DAO를 각 도메인의 `query` 패키지로 이동했다. | command 서비스가 query 객체를 사용하지 않도록 패키지 경계를 분리했다. |
| `a1e12f6`, `aead396` | Category/Product/Option/Wish/Order 입력을 controller request DTO에서 UseCase command/VO로 분리하고 command record를 `dto` 패키지로 이동했다. | 단순 요청값 검증은 컨트롤러에서 끝내고, 트랜잭션 서비스는 검증 완료 입력과 DB 상태 의존 규칙만 다루게 했다. |
| `99bf84c` | Member 회원가입/로그인/관리자 흐름을 UseCase 서비스로 추출했다. | Member 컨트롤러에서 repository와 인증 로직을 제거하고 메서드 단위 트랜잭션 경계를 서비스에 둔다. |
| `f5dafcd` | Member 관련 auth/admin/controller/domain 패키지와 service/usecase workflow를 정리했다. | Member 인증과 관리자 흐름의 소유 도메인과 클래스 책임을 명확히 했다. |
| `27425e4` | Kakao 로그인 UseCase 이름과 Kakao 인가 URI provider 역할을 정리했다. | 사용자가 Kakao로 로그인한다는 핵심 UseCase와 OAuth 보조 컴포넌트를 분리했다. |
| `a79a46b` | 관리자 상품 화면의 목록/단건/생성/수정/삭제/폼 카테고리 조회 UseCase 서비스를 추가했다. | 관리자 상품 컨트롤러의 repository 직접 호출을 제거하고 트랜잭션 경계를 서비스로 이동했다. |
| `이번 변경` | Member 삭제를 물리 삭제에서 소프트 삭제로 변경했다. | Wish/Order FK를 유지하면서 회원 삭제 의도를 표현하고, 삭제된 회원을 조회/로그인/인증 대상에서 제외한다. |
| `이번 변경` | 권한 체크 범위를 명시했다. | 이번까지의 리팩터링과 정책 변경은 권한 체크 동작을 변경하지 않았고, 기존 접근 동작은 그대로 둔다. |

## 완료된 문제 해결

| 커밋 | 해결한 문제 | 결과 |
| --- | --- | --- |
| `1a756f6` | 삭제 요청에서 FK 오류가 `500 Internal Server Error`로 노출되는 실제 오류를 확인했다. | 삭제 실패와 FK 제약을 README에 기록했다. |
| `bb32929` | 상품이 걸린 Category 삭제 정책이 명시되지 않았다. | 처음에는 상품이 있는 Category 삭제를 거절하는 규칙을 추가했다. 이 규칙은 이후 미분류 정책으로 대체됐다. |
| `c72940d` | Category 생명주기가 Product에 과하게 묶여 있었다. | Category 삭제를 Product와 무관하게 허용하고, Category row가 없는 Product는 목록에서 `미분류 카테고리`로 표시하도록 했다. |
| `905d6b1` | 동시 중복 회원가입이 `500 Internal Server Error`로 드러날 수 있었다. | 중복 이메일 저장 실패를 `400 Bad Request`와 `Email is already registered.` 메시지로 변환했다. |
| `81ebc15` | 동시 주문에서 회원 포인트 차감 실패가 DB lock/deadlock 예외로 노출될 수 있었다. | `Member.version`을 추가하고 포인트 낙관적 락 충돌을 도메인 실패로 변환했다. |
| `3542836` | 동시 주문에서 같은 옵션 재고가 초과 판매될 수 있었다. | `Product.version`과 `update_dt`를 추가하고 옵션 재고 차감을 Product 루트 메서드로 수행하게 했다. |
| `3542836` | 주문 생성 서비스가 `saveAndFlush`로 중간 flush를 강제하고 낙관적 락 예외를 트랜잭션 내부에서 변환했다. | 중간 flush를 제거하고 트랜잭션 경계에서 발생한 동시성 실패를 공통 API 예외 처리에서 `409 Conflict`로 변환한다. |
| `3542836` | 주문 이력이 `Option` 엔티티와 DB FK에 묶여 있어 옵션 삭제 정책이 주문 FK에 의해 결정됐다. | `orders.product_id`를 추가하고 `orders.option_id` FK를 제거해 주문 이력은 생성 당시 id 값과 스냅샷을 보관하게 했다. |
| `3542836` | Order 목록이 현재 Product/Option을 조회하면 과거 주문의 상품명, 옵션명, 가격이 바뀌어 보일 수 있었다. | Order에 상품명, 옵션명, 단가, 이미지 URL 스냅샷을 저장하고 목록 응답은 이 값을 사용한다. |
| `264bed7` | Kakao 메시지가 주문 트랜잭션 성공 전에 전송되거나 afterCommit 동기 처리로 응답 시간을 지연시킬 수 있었다. | 메시지에 필요한 스냅샷을 이벤트로 발행하고 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` listener에서 `KakaoMessageSender` 포트를 통해 best-effort로 전송한다. |
| `2ef5190` | 주문 생성 후 Wish를 장바구니처럼 정리해야 한다는 이전 가정이 반복 구매 상품 정책과 맞지 않았다. | 주문 성공 후에도 Wish는 유지한다. Wish는 반복 구매 후보이고 Order는 구매 이력이다. |
| `c6dd158` | 두 옵션을 동시에 삭제하면 둘 다 성공해 등록된 모든 옵션이 제거될 수 있었다. | `Product.version` 경계에서 삭제 충돌을 감지하고, 옵션이 등록된 상품의 마지막 옵션 삭제를 금지하는 규칙을 서비스/API 테스트로 고정했다. 상품 생성 시점에는 옵션 없이 존재할 수 있다. 마지막 옵션 삭제 메시지는 `옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.`이다. |
| `924a49c` | 주문으로 옵션 재고를 차감하는 동시에 같은 Product의 옵션이 삭제될 수 있다. | 두 트랜잭션이 같은 Product version을 읽으면 하나만 커밋되고 다른 하나는 optimistic lock failure로 실패한다는 서비스 테스트를 추가했다. |
| `1ccf7bb`, `a1e12f6` | 단순 요청값 검증이 트랜잭션 서비스 내부 또는 HTTP request DTO 의존 경계에 남아 있을 수 있었다. | 컨트롤러가 Bean Validation 이후 `ProductName`/`OptionName` VO와 각 도메인 command를 생성하고, UseCase 서비스는 command만 받는다. 존재 확인, 중복, 소유권, 재고/포인트 부족처럼 DB 또는 Aggregate 상태에 의존하는 규칙만 트랜잭션 내부에 둔다. |
| `e23c994` | `GET /api/orders` 미인증 요청이 컨트롤러 인증 로직 전에 `400 Bad Request`로 처리됐다. | Order API도 Authorization header를 optional로 받고 인증 resolver 결과가 없으면 `401 Unauthorized`를 반환하게 했다. `OrderApiTest`로 생성, 목록, 실패, 인증, Wish 유지 계약을 고정했다. |
| `99bf84c` | `MemberController.login()`이 `MemberRepository`와 `JwtProvider`를 직접 사용했다. | `LoginMemberUseCase` 구현체를 추가하고 로그인 성공/실패를 `MemberServiceTest`로 고정했다. 컨트롤러는 회원가입/로그인 UseCase만 호출한다. |
| `99bf84c` | Member 포인트 규칙과 관리자 포인트 충전 흐름이 컨트롤러/repository 직접 호출에 기대고 있었다. | `MemberContractTest`로 포인트 충전/차감 규칙을 고정하고 `ChargeMemberPointUseCase` 구현체를 추가했다. 관리자 포인트 충전 endpoint는 해당 UseCase를 호출한다. |
| `99bf84c` | 관리자 회원 생성/목록/수정/삭제가 `AdminMemberController`에서 repository를 직접 호출했다. | 관리자 회원 생성/목록/단건 조회/수정/삭제 UseCase 구현체를 추가하고 `AdminMemberApiTest`와 `MemberServiceTest`로 고정했다. |
| `99bf84c` | Member 오류 메시지가 `Member not found. id=...`처럼 DB 식별자를 노출할 수 있었다. | Member 사용자 노출 오류 메시지는 `회원이 존재하지 않습니다.`로 고정했다. 다른 도메인의 `id=` 노출 메시지는 공통 오류 메시지 정책 정리 작업에서 추가 검토한다. |
| `27425e4` | 일반 회원가입/로그인과 Kakao 로그인/자동 회원가입 흐름의 입력 경계가 달랐고, `KakaoAuthController`가 외부 client와 repository를 직접 호출했다. | 일반 인증 UseCase는 `MemberCredentialsCommand`, Kakao callback UseCase는 `KakaoAuthorizationCodeCommand`를 받게 했다. `KakaoLoginClient`를 외부 API 포트로 분리하고 실제 REST 구현체와 fake 테스트 구현을 나눴다. |
| `f5dafcd` | `auth`, `member`, `admin`이 최상위/동일 패키지에 섞여 있어 Member 관련 인증과 관리자 흐름의 소유 도메인이 불분명했고, `service`/`usecase`도 클래스가 너무 많아 한 패키지에서 목적을 읽기 어려웠다. | `gift.member` 아래 `auth`, `admin`, `controller`, `domain`을 분리하고, `service`/`usecase`는 `auth`와 `management` workflow 하위 패키지로 정리했다. 테스트 패키지도 같은 구조로 맞췄다. |
| `aead396` | UseCase command record가 `usecase` 패키지에 있어 포트와 입력 DTO의 역할이 섞여 보였다. | `CategoryCommand`, `ProductCommand`, `OptionCommand`, `MemberCredentialsCommand`, `KakaoAuthorizationCodeCommand`, `WishCommand`, `OrderCommand`를 각 도메인의 `dto` 패키지로 이동했다. |
| `a79a46b` | 관리자 상품 화면이 `AdminProductController`에서 `ProductRepository`, `CategoryRepository`를 직접 호출해 트랜잭션 경계와 책임이 컨트롤러에 남아 있었다. | 관리자 상품 목록/단건/생성/수정/삭제/폼 카테고리 조회 UseCase 구현체를 추가하고, 컨트롤러는 UseCase만 호출하도록 변경했다. `AdminProductUseCaseServiceTest`로 실제 DB 기반 동작을 고정했다. |
| `이번 변경` | Member 물리 삭제가 `wish.member_id`, `orders.member_id` FK에 막히거나 DB 예외로 노출될 수 있었다. | `member.deleted` 컬럼을 추가하고 삭제 UseCase는 소프트 삭제만 수행한다. Wish/Order row는 유지하며, 삭제된 회원은 관리자 목록/단건 조회/일반 로그인/Kakao 로그인/토큰 인증에서 제외한다. |
| `이번 변경` | 일반 회원가입, 관리자 회원 생성/수정, 일반 로그인 흐름이 비밀번호를 평문으로 저장하고 평문 문자열 비교로 검증했다. | `Password` VO가 고정 BCrypt encoder로 인코딩한 값을 `value()`로 들고 UseCase에 전달되게 했다. `Member` 생성/수정은 `Password`를 받아 내부에서 인코딩 값을 저장하고, 로그인은 `Password.matches(...)`로 검증한다. |
| `이번 변경` | 관리자 회원 수정에서 Kakao 로그인으로 생성된 회원도 로컬 비밀번호를 설정할 수 있었다. | Kakao access token이 있는 회원은 Kakao 계정으로 보고, 관리자 수정에서 비밀번호 변경을 시도하면 `카카오 계정은 비밀번호를 변경할 수 없습니다.`로 거부한다. |

## 식별된 정책 변경

| 객체 | 이전 동작 또는 가정 | 식별한 문제 | 현재 정책 | 근거 |
| --- | --- | --- | --- | --- |
| `Category` | Product가 참조하는 Category는 DB FK 또는 명시적 존재 체크 때문에 삭제할 수 없다고 봤다. | Category 생명주기가 Product에 너무 강하게 묶였다. Product는 삭제된 Category id를 갖고도 목록에 표시될 수 있다. | Category 삭제는 Product와 독립적으로 허용한다. Product를 이동하거나 삭제하지 않고, Category row가 없으면 `미분류 카테고리`로 표시한다. | `c72940d`, `CategoryServiceTest`, `CategoryApiTest`, `AdminProductApiTest` |
| `Product` | Product가 Category 객체를 직접 갖거나 직접 의존한다고 봤다. | Product -> Category 객체 결합 때문에 Aggregate 경계가 흐려졌다. | Product는 `categoryId` 값만 저장한다. Product와 Category는 서로 다른 Aggregate root이다. | `3e01cfa`, `ProductContractTest` |
| `Product/Option` | 주문 생성 시 `OptionRepository`로 Option을 직접 조회해 재고를 차감했다. | Option은 Product Aggregate 내부 객체인데 주문 흐름이 루트를 우회했고, 동시 주문 또는 주문 중 옵션 변경에서 Product 상태가 충돌할 수 있었다. | 옵션 재고 변경은 Product 루트의 `subtractOptionQuantity`로 수행한다. Product는 `version`과 `update_dt`를 갖고 옵션 추가/삭제/재고 차감 시 갱신된다. `OptionRepository`는 제거하고 쓰기 흐름은 Product 루트 저장으로 통일한다. | `ProductContractTest`, `OrderConcurrencyServiceTest`, `V6__Add_product_version.sql`, `V7__Add_product_update_dt.sql` |
| `Product/Option` | Product 조회 응답은 Product 필드만 반환하고 옵션은 별도 endpoint에서만 조회한다고 봤다. | 상품 조회자가 실제 판매 가능한 선택지/재고를 알기 위해 추가 요청을 강제받고, JPA 관계를 직접 노출하면 N+1 위험이 생긴다. | Product 단건/목록 조회 응답은 옵션 목록을 포함한다. Product/Option 조회 API는 `JdbcTemplate` query 객체가 명시적 SQL로 조립한다. 옵션 단독 목록 endpoint는 유지한다. | `ProductQueryDao`, `OptionQueryDao`, `ProductUseCaseServiceTest`, `ProductOptionUseCaseServiceTest` |
| `Product/Option` | Option 이름 중복 범위가 명확하지 않았다. | Option은 전역 루트가 아니라 Product가 소유하는 하위 객체이므로 전역 이름 unique는 과한 제약이다. | 같은 Product 안에서는 Option 이름이 중복되면 안 된다. 서로 다른 Product의 같은 Option 이름은 허용한다. 동시 생성 충돌은 Product version으로 하나만 성공하게 한다. | `Product.addOption`, `ProductOptionUseCaseServiceTest` |
| `Wish` | Wish가 `Product` 객체를 직접 들고, 회원은 `memberId` 값으로만 참조했다. | Wish를 별도 Aggregate root로 삼을 때 Product 객체 참조가 Product Aggregate와의 경계를 흐렸다. | Wish는 `memberId`, `productId` 값만 보관한다. 상품 정보가 필요한 응답 조립은 Wish UseCase 서비스에서 `ProductRepository`로 조회한다. | `WishContractTest`, `WishServiceTest`, `WishApiTest` |
| `Product/Wish` | Product 삭제 시 `wish.product_id -> product.id` FK가 삭제를 막았다. | Product Aggregate 삭제 정책이 Wish Aggregate에 묶였다. | Product 삭제는 Wish를 수정/삭제하지 않는다. `wish.product_id` FK를 제거하고, Product가 없는 Wish는 목록에서 미노출한다. | `V4__Remove_wish_product_foreign_key.sql`, `ProductUseCaseServiceTest`, `WishServiceTest` |
| `Wish/Order` | 주문 생성 후 해당 상품의 Wish를 제거해야 한다고 봤다. | Wish는 반복 구매 후보일 수 있어 주문과 동시에 삭제하면 사용자의 재구매 의도가 사라진다. | 주문 생성은 Wish를 삭제하지 않는다. | `OrderServiceTest` |
| `Member` | 중복 회원가입은 `existsByEmail` 사전 체크로 충분하다고 봤다. | 동시 요청에서는 사전 체크 이후 저장 시점에 unique 제약 위반이 발생할 수 있다. | `member.email` unique 제약을 유지하고, 저장 단계의 중복 이메일 실패도 `400 Bad Request`로 변환한다. | `905d6b1`, `MemberServiceTest`, `MemberApiTest` |
| `Member` | 회원 비밀번호를 문자열 값으로 UseCase에 전달하고 그대로 저장/비교해도 된다고 봤다. | 회원가입/관리자 생성/수정에서 비밀번호가 평문으로 저장되고 로그인도 평문 문자열 비교에 의존했다. | 컨트롤러가 `Password.encode(raw)`로 `Password` VO를 만들고, `Password.value()`는 저장할 BCrypt 인코딩 값이다. encoder는 `Password` 내부의 고정 객체로만 사용한다. `Member`는 `Password`를 넘겨받아 내부에서 저장값을 꺼내고, 로그인은 `Password.matches(...)`로 검증한다. | `PasswordTest`, `MemberContractTest`, `MemberServiceTest`, `MemberApiTest`, `AdminMemberApiTest` |
| `Member` | Kakao 로그인 회원도 관리자 수정에서 로컬 비밀번호를 설정할 수 있다고 봤다. | Kakao 계정에 로컬 비밀번호가 생기면 외부 인증 계정과 일반 비밀번호 계정의 경계가 흐려지고, 일반 로그인 경로가 열릴 수 있다. | Kakao access token이 있는 회원은 Kakao 계정으로 보고 로컬 비밀번호 변경을 금지한다. 관리자 수정 화면은 같은 폼에 에러를 표시하고 기존 email/password/token 상태를 유지한다. | `MemberContractTest`, `MemberServiceTest`, `AdminMemberApiTest` |
| `Member/Order` | 주문 포인트 차감은 현재 포인트를 읽고 메모리에서 차감한 뒤 저장했다. | 동시 주문에서 같은 회원 포인트를 동시에 차감하면 optimistic lock 실패나 MySQL deadlock/lock 실패가 발생할 수 있다. | `Member.version`으로 포인트 차감 충돌을 감지하고, 트랜잭션 경계의 `ConcurrencyFailureException`은 공통 API 처리에서 `409 Conflict`로 변환한다. | `Member.version`, `OrderConcurrencyServiceTest`, `GlobalExceptionHandler`, `V5__Add_member_version.sql` |
| `Member/Wish/Order` | 회원 삭제는 `member` row를 물리 삭제한다고 봤다. | Wish/Order가 `member_id` FK로 회원 row를 참조하므로 물리 삭제는 FK 실패를 만들고, 주문 이력의 소유자 값도 사라진다. | 회원 삭제는 소프트 삭제다. `member.deleted=true`로 표시하고 Wish/Order는 그대로 둔다. 삭제된 회원은 관리자 목록/단건 조회, 일반 로그인, Kakao 로그인, 토큰 인증에서 제외한다. 같은 이메일 재가입은 기존 unique 제약 때문에 계속 거절된다. | `V10__Add_member_deleted.sql`, `MemberContractTest`, `MemberServiceTest`, `AdminMemberApiTest`, `MemberApiTest`, `KakaoAuthServiceTest`, `KakaoAuthApiTest`, `WishApiTest` |
| `Order` | Order가 `Option` 객체를 직접 참조했다. | 주문 이력이 Product/Option 생명주기와 DB FK에 묶이고, afterCommit 메시지 전송 시 lazy/entity 상태에 기대게 된다. | Order는 별도 이력 루트로 보고 `productId`, `optionId`, `memberId` 값과 주문 당시 상품명/옵션명/단가/이미지 URL 스냅샷만 저장한다. | `OrderContractTest`, `OrderServiceTest`, `CreateOrderService`, `V8__Store_order_product_and_option_ids.sql`, `V9__Add_order_snapshot_fields.sql` |

## 진행 중인 작업

| 분류 | 작업 | 현재 상태 |
| --- | --- | --- |
| 구조 해결 | 컨트롤러 로직을 하나의 API 동작당 하나의 UseCase 서비스로 계속 추출한다. | 새로 발견되는 controller 직접 repository 호출이나 트랜잭션 책임을 문제로 기록한 뒤 UseCase 서비스로 옮긴다. |
| 구조 해결 | 서비스/UseCase 메서드에 트랜잭션 경계를 추가한다. | 클래스 단위 `@Transactional`은 사용하지 않고 메서드 단위로만 선언한다. |
| 구조 해결 | 서비스 입력 경계를 HTTP request DTO가 아니라 command/VO로 통일한다. | 단순 값 검증은 컨트롤러에서 트랜잭션 시작 전에 끝내고, 서비스는 검증 완료 입력과 상태 의존 규칙만 처리한다. |
| 문제 해결 | FK 삭제 오류, 런타임 오류, 정책 공백을 계속 수집한다. | 새로 발견한 동작 문제는 구조 변경보다 먼저 문제 해결 항목에 기록한다. |

## 객체별 작업 상태

| 객체 | 계약 단위 테스트 | 서비스/API 테스트 | 구조 상태 | 문제 상태 |
| --- | --- | --- | --- | --- |
| `Category` | 완료: `CategoryContractTest` | 완료: `CategoryServiceTest`, `CategoryApiTest` | 완료: controller, domain, query, service, usecase 패키지 분리, 생성/수정 입력은 `CategoryCommand`로 전달 | 삭제 정책 정의 완료: Product와 무관하게 삭제하고 누락된 Category는 `미분류 카테고리`로 표시한다. |
| `Product` | 완료: `ProductContractTest` | 완료: `ProductUseCaseServiceTest`, `AdminProductUseCaseServiceTest`, 관리자 상품 미분류 API 테스트, 주문 재고 동시성 서비스 테스트 | 진행 중: Product 패키지와 UseCase 서비스가 존재하고 조회 구현은 query 패키지에 있다. 일반 API와 관리자 상품 입력은 `ProductName` VO와 `ProductCommand`로 전달한다. 관리자 상품 컨트롤러는 repository를 직접 호출하지 않는다. | Wish 관련 삭제 정책과 주문 재고 동시성은 완료. Order는 Product id 값만 보관하므로 Product 삭제와 주문 이력은 분리됐다. |
| `Option` | 부분 완료: product/option 서비스 테스트로 일부 커버하지만 독립 계약 테스트는 아직 없다. | 완료: `ProductOptionUseCaseServiceTest`, `OptionApiTest`, `OrderServiceTest`, `OrderConcurrencyServiceTest` | 진행 중: Option 동작은 Product usecase/service 흐름 아래에 있고, 생성/삭제/재고 차감은 Product 루트 메서드로 수행한다. 생성 입력은 `OptionName` VO와 `OptionCommand`로 전달하고 조회 API는 `OptionQueryDao`가 담당한다. | Order는 Option id와 스냅샷만 보관하므로 주문된 Option도 Product Aggregate 규칙상 삭제 가능하면 삭제된다. 상품 생성 시 옵션은 없어도 되지만, 옵션이 등록된 뒤 마지막 옵션 삭제는 금지한다. |
| `Member` | 완료: `MemberContractTest` | 완료: 회원가입/로그인 API, Kakao 인증 API, 관리자 회원 API, 회원가입/로그인/Kakao callback/포인트 충전/관리자 회원 소프트 삭제/수정/포인트 충전 서비스 테스트, 주문 포인트 동시성 서비스 테스트 | 부분 완료: `auth`, `admin`, `controller`, `domain` 패키지를 분리하고, `service`/`usecase`는 `auth`와 `management`로 분류했다. 회원가입/로그인/Kakao 인증/관리자 회원/포인트 충전/소프트 삭제 UseCase 서비스는 존재한다. | 중복 회원가입, 주문 포인트 차감 동시성, Wish/Order가 있는 회원 삭제 정책, Kakao 계정 비밀번호 변경 금지는 해결됐다. TODO: 동시 포인트 충전/차감과 Member 삭제 경쟁 확인 |
| `Wish` | 완료: `WishContractTest` | 완료: `WishServiceTest`, `WishApiTest` | 완료: controller, domain, query, service, usecase 패키지 분리, 추가 입력은 `WishCommand`로 전달, `Member`/`Product` 직접 객체 참조 제거, 목록 응답용 `wish`-`product` 조인 쿼리 분리 | 현재 API 정책은 정의됨: 인증 필요, 중복 추가는 기존 Wish 반환, 삭제는 소유자만 가능. Product가 없는 Wish는 목록에서 미노출한다. 동시 중복 추가와 Product/Member 삭제 정책은 남아 있다. |
| `Order` | 부분 완료: `OrderContractTest`, `CreateOrderServiceTest`, `KakaoOrderMessageListenerTest` | 완료: `OrderServiceTest`, `OrderConcurrencyServiceTest`, `OrderApiTest` | 완료: controller, domain, query, service, usecase 패키지 분리. 생성 입력은 `OrderCommand`로 전달하고, 목록 조회는 `JdbcTemplate` query DAO로 `orders` 스냅샷을 읽는다. Order는 `productId`, `optionId`, `memberId` 값과 주문 당시 스냅샷을 보관한다. | 재고/포인트 동시성, 주문 목록 스냅샷, Kakao afterCommit async, 주문 후 Wish 유지 정책, 미인증 API 응답 정책은 해결됐다. |

## 객체별 리팩터링 TODO

| 객체 | UseCase 식별 | 트랜잭션 경계 확인 | 루트 객체 확인 | 동시성 문제 확인 |
| --- | --- | --- | --- | --- |
| `Category` | 완료: 생성, 목록, 수정, 삭제 UseCase 존재 | 완료: Category 서비스는 메서드 단위 `@Transactional` 사용 | 완료: `Category`는 Product 없이 존재할 수 있는 Aggregate root | TODO: 중복 Category 이름, 동시 수정/삭제 확인 |
| `Product` | 완료: Product/Option 흐름과 관리자 Product 목록/단건/생성/수정/삭제/폼 카테고리 조회 UseCase 존재 | 완료: Product 서비스와 관리자 Product 서비스는 메서드 단위 `@Transactional` 사용 | 완료: `Product`는 Aggregate root이고 `categoryId`만 저장하며 `Option` 재고 변경을 루트 메서드로 수행한다 | 주문 재고 차감과 주문 중 옵션 삭제 동시성은 Product version으로 확인했다. TODO: Wish, Order, 누락 Category와 동시 삭제/수정 경쟁 확인 |
| `Option` | 부분 완료: Product 패키지 아래에서 목록/생성/삭제 UseCase 존재 | 현재 Option 서비스는 완료, 이후 수정 흐름 추가 시 경계 필요 | 완료: `Option`은 별도 root가 아니라 `Product`에 소유된다 | 주문 재고 차감, 동시 옵션 삭제, 주문 중 옵션 삭제 충돌, 동시 중복 이름 생성은 Product 루트 version으로 해결했다. |
| `Member` | 완료: 회원가입/로그인/Kakao 로그인/관리자 생성/목록/단건 조회/수정/삭제/포인트 충전 UseCase 존재. Kakao 인가 URI 생성은 UseCase가 아니라 `KakaoAuthorizationUriProvider`가 담당한다. | 완료: 회원가입, 로그인, Kakao callback, 관리자 회원, 포인트 충전, 주문 포인트 차감, 소프트 삭제 서비스 경계 존재 | 부분 완료: `Member`는 회원 상태와 포인트의 루트이며 Wish/Order는 `memberId` 값만 가진다. 삭제는 `deleted` 상태 변경으로 처리한다. | 중복 회원가입, 주문 포인트 차감, Wish/Order 참조 중 삭제 정책은 완료. TODO: 동시 포인트 충전/차감과 Member 삭제 경쟁 확인 |
| `Wish` | 완료: 추가, 목록, 삭제 UseCase 식별 및 서비스 구현 | 완료: Wish 서비스는 메서드 단위 `@Transactional` 사용 | 완료: `Wish`는 별도 루트이며 `memberId`, `productId` 값만 보관한다. 삭제 소유권은 `memberId`로 검증한다. | TODO: 동시 중복 Wish 추가와 소유권 기반 삭제 경쟁 확인 |
| `Order` | 완료: 생성 UseCase 서비스와 목록 UseCase 서비스 존재 | 완료: 생성/목록 서비스는 메서드 단위 `@Transactional` 사용 | 완료: `Order`는 불변 이력 Aggregate root로 보고 Product/Option/Member를 id 값과 주문 당시 스냅샷으로 보관한다 | 재고/포인트 차감 동시성, 목록 스냅샷, Kakao afterCommit async, 주문 후 Wish 유지는 완료 |

## 예정 작업

| 분류 | 작업 | 이유 |
| --- | --- | --- |
| 문제 해결 | Product/Option 삭제 후 Order 이력 조회 정책 정의 | 주문은 Product/Option id 값과 주문 당시 스냅샷을 보관한다. 삭제/변경된 상품/옵션 이름은 Order 목록에 영향을 주지 않는다. |
| 문제 해결 | 동시 포인트 충전/차감과 Member 삭제 정책 검증 | Member 삭제는 소프트 삭제로 정의됐지만 포인트 변경과 동시에 실행될 때의 응답 계약은 추가 검토가 필요하다. |
| 문제 해결 | 사용자 노출 오류 메시지에서 내부 id 제거 | URL 경로나 DB 식별자를 오류 본문에 반복 노출하지 않고, 내부 추적은 로그로 분리한다. |
| 문제 해결 | 동시성 실패 API 계약을 객체별로 세분화 | Order 생성은 공통 `409 Conflict` 처리로 정리했지만, 다른 흐름의 동시성 실패 응답은 아직 검토가 필요하다. |
| 구조 해결 | 새로 발견되는 controller 직접 로직의 서비스/UseCase 추출 | 명확한 트랜잭션 경계와 도메인 동작 검증을 가능하게 한다. |
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
- [x] Category 조회 UseCase 구현체를 query 패키지로 분리한다.
- [x] Category 서비스의 메서드 단위 트랜잭션 경계를 확인한다.
- [x] Product 없이 존재할 수 있는 `Category` Aggregate root를 확인한다.
- [ ] 중복 Category 이름과 동시 수정/삭제 동작을 검토한다.

### Product

- [x] Product의 `categoryId` 참조 규칙 계약 테스트를 추가한다.
- [x] 현재 Product 생성/목록/단건/수정/삭제 서비스 테스트를 추가한다.
- [x] 관리자 상품 목록의 미분류 Category 표시 API 테스트를 추가한다.
- [x] 현재 Product UseCase 서비스를 추출한다.
- [x] Product와 Category Aggregate를 분리한다.
- [x] Product 조회 응답에 옵션 목록을 포함한다.
- [x] Product 조회 API를 `JdbcTemplate` query 객체로 분리한다.
- [x] Product/Option 조회 UseCase 구현체와 query 객체를 query 패키지로 분리한다.
- [x] 관리자 Product UseCase 구현을 검토한다.
- [x] 관리자 Product 흐름의 메서드 단위 트랜잭션 경계를 확인한다.
- [x] Wish가 Product를 참조할 때 삭제 정책을 정의한다: Product 삭제 시 Wish는 수정/삭제하지 않는다.
- [x] Order가 Product를 참조할 때 삭제 정책을 정의한다: Order는 product id 값과 상품 스냅샷을 보관한다.
- [x] 삭제/변경된 Product id를 가진 Order 목록 표시 정책을 정의한다: 주문 당시 상품 스냅샷을 표시한다.
- [ ] Wish, Option, Order, 누락 Category와 Product 동시 수정/삭제 경쟁을 검토한다.

### Option

- [x] 현재 Product Option 목록/생성/삭제 UseCase 서비스 테스트를 추가한다.
- [x] 현재 Option UseCase 서비스를 Product 패키지 아래에 추출한다.
- [x] `Option`은 별도 Aggregate root가 아니라 `Product` 소유 객체로 본다.
- [x] `OptionRepository`를 제거하고 옵션 쓰기 흐름을 Product 루트 저장으로 통일한다.
- [x] 옵션 목록 조회 API를 `OptionQueryDao` 기반 SQL 읽기 모델로 분리한다.
- [ ] Option 이름, 수량, 재고 규칙에 대한 독립 계약 테스트를 추가한다.
- [x] Order가 Option을 참조할 때 삭제 정책을 정의한다: Order는 option id 값과 옵션명 스냅샷을 보관하고, 주문된 Option도 Product Aggregate 규칙상 삭제 가능하면 삭제된다.
- [x] 삭제/변경된 Option id를 가진 Order 목록 표시 정책을 정의한다: 주문 당시 옵션 스냅샷을 표시한다.
- [x] 상품 생성 시 옵션 없음은 허용하되, 옵션 등록 후 마지막 옵션 삭제는 금지한다는 정책을 정의한다.
- [x] 동시에 옵션을 삭제해도 모든 옵션이 제거되지 않는지 서비스 테스트로 검증한다.
- [x] 주문 재고 차감과 옵션 삭제가 동시에 발생하면 Product optimistic lock failure가 발생하는지 검증한다.
- [x] Option 이름 중복은 같은 Product 안에서만 금지하고, 동시에 같은 상품에 같은 이름의 Option을 생성해도 하나만 저장되는지 서비스 테스트로 검증한다.
- [ ] 동시 재고 변경과 Order 존재 중 삭제를 추가 검토한다.

### Member

- [x] 회원가입, 로그인, 요청 DTO validation, 중복 회원가입 API 테스트를 추가한다.
- [x] 회원가입과 중복 이메일 동작 서비스 테스트를 추가한다.
- [x] Member 회원가입 UseCase 서비스를 추출한다.
- [x] 동시 중복 회원가입이 `400 Bad Request`를 반환하도록 수정한다.
- [x] Member 포인트 충전/차감과 식별성 규칙 계약 테스트를 추가한다.
- [x] 로그인 로직을 하나의 API 동작에 대응하는 UseCase 서비스로 추출한다.
- [x] Kakao callback 자동 회원가입/로그인은 `LoginWithKakaoUseCase`로 두고, Kakao 로그인 URI 생성은 UseCase가 아닌 보조 provider로 분리한다.
- [x] 관리자 포인트 충전 동작을 하나의 UseCase 서비스로 추출한다.
- [x] 관리자 회원 생성/목록/단건 조회/수정/삭제 동작을 각각 하나의 UseCase 서비스로 추출한다.
- [x] Member 관련 일반 API, 인증, 관리자, 도메인 패키지를 `gift.member` 하위로 정리하고, service/usecase는 `auth`와 `management`로 분류한다.
- [x] Wish, Order, Point, Kakao access token 기준으로 `Member` Aggregate root 경계를 확인한다.
- [x] Wish 또는 주문 이력이 있는 Member 삭제 정책을 정의한다: 물리 삭제하지 않고 `deleted=true`로 표시한다.
- [x] 삭제된 Member는 관리자 목록/단건 조회, 일반 로그인, Kakao 로그인, 토큰 인증에서 제외한다.
- [x] 이번까지의 리팩터링/정책 변경에서는 권한 체크 동작을 변경하지 않았다는 범위를 명시한다.
- [x] 일반 회원가입, 관리자 회원 생성/수정, 일반 로그인에서 비밀번호 평문 저장/비교 문제를 해결한다.
- [x] Kakao 계정은 관리자 수정에서 로컬 비밀번호를 설정하거나 변경할 수 없도록 막는다.
- [ ] 동시 포인트 충전/차감과 Member 삭제 동작을 검토한다.

### Wish

- [x] 추가/목록/삭제 UseCase를 식별한다.
- [x] 소유권과 삭제 규칙 계약 테스트를 추가한다.
- [x] 추가/목록/삭제 서비스/API 테스트를 추가한다.
- [x] 컨트롤러 로직을 하나의 API 동작당 하나의 UseCase 서비스로 추출한다.
- [x] 서비스 추출 후 메서드 단위 트랜잭션 경계를 추가한다.
- [x] `Wish`를 별도 Aggregate root로 보고 `Member`/`Product` 직접 객체 참조를 제거한다.
- [x] 목록 응답 조립은 `JdbcTemplate` 기반 query 객체에서 `wish`와 `product`를 inner join해서 처리한다.
- [x] Wish 조회 UseCase 구현체와 query 객체를 query 패키지로 분리한다.
- [x] Wish row는 있지만 Product row가 없으면 목록에 노출하지 않는다.
- [x] Product 삭제 시 Wish는 수정/삭제하지 않고 그대로 둔다.
- [x] 주문 생성 시 Wish는 수정/삭제하지 않고 그대로 둔다.
- [ ] 중복 Wish 추가와 소유권 기반 삭제 경쟁을 검토한다.

### Order

- [x] 생성 UseCase를 식별한다.
- [x] 목록 UseCase 서비스를 구현한다.
- [x] 재고, 포인트 차감, 불변 주문 이력 규칙 계약 테스트를 추가한다.
- [x] 주문 이력 스냅샷 서비스 테스트를 추가한다.
- [x] Kakao 이벤트 발행 단위 테스트와 afterCommit 비동기 전송 서비스 테스트를 추가한다.
- [x] 주문된 Option 삭제 후 Order 이력이 유지되는 서비스 테스트를 추가한다.
- [x] 주문 생성 후 Wish가 유지되는 서비스 테스트를 추가한다.
- [x] 주문 이력 API 테스트를 추가한다.
- [x] 컨트롤러의 주문 목록 로직을 `GetOrdersUseCase`로 추출한다.
- [x] Order controller, domain, service, usecase 패키지를 분리한다.
- [x] Order 조회 UseCase 구현체와 query 객체를 query 패키지로 분리한다.
- [x] `Order`를 불변 이력 Aggregate root로 보고 Product/Option/Member 직접 객체 참조를 제거한다.
- [x] Order 목록과 Kakao 메시지에 필요한 상품명, 옵션명, 단가, 이미지 URL을 주문 당시 스냅샷으로 보관한다.
- [x] 생성 서비스 추출 후 메서드 단위 트랜잭션 경계를 추가한다.
- [x] Kakao 메시지 전송을 Spring 이벤트 기반 afterCommit 비동기 listener로 이동한다.
- [x] 주문 생성 후 구매자와 주문 상품의 Wish는 유지한다.
- [x] 동시 재고 차감과 포인트 차감을 검토한다.

### 공통

- [ ] 각 객체의 계약 테스트를 먼저 세운 뒤 Option 재고와 Member 포인트 도메인 책임을 강화한다.
- [ ] 최종 확인 시 `./gradlew test`와 `./gradlew build`를 실행한다.
- [ ] AI 사용 내역과 검증 근거를 문서에 기록한다.
