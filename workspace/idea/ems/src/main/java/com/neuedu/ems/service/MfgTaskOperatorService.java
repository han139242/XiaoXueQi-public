package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.MfgTaskOperator;

import java.util.List;

/**
 * 任务人员分配管理 Service 接口。
 */
public interface MfgTaskOperatorService extends IService<MfgTaskOperator> {

    /**
     * 分页查询任务人员分配列表，支持任务、员工和任务角色筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param taskId 任务ID
     * @param employeeId 员工ID
     * @param roleInTask 任务角色
     * @return 分页结果
     */
    Page<MfgTaskOperator> pageTaskOperators(Integer current, Integer size, Long taskId, Long employeeId, String roleInTask);

    /**
     * 查询任务人员分配列表，支持任务、员工和任务角色筛选。
     * @param taskId 任务ID
     * @param employeeId 员工ID
     * @param roleInTask 任务角色
     * @return 任务人员分配列表
     */
    List<MfgTaskOperator> listTaskOperators(Long taskId, Long employeeId, String roleInTask);

    /**
     * 根据任务ID查询任务人员分配列表。
     * @param taskId 任务ID
     * @return 任务人员分配列表
     */
    List<MfgTaskOperator> listTaskOperatorsByTaskId(Long taskId);

    /**
     * 根据任务人员分配ID查询详情。
     * @param id 任务人员分配ID
     * @return 任务人员分配实体
     */
    MfgTaskOperator getTaskOperatorById(Long id);

    /**
     * 新增任务人员分配。
     * @param taskOperator 任务人员分配实体
     * @return 是否成功
     */
    boolean saveTaskOperator(MfgTaskOperator taskOperator);

    /**
     * 更新任务人员分配信息。
     * @param taskOperator 任务人员分配实体
     * @return 是否成功
     */
    boolean updateTaskOperator(MfgTaskOperator taskOperator);

    /**
     * 根据ID删除任务人员分配（软删除）。
     * @param id 任务人员分配ID
     * @return 是否成功
     */
    boolean removeTaskOperatorById(Long id);
}
