package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.QmsInspectionSchemeItem;
import com.neuedu.ems.mapper.QmsInspectionSchemeItemMapper;
import com.neuedu.ems.service.QmsInspectionSchemeItemService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 检验方案明细管理 Service 实现类。
 */
@Service
public class QmsInspectionSchemeItemServiceImpl extends ServiceImpl<QmsInspectionSchemeItemMapper, QmsInspectionSchemeItem> implements QmsInspectionSchemeItemService {

    /**
     * 分页查询检验方案明细列表，支持方案、检验项目和检验标准筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param schemeId 检验方案ID
     * @param itemId 检验项目ID
     * @param standardId 检验标准ID
     * @return 分页结果
     */
    @Override
    public Page<QmsInspectionSchemeItem> pageSchemeItems(Integer current, Integer size, Long schemeId, Long itemId, Long standardId) {
        Page<QmsInspectionSchemeItem> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildSchemeItemQuery(schemeId, itemId, standardId));
    }

    /**
     * 查询检验方案明细列表，支持方案、检验项目和检验标准筛选。
     * @param schemeId 检验方案ID
     * @param itemId 检验项目ID
     * @param standardId 检验标准ID
     * @return 检验方案明细列表
     */
    @Override
    public List<QmsInspectionSchemeItem> listSchemeItems(Long schemeId, Long itemId, Long standardId) {
        return baseMapper.selectList(buildSchemeItemQuery(schemeId, itemId, standardId));
    }

    /**
     * 根据方案明细ID查询未删除的检验方案明细信息。
     * @param id 方案明细ID
     * @return 检验方案明细实体
     */
    @Override
    public QmsInspectionSchemeItem getSchemeItemById(Long id) {
        LambdaQueryWrapper<QmsInspectionSchemeItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionSchemeItem::getId, id)
                .eq(QmsInspectionSchemeItem::getDeletedFlag, 0);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 新增检验方案明细，自动设置删除标志为0。
     * @param schemeItem 检验方案明细实体
     * @return 是否成功
     */
    @Override
    public boolean saveSchemeItem(QmsInspectionSchemeItem schemeItem) {
        if (schemeItem.getDeletedFlag() == null) {
            schemeItem.setDeletedFlag(0);
        }
        return baseMapper.insert(schemeItem) > 0;
    }

    /**
     * 更新检验方案明细信息，不允许修改删除标志。
     * @param schemeItem 检验方案明细实体
     * @return 是否成功
     */
    @Override
    public boolean updateSchemeItem(QmsInspectionSchemeItem schemeItem) {
        if (schemeItem.getId() == null) {
            return false;
        }
        schemeItem.setDeletedFlag(null);
        return baseMapper.updateById(schemeItem) > 0;
    }

    /**
     * 根据ID删除检验方案明细（软删除，将删除标志设为1）。
     * @param id 方案明细ID
     * @return 是否成功
     */
    @Override
    public boolean removeSchemeItemById(Long id) {
        QmsInspectionSchemeItem schemeItem = new QmsInspectionSchemeItem();
        schemeItem.setId(id);
        schemeItem.setDeletedFlag(1);
        return baseMapper.updateById(schemeItem) > 0;
    }

    /**
     * 根据ID列表批量删除检验方案明细（软删除，将删除标志设为1）。
     * @param ids 方案明细ID列表
     * @return 是否成功
     */
    @Override
    public boolean removeSchemeItemsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<QmsInspectionSchemeItem> schemeItems = ids.stream().map(id -> {
            QmsInspectionSchemeItem schemeItem = new QmsInspectionSchemeItem();
            schemeItem.setId(id);
            schemeItem.setDeletedFlag(1);
            return schemeItem;
        }).toList();
        return updateBatchById(schemeItems);
    }

    /**
     * 构建检验方案明细查询条件，默认过滤已删除记录。
     * @param schemeId 检验方案ID，可选
     * @param itemId 检验项目ID，可选
     * @param standardId 检验标准ID，可选
     * @return 查询条件包装器
     */
    private LambdaQueryWrapper<QmsInspectionSchemeItem> buildSchemeItemQuery(Long schemeId, Long itemId, Long standardId) {
        LambdaQueryWrapper<QmsInspectionSchemeItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(QmsInspectionSchemeItem::getDeletedFlag, 0)
                .eq(schemeId != null, QmsInspectionSchemeItem::getSchemeId, schemeId)
                .eq(itemId != null, QmsInspectionSchemeItem::getItemId, itemId)
                .eq(standardId != null, QmsInspectionSchemeItem::getStandardId, standardId)
                .orderByAsc(QmsInspectionSchemeItem::getSequenceNo)
                .orderByDesc(QmsInspectionSchemeItem::getCreatedTime);
        return queryWrapper;
    }
}
