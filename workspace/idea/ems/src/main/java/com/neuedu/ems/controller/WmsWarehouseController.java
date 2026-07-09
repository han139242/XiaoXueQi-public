package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.WmsWarehouse;
import com.neuedu.ems.service.WmsWarehouseService;
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
@RequestMapping("/api/wms/warehouse")
public class WmsWarehouseController {

    @Autowired
    private WmsWarehouseService wmsWarehouseService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询仓库列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<WmsWarehouse> result = wmsWarehouseService.pageWarehouses(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询仓库。
     */
    @GetMapping("/{warehouseId}")
    public ResultData getWarehouse(@PathVariable("warehouseId") Long id) {
        WmsWarehouse warehouse = wmsWarehouseService.getWarehouseById(id);
        return ResultData.success(ResultCode.SUCCESS, warehouse);
    }

    /**
     * 新增仓库。
     */
    @PostMapping
    public ResultData save(@RequestBody WmsWarehouse warehouse) {
        boolean flag = wmsWarehouseService.saveWarehouse(warehouse);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新仓库。
     */
    @PutMapping
    public ResultData update(@RequestBody WmsWarehouse warehouse) {
        boolean flag = wmsWarehouseService.updateWarehouse(warehouse);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除仓库，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = wmsWarehouseService.removeWarehouseById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出仓库 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<WmsWarehouse> warehouses = wmsWarehouseService.listWarehouses(keyword, status);
        excelExportUtil.exportExcel(response, warehouses, WmsWarehouse.class, "仓库列表", "仓库列表");
    }
}
