package com.neuedu.ems.service.impl.qms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.qms.QmsNonconformingDetail;
import com.neuedu.ems.entity.qms.QmsNonconformingOrder;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.mapper.qms.QmsNonconformingDetailMapper;
import com.neuedu.ems.mapper.qms.QmsNonconformingOrderMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.qms.QmsNonconformingOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 不合格品处理单 Service 实现类。
 */
@Service
public class QmsNonconformingOrderServiceImpl extends ServiceImpl<QmsNonconformingOrderMapper, QmsNonconformingOrder> implements QmsNonconformingOrderService {

    private static final String SUBMITTED_STATUS = "SUBMITTED";
    private static final String APPROVED_STATUS = "APPROVED";
    private static final String CLOSED_STATUS = "CLOSED";

    @Autowired
    private QmsNonconformingDetailMapper qmsNonconformingDetailMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 分页查询不合格品处理单，支持单号、检验单、产品批次、处理方式、审批状态和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 处理单号关键字
     * @param inspectionId 检验单ID
     * @param productLotId 产品批次ID
     * @param dispositionType 处理方式
     * @param approvalStatus 审批状态
     * @param status 状态
     * @return 不合格品处理单分页结果
     */
    @Override
    public Page<QmsNonconformingOrder> pageNonconformingOrders(Integer current, Integer size, String keyword, Long inspectionId,
                                                               Long productLotId, String dispositionType, String approvalStatus, String status) {
        Page<QmsNonconformingOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildNonconformingOrderQuery(keyword, inspectionId, productLotId, dispositionType, approvalStatus, status));
    }

    /**
     * 查询不合格品处理单列表，支持单号、检验单、产品批次、处理方式、审批状态和状态筛选。
     * @param keyword 处理单号关键字
     * @param inspectionId 检验单ID
     * @param productLotId 产品批次ID
     * @param dispositionType 处理方式
     * @param approvalStatus 审批状态
     * @param status 状态
     * @return 不合格品处理单列表
     */
    @Override
    public List<QmsNonconformingOrder> listNonconformingOrders(String keyword, Long inspectionId, Long productLotId,
                                                               String dispositionType, String approvalStatus, String status) {
        return baseMapper.selectList(buildNonconformingOrderQuery(keyword, inspectionId, productLotId, dispositionType, approvalStatus, status));
    }

    /**
     * 根据ID查询未删除的不合格品处理单。
     * @param ncrId 不合格品处理单ID
     * @return 不合格品处理单实体
     */
    @Override
    public QmsNonconformingOrder getNonconformingOrderById(Long ncrId) {
        LambdaQueryWrapper<QmsNonconformingOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsNonconformingOrder::getNcrId, ncrId)
                .eq(QmsNonconformingOrder::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增不合格品处理单，自动补齐编号、审批状态、状态和删除标记。
     * @param nonconformingOrder 不合格品处理单实体
     * @return 是否成功
     */
    @Override
    public boolean saveNonconformingOrder(QmsNonconformingOrder nonconformingOrder) {
        prepareNonconformingOrder(nonconformingOrder);
        return baseMapper.insert(nonconformingOrder) > 0;
    }

    /**
     * 新增不合格品处理单和处理明细。
     * @param nonconformingOrder 不合格品处理单实体
     * @param details 处理明细列表
     * @return 不合格品处理单ID
     */
    @Override
    @Transactional
    public Long saveNonconformingOrderFull(QmsNonconformingOrder nonconformingOrder, List<QmsNonconformingDetail> details) {
        saveNonconformingOrder(nonconformingOrder);
        insertDetails(nonconformingOrder.getNcrId(), details);
        return nonconformingOrder.getNcrId();
    }

    /**
     * 修改不合格品处理单，已审批或已关闭处理单不允许普通修改。
     * @param nonconformingOrder 不合格品处理单实体
     * @return 是否成功
     */
    @Override
    public boolean updateNonconformingOrder(QmsNonconformingOrder nonconformingOrder) {
        if (nonconformingOrder.getNcrId() == null) {
            return false;
        }
        QmsNonconformingOrder existing = getNonconformingOrderById(nonconformingOrder.getNcrId());
        if (existing == null || APPROVED_STATUS.equals(existing.getApprovalStatus()) || CLOSED_STATUS.equals(existing.getStatus())) {
            return false;
        }
        nonconformingOrder.setDeletedFlag(null);
        return baseMapper.updateById(nonconformingOrder) > 0;
    }

    /**
     * 修改不合格品处理单并替换处理明细，已审批或已关闭处理单不允许替换明细。
     * @param nonconformingOrder 不合格品处理单实体
     * @param details 处理明细列表
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateNonconformingOrderFull(QmsNonconformingOrder nonconformingOrder, List<QmsNonconformingDetail> details) {
        if (!updateNonconformingOrder(nonconformingOrder)) {
            return false;
        }
        if (details != null) {
            softDeleteDetails(nonconformingOrder.getNcrId());
            insertDetails(nonconformingOrder.getNcrId(), details);
        }
        return true;
    }

    /**
     * 根据ID软删除不合格品处理单和明细，已审批或已关闭处理单不允许删除。
     * @param ncrId 不合格品处理单ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean removeNonconformingOrderById(Long ncrId) {
        QmsNonconformingOrder existing = getNonconformingOrderById(ncrId);
        if (existing == null || APPROVED_STATUS.equals(existing.getApprovalStatus()) || CLOSED_STATUS.equals(existing.getStatus())) {
            return false;
        }
        QmsNonconformingOrder order = new QmsNonconformingOrder();
        order.setNcrId(ncrId);
        order.setDeletedFlag(1);
        boolean removed = baseMapper.updateById(order) > 0;
        softDeleteDetails(ncrId);
        return removed;
    }

    /**
     * 审批不合格品处理单，审批结果使用APPROVED或REJECTED。
     * @param ncrId 不合格品处理单ID
     * @param approvalResult 审批结果
     * @param approvalComment 审批备注
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean approveNonconformingOrder(Long ncrId, String approvalResult, String approvalComment, Long operatorId) {
        if (!StringUtils.hasText(approvalResult)) {
            return false;
        }
        QmsNonconformingOrder existing = getNonconformingOrderById(ncrId);
        if (existing == null || APPROVED_STATUS.equals(existing.getApprovalStatus()) || CLOSED_STATUS.equals(existing.getStatus())) {
            return false;
        }
        QmsNonconformingOrder update = new QmsNonconformingOrder();
        update.setNcrId(ncrId);
        update.setApprovalStatus(approvalResult);
        update.setStatus(approvalResult);
        update.setRemark(StringUtils.hasText(approvalComment) ? approvalComment : existing.getRemark());
        boolean updated = baseMapper.updateById(update) > 0;
        if (updated) {
            LocalDateTime now = LocalDateTime.now();
            insertStatusChangeLog(ncrId, existing.getApprovalStatus(), approvalResult, "QMS_NONCONFORMING_ORDER",
                    "APPROVE_NONCONFORMING", operatorId, approvalComment, now);
            insertOperationLog(ncrId, "APPROVE_NONCONFORMING", "qms_nonconforming_order", operatorId, approvalComment, now);
        }
        return updated;
    }

    /**
     * 构建不合格品处理单查询条件，默认过滤已删除记录。
     * @param keyword 处理单号关键字
     * @param inspectionId 检验单ID
     * @param productLotId 产品批次ID
     * @param dispositionType 处理方式
     * @param approvalStatus 审批状态
     * @param status 状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<QmsNonconformingOrder> buildNonconformingOrderQuery(String keyword, Long inspectionId,
                                                                                   Long productLotId,
                                                                                   String dispositionType,
                                                                                   String approvalStatus,
                                                                                   String status) {
        LambdaQueryWrapper<QmsNonconformingOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsNonconformingOrder::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), QmsNonconformingOrder::getNcrNo, keyword)
                .eq(inspectionId != null, QmsNonconformingOrder::getInspectionId, inspectionId)
                .eq(productLotId != null, QmsNonconformingOrder::getProductLotId, productLotId)
                .eq(StringUtils.hasText(dispositionType), QmsNonconformingOrder::getDispositionType, dispositionType)
                .eq(StringUtils.hasText(approvalStatus), QmsNonconformingOrder::getApprovalStatus, approvalStatus)
                .eq(StringUtils.hasText(status), QmsNonconformingOrder::getStatus, status)
                .orderByDesc(QmsNonconformingOrder::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 补齐不合格品处理单默认值。
     * @param order 不合格品处理单实体
     */
    private void prepareNonconformingOrder(QmsNonconformingOrder order) {
        if (!StringUtils.hasText(order.getNcrNo())) {
            order.setNcrNo("NCR" + System.currentTimeMillis());
        }
        if (!StringUtils.hasText(order.getApprovalStatus())) {
            order.setApprovalStatus(SUBMITTED_STATUS);
        }
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus(SUBMITTED_STATUS);
        }
        if (order.getDeletedFlag() == null) {
            order.setDeletedFlag(0);
        }
    }

    /**
     * 批量插入不合格品处理明细。
     * @param ncrId 不合格品处理单ID
     * @param details 处理明细列表
     */
    private void insertDetails(Long ncrId, List<QmsNonconformingDetail> details) {
        if (details == null) {
            return;
        }
        for (QmsNonconformingDetail detail : details) {
            detail.setDetailId(null);
            detail.setNcrId(ncrId);
            if (detail.getDefectQuantity() == null) {
                detail.setDefectQuantity(BigDecimal.ZERO);
            }
            if (detail.getDeletedFlag() == null) {
                detail.setDeletedFlag(0);
            }
            qmsNonconformingDetailMapper.insert(detail);
        }
    }

    /**
     * 软删除指定不合格品处理单的全部处理明细。
     * @param ncrId 不合格品处理单ID
     */
    private void softDeleteDetails(Long ncrId) {
        LambdaQueryWrapper<QmsNonconformingDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsNonconformingDetail::getNcrId, ncrId)
                .eq(QmsNonconformingDetail::getDeletedFlag, 0);
        List<QmsNonconformingDetail> details = qmsNonconformingDetailMapper.selectList(queryWrapper);
        for (QmsNonconformingDetail detail : details) {
            QmsNonconformingDetail update = new QmsNonconformingDetail();
            update.setDetailId(detail.getDetailId());
            update.setDeletedFlag(1);
            qmsNonconformingDetailMapper.updateById(update);
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
        sysStatusChangeLogMapper.insert(statusLog);
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
        sysOperationLogMapper.insert(operationLog);
    }
}
