-- 校验 smart_scheduling_migration.sql 的表结构、基础映射和业务边界。
-- 每一行 result 都必须为 PASS，failed_checks 必须为 0。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @scheduling_marker = 'SMART_SCHEDULING_FOUNDATION';

SELECT checks.`check_group`, checks.`check_name`, checks.`expected_value`, checks.`actual_value`,
       IF(checks.`actual_value` = checks.`expected_value`, 'PASS', 'FAIL') AS `result`,
       SUM(IF(checks.`actual_value` = checks.`expected_value`, 0, 1)) OVER () AS `failed_checks`
FROM (
  SELECT 'schema' AS `check_group`, 'scheduling table count' AS `check_name`, 4 AS `expected_value`,
         (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
          AND TABLE_NAME IN ('mfg_line_product_capability', 'mfg_line_calendar_rule',
                             'mfg_schedule_scenario', 'mfg_schedule_item')) AS `actual_value`
  UNION ALL SELECT 'schema', 'capacity factor DECIMAL(5,4)', 2,
         (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
          AND TABLE_NAME IN ('mfg_line_product_capability', 'mfg_line_calendar_rule')
          AND COLUMN_NAME='capacity_factor' AND COLUMN_TYPE='decimal(5,4)')
  UNION ALL SELECT 'schema', 'schedule item logical foreign key indexes', 3,
         (SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
          AND TABLE_NAME='mfg_schedule_item'
          AND INDEX_NAME IN ('idx_mfg_schedule_item_production_order_id',
                             'idx_mfg_schedule_item_product_id', 'idx_mfg_schedule_item_route_id'))
  UNION ALL SELECT 'count', 'foundation line capabilities', 5,
         (SELECT COUNT(*) FROM mfg_line_product_capability WHERE deleted_flag=0
          AND remark LIKE CONCAT(@scheduling_marker, ':%'))
  UNION ALL SELECT 'count', 'foundation calendar rules', 12,
         (SELECT COUNT(*) FROM mfg_line_calendar_rule WHERE deleted_flag=0
          AND remark LIKE CONCAT(@scheduling_marker, ':%'))
  UNION ALL SELECT 'reference', 'capability orphan/mismatched rows', 0,
         (SELECT COUNT(*) FROM mfg_line_product_capability capability
          LEFT JOIN factory_line line ON line.line_id=capability.line_id AND line.deleted_flag=0
          LEFT JOIN mdm_product product ON product.product_id=capability.product_id AND product.deleted_flag=0
          LEFT JOIN process_route route ON route.route_id=capability.route_id AND route.deleted_flag=0
          WHERE capability.deleted_flag=0
            AND (line.line_id IS NULL OR product.product_id IS NULL OR route.route_id IS NULL
              OR route.product_id<>capability.product_id))
  UNION ALL SELECT 'reference', 'calendar orphan rows', 0,
         (SELECT COUNT(*) FROM mfg_line_calendar_rule calendar_rule
          LEFT JOIN factory_line line ON line.line_id=calendar_rule.line_id AND line.deleted_flag=0
          LEFT JOIN org_shift shift_record ON shift_record.shift_id=calendar_rule.shift_id AND shift_record.deleted_flag=0
          WHERE calendar_rule.deleted_flag=0 AND (line.line_id IS NULL OR shift_record.shift_id IS NULL))
  UNION ALL SELECT 'reference', 'scenario/item orphan rows', 0,
         (SELECT
           (SELECT COUNT(*) FROM mfg_schedule_item item
            LEFT JOIN mfg_schedule_scenario scenario ON scenario.scenario_id=item.scenario_id AND scenario.deleted_flag=0
            WHERE item.deleted_flag=0 AND scenario.scenario_id IS NULL)
           + (SELECT COUNT(*) FROM mfg_schedule_item item
              LEFT JOIN mfg_production_order production_order ON production_order.production_order_id=item.production_order_id AND production_order.deleted_flag=0
              LEFT JOIN mdm_product product ON product.product_id=item.product_id AND product.deleted_flag=0
              LEFT JOIN process_route route ON route.route_id=item.route_id AND route.deleted_flag=0
              LEFT JOIN factory_line line ON line.line_id=item.line_id AND line.deleted_flag=0
              LEFT JOIN mfg_production_plan production_plan ON production_plan.plan_id=item.plan_id AND production_plan.deleted_flag=0
              WHERE item.deleted_flag=0 AND (production_order.production_order_id IS NULL OR product.product_id IS NULL
                OR (item.route_id IS NOT NULL AND route.route_id IS NULL)
                OR (item.route_id IS NOT NULL AND route.product_id<>item.product_id)
                OR (item.line_id IS NOT NULL AND line.line_id IS NULL)
                OR (item.plan_id IS NOT NULL AND production_plan.plan_id IS NULL)))
           + (SELECT COUNT(*) FROM mfg_schedule_scenario scenario
              LEFT JOIN sys_user confirmed_user ON confirmed_user.user_id=scenario.confirmed_by AND confirmed_user.deleted_flag=0
              WHERE scenario.deleted_flag=0 AND scenario.confirmed_by IS NOT NULL AND confirmed_user.user_id IS NULL))
  UNION ALL SELECT 'rule', 'invalid capability values', 0,
         (SELECT COUNT(*) FROM mfg_line_product_capability WHERE deleted_flag=0
          AND (priority_level<=0 OR capacity_factor<=0 OR capacity_factor>2 OR status NOT IN ('ENABLE','DISABLE')))
  UNION ALL SELECT 'rule', 'invalid calendar values', 0,
         (SELECT COUNT(*) FROM mfg_line_calendar_rule WHERE deleted_flag=0
          AND (day_of_week NOT BETWEEN 1 AND 7 OR effective_to<effective_from
            OR capacity_factor<=0 OR capacity_factor>2 OR status NOT IN ('ENABLE','DISABLE')))
  UNION ALL SELECT 'rule', 'foundation capability mapping mismatch', 0,
         (SELECT IF(COUNT(*)=5,0,1) FROM mfg_line_product_capability capability
          JOIN factory_line line ON line.line_id=capability.line_id AND line.deleted_flag=0
          JOIN mdm_product product ON product.product_id=capability.product_id AND product.deleted_flag=0
          JOIN process_route route ON route.route_id=capability.route_id AND route.deleted_flag=0
          WHERE capability.deleted_flag=0 AND capability.status='ENABLE'
            AND ((line.line_code='LINE-TRAIN-01' AND product.product_code='PROD-LNX-T300-76'
                  AND route.route_code='ROUTE-LNX-T300-76-V1')
              OR (line.line_code='LINE-TRAIN-01' AND product.product_code='PROD-LNX-T300-77'
                  AND route.route_code='ROUTE-LNX-T300-77-V1')
              OR (line.line_code='LINE-TRAIN-01' AND product.product_code='PROD-OEM-XY-C500-77'
                  AND route.route_code='ROUTE-OEM-XY-C500-77-V1')
              OR (line.line_code='LINE-MATCH-01' AND product.product_code='PROD-LNX-M700-77'
                  AND route.route_code='ROUTE-LNX-M700-77-V1')
              OR (line.line_code='LINE-MATCH-01' AND product.product_code='PROD-LNX-M700-78'
                  AND route.route_code='ROUTE-LNX-M700-78-V1')))
  UNION ALL SELECT 'rule', 'invalid scheduling scenarios', 0,
         (SELECT COUNT(*) FROM mfg_schedule_scenario WHERE deleted_flag=0
          AND (horizon_start>=horizon_end OR status NOT IN ('DRAFT','CALCULATED','CONFIRMED','CANCELLED')
            OR order_count<0 OR scheduled_order_count<0 OR conflict_order_count<0 OR late_order_count<0
            OR scheduled_order_count+conflict_order_count<>order_count OR late_order_count>scheduled_order_count
            OR total_load_minutes<0 OR (status='CONFIRMED' AND (confirmed_by IS NULL OR confirmed_time IS NULL
              OR conflict_order_count<>0))))
  UNION ALL SELECT 'rule', 'invalid scheduling items', 0,
         (SELECT COUNT(*) FROM mfg_schedule_item WHERE deleted_flag=0
          AND (planned_quantity<=0 OR sequence_no<=0 OR decision_source NOT IN ('RULE_ENGINE','MANUAL')
            OR status NOT IN ('SCHEDULED','CONFLICT','CONFIRMED')
            OR estimated_minutes<0 OR priority_score<0 OR tardiness_minutes<0
            OR (status IN ('SCHEDULED','CONFIRMED') AND (route_id IS NULL OR line_id IS NULL OR planned_start_time IS NULL
              OR planned_end_time IS NULL OR planned_start_time>=planned_end_time))
            OR (status='CONFLICT' AND (conflict_reason IS NULL OR TRIM(conflict_reason)=''))
            OR (status='CONFIRMED' AND plan_id IS NULL)
            OR (status<>'CONFIRMED' AND plan_id IS NOT NULL)))
  UNION ALL SELECT 'rule', 'scheduled item capability mismatch', 0,
         (SELECT COUNT(*) FROM mfg_schedule_item item
          LEFT JOIN mfg_line_product_capability capability
            ON capability.line_id=item.line_id AND capability.product_id=item.product_id
            AND capability.route_id=item.route_id AND capability.status='ENABLE' AND capability.deleted_flag=0
          WHERE item.deleted_flag=0 AND item.status IN ('SCHEDULED','CONFIRMED')
            AND capability.capability_id IS NULL)
  UNION ALL SELECT 'rule', 'duplicate scenario sequence', 0,
         (SELECT COUNT(*) FROM (
           SELECT scenario_id, sequence_no
           FROM mfg_schedule_item
           WHERE deleted_flag=0
           GROUP BY scenario_id, sequence_no
           HAVING COUNT(*)>1
         ) duplicate_sequence)
  UNION ALL SELECT 'boundary', 'foundation-seeded scheduling transaction rows', 0,
         ((SELECT COUNT(*) FROM mfg_schedule_scenario WHERE deleted_flag=0
           AND remark LIKE CONCAT(@scheduling_marker, ':%'))
          + (SELECT COUNT(*) FROM mfg_schedule_item item
             JOIN mfg_schedule_scenario scenario ON scenario.scenario_id=item.scenario_id
             WHERE item.deleted_flag=0 AND scenario.deleted_flag=0
               AND scenario.remark LIKE CONCAT(@scheduling_marker, ':%')))
) checks
ORDER BY FIELD(checks.`check_group`, 'schema', 'count', 'reference', 'rule', 'boundary'), checks.`check_name`;
