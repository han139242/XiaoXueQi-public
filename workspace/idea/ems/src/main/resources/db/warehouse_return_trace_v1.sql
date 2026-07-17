-- 生产退料来源与库位追踪迁移，可重复执行。
-- 执行前必须备份 wms_return_detail；脚本只增加字段、索引并回填可唯一匹配的历史退料。

SET NAMES utf8mb4;
SET @schema_name = DATABASE();

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = @schema_name AND table_name = 'wms_return_detail'
           AND column_name = 'outbound_detail_id'),
  'SELECT 1',
  'ALTER TABLE `wms_return_detail` ADD COLUMN `outbound_detail_id` BIGINT UNSIGNED NULL COMMENT ''来源生产领料明细 ID。'' AFTER `return_id`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = @schema_name AND table_name = 'wms_return_detail'
           AND column_name = 'location_id'),
  'SELECT 1',
  'ALTER TABLE `wms_return_detail` ADD COLUMN `location_id` BIGINT UNSIGNED NULL COMMENT ''退回库位 ID，必须与来源领料明细一致。'' AFTER `batch_id`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `wms_return_detail` detail
JOIN `wms_return_order` return_order
  ON return_order.`return_id` = detail.`return_id` AND return_order.`deleted_flag` = 0
JOIN (
  SELECT outbound_order.`work_order_id`, outbound_detail.`material_id`, outbound_detail.`batch_id`,
         MIN(outbound_detail.`detail_id`) AS `outbound_detail_id`, COUNT(*) AS `source_count`
  FROM `wms_outbound_order` outbound_order
  JOIN `wms_outbound_detail` outbound_detail
    ON outbound_detail.`outbound_id` = outbound_order.`outbound_id`
   AND outbound_detail.`deleted_flag` = 0
  WHERE outbound_order.`deleted_flag` = 0
    AND outbound_order.`outbound_type` = 'MATERIAL_ISSUE'
    AND outbound_order.`audit_status` = 'APPROVED'
  GROUP BY outbound_order.`work_order_id`, outbound_detail.`material_id`, outbound_detail.`batch_id`
) source
  ON source.`work_order_id` = return_order.`work_order_id`
 AND source.`material_id` = detail.`material_id`
 AND source.`batch_id` = detail.`batch_id`
 AND source.`source_count` = 1
JOIN `wms_outbound_detail` outbound_detail
  ON outbound_detail.`detail_id` = source.`outbound_detail_id`
SET detail.`outbound_detail_id` = source.`outbound_detail_id`,
    detail.`location_id` = outbound_detail.`location_id`
WHERE detail.`deleted_flag` = 0
  AND (detail.`outbound_detail_id` IS NULL OR detail.`location_id` IS NULL);

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics
         WHERE table_schema = @schema_name AND table_name = 'wms_return_detail'
           AND index_name = 'idx_wms_return_detail_outbound_detail_id'),
  'SELECT 1',
  'ALTER TABLE `wms_return_detail` ADD KEY `idx_wms_return_detail_outbound_detail_id` (`outbound_detail_id`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics
         WHERE table_schema = @schema_name AND table_name = 'wms_return_detail'
           AND index_name = 'idx_wms_return_detail_location_id'),
  'SELECT 1',
  'ALTER TABLE `wms_return_detail` ADD KEY `idx_wms_return_detail_location_id` (`location_id`)'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) AS `active_return_details`,
       SUM(`outbound_detail_id` IS NULL) AS `missing_source_detail`,
       SUM(`location_id` IS NULL) AS `missing_location`
FROM `wms_return_detail`
WHERE `deleted_flag` = 0;

SELECT `column_name`, `column_type`, `is_nullable`
FROM information_schema.columns
WHERE table_schema = @schema_name AND table_name = 'wms_return_detail'
  AND column_name IN ('outbound_detail_id', 'location_id')
ORDER BY ordinal_position;
