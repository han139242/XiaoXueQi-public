-- 生产任务补偿来源字段迁移，可重复执行。
-- 使用前必须备份 mfg_task；本脚本只增加字段、索引并回填任务类型。

SET NAMES utf8mb4;
SET @schema_name = DATABASE();

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = @schema_name AND table_name = 'mfg_task' AND column_name = 'task_type'),
  'SELECT 1',
  'ALTER TABLE `mfg_task` ADD COLUMN `task_type` VARCHAR(32) NOT NULL DEFAULT ''NORMAL'' COMMENT ''NORMAL:正常任务，REWORK:返工任务，SUPPLEMENT:补产或续作任务。'' AFTER `defect_quantity`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = @schema_name AND table_name = 'mfg_task' AND column_name = 'source_task_id'),
  'SELECT 1',
  'ALTER TABLE `mfg_task` ADD COLUMN `source_task_id` BIGINT UNSIGNED NULL COMMENT ''补偿任务的来源任务 ID。'' AFTER `task_type`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = @schema_name AND table_name = 'mfg_task' AND column_name = 'source_ncr_id'),
  'SELECT 1',
  'ALTER TABLE `mfg_task` ADD COLUMN `source_ncr_id` BIGINT UNSIGNED NULL COMMENT ''质量补偿任务的来源不合格品处理单 ID。'' AFTER `source_task_id`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics
         WHERE table_schema = @schema_name AND table_name = 'mfg_task' AND index_name = 'idx_mfg_task_source_task_id'),
  'SELECT 1',
  'ALTER TABLE `mfg_task` ADD KEY `idx_mfg_task_source_task_id` (`source_task_id`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics
         WHERE table_schema = @schema_name AND table_name = 'mfg_task' AND index_name = 'idx_mfg_task_source_ncr_id'),
  'SELECT 1',
  'ALTER TABLE `mfg_task` ADD KEY `idx_mfg_task_source_ncr_id` (`source_ncr_id`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `mfg_task`
SET `task_type` = 'NORMAL'
WHERE `task_type` IS NULL OR `task_type` = '';

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'mfg_task'
  AND column_name IN ('task_type', 'source_task_id', 'source_ncr_id')
ORDER BY ordinal_position;

SELECT index_name, column_name
FROM information_schema.statistics
WHERE table_schema = @schema_name AND table_name = 'mfg_task'
  AND index_name IN ('idx_mfg_task_source_task_id', 'idx_mfg_task_source_ncr_id')
ORDER BY index_name, seq_in_index;
