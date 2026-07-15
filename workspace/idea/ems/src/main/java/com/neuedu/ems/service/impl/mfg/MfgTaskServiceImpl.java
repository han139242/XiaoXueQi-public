package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.mfg.MfgTaskService;
import com.neuedu.ems.service.sys.DataScopeService;
import com.neuedu.ems.exception.DataScopeAccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

/**
 * 生产任务管理 Service 实现类。
 */
@Service
public class MfgTaskServiceImpl extends ServiceImpl<MfgTaskMapper, MfgTask> implements MfgTaskService {

    @Autowired(required = false)
    private DataScopeService dataScopeService;

    @Autowired(required = false)
    private MfgResponsibilityNotificationService responsibilityNotificationService;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private MfgWorkReportMapper workReportMapper;

    /**
     * 分页查询生产任务列表，支持任务编号、状态、派工单、工单、工序和工位筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配任务编号）
     * @param status 任务状态
     * @param dispatchId 派工单ID
     * @param workOrderId 工单ID
     * @param operationId 工序ID
     * @param stationId 工位ID
     * @param employeeId 已分配员工ID
     * @param sort 创建时间排序
     * @return 分页结果
     */
    @Override
    public Page<MfgTask> pageTasks(Integer current, Integer size, String keyword, String status,
                                   Long dispatchId, Long workOrderId, Long operationId, Long stationId, Long employeeId, String sort) {
        Page<MfgTask> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildTaskQuery(keyword, status, dispatchId, workOrderId, operationId, stationId, employeeId, sort));
    }

    /**
     * 查询生产任务列表，支持任务编号、状态、派工单、工单、工序和工位筛选。
     * @param keyword 关键字（匹配任务编号）
     * @param status 任务状态
     * @param dispatchId 派工单ID
     * @param workOrderId 工单ID
     * @param operationId 工序ID
     * @param stationId 工位ID
     * @param employeeId 已分配员工ID
     * @param sort 创建时间排序
     * @return 生产任务列表
     */
    @Override
    public List<MfgTask> listTasks(String keyword, String status, Long dispatchId, Long workOrderId, Long operationId, Long stationId, Long employeeId, String sort) {
        return baseMapper.selectList(buildTaskQuery(keyword, status, dispatchId, workOrderId, operationId, stationId, employeeId, sort));
    }

    /**
     * 根据生产任务ID查询未删除的生产任务信息。
     * @param taskId 生产任务ID
     * @return 生产任务实体
     */
    @Override
    public MfgTask getTaskById(Long taskId) {
        LambdaQueryWrapper<MfgTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgTask::getTaskId, taskId)
                .eq(MfgTask::getDeletedFlag, 0);
        applyDataScope(queryWrapper);
        MfgTask task = baseMapper.selectOne(queryWrapper);
        if (task == null && dataScopeService != null && dataScopeService.isRestricted()
                && baseMapper.selectCount(new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getTaskId, taskId).eq(MfgTask::getDeletedFlag, 0)) > 0) {
            throw new DataScopeAccessDeniedException();
        }
        return task;
    }

    /**
     * 新增生产任务，自动设置删除标志为0。
     * @param task 生产任务实体
     * @return 是否成功
     */
    @Override
    public boolean saveTask(MfgTask task) {
        throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(),
                "生产任务必须由派工单按工艺路线生成，不能脱离来源手工新增");
    }

    /**
     * 更新生产任务信息，不允许修改删除标志。
     * @param task 生产任务实体
     * @return 是否成功
     */
    @Override
    public boolean updateTask(MfgTask task) {
        if (task.getTaskId() == null) {
            return false;
        }
        MfgTask existing = getTaskById(task.getTaskId());
        if (existing == null || !BusinessStatus.Production.WAIT_START.equals(existing.getStatus())) {
            return false;
        }
        task.setDeletedFlag(null);
        task.setStatus(null);
        task.setCompletedQuantity(null);
        task.setDefectQuantity(null);
        task.setTaskNo(null);
        task.setDispatchId(null);
        task.setWorkOrderId(null);
        task.setOperationId(null);
        task.setPlannedQuantity(null);
        if (task.getVersion() == null) {
            task.setVersion(existing.getVersion());
        }
        return baseMapper.update(task, taskInState(task.getTaskId(), existing.getStatus())) > 0;
    }

    /**
     * 根据ID删除生产任务（软删除，将删除标志设为1）。
     * @param taskId 生产任务ID
     * @return 是否成功
     */
    @Override
    public boolean removeTaskById(Long taskId) {
        MfgTask existing = getTaskById(taskId);
        if (existing == null || !BusinessStatus.Production.WAIT_START.equals(existing.getStatus())) {
            return false;
        }
        MfgTask task = new MfgTask();
        task.setTaskId(taskId);
        task.setDeletedFlag(1);
        task.setVersion(existing.getVersion());
        return baseMapper.update(task, taskInState(taskId, existing.getStatus())) > 0;
    }

    /**
     * 开始生产任务，将任务状态改为RUNNING。
     * @param taskId 生产任务ID
     * @param stationId 工位ID，可选
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean startTask(Long taskId, Long stationId, Long operatorId) {
        MfgTask existing = getTaskById(taskId);
        if (existing == null || operatorId == null || !BusinessStatus.Production.WAIT_START.equals(existing.getStatus())) {
            return false;
        }
        MfgTask task = new MfgTask();
        task.setTaskId(taskId);
        task.setVersion(existing.getVersion());
        if (stationId != null) {
            task.setStationId(stationId);
        }
        task.setStatus(BusinessStatus.Production.RUNNING);
        boolean updated = baseMapper.update(task, taskInState(taskId, existing.getStatus())) > 0;
        if (updated) {
            LocalDateTime now = LocalDateTime.now();
            insertStatusLog(taskId, existing.getStatus(), BusinessStatus.Production.RUNNING,
                    "START_TASK", operatorId, null, now);
            insertOperationLog(taskId, "START_TASK", operatorId, null, now);
            if (responsibilityNotificationService != null) {
                responsibilityNotificationService.cancelOpenTaskNotifications(taskId, operatorId);
            }
        }
        return updated;
    }

    /**
     * 完成生产任务，记录完成数量和不良数量并将状态改为FINISHED。
     * @param taskId 生产任务ID
     * @param completedQuantity 完成数量
     * @param defectQuantity 不良数量
     * @param remark 备注
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean finishTask(Long taskId, BigDecimal completedQuantity, BigDecimal defectQuantity, String remark,
                              Long operatorId) {
        MfgTask existing = getTaskById(taskId);
        boolean finishable = existing != null
                && (BusinessStatus.Production.RUNNING.equals(existing.getStatus())
                || BusinessStatus.Production.PAUSED.equals(existing.getStatus()));
        if (!finishable || operatorId == null || completedQuantity == null || completedQuantity.signum() < 0
                || defectQuantity == null || defectQuantity.signum() < 0) {
            return false;
        }
        MfgTask task = new MfgTask();
        task.setTaskId(taskId);
        task.setVersion(existing.getVersion());
        task.setCompletedQuantity(completedQuantity);
        task.setDefectQuantity(defectQuantity);
        task.setRemark(remark);
        task.setStatus(BusinessStatus.Production.FINISHED);
        boolean updated = baseMapper.update(task, taskInState(taskId, existing.getStatus())) > 0;
        if (updated) {
            LocalDateTime now = LocalDateTime.now();
            insertStatusLog(taskId, existing.getStatus(), BusinessStatus.Production.FINISHED,
                    "FINISH_TASK", operatorId, remark, now);
            insertOperationLog(taskId, "FINISH_TASK", operatorId, remark, now);
            if (responsibilityNotificationService != null) {
                responsibilityNotificationService.cancelOpenTaskNotifications(taskId, operatorId);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawTaskStart(Long taskId, Long operatorId, String remark) {
        if (taskId == null || operatorId == null || !StringUtils.hasText(remark)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "生产任务、操作人和撤回原因不能为空");
        }
        MfgTask existing = getTaskById(taskId);
        boolean started = existing != null && (BusinessStatus.Production.RUNNING.equals(existing.getStatus())
                || BusinessStatus.Production.PAUSED.equals(existing.getStatus()));
        if (!started || positive(existing.getCompletedQuantity()) || positive(existing.getDefectQuantity())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "只有未产生数量的已开工任务可以撤回");
        }
        long reports = workReportMapper.selectCount(new LambdaQueryWrapper<com.neuedu.ems.entity.mfg.MfgWorkReport>()
                .eq(com.neuedu.ems.entity.mfg.MfgWorkReport::getTaskId, taskId)
                .eq(com.neuedu.ems.entity.mfg.MfgWorkReport::getDeletedFlag, 0)
                .ne(com.neuedu.ems.entity.mfg.MfgWorkReport::getAuditStatus, BusinessStatus.Document.WITHDRAWN));
        if (reports > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "任务已有有效报工，不能撤回开工");
        }
        MfgTask update = new MfgTask();
        update.setTaskId(taskId);
        update.setVersion(existing.getVersion());
        update.setStatus(BusinessStatus.Production.WAIT_START);
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(LocalDateTime.now());
        if (baseMapper.update(update, taskInState(taskId, existing.getStatus())) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "生产任务已被其他用户修改，请刷新后重试");
        }
        LocalDateTime now = LocalDateTime.now();
        insertStatusLog(taskId, existing.getStatus(), BusinessStatus.Production.WAIT_START,
                "WITHDRAW_TASK_START", operatorId, remark, now);
        insertOperationLog(taskId, "WITHDRAW_TASK_START", operatorId, remark, now);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    /**
     * 构建带当前状态的更新条件，避免并发请求重复流转同一任务。
     */
    private LambdaUpdateWrapper<MfgTask> taskInState(Long taskId, String currentStatus) {
        LambdaUpdateWrapper<MfgTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MfgTask::getTaskId, taskId)
                .eq(MfgTask::getDeletedFlag, 0)
                .eq(MfgTask::getStatus, currentStatus);
        return updateWrapper;
    }

    /**
     * 构建生产任务查询条件，默认过滤已删除记录。
     * @param keyword 关键字（匹配任务编号）
     * @param status 任务状态
     * @param dispatchId 派工单ID
     * @param workOrderId 工单ID
     * @param operationId 工序ID
     * @param stationId 工位ID
     * @param employeeId 已分配员工ID
     * @param sort 创建时间排序
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<MfgTask> buildTaskQuery(String keyword, String status,
                                                       Long dispatchId, Long workOrderId, Long operationId, Long stationId, Long employeeId, String sort) {
        LambdaQueryWrapper<MfgTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgTask::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), MfgTask::getTaskNo, keyword)
                .eq(StringUtils.hasText(status), MfgTask::getStatus, status)
                .eq(dispatchId != null, MfgTask::getDispatchId, dispatchId)
                .eq(workOrderId != null, MfgTask::getWorkOrderId, workOrderId)
                .eq(operationId != null, MfgTask::getOperationId, operationId)
                .eq(stationId != null, MfgTask::getStationId, stationId)
                .inSql(employeeId != null, MfgTask::getTaskId,
                        "SELECT task_id FROM mfg_task_operator WHERE employee_id = " + employeeId + " AND deleted_flag = 0");
        applyDataScope(queryWrapper);
        if ("oldest".equalsIgnoreCase(sort)) {
            queryWrapper.orderByAsc(MfgTask::getCreatedTime);
        } else {
            queryWrapper.orderByDesc(MfgTask::getCreatedTime);
        }
        return queryWrapper;
    }

    private void applyDataScope(LambdaQueryWrapper<MfgTask> queryWrapper) {
        if (dataScopeService != null) {
            dataScopeService.applyTaskScope(queryWrapper);
        }
    }

    private void insertStatusLog(Long bizId, String oldStatus, String newStatus, String reason,
                                 Long operatorId, String remark, LocalDateTime now) {
        SysStatusChangeLog log = new SysStatusChangeLog();
        log.setBizType("MFG_TASK");
        log.setBizId(bizId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setChangeReason(reason);
        log.setChangedBy(operatorId);
        log.setChangedTime(now);
        log.setRemark(remark);
        log.setCreatedBy(operatorId);
        log.setCreatedTime(now);
        log.setUpdatedBy(operatorId);
        log.setUpdatedTime(now);
        log.setDeletedFlag(0);
        if (sysStatusChangeLogMapper.insert(log) == 0) throw new BusinessException(ResultCode.FAILED);
    }

    private void insertOperationLog(Long bizId, String type, Long operatorId, String remark, LocalDateTime now) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(operatorId);
        log.setOperationType(type);
        log.setModuleCode("mfg_task");
        log.setBizId(bizId);
        log.setOperationTime(now);
        log.setRemark(remark);
        log.setCreatedBy(operatorId);
        log.setCreatedTime(now);
        log.setUpdatedBy(operatorId);
        log.setUpdatedTime(now);
        log.setDeletedFlag(0);
        if (sysOperationLogMapper.insert(log) == 0) throw new BusinessException(ResultCode.FAILED);
    }
}
