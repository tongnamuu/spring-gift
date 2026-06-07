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

## 완료된 문제 해결

| 커밋 | 해결한 문제 | 결과 |
| --- | --- | --- |
| `1a756f6` | 삭제 요청에서 FK 오류가 `500 Internal Server Error`로 노출되는 실제 오류를 확인했다. | 삭제 실패와 FK 제약을 README에 기록했다. |
| `bb32929` | 상품이 걸린 Category 삭제 정책이 명시되지 않았다. | 처음에는 상품이 있는 Category 삭제를 거절하는 규칙을 추가했다. 이 규칙은 이후 미분류 정책으로 대체됐다. |
| `c72940d` | Category 생명주기가 Product에 과하게 묶여 있었다. | Category 삭제를 Product와 무관하게 허용하고, Category row가 없는 Product는 목록에서 `미분류 카테고리`로 표시하도록 했다. |
| `905d6b1` | 동시 중복 회원가입이 `500 Internal Server Error`로 드러날 수 있었다. | 중복 이메일 저장 실패를 `400 Bad Request`와 `Email is already registered.` 메시지로 변환했다. |

## 식별된 정책 변경

| 객체 | 이전 동작 또는 가정 | 식별한 문제 | 현재 정책 | 근거 |
| --- | --- | --- | --- | --- |
| `Category` | Product가 참조하는 Category는 DB FK 또는 명시적 존재 체크 때문에 삭제할 수 없다고 봤다. | Category 생명주기가 Product에 너무 강하게 묶였다. Product는 삭제된 Category id를 갖고도 목록에 표시될 수 있다. | Category 삭제는 Product와 독립적으로 허용한다. Product를 이동하거나 삭제하지 않고, Category row가 없으면 `미분류 카테고리`로 표시한다. | `c72940d`, `CategoryServiceTest`, `CategoryApiTest`, `AdminProductApiTest` |
| `Product` | Product가 Category 객체를 직접 갖거나 직접 의존한다고 봤다. | Product -> Category 객체 결합 때문에 Aggregate 경계가 흐려졌다. | Product는 `categoryId` 값만 저장한다. Product와 Category는 서로 다른 Aggregate root이다. | `3e01cfa`, `ProductContractTest` |
| `Member` | 중복 회원가입은 `existsByEmail` 사전 체크로 충분하다고 봤다. | 동시 요청에서는 사전 체크 이후 저장 시점에 unique 제약 위반이 발생할 수 있다. | `member.email` unique 제약을 유지하고, 저장 단계의 중복 이메일 실패도 `400 Bad Request`로 변환한다. | `905d6b1`, `MemberServiceTest`, `MemberApiTest` |

## 진행 중인 작업

| 분류 | 작업 | 현재 상태 |
| --- | --- | --- |
| 구조 해결 | 컨트롤러 로직을 하나의 API 동작당 하나의 UseCase 서비스로 계속 추출한다. | 로그인, 관리자 회원 기능, Wish, Order, 남은 API/admin 흐름을 검토해야 한다. |
| 구조 해결 | 서비스/UseCase 메서드에 트랜잭션 경계를 추가한다. | 클래스 단위 `@Transactional`은 사용하지 않고 메서드 단위로만 선언한다. |
| 문제 해결 | FK 삭제 오류, 런타임 오류, 정책 공백을 계속 수집한다. | 새로 발견한 동작 문제는 구조 변경보다 먼저 문제 해결 항목에 기록한다. |

## 객체별 작업 상태

| 객체 | 계약 단위 테스트 | 서비스/API 테스트 | 구조 상태 | 문제 상태 |
| --- | --- | --- | --- | --- |
| `Category` | 완료: `CategoryContractTest` | 완료: `CategoryServiceTest`, `CategoryApiTest` | 완료: controller, domain, service, usecase 패키지 분리 | 삭제 정책 정의 완료: Product와 무관하게 삭제하고 누락된 Category는 `미분류 카테고리`로 표시한다. |
| `Product` | 완료: `ProductContractTest` | 완료: `ProductUseCaseServiceTest`, 관리자 상품 미분류 API 테스트 | 진행 중: Product 패키지와 UseCase 서비스가 존재한다. | Wish, Option, Order가 참조할 때의 Product 삭제 정책이 더 필요하다. |
| `Option` | 부분 완료: product/option 서비스 테스트로 일부 커버하지만 독립 계약 테스트는 아직 없다. | 완료: `ProductOptionUseCaseServiceTest` | 진행 중: Option 동작은 Product usecase/service 흐름 아래에 있고 `OptionRepository`는 유지 중이다. | Order가 참조하는 Option 삭제 정책이 더 필요하다. |
| `Member` | 미완료: 포인트와 식별성 규칙 계약 테스트가 필요하다. | 완료: 회원가입/로그인 API, 회원가입 서비스 테스트 | 부분 완료: 회원가입 UseCase 서비스는 존재하고 로그인/관리자 회원 로직은 아직 컨트롤러에 남아 있다. | 중복 회원가입은 해결됐고, Wish/Order가 있을 때의 Member 삭제 정책이 더 필요하다. |
| `Wish` | 미완료 | 미완료 | 예정 | 소유권 검증과 삭제 규칙을 테스트하고 서비스로 추출해야 한다. |
| `Order` | 미완료 | 미완료 | 예정 | 재고, 포인트, Kakao 메시지 시점, Wish 정리 동작을 검증해야 한다. |

## 객체별 리팩터링 TODO

| 객체 | UseCase 식별 | 트랜잭션 경계 확인 | 루트 객체 확인 | 동시성 문제 확인 |
| --- | --- | --- | --- | --- |
| `Category` | 완료: 생성, 목록, 수정, 삭제 UseCase 존재 | 완료: Category 서비스는 메서드 단위 `@Transactional` 사용 | 완료: `Category`는 Product 없이 존재할 수 있는 Aggregate root | TODO: 중복 Category 이름, 동시 수정/삭제 확인 |
| `Product` | 현재 Product/Option 흐름은 완료, 관리자 Product UseCase 구현 검토 필요 | 부분 완료: Product 서비스는 메서드 단위 `@Transactional` 사용, 관리자 흐름 검토 필요 | 완료: `Product`는 Aggregate root이고 `categoryId`만 저장 | TODO: Wish, Option, Order, 누락 Category와 동시 삭제/수정 경쟁 확인 |
| `Option` | 부분 완료: Product 패키지 아래에서 목록/생성/삭제 UseCase 존재 | 현재 Option 서비스는 완료, 이후 수정 흐름 추가 시 경계 필요 | 완료: `Option`은 별도 root가 아니라 `Product`에 소유된다 | TODO: 동시 재고 변경, 중복 Option 생성, Order 존재 중 삭제 확인 |
| `Member` | 부분 완료: 회원가입 UseCase 존재, 로그인/관리자 생성/수정/삭제/포인트 충전 UseCase 필요 | 부분 완료: 회원가입 서비스 경계 존재, 로그인/관리자 기능 검토 필요 | TODO: Wish, Order, Point, Kakao access token을 기준으로 `Member` root 경계 확인 | 중복 회원가입은 완료, TODO: 동시 포인트 충전/차감과 Member 삭제 확인 |
| `Wish` | TODO: 추가, 목록, 삭제 UseCase 식별 | TODO: 서비스 추출 후 메서드 단위 트랜잭션 경계 추가 | TODO: `Wish`가 독립 root인지 `Member` 소유 객체인지 결정 | TODO: 중복 Wish 추가와 소유권 기반 삭제 경쟁 확인 |
| `Order` | TODO: 생성, 목록 UseCase 식별 | TODO: 서비스 추출 후 메서드 단위 트랜잭션 경계 추가 | TODO: `Order`를 불변 이력 Aggregate root로 볼지 확인 | TODO: 동시 재고 차감, 포인트 차감, Kakao 메시지 전송 시점, Wish 정리 확인 |

## 예정 작업

| 분류 | 작업 | 이유 |
| --- | --- | --- |
| 문제 해결 | Wish, Option, Order가 참조하는 Product 삭제 정책 정의 | DB FK 오류가 `500`으로 새는 것을 막아야 한다. |
| 문제 해결 | Order가 참조하는 Option 삭제 정책 정의 | 주문된 Option은 DB 계층 실패가 아니라 도메인 규칙으로 거절해야 한다. |
| 문제 해결 | Wish와 주문 이력이 있는 Member 삭제 정책 정의 | Member 삭제는 Wish/Order FK 위험을 가진다. |
| 문제 해결 | 주문 생성 후 Wish 정리 동작 검증 및 구현 | 의도는 문서화되어 있지만 실제 동작 검증이 필요하다. |
| 문제 해결 | Kakao 메시지 전송을 주문 트랜잭션 성공 이후로 이동 | 외부 부수효과는 트랜잭션 성공 전 발생하면 안 된다. |
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
- [ ] Wish, Option, Order가 Product를 참조할 때 삭제 정책을 정의한다.
- [ ] Wish, Option, Order, 누락 Category와 Product 동시 수정/삭제 경쟁을 검토한다.

### Option

- [x] 현재 Product Option 목록/생성/삭제 UseCase 서비스 테스트를 추가한다.
- [x] 현재 Option UseCase 서비스를 Product 패키지 아래에 추출한다.
- [x] `Option`은 별도 Aggregate root가 아니라 `Product` 소유 객체로 본다.
- [ ] Option 이름, 수량, 재고 규칙에 대한 독립 계약 테스트를 추가한다.
- [ ] Order가 Option을 참조할 때 삭제 정책을 정의한다.
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

- [ ] 추가/목록/삭제 UseCase를 식별한다.
- [ ] 소유권과 삭제 규칙 계약 테스트를 추가한다.
- [ ] 추가/목록/삭제 서비스/API 테스트를 추가한다.
- [ ] 컨트롤러 로직을 하나의 API 동작당 하나의 UseCase 서비스로 추출한다.
- [ ] `Wish`를 독립 Aggregate root로 볼지 `Member` 소유 객체로 볼지 결정한다.
- [ ] 서비스 추출 후 메서드 단위 트랜잭션 경계를 추가한다.
- [ ] 중복 Wish 추가와 소유권 기반 삭제 경쟁을 검토한다.

### Order

- [ ] 생성/목록 UseCase를 식별한다.
- [ ] 재고, 포인트 차감, 불변 주문 이력 규칙 계약 테스트를 추가한다.
- [ ] 주문 생성, 주문 이력, Kakao 부수효과, Wish 정리 서비스/API 테스트를 추가한다.
- [ ] 컨트롤러 로직을 하나의 API 동작당 하나의 UseCase 서비스로 추출한다.
- [ ] `Order`를 불변 이력 Aggregate root로 볼지 확인한다.
- [ ] 서비스 추출 후 메서드 단위 트랜잭션 경계를 추가한다.
- [ ] Kakao 메시지 전송을 주문 트랜잭션 성공 이후로 이동한다.
- [ ] 주문 생성 후 구매자와 주문 상품의 Wish 정리를 구현한다.
- [ ] 동시 재고 차감과 포인트 차감을 검토한다.

### 공통

- [ ] 각 객체의 계약 테스트를 먼저 세운 뒤 Option 재고와 Member 포인트 도메인 책임을 강화한다.
- [ ] 최종 확인 시 `./gradlew test`와 `./gradlew build`를 실행한다.
- [ ] AI 사용 내역과 검증 근거를 문서에 기록한다.

