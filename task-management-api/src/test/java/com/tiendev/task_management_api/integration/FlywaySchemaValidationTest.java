package com.tiendev.task_management_api.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class FlywaySchemaValidationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrationsAppliedSuccessfully() {
        assertNotNull(flyway);
        MigrationInfo current = flyway.info().current();
        assertNotNull(current);
        assertEquals("6", current.getVersion().getVersion());

        MigrationInfo[] applied = flyway.info().applied();
        assertTrue(applied.length >= 6, "Expected at least 6 Flyway migrations applied");
        assertEquals("init schema", applied[0].getDescription());
        assertEquals("add new features", applied[1].getDescription());
        assertEquals("email verification", applied[2].getDescription());
        assertEquals("preserve existing verified users", applied[3].getDescription());
        assertEquals("allow duplicate usernames", applied[4].getDescription());
        assertEquals("mail outbox", applied[5].getDescription());
    }

    @Test
    void allNewTablesAndColumnsExistInSchema() throws Exception {
        Set<String> tableNames = new HashSet<>();
        try (var connection = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableNames.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }

            // Verify all new feature tables exist
            assertTrue(tableNames.contains("workspace_invitations"), "workspace_invitations table must exist");
            assertTrue(tableNames.contains("task_checklist_items"), "task_checklist_items table must exist");
            assertTrue(tableNames.contains("tags"), "tags table must exist");
            assertTrue(tableNames.contains("task_tags"), "task_tags table must exist");
            assertTrue(tableNames.contains("task_dependencies"), "task_dependencies table must exist");
            assertTrue(tableNames.contains("saved_filters"), "saved_filters table must exist");
            assertTrue(tableNames.contains("email_verification_codes"), "email_verification_codes table must exist");
            assertTrue(tableNames.contains("mail_outbox"), "mail_outbox table must exist");

            // Verify parent_id column exists on tasks table
            boolean hasParentId = false;
            try (ResultSet rs = metaData.getColumns(null, null, "TASKS", "PARENT_ID")) {
                if (rs.next()) {
                    hasParentId = true;
                }
            }
            if (!hasParentId) {
                try (ResultSet rs = metaData.getColumns(null, null, "tasks", "parent_id")) {
                    if (rs.next()) {
                        hasParentId = true;
                    }
                }
            }
            assertTrue(hasParentId, "tasks table must have parent_id column");
        }
    }
}
