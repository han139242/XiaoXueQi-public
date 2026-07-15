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
  ('mfg:production-plan:view','mfg:production-plan:generate','生成生产计划'),
  ('mfg:work-order:view','mfg:work-order:generate','生成生产工单'),
  ('mfg:work-order:view','mfg:work-order:release','下达生产工单');

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
  ('SUPER_ADMIN','mfg:production-plan:generate'),
  ('SUPER_ADMIN','mfg:work-order:generate'),
  ('SUPER_ADMIN','mfg:work-order:release'),
  ('MES_MANAGER','mfg:production-order:release'),
  ('MES_MANAGER','mfg:production-plan:generate'),
  ('MES_MANAGER','mfg:work-order:generate'),
  ('MES_MANAGER','mfg:work-order:release'),
  ('PLAN_SCHEDULER','mfg:production-order:release'),
  ('PLAN_SCHEDULER','mfg:production-plan:generate');

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
  'mfg:production-plan:generate',
  'mfg:work-order:generate',
  'mfg:work-order:release'
) AND permission.`deleted_flag` = 0
GROUP BY permission.`permission_code`
ORDER BY permission.`permission_code`;
