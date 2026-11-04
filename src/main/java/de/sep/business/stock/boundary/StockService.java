package de.sep.business.stock.boundary;

import java.util.List;

import de.sep.business.stock.StockSortDirection;
import de.sep.business.stock.StockSortField;
import de.sep.business.stock.entity.Stock;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class StockService {

        @PersistenceContext
        private EntityManager em;

        @RolesAllowed({ "ADMIN", "USER1", "USER2" })
        public long countAllStocks() {
                return em.createQuery("SELECT COUNT(s) FROM Stock s", Long.class)
                                .getSingleResult();
        }

        @RolesAllowed({ "ADMIN", "USER1", "USER2" })
        public long countStocksByProduct(Integer productId) {
                if (productId == null)
                        return countAllStocks();
                return em.createQuery(
                                "SELECT COUNT(s) FROM Stock s WHERE s.product.productId = :pid",
                                Long.class)
                                .setParameter("pid", productId)
                                .getSingleResult();
        }

        @RolesAllowed({ "ADMIN", "USER1", "USER2" })
        public List<Stock> getStocksLimitedPerPage(int offset, int limit,
                        StockSortField field, StockSortDirection direction,
                        Integer filterProductId) {

                String sortColumn = switch (field) {
                        case PRODUCT -> "s.product.productName";
                        case STORE -> "s.storeId";
                        case QUANTITY -> "s.quantity";
                };
                String sortOrder = direction == StockSortDirection.DESC ? "DESC" : "ASC";

                String where = (filterProductId != null)
                                ? "WHERE s.product.productId = :pid "
                                : "";

                var query = em.createQuery(
                                "SELECT s FROM Stock s " +
                                                "LEFT JOIN FETCH s.product " +
                                                "LEFT JOIN FETCH s.product.category " +
                                                where +
                                                "ORDER BY " + sortColumn + " " + sortOrder,
                                Stock.class);

                if (filterProductId != null)
                        query.setParameter("pid", filterProductId);

                return query
                                .setFirstResult(offset)
                                .setMaxResults(limit)
                                .getResultList();
        }

        @RolesAllowed({ "ADMIN", "USER1", "USER2" })
        public List<Object[]> getProductOptions() {
                return em.createQuery(
                                "SELECT DISTINCT p.productId, p.productName " +
                                                "FROM Stock s JOIN s.product p " +
                                                "ORDER BY p.productName",
                                Object[].class)
                                .getResultList();
        }

        @RolesAllowed({ "ADMIN", "USER1", "USER2" })
        public List<Object[]> getStoreOptions() {
                return em.createQuery(
                                "SELECT st.storeId, st.storeName, st.city " +
                                                "FROM Store st " +
                                                "ORDER BY st.storeName",
                                Object[].class)
                                .getResultList();
        }

}
