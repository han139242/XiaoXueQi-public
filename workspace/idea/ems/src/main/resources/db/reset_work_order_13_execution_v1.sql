-- Reset WO-20260715-014-1 to a clean, already-started seven-task state.
-- Also repair task 81 so a task cannot remain started before its parent work order.
-- Dry-run by default. Set @reset_wo13_commit = 1 before sourcing to commit.

SET @reset_wo13_commit = COALESCE(@reset_wo13_commit, 0);
SET @reset_wo13_time = COALESCE(@reset_wo13_time, CURRENT_TIMESTAMP);
SET @reset_wo13_marker = 'RESET_WORK_ORDER_13_EXECUTION_V1';
SET @reset_wo13_user_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS `tmp_reset_wo13_task`;
CREATE TEMPORARY TABLE `tmp_reset_wo13_task` (
  `task_id` BIGINT UNSIGNED NOT NULL,
  `canonical` TINYINT NOT NULL,
  PRIMARY KEY (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_reset_wo13_task` (`task_id`, `canonical`)
SELECT `task_id`, IF(`task_id` IN (85, 86, 87, 88, 89, 90, 91), 1, 0)
FROM `mfg_task`
WHERE `work_order_id` = 13 AND `task_id` IN (85, 86, 87, 88, 89, 90, 91, 318, 319, 320);

DROP TEMPORARY TABLE IF EXISTS `tmp_reset_wo13_report`;
CREATE TEMPORARY TABLE `tmp_reset_wo13_report` (
  `report_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_reset_wo13_report` (`report_id`)
SELECT report.`report_id`
FROM `mfg_work_report` report
JOIN `tmp_reset_wo13_task` task ON task.`task_id` = report.`task_id`;

DROP TEMPORARY TABLE IF EXISTS `tmp_reset_wo13_inspection`;
CREATE TEMPORARY TABLE `tmp_reset_wo13_inspection` (
  `inspection_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`inspection_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_reset_wo13_inspection` (`inspection_id`)
SELECT DISTINCT inspection.`inspection_id`
FROM `qms_inspection_order` inspection
LEFT JOIN `tmp_reset_wo13_task` task ON task.`task_id` = inspection.`task_id`
LEFT JOIN `tmp_reset_wo13_report` report ON report.`report_id` = inspection.`report_id`
WHERE inspection.`work_order_id` = 13 OR task.`task_id` IS NOT NULL OR report.`report_id` IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_reset_wo13_ncr`;
CREATE TEMPORARY TABLE `tmp_reset_wo13_ncr` (
  `ncr_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`ncr_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_reset_wo13_ncr` (`ncr_id`)
SELECT ncr.`ncr_id`
FROM `qms_nonconforming_order` ncr
JOIN `tmp_reset_wo13_inspection` inspection ON inspection.`inspection_id` = ncr.`inspection_id`;

DROP TEMPORARY TABLE IF EXISTS `tmp_reset_wo13_lot`;
CREATE TEMPORARY TABLE `tmp_reset_wo13_lot` (
  `product_lot_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`product_lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_reset_wo13_lot` (`product_lot_id`)
SELECT `product_lot_id` FROM `trace_product_lot` WHERE `work_order_id` = 13;

DROP TEMPORARY TABLE IF EXISTS `tmp_reset_wo13_batch`;
CREATE TEMPORARY TABLE `tmp_reset_wo13_batch` (
  `batch_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_reset_wo13_batch` (`batch_id`)
SELECT batch.`batch_id`
FROM `wms_material_batch` batch
JOIN `tmp_reset_wo13_lot` lot ON lot.`product_lot_id` = batch.`source_product_lot_id`;

DROP TEMPORARY TABLE IF EXISTS `tmp_reset_wo13_inbound`;
CREATE TEMPORARY TABLE `tmp_reset_wo13_inbound` (
  `inbound_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`inbound_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_reset_wo13_inbound` (`inbound_id`)
SELECT DISTINCT inbound.`inbound_id`
FROM `wms_inbound_order` inbound
LEFT JOIN `tmp_reset_wo13_lot` lot
  ON inbound.`source_order_type` = 'PRODUCT_LOT' AND lot.`product_lot_id` = inbound.`source_order_id`
LEFT JOIN `wms_inbound_detail` detail ON detail.`inbound_id` = inbound.`inbound_id`
LEFT JOIN `tmp_reset_wo13_batch` batch ON batch.`batch_id` = detail.`batch_id`
WHERE lot.`product_lot_id` IS NOT NULL OR batch.`batch_id` IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS `tmp_reset_wo13_assert`;
CREATE TEMPORARY TABLE `tmp_reset_wo13_assert` (
  `assertion_name` VARCHAR(128) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

START TRANSACTION;

SELECT `work_order_id`, `work_order_no`, `status`, `planned_quantity`, `completed_quantity`, `version`
FROM `mfg_work_order` WHERE `work_order_id` IN (12, 13) FOR UPDATE;

SELECT task.`task_id`, task.`task_no`, task.`status`, task.`completed_quantity`,
       task.`defect_quantity`, task.`deleted_flag`, task.`version`
FROM `mfg_task` task
JOIN `tmp_reset_wo13_task` target ON target.`task_id` = task.`task_id`
UNION ALL
SELECT task.`task_id`, task.`task_no`, task.`status`, task.`completed_quantity`,
       task.`defect_quantity`, task.`deleted_flag`, task.`version`
FROM `mfg_task` task WHERE task.`task_id` = 81
FOR UPDATE;

SET @reset_wo13_canonical_target_count =
  (SELECT COUNT(*) FROM `tmp_reset_wo13_task` WHERE `canonical` = 1);
SET @reset_wo13_duplicate_target_count =
  (SELECT COUNT(*) FROM `tmp_reset_wo13_task` WHERE `canonical` = 0);
SET @reset_wo13_active_canonical_count = (
  SELECT COUNT(*) FROM `mfg_task` task
  JOIN `tmp_reset_wo13_task` target ON target.`task_id` = task.`task_id`
  WHERE target.`canonical` = 1 AND task.`status` = 'RUNNING'
    AND task.`planned_quantity` = 800.0000 AND task.`deleted_flag` = 0
);
SET @reset_wo13_active_duplicate_count = (
  SELECT COUNT(*) FROM `mfg_task` task
  JOIN `tmp_reset_wo13_task` target ON target.`task_id` = task.`task_id`
  WHERE target.`canonical` = 0 AND task.`deleted_flag` = 0
);
SET @reset_wo13_active_report_count = (
  SELECT COUNT(*) FROM `mfg_work_report` report
  JOIN `tmp_reset_wo13_report` target ON target.`report_id` = report.`report_id`
  WHERE report.`deleted_flag` = 0
);

SET @reset_wo13_precondition = IF(
  @reset_wo13_user_id IS NOT NULL
  AND (SELECT COUNT(*) FROM `mfg_work_order`
       WHERE `work_order_id` = 13 AND `work_order_no` = 'WO-20260715-014-1'
         AND `status` = 'RUNNING' AND `planned_quantity` = 800.0000 AND `deleted_flag` = 0) = 1
  AND @reset_wo13_canonical_target_count = 7
  AND @reset_wo13_duplicate_target_count = 3
  AND @reset_wo13_active_canonical_count = 7
  AND @reset_wo13_active_duplicate_count IN (0, 3)
  AND @reset_wo13_active_report_count IN (0, 12)
  AND (SELECT COUNT(*) FROM `mfg_work_order`
       WHERE `work_order_id` = 12 AND `work_order_no` = 'WO-20260716-015-1'
         AND `status` = 'RELEASED' AND `deleted_flag` = 0) = 1
  AND (SELECT COUNT(*) FROM `mfg_task`
       WHERE `task_id` = 81 AND `work_order_id` = 12
         AND `status` IN ('WAIT_START', 'RUNNING') AND `deleted_flag` = 0) = 1,
  1, 0
);

UPDATE `mfg_task`
SET `status` = 'WAIT_START', `version` = `version` + 1,
    `updated_by` = @reset_wo13_user_id, `updated_time` = @reset_wo13_time,
    `remark` = CONCAT(COALESCE(NULLIF(`remark`, ''), ''),
      CASE WHEN `remark` IS NULL OR `remark` = '' THEN '' ELSE '; ' END, @reset_wo13_marker)
WHERE `task_id` = 81 AND `status` = 'RUNNING' AND `deleted_flag` = 0
  AND @reset_wo13_precondition = 1;
SET @repaired_prestart_task_count = ROW_COUNT();

UPDATE `mfg_task` task
JOIN `tmp_reset_wo13_task` target ON target.`task_id` = task.`task_id` AND target.`canonical` = 1
SET task.`completed_quantity` = 0, task.`defect_quantity` = 0,
    task.`version` = task.`version` + 1,
    task.`updated_by` = @reset_wo13_user_id, task.`updated_time` = @reset_wo13_time,
    task.`remark` = CONCAT(COALESCE(NULLIF(task.`remark`, ''), ''),
      CASE WHEN task.`remark` IS NULL OR task.`remark` = '' THEN '' ELSE '; ' END, @reset_wo13_marker)
WHERE task.`deleted_flag` = 0 AND @reset_wo13_precondition = 1
  AND (task.`completed_quantity` <> 0 OR task.`defect_quantity` <> 0
       OR COALESCE(task.`remark`, '') NOT LIKE CONCAT('%',
          CONVERT(@reset_wo13_marker USING utf8mb4) COLLATE utf8mb4_general_ci, '%'));
SET @reset_canonical_task_count = ROW_COUNT();

UPDATE `mfg_task` task
JOIN `tmp_reset_wo13_task` target ON target.`task_id` = task.`task_id` AND target.`canonical` = 0
SET task.`status` = 'CLOSED', task.`deleted_flag` = 1,
    task.`version` = task.`version` + 1,
    task.`updated_by` = @reset_wo13_user_id, task.`updated_time` = @reset_wo13_time,
    task.`remark` = CONCAT(COALESCE(NULLIF(task.`remark`, ''), ''),
      CASE WHEN task.`remark` IS NULL OR task.`remark` = '' THEN '' ELSE '; ' END, @reset_wo13_marker)
WHERE task.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_duplicate_task_count = ROW_COUNT();

UPDATE `mfg_task_operator` operator_record
JOIN `tmp_reset_wo13_task` target ON target.`task_id` = operator_record.`task_id` AND target.`canonical` = 0
SET operator_record.`deleted_flag` = 1
WHERE operator_record.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_duplicate_operator_count = ROW_COUNT();

UPDATE `mfg_report_material` material
JOIN `tmp_reset_wo13_report` target ON target.`report_id` = material.`report_id`
SET material.`deleted_flag` = 1, material.`version` = material.`version` + 1,
    material.`updated_by` = @reset_wo13_user_id, material.`updated_time` = @reset_wo13_time
WHERE material.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_report_material_count = ROW_COUNT();

UPDATE `trace_material_usage` usage_record
JOIN `tmp_reset_wo13_report` target ON target.`report_id` = usage_record.`report_id`
SET usage_record.`deleted_flag` = 1, usage_record.`version` = usage_record.`version` + 1,
    usage_record.`updated_by` = @reset_wo13_user_id, usage_record.`updated_time` = @reset_wo13_time
WHERE usage_record.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_trace_usage_count = ROW_COUNT();

UPDATE `mfg_work_report` report
JOIN `tmp_reset_wo13_report` target ON target.`report_id` = report.`report_id`
SET report.`deleted_flag` = 1, report.`version` = report.`version` + 1,
    report.`updated_by` = @reset_wo13_user_id, report.`updated_time` = @reset_wo13_time,
    report.`remark` = CONCAT(COALESCE(NULLIF(report.`remark`, ''), ''),
      CASE WHEN report.`remark` IS NULL OR report.`remark` = '' THEN '' ELSE '; ' END, @reset_wo13_marker)
WHERE report.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_report_count = ROW_COUNT();

UPDATE `qms_nonconforming_detail` detail
JOIN `tmp_reset_wo13_ncr` target ON target.`ncr_id` = detail.`ncr_id`
SET detail.`deleted_flag` = 1, detail.`version` = detail.`version` + 1,
    detail.`updated_by` = @reset_wo13_user_id, detail.`updated_time` = @reset_wo13_time
WHERE detail.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_ncr_detail_count = ROW_COUNT();

UPDATE `qms_nonconforming_order` ncr
JOIN `tmp_reset_wo13_ncr` target ON target.`ncr_id` = ncr.`ncr_id`
SET ncr.`deleted_flag` = 1, ncr.`version` = ncr.`version` + 1,
    ncr.`updated_by` = @reset_wo13_user_id, ncr.`updated_time` = @reset_wo13_time,
    ncr.`remark` = CONCAT(COALESCE(NULLIF(ncr.`remark`, ''), ''),
      CASE WHEN ncr.`remark` IS NULL OR ncr.`remark` = '' THEN '' ELSE '; ' END, @reset_wo13_marker)
WHERE ncr.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_ncr_count = ROW_COUNT();

UPDATE `qms_inspection_result` result_record
JOIN `tmp_reset_wo13_inspection` target ON target.`inspection_id` = result_record.`inspection_id`
SET result_record.`deleted_flag` = 1, result_record.`version` = result_record.`version` + 1,
    result_record.`updated_by` = @reset_wo13_user_id, result_record.`updated_time` = @reset_wo13_time
WHERE result_record.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_inspection_result_count = ROW_COUNT();

UPDATE `qms_inspection_order` inspection
JOIN `tmp_reset_wo13_inspection` target ON target.`inspection_id` = inspection.`inspection_id`
SET inspection.`deleted_flag` = 1, inspection.`version` = inspection.`version` + 1,
    inspection.`updated_by` = @reset_wo13_user_id, inspection.`updated_time` = @reset_wo13_time,
    inspection.`remark` = CONCAT(COALESCE(NULLIF(inspection.`remark`, ''), ''),
      CASE WHEN inspection.`remark` IS NULL OR inspection.`remark` = '' THEN '' ELSE '; ' END,
      @reset_wo13_marker)
WHERE inspection.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_inspection_count = ROW_COUNT();

INSERT INTO `wms_stock_movement`
  (`movement_no`, `movement_type`, `source_order_type`, `source_order_id`,
   `material_id`, `batch_id`, `quantity`, `direction`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('MV-RESET-WO13-I', inventory.`inventory_id`), 'ADJUSTMENT', 'MFG_WORK_ORDER', 13,
       inventory.`material_id`, inventory.`batch_id`,
       inventory.`available_quantity` + inventory.`locked_quantity` + inventory.`inspection_quantity`,
       'OUT', @reset_wo13_time, @reset_wo13_marker,
       0, @reset_wo13_user_id, @reset_wo13_time,
       @reset_wo13_user_id, @reset_wo13_time, 0
FROM `wms_inventory` inventory
JOIN `tmp_reset_wo13_batch` target ON target.`batch_id` = inventory.`batch_id`
WHERE inventory.`deleted_flag` = 0 AND @reset_wo13_precondition = 1
  AND inventory.`available_quantity` + inventory.`locked_quantity` + inventory.`inspection_quantity` > 0
  AND NOT EXISTS (
    SELECT 1 FROM `wms_stock_movement` movement
    WHERE movement.`movement_no` = CONCAT('MV-RESET-WO13-I', inventory.`inventory_id`)
  );
SET @inserted_reversal_movement_count = ROW_COUNT();

UPDATE `wms_inventory` inventory
JOIN `tmp_reset_wo13_batch` target ON target.`batch_id` = inventory.`batch_id`
SET inventory.`available_quantity` = 0, inventory.`locked_quantity` = 0,
    inventory.`inspection_quantity` = 0, inventory.`deleted_flag` = 1,
    inventory.`version` = inventory.`version` + 1,
    inventory.`updated_by` = @reset_wo13_user_id, inventory.`updated_time` = @reset_wo13_time,
    inventory.`remark` = CONCAT(COALESCE(NULLIF(inventory.`remark`, ''), ''),
      CASE WHEN inventory.`remark` IS NULL OR inventory.`remark` = '' THEN '' ELSE '; ' END,
      @reset_wo13_marker)
WHERE inventory.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_inventory_count = ROW_COUNT();

UPDATE `wms_inbound_detail` detail
JOIN `tmp_reset_wo13_inbound` target ON target.`inbound_id` = detail.`inbound_id`
SET detail.`deleted_flag` = 1, detail.`version` = detail.`version` + 1,
    detail.`updated_by` = @reset_wo13_user_id, detail.`updated_time` = @reset_wo13_time
WHERE detail.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_inbound_detail_count = ROW_COUNT();

UPDATE `wms_inbound_order` inbound
JOIN `tmp_reset_wo13_inbound` target ON target.`inbound_id` = inbound.`inbound_id`
SET inbound.`deleted_flag` = 1, inbound.`version` = inbound.`version` + 1,
    inbound.`updated_by` = @reset_wo13_user_id, inbound.`updated_time` = @reset_wo13_time,
    inbound.`remark` = CONCAT(COALESCE(NULLIF(inbound.`remark`, ''), ''),
      CASE WHEN inbound.`remark` IS NULL OR inbound.`remark` = '' THEN '' ELSE '; ' END,
      @reset_wo13_marker)
WHERE inbound.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_inbound_count = ROW_COUNT();

UPDATE `wms_material_batch` batch
JOIN `tmp_reset_wo13_batch` target ON target.`batch_id` = batch.`batch_id`
SET batch.`status` = 'EXHAUSTED', batch.`deleted_flag` = 1,
    batch.`version` = batch.`version` + 1,
    batch.`updated_by` = @reset_wo13_user_id, batch.`updated_time` = @reset_wo13_time,
    batch.`remark` = CONCAT(COALESCE(NULLIF(batch.`remark`, ''), ''),
      CASE WHEN batch.`remark` IS NULL OR batch.`remark` = '' THEN '' ELSE '; ' END,
      @reset_wo13_marker)
WHERE batch.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_batch_count = ROW_COUNT();

UPDATE `trace_barcode` barcode
JOIN `tmp_reset_wo13_lot` target
  ON barcode.`biz_type` = 'PRODUCT_LOT' AND target.`product_lot_id` = barcode.`biz_id`
SET barcode.`status` = 'DISABLE', barcode.`deleted_flag` = 1,
    barcode.`version` = barcode.`version` + 1,
    barcode.`updated_by` = @reset_wo13_user_id, barcode.`updated_time` = @reset_wo13_time,
    barcode.`remark` = CONCAT(COALESCE(NULLIF(barcode.`remark`, ''), ''),
      CASE WHEN barcode.`remark` IS NULL OR barcode.`remark` = '' THEN '' ELSE '; ' END,
      @reset_wo13_marker)
WHERE barcode.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_barcode_count = ROW_COUNT();

UPDATE `trace_product_lot` lot
JOIN `tmp_reset_wo13_lot` target ON target.`product_lot_id` = lot.`product_lot_id`
SET lot.`status` = 'CLOSED', lot.`deleted_flag` = 1,
    lot.`version` = lot.`version` + 1,
    lot.`updated_by` = @reset_wo13_user_id, lot.`updated_time` = @reset_wo13_time,
    lot.`remark` = CONCAT(COALESCE(NULLIF(lot.`remark`, ''), ''),
      CASE WHEN lot.`remark` IS NULL OR lot.`remark` = '' THEN '' ELSE '; ' END,
      @reset_wo13_marker)
WHERE lot.`deleted_flag` = 0 AND @reset_wo13_precondition = 1;
SET @deleted_product_lot_count = ROW_COUNT();

UPDATE `mfg_work_order`
SET `completed_quantity` = 0, `version` = `version` + 1,
    `updated_by` = @reset_wo13_user_id, `updated_time` = @reset_wo13_time,
    `remark` = CONCAT(COALESCE(NULLIF(`remark`, ''), ''),
      CASE WHEN `remark` IS NULL OR `remark` = '' THEN '' ELSE '; ' END, @reset_wo13_marker)
WHERE `work_order_id` = 13 AND `status` = 'RUNNING' AND `deleted_flag` = 0
  AND @reset_wo13_precondition = 1
  AND (`completed_quantity` <> 0 OR COALESCE(`remark`, '') NOT LIKE CONCAT('%',
       CONVERT(@reset_wo13_marker USING utf8mb4) COLLATE utf8mb4_general_ci, '%'));
SET @reset_work_order_count = ROW_COUNT();

UPDATE `sys_notification`
SET `status` = 'CANCELLED', `version` = `version` + 1,
    `updated_by` = @reset_wo13_user_id, `updated_time` = @reset_wo13_time
WHERE `status` = 'ACTIVE' AND `deleted_flag` = 0 AND @reset_wo13_precondition = 1
  AND (
    (`biz_type` = 'MFG_TASK' AND `biz_id` IN (SELECT `task_id` FROM `tmp_reset_wo13_task`))
    OR (`biz_type` = 'MFG_WORK_REPORT' AND `biz_id` IN (SELECT `report_id` FROM `tmp_reset_wo13_report`))
    OR (`biz_type` = 'QMS_INSPECTION_ORDER' AND `biz_id` IN (SELECT `inspection_id` FROM `tmp_reset_wo13_inspection`))
    OR (`biz_type` = 'QMS_NONCONFORMING_ORDER' AND `biz_id` IN (SELECT `ncr_id` FROM `tmp_reset_wo13_ncr`))
    OR (`biz_type` = 'TRACE_PRODUCT_LOT' AND `biz_id` IN (SELECT `product_lot_id` FROM `tmp_reset_wo13_lot`))
    OR (`biz_type` = 'WMS_INBOUND_ORDER' AND `biz_id` IN (SELECT `inbound_id` FROM `tmp_reset_wo13_inbound`))
  );
SET @cancelled_notification_count = ROW_COUNT();

UPDATE `sys_event_outbox`
SET `status` = 'CANCELLED', `processed_time` = @reset_wo13_time,
    `version` = `version` + 1,
    `updated_by` = @reset_wo13_user_id, `updated_time` = @reset_wo13_time
WHERE `status` IN ('PENDING', 'FAILED', 'PROCESSING') AND `deleted_flag` = 0
  AND @reset_wo13_precondition = 1
  AND (
    (`aggregate_type` = 'MFG_TASK' AND `aggregate_id` IN (SELECT `task_id` FROM `tmp_reset_wo13_task`))
    OR (`aggregate_type` = 'MFG_WORK_REPORT' AND `aggregate_id` IN (SELECT `report_id` FROM `tmp_reset_wo13_report`))
    OR (`aggregate_type` = 'QMS_INSPECTION_ORDER' AND `aggregate_id` IN (SELECT `inspection_id` FROM `tmp_reset_wo13_inspection`))
    OR (`aggregate_type` = 'QMS_NONCONFORMING_ORDER' AND `aggregate_id` IN (SELECT `ncr_id` FROM `tmp_reset_wo13_ncr`))
  );
SET @cancelled_outbox_count = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'MFG_TASK', 81, 'RUNNING', 'WAIT_START', 'REPAIR_TASK_BEFORE_WORK_ORDER_START',
       @reset_wo13_user_id, @reset_wo13_time, @reset_wo13_marker,
       0, @reset_wo13_user_id, @reset_wo13_time, @reset_wo13_user_id, @reset_wo13_time, 0
WHERE @repaired_prestart_task_count = 1
  AND NOT EXISTS (SELECT 1 FROM `sys_status_change_log`
                  WHERE `biz_type` = 'MFG_TASK' AND `biz_id` = 81
                    AND `change_reason` = 'REPAIR_TASK_BEFORE_WORK_ORDER_START'
                    AND `remark` = CONVERT(@reset_wo13_marker USING utf8mb4) COLLATE utf8mb4_general_ci
                    AND `deleted_flag` = 0);
SET @inserted_prestart_status_log_count = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'MFG_TASK', target.`task_id`, 'WAIT_START', 'CLOSED', 'SOFT_DELETE_DUPLICATE_OPERATION_TASK',
       @reset_wo13_user_id, @reset_wo13_time, @reset_wo13_marker,
       0, @reset_wo13_user_id, @reset_wo13_time, @reset_wo13_user_id, @reset_wo13_time, 0
FROM `tmp_reset_wo13_task` target
WHERE target.`canonical` = 0 AND @deleted_duplicate_task_count = 3
  AND NOT EXISTS (SELECT 1 FROM `sys_status_change_log` existing_log
                  WHERE existing_log.`biz_type` = 'MFG_TASK'
                    AND existing_log.`biz_id` = target.`task_id`
                    AND existing_log.`change_reason` = 'SOFT_DELETE_DUPLICATE_OPERATION_TASK'
                    AND existing_log.`remark` = CONVERT(@reset_wo13_marker USING utf8mb4)
                      COLLATE utf8mb4_general_ci
                    AND existing_log.`deleted_flag` = 0);
SET @inserted_duplicate_status_log_count = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @reset_wo13_user_id, 'RESET_WORK_ORDER_EXECUTION', 'mfg_work_order', 13,
       @reset_wo13_time, @reset_wo13_marker,
       0, @reset_wo13_user_id, @reset_wo13_time, @reset_wo13_user_id, @reset_wo13_time, 0
WHERE @reset_work_order_count = 1
  AND NOT EXISTS (SELECT 1 FROM `sys_operation_log`
                  WHERE `module_code` = 'mfg_work_order' AND `biz_id` = 13
                    AND `operation_type` = 'RESET_WORK_ORDER_EXECUTION'
                    AND `remark` = CONVERT(@reset_wo13_marker USING utf8mb4) COLLATE utf8mb4_general_ci
                    AND `deleted_flag` = 0);
SET @inserted_work_order_log_count = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @reset_wo13_user_id, 'RESET_TASK_EXECUTION', 'mfg_task', target.`task_id`,
       @reset_wo13_time, @reset_wo13_marker,
       0, @reset_wo13_user_id, @reset_wo13_time, @reset_wo13_user_id, @reset_wo13_time, 0
FROM `tmp_reset_wo13_task` target
WHERE target.`canonical` = 1 AND @reset_canonical_task_count > 0
  AND NOT EXISTS (SELECT 1 FROM `sys_operation_log` existing_log
                  WHERE existing_log.`module_code` = 'mfg_task'
                    AND existing_log.`biz_id` = target.`task_id`
                    AND existing_log.`operation_type` = 'RESET_TASK_EXECUTION'
                    AND existing_log.`remark` = CONVERT(@reset_wo13_marker USING utf8mb4)
                      COLLATE utf8mb4_general_ci
                    AND existing_log.`deleted_flag` = 0);
SET @inserted_task_log_count = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @reset_wo13_user_id, 'RESET_WORK_REPORT', 'mfg_work_report', target.`report_id`,
       @reset_wo13_time, @reset_wo13_marker,
       0, @reset_wo13_user_id, @reset_wo13_time, @reset_wo13_user_id, @reset_wo13_time, 0
FROM `tmp_reset_wo13_report` target
WHERE @deleted_report_count > 0
  AND NOT EXISTS (SELECT 1 FROM `sys_operation_log` existing_log
                  WHERE existing_log.`module_code` = 'mfg_work_report'
                    AND existing_log.`biz_id` = target.`report_id`
                    AND existing_log.`operation_type` = 'RESET_WORK_REPORT'
                    AND existing_log.`remark` = CONVERT(@reset_wo13_marker USING utf8mb4)
                      COLLATE utf8mb4_general_ci
                    AND existing_log.`deleted_flag` = 0);
SET @inserted_report_log_count = ROW_COUNT();

SET @result_active_report_count = (
  SELECT COUNT(*) FROM `mfg_work_report` report
  JOIN `tmp_reset_wo13_report` target ON target.`report_id` = report.`report_id`
  WHERE report.`deleted_flag` = 0
);
SET @result_active_inspection_count = (
  SELECT COUNT(*) FROM `qms_inspection_order` inspection
  JOIN `tmp_reset_wo13_inspection` target ON target.`inspection_id` = inspection.`inspection_id`
  WHERE inspection.`deleted_flag` = 0
);
SET @result_active_ncr_count = (
  SELECT COUNT(*) FROM `qms_nonconforming_order` ncr
  JOIN `tmp_reset_wo13_ncr` target ON target.`ncr_id` = ncr.`ncr_id`
  WHERE ncr.`deleted_flag` = 0
);
SET @result_active_lot_count = (
  SELECT COUNT(*) FROM `trace_product_lot` lot
  JOIN `tmp_reset_wo13_lot` target ON target.`product_lot_id` = lot.`product_lot_id`
  WHERE lot.`deleted_flag` = 0
);
SET @result_active_batch_count = (
  SELECT COUNT(*) FROM `wms_material_batch` batch
  JOIN `tmp_reset_wo13_batch` target ON target.`batch_id` = batch.`batch_id`
  WHERE batch.`deleted_flag` = 0
);
SET @result_active_inventory_count = (
  SELECT COUNT(*) FROM `wms_inventory` inventory
  JOIN `tmp_reset_wo13_batch` target ON target.`batch_id` = inventory.`batch_id`
  WHERE inventory.`deleted_flag` = 0
);
SET @result_active_inbound_count = (
  SELECT COUNT(*) FROM `wms_inbound_order` inbound
  JOIN `tmp_reset_wo13_inbound` target ON target.`inbound_id` = inbound.`inbound_id`
  WHERE inbound.`deleted_flag` = 0
);
SET @result_stock_movement_net = (
  SELECT COALESCE(SUM(CASE WHEN movement.`direction` = 'IN'
                           THEN movement.`quantity` ELSE -movement.`quantity` END), 0)
  FROM `wms_stock_movement` movement
  JOIN `tmp_reset_wo13_batch` target ON target.`batch_id` = movement.`batch_id`
  WHERE movement.`deleted_flag` = 0
);

SET @reset_wo13_result_valid = IF(
  (SELECT COUNT(*) FROM `mfg_work_order` WHERE `deleted_flag` = 0) = 34
  AND (SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'FINISHED' AND `deleted_flag` = 0) = 31
  AND (SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'RELEASED' AND `deleted_flag` = 0) = 1
  AND (SELECT COUNT(*) FROM `mfg_work_order` WHERE `status` = 'RUNNING' AND `deleted_flag` = 0) = 2
  AND (SELECT COUNT(*) FROM (
         SELECT work_order.`work_order_id`
         FROM `mfg_work_order` work_order
         LEFT JOIN `mfg_task` task ON task.`work_order_id` = work_order.`work_order_id`
           AND task.`deleted_flag` = 0
         WHERE work_order.`deleted_flag` = 0
         GROUP BY work_order.`work_order_id`
         HAVING COUNT(task.`task_id`) <> 7
       ) invalid_work_order) = 0
  AND (SELECT COUNT(*) FROM `mfg_task`
       WHERE `work_order_id` = 13 AND `deleted_flag` = 0) = 7
  AND (SELECT COUNT(DISTINCT `operation_id`) FROM `mfg_task`
       WHERE `work_order_id` = 13 AND `deleted_flag` = 0) = 7
  AND (SELECT COUNT(*) FROM `mfg_task`
       WHERE `work_order_id` = 13 AND `status` = 'RUNNING' AND `deleted_flag` = 0
         AND `completed_quantity` = 0 AND `defect_quantity` = 0) = 7
  AND @result_active_report_count = 0
  AND @result_active_inspection_count = 0
  AND @result_active_ncr_count = 0
  AND @result_active_lot_count = 0
  AND (SELECT COUNT(*) FROM `trace_material_usage`
       WHERE `work_order_id` = 13 AND `deleted_flag` = 0) = 0
  AND @result_active_batch_count = 0
  AND @result_active_inventory_count = 0
  AND @result_active_inbound_count = 0
  AND @result_stock_movement_net = 0
  AND (SELECT COUNT(*) FROM `mfg_task`
       WHERE `work_order_id` = 12 AND `status` = 'WAIT_START' AND `deleted_flag` = 0) = 7
  AND (SELECT COUNT(*) FROM `mfg_work_report` report JOIN `mfg_task` task
       ON task.`task_id` = report.`task_id`
       WHERE task.`work_order_id` = 12 AND report.`deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM `mfg_work_order` work_order JOIN `mfg_task` task
       ON task.`work_order_id` = work_order.`work_order_id` AND task.`deleted_flag` = 0
       WHERE work_order.`status` = 'FINISHED' AND work_order.`deleted_flag` = 0
         AND task.`status` <> 'FINISHED') = 0,
  1, 0
);

SET @reset_wo13_final_statement = IF(
  @reset_wo13_commit = 1
  AND @reset_wo13_precondition = 1
  AND @reset_wo13_result_valid = 1,
  'COMMIT', 'ROLLBACK'
);
PREPARE reset_wo13_final_statement FROM @reset_wo13_final_statement;
EXECUTE reset_wo13_final_statement;
DEALLOCATE PREPARE reset_wo13_final_statement;

SELECT IF(@reset_wo13_commit = 1
          AND @reset_wo13_precondition = 1
          AND @reset_wo13_result_valid = 1,
          'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @reset_wo13_precondition AS `precondition_valid`,
       @reset_wo13_result_valid AS `result_valid`,
       @repaired_prestart_task_count AS `repaired_prestart_tasks`,
       @reset_canonical_task_count AS `reset_canonical_tasks`,
       @deleted_duplicate_task_count AS `deleted_duplicate_tasks`,
       @deleted_duplicate_operator_count AS `deleted_duplicate_operators`,
       @deleted_report_count AS `deleted_reports`,
       @deleted_report_material_count AS `deleted_report_materials`,
       @deleted_trace_usage_count AS `deleted_trace_usage`,
       @deleted_inspection_count AS `deleted_inspections`,
       @deleted_ncr_count AS `deleted_ncr`,
       @deleted_product_lot_count AS `deleted_product_lots`,
       @inserted_reversal_movement_count AS `reversal_movements`,
       @deleted_inventory_count AS `deleted_inventory`,
       @deleted_inbound_count AS `deleted_inbound`,
       @cancelled_notification_count AS `cancelled_notifications`,
       @cancelled_outbox_count AS `cancelled_outbox`,
       @inserted_prestart_status_log_count + @inserted_duplicate_status_log_count AS `status_logs`,
       @inserted_work_order_log_count + @inserted_task_log_count + @inserted_report_log_count AS `operation_logs`;
