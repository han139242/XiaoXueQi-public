package com.neuedu.ems.controller.process;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.process.ProcessOperation;
import com.neuedu.ems.service.process.ProcessOperationService;
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
@RequestMapping("/api/process/operation")
public class ProcessOperationController {

    @Autowired
    private ProcessOperationService processOperationService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询工序列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<ProcessOperation> result = processOperationService.pageOperations(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询工序。
     */
    @GetMapping("/{operationId}")
    public ResultData getOperation(@PathVariable("operationId") Long id) {
        ProcessOperation operation = processOperationService.getOperationById(id);
        return ResultData.success(ResultCode.SUCCESS, operation);
    }

    /**
     * 新增工序。
     */
    @PostMapping
    public ResultData save(@RequestBody ProcessOperation operation) {
        boolean flag = processOperationService.saveOperation(operation);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新工序。
     */
    @PutMapping
    public ResultData update(@RequestBody ProcessOperation operation) {
        boolean flag = processOperationService.updateOperation(operation);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除工序，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = processOperationService.removeOperationById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出工序 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status) {
        List<ProcessOperation> operations = processOperationService.listOperations(keyword, status);
        excelExportUtil.exportExcel(response, operations, ProcessOperation.class, "工序列表", "工序列表");
    }
}
