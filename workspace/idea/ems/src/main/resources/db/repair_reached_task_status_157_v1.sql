-- Repair task 157, whose valid work reports reached the plan before automatic finishing was implemented.
-- Dry-run by default. Set @repair_reached_task_commit = 1 before sourcing to commit.

SET @repair_reached_task_commit = COALESCE(@repair_reached_task_commit, 0);
SET @repair_reached_task_time = COALESCE(@repair_reached_task_time, CURRENT_TIMESTAMP);
SET @repair_reached_task_marker = 'REPAIR_REACHED_TASK_STATUS_157_V1';
SET @repair_reached_task_user_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS `tmp_reached_task_target`;
CREATE TEMPORARY TABLE `tmp_reached_task_target` AS
SELECT task.`task_id`, task.`task_no`, task.`planned_quantity`,
       COALESCE(SUM(report.`qualified_quantity`), 0) AS `qualified_quantity`,
       COALESCE(SUM(report.`defect_quantity`), 0) AS `defect_quantity`
FROM `mfg_task` task
JOIN `mfg_work_report` report
  ON report.`task_id` = task.`task_id`
 AND report.`deleted_flag` = 0
 AND report.`audit_status` IN ('SUBMITTED', 'APPROVED')
WHERE task.`task_id` = 157
  AND task.`task_no` = 'TASK-20260714-003-1-050'
  AND task.`deleted_flag` = 0
GROUP BY task.`task_id`, task.`task_no`, task.`planned_quantity`;

START TRANSACTION;

SELECT task.`task_id`, task.`task_no`, task.`status`, task.`planned_quantity`,
       target.`qualified_quantity`, target.`defect_quantity`
FROM `mfg_task` task
JOIN `tmp_reached_task_target` target ON target.`task_id` = task.`task_id`
FOR UPDATE;

SET @repair_target_count = (SELECT COUNT(*) FROM `tmp_reached_task_target`);
SET @repair_quantity_reached = (
  SELECT COUNT(*) FROM `tmp_reached_task_target`
  WHERE `qualified_quantity` + `defect_quantity` = `planned_quantity`
);
SET @repair_task_was_running = (
  SELECT COUNT(*) FROM `mfg_task`
  WHERE `task_id` = 157 AND `status` = 'RUNNING' AND `deleted_flag` = 0
);

UPDATE `mfg_task` task
JOIN `tmp_reached_task_target` target ON target.`task_id` = task.`task_id`
SET task.`completed_quantity` = target.`qualified_quantity`,
    task.`defect_quantity` = target.`defect_quantity`,
    task.`status` = 'FINISHED',
    task.`remark` = CASE
      WHEN task.`remark` IS NULL OR task.`remark` = '' THEN '累计报工达到任务计划数量，系统补正为已完成'
      ELSE task.`remark`
    END,
    task.`version` = task.`version` + 1,
    task.`updated_by` = @repair_reached_task_user_id,
    task.`updated_time` = @repair_reached_task_time
WHERE task.`status` = 'RUNNING'
  AND @repair_target_count = 1
  AND @repair_quantity_reached = 1;
SET @repaired_task_count = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'MFG_TASK', 157, 'RUNNING', 'FINISHED', 'REPAIR_REPORTED_TASK_STATUS',
       @repair_reached_task_user_id, @repair_reached_task_time, @repair_reached_task_marker,
       0, @repair_reached_task_user_id, @repair_reached_task_time,
       @repair_reached_task_user_id, @repair_reached_task_time, 0
WHERE @repaired_task_count = 1
  AND NOT EXISTS (
    SELECT 1 FROM `sys_status_change_log`
    WHERE `biz_type` = 'MFG_TASK' AND `biz_id` = 157
      AND `change_reason` = 'REPAIR_REPORTED_TASK_STATUS'
      AND `remark` = CONVERT(@repair_reached_task_marker USING utf8mb4) COLLATE utf8mb4_general_ci
      AND `deleted_flag` = 0
  );
SET @inserted_status_log_count = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @repair_reached_task_user_id, 'REPAIR_REPORTED_TASK_STATUS', 'mfg_task', 157,
       @repair_reached_task_time, @repair_reached_task_marker,
       0, @repair_reached_task_user_id, @repair_reached_task_time,
       @repair_reached_task_user_id, @repair_reached_task_time, 0
WHERE @repaired_task_count = 1
  AND NOT EXISTS (
    SELECT 1 FROM `sys_operation_log`
    WHERE `module_code` = 'mfg_task' AND `biz_id` = 157
      AND `operation_type` = 'REPAIR_REPORTED_TASK_STATUS'
      AND `remark` = CONVERT(@repair_reached_task_marker USING utf8mb4) COLLATE utf8mb4_general_ci
      AND `deleted_flag` = 0
  );
SET @inserted_operation_log_count = ROW_COUNT();

UPDATE `sys_notification`
SET `status` = 'CANCELLED', `version` = `version` + 1,
    `updated_by` = @repair_reached_task_user_id, `updated_time` = @repair_reached_task_time
WHERE `biz_type` = 'MFG_TASK' AND `biz_id` = 157
  AND `event_type` IN ('MFG_TASK_ASSIGNED', 'MFG_TASK_UNASSIGNED', 'MFG_TASK_PAUSED', 'MFG_TASK_OVERDUE')
  AND `status` = 'ACTIVE' AND `deleted_flag` = 0;
SET @cancelled_task_notifications = ROW_COUNT();

UPDATE `sys_event_outbox`
SET `status` = 'CANCELLED', `processed_time` = @repair_reached_task_time,
    `version` = `version` + 1, `updated_by` = @repair_reached_task_user_id,
    `updated_time` = @repair_reached_task_time
WHERE `aggregate_type` = 'MFG_TASK' AND `aggregate_id` = 157
  AND `event_type` IN ('MFG_TASK_ASSIGNED', 'MFG_TASK_UNASSIGNED', 'MFG_TASK_PAUSED', 'MFG_TASK_OVERDUE')
  AND `status` IN ('PENDING', 'FAILED', 'PROCESSING') AND `deleted_flag` = 0;
SET @cancelled_task_outbox = ROW_COUNT();

SET @repair_result_valid = (
  SELECT COUNT(*) FROM `mfg_task` task
  JOIN `tmp_reached_task_target` target ON target.`task_id` = task.`task_id`
  WHERE task.`status` = 'FINISHED'
    AND task.`completed_quantity` = target.`qualified_quantity`
    AND task.`defect_quantity` = target.`defect_quantity`
    AND task.`deleted_flag` = 0
);

SET @repair_reached_task_final_statement = IF(
  @repair_reached_task_commit = 1
  AND @repair_target_count = 1
  AND @repair_quantity_reached = 1
  AND @repair_result_valid = 1,
  'COMMIT', 'ROLLBACK'
);
PREPARE repair_reached_task_final_statement FROM @repair_reached_task_final_statement;
EXECUTE repair_reached_task_final_statement;
DEALLOCATE PREPARE repair_reached_task_final_statement;

SELECT IF(@repair_reached_task_commit = 1 AND @repair_result_valid = 1, 'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @repair_task_was_running AS `task_was_running`,
       @repaired_task_count AS `repaired_tasks`,
       @inserted_status_log_count AS `status_logs`,
       @inserted_operation_log_count AS `operation_logs`,
       @cancelled_task_notifications AS `cancelled_notifications`,
       @cancelled_task_outbox AS `cancelled_outbox`;
