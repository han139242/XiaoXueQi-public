package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.QmsInspectionScheme;
import com.neuedu.ems.entity.QmsInspectionSchemeItem;

import java.util.List;

/**
 * 检验方案管理 Service 接口。
 */
public interface QmsInspectionSchemeService extends IService<QmsInspectionScheme> {

    /**
     * 分页查询检验方案列表，支持方案编码、名称、产品、工序、检验类型和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param schemeCode 方案编码
     * @param schemeName 方案名称
     * @param productId 产品ID
     * @param operationId 工序ID
     * @param inspectionType 检验类型
     * @param status 方案状态
     * @return 分页结果
     */
    Page<QmsInspectionScheme> pageSchemes(Integer current, Integer size, String schemeCode, String schemeName,
                                          Long productId, Long operationId, String inspectionType, String status);

    /**
     * 查询检验方案列表，支持方案编码、名称、产品、工序、检验类型和状态筛选。
     * @param schemeCode 方案编码
     * @param schemeName 方案名称
     * @param productId 产品ID
     * @param operationId 工序ID
     * @param inspectionType 检验类型
     * @param status 方案状态
     * @return 检验方案列表
     */
    List<QmsInspectionScheme> listSchemes(String schemeCode, String schemeName, Long productId, Long operationId,
                                          String inspectionType, String status);

    /**
     * 根据检验方案ID查询方案明细列表。
     * @param schemeId 检验方案ID
     * @return 检验方案明细列表
     */
    List<QmsInspectionSchemeItem> listSchemeItems(Long schemeId);

    /**
     * 根据检验方案ID查询检验方案信息。
     * @param schemeId 检验方案ID
     * @return 检验方案实体
     */
    QmsInspectionScheme getSchemeById(Long schemeId);

    /**
     * 新增检验方案。
     * @param scheme 检验方案实体
     * @return 是否成功
     */
    boolean saveScheme(QmsInspectionScheme scheme);

    /**
     * 更新检验方案信息。
     * @param scheme 检验方案实体
     * @return 是否成功
     */
    boolean updateScheme(QmsInspectionScheme scheme);

    /**
     * 根据ID删除检验方案（软删除）。
     * @param schemeId 检验方案ID
     * @return 是否成功
     */
    boolean removeSchemeById(Long schemeId);

    /**
     * 根据ID列表批量删除检验方案（软删除）。
     * @param schemeIds 检验方案ID列表
     * @return 是否成功
     */
    boolean removeSchemesByIds(List<Long> schemeIds);
}
