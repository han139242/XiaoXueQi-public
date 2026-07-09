package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.MfgProductionOrder;
import com.neuedu.ems.mapper.MfgProductionOrderMapper;
import com.neuedu.ems.service.MfgProductionOrderService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 生产订单管理 Service 实现类。
 */
@Service
public class MfgProductionOrderServiceImpl extends ServiceImpl<MfgProductionOrderMapper, MfgProductionOrder> implements MfgProductionOrderService {

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
    @Override
    public Page<MfgProductionOrder> pageOrders(Integer current, Integer size, String keyword, String status, Long customerId, Long productId) {
        Page<MfgProductionOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildOrderQuery(keyword, status, customerId, productId));
    }

    /**
     * 根据生产订单ID查询生产订单信息。
     * @param productionOrderId 生产订单ID
     * @return 生产订单实体
     */
    @Override
    public MfgProductionOrder getOrderById(Long productionOrderId) {
        QueryWrapper<MfgProductionOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("production_order_id", productionOrderId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增生产订单，自动设置删除标志为0。
     * @param order 生产订单实体
     * @return 是否成功
     */
    @Override
    public boolean saveOrder(MfgProductionOrder order) {
        if (order.getDeletedFlag() == null) {
            order.setDeletedFlag(0);
        }
        return baseMapper.insert(order) > 0;
    }

    /**
     * 更新生产订单信息，不允许修改删除标志。
     * @param order 生产订单实体
     * @return 是否成功
     */
    @Override
    public boolean updateOrder(MfgProductionOrder order) {
        if (order.getProductionOrderId() == null) {
            return false;
        }
        order.setDeletedFlag(null);
        return baseMapper.updateById(order) > 0;
    }

    /**
     * 根据ID删除生产订单（软删除，将删除标志设为1）。
     * @param productionOrderId 生产订单ID
     * @return 是否成功
     */
    @Override
    public boolean removeOrderById(Long productionOrderId) {
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(productionOrderId);
        order.setDeletedFlag(1);
        return baseMapper.updateById(order) > 0;
    }

    /**
     * 查询生产订单列表，支持关键字搜索、状态筛选、客户筛选和产品筛选。
     * @param keyword 关键字（匹配订单编号）
     * @param status 订单状态
     * @param customerId 客户ID
     * @param productId 产品ID
     * @return 生产订单列表
     */
    @Override
    public List<MfgProductionOrder> listOrders(String keyword, String status, Long customerId, Long productId) {
        return baseMapper.selectList(buildOrderQuery(keyword, status, customerId, productId));
    }

    /**
     * 构建生产订单查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @param customerId 客户ID
     * @param productId 产品ID
     * @return 查询条件包装器
     */
    private QueryWrapper<MfgProductionOrder> buildOrderQuery(String keyword, String status, Long customerId, Long productId) {
        QueryWrapper<MfgProductionOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.like("production_order_no", keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (customerId != null) {
            wrapper.eq("customer_id", customerId);
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        wrapper.orderByDesc("production_order_id");
        return wrapper;
    }
}
