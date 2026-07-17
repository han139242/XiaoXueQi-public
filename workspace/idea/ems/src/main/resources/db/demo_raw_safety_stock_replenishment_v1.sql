-- Replenish the missing C500 packaging materials through a confirmed purchase inbound.
-- The script is idempotent and never updates an existing inventory balance directly.

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @replenishment_marker = 'DEMO_RAW_SAFETY_STOCK_20260717_V1';
SET @replenishment_operator_id = 1;
SET @replenishment_time = NOW();
SET @replenishment_inbound_no = 'IB-DEMO-RAW-SAFETY-20260717-01';

DROP TEMPORARY TABLE IF EXISTS `tmp_demo_raw_safety_replenishment`;
CREATE TEMPORARY TABLE `tmp_demo_raw_safety_replenishment` (
  `material_code` VARCHAR(64) NOT NULL PRIMARY KEY,
  `inbound_quantity` DECIMAL(18,4) NOT NULL,
  `batch_no` VARCHAR(64) NOT NULL UNIQUE,
  `supplier_batch_no` VARCHAR(64) NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_demo_raw_safety_replenishment`
  (`material_code`, `inbound_quantity`, `batch_no`, `supplier_batch_no`)
VALUES
  ('PKG-CARTON-XY-25', 250.0000, 'RM-DEMO-C500-CTN-20260717-01', 'SUP-C500-CTN-260717-01'),
  ('PKG-LABEL-XY-C500-77', 5000.0000, 'RM-DEMO-C500-LBL-20260717-01', 'SUP-C500-LBL-260717-01'),
  ('PKG-TUBE-XY-C500', 5000.0000, 'RM-DEMO-C500-TUBE-20260717-01', 'SUP-C500-TUBE-260717-01');

SELECT `warehouse_id` INTO @raw_warehouse_id
FROM `wms_warehouse`
WHERE `warehouse_code` = 'WH-RAW-01' AND `deleted_flag` = 0
LIMIT 1;

SELECT `location_id` INTO @raw_location_id
FROM `wms_location`
WHERE `location_code` = 'LOC-RAW-A01' AND `deleted_flag` = 0
LIMIT 1;

SELECT `supplier_id` INTO @pack_supplier_id
FROM `mdm_supplier`
WHERE `supplier_code` = 'SUP-PACK-001' AND `deleted_flag` = 0
LIMIT 1;

START TRANSACTION;

INSERT INTO `wms_inbound_order`
  (`inbound_no`, `inbound_type`, `warehouse_id`, `source_order_type`, `audit_status`, `inbound_time`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT @replenishment_inbound_no, 'PURCHASE', @raw_warehouse_id, 'PURCHASE_RECEIPT', 'APPROVED',
       @replenishment_time,
       CONCAT(@replenishment_marker, ': C500 packaging safety stock replenishment'),
       @replenishment_operator_id, @replenishment_time,
       @replenishment_operator_id, @replenishment_time
FROM DUAL
WHERE @raw_warehouse_id IS NOT NULL
  AND @raw_location_id IS NOT NULL
  AND @pack_supplier_id IS NOT NULL
  AND (SELECT COUNT(*) FROM `tmp_demo_raw_safety_replenishment` seed
       JOIN `mdm_material` material
         ON material.`material_code` = seed.`material_code` AND material.`deleted_flag` = 0) = 3
  AND NOT EXISTS (
    SELECT 1 FROM `wms_inbound_order`
    WHERE `inbound_no` = @replenishment_inbound_no
  );

SELECT `inbound_id` INTO @replenishment_inbound_id
FROM `wms_inbound_order`
WHERE `inbound_no` = @replenishment_inbound_no AND `deleted_flag` = 0
LIMIT 1;

INSERT INTO `wms_material_batch`
  (`batch_no`, `material_id`, `supplier_id`, `supplier_batch_no`, `production_date`, `received_date`,
   `quality_status`, `status`, `source_order_type`, `source_order_id`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT seed.`batch_no`, material.`material_id`, @pack_supplier_id, seed.`supplier_batch_no`,
       DATE_SUB(CURDATE(), INTERVAL 2 DAY), CURDATE(),
       'PASS', 'AVAILABLE', 'PURCHASE_RECEIPT', @replenishment_inbound_id,
       CONCAT(@replenishment_marker, ': accepted purchase batch'),
       @replenishment_operator_id, @replenishment_time,
       @replenishment_operator_id, @replenishment_time
FROM `tmp_demo_raw_safety_replenishment` seed
JOIN `mdm_material` material
  ON material.`material_code` = seed.`material_code` AND material.`deleted_flag` = 0
WHERE @replenishment_inbound_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `wms_material_batch` batch_record
    WHERE batch_record.`batch_no` = seed.`batch_no`
  );

INSERT INTO `wms_inbound_detail`
  (`inbound_id`, `material_id`, `batch_id`, `location_id`, `inbound_quantity`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT @replenishment_inbound_id, material.`material_id`, batch_record.`batch_id`, @raw_location_id,
       seed.`inbound_quantity`, CONCAT(@replenishment_marker, ': accepted quantity to raw warehouse'),
       @replenishment_operator_id, @replenishment_time,
       @replenishment_operator_id, @replenishment_time
FROM `tmp_demo_raw_safety_replenishment` seed
JOIN `mdm_material` material
  ON material.`material_code` = seed.`material_code` AND material.`deleted_flag` = 0
JOIN `wms_material_batch` batch_record
  ON batch_record.`batch_no` = seed.`batch_no` AND batch_record.`deleted_flag` = 0
LEFT JOIN `wms_inbound_detail` detail
  ON detail.`inbound_id` = @replenishment_inbound_id
 AND detail.`material_id` = material.`material_id`
 AND detail.`deleted_flag` = 0
WHERE @replenishment_inbound_id IS NOT NULL
  AND detail.`detail_id` IS NULL;

INSERT INTO `wms_inventory`
  (`warehouse_id`, `location_id`, `material_id`, `batch_id`, `available_quantity`, `locked_quantity`,
   `inspection_quantity`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT @raw_warehouse_id, detail.`location_id`, detail.`material_id`, detail.`batch_id`,
       detail.`inbound_quantity`, 0, 0,
       CONCAT(@replenishment_marker, ': balance created from confirmed inbound'),
       @replenishment_operator_id, @replenishment_time,
       @replenishment_operator_id, @replenishment_time
FROM `wms_inbound_detail` detail
LEFT JOIN `wms_inventory` inventory
  ON inventory.`warehouse_id` = @raw_warehouse_id
 AND inventory.`location_id` = detail.`location_id`
 AND inventory.`material_id` = detail.`material_id`
 AND inventory.`batch_id` = detail.`batch_id`
 AND inventory.`deleted_flag` = 0
WHERE detail.`inbound_id` = @replenishment_inbound_id
  AND detail.`deleted_flag` = 0
  AND inventory.`inventory_id` IS NULL;

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `created_by`, `created_time`)
SELECT CONCAT('MV-DEMO-RAW-SAFETY-', detail.`detail_id`), 'INBOUND', 'INBOUND_ORDER',
       @replenishment_inbound_id, detail.`material_id`, detail.`batch_id`, detail.`inbound_quantity`,
       'IN', @replenishment_time,
       CONCAT(@replenishment_marker, ': confirmed purchase inbound'),
       @replenishment_operator_id, @replenishment_time
FROM `wms_inbound_detail` detail
WHERE detail.`inbound_id` = @replenishment_inbound_id
  AND detail.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `wms_stock_movement` movement
    WHERE movement.`movement_no` = CONCAT('MV-DEMO-RAW-SAFETY-', detail.`detail_id`)
  );

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `created_by`, `created_time`)
SELECT 'WMS_INBOUND_ORDER', @replenishment_inbound_id, 'SUBMITTED', 'APPROVED', 'CONFIRM_INBOUND',
       @replenishment_operator_id, @replenishment_time,
       CONCAT(@replenishment_marker, ': inventory confirmation recorded'),
       @replenishment_operator_id, @replenishment_time
FROM DUAL
WHERE @replenishment_inbound_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_status_change_log`
    WHERE `biz_type` = 'WMS_INBOUND_ORDER'
      AND `biz_id` = @replenishment_inbound_id
      AND `change_reason` = 'CONFIRM_INBOUND'
      AND `deleted_flag` = 0
  );

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `created_by`, `created_time`)
SELECT @replenishment_operator_id, 'CONFIRM_INBOUND', 'wms_inbound_order', @replenishment_inbound_id,
       @replenishment_time,
       CONCAT(@replenishment_marker, ': C500 packaging safety stock replenished'),
       @replenishment_operator_id, @replenishment_time
FROM DUAL
WHERE @replenishment_inbound_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_operation_log`
    WHERE `operation_type` = 'CONFIRM_INBOUND'
      AND `module_code` = 'wms_inbound_order'
      AND `biz_id` = @replenishment_inbound_id
      AND `deleted_flag` = 0
  );

COMMIT;

SELECT inbound_order.`inbound_id`, inbound_order.`inbound_no`, inbound_order.`audit_status`,
       material.`material_code`, detail.`inbound_quantity`, inventory.`available_quantity`,
       movement.`movement_no`, movement.`direction`
FROM `wms_inbound_order` inbound_order
JOIN `wms_inbound_detail` detail
  ON detail.`inbound_id` = inbound_order.`inbound_id` AND detail.`deleted_flag` = 0
JOIN `mdm_material` material
  ON material.`material_id` = detail.`material_id` AND material.`deleted_flag` = 0
JOIN `wms_inventory` inventory
  ON inventory.`warehouse_id` = inbound_order.`warehouse_id`
 AND inventory.`location_id` = detail.`location_id`
 AND inventory.`material_id` = detail.`material_id`
 AND inventory.`batch_id` = detail.`batch_id`
 AND inventory.`deleted_flag` = 0
JOIN `wms_stock_movement` movement
  ON movement.`source_order_type` = 'INBOUND_ORDER'
 AND movement.`source_order_id` = inbound_order.`inbound_id`
 AND movement.`material_id` = detail.`material_id`
 AND movement.`batch_id` = detail.`batch_id`
 AND movement.`direction` = 'IN'
 AND movement.`deleted_flag` = 0
WHERE inbound_order.`inbound_no` = @replenishment_inbound_no
  AND inbound_order.`deleted_flag` = 0
ORDER BY material.`material_code`;
