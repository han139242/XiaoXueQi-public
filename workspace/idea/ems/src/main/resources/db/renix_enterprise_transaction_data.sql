-- 雷尼克斯企业 30 天真实模拟交易数据（2026-06-14 至 2026-07-13）。
-- 前置顺序：schema.sql -> renix_enterprise_master_migration.sql（旧基线升级时）
--           -> enterprise_baseline_data.sql -> smart_scheduling_migration.sql -> 本脚本。
-- 本脚本默认只允许在业务交易表为空时首次执行；成功后重复执行为 no-op，不重复扣减库存。
-- 严禁在未备份、未核对服务器身份或未获共享库执行授权时运行。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @transaction_marker = 'RENIX_TX_30D_V1';
SET @transaction_base_date = '2026-07-13';
SET @transaction_operator_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  ORDER BY `user_id` LIMIT 1
);
SET @transaction_marker_exists = (
  SELECT COUNT(*) FROM `sys_operation_log`
  WHERE `operation_type` = 'SEED_RENIX_30D'
    AND `module_code` = 'db_seed'
    AND `remark` = @transaction_marker
    AND `deleted_flag` = 0
);
SET @existing_transaction_rows =
    (SELECT COUNT(*) FROM `mfg_production_order` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_production_plan` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_work_order` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_task` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_work_report` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `wms_material_batch` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `wms_inventory` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `wms_stock_movement` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `qms_inspection_order` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `trace_product_lot` WHERE `deleted_flag` = 0);
SET @master_ready = (
  SELECT IF(
      (SELECT COUNT(*) FROM `mdm_product` WHERE `deleted_flag` = 0
           AND `product_code` LIKE 'PROD-%') = 5
      AND (SELECT COUNT(*) FROM `mdm_material` WHERE `deleted_flag` = 0) = 23
      AND (SELECT COUNT(*) FROM `mfg_line_product_capability`
           WHERE `deleted_flag` = 0 AND `status` = 'ENABLE') = 5,
      1, 0)
);
SET @transaction_assertion_sql = IF(
  @master_ready = 1
    AND ((@transaction_marker_exists = 0 AND @existing_transaction_rows = 0)
      OR @transaction_marker_exists = 1),
  'DO 0',
  'SELECT * FROM `__ABORT_RENIX_TX_PRECONDITION__`'
);
PREPARE renix_transaction_assertion_stmt FROM @transaction_assertion_sql;
EXECUTE renix_transaction_assertion_stmt;
DEALLOCATE PREPARE renix_transaction_assertion_stmt;

SET @seed_enabled = IF(@transaction_marker_exists = 0, 1, 0);
SET @raw_warehouse_id = (SELECT `warehouse_id` FROM `wms_warehouse` WHERE `warehouse_code` = 'WH-RAW-01' AND `deleted_flag` = 0 LIMIT 1);
SET @raw_location_id = (SELECT `location_id` FROM `wms_location` WHERE `location_code` = 'LOC-RAW-A01' AND `deleted_flag` = 0 LIMIT 1);
SET @fg_warehouse_id = (SELECT `warehouse_id` FROM `wms_warehouse` WHERE `warehouse_code` = 'WH-FG-01' AND `deleted_flag` = 0 LIMIT 1);
SET @fg_location_id = (SELECT `location_id` FROM `wms_location` WHERE `location_code` = 'LOC-FG-A01' AND `deleted_flag` = 0 LIMIT 1);
SET @planner_employee_id = (SELECT `employee_id` FROM `org_employee` WHERE `employee_no` = 'EMP-PMC-001' AND `deleted_flag` = 0 LIMIT 1);
SET @quality_employee_id = (SELECT `employee_id` FROM `org_employee` WHERE `employee_no` = 'EMP-QMS-001' AND `deleted_flag` = 0 LIMIT 1);
SET @warehouse_employee_id = (SELECT `employee_id` FROM `org_employee` WHERE `employee_no` = 'EMP-WMS-001' AND `deleted_flag` = 0 LIMIT 1);
SET @equipment_employee_id = (SELECT `employee_id` FROM `org_employee` WHERE `employee_no` = 'EMP-EAM-001' AND `deleted_flag` = 0 LIMIT 1);

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `tmp_number`;
CREATE TEMPORARY TABLE `tmp_number` AS
WITH RECURSIVE number_series AS (
  SELECT 1 AS `n`
  UNION ALL SELECT `n` + 1 FROM number_series WHERE `n` < 1000
)
SELECT `n` FROM number_series;

-- 1. 采购到货、批次、来料检验与采购入库。
DROP TEMPORARY TABLE IF EXISTS `tmp_purchase_material`;
CREATE TEMPORARY TABLE `tmp_purchase_material` (
  `material_code` VARCHAR(64) PRIMARY KEY,
  `supplier_code` VARCHAR(64) NOT NULL,
  `short_code` VARCHAR(24) NOT NULL,
  `gross_quantity` DECIMAL(18,4) NOT NULL,
  `iqc_product_code` VARCHAR(64) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_purchase_material`
  (`material_code`, `supplier_code`, `short_code`, `gross_quantity`, `iqc_product_code`)
VALUES
  ('RM-FEATHER-DUCK-A', 'SUP-DUCK-001', 'DUCK', 3750000, 'PROD-LNX-T300-77'),
  ('RM-FEATHER-GOOSE-A', 'SUP-GOOSE-001', 'GOOSE', 2250000, 'PROD-LNX-M700-77'),
  ('RM-CORK-COMPOSITE-76', 'SUP-CORK-001', 'CORK-C76', 35000, 'PROD-LNX-T300-76'),
  ('RM-CORK-COMPOSITE-77', 'SUP-CORK-001', 'CORK-C77', 185000, 'PROD-LNX-T300-77'),
  ('RM-CORK-NATURAL-77', 'SUP-CORK-001', 'CORK-N77', 115000, 'PROD-LNX-M700-77'),
  ('RM-CORK-NATURAL-78', 'SUP-CORK-001', 'CORK-N78', 15000, 'PROD-LNX-M700-78'),
  ('AUX-THREAD-NYLON-07', 'SUP-AUX-001', 'THREAD', 140000, 'PROD-LNX-T300-77'),
  ('AUX-ADHESIVE-RESIN', 'SUP-AUX-001', 'ADHESIVE', 215000, 'PROD-LNX-T300-77'),
  ('PKG-TUBE-LNX-T300', 'SUP-PACK-001', 'TUBE-T300', 14800, 'PROD-LNX-T300-77'),
  ('PKG-TUBE-LNX-M700', 'SUP-PACK-001', 'TUBE-M700', 10500, 'PROD-LNX-M700-77'),
  ('PKG-TUBE-XY-C500', 'SUP-PACK-001', 'TUBE-C500', 2800, 'PROD-OEM-XY-C500-77'),
  ('PKG-LABEL-LNX-T300-76', 'SUP-PACK-001', 'LBL-T30076', 2800, 'PROD-LNX-T300-76'),
  ('PKG-LABEL-LNX-T300-77', 'SUP-PACK-001', 'LBL-T30077', 12000, 'PROD-LNX-T300-77'),
  ('PKG-LABEL-LNX-M700-77', 'SUP-PACK-001', 'LBL-M70077', 9200, 'PROD-LNX-M700-77'),
  ('PKG-LABEL-LNX-M700-78', 'SUP-PACK-001', 'LBL-M70078', 1200, 'PROD-LNX-M700-78'),
  ('PKG-LABEL-XY-C500-77', 'SUP-PACK-001', 'LBL-C50077', 2800, 'PROD-OEM-XY-C500-77'),
  ('PKG-CARTON-LNX-25', 'SUP-PACK-001', 'CTN-LNX', 1000, 'PROD-LNX-T300-77'),
  ('PKG-CARTON-XY-25', 'SUP-PACK-001', 'CTN-XY', 120, 'PROD-OEM-XY-C500-77');

DROP TEMPORARY TABLE IF EXISTS `tmp_receipt_wave`;
CREATE TEMPORARY TABLE `tmp_receipt_wave` (
  `wave_no` INT PRIMARY KEY,
  `received_date` DATE NOT NULL,
  `quantity_factor` DECIMAL(5,4) NOT NULL
);
INSERT INTO `tmp_receipt_wave` VALUES
  (1, '2026-06-15', 0.4000),
  (2, '2026-06-25', 0.3000),
  (3, '2026-07-05', 0.3000);

INSERT INTO `wms_inbound_order`
  (`inbound_no`, `inbound_type`, `warehouse_id`, `source_order_type`, `audit_status`, `inbound_time`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('IB-', DATE_FORMAT(wave.`received_date`, '%Y%m%d'), '-', REPLACE(supplier.`supplier_code`, 'SUP-', '')),
       'PURCHASE', @raw_warehouse_id, 'PURCHASE_RECEIPT', 'APPROVED',
       TIMESTAMPADD(HOUR, IF(supplier.`supplier_code` = 'SUP-DUCK-001' AND wave.`wave_no` = 2, 38, 8),
                    TIMESTAMP(wave.`received_date`, '08:00:00')),
       CONCAT(@transaction_marker, ': 采购到货验收后入库'), @transaction_operator_id,
       TIMESTAMP(wave.`received_date`, '08:00:00'), @transaction_operator_id,
       TIMESTAMPADD(HOUR, IF(supplier.`supplier_code` = 'SUP-DUCK-001' AND wave.`wave_no` = 2, 38, 8),
                    TIMESTAMP(wave.`received_date`, '08:00:00'))
FROM (SELECT DISTINCT `supplier_code` FROM `tmp_purchase_material`) supplier_seed
JOIN `mdm_supplier` supplier ON supplier.`supplier_code` = supplier_seed.`supplier_code` AND supplier.`deleted_flag` = 0
CROSS JOIN `tmp_receipt_wave` wave
WHERE @seed_enabled = 1;

DROP TEMPORARY TABLE IF EXISTS `tmp_purchase_batch_seed`;
CREATE TEMPORARY TABLE `tmp_purchase_batch_seed` AS
SELECT material.`material_id`, material.`material_code`, supplier.`supplier_id`, seed.`supplier_code`, seed.`short_code`,
       wave.`wave_no`, wave.`received_date`,
       ROUND(seed.`gross_quantity` * wave.`quantity_factor`, 4) AS `received_quantity`,
       ROUND(seed.`gross_quantity` * wave.`quantity_factor`, 4)
         - IF(seed.`material_code` = 'RM-FEATHER-DUCK-A' AND wave.`wave_no` = 2, 2000, 0) AS `accepted_quantity`,
       CONCAT('RM-', seed.`short_code`, '-', DATE_FORMAT(wave.`received_date`, '%Y%m%d'), '-', LPAD(wave.`wave_no`, 2, '0')) AS `batch_no`,
       seed.`iqc_product_code`, inbound_order.`inbound_id`
FROM `tmp_purchase_material` seed
JOIN `mdm_material` material ON material.`material_code` = seed.`material_code` AND material.`deleted_flag` = 0
JOIN `mdm_supplier` supplier ON supplier.`supplier_code` = seed.`supplier_code` AND supplier.`deleted_flag` = 0
CROSS JOIN `tmp_receipt_wave` wave
JOIN `wms_inbound_order` inbound_order
  ON inbound_order.`inbound_no` = CONCAT('IB-', DATE_FORMAT(wave.`received_date`, '%Y%m%d'), '-', REPLACE(seed.`supplier_code`, 'SUP-', ''))
 AND inbound_order.`deleted_flag` = 0;

INSERT INTO `wms_material_batch`
  (`batch_no`, `material_id`, `supplier_id`, `supplier_batch_no`, `production_date`, `received_date`,
   `quality_status`, `status`, `source_order_type`, `source_order_id`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT seed.`batch_no`, seed.`material_id`, seed.`supplier_id`,
       CONCAT('SUP-', seed.`short_code`, '-', DATE_FORMAT(seed.`received_date`, '%y%m%d'), '-', seed.`wave_no`),
       DATE_SUB(seed.`received_date`, INTERVAL 5 DAY), seed.`received_date`, 'PASS', 'ENABLE',
       'PURCHASE_RECEIPT', seed.`inbound_id`,
       CONCAT(@transaction_marker, ': ', IF(seed.`received_quantity` = seed.`accepted_quantity`, '来料检验合格', '分选剔除2000根后复检合格')),
       @transaction_operator_id, TIMESTAMP(seed.`received_date`, '08:20:00'),
       @transaction_operator_id,
       IF(seed.`received_quantity` = seed.`accepted_quantity`, TIMESTAMP(seed.`received_date`, '16:00:00'),
          TIMESTAMPADD(DAY, 1, TIMESTAMP(seed.`received_date`, '14:00:00')))
FROM `tmp_purchase_batch_seed` seed
WHERE @seed_enabled = 1;

INSERT INTO `wms_inbound_detail`
  (`inbound_id`, `material_id`, `batch_id`, `location_id`, `inbound_quantity`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT seed.`inbound_id`, seed.`material_id`, batch_record.`batch_id`, @raw_location_id, seed.`accepted_quantity`,
       CONCAT(@transaction_marker, ': 仅检验接收数量进入可用库存'), @transaction_operator_id,
       TIMESTAMP(seed.`received_date`, '09:00:00'), @transaction_operator_id,
       IF(seed.`received_quantity` = seed.`accepted_quantity`, TIMESTAMP(seed.`received_date`, '16:00:00'),
          TIMESTAMPADD(DAY, 1, TIMESTAMP(seed.`received_date`, '14:00:00')))
FROM `tmp_purchase_batch_seed` seed
JOIN `wms_material_batch` batch_record ON batch_record.`batch_no` = seed.`batch_no` AND batch_record.`deleted_flag` = 0
WHERE @seed_enabled = 1;

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `created_by`, `created_time`)
SELECT CONCAT('MV-PI-', LPAD(detail.`detail_id`, 6, '0')), 'INBOUND', 'INBOUND_ORDER', detail.`inbound_id`,
       detail.`material_id`, detail.`batch_id`, detail.`inbound_quantity`, 'IN', inbound_order.`inbound_time`,
       CONCAT(@transaction_marker, ': 采购检验合格入库'), @transaction_operator_id, inbound_order.`inbound_time`
FROM `wms_inbound_detail` detail
JOIN `wms_inbound_order` inbound_order ON inbound_order.`inbound_id` = detail.`inbound_id` AND inbound_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND detail.`deleted_flag` = 0;

INSERT INTO `qms_inspection_order`
  (`inspection_no`, `inspection_type`, `scheme_id`, `batch_id`, `inspector_id`, `inspection_result`, `status`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('IQC-', seed.`batch_no`), 'INCOMING', scheme.`scheme_id`, batch_record.`batch_id`,
       @quality_employee_id,
       IF(seed.`material_code` = 'RM-FEATHER-DUCK-A' AND seed.`wave_no` = 2, 'FAIL', 'PASS'), 'FINISHED',
       CONCAT(@transaction_marker, ': ', IF(seed.`material_code` = 'RM-FEATHER-DUCK-A' AND seed.`wave_no` = 2,
         '外观抽样发现弯曲和色差，转受控分选', '来料检验合格')),
       @transaction_operator_id, TIMESTAMP(seed.`received_date`, '10:00:00'),
       @transaction_operator_id, TIMESTAMP(seed.`received_date`, '12:00:00')
FROM `tmp_purchase_batch_seed` seed
JOIN `wms_material_batch` batch_record ON batch_record.`batch_no` = seed.`batch_no` AND batch_record.`deleted_flag` = 0
JOIN `mdm_product` product ON product.`product_code` = seed.`iqc_product_code` AND product.`deleted_flag` = 0
JOIN `qms_inspection_scheme` scheme ON scheme.`product_id` = product.`product_id`
 AND scheme.`inspection_type` = 'INCOMING' AND scheme.`deleted_flag` = 0
WHERE @seed_enabled = 1;

-- 每类物料只保存有业务意义的来料项目：外观必检，毛片/球头增加尺寸项目。
INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `defect_description`, `remark`,
   `created_by`, `created_time`)
SELECT inspection_order.`inspection_id`, inspection_item.`item_id`,
       IF(seed.`material_code` = 'RM-FEATHER-DUCK-A' AND seed.`wave_no` = 2, '少量弯曲及色差', '合格'),
       IF(seed.`material_code` = 'RM-FEATHER-DUCK-A' AND seed.`wave_no` = 2, 'FAIL', 'PASS'),
       IF(seed.`material_code` = 'RM-FEATHER-DUCK-A' AND seed.`wave_no` = 2, '抽样发现弯曲和色差', NULL),
       CONCAT(@transaction_marker, ': 来料外观抽检'), @transaction_operator_id,
       TIMESTAMP(seed.`received_date`, '11:20:00')
FROM `tmp_purchase_batch_seed` seed
JOIN `qms_inspection_order` inspection_order ON inspection_order.`inspection_no` = CONCAT('IQC-', seed.`batch_no`)
 AND inspection_order.`deleted_flag` = 0
JOIN `qms_inspection_item` inspection_item ON inspection_item.`item_code` = 'QI-RAW-APPEARANCE'
 AND inspection_item.`deleted_flag` = 0
WHERE @seed_enabled = 1;

INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `remark`, `created_by`, `created_time`)
SELECT inspection_order.`inspection_id`, inspection_item.`item_id`,
       CAST(ROUND(66.6 + seed.`wave_no` * 0.2, 1) AS CHAR), 'PASS',
       CONCAT(@transaction_marker, ': 毛片长度抽检'), @transaction_operator_id,
       TIMESTAMP(seed.`received_date`, '11:10:00')
FROM `tmp_purchase_batch_seed` seed
JOIN `qms_inspection_order` inspection_order ON inspection_order.`inspection_no` = CONCAT('IQC-', seed.`batch_no`)
 AND inspection_order.`deleted_flag` = 0
JOIN `qms_inspection_item` inspection_item ON inspection_item.`item_code` = 'QI-FEATHER-LENGTH'
 AND inspection_item.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND seed.`material_code` LIKE 'RM-FEATHER-%';

INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `remark`, `created_by`, `created_time`)
SELECT inspection_order.`inspection_id`, inspection_item.`item_id`,
       CAST(ROUND(26.3 + seed.`wave_no` * 0.1, 1) AS CHAR), 'PASS',
       CONCAT(@transaction_marker, ': 球头直径抽检'), @transaction_operator_id,
       TIMESTAMP(seed.`received_date`, '11:10:00')
FROM `tmp_purchase_batch_seed` seed
JOIN `qms_inspection_order` inspection_order ON inspection_order.`inspection_no` = CONCAT('IQC-', seed.`batch_no`)
 AND inspection_order.`deleted_flag` = 0
JOIN `qms_inspection_item` inspection_item ON inspection_item.`item_code` = 'QI-HEAD-DIAMETER'
 AND inspection_item.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND seed.`material_code` LIKE 'RM-CORK-%';

-- 鸭毛片第二批分选后的复检，构成第 55 张来料检验单。
INSERT INTO `qms_inspection_order`
  (`inspection_no`, `inspection_type`, `scheme_id`, `batch_id`, `inspector_id`, `inspection_result`, `status`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('IQC-RC-', seed.`batch_no`), 'INCOMING', scheme.`scheme_id`, batch_record.`batch_id`,
       @quality_employee_id, 'PASS', 'FINISHED', CONCAT(@transaction_marker, ': 分选后复检合格'),
       @transaction_operator_id, TIMESTAMPADD(DAY, 1, TIMESTAMP(seed.`received_date`, '09:00:00')),
       @transaction_operator_id, TIMESTAMPADD(DAY, 1, TIMESTAMP(seed.`received_date`, '12:00:00'))
FROM `tmp_purchase_batch_seed` seed
JOIN `wms_material_batch` batch_record ON batch_record.`batch_no` = seed.`batch_no` AND batch_record.`deleted_flag` = 0
JOIN `mdm_product` product ON product.`product_code` = seed.`iqc_product_code` AND product.`deleted_flag` = 0
JOIN `qms_inspection_scheme` scheme ON scheme.`product_id` = product.`product_id`
 AND scheme.`inspection_type` = 'INCOMING' AND scheme.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND seed.`material_code` = 'RM-FEATHER-DUCK-A' AND seed.`wave_no` = 2;

INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `remark`, `created_by`, `created_time`)
SELECT recheck.`inspection_id`, item.`item_id`, values_seed.`measured_value`, 'PASS',
       CONCAT(@transaction_marker, ': 分选后复检'), @transaction_operator_id, recheck.`updated_time`
FROM `qms_inspection_order` recheck
JOIN (SELECT 'QI-FEATHER-LENGTH' AS `item_code`, '66.9' AS `measured_value`
      UNION ALL SELECT 'QI-RAW-APPEARANCE', '合格') values_seed
JOIN `qms_inspection_item` item ON item.`item_code` = values_seed.`item_code` AND item.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND recheck.`inspection_no` LIKE 'IQC-RC-%' AND recheck.`deleted_flag` = 0;

INSERT INTO `qms_nonconforming_order`
  (`ncr_no`, `inspection_id`, `defect_reason`, `disposition_type`, `approval_status`, `status`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT 'NCR-20260625-001', inspection_order.`inspection_id`, '鸭毛片外观抽样发现少量弯曲和色差',
       'REWORK', 'APPROVED', 'CLOSED', CONCAT(@transaction_marker, ': 受控分选剔除2000根，保留部分复检合格'),
       @transaction_operator_id, '2026-06-25 12:10:00', @transaction_operator_id, '2026-06-26 12:10:00'
FROM `qms_inspection_order` inspection_order
WHERE @seed_enabled = 1 AND inspection_order.`inspection_no` LIKE 'IQC-RM-DUCK-20260625-%'
  AND inspection_order.`inspection_result` = 'FAIL' AND inspection_order.`deleted_flag` = 0;

INSERT INTO `qms_nonconforming_detail`
  (`ncr_id`, `inspection_result_id`, `defect_quantity`, `final_disposition`, `recheck_result`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT ncr.`ncr_id`, result_record.`result_id`, 2000, 'REWORK', 'PASS',
       CONCAT(@transaction_marker, ': 剔除数量不进入可用库存'), @transaction_operator_id,
       '2026-06-25 12:20:00', @transaction_operator_id, '2026-06-26 12:10:00'
FROM `qms_nonconforming_order` ncr
JOIN `qms_inspection_result` result_record ON result_record.`inspection_id` = ncr.`inspection_id`
 AND result_record.`result` = 'FAIL' AND result_record.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND ncr.`ncr_no` = 'NCR-20260625-001' AND ncr.`deleted_flag` = 0;

-- 2. 30 张生产订单；5 张 DRAFT 订单不进入计划、工单和排程。
DROP TEMPORARY TABLE IF EXISTS `tmp_order_seed`;
CREATE TEMPORARY TABLE `tmp_order_seed` (
  `order_seq` INT PRIMARY KEY,
  `order_date` DATE NOT NULL,
  `product_code` VARCHAR(64) NOT NULL,
  `customer_code` VARCHAR(64) NOT NULL,
  `order_status` VARCHAR(32) NOT NULL,
  `order_quantity` DECIMAL(18,4) NOT NULL,
  `delivery_date` DATE NOT NULL,
  `first_work_date` DATE NULL,
  `second_work_date` DATE NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_order_seed` VALUES
  (1, '2026-06-16', 'PROD-LNX-T300-76', 'CUS-RETAIL-001', 'FINISHED', 600, '2026-06-19', '2026-06-17', NULL),
  (2, '2026-06-25', 'PROD-LNX-T300-76', 'CUS-DIST-001', 'FINISHED', 600, '2026-06-30', '2026-06-27', NULL),
  (3, '2026-07-10', 'PROD-LNX-T300-76', 'CUS-RETAIL-001', 'RELEASED', 600, '2026-07-16', '2026-07-14', NULL),
  (4, '2026-07-12', 'PROD-LNX-T300-76', 'CUS-RETAIL-001', 'DRAFT', 600, '2026-07-18', NULL, NULL),
  (5, '2026-06-14', 'PROD-LNX-T300-77', 'CUS-SCHOOL-001', 'FINISHED', 800, '2026-06-18', '2026-06-16', NULL),
  (6, '2026-06-17', 'PROD-LNX-T300-77', 'CUS-RETAIL-001', 'FINISHED', 800, '2026-06-22', '2026-06-18', NULL),
  (7, '2026-06-19', 'PROD-LNX-T300-77', 'CUS-DIST-001', 'FINISHED', 800, '2026-06-24', '2026-06-20', NULL),
  (8, '2026-06-22', 'PROD-LNX-T300-77', 'CUS-SCHOOL-001', 'FINISHED', 800, '2026-06-26', '2026-06-23', NULL),
  (9, '2026-06-26', 'PROD-LNX-T300-77', 'CUS-RETAIL-001', 'FINISHED', 800, '2026-07-01', '2026-06-29', NULL),
  (10, '2026-06-29', 'PROD-LNX-T300-77', 'CUS-DIST-001', 'FINISHED', 800, '2026-07-03', '2026-06-30', NULL),
  (11, '2026-07-02', 'PROD-LNX-T300-77', 'CUS-SCHOOL-001', 'FINISHED', 800, '2026-07-07', '2026-07-03', NULL),
  (12, '2026-07-05', 'PROD-LNX-T300-77', 'CUS-RETAIL-001', 'FINISHED', 800, '2026-07-09', '2026-07-06', NULL),
  (13, '2026-07-10', 'PROD-LNX-T300-77', 'CUS-SCHOOL-001', 'RUNNING', 800, '2026-07-14', '2026-07-13', NULL),
  (14, '2026-07-11', 'PROD-LNX-T300-77', 'CUS-DIST-001', 'RELEASED', 800, '2026-07-17', '2026-07-15', NULL),
  (15, '2026-07-12', 'PROD-LNX-T300-77', 'CUS-RETAIL-001', 'RELEASED', 800, '2026-07-18', '2026-07-16', NULL),
  (16, '2026-07-12', 'PROD-LNX-T300-77', 'CUS-SCHOOL-001', 'DRAFT', 800, '2026-07-20', NULL, NULL),
  (17, '2026-07-13', 'PROD-LNX-T300-77', 'CUS-DIST-001', 'DRAFT', 800, '2026-07-21', NULL, NULL),
  (18, '2026-06-14', 'PROD-LNX-M700-77', 'CUS-CLUB-001', 'FINISHED', 1000, '2026-06-19', '2026-06-16', '2026-06-17'),
  (19, '2026-06-18', 'PROD-LNX-M700-77', 'CUS-EVENT-001', 'FINISHED', 1000, '2026-06-23', '2026-06-19', '2026-06-20'),
  (20, '2026-06-22', 'PROD-LNX-M700-77', 'CUS-CLUB-001', 'FINISHED', 1000, '2026-06-26', '2026-06-23', '2026-06-24'),
  (21, '2026-06-25', 'PROD-LNX-M700-77', 'CUS-DIST-001', 'FINISHED', 1000, '2026-06-30', '2026-06-26', '2026-06-27'),
  (22, '2026-06-29', 'PROD-LNX-M700-77', 'CUS-EVENT-001', 'FINISHED', 1000, '2026-07-03', '2026-06-30', '2026-07-01'),
  (23, '2026-07-03', 'PROD-LNX-M700-77', 'CUS-CLUB-001', 'FINISHED', 1000, '2026-07-09', '2026-07-06', '2026-07-07'),
  (24, '2026-07-09', 'PROD-LNX-M700-77', 'CUS-CLUB-001', 'RUNNING', 1000, '2026-07-15', '2026-07-11', '2026-07-13'),
  (25, '2026-07-13', 'PROD-LNX-M700-77', 'CUS-EVENT-001', 'DRAFT', 1000, '2026-07-21', NULL, NULL),
  (26, '2026-06-18', 'PROD-LNX-M700-78', 'CUS-EVENT-001', 'FINISHED', 500, '2026-07-01', '2026-06-29', NULL),
  (27, '2026-07-01', 'PROD-LNX-M700-78', 'CUS-EVENT-001', 'FINISHED', 500, '2026-07-10', '2026-07-08', NULL),
  (28, '2026-06-20', 'PROD-OEM-XY-C500-77', 'CUS-OEM-001', 'FINISHED', 800, '2026-06-25', '2026-06-22', NULL),
  (29, '2026-07-01', 'PROD-OEM-XY-C500-77', 'CUS-OEM-001', 'FINISHED', 800, '2026-07-07', '2026-07-04', NULL),
  (30, '2026-07-13', 'PROD-OEM-XY-C500-77', 'CUS-OEM-001', 'DRAFT', 800, '2026-07-22', NULL, NULL);

INSERT INTO `mfg_production_order`
  (`production_order_no`, `customer_id`, `product_id`, `order_quantity`, `delivery_date`, `source_type`, `status`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('MO-', DATE_FORMAT(seed.`order_date`, '%Y%m%d'), '-', LPAD(seed.`order_seq`, 3, '0')),
       customer.`customer_id`, product.`product_id`, seed.`order_quantity`, seed.`delivery_date`,
       IF(seed.`product_code` LIKE 'PROD-OEM-%', 'OEM', 'FORECAST_CONFIRMED'), seed.`order_status`,
       CONCAT(@transaction_marker, ': 30天脱敏客户订单'), @transaction_operator_id,
       TIMESTAMP(seed.`order_date`, '09:00:00'), @transaction_operator_id,
       IF(seed.`order_status` = 'DRAFT', TIMESTAMP(seed.`order_date`, '09:00:00'),
          TIMESTAMP(COALESCE(seed.`second_work_date`, seed.`first_work_date`), '17:00:00'))
FROM `tmp_order_seed` seed
JOIN `mdm_customer` customer ON customer.`customer_code` = seed.`customer_code` AND customer.`deleted_flag` = 0
JOIN `mdm_product` product ON product.`product_code` = seed.`product_code` AND product.`deleted_flag` = 0
WHERE @seed_enabled = 1;

INSERT INTO `mfg_production_plan`
  (`plan_no`, `production_order_id`, `line_id`, `planned_start_time`, `planned_end_time`, `planned_quantity`, `status`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('PP-', DATE_FORMAT(seed.`order_date`, '%Y%m%d'), '-', LPAD(seed.`order_seq`, 3, '0')),
       production_order.`production_order_id`, line.`line_id`, TIMESTAMP(seed.`first_work_date`, '08:00:00'),
       TIMESTAMP(COALESCE(seed.`second_work_date`, seed.`first_work_date`), '16:30:00'), seed.`order_quantity`,
       seed.`order_status`, CONCAT(@transaction_marker, ': 单班产能内正式生产计划'),
       @transaction_operator_id, TIMESTAMPADD(HOUR, 2, production_order.`created_time`), @transaction_operator_id,
       TIMESTAMP(COALESCE(seed.`second_work_date`, seed.`first_work_date`), '17:00:00')
FROM `tmp_order_seed` seed
JOIN `mfg_production_order` production_order
  ON production_order.`production_order_no` = CONCAT('MO-', DATE_FORMAT(seed.`order_date`, '%Y%m%d'), '-', LPAD(seed.`order_seq`, 3, '0'))
 AND production_order.`deleted_flag` = 0
JOIN `factory_line` line ON line.`line_code` = IF(seed.`product_code` IN ('PROD-LNX-M700-77', 'PROD-LNX-M700-78'),
  'LINE-MATCH-01', 'LINE-TRAIN-01') AND line.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND seed.`order_status` <> 'DRAFT';

DROP TEMPORARY TABLE IF EXISTS `tmp_work_order_seed`;
CREATE TEMPORARY TABLE `tmp_work_order_seed` AS
SELECT seed.`order_seq`, number_record.`n` AS `segment_no`, seed.`product_code`, seed.`order_status`,
       CASE WHEN number_record.`n` = 1 THEN seed.`first_work_date` ELSE seed.`second_work_date` END AS `work_date`,
       IF(seed.`product_code` = 'PROD-LNX-M700-77', 500, seed.`order_quantity`) AS `planned_quantity`,
       CASE
         WHEN seed.`order_status` = 'FINISHED' THEN 'FINISHED'
         WHEN seed.`order_status` = 'RUNNING' AND seed.`product_code` = 'PROD-LNX-M700-77' AND number_record.`n` = 1 THEN 'FINISHED'
         WHEN seed.`order_status` = 'RUNNING' THEN 'RUNNING'
         ELSE 'RELEASED'
       END AS `work_order_status`,
       CASE
         WHEN seed.`order_status` = 'FINISHED' THEN IF(seed.`product_code` = 'PROD-LNX-M700-77', 500, seed.`order_quantity`)
         WHEN seed.`order_status` = 'RUNNING' AND seed.`product_code` = 'PROD-LNX-M700-77' AND number_record.`n` = 1 THEN 500
         WHEN seed.`order_status` = 'RUNNING' AND seed.`product_code` = 'PROD-LNX-M700-77' THEN 200
         WHEN seed.`order_status` = 'RUNNING' THEN 320
         ELSE 0
       END AS `current_quantity`
FROM `tmp_order_seed` seed
JOIN `tmp_number` number_record ON number_record.`n` <= IF(seed.`product_code` = 'PROD-LNX-M700-77', 2, 1)
WHERE seed.`order_status` <> 'DRAFT';

INSERT INTO `mfg_work_order`
  (`work_order_no`, `plan_id`, `product_id`, `bom_id`, `route_id`, `planned_quantity`, `completed_quantity`, `status`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('WO-', DATE_FORMAT(seed.`work_date`, '%Y%m%d'), '-', LPAD(seed.`order_seq`, 3, '0'), '-', seed.`segment_no`),
       plan.`plan_id`, product.`product_id`, bom.`bom_id`, route.`route_id`, seed.`planned_quantity`,
       CASE WHEN seed.`work_order_status` = 'FINISHED'
              THEN seed.`planned_quantity` - IF(seed.`product_code` = 'PROD-LNX-T300-77' AND seed.`work_date` = '2026-07-03', 1, 0)
            WHEN seed.`work_order_status` = 'RUNNING' THEN seed.`current_quantity` ELSE 0 END,
       seed.`work_order_status`, CONCAT(@transaction_marker, ': 锁定已发布BOM和工艺路线'),
       @transaction_operator_id, TIMESTAMP(DATE_SUB(seed.`work_date`, INTERVAL 1 DAY), '15:00:00'),
       @transaction_operator_id,
       IF(seed.`work_order_status` = 'RELEASED', TIMESTAMP(DATE_SUB(seed.`work_date`, INTERVAL 1 DAY), '16:00:00'),
          TIMESTAMP(seed.`work_date`, IF(seed.`work_order_status` = 'RUNNING', '13:20:00', '16:30:00')))
FROM `tmp_work_order_seed` seed
JOIN `tmp_order_seed` order_seed ON order_seed.`order_seq` = seed.`order_seq`
JOIN `mfg_production_order` production_order
  ON production_order.`production_order_no` = CONCAT('MO-', DATE_FORMAT(order_seed.`order_date`, '%Y%m%d'), '-', LPAD(order_seed.`order_seq`, 3, '0'))
 AND production_order.`deleted_flag` = 0
JOIN `mfg_production_plan` plan ON plan.`production_order_id` = production_order.`production_order_id` AND plan.`deleted_flag` = 0
JOIN `mdm_product` product ON product.`product_code` = seed.`product_code` AND product.`deleted_flag` = 0
JOIN `mdm_bom` bom ON bom.`product_id` = product.`product_id` AND bom.`status` = 'RELEASED' AND bom.`deleted_flag` = 0
JOIN `process_route` route ON route.`product_id` = product.`product_id` AND route.`status` = 'RELEASED' AND route.`deleted_flag` = 0
WHERE @seed_enabled = 1;

INSERT INTO `mfg_work_order_material`
  (`work_order_id`, `material_id`, `required_quantity`, `issued_quantity`, `shortage_quantity`, `status`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT work_order.`work_order_id`, bom_item.`material_id`,
       CASE WHEN material.`material_code` LIKE 'PKG-CARTON-%'
              THEN CEILING(bom_item.`quantity` * work_order.`planned_quantity` * (1 + bom_item.`loss_rate`))
            ELSE ROUND(bom_item.`quantity` * work_order.`planned_quantity` * (1 + bom_item.`loss_rate`), 4) END,
       0, 0, IF(work_order.`status` = 'RELEASED', 'WAIT_ISSUE', 'ISSUED'),
       CONCAT(@transaction_marker, ': BOM计划需求'), @transaction_operator_id, work_order.`created_time`,
       @transaction_operator_id, work_order.`updated_time`
FROM `mfg_work_order` work_order
JOIN `mdm_bom_item` bom_item ON bom_item.`bom_id` = work_order.`bom_id` AND bom_item.`deleted_flag` = 0
JOIN `mdm_material` material ON material.`material_id` = bom_item.`material_id` AND material.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND work_order.`remark` = CONCAT(@transaction_marker, ': 锁定已发布BOM和工艺路线');

INSERT INTO `mfg_dispatch_order`
  (`dispatch_no`, `work_order_id`, `team_id`, `planned_quantity`, `due_time`, `material_ready_status`, `status`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('DO-', SUBSTRING(work_order.`work_order_no`, 4)), work_order.`work_order_id`, team.`team_id`,
       work_order.`planned_quantity`, TIMESTAMP(seed.`work_date`, '16:30:00'),
       IF(seed.`work_order_status` = 'RELEASED', 'WAITING', 'READY'), seed.`work_order_status`,
       CONCAT(@transaction_marker, ': 产线匹配班组派工'), @transaction_operator_id,
       TIMESTAMP(seed.`work_date`, '07:20:00'), @transaction_operator_id, work_order.`updated_time`
FROM `tmp_work_order_seed` seed
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_no` = CONCAT('WO-', DATE_FORMAT(seed.`work_date`, '%Y%m%d'), '-', LPAD(seed.`order_seq`, 3, '0'), '-', seed.`segment_no`)
 AND work_order.`deleted_flag` = 0
JOIN `org_team` team ON team.`team_code` = IF(seed.`product_code` IN ('PROD-LNX-M700-77', 'PROD-LNX-M700-78'),
  'TEAM-MATCH-01', 'TEAM-TRAIN-01') AND team.`deleted_flag` = 0
WHERE @seed_enabled = 1;

INSERT INTO `mfg_task`
  (`task_no`, `dispatch_id`, `work_order_id`, `operation_id`, `station_id`, `planned_quantity`,
   `completed_quantity`, `defect_quantity`, `status`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('TASK-', DATE_FORMAT(seed.`work_date`, '%Y%m%d'), '-', LPAD(seed.`order_seq`, 3, '0'), '-',
              seed.`segment_no`, '-', LPAD(route_item.`sequence_no`, 3, '0')),
       dispatch_order.`dispatch_id`, work_order.`work_order_id`, route_item.`operation_id`, station.`station_id`,
       work_order.`planned_quantity`,
       CASE WHEN seed.`work_order_status` = 'FINISHED' THEN work_order.`planned_quantity`
            WHEN seed.`work_order_status` = 'RUNNING' AND route_item.`sequence_no` <= 40 THEN work_order.`planned_quantity`
            WHEN seed.`work_order_status` = 'RUNNING' AND route_item.`sequence_no` = 50 THEN seed.`current_quantity`
            ELSE 0 END,
       CASE WHEN seed.`product_code` = 'PROD-LNX-T300-77' AND seed.`work_date` = '2026-07-03'
                  AND route_item.`sequence_no` = 40 THEN 24
            WHEN seed.`product_code` = 'PROD-LNX-T300-77' AND seed.`work_date` = '2026-07-03'
                  AND route_item.`sequence_no` = 70 THEN 1
            ELSE 0 END,
       CASE WHEN seed.`work_order_status` = 'FINISHED' THEN 'FINISHED'
            WHEN seed.`work_order_status` = 'RUNNING' AND route_item.`sequence_no` <= 40 THEN 'FINISHED'
            WHEN seed.`work_order_status` = 'RUNNING' AND route_item.`sequence_no` = 50 THEN 'RUNNING'
            ELSE 'WAIT_START' END,
       CONCAT(@transaction_marker, ': 路线顺序任务'), @transaction_operator_id,
       TIMESTAMP(seed.`work_date`, '07:30:00'), @transaction_operator_id,
       CASE WHEN seed.`work_order_status` = 'RELEASED' OR route_item.`sequence_no` > 50 AND seed.`work_order_status` = 'RUNNING'
              THEN TIMESTAMP(seed.`work_date`, '07:30:00')
            ELSE TIMESTAMPADD(MINUTE, 60 + route_item.`sequence_no` * 6, TIMESTAMP(seed.`work_date`, '08:00:00')) END
FROM `tmp_work_order_seed` seed
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_no` = CONCAT('WO-', DATE_FORMAT(seed.`work_date`, '%Y%m%d'), '-', LPAD(seed.`order_seq`, 3, '0'), '-', seed.`segment_no`)
 AND work_order.`deleted_flag` = 0
JOIN `mfg_dispatch_order` dispatch_order ON dispatch_order.`work_order_id` = work_order.`work_order_id` AND dispatch_order.`deleted_flag` = 0
JOIN `mfg_production_plan` plan ON plan.`plan_id` = work_order.`plan_id` AND plan.`deleted_flag` = 0
JOIN `process_route_item` route_item ON route_item.`route_id` = work_order.`route_id` AND route_item.`deleted_flag` = 0
JOIN `factory_station_operation` station_operation ON station_operation.`operation_id` = route_item.`operation_id`
 AND station_operation.`deleted_flag` = 0
JOIN `factory_station` station ON station.`station_id` = station_operation.`station_id`
 AND station.`line_id` = plan.`line_id` AND station.`deleted_flag` = 0
WHERE @seed_enabled = 1;

DROP TEMPORARY TABLE IF EXISTS `tmp_team_member_rank`;
CREATE TEMPORARY TABLE `tmp_team_member_rank` AS
SELECT member.`team_id`, member.`employee_id`,
       ROW_NUMBER() OVER (PARTITION BY member.`team_id` ORDER BY member.`id`) AS `member_rank`
FROM `org_team_member` member
WHERE member.`deleted_flag` = 0 AND member.`status` = 'ENABLE';

DROP TEMPORARY TABLE IF EXISTS `tmp_work_order_rank`;
CREATE TEMPORARY TABLE `tmp_work_order_rank` AS
SELECT work_order.`work_order_id`, ROW_NUMBER() OVER (ORDER BY plan.`planned_start_time`, work_order.`work_order_id`) AS `work_order_rank`
FROM `mfg_work_order` work_order
JOIN `mfg_production_plan` plan ON plan.`plan_id` = work_order.`plan_id` AND plan.`deleted_flag` = 0
WHERE work_order.`deleted_flag` = 0 AND work_order.`remark` = CONCAT(@transaction_marker, ': 锁定已发布BOM和工艺路线');

INSERT INTO `mfg_task_operator`
  (`task_id`, `employee_id`, `role_in_task`, `assigned_time`, `created_by`, `created_time`)
SELECT task.`task_id`, team_member.`employee_id`, 'PRIMARY', task.`created_time`, @transaction_operator_id, task.`created_time`
FROM `mfg_task` task
JOIN `mfg_dispatch_order` dispatch_order ON dispatch_order.`dispatch_id` = task.`dispatch_id` AND dispatch_order.`deleted_flag` = 0
JOIN `tmp_team_member_rank` team_member ON team_member.`team_id` = dispatch_order.`team_id`
 AND team_member.`member_rank` = MOD(task.`operation_id` - 1, 4) + 1
WHERE @seed_enabled = 1 AND task.`deleted_flag` = 0 AND task.`remark` = CONCAT(@transaction_marker, ': 路线顺序任务');

INSERT INTO `mfg_task_operator`
  (`task_id`, `employee_id`, `role_in_task`, `assigned_time`, `created_by`, `created_time`)
SELECT task.`task_id`, team_member.`employee_id`, 'ASSISTANT', task.`created_time`, @transaction_operator_id, task.`created_time`
FROM `mfg_task` task
JOIN `mfg_dispatch_order` dispatch_order ON dispatch_order.`dispatch_id` = task.`dispatch_id` AND dispatch_order.`deleted_flag` = 0
JOIN `tmp_work_order_rank` work_order_rank ON work_order_rank.`work_order_id` = task.`work_order_id`
JOIN `process_route_item` route_item ON route_item.`route_id` = (SELECT `route_id` FROM `mfg_work_order` WHERE `work_order_id` = task.`work_order_id`)
 AND route_item.`operation_id` = task.`operation_id` AND route_item.`deleted_flag` = 0
JOIN `tmp_team_member_rank` team_member ON team_member.`team_id` = dispatch_order.`team_id`
 AND team_member.`member_rank` = MOD(task.`operation_id`, 4) + 1
WHERE @seed_enabled = 1 AND task.`deleted_flag` = 0 AND work_order_rank.`work_order_rank` <= 28
  AND route_item.`sequence_no` IN (30, 40);

INSERT INTO `mfg_work_report`
  (`report_no`, `task_id`, `operator_id`, `station_id`, `qualified_quantity`, `defect_quantity`,
   `actual_work_hours`, `report_time`, `audit_status`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('WR-', SUBSTRING(task.`task_no`, 6)), task.`task_id`, task_operator.`employee_id`, task.`station_id`,
       task.`completed_quantity` - task.`defect_quantity`, task.`defect_quantity`,
       ROUND(task.`completed_quantity` * route_item.`standard_time` / 60, 4), task.`updated_time`, 'APPROVED',
       CONCAT(@transaction_marker, ': 审核通过生产报工'), @transaction_operator_id,
       TIMESTAMPADD(MINUTE, -10, task.`updated_time`), @transaction_operator_id, task.`updated_time`
FROM `mfg_task` task
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = task.`work_order_id` AND work_order.`deleted_flag` = 0
JOIN `process_route_item` route_item ON route_item.`route_id` = work_order.`route_id`
 AND route_item.`operation_id` = task.`operation_id` AND route_item.`deleted_flag` = 0
JOIN `mfg_task_operator` task_operator ON task_operator.`task_id` = task.`task_id`
 AND task_operator.`role_in_task` = 'PRIMARY' AND task_operator.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND task.`deleted_flag` = 0 AND task.`status` IN ('FINISHED', 'RUNNING');

DROP TEMPORARY TABLE IF EXISTS `tmp_work_order_context`;
CREATE TEMPORARY TABLE `tmp_work_order_context` AS
SELECT work_order.`work_order_id`, work_order.`work_order_no`, seed.`order_seq`, seed.`segment_no`, seed.`work_date`,
       seed.`product_code`, seed.`work_order_status`, seed.`planned_quantity`, seed.`current_quantity`,
       work_order.`plan_id`, work_order.`product_id`, work_order.`bom_id`, work_order.`route_id`
FROM `tmp_work_order_seed` seed
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_no` = CONCAT('WO-', DATE_FORMAT(seed.`work_date`, '%Y%m%d'), '-', LPAD(seed.`order_seq`, 3, '0'), '-', seed.`segment_no`)
 AND work_order.`deleted_flag` = 0;

-- 3. 工单齐套、FIFO 发料、退料和报工实际用料。
DROP TEMPORARY TABLE IF EXISTS `tmp_issue_demand_interval`;
CREATE TEMPORARY TABLE `tmp_issue_demand_interval` AS
SELECT demand_base.*,
       SUM(demand_base.`demand_quantity`) OVER (
         PARTITION BY demand_base.`material_id`
         ORDER BY demand_base.`work_date`, demand_base.`work_order_id`
       ) AS `demand_end`,
       SUM(demand_base.`demand_quantity`) OVER (
         PARTITION BY demand_base.`material_id`
         ORDER BY demand_base.`work_date`, demand_base.`work_order_id`
       ) - demand_base.`demand_quantity` AS `demand_start`
FROM (
  SELECT requirement.`requirement_id`, requirement.`work_order_id`, requirement.`material_id`,
         CASE WHEN material.`material_code` LIKE 'PKG-CARTON-%' AND context.`work_order_status` = 'FINISHED'
                THEN CEILING(work_order.`completed_quantity` / 25)
              ELSE requirement.`required_quantity` END AS `demand_quantity`, context.`work_date`
  FROM `mfg_work_order_material` requirement
  JOIN `tmp_work_order_context` context ON context.`work_order_id` = requirement.`work_order_id`
  JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = context.`work_order_id` AND work_order.`deleted_flag` = 0
  JOIN `mdm_material` material ON material.`material_id` = requirement.`material_id` AND material.`deleted_flag` = 0
  WHERE requirement.`deleted_flag` = 0 AND context.`work_order_status` IN ('FINISHED', 'RUNNING')
) demand_base;

DROP TEMPORARY TABLE IF EXISTS `tmp_batch_supply_interval`;
CREATE TEMPORARY TABLE `tmp_batch_supply_interval` AS
SELECT supply_base.*,
       SUM(supply_base.`supply_quantity`) OVER (
         PARTITION BY supply_base.`material_id`
         ORDER BY supply_base.`received_date`, supply_base.`batch_id`
       ) AS `supply_end`,
       SUM(supply_base.`supply_quantity`) OVER (
         PARTITION BY supply_base.`material_id`
         ORDER BY supply_base.`received_date`, supply_base.`batch_id`
       ) - supply_base.`supply_quantity` AS `supply_start`
FROM (
  SELECT detail.`material_id`, detail.`batch_id`, detail.`inbound_quantity` AS `supply_quantity`, batch_record.`received_date`
  FROM `wms_inbound_detail` detail
  JOIN `wms_material_batch` batch_record ON batch_record.`batch_id` = detail.`batch_id`
   AND batch_record.`source_order_type` = 'PURCHASE_RECEIPT' AND batch_record.`deleted_flag` = 0
  WHERE detail.`deleted_flag` = 0
) supply_base;

DROP TEMPORARY TABLE IF EXISTS `tmp_issue_allocation_base`;
CREATE TEMPORARY TABLE `tmp_issue_allocation_base` AS
SELECT allocation_source.`requirement_id`, allocation_source.`work_order_id`, allocation_source.`material_id`,
       allocation_source.`batch_id`, allocation_source.`received_date`, allocation_source.`allocated_quantity`
FROM (
  SELECT demand.`requirement_id`, demand.`work_order_id`, demand.`material_id`, supply.`batch_id`, supply.`received_date`,
         GREATEST(0, LEAST(demand.`demand_end`, supply.`supply_end`)
           - GREATEST(demand.`demand_start`, supply.`supply_start`)) AS `allocated_quantity`
  FROM `tmp_issue_demand_interval` demand
  JOIN `tmp_batch_supply_interval` supply ON supply.`material_id` = demand.`material_id`
   AND demand.`demand_start` < supply.`supply_end` AND demand.`demand_end` > supply.`supply_start`
) allocation_source
WHERE allocation_source.`allocated_quantity` > 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_issue_allocation`;
CREATE TEMPORARY TABLE `tmp_issue_allocation` AS
SELECT allocation_base.*,
       ROW_NUMBER() OVER (PARTITION BY allocation_base.`work_order_id`
                          ORDER BY allocation_base.`material_id`, allocation_base.`received_date`, allocation_base.`batch_id`) AS `allocation_seq`
FROM `tmp_issue_allocation_base` allocation_base;

-- 完工工单按实际箱数参与 FIFO；含损耗向上取整后多领的完整外箱在同日退回。
DROP TEMPORARY TABLE IF EXISTS `tmp_carton_extra_target`;
CREATE TEMPORARY TABLE `tmp_carton_extra_target` AS
SELECT ranked_allocation.`requirement_id`, ranked_allocation.`batch_id`, ranked_allocation.`extra_quantity`
FROM (
  SELECT allocation.`requirement_id`, allocation.`batch_id`,
         requirement.`required_quantity` - SUM(allocation.`allocated_quantity`) OVER (
           PARTITION BY allocation.`requirement_id`
         ) AS `extra_quantity`,
         ROW_NUMBER() OVER (PARTITION BY allocation.`requirement_id`
                            ORDER BY allocation.`received_date` DESC, allocation.`batch_id` DESC) AS `candidate_rank`
  FROM `tmp_issue_allocation` allocation
  JOIN `mfg_work_order_material` requirement ON requirement.`requirement_id` = allocation.`requirement_id`
   AND requirement.`deleted_flag` = 0
  JOIN `mdm_material` material ON material.`material_id` = allocation.`material_id`
   AND material.`material_code` LIKE 'PKG-CARTON-%' AND material.`deleted_flag` = 0
  JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = allocation.`work_order_id`
   AND work_order.`status` = 'FINISHED' AND work_order.`deleted_flag` = 0
) ranked_allocation
WHERE ranked_allocation.`candidate_rank` = 1 AND ranked_allocation.`extra_quantity` > 0;

UPDATE `tmp_issue_allocation` allocation
JOIN `tmp_carton_extra_target` extra_record
  ON extra_record.`requirement_id` = allocation.`requirement_id` AND extra_record.`batch_id` = allocation.`batch_id`
SET allocation.`allocated_quantity` = allocation.`allocated_quantity` + extra_record.`extra_quantity`;

INSERT INTO `wms_outbound_order`
  (`outbound_no`, `outbound_type`, `warehouse_id`, `work_order_id`, `applicant_id`, `audit_status`, `outbound_time`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('OB-MI-', context.`work_order_no`), 'MATERIAL_ISSUE', @raw_warehouse_id, context.`work_order_id`,
       team.`leader_id`, 'APPROVED', TIMESTAMP(context.`work_date`, '07:40:00'),
       CONCAT(@transaction_marker, ': FIFO生产领料'), @transaction_operator_id,
       TIMESTAMP(DATE_SUB(context.`work_date`, INTERVAL 1 DAY), '16:10:00'), @transaction_operator_id,
       TIMESTAMP(context.`work_date`, '07:40:00')
FROM `tmp_work_order_context` context
JOIN `mfg_dispatch_order` dispatch_order ON dispatch_order.`work_order_id` = context.`work_order_id` AND dispatch_order.`deleted_flag` = 0
JOIN `org_team` team ON team.`team_id` = dispatch_order.`team_id` AND team.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND context.`work_order_status` IN ('FINISHED', 'RUNNING');

INSERT INTO `wms_outbound_detail`
  (`outbound_id`, `material_id`, `batch_id`, `location_id`, `outbound_quantity`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT outbound_order.`outbound_id`, allocation.`material_id`, allocation.`batch_id`, @raw_location_id,
       allocation.`allocated_quantity`, CONCAT(@transaction_marker, ': FIFO批次分配'),
       @transaction_operator_id, outbound_order.`created_time`, @transaction_operator_id, outbound_order.`outbound_time`
FROM `tmp_issue_allocation` allocation
JOIN `tmp_work_order_context` context ON context.`work_order_id` = allocation.`work_order_id`
JOIN `wms_outbound_order` outbound_order ON outbound_order.`outbound_no` = CONCAT('OB-MI-', context.`work_order_no`)
 AND outbound_order.`deleted_flag` = 0
WHERE @seed_enabled = 1;

UPDATE `mfg_work_order_material` requirement
JOIN (
  SELECT `requirement_id`, SUM(`allocated_quantity`) AS `issued_quantity`
  FROM `tmp_issue_allocation` GROUP BY `requirement_id`
) allocation ON allocation.`requirement_id` = requirement.`requirement_id`
SET requirement.`issued_quantity` = allocation.`issued_quantity`,
    requirement.`shortage_quantity` = GREATEST(requirement.`required_quantity` - allocation.`issued_quantity`, 0),
    requirement.`status` = IF(allocation.`issued_quantity` >= requirement.`required_quantity`, 'ISSUED', 'SHORTAGE'),
    requirement.`updated_by` = @transaction_operator_id,
    requirement.`updated_time` = (SELECT `outbound_time` FROM `wms_outbound_order`
      WHERE `work_order_id` = requirement.`work_order_id` AND `outbound_type` = 'MATERIAL_ISSUE' AND `deleted_flag` = 0 LIMIT 1)
WHERE @seed_enabled = 1;

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `created_by`, `created_time`)
SELECT CONCAT('MV-MI-', LPAD(detail.`detail_id`, 6, '0')), 'OUTBOUND', 'OUTBOUND_ORDER', detail.`outbound_id`,
       detail.`material_id`, detail.`batch_id`, detail.`outbound_quantity`, 'OUT', outbound_order.`outbound_time`,
       CONCAT(@transaction_marker, ': 生产领料出库'), @transaction_operator_id, outbound_order.`outbound_time`
FROM `wms_outbound_detail` detail
JOIN `wms_outbound_order` outbound_order ON outbound_order.`outbound_id` = detail.`outbound_id`
 AND outbound_order.`outbound_type` = 'MATERIAL_ISSUE' AND outbound_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND detail.`deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_return_candidate`;
CREATE TEMPORARY TABLE `tmp_return_candidate` AS
SELECT return_base.*,
       ROW_NUMBER() OVER (PARTITION BY return_base.`work_order_id`
                          ORDER BY return_base.`received_date` DESC, return_base.`batch_id` DESC) AS `candidate_rank`
FROM (
  SELECT context.`work_order_id`, context.`work_order_no`, allocation.`material_id`, allocation.`batch_id`,
         allocation.`received_date`, requirement.`required_quantity`,
         requirement.`required_quantity` - CEILING(work_order.`completed_quantity` / 25) AS `return_quantity`
  FROM `tmp_work_order_context` context
  JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = context.`work_order_id`
  JOIN `mfg_work_order_material` requirement ON requirement.`work_order_id` = context.`work_order_id` AND requirement.`deleted_flag` = 0
  JOIN `mdm_material` material ON material.`material_id` = requirement.`material_id`
   AND material.`material_code` LIKE 'PKG-CARTON-%' AND material.`deleted_flag` = 0
  JOIN `tmp_issue_allocation` allocation ON allocation.`requirement_id` = requirement.`requirement_id`
  WHERE context.`work_order_status` = 'FINISHED'
) return_base
WHERE return_base.`return_quantity` > 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_return_seed`;
CREATE TEMPORARY TABLE `tmp_return_seed` AS
SELECT `work_order_id`, `work_order_no`, `material_id`, `batch_id`, `return_quantity`
FROM `tmp_return_candidate` WHERE `candidate_rank` = 1;

INSERT INTO `wms_return_order`
  (`return_no`, `return_type`, `work_order_id`, `applicant_id`, `audit_status`, `return_time`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('RT-', seed.`work_order_no`), 'MATERIAL_RETURN', seed.`work_order_id`, team.`leader_id`, 'APPROVED',
       TIMESTAMP(context.`work_date`, '17:00:00'), CONCAT(@transaction_marker, ': 完整未用外箱退回原批次'),
       @transaction_operator_id, TIMESTAMP(context.`work_date`, '16:40:00'), @transaction_operator_id,
       TIMESTAMP(context.`work_date`, '17:00:00')
FROM `tmp_return_seed` seed
JOIN `tmp_work_order_context` context ON context.`work_order_id` = seed.`work_order_id`
JOIN `mfg_dispatch_order` dispatch_order ON dispatch_order.`work_order_id` = seed.`work_order_id` AND dispatch_order.`deleted_flag` = 0
JOIN `org_team` team ON team.`team_id` = dispatch_order.`team_id` AND team.`deleted_flag` = 0
WHERE @seed_enabled = 1;

INSERT INTO `wms_return_detail`
  (`return_id`, `material_id`, `batch_id`, `return_quantity`, `return_reason`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT return_order.`return_id`, seed.`material_id`, seed.`batch_id`, seed.`return_quantity`,
       '整数外箱领料后的未使用余量', CONCAT(@transaction_marker, ': 可复用完整包装材料'),
       @transaction_operator_id, return_order.`created_time`, @transaction_operator_id, return_order.`return_time`
FROM `tmp_return_seed` seed
JOIN `wms_return_order` return_order ON return_order.`return_no` = CONCAT('RT-', seed.`work_order_no`)
 AND return_order.`deleted_flag` = 0
WHERE @seed_enabled = 1;

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `created_by`, `created_time`)
SELECT CONCAT('MV-RT-', LPAD(detail.`detail_id`, 6, '0')), 'RETURN', 'RETURN_ORDER', detail.`return_id`,
       detail.`material_id`, detail.`batch_id`, detail.`return_quantity`, 'IN', return_order.`return_time`,
       CONCAT(@transaction_marker, ': 生产退料入库'), @transaction_operator_id, return_order.`return_time`
FROM `wms_return_detail` detail
JOIN `wms_return_order` return_order ON return_order.`return_id` = detail.`return_id` AND return_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND detail.`deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_usage_demand`;
CREATE TEMPORARY TABLE `tmp_usage_demand` AS
SELECT requirement.`work_order_id`, requirement.`material_id`,
       CASE WHEN material.`material_code` LIKE 'PKG-CARTON-%'
              THEN CEILING(work_order.`completed_quantity` / 25)
            WHEN work_order.`status` = 'FINISHED' THEN requirement.`issued_quantity`
            ELSE ROUND(requirement.`issued_quantity` * work_order.`completed_quantity` / work_order.`planned_quantity`, 4)
       END AS `usage_quantity`
FROM `mfg_work_order_material` requirement
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = requirement.`work_order_id` AND work_order.`deleted_flag` = 0
JOIN `mdm_material` material ON material.`material_id` = requirement.`material_id` AND material.`deleted_flag` = 0
WHERE requirement.`deleted_flag` = 0 AND work_order.`status` IN ('FINISHED', 'RUNNING');

DROP TEMPORARY TABLE IF EXISTS `tmp_issue_for_usage`;
CREATE TEMPORARY TABLE `tmp_issue_for_usage` AS
SELECT allocation.*,
       SUM(allocation.`allocated_quantity`) OVER (
         PARTITION BY allocation.`work_order_id`, allocation.`material_id`
         ORDER BY allocation.`received_date`, allocation.`batch_id`
       ) AS `issue_end`,
       SUM(allocation.`allocated_quantity`) OVER (
         PARTITION BY allocation.`work_order_id`, allocation.`material_id`
         ORDER BY allocation.`received_date`, allocation.`batch_id`
       ) - allocation.`allocated_quantity` AS `issue_start`
FROM `tmp_issue_allocation` allocation;

DROP TEMPORARY TABLE IF EXISTS `tmp_report_allocation`;
CREATE TEMPORARY TABLE `tmp_report_allocation` AS
SELECT issue_record.`work_order_id`, issue_record.`material_id`, issue_record.`batch_id`,
       GREATEST(0, LEAST(usage_record.`usage_quantity`, issue_record.`issue_end`) - issue_record.`issue_start`) AS `used_quantity`
FROM `tmp_usage_demand` usage_record
JOIN `tmp_issue_for_usage` issue_record ON issue_record.`work_order_id` = usage_record.`work_order_id`
 AND issue_record.`material_id` = usage_record.`material_id`
WHERE issue_record.`issue_start` < usage_record.`usage_quantity`;

INSERT INTO `mfg_report_material`
  (`report_id`, `material_id`, `batch_id`, `used_quantity`, `scrap_quantity`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT work_report.`report_id`, allocation.`material_id`, allocation.`batch_id`, allocation.`used_quantity`,
       CASE WHEN material.`material_code` LIKE 'PKG-CARTON-%' THEN 0
            ELSE ROUND(allocation.`used_quantity` * bom_item.`loss_rate` / (1 + bom_item.`loss_rate`), 4) END,
       CONCAT(@transaction_marker, ': 实际批次用料，不重复扣库存'), @transaction_operator_id,
       work_report.`report_time`, @transaction_operator_id, work_report.`report_time`
FROM `tmp_report_allocation` allocation
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = allocation.`work_order_id` AND work_order.`deleted_flag` = 0
JOIN `mdm_bom_item` bom_item ON bom_item.`bom_id` = work_order.`bom_id`
 AND bom_item.`material_id` = allocation.`material_id` AND bom_item.`deleted_flag` = 0
JOIN `mdm_material` material ON material.`material_id` = allocation.`material_id` AND material.`deleted_flag` = 0
JOIN `mfg_task` task ON task.`work_order_id` = work_order.`work_order_id` AND task.`deleted_flag` = 0
JOIN `process_route_item` route_item ON route_item.`route_id` = work_order.`route_id`
 AND route_item.`operation_id` = task.`operation_id` AND route_item.`deleted_flag` = 0
 AND route_item.`sequence_no` = IF(work_order.`status` = 'FINISHED', 70, 50)
JOIN `mfg_work_report` work_report ON work_report.`task_id` = task.`task_id`
 AND work_report.`audit_status` = 'APPROVED' AND work_report.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND allocation.`used_quantity` > 0;

-- 4. 产品批次、过程/最终检验和绕线张力异常闭环。
DROP TEMPORARY TABLE IF EXISTS `tmp_product_map`;
CREATE TEMPORARY TABLE `tmp_product_map` (
  `product_code` VARCHAR(64) PRIMARY KEY,
  `lot_short_code` VARCHAR(24) NOT NULL,
  `speed_grade` INT NOT NULL,
  `weight_center` DECIMAL(5,2) NOT NULL,
  `flight_limit` INT NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_product_map` VALUES
  ('PROD-LNX-T300-76', 'LNX-T30076', 76, 4.90, 500),
  ('PROD-LNX-T300-77', 'LNX-T30077', 77, 5.00, 500),
  ('PROD-LNX-M700-77', 'LNX-M70077', 77, 5.00, 300),
  ('PROD-LNX-M700-78', 'LNX-M70078', 78, 5.10, 300),
  ('PROD-OEM-XY-C500-77', 'OEM-XY-C50077', 77, 5.00, 400);

INSERT INTO `trace_product_lot`
  (`lot_no`, `product_id`, `work_order_id`, `batch_quantity`, `qualified_quantity`, `defect_quantity`, `status`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('LOT-', product_map.`lot_short_code`, '-', DATE_FORMAT(context.`work_date`, '%Y%m%d'), '-',
              LPAD(context.`order_seq`, 3, '0'), context.`segment_no`),
       context.`product_id`, context.`work_order_id`, work_order.`completed_quantity` +
         IF(context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03', 1, 0),
       work_order.`completed_quantity`,
       IF(context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03', 1, 0),
       IF(context.`work_order_status` = 'FINISHED', 'STORED', 'RUNNING'),
       CONCAT(@transaction_marker, ': 工单生产批次'), @transaction_operator_id,
       TIMESTAMP(context.`work_date`, '08:00:00'), @transaction_operator_id,
       TIMESTAMP(context.`work_date`, IF(context.`work_order_status` = 'FINISHED', '17:10:00', '13:30:00'))
FROM `tmp_work_order_context` context
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = context.`work_order_id`
JOIN `tmp_product_map` product_map ON product_map.`product_code` = context.`product_code`
WHERE @seed_enabled = 1 AND context.`work_order_status` IN ('FINISHED', 'RUNNING');

INSERT INTO `wms_material_batch`
  (`batch_no`, `material_id`, `production_date`, `received_date`, `quality_status`, `status`,
   `source_order_type`, `source_order_id`, `source_product_lot_id`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('FG-', product_lot.`lot_no`), product.`material_id`, context.`work_date`, context.`work_date`,
       'PASS', 'ENABLE', 'PRODUCTION', context.`work_order_id`, product_lot.`product_lot_id`,
       CONCAT(@transaction_marker, ': 最终检验合格成品批次'), @transaction_operator_id,
       TIMESTAMP(context.`work_date`, '16:35:00'), @transaction_operator_id, TIMESTAMP(context.`work_date`, '17:20:00')
FROM `trace_product_lot` product_lot
JOIN `tmp_work_order_context` context ON context.`work_order_id` = product_lot.`work_order_id`
JOIN `mdm_product` product ON product.`product_id` = product_lot.`product_id` AND product.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND context.`work_order_status` = 'FINISHED' AND product_lot.`deleted_flag` = 0;

INSERT INTO `qms_inspection_order`
  (`inspection_no`, `inspection_type`, `scheme_id`, `work_order_id`, `task_id`, `report_id`, `batch_id`,
   `inspector_id`, `inspection_result`, `status`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('IPQC-', context.`work_order_no`), 'PROCESS', scheme.`scheme_id`, context.`work_order_id`,
       task.`task_id`, report.`report_id`, finished_batch.`batch_id`, @quality_employee_id,
       IF(context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03', 'FAIL', 'PASS'),
       'FINISHED', CONCAT(@transaction_marker, ': 过程重量、速度和飞行抽检'), @transaction_operator_id,
       TIMESTAMP(context.`work_date`, '12:10:00'), @transaction_operator_id, TIMESTAMP(context.`work_date`, '12:30:00')
FROM `tmp_work_order_context` context
JOIN `mdm_product` product ON product.`product_id` = context.`product_id` AND product.`deleted_flag` = 0
JOIN `qms_inspection_scheme` scheme ON scheme.`product_id` = product.`product_id`
 AND scheme.`inspection_type` = 'PROCESS' AND scheme.`deleted_flag` = 0
JOIN `mfg_task` task ON task.`work_order_id` = context.`work_order_id` AND task.`deleted_flag` = 0
JOIN `process_route_item` route_item ON route_item.`route_id` = context.`route_id`
 AND route_item.`operation_id` = task.`operation_id` AND route_item.`deleted_flag` = 0
 AND route_item.`sequence_no` = IF(context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03', 40,
                                  IF(context.`work_order_status` = 'FINISHED', 60, 50))
JOIN `mfg_work_report` report ON report.`task_id` = task.`task_id` AND report.`audit_status` = 'APPROVED' AND report.`deleted_flag` = 0
LEFT JOIN `trace_product_lot` product_lot ON product_lot.`work_order_id` = context.`work_order_id` AND product_lot.`deleted_flag` = 0
LEFT JOIN `wms_material_batch` finished_batch ON finished_batch.`source_product_lot_id` = product_lot.`product_lot_id`
 AND finished_batch.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND context.`work_order_status` IN ('FINISHED', 'RUNNING');

INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `defect_description`, `remark`,
   `created_by`, `created_time`)
SELECT inspection_order.`inspection_id`, item.`item_id`,
       CASE item.`item_code`
         WHEN 'QI-FINISHED-WEIGHT' THEN CAST(ROUND(product_map.`weight_center`
              + (CAST(MOD(context.`work_order_id`, 3) AS SIGNED) - 1) * 0.01, 2) AS CHAR)
         WHEN 'QI-SPEED-GRADE' THEN CAST(product_map.`speed_grade` AS CHAR)
         WHEN 'QI-FLIGHT-STABILITY' THEN CAST(IF(context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03',
              520, product_map.`flight_limit` - 30 - MOD(context.`work_order_id`, 4) * 15) AS CHAR)
       END,
       IF(context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03'
          AND item.`item_code` = 'QI-FLIGHT-STABILITY', 'FAIL', 'PASS'),
       IF(context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03'
          AND item.`item_code` = 'QI-FLIGHT-STABILITY', '绕线张力4.7N导致落点偏差超限', NULL),
       CONCAT(@transaction_marker, ': IPQC实测值'), @transaction_operator_id, inspection_order.`updated_time`
FROM `qms_inspection_order` inspection_order
JOIN `tmp_work_order_context` context ON inspection_order.`inspection_no` = CONCAT('IPQC-', context.`work_order_no`)
JOIN `tmp_product_map` product_map ON product_map.`product_code` = context.`product_code`
JOIN `qms_inspection_scheme_item` scheme_item ON scheme_item.`scheme_id` = inspection_order.`scheme_id`
 AND scheme_item.`deleted_flag` = 0
JOIN `qms_inspection_item` item ON item.`item_id` = scheme_item.`item_id` AND item.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND inspection_order.`deleted_flag` = 0;

INSERT INTO `qms_inspection_order`
  (`inspection_no`, `inspection_type`, `scheme_id`, `work_order_id`, `task_id`, `report_id`, `batch_id`,
   `inspector_id`, `inspection_result`, `status`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('IPQC-RC-', context.`work_order_no`), 'PROCESS', scheme.`scheme_id`, context.`work_order_id`,
       task.`task_id`, report.`report_id`, finished_batch.`batch_id`, @quality_employee_id, 'PASS', 'FINISHED',
       CONCAT(@transaction_marker, ': 绕线机校准和返工后复检，23筒合格、1筒报废'),
       @transaction_operator_id, TIMESTAMP(context.`work_date`, '14:10:00'), @transaction_operator_id,
       TIMESTAMP(context.`work_date`, '14:30:00')
FROM `tmp_work_order_context` context
JOIN `qms_inspection_scheme` scheme ON scheme.`product_id` = context.`product_id`
 AND scheme.`inspection_type` = 'PROCESS' AND scheme.`deleted_flag` = 0
JOIN `mfg_task` task ON task.`work_order_id` = context.`work_order_id` AND task.`deleted_flag` = 0
JOIN `process_route_item` route_item ON route_item.`route_id` = context.`route_id`
 AND route_item.`operation_id` = task.`operation_id` AND route_item.`sequence_no` = 40 AND route_item.`deleted_flag` = 0
JOIN `mfg_work_report` report ON report.`task_id` = task.`task_id` AND report.`deleted_flag` = 0
JOIN `trace_product_lot` product_lot ON product_lot.`work_order_id` = context.`work_order_id` AND product_lot.`deleted_flag` = 0
JOIN `wms_material_batch` finished_batch ON finished_batch.`source_product_lot_id` = product_lot.`product_lot_id`
 AND finished_batch.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03';

INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `remark`, `created_by`, `created_time`)
SELECT inspection_order.`inspection_id`, item.`item_id`,
       CASE item.`item_code` WHEN 'QI-FINISHED-WEIGHT' THEN '5.01'
         WHEN 'QI-SPEED-GRADE' THEN '77' WHEN 'QI-FLIGHT-STABILITY' THEN '430' END,
       'PASS', CONCAT(@transaction_marker, ': 返工复检合格'), @transaction_operator_id, inspection_order.`updated_time`
FROM `qms_inspection_order` inspection_order
JOIN `qms_inspection_scheme_item` scheme_item ON scheme_item.`scheme_id` = inspection_order.`scheme_id`
 AND scheme_item.`deleted_flag` = 0
JOIN `qms_inspection_item` item ON item.`item_id` = scheme_item.`item_id` AND item.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND inspection_order.`inspection_no` LIKE 'IPQC-RC-%' AND inspection_order.`deleted_flag` = 0;

INSERT INTO `qms_nonconforming_order`
  (`ncr_no`, `inspection_id`, `product_lot_id`, `defect_reason`, `disposition_type`, `approval_status`, `status`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT 'NCR-20260703-001', inspection_order.`inspection_id`, product_lot.`product_lot_id`,
       '绕线张力短时达到4.7N，24筒隔离', 'REWORK', 'APPROVED', 'CLOSED',
       CONCAT(@transaction_marker, ': 重新校准并返工，23筒复检合格、1筒报废'), @transaction_operator_id,
       '2026-07-03 12:35:00', @transaction_operator_id, '2026-07-03 14:35:00'
FROM `tmp_work_order_context` context
JOIN `qms_inspection_order` inspection_order ON inspection_order.`inspection_no` = CONCAT('IPQC-', context.`work_order_no`)
 AND inspection_order.`inspection_result` = 'FAIL' AND inspection_order.`deleted_flag` = 0
JOIN `trace_product_lot` product_lot ON product_lot.`work_order_id` = context.`work_order_id` AND product_lot.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03';

INSERT INTO `qms_nonconforming_detail`
  (`ncr_id`, `inspection_result_id`, `defect_quantity`, `final_disposition`, `recheck_result`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT ncr.`ncr_id`, inspection_result.`result_id`, 24, 'REWORK', 'PASS',
       CONCAT(@transaction_marker, ': 23筒恢复合格、1筒报废'), @transaction_operator_id,
       '2026-07-03 12:40:00', @transaction_operator_id, '2026-07-03 14:35:00'
FROM `qms_nonconforming_order` ncr
JOIN `qms_inspection_result` inspection_result ON inspection_result.`inspection_id` = ncr.`inspection_id`
 AND inspection_result.`result` = 'FAIL' AND inspection_result.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND ncr.`ncr_no` = 'NCR-20260703-001' AND ncr.`deleted_flag` = 0;

INSERT INTO `qms_inspection_order`
  (`inspection_no`, `inspection_type`, `scheme_id`, `work_order_id`, `task_id`, `report_id`, `batch_id`,
   `inspector_id`, `inspection_result`, `status`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('FQC-', context.`work_order_no`), 'FINAL', scheme.`scheme_id`, context.`work_order_id`,
       task.`task_id`, report.`report_id`, finished_batch.`batch_id`, @quality_employee_id, 'PASS', 'FINISHED',
       CONCAT(@transaction_marker, ': 最终外观与包装放行'), @transaction_operator_id,
       TIMESTAMP(context.`work_date`, '16:35:00'), @transaction_operator_id, TIMESTAMP(context.`work_date`, '16:50:00')
FROM `tmp_work_order_context` context
JOIN `qms_inspection_scheme` scheme ON scheme.`product_id` = context.`product_id`
 AND scheme.`inspection_type` = 'FINAL' AND scheme.`deleted_flag` = 0
JOIN `mfg_task` task ON task.`work_order_id` = context.`work_order_id` AND task.`deleted_flag` = 0
JOIN `process_route_item` route_item ON route_item.`route_id` = context.`route_id`
 AND route_item.`operation_id` = task.`operation_id` AND route_item.`sequence_no` = 70 AND route_item.`deleted_flag` = 0
JOIN `mfg_work_report` report ON report.`task_id` = task.`task_id` AND report.`deleted_flag` = 0
JOIN `trace_product_lot` product_lot ON product_lot.`work_order_id` = context.`work_order_id` AND product_lot.`deleted_flag` = 0
JOIN `wms_material_batch` finished_batch ON finished_batch.`source_product_lot_id` = product_lot.`product_lot_id`
 AND finished_batch.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND context.`work_order_status` = 'FINISHED';

INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `remark`, `created_by`, `created_time`)
SELECT inspection_order.`inspection_id`, item.`item_id`, '合格', 'PASS',
       CONCAT(@transaction_marker, ': 成品外观、球筒和标签检查合格'), @transaction_operator_id, inspection_order.`updated_time`
FROM `qms_inspection_order` inspection_order
JOIN `qms_inspection_scheme_item` scheme_item ON scheme_item.`scheme_id` = inspection_order.`scheme_id`
 AND scheme_item.`deleted_flag` = 0
JOIN `qms_inspection_item` item ON item.`item_id` = scheme_item.`item_id` AND item.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND inspection_order.`inspection_no` LIKE 'FQC-%' AND inspection_order.`deleted_flag` = 0;

-- 5. 最终检验合格成品入库和按产品 FIFO 销售出库。
INSERT INTO `wms_inbound_order`
  (`inbound_no`, `inbound_type`, `warehouse_id`, `source_order_type`, `source_order_id`, `audit_status`, `inbound_time`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('IB-FG-', context.`work_order_no`), 'PRODUCTION', @fg_warehouse_id, 'WORK_ORDER', context.`work_order_id`,
       'APPROVED', TIMESTAMP(context.`work_date`, '17:20:00'), CONCAT(@transaction_marker, ': FQC合格成品入库'),
       @transaction_operator_id, TIMESTAMP(context.`work_date`, '17:00:00'), @transaction_operator_id,
       TIMESTAMP(context.`work_date`, '17:20:00')
FROM `tmp_work_order_context` context
WHERE @seed_enabled = 1 AND context.`work_order_status` = 'FINISHED';

INSERT INTO `wms_inbound_detail`
  (`inbound_id`, `material_id`, `batch_id`, `location_id`, `inbound_quantity`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT inbound_order.`inbound_id`, product.`material_id`, finished_batch.`batch_id`, @fg_location_id,
       product_lot.`qualified_quantity`, CONCAT(@transaction_marker, ': 合格数量入成品库'),
       @transaction_operator_id, inbound_order.`created_time`, @transaction_operator_id, inbound_order.`inbound_time`
FROM `tmp_work_order_context` context
JOIN `wms_inbound_order` inbound_order ON inbound_order.`inbound_no` = CONCAT('IB-FG-', context.`work_order_no`)
 AND inbound_order.`deleted_flag` = 0
JOIN `trace_product_lot` product_lot ON product_lot.`work_order_id` = context.`work_order_id` AND product_lot.`deleted_flag` = 0
JOIN `mdm_product` product ON product.`product_id` = context.`product_id` AND product.`deleted_flag` = 0
JOIN `wms_material_batch` finished_batch ON finished_batch.`source_product_lot_id` = product_lot.`product_lot_id`
 AND finished_batch.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND context.`work_order_status` = 'FINISHED';

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `created_by`, `created_time`)
SELECT CONCAT('MV-FG-', LPAD(detail.`detail_id`, 6, '0')), 'INBOUND', 'INBOUND_ORDER', detail.`inbound_id`,
       detail.`material_id`, detail.`batch_id`, detail.`inbound_quantity`, 'IN', inbound_order.`inbound_time`,
       CONCAT(@transaction_marker, ': 成品入库'), @transaction_operator_id, inbound_order.`inbound_time`
FROM `wms_inbound_detail` detail
JOIN `wms_inbound_order` inbound_order ON inbound_order.`inbound_id` = detail.`inbound_id`
 AND inbound_order.`inbound_type` = 'PRODUCTION' AND inbound_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND detail.`deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_sales_target`;
CREATE TEMPORARY TABLE `tmp_sales_target` (
  `product_code` VARCHAR(64) PRIMARY KEY,
  `shipment_quantity` DECIMAL(18,4) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_sales_target` VALUES
  ('PROD-LNX-T300-76', 800),
  ('PROD-LNX-T300-77', 4800),
  ('PROD-LNX-M700-77', 5500),
  ('PROD-LNX-M700-78', 800),
  ('PROD-OEM-XY-C500-77', 1550);

DROP TEMPORARY TABLE IF EXISTS `tmp_finished_supply_interval`;
CREATE TEMPORARY TABLE `tmp_finished_supply_interval` AS
SELECT supply_base.*,
       SUM(supply_base.`supply_quantity`) OVER (
         PARTITION BY supply_base.`product_code`
         ORDER BY supply_base.`production_date`, supply_base.`batch_id`
       ) AS `supply_end`,
       SUM(supply_base.`supply_quantity`) OVER (
         PARTITION BY supply_base.`product_code`
         ORDER BY supply_base.`production_date`, supply_base.`batch_id`
       ) - supply_base.`supply_quantity` AS `supply_start`
FROM (
  SELECT product.`product_code`, detail.`material_id`, detail.`batch_id`, detail.`inbound_quantity` AS `supply_quantity`,
         batch_record.`production_date`
  FROM `wms_inbound_detail` detail
  JOIN `wms_inbound_order` inbound_order ON inbound_order.`inbound_id` = detail.`inbound_id`
   AND inbound_order.`inbound_type` = 'PRODUCTION' AND inbound_order.`deleted_flag` = 0
  JOIN `wms_material_batch` batch_record ON batch_record.`batch_id` = detail.`batch_id` AND batch_record.`deleted_flag` = 0
  JOIN `mdm_product` product ON product.`material_id` = detail.`material_id` AND product.`deleted_flag` = 0
  WHERE detail.`deleted_flag` = 0
) supply_base;

DROP TEMPORARY TABLE IF EXISTS `tmp_sales_allocation`;
CREATE TEMPORARY TABLE `tmp_sales_allocation` AS
SELECT target.`product_code`, supply.`material_id`, supply.`batch_id`,
       GREATEST(0, LEAST(target.`shipment_quantity`, supply.`supply_end`) - supply.`supply_start`) AS `shipment_quantity`
FROM `tmp_sales_target` target
JOIN `tmp_finished_supply_interval` supply ON supply.`product_code` = target.`product_code`
WHERE supply.`supply_start` < target.`shipment_quantity`;

INSERT INTO `wms_outbound_order`
  (`outbound_no`, `outbound_type`, `warehouse_id`, `applicant_id`, `audit_status`, `outbound_time`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT CONCAT('OB-SALE-', REPLACE(REPLACE(target.`product_code`, 'PROD-', ''), 'OEM-', '')),
       'SALE', @fg_warehouse_id, @warehouse_employee_id, 'APPROVED', '2026-07-13 17:40:00',
       CONCAT(@transaction_marker, ': 30天客户订单销售出库'), @transaction_operator_id,
       '2026-07-13 16:40:00', @transaction_operator_id, '2026-07-13 17:40:00'
FROM `tmp_sales_target` target
WHERE @seed_enabled = 1;

INSERT INTO `wms_outbound_detail`
  (`outbound_id`, `material_id`, `batch_id`, `location_id`, `outbound_quantity`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT outbound_order.`outbound_id`, allocation.`material_id`, allocation.`batch_id`, @fg_location_id,
       allocation.`shipment_quantity`, CONCAT(@transaction_marker, ': 成品FIFO发货'),
       @transaction_operator_id, outbound_order.`created_time`, @transaction_operator_id, outbound_order.`outbound_time`
FROM `tmp_sales_allocation` allocation
JOIN `wms_outbound_order` outbound_order
  ON outbound_order.`outbound_no` = CONCAT('OB-SALE-', REPLACE(REPLACE(allocation.`product_code`, 'PROD-', ''), 'OEM-', ''))
 AND outbound_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND allocation.`shipment_quantity` > 0;

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `created_by`, `created_time`)
SELECT CONCAT('MV-SALE-', LPAD(detail.`detail_id`, 6, '0')), 'OUTBOUND', 'OUTBOUND_ORDER', detail.`outbound_id`,
       detail.`material_id`, detail.`batch_id`, detail.`outbound_quantity`, 'OUT', outbound_order.`outbound_time`,
       CONCAT(@transaction_marker, ': 销售出库'), @transaction_operator_id, outbound_order.`outbound_time`
FROM `wms_outbound_detail` detail
JOIN `wms_outbound_order` outbound_order ON outbound_order.`outbound_id` = detail.`outbound_id`
 AND outbound_order.`outbound_type` = 'SALE' AND outbound_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND detail.`deleted_flag` = 0;

-- 6. 8 种物料循环盘点；T300 77 标签盘亏 6 张。
DROP TEMPORARY TABLE IF EXISTS `tmp_balance_before_stocktake`;
CREATE TEMPORARY TABLE `tmp_balance_before_stocktake` AS
SELECT movement.`material_id`, movement.`batch_id`,
       SUM(IF(movement.`direction` = 'IN', movement.`quantity`, -movement.`quantity`)) AS `book_quantity`
FROM `wms_stock_movement` movement
WHERE movement.`deleted_flag` = 0
GROUP BY movement.`material_id`, movement.`batch_id`;

DROP TEMPORARY TABLE IF EXISTS `tmp_stocktake_candidate`;
CREATE TEMPORARY TABLE `tmp_stocktake_candidate` AS
SELECT material.`material_code`, balance.`material_id`, balance.`batch_id`, balance.`book_quantity`,
       ROW_NUMBER() OVER (PARTITION BY material.`material_code`
                          ORDER BY batch_record.`received_date` DESC, balance.`batch_id` DESC) AS `candidate_rank`
FROM `tmp_balance_before_stocktake` balance
JOIN `mdm_material` material ON material.`material_id` = balance.`material_id` AND material.`deleted_flag` = 0
JOIN `wms_material_batch` batch_record ON batch_record.`batch_id` = balance.`batch_id` AND batch_record.`deleted_flag` = 0
WHERE balance.`book_quantity` > 0 AND material.`material_code` IN (
  'RM-FEATHER-DUCK-A', 'RM-FEATHER-GOOSE-A', 'RM-CORK-COMPOSITE-77', 'RM-CORK-NATURAL-77',
  'AUX-THREAD-NYLON-07', 'AUX-ADHESIVE-RESIN', 'PKG-TUBE-LNX-T300', 'PKG-LABEL-LNX-T300-77'
);

INSERT INTO `wms_stocktake_order`
  (`stocktake_no`, `warehouse_id`, `stocktake_type`, `stocktake_status`, `started_time`, `finished_time`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT 'ST-20260713-001', @raw_warehouse_id, 'CYCLE', 'FINISHED', '2026-07-13 18:00:00', '2026-07-13 19:00:00',
       CONCAT(@transaction_marker, ': 八种常用物料循环盘点'), @transaction_operator_id,
       '2026-07-13 17:50:00', @transaction_operator_id, '2026-07-13 19:00:00'
WHERE @seed_enabled = 1;

INSERT INTO `wms_stocktake_detail`
  (`stocktake_id`, `material_id`, `batch_id`, `location_id`, `book_quantity`, `actual_quantity`,
   `difference_quantity`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT stocktake_order.`stocktake_id`, candidate.`material_id`, candidate.`batch_id`, @raw_location_id,
       candidate.`book_quantity`, candidate.`book_quantity` - IF(candidate.`material_code` = 'PKG-LABEL-LNX-T300-77', 6, 0),
       -IF(candidate.`material_code` = 'PKG-LABEL-LNX-T300-77', 6, 0),
       CONCAT(@transaction_marker, ': ', IF(candidate.`material_code` = 'PKG-LABEL-LNX-T300-77',
         '打印调试和破损未及时登记', '账实一致')), @transaction_operator_id,
       stocktake_order.`started_time`, @transaction_operator_id, stocktake_order.`finished_time`
FROM `tmp_stocktake_candidate` candidate
JOIN `wms_stocktake_order` stocktake_order ON stocktake_order.`stocktake_no` = 'ST-20260713-001'
 AND stocktake_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND candidate.`candidate_rank` = 1;

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `created_by`, `created_time`)
SELECT CONCAT('MV-ST-', LPAD(detail.`detail_id`, 6, '0')), 'STOCKTAKE', 'STOCKTAKE_ORDER', detail.`stocktake_id`,
       detail.`material_id`, detail.`batch_id`, ABS(detail.`difference_quantity`),
       IF(detail.`difference_quantity` > 0, 'IN', 'OUT'), stocktake_order.`finished_time`,
       CONCAT(@transaction_marker, ': 盘点差异调整'), @transaction_operator_id, stocktake_order.`finished_time`
FROM `wms_stocktake_detail` detail
JOIN `wms_stocktake_order` stocktake_order ON stocktake_order.`stocktake_id` = detail.`stocktake_id`
 AND stocktake_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND detail.`deleted_flag` = 0 AND detail.`difference_quantity` <> 0;

-- 库存余额严格由同一事务内的库存流水汇总形成。
INSERT INTO `wms_inventory`
  (`warehouse_id`, `location_id`, `material_id`, `batch_id`, `available_quantity`, `locked_quantity`,
   `inspection_quantity`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT IF(material.`material_type` = 'FINISHED', @fg_warehouse_id, @raw_warehouse_id),
       IF(material.`material_type` = 'FINISHED', @fg_location_id, @raw_location_id),
       movement.`material_id`, movement.`batch_id`,
       SUM(IF(movement.`direction` = 'IN', movement.`quantity`, -movement.`quantity`)), 0, 0,
       CONCAT(@transaction_marker, ': 由库存流水汇总的当前余额'), @transaction_operator_id,
       MIN(movement.`operation_time`), @transaction_operator_id, MAX(movement.`operation_time`)
FROM `wms_stock_movement` movement
JOIN `mdm_material` material ON material.`material_id` = movement.`material_id` AND material.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND movement.`deleted_flag` = 0
GROUP BY material.`material_type`, movement.`material_id`, movement.`batch_id`
HAVING SUM(IF(movement.`direction` = 'IN', movement.`quantity`, -movement.`quantity`)) >= 0;

-- 7. 设备点检、保养、绕线异常和包装机维修闭环。
DROP TEMPORARY TABLE IF EXISTS `tmp_working_date`;
CREATE TEMPORARY TABLE `tmp_working_date` AS
WITH RECURSIVE calendar_date AS (
  SELECT DATE('2026-06-14') AS `work_date`
  UNION ALL SELECT DATE_ADD(`work_date`, INTERVAL 1 DAY)
  FROM calendar_date WHERE `work_date` < '2026-07-13'
)
SELECT `work_date` FROM calendar_date WHERE DAYOFWEEK(`work_date`) <> 1;

INSERT INTO `eam_spot_check_plan`
  (`equipment_id`, `check_cycle`, `check_items`, `responsible_role`, `status`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT equipment.`equipment_id`, '每个生产日开班前',
       '安全防护、传感器、紧固件、润滑、气压、电气和试运行', '设备点检员', 'ENABLE',
       CONCAT(@transaction_marker, ': 开班点检计划'), @transaction_operator_id,
       '2026-06-13 09:00:00', @transaction_operator_id, '2026-06-13 09:00:00'
FROM `eam_equipment` equipment
WHERE @seed_enabled = 1 AND equipment.`deleted_flag` = 0;

INSERT INTO `eam_spot_check_record`
  (`plan_id`, `equipment_id`, `checker_id`, `check_time`, `check_result`, `abnormal_description`, `remark`,
   `created_by`, `created_time`)
SELECT plan.`plan_id`, plan.`equipment_id`, @equipment_employee_id, TIMESTAMP(work_date.`work_date`, '07:45:00'),
       'PASS', NULL, CONCAT(@transaction_marker, ': 开班点检正常'), @transaction_operator_id,
       TIMESTAMP(work_date.`work_date`, '07:45:00')
FROM `eam_spot_check_plan` plan
CROSS JOIN `tmp_working_date` work_date
WHERE @seed_enabled = 1 AND plan.`deleted_flag` = 0 AND plan.`remark` = CONCAT(@transaction_marker, ': 开班点检计划');

INSERT INTO `eam_maintenance_plan`
  (`equipment_id`, `maintenance_type`, `cycle_type`, `cycle_value`, `maintenance_content`, `responsible_id`, `status`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT equipment.`equipment_id`,
       IF(equipment.`equipment_code` LIKE '%WIND%' OR equipment.`equipment_code` LIKE '%TEST%', 'CALIBRATION', 'PREVENTIVE'),
       'MONTH', 1, '清洁、紧固、润滑、安全检查和关键参数校准', @equipment_employee_id, 'ENABLE',
       CONCAT(@transaction_marker, ': 月度预防性保养'), @transaction_operator_id,
       '2026-06-13 09:10:00', @transaction_operator_id, '2026-06-13 09:10:00'
FROM `eam_equipment` equipment
WHERE @seed_enabled = 1 AND equipment.`deleted_flag` = 0;

INSERT INTO `eam_maintenance_record`
  (`plan_id`, `equipment_id`, `maintainer_id`, `maintenance_time`, `maintenance_result`, `next_maintenance_time`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT plan.`plan_id`, plan.`equipment_id`, @equipment_employee_id,
       TIMESTAMP(DATE_ADD('2026-06-16', INTERVAL MOD(plan.`equipment_id`, 18) DAY), '17:30:00'),
       '保养完成，安全与运行参数正常',
       TIMESTAMP(DATE_ADD('2026-07-16', INTERVAL MOD(plan.`equipment_id`, 18) DAY), '17:30:00'),
       CONCAT(@transaction_marker, ': 月度保养记录'), @transaction_operator_id,
       TIMESTAMP(DATE_ADD('2026-06-16', INTERVAL MOD(plan.`equipment_id`, 18) DAY), '17:30:00'),
       @transaction_operator_id,
       TIMESTAMP(DATE_ADD('2026-06-16', INTERVAL MOD(plan.`equipment_id`, 18) DAY), '18:00:00')
FROM `eam_maintenance_plan` plan
WHERE @seed_enabled = 1 AND plan.`deleted_flag` = 0 AND plan.`remark` = CONCAT(@transaction_marker, ': 月度预防性保养');

INSERT INTO `eam_equipment_status_log`
  (`equipment_id`, `running_status`, `station_id`, `collected_time`, `description`, `remark`,
   `created_by`, `created_time`)
SELECT equipment.`equipment_id`, 'RUNNING', equipment.`station_id`, TIMESTAMP(work_date.`work_date`, '08:00:00'),
       '开班点检后投入运行', CONCAT(@transaction_marker, ': 日常运行采集'),
       @transaction_operator_id, TIMESTAMP(work_date.`work_date`, '08:00:00')
FROM `eam_equipment` equipment
CROSS JOIN `tmp_working_date` work_date
WHERE @seed_enabled = 1 AND equipment.`deleted_flag` = 0;

INSERT INTO `mfg_andon_alert`
  (`alert_no`, `line_id`, `station_id`, `triggered_by`, `alert_type`, `trigger_time`, `response_time`, `resolved_time`,
   `status`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT 'ANDON-20260703-001', plan.`line_id`, task.`station_id`, team.`leader_id`, 'QUALITY',
       '2026-07-03 11:40:00', '2026-07-03 11:43:00', '2026-07-03 11:52:00', 'CLOSED',
       CONCAT(@transaction_marker, ': 绕线张力4.7N告警，12分钟内校准恢复'), @transaction_operator_id,
       '2026-07-03 11:40:00', @transaction_operator_id, '2026-07-03 11:52:00'
FROM `tmp_work_order_context` context
JOIN `mfg_production_plan` plan ON plan.`plan_id` = context.`plan_id` AND plan.`deleted_flag` = 0
JOIN `mfg_task` task ON task.`work_order_id` = context.`work_order_id` AND task.`deleted_flag` = 0
JOIN `process_route_item` route_item ON route_item.`route_id` = context.`route_id`
 AND route_item.`operation_id` = task.`operation_id` AND route_item.`sequence_no` = 40 AND route_item.`deleted_flag` = 0
JOIN `mfg_dispatch_order` dispatch_order ON dispatch_order.`work_order_id` = context.`work_order_id` AND dispatch_order.`deleted_flag` = 0
JOIN `org_team` team ON team.`team_id` = dispatch_order.`team_id` AND team.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03';

INSERT INTO `mfg_exception`
  (`exception_no`, `exception_type`, `source_alert_id`, `task_id`, `station_id`, `reported_by`, `description`,
   `handle_result`, `status`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT 'EX-20260703-001', 'PROCESS', alert_record.`alert_id`, task.`task_id`, task.`station_id`, alert_record.`triggered_by`,
       '训练线绕线张力短时达到4.7N，隔离24筒产品',
       '绕线机重新校准；24筒返工复检，23筒合格、1筒报废', 'CLOSED',
       CONCAT(@transaction_marker, ': 过程异常已闭环'), @transaction_operator_id,
       '2026-07-03 11:42:00', @transaction_operator_id, '2026-07-03 14:35:00'
FROM `mfg_andon_alert` alert_record
JOIN `tmp_work_order_context` context ON context.`product_code` = 'PROD-LNX-T300-77' AND context.`work_date` = '2026-07-03'
JOIN `mfg_task` task ON task.`work_order_id` = context.`work_order_id` AND task.`station_id` = alert_record.`station_id`
 AND task.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND alert_record.`alert_no` = 'ANDON-20260703-001' AND alert_record.`deleted_flag` = 0
LIMIT 1;

INSERT INTO `eam_equipment_status_log`
  (`equipment_id`, `running_status`, `downtime_type`, `alert_level`, `station_id`, `collected_time`, `description`, `remark`,
   `created_by`, `created_time`)
SELECT equipment.`equipment_id`, status_seed.`running_status`, 'QUALITY_ADJUSTMENT', 'MEDIUM', equipment.`station_id`,
       status_seed.`collected_time`, status_seed.`description`, CONCAT(@transaction_marker, ': 绕线异常状态'),
       @transaction_operator_id, status_seed.`collected_time`
FROM `eam_equipment` equipment
JOIN (
  SELECT 'STOPPED' AS `running_status`, TIMESTAMP('2026-07-03 11:40:00') AS `collected_time`, '张力超限停机校准' AS `description`
  UNION ALL SELECT 'RUNNING', TIMESTAMP('2026-07-03 11:52:00'), '校准验证通过后恢复运行'
) status_seed
WHERE @seed_enabled = 1 AND equipment.`equipment_code` = 'EQ-TR-WIND-01' AND equipment.`deleted_flag` = 0;

INSERT INTO `eam_repair_order`
  (`repair_no`, `equipment_id`, `reported_by`, `reported_time`, `fault_description`, `repairer_id`, `repair_result`,
   `repair_cost`, `status`, `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT 'REPAIR-20260630-001', equipment.`equipment_id`,
       (SELECT `employee_id` FROM `org_employee` WHERE `employee_no` = 'EMP-MFG-102' AND `deleted_flag` = 0 LIMIT 1),
       '2026-06-30 10:15:00', '包装机光电传感器信号不稳定', @equipment_employee_id,
       '更换光电传感器并完成空载、带载调试，生产班组验收合格', 260.00, 'CLOSED',
       CONCAT(@transaction_marker, ': 停机45分钟，无质量影响'), @transaction_operator_id,
       '2026-06-30 10:15:00', @transaction_operator_id, '2026-06-30 11:00:00'
FROM `eam_equipment` equipment
WHERE @seed_enabled = 1 AND equipment.`equipment_code` = 'EQ-MA-PACK-01' AND equipment.`deleted_flag` = 0;

INSERT INTO `eam_equipment_status_log`
  (`equipment_id`, `running_status`, `downtime_type`, `alert_level`, `station_id`, `collected_time`, `description`, `remark`,
   `created_by`, `created_time`)
SELECT equipment.`equipment_id`, status_seed.`running_status`, 'BREAKDOWN', 'HIGH', equipment.`station_id`,
       status_seed.`collected_time`, status_seed.`description`, CONCAT(@transaction_marker, ': 包装机维修状态'),
       @transaction_operator_id, status_seed.`collected_time`
FROM `eam_equipment` equipment
JOIN (
  SELECT 'REPAIRING' AS `running_status`, TIMESTAMP('2026-06-30 10:15:00') AS `collected_time`, '传感器异常停机维修' AS `description`
  UNION ALL SELECT 'RUNNING', TIMESTAMP('2026-06-30 11:00:00'), '更换传感器并验收后恢复运行'
) status_seed
WHERE @seed_enabled = 1 AND equipment.`equipment_code` = 'EQ-MA-PACK-01' AND equipment.`deleted_flag` = 0;

UPDATE `eam_equipment`
SET `status` = IF(`equipment_code` IN ('EQ-TR-PACK-01', 'EQ-MA-PACK-01'), 'IDLE', 'RUNNING'),
    `updated_by` = @transaction_operator_id,
    `updated_time` = '2026-07-13 16:30:00'
WHERE @seed_enabled = 1 AND `deleted_flag` = 0;

INSERT INTO `eam_equipment_status_log`
  (`equipment_id`, `running_status`, `station_id`, `collected_time`, `description`, `remark`, `created_by`, `created_time`)
SELECT equipment.`equipment_id`, 'IDLE', equipment.`station_id`, '2026-07-13 16:30:00',
       '当日包装任务完成后待机', CONCAT(@transaction_marker, ': 基准日最终状态'),
       @transaction_operator_id, '2026-07-13 16:30:00'
FROM `eam_equipment` equipment
WHERE @seed_enabled = 1 AND equipment.`equipment_code` IN ('EQ-TR-PACK-01', 'EQ-MA-PACK-01')
  AND equipment.`deleted_flag` = 0;

-- 8. 批次、外箱、留样、托盘条码与实际物料追溯。
INSERT INTO `trace_barcode`
  (`barcode_value`, `barcode_type`, `biz_type`, `biz_id`, `generated_rule`, `status`, `remark`,
   `created_by`, `created_time`)
SELECT CONCAT('MES-LOT-', product_lot.`lot_no`), 'PRODUCT_LOT', 'TRACE_PRODUCT_LOT', product_lot.`product_lot_id`,
       'MES-LOT-{LOT_NO}', 'ENABLE', CONCAT(@transaction_marker, ': 产品批次条码'),
       @transaction_operator_id, product_lot.`created_time`
FROM `trace_product_lot` product_lot
WHERE @seed_enabled = 1 AND product_lot.`deleted_flag` = 0;

INSERT INTO `trace_barcode`
  (`barcode_value`, `barcode_type`, `biz_type`, `biz_id`, `generated_rule`, `status`, `remark`,
   `created_by`, `created_time`)
SELECT CONCAT('MES-CTN-', product_lot.`lot_no`, '-', LPAD(number_record.`n`, 4, '0')),
       'OUTER_CARTON', 'TRACE_PRODUCT_LOT', product_lot.`product_lot_id`,
       'MES-CTN-{LOT_NO}-{SEQUENCE}', 'ENABLE',
       CONCAT(@transaction_marker, ': 25筒运输外箱；尾箱按实际数量标识'), @transaction_operator_id,
       product_lot.`updated_time`
FROM `trace_product_lot` product_lot
JOIN `tmp_number` number_record ON number_record.`n` <= CEILING(product_lot.`qualified_quantity` / 25)
WHERE @seed_enabled = 1 AND product_lot.`status` = 'STORED' AND product_lot.`deleted_flag` = 0;

INSERT INTO `trace_barcode`
  (`barcode_value`, `barcode_type`, `biz_type`, `biz_id`, `generated_rule`, `status`, `remark`,
   `created_by`, `created_time`)
SELECT CONCAT('MES-SAMPLE-', product_lot.`lot_no`), 'RETAIN_SAMPLE', 'TRACE_PRODUCT_LOT', product_lot.`product_lot_id`,
       'MES-SAMPLE-{LOT_NO}', 'ENABLE', CONCAT(@transaction_marker, ': 成品留样标识'),
       @transaction_operator_id, product_lot.`updated_time`
FROM `trace_product_lot` product_lot
WHERE @seed_enabled = 1 AND product_lot.`status` = 'STORED' AND product_lot.`deleted_flag` = 0;

INSERT INTO `trace_barcode`
  (`barcode_value`, `barcode_type`, `biz_type`, `biz_id`, `generated_rule`, `status`, `remark`,
   `created_by`, `created_time`)
SELECT CONCAT('MES-PLT-', production_order.`production_order_no`), 'PALLET', 'MFG_PRODUCTION_ORDER',
       production_order.`production_order_id`, 'MES-PLT-{PRODUCTION_ORDER_NO}', 'ENABLE',
       CONCAT(@transaction_marker, ': 完工订单发运托盘标识'), @transaction_operator_id, production_order.`updated_time`
FROM `mfg_production_order` production_order
WHERE @seed_enabled = 1 AND production_order.`status` = 'FINISHED' AND production_order.`deleted_flag` = 0;

INSERT INTO `trace_material_usage`
  (`product_lot_id`, `barcode_id`, `work_order_id`, `report_id`, `material_id`, `batch_id`, `used_quantity`,
   `remark`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT product_lot.`product_lot_id`, lot_barcode.`barcode_id`, product_lot.`work_order_id`, report_material.`report_id`,
       report_material.`material_id`, report_material.`batch_id`, report_material.`used_quantity`,
       CONCAT(@transaction_marker, ': 报工实际用料追溯冗余，不再次扣库存'), @transaction_operator_id,
       report_material.`created_time`, @transaction_operator_id, report_material.`updated_time`
FROM `mfg_report_material` report_material
JOIN `mfg_work_report` work_report ON work_report.`report_id` = report_material.`report_id` AND work_report.`deleted_flag` = 0
JOIN `mfg_task` task ON task.`task_id` = work_report.`task_id` AND task.`deleted_flag` = 0
JOIN `trace_product_lot` product_lot ON product_lot.`work_order_id` = task.`work_order_id` AND product_lot.`deleted_flag` = 0
JOIN `trace_barcode` lot_barcode ON lot_barcode.`biz_type` = 'TRACE_PRODUCT_LOT'
 AND lot_barcode.`biz_id` = product_lot.`product_lot_id` AND lot_barcode.`barcode_type` = 'PRODUCT_LOT'
 AND lot_barcode.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND report_material.`deleted_flag` = 0;

-- 9. 状态、审批与操作审计。日志保存最终快照前的关键业务流转。
INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`, `remark`,
   `created_by`, `created_time`)
SELECT status_seed.`biz_type`, status_seed.`biz_id`, status_seed.`old_status`, status_seed.`new_status`,
       status_seed.`change_reason`, @transaction_operator_id, status_seed.`changed_time`, @transaction_marker,
       @transaction_operator_id, status_seed.`changed_time`
FROM (
  SELECT 'MFG_PRODUCTION_ORDER' AS `biz_type`, `production_order_id` AS `biz_id`, 'DRAFT' AS `old_status`,
         `status` AS `new_status`, 'ORDER_LIFECYCLE_SNAPSHOT' AS `change_reason`, COALESCE(`updated_time`, `created_time`) AS `changed_time`
  FROM `mfg_production_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'MFG_PRODUCTION_PLAN', `plan_id`, 'DRAFT', `status`, 'PLAN_LIFECYCLE_SNAPSHOT', COALESCE(`updated_time`, `created_time`)
  FROM `mfg_production_plan` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'MFG_WORK_ORDER', `work_order_id`, 'DRAFT', `status`, 'WORK_ORDER_LIFECYCLE_SNAPSHOT', COALESCE(`updated_time`, `created_time`)
  FROM `mfg_work_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'MFG_TASK', `task_id`, 'WAIT_START', `status`, 'TASK_LIFECYCLE_SNAPSHOT', COALESCE(`updated_time`, `created_time`)
  FROM `mfg_task` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'MFG_WORK_REPORT', `report_id`, 'SUBMITTED', `audit_status`, 'AUDIT_WORK_REPORT', COALESCE(`updated_time`, `created_time`)
  FROM `mfg_work_report` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'WMS_INBOUND_ORDER', `inbound_id`, 'SUBMITTED', `audit_status`, 'CONFIRM_INBOUND', COALESCE(`updated_time`, `created_time`)
  FROM `wms_inbound_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'WMS_OUTBOUND_ORDER', `outbound_id`, 'SUBMITTED', `audit_status`, 'CONFIRM_OUTBOUND', COALESCE(`updated_time`, `created_time`)
  FROM `wms_outbound_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'WMS_RETURN_ORDER', `return_id`, 'SUBMITTED', `audit_status`, 'CONFIRM_RETURN', COALESCE(`updated_time`, `created_time`)
  FROM `wms_return_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'WMS_STOCKTAKE_ORDER', `stocktake_id`, 'RUNNING', `stocktake_status`, 'FINISH_STOCKTAKE', COALESCE(`updated_time`, `created_time`)
  FROM `wms_stocktake_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'QMS_INSPECTION_ORDER', `inspection_id`, 'DRAFT', `status`, 'SUBMIT_INSPECTION', COALESCE(`updated_time`, `created_time`)
  FROM `qms_inspection_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'QMS_NONCONFORMING_ORDER', `ncr_id`, 'SUBMITTED', `status`, 'CLOSE_NONCONFORMING', COALESCE(`updated_time`, `created_time`)
  FROM `qms_nonconforming_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'EAM_REPAIR_ORDER', `repair_id`, 'SUBMITTED', `status`, 'CLOSE_REPAIR', COALESCE(`updated_time`, `created_time`)
  FROM `eam_repair_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'MFG_ANDON_ALERT', `alert_id`, 'ACTIVE', `status`, 'CLOSE_ANDON', COALESCE(`updated_time`, `created_time`)
  FROM `mfg_andon_alert` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'MFG_EXCEPTION', `exception_id`, 'OPEN', `status`, 'CLOSE_EXCEPTION', COALESCE(`updated_time`, `created_time`)
  FROM `mfg_exception` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
) status_seed
WHERE @seed_enabled = 1;

INSERT INTO `sys_approval_record`
  (`biz_type`, `biz_id`, `approval_node`, `approver_id`, `approval_result`, `approval_comment`, `approval_time`,
   `remark`, `created_by`, `created_time`)
SELECT approval_seed.`biz_type`, approval_seed.`biz_id`, approval_seed.`approval_node`, @transaction_operator_id,
       'APPROVED', approval_seed.`approval_comment`, approval_seed.`approval_time`, @transaction_marker,
       @transaction_operator_id, approval_seed.`approval_time`
FROM (
  SELECT 'MFG_PRODUCTION_PLAN' AS `biz_type`, `plan_id` AS `biz_id`, 'PLAN_RELEASE' AS `approval_node`,
         '计划产能和交期校验通过' AS `approval_comment`, TIMESTAMPADD(MINUTE, 10, `created_time`) AS `approval_time`
  FROM `mfg_production_plan` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'MFG_WORK_REPORT', `report_id`, 'REPORT_AUDIT', '数量和工时审核通过', `updated_time`
  FROM `mfg_work_report` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'WMS_INBOUND_ORDER', `inbound_id`, 'INBOUND_CONFIRM', '检验和数量核对通过', `updated_time`
  FROM `wms_inbound_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'WMS_OUTBOUND_ORDER', `outbound_id`, 'OUTBOUND_CONFIRM', '库存和批次核对通过', `updated_time`
  FROM `wms_outbound_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'WMS_RETURN_ORDER', `return_id`, 'RETURN_CONFIRM', '完整余料验收通过', `updated_time`
  FROM `wms_return_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
  UNION ALL SELECT 'QMS_NONCONFORMING_ORDER', `ncr_id`, 'DISPOSITION_APPROVAL', '同意受控返工和复检方案', `updated_time`
  FROM `qms_nonconforming_order` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@transaction_marker, ':%')
) approval_seed
WHERE @seed_enabled = 1;

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `request_ip`, `operation_time`, `remark`,
   `created_by`, `created_time`)
SELECT @transaction_operator_id, LEFT(status_log.`change_reason`, 32), LOWER(status_log.`biz_type`), status_log.`biz_id`,
       '127.0.0.1', status_log.`changed_time`, CONCAT(@transaction_marker, ': 数据生成业务操作'),
       @transaction_operator_id, status_log.`changed_time`
FROM `sys_status_change_log` status_log
WHERE @seed_enabled = 1 AND status_log.`remark` = @transaction_marker AND status_log.`deleted_flag` = 0;

-- 完成标记与全部业务数据处于同一事务；只有 COMMIT 成功后重复执行才会进入 no-op。
INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `request_ip`, `operation_time`, `remark`,
   `created_by`, `created_time`)
SELECT @transaction_operator_id, 'SEED_RENIX_30D', 'db_seed', 20260713, '127.0.0.1',
       '2026-07-13 20:00:00', @transaction_marker, @transaction_operator_id, '2026-07-13 20:00:00'
WHERE @seed_enabled = 1;

COMMIT;

SELECT @transaction_marker AS `transaction_marker`, @seed_enabled AS `seed_executed`,
       @transaction_base_date AS `base_date`;
SELECT 'production_orders' AS `metric`, COUNT(*) AS `actual_value`
FROM `mfg_production_order` WHERE `deleted_flag` = 0
UNION ALL SELECT 'production_plans', COUNT(*) FROM `mfg_production_plan` WHERE `deleted_flag` = 0
UNION ALL SELECT 'work_orders', COUNT(*) FROM `mfg_work_order` WHERE `deleted_flag` = 0
UNION ALL SELECT 'tasks', COUNT(*) FROM `mfg_task` WHERE `deleted_flag` = 0
UNION ALL SELECT 'work_reports', COUNT(*) FROM `mfg_work_report` WHERE `deleted_flag` = 0
UNION ALL SELECT 'purchase_batches', COUNT(*) FROM `wms_material_batch`
  WHERE `deleted_flag` = 0 AND `source_order_type` = 'PURCHASE_RECEIPT'
UNION ALL SELECT 'inspections', COUNT(*) FROM `qms_inspection_order` WHERE `deleted_flag` = 0
UNION ALL SELECT 'product_lots', COUNT(*) FROM `trace_product_lot` WHERE `deleted_flag` = 0
UNION ALL SELECT 'barcodes', COUNT(*) FROM `trace_barcode` WHERE `deleted_flag` = 0;
