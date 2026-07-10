package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgReportMaterial;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.mapper.mfg.MfgReportMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.mfg.MfgWorkReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产报工 Service 实现类。
 */
@Service
public class MfgWorkReportServiceImpl extends ServiceImpl<MfgWorkReportMapper, MfgWorkReport> implements MfgWorkReportService {

    private static final String SUBMITTED_STATUS = "SUBMITTED";
    private static final String APPROVED_STATUS = "APPROVED";

    @Autowired
    private MfgReportMaterialMapper mfgReportMaterialMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 分页查询生产报工，支持编号、任务、人员、工位和审核状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 报工编号关键字
     * @param taskId 任务ID
     * @param operatorId 操作工ID
     * @param stationId 工位ID
     * @param auditStatus 审核状态
     * @return 报工分页结果
     */
    @Override
    public Page<MfgWorkReport> pageWorkReports(Integer current, Integer size, String keyword, Long taskId,
                                               Long operatorId, Long stationId, String auditStatus) {
        Page<MfgWorkReport> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildWorkReportQuery(keyword, taskId, operatorId, stationId, auditStatus));
    }

    /**
     * 查询生产报工列表，支持编号、任务、人员、工位和审核状态筛选。
     * @param keyword 报工编号关键字
     * @param taskId 任务ID
     * @param operatorId 操作工ID
     * @param stationId 工位ID
     * @param auditStatus 审核状态
     * @return 报工列表
     */
    @Override
    public List<MfgWorkReport> listWorkReports(String keyword, Long taskId, Long operatorId, Long stationId, String auditStatus) {
        return baseMapper.selectList(buildWorkReportQuery(keyword, taskId, operatorId, stationId, auditStatus));
    }

    /**
     * 根据ID查询未删除的生产报工。
     * @param reportId 报工ID
     * @return 报工实体
     */
    @Override
    public MfgWorkReport getWorkReportById(Long reportId) {
        LambdaQueryWrapper<MfgWorkReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgWorkReport::getReportId, reportId)
                .eq(MfgWorkReport::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增生产报工，自动补齐编号、数量、时间、状态和删除标记。
     * @param workReport 报工实体
     * @return 是否成功
     */
    @Override
    public boolean saveWorkReport(MfgWorkReport workReport) {
        prepareWorkReport(workReport);
        return baseMapper.insert(workReport) > 0;
    }

    /**
     * 新增报工主表和用料明细。
     * @param workReport 报工实体
     * @param materials 报工用料明细列表
     * @return 报工ID
     */
    @Override
    @Transactional
    public Long saveWorkReportFull(MfgWorkReport workReport, List<MfgReportMaterial> materials) {
        saveWorkReport(workReport);
        insertMaterials(workReport.getReportId(), materials);
        return workReport.getReportId();
    }

    /**
     * 修改生产报工，已审核通过报工不允许普通修改。
     * @param workReport 报工实体
     * @return 是否成功
     */
    @Override
    public boolean updateWorkReport(MfgWorkReport workReport) {
        if (workReport.getReportId() == null) {
            return false;
        }
        MfgWorkReport existing = getWorkReportById(workReport.getReportId());
        if (existing == null || APPROVED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        fillMissingQuantities(workReport, existing);
        workReport.setDeletedFlag(null);
        return baseMapper.updateById(workReport) > 0;
    }

    /**
     * 修改报工主表并替换用料明细，已审核通过报工不允许替换明细。
     * @param workReport 报工实体
     * @param materials 报工用料明细列表
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateWorkReportFull(MfgWorkReport workReport, List<MfgReportMaterial> materials) {
        if (!updateWorkReport(workReport)) {
            return false;
        }
        if (materials != null) {
            softDeleteMaterials(workReport.getReportId());
            insertMaterials(workReport.getReportId(), materials);
        }
        return true;
    }

    /**
     * 根据ID软删除生产报工及用料明细，已审核通过报工不允许删除。
     * @param reportId 报工ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean removeWorkReportById(Long reportId) {
        MfgWorkReport existing = getWorkReportById(reportId);
        if (existing == null || APPROVED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        MfgWorkReport workReport = new MfgWorkReport();
        workReport.setReportId(reportId);
        workReport.setDeletedFlag(1);
        boolean removed = baseMapper.updateById(workReport) > 0;
        softDeleteMaterials(reportId);
        return removed;
    }

    /**
     * 提交生产报工并保存用料明细，状态写为SUBMITTED。
     * @param workReport 报工实体
     * @param materials 报工用料明细列表
     * @return 报工ID
     */
    @Override
    @Transactional
    public Long submitWorkReport(MfgWorkReport workReport, List<MfgReportMaterial> materials) {
        String oldStatus = workReport.getAuditStatus();
        workReport.setAuditStatus(SUBMITTED_STATUS);
        Long reportId = saveWorkReportFull(workReport, materials);
        LocalDateTime now = LocalDateTime.now();
        insertStatusChangeLog(reportId, oldStatus, SUBMITTED_STATUS, "MFG_WORK_REPORT", "SUBMIT_WORK_REPORT",
                workReport.getOperatorId(), workReport.getRemark(), now);
        insertOperationLog(reportId, "SUBMIT_WORK_REPORT", "mfg_work_report", workReport.getOperatorId(), workReport.getRemark(), now);
        return reportId;
    }

    /**
     * 审核生产报工，审核结果使用APPROVED或REJECTED。
     * @param reportId 报工ID
     * @param auditResult 审核结果
     * @param auditComment 审核备注
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean auditWorkReport(Long reportId, String auditResult, String auditComment, Long operatorId) {
        if (!StringUtils.hasText(auditResult)) {
            return false;
        }
        MfgWorkReport existing = getWorkReportById(reportId);
        if (existing == null || APPROVED_STATUS.equals(existing.getAuditStatus())) {
            return false;
        }
        MfgWorkReport update = new MfgWorkReport();
        update.setReportId(reportId);
        update.setAuditStatus(auditResult);
        update.setRemark(StringUtils.hasText(auditComment) ? auditComment : existing.getRemark());
        boolean updated = baseMapper.updateById(update) > 0;
        if (updated) {
            LocalDateTime now = LocalDateTime.now();
            insertStatusChangeLog(reportId, existing.getAuditStatus(), auditResult, "MFG_WORK_REPORT", "AUDIT_WORK_REPORT",
                    operatorId, auditComment, now);
            insertOperationLog(reportId, "AUDIT_WORK_REPORT", "mfg_work_report", operatorId, auditComment, now);
        }
        return updated;
    }

    /**
     * 构建生产报工查询条件，默认过滤已删除记录。
     * @param keyword 报工编号关键字
     * @param taskId 任务ID
     * @param operatorId 操作工ID
     * @param stationId 工位ID
     * @param auditStatus 审核状态
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<MfgWorkReport> buildWorkReportQuery(String keyword, Long taskId, Long operatorId,
                                                                   Long stationId, String auditStatus) {
        LambdaQueryWrapper<MfgWorkReport> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgWorkReport::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), MfgWorkReport::getReportNo, keyword)
                .eq(taskId != null, MfgWorkReport::getTaskId, taskId)
                .eq(operatorId != null, MfgWorkReport::getOperatorId, operatorId)
                .eq(stationId != null, MfgWorkReport::getStationId, stationId)
                .eq(StringUtils.hasText(auditStatus), MfgWorkReport::getAuditStatus, auditStatus)
                .orderByDesc(MfgWorkReport::getReportTime)
                .orderByDesc(MfgWorkReport::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 补齐报工默认值。
     * @param workReport 报工实体
     */
    private void prepareWorkReport(MfgWorkReport workReport) {
        if (!StringUtils.hasText(workReport.getReportNo())) {
            workReport.setReportNo("WR" + System.currentTimeMillis());
        }
        if (workReport.getQualifiedQuantity() == null) {
            workReport.setQualifiedQuantity(BigDecimal.ZERO);
        }
        if (workReport.getDefectQuantity() == null) {
            workReport.setDefectQuantity(BigDecimal.ZERO);
        }
        if (workReport.getActualWorkHours() == null) {
            workReport.setActualWorkHours(BigDecimal.ZERO);
        }
        if (workReport.getReportTime() == null) {
            workReport.setReportTime(LocalDateTime.now());
        }
        if (!StringUtils.hasText(workReport.getAuditStatus())) {
            workReport.setAuditStatus(SUBMITTED_STATUS);
        }
        if (workReport.getDeletedFlag() == null) {
            workReport.setDeletedFlag(0);
        }
    }

    /**
     * 部分更新报工时，用已有数量回填未传字段。
     * @param workReport 请求中的报工实体
     * @param existing 数据库中已有的报工实体
     */
    private void fillMissingQuantities(MfgWorkReport workReport, MfgWorkReport existing) {
        if (workReport.getQualifiedQuantity() == null) {
            workReport.setQualifiedQuantity(existing.getQualifiedQuantity());
        }
        if (workReport.getDefectQuantity() == null) {
            workReport.setDefectQuantity(existing.getDefectQuantity());
        }
        if (workReport.getActualWorkHours() == null) {
            workReport.setActualWorkHours(existing.getActualWorkHours());
        }
    }

    /**
     * 批量插入报工用料明细。
     * @param reportId 报工ID
     * @param materials 报工用料明细列表
     */
    private void insertMaterials(Long reportId, List<MfgReportMaterial> materials) {
        if (materials == null) {
            return;
        }
        for (MfgReportMaterial material : materials) {
            material.setId(null);
            material.setReportId(reportId);
            if (material.getUsedQuantity() == null) {
                material.setUsedQuantity(BigDecimal.ZERO);
            }
            if (material.getScrapQuantity() == null) {
                material.setScrapQuantity(BigDecimal.ZERO);
            }
            if (material.getDeletedFlag() == null) {
                material.setDeletedFlag(0);
            }
            mfgReportMaterialMapper.insert(material);
        }
    }

    /**
     * 软删除指定报工的全部用料明细。
     * @param reportId 报工ID
     */
    private void softDeleteMaterials(Long reportId) {
        LambdaQueryWrapper<MfgReportMaterial> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgReportMaterial::getReportId, reportId)
                .eq(MfgReportMaterial::getDeletedFlag, 0);
        List<MfgReportMaterial> materials = mfgReportMaterialMapper.selectList(queryWrapper);
        for (MfgReportMaterial material : materials) {
            MfgReportMaterial update = new MfgReportMaterial();
            update.setId(material.getId());
            update.setDeletedFlag(1);
            mfgReportMaterialMapper.updateById(update);
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
