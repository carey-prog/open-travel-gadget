-- 已有 travel_gadget 库手动升级（也可直接重启应用，会自动执行 TripSchemaMigration）
USE travel_gadget;

ALTER TABLE trip ADD COLUMN departure_city VARCHAR(50) NULL COMMENT '出发城市名' AFTER theme;
ALTER TABLE trip ADD COLUMN destination_id VARCHAR(50) NULL COMMENT '目的地ID' AFTER departure_city;
ALTER TABLE trip ADD COLUMN destination_name VARCHAR(100) NULL COMMENT '目的地名称' AFTER destination_id;
ALTER TABLE trip ADD COLUMN arrival_hub_label VARCHAR(100) NULL COMMENT '抵达枢纽说明' AFTER arrival_hub;

-- 若某列已存在会报错，可忽略该条或只执行缺失的 ALTER
