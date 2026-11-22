package lab.stock.adapter.out.persistence.jpa.sql;

import org.springframework.stereotype.Component;


@Component
public class StockCommandSql {


    public String decreaseStockForIds(Long id, Integer decreaseCount) {
        return String.format("UPDATE stock" +
                        "SET quantity = quantity - %d" +
                        "WHERE item_id = %d" +
                        "AND quantity >= 1"
                ,decreaseCount
                ,id
        );
    }


}
