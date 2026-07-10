package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.TraceBarcode;
import com.neuedu.ems.service.TraceBarcodeService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
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
 * 条码追溯管理控制器。
 */
@RestController
@RequestMapping("/api/trace/barcode")
public class TraceBarcodeController {

    @Autowired
    private TraceBarcodeService traceBarcodeService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询条码，支持条码值、条码类型、业务类型、业务ID和状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 关键字，可选
     * @param barcodeType 条码类型，可选
     * @param bizType 业务类型，可选
     * @param bizId 业务ID，可选
     * @param status 状态，可选
     * @return 条码分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String barcodeType,
                           @RequestParam(required = false) String bizType,
                           @RequestParam(required = false) Long bizId,
                           @RequestParam(required = false) String status) {
        Page<TraceBarcode> result = traceBarcodeService.pageBarcodes(current, size, keyword, barcodeType, bizType, bizId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据条码ID查询条码详情。
     * @param barcodeId 条码ID
     * @return 条码实体信息
     */
    @GetMapping("/{barcodeId}")
    public ResultData getBarcode(@PathVariable("barcodeId") Long barcodeId) {
        TraceBarcode barcode = traceBarcodeService.getBarcodeById(barcodeId);
        return ResultData.success(ResultCode.SUCCESS, barcode);
    }

    /**
     * 新增条码。
     * @param barcode 条码实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody TraceBarcode barcode) {
        boolean flag = traceBarcodeService.saveBarcode(barcode);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新条码。
     * @param barcode 条码实体，JSON格式，必须包含barcodeId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody TraceBarcode barcode) {
        boolean flag = traceBarcodeService.updateBarcode(barcode);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除条码，采用软删除。
     * @param barcodeId 条码ID
     * @return 操作结果
     */
    @DeleteMapping("/{barcodeId}")
    public ResultData delete(@PathVariable("barcodeId") Long barcodeId) {
        boolean flag = traceBarcodeService.removeBarcodeById(barcodeId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出条码列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字，可选
     * @param barcodeType 条码类型，可选
     * @param bizType 业务类型，可选
     * @param bizId 业务ID，可选
     * @param status 状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String barcodeType,
                      @RequestParam(required = false) String bizType,
                      @RequestParam(required = false) Long bizId,
                      @RequestParam(required = false) String status) {
        List<TraceBarcode> barcodes = traceBarcodeService.listBarcodes(keyword, barcodeType, bizType, bizId, status);
        excelExportUtil.exportExcel(response, barcodes, TraceBarcode.class, "条码列表", "条码列表");
    }

    /**
     * 按生成规则批量生成条码。
     * @param request 生成请求体，JSON格式
     * @return 新生成的条码列表
     */
    @PostMapping("/generate")
    public ResultData generate(@RequestBody BarcodeGenerateRequest request) {
        if (request == null) {
            return ResultData.fail(ResultCode.FAILED);
        }
        List<TraceBarcode> barcodes = traceBarcodeService.generateBarcodes(
                request.getBarcodeType(), request.getBizType(), request.getBizId(), request.getQuantity(), request.getRemark());
        if (barcodes.isEmpty()) {
            return ResultData.fail(ResultCode.FAILED);
        }
        List<Map<String, Object>> result = barcodes.stream()
                .map(item -> Map.<String, Object>of("barcodeId", item.getBarcodeId(), "barcodeValue", item.getBarcodeValue()))
                .toList();
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据条码值查询条码详情。
     * @param barcodeValue 条码值
     * @return 条码实体信息
     */
    @GetMapping("/scan/{barcodeValue}")
    public ResultData scan(@PathVariable("barcodeValue") String barcodeValue) {
        TraceBarcode barcode = traceBarcodeService.scanBarcode(barcodeValue);
        return ResultData.success(ResultCode.SUCCESS, barcode);
    }

    /**
     * 条码生成请求体。
     */
    @Data
    public static class BarcodeGenerateRequest {
        private String barcodeType;
        private String bizType;
        private Long bizId;
        private Integer quantity;
        private String remark;
    }
}
