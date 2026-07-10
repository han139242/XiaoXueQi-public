package com.neuedu.ems.controller.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.wms.WmsReturnDetail;
import com.neuedu.ems.service.wms.WmsReturnDetailService;
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
 * 退料明细管理控制器。
 */
@RestController
@RequestMapping("/api/wms/return-detail")
public class WmsReturnDetailController {

    @Autowired
    private WmsReturnDetailService wmsReturnDetailService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询退料明细列表，支持退料单、物料和批次筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param returnId 退料单ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @return 退料明细分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long returnId,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) Long batchId) {
        Page<WmsReturnDetail> result = wmsReturnDetailService.pageReturnDetails(current, size, returnId, materialId, batchId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据退料单ID查询退料明细列表。
     * @param returnId 退料单ID
     * @return 退料明细列表
     */
    @GetMapping("/return/{returnId}")
    public ResultData listByReturn(@PathVariable("returnId") Long returnId) {
        List<WmsReturnDetail> returnDetails = wmsReturnDetailService.listReturnDetailsByReturnId(returnId);
        return ResultData.success(ResultCode.SUCCESS, returnDetails);
    }

    /**
     * 根据退料明细ID查询详情。
     * @param detailId 退料明细ID
     * @return 退料明细实体
     */
    @GetMapping("/{detailId}")
    public ResultData getReturnDetail(@PathVariable("detailId") Long detailId) {
        WmsReturnDetail returnDetail = wmsReturnDetailService.getReturnDetailById(detailId);
        return ResultData.success(ResultCode.SUCCESS, returnDetail);
    }

    /**
     * 新增退料明细。
     * @param returnDetail 退料明细实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody WmsReturnDetail returnDetail) {
        boolean flag = wmsReturnDetailService.saveReturnDetail(returnDetail);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新退料明细。
     * @param returnDetail 退料明细实体，JSON格式，必须包含detailId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsReturnDetail returnDetail) {
        boolean flag = wmsReturnDetailService.updateReturnDetail(returnDetail);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除退料明细。
     * @param detailId 退料明细ID
     * @return 操作结果
     */
    @DeleteMapping("/{detailId}")
    public ResultData delete(@PathVariable("detailId") Long detailId) {
        boolean flag = wmsReturnDetailService.removeReturnDetailById(detailId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出退料明细列表为Excel文件。
     * @param response HTTP响应对象
     * @param returnId 退料单ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long returnId,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) Long batchId) {
        List<WmsReturnDetail> returnDetails = wmsReturnDetailService.listReturnDetails(returnId, materialId, batchId);
        excelExportUtil.exportExcel(response, returnDetails, WmsReturnDetail.class, "退料明细列表", "退料明细列表");
    }
}
