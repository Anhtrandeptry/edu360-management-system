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
 * Tự động migration database khi khởi động ứng dụng Fix lỗi: Unknown column
 * 'sc1_0.id' in 'field list'
 */
@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void migrateDatabase() {
        log.info("🔧 [DATABASE MIGRATION] Starting automatic database migration...");

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // Fix session_chapters table
            fixSessionChaptersTable(jdbcTemplate);

            // Fix session_lessons table
            fixSessionLessonsTable(jdbcTemplate);

            log.info("✅ [DATABASE MIGRATION] Migration completed successfully!");

        } catch (Exception e) {
            log.error("❌ [DATABASE MIGRATION] Migration failed: {}", e.getMessage(), e);
            // Không throw exception để không làm crash ứng dụng nếu migration fail
        }
    }

    private void fixSessionChaptersTable(JdbcTemplate jdbcTemplate) {
        String tableName = "session_chapters";
        log.info("📊 [MIGRATION] Checking table: {}", tableName);

        try {
            // Kiểm tra xem cột 'id' đã tồn tại chưa
            String checkColumnSql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND TABLE_NAME = ? "
                    + "AND COLUMN_NAME = 'id'";

            Integer columnExists = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName);

            if (columnExists != null && columnExists > 0) {
                log.info("✅ [MIGRATION] Table '{}' already has 'id' column. Skipping.", tableName);
                return;
            }

            log.info("⚠️  [MIGRATION] Table '{}' missing 'id' column. Starting migration...", tableName);

            // Backup dữ liệu
            String backupTableName = tableName + "_backup_auto";
            log.info("💾 [MIGRATION] Creating backup: {}", backupTableName);
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + backupTableName);
            jdbcTemplate.execute("CREATE TABLE " + backupTableName + " AS SELECT * FROM " + tableName);

            Integer backupCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + backupTableName, Integer.class);
            log.info("✅ [MIGRATION] Backed up {} rows", backupCount);

            // Drop foreign keys
            log.info("🔗 [MIGRATION] Dropping foreign keys...");
            dropForeignKeys(jdbcTemplate, tableName);

            // Drop primary key
            log.info("🔑 [MIGRATION] Dropping primary key...");
            try {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP PRIMARY KEY");
            } catch (Exception e) {
                log.warn("⚠️  [MIGRATION] No primary key to drop or already dropped");
            }

            // Thêm cột id
            log.info("➕ [MIGRATION] Adding 'id' column...");
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST");

            // Recreate foreign keys
            log.info("🔗 [MIGRATION] Recreating foreign keys...");
            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT fk_session_chapters_session "
                    + "FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE");

            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT fk_session_chapters_chapter "
                    + "FOREIGN KEY (chapter_id) REFERENCES course_chapters(id) ON DELETE CASCADE");

            // Add unique constraint
            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT uk_session_chapter UNIQUE (session_id, chapter_id)");

            log.info("✅ [MIGRATION] Table '{}' migrated successfully!", tableName);

        } catch (Exception e) {
            log.error("❌ [MIGRATION] Failed to migrate table '{}': {}", tableName, e.getMessage());
            throw new RuntimeException("Migration failed for " + tableName, e);
        }
    }

    private void fixSessionLessonsTable(JdbcTemplate jdbcTemplate) {
        String tableName = "session_lessons";
        log.info("📊 [MIGRATION] Checking table: {}", tableName);

        try {
            // Kiểm tra xem cột 'id' đã tồn tại chưa
            String checkColumnSql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND TABLE_NAME = ? "
                    + "AND COLUMN_NAME = 'id'";

            Integer columnExists = jdbcTemplate.queryForObject(checkColumnSql, Integer.class, tableName);

            if (columnExists != null && columnExists > 0) {
                log.info("✅ [MIGRATION] Table '{}' already has 'id' column. Skipping.", tableName);
                return;
            }

            log.info("⚠️  [MIGRATION] Table '{}' missing 'id' column. Starting migration...", tableName);

            // Backup dữ liệu
            String backupTableName = tableName + "_backup_auto";
            log.info("💾 [MIGRATION] Creating backup: {}", backupTableName);
            jdbcTemplate.execute("DROP TABLE IF EXISTS " + backupTableName);
            jdbcTemplate.execute("CREATE TABLE " + backupTableName + " AS SELECT * FROM " + tableName);

            Integer backupCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + backupTableName, Integer.class);
            log.info("✅ [MIGRATION] Backed up {} rows", backupCount);

            // Drop foreign keys
            log.info("🔗 [MIGRATION] Dropping foreign keys...");
            dropForeignKeys(jdbcTemplate, tableName);

            // Drop primary key
            log.info("🔑 [MIGRATION] Dropping primary key...");
            try {
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP PRIMARY KEY");
            } catch (Exception e) {
                log.warn("⚠️  [MIGRATION] No primary key to drop or already dropped");
            }

            // Thêm cột id
            log.info("➕ [MIGRATION] Adding 'id' column...");
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST");

            // Recreate foreign keys
            log.info("🔗 [MIGRATION] Recreating foreign keys...");
            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT fk_session_lessons_session "
                    + "FOREIGN KEY (session_id) REFERENCES class_sessions(id) ON DELETE CASCADE");

            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT fk_session_lessons_lesson "
                    + "FOREIGN KEY (lesson_id) REFERENCES course_lessons(id) ON DELETE CASCADE");

            // Add unique constraint
            jdbcTemplate.execute("ALTER TABLE " + tableName + " "
                    + "ADD CONSTRAINT uk_session_lesson UNIQUE (session_id, lesson_id)");

            log.info("✅ [MIGRATION] Table '{}' migrated successfully!", tableName);

        } catch (Exception e) {
            log.error("❌ [MIGRATION] Failed to migrate table '{}': {}", tableName, e.getMessage());
            throw new RuntimeException("Migration failed for " + tableName, e);
        }
    }

    private void dropForeignKeys(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            // Lấy danh sách foreign keys
            String getFkSql = "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND TABLE_NAME = ? "
                    + "AND REFERENCED_TABLE_NAME IS NOT NULL";

            List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(getFkSql, tableName);

            for (Map<String, Object> fk : foreignKeys) {
                String constraintName = (String) fk.get("CONSTRAINT_NAME");
                log.info("🔗 [MIGRATION] Dropping foreign key: {}", constraintName);
                jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP FOREIGN KEY " + constraintName);
            }

            if (foreignKeys.isEmpty()) {
                log.info("ℹ️  [MIGRATION] No foreign keys to drop");
            }

        } catch (Exception e) {
            log.warn("⚠️  [MIGRATION] Error dropping foreign keys: {}", e.getMessage());
        }
    }
}
