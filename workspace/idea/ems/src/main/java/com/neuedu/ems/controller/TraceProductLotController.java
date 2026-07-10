package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.TraceProductLot;
import com.neuedu.ems.service.TraceProductLotService;
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
import java.util.Map;

/**
 * 产品批次追溯管理控制器。
 */
@RestController
@RequestMapping("/api/trace/product-lot")
public class TraceProductLotController {

    @Autowired
    private TraceProductLotService traceProductLotService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询产品批次，支持关键字、产品、工单和状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 关键字，可选
     * @param productId 产品ID，可选
     * @param workOrderId 工单ID，可选
     * @param status 状态，可选
     * @return 产品批次分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long productId,
                           @RequestParam(required = false) Long workOrderId,
                           @RequestParam(required = false) String status) {
        Page<TraceProductLot> result = traceProductLotService.pageProductLots(current, size, keyword, productId, workOrderId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据产品批次ID查询产品批次详情。
     * @param productLotId 产品批次ID
     * @return 产品批次实体信息
     */
    @GetMapping("/{productLotId}")
    public ResultData getProductLot(@PathVariable("productLotId") Long productLotId) {
        TraceProductLot productLot = traceProductLotService.getProductLotById(productLotId);
        return ResultData.success(ResultCode.SUCCESS, productLot);
    }

    /**
     * 新增产品批次。
     * @param productLot 产品批次实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody TraceProductLot productLot) {
        boolean flag = traceProductLotService.saveProductLot(productLot);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新产品批次。
     * @param productLot 产品批次实体，JSON格式，必须包含productLotId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody TraceProductLot productLot) {
        boolean flag = traceProductLotService.updateProductLot(productLot);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除产品批次，采用软删除。
     * @param productLotId 产品批次ID
     * @return 操作结果
     */
    @DeleteMapping("/{productLotId}")
    public ResultData delete(@PathVariable("productLotId") Long productLotId) {
        boolean flag = traceProductLotService.removeProductLotById(productLotId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出产品批次列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字，可选
     * @param productId 产品ID，可选
     * @param workOrderId 工单ID，可选
     * @param status 状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long productId,
                      @RequestParam(required = false) Long workOrderId,
                      @RequestParam(required = false) String status) {
        List<TraceProductLot> productLots = traceProductLotService.listProductLots(keyword, productId, workOrderId, status);
        excelExportUtil.exportExcel(response, productLots, TraceProductLot.class, "产品批次列表", "产品批次列表");
    }

    /**
     * 根据批次号查询产品追溯详情。
     * @param lotNo 产品批次号
     * @return 产品追溯详情
     */
    @GetMapping("/lot-no/{lotNo}")
    public ResultData traceByLotNo(@PathVariable("lotNo") String lotNo) {
        Map<String, Object> result = traceProductLotService.getProductLotTraceByLotNo(lotNo);
        return ResultData.success(ResultCode.SUCCESS, result);
    }
}
