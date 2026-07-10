package com.neuedu.ems.service.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.wms.WmsInboundDetail;
import com.neuedu.ems.entity.wms.WmsInboundOrder;

import java.util.List;

/**
 * 入库单管理 Service 接口。
 */
public interface WmsInboundOrderService extends IService<WmsInboundOrder> {

    /**
     * 分页查询入库单列表，支持入库单号、类型、仓库、来源单据和审核状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配入库单号）
     * @param inboundType 入库类型
     * @param warehouseId 仓库ID
     * @param sourceOrderType 来源单据类型
     * @param sourceOrderId 来源单据ID
     * @param auditStatus 审核状态
     * @return 分页结果
     */
    Page<WmsInboundOrder> pageInboundOrders(Integer current, Integer size, String keyword, String inboundType,
                                            Long warehouseId, String sourceOrderType, Long sourceOrderId, String auditStatus);

    /**
     * 查询入库单列表，支持入库单号、类型、仓库、来源单据和审核状态筛选。
     * @param keyword 关键字（匹配入库单号）
     * @param inboundType 入库类型
     * @param warehouseId 仓库ID
     * @param sourceOrderType 来源单据类型
     * @param sourceOrderId 来源单据ID
     * @param auditStatus 审核状态
     * @return 入库单列表
     */
    List<WmsInboundOrder> listInboundOrders(String keyword, String inboundType, Long warehouseId,
                                            String sourceOrderType, Long sourceOrderId, String auditStatus);

    /**
     * 根据入库单ID查询详情。
     * @param inboundId 入库单ID
     * @return 入库单实体
     */
    WmsInboundOrder getInboundOrderById(Long inboundId);

    /**
     * 新增入库单。
     * @param inboundOrder 入库单实体
     * @return 是否成功
     */
    boolean saveInboundOrder(WmsInboundOrder inboundOrder);

    /**
     * 新增入库单并保存明细。
     * @param inboundOrder 入库单实体
     * @param details 入库明细列表
     * @return 入库单ID
     */
    Long saveInboundOrderFull(WmsInboundOrder inboundOrder, List<WmsInboundDetail> details);

    /**
     * 更新入库单信息。
     * @param inboundOrder 入库单实体
     * @return 是否成功
     */
    boolean updateInboundOrder(WmsInboundOrder inboundOrder);

    /**
     * 更新入库单并替换明细。
     * @param inboundOrder 入库单实体
     * @param details 入库明细列表
     * @return 是否成功
     */
    boolean updateInboundOrderFull(WmsInboundOrder inboundOrder, List<WmsInboundDetail> details);

    /**
     * 根据ID删除入库单（软删除）。
     * @param inboundId 入库单ID
     * @return 是否成功
     */
    boolean removeInboundOrderById(Long inboundId);

    /**
     * 确认入库，更新库存余额并写入库存流水。
     * @param inboundId 入库单ID
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 是否成功
     */
    boolean confirmInboundOrder(Long inboundId, Long operatorId, String remark);
}
