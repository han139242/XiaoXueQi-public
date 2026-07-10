package com.neuedu.ems.service.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mfg.MfgReportMaterial;

import java.util.List;

/**
 * 报工用料消耗 Service 接口。
 */
public interface MfgReportMaterialService extends IService<MfgReportMaterial> {

    /**
     * 分页查询报工用料消耗，支持报工、物料和批次筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param reportId 报工ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 报工用料分页结果
     */
    Page<MfgReportMaterial> pageReportMaterials(Integer current, Integer size, Long reportId, Long materialId, Long batchId);

    /**
     * 查询报工用料消耗列表，支持报工、物料和批次筛选。
     * @param reportId 报工ID
     * @param materialId 物料ID
     * @param batchId 批次ID
     * @return 报工用料列表
     */
    List<MfgReportMaterial> listReportMaterials(Long reportId, Long materialId, Long batchId);

    /**
     * 根据报工ID查询未删除的用料明细。
     * @param reportId 报工ID
     * @return 用料明细列表
     */
    List<MfgReportMaterial> listReportMaterialsByReportId(Long reportId);

    /**
     * 根据ID查询未删除的报工用料。
     * @param id 报工用料ID
     * @return 报工用料实体
     */
    MfgReportMaterial getReportMaterialById(Long id);

    /**
     * 新增报工用料。
     * @param reportMaterial 报工用料实体
     * @return 是否成功
     */
    boolean saveReportMaterial(MfgReportMaterial reportMaterial);

    /**
     * 修改报工用料。
     * @param reportMaterial 报工用料实体
     * @return 是否成功
     */
    boolean updateReportMaterial(MfgReportMaterial reportMaterial);

    /**
     * 根据ID软删除报工用料。
     * @param id 报工用料ID
     * @return 是否成功
     */
    boolean removeReportMaterialById(Long id);
}
