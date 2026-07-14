-- 检验单汇总数量字段迁移。
-- 使用前请先备份并选择目标数据库；脚本只补缺失字段，可重复执行。

SET NAMES utf8mb4;

SET @schema_name = DATABASE();

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

SELECT `column_name`, `column_type`, `is_nullable`, `column_default`
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'qms_inspection_order'
  AND column_name IN ('sample_quantity', 'qualified_quantity', 'unqualified_quantity', 'inspection_time')
ORDER BY `ordinal_position`;
