-- Add the work-order material issue permission for controlled production roles.
-- Default mode is dry-run. Set @work_order_material_issue_permission_commit = 1 before SOURCE to commit.

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @work_order_material_issue_permission_commit = COALESCE(@work_order_material_issue_permission_commit, 0);
SET @work_order_material_issue_permission_time = COALESCE(@work_order_material_issue_permission_time, CURRENT_TIMESTAMP);
SET @work_order_material_issue_permission_marker = 'WORK_ORDER_MATERIAL_ISSUE_PERMISSION_V1';

DROP TEMPORARY TABLE IF EXISTS `tmp_work_order_material_issue_allowed_role`;
CREATE TEMPORARY TABLE `tmp_work_order_material_issue_allowed_role` (
  `role_code` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
INSERT INTO `tmp_work_order_material_issue_allowed_role` VALUES
  ('SUPER_ADMIN'), ('MES_MANAGER'), ('PRODUCTION_SUPERVISOR');

DROP TEMPORARY TABLE IF EXISTS `tmp_work_order_material_issue_assert`;
CREATE TEMPORARY TABLE `tmp_work_order_material_issue_assert` (
  `assertion_name` VARCHAR(160) NOT NULL,
  `assertion_passed` TINYINT NOT NULL,
  PRIMARY KEY (`assertion_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

START TRANSACTION;

SET @work_order_material_issue_admin_id = (
  SELECT user.`user_id` FROM `sys_user` user
  WHERE user.`username` = 'admin' AND user.`deleted_flag` = 0
  LIMIT 1
);
SET @work_order_material_issue_parent_id = (
  SELECT menu.`menu_id` FROM `sys_menu` menu
  WHERE menu.`permission_code` = 'mfg:work-order:view' AND menu.`deleted_flag` = 0
  LIMIT 1
);

INSERT INTO `tmp_work_order_material_issue_assert` (`assertion_name`, `assertion_passed`)
VALUES ('admin user exists', IF(@work_order_material_issue_admin_id IS NOT NULL, 1, NULL)),
       ('work order view permission exists', IF(@work_order_material_issue_parent_id IS NOT NULL, 1, NULL));

INSERT INTO `tmp_work_order_material_issue_assert` (`assertion_name`, `assertion_passed`)
SELECT 'all intended roles exist and are enabled', IF(COUNT(*) = 3, 1, NULL)
FROM `sys_role` role
JOIN `tmp_work_order_material_issue_allowed_role` allowed ON allowed.`role_code` = role.`role_code`
WHERE role.`status` = 'ENABLE' AND role.`deleted_flag` = 0;

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
VALUES
  (@work_order_material_issue_parent_id, '从生产工单发起领料', 'BUTTON',
   'mfg:work-order:material-issue', NULL,
   CONCAT(@work_order_material_issue_permission_marker, ': derive FIFO material issue from work order'), 0,
   @work_order_material_issue_admin_id, @work_order_material_issue_permission_time,
   @work_order_material_issue_admin_id, @work_order_material_issue_permission_time, 0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = 'BUTTON',
  `remark` = VALUES(`remark`), `deleted_flag` = 0;
SET @work_order_material_issue_menu_changed = ROW_COUNT();

SET @work_order_material_issue_menu_id = (
  SELECT menu.`menu_id` FROM `sys_menu` menu
  WHERE menu.`permission_code` = 'mfg:work-order:material-issue' AND menu.`deleted_flag` = 0
  LIMIT 1
);

UPDATE `sys_role_menu` relation
JOIN `sys_role` role ON role.`role_id` = relation.`role_id` AND role.`deleted_flag` = 0
LEFT JOIN `tmp_work_order_material_issue_allowed_role` allowed ON allowed.`role_code` = role.`role_code`
SET relation.`deleted_flag` = 1
WHERE relation.`menu_id` = @work_order_material_issue_menu_id
  AND allowed.`role_code` IS NULL AND relation.`deleted_flag` = 0;
SET @work_order_material_issue_grants_removed = ROW_COUNT();

INSERT INTO `sys_role_menu`
  (`role_id`, `menu_id`, `created_by`, `created_time`, `deleted_flag`)
SELECT role.`role_id`, @work_order_material_issue_menu_id,
       @work_order_material_issue_admin_id, @work_order_material_issue_permission_time, 0
FROM `sys_role` role
JOIN `tmp_work_order_material_issue_allowed_role` allowed ON allowed.`role_code` = role.`role_code`
WHERE role.`status` = 'ENABLE' AND role.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE `deleted_flag` = 0;
SET @work_order_material_issue_grants_changed = ROW_COUNT();

SELECT @work_order_material_issue_permission_commit AS `commit_mode`,
       @work_order_material_issue_menu_changed AS `menu_rows_changed`,
       @work_order_material_issue_grants_changed AS `grant_rows_changed`,
       @work_order_material_issue_grants_removed AS `unexpected_grants_removed`;

SELECT role.`role_code`, menu.`permission_code`, relation.`deleted_flag`
FROM `sys_role_menu` relation
JOIN `sys_role` role ON role.`role_id` = relation.`role_id`
JOIN `sys_menu` menu ON menu.`menu_id` = relation.`menu_id`
WHERE menu.`permission_code` = 'mfg:work-order:material-issue'
ORDER BY role.`role_code`;

SET @work_order_material_issue_permission_finish_sql = IF(
  @work_order_material_issue_permission_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE work_order_material_issue_permission_finish FROM @work_order_material_issue_permission_finish_sql;
EXECUTE work_order_material_issue_permission_finish;
DEALLOCATE PREPARE work_order_material_issue_permission_finish;
