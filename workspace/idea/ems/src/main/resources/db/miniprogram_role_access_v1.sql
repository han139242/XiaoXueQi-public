-- Add the read and scan permissions required by the shop-floor mini program.
-- Default mode is dry-run. Set @miniprogram_access_commit = 1 before sourcing this file to commit.

SET @miniprogram_access_commit = COALESCE(@miniprogram_access_commit, 0);
SET @miniprogram_access_marker = 'MINIPROGRAM_ROLE_ACCESS_V1';

DROP TEMPORARY TABLE IF EXISTS `tmp_miniprogram_role_permission`;
CREATE TEMPORARY TABLE `tmp_miniprogram_role_permission` (
  `role_code` VARCHAR(64) NOT NULL,
  `permission_code` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`role_code`, `permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO `tmp_miniprogram_role_permission` (`role_code`, `permission_code`) VALUES
  ('PRODUCTION_OPERATOR', 'mfg:production-plan:view'),
  ('PRODUCTION_OPERATOR', 'mfg:work-order:view'),
  ('PRODUCTION_OPERATOR', 'mdm:product:view'),
  ('PRODUCTION_OPERATOR', 'process:operation:view'),
  ('PRODUCTION_OPERATOR', 'factory:station:view'),
  ('QUALITY_ENGINEER', 'trace:barcode:scan'),
  ('WAREHOUSE_MANAGER', 'trace:barcode:scan'),
  ('WAREHOUSE_MANAGER', 'eam:equipment:view'),
  ('WAREHOUSE_MANAGER', 'eam:repair-order:view'),
  ('WAREHOUSE_MANAGER', 'factory:station:view'),
  ('WAREHOUSE_MANAGER', 'org:employee:view'),
  ('EQUIPMENT_ENGINEER', 'trace:barcode:scan'),
  ('EQUIPMENT_ENGINEER', 'wms:inbound-order:view'),
  ('EQUIPMENT_ENGINEER', 'wms:inbound-detail:view'),
  ('EQUIPMENT_ENGINEER', 'wms:outbound-order:view'),
  ('EQUIPMENT_ENGINEER', 'wms:outbound-detail:view'),
  ('EQUIPMENT_ENGINEER', 'wms:inventory:view'),
  ('EQUIPMENT_ENGINEER', 'wms:warehouse:view'),
  ('EQUIPMENT_ENGINEER', 'wms:location:view'),
  ('EQUIPMENT_ENGINEER', 'wms:material-batch:view');

DROP TEMPORARY TABLE IF EXISTS `tmp_miniprogram_access_assert`;
CREATE TEMPORARY TABLE `tmp_miniprogram_access_assert` (
  `assertion_name` VARCHAR(128) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

START TRANSACTION;

SELECT role.`role_id`, role.`role_code`
FROM `sys_role` role
WHERE role.`role_code` IN ('PRODUCTION_OPERATOR', 'QUALITY_ENGINEER', 'WAREHOUSE_MANAGER', 'EQUIPMENT_ENGINEER')
FOR UPDATE;

INSERT INTO `tmp_miniprogram_access_assert` (`assertion_name`, `assertion_passed`)
SELECT 'four enabled target roles', IF(COUNT(*) = 4, 1, NULL)
FROM `sys_role` role
WHERE role.`role_code` IN ('PRODUCTION_OPERATOR', 'QUALITY_ENGINEER', 'WAREHOUSE_MANAGER', 'EQUIPMENT_ENGINEER')
  AND role.`status` = 'ENABLE' AND role.`deleted_flag` = 0;

INSERT INTO `tmp_miniprogram_access_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all permission nodes exist exactly once', IF(COUNT(*) = 20, 1, NULL)
FROM `tmp_miniprogram_role_permission` required_permission
JOIN `sys_menu` menu
  ON menu.`permission_code` = required_permission.`permission_code`
 AND menu.`deleted_flag` = 0;

SET @role_menu_count_before = (SELECT COUNT(*) FROM `sys_role_menu` WHERE `deleted_flag` = 0);
SET @missing_role_permission_count = (
  SELECT COUNT(*)
  FROM `tmp_miniprogram_role_permission` required_permission
  JOIN `sys_role` role
    ON role.`role_code` = required_permission.`role_code`
   AND role.`status` = 'ENABLE' AND role.`deleted_flag` = 0
  JOIN `sys_menu` menu
    ON menu.`permission_code` = required_permission.`permission_code`
   AND menu.`deleted_flag` = 0
  WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` role_menu
    WHERE role_menu.`role_id` = role.`role_id`
      AND role_menu.`menu_id` = menu.`menu_id`
      AND role_menu.`deleted_flag` = 0
  )
);

INSERT INTO `sys_role_menu`
  (`role_id`, `menu_id`, `created_by`, `created_time`, `deleted_flag`)
SELECT role.`role_id`, menu.`menu_id`, 1, NOW(), 0
FROM `tmp_miniprogram_role_permission` required_permission
JOIN `sys_role` role
  ON role.`role_code` = required_permission.`role_code`
 AND role.`status` = 'ENABLE' AND role.`deleted_flag` = 0
JOIN `sys_menu` menu
  ON menu.`permission_code` = required_permission.`permission_code`
 AND menu.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_role_menu` role_menu
  WHERE role_menu.`role_id` = role.`role_id`
    AND role_menu.`menu_id` = menu.`menu_id`
    AND role_menu.`deleted_flag` = 0
);
SET @inserted_role_permission_count = ROW_COUNT();

INSERT INTO `tmp_miniprogram_access_assert` (`assertion_name`, `assertion_passed`)
VALUES ('all missing role permissions inserted',
        IF(@inserted_role_permission_count = @missing_role_permission_count, 1, NULL));

INSERT INTO `tmp_miniprogram_access_assert` (`assertion_name`, `assertion_passed`)
SELECT 'role permission total changed only by migration',
       IF(COUNT(*) = @role_menu_count_before + @inserted_role_permission_count, 1, NULL)
FROM `sys_role_menu`
WHERE `deleted_flag` = 0;

INSERT INTO `tmp_miniprogram_access_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all required role permissions are active', IF(COUNT(*) = 20, 1, NULL)
FROM `tmp_miniprogram_role_permission` required_permission
JOIN `sys_role` role
  ON role.`role_code` = required_permission.`role_code`
 AND role.`status` = 'ENABLE' AND role.`deleted_flag` = 0
JOIN `sys_menu` menu
  ON menu.`permission_code` = required_permission.`permission_code`
 AND menu.`deleted_flag` = 0
JOIN `sys_role_menu` role_menu
  ON role_menu.`role_id` = role.`role_id`
 AND role_menu.`menu_id` = menu.`menu_id`
 AND role_menu.`deleted_flag` = 0;

SET @passed_assertion_count = (SELECT COUNT(*) FROM `tmp_miniprogram_access_assert`);
SET @miniprogram_access_final_statement = IF(@miniprogram_access_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE miniprogram_access_final_statement FROM @miniprogram_access_final_statement;
EXECUTE miniprogram_access_final_statement;
DEALLOCATE PREPARE miniprogram_access_final_statement;

SELECT IF(@miniprogram_access_commit = 1, 'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`,
       @inserted_role_permission_count AS `inserted_role_permissions`,
       @passed_assertion_count AS `passed_assertions`;
