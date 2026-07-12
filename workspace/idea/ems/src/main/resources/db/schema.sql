-- Badminton MES database schema
-- Generated from md/database_table_analysis.md, target database: mes
-- MySQL 8.0 / InnoDB / utf8mb4 / logical foreign keys
-- WARNING: This script drops and recreates all MES tables. Select the target database explicitly.
-- Example: mysql -u root -p mes < schema.sql

CREATE DATABASE IF NOT EXISTS `mes` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `trace_material_usage`;
DROP TABLE IF EXISTS `trace_product_lot`;
DROP TABLE IF EXISTS `trace_barcode`;
DROP TABLE IF EXISTS `eam_repair_order`;
DROP TABLE IF EXISTS `eam_spot_check_record`;
DROP TABLE IF EXISTS `eam_spot_check_plan`;
DROP TABLE IF EXISTS `eam_maintenance_record`;
DROP TABLE IF EXISTS `eam_maintenance_plan`;
DROP TABLE IF EXISTS `eam_equipment_status_log`;
DROP TABLE IF EXISTS `eam_equipment`;
DROP TABLE IF EXISTS `eam_equipment_category`;
DROP TABLE IF EXISTS `qms_nonconforming_detail`;
DROP TABLE IF EXISTS `qms_nonconforming_order`;
DROP TABLE IF EXISTS `qms_inspection_result`;
DROP TABLE IF EXISTS `qms_inspection_order`;
DROP TABLE IF EXISTS `qms_inspection_scheme_item`;
DROP TABLE IF EXISTS `qms_inspection_scheme`;
DROP TABLE IF EXISTS `qms_inspection_standard`;
DROP TABLE IF EXISTS `qms_inspection_item`;
DROP TABLE IF EXISTS `wms_replenishment_suggestion`;
DROP TABLE IF EXISTS `wms_stocktake_detail`;
DROP TABLE IF EXISTS `wms_stocktake_order`;
DROP TABLE IF EXISTS `wms_return_detail`;
DROP TABLE IF EXISTS `wms_return_order`;
DROP TABLE IF EXISTS `wms_outbound_detail`;
DROP TABLE IF EXISTS `wms_outbound_order`;
DROP TABLE IF EXISTS `wms_inbound_detail`;
DROP TABLE IF EXISTS `wms_inbound_order`;
DROP TABLE IF EXISTS `wms_stock_movement`;
DROP TABLE IF EXISTS `wms_inventory`;
DROP TABLE IF EXISTS `wms_material_batch`;
DROP TABLE IF EXISTS `wms_location`;
DROP TABLE IF EXISTS `wms_warehouse`;
DROP TABLE IF EXISTS `mfg_andon_alert`;
DROP TABLE IF EXISTS `mfg_exception`;
DROP TABLE IF EXISTS `mfg_report_material`;
DROP TABLE IF EXISTS `mfg_work_report`;
DROP TABLE IF EXISTS `mfg_task_operator`;
DROP TABLE IF EXISTS `mfg_task`;
DROP TABLE IF EXISTS `mfg_dispatch_order`;
DROP TABLE IF EXISTS `mfg_work_order_material`;
DROP TABLE IF EXISTS `mfg_work_order`;
DROP TABLE IF EXISTS `mfg_production_plan`;
DROP TABLE IF EXISTS `mfg_production_order`;
DROP TABLE IF EXISTS `factory_station_operation`;
DROP TABLE IF EXISTS `factory_station`;
DROP TABLE IF EXISTS `factory_line`;
DROP TABLE IF EXISTS `factory_workshop`;
DROP TABLE IF EXISTS `process_work_instruction`;
DROP TABLE IF EXISTS `process_parameter`;
DROP TABLE IF EXISTS `process_route_item`;
DROP TABLE IF EXISTS `process_route`;
DROP TABLE IF EXISTS `process_operation`;
DROP TABLE IF EXISTS `mdm_bom_item`;
DROP TABLE IF EXISTS `mdm_bom`;
DROP TABLE IF EXISTS `mdm_product`;
DROP TABLE IF EXISTS `mdm_material`;
DROP TABLE IF EXISTS `mdm_material_category`;
DROP TABLE IF EXISTS `mdm_customer`;
DROP TABLE IF EXISTS `mdm_supplier`;
DROP TABLE IF EXISTS `mdm_unit`;
DROP TABLE IF EXISTS `org_shift`;
DROP TABLE IF EXISTS `org_team_member`;
DROP TABLE IF EXISTS `org_team`;
DROP TABLE IF EXISTS `org_employee`;
DROP TABLE IF EXISTS `org_department`;
DROP TABLE IF EXISTS `sys_approval_record`;
DROP TABLE IF EXISTS `sys_status_change_log`;
DROP TABLE IF EXISTS `sys_operation_log`;
DROP TABLE IF EXISTS `sys_file`;
DROP TABLE IF EXISTS `sys_system_param`;
DROP TABLE IF EXISTS `sys_dict_data`;
DROP TABLE IF EXISTS `sys_dict_type`;
DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `sys_user` (
  `user_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户主键。',
  `username` VARCHAR(64) NULL COMMENT '登录账号，唯一。',
  `password_hash` VARCHAR(128) NULL COMMENT '加密后的密码。',
  `employee_id` BIGINT UNSIGNED NULL COMMENT '关联员工档案。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:正常，DISABLE:停用，LOCKED:锁定。',
  `login_fail_count` INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数。',
  `locked_time` DATETIME NULL COMMENT '自动或人工锁定时间。',
  `last_login_time` DATETIME NULL COMMENT '最后登录时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_employee_id` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

CREATE TABLE `sys_role` (
  `role_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色主键。',
  `role_code` VARCHAR(64) NULL COMMENT '角色编码，唯一。',
  `role_name` VARCHAR(128) NULL COMMENT '角色名称。',
  `data_scope` VARCHAR(32) NULL COMMENT '数据范围，如全部、部门、本班组、本人。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_sys_role_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

CREATE TABLE `sys_menu` (
  `menu_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单主键。',
  `parent_id` BIGINT UNSIGNED NULL COMMENT '上级菜单。',
  `menu_name` VARCHAR(128) NULL COMMENT '菜单名称。',
  `menu_type` VARCHAR(32) NULL COMMENT '目录、菜单、按钮、接口。',
  `permission_code` VARCHAR(64) NULL COMMENT '权限标识。',
  `route_path` VARCHAR(255) NULL COMMENT '前端路由。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`menu_id`),
  UNIQUE KEY `uk_sys_menu_permission_code` (`permission_code`),
  KEY `idx_sys_menu_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单权限表';

CREATE TABLE `sys_user_role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键。',
  `user_id` BIGINT UNSIGNED NULL COMMENT '用户 ID。',
  `role_id` BIGINT UNSIGNED NULL COMMENT '角色 ID。',
  `assigned_time` DATETIME NULL COMMENT '授权时间。',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`),
  KEY `idx_sys_user_role_user_id` (`user_id`),
  KEY `idx_sys_user_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

CREATE TABLE `sys_role_menu` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键。',
  `role_id` BIGINT UNSIGNED NULL COMMENT '角色 ID。',
  `menu_id` BIGINT UNSIGNED NULL COMMENT '菜单或按钮 ID。',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_menu` (`role_id`, `menu_id`),
  KEY `idx_sys_role_menu_role_id` (`role_id`),
  KEY `idx_sys_role_menu_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

CREATE TABLE `sys_dict_type` (
  `dict_type_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '字典类型主键。',
  `dict_type_code` VARCHAR(64) NULL COMMENT '字典类型编码，唯一。',
  `dict_type_name` VARCHAR(128) NULL COMMENT '字典类型名称。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`dict_type_id`),
  UNIQUE KEY `uk_sys_dict_type_dict_type_code` (`dict_type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典类型表';

CREATE TABLE `sys_dict_data` (
  `dict_data_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '字典数据主键。',
  `dict_type_code` VARCHAR(64) NULL COMMENT '所属字典类型。',
  `dict_value` VARCHAR(64) NULL COMMENT '字典值。',
  `dict_label` VARCHAR(128) NULL COMMENT '显示文本。',
  `sort_no` INT NULL COMMENT '排序号。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`dict_data_id`),
  UNIQUE KEY `uk_sys_dict_data_type_value` (`dict_type_code`, `dict_value`),
  KEY `idx_sys_dict_data_dict_type_code` (`dict_type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='字典数据表';

CREATE TABLE `sys_system_param` (
  `param_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '参数主键。',
  `param_key` VARCHAR(64) NULL COMMENT '参数键，唯一。',
  `param_value` VARCHAR(500) NULL COMMENT '参数值。',
  `param_name` VARCHAR(128) NULL COMMENT '参数名称。',
  `module_code` VARCHAR(64) NULL COMMENT '所属模块。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`param_id`),
  UNIQUE KEY `uk_sys_system_param_param_key` (`param_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统参数表';

CREATE TABLE `sys_file` (
  `file_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '文件主键。',
  `file_name` VARCHAR(128) NULL COMMENT '原始文件名。',
  `file_url` VARCHAR(255) NULL COMMENT '文件访问地址。',
  `file_type` VARCHAR(32) NULL COMMENT '文件类型。',
  `biz_type` VARCHAR(32) NULL COMMENT '业务类型。',
  `biz_id` BIGINT UNSIGNED NULL COMMENT '关联业务 ID。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`file_id`),
  KEY `idx_sys_file_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='附件文件表';

CREATE TABLE `sys_operation_log` (
  `log_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志主键。',
  `user_id` BIGINT UNSIGNED NULL COMMENT '操作用户。',
  `operation_type` VARCHAR(32) NULL COMMENT '操作类型。',
  `module_code` VARCHAR(64) NULL COMMENT '模块编码。',
  `biz_id` BIGINT UNSIGNED NULL COMMENT '业务 ID。',
  `request_ip` VARCHAR(64) NULL COMMENT '请求 IP。',
  `operation_time` DATETIME NULL COMMENT '操作时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`log_id`),
  KEY `idx_sys_operation_log_user_id` (`user_id`),
  KEY `idx_sys_operation_log_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

CREATE TABLE `sys_status_change_log` (
  `log_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志主键。',
  `biz_type` VARCHAR(32) NULL COMMENT '业务类型。',
  `biz_id` BIGINT UNSIGNED NULL COMMENT '业务 ID。',
  `old_status` VARCHAR(32) NULL COMMENT '原状态。',
  `new_status` VARCHAR(32) NULL COMMENT '新状态。',
  `change_reason` VARCHAR(255) NULL COMMENT '变更原因。',
  `changed_by` BIGINT UNSIGNED NULL COMMENT '变更人。',
  `changed_time` DATETIME NULL COMMENT '变更时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`log_id`),
  KEY `idx_sys_status_change_log_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='状态变更日志表';

CREATE TABLE `sys_approval_record` (
  `approval_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '审批记录主键。',
  `biz_type` VARCHAR(32) NULL COMMENT '业务类型。',
  `biz_id` BIGINT UNSIGNED NULL COMMENT '业务 ID。',
  `approval_node` VARCHAR(64) NULL COMMENT '审批节点。',
  `approver_id` BIGINT UNSIGNED NULL COMMENT '审批人。',
  `approval_result` VARCHAR(32) NULL COMMENT 'APPROVED:同意，REJECTED:驳回或退回。',
  `approval_comment` VARCHAR(500) NULL COMMENT '审批意见。',
  `approval_time` DATETIME NULL COMMENT '审批时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`approval_id`),
  KEY `idx_sys_approval_record_biz_id` (`biz_id`),
  KEY `idx_sys_approval_record_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='审批记录表';

CREATE TABLE `org_department` (
  `department_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '部门主键。',
  `parent_id` BIGINT UNSIGNED NULL COMMENT '上级部门。',
  `department_code` VARCHAR(64) NULL COMMENT '部门编码。',
  `department_name` VARCHAR(128) NULL COMMENT '部门名称。',
  `manager_id` BIGINT UNSIGNED NULL COMMENT '负责人。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`department_id`),
  UNIQUE KEY `uk_org_department_department_code` (`department_code`),
  KEY `idx_org_department_parent_id` (`parent_id`),
  KEY `idx_org_department_manager_id` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表';

CREATE TABLE `org_employee` (
  `employee_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '员工主键。',
  `employee_no` VARCHAR(64) NULL COMMENT '工号，唯一。',
  `employee_name` VARCHAR(128) NULL COMMENT '姓名。',
  `department_id` BIGINT UNSIGNED NULL COMMENT '所属部门。',
  `position_name` VARCHAR(128) NULL COMMENT '岗位名称。',
  `skill_level` VARCHAR(64) NULL COMMENT '技能等级。',
  `phone` VARCHAR(64) NULL COMMENT '联系方式。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:在职，RESIGNED:离职，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`employee_id`),
  UNIQUE KEY `uk_org_employee_employee_no` (`employee_no`),
  KEY `idx_org_employee_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工表';

CREATE TABLE `org_team` (
  `team_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '班组主键。',
  `team_code` VARCHAR(64) NULL COMMENT '班组编码。',
  `team_name` VARCHAR(128) NULL COMMENT '班组名称。',
  `leader_id` BIGINT UNSIGNED NULL COMMENT '班组长。',
  `department_id` BIGINT UNSIGNED NULL COMMENT '所属部门。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`team_id`),
  UNIQUE KEY `uk_org_team_team_code` (`team_code`),
  KEY `idx_org_team_leader_id` (`leader_id`),
  KEY `idx_org_team_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='班组表';

CREATE TABLE `org_team_member` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键。',
  `team_id` BIGINT UNSIGNED NULL COMMENT '班组 ID。',
  `employee_id` BIGINT UNSIGNED NULL COMMENT '员工 ID。',
  `join_date` DATE NULL COMMENT '加入日期。',
  `leave_date` DATE NULL COMMENT '离开日期。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:有效，DISABLE:无效。',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_org_team_member_team_id` (`team_id`),
  KEY `idx_org_team_member_employee_id` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='班组成员表';

CREATE TABLE `org_shift` (
  `shift_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '班次主键。',
  `shift_code` VARCHAR(64) NULL COMMENT '班次编码。',
  `shift_name` VARCHAR(128) NULL COMMENT '班次名称。',
  `start_time` DATETIME NULL COMMENT '开始时间。',
  `end_time` DATETIME NULL COMMENT '结束时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`shift_id`),
  UNIQUE KEY `uk_org_shift_shift_code` (`shift_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='班次表';

CREATE TABLE `mdm_unit` (
  `unit_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '单位主键。',
  `unit_code` VARCHAR(64) NULL COMMENT '单位编码。',
  `unit_name` VARCHAR(128) NULL COMMENT '单位名称。',
  `precision_scale` INT NULL COMMENT '小数精度。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`unit_id`),
  UNIQUE KEY `uk_mdm_unit_unit_code` (`unit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='计量单位表';

CREATE TABLE `mdm_supplier` (
  `supplier_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '供应商主键。',
  `supplier_code` VARCHAR(64) NULL COMMENT '供应商编码。',
  `supplier_name` VARCHAR(128) NULL COMMENT '供应商名称。',
  `contact_name` VARCHAR(128) NULL COMMENT '联系人。',
  `contact_phone` VARCHAR(64) NULL COMMENT '联系电话。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:合作中，DISABLE:停止合作。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`supplier_id`),
  UNIQUE KEY `uk_mdm_supplier_supplier_code` (`supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供应商表';

CREATE TABLE `mdm_customer` (
  `customer_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '客户主键。',
  `customer_code` VARCHAR(64) NULL COMMENT '客户编码。',
  `customer_name` VARCHAR(128) NULL COMMENT '客户名称。',
  `contact_name` VARCHAR(128) NULL COMMENT '联系人。',
  `contact_phone` VARCHAR(64) NULL COMMENT '联系电话。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`customer_id`),
  UNIQUE KEY `uk_mdm_customer_customer_code` (`customer_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户表';

CREATE TABLE `mdm_material_category` (
  `category_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类主键。',
  `parent_id` BIGINT UNSIGNED NULL COMMENT '上级分类。',
  `category_code` VARCHAR(64) NULL COMMENT '分类编码。',
  `category_name` VARCHAR(128) NULL COMMENT '分类名称。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_mdm_material_category_category_code` (`category_code`),
  KEY `idx_mdm_material_category_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物料分类表';

CREATE TABLE `mdm_material` (
  `material_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '物料主键。',
  `material_code` VARCHAR(64) NULL COMMENT '物料编码，唯一。',
  `material_name` VARCHAR(128) NULL COMMENT '物料名称。',
  `category_id` BIGINT UNSIGNED NULL COMMENT '物料分类。',
  `spec_model` VARCHAR(64) NULL COMMENT '规格型号。',
  `unit_id` BIGINT UNSIGNED NULL COMMENT '计量单位。',
  `material_type` VARCHAR(32) NULL COMMENT '原料、半成品、成品、辅料。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:正常，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`material_id`),
  UNIQUE KEY `uk_mdm_material_material_code` (`material_code`),
  KEY `idx_mdm_material_category_id` (`category_id`),
  KEY `idx_mdm_material_unit_id` (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物料档案表';

CREATE TABLE `mdm_product` (
  `product_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '产品主键。',
  `product_code` VARCHAR(64) NULL COMMENT '产品编码。',
  `product_name` VARCHAR(128) NULL COMMENT '产品名称。',
  `product_model` VARCHAR(64) NULL COMMENT '产品型号。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '对应成品物料 ID。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`product_id`),
  UNIQUE KEY `uk_mdm_product_product_code` (`product_code`),
  KEY `idx_mdm_product_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产品档案表';

CREATE TABLE `mdm_bom` (
  `bom_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '产品物料清单主键。',
  `bom_code` VARCHAR(64) NULL COMMENT '产品物料清单编码。',
  `product_id` BIGINT UNSIGNED NULL COMMENT '关联产品。',
  `bom_version` VARCHAR(64) NULL COMMENT '版本号。',
  `effective_date` DATE NULL COMMENT '生效日期。',
  `status` VARCHAR(32) NULL COMMENT 'DRAFT:草稿，RELEASED:已发布，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`bom_id`),
  UNIQUE KEY `uk_mdm_bom_bom_code` (`bom_code`),
  KEY `idx_mdm_bom_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产品物料清单主表';

CREATE TABLE `mdm_bom_item` (
  `bom_item_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '产品物料清单明细主键。',
  `bom_id` BIGINT UNSIGNED NULL COMMENT '产品物料清单主表 ID。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '所需物料。',
  `quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '标准用量。',
  `loss_rate` DECIMAL(9,4) NOT NULL DEFAULT 0.0000 COMMENT '损耗率。',
  `sequence_no` INT NULL COMMENT '顺序号。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`bom_item_id`),
  KEY `idx_mdm_bom_item_bom_id` (`bom_id`),
  KEY `idx_mdm_bom_item_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产品物料清单明细表';

CREATE TABLE `process_operation` (
  `operation_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '工序主键。',
  `operation_code` VARCHAR(64) NULL COMMENT '工序编码。',
  `operation_name` VARCHAR(128) NULL COMMENT '工序名称。',
  `operation_type` VARCHAR(32) NULL COMMENT '生产、检验、包装。',
  `standard_time` DATETIME NULL COMMENT '标准工时。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`operation_id`),
  UNIQUE KEY `uk_process_operation_operation_code` (`operation_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工序表';

CREATE TABLE `process_route` (
  `route_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '工艺路线主键。',
  `route_code` VARCHAR(64) NULL COMMENT '路线编码。',
  `route_name` VARCHAR(128) NULL COMMENT '路线名称。',
  `product_id` BIGINT UNSIGNED NULL COMMENT '关联产品。',
  `version` VARCHAR(64) NULL COMMENT '版本号。',
  `status` VARCHAR(32) NULL COMMENT 'DRAFT:草稿，RELEASED:已发布，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`route_id`),
  UNIQUE KEY `uk_process_route_route_code` (`route_code`),
  KEY `idx_process_route_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工艺路线表';

CREATE TABLE `process_route_item` (
  `route_item_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细主键。',
  `route_id` BIGINT UNSIGNED NULL COMMENT '工艺路线 ID。',
  `operation_id` BIGINT UNSIGNED NULL COMMENT '工序 ID。',
  `sequence_no` INT NULL COMMENT '工序顺序。',
  `standard_time` DATETIME NULL COMMENT '标准工时。',
  `qc_required` TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要质检。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`route_item_id`),
  KEY `idx_process_route_item_route_id` (`route_id`),
  KEY `idx_process_route_item_operation_id` (`operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工艺路线明细表';

CREATE TABLE `process_parameter` (
  `parameter_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '参数主键。',
  `operation_id` BIGINT UNSIGNED NULL COMMENT '工序 ID。',
  `parameter_name` VARCHAR(128) NULL COMMENT '参数名称。',
  `standard_value` VARCHAR(64) NULL COMMENT '标准值。',
  `upper_limit` VARCHAR(64) NULL COMMENT '上限。',
  `lower_limit` VARCHAR(64) NULL COMMENT '下限。',
  `unit_id` BIGINT UNSIGNED NULL COMMENT '单位。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`parameter_id`),
  KEY `idx_process_parameter_operation_id` (`operation_id`),
  KEY `idx_process_parameter_unit_id` (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工艺参数表';

CREATE TABLE `process_work_instruction` (
  `instruction_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '指导书主键。',
  `operation_id` BIGINT UNSIGNED NULL COMMENT '工序 ID。',
  `product_id` BIGINT UNSIGNED NULL COMMENT '产品 ID。',
  `version` VARCHAR(64) NULL COMMENT '版本号。',
  `title` VARCHAR(128) NULL COMMENT '指导书标题。',
  `file_id` BIGINT UNSIGNED NULL COMMENT '附件文件 ID。',
  `effective_time` DATETIME NULL COMMENT '生效时间。',
  `status` VARCHAR(32) NULL COMMENT 'DRAFT:草稿，RELEASED:已发布，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`instruction_id`),
  KEY `idx_process_work_instruction_operation_id` (`operation_id`),
  KEY `idx_process_work_instruction_product_id` (`product_id`),
  KEY `idx_process_work_instruction_file_id` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='作业指导书表';

CREATE TABLE `factory_workshop` (
  `workshop_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '车间主键。',
  `workshop_code` VARCHAR(64) NULL COMMENT '车间编码。',
  `workshop_name` VARCHAR(128) NULL COMMENT '车间名称。',
  `manager_id` BIGINT UNSIGNED NULL COMMENT '负责人。',
  `status` VARCHAR(32) NULL COMMENT 'RUNNING:运行，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`workshop_id`),
  UNIQUE KEY `uk_factory_workshop_workshop_code` (`workshop_code`),
  KEY `idx_factory_workshop_manager_id` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='车间表';

CREATE TABLE `factory_line` (
  `line_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '产线主键。',
  `line_code` VARCHAR(64) NULL COMMENT '产线编码。',
  `line_name` VARCHAR(128) NULL COMMENT '产线名称。',
  `workshop_id` BIGINT UNSIGNED NULL COMMENT '所属车间。',
  `daily_capacity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '日产能。',
  `status` VARCHAR(32) NULL COMMENT 'RUNNING:运行，STOPPED:停线，MAINTAINING:维护。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`line_id`),
  UNIQUE KEY `uk_factory_line_line_code` (`line_code`),
  KEY `idx_factory_line_workshop_id` (`workshop_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产线表';

CREATE TABLE `factory_station` (
  `station_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '工位主键。',
  `station_code` VARCHAR(64) NULL COMMENT '工位编码。',
  `station_name` VARCHAR(128) NULL COMMENT '工位名称。',
  `line_id` BIGINT UNSIGNED NULL COMMENT '所属产线。',
  `station_type` VARCHAR(32) NULL COMMENT '生产、检验、包装。',
  `status` VARCHAR(32) NULL COMMENT 'RUNNING:运行，DISABLE:停用，MAINTAINING:维护。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`station_id`),
  UNIQUE KEY `uk_factory_station_station_code` (`station_code`),
  KEY `idx_factory_station_line_id` (`line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工位表';

CREATE TABLE `factory_station_operation` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键。',
  `station_id` BIGINT UNSIGNED NULL COMMENT '工位 ID。',
  `operation_id` BIGINT UNSIGNED NULL COMMENT '工序 ID。',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认工位。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:有效，DISABLE:无效。',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_factory_station_operation` (`station_id`, `operation_id`),
  KEY `idx_factory_station_operation_station_id` (`station_id`),
  KEY `idx_factory_station_operation_operation_id` (`operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工位工序关联表';

CREATE TABLE `mfg_production_order` (
  `production_order_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '生产订单主键。',
  `production_order_no` VARCHAR(64) NULL COMMENT '生产订单编号。',
  `customer_id` BIGINT UNSIGNED NULL COMMENT '客户 ID。',
  `product_id` BIGINT UNSIGNED NULL COMMENT '产品 ID。',
  `order_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '订单数量。',
  `delivery_date` DATE NULL COMMENT '交付日期。',
  `source_type` VARCHAR(32) NULL COMMENT 'ERP 同步、手工创建。',
  `status` VARCHAR(32) NULL COMMENT 'DRAFT:新建，RELEASED:已排产，RUNNING:生产中，FINISHED:已完成，CLOSED:已关闭。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`production_order_id`),
  UNIQUE KEY `uk_mfg_production_order_production_order_no` (`production_order_no`),
  KEY `idx_mfg_production_order_customer_id` (`customer_id`),
  KEY `idx_mfg_production_order_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='生产订单表';

CREATE TABLE `mfg_production_plan` (
  `plan_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '计划主键。',
  `plan_no` VARCHAR(64) NULL COMMENT '计划编号。',
  `production_order_id` BIGINT UNSIGNED NULL COMMENT '关联生产订单。',
  `line_id` BIGINT UNSIGNED NULL COMMENT '计划产线。',
  `planned_start_time` DATETIME NULL COMMENT '计划开始时间。',
  `planned_end_time` DATETIME NULL COMMENT '计划结束时间。',
  `planned_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '计划数量。',
  `status` VARCHAR(32) NULL COMMENT 'DRAFT:待生效，RELEASED:已生效，FINISHED:已完成，CLOSED:已取消。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`plan_id`),
  UNIQUE KEY `uk_mfg_production_plan_plan_no` (`plan_no`),
  KEY `idx_mfg_production_plan_production_order_id` (`production_order_id`),
  KEY `idx_mfg_production_plan_line_id` (`line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='生产计划表';

CREATE TABLE `mfg_work_order` (
  `work_order_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '工单主键。',
  `work_order_no` VARCHAR(64) NULL COMMENT '工单编号，唯一。',
  `plan_id` BIGINT UNSIGNED NULL COMMENT '生产计划 ID。',
  `product_id` BIGINT UNSIGNED NULL COMMENT '产品 ID。',
  `bom_id` BIGINT UNSIGNED NULL COMMENT '使用的产品物料清单。',
  `route_id` BIGINT UNSIGNED NULL COMMENT '使用的工艺路线。',
  `planned_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '计划生产数量。',
  `completed_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '完成数量。',
  `status` VARCHAR(32) NULL COMMENT 'DRAFT:新建，APPROVED:已审核，RELEASED:已下达，RUNNING:生产中，FINISHED:已完成，CLOSED:已关闭。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`work_order_id`),
  UNIQUE KEY `uk_mfg_work_order_work_order_no` (`work_order_no`),
  KEY `idx_mfg_work_order_plan_id` (`plan_id`),
  KEY `idx_mfg_work_order_product_id` (`product_id`),
  KEY `idx_mfg_work_order_bom_id` (`bom_id`),
  KEY `idx_mfg_work_order_route_id` (`route_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='生产工单表';

CREATE TABLE `mfg_work_order_material` (
  `requirement_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '需求主键。',
  `work_order_id` BIGINT UNSIGNED NULL COMMENT '工单 ID。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `required_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '需求数量。',
  `issued_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '已发料数量。',
  `shortage_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '缺料数量。',
  `status` VARCHAR(32) NULL COMMENT 'WAIT_READY:待齐套，PARTIAL_READY:部分齐套，READY:已齐套。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`requirement_id`),
  KEY `idx_mfg_work_order_material_work_order_id` (`work_order_id`),
  KEY `idx_mfg_work_order_material_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工单用料需求表';

CREATE TABLE `mfg_dispatch_order` (
  `dispatch_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '派工单主键。',
  `dispatch_no` VARCHAR(64) NULL COMMENT '派工单编号。',
  `work_order_id` BIGINT UNSIGNED NULL COMMENT '工单 ID。',
  `team_id` BIGINT UNSIGNED NULL COMMENT '派工班组。',
  `planned_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '派工数量。',
  `due_time` DATETIME NULL COMMENT '要求完成时间。',
  `material_ready_status` VARCHAR(32) NULL COMMENT 'WAIT_READY:待齐套，PARTIAL_READY:部分齐套，READY:已齐套。',
  `status` VARCHAR(32) NULL COMMENT 'WAIT_DISPATCH:待派发，RELEASED:已派发，ACCEPTED:已接受，RUNNING:生产中，FINISHED:已完成，REJECTED:已驳回。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`dispatch_id`),
  UNIQUE KEY `uk_mfg_dispatch_order_dispatch_no` (`dispatch_no`),
  KEY `idx_mfg_dispatch_order_work_order_id` (`work_order_id`),
  KEY `idx_mfg_dispatch_order_team_id` (`team_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='派工单表';

CREATE TABLE `mfg_task` (
  `task_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务主键。',
  `task_no` VARCHAR(64) NULL COMMENT '任务编号。',
  `dispatch_id` BIGINT UNSIGNED NULL COMMENT '派工单 ID。',
  `work_order_id` BIGINT UNSIGNED NULL COMMENT '工单 ID。',
  `operation_id` BIGINT UNSIGNED NULL COMMENT '工序 ID。',
  `station_id` BIGINT UNSIGNED NULL COMMENT '工位 ID。',
  `planned_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '计划数量。',
  `completed_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '完成数量。',
  `defect_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '不良数量。',
  `status` VARCHAR(32) NULL COMMENT 'WAIT_START:待开工，RUNNING:生产中，PAUSED:暂停，FINISHED:已完工，CLOSED:已关闭。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_mfg_task_task_no` (`task_no`),
  KEY `idx_mfg_task_dispatch_id` (`dispatch_id`),
  KEY `idx_mfg_task_work_order_id` (`work_order_id`),
  KEY `idx_mfg_task_operation_id` (`operation_id`),
  KEY `idx_mfg_task_station_id` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='生产任务单表';

CREATE TABLE `mfg_task_operator` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键。',
  `task_id` BIGINT UNSIGNED NULL COMMENT '任务 ID。',
  `employee_id` BIGINT UNSIGNED NULL COMMENT '员工 ID。',
  `role_in_task` VARCHAR(32) NULL COMMENT '任务角色，如主操作、协助。',
  `assigned_time` DATETIME NULL COMMENT '分配时间。',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_mfg_task_operator_task_id` (`task_id`),
  KEY `idx_mfg_task_operator_employee_id` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='任务人员分配表';

CREATE TABLE `mfg_work_report` (
  `report_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '报工主键。',
  `report_no` VARCHAR(64) NULL COMMENT '报工编号。',
  `task_id` BIGINT UNSIGNED NULL COMMENT '任务 ID。',
  `operator_id` BIGINT UNSIGNED NULL COMMENT '操作工 ID。',
  `station_id` BIGINT UNSIGNED NULL COMMENT '工位 ID。',
  `qualified_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '合格数量。',
  `defect_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '不良数量。',
  `actual_work_hours` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '实际工时。',
  `report_time` DATETIME NULL COMMENT '报工时间。',
  `audit_status` VARCHAR(32) NULL COMMENT 'SUBMITTED:待审核，APPROVED:通过，REJECTED:驳回。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`report_id`),
  UNIQUE KEY `uk_mfg_work_report_report_no` (`report_no`),
  KEY `idx_mfg_work_report_task_id` (`task_id`),
  KEY `idx_mfg_work_report_operator_id` (`operator_id`),
  KEY `idx_mfg_work_report_station_id` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='生产报工表';

CREATE TABLE `mfg_report_material` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键。',
  `report_id` BIGINT UNSIGNED NULL COMMENT '报工 ID。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `batch_id` BIGINT UNSIGNED NULL COMMENT '物料批次 ID。',
  `used_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '消耗数量。',
  `scrap_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '损耗数量。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_mfg_report_material_report_id` (`report_id`),
  KEY `idx_mfg_report_material_material_id` (`material_id`),
  KEY `idx_mfg_report_material_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报工用料消耗表';

CREATE TABLE `mfg_exception` (
  `exception_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '异常主键。',
  `exception_no` VARCHAR(64) NULL COMMENT '异常编号。',
  `exception_type` VARCHAR(32) NULL COMMENT '异常类型。',
  `source_alert_id` BIGINT UNSIGNED NULL COMMENT '来源安灯告警 ID（现场安灯升级为业务异常时关联，可空）。',
  `task_id` BIGINT UNSIGNED NULL COMMENT '关联任务。',
  `station_id` BIGINT UNSIGNED NULL COMMENT '关联工位。',
  `reported_by` BIGINT UNSIGNED NULL COMMENT '上报人。',
  `description` VARCHAR(500) NULL COMMENT '异常描述。',
  `handle_result` VARCHAR(255) NULL COMMENT '处理结果。',
  `status` VARCHAR(32) NULL COMMENT 'SUBMITTED:待处理，PROCESSING:处理中，RESOLVED:已解决，CLOSED:已关闭。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`exception_id`),
  UNIQUE KEY `uk_mfg_exception_exception_no` (`exception_no`),
  KEY `idx_mfg_exception_source_alert_id` (`source_alert_id`),
  KEY `idx_mfg_exception_task_id` (`task_id`),
  KEY `idx_mfg_exception_station_id` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='生产异常表';

CREATE TABLE `mfg_andon_alert` (
  `alert_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '告警主键。',
  `alert_no` VARCHAR(64) NULL COMMENT '告警编号。',
  `line_id` BIGINT UNSIGNED NULL COMMENT '产线 ID。',
  `station_id` BIGINT UNSIGNED NULL COMMENT '工位 ID。',
  `triggered_by` BIGINT UNSIGNED NULL COMMENT '触发人。',
  `alert_type` VARCHAR(32) NULL COMMENT '缺料、设备、质量、安全、其他。',
  `trigger_time` DATETIME NULL COMMENT '触发时间。',
  `response_time` DATETIME NULL COMMENT '响应时间。',
  `resolved_time` DATETIME NULL COMMENT '解除时间。',
  `status` VARCHAR(32) NULL COMMENT 'ACTIVE:告警中，PROCESSING:处理中，CLOSED:已解除。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`alert_id`),
  UNIQUE KEY `uk_mfg_andon_alert_alert_no` (`alert_no`),
  KEY `idx_mfg_andon_alert_line_id` (`line_id`),
  KEY `idx_mfg_andon_alert_station_id` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='安灯告警表';

CREATE TABLE `wms_warehouse` (
  `warehouse_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '仓库主键。',
  `warehouse_code` VARCHAR(64) NULL COMMENT '仓库编码。',
  `warehouse_name` VARCHAR(128) NULL COMMENT '仓库名称。',
  `warehouse_type` VARCHAR(32) NULL COMMENT '原料、半成品、成品、不良品。',
  `manager_id` BIGINT UNSIGNED NULL COMMENT '负责人。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`warehouse_id`),
  UNIQUE KEY `uk_wms_warehouse_warehouse_code` (`warehouse_code`),
  KEY `idx_wms_warehouse_manager_id` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='仓库表';

CREATE TABLE `wms_location` (
  `location_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库位主键。',
  `location_code` VARCHAR(64) NULL COMMENT '库位编码。',
  `location_name` VARCHAR(128) NULL COMMENT '库位名称。',
  `warehouse_id` BIGINT UNSIGNED NULL COMMENT '所属仓库。',
  `location_type` VARCHAR(32) NULL COMMENT '正常、待检、不良、冻结。',
  `status` VARCHAR(32) NULL COMMENT 'IDLE:空闲，OCCUPIED:占用，LOCKED:锁定，MAINTAINING:维护。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`location_id`),
  UNIQUE KEY `uk_wms_location_location_code` (`location_code`),
  KEY `idx_wms_location_warehouse_id` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库位表';

CREATE TABLE `wms_material_batch` (
  `batch_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '批次主键。',
  `batch_no` VARCHAR(64) NULL COMMENT '批次号，唯一。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `supplier_id` BIGINT UNSIGNED NULL COMMENT '供应商 ID。',
  `supplier_batch_no` VARCHAR(64) NULL COMMENT '供应商批次号。',
  `production_date` DATE NULL COMMENT '生产日期。',
  `received_date` DATE NULL COMMENT '入库日期。',
  `quality_status` VARCHAR(32) NULL COMMENT 'WAIT_INSPECTION:待检，PASS:合格，FAIL:不合格，CONCESSION:让步接收。',
  `status` VARCHAR(32) NULL COMMENT 'AVAILABLE:可用，FROZEN:冻结，EXHAUSTED:耗尽，SCRAPPED:报废。',
  `source_order_type` VARCHAR(32) NULL COMMENT '来源单据类型，用于区分采购入库、生产入库等批次来源',
  `source_order_id` BIGINT UNSIGNED NULL COMMENT '来源单据 ID',
  `source_product_lot_id` BIGINT UNSIGNED NULL COMMENT '来源产品批次 ID，成品入库时关联生产批次',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`batch_id`),
  UNIQUE KEY `uk_wms_material_batch_batch_no` (`batch_no`),
  KEY `idx_wms_material_batch_material_id` (`material_id`),
  KEY `idx_wms_material_batch_supplier_id` (`supplier_id`),
  KEY `idx_wms_material_batch_source_order_id` (`source_order_id`),
  KEY `idx_wms_material_batch_source_product_lot_id` (`source_product_lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物料批次表';

CREATE TABLE `wms_inventory` (
  `inventory_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '库存主键。',
  `warehouse_id` BIGINT UNSIGNED NULL COMMENT '仓库 ID。',
  `location_id` BIGINT UNSIGNED NULL COMMENT '库位 ID。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `batch_id` BIGINT UNSIGNED NULL COMMENT '批次 ID。',
  `available_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '可用数量。',
  `locked_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '锁定数量。',
  `inspection_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '待检数量。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`inventory_id`),
  UNIQUE KEY `uk_wms_inventory_whlmb` (`warehouse_id`, `location_id`, `material_id`, `batch_id`),
  KEY `idx_wms_inventory_warehouse_id` (`warehouse_id`),
  KEY `idx_wms_inventory_location_id` (`location_id`),
  KEY `idx_wms_inventory_material_id` (`material_id`),
  KEY `idx_wms_inventory_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存余额表';

CREATE TABLE `wms_stock_movement` (
  `movement_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水主键。',
  `movement_no` VARCHAR(64) NULL COMMENT '流水编号。',
  `movement_type` VARCHAR(32) NULL COMMENT '入库、出库、退料、盘点、调整。',
  `source_order_type` VARCHAR(32) NULL COMMENT '来源单据类型。',
  `source_order_id` BIGINT UNSIGNED NULL COMMENT '来源单据 ID。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `batch_id` BIGINT UNSIGNED NULL COMMENT '批次 ID。',
  `quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '变动数量。',
  `direction` VARCHAR(32) NULL COMMENT '入、出。',
  `operation_time` DATETIME NULL COMMENT '操作时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`movement_id`),
  UNIQUE KEY `uk_wms_stock_movement_movement_no` (`movement_no`),
  KEY `idx_wms_stock_movement_source_order_id` (`source_order_id`),
  KEY `idx_wms_stock_movement_material_id` (`material_id`),
  KEY `idx_wms_stock_movement_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存流水表';

CREATE TABLE `wms_inbound_order` (
  `inbound_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '入库单主键。',
  `inbound_no` VARCHAR(64) NULL COMMENT '入库单号。',
  `inbound_type` VARCHAR(32) NULL COMMENT '采购入库、生产入库、退料入库。',
  `warehouse_id` BIGINT UNSIGNED NULL COMMENT '入库仓库。',
  `source_order_type` VARCHAR(32) NULL COMMENT '来源单据类型。',
  `source_order_id` BIGINT UNSIGNED NULL COMMENT '来源单据 ID。',
  `audit_status` VARCHAR(32) NULL COMMENT 'SUBMITTED:待审核，APPROVED:已审核，REJECTED:已驳回。',
  `inbound_time` DATETIME NULL COMMENT '入库时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`inbound_id`),
  UNIQUE KEY `uk_wms_inbound_order_inbound_no` (`inbound_no`),
  KEY `idx_wms_inbound_order_warehouse_id` (`warehouse_id`),
  KEY `idx_wms_inbound_order_source_order_id` (`source_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='入库单主表';

CREATE TABLE `wms_inbound_detail` (
  `detail_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细主键。',
  `inbound_id` BIGINT UNSIGNED NULL COMMENT '入库单 ID。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `batch_id` BIGINT UNSIGNED NULL COMMENT '批次 ID。',
  `location_id` BIGINT UNSIGNED NULL COMMENT '库位 ID。',
  `inbound_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '入库数量。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`detail_id`),
  KEY `idx_wms_inbound_detail_inbound_id` (`inbound_id`),
  KEY `idx_wms_inbound_detail_material_id` (`material_id`),
  KEY `idx_wms_inbound_detail_batch_id` (`batch_id`),
  KEY `idx_wms_inbound_detail_location_id` (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='入库单明细表';

CREATE TABLE `wms_outbound_order` (
  `outbound_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '出库单主键。',
  `outbound_no` VARCHAR(64) NULL COMMENT '出库单号。',
  `outbound_type` VARCHAR(32) NULL COMMENT '生产领料、销售出库、调拨出库。',
  `warehouse_id` BIGINT UNSIGNED NULL COMMENT '出库仓库。',
  `work_order_id` BIGINT UNSIGNED NULL COMMENT '关联工单。',
  `applicant_id` BIGINT UNSIGNED NULL COMMENT '申请人。',
  `audit_status` VARCHAR(32) NULL COMMENT 'SUBMITTED:待审核，APPROVED:已审核，REJECTED:已驳回。',
  `outbound_time` DATETIME NULL COMMENT '出库时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`outbound_id`),
  UNIQUE KEY `uk_wms_outbound_order_outbound_no` (`outbound_no`),
  KEY `idx_wms_outbound_order_warehouse_id` (`warehouse_id`),
  KEY `idx_wms_outbound_order_work_order_id` (`work_order_id`),
  KEY `idx_wms_outbound_order_applicant_id` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出库单主表';

CREATE TABLE `wms_outbound_detail` (
  `detail_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细主键。',
  `outbound_id` BIGINT UNSIGNED NULL COMMENT '出库单 ID。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `batch_id` BIGINT UNSIGNED NULL COMMENT '批次 ID。',
  `location_id` BIGINT UNSIGNED NULL COMMENT '库位 ID。',
  `outbound_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '出库数量。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`detail_id`),
  KEY `idx_wms_outbound_detail_outbound_id` (`outbound_id`),
  KEY `idx_wms_outbound_detail_material_id` (`material_id`),
  KEY `idx_wms_outbound_detail_batch_id` (`batch_id`),
  KEY `idx_wms_outbound_detail_location_id` (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出库单明细表';

CREATE TABLE `wms_return_order` (
  `return_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '退料单主键。',
  `return_no` VARCHAR(64) NULL COMMENT '退料单号。',
  `return_type` VARCHAR(32) NULL COMMENT '良品退料、不良退料、成品退库。',
  `work_order_id` BIGINT UNSIGNED NULL COMMENT '关联工单。',
  `applicant_id` BIGINT UNSIGNED NULL COMMENT '申请人。',
  `audit_status` VARCHAR(32) NULL COMMENT 'SUBMITTED:待审核，APPROVED:已审核，REJECTED:已驳回。',
  `return_time` DATETIME NULL COMMENT '退库时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`return_id`),
  UNIQUE KEY `uk_wms_return_order_return_no` (`return_no`),
  KEY `idx_wms_return_order_work_order_id` (`work_order_id`),
  KEY `idx_wms_return_order_applicant_id` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退料/退库单主表';

CREATE TABLE `wms_return_detail` (
  `detail_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细主键。',
  `return_id` BIGINT UNSIGNED NULL COMMENT '退料单 ID。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `batch_id` BIGINT UNSIGNED NULL COMMENT '批次 ID。',
  `return_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '退回数量。',
  `return_reason` VARCHAR(255) NULL COMMENT '退回原因。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`detail_id`),
  KEY `idx_wms_return_detail_return_id` (`return_id`),
  KEY `idx_wms_return_detail_material_id` (`material_id`),
  KEY `idx_wms_return_detail_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='退料/退库明细表';

CREATE TABLE `wms_stocktake_order` (
  `stocktake_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '盘点单主键。',
  `stocktake_no` VARCHAR(64) NULL COMMENT '盘点单号。',
  `warehouse_id` BIGINT UNSIGNED NULL COMMENT '盘点仓库。',
  `stocktake_type` VARCHAR(32) NULL COMMENT '全盘、抽盘、循环盘点。',
  `stocktake_status` VARCHAR(32) NULL COMMENT 'WAIT_START:待盘，RUNNING:盘点中，FINISHED:已完成，CLOSED:已关闭。',
  `started_time` DATETIME NULL COMMENT '开始时间。',
  `finished_time` DATETIME NULL COMMENT '完成时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`stocktake_id`),
  UNIQUE KEY `uk_wms_stocktake_order_stocktake_no` (`stocktake_no`),
  KEY `idx_wms_stocktake_order_warehouse_id` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='盘点单主表';

CREATE TABLE `wms_stocktake_detail` (
  `detail_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细主键。',
  `stocktake_id` BIGINT UNSIGNED NULL COMMENT '盘点单 ID。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `batch_id` BIGINT UNSIGNED NULL COMMENT '批次 ID。',
  `location_id` BIGINT UNSIGNED NULL COMMENT '库位 ID。',
  `book_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '账面数量。',
  `actual_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '实盘数量。',
  `difference_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '差异数量。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`detail_id`),
  KEY `idx_wms_stocktake_detail_stocktake_id` (`stocktake_id`),
  KEY `idx_wms_stocktake_detail_material_id` (`material_id`),
  KEY `idx_wms_stocktake_detail_batch_id` (`batch_id`),
  KEY `idx_wms_stocktake_detail_location_id` (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='盘点单明细表';

CREATE TABLE `wms_replenishment_suggestion` (
  `suggestion_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '建议主键。',
  `suggestion_no` VARCHAR(64) NULL COMMENT '建议编号。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `required_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '需求数量。',
  `available_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '当前可用库存。',
  `suggested_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '建议补货数量。',
  `source_type` VARCHAR(32) NULL COMMENT '库存预警、工单需求、人工发起。',
  `status` VARCHAR(32) NULL COMMENT 'SUBMITTED:待处理，APPROVED:已确认，CLOSED:已关闭。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`suggestion_id`),
  UNIQUE KEY `uk_wms_replenishment_suggestion_suggestion_no` (`suggestion_no`),
  KEY `idx_wms_replenishment_suggestion_material_id` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='补货建议表';

CREATE TABLE `qms_inspection_item` (
  `item_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '检验项目主键。',
  `item_code` VARCHAR(64) NULL COMMENT '项目编码。',
  `item_name` VARCHAR(128) NULL COMMENT '项目名称。',
  `item_type` VARCHAR(32) NULL COMMENT '尺寸、性能、外观、重量。',
  `inspection_method` VARCHAR(255) NULL COMMENT '人工、仪器、目检。',
  `unit_id` BIGINT UNSIGNED NULL COMMENT '单位。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_qms_inspection_item_item_code` (`item_code`),
  KEY `idx_qms_inspection_item_unit_id` (`unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='检验项目表';

CREATE TABLE `qms_inspection_standard` (
  `standard_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '标准主键。',
  `item_id` BIGINT UNSIGNED NULL COMMENT '检验项目 ID。',
  `product_id` BIGINT UNSIGNED NULL COMMENT '产品 ID。',
  `operation_id` BIGINT UNSIGNED NULL COMMENT '工序 ID。',
  `standard_value` VARCHAR(64) NULL COMMENT '标准值。',
  `upper_limit` VARCHAR(64) NULL COMMENT '上限。',
  `lower_limit` VARCHAR(64) NULL COMMENT '下限。',
  `judgement_rule` VARCHAR(255) NULL COMMENT '判定规则。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`standard_id`),
  KEY `idx_qms_inspection_standard_item_id` (`item_id`),
  KEY `idx_qms_inspection_standard_product_id` (`product_id`),
  KEY `idx_qms_inspection_standard_operation_id` (`operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='检验标准表';

CREATE TABLE `qms_inspection_scheme` (
  `scheme_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '方案主键。',
  `scheme_code` VARCHAR(64) NULL COMMENT '方案编码。',
  `scheme_name` VARCHAR(128) NULL COMMENT '方案名称。',
  `product_id` BIGINT UNSIGNED NULL COMMENT '产品 ID。',
  `operation_id` BIGINT UNSIGNED NULL COMMENT '工序 ID。',
  `inspection_type` VARCHAR(32) NULL COMMENT '来料检、过程检、终检。',
  `status` VARCHAR(32) NULL COMMENT 'DRAFT:草稿，RELEASED:已发布，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`scheme_id`),
  UNIQUE KEY `uk_qms_inspection_scheme_scheme_code` (`scheme_code`),
  KEY `idx_qms_inspection_scheme_product_id` (`product_id`),
  KEY `idx_qms_inspection_scheme_operation_id` (`operation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='检验方案表';

CREATE TABLE `qms_inspection_scheme_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键。',
  `scheme_id` BIGINT UNSIGNED NULL COMMENT '方案 ID。',
  `item_id` BIGINT UNSIGNED NULL COMMENT '检验项目 ID。',
  `standard_id` BIGINT UNSIGNED NULL COMMENT '检验标准 ID。',
  `sample_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '抽样数量。',
  `sequence_no` INT NULL COMMENT '检验顺序。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_qms_inspection_scheme_item_scheme_id` (`scheme_id`),
  KEY `idx_qms_inspection_scheme_item_item_id` (`item_id`),
  KEY `idx_qms_inspection_scheme_item_standard_id` (`standard_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='检验方案明细表';

CREATE TABLE `qms_inspection_order` (
  `inspection_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '检验单主键。',
  `inspection_no` VARCHAR(64) NULL COMMENT '检验单号。',
  `inspection_type` VARCHAR(32) NULL COMMENT '来料检、过程检、终检。',
  `scheme_id` BIGINT UNSIGNED NULL COMMENT '检验方案 ID。',
  `work_order_id` BIGINT UNSIGNED NULL COMMENT '工单 ID。',
  `task_id` BIGINT UNSIGNED NULL COMMENT '任务 ID。',
  `report_id` BIGINT UNSIGNED NULL COMMENT '报工 ID。',
  `batch_id` BIGINT UNSIGNED NULL COMMENT '批次 ID。',
  `inspector_id` BIGINT UNSIGNED NULL COMMENT '质检员。',
  `inspection_result` VARCHAR(32) NULL COMMENT 'PASS:合格，FAIL:不合格，CONCESSION:让步接收。',
  `status` VARCHAR(32) NULL COMMENT 'DRAFT:待检，RUNNING:检验中，FINISHED:已完成。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`inspection_id`),
  KEY `idx_qms_inspection_order_scheme_id` (`scheme_id`),
  KEY `idx_qms_inspection_order_work_order_id` (`work_order_id`),
  KEY `idx_qms_inspection_order_task_id` (`task_id`),
  KEY `idx_qms_inspection_order_report_id` (`report_id`),
  KEY `idx_qms_inspection_order_batch_id` (`batch_id`),
  KEY `idx_qms_inspection_order_inspector_id` (`inspector_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='检验单主表';

CREATE TABLE `qms_inspection_result` (
  `result_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '结果主键。',
  `inspection_id` BIGINT UNSIGNED NULL COMMENT '检验单 ID。',
  `item_id` BIGINT UNSIGNED NULL COMMENT '检验项目 ID。',
  `measured_value` VARCHAR(64) NULL COMMENT '实测值。',
  `result` VARCHAR(32) NULL COMMENT 'PASS:合格，FAIL:不合格，CONCESSION:让步接收。',
  `defect_description` VARCHAR(255) NULL COMMENT '缺陷描述。',
  `photo_file_id` BIGINT UNSIGNED NULL COMMENT '质检图片附件。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`result_id`),
  KEY `idx_qms_inspection_result_inspection_id` (`inspection_id`),
  KEY `idx_qms_inspection_result_item_id` (`item_id`),
  KEY `idx_qms_inspection_result_photo_file_id` (`photo_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='检验结果明细表';

CREATE TABLE `qms_nonconforming_order` (
  `ncr_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '处理单主键。',
  `ncr_no` VARCHAR(64) NULL COMMENT '处理单号。',
  `inspection_id` BIGINT UNSIGNED NULL COMMENT '检验单 ID。',
  `product_lot_id` BIGINT UNSIGNED NULL COMMENT '产品批次 ID。',
  `defect_reason` VARCHAR(128) NULL COMMENT '不合格原因。',
  `disposition_type` VARCHAR(32) NULL COMMENT 'REWORK:返工，SCRAP:报废，DOWNGRADE:降级，CONCESSION:让步接收。',
  `approval_status` VARCHAR(32) NULL COMMENT 'SUBMITTED:待审批，APPROVED:已通过，REJECTED:已驳回。',
  `status` VARCHAR(32) NULL COMMENT 'SUBMITTED:待处理，PROCESSING:处理中，CLOSED:已关闭。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`ncr_id`),
  UNIQUE KEY `uk_qms_nonconforming_order_ncr_no` (`ncr_no`),
  KEY `idx_qms_nonconforming_order_inspection_id` (`inspection_id`),
  KEY `idx_qms_nonconforming_order_product_lot_id` (`product_lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='不合格品处理单表';

CREATE TABLE `qms_nonconforming_detail` (
  `detail_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细主键。',
  `ncr_id` BIGINT UNSIGNED NULL COMMENT '处理单 ID。',
  `barcode_id` BIGINT UNSIGNED NULL COMMENT '产品条码 ID。',
  `inspection_result_id` BIGINT UNSIGNED NULL COMMENT '检验结果 ID。',
  `defect_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '不合格数量。',
  `final_disposition` VARCHAR(32) NULL COMMENT 'REWORK:返工，SCRAP:报废，DOWNGRADE:降级，CONCESSION:让步接收。',
  `recheck_result` VARCHAR(32) NULL COMMENT 'PASS:合格，FAIL:不合格，CONCESSION:让步接收。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`detail_id`),
  KEY `idx_qms_nonconforming_detail_ncr_id` (`ncr_id`),
  KEY `idx_qms_nonconforming_detail_barcode_id` (`barcode_id`),
  KEY `idx_qms_nonconforming_detail_inspection_result_id` (`inspection_result_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='不合格品处理明细表';

CREATE TABLE `eam_equipment_category` (
  `category_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设备分类主键。',
  `parent_id` BIGINT UNSIGNED NULL COMMENT '上级分类。',
  `category_code` VARCHAR(64) NULL COMMENT '分类编码。',
  `category_name` VARCHAR(128) NULL COMMENT '分类名称。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_eam_equipment_category_category_code` (`category_code`),
  KEY `idx_eam_equipment_category_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备分类表';

CREATE TABLE `eam_equipment` (
  `equipment_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设备主键。',
  `equipment_code` VARCHAR(64) NULL COMMENT '设备编码。',
  `equipment_name` VARCHAR(128) NULL COMMENT '设备名称。',
  `category_id` BIGINT UNSIGNED NULL COMMENT '设备分类。',
  `model` VARCHAR(64) NULL COMMENT '规格型号。',
  `manufacturer` VARCHAR(64) NULL COMMENT '厂家。',
  `purchase_date` DATE NULL COMMENT '购入日期。',
  `station_id` BIGINT UNSIGNED NULL COMMENT '所在工位。',
  `status` VARCHAR(32) NULL COMMENT 'IDLE:空闲，RUNNING:运行，MAINTAINING:保养中，REPAIRING:维修中，STOPPED:停机，FAULT:故障，SCRAPPED:报废。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`equipment_id`),
  UNIQUE KEY `uk_eam_equipment_equipment_code` (`equipment_code`),
  KEY `idx_eam_equipment_category_id` (`category_id`),
  KEY `idx_eam_equipment_station_id` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备台账表';

CREATE TABLE `eam_equipment_status_log` (
  `status_log_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '状态记录主键。',
  `equipment_id` BIGINT UNSIGNED NULL COMMENT '设备 ID。',
  `running_status` VARCHAR(32) NULL COMMENT 'IDLE、RUNNING、MAINTAINING、REPAIRING、STOPPED、FAULT、SCRAPPED。',
  `downtime_type` VARCHAR(32) NULL COMMENT 'PLANNED、UNPLANNED、FAULT、CHANGEOVER，支撑 OEE 停机分类统计。',
  `alert_level` VARCHAR(32) NULL COMMENT 'LOW、MEDIUM、HIGH、URGENT。',
  `station_id` BIGINT UNSIGNED NULL COMMENT '所在工位。',
  `collected_time` DATETIME NULL COMMENT '采集时间。',
  `description` VARCHAR(500) NULL COMMENT '状态说明。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`status_log_id`),
  KEY `idx_eam_equipment_status_log_equipment_id` (`equipment_id`),
  KEY `idx_eam_equipment_status_log_station_id` (`station_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备状态记录表';

CREATE TABLE `eam_maintenance_plan` (
  `plan_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '保养计划主键。',
  `equipment_id` BIGINT UNSIGNED NULL COMMENT '设备 ID。',
  `maintenance_type` VARCHAR(32) NULL COMMENT '日常、一级、二级。',
  `cycle_type` VARCHAR(32) NULL COMMENT '按天、按小时、按次数。',
  `cycle_value` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '周期值。',
  `maintenance_content` TEXT NULL COMMENT '保养内容。',
  `responsible_id` BIGINT UNSIGNED NULL COMMENT '负责人。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`plan_id`),
  KEY `idx_eam_maintenance_plan_equipment_id` (`equipment_id`),
  KEY `idx_eam_maintenance_plan_responsible_id` (`responsible_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='保养计划表';

CREATE TABLE `eam_maintenance_record` (
  `record_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '保养记录主键。',
  `plan_id` BIGINT UNSIGNED NULL COMMENT '保养计划 ID。',
  `equipment_id` BIGINT UNSIGNED NULL COMMENT '设备 ID。',
  `maintainer_id` BIGINT UNSIGNED NULL COMMENT '保养人。',
  `maintenance_time` DATETIME NULL COMMENT '保养时间。',
  `maintenance_result` VARCHAR(500) NULL COMMENT '保养结果。',
  `next_maintenance_time` DATETIME NULL COMMENT '下次保养时间。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`record_id`),
  KEY `idx_eam_maintenance_record_plan_id` (`plan_id`),
  KEY `idx_eam_maintenance_record_equipment_id` (`equipment_id`),
  KEY `idx_eam_maintenance_record_maintainer_id` (`maintainer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='保养记录表';

CREATE TABLE `eam_spot_check_plan` (
  `plan_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点检计划主键。',
  `equipment_id` BIGINT UNSIGNED NULL COMMENT '设备 ID。',
  `check_cycle` VARCHAR(255) NULL COMMENT '点检周期。',
  `check_items` TEXT NULL COMMENT '点检项目说明。',
  `responsible_role` VARCHAR(64) NULL COMMENT '负责岗位。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`plan_id`),
  KEY `idx_eam_spot_check_plan_equipment_id` (`equipment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='点检计划表';

CREATE TABLE `eam_spot_check_record` (
  `record_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点检记录主键。',
  `plan_id` BIGINT UNSIGNED NULL COMMENT '点检计划 ID。',
  `equipment_id` BIGINT UNSIGNED NULL COMMENT '设备 ID。',
  `checker_id` BIGINT UNSIGNED NULL COMMENT '点检人。',
  `check_time` DATETIME NULL COMMENT '点检时间。',
  `check_result` VARCHAR(500) NULL COMMENT '正常、异常。',
  `abnormal_description` VARCHAR(255) NULL COMMENT '异常说明。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`record_id`),
  KEY `idx_eam_spot_check_record_plan_id` (`plan_id`),
  KEY `idx_eam_spot_check_record_equipment_id` (`equipment_id`),
  KEY `idx_eam_spot_check_record_checker_id` (`checker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='点检记录表';

CREATE TABLE `eam_repair_order` (
  `repair_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '维修单主键。',
  `repair_no` VARCHAR(64) NULL COMMENT '维修单号。',
  `equipment_id` BIGINT UNSIGNED NULL COMMENT '设备 ID。',
  `exception_id` BIGINT UNSIGNED NULL COMMENT '关联生产异常。',
  `reported_by` BIGINT UNSIGNED NULL COMMENT '报修人。',
  `reported_time` DATETIME NULL COMMENT '报修时间。',
  `fault_description` VARCHAR(255) NULL COMMENT '故障描述。',
  `repairer_id` BIGINT UNSIGNED NULL COMMENT '维修人。',
  `repair_result` VARCHAR(255) NULL COMMENT '维修结果。',
  `repair_cost` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '维修费用。',
  `status` VARCHAR(32) NULL COMMENT 'SUBMITTED:待派工，REPAIRING:维修中，WAIT_ACCEPTANCE:待验收，CLOSED:已关闭。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`repair_id`),
  UNIQUE KEY `uk_eam_repair_order_repair_no` (`repair_no`),
  KEY `idx_eam_repair_order_equipment_id` (`equipment_id`),
  KEY `idx_eam_repair_order_exception_id` (`exception_id`),
  KEY `idx_eam_repair_order_repairer_id` (`repairer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备报修/维修单表';

CREATE TABLE `trace_barcode` (
  `barcode_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '条码主键。',
  `barcode_value` VARCHAR(128) NULL COMMENT '条码值，唯一。',
  `barcode_type` VARCHAR(32) NULL COMMENT '物料、批次、产品、单据。',
  `biz_type` VARCHAR(32) NULL COMMENT '关联业务类型。',
  `biz_id` BIGINT UNSIGNED NULL COMMENT '关联业务 ID。',
  `generated_rule` VARCHAR(255) NULL COMMENT '生成规则。',
  `status` VARCHAR(32) NULL COMMENT 'ENABLE:启用，DISABLE:停用，INVALIDATED:作废。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`barcode_id`),
  UNIQUE KEY `uk_trace_barcode_barcode_value` (`barcode_value`),
  KEY `idx_trace_barcode_biz_id` (`biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='条码表';

CREATE TABLE `trace_product_lot` (
  `product_lot_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '产品批次主键。',
  `lot_no` VARCHAR(64) NULL COMMENT '产品批次号。',
  `product_id` BIGINT UNSIGNED NULL COMMENT '产品 ID。',
  `work_order_id` BIGINT UNSIGNED NULL COMMENT '工单 ID。',
  `batch_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '批次数量。',
  `qualified_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '合格数量。',
  `defect_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '不良数量。',
  `status` VARCHAR(32) NULL COMMENT 'RUNNING:生产中，WAIT_INSPECTION:待检，PASS:合格，STORED:入库，CLOSED:关闭。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`product_lot_id`),
  UNIQUE KEY `uk_trace_product_lot_lot_no` (`lot_no`),
  KEY `idx_trace_product_lot_product_id` (`product_id`),
  KEY `idx_trace_product_lot_work_order_id` (`work_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产品批次表';

CREATE TABLE `trace_material_usage` (
  `usage_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '追溯记录主键。',
  `product_lot_id` BIGINT UNSIGNED NULL COMMENT '产品批次 ID。',
  `barcode_id` BIGINT UNSIGNED NULL COMMENT '产品条码 ID。',
  `work_order_id` BIGINT UNSIGNED NULL COMMENT '工单 ID。',
  `report_id` BIGINT UNSIGNED NULL COMMENT '报工 ID。',
  `material_id` BIGINT UNSIGNED NULL COMMENT '物料 ID。',
  `batch_id` BIGINT UNSIGNED NULL COMMENT '原料批次 ID。',
  `used_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '使用数量。',
  `remark` VARCHAR(500) NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_by` BIGINT UNSIGNED NULL COMMENT '创建人',
  `created_time` DATETIME NULL COMMENT '创建时间',
  `updated_by` BIGINT UNSIGNED NULL COMMENT '更新人',
  `updated_time` DATETIME NULL COMMENT '更新时间',
  `deleted_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记 0未删 1已删',
  PRIMARY KEY (`usage_id`),
  KEY `idx_trace_material_usage_product_lot_id` (`product_lot_id`),
  KEY `idx_trace_material_usage_barcode_id` (`barcode_id`),
  KEY `idx_trace_material_usage_work_order_id` (`work_order_id`),
  KEY `idx_trace_material_usage_report_id` (`report_id`),
  KEY `idx_trace_material_usage_material_id` (`material_id`),
  KEY `idx_trace_material_usage_batch_id` (`batch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物料使用追溯表';
