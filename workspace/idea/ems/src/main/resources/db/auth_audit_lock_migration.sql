-- 认证安全、审计字段与乐观锁启用迁移。
-- 使用前请先备份并选择目标数据库；脚本不修改现有密码，可重复执行。

SET NAMES utf8mb4;

SET @schema_name = DATABASE();
SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'sys_user' AND column_name = 'login_fail_count'
  ),
  'SELECT 1',
  'ALTER TABLE `sys_user` ADD COLUMN `login_fail_count` INT NOT NULL DEFAULT 0 COMMENT ''连续登录失败次数。'' AFTER `status`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
  EXISTS(
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = @schema_name AND table_name = 'sys_user' AND column_name = 'locked_time'
  ),
  'SELECT 1',
  'ALTER TABLE `sys_user` ADD COLUMN `locked_time` DATETIME NULL COMMENT ''自动或人工锁定时间。'' AFTER `login_fail_count`'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `sys_user`
SET `login_fail_count` = 0
WHERE `login_fail_count` IS NULL;

SELECT `column_name`, `column_type`, `is_nullable`, `column_default`
FROM information_schema.columns
WHERE table_schema = @schema_name
  AND table_name = 'sys_user'
  AND column_name IN ('login_fail_count', 'locked_time')
ORDER BY `ordinal_position`;
