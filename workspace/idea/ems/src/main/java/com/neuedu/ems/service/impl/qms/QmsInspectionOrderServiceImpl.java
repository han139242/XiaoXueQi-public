package com.neuedu.ems.service.impl.qms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.qms.QmsInspectionOrder;
import com.neuedu.ems.entity.qms.QmsInspectionResult;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.wms.WmsMaterialBatch;
import com.neuedu.ems.mapper.qms.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.qms.QmsInspectionResultMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.wms.WmsMaterialBatchMapper;
import com.neuedu.ems.service.qms.QmsInspectionOrderService;
import com.neuedu.ems.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 检验单 Service 实现类。
 */
@Service
public class QmsInspectionOrderServiceImpl extends ServiceImpl<QmsInspectionOrderMapper, QmsInspectionOrder> implements QmsInspectionOrderService {

    private static final String BATCH_AVAILABLE = "AVAILABLE";
    private static final String BATCH_FROZEN = "FROZEN";
    private static final String BATCH_EXHAUSTED = "EXHAUSTED";
    private static final String BATCH_SCRAPPED = "SCRAPPED";

    @Autowired
    private QmsInspectionResultMapper qmsInspectionResultMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private WmsMaterialBatchMapper wmsMaterialBatchMapper;

    @Autowired
    private QmsResponsibilityNotificationService responsibilityNotificationService;

    /**
     * 分页查询检验单，支持单号、类型、方案、工单、任务、报工、批次、检验员、状态和结果筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 检验单号关键字
     * @param inspectionType 检验类型
     * @param schemeId 方案ID
     * @param workOrderId 工单ID
     * @param taskId 任务ID
     * @param reportId 报工ID
     * @param batchId 批次ID
     * @param inspectorId 检验员ID
     * @param status 状态
     * @param inspectionResult 检验结果
     * @return 检验单分页结果
     */
    @Override
    public Page<QmsInspectionOrder> pageInspectionOrders(Integer current, Integer size, String keyword, String inspectionType,
                                                         Long schemeId, Long workOrderId, Long taskId, Long reportId,
                                                         Long batchId, Long inspectorId, String status, String inspectionResult) {
        Page<QmsInspectionOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildInspectionOrderQuery(keyword, inspectionType, schemeId, workOrderId,
                taskId, reportId, batchId, inspectorId, status, inspectionResult));
    }

    /**
     * 查询检验单列表，支持单号、类型、方案、工单、任务、报工、批次、检验员、状态和结果筛选。
     * @param keyword 检验单号关键字
     * @param inspectionType 检验类型
     * @param schemeId 方案ID
     * @param workOrderId 工单ID
     * @param taskId 任务ID
     * @param reportId 报工ID
     * @param batchId 批次ID
     * @param inspectorId 检验员ID
     * @param status 状态
     * @param inspectionResult 检验结果
     * @return 检验单列表
     */
    @Override
    public List<QmsInspectionOrder> listInspectionOrders(String keyword, String inspectionType, Long schemeId, Long workOrderId,
                                                         Long taskId, Long reportId, Long batchId, Long inspectorId,
                                                         String status, String inspectionResult) {
        return baseMapper.selectList(buildInspectionOrderQuery(keyword, inspectionType, schemeId, workOrderId,
                taskId, reportId, batchId, inspectorId, status, inspectionResult));
    }

    /**
     * 根据ID查询未删除的检验单。
     * @param inspectionId 检验单ID
     * @return 检验单实体
     */
    @Override
    public QmsInspectionOrder getInspectionOrderById(Long inspectionId) {
        LambdaQueryWrapper<QmsInspectionOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionOrder::getInspectionId, inspectionId)
                .eq(QmsInspectionOrder::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增检验单，自动补齐编号、状态和删除标记。
     * @param inspectionOrder 检验单实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean saveInspectionOrder(QmsInspectionOrder inspectionOrder) {
        prepareInspectionOrder(inspectionOrder);
        boolean saved = baseMapper.insert(inspectionOrder) > 0;
        if (saved) {
            responsibilityNotificationService.publishOpenInspection(inspectionOrder, inspectionOrder.getCreatedBy());
        }
        return saved;
    }

    /**
     * 新增检验单和检验结果明细。
     * @param inspectionOrder 检验单实体
     * @param results 检验结果列表
     * @return 检验单ID
     */
    @Override
    @Transactional
    public Long saveInspectionOrderFull(QmsInspectionOrder inspectionOrder, List<QmsInspectionResult> results) {
        saveInspectionOrder(inspectionOrder);
        insertResults(inspectionOrder.getInspectionId(), results);
        return inspectionOrder.getInspectionId();
    }

    /**
     * 修改检验单，已完成检验单不允许普通修改。
     * @param inspectionOrder 检验单实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateInspectionOrder(QmsInspectionOrder inspectionOrder) {
        if (inspectionOrder.getInspectionId() == null) {
            return false;
        }
        QmsInspectionOrder existing = getInspectionOrderById(inspectionOrder.getInspectionId());
        if (existing == null || BusinessStatus.Production.FINISHED.equals(existing.getStatus())) {
            return false;
        }
        inspectionOrder.setStatus(null);
        inspectionOrder.setInspectionResult(null);
        inspectionOrder.setDeletedFlag(null);
        if (inspectionOrder.getVersion() == null) {
            inspectionOrder.setVersion(existing.getVersion());
        }
        boolean updated = baseMapper.updateById(inspectionOrder) > 0;
        if (updated && inspectionOrder.getInspectorId() != null
                && !Objects.equals(existing.getInspectorId(), inspectionOrder.getInspectorId())) {
            responsibilityNotificationService.cancelOpenInspection(existing.getInspectionId(), inspectionOrder.getUpdatedBy());
            QmsInspectionOrder current = getInspectionOrderById(existing.getInspectionId());
            responsibilityNotificationService.publishOpenInspection(current, inspectionOrder.getUpdatedBy());
        }
        return updated;
    }

    /**
     * 修改检验单并替换检验结果明细，已完成检验单不允许替换明细。
     * @param inspectionOrder 检验单实体
     * @param results 检验结果列表
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateInspectionOrderFull(QmsInspectionOrder inspectionOrder, List<QmsInspectionResult> results) {
        if (!updateInspectionOrder(inspectionOrder)) {
            return false;
        }
        if (results != null) {
            softDeleteResults(inspectionOrder.getInspectionId());
            insertResults(inspectionOrder.getInspectionId(), results);
        }
        return true;
    }

    /**
     * 根据ID软删除检验单和检验结果，已完成检验单不允许删除。
     * @param inspectionId 检验单ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean removeInspectionOrderById(Long inspectionId) {
        QmsInspectionOrder existing = getInspectionOrderById(inspectionId);
        if (existing == null || BusinessStatus.Production.FINISHED.equals(existing.getStatus())) {
            return false;
        }
        QmsInspectionOrder inspectionOrder = new QmsInspectionOrder();
        inspectionOrder.setInspectionId(inspectionId);
        inspectionOrder.setDeletedFlag(1);
        inspectionOrder.setVersion(existing.getVersion());
        boolean removed = baseMapper.updateById(inspectionOrder) > 0;
        softDeleteResults(inspectionId);
        if (removed) {
            responsibilityNotificationService.cancelOpenInspection(inspectionId, existing.getUpdatedBy());
        }
        return removed;
    }

    /**
     * 提交检验单结果并结束检验单，重复提交已完成检验单会失败。
     * @param inspectionId 检验单ID
     * @param inspectionResult 检验结果
     * @param remark 备注
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean submitInspectionOrder(Long inspectionId, String inspectionResult, String remark, Long operatorId) {
        String normalizedInspectionResult = BusinessStatus.normalizeCode(inspectionResult);
        if (!BusinessStatus.isInspectionResult(normalizedInspectionResult) || operatorId == null) {
            return false;
        }
        QmsInspectionOrder existing = getInspectionOrderById(inspectionId);
        if (existing == null || BusinessStatus.Production.FINISHED.equals(existing.getStatus())) {
            return false;
        }
        WmsMaterialBatch batch = existing.getBatchId() == null ? null : getActiveBatch(existing.getBatchId());
        if (existing.getBatchId() != null && batch == null) {
            throw business(ResultCode.NOT_FOUND, "检验单关联的物料批次不存在或已删除");
        }
        if (batch != null && !BusinessStatus.Quality.FAIL.equals(normalizedInspectionResult)
                && (BATCH_EXHAUSTED.equals(batch.getStatus()) || BATCH_SCRAPPED.equals(batch.getStatus()))) {
            throw business(ResultCode.BUSINESS_ERROR, "耗尽或报废批次不能通过检验重新放行");
        }
        QmsInspectionOrder update = new QmsInspectionOrder();
        update.setInspectionId(inspectionId);
        update.setVersion(existing.getVersion());
        update.setInspectionResult(normalizedInspectionResult);
        update.setStatus(BusinessStatus.Production.FINISHED);
        update.setRemark(StringUtils.hasText(remark) ? remark : existing.getRemark());
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "检验单已被其他用户修改，请刷新后重试");
        }
        LocalDateTime now = LocalDateTime.now();
        insertStatusChangeLog(inspectionId, existing.getStatus(), BusinessStatus.Production.FINISHED, "QMS_INSPECTION_ORDER",
                "SUBMIT_INSPECTION", operatorId, remark, now);
        insertOperationLog(inspectionId, "SUBMIT_INSPECTION", "qms_inspection_order", operatorId, remark, now);
        if (batch != null) {
            synchronizeBatchQuality(batch, normalizedInspectionResult, inspectionId, operatorId, now);
        }
        responsibilityNotificationService.cancelOpenInspection(inspectionId, operatorId);
        responsibilityNotificationService.publishInspectionCompleted(existing, normalizedInspectionResult, operatorId);
        return true;
    }

    private WmsMaterialBatch getActiveBatch(Long batchId) {
        LambdaQueryWrapper<WmsMaterialBatch> query = new LambdaQueryWrapper<>();
        query.eq(WmsMaterialBatch::getBatchId, batchId)
                .eq(WmsMaterialBatch::getDeletedFlag, 0);
        return wmsMaterialBatchMapper.selectOne(query);
    }

    private void synchronizeBatchQuality(WmsMaterialBatch batch, String inspectionResult, Long inspectionId,
                                         Long operatorId, LocalDateTime now) {
        boolean terminalBatch = BATCH_EXHAUSTED.equals(batch.getStatus()) || BATCH_SCRAPPED.equals(batch.getStatus());
        String targetStatus = terminalBatch ? batch.getStatus()
                : BusinessStatus.Quality.FAIL.equals(inspectionResult) ? BATCH_FROZEN : BATCH_AVAILABLE;
        WmsMaterialBatch update = new WmsMaterialBatch();
        update.setBatchId(batch.getBatchId());
        update.setVersion(batch.getVersion());
        update.setQualityStatus(inspectionResult);
        update.setStatus(targetStatus);
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (wmsMaterialBatchMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "物料批次已被其他用户修改，请刷新后重试");
        }
        String batchRemark = "检验单#" + inspectionId + "同步批次质量状态，批次状态=" + targetStatus;
        insertStatusChangeLog(batch.getBatchId(), batch.getQualityStatus(), inspectionResult, "WMS_MATERIAL_BATCH",
                "SYNC_INSPECTION_RESULT", operatorId, batchRemark, now);
        insertOperationLog(batch.getBatchId(), "SYNC_INSPECTION_RESULT", "wms_material_batch",
                operatorId, batchRemark, now);
    }

    /**
     * 构建检验单查询条件，默认过滤已删除记录。
     * @param keyword 检验单号关键字
     * @param inspectionType 检验类型
     * @param schemeId 方案ID
     * @param workOrderId 工单ID
     * @param taskId 任务ID
     * @param reportId 报工ID
     * @param batchId 批次ID
     * @param inspectorId 检验员ID
     * @param status 状态
     * @param inspectionResult 检验结果
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<QmsInspectionOrder> buildInspectionOrderQuery(String keyword, String inspectionType,
                                                                             Long schemeId, Long workOrderId,
                                                                             Long taskId, Long reportId,
                                                                             Long batchId, Long inspectorId,
                                                                             String status, String inspectionResult) {
        LambdaQueryWrapper<QmsInspectionOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionOrder::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), QmsInspectionOrder::getInspectionNo, keyword)
                .eq(StringUtils.hasText(inspectionType), QmsInspectionOrder::getInspectionType, inspectionType)
                .eq(schemeId != null, QmsInspectionOrder::getSchemeId, schemeId)
                .eq(workOrderId != null, QmsInspectionOrder::getWorkOrderId, workOrderId)
                .eq(taskId != null, QmsInspectionOrder::getTaskId, taskId)
                .eq(reportId != null, QmsInspectionOrder::getReportId, reportId)
                .eq(batchId != null, QmsInspectionOrder::getBatchId, batchId)
                .eq(inspectorId != null, QmsInspectionOrder::getInspectorId, inspectorId)
                .eq(StringUtils.hasText(status), QmsInspectionOrder::getStatus, status)
                .eq(StringUtils.hasText(inspectionResult), QmsInspectionOrder::getInspectionResult, inspectionResult)
                .orderByDesc(QmsInspectionOrder::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 补齐检验单默认值。
     * @param inspectionOrder 检验单实体
     */
    private void prepareInspectionOrder(QmsInspectionOrder inspectionOrder) {
        if (!StringUtils.hasText(inspectionOrder.getInspectionNo())) {
            inspectionOrder.setInspectionNo("QI" + System.currentTimeMillis());
        }
        inspectionOrder.setStatus(BusinessStatus.Document.DRAFT);
        inspectionOrder.setInspectionResult(null);
        if (inspectionOrder.getDeletedFlag() == null) {
            inspectionOrder.setDeletedFlag(0);
        }
    }

    /**
     * 批量插入检验结果明细。
     * @param inspectionId 检验单ID
     * @param results 检验结果列表
     */
    private void insertResults(Long inspectionId, List<QmsInspectionResult> results) {
        if (results == null) {
            return;
        }
        for (QmsInspectionResult result : results) {
            normalizeInspectionResult(result);
            result.setResultId(null);
            result.setInspectionId(inspectionId);
            if (result.getDeletedFlag() == null) {
                result.setDeletedFlag(0);
            }
            qmsInspectionResultMapper.insert(result);
        }
    }

    private void normalizeInspectionResult(QmsInspectionResult inspectionResult) {
        if (!StringUtils.hasText(inspectionResult.getResult())) {
            return;
        }
        String normalized = BusinessStatus.normalizeCode(inspectionResult.getResult());
        if (!BusinessStatus.isInspectionResult(normalized)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        inspectionResult.setResult(normalized);
    }

    /**
     * 软删除指定检验单的全部检验结果明细。
     * @param inspectionId 检验单ID
     */
    private void softDeleteResults(Long inspectionId) {
        LambdaQueryWrapper<QmsInspectionResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionResult::getInspectionId, inspectionId)
                .eq(QmsInspectionResult::getDeletedFlag, 0);
        List<QmsInspectionResult> results = qmsInspectionResultMapper.selectList(queryWrapper);
        for (QmsInspectionResult result : results) {
            QmsInspectionResult update = new QmsInspectionResult();
            update.setResultId(result.getResultId());
            update.setDeletedFlag(1);
            qmsInspectionResultMapper.updateById(update);
        }
    }

    /**
     * 写入状态变更日志。
     * @param bizId 业务ID
     * @param oldStatus 原状态
     * @param newStatus 新状态
     * @param bizType 业务类型
     * @param reason 变更原因
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param changedTime 变更时间
     */
    private void insertStatusChangeLog(Long bizId, String oldStatus, String newStatus, String bizType,
                                       String reason, Long operatorId, String remark, LocalDateTime changedTime) {
        SysStatusChangeLog statusLog = new SysStatusChangeLog();
        statusLog.setBizType(bizType);
        statusLog.setBizId(bizId);
        statusLog.setOldStatus(oldStatus);
        statusLog.setNewStatus(newStatus);
        statusLog.setChangeReason(reason);
        statusLog.setChangedBy(operatorId);
        statusLog.setChangedTime(changedTime);
        statusLog.setCreatedBy(operatorId);
        statusLog.setCreatedTime(changedTime);
        statusLog.setRemark(remark);
        statusLog.setDeletedFlag(0);
        if (sysStatusChangeLogMapper.insert(statusLog) == 0) {
            throw new BusinessException(ResultCode.FAILED);
        }
    }

    /**
     * 写入操作日志。
     * @param bizId 业务ID
     * @param operationType 操作类型
     * @param moduleCode 模块编码
     * @param operatorId 操作人ID
     * @param remark 备注
     * @param operationTime 操作时间
     */
    private void insertOperationLog(Long bizId, String operationType, String moduleCode,
                                    Long operatorId, String remark, LocalDateTime operationTime) {
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setUserId(operatorId);
        operationLog.setOperationType(operationType);
        operationLog.setModuleCode(moduleCode);
        operationLog.setBizId(bizId);
        operationLog.setOperationTime(operationTime);
        operationLog.setCreatedBy(operatorId);
        operationLog.setCreatedTime(operationTime);
        operationLog.setRemark(remark);
        operationLog.setDeletedFlag(0);
        if (sysOperationLogMapper.insert(operationLog) == 0) {
            throw new BusinessException(ResultCode.FAILED);
        }
    }

    private BusinessException business(ResultCode code, String message) {
        return new BusinessException(code.getCode(), message);
    }
}
