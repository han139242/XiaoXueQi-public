package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.QmsNonconformingDetail;
import com.neuedu.ems.entity.QmsNonconformingOrder;
import com.neuedu.ems.mapper.QmsNonconformingDetailMapper;
import com.neuedu.ems.mapper.QmsNonconformingOrderMapper;
import com.neuedu.ems.service.QmsNonconformingDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 不合格品处理明细 Service 实现类。
 */
@Service
public class QmsNonconformingDetailServiceImpl extends ServiceImpl<QmsNonconformingDetailMapper, QmsNonconformingDetail> implements QmsNonconformingDetailService {

    private static final String APPROVED_STATUS = "APPROVED";
    private static final String CLOSED_STATUS = "CLOSED";

    @Autowired
    private QmsNonconformingOrderMapper qmsNonconformingOrderMapper;

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
    @Override
    public Page<QmsNonconformingDetail> pageNonconformingDetails(Integer current, Integer size, Long ncrId, Long barcodeId,
                                                                 Long inspectionResultId, String finalDisposition, String recheckResult) {
        Page<QmsNonconformingDetail> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildNonconformingDetailQuery(ncrId, barcodeId, inspectionResultId, finalDisposition, recheckResult));
    }

    /**
     * 查询不合格品处理明细列表，支持处理单、条码、检验结果、最终处理方式和复检结果筛选。
     * @param ncrId 处理单ID
     * @param barcodeId 条码ID
     * @param inspectionResultId 检验结果ID
     * @param finalDisposition 最终处理方式
     * @param recheckResult 复检结果
     * @return 不合格品处理明细列表
     */
    @Override
    public List<QmsNonconformingDetail> listNonconformingDetails(Long ncrId, Long barcodeId, Long inspectionResultId,
                                                                 String finalDisposition, String recheckResult) {
        return baseMapper.selectList(buildNonconformingDetailQuery(ncrId, barcodeId, inspectionResultId, finalDisposition, recheckResult));
    }

    /**
     * 根据处理单ID查询未删除的不合格品处理明细。
     * @param ncrId 处理单ID
     * @return 不合格品处理明细列表
     */
    @Override
    public List<QmsNonconformingDetail> listNonconformingDetailsByNcrId(Long ncrId) {
        return baseMapper.selectList(buildNonconformingDetailQuery(ncrId, null, null, null, null));
    }

    /**
     * 根据ID查询未删除的不合格品处理明细。
     * @param detailId 明细ID
     * @return 不合格品处理明细实体
     */
    @Override
    public QmsNonconformingDetail getNonconformingDetailById(Long detailId) {
        LambdaQueryWrapper<QmsNonconformingDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsNonconformingDetail::getDetailId, detailId)
                .eq(QmsNonconformingDetail::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增不合格品处理明细，已审批或已关闭处理单不允许新增明细。
     * @param nonconformingDetail 不合格品处理明细实体
     * @return 是否成功
     */
    @Override
    public boolean saveNonconformingDetail(QmsNonconformingDetail nonconformingDetail) {
        if (!canChangeNonconformingOrder(nonconformingDetail.getNcrId())) {
            return false;
        }
        prepareNonconformingDetail(nonconformingDetail);
        return baseMapper.insert(nonconformingDetail) > 0;
    }

    /**
     * 修改不合格品处理明细，已审批或已关闭处理单不允许修改明细。
     * @param nonconformingDetail 不合格品处理明细实体
     * @return 是否成功
     */
    @Override
    public boolean updateNonconformingDetail(QmsNonconformingDetail nonconformingDetail) {
        if (nonconformingDetail.getDetailId() == null) {
            return false;
        }
        QmsNonconformingDetail existing = getNonconformingDetailById(nonconformingDetail.getDetailId());
        if (existing == null || !canChangeNonconformingOrder(existing.getNcrId())) {
            return false;
        }
        if (nonconformingDetail.getNcrId() != null && !canChangeNonconformingOrder(nonconformingDetail.getNcrId())) {
            return false;
        }
        if (nonconformingDetail.getDefectQuantity() == null) {
            nonconformingDetail.setDefectQuantity(existing.getDefectQuantity());
        }
        prepareNonconformingDetail(nonconformingDetail);
        nonconformingDetail.setDeletedFlag(null);
        return baseMapper.updateById(nonconformingDetail) > 0;
    }

    /**
     * 根据ID软删除不合格品处理明细，已审批或已关闭处理单不允许删除明细。
     * @param detailId 明细ID
     * @return 是否成功
     */
    @Override
    public boolean removeNonconformingDetailById(Long detailId) {
        QmsNonconformingDetail existing = getNonconformingDetailById(detailId);
        if (existing == null || !canChangeNonconformingOrder(existing.getNcrId())) {
            return false;
        }
        QmsNonconformingDetail nonconformingDetail = new QmsNonconformingDetail();
        nonconformingDetail.setDetailId(detailId);
        nonconformingDetail.setDeletedFlag(1);
        return baseMapper.updateById(nonconformingDetail) > 0;
    }

    /**
     * 构建不合格品处理明细查询条件，默认过滤已删除记录。
     * @param ncrId 处理单ID
     * @param barcodeId 条码ID
     * @param inspectionResultId 检验结果ID
     * @param finalDisposition 最终处理方式
     * @param recheckResult 复检结果
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<QmsNonconformingDetail> buildNonconformingDetailQuery(Long ncrId, Long barcodeId,
                                                                                    Long inspectionResultId,
                                                                                    String finalDisposition,
                                                                                    String recheckResult) {
        LambdaQueryWrapper<QmsNonconformingDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsNonconformingDetail::getDeletedFlag, 0)
                .eq(ncrId != null, QmsNonconformingDetail::getNcrId, ncrId)
                .eq(barcodeId != null, QmsNonconformingDetail::getBarcodeId, barcodeId)
                .eq(inspectionResultId != null, QmsNonconformingDetail::getInspectionResultId, inspectionResultId)
                .eq(StringUtils.hasText(finalDisposition), QmsNonconformingDetail::getFinalDisposition, finalDisposition)
                .eq(StringUtils.hasText(recheckResult), QmsNonconformingDetail::getRecheckResult, recheckResult)
                .orderByDesc(QmsNonconformingDetail::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 校验不合格品处理单是否允许修改明细。
     * @param ncrId 处理单ID，可为空
     * @return 是否允许修改
     */
    private boolean canChangeNonconformingOrder(Long ncrId) {
        if (ncrId == null) {
            return true;
        }
        LambdaQueryWrapper<QmsNonconformingOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsNonconformingOrder::getNcrId, ncrId)
                .eq(QmsNonconformingOrder::getDeletedFlag, 0);
        QmsNonconformingOrder order = qmsNonconformingOrderMapper.selectOne(queryWrapper);
        return order != null && !APPROVED_STATUS.equals(order.getApprovalStatus()) && !CLOSED_STATUS.equals(order.getStatus());
    }

    /**
     * 补齐不合格品处理明细的默认数量和删除标记。
     * @param detail 不合格品处理明细实体
     */
    private void prepareNonconformingDetail(QmsNonconformingDetail detail) {
        if (detail.getDefectQuantity() == null) {
            detail.setDefectQuantity(BigDecimal.ZERO);
        }
        if (detail.getDeletedFlag() == null) {
            detail.setDeletedFlag(0);
        }
    }
}
