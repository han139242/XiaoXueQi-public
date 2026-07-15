-- Backfill the shared database with traceable quality work based on existing approved reports.
-- Default mode is dry-run. Set @quality_backfill_commit = 1 before sourcing this file to commit.
-- This script does not change table structures, inventory balances, or material-batch states.
-- On Windows, use the mysql client SOURCE command; a PowerShell string pipeline can corrupt UTF-8 literals.

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @quality_backfill_commit = COALESCE(@quality_backfill_commit, 0);
SET @quality_backfill_marker = 'QMS_SHARED_DB_BACKFILL_V1';
SET @quality_backfill_time = COALESCE(@quality_backfill_time, CURRENT_TIMESTAMP);

DROP TEMPORARY TABLE IF EXISTS `tmp_quality_backfill_source`;
CREATE TEMPORARY TABLE `tmp_quality_backfill_source` (
  `inspection_no` VARCHAR(64) NOT NULL,
  `report_no` VARCHAR(64) NOT NULL,
  `target_status` VARCHAR(32) NOT NULL,
  `target_result` VARCHAR(32) NULL,
  `created_offset_minutes` INT NOT NULL,
  PRIMARY KEY (`inspection_no`),
  UNIQUE KEY `uk_tmp_quality_backfill_report_no` (`report_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_quality_backfill_source`
  (`inspection_no`, `report_no`, `target_status`, `target_result`, `created_offset_minutes`)
VALUES
  ('IPQC-WO-20260703-011-1-2', 'WR-20260703-011-1-060', 'DRAFT', NULL, -5),
  ('IPQC-WO-20260713-013-1-2', 'WR-LIVE-20260713-013-1-060', 'DRAFT', NULL, 0),
  ('IPQC-WO-20260713-024-2-2', 'WR-LIVE-20260713-024-2-060', 'FINISHED', 'FAIL', -40);

DROP TEMPORARY TABLE IF EXISTS `tmp_quality_backfill_assert`;
CREATE TEMPORARY TABLE `tmp_quality_backfill_assert` (
  `assertion_name` VARCHAR(160) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

START TRANSACTION;

SET @quality_user_id = (
  SELECT user.`user_id`
  FROM `sys_user` user
  JOIN `sys_user_role` user_role
    ON user_role.`user_id` = user.`user_id` AND user_role.`deleted_flag` = 0
  JOIN `sys_role` role
    ON role.`role_id` = user_role.`role_id` AND role.`deleted_flag` = 0
  WHERE user.`username` = 'zjy001' AND user.`status` = 'ENABLE' AND user.`deleted_flag` = 0
    AND role.`role_code` = 'QUALITY_ENGINEER'
);
SET @quality_employee_id = (
  SELECT user.`employee_id`
  FROM `sys_user` user
  WHERE user.`user_id` = @quality_user_id AND user.`deleted_flag` = 0
);

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'enabled quality account and employee exist',
       IF(@quality_user_id IS NOT NULL AND @quality_employee_id IS NOT NULL, 1, NULL);

SELECT report.`report_id`, report.`report_no`
FROM `mfg_work_report` report
JOIN `tmp_quality_backfill_source` source ON source.`report_no` = report.`report_no`
WHERE report.`deleted_flag` = 0
FOR UPDATE;

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'three approved speed-test reports match released schemes', IF(COUNT(*) = 3, 1, NULL)
FROM `tmp_quality_backfill_source` source
JOIN `mfg_work_report` report
  ON report.`report_no` = source.`report_no`
 AND report.`audit_status` = 'APPROVED' AND report.`deleted_flag` = 0
JOIN `mfg_task` task
  ON task.`task_id` = report.`task_id` AND task.`status` = 'FINISHED' AND task.`deleted_flag` = 0
JOIN `process_operation` operation
  ON operation.`operation_id` = task.`operation_id`
 AND operation.`operation_code` = 'OP-SPEED-TEST' AND operation.`deleted_flag` = 0
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = task.`work_order_id` AND work_order.`deleted_flag` = 0
JOIN `qms_inspection_scheme` scheme
  ON scheme.`product_id` = work_order.`product_id`
 AND scheme.`operation_id` = task.`operation_id`
 AND scheme.`inspection_type` = 'PROCESS'
 AND scheme.`status` = 'RELEASED' AND scheme.`deleted_flag` = 0;

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'source reports have no unrelated active inspections', IF(COUNT(*) = 0, 1, NULL)
FROM `tmp_quality_backfill_source` source
JOIN `mfg_work_report` report
  ON report.`report_no` = source.`report_no` AND report.`deleted_flag` = 0
JOIN `qms_inspection_order` inspection_order
  ON inspection_order.`report_id` = report.`report_id`
 AND inspection_order.`inspection_no` <> source.`inspection_no`
 AND inspection_order.`deleted_flag` = 0;

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'failed source work order has exactly one active product lot', IF(COUNT(*) = 1, 1, NULL)
FROM `tmp_quality_backfill_source` source
JOIN `mfg_work_report` report
  ON report.`report_no` = source.`report_no` AND report.`deleted_flag` = 0
JOIN `mfg_task` task
  ON task.`task_id` = report.`task_id` AND task.`deleted_flag` = 0
JOIN `trace_product_lot` product_lot
  ON product_lot.`work_order_id` = task.`work_order_id` AND product_lot.`deleted_flag` = 0
WHERE source.`target_result` = 'FAIL';

SET @inspection_count_before = (SELECT COUNT(*) FROM `qms_inspection_order` WHERE `deleted_flag` = 0);
SET @result_count_before = (SELECT COUNT(*) FROM `qms_inspection_result` WHERE `deleted_flag` = 0);
SET @ncr_count_before = (SELECT COUNT(*) FROM `qms_nonconforming_order` WHERE `deleted_flag` = 0);
SET @ncr_detail_count_before = (SELECT COUNT(*) FROM `qms_nonconforming_detail` WHERE `deleted_flag` = 0);
SET @status_log_count_before = (SELECT COUNT(*) FROM `sys_status_change_log` WHERE `deleted_flag` = 0);
SET @operation_log_count_before = (SELECT COUNT(*) FROM `sys_operation_log` WHERE `deleted_flag` = 0);
SET @approval_count_before = (SELECT COUNT(*) FROM `sys_approval_record` WHERE `deleted_flag` = 0);
SET @batch_digest_before = (
  SELECT MD5(CONCAT(COUNT(*), '|', COALESCE(SUM(`version`), 0), '|',
                    COALESCE(DATE_FORMAT(MAX(`updated_time`), '%Y-%m-%d %H:%i:%s'), 'NULL')))
  FROM `wms_material_batch` WHERE `deleted_flag` = 0
);
SET @inventory_digest_before = (
  SELECT MD5(CONCAT(COUNT(*), '|', COALESCE(SUM(`version`), 0), '|',
                    COALESCE(SUM(`available_quantity`), 0), '|', COALESCE(SUM(`locked_quantity`), 0), '|',
                    COALESCE(SUM(`inspection_quantity`), 0)))
  FROM `wms_inventory` WHERE `deleted_flag` = 0
);

INSERT INTO `qms_inspection_order`
  (`inspection_no`, `inspection_type`, `scheme_id`, `work_order_id`, `task_id`, `report_id`, `batch_id`,
   `inspector_id`, `inspection_result`, `status`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT source.`inspection_no`, 'PROCESS', scheme.`scheme_id`, work_order.`work_order_id`, task.`task_id`,
       report.`report_id`, NULL, @quality_employee_id, source.`target_result`, source.`target_status`,
       CONCAT(@quality_backfill_marker, ': 已审核速度检测报工质检补录'), 0,
       @quality_user_id, TIMESTAMPADD(MINUTE, source.`created_offset_minutes`, @quality_backfill_time),
       IF(source.`target_status` = 'FINISHED', @quality_user_id, NULL),
       IF(source.`target_status` = 'FINISHED', TIMESTAMPADD(MINUTE, -35, @quality_backfill_time), NULL), 0
FROM `tmp_quality_backfill_source` source
JOIN `mfg_work_report` report
  ON report.`report_no` = source.`report_no` AND report.`audit_status` = 'APPROVED' AND report.`deleted_flag` = 0
JOIN `mfg_task` task
  ON task.`task_id` = report.`task_id` AND task.`deleted_flag` = 0
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = task.`work_order_id` AND work_order.`deleted_flag` = 0
JOIN `qms_inspection_scheme` scheme
  ON scheme.`product_id` = work_order.`product_id`
 AND scheme.`operation_id` = task.`operation_id`
 AND scheme.`inspection_type` = 'PROCESS'
 AND scheme.`status` = 'RELEASED' AND scheme.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `qms_inspection_order` existing
  WHERE existing.`inspection_no` = source.`inspection_no` AND existing.`deleted_flag` = 0
);
SET @inserted_primary_inspections = ROW_COUNT();

INSERT INTO `qms_inspection_order`
  (`inspection_no`, `inspection_type`, `scheme_id`, `work_order_id`, `task_id`, `report_id`, `batch_id`,
   `inspector_id`, `inspection_result`, `status`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'IPQC-RC-WO-20260713-024-2-2', failed.`inspection_type`, failed.`scheme_id`, failed.`work_order_id`,
       failed.`task_id`, NULL, NULL, @quality_employee_id, 'PASS', 'FINISHED',
       CONCAT(@quality_backfill_marker, ': 飞行偏差返工后复检合格'), 0,
       @quality_user_id, TIMESTAMPADD(MINUTE, -20, @quality_backfill_time),
       @quality_user_id, TIMESTAMPADD(MINUTE, -15, @quality_backfill_time), 0
FROM `qms_inspection_order` failed
WHERE failed.`inspection_no` = 'IPQC-WO-20260713-024-2-2'
  AND failed.`inspection_result` = 'FAIL' AND failed.`status` = 'FINISHED' AND failed.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `qms_inspection_order` existing
    WHERE existing.`inspection_no` = 'IPQC-RC-WO-20260713-024-2-2' AND existing.`deleted_flag` = 0
  );
SET @inserted_recheck_inspections = ROW_COUNT();
SET @inserted_inspections = @inserted_primary_inspections + @inserted_recheck_inspections;

INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `defect_description`, `remark`, `version`,
   `created_by`, `created_time`, `deleted_flag`)
SELECT inspection_order.`inspection_id`, item.`item_id`,
       CASE
         WHEN inspection_order.`inspection_no` = 'IPQC-WO-20260713-024-2-2' AND item.`item_code` = 'QI-FINISHED-WEIGHT' THEN '5.01'
         WHEN inspection_order.`inspection_no` = 'IPQC-WO-20260713-024-2-2' AND item.`item_code` = 'QI-SPEED-GRADE' THEN '77'
         WHEN inspection_order.`inspection_no` = 'IPQC-WO-20260713-024-2-2' AND item.`item_code` = 'QI-FLIGHT-STABILITY' THEN '345'
         WHEN inspection_order.`inspection_no` = 'IPQC-RC-WO-20260713-024-2-2' AND item.`item_code` = 'QI-FINISHED-WEIGHT' THEN '5.00'
         WHEN inspection_order.`inspection_no` = 'IPQC-RC-WO-20260713-024-2-2' AND item.`item_code` = 'QI-SPEED-GRADE' THEN '77'
         WHEN inspection_order.`inspection_no` = 'IPQC-RC-WO-20260713-024-2-2' AND item.`item_code` = 'QI-FLIGHT-STABILITY' THEN '270'
       END,
       IF(inspection_order.`inspection_no` = 'IPQC-WO-20260713-024-2-2'
          AND item.`item_code` = 'QI-FLIGHT-STABILITY', 'FAIL', 'PASS'),
       IF(inspection_order.`inspection_no` = 'IPQC-WO-20260713-024-2-2'
          AND item.`item_code` = 'QI-FLIGHT-STABILITY', '抽样落点最大径向偏差345mm，超过300mm上限', NULL),
       CONCAT(@quality_backfill_marker, ': 速度与飞行检测明细'), 0,
       @quality_user_id, inspection_order.`updated_time`, 0
FROM `qms_inspection_order` inspection_order
JOIN `qms_inspection_scheme_item` scheme_item
  ON scheme_item.`scheme_id` = inspection_order.`scheme_id` AND scheme_item.`deleted_flag` = 0
JOIN `qms_inspection_item` item
  ON item.`item_id` = scheme_item.`item_id` AND item.`deleted_flag` = 0
WHERE inspection_order.`inspection_no` IN
      ('IPQC-WO-20260713-024-2-2', 'IPQC-RC-WO-20260713-024-2-2')
  AND inspection_order.`status` = 'FINISHED' AND inspection_order.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `qms_inspection_result` existing
    WHERE existing.`inspection_id` = inspection_order.`inspection_id`
      AND existing.`item_id` = item.`item_id` AND existing.`deleted_flag` = 0
  );
SET @inserted_results = ROW_COUNT();

INSERT INTO `qms_nonconforming_order`
  (`ncr_no`, `inspection_id`, `product_lot_id`, `defect_reason`, `disposition_type`, `approval_status`, `status`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'NCR-20260715-001', failed.`inspection_id`, product_lot.`product_lot_id`,
       '速度检测抽样发现飞行落点偏差超限，隔离4筒复核', 'REWORK', 'APPROVED', 'CLOSED',
       CONCAT(@quality_backfill_marker, ': 校准设备并返工，复检合格后关闭'), 0,
       @quality_user_id, TIMESTAMPADD(MINUTE, -30, @quality_backfill_time),
       @quality_user_id, TIMESTAMPADD(MINUTE, -10, @quality_backfill_time), 0
FROM `qms_inspection_order` failed
JOIN `trace_product_lot` product_lot
  ON product_lot.`work_order_id` = failed.`work_order_id` AND product_lot.`deleted_flag` = 0
WHERE failed.`inspection_no` = 'IPQC-WO-20260713-024-2-2'
  AND failed.`inspection_result` = 'FAIL' AND failed.`status` = 'FINISHED' AND failed.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `qms_nonconforming_order` existing
    WHERE existing.`ncr_no` = 'NCR-20260715-001' AND existing.`deleted_flag` = 0
  );
SET @inserted_ncr_orders = ROW_COUNT();

INSERT INTO `qms_nonconforming_detail`
  (`ncr_id`, `barcode_id`, `inspection_result_id`, `defect_quantity`, `final_disposition`, `recheck_result`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT ncr.`ncr_id`, NULL, result_record.`result_id`, 4.0000, 'REWORK', 'PASS',
       CONCAT(@quality_backfill_marker, ': 隔离4筒返工，复检记录IPQC-RC-WO-20260713-024-2-2合格'), 0,
       @quality_user_id, TIMESTAMPADD(MINUTE, -25, @quality_backfill_time),
       @quality_user_id, TIMESTAMPADD(MINUTE, -10, @quality_backfill_time), 0
FROM `qms_nonconforming_order` ncr
JOIN `qms_inspection_result` result_record
  ON result_record.`inspection_id` = ncr.`inspection_id`
 AND result_record.`result` = 'FAIL' AND result_record.`deleted_flag` = 0
JOIN `qms_inspection_item` item
  ON item.`item_id` = result_record.`item_id`
 AND item.`item_code` = 'QI-FLIGHT-STABILITY' AND item.`deleted_flag` = 0
WHERE ncr.`ncr_no` = 'NCR-20260715-001' AND ncr.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `qms_nonconforming_detail` existing
    WHERE existing.`ncr_id` = ncr.`ncr_id`
      AND existing.`inspection_result_id` = result_record.`result_id`
      AND existing.`deleted_flag` = 0
  );
SET @inserted_ncr_details = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `version`, `created_by`, `created_time`, `deleted_flag`)
SELECT 'QMS_INSPECTION_ORDER', inspection_order.`inspection_id`, 'DRAFT', 'FINISHED', 'SUBMIT_INSPECTION',
       @quality_user_id, inspection_order.`updated_time`, CONCAT(@quality_backfill_marker, ': 检验记录提交'),
       0, @quality_user_id, inspection_order.`updated_time`, 0
FROM `qms_inspection_order` inspection_order
WHERE inspection_order.`inspection_no` IN
      ('IPQC-WO-20260713-024-2-2', 'IPQC-RC-WO-20260713-024-2-2')
  AND inspection_order.`status` = 'FINISHED' AND inspection_order.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `sys_status_change_log` existing
    WHERE existing.`biz_type` = 'QMS_INSPECTION_ORDER'
      AND existing.`biz_id` = inspection_order.`inspection_id`
      AND existing.`change_reason` = 'SUBMIT_INSPECTION'
      AND existing.`remark` LIKE CONCAT(@quality_backfill_marker, '%')
      AND existing.`deleted_flag` = 0
  );
SET @inserted_inspection_status_logs = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `version`, `created_by`, `created_time`, `deleted_flag`)
SELECT 'QMS_NONCONFORMING_ORDER', ncr.`ncr_id`, 'SUBMITTED', 'CLOSED', 'CLOSE_NONCONFORMING',
       @quality_user_id, ncr.`updated_time`, CONCAT(@quality_backfill_marker, ': 返工复检合格后关闭'),
       0, @quality_user_id, ncr.`updated_time`, 0
FROM `qms_nonconforming_order` ncr
WHERE ncr.`ncr_no` = 'NCR-20260715-001' AND ncr.`status` = 'CLOSED' AND ncr.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `sys_status_change_log` existing
    WHERE existing.`biz_type` = 'QMS_NONCONFORMING_ORDER' AND existing.`biz_id` = ncr.`ncr_id`
      AND existing.`change_reason` = 'CLOSE_NONCONFORMING'
      AND existing.`remark` LIKE CONCAT(@quality_backfill_marker, '%')
      AND existing.`deleted_flag` = 0
  );
SET @inserted_ncr_status_logs = ROW_COUNT();
SET @inserted_status_logs = @inserted_inspection_status_logs + @inserted_ncr_status_logs;

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `deleted_flag`)
SELECT @quality_user_id, operation_seed.`operation_type`, operation_seed.`module_code`,
       operation_seed.`biz_id`, operation_seed.`operation_time`,
       CONCAT(@quality_backfill_marker, ': ', operation_seed.`operation_note`),
       0, @quality_user_id, operation_seed.`operation_time`, 0
FROM (
  SELECT 'CREATE_INSPECTION' AS `operation_type`, 'qms_inspection_order' AS `module_code`,
         inspection_order.`inspection_id` AS `biz_id`, inspection_order.`created_time` AS `operation_time`,
         '创建检验单' AS `operation_note`
  FROM `qms_inspection_order` inspection_order
  WHERE inspection_order.`inspection_no` IN
        ('IPQC-WO-20260703-011-1-2', 'IPQC-WO-20260713-013-1-2',
         'IPQC-WO-20260713-024-2-2', 'IPQC-RC-WO-20260713-024-2-2')
    AND inspection_order.`deleted_flag` = 0
  UNION ALL
  SELECT 'SUBMIT_INSPECTION', 'qms_inspection_order', inspection_order.`inspection_id`,
         inspection_order.`updated_time`, '提交检验记录'
  FROM `qms_inspection_order` inspection_order
  WHERE inspection_order.`inspection_no` IN
        ('IPQC-WO-20260713-024-2-2', 'IPQC-RC-WO-20260713-024-2-2')
    AND inspection_order.`status` = 'FINISHED' AND inspection_order.`deleted_flag` = 0
  UNION ALL
  SELECT 'CREATE_NONCONFORMING', 'qms_nonconforming_order', ncr.`ncr_id`, ncr.`created_time`, '创建不合格处理单'
  FROM `qms_nonconforming_order` ncr
  WHERE ncr.`ncr_no` = 'NCR-20260715-001' AND ncr.`deleted_flag` = 0
  UNION ALL
  SELECT 'APPROVE_NONCONFORMING', 'qms_nonconforming_order', ncr.`ncr_id`,
         TIMESTAMPADD(MINUTE, -27, @quality_backfill_time), '审批返工方案'
  FROM `qms_nonconforming_order` ncr
  WHERE ncr.`ncr_no` = 'NCR-20260715-001' AND ncr.`deleted_flag` = 0
  UNION ALL
  SELECT 'CLOSE_NONCONFORMING', 'qms_nonconforming_order', ncr.`ncr_id`, ncr.`updated_time`, '返工复检合格后关闭'
  FROM `qms_nonconforming_order` ncr
  WHERE ncr.`ncr_no` = 'NCR-20260715-001' AND ncr.`deleted_flag` = 0
) operation_seed
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_operation_log` existing
  WHERE existing.`operation_type` = operation_seed.`operation_type`
    AND existing.`module_code` = operation_seed.`module_code`
    AND existing.`biz_id` = operation_seed.`biz_id`
    AND existing.`remark` LIKE CONCAT(@quality_backfill_marker, '%')
    AND existing.`deleted_flag` = 0
);
SET @inserted_operation_logs = ROW_COUNT();

INSERT INTO `sys_approval_record`
  (`biz_type`, `biz_id`, `approval_node`, `approver_id`, `approval_result`, `approval_comment`, `approval_time`,
   `remark`, `version`, `created_by`, `created_time`, `deleted_flag`)
SELECT 'QMS_NONCONFORMING_ORDER', ncr.`ncr_id`, 'DISPOSITION_APPROVAL', @quality_user_id, 'APPROVED',
       '同意设备校准和受控返工，复检合格后关闭', TIMESTAMPADD(MINUTE, -27, @quality_backfill_time),
       CONCAT(@quality_backfill_marker, ': 不合格处置审批'), 0, @quality_user_id,
       TIMESTAMPADD(MINUTE, -27, @quality_backfill_time), 0
FROM `qms_nonconforming_order` ncr
WHERE ncr.`ncr_no` = 'NCR-20260715-001' AND ncr.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `sys_approval_record` existing
    WHERE existing.`biz_type` = 'QMS_NONCONFORMING_ORDER' AND existing.`biz_id` = ncr.`ncr_id`
      AND existing.`approval_node` = 'DISPOSITION_APPROVAL'
      AND existing.`remark` LIKE CONCAT(@quality_backfill_marker, '%')
      AND existing.`deleted_flag` = 0
  );
SET @inserted_approval_records = ROW_COUNT();

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'four target inspection orders are active', IF(COUNT(*) = 4, 1, NULL)
FROM `qms_inspection_order`
WHERE `inspection_no` IN
      ('IPQC-WO-20260703-011-1-2', 'IPQC-WO-20260713-013-1-2',
       'IPQC-WO-20260713-024-2-2', 'IPQC-RC-WO-20260713-024-2-2')
  AND `deleted_flag` = 0;

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'two pending inspections have no result or detail rows', IF(COUNT(*) = 2, 1, NULL)
FROM `qms_inspection_order` inspection_order
WHERE inspection_order.`inspection_no` IN
      ('IPQC-WO-20260703-011-1-2', 'IPQC-WO-20260713-013-1-2')
  AND inspection_order.`status` = 'DRAFT' AND inspection_order.`inspection_result` IS NULL
  AND inspection_order.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `qms_inspection_result` result_record
    WHERE result_record.`inspection_id` = inspection_order.`inspection_id` AND result_record.`deleted_flag` = 0
  );

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'failed inspection has two pass and one fail results',
       IF(COUNT(*) = 3 AND SUM(result_record.`result` = 'PASS') = 2
          AND SUM(result_record.`result` = 'FAIL') = 1, 1, NULL)
FROM `qms_inspection_order` inspection_order
JOIN `qms_inspection_result` result_record
  ON result_record.`inspection_id` = inspection_order.`inspection_id` AND result_record.`deleted_flag` = 0
WHERE inspection_order.`inspection_no` = 'IPQC-WO-20260713-024-2-2'
  AND inspection_order.`status` = 'FINISHED' AND inspection_order.`inspection_result` = 'FAIL'
  AND inspection_order.`deleted_flag` = 0;

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'recheck inspection has three pass results',
       IF(COUNT(*) = 3 AND SUM(result_record.`result` = 'PASS') = 3, 1, NULL)
FROM `qms_inspection_order` inspection_order
JOIN `qms_inspection_result` result_record
  ON result_record.`inspection_id` = inspection_order.`inspection_id` AND result_record.`deleted_flag` = 0
WHERE inspection_order.`inspection_no` = 'IPQC-RC-WO-20260713-024-2-2'
  AND inspection_order.`status` = 'FINISHED' AND inspection_order.`inspection_result` = 'PASS'
  AND inspection_order.`deleted_flag` = 0;

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'nonconforming order and detail close the failed-result loop', IF(COUNT(*) = 1, 1, NULL)
FROM `qms_nonconforming_order` ncr
JOIN `qms_inspection_order` inspection_order
  ON inspection_order.`inspection_id` = ncr.`inspection_id`
 AND inspection_order.`inspection_result` = 'FAIL' AND inspection_order.`status` = 'FINISHED'
 AND inspection_order.`deleted_flag` = 0
JOIN `qms_nonconforming_detail` detail
  ON detail.`ncr_id` = ncr.`ncr_id` AND detail.`defect_quantity` = 4.0000
 AND detail.`final_disposition` = 'REWORK' AND detail.`recheck_result` = 'PASS'
 AND detail.`deleted_flag` = 0
JOIN `qms_inspection_result` result_record
  ON result_record.`result_id` = detail.`inspection_result_id`
 AND result_record.`inspection_id` = inspection_order.`inspection_id`
 AND result_record.`result` = 'FAIL' AND result_record.`deleted_flag` = 0
WHERE ncr.`ncr_no` = 'NCR-20260715-001'
  AND ncr.`approval_status` = 'APPROVED' AND ncr.`status` = 'CLOSED'
  AND ncr.`disposition_type` = 'REWORK' AND ncr.`deleted_flag` = 0;

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'three primary inspections preserve report-task-order-scheme links', IF(COUNT(*) = 3, 1, NULL)
FROM `tmp_quality_backfill_source` source
JOIN `qms_inspection_order` inspection_order
  ON inspection_order.`inspection_no` = source.`inspection_no` AND inspection_order.`deleted_flag` = 0
JOIN `mfg_work_report` report
  ON report.`report_id` = inspection_order.`report_id`
 AND report.`report_no` = source.`report_no` AND report.`deleted_flag` = 0
JOIN `mfg_task` task
  ON task.`task_id` = inspection_order.`task_id`
 AND task.`task_id` = report.`task_id` AND task.`deleted_flag` = 0
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = inspection_order.`work_order_id`
 AND work_order.`work_order_id` = task.`work_order_id` AND work_order.`deleted_flag` = 0
JOIN `qms_inspection_scheme` scheme
  ON scheme.`scheme_id` = inspection_order.`scheme_id`
 AND scheme.`product_id` = work_order.`product_id`
 AND scheme.`operation_id` = task.`operation_id`
 AND scheme.`inspection_type` = inspection_order.`inspection_type`
 AND scheme.`deleted_flag` = 0;

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'target rows have complete audit records', IF(
  (SELECT COUNT(*) FROM `sys_status_change_log`
   WHERE `remark` LIKE CONCAT(@quality_backfill_marker, '%') AND `deleted_flag` = 0) = 3
  AND (SELECT COUNT(*) FROM `sys_operation_log`
       WHERE `remark` LIKE CONCAT(@quality_backfill_marker, '%') AND `deleted_flag` = 0) = 9
  AND (SELECT COUNT(*) FROM `sys_approval_record`
       WHERE `remark` LIKE CONCAT(@quality_backfill_marker, '%') AND `deleted_flag` = 0) = 1,
  1, NULL);

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'active row counts changed only by this backfill', IF(
  (SELECT COUNT(*) FROM `qms_inspection_order` WHERE `deleted_flag` = 0)
       = @inspection_count_before + @inserted_inspections
  AND (SELECT COUNT(*) FROM `qms_inspection_result` WHERE `deleted_flag` = 0)
       = @result_count_before + @inserted_results
  AND (SELECT COUNT(*) FROM `qms_nonconforming_order` WHERE `deleted_flag` = 0)
       = @ncr_count_before + @inserted_ncr_orders
  AND (SELECT COUNT(*) FROM `qms_nonconforming_detail` WHERE `deleted_flag` = 0)
       = @ncr_detail_count_before + @inserted_ncr_details
  AND (SELECT COUNT(*) FROM `sys_status_change_log` WHERE `deleted_flag` = 0)
       = @status_log_count_before + @inserted_status_logs
  AND (SELECT COUNT(*) FROM `sys_operation_log` WHERE `deleted_flag` = 0)
       = @operation_log_count_before + @inserted_operation_logs
  AND (SELECT COUNT(*) FROM `sys_approval_record` WHERE `deleted_flag` = 0)
       = @approval_count_before + @inserted_approval_records,
  1, NULL);

INSERT INTO `tmp_quality_backfill_assert` (`assertion_name`, `assertion_passed`)
SELECT 'batch and inventory data remain unchanged', IF(
  @batch_digest_before = (
    SELECT MD5(CONCAT(COUNT(*), '|', COALESCE(SUM(`version`), 0), '|',
                      COALESCE(DATE_FORMAT(MAX(`updated_time`), '%Y-%m-%d %H:%i:%s'), 'NULL')))
    FROM `wms_material_batch` WHERE `deleted_flag` = 0
  )
  AND @inventory_digest_before = (
    SELECT MD5(CONCAT(COUNT(*), '|', COALESCE(SUM(`version`), 0), '|',
                      COALESCE(SUM(`available_quantity`), 0), '|', COALESCE(SUM(`locked_quantity`), 0), '|',
                      COALESCE(SUM(`inspection_quantity`), 0)))
    FROM `wms_inventory` WHERE `deleted_flag` = 0
  ), 1, NULL);

SET @passed_assertion_count = (SELECT COUNT(*) FROM `tmp_quality_backfill_assert`);
SET @quality_backfill_final_statement = IF(@quality_backfill_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE quality_backfill_final_statement FROM @quality_backfill_final_statement;
EXECUTE quality_backfill_final_statement;
DEALLOCATE PREPARE quality_backfill_final_statement;

SELECT IF(@quality_backfill_commit = 1, 'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @inserted_inspections AS `inserted_inspections`,
       @inserted_results AS `inserted_results`,
       @inserted_ncr_orders AS `inserted_ncr_orders`,
       @inserted_ncr_details AS `inserted_ncr_details`,
       @inserted_status_logs AS `inserted_status_logs`,
       @inserted_operation_logs AS `inserted_operation_logs`,
       @inserted_approval_records AS `inserted_approval_records`,
       @passed_assertion_count AS `passed_assertions`;
