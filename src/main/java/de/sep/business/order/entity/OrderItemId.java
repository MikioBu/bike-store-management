package de.sep.business.order.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderItemId implements Serializable {

    private static final long serialVersionUID = 7L;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "item_id")
    private Integer itemId;

    // Konstruktoren

    public OrderItemId() {
    }

    public OrderItemId(Integer orderId, Integer itemId) {
        this.orderId = orderId;
        this.itemId = itemId;
    }

    public Integer orderId() {
        return orderId;
    }

    public Integer itemId() {
        return itemId;
    }

    // itemId-Setter wird von OrderView benoetigt um die Positions-ID zu setzen
    public void itemId(Integer v) {
        this.itemId = v;
    }

    // Getter

    public Integer getOrderId() {
        return orderId;
    }

    public Integer getItemId() {
        return itemId;
    }

    // equals / hashCode

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OrderItemId other))
            return false;
        return Objects.equals(orderId, other.orderId)
                && Objects.equals(itemId, other.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, itemId);
    }
}
