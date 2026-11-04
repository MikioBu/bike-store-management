package de.sep.business.product.view;

import java.io.Serializable;

/** Hilfsklasse: ein Lagerbestand-Eintrag im Anlegen-Formular */
public class StockEntry implements Serializable {
    private static final long serialVersionUID = 99L;

    private Integer storeId;
    private Integer quantity = 0;

    public StockEntry() {
    }

    public StockEntry(Integer storeId, Integer quantity) {
        this.storeId = storeId;
        this.quantity = quantity;
    }

    public Integer getStoreId() {
        return storeId;
    }

    public void setStoreId(Integer id) {
        this.storeId = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer q) {
        this.quantity = q;
    }
}