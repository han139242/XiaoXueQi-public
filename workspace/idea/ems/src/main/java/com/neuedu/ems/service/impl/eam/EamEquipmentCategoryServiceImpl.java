package com.neuedu.ems.service.impl.eam;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.eam.EamEquipmentCategory;
import com.neuedu.ems.mapper.eam.EamEquipmentCategoryMapper;
import com.neuedu.ems.service.eam.EamEquipmentCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 设备分类管理 Service 实现类。
 */
@Service
public class EamEquipmentCategoryServiceImpl extends ServiceImpl<EamEquipmentCategoryMapper, EamEquipmentCategory> implements EamEquipmentCategoryService {

    /**
     * 分页查询设备分类列表，支持关键字搜索和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配分类编码或分类名称）
     * @param status 分类状态
     * @return 分页结果
     */
    @Override
    public Page<EamEquipmentCategory> pageCategories(Integer current, Integer size, String keyword, String status) {
        Page<EamEquipmentCategory> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildCategoryQuery(keyword, status));
    }

    /**
     * 根据设备分类ID查询设备分类信息。
     * @param categoryId 设备分类ID
     * @return 设备分类实体
     */
    @Override
    public EamEquipmentCategory getCategoryById(Long categoryId) {
        QueryWrapper<EamEquipmentCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("category_id", categoryId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增设备分类，自动设置删除标志为0。
     * @param category 设备分类实体
     * @return 是否成功
     */
    @Override
    public boolean saveCategory(EamEquipmentCategory category) {
        if (category.getDeletedFlag() == null) {
            category.setDeletedFlag(0);
        }
        return baseMapper.insert(category) > 0;
    }

    /**
     * 更新设备分类信息，不允许修改删除标志。
     * @param category 设备分类实体
     * @return 是否成功
     */
    @Override
    public boolean updateCategory(EamEquipmentCategory category) {
        if (category.getCategoryId() == null) {
            return false;
        }
        category.setDeletedFlag(null);
        return baseMapper.updateById(category) > 0;
    }

    /**
     * 根据ID删除设备分类（软删除，将删除标志设为1）。
     * @param categoryId 设备分类ID
     * @return 是否成功
     */
    @Override
    public boolean removeCategoryById(Long categoryId) {
        EamEquipmentCategory category = new EamEquipmentCategory();
        category.setCategoryId(categoryId);
        category.setDeletedFlag(1);
        return baseMapper.updateById(category) > 0;
    }

    /**
     * 查询设备分类列表，支持关键字搜索和状态筛选。
     * @param keyword 关键字（匹配分类编码或分类名称）
     * @param status 分类状态
     * @return 设备分类列表
     */
    @Override
    public List<EamEquipmentCategory> listCategories(String keyword, String status) {
        return baseMapper.selectList(buildCategoryQuery(keyword, status));
    }

    /**
     * 根据父级ID查询子分类列表。
     * @param parentId 父级分类ID
     * @return 子分类列表
     */
    @Override
    public List<EamEquipmentCategory> listCategoriesByParentId(Long parentId) {
        QueryWrapper<EamEquipmentCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (parentId == null) {
            wrapper.isNull("parent_id");
        } else {
            wrapper.eq("parent_id", parentId);
        }
        wrapper.orderByAsc("category_id");
        return baseMapper.selectList(wrapper);
    }

    /**
     * 构建设备分类查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @return 查询条件包装器
     */
    private QueryWrapper<EamEquipmentCategory> buildCategoryQuery(String keyword, String status) {
        QueryWrapper<EamEquipmentCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("category_code", keyword)
                    .or().like("category_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("category_id");
        return wrapper;
    }
}
