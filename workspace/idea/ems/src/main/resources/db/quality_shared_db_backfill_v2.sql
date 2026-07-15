-- Complete a concurrently submitted failed inspection and repair V1 UTF-8 text.
-- Default mode is dry-run. Set @quality_backfill_v2_commit = 1 before sourcing this file to commit.
-- On Windows, use the mysql client SOURCE command; do not pipe this file through a PowerShell string.

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @quality_backfill_v2_commit = COALESCE(@quality_backfill_v2_commit, 0);
SET @quality_backfill_v1_marker = 'QMS_SHARED_DB_BACKFILL_V1';
SET @quality_backfill_v2_marker = 'QMS_SHARED_DB_BACKFILL_V2';
SET @quality_backfill_v2_time = COALESCE(@quality_backfill_v2_time, CURRENT_TIMESTAMP);

DROP TEMPORARY TABLE IF EXISTS `tmp_quality_backfill_v2_assert`;
CREATE TEMPORARY TABLE `tmp_quality_backfill_v2_assert` (
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
  SELECT user.`employee_id` FROM `sys_user` user
  WHERE user.`user_id` = @quality_user_id AND user.`deleted_flag` = 0
);
SET @failed_inspection_id = (
  SELECT inspection_order.`inspection_id`
  FROM `qms_inspection_order` inspection_order
  WHERE inspection_order.`inspection_no` = 'IPQC-WO-20260703-011-1-2'
    AND inspection_order.`deleted_flag` = 0
);

SELECT inspection_order.`inspection_id`, inspection_order.`inspection_no`
FROM `qms_inspection_order` inspection_order
WHERE inspection_order.`inspection_id` = @failed_inspection_id
FOR UPDATE;

INSERT INTO `tmp_quality_backfill_v2_assert` (`assertion_name`, `assertion_passed`)
SELECT 'concurrent failed inspection keeps valid source links', IF(COUNT(*) = 1, 1, NULL)
FROM `qms_inspection_order` inspection_order
JOIN `mfg_work_report` report
  ON report.`report_id` = inspection_order.`report_id`
 AND report.`audit_status` = 'APPROVED' AND report.`deleted_flag` = 0
JOIN `mfg_task` task
  ON task.`task_id` = inspection_order.`task_id`
 AND task.`task_id` = report.`task_id` AND task.`status` = 'FINISHED' AND task.`deleted_flag` = 0
JOIN `process_operation` operation
  ON operation.`operation_id` = task.`operation_id`
 AND operation.`operation_code` = 'OP-SPEED-TEST' AND operation.`deleted_flag` = 0
JOIN `mfg_work_order` work_order
  ON work_order.`work_order_id` = inspection_order.`work_order_id`
 AND work_order.`work_order_id` = task.`work_order_id` AND work_order.`deleted_flag` = 0
JOIN `qms_inspection_scheme` scheme
  ON scheme.`scheme_id` = inspection_order.`scheme_id`
 AND scheme.`product_id` = work_order.`product_id`
 AND scheme.`operation_id` = task.`operation_id`
 AND scheme.`inspection_type` = inspection_order.`inspection_type`
 AND scheme.`status` = 'RELEASED' AND scheme.`deleted_flag` = 0
WHERE inspection_order.`inspection_id` = @failed_inspection_id
  AND inspection_order.`status` = 'FINISHED' AND inspection_order.`inspection_result` = 'FAIL'
  AND inspection_order.`deleted_flag` = 0;

INSERT INTO `tmp_quality_backfill_v2_assert` (`assertion_name`, `assertion_passed`)
SELECT 'failed inspection has no unrelated results or NCR', IF(
  (SELECT COUNT(*) FROM `qms_inspection_result`
   WHERE `inspection_id` = @failed_inspection_id
     AND `remark` NOT LIKE CONCAT(@quality_backfill_v2_marker, '%') AND `deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM `qms_nonconforming_order`
       WHERE `inspection_id` = @failed_inspection_id AND `ncr_no` <> 'NCR-20260715-002'
         AND `deleted_flag` = 0) = 0,
  1, NULL);

INSERT INTO `tmp_quality_backfill_v2_assert` (`assertion_name`, `assertion_passed`)
SELECT 'failed work order has exactly one active product lot', IF(COUNT(*) = 1, 1, NULL)
FROM `qms_inspection_order` inspection_order
JOIN `trace_product_lot` product_lot
  ON product_lot.`work_order_id` = inspection_order.`work_order_id` AND product_lot.`deleted_flag` = 0
WHERE inspection_order.`inspection_id` = @failed_inspection_id;

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

-- Repair V1 text that was corrupted by a Windows PowerShell native-command pipeline.
UPDATE `qms_inspection_order`
SET `remark` = CASE `inspection_no`
  WHEN 'IPQC-WO-20260703-011-1-2' THEN CONCAT(@quality_backfill_v1_marker, ': 已审核速度检测报工补录，管理员提交结果为不合格')
  WHEN 'IPQC-WO-20260713-013-1-2' THEN CONCAT(@quality_backfill_v1_marker, ': 已审核速度检测报工待检')
  WHEN 'IPQC-WO-20260713-024-2-2' THEN CONCAT(@quality_backfill_v1_marker, ': 速度与飞行抽检不合格')
  WHEN 'IPQC-RC-WO-20260713-024-2-2' THEN CONCAT(@quality_backfill_v1_marker, ': 飞行偏差返工后复检合格')
END
WHERE `inspection_no` IN
      ('IPQC-WO-20260703-011-1-2', 'IPQC-WO-20260713-013-1-2',
       'IPQC-WO-20260713-024-2-2', 'IPQC-RC-WO-20260713-024-2-2')
  AND `deleted_flag` = 0;
SET @repaired_v1_inspections = ROW_COUNT();

UPDATE `qms_inspection_result` result_record
JOIN `qms_inspection_order` inspection_order
  ON inspection_order.`inspection_id` = result_record.`inspection_id` AND inspection_order.`deleted_flag` = 0
JOIN `qms_inspection_item` item
  ON item.`item_id` = result_record.`item_id` AND item.`deleted_flag` = 0
SET result_record.`remark` = CONCAT(@quality_backfill_v1_marker, ': 速度与飞行检测明细'),
    result_record.`defect_description` = IF(
      inspection_order.`inspection_no` = 'IPQC-WO-20260713-024-2-2'
      AND item.`item_code` = 'QI-FLIGHT-STABILITY',
      '抽样落点最大径向偏差345mm，超过300mm上限', NULL)
WHERE inspection_order.`inspection_no` IN
      ('IPQC-WO-20260713-024-2-2', 'IPQC-RC-WO-20260713-024-2-2')
  AND result_record.`remark` LIKE CONCAT(@quality_backfill_v1_marker, '%')
  AND result_record.`deleted_flag` = 0;
SET @repaired_v1_results = ROW_COUNT();

UPDATE `qms_nonconforming_order`
SET `defect_reason` = '速度检测抽样发现飞行落点偏差超限，隔离4筒复核',
    `remark` = CONCAT(@quality_backfill_v1_marker, ': 校准设备并返工，复检合格后关闭')
WHERE `ncr_no` = 'NCR-20260715-001' AND `deleted_flag` = 0;
SET @repaired_v1_ncr_orders = ROW_COUNT();

UPDATE `qms_nonconforming_detail` detail
JOIN `qms_nonconforming_order` ncr
  ON ncr.`ncr_id` = detail.`ncr_id` AND ncr.`deleted_flag` = 0
SET detail.`remark` = CONCAT(@quality_backfill_v1_marker,
  ': 隔离4筒返工，复检记录IPQC-RC-WO-20260713-024-2-2合格')
WHERE ncr.`ncr_no` = 'NCR-20260715-001' AND detail.`deleted_flag` = 0;
SET @repaired_v1_ncr_details = ROW_COUNT();

UPDATE `sys_status_change_log` status_log
LEFT JOIN `qms_inspection_order` inspection_order
  ON status_log.`biz_type` = 'QMS_INSPECTION_ORDER'
 AND status_log.`biz_id` = inspection_order.`inspection_id` AND inspection_order.`deleted_flag` = 0
LEFT JOIN `qms_nonconforming_order` ncr
  ON status_log.`biz_type` = 'QMS_NONCONFORMING_ORDER'
 AND status_log.`biz_id` = ncr.`ncr_id` AND ncr.`deleted_flag` = 0
SET status_log.`remark` = CASE inspection_order.`inspection_no`
  WHEN 'IPQC-WO-20260713-024-2-2' THEN CONCAT(@quality_backfill_v1_marker, ': 检验记录提交，不合格')
  WHEN 'IPQC-RC-WO-20260713-024-2-2' THEN CONCAT(@quality_backfill_v1_marker, ': 返工复检提交，合格')
  WHEN 'IPQC-WO-20260703-011-1-2' THEN CONCAT(@quality_backfill_v1_marker, ': 管理员提交检验结果，不合格')
  ELSE IF(ncr.`ncr_no` = 'NCR-20260715-001',
          CONCAT(@quality_backfill_v1_marker, ': 返工复检合格后关闭'), status_log.`remark`)
END
WHERE status_log.`remark` LIKE CONCAT(@quality_backfill_v1_marker, '%')
  AND status_log.`deleted_flag` = 0;
SET @repaired_v1_status_logs = ROW_COUNT();

UPDATE `sys_operation_log` operation_log
LEFT JOIN `qms_inspection_order` inspection_order
  ON operation_log.`module_code` = 'qms_inspection_order'
 AND operation_log.`biz_id` = inspection_order.`inspection_id` AND inspection_order.`deleted_flag` = 0
SET operation_log.`remark` = CONCAT(@quality_backfill_v1_marker, ': ', CASE operation_log.`operation_type`
  WHEN 'CREATE_INSPECTION' THEN '创建检验单'
  WHEN 'SUBMIT_INSPECTION' THEN IF(inspection_order.`inspection_no` = 'IPQC-WO-20260703-011-1-2',
                                   '管理员提交检验结果', '提交检验记录')
  WHEN 'CREATE_NONCONFORMING' THEN '创建不合格处理单'
  WHEN 'APPROVE_NONCONFORMING' THEN '审批返工方案'
  WHEN 'CLOSE_NONCONFORMING' THEN '返工复检合格后关闭'
  ELSE '质量数据补录'
END)
WHERE operation_log.`remark` LIKE CONCAT(@quality_backfill_v1_marker, '%')
  AND operation_log.`deleted_flag` = 0;
SET @repaired_v1_operation_logs = ROW_COUNT();

UPDATE `sys_approval_record`
SET `approval_comment` = '同意设备校准和受控返工，复检合格后关闭',
    `remark` = CONCAT(@quality_backfill_v1_marker, ': 不合格处置审批')
WHERE `remark` LIKE CONCAT(@quality_backfill_v1_marker, '%') AND `deleted_flag` = 0;
SET @repaired_v1_approval_records = ROW_COUNT();

INSERT INTO `qms_inspection_order`
  (`inspection_no`, `inspection_type`, `scheme_id`, `work_order_id`, `task_id`, `report_id`, `batch_id`,
   `inspector_id`, `inspection_result`, `status`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'IPQC-RC-WO-20260703-011-1-2', failed.`inspection_type`, failed.`scheme_id`, failed.`work_order_id`,
       failed.`task_id`, NULL, NULL, @quality_employee_id, 'PASS', 'FINISHED',
       CONCAT(@quality_backfill_v2_marker, ': 飞行偏差返工后复检合格'), 0,
       @quality_user_id, TIMESTAMPADD(MINUTE, -20, @quality_backfill_v2_time),
       @quality_user_id, TIMESTAMPADD(MINUTE, -15, @quality_backfill_v2_time), 0
FROM `qms_inspection_order` failed
WHERE failed.`inspection_id` = @failed_inspection_id
  AND failed.`status` = 'FINISHED' AND failed.`inspection_result` = 'FAIL'
  AND NOT EXISTS (
    SELECT 1 FROM `qms_inspection_order` existing
    WHERE existing.`inspection_no` = 'IPQC-RC-WO-20260703-011-1-2' AND existing.`deleted_flag` = 0
  );
SET @inserted_v2_recheck_inspections = ROW_COUNT();

INSERT INTO `qms_inspection_result`
  (`inspection_id`, `item_id`, `measured_value`, `result`, `defect_description`, `remark`, `version`,
   `created_by`, `created_time`, `deleted_flag`)
SELECT inspection_order.`inspection_id`, item.`item_id`,
       CASE
         WHEN inspection_order.`inspection_no` = 'IPQC-WO-20260703-011-1-2' AND item.`item_code` = 'QI-FINISHED-WEIGHT' THEN '5.00'
         WHEN inspection_order.`inspection_no` = 'IPQC-WO-20260703-011-1-2' AND item.`item_code` = 'QI-SPEED-GRADE' THEN '77'
         WHEN inspection_order.`inspection_no` = 'IPQC-WO-20260703-011-1-2' AND item.`item_code` = 'QI-FLIGHT-STABILITY' THEN '535'
         WHEN inspection_order.`inspection_no` = 'IPQC-RC-WO-20260703-011-1-2' AND item.`item_code` = 'QI-FINISHED-WEIGHT' THEN '5.01'
         WHEN inspection_order.`inspection_no` = 'IPQC-RC-WO-20260703-011-1-2' AND item.`item_code` = 'QI-SPEED-GRADE' THEN '77'
         WHEN inspection_order.`inspection_no` = 'IPQC-RC-WO-20260703-011-1-2' AND item.`item_code` = 'QI-FLIGHT-STABILITY' THEN '455'
       END,
       IF(inspection_order.`inspection_no` = 'IPQC-WO-20260703-011-1-2'
          AND item.`item_code` = 'QI-FLIGHT-STABILITY', 'FAIL', 'PASS'),
       IF(inspection_order.`inspection_no` = 'IPQC-WO-20260703-011-1-2'
          AND item.`item_code` = 'QI-FLIGHT-STABILITY', '抽样落点最大径向偏差535mm，超过500mm上限', NULL),
       CONCAT(@quality_backfill_v2_marker, ': 速度与飞行检测明细'), 0,
       @quality_user_id, COALESCE(inspection_order.`updated_time`, @quality_backfill_v2_time), 0
FROM `qms_inspection_order` inspection_order
JOIN `qms_inspection_scheme_item` scheme_item
  ON scheme_item.`scheme_id` = inspection_order.`scheme_id` AND scheme_item.`deleted_flag` = 0
JOIN `qms_inspection_item` item
  ON item.`item_id` = scheme_item.`item_id` AND item.`deleted_flag` = 0
WHERE inspection_order.`inspection_no` IN
      ('IPQC-WO-20260703-011-1-2', 'IPQC-RC-WO-20260703-011-1-2')
  AND inspection_order.`status` = 'FINISHED' AND inspection_order.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `qms_inspection_result` existing
    WHERE existing.`inspection_id` = inspection_order.`inspection_id`
      AND existing.`item_id` = item.`item_id` AND existing.`deleted_flag` = 0
  );
SET @inserted_v2_results = ROW_COUNT();

INSERT INTO `qms_nonconforming_order`
  (`ncr_no`, `inspection_id`, `product_lot_id`, `defect_reason`, `disposition_type`, `approval_status`, `status`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'NCR-20260715-002', failed.`inspection_id`, product_lot.`product_lot_id`,
       '速度检测抽样发现飞行落点偏差超限，隔离6筒复核', 'REWORK', 'APPROVED', 'CLOSED',
       CONCAT(@quality_backfill_v2_marker, ': 校准设备并返工，复检合格后关闭'), 0,
       @quality_user_id, TIMESTAMPADD(MINUTE, -30, @quality_backfill_v2_time),
       @quality_user_id, TIMESTAMPADD(MINUTE, -10, @quality_backfill_v2_time), 0
FROM `qms_inspection_order` failed
JOIN `trace_product_lot` product_lot
  ON product_lot.`work_order_id` = failed.`work_order_id` AND product_lot.`deleted_flag` = 0
WHERE failed.`inspection_id` = @failed_inspection_id
  AND failed.`status` = 'FINISHED' AND failed.`inspection_result` = 'FAIL'
  AND NOT EXISTS (
    SELECT 1 FROM `qms_nonconforming_order` existing
    WHERE existing.`ncr_no` = 'NCR-20260715-002' AND existing.`deleted_flag` = 0
  );
SET @inserted_v2_ncr_orders = ROW_COUNT();

INSERT INTO `qms_nonconforming_detail`
  (`ncr_id`, `barcode_id`, `inspection_result_id`, `defect_quantity`, `final_disposition`, `recheck_result`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT ncr.`ncr_id`, NULL, result_record.`result_id`, 6.0000, 'REWORK', 'PASS',
       CONCAT(@quality_backfill_v2_marker,
         ': 隔离6筒返工，复检记录IPQC-RC-WO-20260703-011-1-2合格'), 0,
       @quality_user_id, TIMESTAMPADD(MINUTE, -25, @quality_backfill_v2_time),
       @quality_user_id, TIMESTAMPADD(MINUTE, -10, @quality_backfill_v2_time), 0
FROM `qms_nonconforming_order` ncr
JOIN `qms_inspection_result` result_record
  ON result_record.`inspection_id` = ncr.`inspection_id`
 AND result_record.`result` = 'FAIL' AND result_record.`deleted_flag` = 0
JOIN `qms_inspection_item` item
  ON item.`item_id` = result_record.`item_id`
 AND item.`item_code` = 'QI-FLIGHT-STABILITY' AND item.`deleted_flag` = 0
WHERE ncr.`ncr_no` = 'NCR-20260715-002' AND ncr.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `qms_nonconforming_detail` existing
    WHERE existing.`ncr_id` = ncr.`ncr_id`
      AND existing.`inspection_result_id` = result_record.`result_id`
      AND existing.`deleted_flag` = 0
  );
SET @inserted_v2_ncr_details = ROW_COUNT();

INSERT INTO `sys_status_change_log`
  (`biz_type`, `biz_id`, `old_status`, `new_status`, `change_reason`, `changed_by`, `changed_time`,
   `remark`, `version`, `created_by`, `created_time`, `deleted_flag`)
SELECT status_seed.`biz_type`, status_seed.`biz_id`, status_seed.`old_status`, status_seed.`new_status`,
       status_seed.`change_reason`, @quality_user_id, status_seed.`changed_time`, status_seed.`remark`,
       0, @quality_user_id, status_seed.`changed_time`, 0
FROM (
  SELECT 'QMS_INSPECTION_ORDER' AS `biz_type`, inspection_order.`inspection_id` AS `biz_id`,
         'DRAFT' AS `old_status`, 'FINISHED' AS `new_status`, 'SUBMIT_INSPECTION' AS `change_reason`,
         inspection_order.`updated_time` AS `changed_time`,
         CONCAT(@quality_backfill_v2_marker, ': 返工复检提交，合格') AS `remark`
  FROM `qms_inspection_order` inspection_order
  WHERE inspection_order.`inspection_no` = 'IPQC-RC-WO-20260703-011-1-2'
    AND inspection_order.`deleted_flag` = 0
  UNION ALL
  SELECT 'QMS_NONCONFORMING_ORDER', ncr.`ncr_id`, 'SUBMITTED', 'CLOSED', 'CLOSE_NONCONFORMING',
         ncr.`updated_time`, CONCAT(@quality_backfill_v2_marker, ': 返工复检合格后关闭')
  FROM `qms_nonconforming_order` ncr
  WHERE ncr.`ncr_no` = 'NCR-20260715-002' AND ncr.`deleted_flag` = 0
) status_seed
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_status_change_log` existing
  WHERE existing.`biz_type` = status_seed.`biz_type` AND existing.`biz_id` = status_seed.`biz_id`
    AND existing.`change_reason` = status_seed.`change_reason`
    AND existing.`remark` LIKE CONCAT(@quality_backfill_v2_marker, '%')
    AND existing.`deleted_flag` = 0
);
SET @inserted_v2_status_logs = ROW_COUNT();

INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `operation_time`, `remark`,
   `version`, `created_by`, `created_time`, `deleted_flag`)
SELECT @quality_user_id, operation_seed.`operation_type`, operation_seed.`module_code`, operation_seed.`biz_id`,
       operation_seed.`operation_time`, operation_seed.`remark`, 0, @quality_user_id,
       operation_seed.`operation_time`, 0
FROM (
  SELECT 'CREATE_INSPECTION' AS `operation_type`, 'qms_inspection_order' AS `module_code`,
         inspection_order.`inspection_id` AS `biz_id`, inspection_order.`created_time` AS `operation_time`,
         CONCAT(@quality_backfill_v2_marker, ': 创建返工复检单') AS `remark`
  FROM `qms_inspection_order` inspection_order
  WHERE inspection_order.`inspection_no` = 'IPQC-RC-WO-20260703-011-1-2'
    AND inspection_order.`deleted_flag` = 0
  UNION ALL
  SELECT 'SUBMIT_INSPECTION', 'qms_inspection_order', inspection_order.`inspection_id`,
         inspection_order.`updated_time`, CONCAT(@quality_backfill_v2_marker, ': 提交返工复检记录')
  FROM `qms_inspection_order` inspection_order
  WHERE inspection_order.`inspection_no` = 'IPQC-RC-WO-20260703-011-1-2'
    AND inspection_order.`deleted_flag` = 0
  UNION ALL
  SELECT 'CREATE_NONCONFORMING', 'qms_nonconforming_order', ncr.`ncr_id`, ncr.`created_time`,
         CONCAT(@quality_backfill_v2_marker, ': 创建不合格处理单')
  FROM `qms_nonconforming_order` ncr
  WHERE ncr.`ncr_no` = 'NCR-20260715-002' AND ncr.`deleted_flag` = 0
  UNION ALL
  SELECT 'APPROVE_NONCONFORMING', 'qms_nonconforming_order', ncr.`ncr_id`,
         TIMESTAMPADD(MINUTE, -27, @quality_backfill_v2_time), CONCAT(@quality_backfill_v2_marker, ': 审批返工方案')
  FROM `qms_nonconforming_order` ncr
  WHERE ncr.`ncr_no` = 'NCR-20260715-002' AND ncr.`deleted_flag` = 0
  UNION ALL
  SELECT 'CLOSE_NONCONFORMING', 'qms_nonconforming_order', ncr.`ncr_id`, ncr.`updated_time`,
         CONCAT(@quality_backfill_v2_marker, ': 返工复检合格后关闭')
  FROM `qms_nonconforming_order` ncr
  WHERE ncr.`ncr_no` = 'NCR-20260715-002' AND ncr.`deleted_flag` = 0
) operation_seed
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_operation_log` existing
  WHERE existing.`operation_type` = operation_seed.`operation_type`
    AND existing.`module_code` = operation_seed.`module_code`
    AND existing.`biz_id` = operation_seed.`biz_id`
    AND existing.`remark` LIKE CONCAT(@quality_backfill_v2_marker, '%')
    AND existing.`deleted_flag` = 0
);
SET @inserted_v2_operation_logs = ROW_COUNT();

INSERT INTO `sys_approval_record`
  (`biz_type`, `biz_id`, `approval_node`, `approver_id`, `approval_result`, `approval_comment`, `approval_time`,
   `remark`, `version`, `created_by`, `created_time`, `deleted_flag`)
SELECT 'QMS_NONCONFORMING_ORDER', ncr.`ncr_id`, 'DISPOSITION_APPROVAL', @quality_user_id, 'APPROVED',
       '同意设备校准和受控返工，复检合格后关闭', TIMESTAMPADD(MINUTE, -27, @quality_backfill_v2_time),
       CONCAT(@quality_backfill_v2_marker, ': 不合格处置审批'), 0, @quality_user_id,
       TIMESTAMPADD(MINUTE, -27, @quality_backfill_v2_time), 0
FROM `qms_nonconforming_order` ncr
WHERE ncr.`ncr_no` = 'NCR-20260715-002' AND ncr.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `sys_approval_record` existing
    WHERE existing.`biz_type` = 'QMS_NONCONFORMING_ORDER' AND existing.`biz_id` = ncr.`ncr_id`
      AND existing.`approval_node` = 'DISPOSITION_APPROVAL'
      AND existing.`remark` LIKE CONCAT(@quality_backfill_v2_marker, '%')
      AND existing.`deleted_flag` = 0
  );
SET @inserted_v2_approval_records = ROW_COUNT();

INSERT INTO `tmp_quality_backfill_v2_assert` (`assertion_name`, `assertion_passed`)
SELECT 'one pending inspection remains available', IF(COUNT(*) = 1, 1, NULL)
FROM `qms_inspection_order`
WHERE `inspection_no` = 'IPQC-WO-20260713-013-1-2'
  AND `status` = 'DRAFT' AND `inspection_result` IS NULL AND `deleted_flag` = 0;

INSERT INTO `tmp_quality_backfill_v2_assert` (`assertion_name`, `assertion_passed`)
SELECT 'concurrent failed inspection now has complete result details',
       IF(COUNT(*) = 3 AND SUM(result_record.`result` = 'PASS') = 2
          AND SUM(result_record.`result` = 'FAIL') = 1, 1, NULL)
FROM `qms_inspection_result` result_record
WHERE result_record.`inspection_id` = @failed_inspection_id AND result_record.`deleted_flag` = 0;

INSERT INTO `tmp_quality_backfill_v2_assert` (`assertion_name`, `assertion_passed`)
SELECT 'second NCR and recheck close the concurrent failed loop', IF(COUNT(*) = 1, 1, NULL)
FROM `qms_nonconforming_order` ncr
JOIN `qms_nonconforming_detail` detail
  ON detail.`ncr_id` = ncr.`ncr_id` AND detail.`defect_quantity` = 6.0000
 AND detail.`final_disposition` = 'REWORK' AND detail.`recheck_result` = 'PASS'
 AND detail.`deleted_flag` = 0
JOIN `qms_inspection_result` failed_result
  ON failed_result.`result_id` = detail.`inspection_result_id`
 AND failed_result.`inspection_id` = ncr.`inspection_id`
 AND failed_result.`result` = 'FAIL' AND failed_result.`deleted_flag` = 0
JOIN `qms_inspection_order` recheck
  ON recheck.`inspection_no` = 'IPQC-RC-WO-20260703-011-1-2'
 AND recheck.`work_order_id` = (SELECT `work_order_id` FROM `qms_inspection_order`
                               WHERE `inspection_id` = @failed_inspection_id)
 AND recheck.`status` = 'FINISHED' AND recheck.`inspection_result` = 'PASS'
 AND recheck.`deleted_flag` = 0
WHERE ncr.`ncr_no` = 'NCR-20260715-002' AND ncr.`inspection_id` = @failed_inspection_id
  AND ncr.`approval_status` = 'APPROVED' AND ncr.`status` = 'CLOSED'
  AND ncr.`disposition_type` = 'REWORK' AND ncr.`deleted_flag` = 0
  AND (SELECT COUNT(*) FROM `qms_inspection_result` recheck_result
       WHERE recheck_result.`inspection_id` = recheck.`inspection_id`
         AND recheck_result.`result` = 'PASS' AND recheck_result.`deleted_flag` = 0) = 3;

INSERT INTO `tmp_quality_backfill_v2_assert` (`assertion_name`, `assertion_passed`)
SELECT 'V1 and V2 Chinese text contains no replacement question marks', IF(
  (SELECT COUNT(*) FROM `qms_inspection_order`
   WHERE (`remark` LIKE CONCAT(@quality_backfill_v1_marker, '%')
      OR `remark` LIKE CONCAT(@quality_backfill_v2_marker, '%'))
     AND INSTR(`remark`, '?') > 0 AND `deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM `qms_inspection_result`
       WHERE (`remark` LIKE CONCAT(@quality_backfill_v1_marker, '%')
          OR `remark` LIKE CONCAT(@quality_backfill_v2_marker, '%'))
         AND (INSTR(`remark`, '?') > 0 OR INSTR(COALESCE(`defect_description`, ''), '?') > 0)
         AND `deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM `qms_nonconforming_order`
       WHERE (`remark` LIKE CONCAT(@quality_backfill_v1_marker, '%')
          OR `remark` LIKE CONCAT(@quality_backfill_v2_marker, '%'))
         AND (INSTR(`remark`, '?') > 0 OR INSTR(COALESCE(`defect_reason`, ''), '?') > 0)
         AND `deleted_flag` = 0) = 0
  AND (SELECT COUNT(*) FROM `qms_nonconforming_detail`
       WHERE (`remark` LIKE CONCAT(@quality_backfill_v1_marker, '%')
          OR `remark` LIKE CONCAT(@quality_backfill_v2_marker, '%'))
         AND INSTR(`remark`, '?') > 0 AND `deleted_flag` = 0) = 0,
  1, NULL);

INSERT INTO `tmp_quality_backfill_v2_assert` (`assertion_name`, `assertion_passed`)
SELECT 'V2 audit records are complete', IF(
  (SELECT COUNT(*) FROM `sys_status_change_log`
   WHERE `remark` LIKE CONCAT(@quality_backfill_v2_marker, '%') AND `deleted_flag` = 0) = 2
  AND (SELECT COUNT(*) FROM `sys_operation_log`
       WHERE `remark` LIKE CONCAT(@quality_backfill_v2_marker, '%') AND `deleted_flag` = 0) = 5
  AND (SELECT COUNT(*) FROM `sys_approval_record`
       WHERE `remark` LIKE CONCAT(@quality_backfill_v2_marker, '%') AND `deleted_flag` = 0) = 1,
  1, NULL);

INSERT INTO `tmp_quality_backfill_v2_assert` (`assertion_name`, `assertion_passed`)
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

SET @passed_v2_assertion_count = (SELECT COUNT(*) FROM `tmp_quality_backfill_v2_assert`);
SET @quality_backfill_v2_final_statement = IF(@quality_backfill_v2_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE quality_backfill_v2_final_statement FROM @quality_backfill_v2_final_statement;
EXECUTE quality_backfill_v2_final_statement;
DEALLOCATE PREPARE quality_backfill_v2_final_statement;

SELECT IF(@quality_backfill_v2_commit = 1, 'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @repaired_v1_inspections AS `repaired_v1_inspections`,
       @repaired_v1_results AS `repaired_v1_results`,
       @repaired_v1_ncr_orders AS `repaired_v1_ncr_orders`,
       @repaired_v1_ncr_details AS `repaired_v1_ncr_details`,
       @repaired_v1_status_logs AS `repaired_v1_status_logs`,
       @repaired_v1_operation_logs AS `repaired_v1_operation_logs`,
       @repaired_v1_approval_records AS `repaired_v1_approval_records`,
       @inserted_v2_recheck_inspections AS `inserted_v2_recheck_inspections`,
       @inserted_v2_results AS `inserted_v2_results`,
       @inserted_v2_ncr_orders AS `inserted_v2_ncr_orders`,
       @inserted_v2_ncr_details AS `inserted_v2_ncr_details`,
       @inserted_v2_status_logs AS `inserted_v2_status_logs`,
       @inserted_v2_operation_logs AS `inserted_v2_operation_logs`,
       @inserted_v2_approval_records AS `inserted_v2_approval_records`,
       @passed_v2_assertion_count AS `passed_assertions`;
