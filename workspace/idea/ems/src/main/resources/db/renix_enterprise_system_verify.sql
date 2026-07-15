-- 验证 RENIX_SYSTEM_BASE_V2 / RENIX_RBAC_V1 的数量、关联、权限边界和幂等保护。
-- 验收要求：每一行 result=PASS，且 failed_checks=0。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SELECT checks.`check_group`, checks.`check_name`, checks.`expected_value`, checks.`actual_value`,
       IF(checks.`actual_value` = checks.`expected_value`, 'PASS', 'FAIL') AS `result`,
       SUM(IF(checks.`actual_value` = checks.`expected_value`, 0, 1)) OVER () AS `failed_checks`
FROM (
  SELECT 'organization' AS `check_group`, 'active departments' AS `check_name`, '7' AS `expected_value`,
         CAST((SELECT COUNT(*) FROM `org_department` WHERE `deleted_flag` = 0) AS CHAR) AS `actual_value`
  UNION ALL SELECT 'organization', 'active employees', '14',
         CAST((SELECT COUNT(*) FROM `org_employee` WHERE `deleted_flag` = 0 AND `status` = 'ENABLE') AS CHAR)
  UNION ALL SELECT 'organization', 'IT department and administrator employee', '2',
         CAST((SELECT
           (SELECT COUNT(*) FROM `org_department`
            WHERE `department_code` = 'DEPT-IT' AND `department_name` = '数字化与信息部' AND `deleted_flag` = 0)
           + (SELECT COUNT(*) FROM `org_employee`
              WHERE `employee_no` = 'EMP-IT-001' AND `position_name` = 'MES系统管理员'
                AND `status` = 'ENABLE' AND `deleted_flag` = 0)) AS CHAR)

  UNION ALL SELECT 'account', 'enabled employee-linked users', '14',
         CAST((SELECT COUNT(*) FROM `sys_user` user
               JOIN `org_employee` employee
                 ON employee.`employee_id` = user.`employee_id` AND employee.`deleted_flag` = 0
               WHERE user.`status` = 'ENABLE' AND user.`deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'account', 'PBKDF2 password hashes', '14',
         CAST((SELECT COUNT(*) FROM `sys_user`
               WHERE `password_hash` LIKE 'pbkdf2-sha256$210000$%$%' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'account', 'placeholder or invalid seeded accounts', '0',
         CAST((SELECT COUNT(*) FROM `sys_user`
               WHERE `username` IN ('scjh001','scjl001','bzz001','bzz002',
                                    'czg001','czg002','czg003','czg004','czg005','czg006',
                                    'zjy001','ckgly001','sbgly001')
                 AND (`password_hash` = '!' OR `status` <> 'ENABLE' OR `deleted_flag` <> 0)) AS CHAR)

  UNION ALL SELECT 'rbac', 'enabled enterprise roles', '8',
         CAST((SELECT COUNT(*) FROM `sys_role`
               WHERE `remark` LIKE 'RENIX_RBAC_V1%' AND `status` = 'ENABLE' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'rbac', 'role data scopes', '8',
         CAST((SELECT COUNT(*) FROM `sys_role`
               WHERE `deleted_flag` = 0 AND
                 ((`role_code` IN ('SUPER_ADMIN','MES_MANAGER','PLAN_SCHEDULER') AND `data_scope` = 'ALL')
                  OR (`role_code` = 'PRODUCTION_SUPERVISOR' AND `data_scope` = 'TEAM')
                  OR (`role_code` = 'PRODUCTION_OPERATOR' AND `data_scope` = 'SELF')
                  OR (`role_code` IN ('QUALITY_ENGINEER','WAREHOUSE_MANAGER','EQUIPMENT_ENGINEER')
                      AND `data_scope` = 'DEPARTMENT'))) AS CHAR)
  UNION ALL SELECT 'rbac', 'active user-role relations', '14',
         CAST((SELECT COUNT(*) FROM `sys_user_role` WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'rbac', 'users without exactly one active role', '0',
         CAST((SELECT COUNT(*) FROM (
           SELECT user.`user_id`
           FROM `sys_user` user
           LEFT JOIN `sys_user_role` relation
             ON relation.`user_id` = user.`user_id` AND relation.`deleted_flag` = 0
           WHERE user.`deleted_flag` = 0
           GROUP BY user.`user_id`
           HAVING COUNT(relation.`id`) <> 1
         ) invalid_user_role) AS CHAR)
  UNION ALL SELECT 'rbac', 'permission nodes', '436',
         CAST((SELECT COUNT(*) FROM `sys_menu`
               WHERE `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'rbac', 'directory/resource/hidden/special/button shape', '436',
         CAST((SELECT
           (SELECT COUNT(*) FROM `sys_menu`
            WHERE `menu_type` = 'DIRECTORY' AND `remark` = 'RENIX_RBAC_V1: 业务域目录' AND `deleted_flag` = 0)
           + (SELECT COUNT(*) FROM `sys_menu`
              WHERE `menu_type` = 'MENU' AND `remark` = 'RENIX_RBAC_V1: PC资源菜单' AND `deleted_flag` = 0)
           + (SELECT COUNT(*) FROM `sys_menu`
              WHERE `menu_type` = 'API' AND `remark` = 'RENIX_RBAC_V1: 隐藏主从资源查询权限' AND `deleted_flag` = 0)
           + (SELECT COUNT(*) FROM `sys_menu`
              WHERE `menu_type` = 'MENU' AND `permission_code` IN
                    ('dashboard:summary:view','dashboard:big-screen:view','ai:assistant:use')
                AND `deleted_flag` = 0)
           + (SELECT COUNT(*) FROM `sys_menu`
              WHERE `menu_type` = 'BUTTON' AND `remark` LIKE 'RENIX_RBAC_V1%' AND `deleted_flag` = 0)
           + (SELECT COUNT(*) FROM `sys_menu`
              WHERE `remark` LIKE 'RBAC_PERMISSION_V2:%' AND `deleted_flag` = 0)
           + (SELECT COUNT(*) FROM `sys_menu`
              WHERE `remark` LIKE 'P0_PRODUCTION_FLOW_PERMISSION_V1:%' AND `deleted_flag` = 0)) AS CHAR)
  UNION ALL SELECT 'rbac', 'active role-permission relations', '1250',
         CAST((SELECT COUNT(*) FROM `sys_role_menu` relation
               JOIN `sys_role` role ON role.`role_id` = relation.`role_id` AND role.`deleted_flag` = 0
               JOIN `sys_menu` menu ON menu.`menu_id` = relation.`menu_id` AND menu.`deleted_flag` = 0
               WHERE relation.`deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'rbac', 'duplicate active permission codes', '0',
         CAST((SELECT COUNT(*) FROM (
           SELECT `permission_code` FROM `sys_menu` WHERE `deleted_flag` = 0
           GROUP BY `permission_code` HAVING COUNT(*) > 1
         ) duplicate_permission) AS CHAR)
  UNION ALL SELECT 'rbac', 'RBAC orphan rows', '0',
         CAST((SELECT
           (SELECT COUNT(*) FROM `sys_user_role` relation
            LEFT JOIN `sys_user` user ON user.`user_id` = relation.`user_id` AND user.`deleted_flag` = 0
            LEFT JOIN `sys_role` role ON role.`role_id` = relation.`role_id` AND role.`deleted_flag` = 0
            WHERE relation.`deleted_flag` = 0 AND (user.`user_id` IS NULL OR role.`role_id` IS NULL))
           + (SELECT COUNT(*) FROM `sys_role_menu` relation
              LEFT JOIN `sys_role` role ON role.`role_id` = relation.`role_id` AND role.`deleted_flag` = 0
              LEFT JOIN `sys_menu` menu ON menu.`menu_id` = relation.`menu_id` AND menu.`deleted_flag` = 0
              WHERE relation.`deleted_flag` = 0 AND (role.`role_id` IS NULL OR menu.`menu_id` IS NULL))
           + (SELECT COUNT(*) FROM `sys_menu` child
              LEFT JOIN `sys_menu` parent ON parent.`menu_id` = child.`parent_id` AND parent.`deleted_flag` = 0
              WHERE child.`deleted_flag` = 0 AND child.`parent_id` IS NOT NULL AND parent.`menu_id` IS NULL)) AS CHAR)

  UNION ALL SELECT 'permission', 'required allows', '6',
         CAST((SELECT COUNT(*) FROM `sys_role` role
               JOIN `sys_role_menu` relation
                 ON relation.`role_id` = role.`role_id` AND relation.`deleted_flag` = 0
               JOIN `sys_menu` menu
                 ON menu.`menu_id` = relation.`menu_id` AND menu.`deleted_flag` = 0
               WHERE (role.`role_code`, menu.`permission_code`) IN (
                 ROW('PRODUCTION_OPERATOR','mfg:task:start'),
                 ROW('WAREHOUSE_MANAGER','wms:outbound-order:confirm'),
                 ROW('QUALITY_ENGINEER','qms:nonconforming-order:approve'),
                 ROW('EQUIPMENT_ENGINEER','eam:repair-order:close'),
                 ROW('PLAN_SCHEDULER','mfg:production-order:create'),
                 ROW('PRODUCTION_SUPERVISOR','mfg:work-order:start')
                )) AS CHAR)
  UNION ALL SELECT 'permission', 'miniprogram role access allows', '20',
         CAST((SELECT COUNT(*) FROM `sys_role` role
               JOIN `sys_role_menu` relation
                 ON relation.`role_id` = role.`role_id` AND relation.`deleted_flag` = 0
               JOIN `sys_menu` menu
                 ON menu.`menu_id` = relation.`menu_id` AND menu.`deleted_flag` = 0
               WHERE (role.`role_code`, menu.`permission_code`) IN (
                 ROW('PRODUCTION_OPERATOR','mfg:production-plan:view'),
                 ROW('PRODUCTION_OPERATOR','mfg:work-order:view'),
                 ROW('PRODUCTION_OPERATOR','mdm:product:view'),
                 ROW('PRODUCTION_OPERATOR','process:operation:view'),
                 ROW('PRODUCTION_OPERATOR','factory:station:view'),
                 ROW('QUALITY_ENGINEER','trace:barcode:scan'),
                 ROW('WAREHOUSE_MANAGER','trace:barcode:scan'),
                 ROW('WAREHOUSE_MANAGER','eam:equipment:view'),
                 ROW('WAREHOUSE_MANAGER','eam:repair-order:view'),
                 ROW('WAREHOUSE_MANAGER','factory:station:view'),
                 ROW('WAREHOUSE_MANAGER','org:employee:view'),
                 ROW('EQUIPMENT_ENGINEER','trace:barcode:scan'),
                 ROW('EQUIPMENT_ENGINEER','wms:inbound-order:view'),
                 ROW('EQUIPMENT_ENGINEER','wms:inbound-detail:view'),
                 ROW('EQUIPMENT_ENGINEER','wms:outbound-order:view'),
                 ROW('EQUIPMENT_ENGINEER','wms:outbound-detail:view'),
                 ROW('EQUIPMENT_ENGINEER','wms:inventory:view'),
                 ROW('EQUIPMENT_ENGINEER','wms:warehouse:view'),
                 ROW('EQUIPMENT_ENGINEER','wms:location:view'),
                 ROW('EQUIPMENT_ENGINEER','wms:material-batch:view')
               )) AS CHAR)
  UNION ALL SELECT 'permission', 'required denials', '0',
         CAST((SELECT COUNT(*) FROM `sys_role` role
               JOIN `sys_role_menu` relation
                 ON relation.`role_id` = role.`role_id` AND relation.`deleted_flag` = 0
               JOIN `sys_menu` menu
                 ON menu.`menu_id` = relation.`menu_id` AND menu.`deleted_flag` = 0
               WHERE (role.`role_code`, menu.`permission_code`) IN (
                 ROW('PRODUCTION_OPERATOR','sys:user:view'),
                 ROW('PRODUCTION_OPERATOR','wms:outbound-order:confirm'),
                 ROW('PRODUCTION_OPERATOR','qms:nonconforming-order:approve'),
                 ROW('PRODUCTION_OPERATOR','eam:repair-order:close'),
                 ROW('WAREHOUSE_MANAGER','qms:nonconforming-order:approve'),
                  ROW('QUALITY_ENGINEER','sys:user:reset-password'),
                  ROW('EQUIPMENT_ENGINEER','wms:outbound-order:confirm'),
                  ROW('EQUIPMENT_ENGINEER','wms:inbound-order:confirm'),
                  ROW('WAREHOUSE_MANAGER','eam:equipment:update'),
                  ROW('WAREHOUSE_MANAGER','eam:repair-order:assign'),
                  ROW('WAREHOUSE_MANAGER','eam:repair-order:close'),
                  ROW('PRODUCTION_OPERATOR','mfg:work-order:start'),
                  ROW('PLAN_SCHEDULER','sys:role:assign-menu')
               )) AS CHAR)

  UNION ALL SELECT 'dictionary', 'enabled dictionary types', '45',
         CAST((SELECT COUNT(*) FROM `sys_dict_type`
               WHERE `remark` LIKE 'RENIX_SYSTEM_BASE_V2%' AND `status` = 'ENABLE' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'dictionary', 'dictionary data rows', '166',
         CAST((SELECT COUNT(*) FROM `sys_dict_data`
               WHERE `remark` LIKE 'RENIX_SYSTEM_BASE_V2%' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'dictionary', 'dictionary orphan rows', '0',
         CAST((SELECT COUNT(*) FROM `sys_dict_data` data
               LEFT JOIN `sys_dict_type` type
                 ON type.`dict_type_code` = data.`dict_type_code` AND type.`deleted_flag` = 0
               WHERE data.`deleted_flag` = 0 AND type.`dict_type_id` IS NULL) AS CHAR)
  UNION ALL SELECT 'parameter', 'non-sensitive enterprise parameters', '12',
         CAST((SELECT COUNT(*) FROM `sys_system_param`
               WHERE `remark` LIKE 'RENIX_SYSTEM_BASE_V2%' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'parameter', 'sensitive parameter keys', '0',
         CAST((SELECT COUNT(*) FROM `sys_system_param`
               WHERE `deleted_flag` = 0
                 AND LOWER(`param_key`) REGEXP '(password|secret|token|api[._-]?key|jwt|database|tailscale)') AS CHAR)

  UNION ALL SELECT 'instruction', 'work-instruction PDF files', '15',
         CAST((SELECT COUNT(*) FROM `sys_file`
               WHERE `remark` LIKE 'RENIX_SYSTEM_BASE_V2%' AND `file_type` = 'pdf'
                 AND `file_url` LIKE '/upload/sop/%' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'instruction', 'released work instructions', '15',
         CAST((SELECT COUNT(*) FROM `process_work_instruction`
               WHERE `remark` LIKE 'RENIX_SYSTEM_BASE_V2%' AND `status` = 'RELEASED'
                 AND `version` = 'V1.0' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'instruction', 'work-instruction orphan or file-link rows', '0',
         CAST((SELECT COUNT(*) FROM `process_work_instruction` instruction
               LEFT JOIN `process_operation` operation
                 ON operation.`operation_id` = instruction.`operation_id` AND operation.`deleted_flag` = 0
               LEFT JOIN `mdm_product` product
                 ON product.`product_id` = instruction.`product_id` AND product.`deleted_flag` = 0
               LEFT JOIN `sys_file` file
                 ON file.`file_id` = instruction.`file_id` AND file.`deleted_flag` = 0
               WHERE instruction.`remark` LIKE 'RENIX_SYSTEM_BASE_V2%'
                 AND instruction.`deleted_flag` = 0
                 AND (operation.`operation_id` IS NULL OR file.`file_id` IS NULL
                      OR file.`biz_id` <> instruction.`instruction_id`
                      OR (instruction.`product_id` IS NOT NULL AND product.`product_id` IS NULL))) AS CHAR)

  UNION ALL SELECT 'idempotency', 'system baseline completion marker', '1',
         CAST((SELECT COUNT(*) FROM `sys_operation_log`
               WHERE `operation_type` = 'SEED_RENIX_SYSTEM_BASE' AND `module_code` = 'db_seed'
                 AND `remark` = 'RENIX_SYSTEM_BASE_V2' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'protection', 'scheduling E2E draft orders', '3',
         CAST((SELECT COUNT(*) FROM `mfg_production_order`
               WHERE `remark` LIKE 'SCHEDULING_E2E_20260713%' AND `status` = 'DRAFT' AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'protection', 'scheduling scenarios 2/3/4', '3',
         CAST((SELECT COUNT(*) FROM `mfg_schedule_scenario`
               WHERE `scenario_id` IN (2,3,4) AND `deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'protection', 'scheduling items linked to scenarios 2/3/4', '23',
         CAST((SELECT COUNT(*) FROM `mfg_schedule_item` item
               JOIN `mfg_schedule_scenario` scenario
                 ON scenario.`scenario_id` = item.`scenario_id` AND scenario.`deleted_flag` = 0
               WHERE item.`scenario_id` IN (2,3,4) AND item.`deleted_flag` = 0) AS CHAR)
  UNION ALL SELECT 'protection', 'scheduling scenario digest', '12c7bd26d52f7be41179a92895ba77fa',
         (SELECT MD5(GROUP_CONCAT(CONCAT_WS('#', `scenario_id`, `scenario_no`, `status`, `order_count`,
                  `scheduled_order_count`, `conflict_order_count`, `late_order_count`, `total_load_minutes`,
                  COALESCE(`remark`,''), `version`, COALESCE(`updated_time`,''))
                  ORDER BY `scenario_id` SEPARATOR '|'))
          FROM `mfg_schedule_scenario` WHERE `scenario_id` IN (2,3,4))
  UNION ALL SELECT 'protection', 'scheduling item digest', '7082086c59eb62aa20152da8ae435a49',
         (SELECT MD5(GROUP_CONCAT(CONCAT_WS('#', `schedule_item_id`, `scenario_id`, `production_order_id`,
                  COALESCE(`line_id`,''), COALESCE(`plan_id`,''), `sequence_no`, `planned_quantity`,
                  COALESCE(`planned_start_time`,''), COALESCE(`planned_end_time`,''), `estimated_minutes`,
                  `priority_score`, `tardiness_minutes`, `decision_source`, `status`,
                  COALESCE(`conflict_reason`,''), COALESCE(`explanation`,''), `version`, COALESCE(`updated_time`,''))
                  ORDER BY `schedule_item_id` SEPARATOR '|'))
          FROM `mfg_schedule_item` WHERE `scenario_id` IN (2,3,4))
) checks
ORDER BY FIELD(checks.`check_group`, 'organization', 'account', 'rbac', 'permission', 'dictionary',
               'parameter', 'instruction', 'idempotency', 'protection'), checks.`check_name`;
