package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.QmsNonconformingDetail;
import com.neuedu.ems.entity.QmsNonconformingOrder;

import java.util.List;

/**
 * 不合格品处理单 Service 接口。
 */
public interface QmsNonconformingOrderService extends IService<QmsNonconformingOrder> {

    /**
     * 分页查询不合格品处理单，支持单号、检验单、产品批次、处理方式、审批状态和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 处理单号关键字
     * @param inspectionId 检验单ID
     * @param productLotId 产品批次ID
     * @param dispositionType 处理方式
     * @param approvalStatus 审批状态
     * @param status 状态
     * @return 不合格品处理单分页结果
     */
    Page<QmsNonconformingOrder> pageNonconformingOrders(Integer current, Integer size, String keyword, Long inspectionId,
                                                        Long productLotId, String dispositionType, String approvalStatus, String status);

    /**
     * 查询不合格品处理单列表，支持单号、检验单、产品批次、处理方式、审批状态和状态筛选。
     * @param keyword 处理单号关键字
     * @param inspectionId 检验单ID
     * @param productLotId 产品批次ID
     * @param dispositionType 处理方式
     * @param approvalStatus 审批状态
     * @param status 状态
     * @return 不合格品处理单列表
     */
    List<QmsNonconformingOrder> listNonconformingOrders(String keyword, Long inspectionId, Long productLotId,
                                                        String dispositionType, String approvalStatus, String status);

    /**
     * 根据ID查询未删除的不合格品处理单。
     * @param ncrId 不合格品处理单ID
     * @return 不合格品处理单实体
     */
    QmsNonconformingOrder getNonconformingOrderById(Long ncrId);

    /**
     * 新增不合格品处理单。
     * @param nonconformingOrder 不合格品处理单实体
     * @return 是否成功
     */
    boolean saveNonconformingOrder(QmsNonconformingOrder nonconformingOrder);

    /**
     * 新增不合格品处理单和处理明细。
     * @param nonconformingOrder 不合格品处理单实体
     * @param details 处理明细列表
     * @return 不合格品处理单ID
     */
    Long saveNonconformingOrderFull(QmsNonconformingOrder nonconformingOrder, List<QmsNonconformingDetail> details);

    /**
     * 修改不合格品处理单。
     * @param nonconformingOrder 不合格品处理单实体
     * @return 是否成功
     */
    boolean updateNonconformingOrder(QmsNonconformingOrder nonconformingOrder);

    /**
     * 修改不合格品处理单并替换处理明细。
     * @param nonconformingOrder 不合格品处理单实体
     * @param details 处理明细列表
     * @return 是否成功
     */
    boolean updateNonconformingOrderFull(QmsNonconformingOrder nonconformingOrder, List<QmsNonconformingDetail> details);

    /**
     * 根据ID软删除不合格品处理单和明细。
     * @param ncrId 不合格品处理单ID
     * @return 是否成功
     */
    boolean removeNonconformingOrderById(Long ncrId);

    /**
     * 审批不合格品处理单。
     * @param ncrId 不合格品处理单ID
     * @param approvalResult 审批结果
     * @param approvalComment 审批备注
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean approveNonconformingOrder(Long ncrId, String approvalResult, String approvalComment, Long operatorId);
}
