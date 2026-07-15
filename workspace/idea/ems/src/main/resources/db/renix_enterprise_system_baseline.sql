-- 雷尼克斯 MES 企业系统基础、RBAC、字典参数和作业指导书数据。
-- 前置条件：已完成 RENIX_TX_30D_V1，共享库执行前必须完整备份。
-- 本脚本不重置 admin 密码；新账号先以禁用占位创建，再由 seed-enterprise-users.ps1 激活。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;
SET @system_marker = 'RENIX_SYSTEM_BASE_V2';
SET @rbac_marker = 'RENIX_RBAC_V1';
SET @system_time = '2026-07-14 08:00:00';
SET @admin_user_id = (
  SELECT `user_id` FROM `sys_user`
  WHERE `username` = 'admin' AND `deleted_flag` = 0
  ORDER BY `user_id` LIMIT 1
);
SET @baseline_ready = IF(
  @admin_user_id IS NOT NULL
  AND (SELECT COUNT(*) FROM `mdm_product` WHERE `deleted_flag` = 0) = 5
  AND (SELECT COUNT(*) FROM `mfg_production_order`
       WHERE `remark` LIKE 'RENIX_TX_30D_V1%' AND `deleted_flag` = 0) = 30,
  1, 0
);
SET @assertion_sql = IF(
  @baseline_ready = 1,
  'DO 0',
  'SELECT * FROM `__ABORT_RENIX_SYSTEM_BASE_PRECONDITION__`'
);
PREPARE renix_system_assertion_stmt FROM @assertion_sql;
EXECUTE renix_system_assertion_stmt;
DEALLOCATE PREPARE renix_system_assertion_stmt;

START TRANSACTION;

-- 1. 数字化组织和责任人。
INSERT INTO `org_department`
  (`parent_id`, `department_code`, `department_name`, `manager_id`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT plant.`department_id`, 'DEPT-IT', '数字化与信息部', NULL,
       CONCAT(@system_marker, ': MES、账号、权限和企业数字化基础治理'), 0,
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `org_department` plant
WHERE plant.`department_code` = 'DEPT-PLANT' AND plant.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `org_department` existing
    WHERE existing.`department_code` = 'DEPT-IT'
  );

UPDATE `org_department` department
JOIN `org_department` plant
  ON plant.`department_code` = 'DEPT-PLANT' AND plant.`deleted_flag` = 0
SET department.`parent_id` = plant.`department_id`,
    department.`department_name` = '数字化与信息部',
    department.`remark` = CONCAT(@system_marker, ': MES、账号、权限和企业数字化基础治理'),
    department.`updated_by` = @admin_user_id,
    department.`updated_time` = @system_time,
    department.`deleted_flag` = 0
WHERE department.`department_code` = 'DEPT-IT';

INSERT INTO `org_employee`
  (`employee_no`, `employee_name`, `department_id`, `position_name`, `skill_level`, `phone`, `status`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT 'EMP-IT-001', '林川', department.`department_id`, 'MES系统管理员', 'EXPERT', NULL, 'ENABLE',
       CONCAT(@system_marker, ': 项目专用脱敏仿真系统管理员'), 0,
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `org_department` department
WHERE department.`department_code` = 'DEPT-IT' AND department.`deleted_flag` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `org_employee` existing
    WHERE existing.`employee_no` = 'EMP-IT-001'
  );

UPDATE `org_employee` employee
JOIN `org_department` department
  ON department.`department_code` = 'DEPT-IT' AND department.`deleted_flag` = 0
SET employee.`employee_name` = '林川',
    employee.`department_id` = department.`department_id`,
    employee.`position_name` = 'MES系统管理员',
    employee.`skill_level` = 'EXPERT',
    employee.`status` = 'ENABLE',
    employee.`remark` = CONCAT(@system_marker, ': 项目专用脱敏仿真系统管理员'),
    employee.`updated_by` = @admin_user_id,
    employee.`updated_time` = @system_time,
    employee.`deleted_flag` = 0
WHERE employee.`employee_no` = 'EMP-IT-001';

UPDATE `org_department` department
JOIN `org_employee` employee
  ON employee.`employee_no` = 'EMP-IT-001' AND employee.`deleted_flag` = 0
SET department.`manager_id` = employee.`employee_id`,
    department.`updated_by` = @admin_user_id,
    department.`updated_time` = @system_time
WHERE department.`department_code` = 'DEPT-IT' AND department.`deleted_flag` = 0;

UPDATE `sys_user` user
JOIN `org_employee` employee
  ON employee.`employee_no` = 'EMP-IT-001' AND employee.`deleted_flag` = 0
SET user.`employee_id` = employee.`employee_id`,
    user.`status` = 'ENABLE',
    user.`remark` = CONCAT(@rbac_marker, ': 最高权限MES系统管理员'),
    user.`updated_by` = @admin_user_id,
    user.`updated_time` = @system_time
WHERE user.`username` = 'admin' AND user.`deleted_flag` = 0;

-- 2. 角色。
DROP TEMPORARY TABLE IF EXISTS `tmp_renix_role`;
CREATE TEMPORARY TABLE `tmp_renix_role` (
  `role_code` VARCHAR(64) PRIMARY KEY,
  `role_name` VARCHAR(128) NOT NULL,
  `data_scope` VARCHAR(32) NOT NULL,
  `remark` VARCHAR(500) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_renix_role` VALUES
  ('SUPER_ADMIN', '超级管理员', 'ALL', '系统、权限和全部业务的最终管理权限'),
  ('MES_MANAGER', 'MES生产经理', 'ALL', '全局运营查看、生产审批和跨域协调'),
  ('PLAN_SCHEDULER', '生产计划员', 'ALL', '订单、计划、产能和排程管理'),
  ('PRODUCTION_SUPERVISOR', '生产班组长', 'TEAM', '本班组派工、任务、报工和异常处理'),
  ('PRODUCTION_OPERATOR', '生产操作员', 'SELF', '本人任务开工、报工、完工和扫码'),
  ('QUALITY_ENGINEER', '质量工程师', 'DEPARTMENT', '检验、复检、不合格品和质量追溯'),
  ('WAREHOUSE_MANAGER', '仓库主管', 'DEPARTMENT', '收发存、退料、盘点和补货确认'),
  ('EQUIPMENT_ENGINEER', '设备工程师', 'DEPARTMENT', '点检、保养、维修和设备状态管理');

INSERT INTO `sys_role`
  (`role_code`, `role_name`, `data_scope`, `status`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT seed.`role_code`, seed.`role_name`, seed.`data_scope`, 'ENABLE',
       CONCAT(@rbac_marker, ': ', seed.`remark`), 0,
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_role` seed
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `data_scope` = VALUES(`data_scope`),
  `status` = 'ENABLE',
  `remark` = VALUES(`remark`),
  `updated_by` = @admin_user_id,
  `updated_time` = @system_time,
  `deleted_flag` = 0;

-- 3. 岗位账号。非 admin 账号先放入不可登录占位哈希和 DISABLE 状态。
DROP TEMPORARY TABLE IF EXISTS `tmp_renix_user`;
CREATE TEMPORARY TABLE `tmp_renix_user` (
  `username` VARCHAR(64) PRIMARY KEY,
  `employee_no` VARCHAR(64) NOT NULL,
  `role_code` VARCHAR(64) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_renix_user` VALUES
  ('admin', 'EMP-IT-001', 'SUPER_ADMIN'),
  ('scjh001', 'EMP-PMC-001', 'PLAN_SCHEDULER'),
  ('scjl001', 'EMP-MFG-001', 'MES_MANAGER'),
  ('bzz001', 'EMP-MFG-101', 'PRODUCTION_SUPERVISOR'),
  ('bzz002', 'EMP-MFG-102', 'PRODUCTION_SUPERVISOR'),
  ('czg001', 'EMP-MFG-111', 'PRODUCTION_OPERATOR'),
  ('czg002', 'EMP-MFG-112', 'PRODUCTION_OPERATOR'),
  ('czg003', 'EMP-MFG-113', 'PRODUCTION_OPERATOR'),
  ('czg004', 'EMP-MFG-114', 'PRODUCTION_OPERATOR'),
  ('czg005', 'EMP-MFG-115', 'PRODUCTION_OPERATOR'),
  ('czg006', 'EMP-MFG-116', 'PRODUCTION_OPERATOR'),
  ('zjy001', 'EMP-QMS-001', 'QUALITY_ENGINEER'),
  ('ckgly001', 'EMP-WMS-001', 'WAREHOUSE_MANAGER'),
  ('sbgly001', 'EMP-EAM-001', 'EQUIPMENT_ENGINEER');

-- 已有环境先按员工编号原位改名，避免新版基线为同一员工重复创建账号。
UPDATE `sys_user` user
JOIN `org_employee` employee
  ON employee.`employee_id` = user.`employee_id` AND employee.`deleted_flag` = 0
JOIN `tmp_renix_user` seed
  ON seed.`employee_no` = employee.`employee_no`
SET user.`username` = seed.`username`,
    user.`updated_by` = @admin_user_id,
    user.`updated_time` = @system_time
WHERE seed.`username` <> 'admin'
  AND user.`deleted_flag` = 0
  AND user.`username` <> seed.`username`;

INSERT INTO `sys_user`
  (`username`, `password_hash`, `employee_id`, `status`, `login_fail_count`, `locked_time`, `last_login_time`,
   `remark`, `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT seed.`username`, '!', employee.`employee_id`, 'DISABLE', 0, NULL, NULL,
       CONCAT(@rbac_marker, ': 待PBKDF2初始化的岗位账号'), 0,
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_user` seed
JOIN `org_employee` employee
  ON employee.`employee_no` = seed.`employee_no` AND employee.`deleted_flag` = 0
WHERE seed.`username` <> 'admin'
ON DUPLICATE KEY UPDATE
  `employee_id` = VALUES(`employee_id`),
  `updated_by` = @admin_user_id,
  `updated_time` = @system_time,
  `deleted_flag` = 0;

INSERT INTO `sys_user_role`
  (`user_id`, `role_id`, `assigned_time`, `created_by`, `created_time`, `deleted_flag`)
SELECT user.`user_id`, role.`role_id`, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_user` seed
JOIN `sys_user` user ON user.`username` = seed.`username` AND user.`deleted_flag` = 0
JOIN `sys_role` role ON role.`role_code` = seed.`role_code` AND role.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE
  `assigned_time` = VALUES(`assigned_time`),
  `deleted_flag` = 0;

-- 4. PC 端 78 个资源清单。
DROP TEMPORARY TABLE IF EXISTS `tmp_renix_resource`;
CREATE TEMPORARY TABLE `tmp_renix_resource` (
  `domain_code` VARCHAR(32) NOT NULL,
  `resource_key` VARCHAR(64) PRIMARY KEY,
  `resource_name` VARCHAR(128) NOT NULL,
  `route_path` VARCHAR(255) NOT NULL,
  `menu_visible` TINYINT NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_renix_resource` VALUES
  ('eam','eam-equipment','设备台账','/eam/equipment',1),
  ('eam','eam-equipment-category','设备分类','/eam/equipment-categories',1),
  ('eam','eam-equipment-status-log','设备状态日志','/eam/equipment-status',1),
  ('eam','eam-maintenance-plan','保养计划','/eam/maintenance-plans',1),
  ('eam','eam-maintenance-record','保养记录','/eam/maintenance-records',1),
  ('eam','eam-repair-order','维修工单','/eam/repair-orders',1),
  ('eam','eam-spot-check-plan','点检计划','/eam/spot-check-plans',1),
  ('eam','eam-spot-check-record','点检记录','/eam/spot-check-records',1),
  ('factory','factory-line','产线管理','/factory/lines',1),
  ('factory','factory-station','工位管理','/factory/stations',1),
  ('factory','factory-station-operation','工位工序','/factory/station-operations',1),
  ('factory','factory-workshop','车间管理','/factory/workshops',1),
  ('mdm','mdm-bom','产品BOM','/mdm/boms',1),
  ('mdm','mdm-bom-item','BOM物料明细','/mdm/bom-items',0),
  ('mdm','mdm-customer','客户管理','/mdm/customers',1),
  ('mdm','mdm-material','物料管理','/mdm/materials',1),
  ('mdm','mdm-material-category','物料分类','/mdm/material-categories',1),
  ('mdm','mdm-product','产品管理','/mdm/products',1),
  ('mdm','mdm-supplier','供应商管理','/mdm/suppliers',1),
  ('mdm','mdm-unit','计量单位','/mdm/units',1),
  ('mfg','mfg-andon-alert','安灯告警','/mfg/andon-alerts',1),
  ('mfg','mfg-dispatch-order','派工管理','/mfg/dispatch-orders',1),
  ('mfg','mfg-exception','生产异常','/mfg/exceptions',1),
  ('mfg','mfg-line-calendar-rule','产线周历规则','/mfg/line-calendar-rules',1),
  ('mfg','mfg-line-product-capability','产线产品能力','/mfg/line-product-capabilities',1),
  ('mfg','mfg-production-order','生产订单','/mfg/production-orders',1),
  ('mfg','mfg-production-plan','生产计划','/mfg/production-plans',1),
  ('mfg','mfg-report-material','报工用料','/mfg/report-materials',0),
  ('mfg','mfg-schedule-scenario','智能排产工作台','/mfg/schedule-workbench',1),
  ('mfg','mfg-task','生产任务','/mfg/tasks',1),
  ('mfg','mfg-task-operator','任务人员','/mfg/task-operators',0),
  ('mfg','mfg-work-order','生产工单','/mfg/work-orders',1),
  ('mfg','mfg-work-order-material','工单用料需求','/mfg/work-order-materials',0),
  ('mfg','mfg-work-report','生产报工','/mfg/work-reports',1),
  ('org','org-department','部门管理','/org/departments',1),
  ('org','org-employee','员工管理','/org/employees',1),
  ('org','org-shift','班次管理','/org/shifts',1),
  ('org','org-team','班组管理','/org/teams',1),
  ('org','org-team-member','班组成员','/org/team-members',0),
  ('process','process-operation','工序管理','/process/operations',1),
  ('process','process-parameter','工艺参数','/process/parameters',1),
  ('process','process-route','工艺路线','/process/routes',1),
  ('process','process-route-item','路线工序明细','/process/route-items',0),
  ('process','process-work-instruction','作业指导书','/process/instructions',1),
  ('qms','qms-inspection-item','检验项目','/qms/inspection-items',1),
  ('qms','qms-inspection-order','检验单管理','/qms/inspection-orders',1),
  ('qms','qms-inspection-result','检验结果明细','/qms/inspection-results',0),
  ('qms','qms-inspection-scheme','检验方案','/qms/schemes',1),
  ('qms','qms-inspection-scheme-item','方案检验项目','/qms/scheme-items',0),
  ('qms','qms-inspection-standard','检验标准','/qms/standards',1),
  ('qms','qms-nonconforming-detail','不合格品明细','/qms/nonconforming-details',0),
  ('qms','qms-nonconforming-order','不合格品处理','/qms/nonconforming-orders',1),
  ('sys','sys-approval-record','审批记录','/sys/approvals',1),
  ('sys','sys-dict-data','字典数据','/sys/dict-data',1),
  ('sys','sys-dict-type','字典类型','/sys/dict-types',1),
  ('sys','sys-file','附件文件','/sys/files',1),
  ('sys','sys-menu','菜单管理','/sys/menus',1),
  ('sys','sys-notification','消息中心','/notifications',0),
  ('sys','sys-operation-log','操作日志','/sys/operation-logs',1),
  ('sys','sys-role','角色管理','/sys/roles',1),
  ('sys','sys-role-menu','角色菜单关联','/sys/role-menus',0),
  ('sys','sys-status-change-log','状态变更日志','/sys/status-logs',1),
  ('sys','sys-system-param','系统参数','/sys/parameters',1),
  ('sys','sys-user','用户管理','/sys/users',1),
  ('sys','sys-user-role','用户角色关联','/sys/user-roles',0),
  ('trace','trace-barcode','条码管理','/trace/barcodes',1),
  ('trace','trace-material-usage','物料使用追溯','/trace/material-usages',1),
  ('trace','trace-product-lot','产品批次追溯','/trace/product-lots',1),
  ('wms','wms-inbound-detail','入库明细','/wms/inbound-details',0),
  ('wms','wms-inbound-order','入库管理','/wms/inbound-orders',1),
  ('wms','wms-inventory','库存余额','/wms/inventory',1),
  ('wms','wms-location','库位管理','/wms/locations',1),
  ('wms','wms-material-batch','物料批次','/wms/material-batches',1),
  ('wms','wms-outbound-detail','出库明细','/wms/outbound-details',0),
  ('wms','wms-outbound-order','出库管理','/wms/outbound-orders',1),
  ('wms','wms-replenishment','补货建议','/wms/replenishment',1),
  ('wms','wms-return-detail','退料明细','/wms/return-details',0),
  ('wms','wms-return-order','退料管理','/wms/return-orders',1),
  ('wms','wms-stock-movement','库存流水','/wms/movements',1),
  ('wms','wms-stocktake-detail','盘点明细','/wms/stocktake-details',0),
  ('wms','wms-stocktake-order','盘点管理','/wms/stocktakes',1),
  ('wms','wms-warehouse','仓库管理','/wms/warehouses',1);

DROP TEMPORARY TABLE IF EXISTS `tmp_renix_domain`;
CREATE TEMPORARY TABLE `tmp_renix_domain` (
  `domain_code` VARCHAR(32) PRIMARY KEY,
  `domain_name` VARCHAR(64) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_renix_domain` VALUES
  ('mfg','生产制造'),('wms','仓储库存'),('qms','质量管理'),('mdm','主数据'),('process','工艺管理'),
  ('factory','工厂建模'),('eam','设备运维'),('trace','条码追溯'),('org','组织人事'),('sys','系统管理');

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT NULL, seed.`domain_name`, 'DIRECTORY', CONCAT('module:', seed.`domain_code`, ':view'),
       CONCAT('/module/', seed.`domain_code`), CONCAT(@rbac_marker, ': 业务域目录'),
       0, @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_domain` seed
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_type` = 'DIRECTORY', `route_path` = VALUES(`route_path`),
  `remark` = VALUES(`remark`), `updated_by` = @admin_user_id, `updated_time` = @system_time, `deleted_flag` = 0;

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT directory.`menu_id`, resource.`resource_name`, IF(resource.`menu_visible` = 1, 'MENU', 'API'),
       CONCAT(resource.`domain_code`, ':', SUBSTRING(resource.`resource_key`, LENGTH(resource.`domain_code`) + 2), ':view'),
       resource.`route_path`, CONCAT(@rbac_marker, ': ', IF(resource.`menu_visible` = 1, 'PC资源菜单', '隐藏主从资源查询权限')),
       0, @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_resource` resource
JOIN `sys_menu` directory
  ON directory.`permission_code` = CONCAT('module:', resource.`domain_code`, ':view')
 AND directory.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = VALUES(`menu_type`),
  `route_path` = VALUES(`route_path`), `remark` = VALUES(`remark`),
  `updated_by` = @admin_user_id, `updated_time` = @system_time, `deleted_flag` = 0;

-- 首页、大屏和 AI 不是普通资源表，但属于正式可授权能力。
INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
VALUES
  (NULL,'首页','MENU','dashboard:summary:view','/dashboard','RENIX_RBAC_V1: 首页看板',0,@admin_user_id,@system_time,@admin_user_id,@system_time,0),
  (NULL,'生产运营大屏','MENU','dashboard:big-screen:view','/big-screen/production','RENIX_RBAC_V1: 生产运营大屏',0,@admin_user_id,@system_time,@admin_user_id,@system_time,0),
  (NULL,'AI智能助手','MENU','ai:assistant:use',NULL,'RENIX_RBAC_V1: 只读智能问答和运营建议',0,@admin_user_id,@system_time,@admin_user_id,@system_time,0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `menu_type` = VALUES(`menu_type`), `route_path` = VALUES(`route_path`),
  `remark` = VALUES(`remark`), `updated_by` = @admin_user_id, `updated_time` = @system_time, `deleted_flag` = 0;

-- 通用 create/update/delete/export 权限；5 个只读资源只生成 export。
DROP TEMPORARY TABLE IF EXISTS `tmp_renix_action`;
CREATE TEMPORARY TABLE `tmp_renix_action` (`action_code` VARCHAR(16) PRIMARY KEY, `action_name` VARCHAR(32) NOT NULL)
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_renix_action` VALUES
  ('create','新增'),('update','修改'),('delete','删除'),('export','导出');

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT resource_menu.`menu_id`, CONCAT(resource.`resource_name`, '-', action.`action_name`), 'BUTTON',
       CONCAT(resource.`domain_code`, ':', SUBSTRING(resource.`resource_key`, LENGTH(resource.`domain_code`) + 2), ':', action.`action_code`),
       NULL, CONCAT(@rbac_marker, ': 通用资源动作'), 0,
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_resource` resource
CROSS JOIN `tmp_renix_action` action
JOIN `sys_menu` resource_menu
  ON resource_menu.`permission_code` = CONCAT(resource.`domain_code`, ':', SUBSTRING(resource.`resource_key`, LENGTH(resource.`domain_code`) + 2), ':view')
 AND resource_menu.`deleted_flag` = 0
WHERE (action.`action_code` = 'export' AND resource.`resource_key` NOT IN ('mfg-schedule-scenario','sys-notification'))
   OR (action.`action_code` <> 'export' AND resource.`resource_key` NOT IN (
     'sys-operation-log','sys-status-change-log','sys-notification','eam-equipment-status-log','wms-inventory','wms-stock-movement'
   ))
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = 'BUTTON',
  `remark` = VALUES(`remark`), `updated_by` = @admin_user_id, `updated_time` = @system_time, `deleted_flag` = 0;

-- 特殊业务动作必须独立授权。
DROP TEMPORARY TABLE IF EXISTS `tmp_renix_special_permission`;
CREATE TEMPORARY TABLE `tmp_renix_special_permission` (
  `resource_key` VARCHAR(64) NOT NULL,
  `permission_code` VARCHAR(64) PRIMARY KEY,
  `permission_name` VARCHAR(128) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_renix_special_permission` VALUES
  ('sys-user','sys:user:reset-password','重置用户密码'),
  ('sys-user','sys:user:assign-role','分配用户角色'),
  ('sys-role','sys:role:assign-menu','分配角色权限'),
  ('sys-notification','sys:notification:read','标记本人通知已读'),
  ('sys-notification','sys:notification:ack','确认本人通知'),
  ('sys-notification','sys:notification:audit','查看通知投递审计'),
  ('sys-file','sys:file:upload','上传附件'),
  ('sys-file','sys:file:download','下载附件'),
  ('mfg-andon-alert','mfg:andon-alert:trigger','触发安灯告警'),
  ('mfg-andon-alert','mfg:andon-alert:close','关闭安灯告警'),
  ('mfg-exception','mfg:exception:handle','处理生产异常'),
  ('mfg-production-order','mfg:production-order:release','下达生产订单'),
  ('mfg-production-order','mfg:production-order:withdraw','撤回生产订单修改'),
  ('mfg-production-plan','mfg:production-plan:generate','生成生产计划'),
  ('mfg-production-plan','mfg:production-plan:release','生效生产计划'),
  ('mfg-production-plan','mfg:production-plan:withdraw','撤回生产计划修改'),
  ('mfg-work-order','mfg:work-order:generate','生成生产工单'),
  ('mfg-work-order','mfg:work-order:release','下达生产工单'),
  ('mfg-work-order','mfg:work-order:withdraw','撤回生产工单修改'),
  ('mfg-dispatch-order','mfg:dispatch-order:generate','从生产工单生成派工单'),
  ('mfg-dispatch-order','mfg:dispatch-order:release','派发并生成生产任务'),
  ('mfg-dispatch-order','mfg:dispatch-order:withdraw','撤回派工'),
  ('mfg-work-order','mfg:work-order:start','开始生产工单'),
  ('mfg-task','mfg:task:start','开始生产任务'),
  ('mfg-task','mfg:task:withdraw','撤回任务开工'),
  ('mfg-task','mfg:task:finish','完成生产任务'),
  ('mfg-work-report','mfg:work-report:submit','提交生产报工'),
  ('mfg-work-report','mfg:work-report:audit','审核生产报工'),
  ('mfg-work-report','mfg:work-report:withdraw','撤回本人生产报工'),
  ('mfg-schedule-scenario','mfg:schedule-scenario:validate','校验排产草案'),
  ('mfg-schedule-scenario','mfg:schedule-scenario:ai-advice','生成排产 AI 建议'),
  ('mfg-schedule-scenario','mfg:schedule-scenario:confirm','确认排产并生成生产计划'),
  ('wms-inbound-order','wms:inbound-order:confirm','确认入库'),
  ('wms-inbound-order','wms:inbound-order:withdraw','撤回未确认入库单'),
  ('wms-outbound-order','wms:outbound-order:confirm','确认出库'),
  ('wms-outbound-order','wms:outbound-order:withdraw','撤回未确认出库单'),
  ('wms-return-order','wms:return-order:confirm','确认退料'),
  ('wms-return-order','wms:return-order:withdraw','撤回未确认退料单'),
  ('wms-stocktake-order','wms:stocktake-order:finish','完成盘点'),
  ('wms-replenishment','wms:replenishment:generate','生成补货建议'),
  ('qms-inspection-order','qms:inspection-order:submit','提交检验'),
  ('qms-inspection-order','qms:inspection-order:withdraw','撤回未检验检验单'),
  ('qms-inspection-result','qms:inspection-result:batch-save','批量保存检验结果'),
  ('qms-nonconforming-order','qms:nonconforming-order:approve','审批不合格品'),
  ('eam-repair-order','eam:repair-order:assign','维修派工'),
  ('eam-repair-order','eam:repair-order:close','关闭维修单'),
  ('trace-barcode','trace:barcode:generate','生成条码'),
  ('trace-barcode','trace:barcode:scan','扫码查询'),
  ('trace-product-lot','trace:product-lot:trace','产品批次追溯'),
  ('trace-material-usage','trace:material-usage:reverse-trace','原料批次反向追溯');

INSERT INTO `sys_menu`
  (`parent_id`, `menu_name`, `menu_type`, `permission_code`, `route_path`, `remark`,
   `version`, `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT resource_menu.`menu_id`, seed.`permission_name`, 'BUTTON', seed.`permission_code`, NULL,
       CONCAT(@rbac_marker, ': 高风险或独立业务动作'), 0,
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_special_permission` seed
JOIN `tmp_renix_resource` resource ON resource.`resource_key` = seed.`resource_key`
JOIN `sys_menu` resource_menu
  ON resource_menu.`permission_code` = CONCAT(resource.`domain_code`, ':', SUBSTRING(resource.`resource_key`, LENGTH(resource.`domain_code`) + 2), ':view')
 AND resource_menu.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `menu_name` = VALUES(`menu_name`), `menu_type` = 'BUTTON',
  `remark` = VALUES(`remark`), `updated_by` = @admin_user_id, `updated_time` = @system_time, `deleted_flag` = 0;

-- 5. 角色权限关系。先恢复本数据集关系，再按最小权限矩阵补齐。
UPDATE `sys_role_menu` relation
JOIN `sys_role` role ON role.`role_id` = relation.`role_id`
JOIN `sys_menu` menu ON menu.`menu_id` = relation.`menu_id`
SET relation.`deleted_flag` = 1
WHERE role.`role_code` IN (SELECT `role_code` FROM `tmp_renix_role`)
  AND menu.`remark` LIKE CONCAT(@rbac_marker, '%')
  AND relation.`deleted_flag` = 0;

DROP TEMPORARY TABLE IF EXISTS `tmp_renix_role_pattern`;
CREATE TEMPORARY TABLE `tmp_renix_role_pattern` (
  `role_code` VARCHAR(64) NOT NULL,
  `permission_pattern` VARCHAR(64) NOT NULL,
  PRIMARY KEY (`role_code`, `permission_pattern`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 超级管理员：全部权限。
INSERT INTO `tmp_renix_role_pattern` VALUES ('SUPER_ADMIN','%');

-- 生产经理：全域只读，组织和生产可维护并执行，系统仅查看审计。
INSERT INTO `tmp_renix_role_pattern` VALUES
  ('MES_MANAGER','module:%'),('MES_MANAGER','%:view'),('MES_MANAGER','%:export'),
  ('MES_MANAGER','org:%'),('MES_MANAGER','mfg:%'),('MES_MANAGER','dashboard:%'),('MES_MANAGER','ai:%');

-- 计划员：业务域只读，生产订单和计划可维护。
INSERT INTO `tmp_renix_role_pattern` VALUES
  ('PLAN_SCHEDULER','module:org:view'),('PLAN_SCHEDULER','module:mdm:view'),('PLAN_SCHEDULER','module:process:view'),
  ('PLAN_SCHEDULER','module:factory:view'),('PLAN_SCHEDULER','module:mfg:view'),('PLAN_SCHEDULER','module:wms:view'),
  ('PLAN_SCHEDULER','module:qms:view'),('PLAN_SCHEDULER','module:eam:view'),('PLAN_SCHEDULER','module:trace:view'),
  ('PLAN_SCHEDULER','org:%:view'),('PLAN_SCHEDULER','mdm:%:view'),('PLAN_SCHEDULER','process:%:view'),
  ('PLAN_SCHEDULER','factory:%:view'),('PLAN_SCHEDULER','mfg:%:view'),('PLAN_SCHEDULER','wms:%:view'),
  ('PLAN_SCHEDULER','qms:%:view'),('PLAN_SCHEDULER','eam:%:view'),('PLAN_SCHEDULER','trace:%:view'),
  ('PLAN_SCHEDULER','mfg:production-order:%'),('PLAN_SCHEDULER','mfg:production-plan:%'),
  ('PLAN_SCHEDULER','mfg:line-product-capability:%'),('PLAN_SCHEDULER','mfg:line-calendar-rule:%'),
  ('PLAN_SCHEDULER','mfg:schedule-scenario:%'),
  ('PLAN_SCHEDULER','dashboard:%'),('PLAN_SCHEDULER','ai:%');

-- 班组长：制造相关只读，派工、任务、报工、异常和安灯可执行。
INSERT INTO `tmp_renix_role_pattern` VALUES
  ('PRODUCTION_SUPERVISOR','module:org:view'),('PRODUCTION_SUPERVISOR','module:mdm:view'),
  ('PRODUCTION_SUPERVISOR','module:process:view'),('PRODUCTION_SUPERVISOR','module:factory:view'),
  ('PRODUCTION_SUPERVISOR','module:mfg:view'),('PRODUCTION_SUPERVISOR','module:wms:view'),
  ('PRODUCTION_SUPERVISOR','module:qms:view'),('PRODUCTION_SUPERVISOR','module:eam:view'),
  ('PRODUCTION_SUPERVISOR','module:trace:view'),('PRODUCTION_SUPERVISOR','org:%:view'),
  ('PRODUCTION_SUPERVISOR','mdm:%:view'),('PRODUCTION_SUPERVISOR','process:%:view'),
  ('PRODUCTION_SUPERVISOR','factory:%:view'),('PRODUCTION_SUPERVISOR','mfg:%:view'),
  ('PRODUCTION_SUPERVISOR','wms:%:view'),('PRODUCTION_SUPERVISOR','qms:%:view'),
  ('PRODUCTION_SUPERVISOR','eam:%:view'),('PRODUCTION_SUPERVISOR','trace:%:view'),
  ('PRODUCTION_SUPERVISOR','mfg:dispatch-order:%'),('PRODUCTION_SUPERVISOR','mfg:task:%'),
  ('PRODUCTION_SUPERVISOR','mfg:work-order:start'),
  ('PRODUCTION_SUPERVISOR','mfg:task-operator:%'),('PRODUCTION_SUPERVISOR','mfg:work-report:%'),
  ('PRODUCTION_SUPERVISOR','mfg:report-material:%'),('PRODUCTION_SUPERVISOR','mfg:exception:%'),
  ('PRODUCTION_SUPERVISOR','mfg:andon-alert:%'),('PRODUCTION_SUPERVISOR','dashboard:%'),
  ('PRODUCTION_SUPERVISOR','ai:%');

-- 操作员：只访问本人生产执行、指导书和扫码追溯。
INSERT INTO `tmp_renix_role_pattern` VALUES
  ('PRODUCTION_OPERATOR','module:process:view'),('PRODUCTION_OPERATOR','module:mfg:view'),
  ('PRODUCTION_OPERATOR','module:trace:view'),('PRODUCTION_OPERATOR','process:work-instruction:view'),
  ('PRODUCTION_OPERATOR','process:file:download'),('PRODUCTION_OPERATOR','mfg:task:view'),
  ('PRODUCTION_OPERATOR','mfg:task:start'),('PRODUCTION_OPERATOR','mfg:task:finish'),
  ('PRODUCTION_OPERATOR','mfg:work-report:view'),('PRODUCTION_OPERATOR','mfg:work-report:create'),
  ('PRODUCTION_OPERATOR','mfg:work-report:update'),('PRODUCTION_OPERATOR','mfg:work-report:submit'),
  ('PRODUCTION_OPERATOR','mfg:report-material:view'),('PRODUCTION_OPERATOR','mfg:report-material:create'),
  ('PRODUCTION_OPERATOR','mfg:report-material:update'),('PRODUCTION_OPERATOR','trace:barcode:view'),
  ('PRODUCTION_OPERATOR','trace:barcode:scan'),('PRODUCTION_OPERATOR','trace:product-lot:view'),
  ('PRODUCTION_OPERATOR','trace:product-lot:trace'),('PRODUCTION_OPERATOR','dashboard:summary:view');

-- 质量、仓储、设备岗位：本域全权限，依赖域只读。
INSERT INTO `tmp_renix_role_pattern` VALUES
  ('QUALITY_ENGINEER','module:mdm:view'),('QUALITY_ENGINEER','module:process:view'),
  ('QUALITY_ENGINEER','module:factory:view'),('QUALITY_ENGINEER','module:mfg:view'),
  ('QUALITY_ENGINEER','module:wms:view'),('QUALITY_ENGINEER','module:qms:view'),
  ('QUALITY_ENGINEER','module:eam:view'),('QUALITY_ENGINEER','module:trace:view'),
  ('QUALITY_ENGINEER','mdm:%:view'),('QUALITY_ENGINEER','process:%:view'),
  ('QUALITY_ENGINEER','factory:%:view'),('QUALITY_ENGINEER','mfg:%:view'),
  ('QUALITY_ENGINEER','wms:%:view'),('QUALITY_ENGINEER','qms:%'),
  ('QUALITY_ENGINEER','eam:%:view'),('QUALITY_ENGINEER','trace:%:view'),
  ('QUALITY_ENGINEER','dashboard:%'),('QUALITY_ENGINEER','ai:%'),
  ('WAREHOUSE_MANAGER','module:mdm:view'),('WAREHOUSE_MANAGER','module:mfg:view'),
  ('WAREHOUSE_MANAGER','module:wms:view'),('WAREHOUSE_MANAGER','module:qms:view'),
  ('WAREHOUSE_MANAGER','module:trace:view'),('WAREHOUSE_MANAGER','mdm:%:view'),
  ('WAREHOUSE_MANAGER','mfg:%:view'),('WAREHOUSE_MANAGER','wms:%'),
  ('WAREHOUSE_MANAGER','qms:%:view'),('WAREHOUSE_MANAGER','trace:%:view'),
  ('WAREHOUSE_MANAGER','dashboard:%'),('WAREHOUSE_MANAGER','ai:%'),
  ('EQUIPMENT_ENGINEER','module:org:view'),('EQUIPMENT_ENGINEER','module:mdm:view'),
  ('EQUIPMENT_ENGINEER','module:process:view'),('EQUIPMENT_ENGINEER','module:factory:view'),
  ('EQUIPMENT_ENGINEER','module:mfg:view'),('EQUIPMENT_ENGINEER','module:qms:view'),
  ('EQUIPMENT_ENGINEER','module:eam:view'),('EQUIPMENT_ENGINEER','module:trace:view'),
  ('EQUIPMENT_ENGINEER','org:%:view'),('EQUIPMENT_ENGINEER','mdm:%:view'),
  ('EQUIPMENT_ENGINEER','process:%:view'),('EQUIPMENT_ENGINEER','factory:%:view'),
  ('EQUIPMENT_ENGINEER','mfg:%:view'),('EQUIPMENT_ENGINEER','qms:%:view'),
  ('EQUIPMENT_ENGINEER','eam:%'),('EQUIPMENT_ENGINEER','trace:%:view'),
  ('EQUIPMENT_ENGINEER','mfg:exception:handle'),('EQUIPMENT_ENGINEER','mfg:andon-alert:close'),
  ('EQUIPMENT_ENGINEER','dashboard:%'),('EQUIPMENT_ENGINEER','ai:%');

-- 所有有效岗位都能使用个人消息中心；全局投递审计仅由 SUPER_ADMIN 的全权限模式获得。
INSERT INTO `tmp_renix_role_pattern`
SELECT role_seed.`role_code`, permission_seed.`permission_pattern`
FROM `tmp_renix_role` role_seed
CROSS JOIN (
  SELECT 'sys:notification:view' AS `permission_pattern`
  UNION ALL SELECT 'sys:notification:read'
  UNION ALL SELECT 'sys:notification:ack'
) permission_seed
ON DUPLICATE KEY UPDATE `permission_pattern` = VALUES(`permission_pattern`);

-- Mini program cross-domain read access and lookup dependencies.
INSERT INTO `tmp_renix_role_pattern` VALUES
  ('PRODUCTION_OPERATOR','mfg:production-plan:view'),('PRODUCTION_OPERATOR','mfg:work-order:view'),
  ('PRODUCTION_OPERATOR','mdm:product:view'),('PRODUCTION_OPERATOR','process:operation:view'),
  ('PRODUCTION_OPERATOR','factory:station:view'),
  ('QUALITY_ENGINEER','trace:barcode:scan'),
  ('WAREHOUSE_MANAGER','trace:barcode:scan'),('WAREHOUSE_MANAGER','eam:equipment:view'),
  ('WAREHOUSE_MANAGER','eam:repair-order:view'),('WAREHOUSE_MANAGER','factory:station:view'),
  ('WAREHOUSE_MANAGER','org:employee:view'),
  ('EQUIPMENT_ENGINEER','trace:barcode:scan'),('EQUIPMENT_ENGINEER','wms:inbound-order:view'),
  ('EQUIPMENT_ENGINEER','wms:inbound-detail:view'),('EQUIPMENT_ENGINEER','wms:outbound-order:view'),
  ('EQUIPMENT_ENGINEER','wms:outbound-detail:view'),('EQUIPMENT_ENGINEER','wms:inventory:view'),
  ('EQUIPMENT_ENGINEER','wms:warehouse:view'),('EQUIPMENT_ENGINEER','wms:location:view'),
  ('EQUIPMENT_ENGINEER','wms:material-batch:view');

INSERT INTO `sys_role_menu`
  (`role_id`, `menu_id`, `created_by`, `created_time`, `deleted_flag`)
SELECT DISTINCT role.`role_id`, menu.`menu_id`, @admin_user_id, @system_time, 0
FROM `tmp_renix_role_pattern` pattern
JOIN `sys_role` role ON role.`role_code` = pattern.`role_code` AND role.`deleted_flag` = 0
JOIN `sys_menu` menu ON menu.`permission_code` LIKE pattern.`permission_pattern` AND menu.`deleted_flag` = 0
ON DUPLICATE KEY UPDATE `deleted_flag` = 0;

-- 生产下游只允许从上游上下文生成；Service 层对所有角色阻断通用新增。
UPDATE `sys_role_menu` relation
JOIN `sys_role` role
  ON role.`role_id` = relation.`role_id`
 AND role.`deleted_flag` = 0
JOIN `sys_menu` menu
  ON menu.`menu_id` = relation.`menu_id`
 AND menu.`deleted_flag` = 0
SET relation.`deleted_flag` = 1
WHERE menu.`permission_code` IN (
    'mfg:production-plan:create',
    'mfg:work-order:create',
    'mfg:dispatch-order:create',
    'mfg:task:create'
  )
  AND relation.`deleted_flag` = 0;

-- 6. 企业状态字典。所有业务值使用英文编码，中文只保存为显示标签。
DROP TEMPORARY TABLE IF EXISTS `tmp_renix_dict`;
CREATE TEMPORARY TABLE `tmp_renix_dict` (
  `type_code` VARCHAR(64) NOT NULL,
  `type_name` VARCHAR(128) NOT NULL,
  `dict_value` VARCHAR(64) NOT NULL,
  `dict_label` VARCHAR(128) NOT NULL,
  `sort_no` INT NOT NULL,
  PRIMARY KEY (`type_code`, `dict_value`)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_renix_dict` VALUES
  ('common_enable_status','通用启停状态','ENABLE','启用',10),('common_enable_status','通用启停状态','DISABLE','停用',20),
  ('user_status','用户状态','ENABLE','正常',10),('user_status','用户状态','DISABLE','停用',20),('user_status','用户状态','LOCKED','锁定',30),
  ('employee_status','员工状态','ENABLE','在职',10),('employee_status','员工状态','RESIGNED','离职',20),('employee_status','员工状态','DISABLE','停用',30),
  ('skill_level','技能等级','BEGINNER','初级',10),('skill_level','技能等级','INTERMEDIATE','中级',20),('skill_level','技能等级','ADVANCED','高级',30),('skill_level','技能等级','EXPERT','专家',40),
  ('data_scope','数据范围','ALL','全部数据',10),('data_scope','数据范围','DEPARTMENT','本部门',20),('data_scope','数据范围','TEAM','本班组',30),('data_scope','数据范围','SELF','仅本人',40),
  ('menu_type','菜单类型','DIRECTORY','目录',10),('menu_type','菜单类型','MENU','菜单',20),('menu_type','菜单类型','BUTTON','按钮',30),('menu_type','菜单类型','API','接口',40),
  ('release_status','发布状态','DRAFT','草稿',10),('release_status','发布状态','RELEASED','已发布',20),('release_status','发布状态','DISABLE','停用',30),
  ('material_type','物料类型','RAW','原料',10),('material_type','物料类型','SEMI_FINISHED','半成品',20),('material_type','物料类型','FINISHED','成品',30),('material_type','物料类型','AUXILIARY','辅料',40),
  ('production_order_status','生产订单状态','DRAFT','新建',10),('production_order_status','生产订单状态','RELEASED','已排产',20),('production_order_status','生产订单状态','RUNNING','生产中',30),('production_order_status','生产订单状态','FINISHED','已完成',40),('production_order_status','生产订单状态','CLOSED','已关闭',50),
  ('production_plan_status','生产计划状态','DRAFT','待生效',10),('production_plan_status','生产计划状态','RELEASED','已生效',20),('production_plan_status','生产计划状态','RUNNING','执行中',30),('production_plan_status','生产计划状态','FINISHED','已完成',40),('production_plan_status','生产计划状态','CLOSED','已关闭',50),
  ('work_order_status','生产工单状态','DRAFT','新建',10),('work_order_status','生产工单状态','APPROVED','已审核',20),('work_order_status','生产工单状态','RELEASED','已下达',30),('work_order_status','生产工单状态','RUNNING','生产中',40),('work_order_status','生产工单状态','FINISHED','已完成',50),('work_order_status','生产工单状态','CLOSED','已关闭',60),
  ('readiness_status','齐套状态','WAIT_READY','待齐套',10),('readiness_status','齐套状态','PARTIAL_READY','部分齐套',20),('readiness_status','齐套状态','READY','已齐套',30),
  ('dispatch_status','派工状态','WAIT_DISPATCH','待派发',10),('dispatch_status','派工状态','RELEASED','已派发',20),('dispatch_status','派工状态','ACCEPTED','已接受',30),('dispatch_status','派工状态','RUNNING','生产中',40),('dispatch_status','派工状态','FINISHED','已完成',50),('dispatch_status','派工状态','REJECTED','已驳回',60),
  ('task_status','生产任务状态','WAIT_START','待开工',10),('task_status','生产任务状态','RUNNING','生产中',20),('task_status','生产任务状态','PAUSED','暂停',30),('task_status','生产任务状态','FINISHED','已完工',40),('task_status','生产任务状态','CLOSED','已关闭',50),
  ('audit_status','审核状态','SUBMITTED','待审核',10),('audit_status','审核状态','APPROVED','已通过',20),('audit_status','审核状态','REJECTED','已驳回',30),
  ('approval_status','审批状态','SUBMITTED','待审批',10),('approval_status','审批状态','APPROVED','已通过',20),('approval_status','审批状态','REJECTED','已驳回',30),
  ('exception_status','异常状态','SUBMITTED','待处理',10),('exception_status','异常状态','PROCESSING','处理中',20),('exception_status','异常状态','RESOLVED','已解决',30),('exception_status','异常状态','CLOSED','已关闭',40),
  ('andon_status','安灯状态','ACTIVE','告警中',10),('andon_status','安灯状态','PROCESSING','处理中',20),('andon_status','安灯状态','CLOSED','已解除',30),
  ('warehouse_type','仓库类型','RAW','原材料仓',10),('warehouse_type','仓库类型','SEMI_FINISHED','半成品仓',20),('warehouse_type','仓库类型','FINISHED','成品仓',30),('warehouse_type','仓库类型','DEFECTIVE','不良品仓',40),
  ('location_type','库位类型','NORMAL','正常库位',10),('location_type','库位类型','INSPECTION','待检库位',20),('location_type','库位类型','FROZEN','冻结库位',30),('location_type','库位类型','DEFECTIVE','不良品库位',40),
  ('location_status','库位状态','IDLE','空闲',10),('location_status','库位状态','OCCUPIED','占用',20),('location_status','库位状态','LOCKED','锁定',30),('location_status','库位状态','MAINTAINING','维护',40),
  ('batch_quality_status','批次质量状态','WAIT_INSPECTION','待检',10),('batch_quality_status','批次质量状态','PASS','合格',20),('batch_quality_status','批次质量状态','FAIL','不合格',30),('batch_quality_status','批次质量状态','CONCESSION','让步接收',40),
  ('batch_status','批次状态','AVAILABLE','可用',10),('batch_status','批次状态','FROZEN','冻结',20),('batch_status','批次状态','EXHAUSTED','耗尽',30),('batch_status','批次状态','SCRAPPED','报废',40),
  ('inventory_movement_type','库存流水类型','INBOUND','入库',10),('inventory_movement_type','库存流水类型','OUTBOUND','出库',20),('inventory_movement_type','库存流水类型','RETURN','退料',30),('inventory_movement_type','库存流水类型','STOCKTAKE','盘点调整',40),
  ('inbound_type','入库类型','PURCHASE','采购入库',10),('inbound_type','入库类型','PRODUCTION','生产入库',20),
  ('outbound_type','出库类型','MATERIAL_ISSUE','生产领料',10),('outbound_type','出库类型','SALE','销售出库',20),
  ('return_type','退料类型','MATERIAL_RETURN','生产退料',10),
  ('stocktake_status','盘点状态','WAIT_START','待盘',10),('stocktake_status','盘点状态','RUNNING','盘点中',20),('stocktake_status','盘点状态','FINISHED','已完成',30),('stocktake_status','盘点状态','CLOSED','已关闭',40),
  ('replenishment_status','补货建议状态','SUBMITTED','待处理',10),('replenishment_status','补货建议状态','APPROVED','已确认',20),('replenishment_status','补货建议状态','CLOSED','已关闭',30),
  ('inspection_type','检验类型','INCOMING','来料检验',10),('inspection_type','检验类型','PROCESS','过程检验',20),('inspection_type','检验类型','FINAL','最终检验',30),
  ('inspection_result','检验结果','PASS','合格',10),('inspection_result','检验结果','FAIL','不合格',20),('inspection_result','检验结果','CONCESSION','让步接收',30),
  ('inspection_order_status','检验单状态','DRAFT','待检',10),('inspection_order_status','检验单状态','RUNNING','检验中',20),('inspection_order_status','检验单状态','FINISHED','已完成',30),
  ('ncr_status','不合格品状态','SUBMITTED','待处理',10),('ncr_status','不合格品状态','PROCESSING','处理中',20),('ncr_status','不合格品状态','CLOSED','已关闭',30),
  ('disposition_type','不合格处置类型','REWORK','返工',10),('disposition_type','不合格处置类型','SCRAP','报废',20),('disposition_type','不合格处置类型','DOWNGRADE','降级',30),('disposition_type','不合格处置类型','CONCESSION','让步接收',40),
  ('equipment_status','设备状态','IDLE','空闲',10),('equipment_status','设备状态','RUNNING','运行',20),('equipment_status','设备状态','MAINTAINING','保养中',30),('equipment_status','设备状态','REPAIRING','维修中',40),('equipment_status','设备状态','STOPPED','停机',50),('equipment_status','设备状态','FAULT','故障',60),('equipment_status','设备状态','SCRAPPED','报废',70),
  ('downtime_type','停机类型','PLANNED','计划停机',10),('downtime_type','停机类型','BREAKDOWN','故障停机',20),('downtime_type','停机类型','QUALITY_ADJUSTMENT','质量调整',30),('downtime_type','停机类型','CHANGEOVER','换型停机',40),
  ('alert_level','告警级别','INFO','提示',10),('alert_level','告警级别','MEDIUM','中等',20),('alert_level','告警级别','HIGH','高',30),('alert_level','告警级别','CRITICAL','严重',40),
  ('maintenance_type','保养类型','PREVENTIVE','预防保养',10),('maintenance_type','保养类型','CALIBRATION','周期校准',20),('maintenance_type','保养类型','CORRECTIVE','纠正维护',30),
  ('cycle_type','周期类型','DAY','按天',10),('cycle_type','周期类型','WEEK','按周',20),('cycle_type','周期类型','MONTH','按月',30),('cycle_type','周期类型','HOUR','按小时',40),('cycle_type','周期类型','COUNT','按次数',50),
  ('check_result','点检结果','NORMAL','正常',10),('check_result','点检结果','ABNORMAL','异常',20),
  ('repair_status','维修状态','SUBMITTED','待派工',10),('repair_status','维修状态','REPAIRING','维修中',20),('repair_status','维修状态','WAIT_ACCEPTANCE','待验收',30),('repair_status','维修状态','CLOSED','已关闭',40),
  ('barcode_type','条码类型','PRODUCT_LOT','产品批次码',10),('barcode_type','条码类型','OUTER_CARTON','外箱码',20),('barcode_type','条码类型','RETAIN_SAMPLE','留样码',30),('barcode_type','条码类型','PALLET','托盘码',40),
  ('barcode_status','条码状态','ENABLE','启用',10),('barcode_status','条码状态','DISABLE','停用',20),('barcode_status','条码状态','INVALIDATED','已作废',30),
  ('product_lot_status','产品批次状态','RUNNING','生产中',10),('product_lot_status','产品批次状态','WAIT_INSPECTION','待检',20),('product_lot_status','产品批次状态','PASS','合格',30),('product_lot_status','产品批次状态','STORED','已入库',40),('product_lot_status','产品批次状态','CLOSED','已关闭',50),
  ('order_source_type','订单来源','FORECAST_CONFIRMED','预测转正式',10),('order_source_type','订单来源','MANUAL','手工订单',20),('order_source_type','订单来源','OEM','OEM委托',30);

INSERT INTO `sys_dict_type`
  (`dict_type_code`, `dict_type_name`, `status`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT DISTINCT seed.`type_code`, seed.`type_name`, 'ENABLE', CONCAT(@system_marker, ': 企业统一状态字典'), 0,
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_dict` seed
ON DUPLICATE KEY UPDATE
  `dict_type_name` = VALUES(`dict_type_name`), `status` = 'ENABLE', `remark` = VALUES(`remark`),
  `updated_by` = @admin_user_id, `updated_time` = @system_time, `deleted_flag` = 0;

INSERT INTO `sys_dict_data`
  (`dict_type_code`, `dict_value`, `dict_label`, `sort_no`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT seed.`type_code`, seed.`dict_value`, seed.`dict_label`, seed.`sort_no`,
       CONCAT(@system_marker, ': 企业统一状态字典项'), 0,
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_dict` seed
ON DUPLICATE KEY UPDATE
  `dict_label` = VALUES(`dict_label`), `sort_no` = VALUES(`sort_no`), `remark` = VALUES(`remark`),
  `updated_by` = @admin_user_id, `updated_time` = @system_time, `deleted_flag` = 0;

-- 7. 非敏感系统参数。
DROP TEMPORARY TABLE IF EXISTS `tmp_renix_param`;
CREATE TEMPORARY TABLE `tmp_renix_param` (
  `param_key` VARCHAR(64) PRIMARY KEY,
  `param_value` VARCHAR(500) NOT NULL,
  `param_name` VARCHAR(128) NOT NULL,
  `module_code` VARCHAR(64) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_renix_param` VALUES
  ('company.code','LNX','企业编码','company'),
  ('company.name','雷尼克斯体育用品制造有限公司','企业名称','company'),
  ('company.brand','雷尼克斯','自有品牌','company'),
  ('system.timezone','Asia/Shanghai','业务时区','system'),
  ('production.workdays','MON,TUE,WED,THU,FRI,SAT','生产工作日','production'),
  ('production.default_shift','SHIFT-DAY','默认生产班次','production'),
  ('inventory.issue.strategy','FIFO','库存发料策略','inventory'),
  ('inventory.negative.allowed','false','是否允许负库存','inventory'),
  ('inventory.replenishment.cover_days','4','补货建议覆盖生产日','inventory'),
  ('quality.final_inspection.required','true','成品入库是否必须终检','quality'),
  ('trace.carton.capacity','25','每外箱成品筒数','trace'),
  ('demo.live.anchor_date','2026-07-14','企业演示运行快照基准','demo');

INSERT INTO `sys_system_param`
  (`param_key`, `param_value`, `param_name`, `module_code`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT seed.`param_key`, seed.`param_value`, seed.`param_name`, seed.`module_code`,
       CONCAT(@system_marker, ': 非敏感企业参数'), 0,
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_param` seed
ON DUPLICATE KEY UPDATE
  `param_value` = VALUES(`param_value`), `param_name` = VALUES(`param_name`),
  `module_code` = VALUES(`module_code`), `remark` = VALUES(`remark`),
  `updated_by` = @admin_user_id, `updated_time` = @system_time, `deleted_flag` = 0;

-- 8. 作业指导书文件元数据和工序关系。文件由生成脚本同步到上传根目录的 sop/ 子目录。
DROP TEMPORARY TABLE IF EXISTS `tmp_renix_sop`;
CREATE TEMPORARY TABLE `tmp_renix_sop` (
  `file_name` VARCHAR(128) NOT NULL,
  `file_url` VARCHAR(255) PRIMARY KEY,
  `operation_code` VARCHAR(64) NOT NULL,
  `product_code` VARCHAR(64) NULL,
  `title` VARCHAR(128) NOT NULL
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
INSERT INTO `tmp_renix_sop` VALUES
  ('鸭毛片分选作业指导书V1.0.pdf','/upload/sop/sop-feather-sort-duck-v1.0.pdf','OP-FEATHER-SORT','PROD-LNX-T300-77','鸭毛片分选作业指导书'),
  ('鹅毛片分选作业指导书V1.0.pdf','/upload/sop/sop-feather-sort-goose-v1.0.pdf','OP-FEATHER-SORT','PROD-LNX-M700-77','鹅毛片分选作业指导书'),
  ('复合软木球头成型作业指导书V1.0.pdf','/upload/sop/sop-cork-form-composite-v1.0.pdf','OP-CORK-FORM','PROD-LNX-T300-77','复合软木球头成型作业指导书'),
  ('天然软木球头成型作业指导书V1.0.pdf','/upload/sop/sop-cork-form-natural-v1.0.pdf','OP-CORK-FORM','PROD-LNX-M700-77','天然软木球头成型作业指导书'),
  ('植毛作业指导书V1.0.pdf','/upload/sop/sop-feather-insert-v1.0.pdf','OP-FEATHER-INSERT',NULL,'植毛作业指导书'),
  ('绕线作业指导书V1.0.pdf','/upload/sop/sop-thread-wind-v1.0.pdf','OP-THREAD-WIND',NULL,'绕线作业指导书'),
  ('涂胶固化作业指导书V1.0.pdf','/upload/sop/sop-glue-cure-v1.0.pdf','OP-GLUE-CURE',NULL,'涂胶固化作业指导书'),
  ('T300-76速度与飞行检测指导书V1.0.pdf','/upload/sop/sop-flight-test-t300-76-v1.0.pdf','OP-SPEED-TEST','PROD-LNX-T300-76','T300-76速度与飞行检测指导书'),
  ('T300-77速度与飞行检测指导书V1.0.pdf','/upload/sop/sop-flight-test-t300-77-v1.0.pdf','OP-SPEED-TEST','PROD-LNX-T300-77','T300-77速度与飞行检测指导书'),
  ('M700-77速度与飞行检测指导书V1.0.pdf','/upload/sop/sop-flight-test-m700-77-v1.0.pdf','OP-SPEED-TEST','PROD-LNX-M700-77','M700-77速度与飞行检测指导书'),
  ('M700-78速度与飞行检测指导书V1.0.pdf','/upload/sop/sop-flight-test-m700-78-v1.0.pdf','OP-SPEED-TEST','PROD-LNX-M700-78','M700-78速度与飞行检测指导书'),
  ('迅羽C500-77速度与飞行检测指导书V1.0.pdf','/upload/sop/sop-flight-test-c500-77-v1.0.pdf','OP-SPEED-TEST','PROD-OEM-XY-C500-77','迅羽C500-77速度与飞行检测指导书'),
  ('T300系列包装作业指导书V1.0.pdf','/upload/sop/sop-package-t300-v1.0.pdf','OP-PACK','PROD-LNX-T300-77','T300系列包装作业指导书'),
  ('M700系列包装作业指导书V1.0.pdf','/upload/sop/sop-package-m700-v1.0.pdf','OP-PACK','PROD-LNX-M700-77','M700系列包装作业指导书'),
  ('迅羽C500-OEM包装作业指导书V1.0.pdf','/upload/sop/sop-package-c500-oem-v1.0.pdf','OP-PACK','PROD-OEM-XY-C500-77','迅羽C500 OEM包装作业指导书');

INSERT INTO `sys_file`
  (`file_name`, `file_url`, `file_type`, `biz_type`, `biz_id`, `remark`, `version`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT seed.`file_name`, seed.`file_url`, 'pdf', 'WORK_INSTRUCTION', NULL,
       CONCAT(@system_marker, ': 真实作业指导书PDF'), 0,
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_sop` seed
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_file` existing WHERE existing.`file_url` = seed.`file_url`
);

UPDATE `sys_file` file
JOIN `tmp_renix_sop` seed ON seed.`file_url` = file.`file_url`
SET file.`file_name` = seed.`file_name`, file.`file_type` = 'pdf', file.`biz_type` = 'WORK_INSTRUCTION',
    file.`remark` = CONCAT(@system_marker, ': 真实作业指导书PDF'), file.`updated_by` = @admin_user_id,
    file.`updated_time` = @system_time, file.`deleted_flag` = 0;

INSERT INTO `process_work_instruction`
  (`operation_id`, `product_id`, `version`, `title`, `file_id`, `effective_time`, `status`, `remark`,
   `created_by`, `created_time`, `updated_by`, `updated_time`, `deleted_flag`)
SELECT operation.`operation_id`, product.`product_id`, 'V1.0', seed.`title`, file.`file_id`,
       '2026-06-30 08:00:00', 'RELEASED', CONCAT(@system_marker, ': 经批准生效的作业指导书'),
       @admin_user_id, @system_time, @admin_user_id, @system_time, 0
FROM `tmp_renix_sop` seed
JOIN `process_operation` operation
  ON operation.`operation_code` = seed.`operation_code` AND operation.`deleted_flag` = 0
LEFT JOIN `mdm_product` product
  ON product.`product_code` = seed.`product_code` AND product.`deleted_flag` = 0
JOIN `sys_file` file ON file.`file_url` = seed.`file_url` AND file.`deleted_flag` = 0
WHERE NOT EXISTS (
  SELECT 1 FROM `process_work_instruction` existing
  WHERE existing.`title` = seed.`title` AND existing.`version` = 'V1.0'
);

UPDATE `process_work_instruction` instruction
JOIN `tmp_renix_sop` seed ON seed.`title` = instruction.`title` AND instruction.`version` = 'V1.0'
JOIN `process_operation` operation
  ON operation.`operation_code` = seed.`operation_code` AND operation.`deleted_flag` = 0
LEFT JOIN `mdm_product` product
  ON product.`product_code` = seed.`product_code` AND product.`deleted_flag` = 0
JOIN `sys_file` file ON file.`file_url` = seed.`file_url` AND file.`deleted_flag` = 0
SET instruction.`operation_id` = operation.`operation_id`, instruction.`product_id` = product.`product_id`,
    instruction.`file_id` = file.`file_id`, instruction.`effective_time` = '2026-06-30 08:00:00',
    instruction.`status` = 'RELEASED', instruction.`remark` = CONCAT(@system_marker, ': 经批准生效的作业指导书'),
    instruction.`updated_by` = @admin_user_id, instruction.`updated_time` = @system_time,
    instruction.`deleted_flag` = 0;

UPDATE `sys_file` file
JOIN `process_work_instruction` instruction ON instruction.`file_id` = file.`file_id` AND instruction.`deleted_flag` = 0
SET file.`biz_id` = instruction.`instruction_id`, file.`updated_by` = @admin_user_id,
    file.`updated_time` = @system_time
WHERE file.`remark` LIKE CONCAT(@system_marker, '%') AND file.`deleted_flag` = 0;

-- 9. 完成标记。
INSERT INTO `sys_operation_log`
  (`user_id`, `operation_type`, `module_code`, `biz_id`, `request_ip`, `operation_time`, `remark`,
   `created_by`, `created_time`, `deleted_flag`)
SELECT @admin_user_id, 'SEED_RENIX_SYSTEM_BASE', 'db_seed', NULL, '127.0.0.1', @system_time,
       @system_marker, @admin_user_id, @system_time, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_operation_log`
  WHERE `operation_type` = 'SEED_RENIX_SYSTEM_BASE' AND `module_code` = 'db_seed'
    AND `remark` = @system_marker AND `deleted_flag` = 0
);

COMMIT;

SELECT @system_marker AS `dataset`,
       (SELECT COUNT(*) FROM `sys_role` WHERE `remark` LIKE CONCAT(@rbac_marker, '%') AND `deleted_flag` = 0) AS `roles`,
       (SELECT COUNT(*) FROM `sys_user` WHERE `username` IN (SELECT `username` FROM `tmp_renix_user`) AND `deleted_flag` = 0) AS `users`,
       (SELECT COUNT(*) FROM `sys_menu` WHERE `remark` LIKE CONCAT(@rbac_marker, '%') AND `deleted_flag` = 0) AS `permissions`,
       (SELECT COUNT(*) FROM `sys_dict_type` WHERE `remark` LIKE CONCAT(@system_marker, '%') AND `deleted_flag` = 0) AS `dict_types`,
       (SELECT COUNT(*) FROM `process_work_instruction` WHERE `remark` LIKE CONCAT(@system_marker, '%') AND `deleted_flag` = 0) AS `work_instructions`;
