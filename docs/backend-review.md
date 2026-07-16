# Backend Review

검토일: 2026-07-16
대상 커밋: `feaf0da8894741b922d41912319a7f51b3a273df`
대상 범위: `back/` 전체 소스, 설계 문서, Flyway 스키마, 백엔드 테스트 및 CI

## 1. 요약

백엔드는 기능별 패키지 구조, 서비스 계층의 트랜잭션 경계, 도메인 상태 전이, 낙관적 버전과 워크플로 락, 커밋 후 SSE 발행 구조를 갖추고 있다.

현재 운영 투입 전 우선 해결해야 할 항목은 다음과 같다.

1. 기본 실행 프로필의 인증 비활성화
2. JPA 감사 시간 기록 미활성화
3. 진행 중 경기와 참조 데이터의 수정·삭제 보호 부족
4. 실제 MariaDB 검증 부재

## 2. 발견사항

### P0. 기본 프로필에서 인증이 비활성화됨

- `application.yml`은 `local` 프로필을 기본 활성화한다.
- `application-local.yml`은 `boxing.auth.enabled: false`로 인증 인터셉터를 비활성화한다.
- 결과적으로 기본 실행 시 관리자, 심판, 감독, 링 매니저 API가 인증 없이 접근 가능하다.
- `AuthWebConfig`는 속성이 `true`인 경우에만 등록된다.

근거:

- [`application.yml:2`](../back/src/main/resources/application.yml#L2)
- [`application-local.yml:23`](../back/src/main/resources/application-local.yml#L23)
- [`AuthWebConfig.java:9`](../back/src/main/java/com/boxing/bracket/auth/web/AuthWebConfig.java#L9)

개선 방향: 운영 및 일반 local 프로필은 인증을 강제하고, 인증 없는 개발용 프로필을 별도로 분리한다. 속성 누락 시에도 보안 기능이 비활성화되지 않도록 fail-closed 구성을 사용한다.

### P1. DB 접속 비밀번호가 저장소에 평문으로 존재함

`application-local.yml`에 `boxing` 비밀번호가 직접 기록되어 있다. 공개 저장소에 존재한 값은 이미 노출된 것으로 취급해야 한다.

근거: [`application-local.yml:5`](../back/src/main/resources/application-local.yml#L5)

개선 방향: 환경 변수 또는 외부 Secret으로 교체하고, 기존 비밀번호를 변경한다.

### P1. JPA 감사 시간이 활성화되지 않음

`BaseTimeEntity`에는 `AuditingEntityListener`가 선언되어 있지만 애플리케이션에 `@EnableJpaAuditing`이 없다. 따라서 `createdAt`, `updatedAt`이 저장되지 않거나 `NULL`로 남을 수 있다.

감사 로그 조회는 `createdAt` 정렬과 기간 필터를 사용하므로 감사 기록의 정렬과 검색 결과가 부정확해질 수 있다. Flyway 스키마도 해당 컬럼을 nullable로 생성해 이 문제가 시작 시점에 드러나지 않는다.

근거:

- [`BaseTimeEntity.java:12`](../back/src/main/java/com/boxing/bracket/common/entity/BaseTimeEntity.java#L12)
- [`AuditLogService.java:71`](../back/src/main/java/com/boxing/bracket/audit/service/AuditLogService.java#L71)
- [`V1__create_initial_schema.sql:12`](../back/src/main/resources/db/migration/V1__create_initial_schema.sql#L12)

개선 방향: JPA auditing을 명시적으로 활성화하고, 생성·수정 시간 저장 및 감사 검색을 검증하는 통합 테스트를 추가한다.

### P1. 진행 중 경기와 참조 데이터의 무결성 보호가 부족함

현재 엔티티 간 참조가 scalar ID이고 Flyway 스키마에 외래 키가 없다. 동시에 관리자 API는 다음 작업을 허용한다.

- 진행 중 경기의 링, 대회, 선수 정보를 변경
- 점수나 결과가 연결된 경기 삭제
- 경기가 존재하는 링 또는 대회 삭제

그 결과 점수·결과·배정 레코드가 고아 데이터가 되거나 링의 `currentBoutId`가 삭제된 경기를 가리킬 수 있다.

근거:

- [`AdminBoutService.java:179`](../back/src/main/java/com/boxing/bracket/bout/admin/service/AdminBoutService.java#L179)
- [`Bout.java:304`](../back/src/main/java/com/boxing/bracket/bout/domain/Bout.java#L304)
- [`AdminBoutService.java:200`](../back/src/main/java/com/boxing/bracket/bout/admin/service/AdminBoutService.java#L200)
- [`V1__create_initial_schema.sql:1`](../back/src/main/resources/db/migration/V1__create_initial_schema.sql#L1)

개선 방향: 시작 이후 경기의 일정·링·선수 변경을 차단하고, 삭제 대신 비활성화 또는 삭제 전 참조 검사를 적용한다. 외래 키 도입 여부와 삭제 정책을 설계 문서에서 확정한다.

### P1. 세션이 인메모리이며 계정 변경을 반영하지 않음

세션은 `ConcurrentHashMap`에 저장된다. 재시작이나 다중 인스턴스 환경에서는 세션이 공유되지 않는다. 또한 세션 검증 시 계정 저장소를 다시 조회하지 않으므로 계정 비활성화, 역할 변경, 삭제 후에도 기존 토큰이 만료 전까지 유지된다.

근거:

- [`AuthService.java:36`](../back/src/main/java/com/boxing/bracket/auth/service/AuthService.java#L36)
- [`AuthService.java:94`](../back/src/main/java/com/boxing/bracket/auth/service/AuthService.java#L94)
- [`AdminAccountService.java:78`](../back/src/main/java/com/boxing/bracket/user/admin/service/AdminAccountService.java#L78)

개선 방향: 공유 세션 저장소, 토큰 폐기, 계정 상태 재검증, 다중 인스턴스 이벤트 전달 정책을 도입한다.

### P1. 경기 워크플로 락 순서가 일관되지 않음

`startBout`은 경기 행을 잠근 뒤 링 행을 잠근다. 반면 `moveToNextBout`은 링 행을 먼저 잠근 뒤 다음 경기를 저장한다. 서로 다른 요청이 동시에 실행되면 교착 가능성이 있다.

근거:

- [`RingManagerService.java:63`](../back/src/main/java/com/boxing/bracket/ringmanager/service/RingManagerService.java#L63)
- [`RingManagerService.java:68`](../back/src/main/java/com/boxing/bracket/ringmanager/service/RingManagerService.java#L68)
- [`RingManagerService.java:113`](../back/src/main/java/com/boxing/bracket/ringmanager/service/RingManagerService.java#L113)

개선 방향: 관련 경기와 링의 잠금 순서를 하나로 통일하고, 시작·다음 경기 준비·상태 변경을 섞은 동시성 테스트를 추가한다.

### P1. 경기 생성과 가져오기가 재시도에 안전하지 않음

요구사항은 동일 요청 재시도 시 기존 상태를 반환하도록 정의하지만, `createBout`과 CSV/Excel 가져오기는 항상 새 경기를 저장한다. 경기 테이블에도 중복 방지 키가 없다.

근거:

- [`requirements.md:81`](../docs/requirements.md#L81)
- [`AdminBoutService.java:98`](../back/src/main/java/com/boxing/bracket/bout/admin/service/AdminBoutService.java#L98)
- [`V1__create_initial_schema.sql:51`](../back/src/main/resources/db/migration/V1__create_initial_schema.sql#L51)

개선 방향: idempotency key, 가져오기 배치 식별자, 또는 업무상 유일성 제약을 도입한다.

### P1. CI가 실제 MariaDB를 검증하지 않음

Backend CI는 `mvn test`만 실행하고 MariaDB 서비스를 시작하지 않는다. 테스트 프로필은 H2 MySQL 호환 모드다. 따라서 MariaDB DDL, Boolean/CLOB 매핑, 실제 비관적 락과 트랜잭션 격리 수준은 배포 전에 검증되지 않는다.

근거:

- [`backend-ci.yml:45`](../.github/workflows/backend-ci.yml#L45)
- [`application-test.yml:3`](../back/src/test/resources/application-test.yml#L3)
- [`application-local.yml:3`](../back/src/main/resources/application-local.yml#L3)

개선 방향: Testcontainers MariaDB 또는 GitHub Actions MariaDB 서비스를 사용해 migration, JPA validate, 워크플로 동시성 테스트를 실제 DB에서 실행한다.

### P2. 조회 성능과 확장성 위험

홈 화면은 전체 공식 경기, 링 상태, 일정, 결과를 한 번에 조회한다. 링별 경기 조회와 선수별 `findById`가 반복되고, V1 스키마에는 경기의 대회·링 조회 인덱스가 없다.

근거:

- [`HomeService.java:43`](../back/src/main/java/com/boxing/bracket/home/service/HomeService.java#L43)
- [`RingService.java:51`](../back/src/main/java/com/boxing/bracket/ring/service/RingService.java#L51)
- [`BoutService.java:53`](../back/src/main/java/com/boxing/bracket/bout/service/BoutService.java#L53)
- [`V1__create_initial_schema.sql:51`](../back/src/main/resources/db/migration/V1__create_initial_schema.sql#L51)

개선 방향: 벌크 조회와 DTO projection을 사용하고, 공개 목록에 페이지·대회·상태·링 필터를 추가하며, 실제 실행 계획을 기준으로 인덱스를 보완한다.

## 3. 테스트 결과와 공백

검토 시 `mvn -q test`를 실행했고 72개 테스트 클래스에서 실패·에러·스킵 없이 통과했다.

현재 테스트 공백:

- 기본 local 프로필에서 인증이 실제로 활성화되는지 검증하지 않음
- `createdAt`, `updatedAt` 저장 여부를 검증하지 않음
- 실제 MariaDB를 사용하지 않음
- 경기 시작과 다음 경기 준비를 섞은 동시성 시나리오가 없음
- 진행 중 경기 수정·삭제 및 참조 데이터 고아화를 검증하지 않음
- 동일 경기 생성·가져오기 재시도 중복을 검증하지 않음

## 4. 개선 우선순위

1. 인증 기본값을 fail-closed로 변경하고 DB 비밀번호 제거
2. JPA auditing 활성화 및 시간 필드 통합 테스트 추가
3. 경기 lifecycle 이후 관리자 수정·삭제 제한과 참조 무결성 정책 확정
4. MariaDB 기반 CI 및 동시성 테스트 추가
5. 생성·가져오기 멱등성 설계
6. 조회 벌크화, 페이징, 인덱스 보완

## 5. 검토 전제

- 홈, 공지, 일정, 경기 조회와 SSE가 공개 API인 것은 제품 정책으로 가정했다.
- local에서 인증을 끈 설정이 의도된 개발 편의라면 별도 개발 프로필로 분리해야 한다.
- scalar ID와 외래 키 미사용이 의도된 설계라면 삭제·수정 보호 정책이 반드시 함께 있어야 한다.
