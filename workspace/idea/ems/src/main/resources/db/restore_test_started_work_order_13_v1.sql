-- Restore the single work order started for UI testing from RUNNING to RELEASED.
-- Default mode is dry-run. Set @restore_test_work_order_commit = 1 before SOURCE to commit.

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @restore_test_work_order_commit = COALESCE(@restore_test_work_order_commit, 0);
SET @restore_test_work_order_time = COALESCE(@restore_test_work_order_time, CURRENT_TIMESTAMP);
SET @restore_test_work_order_marker = 'RESTORE_TEST_STARTED_WORK_ORDER_13_V1';

DROP TEMPORARY TABLE IF EXISTS `tmp_restore_test_work_order`;
CREATE TEMPORARY TABLE `tmp_restore_test_work_order` (
  `work_order_id` BIGINT UNSIGNED NOT NULL,
  `old_status` VARCHAR(32) NOT NULL,
  `old_version` INT NOT NULL,
  PRIMARY KEY (`work_order_id`)
) ENGINE=InnoDB;

DROP TEMPORARY TABLE IF EXISTS `tmp_restore_test_work_order_assert`;
CREATE TEMPORARY TABLE `tmp_restore_test_work_order_assert` (
  `assertion_name` VARCHAR(160) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB;

START TRANSACTION;

INSERT INTO `tmp_restore_test_work_order` (`work_order_id`, `old_status`, `old_version`)
SELECT work_order.`work_order_id`, work_order.`status`, work_order.`version`
FROM `mfg_work_order` work_order
WHERE work_order.`work_order_id` = 13
  AND work_order.`work_order_no` = 'WO-20260715-014-1'
  AND work_order.`status` IN ('RUNNING', 'RELEASED')
  AND work_order.`deleted_flag` = 0
FOR UPDATE;

INSERT INTO `tmp_restore_test_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly one intended work order', IF(COUNT(*) = 1, 1, NULL)
FROM `tmp_restore_test_work_order`;

INSERT INTO `tmp_restore_test_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'exactly seven active tasks and all remain WAIT_START',
       IF(COUNT(*) = 7 AND SUM(task.`status` = 'WAIT_START') = 7, 1, NULL)
FROM `mfg_task` task
JOIN `tmp_restore_test_work_order` target ON target.`work_order_id` = task.`work_order_id`
WHERE task.`deleted_flag` = 0;

INSERT INTO `tmp_restore_test_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all task quantities remain zero',
       IF(COUNT(*) = 7
          AND SUM(task.`completed_quantity` = 0) = 7
          AND SUM(task.`defect_quantity` = 0) = 7, 1, NULL)
FROM `mfg_task` task
JOIN `tmp_restore_test_work_order` target ON target.`work_order_id` = task.`work_order_id`
WHERE task.`deleted_flag` = 0;

INSERT INTO `tmp_restore_test_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'target tasks have no active reports', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_work_report` report
JOIN `mfg_task` task ON task.`task_id` = report.`task_id` AND task.`deleted_flag` = 0
JOIN `tmp_restore_test_work_order` target ON target.`work_order_id` = task.`work_order_id`
WHERE report.`deleted_flag` = 0;

INSERT INTO `tmp_restore_test_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'matching start audit exists', IF(COUNT(*) = 1, 1, NULL)
FROM `sys_status_change_log` status_log
JOIN `tmp_restore_test_work_order` target ON target.`work_order_id` = status_log.`biz_id`
WHERE status_log.`biz_type` = 'MFG_WORK_ORDER'
  AND status_log.`old_status` = 'RELEASED'
  AND status_log.`new_status` = 'RUNNING'
  AND status_log.`change_reason` = 'START_WORK_ORDER'
  AND status_log.`changed_time` = '2026-07-15 20:08:25'
  AND status_log.`deleted_flag` = 0;

SET @restore_test_work_order_user_id = (
  SELECT user.`user_id` FROM `sys_user` user
  WHERE user.`username` = 'admin' AND user.`deleted_flag` = 0
  LIMIT 1
);

INSERT INTO `tmp_restore_test_work_order_assert` (`assertion_name`, `assertion_passed`)
VALUES ('admin audit user exists', IF(@restore_test_work_order_user_id IS NOT NULL, 1, NULL));

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'MFG_WORK_ORDER', target.`work_order_id`, 'RUNNING', 'RELEASED', 'RESTORE_TEST_START',
       @restore_test_work_order_user_id, @restore_test_work_order_time,
       CONCAT(@restore_test_work_order_marker, ': restore UI test work order'), 0,
       @restore_test_work_order_user_id, @restore_test_work_order_time,
       @restore_test_work_order_user_id, @restore_test_work_order_time, 0
FROM `tmp_restore_test_work_order` target
WHERE target.`old_status` = 'RUNNING'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_status_change_log` existing
    WHERE existing.`biz_type` = 'MFG_WORK_ORDER'
      AND existing.`biz_id` = target.`work_order_id`
      AND existing.`remark` LIKE CONCAT(@restore_test_work_order_marker, '%')
      AND existing.`deleted_flag` = 0
  );
SET @restore_test_work_order_status_logs = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @restore_test_work_order_user_id, 'RESTORE_WORK_ORDER', 'mfg_work_order', target.`work_order_id`,
       @restore_test_work_order_time,
       CONCAT(@restore_test_work_order_marker, ': restore RUNNING to RELEASED'), 0,
       @restore_test_work_order_user_id, @restore_test_work_order_time,
       @restore_test_work_order_user_id, @restore_test_work_order_time, 0
FROM `tmp_restore_test_work_order` target
WHERE target.`old_status` = 'RUNNING'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_operation_log` existing
    WHERE existing.`module_code` = 'mfg_work_order'
      AND existing.`biz_id` = target.`work_order_id`
      AND existing.`remark` LIKE CONCAT(@restore_test_work_order_marker, '%')
      AND existing.`deleted_flag` = 0
  );
SET @restore_test_work_order_operation_logs = ROW_COUNT();

UPDATE `mfg_work_order` work_order
JOIN `tmp_restore_test_work_order` target ON target.`work_order_id` = work_order.`work_order_id`
SET work_order.`status` = 'RELEASED',
    work_order.`version` = work_order.`version` + 1,
    work_order.`updated_by` = @restore_test_work_order_user_id,
    work_order.`updated_time` = @restore_test_work_order_time
WHERE target.`old_status` = 'RUNNING'
  AND work_order.`status` = 'RUNNING'
  AND work_order.`version` = target.`old_version`
  AND work_order.`deleted_flag` = 0;
SET @restore_test_work_order_updated = ROW_COUNT();

INSERT INTO `tmp_restore_test_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'work order update count matches locked source status',
       IF((target.`old_status` = 'RUNNING' AND @restore_test_work_order_updated = 1)
          OR (target.`old_status` = 'RELEASED' AND @restore_test_work_order_updated = 0), 1, NULL)
FROM `tmp_restore_test_work_order` target;

SELECT @restore_test_work_order_commit AS `commit_mode`,
       @restore_test_work_order_updated AS `work_orders_restored`,
       @restore_test_work_order_status_logs AS `status_logs_written`,
       @restore_test_work_order_operation_logs AS `operation_logs_written`;

SELECT work_order.`work_order_id`, work_order.`work_order_no`, work_order.`status`,
       work_order.`version`, work_order.`updated_by`, work_order.`updated_time`
FROM `mfg_work_order` work_order
WHERE work_order.`work_order_id` = 13;

SELECT task.`status`, COUNT(*) AS `task_count`
FROM `mfg_task` task
WHERE task.`work_order_id` = 13 AND task.`deleted_flag` = 0
GROUP BY task.`status`
ORDER BY task.`status`;

SET @restore_test_work_order_finish_sql = IF(
  @restore_test_work_order_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE restore_test_work_order_finish FROM @restore_test_work_order_finish_sql;
EXECUTE restore_test_work_order_finish;
DEALLOCATE PREPARE restore_test_work_order_finish;
