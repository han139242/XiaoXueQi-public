package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.WmsStocktakeDetail;
import com.neuedu.ems.service.WmsStocktakeDetailService;
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
 * 盘点明细管理控制器。
 */
@RestController
@RequestMapping("/api/wms/stocktake-detail")
public class WmsStocktakeDetailController {

    @Autowired
    private WmsStocktakeDetailService wmsStocktakeDetailService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询盘点明细列表，支持盘点单、物料、批次和库位筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param stocktakeId 盘点单ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @param locationId 库位ID，可选
     * @return 盘点明细分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long stocktakeId,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) Long batchId,
                           @RequestParam(required = false) Long locationId) {
        Page<WmsStocktakeDetail> result = wmsStocktakeDetailService.pageStocktakeDetails(current, size, stocktakeId, materialId, batchId, locationId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据盘点单ID查询盘点明细列表。
     * @param stocktakeId 盘点单ID
     * @return 盘点明细列表
     */
    @GetMapping("/stocktake/{stocktakeId}")
    public ResultData listByStocktake(@PathVariable("stocktakeId") Long stocktakeId) {
        List<WmsStocktakeDetail> stocktakeDetails = wmsStocktakeDetailService.listStocktakeDetailsByStocktakeId(stocktakeId);
        return ResultData.success(ResultCode.SUCCESS, stocktakeDetails);
    }

    /**
     * 根据盘点明细ID查询详情。
     * @param detailId 盘点明细ID
     * @return 盘点明细实体
     */
    @GetMapping("/{detailId}")
    public ResultData getStocktakeDetail(@PathVariable("detailId") Long detailId) {
        WmsStocktakeDetail stocktakeDetail = wmsStocktakeDetailService.getStocktakeDetailById(detailId);
        return ResultData.success(ResultCode.SUCCESS, stocktakeDetail);
    }

    /**
     * 新增盘点明细。
     * @param stocktakeDetail 盘点明细实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody WmsStocktakeDetail stocktakeDetail) {
        boolean flag = wmsStocktakeDetailService.saveStocktakeDetail(stocktakeDetail);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新盘点明细。
     * @param stocktakeDetail 盘点明细实体，JSON格式，必须包含detailId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsStocktakeDetail stocktakeDetail) {
        boolean flag = wmsStocktakeDetailService.updateStocktakeDetail(stocktakeDetail);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除盘点明细。
     * @param detailId 盘点明细ID
     * @return 操作结果
     */
    @DeleteMapping("/{detailId}")
    public ResultData delete(@PathVariable("detailId") Long detailId) {
        boolean flag = wmsStocktakeDetailService.removeStocktakeDetailById(detailId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出盘点明细列表为Excel文件。
     * @param response HTTP响应对象
     * @param stocktakeId 盘点单ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @param locationId 库位ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long stocktakeId,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) Long batchId,
                      @RequestParam(required = false) Long locationId) {
        List<WmsStocktakeDetail> stocktakeDetails = wmsStocktakeDetailService.listStocktakeDetails(stocktakeId, materialId, batchId, locationId);
        excelExportUtil.exportExcel(response, stocktakeDetails, WmsStocktakeDetail.class, "盘点明细列表", "盘点明细列表");
    }
}
