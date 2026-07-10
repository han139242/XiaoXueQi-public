package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.WmsInventory;
import com.neuedu.ems.service.WmsInventoryService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存余额管理控制器。
 */
@RestController
@RequestMapping("/api/wms/inventory")
public class WmsInventoryController {

    @Autowired
    private WmsInventoryService wmsInventoryService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询库存余额列表，支持仓库、库位、物料和批次筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param warehouseId 仓库ID，可选
     * @param locationId 库位ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long warehouseId,
                           @RequestParam(required = false) Long locationId,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) Long batchId) {
        Page<WmsInventory> result = wmsInventoryService.pageInventories(current, size, warehouseId, locationId, materialId, batchId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据库存余额ID查询详情。
     * @param inventoryId 库存余额ID
     * @return 库存余额实体信息
     */
    @GetMapping("/{inventoryId}")
    public ResultData getInventory(@PathVariable("inventoryId") Long inventoryId) {
        WmsInventory inventory = wmsInventoryService.getInventoryById(inventoryId);
        return ResultData.success(ResultCode.SUCCESS, inventory);
    }

    /**
     * 导出库存余额列表为Excel文件。
     * @param response HTTP响应对象
     * @param warehouseId 仓库ID，可选
     * @param locationId 库位ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long warehouseId,
                      @RequestParam(required = false) Long locationId,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) Long batchId) {
        List<WmsInventory> inventories = wmsInventoryService.listInventories(warehouseId, locationId, materialId, batchId);
        excelExportUtil.exportExcel(response, inventories, WmsInventory.class, "库存余额列表", "库存余额列表");
    }
}
