package de.sep.business.store.boundary;

import java.util.List;

import de.sep.business.store.StoreSortDirection;
import de.sep.business.store.StoreSortField;
import de.sep.business.store.entity.Store;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class StoreService {

    @PersistenceContext
    private EntityManager em;

    /** Wird von OrderView fuer das Filial-Dropdown benoetigt. */
    public List<Store> getAllStores() {
        return em.createQuery("SELECT s FROM Store s ORDER BY s.storeName ASC", Store.class)
                .getResultList();
    }

    public long countAllStores() {
        return em.createQuery("SELECT COUNT(s) FROM Store s", Long.class)
                .getSingleResult();
    }

    public List<Store> getStoresLimitedPerPage(int offset, int limit,
            StoreSortField field, StoreSortDirection direction) {

        String sortColumn = switch (field) {
            case NAME -> "s.storeName";
            case CITY -> "s.city";
            case STATE -> "s.state";
            case ZIP_CODE -> "s.zipCode";
        };

        String sortOrder = direction == StoreSortDirection.DESC ? "DESC" : "ASC";

        return em.createQuery(
                "SELECT s FROM Store s ORDER BY " + sortColumn + " " + sortOrder,
                Store.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
