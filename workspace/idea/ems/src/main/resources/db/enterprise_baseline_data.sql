-- 羽毛球 MES 最终企业基线主数据。
-- 执行顺序：schema.sql（新库）或 master_data_type_migration.sql（现有库） -> 本脚本。
-- 使用前请先备份数据库并选择目标库；本脚本按业务编码补缺，仅规范化带最终基线标识的数据，可重复执行。
-- 唯一清理动作：软删除所有业务字段均为空的历史设备占位记录，不处理任何有效设备。
-- 本脚本不创建订单、工单、任务、库存余额、库存流水、检验结果等交易数据。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @baseline_marker = 'ENTERPRISE_BASELINE_FINAL';
SET @baseline_time = CURRENT_TIMESTAMP();
SET @baseline_operator_id = (
  SELECT `user_id`
  FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  ORDER BY `user_id`
  LIMIT 1
);

START TRANSACTION;

-- 0. 清理无法参与任何业务的全空设备占位记录，保留物理记录便于审计和回滚。
UPDATE `eam_equipment`
SET `deleted_flag` = 1,
    `remark` = CONCAT(@baseline_marker, ': 已软删除全空历史设备占位'),
    `updated_by` = @baseline_operator_id,
    `updated_time` = @baseline_time
WHERE `deleted_flag` = 0
  AND NULLIF(TRIM(`equipment_code`), '') IS NULL
  AND NULLIF(TRIM(`equipment_name`), '') IS NULL
  AND `category_id` IS NULL
  AND NULLIF(TRIM(`model`), '') IS NULL
  AND NULLIF(TRIM(`manufacturer`), '') IS NULL
  AND `purchase_date` IS NULL
  AND `station_id` IS NULL
  AND NULLIF(TRIM(`status`), '') IS NULL;

-- 1. 组织、人员、班组和班次
INSERT INTO `org_department`
  (`parent_id`, `department_code`, `department_name`, `manager_id`, `remark`, `created_by`, `created_time`)
SELECT NULL, 'DEPT-PLANT', '羽毛球制造中心', NULL,
       CONCAT(@baseline_marker, ': 制造组织根节点'), @baseline_operator_id, @baseline_time
WHERE NOT EXISTS (
  SELECT 1 FROM `org_department` WHERE `department_code` = 'DEPT-PLANT'
);

INSERT INTO `org_department`
  (`parent_id`, `department_code`, `department_name`, `manager_id`, `remark`, `created_by`, `created_time`)
SELECT parent_department.`department_id`, seed.`department_code`, seed.`department_name`, NULL,
       CONCAT(@baseline_marker, ': ', seed.`description`), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'DEPT-PMC' AS `department_code`, '生产计划与物控部' AS `department_name`, '负责计划、排产和物料协调' AS `description`
  UNION ALL SELECT 'DEPT-MFG', '生产制造部', '负责车间生产执行'
  UNION ALL SELECT 'DEPT-QMS', '质量管理部', '负责来料、过程和最终检验'
  UNION ALL SELECT 'DEPT-WMS', '仓储物流部', '负责仓储、收发和盘点'
  UNION ALL SELECT 'DEPT-EAM', '设备工程部', '负责设备、保养和维修'
) seed
JOIN `org_department` parent_department
  ON parent_department.`department_code` = 'DEPT-PLANT' AND parent_department.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `org_department` existing
  WHERE existing.`department_code` = seed.`department_code`
);

INSERT INTO `org_employee`
  (`employee_no`, `employee_name`, `department_id`, `position_name`, `skill_level`, `phone`, `status`,
   `remark`, `created_by`, `created_time`)
SELECT seed.`employee_no`, seed.`employee_name`, department.`department_id`, seed.`position_name`,
       seed.`skill_level`, NULL, 'ENABLE', CONCAT(@baseline_marker, ': ', seed.`responsibility`),
       @baseline_operator_id, @baseline_time
FROM (
  SELECT 'EMP-PMC-001' AS `employee_no`, '张伟' AS `employee_name`, 'DEPT-PMC' AS `department_code`,
         '生产计划主管' AS `position_name`, 'EXPERT' AS `skill_level`, '负责主计划、产能平衡和交付协调' AS `responsibility`
  UNION ALL SELECT 'EMP-MFG-001', '李娜', 'DEPT-MFG', '生产经理', 'EXPERT', '负责制造中心生产执行'
  UNION ALL SELECT 'EMP-MFG-101', '王强', 'DEPT-MFG', '训练球线班组长', 'ADVANCED', '负责训练球产线班组'
  UNION ALL SELECT 'EMP-MFG-102', '刘洋', 'DEPT-MFG', '比赛球线班组长', 'ADVANCED', '负责比赛球产线班组'
  UNION ALL SELECT 'EMP-MFG-111', '陈晨', 'DEPT-MFG', '训练球线操作员', 'INTERMEDIATE', '训练球产线多能工'
  UNION ALL SELECT 'EMP-MFG-112', '赵敏', 'DEPT-MFG', '比赛球线操作员', 'ADVANCED', '比赛球产线多能工'
  UNION ALL SELECT 'EMP-MFG-113', '马超', 'DEPT-MFG', '训练球线操作员', 'INTERMEDIATE', '训练球产线设备与包装操作'
  UNION ALL SELECT 'EMP-MFG-114', '郑欣', 'DEPT-MFG', '训练球线操作员', 'INTERMEDIATE', '训练球产线检验与物料协作'
  UNION ALL SELECT 'EMP-MFG-115', '何涛', 'DEPT-MFG', '比赛球线操作员', 'ADVANCED', '比赛球产线设备与包装操作'
  UNION ALL SELECT 'EMP-MFG-116', '唐悦', 'DEPT-MFG', '比赛球线操作员', 'ADVANCED', '比赛球产线检验与物料协作'
  UNION ALL SELECT 'EMP-QMS-001', '周杰', 'DEPT-QMS', '质量工程师', 'EXPERT', '负责质量标准和检验放行'
  UNION ALL SELECT 'EMP-WMS-001', '吴迪', 'DEPT-WMS', '仓库主管', 'ADVANCED', '负责仓储收发和库存准确性'
  UNION ALL SELECT 'EMP-EAM-001', '孙宇', 'DEPT-EAM', '设备工程师', 'ADVANCED', '负责设备保障和维修'
) seed
JOIN `org_department` department
  ON department.`department_code` = seed.`department_code` AND department.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `org_employee` existing WHERE existing.`employee_no` = seed.`employee_no`
);

UPDATE `org_department` department
JOIN (
  SELECT 'DEPT-PLANT' AS `department_code`, 'EMP-MFG-001' AS `employee_no`
  UNION ALL SELECT 'DEPT-PMC', 'EMP-PMC-001'
  UNION ALL SELECT 'DEPT-MFG', 'EMP-MFG-001'
  UNION ALL SELECT 'DEPT-QMS', 'EMP-QMS-001'
  UNION ALL SELECT 'DEPT-WMS', 'EMP-WMS-001'
  UNION ALL SELECT 'DEPT-EAM', 'EMP-EAM-001'
) manager_map ON manager_map.`department_code` = department.`department_code`
JOIN `org_employee` employee
  ON employee.`employee_no` = manager_map.`employee_no` AND employee.`deleted_flag` = 0
SET department.`manager_id` = employee.`employee_id`,
    department.`updated_by` = @baseline_operator_id,
    department.`updated_time` = @baseline_time
WHERE department.`manager_id` IS NULL
  AND department.`remark` LIKE CONCAT(@baseline_marker, ':%');

INSERT INTO `org_team`
  (`team_code`, `team_name`, `leader_id`, `department_id`, `remark`, `created_by`, `created_time`)
SELECT seed.`team_code`, seed.`team_name`, leader.`employee_id`, department.`department_id`,
       CONCAT(@baseline_marker, ': ', seed.`description`), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'TEAM-TRAIN-01' AS `team_code`, '训练球一班' AS `team_name`, 'EMP-MFG-101' AS `leader_no`,
         '负责训练级 77 速羽毛球生产' AS `description`
  UNION ALL SELECT 'TEAM-MATCH-01', '比赛球一班', 'EMP-MFG-102', '负责比赛级 77 速羽毛球生产'
) seed
JOIN `org_employee` leader
  ON leader.`employee_no` = seed.`leader_no` AND leader.`deleted_flag` = 0
JOIN `org_department` department
  ON department.`department_code` = 'DEPT-MFG' AND department.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `org_team` existing WHERE existing.`team_code` = seed.`team_code`
);

INSERT INTO `org_team_member`
  (`team_id`, `employee_id`, `join_date`, `leave_date`, `status`, `created_by`, `created_time`)
SELECT team.`team_id`, employee.`employee_id`, '2026-01-01', NULL, 'ENABLE',
       @baseline_operator_id, @baseline_time
FROM (
  SELECT 'TEAM-TRAIN-01' AS `team_code`, 'EMP-MFG-101' AS `employee_no`
  UNION ALL SELECT 'TEAM-TRAIN-01', 'EMP-MFG-111'
  UNION ALL SELECT 'TEAM-TRAIN-01', 'EMP-MFG-113'
  UNION ALL SELECT 'TEAM-TRAIN-01', 'EMP-MFG-114'
  UNION ALL SELECT 'TEAM-MATCH-01', 'EMP-MFG-102'
  UNION ALL SELECT 'TEAM-MATCH-01', 'EMP-MFG-112'
  UNION ALL SELECT 'TEAM-MATCH-01', 'EMP-MFG-115'
  UNION ALL SELECT 'TEAM-MATCH-01', 'EMP-MFG-116'
) seed
JOIN `org_team` team ON team.`team_code` = seed.`team_code` AND team.`deleted_flag` = 0
JOIN `org_employee` employee ON employee.`employee_no` = seed.`employee_no` AND employee.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `org_team_member` existing
  WHERE existing.`team_id` = team.`team_id` AND existing.`employee_id` = employee.`employee_id`
);

INSERT INTO `org_shift`
  (`shift_code`, `shift_name`, `start_time`, `end_time`, `remark`, `created_by`, `created_time`)
SELECT seed.`shift_code`, seed.`shift_name`, seed.`start_time`, seed.`end_time`,
       CONCAT(@baseline_marker, ': 每日固定班次模板'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'SHIFT-DAY' AS `shift_code`, '白班' AS `shift_name`, CAST('08:00:00' AS TIME) AS `start_time`, CAST('16:00:00' AS TIME) AS `end_time`
  UNION ALL SELECT 'SHIFT-SWING', '中班', CAST('16:00:00' AS TIME), CAST('00:00:00' AS TIME)
  UNION ALL SELECT 'SHIFT-NIGHT', '夜班', CAST('00:00:00' AS TIME), CAST('08:00:00' AS TIME)
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM `org_shift` existing WHERE existing.`shift_code` = seed.`shift_code`
);

-- 2. 计量单位、供应商和客户
INSERT INTO `mdm_unit`
  (`unit_code`, `unit_name`, `precision_scale`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`unit_code`, seed.`unit_name`, seed.`precision_scale`, 'ENABLE',
       CONCAT(@baseline_marker, ': 标准计量单位'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'PCS' AS `unit_code`, '只' AS `unit_name`, 0 AS `precision_scale`
  UNION ALL SELECT 'EA', '个', 0
  UNION ALL SELECT 'SHEET', '张', 0
  UNION ALL SELECT 'TUBE', '筒', 0
  UNION ALL SELECT 'KG', '千克', 4
  UNION ALL SELECT 'G', '克', 2
  UNION ALL SELECT 'M', '米', 3
  UNION ALL SELECT 'ML', '毫升', 2
  UNION ALL SELECT 'MM', '毫米', 2
  UNION ALL SELECT 'S', '秒', 2
  UNION ALL SELECT 'MIN', '分钟', 2
  UNION ALL SELECT 'N', '牛顿', 2
  UNION ALL SELECT 'CELSIUS', '摄氏度', 1
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_unit` existing WHERE existing.`unit_code` = seed.`unit_code`
);

INSERT INTO `mdm_supplier`
  (`supplier_code`, `supplier_name`, `contact_name`, `contact_phone`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`supplier_code`, seed.`supplier_name`, seed.`contact_name`, NULL, 'ENABLE',
       CONCAT(@baseline_marker, ': ', seed.`supply_scope`), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'SUP-FEATHER-001' AS `supplier_code`, '华东羽材供应有限公司' AS `supplier_name`,
         '采购对接人' AS `contact_name`, '供应鹅毛和鸭毛片' AS `supply_scope`
  UNION ALL SELECT 'SUP-CORK-001', '江南球头材料有限公司', '采购对接人', '供应天然软木和复合软木球头'
  UNION ALL SELECT 'SUP-PACK-001', '新锐包装制品有限公司', '采购对接人', '供应筒装包装和标签'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_supplier` existing WHERE existing.`supplier_code` = seed.`supplier_code`
);

INSERT INTO `mdm_customer`
  (`customer_code`, `customer_name`, `contact_name`, `contact_phone`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`customer_code`, seed.`customer_name`, seed.`contact_name`, NULL, 'ENABLE',
       CONCAT(@baseline_marker, ': ', seed.`customer_type`), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'CUS-DIST-001' AS `customer_code`, '全国体育用品渠道中心' AS `customer_name`,
         '渠道对接人' AS `contact_name`, '经销渠道客户' AS `customer_type`
  UNION ALL SELECT 'CUS-CLUB-001', '专业羽毛球俱乐部联盟', '赛事对接人', '俱乐部与赛事客户'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_customer` existing WHERE existing.`customer_code` = seed.`customer_code`
);

-- 3. 物料分类、物料和产品
INSERT INTO `mdm_material_category`
  (`parent_id`, `category_code`, `category_name`, `status`, `remark`, `created_by`, `created_time`)
SELECT NULL, seed.`category_code`, seed.`category_name`, 'ENABLE',
       CONCAT(@baseline_marker, ': 一级物料分类'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'MAT-RAW' AS `category_code`, '原材料' AS `category_name`
  UNION ALL SELECT 'MAT-AUX', '辅助材料'
  UNION ALL SELECT 'MAT-PACK', '包装材料'
  UNION ALL SELECT 'MAT-FG', '成品'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_material_category` existing WHERE existing.`category_code` = seed.`category_code`
);

INSERT INTO `mdm_material_category`
  (`parent_id`, `category_code`, `category_name`, `status`, `remark`, `created_by`, `created_time`)
SELECT parent_category.`category_id`, seed.`category_code`, seed.`category_name`, 'ENABLE',
       CONCAT(@baseline_marker, ': 二级原材料分类'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'MAT-RAW-FEATHER' AS `category_code`, '毛片' AS `category_name`
  UNION ALL SELECT 'MAT-RAW-CORK', '球头材料'
) seed
JOIN `mdm_material_category` parent_category
  ON parent_category.`category_code` = 'MAT-RAW' AND parent_category.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_material_category` existing WHERE existing.`category_code` = seed.`category_code`
);

INSERT INTO `mdm_material`
  (`material_code`, `material_name`, `category_id`, `spec_model`, `unit_id`, `material_type`, `status`,
   `remark`, `created_by`, `created_time`)
SELECT seed.`material_code`, seed.`material_name`, category.`category_id`, seed.`spec_model`, unit.`unit_id`,
       seed.`material_type`, 'ENABLE', CONCAT(@baseline_marker, ': ', seed.`usage_note`),
       @baseline_operator_id, @baseline_time
FROM (
  SELECT 'RM-FEATHER-DUCK-A' AS `material_code`, 'A级鸭毛片' AS `material_name`, 'MAT-RAW-FEATHER' AS `category_code`,
         '62-70mm' AS `spec_model`, 'PCS' AS `unit_code`, 'RAW' AS `material_type`, '训练级羽毛球用毛片' AS `usage_note`
  UNION ALL SELECT 'RM-FEATHER-GOOSE-A', 'A级鹅毛片', 'MAT-RAW-FEATHER', '62-70mm', 'PCS', 'RAW', '比赛级羽毛球用毛片'
  UNION ALL SELECT 'RM-CORK-COMPOSITE', '复合软木球头', 'MAT-RAW-CORK', '直径25-28mm', 'PCS', 'RAW', '训练级羽毛球球头'
  UNION ALL SELECT 'RM-CORK-NATURAL', '天然软木球头', 'MAT-RAW-CORK', '直径25-28mm', 'PCS', 'RAW', '比赛级羽毛球球头'
  UNION ALL SELECT 'AUX-THREAD-NYLON', '高强尼龙绕线', 'MAT-AUX', '0.7mm', 'M', 'AUXILIARY', '毛片固定绕线'
  UNION ALL SELECT 'AUX-ADHESIVE-RESIN', '羽毛球专用树脂胶', 'MAT-AUX', '低挥发型', 'ML', 'AUXILIARY', '植毛和绕线固化'
  UNION ALL SELECT 'PKG-TUBE-12', '十二只装球筒', 'MAT-PACK', '12只/筒', 'EA', 'AUXILIARY', '成品筒装包装'
  UNION ALL SELECT 'PKG-LABEL-TRAIN-77', '训练级77速标签', 'MAT-PACK', 'TRAIN-77', 'SHEET', 'AUXILIARY', '训练级成品标识'
  UNION ALL SELECT 'PKG-LABEL-MATCH-77', '比赛级77速标签', 'MAT-PACK', 'MATCH-77', 'SHEET', 'AUXILIARY', '比赛级成品标识'
  UNION ALL SELECT 'PKG-CARTON-25', '二十五筒装运输外箱', 'MAT-PACK', '25筒/箱', 'EA', 'AUXILIARY', '成品运输外包装'
  UNION ALL SELECT 'FG-SHUTTLE-TRAIN-77', '训练级77速羽毛球', 'MAT-FG', '77速/12只装', 'TUBE', 'FINISHED', '训练和日常使用'
  UNION ALL SELECT 'FG-SHUTTLE-MATCH-77', '比赛级77速羽毛球', 'MAT-FG', '77速/12只装', 'TUBE', 'FINISHED', '专业比赛和高强度训练'
) seed
JOIN `mdm_material_category` category
  ON category.`category_code` = seed.`category_code` AND category.`deleted_flag` = 0
JOIN `mdm_unit` unit ON unit.`unit_code` = seed.`unit_code` AND unit.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_material` existing WHERE existing.`material_code` = seed.`material_code`
);

INSERT INTO `mdm_product`
  (`product_code`, `product_name`, `product_model`, `material_id`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`product_code`, seed.`product_name`, seed.`product_model`, material.`material_id`, 'ENABLE',
       CONCAT(@baseline_marker, ': ', seed.`positioning`), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'PROD-SHUTTLE-TRAIN-77' AS `product_code`, '训练级77速羽毛球' AS `product_name`,
         'TRAIN-77-12' AS `product_model`, 'FG-SHUTTLE-TRAIN-77' AS `material_code`, '训练和日常使用产品' AS `positioning`
  UNION ALL SELECT 'PROD-SHUTTLE-MATCH-77', '比赛级77速羽毛球', 'MATCH-77-12',
                   'FG-SHUTTLE-MATCH-77', '专业比赛和高强度训练产品'
) seed
JOIN `mdm_material` material
  ON material.`material_code` = seed.`material_code` AND material.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_product` existing WHERE existing.`product_code` = seed.`product_code`
);

-- 4. 已发布 BOM；用量基准为一筒（12 只），损耗率采用小数比例。
INSERT INTO `mdm_bom`
  (`bom_code`, `product_id`, `bom_version`, `effective_date`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`bom_code`, product.`product_id`, '1.0', '2026-01-01', 'RELEASED',
       CONCAT(@baseline_marker, ': 每筒12只标准BOM'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'BOM-TRAIN-77-V1' AS `bom_code`, 'PROD-SHUTTLE-TRAIN-77' AS `product_code`
  UNION ALL SELECT 'BOM-MATCH-77-V1', 'PROD-SHUTTLE-MATCH-77'
) seed
JOIN `mdm_product` product
  ON product.`product_code` = seed.`product_code` AND product.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_bom` existing WHERE existing.`bom_code` = seed.`bom_code`
);

INSERT INTO `mdm_bom_item`
  (`bom_id`, `material_id`, `quantity`, `loss_rate`, `sequence_no`, `remark`, `created_by`, `created_time`)
SELECT bom.`bom_id`, material.`material_id`, seed.`quantity`, seed.`loss_rate`, seed.`sequence_no`,
       CONCAT(@baseline_marker, ': 一筒标准用量'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'BOM-TRAIN-77-V1' AS `bom_code`, 'RM-FEATHER-DUCK-A' AS `material_code`, 192.0000 AS `quantity`, 0.0800 AS `loss_rate`, 10 AS `sequence_no`
  UNION ALL SELECT 'BOM-TRAIN-77-V1', 'RM-CORK-COMPOSITE', 12.0000, 0.0200, 20
  UNION ALL SELECT 'BOM-TRAIN-77-V1', 'AUX-THREAD-NYLON', 4.8000, 0.0300, 30
  UNION ALL SELECT 'BOM-TRAIN-77-V1', 'AUX-ADHESIVE-RESIN', 7.2000, 0.0500, 40
  UNION ALL SELECT 'BOM-TRAIN-77-V1', 'PKG-TUBE-12', 1.0000, 0.0100, 50
  UNION ALL SELECT 'BOM-TRAIN-77-V1', 'PKG-LABEL-TRAIN-77', 1.0000, 0.0100, 60
  UNION ALL SELECT 'BOM-TRAIN-77-V1', 'PKG-CARTON-25', 0.0400, 0.0050, 70
  UNION ALL SELECT 'BOM-MATCH-77-V1', 'RM-FEATHER-GOOSE-A', 192.0000, 0.1000, 10
  UNION ALL SELECT 'BOM-MATCH-77-V1', 'RM-CORK-NATURAL', 12.0000, 0.0300, 20
  UNION ALL SELECT 'BOM-MATCH-77-V1', 'AUX-THREAD-NYLON', 4.8000, 0.0300, 30
  UNION ALL SELECT 'BOM-MATCH-77-V1', 'AUX-ADHESIVE-RESIN', 7.2000, 0.0500, 40
  UNION ALL SELECT 'BOM-MATCH-77-V1', 'PKG-TUBE-12', 1.0000, 0.0100, 50
  UNION ALL SELECT 'BOM-MATCH-77-V1', 'PKG-LABEL-MATCH-77', 1.0000, 0.0100, 60
  UNION ALL SELECT 'BOM-MATCH-77-V1', 'PKG-CARTON-25', 0.0400, 0.0050, 70
) seed
JOIN `mdm_bom` bom ON bom.`bom_code` = seed.`bom_code` AND bom.`deleted_flag` = 0
JOIN `mdm_material` material
  ON material.`material_code` = seed.`material_code` AND material.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `mdm_bom_item` existing
  WHERE existing.`bom_id` = bom.`bom_id` AND existing.`material_id` = material.`material_id`
);

-- 5. 工序、关键工艺参数和已发布工艺路线
INSERT INTO `process_operation`
  (`operation_code`, `operation_name`, `operation_type`, `standard_time`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`operation_code`, seed.`operation_name`, seed.`operation_type`, seed.`standard_time`, 'ENABLE',
       CONCAT(@baseline_marker, ': ', seed.`description`), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'OP-FEATHER-SORT' AS `operation_code`, '毛片分选' AS `operation_name`, 'PRODUCTION' AS `operation_type`,
         0.30 AS `standard_time`, '按长度、弯拱、色泽和完整性分选，工时单位为分钟/筒' AS `description`
  UNION ALL SELECT 'OP-CORK-FORM', '球头成型', 'PRODUCTION', 0.25, '球头修整、钻孔和尺寸确认，工时单位为分钟/筒'
  UNION ALL SELECT 'OP-FEATHER-INSERT', '植毛', 'PRODUCTION', 0.40, '每只球均匀植入16根毛片，工时单位为分钟/筒'
  UNION ALL SELECT 'OP-THREAD-WIND', '绕线', 'PRODUCTION', 0.30, '按规定张力完成双道绕线，工时单位为分钟/筒'
  UNION ALL SELECT 'OP-GLUE-CURE', '涂胶固化', 'PRODUCTION', 0.65, '涂胶后按批次固化，工时为折算后的分钟/筒'
  UNION ALL SELECT 'OP-SPEED-TEST', '速度与飞行检测', 'INSPECTION', 0.35, '检测速度等级、重量和飞行稳定性，工时单位为分钟/筒'
  UNION ALL SELECT 'OP-PACK', '成品包装', 'PACKAGING', 0.20, '十二只装筒、贴标和装箱前确认，工时单位为分钟/筒'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM `process_operation` existing WHERE existing.`operation_code` = seed.`operation_code`
);

INSERT INTO `process_parameter`
  (`operation_id`, `parameter_name`, `standard_value`, `upper_limit`, `lower_limit`, `unit_id`,
   `remark`, `created_by`, `created_time`)
SELECT operation.`operation_id`, seed.`parameter_name`, seed.`standard_value`, seed.`upper_limit`,
       seed.`lower_limit`, unit.`unit_id`, CONCAT(@baseline_marker, ': 受控工艺参数'),
       @baseline_operator_id, @baseline_time
FROM (
  SELECT 'OP-FEATHER-INSERT' AS `operation_code`, '单球植毛数量' AS `parameter_name`,
         '16' AS `standard_value`, '16' AS `upper_limit`, '16' AS `lower_limit`, 'PCS' AS `unit_code`
  UNION ALL SELECT 'OP-THREAD-WIND', '绕线张力', '4.0', '4.5', '3.5', 'N'
  UNION ALL SELECT 'OP-GLUE-CURE', '单球涂胶量', '0.60', '0.70', '0.50', 'ML'
  UNION ALL SELECT 'OP-GLUE-CURE', '固化温度', '45', '48', '42', 'CELSIUS'
  UNION ALL SELECT 'OP-GLUE-CURE', '固化时间', '45', '50', '40', 'MIN'
) seed
JOIN `process_operation` operation
  ON operation.`operation_code` = seed.`operation_code` AND operation.`deleted_flag` = 0
JOIN `mdm_unit` unit ON unit.`unit_code` = seed.`unit_code` AND unit.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `process_parameter` existing
  WHERE existing.`operation_id` = operation.`operation_id`
    AND existing.`parameter_name` = seed.`parameter_name`
);

INSERT INTO `process_route`
  (`route_code`, `route_name`, `product_id`, `version`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`route_code`, seed.`route_name`, product.`product_id`, '1.0', 'RELEASED',
       CONCAT(@baseline_marker, ': 标准生产工艺路线'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'ROUTE-TRAIN-77-V1' AS `route_code`, '训练级77速标准工艺路线' AS `route_name`,
         'PROD-SHUTTLE-TRAIN-77' AS `product_code`
  UNION ALL SELECT 'ROUTE-MATCH-77-V1', '比赛级77速标准工艺路线', 'PROD-SHUTTLE-MATCH-77'
) seed
JOIN `mdm_product` product
  ON product.`product_code` = seed.`product_code` AND product.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `process_route` existing WHERE existing.`route_code` = seed.`route_code`
);

INSERT INTO `process_route_item`
  (`route_id`, `operation_id`, `sequence_no`, `standard_time`, `qc_required`, `remark`, `created_by`, `created_time`)
SELECT route.`route_id`, operation.`operation_id`, seed.`sequence_no`, seed.`standard_time`, seed.`qc_required`,
       CONCAT(@baseline_marker, ': 路线级工时，分钟/筒'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'ROUTE-TRAIN-77-V1' AS `route_code`, 'OP-FEATHER-SORT' AS `operation_code`, 10 AS `sequence_no`, 0.25 AS `standard_time`, 1 AS `qc_required`
  UNION ALL SELECT 'ROUTE-TRAIN-77-V1', 'OP-CORK-FORM', 20, 0.20, 1
  UNION ALL SELECT 'ROUTE-TRAIN-77-V1', 'OP-FEATHER-INSERT', 30, 0.35, 0
  UNION ALL SELECT 'ROUTE-TRAIN-77-V1', 'OP-THREAD-WIND', 40, 0.25, 0
  UNION ALL SELECT 'ROUTE-TRAIN-77-V1', 'OP-GLUE-CURE', 50, 0.55, 0
  UNION ALL SELECT 'ROUTE-TRAIN-77-V1', 'OP-SPEED-TEST', 60, 0.30, 1
  UNION ALL SELECT 'ROUTE-TRAIN-77-V1', 'OP-PACK', 70, 0.20, 1
  UNION ALL SELECT 'ROUTE-MATCH-77-V1', 'OP-FEATHER-SORT', 10, 0.35, 1
  UNION ALL SELECT 'ROUTE-MATCH-77-V1', 'OP-CORK-FORM', 20, 0.30, 1
  UNION ALL SELECT 'ROUTE-MATCH-77-V1', 'OP-FEATHER-INSERT', 30, 0.50, 0
  UNION ALL SELECT 'ROUTE-MATCH-77-V1', 'OP-THREAD-WIND', 40, 0.35, 0
  UNION ALL SELECT 'ROUTE-MATCH-77-V1', 'OP-GLUE-CURE', 50, 0.80, 0
  UNION ALL SELECT 'ROUTE-MATCH-77-V1', 'OP-SPEED-TEST', 60, 0.45, 1
  UNION ALL SELECT 'ROUTE-MATCH-77-V1', 'OP-PACK', 70, 0.25, 1
) seed
JOIN `process_route` route ON route.`route_code` = seed.`route_code` AND route.`deleted_flag` = 0
JOIN `process_operation` operation
  ON operation.`operation_code` = seed.`operation_code` AND operation.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `process_route_item` existing
  WHERE existing.`route_id` = route.`route_id`
    AND (existing.`sequence_no` = seed.`sequence_no` OR existing.`operation_id` = operation.`operation_id`)
);

-- 6. 工厂、产线、工位和工位能力
INSERT INTO `factory_workshop`
  (`workshop_code`, `workshop_name`, `manager_id`, `status`, `remark`, `created_by`, `created_time`)
SELECT 'WS-SHUTTLE-01', '羽毛球制造车间', manager.`employee_id`, 'RUNNING',
       CONCAT(@baseline_marker, ': 羽毛球生产主车间'), @baseline_operator_id, @baseline_time
FROM `org_employee` manager
WHERE manager.`employee_no` = 'EMP-MFG-001' AND manager.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `factory_workshop` existing WHERE existing.`workshop_code` = 'WS-SHUTTLE-01'
  );

INSERT INTO `factory_line`
  (`line_code`, `line_name`, `workshop_id`, `daily_capacity`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`line_code`, seed.`line_name`, workshop.`workshop_id`, seed.`daily_capacity`, 'RUNNING',
       CONCAT(@baseline_marker, ': 日产能按单个8小时有效班次、单位筒'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'LINE-TRAIN-01' AS `line_code`, '训练球一号线' AS `line_name`, 800.0000 AS `daily_capacity`
  UNION ALL SELECT 'LINE-MATCH-01', '比赛球一号线', 500.0000
) seed
JOIN `factory_workshop` workshop
  ON workshop.`workshop_code` = 'WS-SHUTTLE-01' AND workshop.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `factory_line` existing WHERE existing.`line_code` = seed.`line_code`
);

INSERT INTO `factory_station`
  (`station_code`, `station_name`, `line_id`, `station_type`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`station_code`, seed.`station_name`, line.`line_id`, seed.`station_type`, 'RUNNING',
       CONCAT(@baseline_marker, ': ', seed.`description`), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'ST-TR-010' AS `station_code`, '训练线毛片分选工位' AS `station_name`, 'LINE-TRAIN-01' AS `line_code`, 'PRODUCTION' AS `station_type`, '工序10' AS `description`
  UNION ALL SELECT 'ST-TR-020', '训练线球头成型工位', 'LINE-TRAIN-01', 'PRODUCTION', '工序20'
  UNION ALL SELECT 'ST-TR-030', '训练线植毛工位', 'LINE-TRAIN-01', 'PRODUCTION', '工序30'
  UNION ALL SELECT 'ST-TR-040', '训练线绕线工位', 'LINE-TRAIN-01', 'PRODUCTION', '工序40'
  UNION ALL SELECT 'ST-TR-050', '训练线涂胶固化工位', 'LINE-TRAIN-01', 'PRODUCTION', '工序50'
  UNION ALL SELECT 'ST-TR-060', '训练线速度检测工位', 'LINE-TRAIN-01', 'INSPECTION', '工序60'
  UNION ALL SELECT 'ST-TR-070', '训练线包装工位', 'LINE-TRAIN-01', 'PACKAGING', '工序70'
  UNION ALL SELECT 'ST-MA-010', '比赛线毛片分选工位', 'LINE-MATCH-01', 'PRODUCTION', '工序10'
  UNION ALL SELECT 'ST-MA-020', '比赛线球头成型工位', 'LINE-MATCH-01', 'PRODUCTION', '工序20'
  UNION ALL SELECT 'ST-MA-030', '比赛线植毛工位', 'LINE-MATCH-01', 'PRODUCTION', '工序30'
  UNION ALL SELECT 'ST-MA-040', '比赛线绕线工位', 'LINE-MATCH-01', 'PRODUCTION', '工序40'
  UNION ALL SELECT 'ST-MA-050', '比赛线涂胶固化工位', 'LINE-MATCH-01', 'PRODUCTION', '工序50'
  UNION ALL SELECT 'ST-MA-060', '比赛线速度检测工位', 'LINE-MATCH-01', 'INSPECTION', '工序60'
  UNION ALL SELECT 'ST-MA-070', '比赛线包装工位', 'LINE-MATCH-01', 'PACKAGING', '工序70'
) seed
JOIN `factory_line` line ON line.`line_code` = seed.`line_code` AND line.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `factory_station` existing WHERE existing.`station_code` = seed.`station_code`
);

INSERT INTO `factory_station_operation`
  (`station_id`, `operation_id`, `is_default`, `status`, `created_by`, `created_time`)
SELECT station.`station_id`, operation.`operation_id`, 1, 'ENABLE', @baseline_operator_id, @baseline_time
FROM (
  SELECT 'ST-TR-010' AS `station_code`, 'OP-FEATHER-SORT' AS `operation_code`
  UNION ALL SELECT 'ST-TR-020', 'OP-CORK-FORM'
  UNION ALL SELECT 'ST-TR-030', 'OP-FEATHER-INSERT'
  UNION ALL SELECT 'ST-TR-040', 'OP-THREAD-WIND'
  UNION ALL SELECT 'ST-TR-050', 'OP-GLUE-CURE'
  UNION ALL SELECT 'ST-TR-060', 'OP-SPEED-TEST'
  UNION ALL SELECT 'ST-TR-070', 'OP-PACK'
  UNION ALL SELECT 'ST-MA-010', 'OP-FEATHER-SORT'
  UNION ALL SELECT 'ST-MA-020', 'OP-CORK-FORM'
  UNION ALL SELECT 'ST-MA-030', 'OP-FEATHER-INSERT'
  UNION ALL SELECT 'ST-MA-040', 'OP-THREAD-WIND'
  UNION ALL SELECT 'ST-MA-050', 'OP-GLUE-CURE'
  UNION ALL SELECT 'ST-MA-060', 'OP-SPEED-TEST'
  UNION ALL SELECT 'ST-MA-070', 'OP-PACK'
) seed
JOIN `factory_station` station
  ON station.`station_code` = seed.`station_code` AND station.`deleted_flag` = 0
JOIN `process_operation` operation
  ON operation.`operation_code` = seed.`operation_code` AND operation.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `factory_station_operation` existing
  WHERE existing.`station_id` = station.`station_id` AND existing.`operation_id` = operation.`operation_id`
);

-- 7. 仓库和库位；库存必须由正式入库业务生成。
INSERT INTO `wms_warehouse`
  (`warehouse_code`, `warehouse_name`, `warehouse_type`, `manager_id`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`warehouse_code`, seed.`warehouse_name`, seed.`warehouse_type`, manager.`employee_id`, 'ENABLE',
       CONCAT(@baseline_marker, ': ', seed.`description`), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'WH-RAW-01' AS `warehouse_code`, '原材料仓' AS `warehouse_name`, 'RAW' AS `warehouse_type`, '原材料收货、待检和存储' AS `description`
  UNION ALL SELECT 'WH-WIP-01', '半成品仓', 'SEMI_FINISHED', '在制品和工序周转物料存储'
  UNION ALL SELECT 'WH-FG-01', '成品仓', 'FINISHED', '检验放行后的成品存储'
  UNION ALL SELECT 'WH-NCR-01', '不良品仓', 'DEFECTIVE', '不合格和待处置物料隔离'
) seed
JOIN `org_employee` manager
  ON manager.`employee_no` = 'EMP-WMS-001' AND manager.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `wms_warehouse` existing WHERE existing.`warehouse_code` = seed.`warehouse_code`
);

INSERT INTO `wms_location`
  (`location_code`, `location_name`, `warehouse_id`, `location_type`, `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`location_code`, seed.`location_name`, warehouse.`warehouse_id`, seed.`location_type`, 'IDLE',
       CONCAT(@baseline_marker, ': ', seed.`description`), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'LOC-RAW-IQC' AS `location_code`, '原料待检区' AS `location_name`, 'WH-RAW-01' AS `warehouse_code`, 'INSPECTION' AS `location_type`, '来料检验前隔离' AS `description`
  UNION ALL SELECT 'LOC-RAW-A01', '原料合格A区', 'WH-RAW-01', 'NORMAL', '检验合格原料存储'
  UNION ALL SELECT 'LOC-RAW-FROZEN', '原料冻结区', 'WH-RAW-01', 'FROZEN', '冻结物料隔离'
  UNION ALL SELECT 'LOC-WIP-A01', '半成品A区', 'WH-WIP-01', 'NORMAL', '工序周转物料存储'
  UNION ALL SELECT 'LOC-FG-A01', '成品A区', 'WH-FG-01', 'NORMAL', '合格成品存储'
  UNION ALL SELECT 'LOC-NCR-A01', '不良品隔离区', 'WH-NCR-01', 'DEFECTIVE', '不合格品待处置'
) seed
JOIN `wms_warehouse` warehouse
  ON warehouse.`warehouse_code` = seed.`warehouse_code` AND warehouse.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `wms_location` existing WHERE existing.`location_code` = seed.`location_code`
);

-- 8. 设备分类和设备台账
INSERT INTO `eam_equipment_category`
  (`parent_id`, `category_code`, `category_name`, `status`, `remark`, `created_by`, `created_time`)
SELECT NULL, 'EQC-PRODUCTION', '生产与检测设备', 'ENABLE',
       CONCAT(@baseline_marker, ': 设备分类根节点'), @baseline_operator_id, @baseline_time
WHERE NOT EXISTS (
  SELECT 1 FROM `eam_equipment_category` WHERE `category_code` = 'EQC-PRODUCTION'
);

INSERT INTO `eam_equipment_category`
  (`parent_id`, `category_code`, `category_name`, `status`, `remark`, `created_by`, `created_time`)
SELECT parent_category.`category_id`, seed.`category_code`, seed.`category_name`, 'ENABLE',
       CONCAT(@baseline_marker, ': 标准设备分类'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'EQC-FORMING' AS `category_code`, '球头成型设备' AS `category_name`
  UNION ALL SELECT 'EQC-ASSEMBLY', '装配与绕线设备'
  UNION ALL SELECT 'EQC-CURING', '涂胶固化设备'
  UNION ALL SELECT 'EQC-TESTING', '质量检测设备'
  UNION ALL SELECT 'EQC-PACKAGING', '包装设备'
) seed
JOIN `eam_equipment_category` parent_category
  ON parent_category.`category_code` = 'EQC-PRODUCTION' AND parent_category.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `eam_equipment_category` existing WHERE existing.`category_code` = seed.`category_code`
);

INSERT INTO `eam_equipment`
  (`equipment_code`, `equipment_name`, `category_id`, `model`, `manufacturer`, `purchase_date`, `station_id`,
   `status`, `remark`, `created_by`, `created_time`)
SELECT seed.`equipment_code`, seed.`equipment_name`, category.`category_id`, seed.`model`, '羽智装备制造',
       '2025-01-01', station.`station_id`, 'IDLE', CONCAT(@baseline_marker, ': ', seed.`description`),
       @baseline_operator_id, @baseline_time
FROM (
  SELECT 'EQ-TR-CORK-01' AS `equipment_code`, '训练线球头成型机' AS `equipment_name`, 'EQC-FORMING' AS `category_code`, 'CF-800' AS `model`, 'ST-TR-020' AS `station_code`, '训练线主设备' AS `description`
  UNION ALL SELECT 'EQ-TR-INSERT-01', '训练线自动植毛机', 'EQC-ASSEMBLY', 'FI-800', 'ST-TR-030', '训练线主设备'
  UNION ALL SELECT 'EQ-TR-WIND-01', '训练线自动绕线机', 'EQC-ASSEMBLY', 'TW-800', 'ST-TR-040', '训练线主设备'
  UNION ALL SELECT 'EQ-TR-CURE-01', '训练线恒温固化设备', 'EQC-CURING', 'GC-800', 'ST-TR-050', '训练线主设备'
  UNION ALL SELECT 'EQ-TR-TEST-01', '训练线飞行速度检测仪', 'EQC-TESTING', 'ST-800', 'ST-TR-060', '训练线质量检测设备'
  UNION ALL SELECT 'EQ-TR-PACK-01', '训练线自动包装机', 'EQC-PACKAGING', 'PK-800', 'ST-TR-070', '训练线主设备'
  UNION ALL SELECT 'EQ-MA-CORK-01', '比赛线球头成型机', 'EQC-FORMING', 'CF-500P', 'ST-MA-020', '比赛线高精度设备'
  UNION ALL SELECT 'EQ-MA-INSERT-01', '比赛线精密植毛机', 'EQC-ASSEMBLY', 'FI-500P', 'ST-MA-030', '比赛线高精度设备'
  UNION ALL SELECT 'EQ-MA-WIND-01', '比赛线精密绕线机', 'EQC-ASSEMBLY', 'TW-500P', 'ST-MA-040', '比赛线高精度设备'
  UNION ALL SELECT 'EQ-MA-CURE-01', '比赛线恒温固化设备', 'EQC-CURING', 'GC-500P', 'ST-MA-050', '比赛线高精度设备'
  UNION ALL SELECT 'EQ-MA-TEST-01', '比赛线飞行速度检测仪', 'EQC-TESTING', 'ST-500P', 'ST-MA-060', '比赛线质量检测设备'
  UNION ALL SELECT 'EQ-MA-PACK-01', '比赛线自动包装机', 'EQC-PACKAGING', 'PK-500P', 'ST-MA-070', '比赛线主设备'
) seed
JOIN `eam_equipment_category` category
  ON category.`category_code` = seed.`category_code` AND category.`deleted_flag` = 0
JOIN `factory_station` station
  ON station.`station_code` = seed.`station_code` AND station.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `eam_equipment` existing WHERE existing.`equipment_code` = seed.`equipment_code`
);

-- 9. 质量检验项目、标准和已发布检验方案
INSERT INTO `qms_inspection_item`
  (`item_code`, `item_name`, `item_type`, `inspection_method`, `unit_id`, `remark`, `created_by`, `created_time`)
SELECT seed.`item_code`, seed.`item_name`, seed.`item_type`, seed.`inspection_method`, unit.`unit_id`,
       CONCAT(@baseline_marker, ': 标准检验项目'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'QI-FEATHER-LENGTH' AS `item_code`, '毛片长度' AS `item_name`, 'DIMENSION' AS `item_type`, '游标卡尺测量' AS `inspection_method`, 'MM' AS `unit_code`
  UNION ALL SELECT 'QI-HEAD-DIAMETER', '球头直径', 'DIMENSION', '游标卡尺测量', 'MM'
  UNION ALL SELECT 'QI-RAW-APPEARANCE', '原材料外观', 'APPEARANCE', '目视检查完整性、色泽和污染', NULL
  UNION ALL SELECT 'QI-FINISHED-WEIGHT', '成品单球重量', 'WEIGHT', '校准电子天平称量', 'G'
  UNION ALL SELECT 'QI-SPEED-GRADE', '速度等级', 'PERFORMANCE', '标准温湿度下使用速度检测仪', NULL
  UNION ALL SELECT 'QI-FLIGHT-STABILITY', '飞行落点离散', 'PERFORMANCE', '同批随机抽样，每只使用标准发球机单次击发，计算落点相对样品组平均落点的最大径向偏差', 'MM'
  UNION ALL SELECT 'QI-FINISHED-APPEARANCE', '成品外观', 'APPEARANCE', '目视检查球头、毛片、绕线、胶层和标签', NULL
) seed
LEFT JOIN `mdm_unit` unit
  ON unit.`unit_code` = seed.`unit_code` AND unit.`deleted_flag` = 0
WHERE (seed.`unit_code` IS NULL OR unit.`unit_id` IS NOT NULL)
  AND NOT EXISTS (
    SELECT 1 FROM `qms_inspection_item` existing WHERE existing.`item_code` = seed.`item_code`
  );

INSERT INTO `qms_inspection_standard`
  (`item_id`, `product_id`, `operation_id`, `standard_value`, `upper_limit`, `lower_limit`, `judgement_rule`,
   `remark`, `created_by`, `created_time`)
SELECT item.`item_id`, product.`product_id`, operation.`operation_id`, seed.`standard_value`,
       seed.`upper_limit`, seed.`lower_limit`, seed.`judgement_rule`,
       CONCAT(@baseline_marker, ':', seed.`standard_key`), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'STD-IQC-TRAIN-FEATHER-LENGTH' AS `standard_key`, 'QI-FEATHER-LENGTH' AS `item_code`, 'PROD-SHUTTLE-TRAIN-77' AS `product_code`, NULL AS `operation_code`,
         '66' AS `standard_value`, '70' AS `upper_limit`, '62' AS `lower_limit`, '实测值在上下限内判定合格' AS `judgement_rule`
  UNION ALL SELECT 'STD-IQC-TRAIN-HEAD-DIAMETER', 'QI-HEAD-DIAMETER', 'PROD-SHUTTLE-TRAIN-77', NULL, '26.5', '28', '25', '实测值在上下限内判定合格'
  UNION ALL SELECT 'STD-IQC-TRAIN-RAW-APPEARANCE', 'QI-RAW-APPEARANCE', 'PROD-SHUTTLE-TRAIN-77', NULL, '合格', NULL, NULL, '无断毛、霉变、油污和明显色差判定合格'
  UNION ALL SELECT 'STD-IPQC-TRAIN-WEIGHT', 'QI-FINISHED-WEIGHT', 'PROD-SHUTTLE-TRAIN-77', 'OP-SPEED-TEST', '5.10', '5.50', '4.74', '单球重量在上下限内判定合格'
  UNION ALL SELECT 'STD-IPQC-TRAIN-SPEED', 'QI-SPEED-GRADE', 'PROD-SHUTTLE-TRAIN-77', 'OP-SPEED-TEST', '77', '77', '77', '标准环境下检测等级等于77判定合格'
  UNION ALL SELECT 'STD-IPQC-TRAIN-FLIGHT', 'QI-FLIGHT-STABILITY', 'PROD-SHUTTLE-TRAIN-77', 'OP-SPEED-TEST', '500', '500', '0', '同批抽样样品的最大落点径向偏差不超过500mm判定合格'
  UNION ALL SELECT 'STD-FQC-TRAIN-APPEARANCE', 'QI-FINISHED-APPEARANCE', 'PROD-SHUTTLE-TRAIN-77', 'OP-PACK', '合格', NULL, NULL, '球头、毛片、绕线、胶层和标签均无明显缺陷判定合格'
  UNION ALL SELECT 'STD-IQC-MATCH-FEATHER-LENGTH', 'QI-FEATHER-LENGTH', 'PROD-SHUTTLE-MATCH-77', NULL, '66', '70', '62', '实测值在上下限内判定合格'
  UNION ALL SELECT 'STD-IQC-MATCH-HEAD-DIAMETER', 'QI-HEAD-DIAMETER', 'PROD-SHUTTLE-MATCH-77', NULL, '26.5', '28', '25', '实测值在上下限内判定合格'
  UNION ALL SELECT 'STD-IQC-MATCH-RAW-APPEARANCE', 'QI-RAW-APPEARANCE', 'PROD-SHUTTLE-MATCH-77', NULL, '合格', NULL, NULL, '毛片同色、完整，球头无裂纹和污染判定合格'
  UNION ALL SELECT 'STD-IPQC-MATCH-WEIGHT', 'QI-FINISHED-WEIGHT', 'PROD-SHUTTLE-MATCH-77', 'OP-SPEED-TEST', '5.10', '5.50', '4.74', '单球重量在上下限内判定合格'
  UNION ALL SELECT 'STD-IPQC-MATCH-SPEED', 'QI-SPEED-GRADE', 'PROD-SHUTTLE-MATCH-77', 'OP-SPEED-TEST', '77', '77', '77', '标准环境下检测等级等于77判定合格'
  UNION ALL SELECT 'STD-IPQC-MATCH-FLIGHT', 'QI-FLIGHT-STABILITY', 'PROD-SHUTTLE-MATCH-77', 'OP-SPEED-TEST', '300', '300', '0', '同批抽样样品的最大落点径向偏差不超过300mm判定合格'
  UNION ALL SELECT 'STD-FQC-MATCH-APPEARANCE', 'QI-FINISHED-APPEARANCE', 'PROD-SHUTTLE-MATCH-77', 'OP-PACK', '合格', NULL, NULL, '球头、毛片、绕线、胶层和标签均无明显缺陷判定合格'
) seed
JOIN `qms_inspection_item` item
  ON item.`item_code` = seed.`item_code` AND item.`deleted_flag` = 0
JOIN `mdm_product` product
  ON product.`product_code` = seed.`product_code` AND product.`deleted_flag` = 0
LEFT JOIN `process_operation` operation
  ON operation.`operation_code` = seed.`operation_code` AND operation.`deleted_flag` = 0
WHERE (seed.`operation_code` IS NULL OR operation.`operation_id` IS NOT NULL)
  AND NOT EXISTS (
    SELECT 1 FROM `qms_inspection_standard` existing
    WHERE existing.`remark` = CONCAT(@baseline_marker, ':', seed.`standard_key`)
  );

INSERT INTO `qms_inspection_scheme`
  (`scheme_code`, `scheme_name`, `product_id`, `operation_id`, `inspection_type`, `status`, `remark`,
   `created_by`, `created_time`)
SELECT seed.`scheme_code`, seed.`scheme_name`, product.`product_id`, operation.`operation_id`,
       seed.`inspection_type`, 'RELEASED', CONCAT(@baseline_marker, ': 已发布标准检验方案'),
       @baseline_operator_id, @baseline_time
FROM (
  SELECT 'QMS-IQC-TRAIN-77' AS `scheme_code`, '训练级77速来料检验方案' AS `scheme_name`, 'PROD-SHUTTLE-TRAIN-77' AS `product_code`, NULL AS `operation_code`, 'INCOMING' AS `inspection_type`
  UNION ALL SELECT 'QMS-IPQC-TRAIN-77', '训练级77速过程检验方案', 'PROD-SHUTTLE-TRAIN-77', 'OP-SPEED-TEST', 'PROCESS'
  UNION ALL SELECT 'QMS-FQC-TRAIN-77', '训练级77速最终检验方案', 'PROD-SHUTTLE-TRAIN-77', 'OP-PACK', 'FINAL'
  UNION ALL SELECT 'QMS-IQC-MATCH-77', '比赛级77速来料检验方案', 'PROD-SHUTTLE-MATCH-77', NULL, 'INCOMING'
  UNION ALL SELECT 'QMS-IPQC-MATCH-77', '比赛级77速过程检验方案', 'PROD-SHUTTLE-MATCH-77', 'OP-SPEED-TEST', 'PROCESS'
  UNION ALL SELECT 'QMS-FQC-MATCH-77', '比赛级77速最终检验方案', 'PROD-SHUTTLE-MATCH-77', 'OP-PACK', 'FINAL'
) seed
JOIN `mdm_product` product
  ON product.`product_code` = seed.`product_code` AND product.`deleted_flag` = 0
LEFT JOIN `process_operation` operation
  ON operation.`operation_code` = seed.`operation_code` AND operation.`deleted_flag` = 0
WHERE (seed.`operation_code` IS NULL OR operation.`operation_id` IS NOT NULL)
  AND NOT EXISTS (
    SELECT 1 FROM `qms_inspection_scheme` existing WHERE existing.`scheme_code` = seed.`scheme_code`
  );

INSERT INTO `qms_inspection_scheme_item`
  (`scheme_id`, `item_id`, `standard_id`, `sample_quantity`, `sequence_no`, `remark`, `created_by`, `created_time`)
SELECT scheme.`scheme_id`, item.`item_id`, standard.`standard_id`, seed.`sample_quantity`, seed.`sequence_no`,
       CONCAT(@baseline_marker, ': 方案检验明细'), @baseline_operator_id, @baseline_time
FROM (
  SELECT 'QMS-IQC-TRAIN-77' AS `scheme_code`, 'QI-FEATHER-LENGTH' AS `item_code`, 'STD-IQC-TRAIN-FEATHER-LENGTH' AS `standard_key`, 32.0000 AS `sample_quantity`, 10 AS `sequence_no`
  UNION ALL SELECT 'QMS-IQC-TRAIN-77', 'QI-HEAD-DIAMETER', 'STD-IQC-TRAIN-HEAD-DIAMETER', 12.0000, 20
  UNION ALL SELECT 'QMS-IQC-TRAIN-77', 'QI-RAW-APPEARANCE', 'STD-IQC-TRAIN-RAW-APPEARANCE', 32.0000, 30
  UNION ALL SELECT 'QMS-IPQC-TRAIN-77', 'QI-FINISHED-WEIGHT', 'STD-IPQC-TRAIN-WEIGHT', 5.0000, 10
  UNION ALL SELECT 'QMS-IPQC-TRAIN-77', 'QI-SPEED-GRADE', 'STD-IPQC-TRAIN-SPEED', 5.0000, 20
  UNION ALL SELECT 'QMS-IPQC-TRAIN-77', 'QI-FLIGHT-STABILITY', 'STD-IPQC-TRAIN-FLIGHT', 5.0000, 30
  UNION ALL SELECT 'QMS-FQC-TRAIN-77', 'QI-FINISHED-APPEARANCE', 'STD-FQC-TRAIN-APPEARANCE', 5.0000, 10
  UNION ALL SELECT 'QMS-IQC-MATCH-77', 'QI-FEATHER-LENGTH', 'STD-IQC-MATCH-FEATHER-LENGTH', 32.0000, 10
  UNION ALL SELECT 'QMS-IQC-MATCH-77', 'QI-HEAD-DIAMETER', 'STD-IQC-MATCH-HEAD-DIAMETER', 12.0000, 20
  UNION ALL SELECT 'QMS-IQC-MATCH-77', 'QI-RAW-APPEARANCE', 'STD-IQC-MATCH-RAW-APPEARANCE', 32.0000, 30
  UNION ALL SELECT 'QMS-IPQC-MATCH-77', 'QI-FINISHED-WEIGHT', 'STD-IPQC-MATCH-WEIGHT', 8.0000, 10
  UNION ALL SELECT 'QMS-IPQC-MATCH-77', 'QI-SPEED-GRADE', 'STD-IPQC-MATCH-SPEED', 8.0000, 20
  UNION ALL SELECT 'QMS-IPQC-MATCH-77', 'QI-FLIGHT-STABILITY', 'STD-IPQC-MATCH-FLIGHT', 8.0000, 30
  UNION ALL SELECT 'QMS-FQC-MATCH-77', 'QI-FINISHED-APPEARANCE', 'STD-FQC-MATCH-APPEARANCE', 8.0000, 10
) seed
JOIN `qms_inspection_scheme` scheme
  ON scheme.`scheme_code` = seed.`scheme_code` AND scheme.`deleted_flag` = 0
JOIN `qms_inspection_item` item
  ON item.`item_code` = seed.`item_code` AND item.`deleted_flag` = 0
JOIN `qms_inspection_standard` standard
  ON standard.`remark` = CONCAT(@baseline_marker, ':', seed.`standard_key`) AND standard.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `qms_inspection_scheme_item` existing
  WHERE existing.`scheme_id` = scheme.`scheme_id` AND existing.`item_id` = item.`item_id`
);

-- 10. 仅规范化本脚本早期版本创建的基线值，使已执行库与新库保持一致。
UPDATE `mdm_bom_item` item
JOIN `mdm_bom` bom ON bom.`bom_id` = item.`bom_id` AND bom.`deleted_flag` = 0
JOIN `mdm_material` material ON material.`material_id` = item.`material_id` AND material.`deleted_flag` = 0
SET item.`quantity` = 7.2000,
    item.`updated_by` = @baseline_operator_id,
    item.`updated_time` = @baseline_time
WHERE item.`deleted_flag` = 0
  AND item.`remark` LIKE CONCAT(@baseline_marker, ':%')
  AND bom.`bom_code` IN ('BOM-TRAIN-77-V1', 'BOM-MATCH-77-V1')
  AND material.`material_code` = 'AUX-ADHESIVE-RESIN'
  AND item.`quantity` <> 7.2000;

UPDATE `process_parameter` parameter_record
JOIN `process_operation` operation ON operation.`operation_id` = parameter_record.`operation_id` AND operation.`deleted_flag` = 0
SET parameter_record.`standard_value` = CASE parameter_record.`parameter_name`
      WHEN '绕线张力' THEN '4.0' ELSE '0.60' END,
    parameter_record.`upper_limit` = CASE parameter_record.`parameter_name`
      WHEN '绕线张力' THEN '4.5' ELSE '0.70' END,
    parameter_record.`lower_limit` = CASE parameter_record.`parameter_name`
      WHEN '绕线张力' THEN '3.5' ELSE '0.50' END,
    parameter_record.`updated_by` = @baseline_operator_id,
    parameter_record.`updated_time` = @baseline_time
WHERE parameter_record.`deleted_flag` = 0
  AND parameter_record.`remark` LIKE CONCAT(@baseline_marker, ':%')
  AND ((operation.`operation_code` = 'OP-THREAD-WIND' AND parameter_record.`parameter_name` = '绕线张力')
    OR (operation.`operation_code` = 'OP-GLUE-CURE' AND parameter_record.`parameter_name` = '单球涂胶量'))
  AND (NOT (parameter_record.`standard_value` <=> CASE parameter_record.`parameter_name`
        WHEN '绕线张力' THEN '4.0' ELSE '0.60' END)
    OR NOT (parameter_record.`upper_limit` <=> CASE parameter_record.`parameter_name`
        WHEN '绕线张力' THEN '4.5' ELSE '0.70' END)
    OR NOT (parameter_record.`lower_limit` <=> CASE parameter_record.`parameter_name`
        WHEN '绕线张力' THEN '3.5' ELSE '0.50' END));

UPDATE `qms_inspection_item` item
JOIN `mdm_unit` unit ON unit.`unit_code` = 'MM' AND unit.`deleted_flag` = 0
SET item.`item_name` = '飞行落点离散',
    item.`inspection_method` = '同批随机抽样，每只使用标准发球机单次击发，计算落点相对样品组平均落点的最大径向偏差',
    item.`unit_id` = unit.`unit_id`,
    item.`updated_by` = @baseline_operator_id,
    item.`updated_time` = @baseline_time
WHERE item.`item_code` = 'QI-FLIGHT-STABILITY'
  AND item.`deleted_flag` = 0
  AND item.`remark` LIKE CONCAT(@baseline_marker, ':%')
  AND (NOT (item.`item_name` <=> '飞行落点离散')
    OR NOT (item.`inspection_method` <=> '同批随机抽样，每只使用标准发球机单次击发，计算落点相对样品组平均落点的最大径向偏差')
    OR NOT (item.`unit_id` <=> unit.`unit_id`));

UPDATE `qms_inspection_standard`
SET `standard_value` = '500', `upper_limit` = '500', `lower_limit` = '0',
    `judgement_rule` = '同批抽样样品的最大落点径向偏差不超过500mm判定合格',
    `updated_by` = @baseline_operator_id, `updated_time` = @baseline_time
WHERE `deleted_flag` = 0 AND `remark` = CONCAT(@baseline_marker, ':STD-IPQC-TRAIN-FLIGHT')
  AND (NOT (`standard_value` <=> '500') OR NOT (`upper_limit` <=> '500') OR NOT (`lower_limit` <=> '0')
    OR NOT (`judgement_rule` <=> '同批抽样样品的最大落点径向偏差不超过500mm判定合格'));

UPDATE `qms_inspection_standard`
SET `standard_value` = '300', `upper_limit` = '300', `lower_limit` = '0',
    `judgement_rule` = '同批抽样样品的最大落点径向偏差不超过300mm判定合格',
    `updated_by` = @baseline_operator_id, `updated_time` = @baseline_time
WHERE `deleted_flag` = 0 AND `remark` = CONCAT(@baseline_marker, ':STD-IPQC-MATCH-FLIGHT')
  AND (NOT (`standard_value` <=> '300') OR NOT (`upper_limit` <=> '300') OR NOT (`lower_limit` <=> '0')
    OR NOT (`judgement_rule` <=> '同批抽样样品的最大落点径向偏差不超过300mm判定合格'));

COMMIT;

-- 执行结果概览；关联完整性由项目验证脚本和交付检查确认。
SELECT @baseline_marker AS `baseline_marker`, @baseline_time AS `executed_time`;
SELECT 'organization' AS `domain`, COUNT(*) AS `baseline_rows`
FROM `org_department` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@baseline_marker, ':%')
UNION ALL SELECT 'master_data', COUNT(*) FROM `mdm_material` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@baseline_marker, ':%')
UNION ALL SELECT 'process', COUNT(*) FROM `process_operation` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@baseline_marker, ':%')
UNION ALL SELECT 'factory', COUNT(*) FROM `factory_station` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@baseline_marker, ':%')
UNION ALL SELECT 'warehouse', COUNT(*) FROM `wms_location` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@baseline_marker, ':%')
UNION ALL SELECT 'equipment', COUNT(*) FROM `eam_equipment` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@baseline_marker, ':%')
UNION ALL SELECT 'quality', COUNT(*) FROM `qms_inspection_item` WHERE `deleted_flag` = 0 AND `remark` LIKE CONCAT(@baseline_marker, ':%');
