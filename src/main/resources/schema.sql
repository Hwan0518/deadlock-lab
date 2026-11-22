DROP TABLE IF EXISTS stock;

CREATE TABLE stock (
    item_id  BIGINT NOT NULL PRIMARY KEY,
    quantity INT    NOT NULL
) ENGINE = InnoDB;
