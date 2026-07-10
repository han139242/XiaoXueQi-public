-- 将历史中文状态迁移为后端统一使用的英文编码。
-- 使用前请先备份数据库并选择目标库；本脚本仅修改明确列出的中文值，可重复执行。

SET NAMES utf8mb4;
START TRANSACTION;

UPDATE `sys_user`
SET `status` = CASE `status`
  WHEN '正常' THEN 'ENABLE' WHEN '启用' THEN 'ENABLE'
  WHEN '停用' THEN 'DISABLE' WHEN '禁用' THEN 'DISABLE'
  WHEN '锁定' THEN 'LOCKED' ELSE `status` END
WHERE `status` IN ('正常', '启用', '停用', '禁用', '锁定');

UPDATE `sys_role` SET `status` = IF(`status` IN ('启用', '正常'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '正常', '停用', '禁用');
UPDATE `sys_dict_type` SET `status` = IF(`status` IN ('启用', '有效'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '有效', '停用', '禁用', '无效');

UPDATE `org_employee`
SET `status` = CASE `status` WHEN '在职' THEN 'ENABLE' WHEN '离职' THEN 'RESIGNED'
  WHEN '停用' THEN 'DISABLE' WHEN '禁用' THEN 'DISABLE' ELSE `status` END
WHERE `status` IN ('在职', '离职', '停用', '禁用');
UPDATE `org_team_member` SET `status` = IF(`status` IN ('启用', '有效'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '有效', '停用', '禁用', '无效');

UPDATE `mdm_unit` SET `status` = IF(`status` IN ('启用', '有效'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '有效', '停用', '禁用', '无效');
UPDATE `mdm_supplier` SET `status` = IF(`status` IN ('启用', '合作中', '正常'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '合作中', '正常', '停用', '停止合作', '禁用');
UPDATE `mdm_customer` SET `status` = IF(`status` IN ('启用', '正常'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '正常', '停用', '禁用');
UPDATE `mdm_material_category` SET `status` = IF(`status` IN ('启用', '有效'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '有效', '停用', '禁用', '无效');
UPDATE `mdm_material` SET `status` = IF(`status` IN ('启用', '正常'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '正常', '停用', '禁用');
UPDATE `mdm_product` SET `status` = IF(`status` IN ('启用', '正常'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '正常', '停用', '禁用');
UPDATE `mdm_bom`
SET `status` = CASE `status` WHEN '草稿' THEN 'DRAFT' WHEN '已发布' THEN 'RELEASED'
  WHEN '发布' THEN 'RELEASED' ELSE 'DISABLE' END
WHERE `status` IN ('草稿', '已发布', '发布', '停用', '禁用');

UPDATE `process_operation` SET `status` = IF(`status` IN ('启用', '有效'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '有效', '停用', '禁用', '无效');
UPDATE `process_route`
SET `status` = CASE `status` WHEN '草稿' THEN 'DRAFT' WHEN '已发布' THEN 'RELEASED'
  WHEN '发布' THEN 'RELEASED' ELSE 'DISABLE' END
WHERE `status` IN ('草稿', '已发布', '发布', '停用', '禁用');
UPDATE `process_work_instruction`
SET `status` = CASE `status` WHEN '草稿' THEN 'DRAFT' WHEN '已发布' THEN 'RELEASED'
  WHEN '发布' THEN 'RELEASED' ELSE 'DISABLE' END
WHERE `status` IN ('草稿', '已发布', '发布', '停用', '禁用');

UPDATE `factory_workshop`
SET `status` = CASE `status` WHEN '运行' THEN 'RUNNING' WHEN '运行中' THEN 'RUNNING'
  ELSE 'DISABLE' END
WHERE `status` IN ('运行', '运行中', '停用', '禁用');
UPDATE `factory_line`
SET `status` = CASE `status` WHEN '运行' THEN 'RUNNING' WHEN '运行中' THEN 'RUNNING'
  WHEN '停线' THEN 'STOPPED' WHEN '停机' THEN 'STOPPED' ELSE 'MAINTAINING' END
WHERE `status` IN ('运行', '运行中', '停线', '停机', '维护', '维护中');
UPDATE `factory_station`
SET `status` = CASE `status` WHEN '运行' THEN 'RUNNING' WHEN '运行中' THEN 'RUNNING'
  WHEN '维护' THEN 'MAINTAINING' WHEN '维护中' THEN 'MAINTAINING' ELSE 'DISABLE' END
WHERE `status` IN ('运行', '运行中', '维护', '维护中', '停用', '禁用');
UPDATE `factory_station_operation` SET `status` = IF(`status` IN ('启用', '有效'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '有效', '停用', '禁用', '无效');

UPDATE `mfg_production_order`
SET `status` = CASE `status` WHEN '新建' THEN 'DRAFT' WHEN '草稿' THEN 'DRAFT'
  WHEN '已排产' THEN 'RELEASED' WHEN '生产中' THEN 'RUNNING'
  WHEN '已完成' THEN 'FINISHED' WHEN '已完工' THEN 'FINISHED' ELSE 'CLOSED' END
WHERE `status` IN ('新建', '草稿', '已排产', '生产中', '已完成', '已完工', '已关闭', '关闭');
UPDATE `mfg_production_plan`
SET `status` = CASE `status` WHEN '待生效' THEN 'DRAFT' WHEN '已生效' THEN 'RELEASED'
  WHEN '已完成' THEN 'FINISHED' ELSE 'CLOSED' END
WHERE `status` IN ('待生效', '已生效', '已完成', '已取消', '已关闭');
UPDATE `mfg_work_order`
SET `status` = CASE `status` WHEN '新建' THEN 'DRAFT' WHEN '草稿' THEN 'DRAFT'
  WHEN '已审核' THEN 'APPROVED' WHEN '已审批' THEN 'APPROVED' WHEN '已下达' THEN 'RELEASED'
  WHEN '生产中' THEN 'RUNNING' WHEN '已完成' THEN 'FINISHED' WHEN '已完工' THEN 'FINISHED'
  ELSE 'CLOSED' END
WHERE `status` IN ('新建', '草稿', '已审核', '已审批', '已下达', '生产中', '已完成', '已完工', '已关闭', '关闭');
UPDATE `mfg_work_order_material`
SET `status` = CASE `status` WHEN '待齐套' THEN 'WAIT_READY' WHEN '部分齐套' THEN 'PARTIAL_READY'
  ELSE 'READY' END
WHERE `status` IN ('待齐套', '部分齐套', '已齐套');
UPDATE `mfg_dispatch_order`
SET `material_ready_status` = CASE `material_ready_status`
  WHEN '待齐套' THEN 'WAIT_READY' WHEN '部分齐套' THEN 'PARTIAL_READY' ELSE 'READY' END
WHERE `material_ready_status` IN ('待齐套', '部分齐套', '已齐套');
UPDATE `mfg_dispatch_order`
SET `status` = CASE `status` WHEN '待派发' THEN 'WAIT_DISPATCH' WHEN '已派发' THEN 'RELEASED'
  WHEN '已接受' THEN 'ACCEPTED' WHEN '生产中' THEN 'RUNNING' WHEN '已完成' THEN 'FINISHED'
  ELSE 'REJECTED' END
WHERE `status` IN ('待派发', '已派发', '已接受', '生产中', '已完成', '已驳回');
UPDATE `mfg_task`
SET `status` = CASE `status` WHEN '待开工' THEN 'WAIT_START' WHEN '待开始' THEN 'WAIT_START'
  WHEN '生产中' THEN 'RUNNING' WHEN '进行中' THEN 'RUNNING' WHEN '暂停' THEN 'PAUSED'
  WHEN '已暂停' THEN 'PAUSED' WHEN '已完工' THEN 'FINISHED' WHEN '已完成' THEN 'FINISHED'
  ELSE 'CLOSED' END
WHERE `status` IN ('待开工', '待开始', '生产中', '进行中', '暂停', '已暂停', '已完工', '已完成', '已关闭');
UPDATE `mfg_work_report`
SET `audit_status` = CASE `audit_status` WHEN '待审核' THEN 'SUBMITTED' WHEN '已提交' THEN 'SUBMITTED'
  WHEN '通过' THEN 'APPROVED' WHEN '已审核' THEN 'APPROVED' ELSE 'REJECTED' END
WHERE `audit_status` IN ('待审核', '已提交', '通过', '已审核', '驳回', '已驳回');
UPDATE `mfg_exception`
SET `status` = CASE `status` WHEN '待处理' THEN 'SUBMITTED' WHEN '处理中' THEN 'PROCESSING'
  WHEN '已解决' THEN 'RESOLVED' ELSE 'CLOSED' END
WHERE `status` IN ('待处理', '处理中', '已解决', '已关闭', '关闭');
UPDATE `mfg_andon_alert`
SET `status` = CASE `status` WHEN '告警中' THEN 'ACTIVE' WHEN '处理中' THEN 'PROCESSING'
  ELSE 'CLOSED' END
WHERE `status` IN ('告警中', '处理中', '已解除', '已关闭', '关闭');

UPDATE `wms_warehouse` SET `status` = IF(`status` IN ('启用', '正常'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '正常', '停用', '禁用');
UPDATE `wms_location`
SET `status` = CASE `status` WHEN '空闲' THEN 'IDLE' WHEN '占用' THEN 'OCCUPIED'
  WHEN '锁定' THEN 'LOCKED' ELSE 'MAINTAINING' END
WHERE `status` IN ('空闲', '占用', '锁定', '维护', '维护中');
UPDATE `wms_material_batch`
SET `quality_status` = CASE `quality_status` WHEN '待检' THEN 'WAIT_INSPECTION'
  WHEN '合格' THEN 'PASS' WHEN '不合格' THEN 'FAIL' ELSE 'CONCESSION' END
WHERE `quality_status` IN ('待检', '合格', '不合格', '让步接收');
UPDATE `wms_material_batch`
SET `status` = CASE `status` WHEN '可用' THEN 'AVAILABLE' WHEN '冻结' THEN 'FROZEN'
  WHEN '耗尽' THEN 'EXHAUSTED' ELSE 'SCRAPPED' END
WHERE `status` IN ('可用', '冻结', '耗尽', '报废');
UPDATE `wms_inbound_order`
SET `audit_status` = CASE `audit_status` WHEN '待审核' THEN 'SUBMITTED' WHEN '已提交' THEN 'SUBMITTED'
  WHEN '已审核' THEN 'APPROVED' WHEN '通过' THEN 'APPROVED' ELSE 'REJECTED' END
WHERE `audit_status` IN ('待审核', '已提交', '已审核', '通过', '驳回', '已驳回');
UPDATE `wms_outbound_order`
SET `audit_status` = CASE `audit_status` WHEN '待审核' THEN 'SUBMITTED' WHEN '已提交' THEN 'SUBMITTED'
  WHEN '已审核' THEN 'APPROVED' WHEN '通过' THEN 'APPROVED' ELSE 'REJECTED' END
WHERE `audit_status` IN ('待审核', '已提交', '已审核', '通过', '驳回', '已驳回');
UPDATE `wms_return_order`
SET `audit_status` = CASE `audit_status` WHEN '待审核' THEN 'SUBMITTED' WHEN '已提交' THEN 'SUBMITTED'
  WHEN '已审核' THEN 'APPROVED' WHEN '通过' THEN 'APPROVED' ELSE 'REJECTED' END
WHERE `audit_status` IN ('待审核', '已提交', '已审核', '通过', '驳回', '已驳回');
UPDATE `wms_stocktake_order`
SET `stocktake_status` = CASE `stocktake_status` WHEN '待盘' THEN 'WAIT_START'
  WHEN '盘点中' THEN 'RUNNING' WHEN '已完成' THEN 'FINISHED' ELSE 'CLOSED' END
WHERE `stocktake_status` IN ('待盘', '盘点中', '已完成', '已关闭');
UPDATE `wms_replenishment_suggestion`
SET `status` = CASE `status` WHEN '待处理' THEN 'SUBMITTED' WHEN '已确认' THEN 'APPROVED'
  ELSE 'CLOSED' END
WHERE `status` IN ('待处理', '已确认', '已关闭');

UPDATE `qms_inspection_scheme`
SET `status` = CASE `status` WHEN '草稿' THEN 'DRAFT' WHEN '已发布' THEN 'RELEASED'
  WHEN '发布' THEN 'RELEASED' ELSE 'DISABLE' END
WHERE `status` IN ('草稿', '已发布', '发布', '停用', '禁用');
UPDATE `qms_inspection_order`
SET `inspection_result` = CASE `inspection_result` WHEN '合格' THEN 'PASS'
  WHEN '不合格' THEN 'FAIL' ELSE 'CONCESSION' END
WHERE `inspection_result` IN ('合格', '不合格', '让步接收');
UPDATE `qms_inspection_order`
SET `status` = CASE `status` WHEN '待检' THEN 'DRAFT' WHEN '草稿' THEN 'DRAFT'
  WHEN '检验中' THEN 'RUNNING' ELSE 'FINISHED' END
WHERE `status` IN ('待检', '草稿', '检验中', '已完成');
UPDATE `qms_inspection_result`
SET `result` = CASE `result` WHEN '合格' THEN 'PASS' WHEN '不合格' THEN 'FAIL'
  ELSE 'CONCESSION' END
WHERE `result` IN ('合格', '不合格', '让步接收');
UPDATE `qms_nonconforming_order`
SET `disposition_type` = CASE `disposition_type` WHEN '返工' THEN 'REWORK' WHEN '报废' THEN 'SCRAP'
  WHEN '降级' THEN 'DOWNGRADE' ELSE 'CONCESSION' END
WHERE `disposition_type` IN ('返工', '报废', '降级', '让步接收');
UPDATE `qms_nonconforming_order`
SET `approval_status` = CASE `approval_status` WHEN '待审批' THEN 'SUBMITTED' WHEN '已提交' THEN 'SUBMITTED'
  WHEN '通过' THEN 'APPROVED' WHEN '已通过' THEN 'APPROVED' WHEN '已审核' THEN 'APPROVED'
  ELSE 'REJECTED' END
WHERE `approval_status` IN ('待审批', '已提交', '通过', '已通过', '已审核', '驳回', '已驳回', '退回');
UPDATE `qms_nonconforming_order`
SET `status` = CASE `status` WHEN '待处理' THEN 'SUBMITTED' WHEN '处理中' THEN 'PROCESSING'
  WHEN 'APPROVED' THEN 'PROCESSING' ELSE 'CLOSED' END
WHERE `status` IN ('待处理', '处理中', '已关闭', '关闭', 'APPROVED', 'REJECTED');
UPDATE `qms_nonconforming_detail`
SET `final_disposition` = CASE `final_disposition` WHEN '返工' THEN 'REWORK' WHEN '报废' THEN 'SCRAP'
  WHEN '降级' THEN 'DOWNGRADE' ELSE 'CONCESSION' END
WHERE `final_disposition` IN ('返工', '报废', '降级', '让步接收');
UPDATE `qms_nonconforming_detail`
SET `recheck_result` = CASE `recheck_result` WHEN '合格' THEN 'PASS' WHEN '不合格' THEN 'FAIL'
  ELSE 'CONCESSION' END
WHERE `recheck_result` IN ('合格', '不合格', '让步接收');

UPDATE `eam_equipment_category` SET `status` = IF(`status` IN ('启用', '有效'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '有效', '停用', '禁用', '无效');
UPDATE `eam_equipment`
SET `status` = CASE `status` WHEN '空闲' THEN 'IDLE' WHEN '运行' THEN 'RUNNING'
  WHEN '运行中' THEN 'RUNNING' WHEN '保养中' THEN 'MAINTAINING' WHEN '维修中' THEN 'REPAIRING'
  WHEN '停机' THEN 'STOPPED' WHEN '故障' THEN 'FAULT' ELSE 'SCRAPPED' END
WHERE `status` IN ('空闲', '运行', '运行中', '保养中', '维修中', '停机', '故障', '报废');
UPDATE `eam_equipment_status_log`
SET `running_status` = CASE `running_status` WHEN '空闲' THEN 'IDLE' WHEN '运行' THEN 'RUNNING'
  WHEN '运行中' THEN 'RUNNING' WHEN '保养中' THEN 'MAINTAINING' WHEN '维修中' THEN 'REPAIRING'
  WHEN '停机' THEN 'STOPPED' WHEN '故障' THEN 'FAULT' ELSE 'SCRAPPED' END
WHERE `running_status` IN ('空闲', '运行', '运行中', '保养中', '维修中', '停机', '故障', '报废');
UPDATE `eam_equipment_status_log`
SET `downtime_type` = CASE `downtime_type` WHEN '计划停机' THEN 'PLANNED'
  WHEN '非计划停机' THEN 'UNPLANNED' WHEN '故障停机' THEN 'FAULT' ELSE 'CHANGEOVER' END
WHERE `downtime_type` IN ('计划停机', '非计划停机', '故障停机', '换型停机');
UPDATE `eam_equipment_status_log`
SET `alert_level` = CASE `alert_level` WHEN '低' THEN 'LOW' WHEN '中' THEN 'MEDIUM'
  WHEN '高' THEN 'HIGH' ELSE 'URGENT' END
WHERE `alert_level` IN ('低', '中', '高', '紧急');
UPDATE `eam_maintenance_plan` SET `status` = IF(`status` IN ('启用', '有效'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '有效', '停用', '禁用', '无效');
UPDATE `eam_spot_check_plan` SET `status` = IF(`status` IN ('启用', '有效'), 'ENABLE', 'DISABLE')
WHERE `status` IN ('启用', '有效', '停用', '禁用', '无效');
UPDATE `eam_repair_order`
SET `status` = CASE `status` WHEN '待派工' THEN 'SUBMITTED' WHEN '维修中' THEN 'REPAIRING'
  WHEN '待验收' THEN 'WAIT_ACCEPTANCE' ELSE 'CLOSED' END
WHERE `status` IN ('待派工', '维修中', '待验收', '已关闭', '关闭');

UPDATE `trace_barcode`
SET `status` = CASE `status` WHEN '启用' THEN 'ENABLE' WHEN '停用' THEN 'DISABLE'
  WHEN '禁用' THEN 'DISABLE' ELSE 'INVALIDATED' END
WHERE `status` IN ('启用', '停用', '禁用', '作废');
UPDATE `trace_product_lot`
SET `status` = CASE `status` WHEN '生产中' THEN 'RUNNING' WHEN '待检' THEN 'WAIT_INSPECTION'
  WHEN '合格' THEN 'PASS' WHEN '入库' THEN 'STORED' ELSE 'CLOSED' END
WHERE `status` IN ('生产中', '待检', '合格', '入库', '关闭', '已关闭');

UPDATE `sys_approval_record`
SET `approval_result` = CASE `approval_result`
  WHEN '同意' THEN 'APPROVED' WHEN '通过' THEN 'APPROVED' WHEN '已通过' THEN 'APPROVED'
  ELSE 'REJECTED' END
WHERE `approval_result` IN ('同意', '通过', '已通过', '驳回', '已驳回', '退回');

UPDATE `sys_status_change_log`
SET `old_status` = CASE `old_status`
  WHEN '启用' THEN 'ENABLE' WHEN '停用' THEN 'DISABLE' WHEN '禁用' THEN 'DISABLE'
  WHEN '新建' THEN 'DRAFT' WHEN '草稿' THEN 'DRAFT' WHEN '待处理' THEN 'SUBMITTED'
  WHEN '待审核' THEN 'SUBMITTED' WHEN '已提交' THEN 'SUBMITTED' WHEN '已审核' THEN 'APPROVED'
  WHEN '通过' THEN 'APPROVED' WHEN '已确认' THEN 'APPROVED' WHEN '驳回' THEN 'REJECTED'
  WHEN '已驳回' THEN 'REJECTED' WHEN '已下达' THEN 'RELEASED' WHEN '已发布' THEN 'RELEASED'
  WHEN '待开工' THEN 'WAIT_START' WHEN '生产中' THEN 'RUNNING' WHEN '运行' THEN 'RUNNING'
  WHEN '暂停' THEN 'PAUSED' WHEN '已完成' THEN 'FINISHED' WHEN '已完工' THEN 'FINISHED'
  WHEN '处理中' THEN 'PROCESSING' WHEN '已解决' THEN 'RESOLVED' WHEN '维修中' THEN 'REPAIRING'
  WHEN '停机' THEN 'STOPPED' WHEN '故障' THEN 'FAULT' WHEN '已关闭' THEN 'CLOSED'
  WHEN '关闭' THEN 'CLOSED' WHEN '合格' THEN 'PASS' WHEN '不合格' THEN 'FAIL'
  ELSE `old_status` END
WHERE `old_status` IN ('启用', '停用', '禁用', '新建', '草稿', '待处理', '待审核', '已提交',
  '已审核', '通过', '已确认', '驳回', '已驳回', '已下达', '已发布', '待开工', '生产中',
  '运行', '暂停', '已完成', '已完工', '处理中', '已解决', '维修中', '停机', '故障',
  '已关闭', '关闭', '合格', '不合格');
UPDATE `sys_status_change_log`
SET `new_status` = CASE `new_status`
  WHEN '启用' THEN 'ENABLE' WHEN '停用' THEN 'DISABLE' WHEN '禁用' THEN 'DISABLE'
  WHEN '新建' THEN 'DRAFT' WHEN '草稿' THEN 'DRAFT' WHEN '待处理' THEN 'SUBMITTED'
  WHEN '待审核' THEN 'SUBMITTED' WHEN '已提交' THEN 'SUBMITTED' WHEN '已审核' THEN 'APPROVED'
  WHEN '通过' THEN 'APPROVED' WHEN '已确认' THEN 'APPROVED' WHEN '驳回' THEN 'REJECTED'
  WHEN '已驳回' THEN 'REJECTED' WHEN '已下达' THEN 'RELEASED' WHEN '已发布' THEN 'RELEASED'
  WHEN '待开工' THEN 'WAIT_START' WHEN '生产中' THEN 'RUNNING' WHEN '运行' THEN 'RUNNING'
  WHEN '暂停' THEN 'PAUSED' WHEN '已完成' THEN 'FINISHED' WHEN '已完工' THEN 'FINISHED'
  WHEN '处理中' THEN 'PROCESSING' WHEN '已解决' THEN 'RESOLVED' WHEN '维修中' THEN 'REPAIRING'
  WHEN '停机' THEN 'STOPPED' WHEN '故障' THEN 'FAULT' WHEN '已关闭' THEN 'CLOSED'
  WHEN '关闭' THEN 'CLOSED' WHEN '合格' THEN 'PASS' WHEN '不合格' THEN 'FAIL'
  ELSE `new_status` END
WHERE `new_status` IN ('启用', '停用', '禁用', '新建', '草稿', '待处理', '待审核', '已提交',
  '已审核', '通过', '已确认', '驳回', '已驳回', '已下达', '已发布', '待开工', '生产中',
  '运行', '暂停', '已完成', '已完工', '处理中', '已解决', '维修中', '停机', '故障',
  '已关闭', '关闭', '合格', '不合格');

COMMIT;
