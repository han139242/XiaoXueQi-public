-- Normalize the active work-order/task/report chain and backfill missing reports.
-- Default mode is dry-run. Set @mfg_chain_commit = 1 before sourcing to commit.
-- Only FINISHED tasks without an active report are backfilled.

SET @mfg_chain_commit = COALESCE(@mfg_chain_commit, 0);
SET @mfg_chain_time = COALESCE(@mfg_chain_time, CURRENT_TIMESTAMP);
SET @mfg_chain_marker = 'NORMALIZE_MFG_TASK_REPORT_CHAIN_V1';
SET @mfg_chain_audit_user_id = (
  SELECT `user_id`
  FROM `sys_user`
  WHERE `username` = 'admin'
    AND `deleted_flag` = 0
  LIMIT 1
);
SET @mfg_chain_operator_id = 20;

DROP TEMPORARY TABLE IF EXISTS `tmp_mfg_chain_assert`;
CREATE TEMPORARY TABLE `tmp_mfg_chain_assert` (
  `assertion_name` VARCHAR(128) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TEMPORARY TABLE IF EXISTS `tmp_mfg_task_number_mapping`;
CREATE TEMPORARY TABLE `tmp_mfg_task_number_mapping` (
  `task_id` BIGINT UNSIGNED NOT NULL,
  `work_order_id` BIGINT UNSIGNED NOT NULL,
  `sequence_no` INT NOT NULL,
  `old_task_no` VARCHAR(64) NOT NULL,
  `new_task_no` VARCHAR(64) NOT NULL,
  `needs_update` TINYINT NOT NULL,
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_tmp_mfg_task_new_no` (`new_task_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TEMPORARY TABLE IF EXISTS `tmp_mfg_report_number_mapping`;
CREATE TEMPORARY TABLE `tmp_mfg_report_number_mapping` (
  `report_id` BIGINT UNSIGNED NOT NULL,
  `task_id` BIGINT UNSIGNED NOT NULL,
  `old_report_no` VARCHAR(64) NOT NULL,
  `new_report_no` VARCHAR(64) NOT NULL,
  `needs_update` TINYINT NOT NULL,
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uk_tmp_mfg_report_new_no` (`new_report_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TEMPORARY TABLE IF EXISTS `tmp_mfg_missing_report`;
CREATE TEMPORARY TABLE `tmp_mfg_missing_report` (
  `task_id` BIGINT UNSIGNED NOT NULL,
  `report_no` VARCHAR(64) NOT NULL,
  `station_id` BIGINT UNSIGNED NOT NULL,
  `qualified_quantity` DECIMAL(18,4) NOT NULL,
  `defect_quantity` DECIMAL(18,4) NOT NULL,
  `actual_work_hours` DECIMAL(18,4) NOT NULL,
  `report_time` DATETIME NOT NULL,
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_tmp_mfg_missing_report_no` (`report_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DROP TEMPORARY TABLE IF EXISTS `tmp_mfg_inserted_report`;
CREATE TEMPORARY TABLE `tmp_mfg_inserted_report` (
  `report_id` BIGINT UNSIGNED NOT NULL,
  `task_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uk_tmp_mfg_inserted_report_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

START TRANSACTION;

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'shared database has 34 active work orders', IF(COUNT(*) = 34, 1, NULL)
FROM `mfg_work_order`
WHERE `deleted_flag` = 0;

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'every active work order has seven active tasks', IF(COUNT(*) = 34, 1, NULL)
FROM (
  SELECT work_order.`work_order_id`
  FROM `mfg_work_order` work_order
  LEFT JOIN `mfg_task` task
    ON task.`work_order_id` = work_order.`work_order_id`
   AND task.`deleted_flag` = 0
  WHERE work_order.`deleted_flag` = 0
  GROUP BY work_order.`work_order_id`
  HAVING COUNT(task.`task_id`) = 7
) seven_task_work_order;

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'active work order numbers are normalized', IF(COUNT(*) = 34, 1, NULL)
FROM `mfg_work_order`
WHERE `deleted_flag` = 0
  AND `work_order_no` REGEXP '^WO-[0-9]{8}-[0-9]{3}-[0-9]+$';

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'admin audit user exists',
  IF(@mfg_chain_audit_user_id IS NOT NULL, 1, NULL)
);

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'backfill operator exists', IF(COUNT(*) = 1, 1, NULL)
FROM `org_employee`
WHERE `employee_id` = @mfg_chain_operator_id
  AND `deleted_flag` = 0;

INSERT INTO `tmp_mfg_task_number_mapping`
  (`task_id`, `work_order_id`, `sequence_no`, `old_task_no`, `new_task_no`, `needs_update`)
SELECT task.`task_id`, task.`work_order_id`, route_item.`sequence_no`, task.`task_no`,
       CONCAT(
         'TASK-', SUBSTRING(work_order.`work_order_no`, 4), '-',
         LPAD(route_item.`sequence_no`, 3, '0')
       ),
       IF(
         task.`task_no` = CONCAT(
           'TASK-', SUBSTRING(work_order.`work_order_no`, 4), '-',
           LPAD(route_item.`sequence_no`, 3, '0')
         ),
         0, 1
       )
FROM `mfg_task` task
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = task.`work_order_id`
 AND work_order.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id`
 AND route_item.`operation_id` = task.`operation_id`
 AND route_item.`deleted_flag` = 0
WHERE task.`deleted_flag` = 0;

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'every active task maps to one route item', IF(
  COUNT(*) = (
    SELECT COUNT(*)
    FROM `mfg_task` task
    JOIN `mfg_work_order` work_order
      ON work_order.`work_order_id` = task.`work_order_id`
     AND work_order.`deleted_flag` = 0
    WHERE task.`deleted_flag` = 0
  ),
  1, NULL
)
FROM `tmp_mfg_task_number_mapping`;

SET @expected_task_renames = (
  SELECT COUNT(*)
  FROM `tmp_mfg_task_number_mapping`
  WHERE `needs_update` = 1
);

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'task rename count is expected first-run or no-op value',
  IF(@expected_task_renames IN (0, 14), 1, NULL)
);

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'task target numbers do not belong to other rows', IF(COUNT(*) = 0, 1, NULL)
FROM `tmp_mfg_task_number_mapping` mapping
JOIN `mfg_task` existing_task
  ON existing_task.`task_no` = mapping.`new_task_no`
 AND existing_task.`task_id` <> mapping.`task_id`;

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'temporary task numbers do not collide', IF(COUNT(*) = 0, 1, NULL)
FROM `tmp_mfg_task_number_mapping` mapping
JOIN `mfg_task` existing_task
  ON existing_task.`task_no` = CONCAT('TMP-MFG-CHAIN-T-', mapping.`task_id`)
WHERE mapping.`needs_update` = 1;

INSERT INTO `tmp_mfg_report_number_mapping`
  (`report_id`, `task_id`, `old_report_no`, `new_report_no`, `needs_update`)
SELECT ranked.`report_id`, ranked.`task_id`, ranked.`report_no`,
       CONCAT(
         'WR-', SUBSTRING(ranked.`new_task_no`, 6),
         IF(ranked.`report_sequence` = 1, '', CONCAT('-', ranked.`report_sequence`))
       ),
       IF(
         ranked.`report_no` = CONCAT(
           'WR-', SUBSTRING(ranked.`new_task_no`, 6),
           IF(ranked.`report_sequence` = 1, '', CONCAT('-', ranked.`report_sequence`))
         ),
         0, 1
       )
FROM (
  SELECT report.`report_id`, report.`report_no`, report.`task_id`, mapping.`new_task_no`,
         ROW_NUMBER() OVER (
           PARTITION BY report.`task_id`
           ORDER BY report.`report_id`
         ) AS `report_sequence`
  FROM `mfg_work_report` report
  JOIN `tmp_mfg_task_number_mapping` mapping
    ON mapping.`task_id` = report.`task_id`
  WHERE report.`deleted_flag` = 0
) ranked;

SET @expected_report_renames = (
  SELECT COUNT(*)
  FROM `tmp_mfg_report_number_mapping`
  WHERE `needs_update` = 1
);

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'report rename count is expected first-run or no-op value',
  IF(@expected_report_renames IN (0, 22), 1, NULL)
);

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'report target numbers do not belong to other rows', IF(COUNT(*) = 0, 1, NULL)
FROM `tmp_mfg_report_number_mapping` mapping
JOIN `mfg_work_report` existing_report
  ON existing_report.`report_no` = mapping.`new_report_no`
 AND existing_report.`report_id` <> mapping.`report_id`;

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'temporary report numbers do not collide', IF(COUNT(*) = 0, 1, NULL)
FROM `tmp_mfg_report_number_mapping` mapping
JOIN `mfg_work_report` existing_report
  ON existing_report.`report_no` = CONCAT('TMP-MFG-CHAIN-R-', mapping.`report_id`)
WHERE mapping.`needs_update` = 1;

INSERT INTO `tmp_mfg_missing_report`
  (`task_id`, `report_no`, `station_id`, `qualified_quantity`, `defect_quantity`,
   `actual_work_hours`, `report_time`)
SELECT task.`task_id`, CONCAT('WR-', SUBSTRING(mapping.`new_task_no`, 6)), task.`station_id`,
       task.`completed_quantity`, task.`defect_quantity`,
       ROUND(COALESCE(route_item.`standard_time`, 0) * task.`completed_quantity` / 60, 4),
       DATE_SUB(
         COALESCE(task.`updated_time`, task.`created_time`, @mfg_chain_time),
         INTERVAL 1 MINUTE
       )
FROM `mfg_task` task
JOIN `tmp_mfg_task_number_mapping` mapping
  ON mapping.`task_id` = task.`task_id`
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = task.`work_order_id`
 AND work_order.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id`
 AND route_item.`operation_id` = task.`operation_id`
 AND route_item.`deleted_flag` = 0
WHERE task.`deleted_flag` = 0
  AND task.`status` = 'FINISHED'
  AND task.`station_id` IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `mfg_work_report` report
    WHERE report.`task_id` = task.`task_id`
      AND report.`deleted_flag` = 0
  );

SET @expected_backfill_reports = (
  SELECT COUNT(*)
  FROM `tmp_mfg_missing_report`
);

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'backfill count is expected first-run or no-op value',
  IF(@expected_backfill_reports IN (0, 12), 1, NULL)
);

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all missing finished tasks are selected for backfill', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_task` task
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = task.`work_order_id`
 AND work_order.`deleted_flag` = 0
WHERE task.`deleted_flag` = 0
  AND task.`status` = 'FINISHED'
  AND NOT EXISTS (
    SELECT 1
    FROM `mfg_work_report` report
    WHERE report.`task_id` = task.`task_id`
      AND report.`deleted_flag` = 0
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `tmp_mfg_missing_report` missing_report
    WHERE missing_report.`task_id` = task.`task_id`
  );

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'backfill report numbers do not collide', IF(COUNT(*) = 0, 1, NULL)
FROM `tmp_mfg_missing_report` missing_report
JOIN `mfg_work_report` existing_report
  ON existing_report.`report_no` = missing_report.`report_no`;

UPDATE `mfg_task` task
JOIN `tmp_mfg_task_number_mapping` mapping
  ON mapping.`task_id` = task.`task_id`
SET task.`task_no` = CONCAT('TMP-MFG-CHAIN-T-', task.`task_id`)
WHERE mapping.`needs_update` = 1;
SET @temporary_task_renames = ROW_COUNT();

UPDATE `mfg_task` task
JOIN `tmp_mfg_task_number_mapping` mapping
  ON mapping.`task_id` = task.`task_id`
SET task.`task_no` = mapping.`new_task_no`,
    task.`version` = task.`version` + 1,
    task.`updated_by` = @mfg_chain_audit_user_id,
    task.`updated_time` = @mfg_chain_time
WHERE mapping.`needs_update` = 1;
SET @renamed_tasks = ROW_COUNT();

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'all expected task numbers were updated',
  IF(
    @temporary_task_renames = @expected_task_renames
    AND @renamed_tasks = @expected_task_renames,
    1, NULL
  )
);

UPDATE `mfg_work_report` report
JOIN `tmp_mfg_report_number_mapping` mapping
  ON mapping.`report_id` = report.`report_id`
SET report.`report_no` = CONCAT('TMP-MFG-CHAIN-R-', report.`report_id`)
WHERE mapping.`needs_update` = 1;
SET @temporary_report_renames = ROW_COUNT();

UPDATE `mfg_work_report` report
JOIN `tmp_mfg_report_number_mapping` mapping
  ON mapping.`report_id` = report.`report_id`
SET report.`report_no` = mapping.`new_report_no`,
    report.`version` = report.`version` + 1,
    report.`updated_by` = @mfg_chain_audit_user_id,
    report.`updated_time` = @mfg_chain_time
WHERE mapping.`needs_update` = 1;
SET @renamed_reports = ROW_COUNT();

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'all expected report numbers were updated',
  IF(
    @temporary_report_renames = @expected_report_renames
    AND @renamed_reports = @expected_report_renames,
    1, NULL
  )
);

INSERT INTO `mfg_work_report`
  (`report_no`, `task_id`, `operator_id`, `station_id`, `qualified_quantity`,
   `defect_quantity`, `actual_work_hours`, `report_time`, `audit_status`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT missing_report.`report_no`, missing_report.`task_id`, @mfg_chain_operator_id,
       missing_report.`station_id`, missing_report.`qualified_quantity`,
       missing_report.`defect_quantity`, missing_report.`actual_work_hours`,
       missing_report.`report_time`, 'APPROVED', @mfg_chain_marker,
       0, @mfg_chain_audit_user_id, missing_report.`report_time`,
       @mfg_chain_audit_user_id, @mfg_chain_time, 0
FROM `tmp_mfg_missing_report` missing_report;
SET @inserted_reports = ROW_COUNT();

INSERT INTO `tmp_mfg_inserted_report` (`report_id`, `task_id`)
SELECT report.`report_id`, report.`task_id`
FROM `mfg_work_report` report
JOIN `tmp_mfg_missing_report` missing_report
  ON missing_report.`task_id` = report.`task_id`
 AND missing_report.`report_no` = report.`report_no`
WHERE report.`deleted_flag` = 0
  AND report.`remark` = @mfg_chain_marker;

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'all expected missing reports were inserted',
  IF(
    @inserted_reports = @expected_backfill_reports
    AND (SELECT COUNT(*) FROM `tmp_mfg_inserted_report`) = @expected_backfill_reports,
    1, NULL
  )
);

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @mfg_chain_audit_user_id, 'NORMALIZE_NUMBER', 'mfg_task', mapping.`task_id`,
       @mfg_chain_time,
       CONCAT(@mfg_chain_marker, ': ', mapping.`old_task_no`, ' -> ', mapping.`new_task_no`),
       0, @mfg_chain_audit_user_id, @mfg_chain_time,
       @mfg_chain_audit_user_id, @mfg_chain_time, 0
FROM `tmp_mfg_task_number_mapping` mapping
WHERE mapping.`needs_update` = 1
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_operation_log` existing_log
    WHERE existing_log.`module_code` = 'mfg_task'
      AND existing_log.`biz_id` = mapping.`task_id`
      AND existing_log.`remark` LIKE CONCAT(@mfg_chain_marker, ':%')
      AND existing_log.`deleted_flag` = 0
  );
SET @inserted_task_logs = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @mfg_chain_audit_user_id, 'NORMALIZE_NUMBER', 'mfg_work_report', mapping.`report_id`,
       @mfg_chain_time,
       CONCAT(@mfg_chain_marker, ': ', mapping.`old_report_no`, ' -> ', mapping.`new_report_no`),
       0, @mfg_chain_audit_user_id, @mfg_chain_time,
       @mfg_chain_audit_user_id, @mfg_chain_time, 0
FROM `tmp_mfg_report_number_mapping` mapping
WHERE mapping.`needs_update` = 1
  AND NOT EXISTS (
    SELECT 1
    FROM `sys_operation_log` existing_log
    WHERE existing_log.`module_code` = 'mfg_work_report'
      AND existing_log.`biz_id` = mapping.`report_id`
      AND existing_log.`remark` LIKE CONCAT(@mfg_chain_marker, ':%')
      AND existing_log.`deleted_flag` = 0
  );
SET @inserted_report_rename_logs = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @mfg_chain_audit_user_id, 'BACKFILL_APPROVED_REPORT', 'mfg_work_report',
       inserted_report.`report_id`, @mfg_chain_time, @mfg_chain_marker,
       0, @mfg_chain_audit_user_id, @mfg_chain_time,
       @mfg_chain_audit_user_id, @mfg_chain_time, 0
FROM `tmp_mfg_inserted_report` inserted_report
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_operation_log` existing_log
  WHERE existing_log.`module_code` = 'mfg_work_report'
    AND existing_log.`biz_id` = inserted_report.`report_id`
    AND existing_log.`operation_type` = 'BACKFILL_APPROVED_REPORT'
    AND existing_log.`remark` = @mfg_chain_marker
    AND existing_log.`deleted_flag` = 0
);
SET @inserted_backfill_operation_logs = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`,
   `changed_time`, `remark`, `version`, `created_by`, `created_time`, `updated_by`,
   `updated_time`, `deleted_flag`)
SELECT 'MFG_WORK_REPORT', inserted_report.`report_id`, NULL, 'APPROVED',
       'Historical finished-task report backfill', @mfg_chain_audit_user_id,
       @mfg_chain_time, @mfg_chain_marker, 0,
       @mfg_chain_audit_user_id, @mfg_chain_time,
       @mfg_chain_audit_user_id, @mfg_chain_time, 0
FROM `tmp_mfg_inserted_report` inserted_report
WHERE NOT EXISTS (
  SELECT 1
  FROM `sys_status_change_log` existing_log
  WHERE existing_log.`biz_type` = 'MFG_WORK_REPORT'
    AND existing_log.`biz_id` = inserted_report.`report_id`
    AND existing_log.`new_status` = 'APPROVED'
    AND existing_log.`remark` = @mfg_chain_marker
    AND existing_log.`deleted_flag` = 0
);
SET @inserted_backfill_status_logs = ROW_COUNT();

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
VALUES (
  'audit logs match changed rows',
  IF(
    @inserted_task_logs = @expected_task_renames
    AND @inserted_report_rename_logs = @expected_report_renames
    AND @inserted_backfill_operation_logs = @expected_backfill_reports
    AND @inserted_backfill_status_logs = @expected_backfill_reports,
    1, NULL
  )
);

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all active task numbers match their work order and route sequence', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_task` task
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = task.`work_order_id`
 AND work_order.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id`
 AND route_item.`operation_id` = task.`operation_id`
 AND route_item.`deleted_flag` = 0
WHERE task.`deleted_flag` = 0
  AND task.`task_no` <> CONCAT(
    'TASK-', SUBSTRING(work_order.`work_order_no`, 4), '-',
    LPAD(route_item.`sequence_no`, 3, '0')
  );

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all active task numbers have the standard format', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_task` task
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = task.`work_order_id`
 AND work_order.`deleted_flag` = 0
WHERE task.`deleted_flag` = 0
  AND task.`task_no` NOT REGEXP '^TASK-[0-9]{8}-[0-9]{3}-[0-9]+-[0-9]{3}$';

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all active report numbers match task and report sequence', IF(COUNT(*) = 0, 1, NULL)
FROM (
  SELECT report.`report_no`,
         CONCAT(
           'WR-', SUBSTRING(task.`task_no`, 6),
           IF(
             ROW_NUMBER() OVER (
               PARTITION BY report.`task_id`
               ORDER BY report.`report_id`
             ) = 1,
             '',
             CONCAT(
               '-',
               ROW_NUMBER() OVER (
                 PARTITION BY report.`task_id`
                 ORDER BY report.`report_id`
               )
             )
           )
         ) AS `expected_report_no`
  FROM `mfg_work_report` report
  JOIN `mfg_task` task
    ON task.`task_id` = report.`task_id`
   AND task.`deleted_flag` = 0
  JOIN `mfg_work_order` work_order
    ON work_order.`work_order_id` = task.`work_order_id`
   AND work_order.`deleted_flag` = 0
  WHERE report.`deleted_flag` = 0
) report_number_check
WHERE report_number_check.`report_no` <> report_number_check.`expected_report_no`;

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all active report numbers have the standard format', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_work_report` report
JOIN `mfg_task` task
  ON task.`task_id` = report.`task_id`
 AND task.`deleted_flag` = 0
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = task.`work_order_id`
 AND work_order.`deleted_flag` = 0
WHERE report.`deleted_flag` = 0
  AND report.`report_no` NOT REGEXP '^WR-[0-9]{8}-[0-9]{3}-[0-9]+-[0-9]{3}(-[2-9][0-9]*)?$';

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'every finished task has an active approved report', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_task` task
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = task.`work_order_id`
 AND work_order.`deleted_flag` = 0
WHERE task.`deleted_flag` = 0
  AND task.`status` = 'FINISHED'
  AND NOT EXISTS (
    SELECT 1
    FROM `mfg_work_report` report
    WHERE report.`task_id` = task.`task_id`
      AND report.`deleted_flag` = 0
      AND report.`audit_status` = 'APPROVED'
  );

INSERT INTO `tmp_mfg_chain_assert` (`assertion_name`, `assertion_passed`)
SELECT 'every finished work order has seven finished tasks', IF(COUNT(*) = 0, 1, NULL)
FROM (
  SELECT work_order.`work_order_id`,
         COUNT(task.`task_id`) AS `task_count`,
         SUM(task.`status` = 'FINISHED') AS `finished_task_count`
  FROM `mfg_work_order` work_order
  LEFT JOIN `mfg_task` task
    ON task.`work_order_id` = work_order.`work_order_id`
   AND task.`deleted_flag` = 0
  WHERE work_order.`deleted_flag` = 0
    AND work_order.`status` = 'FINISHED'
  GROUP BY work_order.`work_order_id`
) finished_work_order
WHERE finished_work_order.`task_count` <> 7
   OR finished_work_order.`finished_task_count` <> 7;

SET @passed_mfg_chain_assertions = (
  SELECT COUNT(*)
  FROM `tmp_mfg_chain_assert`
);

SET @mfg_chain_final_statement = IF(@mfg_chain_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE mfg_chain_final_statement FROM @mfg_chain_final_statement;
EXECUTE mfg_chain_final_statement;
DEALLOCATE PREPARE mfg_chain_final_statement;

SELECT IF(@mfg_chain_commit = 1, 'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @renamed_tasks AS `renamed_tasks`,
       @renamed_reports AS `renamed_existing_reports`,
       @inserted_reports AS `inserted_reports`,
       @inserted_task_logs AS `task_operation_logs`,
       @inserted_report_rename_logs AS `report_rename_operation_logs`,
       @inserted_backfill_operation_logs AS `backfill_operation_logs`,
       @inserted_backfill_status_logs AS `backfill_status_logs`,
       @passed_mfg_chain_assertions AS `passed_assertions`;
