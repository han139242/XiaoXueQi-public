-- Soft-delete five generated work orders and their direct production children.
-- Default mode is dry-run. Set @soft_delete_commit = 1 before sourcing to commit.
-- Audit/status logs are intentionally retained. Inventory, quality or trace links block execution.

SET @soft_delete_commit = COALESCE(@soft_delete_commit, 0);
SET @soft_delete_time = COALESCE(@soft_delete_time, CURRENT_TIMESTAMP);
SET @soft_delete_marker = 'SOFT_DELETE_GENERATED_WORK_ORDERS_68_72_V1';
SET @soft_delete_user_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  LIMIT 1
);

DROP TEMPORARY TABLE IF EXISTS `tmp_soft_delete_work_order_target`;
CREATE TEMPORARY TABLE `tmp_soft_delete_work_order_target` (
  `work_order_id` BIGINT UNSIGNED NOT NULL,
  `work_order_no` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`work_order_id`),
  UNIQUE KEY `uk_tmp_soft_delete_work_order_no` (`work_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_soft_delete_work_order_target` (`work_order_id`, `work_order_no`)
VALUES
  (68, 'WO-20260715-037-1'),
  (69, 'WO-20260715-038-1'),
  (70, 'WO-20260715-039-1'),
  (71, 'WO-20260715-041-1'),
  (72, 'WO-20260715-043-1');

DROP TEMPORARY TABLE IF EXISTS `tmp_soft_delete_work_order_assert`;
CREATE TEMPORARY TABLE `tmp_soft_delete_work_order_assert` (
  `assertion_name` VARCHAR(128) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

START TRANSACTION;

SELECT work_order.`work_order_id`, work_order.`work_order_no`, work_order.`status`, work_order.`deleted_flag`
FROM `mfg_work_order` work_order
JOIN `tmp_soft_delete_work_order_target` target
  ON target.`work_order_id` = work_order.`work_order_id`
FOR UPDATE;

INSERT INTO `tmp_soft_delete_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'five exact target work orders exist', IF(COUNT(*) = 5, 1, NULL)
FROM `mfg_work_order` work_order
JOIN `tmp_soft_delete_work_order_target` target
  ON target.`work_order_id` = work_order.`work_order_id`
 AND target.`work_order_no` = work_order.`work_order_no`;

SET @active_target_count = (
  SELECT COUNT(*)
  FROM `mfg_work_order` work_order
  JOIN `tmp_soft_delete_work_order_target` target
    ON target.`work_order_id` = work_order.`work_order_id`
  WHERE work_order.`deleted_flag` = 0
);

INSERT INTO `tmp_soft_delete_work_order_assert` (`assertion_name`, `assertion_passed`)
VALUES ('targets are fully active or already deleted', IF(@active_target_count IN (0, 5), 1, NULL));

INSERT INTO `tmp_soft_delete_work_order_assert` (`assertion_name`, `assertion_passed`)
VALUES ('admin audit user exists', IF(@soft_delete_user_id IS NOT NULL, 1, NULL));

INSERT INTO `tmp_soft_delete_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'no outbound order links', IF(COUNT(*) = 0, 1, NULL)
FROM `wms_outbound_order`
WHERE `work_order_id` IN (68, 69, 70, 71, 72)
  AND `deleted_flag` = 0;

INSERT INTO `tmp_soft_delete_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'no return order links', IF(COUNT(*) = 0, 1, NULL)
FROM `wms_return_order`
WHERE `work_order_id` IN (68, 69, 70, 71, 72)
  AND `deleted_flag` = 0;

INSERT INTO `tmp_soft_delete_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'no quality inspection links', IF(COUNT(*) = 0, 1, NULL)
FROM `qms_inspection_order`
WHERE (`work_order_id` IN (68, 69, 70, 71, 72)
    OR `task_id` IN (SELECT `task_id` FROM `mfg_task` WHERE `work_order_id` IN (68, 69, 70, 71, 72))
    OR `report_id` IN (SELECT `report_id` FROM `mfg_work_report` WHERE `task_id` IN (
      SELECT `task_id` FROM `mfg_task` WHERE `work_order_id` IN (68, 69, 70, 71, 72))))
  AND `deleted_flag` = 0;

INSERT INTO `tmp_soft_delete_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'no product lot links', IF(COUNT(*) = 0, 1, NULL)
FROM `trace_product_lot`
WHERE `work_order_id` IN (68, 69, 70, 71, 72)
  AND `deleted_flag` = 0;

INSERT INTO `tmp_soft_delete_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'no material trace links', IF(COUNT(*) = 0, 1, NULL)
FROM `trace_material_usage`
WHERE (`work_order_id` IN (68, 69, 70, 71, 72)
    OR `report_id` IN (SELECT `report_id` FROM `mfg_work_report` WHERE `task_id` IN (
      SELECT `task_id` FROM `mfg_task` WHERE `work_order_id` IN (68, 69, 70, 71, 72))))
  AND `deleted_flag` = 0;

UPDATE `mfg_report_material`
SET `deleted_flag` = 1,
    `version` = `version` + 1,
    `updated_by` = @soft_delete_user_id,
    `updated_time` = @soft_delete_time
WHERE `report_id` IN (
  SELECT `report_id` FROM `mfg_work_report`
  WHERE `task_id` IN (
    SELECT `task_id` FROM `mfg_task`
    WHERE `work_order_id` IN (68, 69, 70, 71, 72)
  )
)
  AND `deleted_flag` = 0;
SET @deleted_report_materials = ROW_COUNT();

UPDATE `mfg_work_report`
SET `deleted_flag` = 1,
    `version` = `version` + 1,
    `updated_by` = @soft_delete_user_id,
    `updated_time` = @soft_delete_time
WHERE `task_id` IN (
  SELECT `task_id` FROM `mfg_task`
  WHERE `work_order_id` IN (68, 69, 70, 71, 72)
)
  AND `deleted_flag` = 0;
SET @deleted_reports = ROW_COUNT();

UPDATE `mfg_task_operator`
SET `deleted_flag` = 1
WHERE `task_id` IN (
  SELECT `task_id` FROM `mfg_task`
  WHERE `work_order_id` IN (68, 69, 70, 71, 72)
)
  AND `deleted_flag` = 0;
SET @deleted_task_operators = ROW_COUNT();

UPDATE `mfg_exception`
SET `deleted_flag` = 1,
    `version` = `version` + 1,
    `updated_by` = @soft_delete_user_id,
    `updated_time` = @soft_delete_time
WHERE `task_id` IN (
  SELECT `task_id` FROM `mfg_task`
  WHERE `work_order_id` IN (68, 69, 70, 71, 72)
)
  AND `deleted_flag` = 0;
SET @deleted_exceptions = ROW_COUNT();

UPDATE `mfg_task`
SET `deleted_flag` = 1,
    `version` = `version` + 1,
    `updated_by` = @soft_delete_user_id,
    `updated_time` = @soft_delete_time
WHERE `work_order_id` IN (68, 69, 70, 71, 72)
  AND `deleted_flag` = 0;
SET @deleted_tasks = ROW_COUNT();

UPDATE `mfg_work_order_material`
SET `deleted_flag` = 1,
    `version` = `version` + 1,
    `updated_by` = @soft_delete_user_id,
    `updated_time` = @soft_delete_time
WHERE `work_order_id` IN (68, 69, 70, 71, 72)
  AND `deleted_flag` = 0;
SET @deleted_work_order_materials = ROW_COUNT();

UPDATE `mfg_dispatch_order`
SET `deleted_flag` = 1,
    `version` = `version` + 1,
    `updated_by` = @soft_delete_user_id,
    `updated_time` = @soft_delete_time
WHERE `work_order_id` IN (68, 69, 70, 71, 72)
  AND `deleted_flag` = 0;
SET @deleted_dispatches = ROW_COUNT();

UPDATE `mfg_work_order`
SET `deleted_flag` = 1,
    `version` = `version` + 1,
    `updated_by` = @soft_delete_user_id,
    `updated_time` = @soft_delete_time
WHERE `work_order_id` IN (68, 69, 70, 71, 72)
  AND `deleted_flag` = 0;
SET @deleted_work_orders = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT @soft_delete_user_id, 'SOFT_DELETE_TEST_DATA', target.`module_code`, target.`biz_id`,
       @soft_delete_time, @soft_delete_marker, 0,
       @soft_delete_user_id, @soft_delete_time, @soft_delete_user_id, @soft_delete_time, 0
FROM (
  SELECT 'mfg_work_order' AS `module_code`, `work_order_id` AS `biz_id`
  FROM `mfg_work_order` WHERE `work_order_id` IN (68, 69, 70, 71, 72)
  UNION ALL
  SELECT 'mfg_work_order_material', `requirement_id`
  FROM `mfg_work_order_material` WHERE `work_order_id` IN (68, 69, 70, 71, 72)
  UNION ALL
  SELECT 'mfg_dispatch_order', `dispatch_id`
  FROM `mfg_dispatch_order` WHERE `dispatch_id` = 68 AND `work_order_id` = 71
  UNION ALL
  SELECT 'mfg_task', `task_id`
  FROM `mfg_task` WHERE `work_order_id` IN (68, 69, 70, 71, 72)
) target
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_operation_log` existing_log
  WHERE existing_log.`module_code` COLLATE utf8mb4_general_ci = target.`module_code` COLLATE utf8mb4_general_ci
    AND existing_log.`biz_id` = target.`biz_id`
    AND existing_log.`remark` COLLATE utf8mb4_general_ci = @soft_delete_marker COLLATE utf8mb4_general_ci
    AND existing_log.`deleted_flag` = 0
);
SET @inserted_cleanup_logs = ROW_COUNT();

INSERT INTO `tmp_soft_delete_work_order_assert` (`assertion_name`, `assertion_passed`)
VALUES ('cleanup logs are complete or already present', IF(@inserted_cleanup_logs IN (0, 41), 1, NULL));

INSERT INTO `tmp_soft_delete_work_order_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all target work orders and direct children are deleted', IF(
  (SELECT COUNT(*) FROM `mfg_work_order` WHERE `work_order_id` IN (68, 69, 70, 71, 72) AND `deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM `mfg_task` WHERE `work_order_id` IN (68, 69, 70, 71, 72) AND `deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM `mfg_work_order_material` WHERE `work_order_id` IN (68, 69, 70, 71, 72) AND `deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM `mfg_dispatch_order` WHERE `work_order_id` IN (68, 69, 70, 71, 72) AND `deleted_flag` = 0) = 0,
  1, NULL
);

SET @passed_assertion_count = (SELECT COUNT(*) FROM `tmp_soft_delete_work_order_assert`);
SET @soft_delete_final_statement = IF(@soft_delete_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE soft_delete_final_statement FROM @soft_delete_final_statement;
EXECUTE soft_delete_final_statement;
DEALLOCATE PREPARE soft_delete_final_statement;

SELECT IF(@soft_delete_commit = 1, 'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @deleted_work_orders AS `work_orders`,
       @deleted_tasks AS `tasks`,
       @deleted_reports AS `reports`,
       @deleted_report_materials AS `report_materials`,
       @deleted_task_operators AS `task_operators`,
       @deleted_exceptions AS `task_exceptions`,
       @deleted_work_order_materials AS `work_order_materials`,
       @deleted_dispatches AS `dispatches`,
       @inserted_cleanup_logs AS `operation_logs`,
       @passed_assertion_count AS `passed_assertions`;
