package com.neuedu.ems.controller.wms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.wms.WmsInboundDetail;
import com.neuedu.ems.service.wms.WmsInboundDetailService;
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
 * 入库明细管理控制器。
 */
@RestController
@RequestMapping("/api/wms/inbound-detail")
public class WmsInboundDetailController {

    @Autowired
    private WmsInboundDetailService wmsInboundDetailService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询入库明细列表，支持入库单、物料、批次和库位筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param inboundId 入库单ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @param locationId 库位ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long inboundId,
                           @RequestParam(required = false) Long materialId,
                           @RequestParam(required = false) Long batchId,
                           @RequestParam(required = false) Long locationId) {
        Page<WmsInboundDetail> result = wmsInboundDetailService.pageInboundDetails(current, size, inboundId, materialId, batchId, locationId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据入库单ID查询入库明细列表。
     * @param inboundId 入库单ID
     * @return 入库明细列表
     */
    @GetMapping("/inbound/{inboundId}")
    public ResultData listByInbound(@PathVariable("inboundId") Long inboundId) {
        List<WmsInboundDetail> inboundDetails = wmsInboundDetailService.listInboundDetailsByInboundId(inboundId);
        return ResultData.success(ResultCode.SUCCESS, inboundDetails);
    }

    /**
     * 根据入库明细ID查询详情。
     * @param detailId 入库明细ID
     * @return 入库明细实体信息
     */
    @GetMapping("/{detailId}")
    public ResultData getInboundDetail(@PathVariable("detailId") Long detailId) {
        WmsInboundDetail inboundDetail = wmsInboundDetailService.getInboundDetailById(detailId);
        return ResultData.success(ResultCode.SUCCESS, inboundDetail);
    }

    /**
     * 新增入库明细。
     * @param inboundDetail 入库明细实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody WmsInboundDetail inboundDetail) {
        boolean flag = wmsInboundDetailService.saveInboundDetail(inboundDetail);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新入库明细信息。
     * @param inboundDetail 入库明细实体（JSON格式，必须包含detailId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody WmsInboundDetail inboundDetail) {
        boolean flag = wmsInboundDetailService.updateInboundDetail(inboundDetail);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除入库明细（软删除，将删除标志设为1）。
     * @param detailId 入库明细ID
     * @return 操作结果
     */
    @DeleteMapping("/{detailId}")
    public ResultData delete(@PathVariable("detailId") Long detailId) {
        boolean flag = wmsInboundDetailService.removeInboundDetailById(detailId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出入库明细列表为Excel文件。
     * @param response HTTP响应对象
     * @param inboundId 入库单ID，可选
     * @param materialId 物料ID，可选
     * @param batchId 批次ID，可选
     * @param locationId 库位ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long inboundId,
                      @RequestParam(required = false) Long materialId,
                      @RequestParam(required = false) Long batchId,
                      @RequestParam(required = false) Long locationId) {
        List<WmsInboundDetail> inboundDetails = wmsInboundDetailService.listInboundDetails(inboundId, materialId, batchId, locationId);
        excelExportUtil.exportExcel(response, inboundDetails, WmsInboundDetail.class, "入库明细列表", "入库明细列表");
    }
}
