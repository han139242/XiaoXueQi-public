package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.QmsInspectionSchemeItem;

import java.util.List;

/**
 * 检验方案明细管理 Service 接口。
 */
public interface QmsInspectionSchemeItemService extends IService<QmsInspectionSchemeItem> {

    /**
     * 分页查询检验方案明细列表，支持方案、检验项目和检验标准筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param schemeId 检验方案ID
     * @param itemId 检验项目ID
     * @param standardId 检验标准ID
     * @return 分页结果
     */
    Page<QmsInspectionSchemeItem> pageSchemeItems(Integer current, Integer size, Long schemeId, Long itemId, Long standardId);

    /**
     * 查询检验方案明细列表，支持方案、检验项目和检验标准筛选。
     * @param schemeId 检验方案ID
     * @param itemId 检验项目ID
     * @param standardId 检验标准ID
     * @return 检验方案明细列表
     */
    List<QmsInspectionSchemeItem> listSchemeItems(Long schemeId, Long itemId, Long standardId);

    /**
     * 根据方案明细ID查询检验方案明细信息。
     * @param id 方案明细ID
     * @return 检验方案明细实体
     */
    QmsInspectionSchemeItem getSchemeItemById(Long id);

    /**
     * 新增检验方案明细。
     * @param schemeItem 检验方案明细实体
     * @return 是否成功
     */
    boolean saveSchemeItem(QmsInspectionSchemeItem schemeItem);

    /**
     * 更新检验方案明细信息。
     * @param schemeItem 检验方案明细实体
     * @return 是否成功
     */
    boolean updateSchemeItem(QmsInspectionSchemeItem schemeItem);

    /**
     * 根据ID删除检验方案明细（软删除）。
     * @param id 方案明细ID
     * @return 是否成功
     */
    boolean removeSchemeItemById(Long id);

    /**
     * 根据ID列表批量删除检验方案明细（软删除）。
     * @param ids 方案明细ID列表
     * @return 是否成功
     */
    boolean removeSchemeItemsByIds(List<Long> ids);
}
