package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.MdmMaterial;

import java.util.List;

/**
 * 物料 Service 接口。
 */
public interface MdmMaterialService extends IService<MdmMaterial> {

    Page<MdmMaterial> pageMaterials(Integer current, Integer size, String materialCode,
                                    String materialName, Long categoryId, String status);

    MdmMaterial getMaterialById(Long materialId);

    boolean saveMaterial(MdmMaterial material);

    boolean updateMaterial(MdmMaterial material);

    boolean removeMaterialById(Long materialId);

    List<MdmMaterial> listMaterials(String materialCode, String materialName, Long categoryId, String status);
}
