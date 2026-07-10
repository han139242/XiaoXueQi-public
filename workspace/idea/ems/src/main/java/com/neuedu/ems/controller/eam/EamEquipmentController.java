package com.neuedu.ems.controller.eam;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.eam.EamEquipment;
import com.neuedu.ems.service.eam.EamEquipmentService;
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
 * 设备管理控制器。
 * 提供设备的增删改查、分页查询和导出功能。
 */
@RestController
@RequestMapping("/api/eam/equipment")
public class EamEquipmentController {

    @Autowired
    private EamEquipmentService eamEquipmentService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询设备列表，支持关键字搜索、状态筛选和分类筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配设备编码或设备名称），可选
     * @param status 设备状态（运行、停机、故障、维修中、报废），可选
     * @param categoryId 设备分类ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long categoryId) {
        Page<EamEquipment> result = eamEquipmentService.pageEquipments(current, size, keyword, status, categoryId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据设备ID查询设备详情。
     * @param equipmentId 设备ID
     * @return 设备实体信息
     */
    @GetMapping("/{equipmentId}")
    public ResultData getEquipment(@PathVariable("equipmentId") Long equipmentId) {
        EamEquipment equipment = eamEquipmentService.getEquipmentById(equipmentId);
        return ResultData.success(ResultCode.SUCCESS, equipment);
    }

    /**
     * 新增设备。
     * @param equipment 设备实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody EamEquipment equipment) {
        boolean flag = eamEquipmentService.saveEquipment(equipment);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新设备信息。
     * @param equipment 设备实体（JSON格式，必须包含equipmentId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody EamEquipment equipment) {
        boolean flag = eamEquipmentService.updateEquipment(equipment);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除设备（软删除，将删除标志设为1）。
     * @param equipmentId 设备ID
     * @return 操作结果
     */
    @DeleteMapping("/{equipmentId}")
    public ResultData delete(@PathVariable("equipmentId") Long equipmentId) {
        boolean flag = eamEquipmentService.removeEquipmentById(equipmentId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出设备列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配设备编码或设备名称），可选
     * @param status 设备状态，可选
     * @param categoryId 设备分类ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) Long categoryId) {
        List<EamEquipment> equipments = eamEquipmentService.listEquipments(keyword, status, categoryId);
        excelExportUtil.exportExcel(response, equipments, EamEquipment.class, "设备台账", "设备台账");
    }
}
