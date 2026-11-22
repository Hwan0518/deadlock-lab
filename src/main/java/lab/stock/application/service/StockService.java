package lab.stock.application.service;


import lab.stock.adapter.out.persistence.jpa.sql.StockCommandSql;
import lab.stock.application.port.out.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;


@Service
@RequiredArgsConstructor
public class StockService {

	// port
	private final StockRepository stockRepository;
    // db
    private final JdbcTemplate jdbcTemplate;
    // sql
    private final StockCommandSql stockCommandSql;

	/**
	 * 재고를 차감한다
	 */
	@Transactional
	public void decreaseStock(Map<Long, Integer> decreaseStockQuantityForIds) {
		// 1. 상품별 차감량 map을 순회
		for (Map.Entry<Long, Integer> e : decreaseStockQuantityForIds.entrySet()) {
			Long id = e.getKey();
			Integer decreaseCount = e.getValue();

			// 2. 해당 상품 재고 차감
            String stockDecreaseSql = stockCommandSql.decreaseStockForIds(id, decreaseCount);
            jdbcTemplate.execute(stockDecreaseSql);
		}

	}

}
