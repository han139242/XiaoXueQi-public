package com.neuedu.ems.controller.qms;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.qms.QmsNonconformingDetail;
import com.neuedu.ems.service.qms.QmsNonconformingDetailService;
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
 * 不合格品处理明细管理控制器。
 */
@RestController
@RequestMapping("/api/qms/nonconforming-detail")
public class QmsNonconformingDetailController {

    @Autowired
    private QmsNonconformingDetailService qmsNonconformingDetailService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询不合格品处理明细列表，支持处理单、条码、检验结果、最终处理方式和复检结果筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param ncrId 处理单ID，可选
     * @param barcodeId 条码ID，可选
     * @param inspectionResultId 检验结果ID，可选
     * @param finalDisposition 最终处理方式，可选
     * @param recheckResult 复检结果，可选
     * @return 不合格品处理明细分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long ncrId,
                           @RequestParam(required = false) Long barcodeId,
                           @RequestParam(required = false) Long inspectionResultId,
                           @RequestParam(required = false) String finalDisposition,
                           @RequestParam(required = false) String recheckResult) {
        Page<QmsNonconformingDetail> result = qmsNonconformingDetailService.pageNonconformingDetails(
                current, size, ncrId, barcodeId, inspectionResultId, finalDisposition, recheckResult);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据处理单ID查询不合格品处理明细列表。
     * @param ncrId 处理单ID
     * @return 不合格品处理明细列表
     */
    @GetMapping("/ncr/{ncrId}")
    public ResultData listByNcr(@PathVariable("ncrId") Long ncrId) {
        List<QmsNonconformingDetail> details = qmsNonconformingDetailService.listNonconformingDetailsByNcrId(ncrId);
        return ResultData.success(ResultCode.SUCCESS, details);
    }

    /**
     * 根据不合格品处理明细ID查询详情。
     * @param detailId 明细ID
     * @return 不合格品处理明细实体
     */
    @GetMapping("/{detailId}")
    public ResultData getNonconformingDetail(@PathVariable("detailId") Long detailId) {
        QmsNonconformingDetail detail = qmsNonconformingDetailService.getNonconformingDetailById(detailId);
        return ResultData.success(ResultCode.SUCCESS, detail);
    }

    /**
     * 新增不合格品处理明细。
     * @param detail 不合格品处理明细实体，JSON格式
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody QmsNonconformingDetail detail) {
        boolean flag = qmsNonconformingDetailService.saveNonconformingDetail(detail);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 修改不合格品处理明细。
     * @param detail 不合格品处理明细实体，JSON格式，必须包含detailId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody QmsNonconformingDetail detail) {
        boolean flag = qmsNonconformingDetailService.updateNonconformingDetail(detail);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除不合格品处理明细。
     * @param detailId 明细ID
     * @return 操作结果
     */
    @DeleteMapping("/{detailId}")
    public ResultData delete(@PathVariable("detailId") Long detailId) {
        boolean flag = qmsNonconformingDetailService.removeNonconformingDetailById(detailId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出不合格品处理明细列表为Excel文件。
     * @param response HTTP响应对象
     * @param ncrId 处理单ID，可选
     * @param barcodeId 条码ID，可选
     * @param inspectionResultId 检验结果ID，可选
     * @param finalDisposition 最终处理方式，可选
     * @param recheckResult 复检结果，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long ncrId,
                      @RequestParam(required = false) Long barcodeId,
                      @RequestParam(required = false) Long inspectionResultId,
                      @RequestParam(required = false) String finalDisposition,
                      @RequestParam(required = false) String recheckResult) {
        List<QmsNonconformingDetail> details = qmsNonconformingDetailService.listNonconformingDetails(
                ncrId, barcodeId, inspectionResultId, finalDisposition, recheckResult);
        excelExportUtil.exportExcel(response, details, QmsNonconformingDetail.class, "不合格品处理明细列表", "不合格品处理明细列表");
    }
}
