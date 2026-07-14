-- 羽毛球 MES 权限目录增量修复 V2。
-- 目标：补齐智能排产权限，并保证 admin/SUPER_ADMIN 对新增权限立即具备完整授权关系。
-- 本脚本幂等；执行前仍应按共享库规范完成备份。

SET @rbac_v2_time = NOW();
SET @rbac_v2_admin_user_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  ORDER BY `user_id` LIMIT 1
);
SET @rbac_v2_super_role_id = (
  SELECT `role_id` FROM `sys_role`
  WHERE `role_code` = 'SUPER_ADMIN' AND `deleted_flag` = 0
  ORDER BY `role_id` LIMIT 1
);

START TRANSACTION;

UPDATE `sys_role`
SET `status` = 'ENABLE', `data_scope` = 'ALL', `updated_by` = @rbac_v2_admin_user_id,
    `updated_time` = @rbac_v2_time
WHERE `role_id` = @rbac_v2_super_role_id;

INSERT INTO `sys_user_role`
  (`user_id`, `role_id`, `assigned_time`, `created_time`, `deleted_flag`)
SELECT @rbac_v2_admin_user_id, @rbac_v2_super_role_id, @rbac_v2_time, @rbac_v2_time, 0
WHERE @rbac_v2_admin_user_id IS NOT NULL AND @rbac_v2_super_role_id IS NOT NULL
ON DUPLICATE KEY UPDATE `assigned_time` = VALUES(`assigned_time`), `deleted_flag` = 0;

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
VALUES
  (NULL,'生产制造','DIRECTORY','module:mfg:view','/module/mfg','RBAC_PERMISSION_V2: 生产制造目录',0,@rbac_v2_admin_user_id,@rbac_v2_time,@rbac_v2_admin_user_id,@rbac_v2_time,0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_type` = 'DIRECTORY', `route_path` = VALUES(`route_path`),
  `updated_by` = @rbac_v2_admin_user_id, `updated_time` = @rbac_v2_time, `deleted_flag` = 0;

SET @rbac_v2_mfg_menu_id = (
  SELECT `menu_id` FROM `sys_menu`
  WHERE `permission_code` = 'module:mfg:view' AND `deleted_flag` = 0
  ORDER BY `menu_id` LIMIT 1
);

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
VALUES
  (@rbac_v2_mfg_menu_id,'产线产品能力','MENU','mfg:line-product-capability:view','/mfg/line-product-capabilities','RBAC_PERMISSION_V2: 排产资源',0,@rbac_v2_admin_user_id,@rbac_v2_time,@rbac_v2_admin_user_id,@rbac_v2_time,0),
  (@rbac_v2_mfg_menu_id,'产线周历规则','MENU','mfg:line-calendar-rule:view','/mfg/line-calendar-rules','RBAC_PERMISSION_V2: 排产资源',0,@rbac_v2_admin_user_id,@rbac_v2_time,@rbac_v2_admin_user_id,@rbac_v2_time,0),
  (@rbac_v2_mfg_menu_id,'智能排产工作台','MENU','mfg:schedule-scenario:view','/mfg/schedule-workbench','RBAC_PERMISSION_V2: 人工排产工作台',0,@rbac_v2_admin_user_id,@rbac_v2_time,@rbac_v2_admin_user_id,@rbac_v2_time,0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = 'MENU',
  `route_path` = VALUES(`route_path`), `updated_by` = @rbac_v2_admin_user_id,
  `updated_time` = @rbac_v2_time, `deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_rbac_v2_action`;
CREATE TEMPORARY TABLE `tmp_rbac_v2_action` (
  `parent_permission` VARCHAR(64) NOT NULL,
  `permission_code` VARCHAR(64) PRIMARY KEY,
  `permission_name` VARCHAR(128) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

INSERT INTO `tmp_rbac_v2_action` VALUES
  ('mfg:line-product-capability:view','mfg:line-product-capability:create','新增产线产品能力'),
  ('mfg:line-product-capability:view','mfg:line-product-capability:update','修改产线产品能力'),
  ('mfg:line-product-capability:view','mfg:line-product-capability:delete','删除产线产品能力'),
  ('mfg:line-product-capability:view','mfg:line-product-capability:export','导出产线产品能力'),
  ('mfg:line-calendar-rule:view','mfg:line-calendar-rule:create','新增产线周历规则'),
  ('mfg:line-calendar-rule:view','mfg:line-calendar-rule:update','修改产线周历规则'),
  ('mfg:line-calendar-rule:view','mfg:line-calendar-rule:delete','删除产线周历规则'),
  ('mfg:line-calendar-rule:view','mfg:line-calendar-rule:export','导出产线周历规则'),
  ('mfg:schedule-scenario:view','mfg:schedule-scenario:create','创建人工排产草案'),
  ('mfg:schedule-scenario:view','mfg:schedule-scenario:update','编辑人工排产草案'),
  ('mfg:schedule-scenario:view','mfg:schedule-scenario:delete','移出人工排产订单'),
  ('mfg:schedule-scenario:view','mfg:schedule-scenario:validate','校验排产草案'),
  ('mfg:schedule-scenario:view','mfg:schedule-scenario:ai-advice','生成排产 AI 建议'),
  ('mfg:schedule-scenario:view','mfg:schedule-scenario:confirm','确认排产并生成生产计划');

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT parent.`menu_id`, action.`permission_name`, 'BUTTON', action.`permission_code`, NULL,
       'RBAC_PERMISSION_V2: 排产操作权限', 0, @rbac_v2_admin_user_id, @rbac_v2_time,
       @rbac_v2_admin_user_id, @rbac_v2_time, 0
FROM `tmp_rbac_v2_action` action
JOIN `sys_menu` parent
  ON parent.`permission_code` = action.`parent_permission` AND parent.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = 'BUTTON',
  `updated_by` = @rbac_v2_admin_user_id, `updated_time` = @rbac_v2_time, `deleted_flag` = 0;

-- 超级管理员始终关联当前全部有效权限；运行时仍由后端 SUPER_ADMIN 语义兜底。
INSERT INTO `sys_role_menu`
  (`role_id`, `menu_id`, `created_by`, `created_time`, `deleted_flag`)
SELECT @rbac_v2_super_role_id, menu.`menu_id`, @rbac_v2_admin_user_id, @rbac_v2_time, 0
FROM `sys_menu` menu
WHERE @rbac_v2_super_role_id IS NOT NULL AND menu.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE `deleted_flag` = 0;

-- 生产经理和计划员获得排产工作台完整能力；其它岗位仍按最小权限分配。
INSERT INTO `sys_role_menu`
  (`role_id`, `menu_id`, `created_by`, `created_time`, `deleted_flag`)
SELECT role.`role_id`, menu.`menu_id`, @rbac_v2_admin_user_id, @rbac_v2_time, 0
FROM `sys_role` role
JOIN `sys_menu` menu
  ON (menu.`permission_code` = 'module:mfg:view'
      OR menu.`permission_code` LIKE 'mfg:line-product-capability:%'
      OR menu.`permission_code` LIKE 'mfg:line-calendar-rule:%'
      OR menu.`permission_code` LIKE 'mfg:schedule-scenario:%')
 AND menu.`deleted_flag` = 0
WHERE role.`role_code` IN ('MES_MANAGER','PLAN_SCHEDULER')
  AND role.`status` = 'ENABLE' AND role.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE `deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_rbac_v2_action`;
COMMIT;

SELECT role.`role_code`, COUNT(DISTINCT relation.`menu_id`) AS `permission_count`
FROM `sys_role` role
LEFT JOIN `sys_role_menu` relation
  ON relation.`role_id` = role.`role_id` AND relation.`deleted_flag` = 0
WHERE role.`role_code` IN ('SUPER_ADMIN','MES_MANAGER','PLAN_SCHEDULER')
  AND role.`deleted_flag` = 0
GROUP BY role.`role_code`
ORDER BY role.`role_code`;
