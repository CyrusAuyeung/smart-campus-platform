package com.unikorn.campus.catalog.support;

import com.unikorn.campus.catalog.api.ResourceItem;
import com.unikorn.campus.catalog.api.SportUnitItem;
import com.unikorn.campus.catalog.api.UserItem;
import com.unikorn.campus.catalog.domain.CatalogRepository;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCatalogRepository implements CatalogRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<UserItem> findUsers() {
        return jdbcTemplate.query(
                """
                        SELECT id, student_no, display_name, role_code, credit_score, recent_no_show_count
                        FROM app_user
                        ORDER BY student_no
                        """,
                (rs, rowNum) -> new UserItem(
                        rs.getString("id"),
                        rs.getString("student_no"),
                        rs.getString("display_name"),
                        rs.getString("role_code"),
                        rs.getInt("credit_score"),
                        rs.getInt("recent_no_show_count")));
    }

    @Override
    public List<ResourceItem> findAcademicSpaces() {
        return findResourcesByType("ACADEMIC_SPACE");
    }

    @Override
    public List<ResourceItem> findSportFacilities() {
        return findResourcesByType("SPORT_FACILITY");
    }

    @Override
    public List<SportUnitItem> findSportUnits() {
        return jdbcTemplate.query(
                """
                        SELECT id, facility_id, unit_code, name, status
                        FROM sport_unit
                        ORDER BY unit_code
                        """,
                (rs, rowNum) -> new SportUnitItem(
                        rs.getString("id"),
                        rs.getString("facility_id"),
                        rs.getString("unit_code"),
                        rs.getString("name"),
                        rs.getString("status")));
    }

    private List<ResourceItem> findResourcesByType(String resourceType) {
        return jdbcTemplate.query(
                """
                        SELECT id, resource_code, resource_type, name, campus, building, capacity, status
                        FROM resource_space
                        WHERE resource_type = ?
                        ORDER BY resource_code
                        """,
                (rs, rowNum) -> new ResourceItem(
                        rs.getString("id"),
                        rs.getString("resource_code"),
                        rs.getString("resource_type"),
                        rs.getString("name"),
                        rs.getString("campus"),
                        rs.getString("building"),
                        rs.getInt("capacity"),
                        rs.getString("status")),
                resourceType);
    }
}
