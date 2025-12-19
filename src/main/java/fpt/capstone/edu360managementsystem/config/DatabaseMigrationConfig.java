package fpt.capstone.edu360managementsystem.config;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.PostConstruct;

/**
 * Database migration configuration for automatic schema updates.
 * Handles automatic migration of database tables on application startup,
 * including adding missing columns and recreating constraints.
 *
 * @author 360edu
 * @version 1.0
 */
@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Autowired
    private DataSource dataSource;

    /**
     * Executes database migration on application startup.
     * Migrates session_chapters and session_lessons tables to add missing 'id' columns.
     */
    @PostConstruct
    public void migrateDatabase() {
        log.info("[DATABASE MIGRATION] Starting automatic database migration...");

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            fixSessionChaptersTable(jdbcTemplate);
            fixSessionLessonsTable(jdbcTemplate);

            log.info("[DATABASE MIGRATION] Migration completed successfully!");

        } catch (Exception e) {
            log.error("[DATABASE MIGRATION] Migration failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Migrates the session_chapters table to add auto-increment primary key.
     * Creates backup, drops existing constraints, adds 'id' column,
     * and recreates foreign keys with unique constraint.
     *
     * @param jdbcTemplate the JDBC template for database operations
     */
    private void fixSessionChaptersTable(JdbcTemplate jdbcTemplate) {
        String tableName = "session_chapters";
        log.info("[MIGRATION] Checking table: {}", tableName);

        try {
            String checkColumnSql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND TABLE_NAME = ? "
                    + "AND COLUMN_NAME = 'id'";

            Integer columnExists = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName);

            if (columnExists != null && columnExists > 0) {
                log.info("[MIGRATION] Table '{}' already has 'id' column. Skipping.", tableName);
                return;
            }

            log.info("[MIGRATION] Table '{}' missing 'id' column. Starting migration...", tableName);

            String backupTableName = tableName + "_backup_auto";
            log.info("[MIGRATION] Creating backup: {}", backupTableName);
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + backupTableName);
            jdbcTemplate.execute("CREATE TABLE " + backupTableName + " AS SELECT * FROM " + tableName);

            Integer backupCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + backupTableName, Integer.class);
            log.info("[MIGRATION] Backed up {} rows", backupCount);

            log.info("[MIGRATION] Dropping foreign keys...");
            dropForeignKeys(jdbcTemplate, tableName);

            log.info("[MIGRATION] Dropping primary key...");
            try {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP PRIMARY KEY");
            } catch (Exception e) {
                log.warn("[MIGRATION] No primary key to drop or already dropped");
            }

            log.info("[MIGRATION] Adding 'id' column...");
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST");

            log.info("[MIGRATION] Recreating foreign keys...");
            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT fk_session_chapters_session "
                    + "FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE");

            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT fk_session_chapters_chapter "
                    + "FOREIGN KEY (chapter_id) REFERENCES course_chapters(id) ON DELETE CASCADE");

            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT uk_session_chapter UNIQUE (session_id, chapter_id)");

            log.info("[MIGRATION] Table '{}' migrated successfully!", tableName);

        } catch (Exception e) {
            log.error("[MIGRATION] Failed to migrate table '{}': {}", tableName, e.getMessage());
            throw new RuntimeException("Migration failed for " + tableName, e);
        }
    }

    /**
     * Migrates the session_lessons table to add auto-increment primary key.
     * Creates backup, drops existing constraints, adds 'id' column,
     * and recreates foreign keys with unique constraint.
     *
     * @param jdbcTemplate the JDBC template for database operations
     */
    private void fixSessionLessonsTable(JdbcTemplate jdbcTemplate) {
        String tableName = "session_lessons";
        log.info("[MIGRATION] Checking table: {}", tableName);

        try {
            String checkColumnSql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND TABLE_NAME = ? "
                    + "AND COLUMN_NAME = 'id'";

            Integer columnExists = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName);

            if (columnExists != null && columnExists > 0) {
                log.info("[MIGRATION] Table '{}' already has 'id' column. Skipping.", tableName);
                return;
            }

            log.info("[MIGRATION] Table '{}' missing 'id' column. Starting migration...", tableName);

            String backupTableName = tableName + "_backup_auto";
            log.info("[MIGRATION] Creating backup: {}", backupTableName);
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + backupTableName);
            jdbcTemplate.execute("CREATE TABLE " + backupTableName + " AS SELECT * FROM " + tableName);

            Integer backupCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + backupTableName, Integer.class);
            log.info("[MIGRATION] Backed up {} rows", backupCount);

            log.info("[MIGRATION] Dropping foreign keys...");
            dropForeignKeys(jdbcTemplate, tableName);

            log.info("[MIGRATION] Dropping primary key...");
            try {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP PRIMARY KEY");
            } catch (Exception e) {
                log.warn("[MIGRATION] No primary key to drop or already dropped");
            }

            log.info("[MIGRATION] Adding 'id' column...");
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST");

            log.info("[MIGRATION] Recreating foreign keys...");
            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT fk_session_lessons_session "
                    + "FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE");

            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT fk_session_lessons_lesson "
                    + "FOREIGN KEY (lesson_id) REFERENCES course_lessons(id) ON DELETE CASCADE");

            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT uk_session_lesson UNIQUE (session_id, lesson_id)");

            log.info("[MIGRATION] Table '{}' migrated successfully!", tableName);

        } catch (Exception e) {
            log.error("[MIGRATION] Failed to migrate table '{}': {}", tableName, e.getMessage());
            throw new RuntimeException("Migration failed for " + tableName, e);
        }
    }

    /**
     * Drops all foreign keys from the specified table.
     *
     * @param jdbcTemplate the JDBC template for database operations
     * @param tableName    the name of the table to drop foreign keys from
     */
    private void dropForeignKeys(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            String getFkSql = "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND TABLE_NAME = ? "
                    + "AND REFERENCED_TABLE_NAME IS NOT NULL";

            List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(getFkSql, tableName);

            for (Map<String, Object> fk : foreignKeys) {
                String constraintName = (String) fk.get("CONSTRAINT_NAME");
                log.info("[MIGRATION] Dropping foreign key: {}", constraintName);
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
            }

            if (foreignKeys.isEmpty()) {
                log.info("[MIGRATION] No foreign keys to drop");
            }

        } catch (Exception e) {
            log.warn("[MIGRATION] Error dropping foreign keys: {}", e.getMessage());
        }
    }
}
