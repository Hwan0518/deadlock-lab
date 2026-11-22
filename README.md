# deadlock-lab

데드락 블로그 시리즈의 실험용 프로젝트.
문제는 `problems/`에 하나씩 추가되고, 풀이 코드와 측정 결과가 각 편의 본문이 된다.

> **원칙: 글에는 직접 실행해서 얻은 결과만 쓴다.** 측정값, 로그, 예외 메시지를 추정하거나 지어내지 않는다.

## 문제 현황

**지금 할 일:** 상태가 `진행 중`인 문제의 브랜치로 이동해 문제지를 읽고 푼다. 순서는 아래 [진행 방식](#진행-방식)을 따른다.

> 에이전트 세션을 새로 열었다면 브리핑부터 받는다. 저장소 상태를 읽어서 전체 목표, 현재 진행 상황, 사람이 할 일, AI 세션이 할 일을 알려 준다.
> - Claude Code: `/lab-status`
> - 그 밖의 에이전트(Codex 등): "AGENTS.md의 진행 상황 브리핑을 해줘"
>
> 에이전트 지침은 [`AGENTS.md`](AGENTS.md)에 있다. Claude Code는 `CLAUDE.md`에서 같은 파일을 불러온다.

| 문제 | 문제지 | 브랜치 | 상태 |
|---|---|---|---|
| 1. 역순 접근 데드락 재현 | [`problems/01-reproduce-deadlock.md`](problems/01-reproduce-deadlock.md) | `problem/01-reproduce-deadlock` | 진행 중 |

## 역할 분담

이 프로젝트는 **사람이 직접 풀고, AI 세션은 문제를 내고 검토하는** 방식으로 진행한다. 어느 PC, 어느 에이전트에서 이어받든 같다.

| | 하는 일 | 하지 않는 일 |
|---|---|---|
| **사람** | 풀이 코드와 테스트 작성, 실행과 측정, `answers/` 작성, PR 제출과 머지 | |
| **AI 세션** | 문제 출제(문제지를 `main`에 추가), PR 리뷰, 측정 결과와 답변 검증, 요청받은 실험 장치 보강 | 풀이 코드 작성(힌트는 요청받을 때만), `answers/`의 측정값·답변 채우기, 요청 없는 `lab.support` 수정 |

작성 중인 풀이가 브랜치에 있어도 AI 세션은 그 코드를 완성하지 않는다. 막힌 부분이 있으면 사람이 먼저 질문한다.

## 시리즈 구성

| 편 | 주제 | 실험 |
|---|---|---|
| 1편 | 이론 — 데드락은 왜 생기고, 어떤 선택지가 있는가 | 없음 |
| 2편 | 재현과 예방 — 역순 접근 데드락, ID 정렬, 일괄 UPDATE `ORDER BY` | 문제 1 ~ |
| 3편 | 재시도 — 그래도 남는 데드락을 견디는 설계 | 앞 편 문제가 끝나면 출제 |
| 4편 | 큐 — 고부하에서 같은 재고에 몰리는 요청을 직렬화 | 앞 편 문제가 끝나면 출제 |
| 5편 | 경험 — 실제로 겪은 동시성 문제와 해결 | 없음 (실제 자료 기반) |

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
3. **결과 기록** — `answers/NN-*.md`에 측정 표, `latestDeadlock()` 원문, 질문 답을 채운다. (틀은 문제지와 함께 `main`에 추가된다. 문제 1은 문제 브랜치에 있다)
4. **제출** — `main` 대상으로 PR을 연다. 본문에 `./gradlew test` 결과를 붙인다.
   ```bash
   gh pr create --base main --title "문제 01. 역순 접근 데드락 재현"
   ```
5. **피드백 → 수정** — AI 세션이 PR을 **문제지의 요구사항·제약 기준으로** 리뷰하고, 피드백은 PR 코멘트로 남긴다. 사람은 같은 브랜치에 수정 커밋을 추가한다.
6. **머지** — 피드백이 끝나면 사람이 머지하고, [문제 현황](#문제-현황)의 상태를 `완료`로 바꾼다. 검증된 결과가 해당 편 본문의 재료가 된다.
7. **다음 문제** — AI 세션이 새 문제지를 `main`에 추가하고, 문제 현황 표에 `진행 중`으로 한 줄 추가한다. 1번부터 반복한다.

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
