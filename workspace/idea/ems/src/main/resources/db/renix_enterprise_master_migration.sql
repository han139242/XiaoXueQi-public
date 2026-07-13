-- 将现有两款通用羽毛球基线原位升级为雷尼克斯企业主数据。
-- 安全顺序：本脚本 -> enterprise_baseline_data.sql -> smart_scheduling_migration.sql
--           -> enterprise_baseline_verify.sql -> smart_scheduling_verify.sql。
-- 本脚本只允许在交易数据为空时执行；不删除任何有效记录，不写交易数据。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @baseline_marker = 'ENTERPRISE_BASELINE_FINAL';
SET @migration_marker = 'RENIX_ENTERPRISE_MASTER_V1';
SET @migration_time = '2026-07-13 12:00:00';
SET @migration_operator_id = (
  SELECT `user_id`
  FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  ORDER BY `user_id`
  LIMIT 1
);

-- 利用预编译语句在不要求 CREATE ROUTINE 权限的前提下执行硬性前置检查。
SET @transaction_rows =
    (SELECT COUNT(*) FROM `mfg_production_order` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_production_plan` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_work_order` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_task` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mfg_work_report` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `wms_material_batch` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `wms_inventory` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `wms_stock_movement` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `qms_inspection_order` WHERE `deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `trace_product_lot` WHERE `deleted_flag` = 0);

SET @conflicting_codes =
    (SELECT COUNT(*) FROM `mdm_product` old_record
      JOIN `mdm_product` new_record
        ON old_record.`product_code` = 'PROD-SHUTTLE-TRAIN-77'
       AND new_record.`product_code` = 'PROD-LNX-T300-77'
       AND old_record.`product_id` <> new_record.`product_id`
     WHERE old_record.`deleted_flag` = 0 AND new_record.`deleted_flag` = 0)
  + (SELECT COUNT(*) FROM `mdm_product` old_record
      JOIN `mdm_product` new_record
        ON old_record.`product_code` = 'PROD-SHUTTLE-MATCH-77'
       AND new_record.`product_code` = 'PROD-LNX-M700-77'
       AND old_record.`product_id` <> new_record.`product_id`
     WHERE old_record.`deleted_flag` = 0 AND new_record.`deleted_flag` = 0);

SET @assertion_sql = IF(
  @transaction_rows = 0 AND @conflicting_codes = 0,
  'DO 0',
  'SELECT * FROM `__ABORT_RENIX_MASTER_PRECONDITION__`'
);
PREPARE renix_assertion_stmt FROM @assertion_sql;
EXECUTE renix_assertion_stmt;
DEALLOCATE PREPARE renix_assertion_stmt;

START TRANSACTION;

-- 1. 企业和生产组织名称。
UPDATE `org_department`
SET `department_name` = '雷尼克斯制造中心',
    `remark` = CONCAT(@baseline_marker, ': 雷尼克斯体育用品制造有限公司制造组织根节点'),
    `updated_by` = @migration_operator_id,
    `updated_time` = @migration_time
WHERE `department_code` = 'DEPT-PLANT' AND `deleted_flag` = 0;

UPDATE `org_team`
SET `remark` = CONCAT(@baseline_marker, ': ', CASE `team_code`
      WHEN 'TEAM-TRAIN-01' THEN '负责雷尼克斯T300及迅羽C500 OEM产品生产'
      ELSE '负责雷尼克斯M700比赛级产品生产' END),
    `updated_by` = @migration_operator_id,
    `updated_time` = @migration_time
WHERE `team_code` IN ('TEAM-TRAIN-01', 'TEAM-MATCH-01') AND `deleted_flag` = 0;

UPDATE `factory_workshop`
SET `workshop_name` = '雷尼克斯羽毛球制造车间',
    `remark` = CONCAT(@baseline_marker, ': 雷尼克斯羽毛球生产主车间'),
    `updated_by` = @migration_operator_id,
    `updated_time` = @migration_time
WHERE `workshop_code` = 'WS-SHUTTLE-01' AND `deleted_flag` = 0;

-- 2. 原位升级已有供应商和客户；新增档案由最终基线脚本补齐。
UPDATE `mdm_supplier`
SET `supplier_code` = 'SUP-DUCK-001',
    `supplier_name` = '皖东羽材精选有限公司',
    `contact_name` = '采购对接人', `contact_phone` = NULL, `status` = 'ENABLE',
    `remark` = CONCAT(@baseline_marker, ': 供应A级鸭毛片'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `supplier_code` = 'SUP-FEATHER-001' AND `deleted_flag` = 0
  AND NOT EXISTS (SELECT 1 FROM (SELECT `supplier_code` FROM `mdm_supplier`) code_guard WHERE code_guard.`supplier_code` = 'SUP-DUCK-001');

UPDATE `mdm_supplier`
SET `supplier_name` = '华南软木制品有限公司', `contact_name` = '采购对接人',
    `contact_phone` = NULL, `status` = 'ENABLE',
    `remark` = CONCAT(@baseline_marker, ': 供应76/77速复合及77/78速天然软木球头'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `supplier_code` = 'SUP-CORK-001' AND `deleted_flag` = 0;

UPDATE `mdm_supplier`
SET `supplier_name` = '新彩包装制品有限公司', `contact_name` = '采购对接人',
    `contact_phone` = NULL, `status` = 'ENABLE',
    `remark` = CONCAT(@baseline_marker, ': 供应雷尼克斯和迅羽OEM包装材料'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `supplier_code` = 'SUP-PACK-001' AND `deleted_flag` = 0;

UPDATE `mdm_customer`
SET `customer_name` = '城际运动装备有限公司', `contact_name` = '渠道对接人',
    `contact_phone` = NULL, `status` = 'ENABLE',
    `remark` = CONCAT(@baseline_marker, ': 区域经销客户，采购雷尼克斯全系列'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `customer_code` = 'CUS-DIST-001' AND `deleted_flag` = 0;

UPDATE `mdm_customer`
SET `customer_name` = '凌云羽毛球俱乐部', `contact_name` = '俱乐部对接人',
    `contact_phone` = NULL, `status` = 'ENABLE',
    `remark` = CONCAT(@baseline_marker, ': 俱乐部客户，主要采购M700 77速'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `customer_code` = 'CUS-CLUB-001' AND `deleted_flag` = 0;

-- 3. 原位升级已有物料编码，保留 material_id 和所有逻辑关联。
UPDATE `mdm_material`
SET `spec_model` = '64-70mm',
    `remark` = CONCAT(@baseline_marker, ': T300和迅羽C500用毛片'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'RM-FEATHER-DUCK-A' AND `deleted_flag` = 0;

UPDATE `mdm_material`
SET `spec_model` = '64-70mm',
    `remark` = CONCAT(@baseline_marker, ': M700比赛级羽毛球用毛片'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'RM-FEATHER-GOOSE-A' AND `deleted_flag` = 0;

UPDATE `mdm_material`
SET `material_code` = 'RM-CORK-COMPOSITE-77', `material_name` = '77速复合软木球头',
    `spec_model` = '77速配重/直径25-28mm',
    `remark` = CONCAT(@baseline_marker, ': T300 77速和迅羽C500球头'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'RM-CORK-COMPOSITE' AND `deleted_flag` = 0;

UPDATE `mdm_material`
SET `material_code` = 'RM-CORK-NATURAL-77', `material_name` = '77速天然软木球头',
    `spec_model` = '77速配重/直径25-28mm',
    `remark` = CONCAT(@baseline_marker, ': M700 77速球头'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'RM-CORK-NATURAL' AND `deleted_flag` = 0;

UPDATE `mdm_material`
SET `material_code` = 'AUX-THREAD-NYLON-07', `material_name` = '高强尼龙绕线',
    `spec_model` = '0.7mm', `remark` = CONCAT(@baseline_marker, ': 全部产品毛片固定绕线'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'AUX-THREAD-NYLON' AND `deleted_flag` = 0;

UPDATE `mdm_material`
SET `material_code` = 'PKG-TUBE-LNX-T300', `material_name` = '雷尼克斯T300十二只装球筒',
    `spec_model` = '12只/筒', `remark` = CONCAT(@baseline_marker, ': T300 76/77速包装'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'PKG-TUBE-12' AND `deleted_flag` = 0;

UPDATE `mdm_material`
SET `material_code` = 'PKG-LABEL-LNX-T300-77', `material_name` = '雷尼克斯T300 77速标签',
    `spec_model` = 'LNX-T300-77', `remark` = CONCAT(@baseline_marker, ': T300 77速成品标识'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'PKG-LABEL-TRAIN-77' AND `deleted_flag` = 0;

UPDATE `mdm_material`
SET `material_code` = 'PKG-LABEL-LNX-M700-77', `material_name` = '雷尼克斯M700 77速标签',
    `spec_model` = 'LNX-M700-77', `remark` = CONCAT(@baseline_marker, ': M700 77速成品标识'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'PKG-LABEL-MATCH-77' AND `deleted_flag` = 0;

UPDATE `mdm_material`
SET `material_code` = 'PKG-CARTON-LNX-25', `material_name` = '雷尼克斯二十五筒装运输外箱',
    `spec_model` = '25筒/箱', `remark` = CONCAT(@baseline_marker, ': 雷尼克斯成品运输外包装'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'PKG-CARTON-25' AND `deleted_flag` = 0;

UPDATE `mdm_material`
SET `material_code` = 'FG-LNX-T300-77', `material_name` = '雷尼克斯T300训练级羽毛球77速',
    `spec_model` = 'T300/77速/12只装', `remark` = CONCAT(@baseline_marker, ': 常温日常训练和校园渠道'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'FG-SHUTTLE-TRAIN-77' AND `deleted_flag` = 0;

UPDATE `mdm_material`
SET `material_code` = 'FG-LNX-M700-77', `material_name` = '雷尼克斯M700比赛级羽毛球77速',
    `spec_model` = 'M700/77速/12只装', `remark` = CONCAT(@baseline_marker, ': 俱乐部和常规赛事'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `material_code` = 'FG-SHUTTLE-MATCH-77' AND `deleted_flag` = 0;

-- 比赛级已有BOM需要独立球筒；先补该物料，剩余新物料由最终基线补齐。
INSERT INTO `mdm_material`
  (`material_code`, `material_name`, `category_id`, `spec_model`, `unit_id`, `material_type`, `status`,
   `remark`, `created_by`, `created_time`)
SELECT 'PKG-TUBE-LNX-M700', '雷尼克斯M700十二只装球筒', category.`category_id`, '12只/筒', unit_record.`unit_id`,
       'AUXILIARY', 'ENABLE', CONCAT(@baseline_marker, ': M700 77/78速包装'),
       @migration_operator_id, @migration_time
FROM `mdm_material_category` category
JOIN `mdm_unit` unit_record ON unit_record.`unit_code` = 'EA' AND unit_record.`deleted_flag` = 0
WHERE category.`category_code` = 'MAT-PACK' AND category.`deleted_flag` = 0
  AND NOT EXISTS (SELECT 1 FROM `mdm_material` WHERE `material_code` = 'PKG-TUBE-LNX-M700');

-- 4. 原位升级两款产品、BOM和路线。
UPDATE `mdm_product`
SET `product_code` = 'PROD-LNX-T300-77', `product_name` = '雷尼克斯T300训练级羽毛球77速',
    `product_model` = 'T300-77-12', `status` = 'ENABLE',
    `remark` = CONCAT(@baseline_marker, ': 常温日常训练和校园渠道产品'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `product_code` = 'PROD-SHUTTLE-TRAIN-77' AND `deleted_flag` = 0;

UPDATE `mdm_product`
SET `product_code` = 'PROD-LNX-M700-77', `product_name` = '雷尼克斯M700比赛级羽毛球77速',
    `product_model` = 'M700-77-12', `status` = 'ENABLE',
    `remark` = CONCAT(@baseline_marker, ': 俱乐部和常规赛事产品'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `product_code` = 'PROD-SHUTTLE-MATCH-77' AND `deleted_flag` = 0;

UPDATE `mdm_bom`
SET `bom_code` = 'BOM-LNX-T300-77-V1', `remark` = CONCAT(@baseline_marker, ': 每筒12只标准BOM'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `bom_code` = 'BOM-TRAIN-77-V1' AND `deleted_flag` = 0;

UPDATE `mdm_bom`
SET `bom_code` = 'BOM-LNX-M700-77-V1', `remark` = CONCAT(@baseline_marker, ': 每筒12只标准BOM'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `bom_code` = 'BOM-MATCH-77-V1' AND `deleted_flag` = 0;

UPDATE `process_route`
SET `route_code` = 'ROUTE-LNX-T300-77-V1', `route_name` = '雷尼克斯T300 77速标准工艺路线',
    `remark` = CONCAT(@baseline_marker, ': 标准生产工艺路线'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `route_code` = 'ROUTE-TRAIN-77-V1' AND `deleted_flag` = 0;

UPDATE `process_route`
SET `route_code` = 'ROUTE-LNX-M700-77-V1', `route_name` = '雷尼克斯M700 77速标准工艺路线',
    `remark` = CONCAT(@baseline_marker, ': 标准生产工艺路线'),
    `updated_by` = @migration_operator_id, `updated_time` = @migration_time
WHERE `route_code` = 'ROUTE-MATCH-77-V1' AND `deleted_flag` = 0;

UPDATE `mdm_bom_item` item
JOIN `mdm_bom` bom ON bom.`bom_id` = item.`bom_id` AND bom.`bom_code` = 'BOM-LNX-M700-77-V1'
JOIN `mdm_material` material ON material.`material_code` = 'PKG-TUBE-LNX-M700' AND material.`deleted_flag` = 0
SET item.`material_id` = material.`material_id`,
    item.`updated_by` = @migration_operator_id,
    item.`updated_time` = @migration_time
WHERE item.`sequence_no` = 50 AND item.`deleted_flag` = 0;

-- 5. 原位升级两款既有产品的质量标准和方案。
UPDATE `qms_inspection_standard` standard_record
JOIN (
  SELECT 'STD-IQC-TRAIN-FEATHER-LENGTH' AS `old_key`, 'STD-LNX-T300-77-IQC-FEATHER-LENGTH' AS `new_key`, '67' AS `standard_value`, '70' AS `upper_limit`, '64' AS `lower_limit`, '毛片长度在64-70mm内判定合格' AS `judgement_rule`
  UNION ALL SELECT 'STD-IQC-TRAIN-HEAD-DIAMETER', 'STD-LNX-T300-77-IQC-HEAD-DIAMETER', '26.5', '28', '25', '球头直径在25-28mm内判定合格'
  UNION ALL SELECT 'STD-IQC-TRAIN-RAW-APPEARANCE', 'STD-LNX-T300-77-IQC-RAW-APPEARANCE', '合格', NULL, NULL, '无断毛、霉变、油污、裂纹和明显色差判定合格'
  UNION ALL SELECT 'STD-IPQC-TRAIN-WEIGHT', 'STD-LNX-T300-77-IPQC-WEIGHT', '5.00', '5.05', '4.95', '单球重量在4.95-5.05g内判定合格'
  UNION ALL SELECT 'STD-IPQC-TRAIN-SPEED', 'STD-LNX-T300-77-IPQC-SPEED', '77', '77', '77', '标准环境下检测等级等于77判定合格'
  UNION ALL SELECT 'STD-IPQC-TRAIN-FLIGHT', 'STD-LNX-T300-77-IPQC-FLIGHT', '500', '500', '0', '最大落点径向偏差不超过500mm判定合格'
  UNION ALL SELECT 'STD-FQC-TRAIN-APPEARANCE', 'STD-LNX-T300-77-FQC-APPEARANCE', '合格', NULL, NULL, '球头、毛片、绕线、胶层、球筒和标签均无明显缺陷判定合格'
  UNION ALL SELECT 'STD-IQC-MATCH-FEATHER-LENGTH', 'STD-LNX-M700-77-IQC-FEATHER-LENGTH', '67', '70', '64', '毛片长度在64-70mm内判定合格'
  UNION ALL SELECT 'STD-IQC-MATCH-HEAD-DIAMETER', 'STD-LNX-M700-77-IQC-HEAD-DIAMETER', '26.5', '28', '25', '球头直径在25-28mm内判定合格'
  UNION ALL SELECT 'STD-IQC-MATCH-RAW-APPEARANCE', 'STD-LNX-M700-77-IQC-RAW-APPEARANCE', '合格', NULL, NULL, '无断毛、霉变、油污、裂纹和明显色差判定合格'
  UNION ALL SELECT 'STD-IPQC-MATCH-WEIGHT', 'STD-LNX-M700-77-IPQC-WEIGHT', '5.00', '5.05', '4.95', '单球重量在4.95-5.05g内判定合格'
  UNION ALL SELECT 'STD-IPQC-MATCH-SPEED', 'STD-LNX-M700-77-IPQC-SPEED', '77', '77', '77', '标准环境下检测等级等于77判定合格'
  UNION ALL SELECT 'STD-IPQC-MATCH-FLIGHT', 'STD-LNX-M700-77-IPQC-FLIGHT', '300', '300', '0', '最大落点径向偏差不超过300mm判定合格'
  UNION ALL SELECT 'STD-FQC-MATCH-APPEARANCE', 'STD-LNX-M700-77-FQC-APPEARANCE', '合格', NULL, NULL, '球头、毛片、绕线、胶层、球筒和标签均无明显缺陷判定合格'
) seed ON standard_record.`remark` = CONCAT(@baseline_marker, ':', seed.`old_key`)
SET standard_record.`standard_value` = seed.`standard_value`,
    standard_record.`upper_limit` = seed.`upper_limit`,
    standard_record.`lower_limit` = seed.`lower_limit`,
    standard_record.`judgement_rule` = seed.`judgement_rule`,
    standard_record.`remark` = CONCAT(@baseline_marker, ':', seed.`new_key`),
    standard_record.`updated_by` = @migration_operator_id,
    standard_record.`updated_time` = @migration_time
WHERE standard_record.`deleted_flag` = 0;

UPDATE `qms_inspection_scheme` scheme
JOIN (
  SELECT 'QMS-IQC-TRAIN-77' AS `old_code`, 'QMS-IQC-LNX-T300-77' AS `new_code`, '雷尼克斯T300 77速来料检验方案' AS `new_name`
  UNION ALL SELECT 'QMS-IPQC-TRAIN-77', 'QMS-IPQC-LNX-T300-77', '雷尼克斯T300 77速过程检验方案'
  UNION ALL SELECT 'QMS-FQC-TRAIN-77', 'QMS-FQC-LNX-T300-77', '雷尼克斯T300 77速最终检验方案'
  UNION ALL SELECT 'QMS-IQC-MATCH-77', 'QMS-IQC-LNX-M700-77', '雷尼克斯M700 77速来料检验方案'
  UNION ALL SELECT 'QMS-IPQC-MATCH-77', 'QMS-IPQC-LNX-M700-77', '雷尼克斯M700 77速过程检验方案'
  UNION ALL SELECT 'QMS-FQC-MATCH-77', 'QMS-FQC-LNX-M700-77', '雷尼克斯M700 77速最终检验方案'
) seed ON scheme.`scheme_code` = seed.`old_code`
SET scheme.`scheme_code` = seed.`new_code`,
    scheme.`scheme_name` = seed.`new_name`,
    scheme.`updated_by` = @migration_operator_id,
    scheme.`updated_time` = @migration_time
WHERE scheme.`deleted_flag` = 0;

COMMIT;

SELECT @migration_marker AS `migration_marker`, @migration_time AS `migration_time`;
SELECT 'renamed_products' AS `check_name`, COUNT(*) AS `actual_value`
FROM `mdm_product`
WHERE `deleted_flag` = 0 AND `product_code` IN ('PROD-LNX-T300-77', 'PROD-LNX-M700-77')
UNION ALL
SELECT 'renamed_boms', COUNT(*) FROM `mdm_bom`
WHERE `deleted_flag` = 0 AND `bom_code` IN ('BOM-LNX-T300-77-V1', 'BOM-LNX-M700-77-V1')
UNION ALL
SELECT 'renamed_routes', COUNT(*) FROM `process_route`
WHERE `deleted_flag` = 0 AND `route_code` IN ('ROUTE-LNX-T300-77-V1', 'ROUTE-LNX-M700-77-V1');
