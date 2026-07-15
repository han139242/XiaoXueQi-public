package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgReportMaterial;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.service.mfg.MfgWorkReportService;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 生产报工管理控制器。
 */
@RestController
@RequestMapping("/api/mfg/work-report")
public class MfgWorkReportController {

    @Autowired
    private MfgWorkReportService mfgWorkReportService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询生产报工列表，支持编号、任务、人员、工位和审核状态筛选。
     * @param current 当前页码，默认1
     * @param size 每页记录数，默认10
     * @param keyword 报工编号或任务编号关键字，可选
     * @param taskId 任务ID，可选
     * @param operatorId 操作工ID，可选
     * @param stationId 工位ID，可选
     * @param auditStatus 审核状态，可选
     * @return 报工分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long taskId,
                           @RequestParam(required = false) Long operatorId,
                           @RequestParam(required = false) Long stationId,
                           @RequestParam(required = false) String auditStatus) {
        Page<MfgWorkReport> result = mfgWorkReportService.pageWorkReports(current, size, keyword, taskId, operatorId, stationId, auditStatus);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据报工ID查询详情。
     * @param reportId 报工ID
     * @return 报工实体
     */
    @GetMapping("/{reportId}")
    public ResultData getWorkReport(@PathVariable("reportId") Long reportId) {
        MfgWorkReport workReport = mfgWorkReportService.getWorkReportById(reportId);
        return ResultData.success(ResultCode.SUCCESS, workReport);
    }

    /**
     * 新增生产报工；如果请求体包含materials，则同步保存用料明细。
     * @param request 报工请求体，JSON格式，可包含materials
     * @return 操作结果或报工ID
     */
    @PostMapping
    public ResultData save(@RequestBody WorkReportFullRequest request) {
        Long reportId = mfgWorkReportService.submitWorkReport(request, request.getMaterials());
        return ResultData.success(ResultCode.SUCCESS, Map.of("reportId", reportId));
    }

    /**
     * 新增报工主表并保存报工用料明细。
     * @param request 报工主表和用料明细请求体，JSON格式，包含materials
     * @return 报工ID
     */
    @PostMapping("/full")
    public ResultData saveFull(@RequestBody WorkReportFullRequest request) {
        Long reportId = mfgWorkReportService.submitWorkReport(request, request.getMaterials());
        return ResultData.success(ResultCode.SUCCESS, Map.of("reportId", reportId));
    }

    /**
     * 提交生产报工并保存用料明细。
     * @param request 报工主表和用料明细请求体，JSON格式，支持materials
     * @return 报工ID
     */
    @PostMapping("/submit")
    public ResultData submit(@RequestBody WorkReportFullRequest request) {
        Long reportId = mfgWorkReportService.submitWorkReport(request, request.getMaterials());
        return ResultData.success(ResultCode.SUCCESS, Map.of("reportId", reportId));
    }

    /**
     * 修改生产报工。
     * @param workReport 报工实体，JSON格式，必须包含reportId
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgWorkReport workReport) {
        boolean flag = mfgWorkReportService.updateWorkReport(workReport);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 修改报工主表并替换用料明细。
     * @param request 报工主表和用料明细请求体，JSON格式，必须包含reportId
     * @return 操作结果
     */
    @PutMapping("/full")
    public ResultData updateFull(@RequestBody WorkReportFullRequest request) {
        boolean flag = mfgWorkReportService.updateWorkReportFull(request, request.getMaterials());
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 审核生产报工。
     * @param reportId 报工ID
     * @param request 请求体，支持auditResult、auditComment参数
     * @param userId JWT 中的当前用户ID
     * @return 操作结果
     */
    @PutMapping("/{reportId}/audit")
    public ResultData audit(@PathVariable("reportId") Long reportId,
                            @RequestAttribute("userId") Long userId,
                            @RequestBody Map<String, Object> request) {
        boolean flag = mfgWorkReportService.auditWorkReport(reportId, getString(request, "auditResult"),
                getString(request, "auditComment"), userId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 软删除生产报工和报工用料明细。
     * @param reportId 报工ID
     * @return 操作结果
     */
    @DeleteMapping("/{reportId}")
    public ResultData delete(@PathVariable("reportId") Long reportId) {
        boolean flag = mfgWorkReportService.removeWorkReportById(reportId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出生产报工列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 报工编号关键字，可选
     * @param taskId 任务ID，可选
     * @param operatorId 操作工ID，可选
     * @param stationId 工位ID，可选
     * @param auditStatus 审核状态，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long taskId,
                      @RequestParam(required = false) Long operatorId,
                      @RequestParam(required = false) Long stationId,
                      @RequestParam(required = false) String auditStatus) {
        List<MfgWorkReport> reports = mfgWorkReportService.listWorkReports(keyword, taskId, operatorId, stationId, auditStatus);
        excelExportUtil.exportExcel(response, reports, MfgWorkReport.class, "生产报工列表", "生产报工列表");
    }

    /**
     * 从请求体中读取String类型参数。
     * @param request 请求体Map
     * @param key 参数名
     * @return String类型参数值
     */
    private String getString(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 报工主表和用料明细请求体。
     */
    public static class WorkReportFullRequest extends MfgWorkReport {

        private List<MfgReportMaterial> materials;

        /**
         * 获取报工用料明细列表。
         * @return 报工用料明细列表
         */
        public List<MfgReportMaterial> getMaterials() {
            return materials;
        }

        /**
         * 设置报工用料明细列表。
         * @param materials 报工用料明细列表
         */
        public void setMaterials(List<MfgReportMaterial> materials) {
            this.materials = materials;
        }
    }
}
