-- Soft-delete five work reports created by client timeout retries.
-- Default mode is dry-run. Set @retry_report_cleanup_commit = 1 before sourcing to commit.
-- Report 315 is the retained first submission. Processed outbox rows remain as audit history.

SET @retry_report_cleanup_commit = COALESCE(@retry_report_cleanup_commit, 0);
SET @retry_report_cleanup_time = COALESCE(@retry_report_cleanup_time, CURRENT_TIMESTAMP);
SET @retry_report_cleanup_marker = 'SOFT_DELETE_TIMEOUT_RETRY_WORK_REPORTS_316_320_V1';
SET @retry_report_cleanup_user_id = (
  SELECT `user_id`
  FROM `sys_user`
  WHERE `username` = 'admin'
    AND `deleted_flag` = 0
  LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS `tmp_retry_report_cleanup_target`;
CREATE TEMPORARY TABLE `tmp_retry_report_cleanup_target` (
  `report_id` BIGINT UNSIGNED NOT NULL,
  `report_no` VARCHAR(64) NOT NULL,
  `task_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uk_tmp_retry_report_no` (`report_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_retry_report_cleanup_target` (`report_id`, `report_no`, `task_id`)
VALUES
  (316, 'WR-20260715-014-1-010-3', 91),
  (317, 'WR-20260715-014-1-010-4', 91),
  (318, 'WR-20260715-014-1-010-5', 91),
  (319, 'WR-20260714-003-1-050-4', 157),
  (320, 'WR-20260714-003-1-050-5', 157);

DROP TEMPORARY TABLE IF EXISTS `tmp_retry_report_cleanup_assert`;
CREATE TEMPORARY TABLE `tmp_retry_report_cleanup_assert` (
  `assertion_name` VARCHAR(128) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

START TRANSACTION;

SELECT report.`report_id`, report.`report_no`, report.`task_id`, report.`audit_status`, report.`deleted_flag`
FROM `mfg_work_report` report
JOIN `tmp_retry_report_cleanup_target` target
  ON target.`report_id` = report.`report_id`
FOR UPDATE;

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
SELECT 'five exact retry reports exist', IF(COUNT(*) = 5, 1, NULL)
FROM `mfg_work_report` report
JOIN `tmp_retry_report_cleanup_target` target
  ON target.`report_id` = report.`report_id`
 AND target.`report_no` = report.`report_no`
 AND target.`task_id` = report.`task_id`;

SET @active_retry_report_count = (
  SELECT COUNT(*)
  FROM `mfg_work_report` report
  JOIN `tmp_retry_report_cleanup_target` target
    ON target.`report_id` = report.`report_id`
  WHERE report.`deleted_flag` = 0
);

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'retry reports are fully active or already deleted',
  IF(@active_retry_report_count IN (0, 5), 1, NULL)
);

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
SELECT 'active retry reports remain submitted', IF(COUNT(*) = @active_retry_report_count, 1, NULL)
FROM `mfg_work_report` report
JOIN `tmp_retry_report_cleanup_target` target
  ON target.`report_id` = report.`report_id`
WHERE report.`deleted_flag` = 0
  AND report.`audit_status` = 'SUBMITTED';

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
SELECT 'retained first submission is active', IF(COUNT(*) = 1, 1, NULL)
FROM `mfg_work_report`
WHERE `report_id` = 315
  AND `report_no` = 'WR-20260715-014-1-010-2'
  AND `task_id` = 91
  AND `deleted_flag` = 0;

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
VALUES ('admin audit user exists', IF(@retry_report_cleanup_user_id IS NOT NULL, 1, NULL));

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
SELECT 'retry reports have no material links', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_report_material`
WHERE `report_id` IN (316, 317, 318, 319, 320)
  AND `deleted_flag` = 0;

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
SELECT 'retry reports have no quality links', IF(COUNT(*) = 0, 1, NULL)
FROM `qms_inspection_order`
WHERE `report_id` IN (316, 317, 318, 319, 320)
  AND `deleted_flag` = 0;

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
SELECT 'retry reports have no trace links', IF(COUNT(*) = 0, 1, NULL)
FROM `trace_material_usage`
WHERE `report_id` IN (316, 317, 318, 319, 320)
  AND `deleted_flag` = 0;

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
SELECT 'five retry notifications exist', IF(COUNT(*) = 5, 1, NULL)
FROM `sys_notification`
WHERE `biz_type` = 'MFG_WORK_REPORT'
  AND `biz_id` IN (316, 317, 318, 319, 320)
  AND `event_type` = 'MFG_WORK_REPORT_SUBMITTED'
  AND `deleted_flag` = 0;

UPDATE `mfg_report_material`
SET `deleted_flag` = 1,
    `version` = `version` + 1,
    `updated_by` = @retry_report_cleanup_user_id,
    `updated_time` = @retry_report_cleanup_time
WHERE `report_id` IN (316, 317, 318, 319, 320)
  AND `deleted_flag` = 0;
SET @deleted_retry_report_materials = ROW_COUNT();

UPDATE `mfg_work_report` report
JOIN `tmp_retry_report_cleanup_target` target
  ON target.`report_id` = report.`report_id`
SET report.`deleted_flag` = 1,
    report.`version` = report.`version` + 1,
    report.`updated_by` = @retry_report_cleanup_user_id,
    report.`updated_time` = @retry_report_cleanup_time
WHERE report.`deleted_flag` = 0;
SET @deleted_retry_reports = ROW_COUNT();

UPDATE `sys_notification`
SET `status` = 'CANCELLED',
    `version` = `version` + 1,
    `updated_by` = @retry_report_cleanup_user_id,
    `updated_time` = @retry_report_cleanup_time
WHERE `biz_type` = 'MFG_WORK_REPORT'
  AND `biz_id` IN (316, 317, 318, 319, 320)
  AND `event_type` = 'MFG_WORK_REPORT_SUBMITTED'
  AND `status` = 'ACTIVE'
  AND `deleted_flag` = 0;
SET @cancelled_retry_notifications = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @retry_report_cleanup_user_id, 'SOFT_DELETE_TIMEOUT_RETRY', 'mfg_work_report',
       target.`report_id`, @retry_report_cleanup_time, @retry_report_cleanup_marker,
       0, @retry_report_cleanup_user_id, @retry_report_cleanup_time,
       @retry_report_cleanup_user_id, @retry_report_cleanup_time, 0
FROM `tmp_retry_report_cleanup_target` target
WHERE @active_retry_report_count = 5
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_operation_log` existing_log
    WHERE existing_log.`module_code` = 'mfg_work_report'
      AND existing_log.`biz_id` = target.`report_id`
      AND existing_log.`operation_type` = 'SOFT_DELETE_TIMEOUT_RETRY'
      AND existing_log.`remark` = @retry_report_cleanup_marker
      AND existing_log.`deleted_flag` = 0
  );
SET @inserted_retry_cleanup_logs = ROW_COUNT();

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'all active retry reports were deleted',
  IF(@deleted_retry_reports = @active_retry_report_count, 1, NULL)
);

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'retry notifications were cancelled or already cancelled',
  IF(@cancelled_retry_notifications IN (0, 5), 1, NULL)
);

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'cleanup logs match first-run or no-op mode',
  IF(@inserted_retry_cleanup_logs = @active_retry_report_count, 1, NULL)
);

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
SELECT 'retry reports are no longer active', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_work_report`
WHERE `report_id` IN (316, 317, 318, 319, 320)
  AND `deleted_flag` = 0;

INSERT INTO `tmp_retry_report_cleanup_assert` (`assertion_name`, `assertion_passed`)
SELECT 'retained first submission remains active after cleanup', IF(COUNT(*) = 1, 1, NULL)
FROM `mfg_work_report`
WHERE `report_id` = 315
  AND `deleted_flag` = 0;

SET @passed_retry_report_cleanup_assertions = (
  SELECT COUNT(*)
  FROM `tmp_retry_report_cleanup_assert`
);

SET @retry_report_cleanup_final_statement = IF(
  @retry_report_cleanup_commit = 1,
  'COMMIT',
  'ROLLBACK'
);
PREPARE retry_report_cleanup_final_statement FROM @retry_report_cleanup_final_statement;
EXECUTE retry_report_cleanup_final_statement;
DEALLOCATE PREPARE retry_report_cleanup_final_statement;

SELECT IF(
         @retry_report_cleanup_commit = 1,
         'COMMITTED',
         'DRY_RUN_ROLLED_BACK'
       ) AS `execution_mode`,
       @deleted_retry_reports AS `deleted_reports`,
       @deleted_retry_report_materials AS `deleted_report_materials`,
       @cancelled_retry_notifications AS `cancelled_notifications`,
       @inserted_retry_cleanup_logs AS `operation_logs`,
       @passed_retry_report_cleanup_assertions AS `passed_assertions`;
