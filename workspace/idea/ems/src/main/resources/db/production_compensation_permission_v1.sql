-- Add controlled production compensation and nonconforming disposition permissions.
-- Default mode is dry-run. Set @production_compensation_permission_commit = 1 before SOURCE to commit.

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @production_compensation_permission_commit = COALESCE(@production_compensation_permission_commit, 0);
SET @production_compensation_permission_time = COALESCE(@production_compensation_permission_time, CURRENT_TIMESTAMP);
SET @production_compensation_permission_marker = 'PRODUCTION_COMPENSATION_PERMISSION_V1';

DROP TEMPORARY TABLE IF EXISTS `tmp_production_compensation_permission`;
CREATE TEMPORARY TABLE `tmp_production_compensation_permission` (
  `permission_code` VARCHAR(128) NOT NULL,
  `parent_permission_code` VARCHAR(128) NOT NULL,
  `menu_name` VARCHAR(128) NOT NULL,
  PRIMARY KEY (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
INSERT INTO `tmp_production_compensation_permission` VALUES
  ('mfg:task:controlled-close', 'mfg:task:view', '受控提前关闭生产任务'),
  ('qms:nonconforming-order:generate', 'qms:nonconforming-order:view', '从不合格检验发起处置'),
  ('qms:nonconforming-order:complete', 'qms:nonconforming-order:view', '完成不合格品处置');

DROP TEMPORARY TABLE IF EXISTS `tmp_production_compensation_grant`;
CREATE TEMPORARY TABLE `tmp_production_compensation_grant` (
  `permission_code` VARCHAR(128) NOT NULL,
  `role_code` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`permission_code`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
INSERT INTO `tmp_production_compensation_grant` VALUES
  ('mfg:task:controlled-close', 'SUPER_ADMIN'),
  ('mfg:task:controlled-close', 'MES_MANAGER'),
  ('mfg:task:controlled-close', 'PRODUCTION_SUPERVISOR'),
  ('qms:nonconforming-order:generate', 'SUPER_ADMIN'),
  ('qms:nonconforming-order:generate', 'MES_MANAGER'),
  ('qms:nonconforming-order:generate', 'QUALITY_ENGINEER'),
  ('qms:nonconforming-order:complete', 'SUPER_ADMIN'),
  ('qms:nonconforming-order:complete', 'MES_MANAGER'),
  ('qms:nonconforming-order:complete', 'QUALITY_ENGINEER');

DROP TEMPORARY TABLE IF EXISTS `tmp_production_compensation_assert`;
CREATE TEMPORARY TABLE `tmp_production_compensation_assert` (
  `assertion_name` VARCHAR(160) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

START TRANSACTION;

SET @production_compensation_admin_id = (
  SELECT user.`user_id` FROM `sys_user` user
  WHERE user.`username` = 'admin' AND user.`deleted_flag` = 0 LIMIT 1
);

INSERT INTO `tmp_production_compensation_assert` (`assertion_name`, `assertion_passed`)
VALUES ('admin user exists', IF(@production_compensation_admin_id IS NOT NULL, 1, NULL));

INSERT INTO `tmp_production_compensation_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all parent permissions exist', IF(COUNT(DISTINCT parent.`permission_code`) = 2, 1, NULL)
FROM `sys_menu` parent
WHERE parent.`permission_code` IN ('mfg:task:view', 'qms:nonconforming-order:view')
  AND parent.`deleted_flag` = 0;

INSERT INTO `tmp_production_compensation_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all intended roles exist and are enabled', IF(COUNT(DISTINCT role.`role_code`) = 4, 1, NULL)
FROM `sys_role` role
WHERE role.`role_code` IN ('SUPER_ADMIN', 'MES_MANAGER', 'PRODUCTION_SUPERVISOR', 'QUALITY_ENGINEER')
  AND role.`status` = 'ENABLE' AND role.`deleted_flag` = 0;

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT parent.`menu_id`, definition.`menu_name`, 'BUTTON', definition.`permission_code`, NULL,
       CONCAT(@production_compensation_permission_marker, ': controlled compensation command'), 0,
       @production_compensation_admin_id, @production_compensation_permission_time,
       @production_compensation_admin_id, @production_compensation_permission_time, 0
FROM `tmp_production_compensation_permission` definition
JOIN `sys_menu` parent
  ON parent.`permission_code` = definition.`parent_permission_code` AND parent.`deleted_flag` = 0
WHERE @production_compensation_admin_id IS NOT NULL
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = 'BUTTON',
  `remark` = VALUES(`remark`), `deleted_flag` = 0;
SET @production_compensation_menu_changed = ROW_COUNT();

UPDATE `sys_role_menu` relation
JOIN `sys_menu` menu ON menu.`menu_id` = relation.`menu_id`
JOIN `tmp_production_compensation_permission` definition
  ON definition.`permission_code` = menu.`permission_code`
JOIN `sys_role` role ON role.`role_id` = relation.`role_id` AND role.`deleted_flag` = 0
LEFT JOIN `tmp_production_compensation_grant` allowed
  ON allowed.`permission_code` = menu.`permission_code` AND allowed.`role_code` = role.`role_code`
SET relation.`deleted_flag` = 1
WHERE allowed.`role_code` IS NULL AND relation.`deleted_flag` = 0;
SET @production_compensation_grants_removed = ROW_COUNT();

INSERT INTO `sys_role_menu`
  (`role_id`, `menu_id`, `created_by`, `created_time`, `deleted_flag`)
SELECT role.`role_id`, menu.`menu_id`, @production_compensation_admin_id,
       @production_compensation_permission_time, 0
FROM `tmp_production_compensation_grant` allowed
JOIN `sys_role` role
  ON role.`role_code` = allowed.`role_code` AND role.`status` = 'ENABLE' AND role.`deleted_flag` = 0
JOIN `sys_menu` menu
  ON menu.`permission_code` = allowed.`permission_code` AND menu.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE `deleted_flag` = 0;
SET @production_compensation_grants_changed = ROW_COUNT();

SELECT @production_compensation_permission_commit AS `commit_mode`,
       @production_compensation_menu_changed AS `menu_rows_changed`,
       @production_compensation_grants_changed AS `grant_rows_changed`,
       @production_compensation_grants_removed AS `unexpected_grants_removed`;

SELECT role.`role_code`, menu.`permission_code`, relation.`deleted_flag`
FROM `sys_role_menu` relation
JOIN `sys_role` role ON role.`role_id` = relation.`role_id`
JOIN `sys_menu` menu ON menu.`menu_id` = relation.`menu_id`
JOIN `tmp_production_compensation_permission` definition
  ON definition.`permission_code` = menu.`permission_code`
ORDER BY menu.`permission_code`, role.`role_code`;

SET @production_compensation_permission_finish_sql = IF(
  @production_compensation_permission_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE production_compensation_permission_finish FROM @production_compensation_permission_finish_sql;
EXECUTE production_compensation_permission_finish;
DEALLOCATE PREPARE production_compensation_permission_finish;
