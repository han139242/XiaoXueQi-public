package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.QmsNonconformingDetail;

import java.util.List;

/**
 * 不合格品处理明细 Service 接口。
 */
public interface QmsNonconformingDetailService extends IService<QmsNonconformingDetail> {

    /**
     * 分页查询不合格品处理明细，支持处理单、条码、检验结果、最终处理方式和复检结果筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param ncrId 处理单ID
     * @param barcodeId 条码ID
     * @param inspectionResultId 检验结果ID
     * @param finalDisposition 最终处理方式
     * @param recheckResult 复检结果
     * @return 不合格品处理明细分页结果
     */
    Page<QmsNonconformingDetail> pageNonconformingDetails(Integer current, Integer size, Long ncrId, Long barcodeId,
                                                          Long inspectionResultId, String finalDisposition, String recheckResult);

    /**
     * 查询不合格品处理明细列表，支持处理单、条码、检验结果、最终处理方式和复检结果筛选。
     * @param ncrId 处理单ID
     * @param barcodeId 条码ID
     * @param inspectionResultId 检验结果ID
     * @param finalDisposition 最终处理方式
     * @param recheckResult 复检结果
     * @return 不合格品处理明细列表
     */
    List<QmsNonconformingDetail> listNonconformingDetails(Long ncrId, Long barcodeId, Long inspectionResultId,
                                                          String finalDisposition, String recheckResult);

    /**
     * 根据处理单ID查询未删除的不合格品处理明细。
     * @param ncrId 处理单ID
     * @return 不合格品处理明细列表
     */
    List<QmsNonconformingDetail> listNonconformingDetailsByNcrId(Long ncrId);

    /**
     * 根据ID查询未删除的不合格品处理明细。
     * @param detailId 明细ID
     * @return 不合格品处理明细实体
     */
    QmsNonconformingDetail getNonconformingDetailById(Long detailId);

    /**
     * 新增不合格品处理明细。
     * @param nonconformingDetail 不合格品处理明细实体
     * @return 是否成功
     */
    boolean saveNonconformingDetail(QmsNonconformingDetail nonconformingDetail);

    /**
     * 修改不合格品处理明细。
     * @param nonconformingDetail 不合格品处理明细实体
     * @return 是否成功
     */
    boolean updateNonconformingDetail(QmsNonconformingDetail nonconformingDetail);

    /**
     * 根据ID软删除不合格品处理明细。
     * @param detailId 明细ID
     * @return 是否成功
     */
    boolean removeNonconformingDetailById(Long detailId);
}
