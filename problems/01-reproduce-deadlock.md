# 문제 1. 역순 접근 데드락 재현하기

> 시리즈 2편 「재현과 예방」의 첫 실험이다.
> 목표는 **데드락을 일부러, 그리고 반드시 만들어 보는 것**이다. 고치는 건 다음 문제에서 한다.

---

## 배경

주문 A는 상품 [1, 2], 주문 B는 상품 [2, 1] 순서로 재고를 차감한다.
각 차감은 조건부 UPDATE 한 줄이고, 두 차감은 하나의 트랜잭션으로 묶여 있다.

```sql
UPDATE stock
SET quantity = quantity - ?
WHERE item_id = ?
  AND quantity >= ?;
```

이론상으로는 A가 상품 1을, B가 상품 2를 먼저 잠근 상태에서 서로의 두 번째 상품을 기다리면 데드락이 생긴다.
이번 문제에서는 그 "이론상"을 실제 InnoDB 위에서 눈으로 확인한다.

---

## 구현 범위

계층 구조와 엔티티는 `main`에 **기존 코드**로 들어 있다. 실무에서 이미 있는 코드베이스에 들어가 작업하는 상황을 가정한다. 기존 코드는 채점 대상이 아니고, 계층을 새로 만들 필요도 없다.

| 기존 코드 | 패키지 (`lab.stock.` 아래) |
|---|---|
| 출력 포트 `StockRepository` (메서드 없음) | `application.port.out` |
| 어댑터 `StockRepositoryImpl` (구현 없음) | `adapter.out.persistence.repository` |
| JPA 엔티티 `StockEntity` (`stock` 테이블 매핑) | `adapter.out.persistence.jpa.entity` |
| Spring Data 리포지토리 `StockJpaRepository` (메서드 없음) | `adapter.out.persistence.jpa.repository` |

이번 실험은 "한 트랜잭션 안에서 UPDATE가 어떤 순서로 실행되느냐"를 본다. 직접 작성하는 건 두 가지다.

1. **트랜잭션 경계가 있는 서비스 메서드 하나**

   ```java
   @Service
   public class StockService {

       @Transactional
       public void decreaseInOrder(List<Long> itemIds, int quantity) { ... }
   }
   ```

   - 모든 상품을 같은 `quantity`만큼 차감한다. (상품별 수량은 필요 없다)
   - 위치는 `lab.stock.application.service`다.
   - UPDATE는 출력 포트(`StockRepository`)를 거쳐 실행한다.
   - 테스트에서 이 빈을 주입받아 호출한다.

2. **포트 메서드와 어댑터 구현** — 어댑터 안에서 UPDATE를 실행할 수단을 둘 중 하나 고른다.
   - **JdbcTemplate**: 어댑터에서 `jdbcTemplate.update(...)`.
   - **JPA**: `StockJpaRepository`에 `@Modifying @Query` 조건부 UPDATE 메서드를 추가하고 어댑터가 호출한다.

---

## 요구사항

### 1-1. 재고 차감 서비스 (`src/main/java`)

- `itemIds`를 **주어진 순서 그대로** 돌면서 위의 조건부 UPDATE를 하나씩 실행한다.
- 전체가 **하나의 트랜잭션**이어야 한다.
- 한 건이라도 차감되지 않으면(영향받은 행 수가 1이 아니면) 예외를 던져 전체를 롤백한다.
- JdbcTemplate, JPA 중 무엇을 골랐는지와 그 이유를 한 줄로 적는다.

### 1-2. 결정적 재현 테스트 (`src/test/java`)

데드락이 "가끔" 나는 게 아니라 **매번** 나게 만든다.

- A와 B가 **각자 첫 번째 UPDATE를 끝낸 뒤**에야 두 번째 UPDATE로 넘어가도록 실행 순서를 강제한다.
- 실행 순서를 어떻게 강제할지는 직접 설계한다. (운영 코드에 테스트 전용 흔적을 얼마나 남길지도 판단 대상이다)
- 아래를 모두 검증한다.
  - 두 요청 중 **정확히 하나만 실패**하고, 나머지 하나는 성공한다.
  - 실험 전후 `deadlockCount()`가 **정확히 1** 증가한다.
  - 최종 재고가 **성공한 요청의 차감분만** 반영된 값이다.

### 1-3. 확률적 재현 측정

실행 순서를 강제하지 않고, 그냥 동시에 출발만 시켰을 때 데드락이 얼마나 자주 나는지 잰다.

- A와 B를 `ConcurrentRunner`로 동시에 출발시키는 라운드를 **최소 200회** 반복한다.
- 라운드마다 재고를 초기화한다.
- 아래를 기록해서 표로 만든다.

  | 항목 | 값 |
  |---|---|
  | 라운드 수 | |
  | 데드락 발생 라운드 수 (`deadlockCount()` 증가분 기준) | |
  | 두 요청 모두 성공한 라운드 수 | |
  | 최종 재고가 기대값과 다른 라운드 수 | |

### 1-4. 관찰 기록

결정적 재현 테스트가 끝난 직후 `latestDeadlock()` 출력을 저장하고, 아래 질문에 답한다.

1. 애플리케이션까지 올라온 예외는 무엇인가?
   - MySQL 에러 코드, SQLState, Spring이 변환한 예외 클래스(상속 계층 포함)를 적는다.
2. InnoDB 로그에서 두 트랜잭션은 각각 **무엇을 쥐고(HOLDS)** **무엇을 기다렸나(WAITING FOR)**?
   - 인덱스 이름, 락 모드, 레코드 값을 적는다.
3. `lock_mode X locks rec but not gap`는 무슨 뜻인가? 왜 gap 락이 아니었을까?
4. 희생자로 롤백된 쪽은 누구였고, 로그의 어느 줄이 그걸 말해 주는가?
5. 롤백된 트랜잭션이 **이미 성공했던 첫 번째 UPDATE**는 최종적으로 어떻게 됐나?
6. 1-3에서 데드락이 매번 나지 않은 이유를 설명한다.

---

## 제약

- **정렬 금지.** 이번 문제는 데드락을 만드는 게 목적이다.
- `SELECT ... FOR UPDATE` 금지. 조건부 UPDATE만 쓴다.
- 실험 장치(`lab.support`)는 수정하지 않는다. 필요한 게 있으면 말해 주면 추가한다.
- `innodb_lock_wait_timeout`은 5초로 설정돼 있다. 테스트가 5초쯤 멈췄다가 실패한다면, 데드락이 아니라 **단순 락 대기**에 빠진 것이다.

---

## 제출할 것

1. 작성한 코드 (서비스 + 테스트)
2. `./gradlew test` 실행 결과
3. 1-3 측정 표
4. `latestDeadlock()` 출력 원문
5. 1-4 질문에 대한 답

제출하면 코드와 답을 같이 피드백한다. 검증된 결과는 2편 본문에 그대로 들어간다.
