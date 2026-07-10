package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.MfgWorkOrderMaterial;

import java.util.List;

/**
 * 工单物料需求业务逻辑接口。
 * 提供工单物料需求的业务处理方法，包括分页查询、条件查询、新增、更新和删除。
 */
public interface MfgWorkOrderMaterialService extends IService<MfgWorkOrderMaterial> {

    /**
     * 分页查询工单物料需求，支持工单ID、物料ID和状态筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param workOrderId 工单ID，可选
     * @param materialId 物料ID，可选
     * @param status 物料齐套状态，可选
     * @return 分页结果
     */
    Page<MfgWorkOrderMaterial> pageWorkOrderMaterials(Integer current, Integer size, Long workOrderId, Long materialId, String status);

    /**
     * 根据工单ID查询未删除的工单物料需求列表。
     * @param workOrderId 工单ID
     * @return 工单物料需求列表
     */
    List<MfgWorkOrderMaterial> listWorkOrderMaterialsByWorkOrderId(Long workOrderId);

    /**
     * 根据条件查询工单物料需求列表，支持工单ID、物料ID和状态筛选。
     * @param workOrderId 工单ID，可选
     * @param materialId 物料ID，可选
     * @param status 物料齐套状态，可选
     * @return 工单物料需求列表
     */
    List<MfgWorkOrderMaterial> listWorkOrderMaterials(Long workOrderId, Long materialId, String status);

    /**
     * 根据需求ID查询工单物料需求详情。
     * @param requirementId 需求ID
     * @return 工单物料需求实体
     */
    MfgWorkOrderMaterial getWorkOrderMaterialById(Long requirementId);

    /**
     * 新增工单物料需求。
     * @param workOrderMaterial 工单物料需求实体
     * @return 操作是否成功
     */
    boolean saveWorkOrderMaterial(MfgWorkOrderMaterial workOrderMaterial);

    /**
     * 更新工单物料需求信息。
     * @param workOrderMaterial 工单物料需求实体（必须包含requirementId）
     * @return 操作是否成功
     */
    boolean updateWorkOrderMaterial(MfgWorkOrderMaterial workOrderMaterial);

    /**
     * 删除工单物料需求（软删除，将deletedFlag设为1）。
     * @param requirementId 需求ID
     * @return 操作是否成功
     */
    boolean removeWorkOrderMaterialById(Long requirementId);
}
