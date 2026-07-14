-- 验证 RENIX_LIVE_14D_V1 的状态、数量、库存、质量、设备、追溯、审计和排产保护。
-- 验收要求：每一行 result=PASS，且 failed_checks=0。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @live_marker = 'RENIX_LIVE_14D_V1';

SELECT checks.`check_group`, checks.`check_name`, checks.`expected_value`, checks.`actual_value`,
       IF(checks.`actual_value` = checks.`expected_value`, 'PASS', 'FAIL') AS `result`,
       SUM(IF(checks.`actual_value` = checks.`expected_value`, 0, 1)) OVER () AS `failed_checks`
FROM (
  SELECT 'marker' AS `check_group`, 'completion marker' AS `check_name`, '1' AS `expected_value`,
         CAST((SELECT COUNT(*) FROM `sys_operation_log`
               WHERE `operation_type` = 'ROLLFORWARD_RENIX_14D' AND `module_code` = 'db_seed'
                 AND `remark` = @live_marker AND `deleted_flag` = 0) AS CHAR) AS `actual_value`

  UNION ALL SELECT 'count', 'production orders', '33',
         CAST((SELECT COUNT(*) FROM `mfg_production_order` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'production plans', '25',
         CAST((SELECT COUNT(*) FROM `mfg_production_plan` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'work orders', '32',
         CAST((SELECT COUNT(*) FROM `mfg_work_order` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'tasks', '224',
         CAST((SELECT COUNT(*) FROM `mfg_task` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'approved reports', '210',
         CAST((SELECT COUNT(*) FROM `mfg_work_report` WHERE `audit_status` = 'APPROVED' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'report materials', '248',
         CAST((SELECT COUNT(*) FROM `mfg_report_material` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'material issue orders', '30',
         CAST((SELECT COUNT(*) FROM `wms_outbound_order` WHERE `outbound_type` = 'MATERIAL_ISSUE' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'material return orders', '29',
         CAST((SELECT COUNT(*) FROM `wms_return_order` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'finished goods inbound orders', '29',
         CAST((SELECT COUNT(*) FROM `wms_inbound_order` WHERE `inbound_type` = 'PRODUCTION' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'stock movements', '370',
         CAST((SELECT COUNT(*) FROM `wms_stock_movement` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'process inspections', '31',
         CAST((SELECT COUNT(*) FROM `qms_inspection_order` WHERE `inspection_type` = 'PROCESS' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'final inspections', '29',
         CAST((SELECT COUNT(*) FROM `qms_inspection_order` WHERE `inspection_type` = 'FINAL' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'inspection results', '196',
         CAST((SELECT COUNT(*) FROM `qms_inspection_result` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'product lots', '30',
         CAST((SELECT COUNT(*) FROM `trace_product_lot` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'barcodes', '801',
         CAST((SELECT COUNT(*) FROM `trace_barcode` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'count', 'material usage trace rows', '248',
         CAST((SELECT COUNT(*) FROM `trace_material_usage` WHERE `deleted_flag` = 0) AS CHAR)

  UNION ALL SELECT 'status', 'finished-running-released-draft orders', '22/1/2/8',
         CONCAT(
           (SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0), '/',
           (SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0), '/',
           (SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'RELEASED' AND `deleted_flag` = 0), '/',
           (SELECT COUNT(*) FROM `mfg_production_order` WHERE `status` = 'DRAFT' AND `deleted_flag` = 0))
  UNION ALL SELECT 'status', 'finished-running-released plans', '22/1/2',
         CONCAT(
           (SELECT COUNT(*) FROM `mfg_production_plan` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0), '/',
           (SELECT COUNT(*) FROM `mfg_production_plan` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0), '/',
           (SELECT COUNT(*) FROM `mfg_production_plan` WHERE `status` = 'RELEASED' AND `deleted_flag` = 0))
  UNION ALL SELECT 'status', 'finished-running-released work orders', '29/1/2',
         CONCAT(
           (SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0), '/',
           (SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0), '/',
           (SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'RELEASED' AND `deleted_flag` = 0))
  UNION ALL SELECT 'status', 'finished-running-waiting tasks', '207/1/16',
         CONCAT(
           (SELECT COUNT(*) FROM `mfg_task` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0), '/',
           (SELECT COUNT(*) FROM `mfg_task` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0), '/',
           (SELECT COUNT(*) FROM `mfg_task` WHERE `status` = 'WAIT_START' AND `deleted_flag` = 0))
  UNION ALL SELECT 'status', 'stored-running product lots', '29/1',
         CONCAT(
           (SELECT COUNT(*) FROM `trace_product_lot` WHERE `status` = 'STORED' AND `deleted_flag` = 0), '/',
           (SELECT COUNT(*) FROM `trace_product_lot` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0))

  -- 生产订单到报工的数量与关系守恒。
  UNION ALL SELECT 'production', 'order-plan quantity mismatch', '0',
         CAST((SELECT COUNT(*) FROM (
           SELECT production_order.`production_order_id`, production_order.`status`, production_order.`order_quantity`,
                  COUNT(plan.`plan_id`) AS `plan_count`, COALESCE(SUM(plan.`planned_quantity`), 0) AS `plan_quantity`
           FROM `mfg_production_order` production_order
           LEFT JOIN `mfg_production_plan` plan
             ON plan.`production_order_id` = production_order.`production_order_id` AND plan.`deleted_flag` = 0
           WHERE production_order.`deleted_flag` = 0
           GROUP BY production_order.`production_order_id`, production_order.`status`, production_order.`order_quantity`
           HAVING (production_order.`status` = 'DRAFT' AND `plan_count` <> 0)
              OR (production_order.`status` <> 'DRAFT' AND (`plan_count` <> 1 OR `plan_quantity` <> production_order.`order_quantity`))
         ) invalid_order) AS CHAR)
  UNION ALL SELECT 'production', 'plan-work-order quantity mismatch', '0',
         CAST((SELECT COUNT(*) FROM (
           SELECT plan.`plan_id`, plan.`planned_quantity`, COALESCE(SUM(work_order.`planned_quantity`), 0) AS `work_order_quantity`
           FROM `mfg_production_plan` plan
           LEFT JOIN `mfg_work_order` work_order
             ON work_order.`plan_id` = plan.`plan_id` AND work_order.`deleted_flag` = 0
           WHERE plan.`deleted_flag` = 0 GROUP BY plan.`plan_id`, plan.`planned_quantity`
           HAVING `work_order_quantity` <> plan.`planned_quantity`
         ) invalid_plan) AS CHAR)
  UNION ALL SELECT 'production', 'work-order task shape mismatch', '0',
         CAST((SELECT COUNT(*) FROM (
           SELECT work_order.`work_order_id`, COUNT(task.`task_id`) AS `task_count`,
                  COUNT(DISTINCT route_item.`sequence_no`) AS `sequence_count`
           FROM `mfg_work_order` work_order
           LEFT JOIN `mfg_task` task ON task.`work_order_id` = work_order.`work_order_id` AND task.`deleted_flag` = 0
           LEFT JOIN `process_route_item` route_item
             ON route_item.`route_id` = work_order.`route_id` AND route_item.`operation_id` = task.`operation_id`
            AND route_item.`deleted_flag` = 0
           WHERE work_order.`deleted_flag` = 0 GROUP BY work_order.`work_order_id`
           HAVING `task_count` <> 7 OR `sequence_count` <> 7
         ) invalid_work_order) AS CHAR)
  UNION ALL SELECT 'production', 'task-report quantity mismatch', '0',
         CAST((SELECT COUNT(*) FROM `mfg_task` task
               WHERE task.`deleted_flag` = 0 AND (
                 task.`completed_quantity` <> COALESCE((
                   SELECT SUM(work_report.`qualified_quantity` + work_report.`defect_quantity`)
                   FROM `mfg_work_report` work_report
                   WHERE work_report.`task_id` = task.`task_id` AND work_report.`audit_status` = 'APPROVED'
                     AND work_report.`deleted_flag` = 0), 0)
                 OR task.`defect_quantity` <> COALESCE((
                   SELECT SUM(work_report.`defect_quantity`) FROM `mfg_work_report` work_report
                   WHERE work_report.`task_id` = task.`task_id` AND work_report.`audit_status` = 'APPROVED'
                     AND work_report.`deleted_flag` = 0), 0))) AS CHAR)
  UNION ALL SELECT 'production', 'issued quantity-outbound mismatch', '0',
         CAST((SELECT COUNT(*) FROM `mfg_work_order_material` requirement
               JOIN `mfg_work_order` work_order
                 ON work_order.`work_order_id` = requirement.`work_order_id` AND work_order.`deleted_flag` = 0
               WHERE requirement.`deleted_flag` = 0
                 AND ABS(requirement.`issued_quantity` - COALESCE((
                   SELECT SUM(detail.`outbound_quantity`)
                   FROM `wms_outbound_order` header
                   JOIN `wms_outbound_detail` detail
                     ON detail.`outbound_id` = header.`outbound_id` AND detail.`material_id` = requirement.`material_id`
                    AND detail.`deleted_flag` = 0
                   WHERE header.`work_order_id` = requirement.`work_order_id`
                     AND header.`outbound_type` = 'MATERIAL_ISSUE' AND header.`deleted_flag` = 0), 0)) > 0.0001) AS CHAR)
  UNION ALL SELECT 'production', 'material shortages', '0',
         CAST((SELECT COUNT(*) FROM `mfg_work_order_material`
               WHERE (`shortage_quantity` <> 0 OR `status` = 'SHORTAGE') AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'production', 'current work-order task snapshot', '4/1/2/240',
         (SELECT CONCAT(SUM(task.`status` = 'FINISHED'), '/', SUM(task.`status` = 'RUNNING'), '/',
                        SUM(task.`status` = 'WAIT_START'), '/', CAST(work_order.`completed_quantity` AS UNSIGNED))
          FROM `mfg_work_order` work_order
          JOIN `mfg_task` task ON task.`work_order_id` = work_order.`work_order_id` AND task.`deleted_flag` = 0
          WHERE work_order.`work_order_no` = 'WO-20260714-003-1' AND work_order.`deleted_flag` = 0
          GROUP BY work_order.`completed_quantity`)
  UNION ALL SELECT 'production', 'reports on planned shutdown Sundays', '0',
         CAST((SELECT COUNT(*) FROM `mfg_work_report`
               WHERE DATE(`report_time`) IN ('2026-07-05', '2026-07-12') AND `deleted_flag` = 0) AS CHAR)

  -- 库存流水、FIFO、质量和当前在制边界。
  UNION ALL SELECT 'inventory', 'negative running batch balances', '0',
         CAST((SELECT COUNT(*) FROM (
           SELECT SUM(IF(`direction` = 'IN', `quantity`, -`quantity`)) OVER (
             PARTITION BY `batch_id` ORDER BY `operation_time`, `movement_id`
           ) AS `running_balance`
           FROM `wms_stock_movement` WHERE `deleted_flag` = 0
         ) running_record WHERE running_record.`running_balance` < 0) AS CHAR)
  UNION ALL SELECT 'inventory', 'negative inventory balances', '0',
         CAST((SELECT COUNT(*) FROM `wms_inventory`
               WHERE (`available_quantity` < 0 OR `locked_quantity` < 0 OR `inspection_quantity` < 0)
                 AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'inventory', 'movement-balance mismatch', '0',
         CAST((SELECT COUNT(*) FROM (
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
         ) invalid_balance) AS CHAR)
  UNION ALL SELECT 'inventory', 'document details without movement', '0',
         CAST(((SELECT COUNT(*) FROM `wms_inbound_detail` detail
                LEFT JOIN `wms_stock_movement` movement
                  ON movement.`source_order_type` = 'INBOUND_ORDER' AND movement.`source_order_id` = detail.`inbound_id`
                 AND movement.`material_id` = detail.`material_id` AND movement.`batch_id` = detail.`batch_id`
                 AND movement.`quantity` = detail.`inbound_quantity` AND movement.`direction` = 'IN'
                 AND movement.`deleted_flag` = 0
                WHERE detail.`deleted_flag` = 0 AND movement.`movement_id` IS NULL)
              + (SELECT COUNT(*) FROM `wms_outbound_detail` detail
                 LEFT JOIN `wms_stock_movement` movement
                   ON movement.`source_order_type` = 'OUTBOUND_ORDER' AND movement.`source_order_id` = detail.`outbound_id`
                  AND movement.`material_id` = detail.`material_id` AND movement.`batch_id` = detail.`batch_id`
                  AND movement.`quantity` = detail.`outbound_quantity` AND movement.`direction` = 'OUT'
                  AND movement.`deleted_flag` = 0
                 WHERE detail.`deleted_flag` = 0 AND movement.`movement_id` IS NULL)
              + (SELECT COUNT(*) FROM `wms_return_detail` detail
                 LEFT JOIN `wms_stock_movement` movement
                   ON movement.`source_order_type` = 'RETURN_ORDER' AND movement.`source_order_id` = detail.`return_id`
                  AND movement.`material_id` = detail.`material_id` AND movement.`batch_id` = detail.`batch_id`
                  AND movement.`quantity` = detail.`return_quantity` AND movement.`direction` = 'IN'
                  AND movement.`deleted_flag` = 0
                 WHERE detail.`deleted_flag` = 0 AND movement.`movement_id` IS NULL)) AS CHAR)
  UNION ALL SELECT 'inventory', 'live replenishment suggestions', '2',
         CAST((SELECT COUNT(*) FROM `wms_replenishment_suggestion`
               WHERE `source_type` = 'INVENTORY_WARNING' AND `status` = 'SUBMITTED'
                 AND `suggested_quantity` > 0 AND `remark` LIKE CONCAT(@live_marker, ':%')
                 AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'quality', 'inspection headers without results', '0',
         CAST((SELECT COUNT(*) FROM (
           SELECT inspection_order.`inspection_id`
           FROM `qms_inspection_order` inspection_order
           LEFT JOIN `qms_inspection_result` result_record
             ON result_record.`inspection_id` = inspection_order.`inspection_id` AND result_record.`deleted_flag` = 0
           WHERE inspection_order.`deleted_flag` = 0 GROUP BY inspection_order.`inspection_id`
           HAVING COUNT(result_record.`result_id`) = 0
         ) inspection_without_result) AS CHAR)
  UNION ALL SELECT 'quality', 'finished lot FQC-inbound mismatch', '0',
         CAST((SELECT COUNT(*) FROM `trace_product_lot` product_lot
               LEFT JOIN `wms_material_batch` batch_record
                 ON batch_record.`source_product_lot_id` = product_lot.`product_lot_id` AND batch_record.`deleted_flag` = 0
               LEFT JOIN `wms_inbound_detail` inbound_detail
                 ON inbound_detail.`batch_id` = batch_record.`batch_id` AND inbound_detail.`deleted_flag` = 0
               LEFT JOIN `qms_inspection_order` inspection_order
                 ON inspection_order.`batch_id` = batch_record.`batch_id` AND inspection_order.`inspection_type` = 'FINAL'
                AND inspection_order.`inspection_result` = 'PASS' AND inspection_order.`deleted_flag` = 0
               WHERE product_lot.`status` = 'STORED' AND product_lot.`deleted_flag` = 0
                 AND (batch_record.`batch_id` IS NULL OR inbound_detail.`detail_id` IS NULL
                   OR inspection_order.`inspection_id` IS NULL
                   OR inbound_detail.`inbound_quantity` <> product_lot.`qualified_quantity`)) AS CHAR)
  UNION ALL SELECT 'quality', 'running lot premature FQC or inbound', '0',
         CAST((SELECT COUNT(*) FROM `trace_product_lot` product_lot
               LEFT JOIN `wms_material_batch` batch_record
                 ON batch_record.`source_product_lot_id` = product_lot.`product_lot_id` AND batch_record.`deleted_flag` = 0
               LEFT JOIN `qms_inspection_order` inspection_order
                 ON inspection_order.`work_order_id` = product_lot.`work_order_id`
                AND inspection_order.`inspection_type` = 'FINAL' AND inspection_order.`deleted_flag` = 0
               WHERE product_lot.`status` = 'RUNNING' AND product_lot.`deleted_flag` = 0
                 AND (batch_record.`batch_id` IS NOT NULL OR inspection_order.`inspection_id` IS NOT NULL)) AS CHAR)
  UNION ALL SELECT 'quality', 'product lot quantity mismatch', '0',
         CAST((SELECT COUNT(*) FROM `trace_product_lot`
               WHERE `batch_quantity` <> `qualified_quantity` + `defect_quantity` AND `deleted_flag` = 0) AS CHAR)

  -- 设备、条码追溯、审计与队友排产保护。
  UNION ALL SELECT 'equipment', '14-day opening spot checks', '144',
         CAST((SELECT COUNT(*) FROM `eam_spot_check_record`
               WHERE `check_time` >= '2026-07-01' AND `check_time` < '2026-07-15'
                 AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'equipment', 'shutdown-day spot checks', '0',
         CAST((SELECT COUNT(*) FROM `eam_spot_check_record`
               WHERE DATE(`check_time`) IN ('2026-07-05', '2026-07-12') AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'equipment', 'current equipment state', '10/2/0',
         (SELECT CONCAT(SUM(`status` = 'RUNNING'), '/', SUM(`status` = 'IDLE'), '/',
                        SUM(`status` NOT IN ('RUNNING', 'IDLE')))
          FROM `eam_equipment` WHERE `deleted_flag` = 0)
  UNION ALL SELECT 'trace', 'report-material trace mismatch', '0',
         CAST(((SELECT COUNT(*) FROM `trace_material_usage` usage_record
                LEFT JOIN `mfg_report_material` report_material
                  ON report_material.`report_id` = usage_record.`report_id`
                 AND report_material.`material_id` = usage_record.`material_id`
                 AND report_material.`batch_id` = usage_record.`batch_id`
                 AND report_material.`used_quantity` = usage_record.`used_quantity`
                 AND report_material.`deleted_flag` = 0
                WHERE usage_record.`deleted_flag` = 0 AND report_material.`id` IS NULL)
              + (SELECT COUNT(*) FROM `mfg_report_material` report_material
                 LEFT JOIN `trace_material_usage` usage_record
                   ON usage_record.`report_id` = report_material.`report_id`
                  AND usage_record.`material_id` = report_material.`material_id`
                  AND usage_record.`batch_id` = report_material.`batch_id`
                  AND usage_record.`used_quantity` = report_material.`used_quantity`
                  AND usage_record.`deleted_flag` = 0
                 WHERE report_material.`deleted_flag` = 0 AND usage_record.`usage_id` IS NULL)) AS CHAR)
  UNION ALL SELECT 'trace', 'lot barcode cardinality mismatch', '0',
         CAST((SELECT COUNT(*) FROM (
           SELECT product_lot.`product_lot_id`, COUNT(barcode.`barcode_id`) AS `barcode_count`
           FROM `trace_product_lot` product_lot
           LEFT JOIN `trace_barcode` barcode
             ON barcode.`biz_type` = 'TRACE_PRODUCT_LOT' AND barcode.`biz_id` = product_lot.`product_lot_id`
            AND barcode.`barcode_type` = 'PRODUCT_LOT' AND barcode.`deleted_flag` = 0
           WHERE product_lot.`deleted_flag` = 0 GROUP BY product_lot.`product_lot_id`
           HAVING `barcode_count` <> 1
         ) invalid_lot) AS CHAR)
  UNION ALL SELECT 'trace', 'outer carton barcode quantity mismatch', '0',
         CAST((SELECT ABS(
           (SELECT COUNT(*) FROM `trace_barcode` WHERE `barcode_type` = 'OUTER_CARTON' AND `deleted_flag` = 0)
           - (SELECT COALESCE(SUM(CEILING(`qualified_quantity` / 25)), 0)
              FROM `trace_product_lot` WHERE `status` = 'STORED' AND `deleted_flag` = 0)
         )) AS CHAR)
  UNION ALL SELECT 'audit', 'recent role coverage', '8',
         CAST((SELECT COUNT(DISTINCT role.`role_code`)
               FROM `sys_operation_log` operation_log
               JOIN `sys_user_role` user_role
                 ON user_role.`user_id` = operation_log.`user_id` AND user_role.`deleted_flag` = 0
               JOIN `sys_role` role ON role.`role_id` = user_role.`role_id` AND role.`deleted_flag` = 0
               WHERE operation_log.`operation_time` >= '2026-07-01'
                 AND operation_log.`operation_time` < '2026-07-15' AND operation_log.`deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'audit', 'invalid operation-log users', '0',
         CAST((SELECT COUNT(*) FROM `sys_operation_log` operation_log
               LEFT JOIN `sys_user` user_record
                 ON user_record.`user_id` = operation_log.`user_id` AND user_record.`deleted_flag` = 0
               WHERE operation_log.`user_id` IS NOT NULL AND operation_log.`deleted_flag` = 0
                 AND user_record.`user_id` IS NULL) AS CHAR)
  UNION ALL SELECT 'audit', 'invalid status-log users', '0',
         CAST((SELECT COUNT(*) FROM `sys_status_change_log` status_log
               LEFT JOIN `sys_user` user_record
                 ON user_record.`user_id` = status_log.`changed_by` AND user_record.`deleted_flag` = 0
               WHERE status_log.`changed_by` IS NOT NULL AND status_log.`deleted_flag` = 0
                 AND user_record.`user_id` IS NULL) AS CHAR)
  UNION ALL SELECT 'audit', 'invalid approval users', '0',
         CAST((SELECT COUNT(*) FROM `sys_approval_record` approval_record
               LEFT JOIN `sys_user` user_record
                 ON user_record.`user_id` = approval_record.`approver_id` AND user_record.`deleted_flag` = 0
               WHERE approval_record.`approver_id` IS NOT NULL AND approval_record.`deleted_flag` = 0
                 AND user_record.`user_id` IS NULL) AS CHAR)
  UNION ALL SELECT 'protection', 'scheduling E2E draft orders', '3',
         CAST((SELECT COUNT(*) FROM `mfg_production_order`
               WHERE `remark` LIKE 'SCHEDULING_E2E_20260713%' AND `status` = 'DRAFT' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'protection', 'scheduling scenarios 2-4', '3',
         CAST((SELECT COUNT(*) FROM `mfg_schedule_scenario`
               WHERE `scenario_id` IN (2, 3, 4) AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'protection', 'scheduling items 2-4', '23',
         CAST((SELECT COUNT(*) FROM `mfg_schedule_item`
               WHERE `scenario_id` IN (2, 3, 4) AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'protection', 'scheduling scenario digest', '12c7bd26d52f7be41179a92895ba77fa',
         (SELECT MD5(GROUP_CONCAT(CONCAT_WS('#', `scenario_id`, `scenario_no`, `status`, `order_count`,
                  `scheduled_order_count`, `conflict_order_count`, `late_order_count`, `total_load_minutes`,
                  COALESCE(`remark`, ''), `version`, COALESCE(`updated_time`, ''))
                  ORDER BY `scenario_id` SEPARATOR '|'))
          FROM `mfg_schedule_scenario` WHERE `scenario_id` IN (2, 3, 4))
  UNION ALL SELECT 'protection', 'scheduling item digest', '7082086c59eb62aa20152da8ae435a49',
         (SELECT MD5(GROUP_CONCAT(CONCAT_WS('#', `schedule_item_id`, `scenario_id`, `production_order_id`,
                  COALESCE(`line_id`, ''), COALESCE(`plan_id`, ''), `sequence_no`, `planned_quantity`,
                  COALESCE(`planned_start_time`, ''), COALESCE(`planned_end_time`, ''), `estimated_minutes`,
                  `priority_score`, `tardiness_minutes`, `decision_source`, `status`,
                  COALESCE(`conflict_reason`, ''), COALESCE(`explanation`, ''), `version`, COALESCE(`updated_time`, ''))
                  ORDER BY `schedule_item_id` SEPARATOR '|'))
          FROM `mfg_schedule_item` WHERE `scenario_id` IN (2, 3, 4))
) checks
ORDER BY FIELD(checks.`check_group`, 'marker', 'count', 'status', 'production', 'inventory', 'quality',
               'equipment', 'trace', 'audit', 'protection'), checks.`check_name`;
