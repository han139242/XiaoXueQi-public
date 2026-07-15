-- 雷尼克斯 MES 2026-07-01 至 2026-07-14 真实运行窗口滚动。
-- 前置条件：RENIX_TX_30D_V1 与 RENIX_SYSTEM_BASE_V2 已验收通过。
-- 本脚本只处理明确业务编号和数据集标记；成功后重复执行为 no-op。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @live_marker = 'RENIX_LIVE_14D_V1';
SET @tx_marker = 'RENIX_TX_30D_V1';
SET @live_snapshot_time = '2026-07-14 14:30:00';
SET @live_marker_exists = (
  SELECT COUNT(*) FROM `sys_operation_log`
  WHERE `operation_type` = 'ROLLFORWARD_RENIX_14D'
    AND `module_code` = 'db_seed'
    AND `remark` = @live_marker
    AND `deleted_flag` = 0
);

SET @admin_user_id = (SELECT `user_id` FROM `sys_user` WHERE `username` = 'admin' AND `deleted_flag` = 0 LIMIT 1);
SET @planner_user_id = (SELECT `user_id` FROM `sys_user` WHERE `username` = 'shengchanjihua001' AND `deleted_flag` = 0 LIMIT 1);
SET @manager_user_id = (SELECT `user_id` FROM `sys_user` WHERE `username` = 'shengchanjingli001' AND `deleted_flag` = 0 LIMIT 1);
SET @quality_user_id = (SELECT `user_id` FROM `sys_user` WHERE `username` = 'zhijianyuan001' AND `deleted_flag` = 0 LIMIT 1);
SET @warehouse_user_id = (SELECT `user_id` FROM `sys_user` WHERE `username` = 'cangkuguanliyuan001' AND `deleted_flag` = 0 LIMIT 1);
SET @equipment_user_id = (SELECT `user_id` FROM `sys_user` WHERE `username` = 'shebeiguanliyuan001' AND `deleted_flag` = 0 LIMIT 1);
SET @quality_employee_id = (SELECT `employee_id` FROM `org_employee` WHERE `employee_no` = 'EMP-QMS-001' AND `deleted_flag` = 0 LIMIT 1);
SET @warehouse_employee_id = (SELECT `employee_id` FROM `org_employee` WHERE `employee_no` = 'EMP-WMS-001' AND `deleted_flag` = 0 LIMIT 1);
SET @equipment_employee_id = (SELECT `employee_id` FROM `org_employee` WHERE `employee_no` = 'EMP-EAM-001' AND `deleted_flag` = 0 LIMIT 1);
SET @raw_warehouse_id = (SELECT `warehouse_id` FROM `wms_warehouse` WHERE `warehouse_code` = 'WH-RAW-01' AND `deleted_flag` = 0 LIMIT 1);
SET @raw_location_id = (SELECT `location_id` FROM `wms_location` WHERE `location_code` = 'LOC-RAW-A01' AND `deleted_flag` = 0 LIMIT 1);
SET @fg_warehouse_id = (SELECT `warehouse_id` FROM `wms_warehouse` WHERE `warehouse_code` = 'WH-FG-01' AND `deleted_flag` = 0 LIMIT 1);
SET @fg_location_id = (SELECT `location_id` FROM `wms_location` WHERE `location_code` = 'LOC-FG-A01' AND `deleted_flag` = 0 LIMIT 1);

SET @live_precondition_ok = IF(
  @admin_user_id IS NOT NULL
  AND @planner_user_id IS NOT NULL
  AND @manager_user_id IS NOT NULL
  AND @quality_user_id IS NOT NULL
  AND @warehouse_user_id IS NOT NULL
  AND @equipment_user_id IS NOT NULL
  AND (SELECT COUNT(*) FROM `sys_role` WHERE `remark` LIKE 'RENIX_RBAC_V1%' AND `deleted_flag` = 0) = 8
  AND (SELECT COUNT(*) FROM `mfg_production_order` WHERE `remark` LIKE CONCAT(@tx_marker, '%') AND `deleted_flag` = 0) = 30
  AND (SELECT COUNT(*) FROM `mfg_schedule_scenario` WHERE `scenario_id` IN (2, 3, 4) AND `deleted_flag` = 0) = 3
  AND (SELECT COUNT(*) FROM `mfg_schedule_item` WHERE `scenario_id` IN (2, 3, 4) AND `deleted_flag` = 0) = 23
  AND (
    @live_marker_exists = 1
    OR (
      (SELECT COUNT(*) FROM `mfg_production_order` WHERE `deleted_flag` = 0 AND `status` = 'FINISHED') = 20
      AND (SELECT COUNT(*) FROM `mfg_production_order` WHERE `deleted_flag` = 0 AND `status` = 'RUNNING') = 2
      AND (SELECT COUNT(*) FROM `mfg_work_order` WHERE `deleted_flag` = 0 AND `status` = 'RUNNING') = 2
      AND (SELECT COUNT(*) FROM `mfg_task` WHERE `deleted_flag` = 0 AND `status` = 'RUNNING') = 2
      AND (SELECT COUNT(*) FROM `trace_product_lot` WHERE `deleted_flag` = 0 AND `status` = 'RUNNING') = 2
      AND (SELECT COUNT(*) FROM `mfg_work_order`
           WHERE `work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2')
             AND `status` = 'RUNNING' AND `deleted_flag` = 0) = 2
      AND (SELECT COUNT(*) FROM `mfg_work_order`
           WHERE `work_order_no` = 'WO-20260714-003-1'
             AND `status` = 'RELEASED' AND `deleted_flag` = 0) = 1
    )
  ), 1, 0
);
SET @live_assertion_sql = IF(
  @live_precondition_ok = 1,
  'DO 0',
  'SELECT * FROM `__ABORT_RENIX_LIVE_14D_PRECONDITION__`'
);
PREPARE renix_live_assertion_stmt FROM @live_assertion_sql;
EXECUTE renix_live_assertion_stmt;
DEALLOCATE PREPARE renix_live_assertion_stmt;

SET @seed_enabled = IF(@live_marker_exists = 0, 1, 0);

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `tmp_live_number`;
CREATE TEMPORARY TABLE `tmp_live_number` AS
WITH RECURSIVE number_series AS (
  SELECT 1 AS `n`
  UNION ALL SELECT `n` + 1 FROM number_series WHERE `n` < 100
)
SELECT `n` FROM number_series;

DROP TEMPORARY TABLE IF EXISTS `tmp_live_task_audit`;
CREATE TEMPORARY TABLE `tmp_live_task_audit` AS
SELECT task.`task_id`, operator_user.`user_id` AS `operator_user_id`, supervisor_user.`user_id` AS `supervisor_user_id`
FROM `mfg_task` task
JOIN `mfg_dispatch_order` dispatch_order
  ON dispatch_order.`dispatch_id` = task.`dispatch_id` AND dispatch_order.`deleted_flag` = 0
JOIN `org_team` team ON team.`team_id` = dispatch_order.`team_id` AND team.`deleted_flag` = 0
JOIN `sys_user` supervisor_user
  ON supervisor_user.`employee_id` = team.`leader_id` AND supervisor_user.`deleted_flag` = 0
JOIN `mfg_task_operator` task_operator
  ON task_operator.`task_id` = task.`task_id` AND task_operator.`role_in_task` = 'PRIMARY'
 AND task_operator.`deleted_flag` = 0
JOIN `sys_user` operator_user
  ON operator_user.`employee_id` = task_operator.`employee_id` AND operator_user.`deleted_flag` = 0
WHERE task.`deleted_flag` = 0;

-- 1. 将既有 30 天仿真记录按真实岗位重新归属审计责任。
UPDATE `mfg_production_order`
SET `created_by` = @planner_user_id, `updated_by` = @planner_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;

UPDATE `mfg_production_plan`
SET `created_by` = @planner_user_id, `updated_by` = @planner_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;

UPDATE `mfg_work_order`
SET `created_by` = @manager_user_id, `updated_by` = @manager_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;

UPDATE `mfg_work_order_material`
SET `created_by` = @manager_user_id,
    `updated_by` = IF(`status` = 'ISSUED', @warehouse_user_id, @manager_user_id)
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;

UPDATE `mfg_dispatch_order` dispatch_order
JOIN `org_team` team ON team.`team_id` = dispatch_order.`team_id` AND team.`deleted_flag` = 0
JOIN `sys_user` supervisor_user
  ON supervisor_user.`employee_id` = team.`leader_id` AND supervisor_user.`deleted_flag` = 0
SET dispatch_order.`created_by` = @manager_user_id,
    dispatch_order.`updated_by` = supervisor_user.`user_id`
WHERE @seed_enabled = 1 AND dispatch_order.`remark` LIKE CONCAT(@tx_marker, ':%')
  AND dispatch_order.`deleted_flag` = 0;

UPDATE `mfg_task` task
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = task.`task_id`
SET task.`created_by` = audit.`supervisor_user_id`,
    task.`updated_by` = IF(task.`status` = 'WAIT_START', audit.`supervisor_user_id`, audit.`operator_user_id`)
WHERE @seed_enabled = 1 AND task.`remark` LIKE CONCAT(@tx_marker, ':%') AND task.`deleted_flag` = 0;

UPDATE `mfg_task_operator` task_operator
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = task_operator.`task_id`
SET task_operator.`created_by` = audit.`supervisor_user_id`
WHERE @seed_enabled = 1 AND task_operator.`deleted_flag` = 0;

UPDATE `mfg_work_report` work_report
JOIN `sys_user` operator_user
  ON operator_user.`employee_id` = work_report.`operator_id` AND operator_user.`deleted_flag` = 0
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = work_report.`task_id`
SET work_report.`created_by` = operator_user.`user_id`,
    work_report.`updated_by` = audit.`supervisor_user_id`
WHERE @seed_enabled = 1 AND work_report.`remark` LIKE CONCAT(@tx_marker, ':%')
  AND work_report.`deleted_flag` = 0;

UPDATE `mfg_report_material` report_material
JOIN `mfg_work_report` work_report ON work_report.`report_id` = report_material.`report_id` AND work_report.`deleted_flag` = 0
SET report_material.`created_by` = work_report.`created_by`,
    report_material.`updated_by` = work_report.`updated_by`
WHERE @seed_enabled = 1 AND report_material.`remark` LIKE CONCAT(@tx_marker, ':%')
  AND report_material.`deleted_flag` = 0;

UPDATE `mfg_exception`
SET `created_by` = @manager_user_id, `updated_by` = @manager_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;

UPDATE `mfg_andon_alert`
SET `created_by` = @manager_user_id, `updated_by` = @manager_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;

UPDATE `wms_material_batch`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `wms_inventory`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `wms_stock_movement`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `wms_inbound_order`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `wms_inbound_detail`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `wms_outbound_order`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `wms_outbound_detail`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `wms_return_order`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `wms_return_detail`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `wms_stocktake_order`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `wms_stocktake_detail`
SET `created_by` = @warehouse_user_id, `updated_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;

UPDATE `qms_inspection_order`
SET `created_by` = @quality_user_id, `updated_by` = @quality_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `qms_inspection_result`
SET `created_by` = @quality_user_id, `updated_by` = @quality_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `qms_nonconforming_order`
SET `created_by` = @quality_user_id, `updated_by` = @quality_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `qms_nonconforming_detail`
SET `created_by` = @quality_user_id, `updated_by` = @quality_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;

UPDATE `eam_spot_check_plan`
SET `created_by` = @equipment_user_id, `updated_by` = @equipment_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `eam_spot_check_record`
SET `created_by` = @equipment_user_id, `updated_by` = @equipment_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `eam_maintenance_plan`
SET `created_by` = @equipment_user_id, `updated_by` = @equipment_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `eam_maintenance_record`
SET `created_by` = @equipment_user_id, `updated_by` = @equipment_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `eam_equipment_status_log`
SET `created_by` = @equipment_user_id, `updated_by` = @equipment_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `eam_repair_order`
SET `created_by` = @equipment_user_id, `updated_by` = @equipment_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%') AND `deleted_flag` = 0;
UPDATE `eam_equipment`
SET `updated_by` = @equipment_user_id
WHERE @seed_enabled = 1 AND `deleted_flag` = 0;

UPDATE `trace_product_lot` product_lot
JOIN `mfg_task` task ON task.`work_order_id` = product_lot.`work_order_id` AND task.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = (SELECT `route_id` FROM `mfg_work_order` WHERE `work_order_id` = product_lot.`work_order_id`)
 AND route_item.`operation_id` = task.`operation_id` AND route_item.`sequence_no` = 50 AND route_item.`deleted_flag` = 0
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = task.`task_id`
SET product_lot.`created_by` = audit.`operator_user_id`, product_lot.`updated_by` = audit.`operator_user_id`
WHERE @seed_enabled = 1 AND product_lot.`remark` LIKE CONCAT(@tx_marker, ':%')
  AND product_lot.`deleted_flag` = 0;

UPDATE `trace_material_usage` usage_record
JOIN `mfg_work_report` work_report ON work_report.`report_id` = usage_record.`report_id` AND work_report.`deleted_flag` = 0
SET usage_record.`created_by` = work_report.`created_by`, usage_record.`updated_by` = work_report.`created_by`
WHERE @seed_enabled = 1 AND usage_record.`remark` LIKE CONCAT(@tx_marker, ':%')
  AND usage_record.`deleted_flag` = 0;

UPDATE `trace_barcode` barcode
LEFT JOIN `trace_product_lot` product_lot
  ON barcode.`biz_type` = 'TRACE_PRODUCT_LOT' AND barcode.`biz_id` = product_lot.`product_lot_id`
SET barcode.`created_by` = CASE
      WHEN barcode.`barcode_type` = 'RETAIN_SAMPLE' THEN @quality_user_id
      WHEN barcode.`barcode_type` IN ('OUTER_CARTON', 'PALLET') THEN @warehouse_user_id
      ELSE COALESCE(product_lot.`created_by`, @manager_user_id)
    END,
    barcode.`updated_by` = CASE
      WHEN barcode.`barcode_type` = 'RETAIN_SAMPLE' THEN @quality_user_id
      WHEN barcode.`barcode_type` IN ('OUTER_CARTON', 'PALLET') THEN @warehouse_user_id
      ELSE COALESCE(product_lot.`updated_by`, @manager_user_id)
    END
WHERE @seed_enabled = 1 AND barcode.`remark` LIKE CONCAT(@tx_marker, ':%') AND barcode.`deleted_flag` = 0;

-- 2. 收口两张旧在制工单，并启动 2026-07-14 训练线工单。
DROP TEMPORARY TABLE IF EXISTS `tmp_live_task_target`;
CREATE TEMPORARY TABLE `tmp_live_task_target` (
  `work_order_no` VARCHAR(64) NOT NULL,
  `sequence_no` INT NOT NULL,
  `completed_quantity` DECIMAL(18,4) NOT NULL,
  `target_status` VARCHAR(32) NOT NULL,
  `updated_time` DATETIME NOT NULL,
  PRIMARY KEY (`work_order_no`, `sequence_no`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_live_task_target` VALUES
  ('WO-20260713-013-1', 50, 800, 'FINISHED', '2026-07-13 15:30:00'),
  ('WO-20260713-013-1', 60, 800, 'FINISHED', '2026-07-13 16:10:00'),
  ('WO-20260713-013-1', 70, 800, 'FINISHED', '2026-07-13 16:45:00'),
  ('WO-20260713-024-2', 50, 500, 'FINISHED', '2026-07-14 08:50:00'),
  ('WO-20260713-024-2', 60, 500, 'FINISHED', '2026-07-14 09:30:00'),
  ('WO-20260713-024-2', 70, 500, 'FINISHED', '2026-07-14 10:10:00'),
  ('WO-20260714-003-1', 10, 600, 'FINISHED', '2026-07-14 08:45:00'),
  ('WO-20260714-003-1', 20, 600, 'FINISHED', '2026-07-14 09:35:00'),
  ('WO-20260714-003-1', 30, 600, 'FINISHED', '2026-07-14 10:35:00'),
  ('WO-20260714-003-1', 40, 600, 'FINISHED', '2026-07-14 11:25:00'),
  ('WO-20260714-003-1', 50, 240, 'RUNNING', '2026-07-14 14:30:00');

UPDATE `mfg_task` task
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = task.`work_order_id` AND work_order.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id` AND route_item.`operation_id` = task.`operation_id`
 AND route_item.`deleted_flag` = 0
JOIN `tmp_live_task_target` target
  ON target.`work_order_no` = work_order.`work_order_no` AND target.`sequence_no` = route_item.`sequence_no`
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = task.`task_id`
SET task.`completed_quantity` = target.`completed_quantity`,
    task.`defect_quantity` = 0,
    task.`status` = target.`target_status`,
    task.`updated_by` = audit.`operator_user_id`,
    task.`updated_time` = target.`updated_time`,
    task.`version` = task.`version` + 1
WHERE @seed_enabled = 1;

UPDATE `mfg_work_order`
SET `completed_quantity` = `planned_quantity`, `status` = 'FINISHED',
    `updated_by` = @manager_user_id,
    `updated_time` = IF(`work_order_no` = 'WO-20260713-013-1', '2026-07-13 16:45:00', '2026-07-14 10:10:00'),
    `version` = `version` + 1
WHERE @seed_enabled = 1
  AND `work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2') AND `deleted_flag` = 0;

UPDATE `mfg_work_order`
SET `completed_quantity` = 240, `status` = 'RUNNING', `updated_by` = @manager_user_id,
    `updated_time` = @live_snapshot_time, `version` = `version` + 1
WHERE @seed_enabled = 1 AND `work_order_no` = 'WO-20260714-003-1' AND `deleted_flag` = 0;

UPDATE `mfg_dispatch_order` dispatch_order
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = dispatch_order.`work_order_id`
SET dispatch_order.`status` = IF(work_order.`work_order_no` = 'WO-20260714-003-1', 'RUNNING', 'FINISHED'),
    dispatch_order.`material_ready_status` = 'READY',
    dispatch_order.`updated_time` = work_order.`updated_time`,
    dispatch_order.`version` = dispatch_order.`version` + 1
WHERE @seed_enabled = 1
  AND work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2', 'WO-20260714-003-1')
  AND dispatch_order.`deleted_flag` = 0;

UPDATE `mfg_production_plan` plan
JOIN `mfg_production_order` production_order ON production_order.`production_order_id` = plan.`production_order_id`
SET plan.`status` = IF(production_order.`production_order_no` = 'MO-20260710-003', 'RUNNING', 'FINISHED'),
    plan.`updated_by` = @planner_user_id,
    plan.`updated_time` = IF(production_order.`production_order_no` = 'MO-20260710-003', @live_snapshot_time,
      IF(production_order.`production_order_no` = 'MO-20260710-013', '2026-07-13 16:45:00', '2026-07-14 10:10:00')),
    plan.`version` = plan.`version` + 1
WHERE @seed_enabled = 1
  AND production_order.`production_order_no` IN ('MO-20260710-003', 'MO-20260710-013', 'MO-20260709-024')
  AND plan.`deleted_flag` = 0;

UPDATE `mfg_production_order`
SET `status` = IF(`production_order_no` = 'MO-20260710-003', 'RUNNING', 'FINISHED'),
    `updated_by` = @planner_user_id,
    `updated_time` = IF(`production_order_no` = 'MO-20260710-003', @live_snapshot_time,
      IF(`production_order_no` = 'MO-20260710-013', '2026-07-13 16:45:00', '2026-07-14 10:10:00')),
    `version` = `version` + 1
WHERE @seed_enabled = 1
  AND `production_order_no` IN ('MO-20260710-003', 'MO-20260710-013', 'MO-20260709-024')
  AND `deleted_flag` = 0;

-- 3. 当日工单按当前库存执行 FIFO 领料，余额只通过库存流水重算。
DROP TEMPORARY TABLE IF EXISTS `tmp_live_issue_supply`;
CREATE TEMPORARY TABLE `tmp_live_issue_supply` AS
SELECT supply_base.*,
       SUM(supply_base.`available_quantity`) OVER (
         PARTITION BY supply_base.`material_id`
         ORDER BY supply_base.`received_date`, supply_base.`batch_id`
       ) AS `supply_end`,
       SUM(supply_base.`available_quantity`) OVER (
         PARTITION BY supply_base.`material_id`
         ORDER BY supply_base.`received_date`, supply_base.`batch_id`
       ) - supply_base.`available_quantity` AS `supply_start`
FROM (
  SELECT inventory.`inventory_id`, inventory.`material_id`, inventory.`batch_id`,
         inventory.`available_quantity`, material_batch.`received_date`
  FROM `wms_inventory` inventory
  JOIN `wms_material_batch` material_batch
    ON material_batch.`batch_id` = inventory.`batch_id` AND material_batch.`deleted_flag` = 0
  WHERE inventory.`warehouse_id` = @raw_warehouse_id
    AND inventory.`available_quantity` > 0 AND inventory.`deleted_flag` = 0
) supply_base;

DROP TEMPORARY TABLE IF EXISTS `tmp_live_issue_allocation`;
CREATE TEMPORARY TABLE `tmp_live_issue_allocation` AS
SELECT allocation_base.*,
       ROW_NUMBER() OVER (ORDER BY allocation_base.`material_id`, allocation_base.`received_date`, allocation_base.`batch_id`) AS `allocation_seq`
FROM (
  SELECT requirement.`requirement_id`, requirement.`work_order_id`, requirement.`material_id`,
         supply.`inventory_id`, supply.`batch_id`, supply.`received_date`,
         GREATEST(0, LEAST(requirement.`required_quantity`, supply.`supply_end`) - supply.`supply_start`) AS `allocated_quantity`
  FROM `mfg_work_order_material` requirement
  JOIN `mfg_work_order` work_order
    ON work_order.`work_order_id` = requirement.`work_order_id`
   AND work_order.`work_order_no` = 'WO-20260714-003-1' AND work_order.`deleted_flag` = 0
  JOIN `tmp_live_issue_supply` supply ON supply.`material_id` = requirement.`material_id`
  WHERE requirement.`deleted_flag` = 0 AND supply.`supply_start` < requirement.`required_quantity`
) allocation_base
WHERE allocation_base.`allocated_quantity` > 0;

INSERT INTO `wms_outbound_order`
  (`outbound_no`, `outbound_type`, `warehouse_id`, `work_order_id`, `applicant_id`, `audit_status`, `outbound_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'OB-MI-WO-20260714-003-1', 'MATERIAL_ISSUE', @raw_warehouse_id, work_order.`work_order_id`, team.`leader_id`,
       'APPROVED', '2026-07-14 07:40:00', CONCAT(@live_marker, ': 当日训练线FIFO生产领料'), 0,
       @warehouse_user_id, '2026-07-13 16:10:00', @warehouse_user_id, '2026-07-14 07:40:00', 0
FROM `mfg_work_order` work_order
JOIN `mfg_dispatch_order` dispatch_order
  ON dispatch_order.`work_order_id` = work_order.`work_order_id` AND dispatch_order.`deleted_flag` = 0
JOIN `org_team` team ON team.`team_id` = dispatch_order.`team_id` AND team.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND work_order.`work_order_no` = 'WO-20260714-003-1'
  AND work_order.`deleted_flag` = 0;

INSERT INTO `wms_outbound_detail`
  (`outbound_id`, `material_id`, `batch_id`, `location_id`, `outbound_quantity`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT outbound_order.`outbound_id`, allocation.`material_id`, allocation.`batch_id`, @raw_location_id,
       allocation.`allocated_quantity`, CONCAT(@live_marker, ': FIFO批次分配'), 0,
       @warehouse_user_id, outbound_order.`created_time`, @warehouse_user_id, outbound_order.`outbound_time`, 0
FROM `tmp_live_issue_allocation` allocation
JOIN `wms_outbound_order` outbound_order
  ON outbound_order.`outbound_no` = 'OB-MI-WO-20260714-003-1' AND outbound_order.`deleted_flag` = 0
WHERE @seed_enabled = 1;

UPDATE `mfg_work_order_material` requirement
JOIN (
  SELECT `requirement_id`, SUM(`allocated_quantity`) AS `issued_quantity`
  FROM `tmp_live_issue_allocation` GROUP BY `requirement_id`
) allocation ON allocation.`requirement_id` = requirement.`requirement_id`
SET requirement.`issued_quantity` = allocation.`issued_quantity`,
    requirement.`shortage_quantity` = GREATEST(requirement.`required_quantity` - allocation.`issued_quantity`, 0),
    requirement.`status` = IF(allocation.`issued_quantity` >= requirement.`required_quantity`, 'ISSUED', 'SHORTAGE'),
    requirement.`updated_by` = @warehouse_user_id,
    requirement.`updated_time` = '2026-07-14 07:40:00',
    requirement.`version` = requirement.`version` + 1
WHERE @seed_enabled = 1;

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `version`, `created_by`, `created_time`,
   `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('MV-L14-MI-', LPAD(detail.`detail_id`, 6, '0')), 'OUTBOUND', 'OUTBOUND_ORDER', detail.`outbound_id`,
       detail.`material_id`, detail.`batch_id`, detail.`outbound_quantity`, 'OUT', outbound_order.`outbound_time`,
       CONCAT(@live_marker, ': 当日生产领料出库'), 0, @warehouse_user_id, outbound_order.`outbound_time`,
       @warehouse_user_id, outbound_order.`outbound_time`, 0
FROM `wms_outbound_detail` detail
JOIN `wms_outbound_order` outbound_order
  ON outbound_order.`outbound_id` = detail.`outbound_id`
 AND outbound_order.`outbound_no` = 'OB-MI-WO-20260714-003-1' AND outbound_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND detail.`deleted_flag` = 0;

-- 两张完工工单退回按损耗率多领但未使用的完整外箱。
DROP TEMPORARY TABLE IF EXISTS `tmp_live_carton_return`;
CREATE TEMPORARY TABLE `tmp_live_carton_return` AS
SELECT ranked.*
FROM (
  SELECT work_order.`work_order_id`, work_order.`work_order_no`, requirement.`material_id`, detail.`batch_id`,
         requirement.`issued_quantity` - CEILING(work_order.`completed_quantity` / 25) AS `return_quantity`,
         ROW_NUMBER() OVER (PARTITION BY work_order.`work_order_id` ORDER BY material_batch.`received_date` DESC, detail.`batch_id` DESC) AS `candidate_rank`
  FROM `mfg_work_order` work_order
  JOIN `mfg_work_order_material` requirement
    ON requirement.`work_order_id` = work_order.`work_order_id` AND requirement.`deleted_flag` = 0
  JOIN `mdm_material` material
    ON material.`material_id` = requirement.`material_id`
   AND material.`material_code` LIKE 'PKG-CARTON-%' AND material.`deleted_flag` = 0
  JOIN `wms_outbound_order` outbound_order
    ON outbound_order.`work_order_id` = work_order.`work_order_id`
   AND outbound_order.`outbound_type` = 'MATERIAL_ISSUE' AND outbound_order.`deleted_flag` = 0
  JOIN `wms_outbound_detail` detail
    ON detail.`outbound_id` = outbound_order.`outbound_id` AND detail.`material_id` = requirement.`material_id`
   AND detail.`deleted_flag` = 0
  JOIN `wms_material_batch` material_batch
    ON material_batch.`batch_id` = detail.`batch_id` AND material_batch.`deleted_flag` = 0
  WHERE work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2')
    AND work_order.`deleted_flag` = 0
) ranked
WHERE ranked.`candidate_rank` = 1 AND ranked.`return_quantity` > 0;

INSERT INTO `wms_return_order`
  (`return_no`, `return_type`, `work_order_id`, `applicant_id`, `audit_status`, `return_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('RT-LIVE-', seed.`work_order_no`), 'MATERIAL_RETURN', seed.`work_order_id`, team.`leader_id`, 'APPROVED',
       IF(seed.`work_order_no` = 'WO-20260713-013-1', '2026-07-13 17:00:00', '2026-07-14 10:20:00'),
       CONCAT(@live_marker, ': 完工后完整外箱退回原批次'), 0,
       @warehouse_user_id,
       IF(seed.`work_order_no` = 'WO-20260713-013-1', '2026-07-13 16:50:00', '2026-07-14 10:15:00'),
       @warehouse_user_id,
       IF(seed.`work_order_no` = 'WO-20260713-013-1', '2026-07-13 17:00:00', '2026-07-14 10:20:00'), 0
FROM `tmp_live_carton_return` seed
JOIN `mfg_dispatch_order` dispatch_order
  ON dispatch_order.`work_order_id` = seed.`work_order_id` AND dispatch_order.`deleted_flag` = 0
JOIN `org_team` team ON team.`team_id` = dispatch_order.`team_id` AND team.`deleted_flag` = 0
WHERE @seed_enabled = 1;

INSERT INTO `wms_return_detail`
  (`return_id`, `material_id`, `batch_id`, `return_quantity`, `return_reason`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT return_order.`return_id`, seed.`material_id`, seed.`batch_id`, seed.`return_quantity`,
       '完工后完整包装材料余量', CONCAT(@live_marker, ': 可复用完整外箱'), 0,
       @warehouse_user_id, return_order.`created_time`, @warehouse_user_id, return_order.`return_time`, 0
FROM `tmp_live_carton_return` seed
JOIN `wms_return_order` return_order
  ON return_order.`return_no` = CONCAT('RT-LIVE-', seed.`work_order_no`) AND return_order.`deleted_flag` = 0
WHERE @seed_enabled = 1;

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `version`, `created_by`, `created_time`,
   `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('MV-L14-RT-', LPAD(detail.`detail_id`, 6, '0')), 'RETURN', 'RETURN_ORDER', detail.`return_id`,
       detail.`material_id`, detail.`batch_id`, detail.`return_quantity`, 'IN', return_order.`return_time`,
       CONCAT(@live_marker, ': 完工余料退回'), 0, @warehouse_user_id, return_order.`return_time`,
       @warehouse_user_id, return_order.`return_time`, 0
FROM `wms_return_detail` detail
JOIN `wms_return_order` return_order
  ON return_order.`return_id` = detail.`return_id` AND return_order.`remark` LIKE CONCAT(@live_marker, ':%')
 AND return_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND detail.`deleted_flag` = 0;

-- 4. 补齐剩余报工和当日 240 筒在制报工。
DROP TEMPORARY TABLE IF EXISTS `tmp_live_report_seed`;
CREATE TEMPORARY TABLE `tmp_live_report_seed` (
  `work_order_no` VARCHAR(64) NOT NULL,
  `sequence_no` INT NOT NULL,
  `report_no` VARCHAR(64) NOT NULL,
  `qualified_quantity` DECIMAL(18,4) NOT NULL,
  `report_time` DATETIME NOT NULL,
  PRIMARY KEY (`report_no`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_live_report_seed` VALUES
  ('WO-20260713-013-1', 50, 'WR-LIVE-20260713-013-1-050-2', 480, '2026-07-13 15:30:00'),
  ('WO-20260713-013-1', 60, 'WR-LIVE-20260713-013-1-060', 800, '2026-07-13 16:10:00'),
  ('WO-20260713-013-1', 70, 'WR-LIVE-20260713-013-1-070', 800, '2026-07-13 16:45:00'),
  ('WO-20260713-024-2', 50, 'WR-LIVE-20260713-024-2-050-2', 300, '2026-07-14 08:50:00'),
  ('WO-20260713-024-2', 60, 'WR-LIVE-20260713-024-2-060', 500, '2026-07-14 09:30:00'),
  ('WO-20260713-024-2', 70, 'WR-LIVE-20260713-024-2-070', 500, '2026-07-14 10:10:00'),
  ('WO-20260714-003-1', 10, 'WR-LIVE-20260714-003-1-010', 600, '2026-07-14 08:45:00'),
  ('WO-20260714-003-1', 20, 'WR-LIVE-20260714-003-1-020', 600, '2026-07-14 09:35:00'),
  ('WO-20260714-003-1', 30, 'WR-LIVE-20260714-003-1-030', 600, '2026-07-14 10:35:00'),
  ('WO-20260714-003-1', 40, 'WR-LIVE-20260714-003-1-040', 600, '2026-07-14 11:25:00'),
  ('WO-20260714-003-1', 50, 'WR-LIVE-20260714-003-1-050', 240, '2026-07-14 14:20:00');

INSERT INTO `mfg_work_report`
  (`report_no`, `task_id`, `operator_id`, `station_id`, `qualified_quantity`, `defect_quantity`,
   `actual_work_hours`, `report_time`, `audit_status`, `remark`, `version`, `created_by`, `created_time`,
   `updated_by`, `updated_time`, `deleted_flag`)
SELECT seed.`report_no`, task.`task_id`, task_operator.`employee_id`, task.`station_id`, seed.`qualified_quantity`, 0,
       ROUND(seed.`qualified_quantity` * route_item.`standard_time` / 60, 4), seed.`report_time`, 'APPROVED',
       CONCAT(@live_marker, ': 岗位报工并由班组长审核'), 0, audit.`operator_user_id`,
       TIMESTAMPADD(MINUTE, -10, seed.`report_time`), audit.`supervisor_user_id`, seed.`report_time`, 0
FROM `tmp_live_report_seed` seed
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_no` = seed.`work_order_no` AND work_order.`deleted_flag` = 0
JOIN `mfg_task` task ON task.`work_order_id` = work_order.`work_order_id` AND task.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id` AND route_item.`operation_id` = task.`operation_id`
 AND route_item.`sequence_no` = seed.`sequence_no` AND route_item.`deleted_flag` = 0
JOIN `mfg_task_operator` task_operator
  ON task_operator.`task_id` = task.`task_id` AND task_operator.`role_in_task` = 'PRIMARY'
 AND task_operator.`deleted_flag` = 0
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = task.`task_id`
WHERE @seed_enabled = 1;

-- 报工用料按累计目标减既有用量，继续沿原领料批次 FIFO 分配。
DROP TEMPORARY TABLE IF EXISTS `tmp_live_usage_interval`;
CREATE TEMPORARY TABLE `tmp_live_usage_interval` AS
SELECT demand.*
FROM (
  SELECT work_order.`work_order_id`, requirement.`material_id`,
         COALESCE((SELECT SUM(report_material.`used_quantity`)
                   FROM `mfg_report_material` report_material
                   JOIN `mfg_work_report` report ON report.`report_id` = report_material.`report_id`
                   JOIN `mfg_task` report_task ON report_task.`task_id` = report.`task_id`
                   WHERE report_task.`work_order_id` = work_order.`work_order_id`
                     AND report_material.`material_id` = requirement.`material_id`
                     AND report_material.`deleted_flag` = 0 AND report.`deleted_flag` = 0), 0) AS `demand_start`,
         CASE
           WHEN material.`material_code` LIKE 'PKG-CARTON-%'
             THEN CEILING(work_order.`completed_quantity` / 25)
           WHEN work_order.`status` = 'FINISHED' THEN requirement.`issued_quantity`
           ELSE ROUND(requirement.`issued_quantity` * work_order.`completed_quantity` / work_order.`planned_quantity`, 4)
         END AS `demand_end`
  FROM `mfg_work_order` work_order
  JOIN `mfg_work_order_material` requirement
    ON requirement.`work_order_id` = work_order.`work_order_id` AND requirement.`deleted_flag` = 0
  JOIN `mdm_material` material ON material.`material_id` = requirement.`material_id` AND material.`deleted_flag` = 0
  WHERE work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2', 'WO-20260714-003-1')
    AND work_order.`deleted_flag` = 0
) demand
WHERE demand.`demand_end` > demand.`demand_start`;

DROP TEMPORARY TABLE IF EXISTS `tmp_live_issue_interval`;
CREATE TEMPORARY TABLE `tmp_live_issue_interval` AS
SELECT issue_base.*,
       SUM(issue_base.`outbound_quantity`) OVER (
         PARTITION BY issue_base.`work_order_id`, issue_base.`material_id`
         ORDER BY issue_base.`received_date`, issue_base.`batch_id`
       ) AS `issue_end`,
       SUM(issue_base.`outbound_quantity`) OVER (
         PARTITION BY issue_base.`work_order_id`, issue_base.`material_id`
         ORDER BY issue_base.`received_date`, issue_base.`batch_id`
       ) - issue_base.`outbound_quantity` AS `issue_start`
FROM (
  SELECT outbound_order.`work_order_id`, detail.`material_id`, detail.`batch_id`, detail.`outbound_quantity`,
         material_batch.`received_date`
  FROM `wms_outbound_order` outbound_order
  JOIN `wms_outbound_detail` detail
    ON detail.`outbound_id` = outbound_order.`outbound_id` AND detail.`deleted_flag` = 0
  JOIN `wms_material_batch` material_batch
    ON material_batch.`batch_id` = detail.`batch_id` AND material_batch.`deleted_flag` = 0
  JOIN `mfg_work_order` work_order
    ON work_order.`work_order_id` = outbound_order.`work_order_id`
   AND work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2', 'WO-20260714-003-1')
  WHERE outbound_order.`outbound_type` = 'MATERIAL_ISSUE' AND outbound_order.`deleted_flag` = 0
) issue_base;

DROP TEMPORARY TABLE IF EXISTS `tmp_live_usage_allocation`;
CREATE TEMPORARY TABLE `tmp_live_usage_allocation` AS
SELECT demand.`work_order_id`, demand.`material_id`, issue_record.`batch_id`,
       GREATEST(0, LEAST(demand.`demand_end`, issue_record.`issue_end`)
         - GREATEST(demand.`demand_start`, issue_record.`issue_start`)) AS `used_quantity`
FROM `tmp_live_usage_interval` demand
JOIN `tmp_live_issue_interval` issue_record
  ON issue_record.`work_order_id` = demand.`work_order_id` AND issue_record.`material_id` = demand.`material_id`
 AND demand.`demand_start` < issue_record.`issue_end` AND demand.`demand_end` > issue_record.`issue_start`;

INSERT INTO `mfg_report_material`
  (`report_id`, `material_id`, `batch_id`, `used_quantity`, `scrap_quantity`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT work_report.`report_id`, allocation.`material_id`, allocation.`batch_id`, allocation.`used_quantity`,
       CASE WHEN material.`material_code` LIKE 'PKG-CARTON-%' THEN 0
            ELSE ROUND(allocation.`used_quantity` * bom_item.`loss_rate` / (1 + bom_item.`loss_rate`), 4) END,
       CONCAT(@live_marker, ': 增量实际批次用料，不重复扣库存'), 0,
       work_report.`created_by`, work_report.`report_time`, work_report.`updated_by`, work_report.`report_time`, 0
FROM `tmp_live_usage_allocation` allocation
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = allocation.`work_order_id`
JOIN `mdm_bom_item` bom_item
  ON bom_item.`bom_id` = work_order.`bom_id` AND bom_item.`material_id` = allocation.`material_id`
 AND bom_item.`deleted_flag` = 0
JOIN `mdm_material` material ON material.`material_id` = allocation.`material_id` AND material.`deleted_flag` = 0
JOIN `tmp_live_report_seed` report_seed
  ON report_seed.`work_order_no` = work_order.`work_order_no` AND report_seed.`sequence_no` = 50
JOIN `mfg_work_report` work_report
  ON work_report.`report_no` = report_seed.`report_no` AND work_report.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND allocation.`used_quantity` > 0;

-- 5. 产品批次、过程检验、最终检验和成品入库闭环。
UPDATE `trace_product_lot` product_lot
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = product_lot.`work_order_id`
JOIN `mfg_task` glue_task ON glue_task.`work_order_id` = work_order.`work_order_id` AND glue_task.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id` AND route_item.`operation_id` = glue_task.`operation_id`
 AND route_item.`sequence_no` = 50 AND route_item.`deleted_flag` = 0
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = glue_task.`task_id`
SET product_lot.`batch_quantity` = work_order.`planned_quantity`,
    product_lot.`qualified_quantity` = work_order.`planned_quantity`,
    product_lot.`defect_quantity` = 0,
    product_lot.`status` = 'STORED',
    product_lot.`updated_by` = audit.`operator_user_id`,
    product_lot.`updated_time` = IF(work_order.`work_order_no` = 'WO-20260713-013-1',
      '2026-07-13 17:10:00', '2026-07-14 10:40:00'),
    product_lot.`version` = product_lot.`version` + 1
WHERE @seed_enabled = 1
  AND work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2')
  AND product_lot.`deleted_flag` = 0;

INSERT INTO `trace_product_lot`
  (`lot_no`, `product_id`, `work_order_id`, `batch_quantity`, `qualified_quantity`, `defect_quantity`, `status`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'LOT-LNX-T30076-20260714-0031', work_order.`product_id`, work_order.`work_order_id`, 240, 240, 0, 'RUNNING',
       CONCAT(@live_marker, ': 当日训练线在制产品批次'), 0, audit.`operator_user_id`,
       '2026-07-14 08:00:00', audit.`operator_user_id`, @live_snapshot_time, 0
FROM `mfg_work_order` work_order
JOIN `mfg_task` glue_task ON glue_task.`work_order_id` = work_order.`work_order_id` AND glue_task.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id` AND route_item.`operation_id` = glue_task.`operation_id`
 AND route_item.`sequence_no` = 50 AND route_item.`deleted_flag` = 0
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = glue_task.`task_id`
WHERE @seed_enabled = 1 AND work_order.`work_order_no` = 'WO-20260714-003-1'
  AND work_order.`deleted_flag` = 0;

INSERT INTO `trace_barcode`
  (`barcode_value`, `barcode_type`, `biz_type`, `biz_id`, `generated_rule`, `status`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('MES-LOT-', product_lot.`lot_no`), 'PRODUCT_LOT', 'TRACE_PRODUCT_LOT', product_lot.`product_lot_id`,
       'MES-LOT-{LOT_NO}', 'ENABLE', CONCAT(@live_marker, ': 当日在制产品批次条码'), 0,
       product_lot.`created_by`, product_lot.`created_time`, product_lot.`updated_by`, product_lot.`updated_time`, 0
FROM `trace_product_lot` product_lot
WHERE @seed_enabled = 1 AND product_lot.`lot_no` = 'LOT-LNX-T30076-20260714-0031'
  AND product_lot.`deleted_flag` = 0;

INSERT INTO `wms_material_batch`
  (`batch_no`, `material_id`, `production_date`, `received_date`, `quality_status`, `status`,
   `source_order_type`, `source_order_id`, `source_product_lot_id`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('FG-', product_lot.`lot_no`), product.`material_id`,
       IF(work_order.`work_order_no` = 'WO-20260713-013-1', '2026-07-13', '2026-07-14'),
       IF(work_order.`work_order_no` = 'WO-20260713-013-1', '2026-07-13', '2026-07-14'),
       'PASS', 'AVAILABLE', 'PRODUCTION', work_order.`work_order_id`, product_lot.`product_lot_id`,
       CONCAT(@live_marker, ': 最终检验合格成品批次'), 0, @warehouse_user_id,
       IF(work_order.`work_order_no` = 'WO-20260713-013-1', '2026-07-13 16:35:00', '2026-07-14 10:25:00'),
       @warehouse_user_id,
       IF(work_order.`work_order_no` = 'WO-20260713-013-1', '2026-07-13 17:20:00', '2026-07-14 10:50:00'), 0
FROM `trace_product_lot` product_lot
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = product_lot.`work_order_id`
JOIN `mdm_product` product ON product.`product_id` = product_lot.`product_id` AND product.`deleted_flag` = 0
WHERE @seed_enabled = 1
  AND work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2')
  AND product_lot.`deleted_flag` = 0;

-- 当日在制只做过程检验，不创建最终检验或成品入库。
INSERT INTO `qms_inspection_order`
  (`inspection_no`, `inspection_type`, `scheme_id`, `work_order_id`, `task_id`, `report_id`, `batch_id`,
   `inspector_id`, `inspection_result`, `status`, `remark`, `version`, `created_by`, `created_time`,
   `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'IPQC-WO-20260714-003-1', 'PROCESS', scheme.`scheme_id`, work_order.`work_order_id`, task.`task_id`,
       work_report.`report_id`, NULL, @quality_employee_id, 'PASS', 'FINISHED',
       CONCAT(@live_marker, ': T300 76速在制重量、速度和飞行抽检'), 0, @quality_user_id,
       '2026-07-14 14:10:00', @quality_user_id, '2026-07-14 14:28:00', 0
FROM `mfg_work_order` work_order
JOIN `qms_inspection_scheme` scheme
  ON scheme.`product_id` = work_order.`product_id` AND scheme.`inspection_type` = 'PROCESS'
 AND scheme.`deleted_flag` = 0
JOIN `mfg_task` task ON task.`work_order_id` = work_order.`work_order_id` AND task.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id` AND route_item.`operation_id` = task.`operation_id`
 AND route_item.`sequence_no` = 50 AND route_item.`deleted_flag` = 0
JOIN `mfg_work_report` work_report
  ON work_report.`task_id` = task.`task_id` AND work_report.`report_no` = 'WR-LIVE-20260714-003-1-050'
 AND work_report.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND work_order.`work_order_no` = 'WO-20260714-003-1'
  AND work_order.`deleted_flag` = 0;

INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `defect_description`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT inspection_order.`inspection_id`, item.`item_id`,
       CASE item.`item_code`
         WHEN 'QI-FINISHED-WEIGHT' THEN '4.89'
         WHEN 'QI-SPEED-GRADE' THEN '76'
         WHEN 'QI-FLIGHT-STABILITY' THEN '455'
       END,
       'PASS', NULL, CONCAT(@live_marker, ': 当日在制自然离散实测值'), 0,
       @quality_user_id, inspection_order.`updated_time`, @quality_user_id, inspection_order.`updated_time`, 0
FROM `qms_inspection_order` inspection_order
JOIN `qms_inspection_scheme_item` scheme_item
  ON scheme_item.`scheme_id` = inspection_order.`scheme_id` AND scheme_item.`deleted_flag` = 0
JOIN `qms_inspection_item` item ON item.`item_id` = scheme_item.`item_id` AND item.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND inspection_order.`inspection_no` = 'IPQC-WO-20260714-003-1'
  AND inspection_order.`deleted_flag` = 0;

INSERT INTO `qms_inspection_order`
  (`inspection_no`, `inspection_type`, `scheme_id`, `work_order_id`, `task_id`, `report_id`, `batch_id`,
   `inspector_id`, `inspection_result`, `status`, `remark`, `version`, `created_by`, `created_time`,
   `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('FQC-LIVE-', work_order.`work_order_no`), 'FINAL', scheme.`scheme_id`, work_order.`work_order_id`,
       task.`task_id`, work_report.`report_id`, finished_batch.`batch_id`, @quality_employee_id, 'PASS', 'FINISHED',
       CONCAT(@live_marker, ': 最终外观、球筒和标签放行'), 0, @quality_user_id,
       IF(work_order.`work_order_no` = 'WO-20260713-013-1', '2026-07-13 16:35:00', '2026-07-14 10:20:00'),
       @quality_user_id,
       IF(work_order.`work_order_no` = 'WO-20260713-013-1', '2026-07-13 16:50:00', '2026-07-14 10:35:00'), 0
FROM `mfg_work_order` work_order
JOIN `qms_inspection_scheme` scheme
  ON scheme.`product_id` = work_order.`product_id` AND scheme.`inspection_type` = 'FINAL'
 AND scheme.`deleted_flag` = 0
JOIN `mfg_task` task ON task.`work_order_id` = work_order.`work_order_id` AND task.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id` AND route_item.`operation_id` = task.`operation_id`
 AND route_item.`sequence_no` = 70 AND route_item.`deleted_flag` = 0
JOIN `mfg_work_report` work_report
  ON work_report.`task_id` = task.`task_id` AND work_report.`remark` LIKE CONCAT(@live_marker, ':%')
 AND work_report.`deleted_flag` = 0
JOIN `trace_product_lot` product_lot
  ON product_lot.`work_order_id` = work_order.`work_order_id` AND product_lot.`deleted_flag` = 0
JOIN `wms_material_batch` finished_batch
  ON finished_batch.`source_product_lot_id` = product_lot.`product_lot_id` AND finished_batch.`deleted_flag` = 0
WHERE @seed_enabled = 1
  AND work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2')
  AND work_order.`deleted_flag` = 0;

INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `defect_description`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT inspection_order.`inspection_id`, item.`item_id`, '合格', 'PASS', NULL,
       CONCAT(@live_marker, ': 完工批次最终外观与包装合格'), 0,
       @quality_user_id, inspection_order.`updated_time`, @quality_user_id, inspection_order.`updated_time`, 0
FROM `qms_inspection_order` inspection_order
JOIN `qms_inspection_scheme_item` scheme_item
  ON scheme_item.`scheme_id` = inspection_order.`scheme_id` AND scheme_item.`deleted_flag` = 0
JOIN `qms_inspection_item` item ON item.`item_id` = scheme_item.`item_id` AND item.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND inspection_order.`inspection_no` LIKE 'FQC-LIVE-%'
  AND inspection_order.`deleted_flag` = 0;

INSERT INTO `wms_inbound_order`
  (`inbound_no`, `inbound_type`, `warehouse_id`, `source_order_type`, `source_order_id`, `audit_status`, `inbound_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('IB-FG-LIVE-', work_order.`work_order_no`), 'PRODUCTION', @fg_warehouse_id, 'WORK_ORDER',
       work_order.`work_order_id`, 'APPROVED',
       IF(work_order.`work_order_no` = 'WO-20260713-013-1', '2026-07-13 17:20:00', '2026-07-14 10:50:00'),
       CONCAT(@live_marker, ': FQC合格成品入库'), 0, @warehouse_user_id,
       IF(work_order.`work_order_no` = 'WO-20260713-013-1', '2026-07-13 17:00:00', '2026-07-14 10:40:00'),
       @warehouse_user_id,
       IF(work_order.`work_order_no` = 'WO-20260713-013-1', '2026-07-13 17:20:00', '2026-07-14 10:50:00'), 0
FROM `mfg_work_order` work_order
WHERE @seed_enabled = 1
  AND work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2')
  AND work_order.`deleted_flag` = 0;

INSERT INTO `wms_inbound_detail`
  (`inbound_id`, `material_id`, `batch_id`, `location_id`, `inbound_quantity`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT inbound_order.`inbound_id`, product.`material_id`, finished_batch.`batch_id`, @fg_location_id,
       product_lot.`qualified_quantity`, CONCAT(@live_marker, ': 合格数量入成品库'), 0,
       @warehouse_user_id, inbound_order.`created_time`, @warehouse_user_id, inbound_order.`inbound_time`, 0
FROM `wms_inbound_order` inbound_order
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = inbound_order.`source_order_id`
JOIN `trace_product_lot` product_lot
  ON product_lot.`work_order_id` = work_order.`work_order_id` AND product_lot.`deleted_flag` = 0
JOIN `mdm_product` product ON product.`product_id` = product_lot.`product_id` AND product.`deleted_flag` = 0
JOIN `wms_material_batch` finished_batch
  ON finished_batch.`source_product_lot_id` = product_lot.`product_lot_id` AND finished_batch.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND inbound_order.`remark` LIKE CONCAT(@live_marker, ':%')
  AND inbound_order.`deleted_flag` = 0;

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`, `material_id`, `batch_id`,
   `quantity`, `direction`, `operation_time`, `remark`, `version`, `created_by`, `created_time`,
   `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('MV-L14-FG-', LPAD(detail.`detail_id`, 6, '0')), 'INBOUND', 'INBOUND_ORDER', detail.`inbound_id`,
       detail.`material_id`, detail.`batch_id`, detail.`inbound_quantity`, 'IN', inbound_order.`inbound_time`,
       CONCAT(@live_marker, ': 完工成品入库'), 0, @warehouse_user_id, inbound_order.`inbound_time`,
       @warehouse_user_id, inbound_order.`inbound_time`, 0
FROM `wms_inbound_detail` detail
JOIN `wms_inbound_order` inbound_order
  ON inbound_order.`inbound_id` = detail.`inbound_id`
 AND inbound_order.`remark` LIKE CONCAT(@live_marker, ':%') AND inbound_order.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND detail.`deleted_flag` = 0;

-- 6. 完工外箱、留样、托盘条码与新增报工物料追溯。
INSERT INTO `trace_barcode`
  (`barcode_value`, `barcode_type`, `biz_type`, `biz_id`, `generated_rule`, `status`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('MES-CTN-', product_lot.`lot_no`, '-', LPAD(number_record.`n`, 4, '0')),
       'OUTER_CARTON', 'TRACE_PRODUCT_LOT', product_lot.`product_lot_id`, 'MES-CTN-{LOT_NO}-{SEQUENCE}',
       'ENABLE', CONCAT(@live_marker, ': 25筒运输外箱；尾箱按实际数量标识'), 0,
       product_lot.`updated_by`, product_lot.`updated_time`, product_lot.`updated_by`, product_lot.`updated_time`, 0
FROM `trace_product_lot` product_lot
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = product_lot.`work_order_id`
JOIN `tmp_live_number` number_record ON number_record.`n` <= CEILING(product_lot.`qualified_quantity` / 25)
WHERE @seed_enabled = 1
  AND work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2')
  AND product_lot.`status` = 'STORED' AND product_lot.`deleted_flag` = 0;

INSERT INTO `trace_barcode`
  (`barcode_value`, `barcode_type`, `biz_type`, `biz_id`, `generated_rule`, `status`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('MES-SAMPLE-', product_lot.`lot_no`), 'RETAIN_SAMPLE', 'TRACE_PRODUCT_LOT', product_lot.`product_lot_id`,
       'MES-SAMPLE-{LOT_NO}', 'ENABLE', CONCAT(@live_marker, ': 完工成品留样标识'), 0,
       @quality_user_id, product_lot.`updated_time`, @quality_user_id, product_lot.`updated_time`, 0
FROM `trace_product_lot` product_lot
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = product_lot.`work_order_id`
WHERE @seed_enabled = 1
  AND work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2')
  AND product_lot.`status` = 'STORED' AND product_lot.`deleted_flag` = 0;

INSERT INTO `trace_barcode`
  (`barcode_value`, `barcode_type`, `biz_type`, `biz_id`, `generated_rule`, `status`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('MES-PLT-', production_order.`production_order_no`), 'PALLET', 'MFG_PRODUCTION_ORDER',
       production_order.`production_order_id`, 'MES-PLT-{PRODUCTION_ORDER_NO}', 'ENABLE',
       CONCAT(@live_marker, ': 完工订单发运托盘标识'), 0, @warehouse_user_id,
       production_order.`updated_time`, @warehouse_user_id, production_order.`updated_time`, 0
FROM `mfg_production_order` production_order
WHERE @seed_enabled = 1
  AND production_order.`production_order_no` IN ('MO-20260710-013', 'MO-20260709-024')
  AND production_order.`status` = 'FINISHED' AND production_order.`deleted_flag` = 0;

INSERT INTO `trace_material_usage`
  (`product_lot_id`, `barcode_id`, `work_order_id`, `report_id`, `material_id`, `batch_id`, `used_quantity`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT product_lot.`product_lot_id`, lot_barcode.`barcode_id`, task.`work_order_id`, report_material.`report_id`,
       report_material.`material_id`, report_material.`batch_id`, report_material.`used_quantity`,
       CONCAT(@live_marker, ': 增量报工实际用料追溯，不再次扣库存'), 0,
       report_material.`created_by`, report_material.`created_time`, report_material.`updated_by`, report_material.`updated_time`, 0
FROM `mfg_report_material` report_material
JOIN `mfg_work_report` work_report
  ON work_report.`report_id` = report_material.`report_id` AND work_report.`deleted_flag` = 0
JOIN `mfg_task` task ON task.`task_id` = work_report.`task_id` AND task.`deleted_flag` = 0
JOIN `trace_product_lot` product_lot
  ON product_lot.`work_order_id` = task.`work_order_id` AND product_lot.`deleted_flag` = 0
JOIN `trace_barcode` lot_barcode
  ON lot_barcode.`biz_type` = 'TRACE_PRODUCT_LOT' AND lot_barcode.`biz_id` = product_lot.`product_lot_id`
 AND lot_barcode.`barcode_type` = 'PRODUCT_LOT' AND lot_barcode.`deleted_flag` = 0
WHERE @seed_enabled = 1 AND report_material.`remark` LIKE CONCAT(@live_marker, ':%')
  AND report_material.`deleted_flag` = 0;

-- 7. 由全部库存流水重算余额，禁止直接凑数修改库存。
DROP TEMPORARY TABLE IF EXISTS `tmp_live_movement_balance`;
CREATE TEMPORARY TABLE `tmp_live_movement_balance` AS
SELECT movement.`material_id`, movement.`batch_id`,
       SUM(IF(movement.`direction` = 'IN', movement.`quantity`, -movement.`quantity`)) AS `balance_quantity`,
       MIN(movement.`operation_time`) AS `first_operation_time`, MAX(movement.`operation_time`) AS `last_operation_time`
FROM `wms_stock_movement` movement
WHERE movement.`deleted_flag` = 0
GROUP BY movement.`material_id`, movement.`batch_id`;

UPDATE `wms_inventory` inventory
JOIN `tmp_live_movement_balance` balance
  ON balance.`material_id` = inventory.`material_id` AND balance.`batch_id` = inventory.`batch_id`
SET inventory.`available_quantity` = balance.`balance_quantity`,
    inventory.`updated_by` = @warehouse_user_id,
    inventory.`updated_time` = balance.`last_operation_time`,
    inventory.`version` = inventory.`version` + 1
WHERE @seed_enabled = 1
  AND ABS(inventory.`available_quantity` - balance.`balance_quantity`) > 0.0001
  AND inventory.`deleted_flag` = 0;

INSERT INTO `wms_inventory`
  (`warehouse_id`, `location_id`, `material_id`, `batch_id`, `available_quantity`, `locked_quantity`,
   `inspection_quantity`, `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT IF(material.`material_type` = 'FINISHED', @fg_warehouse_id, @raw_warehouse_id),
       IF(material.`material_type` = 'FINISHED', @fg_location_id, @raw_location_id),
       balance.`material_id`, balance.`batch_id`, balance.`balance_quantity`, 0, 0,
       CONCAT(@live_marker, ': 由库存流水汇总的新增批次余额'), 0, @warehouse_user_id,
       balance.`first_operation_time`, @warehouse_user_id, balance.`last_operation_time`, 0
FROM `tmp_live_movement_balance` balance
JOIN `mdm_material` material ON material.`material_id` = balance.`material_id` AND material.`deleted_flag` = 0
WHERE @seed_enabled = 1
  AND NOT EXISTS (
    SELECT 1 FROM `wms_inventory` inventory
    WHERE inventory.`material_id` = balance.`material_id` AND inventory.`batch_id` = balance.`batch_id`
      AND inventory.`deleted_flag` = 0
  );

-- 8. 2026-07-14 开班点检、当前设备状态和两条补货建议。
INSERT INTO `eam_spot_check_record`
  (`plan_id`, `equipment_id`, `checker_id`, `check_time`, `check_result`, `abnormal_description`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT plan.`plan_id`, plan.`equipment_id`, @equipment_employee_id,
       TIMESTAMPADD(SECOND, MOD(plan.`equipment_id`, 6) * 20, TIMESTAMP('2026-07-14 07:44:00')),
       'PASS', NULL, CONCAT(@live_marker, ': 07-14开班点检正常'), 0, @equipment_user_id,
       TIMESTAMPADD(SECOND, MOD(plan.`equipment_id`, 6) * 20, TIMESTAMP('2026-07-14 07:44:00')),
       @equipment_user_id,
       TIMESTAMPADD(SECOND, MOD(plan.`equipment_id`, 6) * 20, TIMESTAMP('2026-07-14 07:44:00')), 0
FROM `eam_spot_check_plan` plan
WHERE @seed_enabled = 1 AND plan.`status` = 'ENABLE' AND plan.`deleted_flag` = 0;

UPDATE `eam_equipment`
SET `status` = IF(`equipment_code` IN ('EQ-TR-PACK-01', 'EQ-MA-PACK-01'), 'IDLE', 'RUNNING'),
    `updated_by` = @equipment_user_id, `updated_time` = @live_snapshot_time,
    `version` = `version` + 1
WHERE @seed_enabled = 1 AND `deleted_flag` = 0;

INSERT INTO `eam_equipment_status_log`
  (`equipment_id`, `running_status`, `downtime_type`, `alert_level`, `station_id`, `collected_time`,
   `description`, `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT equipment.`equipment_id`, equipment.`status`,
       IF(equipment.`status` = 'IDLE', 'PLANNED', NULL), IF(equipment.`status` = 'IDLE', 'LOW', NULL),
       equipment.`station_id`, @live_snapshot_time,
       IF(equipment.`status` = 'IDLE', '当前无包装任务，设备受控待机', '点检正常，按当日生产节拍运行'),
       CONCAT(@live_marker, ': 当前设备运行快照'), 0, @equipment_user_id, @live_snapshot_time,
       @equipment_user_id, @live_snapshot_time, 0
FROM `eam_equipment` equipment
WHERE @seed_enabled = 1 AND equipment.`deleted_flag` = 0;

INSERT INTO `wms_replenishment_suggestion`
  (`suggestion_no`, `material_id`, `required_quantity`, `available_quantity`, `suggested_quantity`,
   `source_type`, `status`, `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT IF(material.`material_code` = 'RM-CORK-NATURAL-78', 'RPS-20260714-001', 'RPS-20260714-002'),
       material.`material_id`, ROUND(1000 * bom_item.`quantity` * (1 + bom_item.`loss_rate`), 4),
       COALESCE(inventory_balance.`available_quantity`, 0),
       GREATEST(ROUND(1000 * bom_item.`quantity` * (1 + bom_item.`loss_rate`), 4)
         - COALESCE(inventory_balance.`available_quantity`, 0), 0),
       'INVENTORY_WARNING', 'SUBMITTED',
       CONCAT(@live_marker, ': M700 78速标准1000筒备料覆盖不足，建议补足一个生产批次'), 0,
       @warehouse_user_id, '2026-07-14 14:35:00', @warehouse_user_id, '2026-07-14 14:35:00', 0
FROM `mdm_product` product
JOIN `mdm_bom` bom
  ON bom.`product_id` = product.`product_id` AND bom.`status` = 'RELEASED' AND bom.`deleted_flag` = 0
JOIN `mdm_bom_item` bom_item ON bom_item.`bom_id` = bom.`bom_id` AND bom_item.`deleted_flag` = 0
JOIN `mdm_material` material ON material.`material_id` = bom_item.`material_id` AND material.`deleted_flag` = 0
LEFT JOIN (
  SELECT `material_id`, SUM(`available_quantity`) AS `available_quantity`
  FROM `wms_inventory` WHERE `deleted_flag` = 0 GROUP BY `material_id`
) inventory_balance ON inventory_balance.`material_id` = material.`material_id`
WHERE @seed_enabled = 1 AND product.`product_code` = 'PROD-LNX-M700-78'
  AND material.`material_code` IN ('RM-CORK-NATURAL-78', 'PKG-LABEL-LNX-M700-78')
  AND ROUND(1000 * bom_item.`quantity` * (1 + bom_item.`loss_rate`), 4)
      > COALESCE(inventory_balance.`available_quantity`, 0);

-- 9. 既有状态、审批、操作日志按岗位重新归属。
UPDATE `sys_status_change_log`
SET `changed_by` = @planner_user_id, `created_by` = @planner_user_id
WHERE @seed_enabled = 1 AND `remark` = @tx_marker
  AND `biz_type` IN ('MFG_PRODUCTION_ORDER', 'MFG_PRODUCTION_PLAN') AND `deleted_flag` = 0;
UPDATE `sys_status_change_log`
SET `changed_by` = @manager_user_id, `created_by` = @manager_user_id
WHERE @seed_enabled = 1 AND `remark` = @tx_marker AND `biz_type` = 'MFG_WORK_ORDER' AND `deleted_flag` = 0;
UPDATE `sys_status_change_log` status_log
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = status_log.`biz_id`
SET status_log.`changed_by` = audit.`operator_user_id`, status_log.`created_by` = audit.`operator_user_id`
WHERE @seed_enabled = 1 AND status_log.`remark` = @tx_marker
  AND status_log.`biz_type` = 'MFG_TASK' AND status_log.`deleted_flag` = 0;
UPDATE `sys_status_change_log` status_log
JOIN `mfg_work_report` work_report ON work_report.`report_id` = status_log.`biz_id`
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = work_report.`task_id`
SET status_log.`changed_by` = audit.`supervisor_user_id`, status_log.`created_by` = audit.`supervisor_user_id`
WHERE @seed_enabled = 1 AND status_log.`remark` = @tx_marker
  AND status_log.`biz_type` = 'MFG_WORK_REPORT' AND status_log.`deleted_flag` = 0;
UPDATE `sys_status_change_log`
SET `changed_by` = @manager_user_id, `created_by` = @manager_user_id
WHERE @seed_enabled = 1 AND `remark` = @tx_marker
  AND `biz_type` IN ('MFG_ANDON_ALERT', 'MFG_EXCEPTION') AND `deleted_flag` = 0;
UPDATE `sys_status_change_log`
SET `changed_by` = @warehouse_user_id, `created_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` = @tx_marker AND `biz_type` LIKE 'WMS_%' AND `deleted_flag` = 0;
UPDATE `sys_status_change_log`
SET `changed_by` = @quality_user_id, `created_by` = @quality_user_id
WHERE @seed_enabled = 1 AND `remark` = @tx_marker AND `biz_type` LIKE 'QMS_%' AND `deleted_flag` = 0;
UPDATE `sys_status_change_log`
SET `changed_by` = @equipment_user_id, `created_by` = @equipment_user_id
WHERE @seed_enabled = 1 AND `remark` = @tx_marker AND `biz_type` LIKE 'EAM_%' AND `deleted_flag` = 0;

UPDATE `sys_approval_record`
SET `approver_id` = @manager_user_id, `created_by` = @manager_user_id
WHERE @seed_enabled = 1 AND `remark` = @tx_marker AND `biz_type` = 'MFG_PRODUCTION_PLAN' AND `deleted_flag` = 0;
UPDATE `sys_approval_record` approval_record
JOIN `mfg_work_report` work_report ON work_report.`report_id` = approval_record.`biz_id`
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = work_report.`task_id`
SET approval_record.`approver_id` = audit.`supervisor_user_id`, approval_record.`created_by` = audit.`supervisor_user_id`
WHERE @seed_enabled = 1 AND approval_record.`remark` = @tx_marker
  AND approval_record.`biz_type` = 'MFG_WORK_REPORT' AND approval_record.`deleted_flag` = 0;
UPDATE `sys_approval_record`
SET `approver_id` = @warehouse_user_id, `created_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` = @tx_marker AND `biz_type` LIKE 'WMS_%' AND `deleted_flag` = 0;
UPDATE `sys_approval_record`
SET `approver_id` = @quality_user_id, `created_by` = @quality_user_id
WHERE @seed_enabled = 1 AND `remark` = @tx_marker AND `biz_type` LIKE 'QMS_%' AND `deleted_flag` = 0;

UPDATE `sys_operation_log`
SET `user_id` = @planner_user_id, `created_by` = @planner_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%')
  AND `module_code` IN ('mfg_production_order', 'mfg_production_plan') AND `deleted_flag` = 0;
UPDATE `sys_operation_log`
SET `user_id` = @manager_user_id, `created_by` = @manager_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%')
  AND `module_code` IN ('mfg_work_order', 'mfg_andon_alert', 'mfg_exception') AND `deleted_flag` = 0;
UPDATE `sys_operation_log` operation_log
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = operation_log.`biz_id`
SET operation_log.`user_id` = audit.`operator_user_id`, operation_log.`created_by` = audit.`operator_user_id`
WHERE @seed_enabled = 1 AND operation_log.`remark` LIKE CONCAT(@tx_marker, ':%')
  AND operation_log.`module_code` = 'mfg_task' AND operation_log.`deleted_flag` = 0;
UPDATE `sys_operation_log` operation_log
JOIN `mfg_work_report` work_report ON work_report.`report_id` = operation_log.`biz_id`
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = work_report.`task_id`
SET operation_log.`user_id` = audit.`supervisor_user_id`, operation_log.`created_by` = audit.`supervisor_user_id`
WHERE @seed_enabled = 1 AND operation_log.`remark` LIKE CONCAT(@tx_marker, ':%')
  AND operation_log.`module_code` = 'mfg_work_report' AND operation_log.`deleted_flag` = 0;
UPDATE `sys_operation_log`
SET `user_id` = @warehouse_user_id, `created_by` = @warehouse_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%')
  AND `module_code` LIKE 'wms_%' AND `deleted_flag` = 0;
UPDATE `sys_operation_log`
SET `user_id` = @quality_user_id, `created_by` = @quality_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%')
  AND `module_code` LIKE 'qms_%' AND `deleted_flag` = 0;
UPDATE `sys_operation_log`
SET `user_id` = @equipment_user_id, `created_by` = @equipment_user_id
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@tx_marker, ':%')
  AND `module_code` LIKE 'eam_%' AND `deleted_flag` = 0;

-- 10. 新增滚动状态、审批和操作审计。
DROP TEMPORARY TABLE IF EXISTS `tmp_live_status_event`;
CREATE TEMPORARY TABLE `tmp_live_status_event` (
  `biz_type` VARCHAR(32) NOT NULL,
  `biz_id` BIGINT UNSIGNED NOT NULL,
  `old_status` VARCHAR(32) NOT NULL,
  `new_status` VARCHAR(32) NOT NULL,
  `change_reason` VARCHAR(64) NOT NULL,
  `changed_time` DATETIME NOT NULL,
  `actor_user_id` BIGINT UNSIGNED NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

INSERT INTO `tmp_live_status_event`
SELECT 'MFG_PRODUCTION_ORDER', `production_order_id`,
       IF(`production_order_no` = 'MO-20260710-003', 'RELEASED', 'RUNNING'), `status`, 'LIVE_ORDER_ROLLFORWARD',
       `updated_time`, @planner_user_id
FROM `mfg_production_order`
WHERE @seed_enabled = 1
  AND `production_order_no` IN ('MO-20260710-003', 'MO-20260710-013', 'MO-20260709-024') AND `deleted_flag` = 0;
INSERT INTO `tmp_live_status_event`
SELECT 'MFG_PRODUCTION_PLAN', plan.`plan_id`,
       IF(production_order.`production_order_no` = 'MO-20260710-003', 'RELEASED', 'RUNNING'), plan.`status`,
       'LIVE_PLAN_ROLLFORWARD', plan.`updated_time`, @planner_user_id
FROM `mfg_production_plan` plan
JOIN `mfg_production_order` production_order ON production_order.`production_order_id` = plan.`production_order_id`
WHERE @seed_enabled = 1
  AND production_order.`production_order_no` IN ('MO-20260710-003', 'MO-20260710-013', 'MO-20260709-024')
  AND plan.`deleted_flag` = 0;
INSERT INTO `tmp_live_status_event`
SELECT 'MFG_WORK_ORDER', work_order.`work_order_id`,
       IF(work_order.`work_order_no` = 'WO-20260714-003-1', 'RELEASED', 'RUNNING'), work_order.`status`,
       'LIVE_WORK_ORDER_ROLLFORWARD', work_order.`updated_time`, @manager_user_id
FROM `mfg_work_order` work_order
WHERE @seed_enabled = 1
  AND work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2', 'WO-20260714-003-1')
  AND work_order.`deleted_flag` = 0;
INSERT INTO `tmp_live_status_event`
SELECT 'MFG_TASK', task.`task_id`,
       IF(target.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2') AND target.`sequence_no` = 50,
          'RUNNING', 'WAIT_START'), target.`target_status`, 'LIVE_TASK_ROLLFORWARD', target.`updated_time`,
       audit.`operator_user_id`
FROM `tmp_live_task_target` target
JOIN `mfg_work_order` work_order ON work_order.`work_order_no` = target.`work_order_no`
JOIN `mfg_task` task ON task.`work_order_id` = work_order.`work_order_id` AND task.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id` AND route_item.`operation_id` = task.`operation_id`
 AND route_item.`sequence_no` = target.`sequence_no` AND route_item.`deleted_flag` = 0
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = task.`task_id`
WHERE @seed_enabled = 1;
INSERT INTO `tmp_live_status_event`
SELECT 'MFG_WORK_REPORT', work_report.`report_id`, 'SUBMITTED', 'APPROVED', 'LIVE_REPORT_AUDIT',
       work_report.`updated_time`, audit.`supervisor_user_id`
FROM `mfg_work_report` work_report
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = work_report.`task_id`
WHERE @seed_enabled = 1 AND work_report.`remark` LIKE CONCAT(@live_marker, ':%') AND work_report.`deleted_flag` = 0;
INSERT INTO `tmp_live_status_event`
SELECT 'TRACE_PRODUCT_LOT', product_lot.`product_lot_id`,
       IF(product_lot.`lot_no` = 'LOT-LNX-T30076-20260714-0031', 'NOT_CREATED', 'RUNNING'), product_lot.`status`,
       'LIVE_PRODUCT_LOT_ROLLFORWARD', product_lot.`updated_time`, product_lot.`updated_by`
FROM `trace_product_lot` product_lot
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = product_lot.`work_order_id`
WHERE @seed_enabled = 1
  AND work_order.`work_order_no` IN ('WO-20260713-013-1', 'WO-20260713-024-2', 'WO-20260714-003-1')
  AND product_lot.`deleted_flag` = 0;
INSERT INTO `tmp_live_status_event`
SELECT 'WMS_OUTBOUND_ORDER', `outbound_id`, 'SUBMITTED', `audit_status`, 'LIVE_CONFIRM_OUTBOUND',
       `updated_time`, @warehouse_user_id FROM `wms_outbound_order`
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@live_marker, ':%') AND `deleted_flag` = 0;
INSERT INTO `tmp_live_status_event`
SELECT 'WMS_RETURN_ORDER', `return_id`, 'SUBMITTED', `audit_status`, 'LIVE_CONFIRM_RETURN',
       `updated_time`, @warehouse_user_id FROM `wms_return_order`
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@live_marker, ':%') AND `deleted_flag` = 0;
INSERT INTO `tmp_live_status_event`
SELECT 'WMS_INBOUND_ORDER', `inbound_id`, 'SUBMITTED', `audit_status`, 'LIVE_CONFIRM_INBOUND',
       `updated_time`, @warehouse_user_id FROM `wms_inbound_order`
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@live_marker, ':%') AND `deleted_flag` = 0;
INSERT INTO `tmp_live_status_event`
SELECT 'QMS_INSPECTION_ORDER', `inspection_id`, 'DRAFT', `status`, 'LIVE_SUBMIT_INSPECTION',
       `updated_time`, @quality_user_id FROM `qms_inspection_order`
WHERE @seed_enabled = 1 AND `remark` LIKE CONCAT(@live_marker, ':%') AND `deleted_flag` = 0;

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT event_record.`biz_type`, event_record.`biz_id`, event_record.`old_status`, event_record.`new_status`,
       event_record.`change_reason`, event_record.`actor_user_id`, event_record.`changed_time`, @live_marker, 0,
       event_record.`actor_user_id`, event_record.`changed_time`, event_record.`actor_user_id`, event_record.`changed_time`, 0
FROM `tmp_live_status_event` event_record
WHERE @seed_enabled = 1;

INSERT INTO `sys_approval_record`
  (`biz_type`, `biz_id`, `approval_node`, `approver_id`, `approval_result`, `approval_comment`, `approval_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'MFG_WORK_REPORT', work_report.`report_id`, 'REPORT_AUDIT', audit.`supervisor_user_id`, 'APPROVED',
       '数量、工时和岗位归属审核通过', work_report.`updated_time`, @live_marker, 0,
       audit.`supervisor_user_id`, work_report.`updated_time`, audit.`supervisor_user_id`, work_report.`updated_time`, 0
FROM `mfg_work_report` work_report
JOIN `tmp_live_task_audit` audit ON audit.`task_id` = work_report.`task_id`
WHERE @seed_enabled = 1 AND work_report.`remark` LIKE CONCAT(@live_marker, ':%') AND work_report.`deleted_flag` = 0;
INSERT INTO `sys_approval_record`
  (`biz_type`, `biz_id`, `approval_node`, `approver_id`, `approval_result`, `approval_comment`, `approval_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT status_event.`biz_type`, status_event.`biz_id`,
       CASE status_event.`biz_type` WHEN 'WMS_OUTBOUND_ORDER' THEN 'OUTBOUND_CONFIRM'
         WHEN 'WMS_RETURN_ORDER' THEN 'RETURN_CONFIRM' ELSE 'INBOUND_CONFIRM' END,
       @warehouse_user_id, 'APPROVED', '数量、批次和来源单据核对通过', status_event.`changed_time`,
       @live_marker, 0, @warehouse_user_id, status_event.`changed_time`, @warehouse_user_id, status_event.`changed_time`, 0
FROM `tmp_live_status_event` status_event
WHERE @seed_enabled = 1 AND status_event.`biz_type` IN ('WMS_OUTBOUND_ORDER', 'WMS_RETURN_ORDER', 'WMS_INBOUND_ORDER');
INSERT INTO `sys_approval_record`
  (`biz_type`, `biz_id`, `approval_node`, `approver_id`, `approval_result`, `approval_comment`, `approval_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'QMS_INSPECTION_ORDER', inspection_order.`inspection_id`, 'INSPECTION_RELEASE', @quality_user_id, 'APPROVED',
       '检验项目和实测结果复核通过', inspection_order.`updated_time`, @live_marker, 0,
       @quality_user_id, inspection_order.`updated_time`, @quality_user_id, inspection_order.`updated_time`, 0
FROM `qms_inspection_order` inspection_order
WHERE @seed_enabled = 1 AND inspection_order.`remark` LIKE CONCAT(@live_marker, ':%')
  AND inspection_order.`deleted_flag` = 0;

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `request_ip`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT status_event.`actor_user_id`, LEFT(status_event.`change_reason`, 32), LOWER(status_event.`biz_type`),
       status_event.`biz_id`, '127.0.0.1', status_event.`changed_time`, CONCAT(@live_marker, ': 岗位业务操作'), 0,
       status_event.`actor_user_id`, status_event.`changed_time`, status_event.`actor_user_id`, status_event.`changed_time`, 0
FROM `tmp_live_status_event` status_event
WHERE @seed_enabled = 1;

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `request_ip`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @equipment_user_id, 'SPOT_CHECK', 'eam_spot_check_record', spot_record.`record_id`, '127.0.0.1',
       spot_record.`check_time`, CONCAT(@live_marker, ': 设备工程师完成开班点检'), 0,
       @equipment_user_id, spot_record.`check_time`, @equipment_user_id, spot_record.`check_time`, 0
FROM `eam_spot_check_record` spot_record
WHERE @seed_enabled = 1 AND spot_record.`remark` LIKE CONCAT(@live_marker, ':%')
  AND spot_record.`deleted_flag` = 0;

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `request_ip`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @admin_user_id, 'ROLLFORWARD_RENIX_14D', 'db_seed', 20260714, '127.0.0.1',
       '2026-07-14 15:00:00', @live_marker, 0, @admin_user_id, '2026-07-14 15:00:00',
       @admin_user_id, '2026-07-14 15:00:00', 0
WHERE @seed_enabled = 1;

-- 提交前硬门禁；断言失败时故意访问不存在表，连接结束后事务自动回滚。
SET @live_postcondition_ok = IF(
  (SELECT COUNT(*) FROM `mfg_production_order` WHERE `deleted_flag` = 0) = 33
  AND (SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0) = 22
  AND (SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) = 1
  AND (SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'RELEASED' AND `deleted_flag` = 0) = 2
  AND (SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'DRAFT' AND `deleted_flag` = 0) = 8
  AND (SELECT COUNT(*) FROM `mfg_production_plan` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0) = 22
  AND (SELECT COUNT(*) FROM `mfg_production_plan` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) = 1
  AND (SELECT COUNT(*) FROM `mfg_production_plan` WHERE `status` = 'RELEASED' AND `deleted_flag` = 0) = 2
  AND (SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0) = 29
  AND (SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) = 1
  AND (SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'RELEASED' AND `deleted_flag` = 0) = 2
  AND (SELECT COUNT(*) FROM `mfg_task` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0) = 207
  AND (SELECT COUNT(*) FROM `mfg_task` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) = 1
  AND (SELECT COUNT(*) FROM `mfg_task` WHERE `status` = 'WAIT_START' AND `deleted_flag` = 0) = 16
  AND (SELECT COUNT(*) FROM `trace_product_lot` WHERE `status` = 'STORED' AND `deleted_flag` = 0) = 29
  AND (SELECT COUNT(*) FROM `trace_product_lot` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) = 1
  AND (SELECT COUNT(*) FROM `eam_spot_check_record`
       WHERE `check_time` >= '2026-07-01' AND `check_time` < '2026-07-15' AND `deleted_flag` = 0) = 144
  AND (SELECT COUNT(*) FROM `wms_replenishment_suggestion`
       WHERE `remark` LIKE CONCAT(@live_marker, ':%') AND `status` = 'SUBMITTED' AND `deleted_flag` = 0) = 2
  AND (SELECT COUNT(*) FROM `wms_inventory`
       WHERE (`available_quantity` < 0 OR `locked_quantity` < 0 OR `inspection_quantity` < 0) AND `deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM (
    SELECT movement.`material_id`, movement.`batch_id`,
           SUM(IF(movement.`direction` = 'IN', movement.`quantity`, -movement.`quantity`)) AS `movement_balance`,
           COALESCE(inventory.`available_quantity`, 0) AS `inventory_balance`
    FROM `wms_stock_movement` movement
    LEFT JOIN `wms_inventory` inventory
      ON inventory.`material_id` = movement.`material_id` AND inventory.`batch_id` = movement.`batch_id`
     AND inventory.`deleted_flag` = 0
    WHERE movement.`deleted_flag` = 0
    GROUP BY movement.`material_id`, movement.`batch_id`, inventory.`available_quantity`
    HAVING ABS(`movement_balance` - `inventory_balance`) > 0.0001
  ) invalid_inventory) = 0
  AND (SELECT COUNT(*) FROM `mfg_work_order_material`
       WHERE `work_order_id` = (SELECT `work_order_id` FROM `mfg_work_order`
                               WHERE `work_order_no` = 'WO-20260714-003-1' AND `deleted_flag` = 0)
         AND (`status` <> 'ISSUED' OR `shortage_quantity` <> 0 OR `issued_quantity` <> `required_quantity`)
         AND `deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM (
    SELECT task.`task_id`, task.`completed_quantity`, task.`defect_quantity`,
           COALESCE(SUM(work_report.`qualified_quantity` + work_report.`defect_quantity`), 0) AS `reported_quantity`
    FROM `mfg_task` task
    LEFT JOIN `mfg_work_report` work_report
      ON work_report.`task_id` = task.`task_id` AND work_report.`audit_status` = 'APPROVED'
     AND work_report.`deleted_flag` = 0
    WHERE task.`deleted_flag` = 0
    GROUP BY task.`task_id`, task.`completed_quantity`, task.`defect_quantity`
    HAVING ABS(task.`completed_quantity` - `reported_quantity`) > 0.0001
  ) invalid_report) = 0
  AND (SELECT COUNT(*) FROM `mfg_work_report`
       WHERE DATE(`report_time`) IN ('2026-07-05', '2026-07-12') AND `deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM `mfg_schedule_scenario` WHERE `scenario_id` IN (2, 3, 4) AND `deleted_flag` = 0) = 3
  AND (SELECT COUNT(*) FROM `mfg_schedule_item` WHERE `scenario_id` IN (2, 3, 4) AND `deleted_flag` = 0) = 23
  AND (SELECT MD5(GROUP_CONCAT(CONCAT_WS('#', `scenario_id`, `scenario_no`, `status`, `order_count`,
       `scheduled_order_count`, `conflict_order_count`, `late_order_count`, `total_load_minutes`,
       COALESCE(`remark`, ''), `version`, COALESCE(`updated_time`, '')) ORDER BY `scenario_id` SEPARATOR '|'))
       FROM `mfg_schedule_scenario` WHERE `scenario_id` IN (2, 3, 4)) = '12c7bd26d52f7be41179a92895ba77fa'
  AND (SELECT MD5(GROUP_CONCAT(CONCAT_WS('#', `schedule_item_id`, `scenario_id`, `production_order_id`,
       COALESCE(`line_id`, ''), COALESCE(`plan_id`, ''), `sequence_no`, `planned_quantity`,
       COALESCE(`planned_start_time`, ''), COALESCE(`planned_end_time`, ''), `estimated_minutes`,
       `priority_score`, `tardiness_minutes`, `decision_source`, `status`, COALESCE(`conflict_reason`, ''),
       COALESCE(`explanation`, ''), `version`, COALESCE(`updated_time`, ''))
       ORDER BY `schedule_item_id` SEPARATOR '|'))
       FROM `mfg_schedule_item` WHERE `scenario_id` IN (2, 3, 4)) = '7082086c59eb62aa20152da8ae435a49'
  AND (SELECT COUNT(DISTINCT `user_id`) FROM `sys_operation_log`
       WHERE `operation_time` >= '2026-07-01' AND `operation_time` < '2026-07-15'
         AND `user_id` IS NOT NULL AND `deleted_flag` = 0) >= 8
  AND (SELECT COUNT(*) FROM `sys_operation_log`
       WHERE `operation_type` = 'ROLLFORWARD_RENIX_14D' AND `module_code` = 'db_seed'
         AND `remark` = @live_marker AND `deleted_flag` = 0) = 1,
  1, 0
);

SELECT 'postcondition' AS `check_group`, metric.`check_name`, metric.`actual_value`
FROM (
  SELECT 'orders_finished' AS `check_name`, CAST((SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0) AS CHAR) AS `actual_value`
  UNION ALL SELECT 'orders_running', CAST((SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'orders_released', CAST((SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'RELEASED' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'orders_draft', CAST((SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'DRAFT' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'plans_finished', CAST((SELECT COUNT(*) FROM `mfg_production_plan` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'plans_running', CAST((SELECT COUNT(*) FROM `mfg_production_plan` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'plans_released', CAST((SELECT COUNT(*) FROM `mfg_production_plan` WHERE `status` = 'RELEASED' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'work_orders_finished', CAST((SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'work_orders_running', CAST((SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'work_orders_released', CAST((SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'RELEASED' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'tasks_finished', CAST((SELECT COUNT(*) FROM `mfg_task` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'tasks_running', CAST((SELECT COUNT(*) FROM `mfg_task` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'tasks_waiting', CAST((SELECT COUNT(*) FROM `mfg_task` WHERE `status` = 'WAIT_START' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'lots_stored', CAST((SELECT COUNT(*) FROM `trace_product_lot` WHERE `status` = 'STORED' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'lots_running', CAST((SELECT COUNT(*) FROM `trace_product_lot` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'window_spot_checks', CAST((SELECT COUNT(*) FROM `eam_spot_check_record` WHERE `check_time` >= '2026-07-01' AND `check_time` < '2026-07-15' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'replenishment_suggestions', CAST((SELECT COUNT(*) FROM `wms_replenishment_suggestion` WHERE `remark` LIKE CONCAT(@live_marker, ':%') AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'negative_inventory', CAST((SELECT COUNT(*) FROM `wms_inventory` WHERE `available_quantity` < 0 AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'current_issue_mismatch', CAST((SELECT COUNT(*) FROM `mfg_work_order_material` WHERE `work_order_id` = (SELECT `work_order_id` FROM `mfg_work_order` WHERE `work_order_no` = 'WO-20260714-003-1' AND `deleted_flag` = 0) AND (`status` <> 'ISSUED' OR `shortage_quantity` <> 0 OR `issued_quantity` <> `required_quantity`) AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'task_report_mismatch', CAST((SELECT COUNT(*) FROM (
    SELECT task.`task_id`, task.`completed_quantity`, COALESCE(SUM(work_report.`qualified_quantity` + work_report.`defect_quantity`), 0) AS `reported_quantity`
    FROM `mfg_task` task LEFT JOIN `mfg_work_report` work_report ON work_report.`task_id` = task.`task_id` AND work_report.`audit_status` = 'APPROVED' AND work_report.`deleted_flag` = 0
    WHERE task.`deleted_flag` = 0 GROUP BY task.`task_id`, task.`completed_quantity`
    HAVING ABS(task.`completed_quantity` - `reported_quantity`) > 0.0001
  ) invalid_report) AS CHAR)
  UNION ALL SELECT 'sunday_reports', CAST((SELECT COUNT(*) FROM `mfg_work_report` WHERE DATE(`report_time`) IN ('2026-07-05', '2026-07-12') AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'recent_audit_users', CAST((SELECT COUNT(DISTINCT `user_id`) FROM `sys_operation_log` WHERE `operation_time` >= '2026-07-01' AND `operation_time` < '2026-07-15' AND `user_id` IS NOT NULL AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'completion_markers', CAST((SELECT COUNT(*) FROM `sys_operation_log` WHERE `operation_type` = 'ROLLFORWARD_RENIX_14D' AND `module_code` = 'db_seed' AND `remark` = @live_marker AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'scenario_digest', (SELECT MD5(GROUP_CONCAT(CONCAT_WS('#', `scenario_id`, `scenario_no`, `status`, `order_count`, `scheduled_order_count`, `conflict_order_count`, `late_order_count`, `total_load_minutes`, COALESCE(`remark`, ''), `version`, COALESCE(`updated_time`, '')) ORDER BY `scenario_id` SEPARATOR '|')) FROM `mfg_schedule_scenario` WHERE `scenario_id` IN (2, 3, 4))
  UNION ALL SELECT 'schedule_item_digest', (SELECT MD5(GROUP_CONCAT(CONCAT_WS('#', `schedule_item_id`, `scenario_id`, `production_order_id`, COALESCE(`line_id`, ''), COALESCE(`plan_id`, ''), `sequence_no`, `planned_quantity`, COALESCE(`planned_start_time`, ''), COALESCE(`planned_end_time`, ''), `estimated_minutes`, `priority_score`, `tardiness_minutes`, `decision_source`, `status`, COALESCE(`conflict_reason`, ''), COALESCE(`explanation`, ''), `version`, COALESCE(`updated_time`, '')) ORDER BY `schedule_item_id` SEPARATOR '|')) FROM `mfg_schedule_item` WHERE `scenario_id` IN (2, 3, 4))
) metric;

SET @live_commit_assertion_sql = IF(
  @live_postcondition_ok = 1,
  'DO 0',
  'SELECT * FROM `__ABORT_RENIX_LIVE_14D_POSTCONDITION__`'
);
PREPARE renix_live_commit_assertion_stmt FROM @live_commit_assertion_sql;
EXECUTE renix_live_commit_assertion_stmt;
DEALLOCATE PREPARE renix_live_commit_assertion_stmt;

COMMIT;

SELECT @live_marker AS `dataset`, @seed_enabled AS `seed_executed`, @live_snapshot_time AS `snapshot_time`;
SELECT 'production_orders' AS `metric`, `status`, COUNT(*) AS `actual_value`
FROM `mfg_production_order` WHERE `deleted_flag` = 0 GROUP BY `status`
UNION ALL SELECT 'production_plans', `status`, COUNT(*) FROM `mfg_production_plan` WHERE `deleted_flag` = 0 GROUP BY `status`
UNION ALL SELECT 'work_orders', `status`, COUNT(*) FROM `mfg_work_order` WHERE `deleted_flag` = 0 GROUP BY `status`
UNION ALL SELECT 'tasks', `status`, COUNT(*) FROM `mfg_task` WHERE `deleted_flag` = 0 GROUP BY `status`
UNION ALL SELECT 'product_lots', `status`, COUNT(*) FROM `trace_product_lot` WHERE `deleted_flag` = 0 GROUP BY `status`;
