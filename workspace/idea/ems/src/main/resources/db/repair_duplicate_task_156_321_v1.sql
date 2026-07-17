-- Repair the duplicate operation task shown on work order 23.
-- Task 156 is the canonical route task. Task 321 is an erroneous supplement task.
-- Dry-run by default. Set @repair_duplicate_task_commit = 1 before sourcing to commit.

SET @repair_duplicate_task_commit = COALESCE(@repair_duplicate_task_commit, 0);
SET @repair_duplicate_task_time = COALESCE(@repair_duplicate_task_time, CURRENT_TIMESTAMP);
SET @repair_duplicate_task_marker = 'REPAIR_DUPLICATE_TASK_156_321_V1';
SET @repair_duplicate_task_user_id = (
  SELECT `user_id`
  FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  LIMIT 1
);

START TRANSACTION;

SELECT `task_id`, `task_no`, `work_order_id`, `operation_id`, `status`,
       `deleted_flag`, `version`
FROM `mfg_task`
WHERE `task_id` IN (156, 321)
FOR UPDATE;

SELECT `id`, `task_id`, `employee_id`, `deleted_flag`
FROM `mfg_task_operator`
WHERE `id` IN (59, 369)
FOR UPDATE;

SET @repair_duplicate_task_precondition = (
  SELECT IF(
    (SELECT COUNT(*) FROM `mfg_task`
     WHERE `task_id` = 156
       AND `task_no` = 'TASK-20260714-003-1-060'
       AND `work_order_id` = 23
       AND `operation_id` = 6
       AND `deleted_flag` = 0
       AND `status` IN ('CLOSED', 'RUNNING')) = 1
    AND
    (SELECT COUNT(*) FROM `mfg_task`
     WHERE `task_id` = 321
       AND `task_no` = 'TASK-SUPPLEMENT-156-20260716190307819'
       AND `work_order_id` = 23
       AND `operation_id` = 6
       AND ((`status` = 'RUNNING' AND `deleted_flag` = 0)
         OR (`status` = 'CLOSED' AND `deleted_flag` = 1))) = 1,
    1, 0
  )
);

UPDATE `mfg_task`
SET `status` = 'RUNNING',
    `remark` = @repair_duplicate_task_marker,
    `version` = `version` + 1,
    `updated_by` = @repair_duplicate_task_user_id,
    `updated_time` = @repair_duplicate_task_time
WHERE `task_id` = 156
  AND `status` = 'CLOSED'
  AND `deleted_flag` = 0
  AND @repair_duplicate_task_precondition = 1;
SET @restored_canonical_task_count = ROW_COUNT();

UPDATE `mfg_task`
SET `status` = 'CLOSED',
    `deleted_flag` = 1,
    `remark` = CONCAT(COALESCE(NULLIF(`remark`, ''), ''),
      CASE WHEN `remark` IS NULL OR `remark` = '' THEN '' ELSE '; ' END,
      @repair_duplicate_task_marker),
    `version` = `version` + 1,
    `updated_by` = @repair_duplicate_task_user_id,
    `updated_time` = @repair_duplicate_task_time
WHERE `task_id` = 321
  AND `status` = 'RUNNING'
  AND `deleted_flag` = 0
  AND @repair_duplicate_task_precondition = 1;
SET @soft_deleted_supplement_task_count = ROW_COUNT();

UPDATE `mfg_task_operator`
SET `deleted_flag` = 1
WHERE `id` = 369
  AND `task_id` = 321
  AND `deleted_flag` = 0
  AND @repair_duplicate_task_precondition = 1;
SET @soft_deleted_operator_count = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'MFG_TASK', 156, 'CLOSED', 'RUNNING', 'REPAIR_DUPLICATE_OPERATION_TASK',
       @repair_duplicate_task_user_id, @repair_duplicate_task_time, @repair_duplicate_task_marker,
       0, @repair_duplicate_task_user_id, @repair_duplicate_task_time,
       @repair_duplicate_task_user_id, @repair_duplicate_task_time, 0
WHERE @restored_canonical_task_count = 1
  AND NOT EXISTS (
    SELECT 1 FROM `sys_status_change_log`
    WHERE `biz_type` = 'MFG_TASK' AND `biz_id` = 156
      AND `change_reason` = 'REPAIR_DUPLICATE_OPERATION_TASK'
      AND `remark` = CONVERT(@repair_duplicate_task_marker USING utf8mb4) COLLATE utf8mb4_general_ci
      AND `deleted_flag` = 0
  );
SET @canonical_status_log_count = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'MFG_TASK', 321, 'RUNNING', 'CLOSED', 'SOFT_DELETE_DUPLICATE_OPERATION_TASK',
       @repair_duplicate_task_user_id, @repair_duplicate_task_time, @repair_duplicate_task_marker,
       0, @repair_duplicate_task_user_id, @repair_duplicate_task_time,
       @repair_duplicate_task_user_id, @repair_duplicate_task_time, 0
WHERE @soft_deleted_supplement_task_count = 1
  AND NOT EXISTS (
    SELECT 1 FROM `sys_status_change_log`
    WHERE `biz_type` = 'MFG_TASK' AND `biz_id` = 321
      AND `change_reason` = 'SOFT_DELETE_DUPLICATE_OPERATION_TASK'
      AND `remark` = CONVERT(@repair_duplicate_task_marker USING utf8mb4) COLLATE utf8mb4_general_ci
      AND `deleted_flag` = 0
  );
SET @supplement_status_log_count = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @repair_duplicate_task_user_id, 'REPAIR_DUPLICATE_TASK', 'mfg_task', 156,
       @repair_duplicate_task_time, @repair_duplicate_task_marker,
       0, @repair_duplicate_task_user_id, @repair_duplicate_task_time,
       @repair_duplicate_task_user_id, @repair_duplicate_task_time, 0
WHERE @restored_canonical_task_count = 1
  AND NOT EXISTS (
    SELECT 1 FROM `sys_operation_log`
    WHERE `module_code` = 'mfg_task' AND `biz_id` = 156
      AND `operation_type` = 'REPAIR_DUPLICATE_TASK'
      AND `remark` = CONVERT(@repair_duplicate_task_marker USING utf8mb4) COLLATE utf8mb4_general_ci
      AND `deleted_flag` = 0
  );
SET @canonical_operation_log_count = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @repair_duplicate_task_user_id, 'SOFT_DELETE_DUPLICATE_TASK', 'mfg_task', 321,
       @repair_duplicate_task_time, @repair_duplicate_task_marker,
       0, @repair_duplicate_task_user_id, @repair_duplicate_task_time,
       @repair_duplicate_task_user_id, @repair_duplicate_task_time, 0
WHERE @soft_deleted_supplement_task_count = 1
  AND NOT EXISTS (
    SELECT 1 FROM `sys_operation_log`
    WHERE `module_code` = 'mfg_task' AND `biz_id` = 321
      AND `operation_type` = 'SOFT_DELETE_DUPLICATE_TASK'
      AND `remark` = CONVERT(@repair_duplicate_task_marker USING utf8mb4) COLLATE utf8mb4_general_ci
      AND `deleted_flag` = 0
  );
SET @supplement_operation_log_count = ROW_COUNT();

UPDATE `sys_notification`
SET `status` = 'CANCELLED',
    `version` = `version` + 1,
    `updated_by` = @repair_duplicate_task_user_id,
    `updated_time` = @repair_duplicate_task_time
WHERE `biz_type` = 'MFG_TASK'
  AND `biz_id` = 321
  AND `status` = 'ACTIVE'
  AND `deleted_flag` = 0;
SET @cancelled_supplement_notifications = ROW_COUNT();

UPDATE `sys_event_outbox`
SET `status` = 'CANCELLED',
    `processed_time` = @repair_duplicate_task_time,
    `version` = `version` + 1,
    `updated_by` = @repair_duplicate_task_user_id,
    `updated_time` = @repair_duplicate_task_time
WHERE `aggregate_type` = 'MFG_TASK'
  AND `aggregate_id` = 321
  AND `status` IN ('PENDING', 'FAILED', 'PROCESSING')
  AND `deleted_flag` = 0;
SET @cancelled_supplement_outbox = ROW_COUNT();

SET @repair_duplicate_task_result_valid = (
  SELECT IF(
    (SELECT COUNT(*) FROM `mfg_task`
     WHERE `task_id` = 156 AND `status` = 'RUNNING' AND `deleted_flag` = 0) = 1
    AND
    (SELECT COUNT(*) FROM `mfg_task`
     WHERE `task_id` = 321 AND `status` = 'CLOSED' AND `deleted_flag` = 1) = 1
    AND
    (SELECT COUNT(*) FROM `mfg_task_operator`
     WHERE `id` = 59 AND `task_id` = 156 AND `deleted_flag` = 0) = 1
    AND
    (SELECT COUNT(*) FROM `mfg_task_operator`
     WHERE `id` = 369 AND `task_id` = 321 AND `deleted_flag` = 1) = 1
    AND
    (SELECT COUNT(*) FROM `mfg_task`
     WHERE `work_order_id` = 23 AND `deleted_flag` = 0) = 7
    AND
    (SELECT COUNT(DISTINCT `operation_id`) FROM `mfg_task`
     WHERE `work_order_id` = 23 AND `deleted_flag` = 0) = 7
    AND
    (SELECT COUNT(*) FROM `mfg_work_report`
     WHERE `task_id` = 321 AND `deleted_flag` = 0) = 0
    AND
    (SELECT COUNT(*) FROM `mfg_exception`
     WHERE `task_id` = 321 AND `deleted_flag` = 0) = 0
    AND
    (SELECT COUNT(*) FROM `qms_inspection_order`
     WHERE `task_id` = 321 AND `deleted_flag` = 0) = 0,
    1, 0
  )
);

SET @repair_duplicate_task_final_statement = IF(
  @repair_duplicate_task_commit = 1
  AND @repair_duplicate_task_precondition = 1
  AND @repair_duplicate_task_result_valid = 1,
  'COMMIT', 'ROLLBACK'
);
PREPARE repair_duplicate_task_final_statement FROM @repair_duplicate_task_final_statement;
EXECUTE repair_duplicate_task_final_statement;
DEALLOCATE PREPARE repair_duplicate_task_final_statement;

SELECT IF(@repair_duplicate_task_commit = 1
          AND @repair_duplicate_task_precondition = 1
          AND @repair_duplicate_task_result_valid = 1,
          'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @repair_duplicate_task_precondition AS `precondition_valid`,
       @restored_canonical_task_count AS `restored_canonical_tasks`,
       @soft_deleted_supplement_task_count AS `soft_deleted_supplement_tasks`,
       @soft_deleted_operator_count AS `soft_deleted_operators`,
       @canonical_status_log_count + @supplement_status_log_count AS `status_logs`,
       @canonical_operation_log_count + @supplement_operation_log_count AS `operation_logs`,
       @cancelled_supplement_notifications AS `cancelled_notifications`,
       @cancelled_supplement_outbox AS `cancelled_outbox`,
       @repair_duplicate_task_result_valid AS `result_valid`;
