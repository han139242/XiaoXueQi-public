-- 生产大屏演示快照 V1。
-- 目标：补齐扩展产线执行主数据，并将演示排期整理到 2026-07-17 至 2026-07-19。
-- 约束：脚本可重复执行；不删除业务数据，不修改库存余额，不伪造报工或质检结果。

SET NAMES utf8mb4;
START TRANSACTION;

SET @operator_id := 1;
SET @snapshot_remark := _utf8mb4'PRODUCTION_DASHBOARD_DEMO_SNAPSHOT_V1' COLLATE utf8mb4_general_ci;
SET @snapshot_date := DATE('2026-07-17');

SET @train_line_1 := (SELECT line_id FROM factory_line WHERE line_code = 'LINE-TRAIN-01' AND deleted_flag = 0 LIMIT 1);
SET @match_line_1 := (SELECT line_id FROM factory_line WHERE line_code = 'LINE-MATCH-01' AND deleted_flag = 0 LIMIT 1);
SET @train_line_2 := (SELECT line_id FROM factory_line WHERE line_code = 'LINE-TRAIN-02' AND deleted_flag = 0 LIMIT 1);
SET @match_line_2 := (SELECT line_id FROM factory_line WHERE line_code = 'LINE-MATCH-02' AND deleted_flag = 0 LIMIT 1);
SET @match_line_3 := (SELECT line_id FROM factory_line WHERE line_code = 'LINE-MATCH-03' AND deleted_flag = 0 LIMIT 1);

-- 扩展产线此前只有产能、产品能力和周历，补齐工位及工序能力。
INSERT INTO factory_station
    (station_code, station_name, line_id, station_type, status, remark, version,
     created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT CONCAT('ST-TR2-', RIGHT(src.station_code, 3)),
       REPLACE(src.station_name, '训练线', '训练二线'),
       @train_line_2, src.station_type, 'RUNNING', '训练球2号线标准工位', 0,
       @operator_id, NOW(), @operator_id, NOW(), 0
FROM factory_station src
WHERE src.line_id = @train_line_1
  AND src.deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM factory_station target
      WHERE target.station_code = CONCAT('ST-TR2-', RIGHT(src.station_code, 3))
  );

INSERT INTO factory_station
    (station_code, station_name, line_id, station_type, status, remark, version,
     created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT CONCAT('ST-MA2-', RIGHT(src.station_code, 3)),
       REPLACE(src.station_name, '比赛线', '比赛二线'),
       @match_line_2, src.station_type, 'RUNNING', '比赛球2号线标准工位', 0,
       @operator_id, NOW(), @operator_id, NOW(), 0
FROM factory_station src
WHERE src.line_id = @match_line_1
  AND src.deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM factory_station target
      WHERE target.station_code = CONCAT('ST-MA2-', RIGHT(src.station_code, 3))
  );

INSERT INTO factory_station
    (station_code, station_name, line_id, station_type, status, remark, version,
     created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT CONCAT('ST-MA3-', RIGHT(src.station_code, 3)),
       REPLACE(src.station_name, '比赛线', '比赛三线'),
       @match_line_3, src.station_type, 'MAINTAINING', '比赛球3号线标准工位，当前计划保养', 0,
       @operator_id, NOW(), @operator_id, NOW(), 0
FROM factory_station src
WHERE src.line_id = @match_line_1
  AND src.deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM factory_station target
      WHERE target.station_code = CONCAT('ST-MA3-', RIGHT(src.station_code, 3))
  );

INSERT INTO factory_station_operation
    (station_id, operation_id, is_default, status, created_by, created_time, deleted_flag)
SELECT target.station_id, source_binding.operation_id, source_binding.is_default,
       'ENABLE', @operator_id, NOW(), 0
FROM factory_station source_station
JOIN factory_station_operation source_binding
  ON source_binding.station_id = source_station.station_id AND source_binding.deleted_flag = 0
JOIN factory_station target
  ON target.station_code = CONCAT('ST-TR2-', RIGHT(source_station.station_code, 3))
WHERE source_station.line_id = @train_line_1
  AND source_station.deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM factory_station_operation existing
      WHERE existing.station_id = target.station_id
        AND existing.operation_id = source_binding.operation_id
  );

INSERT INTO factory_station_operation
    (station_id, operation_id, is_default, status, created_by, created_time, deleted_flag)
SELECT target.station_id, source_binding.operation_id, source_binding.is_default,
       'ENABLE', @operator_id, NOW(), 0
FROM factory_station source_station
JOIN factory_station_operation source_binding
  ON source_binding.station_id = source_station.station_id AND source_binding.deleted_flag = 0
JOIN factory_station target
  ON target.station_code = CONCAT('ST-MA2-', RIGHT(source_station.station_code, 3))
WHERE source_station.line_id = @match_line_1
  AND source_station.deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM factory_station_operation existing
      WHERE existing.station_id = target.station_id
        AND existing.operation_id = source_binding.operation_id
  );

INSERT INTO factory_station_operation
    (station_id, operation_id, is_default, status, created_by, created_time, deleted_flag)
SELECT target.station_id, source_binding.operation_id, source_binding.is_default,
       'ENABLE', @operator_id, NOW(), 0
FROM factory_station source_station
JOIN factory_station_operation source_binding
  ON source_binding.station_id = source_station.station_id AND source_binding.deleted_flag = 0
JOIN factory_station target
  ON target.station_code = CONCAT('ST-MA3-', RIGHT(source_station.station_code, 3))
WHERE source_station.line_id = @match_line_1
  AND source_station.deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM factory_station_operation existing
      WHERE existing.station_id = target.station_id
        AND existing.operation_id = source_binding.operation_id
  );

-- 每条线复制 6 台对应工艺设备；人工分选工位不虚构设备。
INSERT INTO eam_equipment
    (equipment_code, equipment_name, category_id, model, manufacturer, purchase_date,
     station_id, status, remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT REPLACE(src.equipment_code, 'EQ-TR-', 'EQ-TR2-'),
       REPLACE(src.equipment_name, '训练线', '训练二线'),
       src.category_id, src.model, src.manufacturer, DATE('2025-09-15'),
       target.station_id, 'IDLE', '训练球2号线扩建设备，等待生产计划', 0,
       @operator_id, NOW(), @operator_id, NOW(), 0
FROM eam_equipment src
JOIN factory_station source_station ON source_station.station_id = src.station_id
JOIN factory_station target ON target.station_code = CONCAT('ST-TR2-', RIGHT(source_station.station_code, 3))
WHERE source_station.line_id = @train_line_1
  AND src.deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM eam_equipment existing
      WHERE existing.equipment_code = REPLACE(src.equipment_code, 'EQ-TR-', 'EQ-TR2-')
  );

INSERT INTO eam_equipment
    (equipment_code, equipment_name, category_id, model, manufacturer, purchase_date,
     station_id, status, remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT REPLACE(src.equipment_code, 'EQ-MA-', 'EQ-MA2-'),
       REPLACE(src.equipment_name, '比赛线', '比赛二线'),
       src.category_id, src.model, src.manufacturer, DATE('2025-11-20'),
       target.station_id, 'IDLE', '比赛球2号线扩建设备，等待生产计划', 0,
       @operator_id, NOW(), @operator_id, NOW(), 0
FROM eam_equipment src
JOIN factory_station source_station ON source_station.station_id = src.station_id
JOIN factory_station target ON target.station_code = CONCAT('ST-MA2-', RIGHT(source_station.station_code, 3))
WHERE source_station.line_id = @match_line_1
  AND src.deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM eam_equipment existing
      WHERE existing.equipment_code = REPLACE(src.equipment_code, 'EQ-MA-', 'EQ-MA2-')
  );

INSERT INTO eam_equipment
    (equipment_code, equipment_name, category_id, model, manufacturer, purchase_date,
     station_id, status, remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT REPLACE(src.equipment_code, 'EQ-MA-', 'EQ-MA3-'),
       REPLACE(src.equipment_name, '比赛线', '比赛三线'),
       src.category_id, src.model, src.manufacturer, DATE('2026-01-12'),
       target.station_id, 'MAINTAINING', '比赛球3号线周保养', 0,
       @operator_id, NOW(), @operator_id, NOW(), 0
FROM eam_equipment src
JOIN factory_station source_station ON source_station.station_id = src.station_id
JOIN factory_station target ON target.station_code = CONCAT('ST-MA3-', RIGHT(source_station.station_code, 3))
WHERE source_station.line_id = @match_line_1
  AND src.deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM eam_equipment existing
      WHERE existing.equipment_code = REPLACE(src.equipment_code, 'EQ-MA-', 'EQ-MA3-')
  );

-- 产线状态表达设备可用性；无当前计划的可用产线由大屏显示为待机/待开工。
INSERT INTO sys_status_change_log
    (biz_type, biz_id, old_status, new_status, change_reason, changed_by, changed_time,
     remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT 'FACTORY_LINE', line_id, status, 'MAINTAINING', 'SCHEDULED_MAINTENANCE',
       @operator_id, NOW(), @snapshot_remark, 0, @operator_id, NOW(), @operator_id, NOW(), 0
FROM factory_line
WHERE line_id = @match_line_3
  AND deleted_flag = 0
  AND status <> 'MAINTAINING'
  AND NOT EXISTS (
      SELECT 1 FROM sys_status_change_log log
      WHERE log.biz_type = 'FACTORY_LINE'
        AND log.biz_id = @match_line_3
        AND log.remark = @snapshot_remark
        AND log.deleted_flag = 0
  );

UPDATE factory_line
SET status = 'RUNNING', remark = '扩展产线已完成工位、工序、设备及排产能力配置',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE line_id IN (@train_line_2, @match_line_2)
  AND deleted_flag = 0
  AND (status <> 'RUNNING' OR remark <> '扩展产线已完成工位、工序、设备及排产能力配置');

UPDATE factory_line
SET status = 'MAINTAINING', remark = '计划保养至2026-07-17 16:00，保养完成后恢复待机',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE line_id = @match_line_3
  AND deleted_flag = 0
  AND (status <> 'MAINTAINING' OR remark <> '计划保养至2026-07-17 16:00，保养完成后恢复待机');

UPDATE factory_station
SET status = 'MAINTAINING', remark = '比赛球3号线计划保养',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE line_id = @match_line_3
  AND deleted_flag = 0
  AND (status <> 'MAINTAINING' OR remark <> '比赛球3号线计划保养');

UPDATE eam_equipment e
JOIN factory_station s ON s.station_id = e.station_id AND s.deleted_flag = 0
SET e.status = 'MAINTAINING', e.remark = '比赛球3号线周保养',
    e.version = e.version + 1, e.updated_by = @operator_id, e.updated_time = NOW()
WHERE s.line_id = @match_line_3
  AND e.deleted_flag = 0
  AND (e.status <> 'MAINTAINING' OR e.remark <> '比赛球3号线周保养');

UPDATE mfg_line_product_capability
SET remark = '扩展产线产品与已发布工艺路线能力',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE line_id IN (@train_line_2, @match_line_2, @match_line_3)
  AND deleted_flag = 0
  AND remark <> '扩展产线产品与已发布工艺路线能力';

-- 修复误启动但从未报工的工单，恢复到待开工状态并保留状态日志。
INSERT INTO sys_status_change_log
    (biz_type, biz_id, old_status, new_status, change_reason, changed_by, changed_time,
     remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT 'MFG_TASK', task.task_id, task.status, 'WAIT_START', 'REPAIR_ACCIDENTAL_START_WITHOUT_REPORT',
       @operator_id, NOW(), @snapshot_remark, 0, @operator_id, NOW(), @operator_id, NOW(), 0
FROM mfg_task task
JOIN mfg_work_order work_order ON work_order.work_order_id = task.work_order_id
JOIN mfg_production_plan plan ON plan.plan_id = work_order.plan_id
WHERE plan.plan_no = 'PP-20260711-014'
  AND task.deleted_flag = 0
  AND task.status = 'RUNNING'
  AND NOT EXISTS (SELECT 1 FROM mfg_work_report report WHERE report.task_id = task.task_id AND report.deleted_flag = 0)
  AND NOT EXISTS (
      SELECT 1 FROM sys_status_change_log log
      WHERE log.biz_type = 'MFG_TASK' AND log.biz_id = task.task_id
        AND log.remark = @snapshot_remark AND log.deleted_flag = 0
  );

INSERT INTO sys_status_change_log
    (biz_type, biz_id, old_status, new_status, change_reason, changed_by, changed_time,
     remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT 'MFG_WORK_ORDER', work_order.work_order_id, work_order.status, 'RELEASED',
       'REPAIR_ACCIDENTAL_START_WITHOUT_REPORT', @operator_id, NOW(), @snapshot_remark,
       0, @operator_id, NOW(), @operator_id, NOW(), 0
FROM mfg_work_order work_order
JOIN mfg_production_plan plan ON plan.plan_id = work_order.plan_id
WHERE plan.plan_no = 'PP-20260711-014'
  AND work_order.deleted_flag = 0
  AND work_order.status = 'RUNNING'
  AND NOT EXISTS (
      SELECT 1 FROM sys_status_change_log log
      WHERE log.biz_type = 'MFG_WORK_ORDER' AND log.biz_id = work_order.work_order_id
        AND log.remark = @snapshot_remark AND log.deleted_flag = 0
  );

UPDATE mfg_task task
JOIN mfg_work_order work_order ON work_order.work_order_id = task.work_order_id
JOIN mfg_production_plan plan ON plan.plan_id = work_order.plan_id
SET task.status = 'WAIT_START', task.version = task.version + 1,
    task.updated_by = @operator_id, task.updated_time = NOW()
WHERE plan.plan_no = 'PP-20260711-014'
  AND task.deleted_flag = 0
  AND task.status = 'RUNNING'
  AND NOT EXISTS (SELECT 1 FROM mfg_work_report report WHERE report.task_id = task.task_id AND report.deleted_flag = 0);

UPDATE mfg_work_order work_order
JOIN mfg_production_plan plan ON plan.plan_id = work_order.plan_id
SET work_order.status = 'RELEASED', work_order.version = work_order.version + 1,
    work_order.updated_by = @operator_id, work_order.updated_time = NOW()
WHERE plan.plan_no = 'PP-20260711-014'
  AND work_order.deleted_flag = 0
  AND work_order.status = 'RUNNING'
  AND work_order.completed_quantity = 0
  AND NOT EXISTS (
      SELECT 1 FROM mfg_work_report report
      JOIN mfg_task task ON task.task_id = report.task_id
      WHERE task.work_order_id = work_order.work_order_id AND report.deleted_flag = 0
  );

-- 重新排定既有计划：在制计划跨班续产，其余按产线能力分散到后续白班。
UPDATE mfg_production_plan
SET planned_start_time = DATE_SUB(@snapshot_date, INTERVAL 12 HOUR),
    planned_end_time = DATE_ADD(@snapshot_date, INTERVAL 16 HOUR),
    remark = '跨班续产，当前工序为速度与飞行检测，夜间不连续加工',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE plan_no = 'PP-20260710-003' AND deleted_flag = 0
  AND (planned_start_time <> DATE_SUB(@snapshot_date, INTERVAL 12 HOUR)
       OR planned_end_time <> DATE_ADD(@snapshot_date, INTERVAL 16 HOUR)
       OR remark <> '跨班续产，当前工序为速度与飞行检测，夜间不连续加工');

UPDATE mfg_production_order
SET delivery_date = DATE_ADD(@snapshot_date, INTERVAL 1 DAY),
    remark = '跨班续产，计划员确认交付顺延至2026-07-18',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE production_order_no = 'MO-20260710-003'
  AND deleted_flag = 0
  AND (delivery_date <> DATE_ADD(@snapshot_date, INTERVAL 1 DAY)
       OR remark <> '跨班续产，计划员确认交付顺延至2026-07-18');

UPDATE mfg_production_plan
SET planned_start_time = DATE_ADD(@snapshot_date, INTERVAL 1 DAY) + INTERVAL 8 HOUR,
    planned_end_time = DATE_ADD(@snapshot_date, INTERVAL 1 DAY) + INTERVAL 16 HOUR,
    remark = '已纠正误启动记录，等待白班开工确认',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE plan_no = 'PP-20260711-014' AND deleted_flag = 0
  AND (planned_start_time <> DATE_ADD(@snapshot_date, INTERVAL 1 DAY) + INTERVAL 8 HOUR
       OR planned_end_time <> DATE_ADD(@snapshot_date, INTERVAL 1 DAY) + INTERVAL 16 HOUR
       OR remark <> '已纠正误启动记录，等待白班开工确认');

UPDATE mfg_production_order
SET delivery_date = DATE_ADD(@snapshot_date, INTERVAL 3 DAY),
    remark = '误启动记录已纠正，计划员确认交付顺延至2026-07-20',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE production_order_no = 'MO-20260711-014'
  AND deleted_flag = 0
  AND (delivery_date <> DATE_ADD(@snapshot_date, INTERVAL 3 DAY)
       OR remark <> '误启动记录已纠正，计划员确认交付顺延至2026-07-20');

UPDATE mfg_production_plan
SET line_id = @train_line_2,
    planned_start_time = @snapshot_date + INTERVAL 8 HOUR,
    planned_end_time = @snapshot_date + INTERVAL 16 HOUR,
    remark = '训练球2号线首批量产，物料齐套后开工',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE plan_no = 'PP-20260712-015' AND deleted_flag = 0
  AND (line_id <> @train_line_2
       OR planned_start_time <> @snapshot_date + INTERVAL 8 HOUR
       OR planned_end_time <> @snapshot_date + INTERVAL 16 HOUR
       OR remark <> '训练球2号线首批量产，物料齐套后开工');

UPDATE mfg_production_plan
SET line_id = @train_line_2,
    planned_start_time = DATE_ADD(@snapshot_date, INTERVAL 3 DAY) + INTERVAL 8 HOUR,
    planned_end_time = DATE_ADD(@snapshot_date, INTERVAL 3 DAY) + INTERVAL 16 HOUR,
    remark = '训练球2号线周一白班计划',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE plan_no = 'SP-6-1' AND deleted_flag = 0
  AND (line_id <> @train_line_2
       OR planned_start_time <> DATE_ADD(@snapshot_date, INTERVAL 3 DAY) + INTERVAL 8 HOUR
       OR planned_end_time <> DATE_ADD(@snapshot_date, INTERVAL 3 DAY) + INTERVAL 16 HOUR
       OR remark <> '训练球2号线周一白班计划');

UPDATE mfg_production_order
SET delivery_date = DATE_ADD(@snapshot_date, INTERVAL 4 DAY),
    remark = '周一白班生产，次日交付',
    version = version + 1, updated_by = @operator_id, updated_time = NOW()
WHERE production_order_no = 'MO-20260712-016'
  AND deleted_flag = 0
  AND (delivery_date <> DATE_ADD(@snapshot_date, INTERVAL 4 DAY)
       OR remark <> '周一白班生产，次日交付');

-- 计划 PP-20260712-015 尚未开工，将任务工位同步到新产线的同工序工位。
UPDATE mfg_task task
JOIN mfg_work_order work_order ON work_order.work_order_id = task.work_order_id
JOIN mfg_production_plan plan ON plan.plan_id = work_order.plan_id
JOIN factory_station_operation binding
  ON binding.operation_id = task.operation_id AND binding.deleted_flag = 0 AND binding.status = 'ENABLE'
JOIN factory_station station
  ON station.station_id = binding.station_id AND station.line_id = @train_line_2 AND station.deleted_flag = 0
SET task.station_id = station.station_id,
    task.version = task.version + 1, task.updated_by = @operator_id, task.updated_time = NOW()
WHERE plan.plan_no = 'PP-20260712-015'
  AND task.deleted_flag = 0
  AND task.status = 'WAIT_START'
  AND task.station_id <> station.station_id;

UPDATE mfg_dispatch_order dispatch_order
JOIN mfg_work_order work_order ON work_order.work_order_id = dispatch_order.work_order_id
JOIN mfg_production_plan plan ON plan.plan_id = work_order.plan_id
SET dispatch_order.due_time = plan.planned_end_time,
    dispatch_order.version = dispatch_order.version + 1,
    dispatch_order.updated_by = @operator_id, dispatch_order.updated_time = NOW()
WHERE plan.plan_no IN ('PP-20260711-014', 'PP-20260712-015')
  AND dispatch_order.deleted_flag = 0
  AND (dispatch_order.due_time IS NULL OR dispatch_order.due_time <> plan.planned_end_time);

-- 使用已有正式订单安排比赛球1号线两个白班。
SET @match_order_1 := (SELECT production_order_id FROM mfg_production_order
                       WHERE production_order_no = 'MO-20260713-025' AND deleted_flag = 0 LIMIT 1);

INSERT INTO sys_status_change_log
    (biz_type, biz_id, old_status, new_status, change_reason, changed_by, changed_time,
     remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT 'MFG_PRODUCTION_ORDER', production_order_id, status, 'RELEASED', 'RELEASE_FOR_DEMO_SCHEDULE',
       @operator_id, NOW(), @snapshot_remark, 0, @operator_id, NOW(), @operator_id, NOW(), 0
FROM mfg_production_order
WHERE production_order_id = @match_order_1 AND status = 'DRAFT' AND deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_status_change_log log
      WHERE log.biz_type = 'MFG_PRODUCTION_ORDER' AND log.biz_id = @match_order_1
        AND log.remark = @snapshot_remark AND log.deleted_flag = 0
  );

UPDATE mfg_production_order
SET status = 'RELEASED', version = version + 1,
    updated_by = @operator_id, updated_time = NOW()
WHERE production_order_id = @match_order_1 AND status = 'DRAFT' AND deleted_flag = 0;

INSERT INTO mfg_production_plan
    (plan_no, production_order_id, line_id, planned_start_time, planned_end_time,
     planned_quantity, status, remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT 'DEMO-PP-MATCH-01-A', @match_order_1, @match_line_1,
       @snapshot_date + INTERVAL 8 HOUR, @snapshot_date + INTERVAL 16 HOUR,
       500, 'RELEASED', '比赛球1号线第一白班计划', 0,
       @operator_id, NOW(), @operator_id, NOW(), 0
WHERE @match_order_1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM mfg_production_plan WHERE plan_no = 'DEMO-PP-MATCH-01-A');

INSERT INTO mfg_production_plan
    (plan_no, production_order_id, line_id, planned_start_time, planned_end_time,
     planned_quantity, status, remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT 'DEMO-PP-MATCH-01-B', @match_order_1, @match_line_1,
       DATE_ADD(@snapshot_date, INTERVAL 1 DAY) + INTERVAL 8 HOUR,
       DATE_ADD(@snapshot_date, INTERVAL 1 DAY) + INTERVAL 16 HOUR,
       500, 'RELEASED', '比赛球1号线第二白班计划', 0,
       @operator_id, NOW(), @operator_id, NOW(), 0
WHERE @match_order_1 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM mfg_production_plan WHERE plan_no = 'DEMO-PP-MATCH-01-B');

-- 为比赛球2号线建立一张可追溯到产品和交付日的演示订单，不生成虚假报工。
INSERT INTO mfg_production_order
    (production_order_no, customer_id, product_id, order_quantity, delivery_date,
     source_type, status, remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT 'DEMO-MO-MATCH-02-001', NULL, product_id, 500, DATE_ADD(@snapshot_date, INTERVAL 4 DAY),
       'ERP', 'RELEASED', '演示客户比赛球补库订单', 0,
       @operator_id, NOW(), @operator_id, NOW(), 0
FROM mdm_product
WHERE product_code = 'PROD-LNX-M700-78' AND deleted_flag = 0
  AND NOT EXISTS (
      SELECT 1 FROM mfg_production_order WHERE production_order_no = 'DEMO-MO-MATCH-02-001'
  );

SET @match_order_2 := (SELECT production_order_id FROM mfg_production_order
                       WHERE production_order_no = 'DEMO-MO-MATCH-02-001' AND deleted_flag = 0 LIMIT 1);

INSERT INTO mfg_production_plan
    (plan_no, production_order_id, line_id, planned_start_time, planned_end_time,
     planned_quantity, status, remark, version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT 'DEMO-PP-MATCH-02-001', @match_order_2, @match_line_2,
       DATE_ADD(@snapshot_date, INTERVAL 1 DAY) + INTERVAL 8 HOUR,
       DATE_ADD(@snapshot_date, INTERVAL 1 DAY) + INTERVAL 16 HOUR,
       500, 'RELEASED', '比赛球2号线白班计划，等待开工确认', 0,
       @operator_id, NOW(), @operator_id, NOW(), 0
WHERE @match_order_2 IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM mfg_production_plan WHERE plan_no = 'DEMO-PP-MATCH-02-001');

INSERT INTO sys_operation_log
    (user_id, operation_type, module_code, biz_id, operation_time, remark,
     version, created_by, created_time, updated_by, updated_time, deleted_flag)
SELECT @operator_id, 'DEMO_SNAPSHOT', 'PRODUCTION_DASHBOARD', NULL, NOW(), @snapshot_remark,
       0, @operator_id, NOW(), @operator_id, NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_operation_log
    WHERE module_code = 'PRODUCTION_DASHBOARD'
      AND operation_type = 'DEMO_SNAPSHOT'
      AND remark = @snapshot_remark
      AND deleted_flag = 0
);

COMMIT;

-- 执行后核对：5 条产线都应有 7 个工位、7 个工序能力；其中 4 条可生产，1 条计划保养。
SELECT line.line_code, line.line_name, line.status, line.daily_capacity,
       COUNT(DISTINCT station.station_id) AS station_count,
       COUNT(DISTINCT binding.id) AS operation_count,
       COUNT(DISTINCT equipment.equipment_id) AS equipment_count
FROM factory_line line
LEFT JOIN factory_station station ON station.line_id = line.line_id AND station.deleted_flag = 0
LEFT JOIN factory_station_operation binding ON binding.station_id = station.station_id AND binding.deleted_flag = 0
LEFT JOIN eam_equipment equipment ON equipment.station_id = station.station_id AND equipment.deleted_flag = 0
WHERE line.deleted_flag = 0
GROUP BY line.line_id, line.line_code, line.line_name, line.status, line.daily_capacity
ORDER BY line.line_id;

SELECT plan.plan_no, production_order.production_order_no, product.product_name,
       line.line_name, plan.planned_start_time, plan.planned_end_time,
       plan.planned_quantity, plan.status
FROM mfg_production_plan plan
JOIN mfg_production_order production_order
  ON production_order.production_order_id = plan.production_order_id AND production_order.deleted_flag = 0
JOIN mdm_product product ON product.product_id = production_order.product_id AND product.deleted_flag = 0
JOIN factory_line line ON line.line_id = plan.line_id AND line.deleted_flag = 0
WHERE plan.deleted_flag = 0
  AND plan.status IN ('RELEASED', 'RUNNING')
  AND plan.planned_end_time >= DATE_SUB(@snapshot_date, INTERVAL 1 DAY)
ORDER BY plan.planned_start_time, line.line_id;
