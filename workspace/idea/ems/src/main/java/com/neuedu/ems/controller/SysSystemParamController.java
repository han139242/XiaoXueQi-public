package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.SysSystemParam;
import com.neuedu.ems.service.SysSystemParamService;
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

@RestController
@RequestMapping("/api/sys/system-param")
public class SysSystemParamController {

    @Autowired
    private SysSystemParamService sysSystemParamService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询系统参数列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String moduleCode) {
        Page<SysSystemParam> result = sysSystemParamService.pageSystemParams(current, size, keyword, moduleCode);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询系统参数。
     */
    @GetMapping("/{paramId}")
    public ResultData getSystemParam(@PathVariable("paramId") Long id) {
        SysSystemParam systemParam = sysSystemParamService.getSystemParamById(id);
        return ResultData.success(ResultCode.SUCCESS, systemParam);
    }

    /**
     * 新增系统参数。
     */
    @PostMapping
    public ResultData save(@RequestBody SysSystemParam systemParam) {
        boolean flag = sysSystemParamService.saveSystemParam(systemParam);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新系统参数。
     */
    @PutMapping
    public ResultData update(@RequestBody SysSystemParam systemParam) {
        boolean flag = sysSystemParamService.updateSystemParam(systemParam);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除系统参数，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = sysSystemParamService.removeSystemParamById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出系统参数 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String moduleCode) {
        List<SysSystemParam> systemParams = sysSystemParamService.listSystemParams(keyword, moduleCode);
        excelExportUtil.exportExcel(response, systemParams, SysSystemParam.class, "系统参数列表", "系统参数列表");
    }
}
