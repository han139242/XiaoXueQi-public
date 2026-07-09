package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.MfgDispatchOrder;
import com.neuedu.ems.mapper.MfgDispatchOrderMapper;
import com.neuedu.ems.service.MfgDispatchOrderService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 派工单业务逻辑实现类。
 * 实现派工单的分页查询、条件查询、新增、更新和删除功能。
 */
@Service
public class MfgDispatchOrderServiceImpl extends ServiceImpl<MfgDispatchOrderMapper, MfgDispatchOrder> implements MfgDispatchOrderService {

    /**
     * 分页查询派工单，支持关键字、状态、工单ID和班组ID筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配派工单编号），可选
     * @param status 派工单状态（待派发、已派发、已接受、生产中、已完成、已驳回），可选
     * @param workOrderId 工单ID，可选
     * @param teamId 班组ID，可选
     * @return 分页结果
     */
    @Override
    public Page<MfgDispatchOrder> pageDispatchOrders(Integer current, Integer size, String keyword, String status, Long workOrderId, Long teamId) {
        Page<MfgDispatchOrder> page = new Page<>(current, size);
        LambdaQueryWrapper<MfgDispatchOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgDispatchOrder::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), MfgDispatchOrder::getDispatchNo, keyword)
                .eq(StringUtils.hasText(status), MfgDispatchOrder::getStatus, status)
                .eq(workOrderId != null, MfgDispatchOrder::getWorkOrderId, workOrderId)
                .eq(teamId != null, MfgDispatchOrder::getTeamId, teamId)
                .orderByDesc(MfgDispatchOrder::getCreatedTime);
        return this.page(page, queryWrapper);
    }

    /**
     * 根据条件查询派工单列表，支持关键字、状态、工单ID和班组ID筛选。
     * @param keyword 关键字（匹配派工单编号），可选
     * @param status 派工单状态，可选
     * @param workOrderId 工单ID，可选
     * @param teamId 班组ID，可选
     * @return 派工单列表
     */
    @Override
    public List<MfgDispatchOrder> listDispatchOrders(String keyword, String status, Long workOrderId, Long teamId) {
        LambdaQueryWrapper<MfgDispatchOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgDispatchOrder::getDeletedFlag, 0)
                .like(StringUtils.hasText(keyword), MfgDispatchOrder::getDispatchNo, keyword)
                .eq(StringUtils.hasText(status), MfgDispatchOrder::getStatus, status)
                .eq(workOrderId != null, MfgDispatchOrder::getWorkOrderId, workOrderId)
                .eq(teamId != null, MfgDispatchOrder::getTeamId, teamId)
                .orderByDesc(MfgDispatchOrder::getCreatedTime);
        return this.list(queryWrapper);
    }

    /**
     * 根据派工单ID查询派工单详情。
     * @param dispatchId 派工单ID
     * @return 派工单实体
     */
    @Override
    public MfgDispatchOrder getDispatchOrderById(Long dispatchId) {
        LambdaQueryWrapper<MfgDispatchOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MfgDispatchOrder::getDispatchId, dispatchId)
                .eq(MfgDispatchOrder::getDeletedFlag, 0);
        return this.getOne(queryWrapper);
    }

    /**
     * 新增派工单。
     * @param dispatchOrder 派工单实体
     * @return 操作是否成功
     */
    @Override
    public boolean saveDispatchOrder(MfgDispatchOrder dispatchOrder) {
        return this.save(dispatchOrder);
    }

    /**
     * 更新派工单信息。
     * @param dispatchOrder 派工单实体（必须包含dispatchId）
     * @return 操作是否成功
     */
    @Override
    public boolean updateDispatchOrder(MfgDispatchOrder dispatchOrder) {
        return this.updateById(dispatchOrder);
    }

    /**
     * 删除派工单（软删除，将deletedFlag设为1）。
     * @param dispatchId 派工单ID
     * @return 操作是否成功
     */
    @Override
    public boolean removeDispatchOrderById(Long dispatchId) {
        MfgDispatchOrder dispatchOrder = new MfgDispatchOrder();
        dispatchOrder.setDispatchId(dispatchId);
        dispatchOrder.setDeletedFlag(1);
        return this.updateById(dispatchOrder);
    }
}
