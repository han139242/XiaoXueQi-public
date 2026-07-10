package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.service.mfg.MfgWorkOrderService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 生产工单管理 Service 实现类。
 */
@Service
public class MfgWorkOrderServiceImpl extends ServiceImpl<MfgWorkOrderMapper, MfgWorkOrder> implements MfgWorkOrderService {

    /**
     * 分页查询生产工单列表，支持关键字搜索、状态筛选、生产计划筛选和产品筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配工单编号）
     * @param status 工单状态
     * @param planId 生产计划ID
     * @param productId 产品ID
     * @return 分页结果
     */
    @Override
    public Page<MfgWorkOrder> pageWorkOrders(Integer current, Integer size, String keyword, String status, Long planId, Long productId) {
        Page<MfgWorkOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildWorkOrderQuery(keyword, status, planId, productId));
    }

    /**
     * 根据生产工单ID查询生产工单信息。
     * @param workOrderId 生产工单ID
     * @return 生产工单实体
     */
    @Override
    public MfgWorkOrder getWorkOrderById(Long workOrderId) {
        QueryWrapper<MfgWorkOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("work_order_id", workOrderId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增生产工单，自动设置删除标志为0。
     * @param workOrder 生产工单实体
     * @return 是否成功
     */
    @Override
    public boolean saveWorkOrder(MfgWorkOrder workOrder) {
        if (workOrder.getDeletedFlag() == null) {
            workOrder.setDeletedFlag(0);
        }
        return baseMapper.insert(workOrder) > 0;
    }

    /**
     * 更新生产工单信息，不允许修改删除标志。
     * @param workOrder 生产工单实体
     * @return 是否成功
     */
    @Override
    public boolean updateWorkOrder(MfgWorkOrder workOrder) {
        if (workOrder.getWorkOrderId() == null) {
            return false;
        }
        workOrder.setDeletedFlag(null);
        return baseMapper.updateById(workOrder) > 0;
    }

    /**
     * 根据ID删除生产工单（软删除，将删除标志设为1）。
     * @param workOrderId 生产工单ID
     * @return 是否成功
     */
    @Override
    public boolean removeWorkOrderById(Long workOrderId) {
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(workOrderId);
        workOrder.setDeletedFlag(1);
        return baseMapper.updateById(workOrder) > 0;
    }

    /**
     * 查询生产工单列表，支持关键字搜索、状态筛选、生产计划筛选和产品筛选。
     * @param keyword 关键字（匹配工单编号）
     * @param status 工单状态
     * @param planId 生产计划ID
     * @param productId 产品ID
     * @return 生产工单列表
     */
    @Override
    public List<MfgWorkOrder> listWorkOrders(String keyword, String status, Long planId, Long productId) {
        return baseMapper.selectList(buildWorkOrderQuery(keyword, status, planId, productId));
    }

    /**
     * 构建生产工单查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @param planId 生产计划ID
     * @param productId 产品ID
     * @return 查询条件包装器
     */
    private QueryWrapper<MfgWorkOrder> buildWorkOrderQuery(String keyword, String status, Long planId, Long productId) {
        QueryWrapper<MfgWorkOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.like("work_order_no", keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (planId != null) {
            wrapper.eq("plan_id", planId);
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        wrapper.orderByDesc("work_order_id");
        return wrapper;
    }
}
