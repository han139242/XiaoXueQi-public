package com.neuedu.ems.service.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;

import java.util.List;

/**
 * 生产工单管理 Service 接口。
 */
public interface MfgWorkOrderService extends IService<MfgWorkOrder> {

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
    Page<MfgWorkOrder> pageWorkOrders(Integer current, Integer size, String keyword, String status, Long planId, Long productId);

    /**
     * 根据生产工单ID查询生产工单信息。
     * @param workOrderId 生产工单ID
     * @return 生产工单实体
     */
    MfgWorkOrder getWorkOrderById(Long workOrderId);

    /**
     * 新增生产工单。
     * @param workOrder 生产工单实体
     * @return 是否成功
     */
    boolean saveWorkOrder(MfgWorkOrder workOrder);

    /**
     * 更新生产工单信息。
     * @param workOrder 生产工单实体
     * @return 是否成功
     */
    boolean updateWorkOrder(MfgWorkOrder workOrder);

    /**
     * 根据ID删除生产工单（软删除）。
     * @param workOrderId 生产工单ID
     * @return 是否成功
     */
    boolean removeWorkOrderById(Long workOrderId);

    MfgWorkOrder generateWorkOrder(Long planId, MfgWorkOrder request, Long operatorId);

    MfgWorkOrder releaseWorkOrder(Long workOrderId, Long operatorId, String remark);

    void withdrawWorkOrder(Long workOrderId, Long operatorId, String remark);

    /**
     * 查询生产工单列表，支持关键字搜索、状态筛选、生产计划筛选和产品筛选。
     * @param keyword 关键字（匹配工单编号）
     * @param status 工单状态
     * @param planId 生产计划ID
     * @param productId 产品ID
     * @return 生产工单列表
     */
    List<MfgWorkOrder> listWorkOrders(String keyword, String status, Long planId, Long productId);
}
