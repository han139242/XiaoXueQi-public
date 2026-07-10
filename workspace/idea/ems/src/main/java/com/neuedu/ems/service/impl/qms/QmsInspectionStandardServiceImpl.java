package com.neuedu.ems.service.impl.qms;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.qms.QmsInspectionStandard;
import com.neuedu.ems.mapper.qms.QmsInspectionStandardMapper;
import com.neuedu.ems.service.qms.QmsInspectionStandardService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 检验标准管理 Service 实现类。
 */
@Service
public class QmsInspectionStandardServiceImpl extends ServiceImpl<QmsInspectionStandardMapper, QmsInspectionStandard> implements QmsInspectionStandardService {

    /**
     * 分页查询检验标准列表，支持检验项目、产品和工序筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param itemId 检验项目ID
     * @param productId 产品ID
     * @param operationId 工序ID
     * @return 分页结果
     */
    @Override
    public Page<QmsInspectionStandard> pageStandards(Integer current, Integer size, Long itemId, Long productId, Long operationId) {
        Page<QmsInspectionStandard> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildStandardQuery(itemId, productId, operationId));
    }

    /**
     * 查询检验标准列表，支持检验项目、产品和工序筛选。
     * @param itemId 检验项目ID
     * @param productId 产品ID
     * @param operationId 工序ID
     * @return 检验标准列表
     */
    @Override
    public List<QmsInspectionStandard> listStandards(Long itemId, Long productId, Long operationId) {
        return baseMapper.selectList(buildStandardQuery(itemId, productId, operationId));
    }

    /**
     * 根据检验标准ID查询未删除的检验标准信息。
     * @param standardId 检验标准ID
     * @return 检验标准实体
     */
    @Override
    public QmsInspectionStandard getStandardById(Long standardId) {
        LambdaQueryWrapper<QmsInspectionStandard> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionStandard::getStandardId, standardId)
                .eq(QmsInspectionStandard::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增检验标准，自动设置删除标志为0。
     * @param standard 检验标准实体
     * @return 是否成功
     */
    @Override
    public boolean saveStandard(QmsInspectionStandard standard) {
        if (standard.getDeletedFlag() == null) {
            standard.setDeletedFlag(0);
        }
        return baseMapper.insert(standard) > 0;
    }

    /**
     * 更新检验标准信息，不允许修改删除标志。
     * @param standard 检验标准实体
     * @return 是否成功
     */
    @Override
    public boolean updateStandard(QmsInspectionStandard standard) {
        if (standard.getStandardId() == null) {
            return false;
        }
        standard.setDeletedFlag(null);
        return baseMapper.updateById(standard) > 0;
    }

    /**
     * 根据ID删除检验标准（软删除，将删除标志设为1）。
     * @param standardId 检验标准ID
     * @return 是否成功
     */
    @Override
    public boolean removeStandardById(Long standardId) {
        QmsInspectionStandard standard = new QmsInspectionStandard();
        standard.setStandardId(standardId);
        standard.setDeletedFlag(1);
        return baseMapper.updateById(standard) > 0;
    }

    /**
     * 根据ID列表批量删除检验标准（软删除，将删除标志设为1）。
     * @param standardIds 检验标准ID列表
     * @return 是否成功
     */
    @Override
    public boolean removeStandardsByIds(List<Long> standardIds) {
        if (standardIds == null || standardIds.isEmpty()) {
            return false;
        }
        List<QmsInspectionStandard> standards = standardIds.stream().map(standardId -> {
            QmsInspectionStandard standard = new QmsInspectionStandard();
            standard.setStandardId(standardId);
            standard.setDeletedFlag(1);
            return standard;
        }).toList();
        return updateBatchById(standards);
    }

    /**
     * 构建检验标准查询条件，默认过滤已删除记录。
     * @param itemId 检验项目ID，可选
     * @param productId 产品ID，可选
     * @param operationId 工序ID，可选
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<QmsInspectionStandard> buildStandardQuery(Long itemId, Long productId, Long operationId) {
        LambdaQueryWrapper<QmsInspectionStandard> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionStandard::getDeletedFlag, 0)
                .eq(itemId != null, QmsInspectionStandard::getItemId, itemId)
                .eq(productId != null, QmsInspectionStandard::getProductId, productId)
                .eq(operationId != null, QmsInspectionStandard::getOperationId, operationId)
                .orderByDesc(QmsInspectionStandard::getCreatedTime);
        return queryWrapper;
    }
}
