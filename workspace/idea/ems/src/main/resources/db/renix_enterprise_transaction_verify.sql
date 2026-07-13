-- 雷尼克斯 30 天企业交易数据验收。
-- 只读校验；failed_checks 必须为 0 才允许申请写入共享库。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @transaction_marker = 'RENIX_TX_30D_V1';

SELECT checks.`check_group`, checks.`check_name`, checks.`expected_value`, checks.`actual_value`,
       IF(checks.`expected_value` = checks.`actual_value`, 'PASS', 'FAIL') AS `result`,
       SUM(IF(checks.`expected_value` = checks.`actual_value`, 0, 1)) OVER () AS `failed_checks`
FROM (
  -- 完成标记与核心规模。
  SELECT 'marker' AS `check_group`, 'transaction completion marker' AS `check_name`, 1 AS `expected_value`,
         (SELECT COUNT(*) FROM sys_operation_log WHERE operation_type='SEED_RENIX_30D'
          AND module_code='db_seed' AND remark=@transaction_marker AND deleted_flag=0) AS `actual_value`
  UNION ALL SELECT 'count', 'purchase inbound orders', 15,
         (SELECT COUNT(*) FROM wms_inbound_order WHERE inbound_type='PURCHASE' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'purchase inbound details', 54,
         (SELECT COUNT(*) FROM wms_inbound_detail detail JOIN wms_inbound_order header ON header.inbound_id=detail.inbound_id
          WHERE detail.deleted_flag=0 AND header.deleted_flag=0 AND header.inbound_type='PURCHASE')
  UNION ALL SELECT 'count', 'purchase material batches', 54,
         (SELECT COUNT(*) FROM wms_material_batch WHERE source_order_type='PURCHASE_RECEIPT' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'incoming inspections', 55,
         (SELECT COUNT(*) FROM qms_inspection_order WHERE inspection_type='INCOMING' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'production orders', 30,
         (SELECT COUNT(*) FROM mfg_production_order
          WHERE remark LIKE CONCAT(@transaction_marker,'%') AND deleted_flag=0)
  UNION ALL SELECT 'count', 'production order quantity', 24200,
         (SELECT COALESCE(SUM(order_quantity),0) FROM mfg_production_order
          WHERE remark LIKE CONCAT(@transaction_marker,'%') AND deleted_flag=0)
  UNION ALL SELECT 'count', 'finished production orders', 20,
         (SELECT COUNT(*) FROM mfg_production_order
          WHERE status='FINISHED' AND remark LIKE CONCAT(@transaction_marker,'%') AND deleted_flag=0)
  UNION ALL SELECT 'count', 'running production orders', 2,
         (SELECT COUNT(*) FROM mfg_production_order
          WHERE status='RUNNING' AND remark LIKE CONCAT(@transaction_marker,'%') AND deleted_flag=0)
  UNION ALL SELECT 'count', 'released production orders', 3,
         (SELECT COUNT(*) FROM mfg_production_order
          WHERE status='RELEASED' AND remark LIKE CONCAT(@transaction_marker,'%') AND deleted_flag=0)
  UNION ALL SELECT 'count', 'draft production orders', 5,
         (SELECT COUNT(*) FROM mfg_production_order
          WHERE status='DRAFT' AND remark LIKE CONCAT(@transaction_marker,'%') AND deleted_flag=0)
  UNION ALL SELECT 'count', 'production plans', 25,
         (SELECT COUNT(*) FROM mfg_production_plan WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'work orders', 32,
         (SELECT COUNT(*) FROM mfg_work_order WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'finished work orders', 27,
         (SELECT COUNT(*) FROM mfg_work_order WHERE status='FINISHED' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'running work orders', 2,
         (SELECT COUNT(*) FROM mfg_work_order WHERE status='RUNNING' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'released work orders', 3,
         (SELECT COUNT(*) FROM mfg_work_order WHERE status='RELEASED' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'work order materials', 224,
         (SELECT COUNT(*) FROM mfg_work_order_material WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'dispatch orders', 32,
         (SELECT COUNT(*) FROM mfg_dispatch_order WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'production tasks', 224,
         (SELECT COUNT(*) FROM mfg_task WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'finished tasks', 197,
         (SELECT COUNT(*) FROM mfg_task WHERE status='FINISHED' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'running tasks', 2,
         (SELECT COUNT(*) FROM mfg_task WHERE status='RUNNING' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'waiting tasks', 25,
         (SELECT COUNT(*) FROM mfg_task WHERE status='WAIT_START' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'task operators', 280,
         (SELECT COUNT(*) FROM mfg_task_operator WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'approved work reports', 199,
         (SELECT COUNT(*) FROM mfg_work_report WHERE audit_status='APPROVED' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'report material range', 0,
         (SELECT IF(COUNT(*) BETWEEN 203 AND 240,0,1) FROM mfg_report_material WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'material issue outbound orders', 29,
         (SELECT COUNT(*) FROM wms_outbound_order WHERE outbound_type='MATERIAL_ISSUE' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'material return orders', 27,
         (SELECT COUNT(*) FROM wms_return_order WHERE return_type='MATERIAL_RETURN' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'finished goods inbound orders', 27,
         (SELECT COUNT(*) FROM wms_inbound_order WHERE inbound_type='PRODUCTION' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'sales outbound orders', 5,
         (SELECT COUNT(*) FROM wms_outbound_order WHERE outbound_type='SALE' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'stocktake orders', 1,
         (SELECT COUNT(*) FROM wms_stocktake_order WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'stocktake details', 8,
         (SELECT COUNT(*) FROM wms_stocktake_detail WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'process inspections', 30,
         (SELECT COUNT(*) FROM qms_inspection_order WHERE inspection_type='PROCESS' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'final inspections', 27,
         (SELECT COUNT(*) FROM qms_inspection_order WHERE inspection_type='FINAL' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'inspection results', 191,
         (SELECT COUNT(*) FROM qms_inspection_result WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'nonconforming orders', 2,
         (SELECT COUNT(*) FROM qms_nonconforming_order WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'product lots', 29,
         (SELECT COUNT(*) FROM trace_product_lot WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'stored product lots', 27,
         (SELECT COUNT(*) FROM trace_product_lot WHERE status='STORED' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'running product lots', 2,
         (SELECT COUNT(*) FROM trace_product_lot WHERE status='RUNNING' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'trace barcodes', 744,
         (SELECT COUNT(*) FROM trace_barcode WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'material usage trace rows', 227,
         (SELECT COUNT(*) FROM trace_material_usage WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'product lot barcodes', 29,
         (SELECT COUNT(*) FROM trace_barcode WHERE barcode_type='PRODUCT_LOT' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'outer carton barcodes', 668,
         (SELECT COUNT(*) FROM trace_barcode WHERE barcode_type='OUTER_CARTON' AND deleted_flag=0)
  UNION ALL SELECT 'count', 'spot check plans', 12,
         (SELECT COUNT(*) FROM eam_spot_check_plan WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'spot check records', 300,
         (SELECT COUNT(*) FROM eam_spot_check_record WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'maintenance plans', 12,
         (SELECT COUNT(*) FROM eam_maintenance_plan WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'maintenance records', 12,
         (SELECT COUNT(*) FROM eam_maintenance_record WHERE deleted_flag=0)
  UNION ALL SELECT 'count', 'repair orders', 1,
         (SELECT COUNT(*) FROM eam_repair_order WHERE deleted_flag=0)

  -- 生产数量、关联和时间守恒。
  UNION ALL SELECT 'production', 'order plan quantity mismatch', 0,
         (SELECT COUNT(*) FROM (
           SELECT production_order.production_order_id, production_order.status, production_order.order_quantity,
                  COUNT(plan.plan_id) plan_count, COALESCE(SUM(plan.planned_quantity),0) plan_quantity
           FROM mfg_production_order production_order
           LEFT JOIN mfg_production_plan plan ON plan.production_order_id=production_order.production_order_id AND plan.deleted_flag=0
           WHERE production_order.deleted_flag=0
           GROUP BY production_order.production_order_id,production_order.status,production_order.order_quantity
           HAVING (production_order.status='DRAFT' AND plan_count<>0)
              OR (production_order.status<>'DRAFT' AND (plan_count<>1 OR plan_quantity<>production_order.order_quantity))
         ) invalid_order)
  UNION ALL SELECT 'production', 'plan work order quantity mismatch', 0,
         (SELECT COUNT(*) FROM (
           SELECT plan.plan_id,plan.planned_quantity,COALESCE(SUM(work_order.planned_quantity),0) work_order_quantity
           FROM mfg_production_plan plan
           LEFT JOIN mfg_work_order work_order ON work_order.plan_id=plan.plan_id AND work_order.deleted_flag=0
           WHERE plan.deleted_flag=0 GROUP BY plan.plan_id,plan.planned_quantity
           HAVING work_order_quantity<>plan.planned_quantity
         ) invalid_plan)
  UNION ALL SELECT 'production', 'work order product BOM route mismatch', 0,
         (SELECT COUNT(*) FROM mfg_work_order work_order
          LEFT JOIN mdm_bom bom ON bom.bom_id=work_order.bom_id AND bom.product_id=work_order.product_id AND bom.deleted_flag=0
          LEFT JOIN process_route route ON route.route_id=work_order.route_id AND route.product_id=work_order.product_id AND route.deleted_flag=0
          WHERE work_order.deleted_flag=0 AND (bom.bom_id IS NULL OR route.route_id IS NULL))
  UNION ALL SELECT 'production', 'work order task count or sequence mismatch', 0,
         (SELECT COUNT(*) FROM (
           SELECT work_order.work_order_id,COUNT(task.task_id) task_count,COUNT(DISTINCT route_item.sequence_no) sequence_count
           FROM mfg_work_order work_order
           LEFT JOIN mfg_task task ON task.work_order_id=work_order.work_order_id AND task.deleted_flag=0
           LEFT JOIN process_route_item route_item ON route_item.route_id=work_order.route_id
             AND route_item.operation_id=task.operation_id AND route_item.deleted_flag=0
           WHERE work_order.deleted_flag=0 GROUP BY work_order.work_order_id
           HAVING task_count<>7 OR sequence_count<>7
         ) invalid_work_order)
  UNION ALL SELECT 'production', 'task station operation line mismatch', 0,
         (SELECT COUNT(*) FROM mfg_task task
          JOIN mfg_work_order work_order ON work_order.work_order_id=task.work_order_id AND work_order.deleted_flag=0
          JOIN mfg_production_plan plan ON plan.plan_id=work_order.plan_id AND plan.deleted_flag=0
          LEFT JOIN factory_station station ON station.station_id=task.station_id AND station.line_id=plan.line_id AND station.deleted_flag=0
          LEFT JOIN factory_station_operation relation ON relation.station_id=task.station_id
            AND relation.operation_id=task.operation_id AND relation.deleted_flag=0
          WHERE task.deleted_flag=0 AND (station.station_id IS NULL OR relation.id IS NULL))
  UNION ALL SELECT 'production', 'task primary operator mismatch', 0,
         (SELECT COUNT(*) FROM (
           SELECT task.task_id,SUM(operator_record.role_in_task='PRIMARY') primary_count
           FROM mfg_task task LEFT JOIN mfg_task_operator operator_record
             ON operator_record.task_id=task.task_id AND operator_record.deleted_flag=0
           WHERE task.deleted_flag=0 GROUP BY task.task_id HAVING primary_count<>1
         ) invalid_task)
  UNION ALL SELECT 'production', 'task operator outside assigned team', 0,
         (SELECT COUNT(*) FROM mfg_task_operator operator_record
          JOIN mfg_task task ON task.task_id=operator_record.task_id AND task.deleted_flag=0
          JOIN mfg_dispatch_order dispatch_order ON dispatch_order.dispatch_id=task.dispatch_id AND dispatch_order.deleted_flag=0
          LEFT JOIN org_team_member member ON member.team_id=dispatch_order.team_id
            AND member.employee_id=operator_record.employee_id AND member.status='ENABLE' AND member.deleted_flag=0
          WHERE operator_record.deleted_flag=0 AND member.id IS NULL)
  UNION ALL SELECT 'production', 'task report quantity mismatch', 0,
         (SELECT COUNT(*) FROM mfg_task task
          WHERE task.deleted_flag=0 AND (
            task.completed_quantity<>COALESCE((SELECT SUM(report.qualified_quantity+report.defect_quantity)
              FROM mfg_work_report report WHERE report.task_id=task.task_id AND report.audit_status='APPROVED' AND report.deleted_flag=0),0)
            OR task.defect_quantity<>COALESCE((SELECT SUM(report.defect_quantity)
              FROM mfg_work_report report WHERE report.task_id=task.task_id AND report.audit_status='APPROVED' AND report.deleted_flag=0),0)))
  UNION ALL SELECT 'production', 'invalid report quantities', 0,
         (SELECT COUNT(*) FROM mfg_work_report report
          JOIN mfg_task task ON task.task_id=report.task_id AND task.deleted_flag=0
          WHERE report.deleted_flag=0 AND (report.qualified_quantity<0 OR report.defect_quantity<0
            OR report.qualified_quantity+report.defect_quantity>task.planned_quantity OR report.actual_work_hours<0))
  UNION ALL SELECT 'production', 'work order material formula mismatch', 0,
         (SELECT COUNT(*) FROM mfg_work_order_material requirement
          JOIN mfg_work_order work_order ON work_order.work_order_id=requirement.work_order_id AND work_order.deleted_flag=0
          JOIN mdm_bom_item bom_item ON bom_item.bom_id=work_order.bom_id
            AND bom_item.material_id=requirement.material_id AND bom_item.deleted_flag=0
          JOIN mdm_material material ON material.material_id=requirement.material_id AND material.deleted_flag=0
          WHERE requirement.deleted_flag=0 AND ABS(requirement.required_quantity-
            IF(material.material_code LIKE 'PKG-CARTON-%',
               CEILING(bom_item.quantity*work_order.planned_quantity*(1+bom_item.loss_rate)),
               ROUND(bom_item.quantity*work_order.planned_quantity*(1+bom_item.loss_rate),4)))>0.0001)
  UNION ALL SELECT 'production', 'issued quantity outbound mismatch', 0,
         (SELECT COUNT(*) FROM mfg_work_order_material requirement
          JOIN mfg_work_order work_order ON work_order.work_order_id=requirement.work_order_id AND work_order.deleted_flag=0
          WHERE requirement.deleted_flag=0 AND ABS(requirement.issued_quantity-COALESCE((
            SELECT SUM(detail.outbound_quantity) FROM wms_outbound_order header
            JOIN wms_outbound_detail detail ON detail.outbound_id=header.outbound_id AND detail.material_id=requirement.material_id
              AND detail.deleted_flag=0
            WHERE header.work_order_id=requirement.work_order_id AND header.outbound_type='MATERIAL_ISSUE' AND header.deleted_flag=0
          ),0))>0.0001)
  UNION ALL SELECT 'production', 'material shortages', 0,
         (SELECT COUNT(*) FROM mfg_work_order_material WHERE deleted_flag=0 AND (shortage_quantity<>0 OR status='SHORTAGE'))
  UNION ALL SELECT 'production', 'invalid production times', 0,
         ((SELECT COUNT(*) FROM mfg_production_plan plan JOIN mfg_production_order production_order
           ON production_order.production_order_id=plan.production_order_id AND production_order.deleted_flag=0
           WHERE plan.deleted_flag=0 AND (plan.created_time<production_order.created_time
             OR plan.planned_start_time>=plan.planned_end_time))
          +(SELECT COUNT(*) FROM mfg_work_report report JOIN mfg_task task ON task.task_id=report.task_id AND task.deleted_flag=0
            WHERE report.deleted_flag=0 AND (report.created_time<task.created_time OR report.report_time<report.created_time)))

  -- 库存、批次与 FIFO。
  UNION ALL SELECT 'inventory', 'future batch material issues', 0,
         (SELECT COUNT(*) FROM wms_outbound_detail detail
          JOIN wms_outbound_order header ON header.outbound_id=detail.outbound_id AND header.outbound_type='MATERIAL_ISSUE'
          JOIN wms_material_batch batch_record ON batch_record.batch_id=detail.batch_id
          WHERE detail.deleted_flag=0 AND header.deleted_flag=0 AND DATE(header.outbound_time)<batch_record.received_date)
  UNION ALL SELECT 'inventory', 'FIFO earlier batch still available', 0,
         (SELECT COUNT(*) FROM wms_outbound_detail detail
          JOIN wms_outbound_order header ON header.outbound_id=detail.outbound_id AND header.outbound_type='MATERIAL_ISSUE'
          JOIN wms_material_batch used_batch ON used_batch.batch_id=detail.batch_id
          WHERE detail.deleted_flag=0 AND header.deleted_flag=0 AND EXISTS (
            SELECT 1 FROM wms_material_batch earlier_batch
            WHERE earlier_batch.material_id=detail.material_id AND earlier_batch.received_date<used_batch.received_date
              AND earlier_batch.deleted_flag=0 AND (SELECT COALESCE(SUM(IF(movement.direction='IN',movement.quantity,-movement.quantity)),0)
                FROM wms_stock_movement movement WHERE movement.batch_id=earlier_batch.batch_id
                  AND movement.deleted_flag=0 AND movement.operation_time<=header.outbound_time)>0))
  UNION ALL SELECT 'inventory', 'negative running batch balances', 0,
         (SELECT COUNT(*) FROM (
           SELECT SUM(IF(direction='IN',quantity,-quantity)) OVER (
             PARTITION BY batch_id ORDER BY operation_time,movement_id
           ) running_balance FROM wms_stock_movement WHERE deleted_flag=0
         ) running_record WHERE running_record.running_balance<0)
  UNION ALL SELECT 'inventory', 'negative inventory balances', 0,
         (SELECT COUNT(*) FROM wms_inventory WHERE deleted_flag=0
          AND (available_quantity<0 OR locked_quantity<0 OR inspection_quantity<0))
  UNION ALL SELECT 'inventory', 'inventory movement balance mismatch', 0,
         (SELECT COUNT(*) FROM (
           SELECT movement.material_id,movement.batch_id,
                  SUM(IF(movement.direction='IN',movement.quantity,-movement.quantity)) movement_balance,
                  COALESCE(inventory.available_quantity,0) inventory_balance
           FROM wms_stock_movement movement
           LEFT JOIN wms_inventory inventory ON inventory.material_id=movement.material_id
             AND inventory.batch_id=movement.batch_id AND inventory.deleted_flag=0
           WHERE movement.deleted_flag=0
           GROUP BY movement.material_id,movement.batch_id,inventory.available_quantity
           HAVING ABS(movement_balance-inventory_balance)>0.0001
         ) invalid_balance)
  UNION ALL SELECT 'inventory', 'document details without movement', 0,
         ((SELECT COUNT(*) FROM wms_inbound_detail detail
           LEFT JOIN wms_stock_movement movement ON movement.source_order_type='INBOUND_ORDER'
             AND movement.source_order_id=detail.inbound_id AND movement.material_id=detail.material_id
             AND movement.batch_id=detail.batch_id AND movement.quantity=detail.inbound_quantity
             AND movement.direction='IN' AND movement.deleted_flag=0
           WHERE detail.deleted_flag=0 AND movement.movement_id IS NULL)
          +(SELECT COUNT(*) FROM wms_outbound_detail detail
            LEFT JOIN wms_stock_movement movement ON movement.source_order_type='OUTBOUND_ORDER'
              AND movement.source_order_id=detail.outbound_id AND movement.material_id=detail.material_id
              AND movement.batch_id=detail.batch_id AND movement.quantity=detail.outbound_quantity
              AND movement.direction='OUT' AND movement.deleted_flag=0
            WHERE detail.deleted_flag=0 AND movement.movement_id IS NULL)
          +(SELECT COUNT(*) FROM wms_return_detail detail
            LEFT JOIN wms_stock_movement movement ON movement.source_order_type='RETURN_ORDER'
              AND movement.source_order_id=detail.return_id AND movement.material_id=detail.material_id
              AND movement.batch_id=detail.batch_id AND movement.quantity=detail.return_quantity
              AND movement.direction='IN' AND movement.deleted_flag=0
            WHERE detail.deleted_flag=0 AND movement.movement_id IS NULL)
          +(SELECT COUNT(*) FROM wms_stocktake_detail detail
            LEFT JOIN wms_stock_movement movement ON movement.source_order_type='STOCKTAKE_ORDER'
              AND movement.source_order_id=detail.stocktake_id AND movement.material_id=detail.material_id
              AND movement.batch_id=detail.batch_id AND movement.quantity=ABS(detail.difference_quantity)
              AND movement.deleted_flag=0
            WHERE detail.deleted_flag=0 AND detail.difference_quantity<>0 AND movement.movement_id IS NULL))
  UNION ALL SELECT 'inventory', 'T300 77 label stocktake difference', -6,
         (SELECT COALESCE(SUM(detail.difference_quantity),0) FROM wms_stocktake_detail detail
          JOIN mdm_material material ON material.material_id=detail.material_id
          WHERE detail.deleted_flag=0 AND material.material_code='PKG-LABEL-LNX-T300-77')
  UNION ALL SELECT 'inventory', 'finished goods target balance mismatch', 0,
         (SELECT COUNT(*) FROM (
           SELECT product.product_code,SUM(inventory.available_quantity) actual_quantity,
                  CASE product.product_code WHEN 'PROD-LNX-T300-76' THEN 400
                    WHEN 'PROD-LNX-T300-77' THEN 1599 WHEN 'PROD-LNX-M700-77' THEN 1000
                    WHEN 'PROD-LNX-M700-78' THEN 200 WHEN 'PROD-OEM-XY-C500-77' THEN 50 END expected_quantity
           FROM mdm_product product JOIN wms_inventory inventory
             ON inventory.material_id=product.material_id AND inventory.deleted_flag=0
           WHERE product.deleted_flag=0 GROUP BY product.product_code
           HAVING actual_quantity<>expected_quantity
         ) invalid_product)

  -- 质量、设备与追溯闭环。
  UNION ALL SELECT 'quality', 'inspection headers without results', 0,
         (SELECT COUNT(*) FROM (
           SELECT inspection_order.inspection_id
           FROM qms_inspection_order inspection_order
           LEFT JOIN qms_inspection_result result_record ON result_record.inspection_id=inspection_order.inspection_id
             AND result_record.deleted_flag=0
           WHERE inspection_order.deleted_flag=0 GROUP BY inspection_order.inspection_id
           HAVING COUNT(result_record.result_id)=0
         ) inspection_without_result)
  UNION ALL SELECT 'quality', 'inspection header detail result mismatch', 0,
         (SELECT COUNT(*) FROM qms_inspection_order inspection_order
          WHERE inspection_order.deleted_flag=0 AND (
            (inspection_order.inspection_result='PASS' AND EXISTS (SELECT 1 FROM qms_inspection_result result_record
              WHERE result_record.inspection_id=inspection_order.inspection_id AND result_record.result='FAIL' AND result_record.deleted_flag=0))
            OR (inspection_order.inspection_result='FAIL' AND NOT EXISTS (SELECT 1 FROM qms_inspection_result result_record
              WHERE result_record.inspection_id=inspection_order.inspection_id AND result_record.result='FAIL' AND result_record.deleted_flag=0))))
  UNION ALL SELECT 'quality', 'numeric measurement judgement mismatch', 0,
         (SELECT COUNT(*) FROM qms_inspection_result result_record
          JOIN qms_inspection_order inspection_order ON inspection_order.inspection_id=result_record.inspection_id AND inspection_order.deleted_flag=0
          JOIN qms_inspection_scheme_item scheme_item ON scheme_item.scheme_id=inspection_order.scheme_id
            AND scheme_item.item_id=result_record.item_id AND scheme_item.deleted_flag=0
          JOIN qms_inspection_standard standard_record ON standard_record.standard_id=scheme_item.standard_id
            AND standard_record.deleted_flag=0
          WHERE result_record.deleted_flag=0 AND result_record.measured_value REGEXP '^[0-9]+(\\\\.[0-9]+)?$'
            AND standard_record.lower_limit IS NOT NULL AND standard_record.upper_limit IS NOT NULL
            AND ((result_record.result='PASS' AND (CAST(result_record.measured_value AS DECIMAL(18,4))<CAST(standard_record.lower_limit AS DECIMAL(18,4))
                 OR CAST(result_record.measured_value AS DECIMAL(18,4))>CAST(standard_record.upper_limit AS DECIMAL(18,4))))
              OR (result_record.result='FAIL' AND CAST(result_record.measured_value AS DECIMAL(18,4)) BETWEEN
                 CAST(standard_record.lower_limit AS DECIMAL(18,4)) AND CAST(standard_record.upper_limit AS DECIMAL(18,4)))))
  UNION ALL SELECT 'quality', 'nonconforming closure mismatch', 0,
         (SELECT COUNT(*) FROM (
           SELECT ncr.ncr_id,ncr.approval_status,ncr.status,COUNT(detail.detail_id) detail_count,
                  SUM(detail.recheck_result='PASS') pass_recheck_count
           FROM qms_nonconforming_order ncr
           LEFT JOIN qms_nonconforming_detail detail ON detail.ncr_id=ncr.ncr_id AND detail.deleted_flag=0
           WHERE ncr.deleted_flag=0 GROUP BY ncr.ncr_id,ncr.approval_status,ncr.status
           HAVING ncr.approval_status<>'APPROVED' OR ncr.status<>'CLOSED' OR detail_count=0 OR pass_recheck_count=0
         ) invalid_ncr)
  UNION ALL SELECT 'quality', 'finished lot FQC or inbound mismatch', 0,
         (SELECT COUNT(*) FROM trace_product_lot product_lot
          LEFT JOIN wms_material_batch batch_record ON batch_record.source_product_lot_id=product_lot.product_lot_id
            AND batch_record.deleted_flag=0
          LEFT JOIN wms_inbound_detail inbound_detail ON inbound_detail.batch_id=batch_record.batch_id AND inbound_detail.deleted_flag=0
          LEFT JOIN qms_inspection_order inspection_order ON inspection_order.batch_id=batch_record.batch_id
            AND inspection_order.inspection_type='FINAL' AND inspection_order.inspection_result='PASS' AND inspection_order.deleted_flag=0
          WHERE product_lot.deleted_flag=0 AND product_lot.status='STORED'
            AND (batch_record.batch_id IS NULL OR inbound_detail.detail_id IS NULL OR inspection_order.inspection_id IS NULL
              OR inbound_detail.inbound_quantity<>product_lot.qualified_quantity))
  UNION ALL SELECT 'quality', 'product lot quantity mismatch', 0,
         (SELECT COUNT(*) FROM trace_product_lot WHERE deleted_flag=0
          AND batch_quantity<>qualified_quantity+defect_quantity)
  UNION ALL SELECT 'equipment', 'final equipment state mismatch', 0,
         (SELECT IF(SUM(status='RUNNING')=10 AND SUM(status='IDLE')=2
           AND SUM(status NOT IN ('RUNNING','IDLE'))=0,0,1) FROM eam_equipment WHERE deleted_flag=0)
  UNION ALL SELECT 'equipment', 'andon closure duration mismatch', 0,
         (SELECT COUNT(*) FROM mfg_andon_alert WHERE deleted_flag=0
          AND (status<>'CLOSED' OR TIMESTAMPDIFF(MINUTE,trigger_time,resolved_time)<>12))
  UNION ALL SELECT 'equipment', 'repair closure mismatch', 0,
         (SELECT COUNT(*) FROM eam_repair_order WHERE deleted_flag=0
          AND (status<>'CLOSED' OR repair_cost<>260 OR TIMESTAMPDIFF(MINUTE,reported_time,updated_time)<>45))
  UNION ALL SELECT 'trace', 'report material trace mismatch', 0,
         ((SELECT COUNT(*) FROM trace_material_usage usage_record
           LEFT JOIN mfg_report_material report_material ON report_material.report_id=usage_record.report_id
             AND report_material.material_id=usage_record.material_id AND report_material.batch_id=usage_record.batch_id
             AND report_material.used_quantity=usage_record.used_quantity AND report_material.deleted_flag=0
           WHERE usage_record.deleted_flag=0 AND report_material.id IS NULL)
          +(SELECT COUNT(*) FROM mfg_report_material report_material
            LEFT JOIN trace_material_usage usage_record ON usage_record.report_id=report_material.report_id
              AND usage_record.material_id=report_material.material_id AND usage_record.batch_id=report_material.batch_id
              AND usage_record.used_quantity=report_material.used_quantity AND usage_record.deleted_flag=0
            WHERE report_material.deleted_flag=0 AND usage_record.usage_id IS NULL))
  UNION ALL SELECT 'trace', 'lot barcode cardinality mismatch', 0,
         (SELECT COUNT(*) FROM (
           SELECT product_lot.product_lot_id,COUNT(barcode.barcode_id) barcode_count
           FROM trace_product_lot product_lot LEFT JOIN trace_barcode barcode
             ON barcode.biz_type='TRACE_PRODUCT_LOT' AND barcode.biz_id=product_lot.product_lot_id
             AND barcode.barcode_type='PRODUCT_LOT' AND barcode.deleted_flag=0
           WHERE product_lot.deleted_flag=0 GROUP BY product_lot.product_lot_id HAVING barcode_count<>1
         ) invalid_lot)
  UNION ALL SELECT 'trace', 'outer carton barcode quantity mismatch', 0,
         (SELECT ABS((SELECT COUNT(*) FROM trace_barcode WHERE barcode_type='OUTER_CARTON' AND deleted_flag=0)
           -(SELECT COALESCE(SUM(CEILING(qualified_quantity/25)),0) FROM trace_product_lot
             WHERE status='STORED' AND deleted_flag=0)))

  -- 智能排产边界和审计覆盖。
  UNION ALL SELECT 'boundary', 'draft orders with formal plan', 0,
         (SELECT COUNT(*) FROM mfg_production_order production_order
          JOIN mfg_production_plan plan ON plan.production_order_id=production_order.production_order_id AND plan.deleted_flag=0
          WHERE production_order.status='DRAFT' AND production_order.deleted_flag=0)
  UNION ALL SELECT 'boundary', 'seed-marked schedule rows', 0,
         ((SELECT COUNT(*) FROM mfg_schedule_scenario
           WHERE remark LIKE CONCAT(@transaction_marker,'%') AND deleted_flag=0)
          +(SELECT COUNT(*) FROM mfg_schedule_item
            WHERE remark LIKE CONCAT(@transaction_marker,'%') AND deleted_flag=0))
  UNION ALL SELECT 'audit', 'status log coverage', 731,
         (SELECT COUNT(*) FROM sys_status_change_log WHERE remark=@transaction_marker AND deleted_flag=0)
  UNION ALL SELECT 'audit', 'approval record coverage', 329,
         (SELECT COUNT(*) FROM sys_approval_record WHERE remark=@transaction_marker AND deleted_flag=0)
  UNION ALL SELECT 'audit', 'operation log coverage including marker', 732,
         (SELECT COUNT(*) FROM sys_operation_log WHERE remark LIKE CONCAT(@transaction_marker,'%') AND deleted_flag=0)
  UNION ALL SELECT 'audit', 'invalid business status codes', 0,
         ((SELECT COUNT(*) FROM mfg_production_order WHERE deleted_flag=0 AND status NOT IN ('DRAFT','RELEASED','RUNNING','FINISHED'))
          +(SELECT COUNT(*) FROM mfg_production_plan WHERE deleted_flag=0 AND status NOT IN ('RELEASED','RUNNING','FINISHED'))
          +(SELECT COUNT(*) FROM mfg_work_order WHERE deleted_flag=0 AND status NOT IN ('RELEASED','RUNNING','FINISHED'))
          +(SELECT COUNT(*) FROM mfg_task WHERE deleted_flag=0 AND status NOT IN ('WAIT_START','RUNNING','FINISHED'))
          +(SELECT COUNT(*) FROM qms_inspection_order WHERE deleted_flag=0 AND status<>'FINISHED')
          +(SELECT COUNT(*) FROM qms_nonconforming_order WHERE deleted_flag=0
            AND (approval_status<>'APPROVED' OR status<>'CLOSED')))
) checks
ORDER BY FIELD(checks.`check_group`, 'marker', 'count', 'production', 'inventory', 'quality', 'equipment', 'trace', 'boundary', 'audit'),
         checks.`check_name`;
