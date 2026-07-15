-- Restore two accidentally started packaging tasks and their submitted test reports.
-- Default mode is dry-run. Set @restore_accidental_tasks_commit = 1 before SOURCE to commit.
-- This script does not alter schema or touch any FINISHED work order.

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @restore_accidental_tasks_commit = COALESCE(@restore_accidental_tasks_commit, 0);
SET @restore_accidental_tasks_time = COALESCE(@restore_accidental_tasks_time, CURRENT_TIMESTAMP);
SET @restore_accidental_tasks_marker = 'RESTORE_ACCIDENTAL_STARTED_TASKS_V1';

DROP TEMPORARY TABLE IF EXISTS `tmp_restore_accidental_tasks`;
CREATE TEMPORARY TABLE `tmp_restore_accidental_tasks` (
  `task_id` BIGINT UNSIGNED NOT NULL,
  `old_status` VARCHAR(32) NOT NULL,
  PRIMARY KEY (`task_id`)
) ENGINE=InnoDB;

DROP TEMPORARY TABLE IF EXISTS `tmp_restore_accidental_tasks_assert`;
CREATE TEMPORARY TABLE `tmp_restore_accidental_tasks_assert` (
  `assertion_name` VARCHAR(160) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB;

DROP TEMPORARY TABLE IF EXISTS `tmp_restore_accidental_reports`;
CREATE TEMPORARY TABLE `tmp_restore_accidental_reports` (
  `report_id` BIGINT UNSIGNED NOT NULL,
  `old_deleted_flag` TINYINT NOT NULL,
  PRIMARY KEY (`report_id`)
) ENGINE=InnoDB;

START TRANSACTION;

INSERT INTO `tmp_restore_accidental_tasks` (`task_id`, `old_status`)
SELECT task.`task_id`, task.`status`
FROM `mfg_task` task
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = task.`work_order_id`
 AND work_order.`status` = 'RELEASED' AND work_order.`deleted_flag` = 0
JOIN `process_operation` operation
  ON operation.`operation_id` = task.`operation_id`
 AND operation.`operation_code` = 'OP-PACK' AND operation.`deleted_flag` = 0
WHERE task.`task_no` IN ('TASK-20260716-015-1-070', 'TASK-20260715-014-1-070')
  AND task.`status` IN ('RUNNING', 'WAIT_START')
  AND task.`completed_quantity` = 0 AND task.`defect_quantity` = 0
  AND task.`deleted_flag` = 0
FOR UPDATE;

INSERT INTO `tmp_restore_accidental_tasks_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly two intended RELEASED work-order packaging tasks', IF(COUNT(*) = 2, 1, NULL)
FROM `tmp_restore_accidental_tasks`;

INSERT INTO `tmp_restore_accidental_reports` (`report_id`, `old_deleted_flag`)
SELECT report.`report_id`, report.`deleted_flag`
FROM `mfg_work_report` report
JOIN `tmp_restore_accidental_tasks` target ON target.`task_id` = report.`task_id`
WHERE report.`report_no` IN ('WR1784080308896', 'WR1784080408722')
  AND report.`audit_status` = 'SUBMITTED'
FOR UPDATE;

INSERT INTO `tmp_restore_accidental_tasks_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly two intended submitted test reports', IF(COUNT(*) = 2, 1, NULL)
FROM `tmp_restore_accidental_reports`;

INSERT INTO `tmp_restore_accidental_tasks_assert` (`assertion_name`, `assertion_passed`)
SELECT 'target tasks have no other active work reports', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_work_report` report
JOIN `tmp_restore_accidental_tasks` target ON target.`task_id` = report.`task_id`
LEFT JOIN `tmp_restore_accidental_reports` intended ON intended.`report_id` = report.`report_id`
WHERE report.`deleted_flag` = 0 AND intended.`report_id` IS NULL;

INSERT INTO `tmp_restore_accidental_tasks_assert` (`assertion_name`, `assertion_passed`)
SELECT 'test reports have no material consumption rows', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_report_material` material
JOIN `tmp_restore_accidental_reports` report ON report.`report_id` = material.`report_id`
WHERE material.`deleted_flag` = 0;

SET @restore_accidental_tasks_user_id = (
  SELECT user.`user_id` FROM `sys_user` user
  WHERE user.`username` = 'admin' AND user.`deleted_flag` = 0
  LIMIT 1
);

INSERT INTO `tmp_restore_accidental_tasks_assert` (`assertion_name`, `assertion_passed`)
VALUES ('admin audit user exists', IF(@restore_accidental_tasks_user_id IS NOT NULL, 1, NULL));

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'MFG_TASK', target.`task_id`, 'RUNNING', 'WAIT_START', 'RESTORE_ACCIDENTAL_START',
       @restore_accidental_tasks_user_id, @restore_accidental_tasks_time,
       CONCAT(@restore_accidental_tasks_marker, ': restore test-modified packaging task'), 0,
       @restore_accidental_tasks_user_id, @restore_accidental_tasks_time,
       @restore_accidental_tasks_user_id, @restore_accidental_tasks_time, 0
FROM `tmp_restore_accidental_tasks` target
WHERE target.`old_status` = 'RUNNING';
SET @restore_accidental_tasks_status_logs = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @restore_accidental_tasks_user_id, 'RESTORE_TASK', 'mfg_task', target.`task_id`,
       @restore_accidental_tasks_time,
       CONCAT(@restore_accidental_tasks_marker, ': restore RUNNING to WAIT_START'), 0,
       @restore_accidental_tasks_user_id, @restore_accidental_tasks_time,
       @restore_accidental_tasks_user_id, @restore_accidental_tasks_time, 0
FROM `tmp_restore_accidental_tasks` target
WHERE target.`old_status` = 'RUNNING';
SET @restore_accidental_tasks_operation_logs = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @restore_accidental_tasks_user_id, 'RESTORE_REPORT', 'mfg_work_report', report.`report_id`,
       @restore_accidental_tasks_time,
       CONCAT(@restore_accidental_tasks_marker, ': soft-delete accidental submitted report'), 0,
       @restore_accidental_tasks_user_id, @restore_accidental_tasks_time,
       @restore_accidental_tasks_user_id, @restore_accidental_tasks_time, 0
FROM `tmp_restore_accidental_reports` report
WHERE report.`old_deleted_flag` = 0;
SET @restore_accidental_tasks_report_logs = ROW_COUNT();

UPDATE `mfg_work_report` report
JOIN `tmp_restore_accidental_reports` target ON target.`report_id` = report.`report_id`
SET report.`deleted_flag` = 1,
    report.`version` = report.`version` + 1,
    report.`updated_by` = @restore_accidental_tasks_user_id,
    report.`updated_time` = @restore_accidental_tasks_time
WHERE report.`audit_status` = 'SUBMITTED' AND report.`deleted_flag` = 0;
SET @restore_accidental_tasks_reports_deleted = ROW_COUNT();

UPDATE `mfg_task` task
JOIN `tmp_restore_accidental_tasks` target ON target.`task_id` = task.`task_id`
SET task.`status` = 'WAIT_START',
    task.`version` = task.`version` + 1,
    task.`updated_by` = @restore_accidental_tasks_user_id,
    task.`updated_time` = @restore_accidental_tasks_time
WHERE target.`old_status` = 'RUNNING' AND task.`status` = 'RUNNING'
  AND task.`deleted_flag` = 0;
SET @restore_accidental_tasks_updated = ROW_COUNT();

SELECT @restore_accidental_tasks_commit AS `commit_mode`,
       @restore_accidental_tasks_updated AS `tasks_restored`,
       @restore_accidental_tasks_reports_deleted AS `reports_soft_deleted`,
       @restore_accidental_tasks_status_logs AS `status_logs_written`,
       @restore_accidental_tasks_operation_logs + @restore_accidental_tasks_report_logs AS `operation_logs_written`;

SELECT task.`task_no`, work_order.`work_order_no`, work_order.`status` AS `work_order_status`,
       task.`status` AS `task_status`, task.`completed_quantity`, task.`defect_quantity`
FROM `mfg_task` task
JOIN `mfg_work_order` work_order ON work_order.`work_order_id` = task.`work_order_id`
WHERE task.`task_id` IN (SELECT target.`task_id` FROM `tmp_restore_accidental_tasks` target)
ORDER BY task.`task_no`;

SELECT report.`report_no`, report.`audit_status`, report.`qualified_quantity`, report.`defect_quantity`,
       report.`deleted_flag`
FROM `mfg_work_report` report
WHERE report.`report_id` IN (SELECT target.`report_id` FROM `tmp_restore_accidental_reports` target)
ORDER BY report.`report_no`;

SET @restore_accidental_tasks_finish_sql = IF(@restore_accidental_tasks_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE restore_accidental_tasks_finish FROM @restore_accidental_tasks_finish_sql;
EXECUTE restore_accidental_tasks_finish;
DEALLOCATE PREPARE restore_accidental_tasks_finish;
