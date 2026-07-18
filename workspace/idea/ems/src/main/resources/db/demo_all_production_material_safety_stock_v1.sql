-- Build high safety stock for every active BOM consumption material.
-- The script writes confirmed purchase inbound documents, batches, balances, movements, and audit records.

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @safety_marker = 'DEMO_ALL_PRODUCTION_MATERIAL_SAFETY_STOCK_20260717_V1';
SET @safety_operator_id = 1;
SET @safety_time = NOW();

DROP TEMPORARY TABLE IF EXISTS `tmp_demo_all_material_safety_stock`;
CREATE TEMPORARY TABLE `tmp_demo_all_material_safety_stock` (
  `material_code` VARCHAR(64) NOT NULL PRIMARY KEY,
  `supplier_code` VARCHAR(64) NOT NULL,
  `inbound_quantity` DECIMAL(18,4) NOT NULL,
  `batch_no` VARCHAR(64) NOT NULL UNIQUE,
  `supplier_batch_no` VARCHAR(64) NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_demo_all_material_safety_stock`
  (`material_code`, `supplier_code`, `inbound_quantity`, `batch_no`, `supplier_batch_no`)
VALUES
  ('AUX-ADHESIVE-RESIN', 'SUP-AUX-001', 1000000.0000, 'RM-DEMO-HIGH-ADHESIVE-20260717-01', 'SUP-HIGH-ADHESIVE-260717-01'),
  ('AUX-THREAD-NYLON-07', 'SUP-AUX-001', 500000.0000, 'RM-DEMO-HIGH-THREAD-20260717-01', 'SUP-HIGH-THREAD-260717-01'),
  ('PKG-CARTON-LNX-25', 'SUP-PACK-001', 5000.0000, 'RM-DEMO-HIGH-CTN-LNX-20260717-01', 'SUP-HIGH-CTN-LNX-260717-01'),
  ('PKG-CARTON-XY-25', 'SUP-PACK-001', 5000.0000, 'RM-DEMO-HIGH-CTN-XY-20260717-01', 'SUP-HIGH-CTN-XY-260717-01'),
  ('PKG-LABEL-LNX-M700-77', 'SUP-PACK-001', 50000.0000, 'RM-DEMO-HIGH-LBL-M70077-20260717-01', 'SUP-HIGH-LBL-M70077-260717-01'),
  ('PKG-LABEL-LNX-M700-78', 'SUP-PACK-001', 50000.0000, 'RM-DEMO-HIGH-LBL-M70078-20260717-01', 'SUP-HIGH-LBL-M70078-260717-01'),
  ('PKG-LABEL-LNX-T300-76', 'SUP-PACK-001', 50000.0000, 'RM-DEMO-HIGH-LBL-T30076-20260717-01', 'SUP-HIGH-LBL-T30076-260717-01'),
  ('PKG-LABEL-LNX-T300-77', 'SUP-PACK-001', 50000.0000, 'RM-DEMO-HIGH-LBL-T30077-20260717-01', 'SUP-HIGH-LBL-T30077-260717-01'),
  ('PKG-LABEL-XY-C500-77', 'SUP-PACK-001', 50000.0000, 'RM-DEMO-HIGH-LBL-C50077-20260717-01', 'SUP-HIGH-LBL-C50077-260717-01'),
  ('PKG-TUBE-LNX-M700', 'SUP-PACK-001', 50000.0000, 'RM-DEMO-HIGH-TUBE-M700-20260717-01', 'SUP-HIGH-TUBE-M700-260717-01'),
  ('PKG-TUBE-LNX-T300', 'SUP-PACK-001', 50000.0000, 'RM-DEMO-HIGH-TUBE-T300-20260717-01', 'SUP-HIGH-TUBE-T300-260717-01'),
  ('PKG-TUBE-XY-C500', 'SUP-PACK-001', 50000.0000, 'RM-DEMO-HIGH-TUBE-C500-20260717-01', 'SUP-HIGH-TUBE-C500-260717-01'),
  ('RM-CORK-COMPOSITE-76', 'SUP-CORK-001', 500000.0000, 'RM-DEMO-HIGH-CORK-C76-20260717-01', 'SUP-HIGH-CORK-C76-260717-01'),
  ('RM-CORK-COMPOSITE-77', 'SUP-CORK-001', 500000.0000, 'RM-DEMO-HIGH-CORK-C77-20260717-01', 'SUP-HIGH-CORK-C77-260717-01'),
  ('RM-CORK-NATURAL-77', 'SUP-CORK-001', 500000.0000, 'RM-DEMO-HIGH-CORK-N77-20260717-01', 'SUP-HIGH-CORK-N77-260717-01'),
  ('RM-CORK-NATURAL-78', 'SUP-CORK-001', 500000.0000, 'RM-DEMO-HIGH-CORK-N78-20260717-01', 'SUP-HIGH-CORK-N78-260717-01'),
  ('RM-FEATHER-DUCK-A', 'SUP-DUCK-001', 5000000.0000, 'RM-DEMO-HIGH-DUCK-20260717-01', 'SUP-HIGH-DUCK-260717-01'),
  ('RM-FEATHER-GOOSE-A', 'SUP-GOOSE-001', 5000000.0000, 'RM-DEMO-HIGH-GOOSE-20260717-01', 'SUP-HIGH-GOOSE-260717-01');

SELECT `warehouse_id` INTO @raw_warehouse_id
FROM `wms_warehouse`
WHERE `warehouse_code` = 'WH-RAW-01' AND `deleted_flag` = 0
LIMIT 1;

SELECT `location_id` INTO @raw_location_id
FROM `wms_location`
WHERE `location_code` = 'LOC-RAW-A01' AND `deleted_flag` = 0
LIMIT 1;

SELECT COUNT(*) INTO @safety_material_count
FROM `tmp_demo_all_material_safety_stock` seed
JOIN `mdm_material` material
  ON material.`material_code` = seed.`material_code` AND material.`deleted_flag` = 0;

SELECT COUNT(*) INTO @safety_supplier_count
FROM `tmp_demo_all_material_safety_stock` seed
JOIN `mdm_supplier` supplier
  ON supplier.`supplier_code` = seed.`supplier_code` AND supplier.`deleted_flag` = 0;

START TRANSACTION;

INSERT INTO `wms_inbound_order`
  (`inbound_no`, `inbound_type`, `warehouse_id`, `source_order_type`, `audit_status`, `inbound_time`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('IB-DEMO-RAW-HIGH-20260717-', REPLACE(seed.`supplier_code`, 'SUP-', '')),
       'PURCHASE', @raw_warehouse_id, 'PURCHASE_RECEIPT', 'APPROVED', @safety_time,
       CONCAT(@safety_marker, ': high safety stock purchase inbound'),
       @safety_operator_id, @safety_time, @safety_operator_id, @safety_time
FROM (SELECT DISTINCT `supplier_code` FROM `tmp_demo_all_material_safety_stock`) seed
JOIN `mdm_supplier` supplier
  ON supplier.`supplier_code` = seed.`supplier_code` AND supplier.`deleted_flag` = 0
WHERE @raw_warehouse_id IS NOT NULL
  AND @raw_location_id IS NOT NULL
  AND @safety_material_count = 18
  AND @safety_supplier_count = 18
  AND NOT EXISTS (
    SELECT 1 FROM `wms_inbound_order` inbound_order
    WHERE inbound_order.`inbound_no` = CONCAT('IB-DEMO-RAW-HIGH-20260717-', REPLACE(seed.`supplier_code`, 'SUP-', ''))
  );

INSERT INTO `wms_material_batch`
  (`batch_no`, `material_id`, `supplier_id`, `supplier_batch_no`, `production_date`, `received_date`,
   `quality_status`, `status`, `source_order_type`, `source_order_id`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT seed.`batch_no`, material.`material_id`, supplier.`supplier_id`, seed.`supplier_batch_no`,
       DATE_SUB(CURDATE(), INTERVAL 2 DAY), CURDATE(),
       'PASS', 'AVAILABLE', 'PURCHASE_RECEIPT', inbound_order.`inbound_id`,
       CONCAT(@safety_marker, ': accepted high safety stock batch'),
       @safety_operator_id, @safety_time, @safety_operator_id, @safety_time
FROM `tmp_demo_all_material_safety_stock` seed
JOIN `mdm_material` material
  ON material.`material_code` = seed.`material_code` AND material.`deleted_flag` = 0
JOIN `mdm_supplier` supplier
  ON supplier.`supplier_code` = seed.`supplier_code` AND supplier.`deleted_flag` = 0
JOIN `wms_inbound_order` inbound_order
  ON inbound_order.`inbound_no` = CONCAT('IB-DEMO-RAW-HIGH-20260717-', REPLACE(seed.`supplier_code`, 'SUP-', ''))
 AND inbound_order.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `wms_material_batch` batch_record
  WHERE batch_record.`batch_no` = seed.`batch_no`
);

INSERT INTO `wms_inbound_detail`
  (`inbound_id`, `material_id`, `batch_id`, `location_id`, `inbound_quantity`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT inbound_order.`inbound_id`, material.`material_id`, batch_record.`batch_id`, @raw_location_id,
       seed.`inbound_quantity`, CONCAT(@safety_marker, ': accepted quantity to raw warehouse'),
       @safety_operator_id, @safety_time, @safety_operator_id, @safety_time
FROM `tmp_demo_all_material_safety_stock` seed
JOIN `mdm_material` material
  ON material.`material_code` = seed.`material_code` AND material.`deleted_flag` = 0
JOIN `wms_material_batch` batch_record
  ON batch_record.`batch_no` = seed.`batch_no` AND batch_record.`deleted_flag` = 0
JOIN `wms_inbound_order` inbound_order
  ON inbound_order.`inbound_no` = CONCAT('IB-DEMO-RAW-HIGH-20260717-', REPLACE(seed.`supplier_code`, 'SUP-', ''))
 AND inbound_order.`deleted_flag` = 0
LEFT JOIN `wms_inbound_detail` detail
  ON detail.`inbound_id` = inbound_order.`inbound_id`
 AND detail.`material_id` = material.`material_id`
 AND detail.`deleted_flag` = 0
WHERE detail.`detail_id` IS NULL;

INSERT INTO `wms_inventory`
  (`warehouse_id`, `location_id`, `material_id`, `batch_id`, `available_quantity`, `locked_quantity`,
   `inspection_quantity`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT inbound_order.`warehouse_id`, detail.`location_id`, detail.`material_id`, detail.`batch_id`,
       detail.`inbound_quantity`, 0, 0,
       CONCAT(@safety_marker, ': balance created from confirmed inbound'),
       @safety_operator_id, @safety_time, @safety_operator_id, @safety_time
FROM `wms_inbound_detail` detail
JOIN `wms_inbound_order` inbound_order
  ON inbound_order.`inbound_id` = detail.`inbound_id` AND inbound_order.`deleted_flag` = 0
LEFT JOIN `wms_inventory` inventory
  ON inventory.`warehouse_id` = inbound_order.`warehouse_id`
 AND inventory.`location_id` = detail.`location_id`
 AND inventory.`material_id` = detail.`material_id`
 AND inventory.`batch_id` = detail.`batch_id`
 AND inventory.`deleted_flag` = 0
WHERE inbound_order.`remark` = CONCAT(@safety_marker, ': high safety stock purchase inbound')
  AND detail.`deleted_flag` = 0
  AND inventory.`inventory_id` IS NULL;

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `created_by`, `created_time`)
SELECT CONCAT('MV-DEMO-RAW-HIGH-', detail.`detail_id`), 'INBOUND', 'INBOUND_ORDER',
       detail.`inbound_id`, detail.`material_id`, detail.`batch_id`, detail.`inbound_quantity`,
       'IN', @safety_time, CONCAT(@safety_marker, ': confirmed purchase inbound'),
       @safety_operator_id, @safety_time
FROM `wms_inbound_detail` detail
JOIN `wms_inbound_order` inbound_order
  ON inbound_order.`inbound_id` = detail.`inbound_id` AND inbound_order.`deleted_flag` = 0
WHERE inbound_order.`remark` = CONCAT(@safety_marker, ': high safety stock purchase inbound')
  AND detail.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `wms_stock_movement` movement
    WHERE movement.`movement_no` = CONCAT('MV-DEMO-RAW-HIGH-', detail.`detail_id`)
  );

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `created_by`, `created_time`)
SELECT 'WMS_INBOUND_ORDER', inbound_order.`inbound_id`, 'SUBMITTED', 'APPROVED', 'CONFIRM_INBOUND',
       @safety_operator_id, @safety_time,
       CONCAT(@safety_marker, ': inventory confirmation recorded'),
       @safety_operator_id, @safety_time
FROM `wms_inbound_order` inbound_order
WHERE inbound_order.`remark` = CONCAT(@safety_marker, ': high safety stock purchase inbound')
  AND inbound_order.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `sys_status_change_log` status_log
    WHERE status_log.`biz_type` = 'WMS_INBOUND_ORDER'
      AND status_log.`biz_id` = inbound_order.`inbound_id`
      AND status_log.`change_reason` = 'CONFIRM_INBOUND'
      AND status_log.`deleted_flag` = 0
  );

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `created_by`, `created_time`)
SELECT @safety_operator_id, 'CONFIRM_INBOUND', 'wms_inbound_order', inbound_order.`inbound_id`,
       @safety_time, CONCAT(@safety_marker, ': high safety stock replenished'),
       @safety_operator_id, @safety_time
FROM `wms_inbound_order` inbound_order
WHERE inbound_order.`remark` = CONCAT(@safety_marker, ': high safety stock purchase inbound')
  AND inbound_order.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `sys_operation_log` operation_log
    WHERE operation_log.`operation_type` = 'CONFIRM_INBOUND'
      AND operation_log.`module_code` = 'wms_inbound_order'
      AND operation_log.`biz_id` = inbound_order.`inbound_id`
      AND operation_log.`deleted_flag` = 0
  );

COMMIT;

SELECT material.`material_code`, material.`material_name`, unit.`unit_code`,
       detail.`inbound_quantity`, inventory.`available_quantity`, inbound_order.`inbound_no`
FROM `wms_inbound_order` inbound_order
JOIN `wms_inbound_detail` detail
  ON detail.`inbound_id` = inbound_order.`inbound_id` AND detail.`deleted_flag` = 0
JOIN `mdm_material` material
  ON material.`material_id` = detail.`material_id` AND material.`deleted_flag` = 0
LEFT JOIN `mdm_unit` unit
  ON unit.`unit_id` = material.`unit_id` AND unit.`deleted_flag` = 0
JOIN `wms_inventory` inventory
  ON inventory.`warehouse_id` = inbound_order.`warehouse_id`
 AND inventory.`location_id` = detail.`location_id`
 AND inventory.`material_id` = detail.`material_id`
 AND inventory.`batch_id` = detail.`batch_id`
 AND inventory.`deleted_flag` = 0
WHERE inbound_order.`remark` = CONCAT(@safety_marker, ': high safety stock purchase inbound')
  AND inbound_order.`deleted_flag` = 0
ORDER BY material.`material_code`;
