-- Reverse and soft-delete two E2E material-issue outbound orders.
-- Original stock movements remain immutable; compensating IN movements restore stock.
-- Dry-run by default. Set @repair_outbound_commit = 1 before sourcing to commit.

SET @repair_outbound_commit = COALESCE(@repair_outbound_commit, 0);
SET @repair_outbound_time = COALESCE(@repair_outbound_time, CURRENT_TIMESTAMP);
SET @repair_outbound_marker = 'SOFT_DELETE_OUTBOUND_41_42_V1';
SET @repair_outbound_user_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS `tmp_outbound_42_reversal`;
CREATE TEMPORARY TABLE `tmp_outbound_42_reversal` AS
SELECT detail.`detail_id`, detail.`material_id`, detail.`batch_id`, detail.`location_id`,
       detail.`outbound_quantity`, movement.`movement_id`, inventory.`inventory_id`,
       inventory.`available_quantity` AS `available_before`, inventory.`version` AS `inventory_version`
FROM `wms_outbound_detail` detail
JOIN `wms_stock_movement` movement
  ON movement.`source_order_type` = 'OUTBOUND_ORDER'
 AND movement.`source_order_id` = detail.`outbound_id`
 AND movement.`material_id` = detail.`material_id`
 AND movement.`batch_id` = detail.`batch_id`
 AND movement.`quantity` = detail.`outbound_quantity`
 AND movement.`direction` = 'OUT'
 AND movement.`deleted_flag` = 0
JOIN `wms_outbound_order` outbound_order
  ON outbound_order.`outbound_id` = detail.`outbound_id`
JOIN `wms_inventory` inventory
  ON inventory.`warehouse_id` = outbound_order.`warehouse_id`
 AND inventory.`location_id` = detail.`location_id`
 AND inventory.`material_id` = detail.`material_id`
 AND inventory.`batch_id` = detail.`batch_id`
 AND inventory.`deleted_flag` = 0
WHERE detail.`outbound_id` = 42
  AND detail.`deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_outbound_42_requirement`;
CREATE TEMPORARY TABLE `tmp_outbound_42_requirement` AS
SELECT requirement.`requirement_id`, requirement.`material_id`,
       requirement.`required_quantity`, requirement.`issued_quantity`,
       SUM(detail.`outbound_quantity`) AS `reversal_quantity`
FROM `mfg_work_order_material` requirement
JOIN `wms_outbound_detail` detail
  ON detail.`material_id` = requirement.`material_id`
 AND detail.`outbound_id` = 42
 AND detail.`deleted_flag` = 0
WHERE requirement.`work_order_id` = 13
  AND requirement.`deleted_flag` = 0
GROUP BY requirement.`requirement_id`, requirement.`material_id`,
         requirement.`required_quantity`, requirement.`issued_quantity`;

SET @repair_outbound_reversal_row_count =
  (SELECT COUNT(*) FROM `tmp_outbound_42_reversal`);
SET @repair_outbound_requirement_row_count =
  (SELECT COUNT(*) FROM `tmp_outbound_42_requirement`);
SET @repair_outbound_invalid_requirement_count =
  (SELECT COUNT(*) FROM `tmp_outbound_42_requirement`
   WHERE `issued_quantity` < `reversal_quantity`
      OR `required_quantity` < `reversal_quantity`);

START TRANSACTION;

SELECT `outbound_id`, `outbound_no`, `audit_status`, `deleted_flag`, `version`
FROM `wms_outbound_order`
WHERE `outbound_id` IN (41, 42)
FOR UPDATE;

SELECT inventory.`inventory_id`, inventory.`available_quantity`, inventory.`version`
FROM `wms_inventory` inventory
JOIN `tmp_outbound_42_reversal` target ON target.`inventory_id` = inventory.`inventory_id`
FOR UPDATE;

SELECT requirement.`requirement_id`, requirement.`required_quantity`,
       requirement.`issued_quantity`, requirement.`version`
FROM `mfg_work_order_material` requirement
JOIN `tmp_outbound_42_requirement` target ON target.`requirement_id` = requirement.`requirement_id`
FOR UPDATE;

SET @repair_outbound_initial_state = (
  SELECT IF(
    (SELECT COUNT(*) FROM `wms_outbound_order`
     WHERE `outbound_id` = 41
       AND `outbound_no` = 'MI-WO12-20260716161027515'
       AND `audit_status` = 'CLOSED' AND `deleted_flag` = 0) = 1
    AND
    (SELECT COUNT(*) FROM `wms_outbound_order`
     WHERE `outbound_id` = 42
       AND `outbound_no` = 'MI-WO13-20260716190829191'
       AND `audit_status` = 'APPROVED' AND `deleted_flag` = 0) = 1,
    1, 0
  )
);

SET @repair_outbound_final_state = (
  SELECT IF(
    (SELECT COUNT(*) FROM `wms_outbound_order`
     WHERE `outbound_id` IN (41, 42)
       AND `audit_status` = 'CLOSED' AND `deleted_flag` = 1) = 2,
    1, 0
  )
);

SET @repair_outbound_precondition = IF(
  @repair_outbound_initial_state = 1
  AND @repair_outbound_reversal_row_count = 7
  AND @repair_outbound_requirement_row_count = 7
  AND (SELECT COUNT(*) FROM `wms_outbound_detail`
       WHERE `outbound_id` IN (41, 42) AND `deleted_flag` = 0) = 18
  AND @repair_outbound_invalid_requirement_count = 0,
  1,
  @repair_outbound_final_state
);

SET @repair_outbound_needs_reversal = @repair_outbound_initial_state;

UPDATE `wms_inventory` inventory
JOIN `tmp_outbound_42_reversal` target ON target.`inventory_id` = inventory.`inventory_id`
SET inventory.`available_quantity` = inventory.`available_quantity` + target.`outbound_quantity`,
    inventory.`version` = inventory.`version` + 1,
    inventory.`updated_by` = @repair_outbound_user_id,
    inventory.`updated_time` = @repair_outbound_time
WHERE @repair_outbound_needs_reversal = 1;
SET @restored_inventory_count = ROW_COUNT();

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`,
   `material_id`, `batch_id`, `quantity`, `direction`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('WMS-REV-OUT-42-', target.`detail_id`), 'OUTBOUND_REVERSAL',
       'OUTBOUND_ORDER_REVERSAL', 42, target.`material_id`, target.`batch_id`,
       target.`outbound_quantity`, 'IN', @repair_outbound_time, @repair_outbound_marker,
       0, @repair_outbound_user_id, @repair_outbound_time,
       @repair_outbound_user_id, @repair_outbound_time, 0
FROM `tmp_outbound_42_reversal` target
WHERE @repair_outbound_needs_reversal = 1
  AND NOT EXISTS (
    SELECT 1 FROM `wms_stock_movement` movement
    WHERE movement.`movement_no` = CONCAT('WMS-REV-OUT-42-', target.`detail_id`)
  );
SET @inserted_reversal_movement_count = ROW_COUNT();

UPDATE `mfg_work_order_material` requirement
JOIN `tmp_outbound_42_requirement` target ON target.`requirement_id` = requirement.`requirement_id`
SET requirement.`required_quantity` = target.`required_quantity` - target.`reversal_quantity`,
    requirement.`issued_quantity` = target.`issued_quantity` - target.`reversal_quantity`,
    requirement.`shortage_quantity` = GREATEST(
      (target.`required_quantity` - target.`reversal_quantity`)
        - (target.`issued_quantity` - target.`reversal_quantity`), 0),
    requirement.`status` = CASE
      WHEN target.`issued_quantity` - target.`reversal_quantity`
        >= target.`required_quantity` - target.`reversal_quantity` THEN 'READY'
      WHEN target.`issued_quantity` - target.`reversal_quantity` > 0 THEN 'PARTIAL_READY'
      ELSE 'WAIT_READY'
    END,
    requirement.`version` = requirement.`version` + 1,
    requirement.`updated_by` = @repair_outbound_user_id,
    requirement.`updated_time` = @repair_outbound_time
WHERE @repair_outbound_needs_reversal = 1;
SET @restored_requirement_count = ROW_COUNT();

UPDATE `wms_outbound_detail`
SET `deleted_flag` = 1,
    `version` = `version` + 1,
    `updated_by` = @repair_outbound_user_id,
    `updated_time` = @repair_outbound_time
WHERE `outbound_id` IN (41, 42)
  AND `deleted_flag` = 0
  AND @repair_outbound_initial_state = 1;
SET @soft_deleted_outbound_detail_count = ROW_COUNT();

UPDATE `wms_outbound_order`
SET `audit_status` = 'CLOSED',
    `deleted_flag` = 1,
    `remark` = CONCAT(COALESCE(NULLIF(`remark`, ''), ''),
      CASE WHEN `remark` IS NULL OR `remark` = '' THEN '' ELSE '; ' END,
      @repair_outbound_marker),
    `version` = `version` + 1,
    `updated_by` = @repair_outbound_user_id,
    `updated_time` = @repair_outbound_time
WHERE `outbound_id` IN (41, 42)
  AND `deleted_flag` = 0
  AND @repair_outbound_initial_state = 1;
SET @soft_deleted_outbound_order_count = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'WMS_OUTBOUND_ORDER', 42, 'APPROVED', 'CLOSED', 'REVERSE_AND_SOFT_DELETE_OUTBOUND',
       @repair_outbound_user_id, @repair_outbound_time, @repair_outbound_marker,
       0, @repair_outbound_user_id, @repair_outbound_time,
       @repair_outbound_user_id, @repair_outbound_time, 0
WHERE @repair_outbound_initial_state = 1
  AND NOT EXISTS (
    SELECT 1 FROM `sys_status_change_log`
    WHERE `biz_type` = 'WMS_OUTBOUND_ORDER' AND `biz_id` = 42
      AND `change_reason` = 'REVERSE_AND_SOFT_DELETE_OUTBOUND'
      AND `remark` = CONVERT(@repair_outbound_marker USING utf8mb4) COLLATE utf8mb4_general_ci
      AND `deleted_flag` = 0
  );
SET @inserted_outbound_status_log_count = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @repair_outbound_user_id, 'REVERSE_AND_SOFT_DELETE_OUTBOUND', 'wms_outbound_order', target.`outbound_id`,
       @repair_outbound_time, @repair_outbound_marker,
       0, @repair_outbound_user_id, @repair_outbound_time,
       @repair_outbound_user_id, @repair_outbound_time, 0
FROM (SELECT 41 AS `outbound_id` UNION ALL SELECT 42) target
WHERE @repair_outbound_initial_state = 1
  AND NOT EXISTS (
    SELECT 1 FROM `sys_operation_log`
    WHERE `module_code` = 'wms_outbound_order' AND `biz_id` = target.`outbound_id`
      AND `operation_type` = 'REVERSE_AND_SOFT_DELETE_OUTBOUND'
      AND `remark` = CONVERT(@repair_outbound_marker USING utf8mb4) COLLATE utf8mb4_general_ci
      AND `deleted_flag` = 0
  );
SET @inserted_outbound_operation_log_count = ROW_COUNT();

UPDATE `sys_notification`
SET `status` = 'CANCELLED', `version` = `version` + 1,
    `updated_by` = @repair_outbound_user_id, `updated_time` = @repair_outbound_time
WHERE `biz_type` = 'WMS_OUTBOUND_ORDER' AND `biz_id` IN (41, 42)
  AND `status` = 'ACTIVE' AND `deleted_flag` = 0;
SET @cancelled_outbound_notification_count = ROW_COUNT();

UPDATE `sys_event_outbox`
SET `status` = 'CANCELLED', `processed_time` = @repair_outbound_time,
    `version` = `version` + 1,
    `updated_by` = @repair_outbound_user_id, `updated_time` = @repair_outbound_time
WHERE `aggregate_type` = 'WMS_OUTBOUND_ORDER' AND `aggregate_id` IN (41, 42)
  AND `status` IN ('PENDING', 'FAILED', 'PROCESSING') AND `deleted_flag` = 0;
SET @cancelled_outbound_outbox_count = ROW_COUNT();

SET @repair_outbound_result_valid = (
  SELECT IF(
    (SELECT COUNT(*) FROM `wms_outbound_order`
     WHERE `outbound_id` IN (41, 42) AND `audit_status` = 'CLOSED' AND `deleted_flag` = 1) = 2
    AND
    (SELECT COUNT(*) FROM `wms_outbound_detail`
     WHERE `outbound_id` IN (41, 42) AND `deleted_flag` = 0) = 0
    AND
    (SELECT COUNT(*) FROM `wms_stock_movement`
     WHERE `source_order_type` = 'OUTBOUND_ORDER_REVERSAL'
       AND `source_order_id` = 42 AND `direction` = 'IN' AND `deleted_flag` = 0) = 7
    AND
    (SELECT COUNT(*) FROM `mfg_work_order_material`
     WHERE `work_order_id` = 13 AND `deleted_flag` = 0
       AND (`issued_quantity` <> `required_quantity` OR `status` <> 'READY')) = 0
    AND
    (SELECT COUNT(*) FROM `sys_event_outbox`
     WHERE `aggregate_type` = 'WMS_OUTBOUND_ORDER' AND `aggregate_id` IN (41, 42)
       AND `status` IN ('PENDING', 'FAILED', 'PROCESSING') AND `deleted_flag` = 0) = 0,
    1, 0
  )
);

SET @repair_outbound_final_statement = IF(
  @repair_outbound_commit = 1
  AND @repair_outbound_precondition = 1
  AND @repair_outbound_result_valid = 1,
  'COMMIT', 'ROLLBACK'
);
PREPARE repair_outbound_final_statement FROM @repair_outbound_final_statement;
EXECUTE repair_outbound_final_statement;
DEALLOCATE PREPARE repair_outbound_final_statement;

SELECT IF(@repair_outbound_commit = 1
          AND @repair_outbound_precondition = 1
          AND @repair_outbound_result_valid = 1,
          'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @repair_outbound_precondition AS `precondition_valid`,
       @restored_inventory_count AS `restored_inventory_rows`,
       @inserted_reversal_movement_count AS `reversal_movements`,
       @restored_requirement_count AS `restored_requirement_rows`,
       @soft_deleted_outbound_order_count AS `soft_deleted_orders`,
       @soft_deleted_outbound_detail_count AS `soft_deleted_details`,
       @inserted_outbound_status_log_count AS `status_logs`,
       @inserted_outbound_operation_log_count AS `operation_logs`,
       @cancelled_outbound_notification_count AS `cancelled_notifications`,
       @cancelled_outbound_outbox_count AS `cancelled_outbox`,
       @repair_outbound_result_valid AS `result_valid`;
