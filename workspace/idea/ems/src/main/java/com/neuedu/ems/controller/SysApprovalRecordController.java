package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.SysApprovalRecord;
import com.neuedu.ems.service.SysApprovalRecordService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/sys/approval-record")
public class SysApprovalRecordController {

    @Autowired
    private SysApprovalRecordService sysApprovalRecordService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询审批记录列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String bizType,
                           @RequestParam(required = false) Long bizId,
                           @RequestParam(required = false) String approvalNode,
                           @RequestParam(required = false) Long approverId,
                           @RequestParam(required = false) String approvalResult,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                           @RequestParam(required = false)
                           @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<SysApprovalRecord> result = sysApprovalRecordService.pageApprovalRecords(
                current, size, bizType, bizId, approvalNode, approverId, approvalResult, startTime, endTime);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询审批记录。
     */
    @GetMapping("/{approvalId}")
    public ResultData getApprovalRecord(@PathVariable("approvalId") Long id) {
        SysApprovalRecord approvalRecord = sysApprovalRecordService.getApprovalRecordById(id);
        return ResultData.success(ResultCode.SUCCESS, approvalRecord);
    }

    /**
     * 新增审批记录。
     */
    @PostMapping
    public ResultData save(@RequestBody SysApprovalRecord approvalRecord) {
        boolean flag = sysApprovalRecordService.saveApprovalRecord(approvalRecord);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新审批记录。
     */
    @PutMapping
    public ResultData update(@RequestBody SysApprovalRecord approvalRecord) {
        boolean flag = sysApprovalRecordService.updateApprovalRecord(approvalRecord);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除审批记录，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = sysApprovalRecordService.removeApprovalRecordById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出审批记录 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String bizType,
                      @RequestParam(required = false) Long bizId,
                      @RequestParam(required = false) String approvalNode,
                      @RequestParam(required = false) Long approverId,
                      @RequestParam(required = false) String approvalResult,
                      @RequestParam(required = false)
                      @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                      @RequestParam(required = false)
                      @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<SysApprovalRecord> approvalRecords = sysApprovalRecordService.listApprovalRecords(
                bizType, bizId, approvalNode, approverId, approvalResult, startTime, endTime);
        excelExportUtil.exportExcel(response, approvalRecords, SysApprovalRecord.class, "审批记录列表", "审批记录列表");
    }
}
