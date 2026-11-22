package lab.support;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 여러 작업을 "동시에 출발"시키고 각 작업의 결과(성공 값 또는 예외)를 모아 돌려준다.
 *
 * <pre>
 * List&lt;Outcome&lt;Void&gt;&gt; outcomes = ConcurrentRunner.run(List.of(taskA, taskB), Duration.ofSeconds(10));
 * </pre>
 *
 * <ul>
 *   <li>모든 스레드가 준비될 때까지 기다린 뒤, 한 번에 출발 신호를 준다.</li>
 *   <li>출발 순서까지 보장하지는 않는다. 특정 인터리빙을 강제하려면 작업 안에서 직접 동기화해야 한다.</li>
 *   <li>결과 리스트의 순서는 입력 작업 순서와 같다.</li>
 * </ul>
 */
public final class ConcurrentRunner {

    private ConcurrentRunner() {
    }

    public static <T> List<Outcome<T>> run(List<? extends Callable<T>> tasks, Duration timeout) {
        int n = tasks.size();
        ExecutorService executor = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>(n);
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            if (!ready.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("작업 스레드가 시간 안에 준비되지 않았다");
            }
            start.countDown();

            long deadline = System.nanoTime() + timeout.toNanos();
            List<Outcome<T>> outcomes = new ArrayList<>(n);
            for (Future<T> future : futures) {
                long remaining = Math.max(0, deadline - System.nanoTime());
                outcomes.add(await(future, remaining));
            }
            return outcomes;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> Outcome<T> await(Future<T> future, long remainingNanos) throws InterruptedException {
        try {
            return Outcome.success(future.get(remainingNanos, TimeUnit.NANOSECONDS));
        } catch (ExecutionException e) {
            return Outcome.failure(e.getCause());
        } catch (TimeoutException e) {
            future.cancel(true);
            return Outcome.failure(e);
        }
    }

    /** 작업 하나의 결과. 성공이면 value, 실패면 error가 채워진다. */
    public record Outcome<T>(T value, Throwable error) {

        static <T> Outcome<T> success(T value) {
            return new Outcome<>(value, null);
        }

        static <T> Outcome<T> failure(Throwable error) {
            return new Outcome<>(null, error);
        }

        public boolean isSuccess() {
            return error == null;
        }
    }
}
