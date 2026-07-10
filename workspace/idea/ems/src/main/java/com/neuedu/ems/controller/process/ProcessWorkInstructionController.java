package com.neuedu.ems.controller.process;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.process.ProcessWorkInstruction;
import com.neuedu.ems.service.process.ProcessWorkInstructionService;
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
@RequestMapping("/api/process/work-instruction")
public class ProcessWorkInstructionController {

    @Autowired
    private ProcessWorkInstructionService processWorkInstructionService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询作业指导书列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long operationId,
                           @RequestParam(required = false) Long productId,
                           @RequestParam(required = false) String status) {
        Page<ProcessWorkInstruction> result = processWorkInstructionService.pageInstructions(
                current, size, keyword, operationId, productId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询作业指导书。
     */
    @GetMapping("/{instructionId}")
    public ResultData getInstruction(@PathVariable("instructionId") Long id) {
        ProcessWorkInstruction instruction = processWorkInstructionService.getInstructionById(id);
        return ResultData.success(ResultCode.SUCCESS, instruction);
    }

    /**
     * 新增作业指导书。
     */
    @PostMapping
    public ResultData save(@RequestBody ProcessWorkInstruction instruction) {
        boolean flag = processWorkInstructionService.saveInstruction(instruction);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新作业指导书。
     */
    @PutMapping
    public ResultData update(@RequestBody ProcessWorkInstruction instruction) {
        boolean flag = processWorkInstructionService.updateInstruction(instruction);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除作业指导书，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = processWorkInstructionService.removeInstructionById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出作业指导书 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long operationId,
                      @RequestParam(required = false) Long productId,
                      @RequestParam(required = false) String status) {
        List<ProcessWorkInstruction> instructions = processWorkInstructionService.listInstructions(
                keyword, operationId, productId, status);
        excelExportUtil.exportExcel(response, instructions, ProcessWorkInstruction.class, "作业指导书列表", "作业指导书列表");
    }
}
