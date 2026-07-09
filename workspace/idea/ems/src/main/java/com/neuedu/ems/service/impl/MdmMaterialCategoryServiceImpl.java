package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.MdmMaterialCategory;
import com.neuedu.ems.mapper.MdmMaterialCategoryMapper;
import com.neuedu.ems.service.MdmMaterialCategoryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 物料分类 Service 实现类。
 */
@Service
public class MdmMaterialCategoryServiceImpl
        extends ServiceImpl<MdmMaterialCategoryMapper, MdmMaterialCategory>
        implements MdmMaterialCategoryService {

    @Override
    public Page<MdmMaterialCategory> pageCategories(Integer current, Integer size, String keyword, String status) {
        Page<MdmMaterialCategory> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildCategoryQuery(keyword, status));
    }

    @Override
    public MdmMaterialCategory getCategoryById(Long categoryId) {
        QueryWrapper<MdmMaterialCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("category_id", categoryId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveCategory(MdmMaterialCategory category) {
        if (category.getDeletedFlag() == null) {
            category.setDeletedFlag(0);
        }
        return baseMapper.insert(category) > 0;
    }

    @Override
    public boolean updateCategory(MdmMaterialCategory category) {
        if (category.getCategoryId() == null) {
            return false;
        }
        category.setDeletedFlag(null);
        category.setChildren(null);
        return baseMapper.updateById(category) > 0;
    }

    @Override
    public boolean removeCategoryById(Long categoryId) {
        MdmMaterialCategory category = new MdmMaterialCategory();
        category.setCategoryId(categoryId);
        category.setDeletedFlag(1);
        return baseMapper.updateById(category) > 0;
    }

    @Override
    public List<MdmMaterialCategory> listCategories(String keyword, String status) {
        return baseMapper.selectList(buildCategoryQuery(keyword, status));
    }

    @Override
    public List<MdmMaterialCategory> treeCategories(String status) {
        QueryWrapper<MdmMaterialCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("parent_id", "category_id");
        return buildTree(baseMapper.selectList(wrapper));
    }

    private QueryWrapper<MdmMaterialCategory> buildCategoryQuery(String keyword, String status) {
        QueryWrapper<MdmMaterialCategory> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("category_code", keyword).or().like("category_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("category_id");
        return wrapper;
    }

    private List<MdmMaterialCategory> buildTree(List<MdmMaterialCategory> categories) {
        Map<Long, MdmMaterialCategory> categoryMap = new LinkedHashMap<>();
        for (MdmMaterialCategory category : categories) {
            category.setChildren(new ArrayList<>());
            categoryMap.put(category.getCategoryId(), category);
        }

        List<MdmMaterialCategory> roots = new ArrayList<>();
        for (MdmMaterialCategory category : categories) {
            Long parentId = category.getParentId();
            MdmMaterialCategory parent = parentId == null ? null : categoryMap.get(parentId);
            if (parent == null || parentId == 0) {
                roots.add(category);
            } else {
                parent.getChildren().add(category);
            }
        }
        return roots;
    }
}
