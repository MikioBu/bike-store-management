package de.sep.business.stock.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class StockId implements Serializable {

    private static final long serialVersionUID = 5L;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "store_id")
    private Integer storeId;

    // Konstruktoren

    public StockId() {
    }

    public StockId(Integer productId, Integer storeId) {
        this.productId = productId;
        this.storeId = storeId;
    }

    // Getter Setter

    public Integer productId() {
        return productId;
    }

    public void productId(Integer v) {
        this.productId = v;
    }

    public Integer storeId() {
        return storeId;
    }

    public void storeId(Integer v) {
        this.storeId = v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof StockId other))
            return false;
        return Objects.equals(productId, other.productId)
                && Objects.equals(storeId, other.storeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, storeId);
    }
}