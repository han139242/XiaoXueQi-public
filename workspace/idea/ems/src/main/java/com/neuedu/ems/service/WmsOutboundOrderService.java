package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.WmsOutboundDetail;
import com.neuedu.ems.entity.WmsOutboundOrder;

import java.util.List;

/**
 * 出库单管理 Service 接口。
 */
public interface WmsOutboundOrderService extends IService<WmsOutboundOrder> {

    /**
     * 分页查询出库单，支持单号、类型、仓库、工单、申请人和审核状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配出库单号
     * @param outboundType 出库类型
     * @param warehouseId 仓库ID
     * @param workOrderId 工单ID
     * @param applicantId 申请人ID
     * @param auditStatus 审核状态
     * @return 出库单分页结果
     */
    Page<WmsOutboundOrder> pageOutboundOrders(Integer current, Integer size, String keyword, String outboundType,
                                              Long warehouseId, Long workOrderId, Long applicantId, String auditStatus);

    /**
     * 查询出库单列表，支持单号、类型、仓库、工单、申请人和审核状态筛选。
     * @param keyword 关键字，匹配出库单号
     * @param outboundType 出库类型
     * @param warehouseId 仓库ID
     * @param workOrderId 工单ID
     * @param applicantId 申请人ID
     * @param auditStatus 审核状态
     * @return 出库单列表
     */
    List<WmsOutboundOrder> listOutboundOrders(String keyword, String outboundType, Long warehouseId,
                                              Long workOrderId, Long applicantId, String auditStatus);

    /**
     * 根据出库单ID查询详情。
     * @param outboundId 出库单ID
     * @return 出库单实体
     */
    WmsOutboundOrder getOutboundOrderById(Long outboundId);

    /**
     * 新增出库单。
     * @param outboundOrder 出库单实体
     * @return 是否成功
     */
    boolean saveOutboundOrder(WmsOutboundOrder outboundOrder);

    /**
     * 新增出库单并保存明细。
     * @param outboundOrder 出库单实体
     * @param details 出库明细列表
     * @return 出库单ID
     */
    Long saveOutboundOrderFull(WmsOutboundOrder outboundOrder, List<WmsOutboundDetail> details);

    /**
     * 更新出库单。
     * @param outboundOrder 出库单实体
     * @return 是否成功
     */
    boolean updateOutboundOrder(WmsOutboundOrder outboundOrder);

    /**
     * 更新出库单并替换明细。
     * @param outboundOrder 出库单实体
     * @param details 出库明细列表
     * @return 是否成功
     */
    boolean updateOutboundOrderFull(WmsOutboundOrder outboundOrder, List<WmsOutboundDetail> details);

    /**
     * 根据ID软删除出库单和明细。
     * @param outboundId 出库单ID
     * @return 是否成功
     */
    boolean removeOutboundOrderById(Long outboundId);

    /**
     * 确认出库，扣减库存并写入库存流水。
     * @param outboundId 出库单ID
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 是否成功
     */
    boolean confirmOutboundOrder(Long outboundId, Long operatorId, String remark);
}
