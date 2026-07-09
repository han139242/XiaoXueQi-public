package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.MfgProductionOrder;

import java.util.List;

/**
 * 生产订单管理 Service 接口。
 */
public interface MfgProductionOrderService extends IService<MfgProductionOrder> {

    /**
     * 分页查询生产订单列表，支持关键字搜索、状态筛选、客户筛选和产品筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配订单编号）
     * @param status 订单状态
     * @param customerId 客户ID
     * @param productId 产品ID
     * @return 分页结果
     */
    Page<MfgProductionOrder> pageOrders(Integer current, Integer size, String keyword, String status, Long customerId, Long productId);

    /**
     * 根据生产订单ID查询生产订单信息。
     * @param productionOrderId 生产订单ID
     * @return 生产订单实体
     */
    MfgProductionOrder getOrderById(Long productionOrderId);

    /**
     * 新增生产订单。
     * @param order 生产订单实体
     * @return 是否成功
     */
    boolean saveOrder(MfgProductionOrder order);

    /**
     * 更新生产订单信息。
     * @param order 生产订单实体
     * @return 是否成功
     */
    boolean updateOrder(MfgProductionOrder order);

    /**
     * 根据ID删除生产订单（软删除）。
     * @param productionOrderId 生产订单ID
     * @return 是否成功
     */
    boolean removeOrderById(Long productionOrderId);

    /**
     * 查询生产订单列表，支持关键字搜索、状态筛选、客户筛选和产品筛选。
     * @param keyword 关键字（匹配订单编号）
     * @param status 订单状态
     * @param customerId 客户ID
     * @param productId 产品ID
     * @return 生产订单列表
     */
    List<MfgProductionOrder> listOrders(String keyword, String status, Long customerId, Long productId);
}
