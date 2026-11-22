package lab.application;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;


@Service
public class StockService {

	// DB
	private static JdbcTemplate jdbcTemplate;

	// sql
	private static String STOCK_DECREASE = "UPDATE STOCK SET quantity = quantity-"

	// Service

	/**
	 * 재고를 차감한다
	 */
	@Transactional
	public void decreaseStock(Map<Long, Integer> idsForDecreaseQuantity) {
		// 1. 상품별 차감량 map을 순회
		for (Map.Entry<Long, Integer> e : idsForDecreaseQuantity.entrySet()) {
			Long id = e.getKey();
			Integer quantity = e.getValue();

			// 2. 해당 상품 재고 차감
			jdbcTemplate.execute();
		}

	}

}
