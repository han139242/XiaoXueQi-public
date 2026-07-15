package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgTaskOperator;
import com.neuedu.ems.mapper.mfg.MfgTaskOperatorMapper;
import com.neuedu.ems.service.mfg.MfgTaskOperatorService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.time.LocalDateTime;

/**
 * 任务人员分配管理 Service 实现类。
 */
@Service
public class MfgTaskOperatorServiceImpl extends ServiceImpl<MfgTaskOperatorMapper, MfgTaskOperator> implements MfgTaskOperatorService {

    @Autowired(required = false)
    private MfgResponsibilityNotificationService notificationService;

    /**
     * 分页查询任务人员分配列表，支持任务、员工和任务角色筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param taskId 任务ID
     * @param employeeId 员工ID
     * @param roleInTask 任务角色
     * @return 分页结果
     */
    @Override
    public Page<MfgTaskOperator> pageTaskOperators(Integer current, Integer size, Long taskId, Long employeeId, String roleInTask) {
        Page<MfgTaskOperator> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildTaskOperatorQuery(taskId, employeeId, roleInTask));
    }

    /**
     * 查询任务人员分配列表，支持任务、员工和任务角色筛选。
     * @param taskId 任务ID
     * @param employeeId 员工ID
     * @param roleInTask 任务角色
     * @return 任务人员分配列表
     */
    @Override
    public List<MfgTaskOperator> listTaskOperators(Long taskId, Long employeeId, String roleInTask) {
        return baseMapper.selectList(buildTaskOperatorQuery(taskId, employeeId, roleInTask));
    }

    /**
     * 根据任务ID查询未删除的任务人员分配列表。
     * @param taskId 任务ID
     * @return 任务人员分配列表
     */
    @Override
    public List<MfgTaskOperator> listTaskOperatorsByTaskId(Long taskId) {
        return baseMapper.selectList(buildTaskOperatorQuery(taskId, null, null));
    }

    /**
     * 根据任务人员分配ID查询未删除的任务人员分配详情。
     * @param id 任务人员分配ID
     * @return 任务人员分配实体
     */
    @Override
    public MfgTaskOperator getTaskOperatorById(Long id) {
        LambdaQueryWrapper<MfgTaskOperator> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgTaskOperator::getId, id)
                .eq(MfgTaskOperator::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增任务人员分配，自动设置删除标志为0。
     * @param taskOperator 任务人员分配实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean saveTaskOperator(MfgTaskOperator taskOperator, Long sourceUserId) {
        if (taskOperator.getDeletedFlag() == null) {
            taskOperator.setDeletedFlag(0);
        }
        LocalDateTime now = LocalDateTime.now();
        if (taskOperator.getAssignedTime() == null) {
            taskOperator.setAssignedTime(now);
        }
        if (taskOperator.getCreatedBy() == null) {
            taskOperator.setCreatedBy(sourceUserId);
        }
        if (taskOperator.getCreatedTime() == null) {
            taskOperator.setCreatedTime(now);
        }
        boolean inserted = baseMapper.insert(taskOperator) > 0;
        if (inserted && notificationService != null) {
            notificationService.cancelTaskUnassigned(taskOperator.getTaskId(), sourceUserId);
            notificationService.publishTaskAssigned(taskOperator.getTaskId(), taskOperator.getEmployeeId(), sourceUserId);
        }
        return inserted;
    }

    /**
     * 更新任务人员分配信息，不允许修改删除标志。
     * @param taskOperator 任务人员分配实体
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean updateTaskOperator(MfgTaskOperator taskOperator, Long sourceUserId) {
        if (taskOperator.getId() == null) {
            return false;
        }
        MfgTaskOperator existing = getTaskOperatorById(taskOperator.getId());
        taskOperator.setDeletedFlag(null);
        boolean updated = baseMapper.updateById(taskOperator) > 0;
        if (updated && notificationService != null) {
            Long taskId = taskOperator.getTaskId() == null && existing != null ? existing.getTaskId() : taskOperator.getTaskId();
            Long employeeId = taskOperator.getEmployeeId() == null && existing != null
                    ? existing.getEmployeeId() : taskOperator.getEmployeeId();
            if (existing != null && existing.getEmployeeId() != null && !existing.getEmployeeId().equals(employeeId)) {
                notificationService.cancelTaskAssignment(existing.getTaskId(), existing.getEmployeeId(), sourceUserId);
            }
            notificationService.cancelTaskUnassigned(taskId, sourceUserId);
            notificationService.publishTaskAssigned(taskId, employeeId, sourceUserId);
        }
        return updated;
    }

    /**
     * 根据ID删除任务人员分配（软删除，将删除标志设为1）。
     * @param id 任务人员分配ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean removeTaskOperatorById(Long id, Long sourceUserId) {
        MfgTaskOperator existing = getTaskOperatorById(id);
        MfgTaskOperator taskOperator = new MfgTaskOperator();
        taskOperator.setId(id);
        taskOperator.setDeletedFlag(1);
        boolean removed = baseMapper.updateById(taskOperator) > 0;
        if (removed && notificationService != null && existing != null) {
            notificationService.cancelTaskAssignment(existing.getTaskId(), existing.getEmployeeId(), sourceUserId);
        }
        return removed;
    }

    /**
     * 构建任务人员分配查询条件，默认过滤已删除记录。
     * @param taskId 任务ID
     * @param employeeId 员工ID
     * @param roleInTask 任务角色
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<MfgTaskOperator> buildTaskOperatorQuery(Long taskId, Long employeeId, String roleInTask) {
        LambdaQueryWrapper<MfgTaskOperator> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgTaskOperator::getDeletedFlag, 0)
                .eq(taskId != null, MfgTaskOperator::getTaskId, taskId)
                .eq(employeeId != null, MfgTaskOperator::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(roleInTask), MfgTaskOperator::getRoleInTask, roleInTask)
                .orderByDesc(MfgTaskOperator::getCreatedTime);
        return queryWrapper;
    }
}
