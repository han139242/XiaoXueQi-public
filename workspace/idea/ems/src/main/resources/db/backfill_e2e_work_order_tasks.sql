-- Backfill missing route tasks for shared-db work orders 64 and 65.
-- Default mode is dry-run. Set @backfill_commit = 1 before sourcing this file to commit.

SET @backfill_commit = COALESCE(@backfill_commit, 0);
SET @backfill_marker = 'SHARED_TASK_BACKFILL_20260715';

DROP TEMPORARY TABLE IF EXISTS `tmp_backfill_target`;
CREATE TEMPORARY TABLE `tmp_backfill_target` (
  `work_order_id` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`work_order_id`)
);
INSERT INTO `tmp_backfill_target` (`work_order_id`) VALUES (64), (65);

DROP TEMPORARY TABLE IF EXISTS `tmp_backfill_assert`;
CREATE TEMPORARY TABLE `tmp_backfill_assert` (
  `assertion_name` VARCHAR(128) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
);

DROP TEMPORARY TABLE IF EXISTS `tmp_backfill_route`;
CREATE TEMPORARY TABLE `tmp_backfill_route` (
  `work_order_id` BIGINT UNSIGNED NOT NULL,
  `dispatch_id` BIGINT UNSIGNED NOT NULL,
  `team_id` BIGINT UNSIGNED NOT NULL,
  `operation_id` BIGINT UNSIGNED NOT NULL,
  `station_id` BIGINT UNSIGNED NOT NULL,
  `sequence_no` INT NOT NULL,
  `planned_quantity` DECIMAL(18,4) NOT NULL,
  `created_by` BIGINT UNSIGNED NULL,
  `created_time` DATETIME NOT NULL,
  `updated_by` BIGINT UNSIGNED NULL,
  `updated_time` DATETIME NOT NULL,
  PRIMARY KEY (`work_order_id`, `operation_id`),
  UNIQUE KEY `uk_tmp_backfill_route_sequence` (`work_order_id`, `sequence_no`)
);

DROP TEMPORARY TABLE IF EXISTS `tmp_backfill_team_member_rank`;
CREATE TEMPORARY TABLE `tmp_backfill_team_member_rank` (
  `team_id` BIGINT UNSIGNED NOT NULL,
  `employee_id` BIGINT UNSIGNED NOT NULL,
  `member_rank` BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (`team_id`, `member_rank`)
);

START TRANSACTION;

SELECT work_order.`work_order_id`
FROM `mfg_work_order` work_order
JOIN `tmp_backfill_target` target ON target.`work_order_id` = work_order.`work_order_id`
FOR UPDATE;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'two finished target work orders',
       IF(COUNT(*) = 2
          AND SUM(work_order.`status` = 'FINISHED') = 2
          AND SUM(work_order.`planned_quantity` = work_order.`completed_quantity`) = 2,
          1, NULL)
FROM `mfg_work_order` work_order
JOIN `tmp_backfill_target` target ON target.`work_order_id` = work_order.`work_order_id`
WHERE work_order.`deleted_flag` = 0;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'one active dispatch per target', IF(COUNT(*) = 2, 1, NULL)
FROM (
  SELECT dispatch_order.`work_order_id`
  FROM `mfg_dispatch_order` dispatch_order
  JOIN `tmp_backfill_target` target ON target.`work_order_id` = dispatch_order.`work_order_id`
  WHERE dispatch_order.`deleted_flag` = 0
  GROUP BY dispatch_order.`work_order_id`
  HAVING COUNT(*) = 1
) valid_dispatch;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'seven unique route steps per target', IF(COUNT(*) = 2, 1, NULL)
FROM (
  SELECT work_order.`work_order_id`
  FROM `mfg_work_order` work_order
  JOIN `tmp_backfill_target` target ON target.`work_order_id` = work_order.`work_order_id`
  JOIN `process_route_item` route_item
    ON route_item.`route_id` = work_order.`route_id` AND route_item.`deleted_flag` = 0
  GROUP BY work_order.`work_order_id`
  HAVING COUNT(*) = 7
     AND COUNT(DISTINCT route_item.`operation_id`) = 7
     AND COUNT(DISTINCT route_item.`sequence_no`) = 7
) valid_route;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'one station per target route step', IF(COUNT(*) = 14, 1, NULL)
FROM (
  SELECT work_order.`work_order_id`, route_item.`route_item_id`
  FROM `mfg_work_order` work_order
  JOIN `tmp_backfill_target` target ON target.`work_order_id` = work_order.`work_order_id`
  JOIN `mfg_production_plan` plan
    ON plan.`plan_id` = work_order.`plan_id` AND plan.`deleted_flag` = 0
  JOIN `process_route_item` route_item
    ON route_item.`route_id` = work_order.`route_id` AND route_item.`deleted_flag` = 0
  JOIN `factory_station_operation` station_operation
    ON station_operation.`operation_id` = route_item.`operation_id`
   AND station_operation.`deleted_flag` = 0
  JOIN `factory_station` station
    ON station.`station_id` = station_operation.`station_id`
   AND station.`line_id` = plan.`line_id`
   AND station.`deleted_flag` = 0
  GROUP BY work_order.`work_order_id`, route_item.`route_item_id`
  HAVING COUNT(DISTINCT station.`station_id`) = 1
) valid_station;

INSERT INTO `tmp_backfill_route`
  (`work_order_id`, `dispatch_id`, `team_id`, `operation_id`, `station_id`, `sequence_no`,
   `planned_quantity`, `created_by`, `created_time`, `updated_by`, `updated_time`)
SELECT work_order.`work_order_id`, dispatch_order.`dispatch_id`, dispatch_order.`team_id`,
       route_item.`operation_id`, station.`station_id`, route_item.`sequence_no`,
       work_order.`planned_quantity`,
       COALESCE(anchor_task.`created_by`, work_order.`created_by`),
       COALESCE(anchor_task.`created_time`, work_order.`created_time`, NOW()),
       COALESCE(anchor_task.`updated_by`, work_order.`updated_by`),
       COALESCE(anchor_task.`updated_time`, work_order.`updated_time`, NOW())
FROM `mfg_work_order` work_order
JOIN `tmp_backfill_target` target ON target.`work_order_id` = work_order.`work_order_id`
JOIN `mfg_production_plan` plan
  ON plan.`plan_id` = work_order.`plan_id` AND plan.`deleted_flag` = 0
JOIN `mfg_dispatch_order` dispatch_order
  ON dispatch_order.`work_order_id` = work_order.`work_order_id`
 AND dispatch_order.`deleted_flag` = 0
JOIN `process_route_item` route_item
  ON route_item.`route_id` = work_order.`route_id` AND route_item.`deleted_flag` = 0
JOIN `factory_station_operation` station_operation
  ON station_operation.`operation_id` = route_item.`operation_id`
 AND station_operation.`deleted_flag` = 0
JOIN `factory_station` station
  ON station.`station_id` = station_operation.`station_id`
 AND station.`line_id` = plan.`line_id`
 AND station.`deleted_flag` = 0
LEFT JOIN (
  SELECT task.`work_order_id`, MIN(task.`created_by`) AS `created_by`,
         MIN(task.`created_time`) AS `created_time`, MIN(task.`updated_by`) AS `updated_by`,
         MAX(task.`updated_time`) AS `updated_time`
  FROM `mfg_task` task
  WHERE task.`work_order_id` IN (64, 65) AND task.`deleted_flag` = 0
  GROUP BY task.`work_order_id`
) anchor_task ON anchor_task.`work_order_id` = work_order.`work_order_id`
WHERE work_order.`deleted_flag` = 0;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'four enabled team members cover route tasks', IF(COUNT(*) = 14, 1, NULL)
FROM `tmp_backfill_route` backfill_route
JOIN (
  SELECT member.`team_id`, member.`employee_id`,
         ROW_NUMBER() OVER (PARTITION BY member.`team_id` ORDER BY member.`id`) AS `member_rank`
  FROM `org_team_member` member
  WHERE member.`deleted_flag` = 0 AND member.`status` = 'ENABLE'
) ranked_member
  ON ranked_member.`team_id` = backfill_route.`team_id`
 AND ranked_member.`member_rank` = MOD(backfill_route.`operation_id` - 1, 4) + 1;

INSERT INTO `tmp_backfill_team_member_rank` (`team_id`, `employee_id`, `member_rank`)
SELECT member.`team_id`, member.`employee_id`,
       ROW_NUMBER() OVER (PARTITION BY member.`team_id` ORDER BY member.`id`) AS `member_rank`
FROM `org_team_member` member
WHERE member.`deleted_flag` = 0 AND member.`status` = 'ENABLE';

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'existing tasks are unique route operations', IF(COUNT(*) = 0, 1, NULL)
FROM (
  SELECT task.`work_order_id`, task.`operation_id`
  FROM `mfg_task` task
  JOIN `tmp_backfill_target` target ON target.`work_order_id` = task.`work_order_id`
  WHERE task.`deleted_flag` = 0
  GROUP BY task.`work_order_id`, task.`operation_id`
  HAVING COUNT(*) > 1
) duplicate_task;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'existing tasks belong to selected routes', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_task` task
JOIN `tmp_backfill_target` target ON target.`work_order_id` = task.`work_order_id`
LEFT JOIN `tmp_backfill_route` backfill_route
  ON backfill_route.`work_order_id` = task.`work_order_id`
 AND backfill_route.`operation_id` = task.`operation_id`
WHERE task.`deleted_flag` = 0 AND backfill_route.`operation_id` IS NULL;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'generated task numbers are available', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_task` task
JOIN `tmp_backfill_route` backfill_route
  ON task.`task_no` = CONCAT('TASK-BF-', backfill_route.`work_order_id`, '-', LPAD(backfill_route.`sequence_no`, 3, '0'))
WHERE task.`deleted_flag` <> 0
   OR task.`work_order_id` <> backfill_route.`work_order_id`
   OR task.`operation_id` <> backfill_route.`operation_id`;

SET @task_count_before = (SELECT COUNT(*) FROM `mfg_task` WHERE `deleted_flag` = 0);
SET @missing_task_count = (
  SELECT COUNT(*)
  FROM `tmp_backfill_route` backfill_route
  WHERE NOT EXISTS (
    SELECT 1 FROM `mfg_task` task
    WHERE task.`work_order_id` = backfill_route.`work_order_id`
      AND task.`operation_id` = backfill_route.`operation_id`
      AND task.`deleted_flag` = 0
  )
);

INSERT INTO `mfg_task`
  (`task_no`, `dispatch_id`, `work_order_id`, `operation_id`, `station_id`, `planned_quantity`,
   `completed_quantity`, `defect_quantity`, `status`, `remark`, `version`, `created_by`,
   `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT CONCAT('TASK-BF-', backfill_route.`work_order_id`, '-', LPAD(backfill_route.`sequence_no`, 3, '0')),
       backfill_route.`dispatch_id`, backfill_route.`work_order_id`, backfill_route.`operation_id`,
       backfill_route.`station_id`, backfill_route.`planned_quantity`, backfill_route.`planned_quantity`,
       0, 'FINISHED', CONCAT(@backfill_marker, ': route task backfill'), 0,
       backfill_route.`created_by`, backfill_route.`created_time`, backfill_route.`updated_by`,
       backfill_route.`updated_time`, 0
FROM `tmp_backfill_route` backfill_route
WHERE NOT EXISTS (
  SELECT 1 FROM `mfg_task` task
  WHERE task.`work_order_id` = backfill_route.`work_order_id`
    AND task.`operation_id` = backfill_route.`operation_id`
    AND task.`deleted_flag` = 0
);
SET @inserted_task_count = ROW_COUNT();

SET @operator_count_before = (SELECT COUNT(*) FROM `mfg_task_operator` WHERE `deleted_flag` = 0);
SET @missing_operator_count = (
  SELECT COUNT(*)
  FROM `mfg_task` task
  JOIN `tmp_backfill_route` backfill_route
    ON backfill_route.`work_order_id` = task.`work_order_id`
   AND backfill_route.`operation_id` = task.`operation_id`
  WHERE task.`task_no` = CONCAT('TASK-BF-', backfill_route.`work_order_id`, '-', LPAD(backfill_route.`sequence_no`, 3, '0'))
    AND task.`deleted_flag` = 0
    AND NOT EXISTS (
      SELECT 1 FROM `mfg_task_operator` task_operator
      WHERE task_operator.`task_id` = task.`task_id`
        AND task_operator.`role_in_task` = 'PRIMARY'
        AND task_operator.`deleted_flag` = 0
    )
);

INSERT INTO `mfg_task_operator`
  (`task_id`, `employee_id`, `role_in_task`, `assigned_time`, `created_by`, `created_time`, `deleted_flag`)
SELECT task.`task_id`, ranked_member.`employee_id`, 'PRIMARY', task.`created_time`,
       task.`created_by`, task.`created_time`, 0
FROM `mfg_task` task
JOIN `tmp_backfill_route` backfill_route
  ON backfill_route.`work_order_id` = task.`work_order_id`
 AND backfill_route.`operation_id` = task.`operation_id`
JOIN `tmp_backfill_team_member_rank` ranked_member
  ON ranked_member.`team_id` = backfill_route.`team_id`
 AND ranked_member.`member_rank` = MOD(backfill_route.`operation_id` - 1, 4) + 1
WHERE task.`task_no` = CONCAT('TASK-BF-', backfill_route.`work_order_id`, '-', LPAD(backfill_route.`sequence_no`, 3, '0'))
  AND task.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `mfg_task_operator` task_operator
    WHERE task_operator.`task_id` = task.`task_id`
      AND task_operator.`role_in_task` = 'PRIMARY'
      AND task_operator.`deleted_flag` = 0
  );
SET @inserted_operator_count = ROW_COUNT();

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
VALUES ('all missing tasks inserted', IF(@inserted_task_count = @missing_task_count, 1, NULL));

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
VALUES ('all missing primary operators inserted', IF(@inserted_operator_count = @missing_operator_count, 1, NULL));

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'task total changed only by backfill',
       IF(COUNT(*) = @task_count_before + @inserted_task_count, 1, NULL)
FROM `mfg_task`
WHERE `deleted_flag` = 0;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'operator total changed only by backfill',
       IF(COUNT(*) = @operator_count_before + @inserted_operator_count, 1, NULL)
FROM `mfg_task_operator`
WHERE `deleted_flag` = 0;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'seven tasks cover each target route', IF(COUNT(*) = 2, 1, NULL)
FROM (
  SELECT backfill_route.`work_order_id`
  FROM `tmp_backfill_route` backfill_route
  JOIN `mfg_task` task
    ON task.`work_order_id` = backfill_route.`work_order_id`
   AND task.`operation_id` = backfill_route.`operation_id`
   AND task.`deleted_flag` = 0
  GROUP BY backfill_route.`work_order_id`
  HAVING COUNT(*) = 7 AND COUNT(DISTINCT task.`operation_id`) = 7
) complete_work_order;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'backfilled tasks are finished with full quantity', IF(COUNT(*) = 12, 1, NULL)
FROM `mfg_task` task
JOIN `tmp_backfill_route` backfill_route
  ON backfill_route.`work_order_id` = task.`work_order_id`
 AND backfill_route.`operation_id` = task.`operation_id`
WHERE task.`task_no` = CONCAT('TASK-BF-', backfill_route.`work_order_id`, '-', LPAD(backfill_route.`sequence_no`, 3, '0'))
  AND task.`status` = 'FINISHED'
  AND task.`planned_quantity` = backfill_route.`planned_quantity`
  AND task.`completed_quantity` = backfill_route.`planned_quantity`
  AND task.`defect_quantity` = 0
  AND task.`deleted_flag` = 0;

INSERT INTO `tmp_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'one primary operator per backfilled task', IF(COUNT(*) = 12, 1, NULL)
FROM (
  SELECT task.`task_id`
  FROM `mfg_task` task
  JOIN `tmp_backfill_route` backfill_route
    ON backfill_route.`work_order_id` = task.`work_order_id`
   AND backfill_route.`operation_id` = task.`operation_id`
  JOIN `mfg_task_operator` task_operator
    ON task_operator.`task_id` = task.`task_id`
   AND task_operator.`role_in_task` = 'PRIMARY'
   AND task_operator.`deleted_flag` = 0
  WHERE task.`task_no` = CONCAT('TASK-BF-', backfill_route.`work_order_id`, '-', LPAD(backfill_route.`sequence_no`, 3, '0'))
    AND task.`deleted_flag` = 0
  GROUP BY task.`task_id`
  HAVING COUNT(*) = 1
) valid_operator;

SET @passed_assertion_count = (SELECT COUNT(*) FROM `tmp_backfill_assert`);
SET @backfill_final_statement = IF(@backfill_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE backfill_final_statement FROM @backfill_final_statement;
EXECUTE backfill_final_statement;
DEALLOCATE PREPARE backfill_final_statement;

SELECT IF(@backfill_commit = 1, 'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @inserted_task_count AS `inserted_tasks`,
       @inserted_operator_count AS `inserted_primary_operators`,
       @passed_assertion_count AS `passed_assertions`;
