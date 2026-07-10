package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgWorkOrderMaterial;
import com.neuedu.ems.service.mfg.MfgWorkOrderMaterialService;
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

/**
 * 工单物料需求管理控制器。
 * 提供工单物料需求的增删改查、分页查询和导出功能。
 */
@RestController
@RequestMapping("/api/mfg/work-order-material")
public class MfgWorkOrderMaterialController {

    @Autowired
    private MfgWorkOrderMaterialService mfgWorkOrderMaterialService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询工单物料需求列表，支持工单筛选、物料筛选和状态筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param workOrderId 工单ID，可选
     * @param materialId 物料ID，可选
     * @param status 物料齐套状态，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long workOrderId,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) String status) {
        Page<MfgWorkOrderMaterial> result = mfgWorkOrderMaterialService.pageWorkOrderMaterials(current, size, workOrderId, materialId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据工单ID查询未删除的工单物料需求列表。
     * @param workOrderId 工单ID
     * @return 工单物料需求列表
     */
    @GetMapping("/work-order/{workOrderId}")
    public ResultData listByWorkOrder(@PathVariable("workOrderId") Long workOrderId) {
        List<MfgWorkOrderMaterial> workOrderMaterials = mfgWorkOrderMaterialService.listWorkOrderMaterialsByWorkOrderId(workOrderId);
        return ResultData.success(ResultCode.SUCCESS, workOrderMaterials);
    }

    /**
     * 根据需求ID查询工单物料需求详情。
     * @param requirementId 需求ID
     * @return 工单物料需求实体信息
     */
    @GetMapping("/{requirementId}")
    public ResultData getWorkOrderMaterial(@PathVariable("requirementId") Long requirementId) {
        MfgWorkOrderMaterial workOrderMaterial = mfgWorkOrderMaterialService.getWorkOrderMaterialById(requirementId);
        return ResultData.success(ResultCode.SUCCESS, workOrderMaterial);
    }

    /**
     * 新增工单物料需求。
     * @param workOrderMaterial 工单物料需求实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody MfgWorkOrderMaterial workOrderMaterial) {
        boolean flag = mfgWorkOrderMaterialService.saveWorkOrderMaterial(workOrderMaterial);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新工单物料需求信息。
     * @param workOrderMaterial 工单物料需求实体（JSON格式，必须包含requirementId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgWorkOrderMaterial workOrderMaterial) {
        boolean flag = mfgWorkOrderMaterialService.updateWorkOrderMaterial(workOrderMaterial);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除工单物料需求（软删除，将删除标志设为1）。
     * @param requirementId 需求ID
     * @return 操作结果
     */
    @DeleteMapping("/{requirementId}")
    public ResultData delete(@PathVariable("requirementId") Long requirementId) {
        boolean flag = mfgWorkOrderMaterialService.removeWorkOrderMaterialById(requirementId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出工单物料需求列表为Excel文件。
     * @param response HTTP响应对象
     * @param workOrderId 工单ID，可选
     * @param materialId 物料ID，可选
     * @param status 物料齐套状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long workOrderId,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) String status) {
        List<MfgWorkOrderMaterial> workOrderMaterials = mfgWorkOrderMaterialService.listWorkOrderMaterials(workOrderId, materialId, status);
        excelExportUtil.exportExcel(response, workOrderMaterials, MfgWorkOrderMaterial.class, "工单物料需求列表", "工单物料需求列表");
    }
}
