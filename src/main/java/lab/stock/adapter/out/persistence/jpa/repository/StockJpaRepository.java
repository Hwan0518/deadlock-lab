package lab.stock.adapter.out.persistence.jpa.repository;

import lab.stock.adapter.out.persistence.jpa.entity.StockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockJpaRepository extends JpaRepository<StockEntity, Long> {
}
