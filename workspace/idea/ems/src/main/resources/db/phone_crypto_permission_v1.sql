-- 联系电话字段权限目录 V1。普通角色默认不授予完整查看或写入权限。
-- 超级管理员运行时已有全权限语义，本脚本同时补全其菜单关联，便于审计和管理端展示。

SET @phone_crypto_permission_time = NOW();
SET @phone_crypto_admin_user_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  ORDER BY `user_id` LIMIT 1
);
SET @phone_crypto_super_role_id = (
  SELECT `role_id` FROM `sys_role`
  WHERE `role_code` = 'SUPER_ADMIN' AND `deleted_flag` = 0
  ORDER BY `role_id` LIMIT 1
);

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `tmp_phone_crypto_permission`;
CREATE TEMPORARY TABLE `tmp_phone_crypto_permission` (
  `parent_permission_code` VARCHAR(64) NOT NULL,
  `permission_code` VARCHAR(64) NOT NULL PRIMARY KEY,
  `permission_name` VARCHAR(128) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

INSERT INTO `tmp_phone_crypto_permission` VALUES
  ('org:employee:view', 'org:employee:phone:view-full', '查看员工完整联系电话'),
  ('org:employee:view', 'org:employee:phone:write', '维护员工联系电话'),
  ('mdm:customer:view', 'mdm:customer:contact-phone:view-full', '查看客户完整联系电话'),
  ('mdm:customer:view', 'mdm:customer:contact-phone:write', '维护客户联系电话'),
  ('mdm:supplier:view', 'mdm:supplier:contact-phone:view-full', '查看供应商完整联系电话'),
  ('mdm:supplier:view', 'mdm:supplier:contact-phone:write', '维护供应商联系电话');

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT parent.`menu_id`, permission.`permission_name`, 'BUTTON', permission.`permission_code`, NULL,
       'PHONE_CRYPTO_V1: 联系电话字段权限', 0, @phone_crypto_admin_user_id,
       @phone_crypto_permission_time, @phone_crypto_admin_user_id, @phone_crypto_permission_time, 0
FROM `tmp_phone_crypto_permission` permission
JOIN `sys_menu` parent
  ON parent.`permission_code` = permission.`parent_permission_code` AND parent.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = 'BUTTON',
  `updated_by` = VALUES(`updated_by`), `updated_time` = VALUES(`updated_time`), `deleted_flag` = 0;

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `created_by`, `created_time`, `deleted_flag`)
SELECT @phone_crypto_super_role_id, menu.`menu_id`, @phone_crypto_admin_user_id,
       @phone_crypto_permission_time, 0
FROM `sys_menu` menu
JOIN `tmp_phone_crypto_permission` permission ON permission.`permission_code` = menu.`permission_code`
WHERE @phone_crypto_super_role_id IS NOT NULL AND menu.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE `deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_phone_crypto_permission`;
COMMIT;

SELECT menu.`permission_code`, menu.`menu_name`, relation.`role_id`
FROM `sys_menu` menu
LEFT JOIN `sys_role_menu` relation
  ON relation.`menu_id` = menu.`menu_id` AND relation.`role_id` = @phone_crypto_super_role_id
 AND relation.`deleted_flag` = 0
WHERE menu.`permission_code` IN (
  'org:employee:phone:view-full', 'org:employee:phone:write',
  'mdm:customer:contact-phone:view-full', 'mdm:customer:contact-phone:write',
  'mdm:supplier:contact-phone:view-full', 'mdm:supplier:contact-phone:write'
)
ORDER BY menu.`permission_code`;
