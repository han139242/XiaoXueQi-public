package com.neuedu.ems.controller.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.wms.WmsOutboundDetail;
import com.neuedu.ems.service.wms.WmsOutboundDetailService;
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
 * 出库明细管理控制器。
 */
@RestController
@RequestMapping("/api/wms/outbound-detail")
public class WmsOutboundDetailController {

    @Autowired
    private WmsOutboundDetailService wmsOutboundDetailService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询出库明细列表，支持出库单、物料、批次和库位筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param outboundId 出库单ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @param locationId 库位ID，可选
     * @return 出库明细分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long outboundId,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) Long batchId,
                           @RequestParam(required = false) Long locationId) {
        Page<WmsOutboundDetail> result = wmsOutboundDetailService.pageOutboundDetails(current, size, outboundId, materialId, batchId, locationId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据出库单ID查询出库明细列表。
     * @param outboundId 出库单ID
     * @return 出库明细列表
     */
    @GetMapping("/outbound/{outboundId}")
    public ResultData listByOutbound(@PathVariable("outboundId") Long outboundId) {
        List<WmsOutboundDetail> outboundDetails = wmsOutboundDetailService.listOutboundDetailsByOutboundId(outboundId);
        return ResultData.success(ResultCode.SUCCESS, outboundDetails);
    }

    /**
     * 根据出库明细ID查询详情。
     * @param detailId 出库明细ID
     * @return 出库明细实体
     */
    @GetMapping("/{detailId}")
    public ResultData getOutboundDetail(@PathVariable("detailId") Long detailId) {
        WmsOutboundDetail outboundDetail = wmsOutboundDetailService.getOutboundDetailById(detailId);
        return ResultData.success(ResultCode.SUCCESS, outboundDetail);
    }

    /**
     * 新增出库明细。
     * @param outboundDetail 出库明细实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody WmsOutboundDetail outboundDetail) {
        boolean flag = wmsOutboundDetailService.saveOutboundDetail(outboundDetail);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新出库明细。
     * @param outboundDetail 出库明细实体，JSON格式，必须包含detailId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsOutboundDetail outboundDetail) {
        boolean flag = wmsOutboundDetailService.updateOutboundDetail(outboundDetail);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除出库明细。
     * @param detailId 出库明细ID
     * @return 操作结果
     */
    @DeleteMapping("/{detailId}")
    public ResultData delete(@PathVariable("detailId") Long detailId) {
        boolean flag = wmsOutboundDetailService.removeOutboundDetailById(detailId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出出库明细列表为Excel文件。
     * @param response HTTP响应对象
     * @param outboundId 出库单ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @param locationId 库位ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long outboundId,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) Long batchId,
                      @RequestParam(required = false) Long locationId) {
        List<WmsOutboundDetail> outboundDetails = wmsOutboundDetailService.listOutboundDetails(outboundId, materialId, batchId, locationId);
        excelExportUtil.exportExcel(response, outboundDetails, WmsOutboundDetail.class, "出库明细列表", "出库明细列表");
    }
}
