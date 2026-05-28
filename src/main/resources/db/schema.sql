-- 旅游神器数据库
-- mysql -h 127.0.0.1 -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS travel_gadget DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travel_gadget;

CREATE TABLE IF NOT EXISTS trip (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '行程ID',
    session_id          VARCHAR(64)  DEFAULT NULL COMMENT 'Agent会话ID',
    title               VARCHAR(200) NOT NULL COMMENT '行程标题',
    days                INT          NOT NULL COMMENT '天数',
    travelers           VARCHAR(50)  DEFAULT NULL COMMENT '同行人类型',
    budget_tier         VARCHAR(20)  DEFAULT NULL COMMENT '预算档：经济/舒适',
    theme               VARCHAR(50)  DEFAULT NULL COMMENT '主题偏好',
    departure_date      DATE         DEFAULT NULL COMMENT '计划出发日',
    transport_preference VARCHAR(30) DEFAULT NULL COMMENT '大交通偏好代码',
    transport_preference_label VARCHAR(50) DEFAULT NULL COMMENT '大交通偏好说明',
    departure_city      VARCHAR(50)  DEFAULT NULL COMMENT '出发城市名',
    destination_id      VARCHAR(50)  DEFAULT NULL COMMENT '目的地ID',
    destination_name    VARCHAR(100) DEFAULT NULL COMMENT '目的地名称',
    arrival_hub         VARCHAR(50)  DEFAULT NULL COMMENT '抵达枢纽代码',
    arrival_hub_label   VARCHAR(100) DEFAULT NULL COMMENT '抵达枢纽说明',
    transport_mode      VARCHAR(50)  DEFAULT NULL COMMENT '当地交通方式',
    custom_require      TEXT         DEFAULT NULL COMMENT '自定义要求',
    summary             TEXT         DEFAULT NULL COMMENT '行程摘要',
    itinerary_json      JSON         NOT NULL COMMENT '行程结构化JSON',
    budget_json         JSON         DEFAULT NULL COMMENT '预算估算JSON',
    rag_context         MEDIUMTEXT   DEFAULT NULL COMMENT 'RAG上下文',
    web_context         MEDIUMTEXT   DEFAULT NULL COMMENT '联网搜索上下文',
    status              VARCHAR(20)  DEFAULT 'completed',
    share_token         VARCHAR(64)  DEFAULT NULL COMMENT '分享令牌',
    created_at          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_destination (destination_id),
    INDEX idx_trip_share_token (share_token),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全国旅游行程';
