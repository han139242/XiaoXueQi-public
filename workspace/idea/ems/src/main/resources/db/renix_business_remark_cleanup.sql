-- 雷尼克斯 MES 正式业务备注清理
-- 清除历史初始化、迁移和数据批次前缀，只保留人工可读的备注正文。
-- 无冒号的系统幂等标记不会被修改。本脚本不修改主键、业务编码、状态、关联关系和审计时间。

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @technical_prefix_pattern =
  '^(ENTERPRISE_BASELINE_FINAL|RENIX_TX_30D_V1|RENIX_RBAC_V1|RENIX_SYSTEM_BASE_V2|RENIX_LIVE_14D_V1|RBAC_PERMISSION_V2|SMART_SCHEDULING_FOUNDATION|SCHEDULING_E2E_20260713|AI_BUSINESS_DRAFT_V1|P0_PRODUCTION_FLOW_PERMISSION_V1):';
SET @normalized_technical_remark_rows = 0;
SET @normalized_equipment_category_rows = 0;

START TRANSACTION;

UPDATE `eam_equipment`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `eam_equipment_category`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `eam_equipment_status_log`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `eam_maintenance_plan`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `eam_maintenance_record`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `eam_repair_order`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `eam_spot_check_plan`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `eam_spot_check_record`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `factory_line`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `factory_station`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `factory_workshop`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mdm_bom`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mdm_bom_item`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mdm_customer`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mdm_material`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mdm_material_category`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mdm_product`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mdm_supplier`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mdm_unit`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_andon_alert`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_dispatch_order`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_exception`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_line_calendar_rule`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_line_product_capability`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_production_order`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_production_plan`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_report_material`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_schedule_scenario`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_task`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_work_order`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_work_order_material`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `mfg_work_report`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `org_department`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `org_employee`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `org_shift`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `org_team`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `process_operation`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `process_parameter`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `process_route`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `process_route_item`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `process_work_instruction`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `qms_inspection_item`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `qms_inspection_order`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `qms_inspection_result`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `qms_inspection_scheme`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `qms_inspection_scheme_item`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `qms_inspection_standard`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `qms_nonconforming_detail`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `qms_nonconforming_order`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `sys_dict_data`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `sys_dict_type`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `sys_file`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `sys_menu`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `sys_operation_log`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `sys_role`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `sys_system_param`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `sys_user`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `trace_barcode`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `trace_material_usage`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `trace_product_lot`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_inbound_detail`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_inbound_order`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_inventory`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_location`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_material_batch`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_outbound_detail`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_outbound_order`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_replenishment_suggestion`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_return_detail`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_return_order`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_stock_movement`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_stocktake_detail`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_stocktake_order`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `wms_warehouse`
SET `remark` = NULLIF(TRIM(SUBSTRING(`remark`, LOCATE(':', `remark`) + 1)), '')
WHERE `remark` REGEXP @technical_prefix_pattern;
SET @normalized_technical_remark_rows =
  @normalized_technical_remark_rows + ROW_COUNT();

UPDATE `eam_equipment_category`
SET `remark` = CASE `category_code`
  WHEN 'EQC-PRODUCTION' THEN '生产、检测及包装相关设备的统一分类'
  WHEN 'EQC-FORMING' THEN '用于软木球头成型加工的设备'
  WHEN 'EQC-ASSEMBLY' THEN '用于植毛、绕线等装配工序的设备'
  WHEN 'EQC-CURING' THEN '用于涂胶及恒温固化工序的设备'
  WHEN 'EQC-TESTING' THEN '用于飞行速度及质量检测的设备'
  WHEN 'EQC-PACKAGING' THEN '用于成品装筒、封装等包装作业的设备'
  ELSE `remark`
END
WHERE `category_code` IN (
  'EQC-PRODUCTION',
  'EQC-FORMING',
  'EQC-ASSEMBLY',
  'EQC-CURING',
  'EQC-TESTING',
  'EQC-PACKAGING'
);
SET @normalized_equipment_category_rows = ROW_COUNT();

COMMIT;

SELECT @normalized_technical_remark_rows AS `normalized_technical_remark_rows`,
       @normalized_equipment_category_rows AS `normalized_equipment_category_rows`;
