package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.ProcessParameter;
import com.neuedu.ems.service.ProcessParameterService;
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
@RequestMapping("/api/process/parameter")
public class ProcessParameterController {

    @Autowired
    private ProcessParameterService processParameterService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询工艺参数列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long operationId,
                           @RequestParam(required = false) String keyword) {
        Page<ProcessParameter> result = processParameterService.pageParameters(current, size, operationId, keyword);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询工艺参数。
     */
    @GetMapping("/{parameterId}")
    public ResultData getParameter(@PathVariable("parameterId") Long id) {
        ProcessParameter parameter = processParameterService.getParameterById(id);
        return ResultData.success(ResultCode.SUCCESS, parameter);
    }

    /**
     * 新增工艺参数。
     */
    @PostMapping
    public ResultData save(@RequestBody ProcessParameter parameter) {
        boolean flag = processParameterService.saveParameter(parameter);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新工艺参数。
     */
    @PutMapping
    public ResultData update(@RequestBody ProcessParameter parameter) {
        boolean flag = processParameterService.updateParameter(parameter);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除工艺参数，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = processParameterService.removeParameterById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出工艺参数 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long operationId,
                      @RequestParam(required = false) String keyword) {
        List<ProcessParameter> parameters = processParameterService.listParameters(operationId, keyword);
        excelExportUtil.exportExcel(response, parameters, ProcessParameter.class, "工艺参数列表", "工艺参数列表");
    }
}
