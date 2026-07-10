package com.neuedu.ems.service.qms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.qms.QmsInspectionStandard;

import java.util.List;

/**
 * 检验标准管理 Service 接口。
 */
public interface QmsInspectionStandardService extends IService<QmsInspectionStandard> {

    /**
     * 分页查询检验标准列表，支持检验项目、产品和工序筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param itemId 检验项目ID
     * @param productId 产品ID
     * @param operationId 工序ID
     * @return 分页结果
     */
    Page<QmsInspectionStandard> pageStandards(Integer current, Integer size, Long itemId, Long productId, Long operationId);

    /**
     * 查询检验标准列表，支持检验项目、产品和工序筛选。
     * @param itemId 检验项目ID
     * @param productId 产品ID
     * @param operationId 工序ID
     * @return 检验标准列表
     */
    List<QmsInspectionStandard> listStandards(Long itemId, Long productId, Long operationId);

    /**
     * 根据检验标准ID查询检验标准信息。
     * @param standardId 检验标准ID
     * @return 检验标准实体
     */
    QmsInspectionStandard getStandardById(Long standardId);

    /**
     * 新增检验标准。
     * @param standard 检验标准实体
     * @return 是否成功
     */
    boolean saveStandard(QmsInspectionStandard standard);

    /**
     * 更新检验标准信息。
     * @param standard 检验标准实体
     * @return 是否成功
     */
    boolean updateStandard(QmsInspectionStandard standard);

    /**
     * 根据ID删除检验标准（软删除）。
     * @param standardId 检验标准ID
     * @return 是否成功
     */
    boolean removeStandardById(Long standardId);

    /**
     * 根据ID列表批量删除检验标准（软删除）。
     * @param standardIds 检验标准ID列表
     * @return 是否成功
     */
    boolean removeStandardsByIds(List<Long> standardIds);
}
