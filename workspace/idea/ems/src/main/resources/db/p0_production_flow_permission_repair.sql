-- P0 生产主链独立业务动作权限。脚本可重复执行。
SET @p0_permission_time = CURRENT_TIMESTAMP;
SET @p0_admin_user_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  ORDER BY `user_id` LIMIT 1
);

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `tmp_p0_production_permission`;
CREATE TEMPORARY TABLE `tmp_p0_production_permission` (
  `parent_permission_code` VARCHAR(64) NOT NULL,
  `permission_code` VARCHAR(64) PRIMARY KEY,
  `permission_name` VARCHAR(128) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

INSERT INTO `tmp_p0_production_permission` VALUES
  ('mfg:production-order:view','mfg:production-order:release','下达生产订单'),
  ('mfg:production-order:view','mfg:production-order:withdraw','撤回生产订单修改'),
  ('mfg:production-plan:view','mfg:production-plan:generate','生成生产计划'),
  ('mfg:production-plan:view','mfg:production-plan:release','生效生产计划'),
  ('mfg:production-plan:view','mfg:production-plan:withdraw','撤回生产计划修改'),
  ('mfg:work-order:view','mfg:work-order:generate','生成生产工单'),
  ('mfg:work-order:view','mfg:work-order:release','下达生产工单'),
  ('mfg:work-order:view','mfg:work-order:withdraw','撤回生产工单修改'),
  ('mfg:dispatch-order:view','mfg:dispatch-order:generate','从生产工单生成派工单'),
  ('mfg:dispatch-order:view','mfg:dispatch-order:release','派发并生成生产任务'),
  ('mfg:dispatch-order:view','mfg:dispatch-order:withdraw','撤回派工'),
  ('mfg:task:view','mfg:task:withdraw','撤回任务开工'),
  ('mfg:work-report:view','mfg:work-report:withdraw','撤回本人生产报工'),
  ('wms:inbound-order:view','wms:inbound-order:withdraw','撤回未确认入库单'),
  ('wms:outbound-order:view','wms:outbound-order:withdraw','撤回未确认出库单'),
  ('wms:return-order:view','wms:return-order:withdraw','撤回未确认退料单'),
  ('qms:inspection-order:view','qms:inspection-order:withdraw','撤回未检验检验单'),
  ('mfg:work-order:view','mfg:work-order:start','开始生产工单');

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT parent.`menu_id`, permission.`permission_name`, 'BUTTON', permission.`permission_code`, NULL,
       'P0_PRODUCTION_FLOW_PERMISSION_V1: 生产主链独立业务动作', 0,
       @p0_admin_user_id, @p0_permission_time, @p0_admin_user_id, @p0_permission_time, 0
FROM `tmp_p0_production_permission` permission
JOIN `sys_menu` parent
  ON parent.`permission_code` = permission.`parent_permission_code`
 AND parent.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = 'BUTTON',
  `remark` = VALUES(`remark`), `updated_by` = @p0_admin_user_id,
  `updated_time` = @p0_permission_time, `deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_p0_production_role_permission`;
CREATE TEMPORARY TABLE `tmp_p0_production_role_permission` (
  `role_code` VARCHAR(64) NOT NULL,
  `permission_code` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`role_code`, `permission_code`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

INSERT INTO `tmp_p0_production_role_permission` VALUES
  ('SUPER_ADMIN','mfg:production-order:release'),
  ('SUPER_ADMIN','mfg:production-order:withdraw'),
  ('SUPER_ADMIN','mfg:production-plan:generate'),
  ('SUPER_ADMIN','mfg:production-plan:release'),
  ('SUPER_ADMIN','mfg:production-plan:withdraw'),
  ('SUPER_ADMIN','mfg:work-order:generate'),
  ('SUPER_ADMIN','mfg:work-order:release'),
  ('SUPER_ADMIN','mfg:work-order:withdraw'),
  ('SUPER_ADMIN','mfg:dispatch-order:generate'),
  ('SUPER_ADMIN','mfg:dispatch-order:release'),
  ('SUPER_ADMIN','mfg:dispatch-order:withdraw'),
  ('SUPER_ADMIN','mfg:task:withdraw'),
  ('SUPER_ADMIN','mfg:work-report:withdraw'),
  ('SUPER_ADMIN','wms:inbound-order:withdraw'),
  ('SUPER_ADMIN','wms:outbound-order:withdraw'),
  ('SUPER_ADMIN','wms:return-order:withdraw'),
  ('SUPER_ADMIN','qms:inspection-order:withdraw'),
  ('SUPER_ADMIN','mfg:work-order:start'),
  ('MES_MANAGER','mfg:production-order:release'),
  ('MES_MANAGER','mfg:production-order:withdraw'),
  ('MES_MANAGER','mfg:production-plan:generate'),
  ('MES_MANAGER','mfg:production-plan:release'),
  ('MES_MANAGER','mfg:production-plan:withdraw'),
  ('MES_MANAGER','mfg:work-order:generate'),
  ('MES_MANAGER','mfg:work-order:release'),
  ('MES_MANAGER','mfg:work-order:withdraw'),
  ('MES_MANAGER','mfg:dispatch-order:generate'),
  ('MES_MANAGER','mfg:dispatch-order:release'),
  ('MES_MANAGER','mfg:dispatch-order:withdraw'),
  ('MES_MANAGER','mfg:task:withdraw'),
  ('MES_MANAGER','mfg:work-report:withdraw'),
  ('MES_MANAGER','wms:inbound-order:withdraw'),
  ('MES_MANAGER','wms:outbound-order:withdraw'),
  ('MES_MANAGER','wms:return-order:withdraw'),
  ('MES_MANAGER','qms:inspection-order:withdraw'),
  ('MES_MANAGER','mfg:work-order:start'),
  ('PRODUCTION_SUPERVISOR','mfg:work-order:start'),
  ('PLAN_SCHEDULER','mfg:production-order:release'),
  ('PLAN_SCHEDULER','mfg:production-order:withdraw'),
  ('PLAN_SCHEDULER','mfg:production-plan:generate'),
  ('PLAN_SCHEDULER','mfg:production-plan:release'),
  ('PLAN_SCHEDULER','mfg:production-plan:withdraw'),
  ('PRODUCTION_SUPERVISOR','mfg:work-order:withdraw'),
  ('PRODUCTION_SUPERVISOR','mfg:dispatch-order:generate'),
  ('PRODUCTION_SUPERVISOR','mfg:dispatch-order:release'),
  ('PRODUCTION_SUPERVISOR','mfg:dispatch-order:withdraw'),
  ('PRODUCTION_SUPERVISOR','mfg:task:withdraw'),
  ('PRODUCTION_OPERATOR','mfg:task:withdraw'),
  ('PRODUCTION_OPERATOR','mfg:work-report:withdraw'),
  ('WAREHOUSE_MANAGER','wms:inbound-order:withdraw'),
  ('WAREHOUSE_MANAGER','wms:outbound-order:withdraw'),
  ('WAREHOUSE_MANAGER','wms:return-order:withdraw'),
  ('QUALITY_ENGINEER','qms:inspection-order:withdraw');

UPDATE `sys_role_menu` relation
JOIN `sys_role` role
  ON role.`role_id` = relation.`role_id`
 AND role.`deleted_flag` = 0
JOIN `sys_menu` menu
  ON menu.`menu_id` = relation.`menu_id`
 AND menu.`deleted_flag` = 0
LEFT JOIN `tmp_p0_production_role_permission` allowed
  ON allowed.`role_code` = role.`role_code`
 AND allowed.`permission_code` = menu.`permission_code`
SET relation.`deleted_flag` = 1
WHERE menu.`permission_code` IN (
  SELECT permission.`permission_code` FROM `tmp_p0_production_permission` permission
)
  AND allowed.`permission_code` IS NULL
  AND relation.`deleted_flag` = 0;

-- 所有岗位都只能从上游上下文生成生产单据，不保留脱离来源的通用新增权限。
UPDATE `sys_role_menu` relation
JOIN `sys_role` role
  ON role.`role_id` = relation.`role_id`
 AND role.`deleted_flag` = 0
JOIN `sys_menu` menu
  ON menu.`menu_id` = relation.`menu_id`
 AND menu.`deleted_flag` = 0
SET relation.`deleted_flag` = 1
WHERE menu.`permission_code` IN (
    'mfg:production-plan:create',
    'mfg:work-order:create',
    'mfg:dispatch-order:create',
    'mfg:task:create'
  )
  AND relation.`deleted_flag` = 0;

INSERT INTO `sys_role_menu`
  (`role_id`, `menu_id`, `created_by`, `created_time`, `deleted_flag`)
SELECT role.`role_id`, menu.`menu_id`, @p0_admin_user_id, @p0_permission_time, 0
FROM `tmp_p0_production_role_permission` relation
JOIN `sys_role` role
  ON role.`role_code` = relation.`role_code`
 AND role.`status` = 'ENABLE' AND role.`deleted_flag` = 0
JOIN `sys_menu` menu
  ON menu.`permission_code` = relation.`permission_code`
 AND menu.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE `deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_p0_production_role_permission`;
DROP TEMPORARY TABLE IF EXISTS `tmp_p0_production_permission`;

COMMIT;

SELECT permission.`permission_code`,
       COUNT(DISTINCT relation.`role_id`) AS `granted_role_count`
FROM `sys_menu` permission
LEFT JOIN `sys_role_menu` relation
  ON relation.`menu_id` = permission.`menu_id` AND relation.`deleted_flag` = 0
WHERE permission.`permission_code` IN (
  'mfg:production-order:release',
  'mfg:production-order:withdraw',
  'mfg:production-plan:generate',
  'mfg:production-plan:release',
  'mfg:production-plan:withdraw',
  'mfg:work-order:generate',
  'mfg:work-order:release',
  'mfg:work-order:withdraw',
  'mfg:dispatch-order:generate',
  'mfg:dispatch-order:release',
  'mfg:dispatch-order:withdraw',
  'mfg:task:withdraw',
  'mfg:work-report:withdraw',
  'wms:inbound-order:withdraw',
  'wms:outbound-order:withdraw',
  'wms:return-order:withdraw',
  'qms:inspection-order:withdraw',
  'mfg:work-order:start'
) AND permission.`deleted_flag` = 0
GROUP BY permission.`permission_code`
ORDER BY permission.`permission_code`;
