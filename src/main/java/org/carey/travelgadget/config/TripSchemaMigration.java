package org.carey.travelgadget.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时自动补齐 trip 表新字段（已有库无需手动执行 migration SQL）。
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class TripSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureColumn("departure_city", "VARCHAR(50) NULL COMMENT '出发城市名' AFTER theme");
            ensureColumn("destination_id", "VARCHAR(50) NULL COMMENT '目的地ID' AFTER departure_city");
            ensureColumn("destination_name", "VARCHAR(100) NULL COMMENT '目的地名称' AFTER destination_id");
            ensureColumn("arrival_hub_label", "VARCHAR(100) NULL COMMENT '抵达枢纽说明' AFTER arrival_hub");
            ensureColumn("departure_date", "DATE NULL COMMENT '计划出发日' AFTER theme");
            ensureColumn("transport_preference", "VARCHAR(30) NULL COMMENT '大交通偏好代码' AFTER departure_date");
            ensureColumn("transport_preference_label", "VARCHAR(50) NULL COMMENT '大交通偏好说明' AFTER transport_preference");
            ensureColumn("share_token", "VARCHAR(64) NULL COMMENT '分享令牌' AFTER status");
            ensureIndex("idx_trip_share_token", "share_token");
        } catch (Exception e) {
            log.warn("trip 表结构自动升级失败，请手动执行 db/migration-v2-national.sql: {}", e.getMessage());
        }
    }

    private void ensureColumn(String columnName, String definition) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trip' AND COLUMN_NAME = ?
                """,
                Integer.class,
                columnName);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE trip ADD COLUMN " + columnName + " " + definition);
        log.info("trip 表已添加列: {}", columnName);
    }

    private void ensureIndex(String indexName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trip' AND INDEX_NAME = ?
                """,
                Integer.class,
                indexName);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("CREATE INDEX " + indexName + " ON trip (" + columnName + ")");
        log.info("trip 表已添加索引: {}", indexName);
    }
}
