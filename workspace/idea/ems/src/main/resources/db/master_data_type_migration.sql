-- 将班次和工艺工时修正为可计算的数据类型。
-- 使用前请先备份数据库并选择目标库；脚本可重复执行。
-- DATETIME 工时的历史值按其时分秒换算为分钟，日期部分不参与计算。

SET NAMES utf8mb4;

-- org_shift.start_time: DATETIME -> TIME
SET @source_type = (
  SELECT DATA_TYPE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'start_time'
);
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'start_time_value'
);
SET @ddl = IF(@source_type IN ('datetime', 'timestamp') AND @temp_exists = 0,
  'ALTER TABLE `org_shift` ADD COLUMN `start_time_value` TIME NULL COMMENT ''每日开始时间'' AFTER `start_time`',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'start_time_value'
);
SET @ddl = IF(@source_type IN ('datetime', 'timestamp') AND @temp_exists = 1,
  'UPDATE `org_shift` SET `start_time_value` = TIME(`start_time`) WHERE `start_time` IS NOT NULL',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(@source_type IN ('datetime', 'timestamp') AND @temp_exists = 1,
  'ALTER TABLE `org_shift` DROP COLUMN `start_time`',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @source_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'start_time'
);
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'start_time_value'
);
SET @ddl = IF(@source_exists = 0 AND @temp_exists = 1,
  'ALTER TABLE `org_shift` CHANGE COLUMN `start_time_value` `start_time` TIME NULL COMMENT ''每日开始时间''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- org_shift.end_time: DATETIME -> TIME
SET @source_type = (
  SELECT DATA_TYPE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'end_time'
);
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'end_time_value'
);
SET @ddl = IF(@source_type IN ('datetime', 'timestamp') AND @temp_exists = 0,
  'ALTER TABLE `org_shift` ADD COLUMN `end_time_value` TIME NULL COMMENT ''每日结束时间，跨日班次小于开始时间'' AFTER `end_time`',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'end_time_value'
);
SET @ddl = IF(@source_type IN ('datetime', 'timestamp') AND @temp_exists = 1,
  'UPDATE `org_shift` SET `end_time_value` = TIME(`end_time`) WHERE `end_time` IS NOT NULL',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(@source_type IN ('datetime', 'timestamp') AND @temp_exists = 1,
  'ALTER TABLE `org_shift` DROP COLUMN `end_time`',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @source_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'end_time'
);
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'end_time_value'
);
SET @ddl = IF(@source_exists = 0 AND @temp_exists = 1,
  'ALTER TABLE `org_shift` CHANGE COLUMN `end_time_value` `end_time` TIME NULL COMMENT ''每日结束时间，跨日班次小于开始时间''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- process_operation.standard_time: DATETIME -> DECIMAL minutes
SET @source_type = (
  SELECT DATA_TYPE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_operation' AND COLUMN_NAME = 'standard_time'
);
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_operation' AND COLUMN_NAME = 'standard_time_minutes'
);
SET @ddl = IF(@source_type IN ('datetime', 'timestamp', 'time') AND @temp_exists = 0,
  'ALTER TABLE `process_operation` ADD COLUMN `standard_time_minutes` DECIMAL(10,2) NULL COMMENT ''标准工时，分钟/产品计量单位'' AFTER `standard_time`',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_operation' AND COLUMN_NAME = 'standard_time_minutes'
);
SET @ddl = IF(@source_type IN ('datetime', 'timestamp', 'time') AND @temp_exists = 1,
  'UPDATE `process_operation` SET `standard_time_minutes` = ROUND(TIME_TO_SEC(TIME(`standard_time`)) / 60, 2) WHERE `standard_time` IS NOT NULL',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(@source_type IN ('datetime', 'timestamp', 'time') AND @temp_exists = 1,
  'ALTER TABLE `process_operation` DROP COLUMN `standard_time`',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @source_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_operation' AND COLUMN_NAME = 'standard_time'
);
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_operation' AND COLUMN_NAME = 'standard_time_minutes'
);
SET @ddl = IF(@source_exists = 0 AND @temp_exists = 1,
  'ALTER TABLE `process_operation` CHANGE COLUMN `standard_time_minutes` `standard_time` DECIMAL(10,2) NULL COMMENT ''标准工时，单位为分钟/产品计量单位''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- process_route_item.standard_time: DATETIME -> DECIMAL minutes
SET @source_type = (
  SELECT DATA_TYPE FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_route_item' AND COLUMN_NAME = 'standard_time'
);
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_route_item' AND COLUMN_NAME = 'standard_time_minutes'
);
SET @ddl = IF(@source_type IN ('datetime', 'timestamp', 'time') AND @temp_exists = 0,
  'ALTER TABLE `process_route_item` ADD COLUMN `standard_time_minutes` DECIMAL(10,2) NULL COMMENT ''路线标准工时，分钟/产品计量单位'' AFTER `standard_time`',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_route_item' AND COLUMN_NAME = 'standard_time_minutes'
);
SET @ddl = IF(@source_type IN ('datetime', 'timestamp', 'time') AND @temp_exists = 1,
  'UPDATE `process_route_item` SET `standard_time_minutes` = ROUND(TIME_TO_SEC(TIME(`standard_time`)) / 60, 2) WHERE `standard_time` IS NOT NULL',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl = IF(@source_type IN ('datetime', 'timestamp', 'time') AND @temp_exists = 1,
  'ALTER TABLE `process_route_item` DROP COLUMN `standard_time`',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @source_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_route_item' AND COLUMN_NAME = 'standard_time'
);
SET @temp_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_route_item' AND COLUMN_NAME = 'standard_time_minutes'
);
SET @ddl = IF(@source_exists = 0 AND @temp_exists = 1,
  'ALTER TABLE `process_route_item` CHANGE COLUMN `standard_time_minutes` `standard_time` DECIMAL(10,2) NULL COMMENT ''当前产品路线标准工时，单位为分钟/产品计量单位''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND ((TABLE_NAME = 'org_shift' AND COLUMN_NAME IN ('start_time', 'end_time'))
    OR (TABLE_NAME IN ('process_operation', 'process_route_item') AND COLUMN_NAME = 'standard_time'))
ORDER BY TABLE_NAME, ORDINAL_POSITION;
