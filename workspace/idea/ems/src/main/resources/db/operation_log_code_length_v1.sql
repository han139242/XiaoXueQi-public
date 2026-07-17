-- 扩展操作日志业务编码长度，可重复执行。
-- 生产补偿、质量处置和库存闭环使用稳定英文操作编码，32 位不足以完整保存。

SET @schema_name = DATABASE();
SET @ddl = IF(
  (SELECT character_maximum_length
   FROM information_schema.columns
   WHERE table_schema = @schema_name AND table_name = 'sys_operation_log'
     AND column_name = 'operation_type') >= 64,
  'SELECT 1',
  'ALTER TABLE `sys_operation_log` MODIFY COLUMN `operation_type` VARCHAR(64) NULL COMMENT ''操作类型。'''
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT column_name, column_type, character_maximum_length
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'sys_operation_log'
  AND column_name = 'operation_type';
