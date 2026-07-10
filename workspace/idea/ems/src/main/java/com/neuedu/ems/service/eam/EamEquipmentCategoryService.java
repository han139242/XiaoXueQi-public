package com.neuedu.ems.service.eam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.eam.EamEquipmentCategory;

import java.util.List;

/**
 * 设备分类管理 Service 接口。
 */
public interface EamEquipmentCategoryService extends IService<EamEquipmentCategory> {

    /**
     * 分页查询设备分类列表，支持关键字搜索和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配分类编码或分类名称）
     * @param status 分类状态
     * @return 分页结果
     */
    Page<EamEquipmentCategory> pageCategories(Integer current, Integer size, String keyword, String status);

    /**
     * 根据设备分类ID查询设备分类信息。
     * @param categoryId 设备分类ID
     * @return 设备分类实体
     */
    EamEquipmentCategory getCategoryById(Long categoryId);

    /**
     * 新增设备分类。
     * @param category 设备分类实体
     * @return 是否成功
     */
    boolean saveCategory(EamEquipmentCategory category);

    /**
     * 更新设备分类信息。
     * @param category 设备分类实体
     * @return 是否成功
     */
    boolean updateCategory(EamEquipmentCategory category);

    /**
     * 根据ID删除设备分类（软删除）。
     * @param categoryId 设备分类ID
     * @return 是否成功
     */
    boolean removeCategoryById(Long categoryId);

    /**
     * 查询设备分类列表，支持关键字搜索和状态筛选。
     * @param keyword 关键字（匹配分类编码或分类名称）
     * @param status 分类状态
     * @return 设备分类列表
     */
    List<EamEquipmentCategory> listCategories(String keyword, String status);

    /**
     * 根据父级ID查询子分类列表。
     * @param parentId 父级分类ID
     * @return 子分类列表
     */
    List<EamEquipmentCategory> listCategoriesByParentId(Long parentId);
}
