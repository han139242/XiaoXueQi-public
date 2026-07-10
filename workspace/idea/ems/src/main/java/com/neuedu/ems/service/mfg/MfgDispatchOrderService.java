package com.neuedu.ems.service.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;

import java.util.List;

/**
 * 派工单业务逻辑接口。
 * 提供派工单的业务处理方法，包括分页查询、条件查询、新增、更新和删除。
 */
public interface MfgDispatchOrderService extends IService<MfgDispatchOrder> {

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
    Page<MfgDispatchOrder> pageDispatchOrders(Integer current, Integer size, String keyword, String status, Long workOrderId, Long teamId);

    /**
     * 根据条件查询派工单列表，支持关键字、状态、工单ID和班组ID筛选。
     * @param keyword 关键字（匹配派工单编号），可选
     * @param status 派工单状态，可选
     * @param workOrderId 工单ID，可选
     * @param teamId 班组ID，可选
     * @return 派工单列表
     */
    List<MfgDispatchOrder> listDispatchOrders(String keyword, String status, Long workOrderId, Long teamId);

    /**
     * 根据派工单ID查询派工单详情。
     * @param dispatchId 派工单ID
     * @return 派工单实体
     */
    MfgDispatchOrder getDispatchOrderById(Long dispatchId);

    /**
     * 新增派工单。
     * @param dispatchOrder 派工单实体
     * @return 操作是否成功
     */
    boolean saveDispatchOrder(MfgDispatchOrder dispatchOrder);

    /**
     * 更新派工单信息。
     * @param dispatchOrder 派工单实体（必须包含dispatchId）
     * @return 操作是否成功
     */
    boolean updateDispatchOrder(MfgDispatchOrder dispatchOrder);

    /**
     * 删除派工单（软删除，将deletedFlag设为1）。
     * @param dispatchId 派工单ID
     * @return 操作是否成功
     */
    boolean removeDispatchOrderById(Long dispatchId);
}
