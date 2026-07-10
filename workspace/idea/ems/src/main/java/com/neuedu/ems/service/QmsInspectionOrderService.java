package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.QmsInspectionOrder;
import com.neuedu.ems.entity.QmsInspectionResult;

import java.util.List;

/**
 * 检验单 Service 接口。
 */
public interface QmsInspectionOrderService extends IService<QmsInspectionOrder> {

    /**
     * 分页查询检验单，支持单号、类型、方案、工单、任务、报工、批次、检验员、状态和结果筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 检验单号关键字
     * @param inspectionType 检验类型
     * @param schemeId 方案ID
     * @param workOrderId 工单ID
     * @param taskId 任务ID
     * @param reportId 报工ID
     * @param batchId 批次ID
     * @param inspectorId 检验员ID
     * @param status 状态
     * @param inspectionResult 检验结果
     * @return 检验单分页结果
     */
    Page<QmsInspectionOrder> pageInspectionOrders(Integer current, Integer size, String keyword, String inspectionType,
                                                  Long schemeId, Long workOrderId, Long taskId, Long reportId,
                                                  Long batchId, Long inspectorId, String status, String inspectionResult);

    /**
     * 查询检验单列表，支持单号、类型、方案、工单、任务、报工、批次、检验员、状态和结果筛选。
     * @param keyword 检验单号关键字
     * @param inspectionType 检验类型
     * @param schemeId 方案ID
     * @param workOrderId 工单ID
     * @param taskId 任务ID
     * @param reportId 报工ID
     * @param batchId 批次ID
     * @param inspectorId 检验员ID
     * @param status 状态
     * @param inspectionResult 检验结果
     * @return 检验单列表
     */
    List<QmsInspectionOrder> listInspectionOrders(String keyword, String inspectionType, Long schemeId, Long workOrderId,
                                                  Long taskId, Long reportId, Long batchId, Long inspectorId,
                                                  String status, String inspectionResult);

    /**
     * 根据ID查询未删除的检验单。
     * @param inspectionId 检验单ID
     * @return 检验单实体
     */
    QmsInspectionOrder getInspectionOrderById(Long inspectionId);

    /**
     * 新增检验单。
     * @param inspectionOrder 检验单实体
     * @return 是否成功
     */
    boolean saveInspectionOrder(QmsInspectionOrder inspectionOrder);

    /**
     * 新增检验单和检验结果明细。
     * @param inspectionOrder 检验单实体
     * @param results 检验结果列表
     * @return 检验单ID
     */
    Long saveInspectionOrderFull(QmsInspectionOrder inspectionOrder, List<QmsInspectionResult> results);

    /**
     * 修改检验单。
     * @param inspectionOrder 检验单实体
     * @return 是否成功
     */
    boolean updateInspectionOrder(QmsInspectionOrder inspectionOrder);

    /**
     * 修改检验单并替换检验结果明细。
     * @param inspectionOrder 检验单实体
     * @param results 检验结果列表
     * @return 是否成功
     */
    boolean updateInspectionOrderFull(QmsInspectionOrder inspectionOrder, List<QmsInspectionResult> results);

    /**
     * 根据ID软删除检验单和检验结果。
     * @param inspectionId 检验单ID
     * @return 是否成功
     */
    boolean removeInspectionOrderById(Long inspectionId);

    /**
     * 提交检验单结果并结束检验单。
     * @param inspectionId 检验单ID
     * @param inspectionResult 检验结果
     * @param remark 备注
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean submitInspectionOrder(Long inspectionId, String inspectionResult, String remark, Long operatorId);
}
