package lab.support;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 실험용 MySQL 8.0 컨테이너를 띄우고 Spring 컨텍스트를 연결하는 테스트 베이스.
 *
 * <ul>
 *   <li>컨테이너는 JVM당 한 번만 뜬다(모든 테스트 클래스가 공유).</li>
 *   <li>root 계정으로 접속한다. INNODB STATUS, performance_schema 조회에 PROCESS 권한이 필요하기 때문이다.</li>
 *   <li>innodb_lock_wait_timeout = 5초. 데드락이 감지되지 않고 단순 대기에 빠지면 5초 뒤 실패한다.</li>
 * </ul>
 */
@SpringBootTest
public abstract class MySqlTestSupport {

    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("lab")
            .withUsername("root")
            .withPassword("test")
            .withCommand("--innodb_lock_wait_timeout=5");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /** InnoDB 내부 상태(데드락 카운터, 최근 데드락 로그, 현재 락 목록) 조회 도우미. */
    protected InnoDbInspector innoDb() {
        return new InnoDbInspector(jdbcTemplate);
    }

    /** stock 테이블을 비우고 주어진 재고로 다시 채운다. (itemId → quantity) */
    protected void givenStocks(Map<Long, Integer> stocks) {
        jdbcTemplate.update("DELETE FROM stock");
        stocks.forEach((itemId, quantity) ->
                jdbcTemplate.update("INSERT INTO stock (item_id, quantity) VALUES (?, ?)", itemId, quantity));
    }

    /** 현재 재고 수량을 조회한다. */
    protected int stockOf(long itemId) {
        Integer quantity = jdbcTemplate.queryForObject(
                "SELECT quantity FROM stock WHERE item_id = ?", Integer.class, itemId);
        return quantity == null ? 0 : quantity;
    }
}
