package com.neuedu.ems.service.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.wms.WmsReturnDetail;
import com.neuedu.ems.entity.wms.WmsReturnOrder;

import java.util.List;

/**
 * 退料单管理 Service 接口。
 */
public interface WmsReturnOrderService extends IService<WmsReturnOrder> {

    /**
     * 分页查询退料单，支持单号、类型、工单、申请人和审核状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字，匹配退料单号
     * @param returnType 退料类型
     * @param workOrderId 工单ID
     * @param applicantId 申请人ID
     * @param auditStatus 审核状态
     * @return 退料单分页结果
     */
    Page<WmsReturnOrder> pageReturnOrders(Integer current, Integer size, String keyword, String returnType,
                                          Long workOrderId, Long applicantId, String auditStatus);

    /**
     * 查询退料单列表，支持单号、类型、工单、申请人和审核状态筛选。
     * @param keyword 关键字，匹配退料单号
     * @param returnType 退料类型
     * @param workOrderId 工单ID
     * @param applicantId 申请人ID
     * @param auditStatus 审核状态
     * @return 退料单列表
     */
    List<WmsReturnOrder> listReturnOrders(String keyword, String returnType, Long workOrderId, Long applicantId, String auditStatus);

    /**
     * 根据退料单ID查询详情。
     * @param returnId 退料单ID
     * @return 退料单实体
     */
    WmsReturnOrder getReturnOrderById(Long returnId);

    /**
     * 新增退料单。
     * @param returnOrder 退料单实体
     * @return 是否成功
     */
    boolean saveReturnOrder(WmsReturnOrder returnOrder);

    /**
     * 新增退料单并保存明细。
     * @param returnOrder 退料单实体
     * @param details 退料明细列表
     * @return 退料单ID
     */
    Long saveReturnOrderFull(WmsReturnOrder returnOrder, List<WmsReturnDetail> details);

    /**
     * 更新退料单。
     * @param returnOrder 退料单实体
     * @return 是否成功
     */
    boolean updateReturnOrder(WmsReturnOrder returnOrder);

    /**
     * 更新退料单并替换明细。
     * @param returnOrder 退料单实体
     * @param details 退料明细列表
     * @return 是否成功
     */
    boolean updateReturnOrderFull(WmsReturnOrder returnOrder, List<WmsReturnDetail> details);

    /**
     * 根据ID软删除退料单和明细。
     * @param returnId 退料单ID
     * @return 是否成功
     */
    boolean removeReturnOrderById(Long returnId);

    /**
     * 确认退料入库，回加库存并写入库存流水。
     * @param returnId 退料单ID
     * @param operatorId 操作人ID
     * @param remark 备注
     * @return 是否成功
     */
    boolean confirmReturnOrder(Long returnId, Long operatorId, String remark);
}
