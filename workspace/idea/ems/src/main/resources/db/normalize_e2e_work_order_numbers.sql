-- Normalize two E2E work-order numbers to the shared database naming convention.
-- Default mode is dry-run. Set @normalize_commit = 1 before sourcing this file to commit.
-- Business timestamps are intentionally preserved because updated_time is used as a completion-time fallback.

SET @normalize_commit = COALESCE(@normalize_commit, 0);

DROP TEMPORARY TABLE IF EXISTS `tmp_work_order_number_mapping`;
CREATE TEMPORARY TABLE `tmp_work_order_number_mapping` (
  `work_order_id` BIGINT UNSIGNED NOT NULL,
  `old_work_order_no` VARCHAR(64) NOT NULL,
  `new_work_order_no` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`work_order_id`),
  UNIQUE KEY `uk_tmp_new_work_order_no` (`new_work_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_work_order_number_mapping`
  (`work_order_id`, `old_work_order_no`, `new_work_order_no`)
VALUES
  (64, 'E2E-20260714-154114-WO', 'WO-20260714-030-1'),
  (65, 'WO-33-20260715001653005', 'WO-20260715-031-1');

DROP TEMPORARY TABLE IF EXISTS `tmp_work_order_number_assert`;
CREATE TEMPORARY TABLE `tmp_work_order_number_assert` (
  `assertion_name` VARCHAR(128) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

START TRANSACTION;

SELECT work_order.`work_order_id`, work_order.`work_order_no`
FROM `mfg_work_order` work_order
JOIN `tmp_work_order_number_mapping` mapping
  ON mapping.`work_order_id` = work_order.`work_order_id`
FOR UPDATE;

INSERT INTO `tmp_work_order_number_assert` (`assertion_name`, `assertion_passed`)
SELECT 'two active target work orders', IF(COUNT(*) = 2, 1, NULL)
FROM `mfg_work_order` work_order
JOIN `tmp_work_order_number_mapping` mapping
  ON mapping.`work_order_id` = work_order.`work_order_id`
WHERE work_order.`deleted_flag` = 0;

INSERT INTO `tmp_work_order_number_assert` (`assertion_name`, `assertion_passed`)
SELECT 'current numbers are expected old or normalized values', IF(COUNT(*) = 2, 1, NULL)
FROM `mfg_work_order` work_order
JOIN `tmp_work_order_number_mapping` mapping
  ON mapping.`work_order_id` = work_order.`work_order_id`
WHERE work_order.`deleted_flag` = 0
  AND work_order.`work_order_no` IN (mapping.`old_work_order_no`, mapping.`new_work_order_no`);

INSERT INTO `tmp_work_order_number_assert` (`assertion_name`, `assertion_passed`)
SELECT 'normalized numbers do not belong to other rows', IF(COUNT(*) = 0, 1, NULL)
FROM `mfg_work_order` existing_work_order
JOIN `tmp_work_order_number_mapping` mapping
  ON mapping.`new_work_order_no` = existing_work_order.`work_order_no`
WHERE existing_work_order.`work_order_id` <> mapping.`work_order_id`;

SET @expected_update_count = (
  SELECT COUNT(*)
  FROM `mfg_work_order` work_order
  JOIN `tmp_work_order_number_mapping` mapping
    ON mapping.`work_order_id` = work_order.`work_order_id`
  WHERE work_order.`work_order_no` = mapping.`old_work_order_no`
    AND work_order.`deleted_flag` = 0
);

UPDATE `mfg_work_order` work_order
JOIN `tmp_work_order_number_mapping` mapping
  ON mapping.`work_order_id` = work_order.`work_order_id`
SET work_order.`work_order_no` = mapping.`new_work_order_no`
WHERE work_order.`work_order_no` = mapping.`old_work_order_no`
  AND work_order.`deleted_flag` = 0;
SET @updated_work_order_count = ROW_COUNT();

INSERT INTO `tmp_work_order_number_assert` (`assertion_name`, `assertion_passed`)
VALUES ('all expected rows updated', IF(@updated_work_order_count = @expected_update_count, 1, NULL));

INSERT INTO `tmp_work_order_number_assert` (`assertion_name`, `assertion_passed`)
SELECT 'both work orders have normalized numbers', IF(COUNT(*) = 2, 1, NULL)
FROM `mfg_work_order` work_order
JOIN `tmp_work_order_number_mapping` mapping
  ON mapping.`work_order_id` = work_order.`work_order_id`
 AND mapping.`new_work_order_no` = work_order.`work_order_no`
WHERE work_order.`deleted_flag` = 0;

INSERT INTO `tmp_work_order_number_assert` (`assertion_name`, `assertion_passed`)
SELECT 'normalized numbers follow enterprise format', IF(COUNT(*) = 2, 1, NULL)
FROM `mfg_work_order` work_order
JOIN `tmp_work_order_number_mapping` mapping
  ON mapping.`work_order_id` = work_order.`work_order_id`
WHERE work_order.`work_order_no` REGEXP '^WO-[0-9]{8}-[0-9]{3}-[0-9]+$'
  AND work_order.`deleted_flag` = 0;

SET @passed_assertion_count = (SELECT COUNT(*) FROM `tmp_work_order_number_assert`);
SET @normalize_final_statement = IF(@normalize_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE normalize_final_statement FROM @normalize_final_statement;
EXECUTE normalize_final_statement;
DEALLOCATE PREPARE normalize_final_statement;

SELECT IF(@normalize_commit = 1, 'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @updated_work_order_count AS `updated_work_orders`,
       @passed_assertion_count AS `passed_assertions`;
