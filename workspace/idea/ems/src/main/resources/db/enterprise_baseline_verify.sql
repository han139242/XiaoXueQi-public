-- 校验 enterprise_baseline_data.sql 的最终基线数量、类型、关联和业务边界。
-- failed_checks 必须在每一行都为 0；初次落库验收时交易数据也必须为 0。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @baseline_marker = 'ENTERPRISE_BASELINE_FINAL';

SELECT checks.`check_group`, checks.`check_name`, checks.`expected_value`, checks.`actual_value`,
       IF(checks.`actual_value` = checks.`expected_value`, 'PASS', 'FAIL') AS `result`,
       SUM(IF(checks.`actual_value` = checks.`expected_value`, 0, 1)) OVER () AS `failed_checks`
FROM (
  -- 字段类型
  SELECT 'schema' AS `check_group`, 'org_shift.start_time TIME' AS `check_name`, 1 AS `expected_value`,
         (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'start_time' AND DATA_TYPE = 'time') AS `actual_value`
  UNION ALL SELECT 'schema', 'org_shift.end_time TIME', 1,
         (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_shift' AND COLUMN_NAME = 'end_time' AND DATA_TYPE = 'time')
  UNION ALL SELECT 'schema', 'process_operation.standard_time DECIMAL(10,2)', 1,
         (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_operation' AND COLUMN_NAME = 'standard_time' AND COLUMN_TYPE = 'decimal(10,2)')
  UNION ALL SELECT 'schema', 'process_route_item.standard_time DECIMAL(10,2)', 1,
         (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'process_route_item' AND COLUMN_NAME = 'standard_time' AND COLUMN_TYPE = 'decimal(10,2)')

  -- 基线记录数量，只统计带最终基线标识或明确关联到基线编码的记录。
  UNION ALL SELECT 'count', 'org_department', 6, (SELECT COUNT(*) FROM org_department WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'org_employee', 9, (SELECT COUNT(*) FROM org_employee WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'org_team', 2, (SELECT COUNT(*) FROM org_team WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'org_team_member', 4,
         (SELECT COUNT(*) FROM org_team_member member JOIN org_team team ON team.team_id = member.team_id JOIN org_employee employee ON employee.employee_id = member.employee_id WHERE member.deleted_flag = 0 AND team.deleted_flag=0 AND employee.deleted_flag=0 AND team.remark LIKE CONCAT(@baseline_marker, ':%') AND employee.remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'org_shift', 3, (SELECT COUNT(*) FROM org_shift WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'mdm_unit', 13, (SELECT COUNT(*) FROM mdm_unit WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'mdm_supplier', 3, (SELECT COUNT(*) FROM mdm_supplier WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'mdm_customer', 2, (SELECT COUNT(*) FROM mdm_customer WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'mdm_material_category', 6, (SELECT COUNT(*) FROM mdm_material_category WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'mdm_material', 11, (SELECT COUNT(*) FROM mdm_material WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'mdm_product', 2, (SELECT COUNT(*) FROM mdm_product WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'mdm_bom', 2, (SELECT COUNT(*) FROM mdm_bom WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'mdm_bom_item', 12, (SELECT COUNT(*) FROM mdm_bom_item WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'process_operation', 7, (SELECT COUNT(*) FROM process_operation WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'process_parameter', 5, (SELECT COUNT(*) FROM process_parameter WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'process_route', 2, (SELECT COUNT(*) FROM process_route WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'process_route_item', 14, (SELECT COUNT(*) FROM process_route_item WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'factory_workshop', 1, (SELECT COUNT(*) FROM factory_workshop WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'factory_line', 2, (SELECT COUNT(*) FROM factory_line WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'factory_station', 14, (SELECT COUNT(*) FROM factory_station WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'factory_station_operation', 14,
         (SELECT COUNT(*) FROM factory_station_operation relation JOIN factory_station station ON station.station_id = relation.station_id WHERE relation.deleted_flag = 0 AND station.deleted_flag=0 AND station.remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'wms_warehouse', 4, (SELECT COUNT(*) FROM wms_warehouse WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'wms_location', 6, (SELECT COUNT(*) FROM wms_location WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'eam_equipment_category', 6, (SELECT COUNT(*) FROM eam_equipment_category WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'eam_equipment', 12, (SELECT COUNT(*) FROM eam_equipment WHERE deleted_flag = 0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'qms_inspection_item', 7, (SELECT COUNT(*) FROM qms_inspection_item WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'qms_inspection_standard', 14, (SELECT COUNT(*) FROM qms_inspection_standard WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'qms_inspection_scheme', 6, (SELECT COUNT(*) FROM qms_inspection_scheme WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))
  UNION ALL SELECT 'count', 'qms_inspection_scheme_item', 14, (SELECT COUNT(*) FROM qms_inspection_scheme_item WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%'))

  -- 逻辑外键完整性
  UNION ALL SELECT 'reference', 'organization orphan rows', 0,
    (SELECT
      (SELECT COUNT(*) FROM org_department t LEFT JOIN org_department r ON r.department_id=t.parent_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND t.parent_id IS NOT NULL AND r.department_id IS NULL)
      + (SELECT COUNT(*) FROM org_employee t LEFT JOIN org_department r ON r.department_id=t.department_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.department_id IS NULL)
      + (SELECT COUNT(*) FROM org_team t LEFT JOIN org_department r ON r.department_id=t.department_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.department_id IS NULL)
      + (SELECT COUNT(*) FROM org_team_member t LEFT JOIN org_team r ON r.team_id=t.team_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.team_id IS NULL)
      + (SELECT COUNT(*) FROM org_team_member t LEFT JOIN org_employee r ON r.employee_id=t.employee_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.employee_id IS NULL))
  UNION ALL SELECT 'reference', 'master data orphan rows', 0,
    (SELECT
      (SELECT COUNT(*) FROM mdm_material t LEFT JOIN mdm_material_category r ON r.category_id=t.category_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.category_id IS NULL)
      + (SELECT COUNT(*) FROM mdm_material t LEFT JOIN mdm_unit r ON r.unit_id=t.unit_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.unit_id IS NULL)
      + (SELECT COUNT(*) FROM mdm_product t LEFT JOIN mdm_material r ON r.material_id=t.material_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.material_id IS NULL)
      + (SELECT COUNT(*) FROM mdm_bom t LEFT JOIN mdm_product r ON r.product_id=t.product_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.product_id IS NULL)
      + (SELECT COUNT(*) FROM mdm_bom_item t LEFT JOIN mdm_bom r ON r.bom_id=t.bom_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.bom_id IS NULL)
      + (SELECT COUNT(*) FROM mdm_bom_item t LEFT JOIN mdm_material r ON r.material_id=t.material_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.material_id IS NULL))
  UNION ALL SELECT 'reference', 'process/factory orphan rows', 0,
    (SELECT
      (SELECT COUNT(*) FROM process_parameter t LEFT JOIN process_operation r ON r.operation_id=t.operation_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.operation_id IS NULL)
      + (SELECT COUNT(*) FROM process_parameter t LEFT JOIN mdm_unit r ON r.unit_id=t.unit_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.unit_id IS NULL)
      + (SELECT COUNT(*) FROM process_route t LEFT JOIN mdm_product r ON r.product_id=t.product_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.product_id IS NULL)
      + (SELECT COUNT(*) FROM process_route_item t LEFT JOIN process_route r ON r.route_id=t.route_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.route_id IS NULL)
      + (SELECT COUNT(*) FROM process_route_item t LEFT JOIN process_operation r ON r.operation_id=t.operation_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.operation_id IS NULL)
      + (SELECT COUNT(*) FROM factory_line t LEFT JOIN factory_workshop r ON r.workshop_id=t.workshop_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.workshop_id IS NULL)
      + (SELECT COUNT(*) FROM factory_station t LEFT JOIN factory_line r ON r.line_id=t.line_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.line_id IS NULL)
      + (SELECT COUNT(*) FROM factory_station_operation t LEFT JOIN factory_station r ON r.station_id=t.station_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.station_id IS NULL)
      + (SELECT COUNT(*) FROM factory_station_operation t LEFT JOIN process_operation r ON r.operation_id=t.operation_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.operation_id IS NULL))
  UNION ALL SELECT 'reference', 'warehouse/equipment orphan rows', 0,
    (SELECT
      (SELECT COUNT(*) FROM wms_location t LEFT JOIN wms_warehouse r ON r.warehouse_id=t.warehouse_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.warehouse_id IS NULL)
      + (SELECT COUNT(*) FROM eam_equipment t LEFT JOIN eam_equipment_category r ON r.category_id=t.category_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.category_id IS NULL)
      + (SELECT COUNT(*) FROM eam_equipment t LEFT JOIN factory_station r ON r.station_id=t.station_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.station_id IS NULL))
  UNION ALL SELECT 'reference', 'quality orphan rows', 0,
    (SELECT
      (SELECT COUNT(*) FROM qms_inspection_item t LEFT JOIN mdm_unit r ON r.unit_id=t.unit_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND t.unit_id IS NOT NULL AND r.unit_id IS NULL)
      + (SELECT COUNT(*) FROM qms_inspection_standard t LEFT JOIN qms_inspection_item r ON r.item_id=t.item_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.item_id IS NULL)
      + (SELECT COUNT(*) FROM qms_inspection_standard t LEFT JOIN mdm_product r ON r.product_id=t.product_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.product_id IS NULL)
      + (SELECT COUNT(*) FROM qms_inspection_scheme t LEFT JOIN mdm_product r ON r.product_id=t.product_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.product_id IS NULL)
      + (SELECT COUNT(*) FROM qms_inspection_scheme_item t LEFT JOIN qms_inspection_scheme r ON r.scheme_id=t.scheme_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.scheme_id IS NULL)
      + (SELECT COUNT(*) FROM qms_inspection_scheme_item t LEFT JOIN qms_inspection_standard r ON r.standard_id=t.standard_id AND r.deleted_flag=0 WHERE t.deleted_flag=0 AND r.standard_id IS NULL))

  -- 关键业务规则
  UNION ALL SELECT 'rule', 'active blank equipment', 0,
         (SELECT COUNT(*) FROM eam_equipment WHERE deleted_flag=0 AND (equipment_code IS NULL OR equipment_name IS NULL OR category_id IS NULL OR station_id IS NULL))
  UNION ALL SELECT 'rule', 'invalid BOM values', 0,
         (SELECT COUNT(*) FROM mdm_bom_item item JOIN mdm_bom bom ON bom.bom_id=item.bom_id WHERE item.deleted_flag=0 AND bom.remark LIKE CONCAT(@baseline_marker, ':%') AND (item.quantity <= 0 OR item.loss_rate < 0 OR item.loss_rate >= 1))
  UNION ALL SELECT 'rule', 'BOM item count not 6', 0,
         (SELECT COUNT(*) FROM (SELECT bom.bom_id FROM mdm_bom bom LEFT JOIN mdm_bom_item item ON item.bom_id=bom.bom_id AND item.deleted_flag=0 WHERE bom.deleted_flag=0 AND bom.remark LIKE CONCAT(@baseline_marker, ':%') GROUP BY bom.bom_id HAVING COUNT(item.bom_item_id) <> 6) invalid_bom)
  UNION ALL SELECT 'rule', 'route item count not 7', 0,
         (SELECT COUNT(*) FROM (SELECT route.route_id FROM process_route route LEFT JOIN process_route_item item ON item.route_id=route.route_id AND item.deleted_flag=0 WHERE route.deleted_flag=0 AND route.remark LIKE CONCAT(@baseline_marker, ':%') GROUP BY route.route_id HAVING COUNT(item.route_item_id) <> 7 OR COUNT(DISTINCT item.sequence_no) <> 7) invalid_route)
  UNION ALL SELECT 'rule', 'non-positive standard time', 0,
         (SELECT COUNT(*) FROM process_route_item WHERE deleted_flag=0 AND remark LIKE CONCAT(@baseline_marker, ':%') AND (standard_time IS NULL OR standard_time <= 0))
  UNION ALL SELECT 'rule', 'line capacity exceeds route bottleneck', 0,
         (SELECT COUNT(*) FROM (
           SELECT line.line_id
           FROM (SELECT 'LINE-TRAIN-01' AS line_code, 'ROUTE-TRAIN-77-V1' AS route_code UNION ALL SELECT 'LINE-MATCH-01', 'ROUTE-MATCH-77-V1') mapping
           JOIN factory_line line ON line.line_code=mapping.line_code AND line.deleted_flag=0
           JOIN process_route route ON route.route_code=mapping.route_code AND route.deleted_flag=0
           JOIN process_route_item item ON item.route_id=route.route_id AND item.deleted_flag=0
           GROUP BY line.line_id, line.daily_capacity
           HAVING line.daily_capacity > FLOOR(480 / MAX(item.standard_time))
         ) invalid_capacity)
  UNION ALL SELECT 'rule', 'inspection scheme/standard mismatch', 0,
         (SELECT COUNT(*) FROM qms_inspection_scheme_item detail
          JOIN qms_inspection_scheme scheme ON scheme.scheme_id=detail.scheme_id AND scheme.deleted_flag=0
          JOIN qms_inspection_standard standard ON standard.standard_id=detail.standard_id AND standard.deleted_flag=0
          WHERE detail.deleted_flag=0 AND scheme.remark LIKE CONCAT(@baseline_marker, ':%')
            AND (detail.item_id <> standard.item_id OR scheme.product_id <> standard.product_id OR NOT (scheme.operation_id <=> standard.operation_id)))
  UNION ALL SELECT 'rule', 'invalid final baseline status', 0,
         (SELECT
           (SELECT COUNT(*) FROM mdm_bom WHERE remark LIKE CONCAT(@baseline_marker, ':%') AND status <> 'RELEASED')
           + (SELECT COUNT(*) FROM process_route WHERE remark LIKE CONCAT(@baseline_marker, ':%') AND status <> 'RELEASED')
           + (SELECT COUNT(*) FROM qms_inspection_scheme WHERE remark LIKE CONCAT(@baseline_marker, ':%') AND status <> 'RELEASED')
           + (SELECT COUNT(*) FROM factory_line WHERE remark LIKE CONCAT(@baseline_marker, ':%') AND status <> 'RUNNING')
           + (SELECT COUNT(*) FROM wms_warehouse WHERE remark LIKE CONCAT(@baseline_marker, ':%') AND status <> 'ENABLE'))
  UNION ALL SELECT 'boundary', 'transaction rows after initial seed', 0,
         (SELECT
           (SELECT COUNT(*) FROM mfg_production_order WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM mfg_production_plan WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM mfg_work_order WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM mfg_dispatch_order WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM mfg_task WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM mfg_work_report WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM wms_material_batch WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM wms_inventory WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM wms_stock_movement WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM qms_inspection_order WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM qms_inspection_result WHERE deleted_flag=0)
           + (SELECT COUNT(*) FROM qms_nonconforming_order WHERE deleted_flag=0))
) checks
ORDER BY FIELD(checks.`check_group`, 'schema', 'count', 'reference', 'rule', 'boundary'), checks.`check_name`;
