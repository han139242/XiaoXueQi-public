-- 联系电话静态加密 V1：仅执行结构升级，不包含密钥、明文或回填逻辑。
-- 执行前必须核验当前库为目标 mes 并完成备份；禁止用 schema.sql 替代本脚本。

SET @phone_crypto_schema = DATABASE();
SET @phone_crypto_expected_schema = 'mes';

SET @phone_crypto_target_sql = IF(
  @phone_crypto_schema = @phone_crypto_expected_schema,
  'SELECT ''target database verified'' AS `target_check`',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''phone crypto migration must run on mes database''');
PREPARE phone_crypto_target_stmt FROM @phone_crypto_target_sql;
EXECUTE phone_crypto_target_stmt;
DEALLOCATE PREPARE phone_crypto_target_stmt;

-- MySQL 8.0 兼容的幂等 DDL：新增列和索引前先检查 information_schema。
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @phone_crypto_schema AND table_name = 'org_employee' AND column_name = 'phone_lookup_hash') = 0,
  'ALTER TABLE `org_employee` MODIFY COLUMN `phone` VARCHAR(255) NULL COMMENT ''联系方式 AES-GCM 密文。'', ADD COLUMN `phone_lookup_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT ''联系电话 HMAC-SHA-256 精确检索哈希。'' AFTER `phone`',
  'ALTER TABLE `org_employee` MODIFY COLUMN `phone` VARCHAR(255) NULL COMMENT ''联系方式 AES-GCM 密文。''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @phone_crypto_schema AND table_name = 'org_employee' AND index_name = 'idx_org_employee_phone_lookup_hash') = 0,
  'ALTER TABLE `org_employee` ADD KEY `idx_org_employee_phone_lookup_hash` (`phone_lookup_hash`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @phone_crypto_schema AND table_name = 'mdm_customer' AND column_name = 'contact_phone_lookup_hash') = 0,
  'ALTER TABLE `mdm_customer` MODIFY COLUMN `contact_phone` VARCHAR(255) NULL COMMENT ''联系电话 AES-GCM 密文。'', ADD COLUMN `contact_phone_lookup_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT ''联系电话 HMAC-SHA-256 精确检索哈希。'' AFTER `contact_phone`',
  'ALTER TABLE `mdm_customer` MODIFY COLUMN `contact_phone` VARCHAR(255) NULL COMMENT ''联系电话 AES-GCM 密文。''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @phone_crypto_schema AND table_name = 'mdm_customer' AND index_name = 'idx_mdm_customer_contact_phone_lookup_hash') = 0,
  'ALTER TABLE `mdm_customer` ADD KEY `idx_mdm_customer_contact_phone_lookup_hash` (`contact_phone_lookup_hash`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = @phone_crypto_schema AND table_name = 'mdm_supplier' AND column_name = 'contact_phone_lookup_hash') = 0,
  'ALTER TABLE `mdm_supplier` MODIFY COLUMN `contact_phone` VARCHAR(255) NULL COMMENT ''联系电话 AES-GCM 密文。'', ADD COLUMN `contact_phone_lookup_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT ''联系电话 HMAC-SHA-256 精确检索哈希。'' AFTER `contact_phone`',
  'ALTER TABLE `mdm_supplier` MODIFY COLUMN `contact_phone` VARCHAR(255) NULL COMMENT ''联系电话 AES-GCM 密文。''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = @phone_crypto_schema AND table_name = 'mdm_supplier' AND index_name = 'idx_mdm_supplier_contact_phone_lookup_hash') = 0,
  'ALTER TABLE `mdm_supplier` ADD KEY `idx_mdm_supplier_contact_phone_lookup_hash` (`contact_phone_lookup_hash`)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 回填前后均可执行的只读核验基线。
SELECT 'org_employee' AS table_name, COUNT(*) AS non_null_phone_count,
       SUM(CASE WHEN `phone` LIKE 'v1:%' THEN 1 ELSE 0 END) AS encrypted_phone_count,
       SUM(CASE WHEN `phone` IS NOT NULL AND `phone_lookup_hash` IS NOT NULL THEN 1 ELSE 0 END) AS hashed_phone_count
FROM `org_employee` WHERE `deleted_flag` = 0
UNION ALL
SELECT 'mdm_customer', COUNT(*),
       SUM(CASE WHEN `contact_phone` LIKE 'v1:%' THEN 1 ELSE 0 END),
       SUM(CASE WHEN `contact_phone` IS NOT NULL AND `contact_phone_lookup_hash` IS NOT NULL THEN 1 ELSE 0 END)
FROM `mdm_customer` WHERE `deleted_flag` = 0
UNION ALL
SELECT 'mdm_supplier', COUNT(*),
       SUM(CASE WHEN `contact_phone` LIKE 'v1:%' THEN 1 ELSE 0 END),
       SUM(CASE WHEN `contact_phone` IS NOT NULL AND `contact_phone_lookup_hash` IS NOT NULL THEN 1 ELSE 0 END)
FROM `mdm_supplier` WHERE `deleted_flag` = 0;
