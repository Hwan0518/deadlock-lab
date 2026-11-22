package lab.stock.adapter.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * stock 테이블 매핑. 컬럼은 schema.sql과 같다. (ddl-auto: none이라 Hibernate는 테이블을 만들지 않는다)
 */
@Entity
@Table(name = "stock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockEntity {

    @Id
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "quantity", nullable = false)
    private int quantity;
}
