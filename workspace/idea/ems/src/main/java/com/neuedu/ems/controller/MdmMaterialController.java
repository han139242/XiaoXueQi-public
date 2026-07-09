package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.MdmMaterial;
import com.neuedu.ems.service.MdmMaterialService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mdm/material")
public class MdmMaterialController {

    @Autowired
    private MdmMaterialService mdmMaterialService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询物料列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String materialCode,
                           @RequestParam(required = false) String materialName,
                           @RequestParam(required = false) Long categoryId,
                           @RequestParam(required = false) String status) {
        Page<MdmMaterial> result = mdmMaterialService.pageMaterials(
                current, size, materialCode, materialName, categoryId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询物料。
     */
    @GetMapping("/{materialId}")
    public ResultData getMaterial(@PathVariable("materialId") Long id) {
        MdmMaterial material = mdmMaterialService.getMaterialById(id);
        return ResultData.success(ResultCode.SUCCESS, material);
    }

    /**
     * 新增物料。
     */
    @PostMapping
    public ResultData save(@RequestBody MdmMaterial material) {
        boolean flag = mdmMaterialService.saveMaterial(material);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新物料。
     */
    @PutMapping
    public ResultData update(@RequestBody MdmMaterial material) {
        boolean flag = mdmMaterialService.updateMaterial(material);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除物料，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = mdmMaterialService.removeMaterialById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出物料 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String materialCode,
                      @RequestParam(required = false) String materialName,
                      @RequestParam(required = false) Long categoryId,
                      @RequestParam(required = false) String status) {
        List<MdmMaterial> materials = mdmMaterialService.listMaterials(materialCode, materialName, categoryId, status);
        excelExportUtil.exportExcel(response, materials, MdmMaterial.class, "物料列表", "物料列表");
    }
}
