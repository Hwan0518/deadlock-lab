# deadlock-lab

데드락 블로그 시리즈의 실험용 프로젝트.
문제는 `problems/`에 하나씩 추가되고, 풀이 코드와 측정 결과가 각 편의 본문이 된다.

## 시리즈 구성

| 편 | 주제 | 실험 |
|---|---|---|
| 1편 | 이론 — 데드락은 왜 생기고, 어떤 선택지가 있는가 | 없음 |
| 2편 | 재현과 예방 — 역순 접근 데드락, ID 정렬, 일괄 UPDATE `ORDER BY` | 문제 1 ~ |
| 3편 | 재시도 — 그래도 남는 데드락을 견디는 설계 | |
| 4편 | 큐 — 고부하에서 같은 재고에 몰리는 요청을 직렬화 | |
| 5편 | 경험 — 실제로 겪은 동시성 문제와 해결 | |

## 진행 방식

문제 하나 = 브랜치 하나 = PR 하나. `main`에는 실험 장치와 문제지만 두고, **항상 스모크 테스트가 통과하는 상태**로 유지한다.

```
main ──●─────────────●────────────●──── (문제지 추가)   (풀이 머지)
        \                        /
         problem/01-... ─●──●──●   (1-1 → 1-2 → 1-3 → 1-4)
```

1. **브랜치 생성** — 최신 `main`에서 문제 번호로 딴다.
   ```bash
   git switch main && git pull
   git switch -c problem/01-reproduce-deadlock
   ```
2. **단계별 커밋** — 문제지의 하위 단계(1-1, 1-2, …)마다 커밋한다. 중간 상태도 push해 두면 다른 PC에서 이어갈 수 있다.
   ```bash
   git commit -m "feat(01): 1-1 재고 차감 서비스"
   git push -u origin problem/01-reproduce-deadlock
   ```
   다른 PC에서 이어갈 때: `git fetch && git switch problem/01-reproduce-deadlock && git pull`
3. **결과 기록** — `answers/NN-*.md`에 측정 표, `latestDeadlock()` 원문, 질문 답을 채운다. (문제 브랜치에 틀이 들어 있다)
4. **제출** — `main` 대상으로 PR을 연다. 본문에 `./gradlew test` 결과를 붙인다.
   ```bash
   gh pr create --base main --title "문제 01. 역순 접근 데드락 재현"
   ```
5. **피드백 → 수정** — PR에서 피드백을 받고 같은 브랜치에 커밋을 추가한다.
6. **머지** — 피드백이 끝나면 머지한다. 검증된 결과가 해당 편 본문의 재료가 된다.
7. **다음 문제** — 새 문제지는 `main`에 추가된다. 1번부터 반복한다.

| 브랜치 | 문제 |
|---|---|
| `problem/01-reproduce-deadlock` | 문제 1. 역순 접근 데드락 재현 |

## 다른 PC에서 시작하기

필요한 것은 **Git, Docker, 아무 버전의 JDK(Gradle 실행용)** 뿐이다.

```bash
git clone https://github.com/Hwan0518/deadlock-lab.git
cd deadlock-lab
# Docker Desktop(또는 Docker 데몬)을 먼저 켠다
./gradlew test --tests 'lab.support.HarnessSmokeTest'   # Windows: gradlew.bat
```

- JDK 21이 없으면 foojay 플러그인이 첫 빌드 때 자동으로 내려받는다.
- 첫 테스트 때 `mysql:8.0` 이미지(약 600MB)를 받는다. Docker 디스크 여유를 확인한다.
- `Could not find a valid Docker environment`가 나면 Docker가 꺼져 있는 것이다.

## 환경

- Java 21 (Gradle toolchain), Spring Boot 4.0.6, Gradle Wrapper 9.4.1
- MySQL 8.0 — Testcontainers 2.0.3가 테스트 시작 시 컨테이너를 띄운다. **Docker 실행 필요.**
- `innodb_lock_wait_timeout = 5` (단순 락 대기는 5초 뒤 실패)

```bash
./gradlew test                                  # 전체
./gradlew test --tests 'lab.support.HarnessSmokeTest'   # 실험 장치 동작 확인
```

## 실험 장치 (`src/test/java/lab/support`)

| 클래스 | 역할 |
|---|---|
| `MySqlTestSupport` | 테스트 베이스. 컨테이너 기동, 데이터소스 연결, `givenStocks(...)`, `stockOf(...)` |
| `InnoDbInspector` | `deadlockCount()` 데드락 누적 횟수, `latestDeadlock()` 최근 데드락 로그, `currentLocks()` 현재 락 목록 |
| `ConcurrentRunner` | 여러 작업을 동시에 출발시키고 결과(값 또는 예외)를 입력 순서대로 모은다 |

스키마는 `src/main/resources/schema.sql` — 지금은 `stock(item_id PK, quantity)` 하나다.
