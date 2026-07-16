-- 生产报工、产品批次和终检来源链迁移。
-- 使用前必须备份目标数据库；脚本只补字段和索引，可重复执行。

SET NAMES utf8mb4;
SET @schema_name = DATABASE();

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'trace_product_lot' AND column_name = 'source_report_id'
  ),
  'SELECT 1',
  'ALTER TABLE `trace_product_lot` ADD COLUMN `source_report_id` BIGINT UNSIGNED NULL COMMENT ''来源末工序报工 ID。'' AFTER `work_order_id`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'trace_product_lot' AND index_name = 'uk_trace_product_lot_source_report_id'
  ),
  'SELECT 1',
  'ALTER TABLE `trace_product_lot` ADD UNIQUE KEY `uk_trace_product_lot_source_report_id` (`source_report_id`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'qms_inspection_order' AND column_name = 'product_lot_id'
  ),
  'SELECT 1',
  'ALTER TABLE `qms_inspection_order` ADD COLUMN `product_lot_id` BIGINT UNSIGNED NULL COMMENT ''产品批次 ID，仅生产终检使用。'' AFTER `batch_id`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema = @schema_name AND table_name = 'qms_inspection_order' AND index_name = 'idx_qms_inspection_order_product_lot_id'
  ),
  'SELECT 1',
  'ALTER TABLE `qms_inspection_order` ADD KEY `idx_qms_inspection_order_product_lot_id` (`product_lot_id`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'qms_inspection_order' AND column_name = 'sample_quantity'
  ),
  'SELECT 1',
  'ALTER TABLE `qms_inspection_order` ADD COLUMN `sample_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT ''实际抽检数量。'' AFTER `inspector_id`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'qms_inspection_order' AND column_name = 'qualified_quantity'
  ),
  'SELECT 1',
  'ALTER TABLE `qms_inspection_order` ADD COLUMN `qualified_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT ''抽检合格数量。'' AFTER `sample_quantity`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'qms_inspection_order' AND column_name = 'unqualified_quantity'
  ),
  'SELECT 1',
  'ALTER TABLE `qms_inspection_order` ADD COLUMN `unqualified_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT ''抽检不合格数量。'' AFTER `qualified_quantity`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'qms_inspection_order' AND column_name = 'inspection_time'
  ),
  'SELECT 1',
  'ALTER TABLE `qms_inspection_order` ADD COLUMN `inspection_time` DATETIME NULL COMMENT ''实际检验记录提交时间。'' AFTER `unqualified_quantity`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT table_name, column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND ((table_name = 'trace_product_lot' AND column_name = 'source_report_id')
    OR (table_name = 'qms_inspection_order' AND column_name IN (
      'product_lot_id', 'sample_quantity', 'qualified_quantity',
      'unqualified_quantity', 'inspection_time'
    )))
ORDER BY table_name, ordinal_position;
