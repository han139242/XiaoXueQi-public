-- Grant quality engineers the attachment permissions required by nonconforming-order photos.
-- Dry-run by default. Set @qms_photo_permission_commit = 1 before sourcing to commit.

SET @qms_photo_permission_commit = COALESCE(@qms_photo_permission_commit, 0);

START TRANSACTION;

INSERT INTO `sys_role_menu`
  (`role_id`, `menu_id`, `created_by`, `created_time`, `deleted_flag`)
SELECT role.`role_id`, menu.`menu_id`, 1, NOW(), 0
FROM `sys_role` role
JOIN `sys_menu` menu
  ON menu.`permission_code` IN ('sys:file:view', 'sys:file:upload')
 AND menu.`deleted_flag` = 0
WHERE role.`role_code` = 'QUALITY_ENGINEER'
  AND role.`status` = 'ENABLE'
  AND role.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE
  `deleted_flag` = 0;

SET @qms_photo_permission_final_statement = IF(@qms_photo_permission_commit = 1, 'COMMIT', 'ROLLBACK');
PREPARE qms_photo_permission_final_statement FROM @qms_photo_permission_final_statement;
EXECUTE qms_photo_permission_final_statement;
DEALLOCATE PREPARE qms_photo_permission_final_statement;

SELECT IF(@qms_photo_permission_commit = 1, 'COMMITTED', 'DRY_RUN_ROLLED_BACK') AS `execution_mode`;
