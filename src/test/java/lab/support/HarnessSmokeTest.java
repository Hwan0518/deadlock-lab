package lab.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

/**
 * 실험 장치 자체가 동작하는지만 확인한다. (문제 풀이와 무관)
 */
class HarnessSmokeTest extends MySqlTestSupport {

    @Test
    void 컨테이너와_스키마가_준비된다() {
        givenStocks(Map.of(1L, 10, 2L, 20));

        assertThat(stockOf(1L)).isEqualTo(10);
        assertThat(stockOf(2L)).isEqualTo(20);
    }

    @Test
    void InnoDB_상태를_조회할_수_있다() {
        InnoDbInspector innoDb = innoDb();

        assertThat(innoDb.deadlockCount()).isGreaterThanOrEqualTo(0);
        assertThat(innoDb.latestDeadlock()).isNotNull();
        assertThat(innoDb.currentLocks()).isNotNull();
    }

    @Test
    void 동시_실행_결과를_입력_순서대로_모은다() {
        Callable<String> ok = () -> "ok";
        Callable<String> fail = () -> {
            throw new IllegalStateException("boom");
        };

        List<ConcurrentRunner.Outcome<String>> outcomes =
                ConcurrentRunner.run(List.of(ok, fail), Duration.ofSeconds(5));

        assertThat(outcomes.get(0).isSuccess()).isTrue();
        assertThat(outcomes.get(0).value()).isEqualTo("ok");
        assertThat(outcomes.get(1).isSuccess()).isFalse();
        assertThat(outcomes.get(1).error()).hasMessage("boom");
    }
}
