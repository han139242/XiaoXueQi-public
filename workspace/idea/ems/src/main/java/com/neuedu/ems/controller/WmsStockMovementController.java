package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.WmsStockMovement;
import com.neuedu.ems.service.WmsStockMovementService;
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
 * 库存流水管理控制器。
 */
@RestController
@RequestMapping("/api/wms/stock-movement")
public class WmsStockMovementController {

    @Autowired
    private WmsStockMovementService wmsStockMovementService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询库存流水列表，支持流水号、变动类型、来源单据、物料、批次和方向筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配流水号），可选
     * @param movementType 变动类型，可选
     * @param sourceOrderType 来源单据类型，可选
     * @param sourceOrderId 来源单据ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @param direction 方向，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String movementType,
                           @RequestParam(required = false) String sourceOrderType,
                           @RequestParam(required = false) Long sourceOrderId,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) Long batchId,
                           @RequestParam(required = false) String direction) {
        Page<WmsStockMovement> result = wmsStockMovementService.pageStockMovements(
                current, size, keyword, movementType, sourceOrderType, sourceOrderId, materialId, batchId, direction);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据库存流水ID查询详情。
     * @param movementId 库存流水ID
     * @return 库存流水实体信息
     */
    @GetMapping("/{movementId}")
    public ResultData getStockMovement(@PathVariable("movementId") Long movementId) {
        WmsStockMovement stockMovement = wmsStockMovementService.getStockMovementById(movementId);
        return ResultData.success(ResultCode.SUCCESS, stockMovement);
    }

    /**
     * 导出库存流水列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配流水号），可选
     * @param movementType 变动类型，可选
     * @param sourceOrderType 来源单据类型，可选
     * @param sourceOrderId 来源单据ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @param direction 方向，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String movementType,
                      @RequestParam(required = false) String sourceOrderType,
                      @RequestParam(required = false) Long sourceOrderId,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) Long batchId,
                      @RequestParam(required = false) String direction) {
        List<WmsStockMovement> movements = wmsStockMovementService.listStockMovements(
                keyword, movementType, sourceOrderType, sourceOrderId, materialId, batchId, direction);
        excelExportUtil.exportExcel(response, movements, WmsStockMovement.class, "库存流水列表", "库存流水列表");
    }
}
