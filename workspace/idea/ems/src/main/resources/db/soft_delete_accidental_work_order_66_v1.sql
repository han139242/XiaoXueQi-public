-- Soft-delete the accidentally generated and released work order with no downstream records.
-- Default mode is dry-run. Set @delete_accidental_work_order_commit = 1 before SOURCE to commit.

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @delete_accidental_work_order_commit = COALESCE(@delete_accidental_work_order_commit, 0);
SET @delete_accidental_work_order_time = COALESCE(@delete_accidental_work_order_time, CURRENT_TIMESTAMP);
SET @delete_accidental_work_order_marker = 'SOFT_DELETE_ACCIDENTAL_WORK_ORDER_66_V1';

DROP TEMPORARY TABLE IF EXISTS `tmp_delete_accidental_work_order`;
CREATE TEMPORARY TABLE `tmp_delete_accidental_work_order` (
  `work_order_id` BIGINT UNSIGNED NOT NULL,
  `old_deleted_flag` TINYINT NOT NULL,
  PRIMARY KEY (`work_order_id`)
) ENGINE=InnoDB;

DROP TEMPORARY TABLE IF EXISTS `tmp_delete_accidental_dispatch`;
CREATE TEMPORARY TABLE `tmp_delete_accidental_dispatch` (
  `dispatch_id` BIGINT UNSIGNED NOT NULL,
  `old_deleted_flag` TINYINT NOT NULL,
  PRIMARY KEY (`dispatch_id`)
) ENGINE=InnoDB;

DROP TEMPORARY TABLE IF EXISTS `tmp_delete_accidental_task`;
CREATE TEMPORARY TABLE `tmp_delete_accidental_task` (
  `task_id` BIGINT UNSIGNED NOT NULL,
  `old_deleted_flag` TINYINT NOT NULL,
  PRIMARY KEY (`task_id`)
) ENGINE=InnoDB;

DROP TEMPORARY TABLE IF EXISTS `tmp_delete_accidental_report`;
CREATE TEMPORARY TABLE `tmp_delete_accidental_report` (
  `report_id` BIGINT UNSIGNED NOT NULL,
  `old_deleted_flag` TINYINT NOT NULL,
  PRIMARY KEY (`report_id`)
) ENGINE=InnoDB;

DROP TEMPORARY TABLE IF EXISTS `tmp_delete_accidental_report_material`;
CREATE TEMPORARY TABLE `tmp_delete_accidental_report_material` (
  `id` BIGINT UNSIGNED NOT NULL,
  `old_deleted_flag` TINYINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB;

DROP TEMPORARY TABLE IF EXISTS `tmp_delete_accidental_plan`;
CREATE TEMPORARY TABLE `tmp_delete_accidental_plan` (
  `plan_id` BIGINT UNSIGNED NOT NULL,
  `old_status` VARCHAR(32) NOT NULL,
  PRIMARY KEY (`plan_id`)
) ENGINE=InnoDB;

DROP TEMPORARY TABLE IF EXISTS `tmp_delete_accidental_order`;
CREATE TEMPORARY TABLE `tmp_delete_accidental_order` (
  `production_order_id` BIGINT UNSIGNED NOT NULL,
  `old_status` VARCHAR(32) NOT NULL,
  PRIMARY KEY (`production_order_id`)
) ENGINE=InnoDB;

DROP TEMPORARY TABLE IF EXISTS `tmp_delete_accidental_work_order_assert`;
CREATE TEMPORARY TABLE `tmp_delete_accidental_work_order_assert` (
  `assertion_name` VARCHAR(160) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB;

START TRANSACTION;

INSERT INTO `tmp_delete_accidental_work_order` (`work_order_id`, `old_deleted_flag`)
SELECT work_order.`work_order_id`, work_order.`deleted_flag`
FROM `mfg_work_order` work_order
WHERE work_order.`work_order_no` = 'WO-34-20260715183359975'
  AND work_order.`plan_id` = 34
  AND work_order.`status` = 'FINISHED'
  AND work_order.`planned_quantity` = 50
  AND work_order.`completed_quantity` = 50
  AND work_order.`created_by` = 1
  AND work_order.`created_time` = '2026-07-15 18:34:00'
FOR UPDATE;

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly one intended work order exists', IF(COUNT(*) = 1, 1, NULL)
FROM `tmp_delete_accidental_work_order`;

SET @delete_accidental_work_order_id = (
  SELECT target.`work_order_id` FROM `tmp_delete_accidental_work_order` target LIMIT 1
);

INSERT INTO `tmp_delete_accidental_plan` (`plan_id`, `old_status`)
SELECT plan.`plan_id`, plan.`status`
FROM `mfg_production_plan` plan
WHERE plan.`plan_id` = 34
  AND plan.`production_order_id` = 37
  AND plan.`status` IN ('FINISHED', 'RELEASED')
  AND plan.`deleted_flag` = 0
FOR UPDATE;

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly one intended production plan exists', IF(COUNT(*) = 1, 1, NULL)
FROM `tmp_delete_accidental_plan`;

INSERT INTO `tmp_delete_accidental_order` (`production_order_id`, `old_status`)
SELECT production_order.`production_order_id`, production_order.`status`
FROM `mfg_production_order` production_order
WHERE production_order.`production_order_id` = 37
  AND production_order.`production_order_no` = 'test-001'
  AND production_order.`status` IN ('FINISHED', 'RELEASED')
  AND production_order.`deleted_flag` = 0
FOR UPDATE;

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly one intended production order exists', IF(COUNT(*) = 1, 1, NULL)
FROM `tmp_delete_accidental_order`;

INSERT INTO `tmp_delete_accidental_dispatch` (`dispatch_id`, `old_deleted_flag`)
SELECT dispatch_order.`dispatch_id`, dispatch_order.`deleted_flag`
FROM `mfg_dispatch_order` dispatch_order
WHERE dispatch_order.`work_order_id` = @delete_accidental_work_order_id
  AND dispatch_order.`dispatch_no` = 'test001'
  AND dispatch_order.`status` = 'RELEASED'
  AND dispatch_order.`planned_quantity` = 50
  AND dispatch_order.`created_by` = 1
  AND dispatch_order.`created_time` = '2026-07-15 18:55:54'
FOR UPDATE;

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly one intended test dispatch exists', IF(COUNT(*) = 1, 1, NULL)
FROM `tmp_delete_accidental_dispatch`;

SET @delete_accidental_dispatch_id = (
  SELECT target.`dispatch_id` FROM `tmp_delete_accidental_dispatch` target LIMIT 1
);

INSERT INTO `tmp_delete_accidental_task` (`task_id`, `old_deleted_flag`)
SELECT task.`task_id`, task.`deleted_flag`
FROM `mfg_task` task
WHERE task.`work_order_id` = @delete_accidental_work_order_id
  AND task.`dispatch_id` = @delete_accidental_dispatch_id
  AND task.`task_no` = 'test001'
  AND task.`status` = 'FINISHED'
  AND task.`planned_quantity` = 50
  AND task.`completed_quantity` = 50
  AND task.`defect_quantity` = 0
  AND task.`created_by` = 1
  AND task.`created_time` = '2026-07-15 18:57:09'
FOR UPDATE;

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly one intended finished test task exists', IF(COUNT(*) = 1, 1, NULL)
FROM `tmp_delete_accidental_task`;

SET @delete_accidental_task_id = (
  SELECT target.`task_id` FROM `tmp_delete_accidental_task` target LIMIT 1
);

INSERT INTO `tmp_delete_accidental_report` (`report_id`, `old_deleted_flag`)
SELECT report.`report_id`, report.`deleted_flag`
FROM `mfg_work_report` report
WHERE report.`report_id` = 277
  AND report.`report_no` = '001'
  AND report.`task_id` = @delete_accidental_task_id
  AND report.`audit_status` = 'APPROVED'
  AND report.`qualified_quantity` = 50
  AND report.`defect_quantity` = 0
FOR UPDATE;

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly one intended approved test report exists', IF(COUNT(*) = 1, 1, NULL)
FROM `tmp_delete_accidental_report`;

SET @delete_accidental_report_id = (
  SELECT target.`report_id` FROM `tmp_delete_accidental_report` target LIMIT 1
);

INSERT INTO `tmp_delete_accidental_report_material` (`id`, `old_deleted_flag`)
SELECT material.`id`, material.`deleted_flag`
FROM `mfg_report_material` material
WHERE material.`id` = 287
  AND material.`report_id` = @delete_accidental_report_id
  AND material.`material_id` = 20
  AND material.`batch_id` = 75
  AND material.`used_quantity` = 2
  AND material.`scrap_quantity` = 2
FOR UPDATE;

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly one intended test report material exists', IF(COUNT(*) = 1, 1, NULL)
FROM `tmp_delete_accidental_report_material`;

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'work order has no other active downstream records', IF(
  (SELECT COUNT(*) FROM `mfg_dispatch_order` WHERE `work_order_id` = @delete_accidental_work_order_id
     AND `dispatch_id` <> @delete_accidental_dispatch_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_task` WHERE `work_order_id` = @delete_accidental_work_order_id
     AND `task_id` <> @delete_accidental_task_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_work_order_material` WHERE `work_order_id` = @delete_accidental_work_order_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `qms_inspection_order` WHERE `work_order_id` = @delete_accidental_work_order_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `trace_material_usage` WHERE `work_order_id` = @delete_accidental_work_order_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `trace_product_lot` WHERE `work_order_id` = @delete_accidental_work_order_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `wms_outbound_order` WHERE `work_order_id` = @delete_accidental_work_order_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `wms_return_order` WHERE `work_order_id` = @delete_accidental_work_order_id AND `deleted_flag` = 0) = 0,
  1, NULL);

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'test task has no active business children', IF(
  (SELECT COUNT(*) FROM `mfg_task_operator` WHERE `task_id` = @delete_accidental_task_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_work_report` WHERE `task_id` = @delete_accidental_task_id
     AND `report_id` <> @delete_accidental_report_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `qms_inspection_order` WHERE `task_id` = @delete_accidental_task_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_exception` WHERE `task_id` = @delete_accidental_task_id AND `deleted_flag` = 0) = 0,
  1, NULL);

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'test report has no other active business children', IF(
  (SELECT COUNT(*) FROM `mfg_report_material` WHERE `report_id` = @delete_accidental_report_id
     AND `id` <> 287 AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `qms_inspection_order` WHERE `report_id` = @delete_accidental_report_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `trace_material_usage` WHERE `report_id` = @delete_accidental_report_id AND `deleted_flag` = 0) = 0,
  1, NULL);

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'plan and order have no other active children', IF(
  (SELECT COUNT(*) FROM `mfg_work_order` WHERE `plan_id` = 34
     AND `work_order_id` <> @delete_accidental_work_order_id AND `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_production_plan` WHERE `production_order_id` = 37
     AND `plan_id` <> 34 AND `deleted_flag` = 0) = 0,
  1, NULL);

SET @delete_accidental_work_order_user_id = (
  SELECT user.`user_id` FROM `sys_user` user
  WHERE user.`username` = 'admin' AND user.`deleted_flag` = 0
  LIMIT 1
);

INSERT INTO `tmp_delete_accidental_work_order_assert` (`assertion_name`, `assertion_passed`)
VALUES ('admin audit user exists', IF(@delete_accidental_work_order_user_id IS NOT NULL, 1, NULL));

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @delete_accidental_work_order_user_id, 'REMOVE_WORK_ORDER', 'mfg_work_order', target.`work_order_id`,
       @delete_accidental_work_order_time,
       CONCAT(@delete_accidental_work_order_marker, ': soft-delete accidental finished work order'), 0,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time, 0
FROM `tmp_delete_accidental_work_order` target
WHERE target.`old_deleted_flag` = 0;
SET @delete_accidental_work_order_logs = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @delete_accidental_work_order_user_id, 'REMOVE_DISPATCH', 'mfg_dispatch_order', target.`dispatch_id`,
       @delete_accidental_work_order_time,
       CONCAT(@delete_accidental_work_order_marker, ': soft-delete accidental test dispatch'), 0,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time, 0
FROM `tmp_delete_accidental_dispatch` target
WHERE target.`old_deleted_flag` = 0;
SET @delete_accidental_dispatch_logs = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @delete_accidental_work_order_user_id, 'REMOVE_TASK', 'mfg_task', target.`task_id`,
       @delete_accidental_work_order_time,
       CONCAT(@delete_accidental_work_order_marker, ': soft-delete accidental finished test task'), 0,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time, 0
FROM `tmp_delete_accidental_task` target
WHERE target.`old_deleted_flag` = 0;
SET @delete_accidental_task_logs = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @delete_accidental_work_order_user_id, 'REMOVE_WORK_REPORT', 'mfg_work_report', target.`report_id`,
       @delete_accidental_work_order_time,
       CONCAT(@delete_accidental_work_order_marker, ': soft-delete accidental approved test report'), 0,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time, 0
FROM `tmp_delete_accidental_report` target
WHERE target.`old_deleted_flag` = 0;
SET @delete_accidental_report_logs = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @delete_accidental_work_order_user_id, 'REMOVE_REPORT_MATERIAL', 'mfg_report_material', target.`id`,
       @delete_accidental_work_order_time,
       CONCAT(@delete_accidental_work_order_marker, ': soft-delete accidental test material usage'), 0,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time, 0
FROM `tmp_delete_accidental_report_material` target
WHERE target.`old_deleted_flag` = 0;
SET @delete_accidental_report_material_logs = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'MFG_PRODUCTION_PLAN', target.`plan_id`, 'FINISHED', 'RELEASED',
       'ROLLBACK_ACCIDENTAL_WORK_ORDER', @delete_accidental_work_order_user_id,
       @delete_accidental_work_order_time, @delete_accidental_work_order_marker, 0,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time, 0
FROM `tmp_delete_accidental_plan` target
WHERE target.`old_status` = 'FINISHED';
SET @delete_accidental_plan_status_logs = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'MFG_PRODUCTION_ORDER', target.`production_order_id`, 'FINISHED', 'RELEASED',
       'ROLLBACK_ACCIDENTAL_WORK_ORDER', @delete_accidental_work_order_user_id,
       @delete_accidental_work_order_time, @delete_accidental_work_order_marker, 0,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time,
       @delete_accidental_work_order_user_id, @delete_accidental_work_order_time, 0
FROM `tmp_delete_accidental_order` target
WHERE target.`old_status` = 'FINISHED';
SET @delete_accidental_order_status_logs = ROW_COUNT();

UPDATE `mfg_report_material` material
JOIN `tmp_delete_accidental_report_material` target ON target.`id` = material.`id`
SET material.`deleted_flag` = 1,
    material.`version` = material.`version` + 1,
    material.`updated_by` = @delete_accidental_work_order_user_id,
    material.`updated_time` = @delete_accidental_work_order_time
WHERE target.`old_deleted_flag` = 0 AND material.`deleted_flag` = 0;
SET @delete_accidental_report_materials_updated = ROW_COUNT();

UPDATE `mfg_work_report` report
JOIN `tmp_delete_accidental_report` target ON target.`report_id` = report.`report_id`
SET report.`deleted_flag` = 1,
    report.`version` = report.`version` + 1,
    report.`updated_by` = @delete_accidental_work_order_user_id,
    report.`updated_time` = @delete_accidental_work_order_time
WHERE target.`old_deleted_flag` = 0 AND report.`deleted_flag` = 0;
SET @delete_accidental_reports_updated = ROW_COUNT();

UPDATE `mfg_task` task
JOIN `tmp_delete_accidental_task` target ON target.`task_id` = task.`task_id`
SET task.`deleted_flag` = 1,
    task.`version` = task.`version` + 1,
    task.`updated_by` = @delete_accidental_work_order_user_id,
    task.`updated_time` = @delete_accidental_work_order_time
WHERE target.`old_deleted_flag` = 0 AND task.`deleted_flag` = 0;
SET @delete_accidental_tasks_updated = ROW_COUNT();

UPDATE `mfg_dispatch_order` dispatch_order
JOIN `tmp_delete_accidental_dispatch` target ON target.`dispatch_id` = dispatch_order.`dispatch_id`
SET dispatch_order.`deleted_flag` = 1,
    dispatch_order.`version` = dispatch_order.`version` + 1,
    dispatch_order.`updated_by` = @delete_accidental_work_order_user_id,
    dispatch_order.`updated_time` = @delete_accidental_work_order_time
WHERE target.`old_deleted_flag` = 0 AND dispatch_order.`deleted_flag` = 0;
SET @delete_accidental_dispatches_updated = ROW_COUNT();

UPDATE `mfg_work_order` work_order
JOIN `tmp_delete_accidental_work_order` target ON target.`work_order_id` = work_order.`work_order_id`
SET work_order.`deleted_flag` = 1,
    work_order.`version` = work_order.`version` + 1,
    work_order.`updated_by` = @delete_accidental_work_order_user_id,
    work_order.`updated_time` = @delete_accidental_work_order_time
WHERE target.`old_deleted_flag` = 0 AND work_order.`deleted_flag` = 0;
SET @delete_accidental_work_order_updated = ROW_COUNT();

UPDATE `mfg_production_plan` plan
JOIN `tmp_delete_accidental_plan` target ON target.`plan_id` = plan.`plan_id`
SET plan.`status` = 'RELEASED',
    plan.`version` = plan.`version` + 1,
    plan.`updated_by` = @delete_accidental_work_order_user_id,
    plan.`updated_time` = @delete_accidental_work_order_time
WHERE target.`old_status` = 'FINISHED' AND plan.`status` = 'FINISHED' AND plan.`deleted_flag` = 0;
SET @delete_accidental_plans_restored = ROW_COUNT();

UPDATE `mfg_production_order` production_order
JOIN `tmp_delete_accidental_order` target
  ON target.`production_order_id` = production_order.`production_order_id`
SET production_order.`status` = 'RELEASED',
    production_order.`version` = production_order.`version` + 1,
    production_order.`updated_by` = @delete_accidental_work_order_user_id,
    production_order.`updated_time` = @delete_accidental_work_order_time
WHERE target.`old_status` = 'FINISHED' AND production_order.`status` = 'FINISHED'
  AND production_order.`deleted_flag` = 0;
SET @delete_accidental_orders_restored = ROW_COUNT();

SELECT @delete_accidental_work_order_commit AS `commit_mode`,
       @delete_accidental_work_order_updated AS `work_orders_soft_deleted`,
       @delete_accidental_dispatches_updated AS `dispatches_soft_deleted`,
       @delete_accidental_tasks_updated AS `tasks_soft_deleted`,
       @delete_accidental_reports_updated AS `reports_soft_deleted`,
       @delete_accidental_report_materials_updated AS `report_materials_soft_deleted`,
       @delete_accidental_plans_restored AS `plans_restored`,
       @delete_accidental_orders_restored AS `orders_restored`,
       @delete_accidental_work_order_logs + @delete_accidental_dispatch_logs
         + @delete_accidental_task_logs + @delete_accidental_report_logs
         + @delete_accidental_report_material_logs AS `operation_logs_written`,
       @delete_accidental_plan_status_logs + @delete_accidental_order_status_logs AS `status_logs_written`;

SELECT work_order.`work_order_id`, work_order.`work_order_no`, work_order.`status`,
       work_order.`planned_quantity`, work_order.`completed_quantity`, work_order.`deleted_flag`
FROM `mfg_work_order` work_order
WHERE work_order.`work_order_id` = @delete_accidental_work_order_id;

SELECT dispatch_order.`dispatch_id`, dispatch_order.`dispatch_no`, dispatch_order.`status`,
       dispatch_order.`deleted_flag`
FROM `mfg_dispatch_order` dispatch_order
WHERE dispatch_order.`dispatch_id` = @delete_accidental_dispatch_id;

SELECT task.`task_id`, task.`task_no`, task.`status`, task.`completed_quantity`, task.`defect_quantity`,
       task.`deleted_flag`
FROM `mfg_task` task
WHERE task.`task_id` = @delete_accidental_task_id;

SELECT report.`report_id`, report.`report_no`, report.`audit_status`, report.`qualified_quantity`,
       report.`deleted_flag`
FROM `mfg_work_report` report
WHERE report.`report_id` = @delete_accidental_report_id;

SELECT plan.`plan_id`, plan.`plan_no`, plan.`status`, production_order.`production_order_id`,
       production_order.`production_order_no`, production_order.`status` AS `production_order_status`
FROM `mfg_production_plan` plan
JOIN `mfg_production_order` production_order
  ON production_order.`production_order_id` = plan.`production_order_id`
WHERE plan.`plan_id` = 34;

SELECT COUNT(*) AS `active_work_orders`
FROM `mfg_work_order` work_order
WHERE work_order.`deleted_flag` = 0;

SET @delete_accidental_work_order_finish_sql = IF(
  @delete_accidental_work_order_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE delete_accidental_work_order_finish FROM @delete_accidental_work_order_finish_sql;
EXECUTE delete_accidental_work_order_finish;
DEALLOCATE PREPARE delete_accidental_work_order_finish;
