package de.sep.business.staff.boundary;

import java.util.List;

import de.sep.business.staff.StaffSortDirection;
import de.sep.business.staff.StaffSortField;
import de.sep.business.staff.entity.Staff;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
@RolesAllowed({"ADMIN", "USER1"})
public class StaffService {

    @PersistenceContext
    private EntityManager em;

    public long countAllStaff() {
        return em.createQuery("SELECT COUNT(s) FROM Staff s", Long.class).getSingleResult();
    }

    public Staff findStaffById(Integer id) {
        return em.find(Staff.class, id);
    }

    public List<Staff> getStaffLimitedPerPage(int offset, int limit,
            StaffSortField field, StaffSortDirection direction) {
        String col = switch (field) {
            case FIRST_NAME -> "s.firstName";
            case LAST_NAME  -> "s.lastName";
            case EMAIL      -> "s.email";
            case ACTIVE     -> "s.active";
            case STORE      -> "s.storeId";
        };
        String ord = direction == StaffSortDirection.DESC ? "DESC" : "ASC";
        return em.createQuery(
                "SELECT s FROM Staff s LEFT JOIN FETCH s.manager ORDER BY " + col + " " + ord,
                Staff.class)
                .setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public List<Staff> getAllStaffAsManagers() {
        return em.createQuery(
                "SELECT s FROM Staff s WHERE s.active = 1 ORDER BY s.lastName ASC",
                Staff.class).getResultList();
    }

    // Dropdown für OrderView
    public List<Staff> getAllStaff() {
        return em.createQuery(
                "SELECT s FROM Staff s WHERE s.active = 1 ORDER BY s.lastName ASC",
                Staff.class).getResultList();
    }

    // Neuen Mitarbeiter anlegen
    public void saveStaff(Staff staff, Integer managerId) {
        if (staff.getFirstName() == null || staff.getFirstName().isBlank())
            throw new IllegalArgumentException("Vorname ist erforderlich");
        if (staff.getLastName() == null || staff.getLastName().isBlank())
            throw new IllegalArgumentException("Nachname ist erforderlich");
        if (staff.getEmail() == null || staff.getEmail().isBlank())
            throw new IllegalArgumentException("E-Mail ist erforderlich");
        if (staff.getStoreId() == null)
            throw new IllegalArgumentException("Niederlassung ist erforderlich");
        if (staff.getActive() == null) staff.setActive(1);
        if (managerId != null) staff.setManager(em.find(Staff.class, managerId));
        em.persist(staff);
    }

    /**
     * Mitarbeiter bearbeiten (Vorname, Nachname, E-Mail, Telefon, Filiale, Manager).
     * active wird separat per activate/deactivate geändert.
     */
    public void updateStaff(Integer id, Staff updated, Integer managerId) {
        Staff existing = em.find(Staff.class, id);
        if (existing == null) throw new IllegalStateException("Mitarbeiter nicht gefunden");

        if (updated.getFirstName() == null || updated.getFirstName().isBlank())
            throw new IllegalArgumentException("Vorname ist erforderlich");
        if (updated.getLastName() == null || updated.getLastName().isBlank())
            throw new IllegalArgumentException("Nachname ist erforderlich");
        if (updated.getEmail() == null || updated.getEmail().isBlank())
            throw new IllegalArgumentException("E-Mail ist erforderlich");
        if (updated.getStoreId() == null)
            throw new IllegalArgumentException("Niederlassung ist erforderlich");

        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setStoreId(updated.getStoreId());

        if (managerId != null)
            existing.setManager(em.find(Staff.class, managerId));
        else
            existing.setManager(null);

        em.merge(existing);
    }

    // Deaktiviert einen Mitarbeiter (active = 0) - Löschen nicht erlaubt
    public void deactivateStaff(Integer id) {
        Staff s = em.find(Staff.class, id);
        if (s == null) throw new IllegalStateException("Mitarbeiter nicht gefunden");
        s.setActive(0);
        em.merge(s);
    }

    // Aktiviert einen Mitarbeiter wieder
    public void activateStaff(Integer id) {
        Staff s = em.find(Staff.class, id);
        if (s == null) throw new IllegalStateException("Mitarbeiter nicht gefunden");
        s.setActive(1);
        em.merge(s);
    }
}
