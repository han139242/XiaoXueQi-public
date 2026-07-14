package com.neuedu.ems.service.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mfg.MfgTask;

import java.math.BigDecimal;
import java.util.List;

/**
 * 生产任务管理 Service 接口。
 */
public interface MfgTaskService extends IService<MfgTask> {

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
    Page<MfgTask> pageTasks(Integer current, Integer size, String keyword, String status,
                            Long dispatchId, Long workOrderId, Long operationId, Long stationId, Long employeeId, String sort);

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
    List<MfgTask> listTasks(String keyword, String status, Long dispatchId, Long workOrderId, Long operationId, Long stationId, Long employeeId, String sort);

    /**
     * 根据生产任务ID查询生产任务信息。
     * @param taskId 生产任务ID
     * @return 生产任务实体
     */
    MfgTask getTaskById(Long taskId);

    /**
     * 新增生产任务。
     * @param task 生产任务实体
     * @return 是否成功
     */
    boolean saveTask(MfgTask task);

    /**
     * 更新生产任务信息。
     * @param task 生产任务实体
     * @return 是否成功
     */
    boolean updateTask(MfgTask task);

    /**
     * 根据ID删除生产任务（软删除）。
     * @param taskId 生产任务ID
     * @return 是否成功
     */
    boolean removeTaskById(Long taskId);

    /**
     * 开始生产任务，将任务状态改为RUNNING。
     * @param taskId 生产任务ID
     * @param stationId 工位ID，可选
     * @return 是否成功
     */
    boolean startTask(Long taskId, Long stationId);

    /**
     * 完成生产任务，记录完成数量和不良数量并将状态改为FINISHED。
     * @param taskId 生产任务ID
     * @param completedQuantity 完成数量
     * @param defectQuantity 不良数量
     * @param remark 备注
     * @return 是否成功
     */
    boolean finishTask(Long taskId, BigDecimal completedQuantity, BigDecimal defectQuantity, String remark);
}
