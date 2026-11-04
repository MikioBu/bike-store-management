package de.sep.business.store.view;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import de.sep.business.store.StoreSortDirection;
import de.sep.business.store.StoreSortField;
import de.sep.business.store.boundary.StoreService;
import de.sep.business.store.entity.Store;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@ViewScoped
public class StoreView implements Serializable {

    private static final long serialVersionUID = 31L;

    @Inject
    private StoreService storeService;

    private List<Store> storeList;
    private long totalCount;
    private int page = 0;
    private int rowsPerPage = 50;

    private StoreSortField sortField = StoreSortField.NAME;
    private StoreSortDirection sortDirection = StoreSortDirection.ASC;

    // Initialisierung

    @PostConstruct
    public void init() {
        totalCount = storeService.countAllStores();
        reload();
    }

    private void reload() {
        List<Store> result = storeService.getStoresLimitedPerPage(
                page * rowsPerPage, rowsPerPage, sortField, sortDirection);
        storeList = result != null ? result : Collections.emptyList();
    }

    // Seitenaufteilung

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

    // Getter & Setter

    public List<Store> getStoreList() {
        return storeList;
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

    public void setRowsPerPage(int rowsPerPage) {
        this.page = 0;
        this.rowsPerPage = rowsPerPage;
        reload();
    }

    public StoreSortField getSortField() {
        return sortField;
    }

    public void setSortField(StoreSortField f) {
        this.sortField = f != null ? f : StoreSortField.NAME;
    }

    public StoreSortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(StoreSortDirection d) {
        this.sortDirection = d != null ? d : StoreSortDirection.ASC;
    }
}
