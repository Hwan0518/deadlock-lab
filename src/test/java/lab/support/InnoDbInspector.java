package lab.support;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * InnoDB 내부 상태를 조회하는 도우미.
 */
public class InnoDbInspector {

    private static final String DEADLOCK_SECTION = "LATEST DETECTED DEADLOCK";
    private static final String NEXT_SECTION = "TRANSACTIONS\n------------";

    private final JdbcTemplate jdbcTemplate;

    public InnoDbInspector(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 서버 기동 이후 InnoDB가 감지한 데드락 누적 횟수.
     * 실험 전후 값을 비교해서 "이번 실험에서 몇 번 발생했는지"를 계산한다.
     */
    public long deadlockCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT `COUNT` FROM information_schema.INNODB_METRICS WHERE NAME = 'lock_deadlocks'",
                Long.class);
        return count == null ? 0 : count;
    }

    /**
     * SHOW ENGINE INNODB STATUS 의 LATEST DETECTED DEADLOCK 구간.
     * 데드락이 한 번도 없었다면 빈 문자열을 돌려준다.
     */
    public String latestDeadlock() {
        String status = jdbcTemplate.queryForObject(
                "SHOW ENGINE INNODB STATUS", (rs, rowNum) -> rs.getString("Status"));
        if (status == null) {
            return "";
        }
        int start = status.indexOf(DEADLOCK_SECTION);
        if (start < 0) {
            return "";
        }
        int end = status.indexOf(NEXT_SECTION, start);
        return (end < 0 ? status.substring(start) : status.substring(start, end)).strip();
    }

    /**
     * 지금 이 순간 잡혀 있거나 대기 중인 락 목록 (performance_schema.data_locks).
     * 트랜잭션이 열려 있는 동안 다른 커넥션에서 호출해야 의미가 있다.
     */
    public List<Map<String, Object>> currentLocks() {
        return jdbcTemplate.queryForList("""
                SELECT ENGINE_TRANSACTION_ID AS trx_id,
                       OBJECT_NAME           AS table_name,
                       INDEX_NAME            AS index_name,
                       LOCK_TYPE             AS lock_type,
                       LOCK_MODE             AS lock_mode,
                       LOCK_STATUS           AS lock_status,
                       LOCK_DATA             AS lock_data
                FROM performance_schema.data_locks
                ORDER BY ENGINE_TRANSACTION_ID, OBJECT_NAME, INDEX_NAME
                """);
    }
}
