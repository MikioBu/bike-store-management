package de.sep.business.stock.view;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import de.sep.business.product.boundary.ProductService;
import de.sep.business.stock.StockSortDirection;
import de.sep.business.stock.StockSortField;
import de.sep.business.stock.boundary.StockService;
import de.sep.business.stock.entity.Stock;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class StockView implements Serializable {

    private static final long serialVersionUID = 23L;

    @Inject
    private StockService stockService;
    @Inject
    private ProductService productService;

    private List<Stock> stockList;
    private long totalCount;
    private int page = 0;
    private int rowsPerPage = 50;

    private StockSortField sortField = StockSortField.PRODUCT;
    private StockSortDirection sortDirection = StockSortDirection.ASC;

    // Filter
    private Integer filterProductId;
    private List<Object[]> allProducts;

    // Bestand-Formular (ADMIN)
    private boolean stockFormVisible = false;
    private Integer formProductId;
    private Integer formStoreId;
    private Integer formQuantity = 0;
    private List<Object[]> allStores;

    // Initialisierung

    @PostConstruct
    public void init() {
        // Für Filter/Formular werden nur ID + Anzeigename geladen.
        // Das vermeidet das teure Laden vollständiger Product-/Store-Entities.
        allProducts = stockService.getProductOptions();
        allStores = stockService.getStoreOptions();
        if (allProducts == null)
            allProducts = Collections.emptyList();
        if (allStores == null)
            allStores = Collections.emptyList();
        totalCount = stockService.countAllStocks();
        reload();
    }

    private void reload() {
        totalCount = stockService.countStocksByProduct(filterProductId);
        List<Stock> result = stockService.getStocksLimitedPerPage(
                page * rowsPerPage, rowsPerPage, sortField, sortDirection, filterProductId);
        stockList = result != null ? result : Collections.emptyList();
    }

    // Paginierung

    public void nextPage() {
        if (hasFollowingPage()) {
            page++;
            reload();
        }
    }

    public void previousPage() {
        if (hasPrecedingPage()) {
            page--;
            reload();
        }
    }

    public boolean hasFollowingPage() {
        return (long) (page + 1) * rowsPerPage < totalCount;
    }

    public boolean hasPrecedingPage() {
        return page > 0;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) totalCount / rowsPerPage);
    }

    // Sortierung

    public void applySort() {
        page = 0;
        reload();
    }

    // Filter

    public void applyFilter() {
        page = 0;
        reload();
    }

    // Wendet Filter und Sortierung gleichzeitig an
    public void applyFilterAndSort() {
        page = 0;
        reload();
    }

    public void resetFilter() {
        filterProductId = null;
        page = 0;
        reload();
    }

    // Bestand-Formular

    public void openStockForm() {
        formProductId = null;
        formStoreId = null;
        formQuantity = 0;
        stockFormVisible = true;
    }

    public void closeStockForm() {
        stockFormVisible = false;
    }

    public void saveStock() {
        try {
            productService.updateStock(formProductId, formStoreId, formQuantity);
            stockFormVisible = false;
            reload();
            addInfo("Lagerbestand wurde aktualisiert");
        } catch (IllegalArgumentException e) {
            addError(e.getMessage());
        } catch (Exception e) {
            addError("Lagerbestand konnte nicht gespeichert werden");
        }
    }

    // Hilfsmethoden

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", msg));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", msg));
    }

    // Getter & Setter

    public List<Stock> getStockList() {
        return stockList;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int r) {
        this.page = 0;
        this.rowsPerPage = r;
        reload();
    }

    public StockSortField getSortField() {
        return sortField;
    }

    public void setSortField(StockSortField f) {
        this.sortField = f != null ? f : StockSortField.PRODUCT;
    }

    public StockSortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(StockSortDirection d) {
        this.sortDirection = d != null ? d : StockSortDirection.ASC;
    }

    public Integer getFilterProductId() {
        return filterProductId;
    }

    public void setFilterProductId(Integer id) {
        this.filterProductId = id;
    }

    public List<Object[]> getAllProducts() {
        return allProducts;
    }

    public List<Object[]> getAllStores() {
        return allStores;
    }

    public boolean isStockFormVisible() {
        return stockFormVisible;
    }

    public void setStockFormVisible(boolean v) {
        this.stockFormVisible = v;
    }

    public Integer getFormProductId() {
        return formProductId;
    }

    public void setFormProductId(Integer id) {
        this.formProductId = id;
    }

    public Integer getFormStoreId() {
        return formStoreId;
    }

    public void setFormStoreId(Integer id) {
        this.formStoreId = id;
    }

    public Integer getFormQuantity() {
        return formQuantity;
    }

    public void setFormQuantity(Integer q) {
        this.formQuantity = q;
    }
}
