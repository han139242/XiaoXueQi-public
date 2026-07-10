package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.MfgException;

import java.util.List;

/**
 * 生产异常管理 Service 接口。
 */
public interface MfgExceptionService extends IService<MfgException> {

    /**
     * 分页查询生产异常列表，支持异常编号、类型、状态、任务和工位筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配异常编号）
     * @param exceptionType 异常类型
     * @param status 异常状态
     * @param taskId 任务ID
     * @param stationId 工位ID
     * @return 分页结果
     */
    Page<MfgException> pageExceptions(Integer current, Integer size, String keyword, String exceptionType,
                                      String status, Long taskId, Long stationId);

    /**
     * 查询生产异常列表，支持异常编号、类型、状态、任务和工位筛选。
     * @param keyword 关键字（匹配异常编号）
     * @param exceptionType 异常类型
     * @param status 异常状态
     * @param taskId 任务ID
     * @param stationId 工位ID
     * @return 生产异常列表
     */
    List<MfgException> listExceptions(String keyword, String exceptionType, String status, Long taskId, Long stationId);

    /**
     * 根据生产异常ID查询详情。
     * @param exceptionId 生产异常ID
     * @return 生产异常实体
     */
    MfgException getExceptionById(Long exceptionId);

    /**
     * 新增生产异常。
     * @param exception 生产异常实体
     * @return 是否成功
     */
    boolean saveException(MfgException exception);

    /**
     * 更新生产异常信息。
     * @param exception 生产异常实体
     * @return 是否成功
     */
    boolean updateException(MfgException exception);

    /**
     * 根据ID删除生产异常（软删除）。
     * @param exceptionId 生产异常ID
     * @return 是否成功
     */
    boolean removeExceptionById(Long exceptionId);

    /**
     * 处理生产异常，记录处理结果并将状态改为CLOSED。
     * @param exceptionId 生产异常ID
     * @param handleResult 处理结果
     * @param remark 备注
     * @return 是否成功
     */
    boolean handleException(Long exceptionId, String handleResult, String remark);
}
