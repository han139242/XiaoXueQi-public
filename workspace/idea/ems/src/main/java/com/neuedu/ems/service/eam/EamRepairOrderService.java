package com.neuedu.ems.service.eam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.eam.EamRepairOrder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 设备维修单 Service 接口。
 */
public interface EamRepairOrderService extends IService<EamRepairOrder> {

    /**
     * 分页查询维修单，支持单号、设备、异常、维修人和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配维修单号或故障描述
     * @param equipmentId 设备ID
     * @param exceptionId 生产异常ID
     * @param repairerId 维修人ID
     * @param status 状态
     * @return 分页结果
     */
    Page<EamRepairOrder> pageRepairOrders(Integer current, Integer size, String keyword, Long equipmentId,
                                          Long exceptionId, Long repairerId, String status);

    /**
     * 查询维修单列表，支持单号、设备、异常、维修人和状态筛选。
     * @param keyword 关键字，匹配维修单号或故障描述
     * @param equipmentId 设备ID
     * @param exceptionId 生产异常ID
     * @param repairerId 维修人ID
     * @param status 状态
     * @return 维修单列表
     */
    List<EamRepairOrder> listRepairOrders(String keyword, Long equipmentId, Long exceptionId, Long repairerId, String status);

    /**
     * 根据维修单ID查询未删除的维修单详情。
     * @param repairId 维修单ID
     * @return 维修单实体
     */
    EamRepairOrder getRepairOrderById(Long repairId);

    /**
     * 新增维修单。
     * @param repairOrder 维修单实体
     * @return 是否成功
     */
    boolean saveRepairOrder(EamRepairOrder repairOrder);

    /**
     * 更新维修单。
     * @param repairOrder 维修单实体，必须包含repairId
     * @return 是否成功
     */
    boolean updateRepairOrder(EamRepairOrder repairOrder);

    /**
     * 根据ID软删除维修单。
     * @param repairId 维修单ID
     * @return 是否成功
     */
    boolean removeRepairOrderById(Long repairId);

    /**
     * 派工维修单并同步设备状态。
     * @param repairId 维修单ID
     * @param repairerId 维修人ID
     * @param remark 派工备注
     * @return 是否成功
     */
    boolean assignRepairOrder(Long repairId, Long repairerId, String remark);

    /**
     * 关闭维修单并同步设备状态。
     * @param repairId 维修单ID
     * @param repairResult 维修结果
     * @param repairCost 维修费用
     * @return 是否成功
     */
    boolean closeRepairOrder(Long repairId, String repairResult, BigDecimal repairCost);
}
