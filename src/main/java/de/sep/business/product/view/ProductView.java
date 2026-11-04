package de.sep.business.product.view;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.sep.business.product.ProductSortDirection;
import de.sep.business.product.ProductSortField;
import de.sep.business.product.boundary.ProductService;
import de.sep.business.product.entity.Brand;
import de.sep.business.product.entity.Category;
import de.sep.business.product.entity.Product;
import de.sep.business.store.boundary.StoreService;
import de.sep.business.store.entity.Store;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class ProductView implements Serializable {

    private static final long serialVersionUID = 21L;

    @Inject
    private ProductService productService;
    @Inject
    private StoreService storeService;

    private List<Product> productList;
    private long totalCount;
    private int page = 0;
    private int rowsPerPage = 50;
    private ProductSortField sortField = ProductSortField.NAME;
    private ProductSortDirection sortDirection = ProductSortDirection.ASC;

    // Anlegen
    private Product formProduct;
    private boolean creationPanelVisible = false;
    private Integer selectedBrandId;
    private Integer selectedCategoryId;

    // Lagerbestand-Einträge im Anlegen-Formular für mehrere Filialen gleichzeitig
    private List<StockEntry> stockEntries = new ArrayList<>();
    private Integer newEntryStoreId;
    private Integer newEntryQuantity = 0;

    // Löschen per f:setPropertyActionListener
    private Integer selectedIdToDelete;

    private List<Brand> brandList;
    private List<Category> categoryList;
    private List<Store> storeList;

    @PostConstruct
    public void init() {
        totalCount = productService.countAllProducts();
        brandList = productService.getAllBrands();
        categoryList = productService.getAllCategories();
        storeList = storeService.getAllStores();
        if (brandList == null)
            brandList = Collections.emptyList();
        if (categoryList == null)
            categoryList = Collections.emptyList();
        if (storeList == null)
            storeList = Collections.emptyList();
        reload();
    }

    private void reload() {
        List<Product> result = productService.getProductsLimitedPerPage(
                page * rowsPerPage, rowsPerPage, sortField, sortDirection);
        productList = result != null ? result : Collections.emptyList();
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

    public void applySort() {
        page = 0;
        reload();
    }

    // Anlegen-Panel
    public void openCreationPanel() {
        formProduct = new Product();
        selectedBrandId = null;
        selectedCategoryId = null;
        stockEntries = new ArrayList<>();
        newEntryStoreId = null;
        newEntryQuantity = 0;
        creationPanelVisible = true;
    }

    public void closeCreationPanel() {
        creationPanelVisible = false;
        formProduct = null;
        selectedBrandId = null;
        selectedCategoryId = null;
        stockEntries = new ArrayList<>();
        newEntryStoreId = null;
        newEntryQuantity = 0;
    }

    // Fügt eine Filiale zur Lagerbestand-Liste hinzu
    public void addStockEntry() {
        if (newEntryStoreId == null) {
            addError("Bitte eine Filiale auswählen");
            return;
        }
        for (StockEntry e : stockEntries) {
            if (e.getStoreId().equals(newEntryStoreId)) {
                addError("Filiale bereits in der Liste");
                return;
            }
        }
        stockEntries.add(new StockEntry(newEntryStoreId, newEntryQuantity != null ? newEntryQuantity : 0));
        newEntryStoreId = null;
        newEntryQuantity = 0;
    }

    // Entfernt einen Lagerbestand-Eintrag aus der Liste
    public void removeStockEntry(StockEntry entry) {
        stockEntries.remove(entry);
    }

    public void saveProduct() {
        try {
            productService.saveProductWithStocks(
                    formProduct, selectedBrandId, selectedCategoryId, stockEntries);
            closeCreationPanel();
            totalCount = productService.countAllProducts();
            reload();
            addInfo("Produkt wurde angelegt");
        } catch (IllegalArgumentException e) {
            addError(e.getMessage());
        } catch (Exception e) {
            addError("Produkt konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    // Löschen per f:setPropertyActionListener
    public void deleteProduct() {
        if (selectedIdToDelete == null)
            return;
        try {
            productService.deleteProduct(selectedIdToDelete);
            totalCount = productService.countAllProducts();
            if (page > 0 && page >= getTotalPages())
                page--;
            reload();
            addInfo("Produkt wurde gelöscht");
        } catch (IllegalStateException e) {
            addError(e.getMessage());
        } catch (Exception e) {
            addError("Produkt konnte nicht gelöscht werden");
        } finally {
            selectedIdToDelete = null;
        }
    }

    // Gibt den Filialnamen für eine Store-ID zurück (für Anzeige in der Liste)
    public String getStoreName(Integer storeId) {
        if (storeId == null)
            return "-";
        return storeList.stream()
                .filter(s -> s.getStoreId().equals(storeId))
                .map(Store::getStoreName)
                .findFirst().orElse("Filiale " + storeId);
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", msg));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", msg));
    }

    // Getter & Setter
    public List<Product> getProductList() {
        return productList;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int p) {
        this.page = p;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int r) {
        this.page = 0;
        this.rowsPerPage = r;
        reload();
    }

    public ProductSortField getSortField() {
        return sortField;
    }

    public void setSortField(ProductSortField f) {
        this.sortField = f != null ? f : ProductSortField.NAME;
    }

    public ProductSortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(ProductSortDirection d) {
        this.sortDirection = d != null ? d : ProductSortDirection.ASC;
    }

    public Product getFormProduct() {
        return formProduct;
    }

    public void setFormProduct(Product p) {
        this.formProduct = p;
    }

    public boolean isCreationPanelVisible() {
        return creationPanelVisible;
    }

    public void setCreationPanelVisible(boolean v) {
        this.creationPanelVisible = v;
    }

    public List<Brand> getBrandList() {
        return brandList;
    }

    public List<Category> getCategoryList() {
        return categoryList;
    }

    public List<Store> getStoreList() {
        return storeList;
    }

    public Integer getSelectedBrandId() {
        return selectedBrandId;
    }

    public void setSelectedBrandId(Integer id) {
        this.selectedBrandId = id;
    }

    public Integer getSelectedCategoryId() {
        return selectedCategoryId;
    }

    public void setSelectedCategoryId(Integer id) {
        this.selectedCategoryId = id;
    }

    public List<StockEntry> getStockEntries() {
        return stockEntries;
    }

    public Integer getNewEntryStoreId() {
        return newEntryStoreId;
    }

    public void setNewEntryStoreId(Integer id) {
        this.newEntryStoreId = id;
    }

    public Integer getNewEntryQuantity() {
        return newEntryQuantity;
    }

    public void setNewEntryQuantity(Integer q) {
        this.newEntryQuantity = q;
    }

    public Integer getSelectedIdToDelete() {
        return selectedIdToDelete;
    }

    public void setSelectedIdToDelete(Integer id) {
        this.selectedIdToDelete = id;
    }
}