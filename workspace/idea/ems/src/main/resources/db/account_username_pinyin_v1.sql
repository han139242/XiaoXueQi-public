-- 羽毛球 MES 岗位账号拼音化迁移 V1。
-- 仅修改登录用户名；user_id、员工绑定、密码哈希、角色和业务审计归属保持不变。
-- 本脚本按员工编号定位账号，可重复执行；执行前仍应按共享库规范完成备份。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @account_pinyin_time = NOW();
SET @account_pinyin_admin_user_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  ORDER BY `user_id` LIMIT 1
);

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `tmp_account_pinyin_v1`;
CREATE TEMPORARY TABLE `tmp_account_pinyin_v1` (
  `employee_no` VARCHAR(64) PRIMARY KEY,
  `old_username` VARCHAR(64) NOT NULL,
  `new_username` VARCHAR(64) NOT NULL UNIQUE
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

INSERT INTO `tmp_account_pinyin_v1` VALUES
  ('EMP-PMC-001', 'pmc001', 'shengchanjihua001'),
  ('EMP-MFG-001', 'mfg001', 'shengchanjingli001'),
  ('EMP-MFG-101', 'mfg101', 'banzuzhang001'),
  ('EMP-MFG-102', 'mfg102', 'banzuzhang002'),
  ('EMP-MFG-111', 'mfg111', 'caozuogong001'),
  ('EMP-MFG-112', 'mfg112', 'caozuogong002'),
  ('EMP-MFG-113', 'mfg113', 'caozuogong003'),
  ('EMP-MFG-114', 'mfg114', 'caozuogong004'),
  ('EMP-MFG-115', 'mfg115', 'caozuogong005'),
  ('EMP-MFG-116', 'mfg116', 'caozuogong006'),
  ('EMP-QMS-001', 'qms001', 'zhijianyuan001'),
  ('EMP-WMS-001', 'wms001', 'cangkuguanliyuan001'),
  ('EMP-EAM-001', 'eam001', 'shebeiguanliyuan001');

UPDATE `sys_user` user
JOIN `org_employee` employee
  ON employee.`employee_id` = user.`employee_id` AND employee.`deleted_flag` = 0
JOIN `tmp_account_pinyin_v1` mapping
  ON mapping.`employee_no` = employee.`employee_no`
SET user.`username` = mapping.`new_username`,
    user.`updated_by` = @account_pinyin_admin_user_id,
    user.`updated_time` = @account_pinyin_time
WHERE user.`deleted_flag` = 0
  AND user.`username` = mapping.`old_username`;

COMMIT;

SELECT mapping.`new_username`, employee.`employee_no`, user.`user_id`, role.`role_code`,
       user.`status`, IF(user.`password_hash` LIKE 'pbkdf2-sha256$%', 'PBKDF2', 'INVALID') AS `password_status`
FROM `tmp_account_pinyin_v1` mapping
LEFT JOIN `org_employee` employee
  ON employee.`employee_no` = mapping.`employee_no` AND employee.`deleted_flag` = 0
LEFT JOIN `sys_user` user
  ON user.`employee_id` = employee.`employee_id` AND user.`deleted_flag` = 0
LEFT JOIN `sys_user_role` relation
  ON relation.`user_id` = user.`user_id` AND relation.`deleted_flag` = 0
LEFT JOIN `sys_role` role
  ON role.`role_id` = relation.`role_id` AND role.`deleted_flag` = 0
ORDER BY mapping.`new_username`;

SELECT
  (SELECT COUNT(*) FROM `sys_user` user
   JOIN `org_employee` employee ON employee.`employee_id` = user.`employee_id` AND employee.`deleted_flag` = 0
   JOIN `tmp_account_pinyin_v1` mapping ON mapping.`employee_no` = employee.`employee_no`
   WHERE user.`username` = mapping.`new_username` AND user.`deleted_flag` = 0) AS `renamed_accounts`,
  (SELECT COUNT(*) FROM `sys_user` WHERE `username` IN
    ('pmc001','mfg001','mfg101','mfg102','mfg111','mfg112','mfg113',
     'mfg114','mfg115','mfg116','qms001','wms001','eam001') AND `deleted_flag` = 0) AS `legacy_accounts`;

DROP TEMPORARY TABLE IF EXISTS `tmp_account_pinyin_v1`;
