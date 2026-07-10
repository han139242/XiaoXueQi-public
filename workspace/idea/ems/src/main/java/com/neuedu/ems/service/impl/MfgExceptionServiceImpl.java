package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.MfgException;
import com.neuedu.ems.mapper.MfgExceptionMapper;
import com.neuedu.ems.service.MfgExceptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 生产异常管理 Service 实现类。
 */
@Service
public class MfgExceptionServiceImpl extends ServiceImpl<MfgExceptionMapper, MfgException> implements MfgExceptionService {

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
    @Override
    public Page<MfgException> pageExceptions(Integer current, Integer size, String keyword, String exceptionType,
                                             String status, Long taskId, Long stationId) {
        Page<MfgException> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildExceptionQuery(keyword, exceptionType, status, taskId, stationId));
    }

    /**
     * 查询生产异常列表，支持异常编号、类型、状态、任务和工位筛选。
     * @param keyword 关键字（匹配异常编号）
     * @param exceptionType 异常类型
     * @param status 异常状态
     * @param taskId 任务ID
     * @param stationId 工位ID
     * @return 生产异常列表
     */
    @Override
    public List<MfgException> listExceptions(String keyword, String exceptionType, String status, Long taskId, Long stationId) {
        return baseMapper.selectList(buildExceptionQuery(keyword, exceptionType, status, taskId, stationId));
    }

    /**
     * 根据生产异常ID查询未删除的生产异常详情。
     * @param exceptionId 生产异常ID
     * @return 生产异常实体
     */
    @Override
    public MfgException getExceptionById(Long exceptionId) {
        LambdaQueryWrapper<MfgException> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgException::getExceptionId, exceptionId)
                .eq(MfgException::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增生产异常，自动设置删除标志为0。
     * @param exception 生产异常实体
     * @return 是否成功
     */
    @Override
    public boolean saveException(MfgException exception) {
        if (exception.getDeletedFlag() == null) {
            exception.setDeletedFlag(0);
        }
        return baseMapper.insert(exception) > 0;
    }

    /**
     * 更新生产异常信息，不允许修改删除标志。
     * @param exception 生产异常实体
     * @return 是否成功
     */
    @Override
    public boolean updateException(MfgException exception) {
        if (exception.getExceptionId() == null) {
            return false;
        }
        exception.setDeletedFlag(null);
        return baseMapper.updateById(exception) > 0;
    }

    /**
     * 根据ID删除生产异常（软删除，将删除标志设为1）。
     * @param exceptionId 生产异常ID
     * @return 是否成功
     */
    @Override
    public boolean removeExceptionById(Long exceptionId) {
        MfgException exception = new MfgException();
        exception.setExceptionId(exceptionId);
        exception.setDeletedFlag(1);
        return baseMapper.updateById(exception) > 0;
    }

    /**
     * 处理生产异常，记录处理结果并将状态改为CLOSED。
     * @param exceptionId 生产异常ID
     * @param handleResult 处理结果
     * @param remark 备注
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean handleException(Long exceptionId, String handleResult, String remark) {
        MfgException exception = new MfgException();
        exception.setExceptionId(exceptionId);
        exception.setHandleResult(handleResult);
        exception.setRemark(remark);
        exception.setStatus("CLOSED");
        return baseMapper.updateById(exception) > 0;
    }

    /**
     * 构建生产异常查询条件，默认过滤已删除记录。
     * @param keyword 关键字（匹配异常编号）
     * @param exceptionType 异常类型
     * @param status 异常状态
     * @param taskId 任务ID
     * @param stationId 工位ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<MfgException> buildExceptionQuery(String keyword, String exceptionType,
                                                                 String status, Long taskId, Long stationId) {
        LambdaQueryWrapper<MfgException> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgException::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), MfgException::getExceptionNo, keyword)
                .eq(StringUtils.hasText(exceptionType), MfgException::getExceptionType, exceptionType)
                .eq(StringUtils.hasText(status), MfgException::getStatus, status)
                .eq(taskId != null, MfgException::getTaskId, taskId)
                .eq(stationId != null, MfgException::getStationId, stationId)
                .orderByDesc(MfgException::getCreatedTime);
        return queryWrapper;
    }
}
