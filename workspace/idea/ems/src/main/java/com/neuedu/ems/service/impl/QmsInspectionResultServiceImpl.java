package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.QmsInspectionOrder;
import com.neuedu.ems.entity.QmsInspectionResult;
import com.neuedu.ems.mapper.QmsInspectionOrderMapper;
import com.neuedu.ems.mapper.QmsInspectionResultMapper;
import com.neuedu.ems.service.QmsInspectionResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 检验结果 Service 实现类。
 */
@Service
public class QmsInspectionResultServiceImpl extends ServiceImpl<QmsInspectionResultMapper, QmsInspectionResult> implements QmsInspectionResultService {

    private static final String FINISHED_STATUS = "FINISHED";

    @Autowired
    private QmsInspectionOrderMapper qmsInspectionOrderMapper;

    /**
     * 分页查询检验结果，支持检验单、检验项目、结果和图片筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param inspectionId 检验单ID
     * @param itemId 检验项目ID
     * @param result 检验结果
     * @param photoFileId 图片附件ID
     * @return 检验结果分页结果
     */
    @Override
    public Page<QmsInspectionResult> pageInspectionResults(Integer current, Integer size, Long inspectionId, Long itemId, String result, Long photoFileId) {
        Page<QmsInspectionResult> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildInspectionResultQuery(inspectionId, itemId, result, photoFileId));
    }

    /**
     * 查询检验结果列表，支持检验单、检验项目、结果和图片筛选。
     * @param inspectionId 检验单ID
     * @param itemId 检验项目ID
     * @param result 检验结果
     * @param photoFileId 图片附件ID
     * @return 检验结果列表
     */
    @Override
    public List<QmsInspectionResult> listInspectionResults(Long inspectionId, Long itemId, String result, Long photoFileId) {
        return baseMapper.selectList(buildInspectionResultQuery(inspectionId, itemId, result, photoFileId));
    }

    /**
     * 根据检验单ID查询未删除的检验结果。
     * @param inspectionId 检验单ID
     * @return 检验结果列表
     */
    @Override
    public List<QmsInspectionResult> listInspectionResultsByInspectionId(Long inspectionId) {
        return baseMapper.selectList(buildInspectionResultQuery(inspectionId, null, null, null));
    }

    /**
     * 根据ID查询未删除的检验结果。
     * @param resultId 检验结果ID
     * @return 检验结果实体
     */
    @Override
    public QmsInspectionResult getInspectionResultById(Long resultId) {
        LambdaQueryWrapper<QmsInspectionResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionResult::getResultId, resultId)
                .eq(QmsInspectionResult::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增检验结果，已完成检验单不允许新增明细。
     * @param inspectionResult 检验结果实体
     * @return 是否成功
     */
    @Override
    public boolean saveInspectionResult(QmsInspectionResult inspectionResult) {
        if (!canChangeInspectionOrder(inspectionResult.getInspectionId())) {
            return false;
        }
        if (inspectionResult.getDeletedFlag() == null) {
            inspectionResult.setDeletedFlag(0);
        }
        return baseMapper.insert(inspectionResult) > 0;
    }

    /**
     * 批量保存检验结果，已完成检验单不允许再保存。
     * @param inspectionId 检验单ID
     * @param results 检验结果列表
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean saveInspectionResultsBatch(Long inspectionId, List<QmsInspectionResult> results) {
        if (inspectionId == null || !canChangeInspectionOrder(inspectionId)) {
            return false;
        }
        if (results == null) {
            return true;
        }
        for (QmsInspectionResult result : results) {
            result.setInspectionId(inspectionId);
            if (result.getDeletedFlag() == null) {
                result.setDeletedFlag(0);
            }
            baseMapper.insert(result);
        }
        return true;
    }

    /**
     * 修改检验结果，已完成检验单不允许修改明细。
     * @param inspectionResult 检验结果实体
     * @return 是否成功
     */
    @Override
    public boolean updateInspectionResult(QmsInspectionResult inspectionResult) {
        if (inspectionResult.getResultId() == null) {
            return false;
        }
        QmsInspectionResult existing = getInspectionResultById(inspectionResult.getResultId());
        if (existing == null || !canChangeInspectionOrder(existing.getInspectionId())) {
            return false;
        }
        if (inspectionResult.getInspectionId() != null && !canChangeInspectionOrder(inspectionResult.getInspectionId())) {
            return false;
        }
        inspectionResult.setDeletedFlag(null);
        return baseMapper.updateById(inspectionResult) > 0;
    }

    /**
     * 根据ID软删除检验结果，已完成检验单不允许删除明细。
     * @param resultId 检验结果ID
     * @return 是否成功
     */
    @Override
    public boolean removeInspectionResultById(Long resultId) {
        QmsInspectionResult existing = getInspectionResultById(resultId);
        if (existing == null || !canChangeInspectionOrder(existing.getInspectionId())) {
            return false;
        }
        QmsInspectionResult inspectionResult = new QmsInspectionResult();
        inspectionResult.setResultId(resultId);
        inspectionResult.setDeletedFlag(1);
        return baseMapper.updateById(inspectionResult) > 0;
    }

    /**
     * 构建检验结果查询条件，默认过滤已删除记录。
     * @param inspectionId 检验单ID
     * @param itemId 检验项目ID
     * @param result 检验结果
     * @param photoFileId 图片附件ID
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<QmsInspectionResult> buildInspectionResultQuery(Long inspectionId, Long itemId, String result, Long photoFileId) {
        LambdaQueryWrapper<QmsInspectionResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionResult::getDeletedFlag, 0)
                .eq(inspectionId != null, QmsInspectionResult::getInspectionId, inspectionId)
                .eq(itemId != null, QmsInspectionResult::getItemId, itemId)
                .eq(StringUtils.hasText(result), QmsInspectionResult::getResult, result)
                .eq(photoFileId != null, QmsInspectionResult::getPhotoFileId, photoFileId)
                .orderByDesc(QmsInspectionResult::getCreatedTime);
        return queryWrapper;
    }

    /**
     * 校验检验单是否允许修改检验结果。
     * @param inspectionId 检验单ID，可为空
     * @return 是否允许修改
     */
    private boolean canChangeInspectionOrder(Long inspectionId) {
        if (inspectionId == null) {
            return true;
        }
        LambdaQueryWrapper<QmsInspectionOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionOrder::getInspectionId, inspectionId)
                .eq(QmsInspectionOrder::getDeletedFlag, 0);
        QmsInspectionOrder inspectionOrder = qmsInspectionOrderMapper.selectOne(queryWrapper);
        return inspectionOrder != null && !FINISHED_STATUS.equals(inspectionOrder.getStatus());
    }
}
