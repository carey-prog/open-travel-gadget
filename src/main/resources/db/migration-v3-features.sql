-- v3：出发日、交通偏好、分享令牌（重启应用也会自动迁移）
USE travel_gadget;

ALTER TABLE trip ADD COLUMN departure_date DATE NULL COMMENT '计划出发日' AFTER theme;
ALTER TABLE trip ADD COLUMN transport_preference VARCHAR(30) NULL COMMENT '大交通偏好代码' AFTER departure_date;
ALTER TABLE trip ADD COLUMN transport_preference_label VARCHAR(50) NULL COMMENT '大交通偏好说明' AFTER transport_preference;
ALTER TABLE trip ADD COLUMN share_token VARCHAR(64) NULL COMMENT '分享令牌' AFTER status;
CREATE INDEX idx_trip_share_token ON trip (share_token);
