package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgTaskOperator;
import com.neuedu.ems.service.mfg.MfgTaskOperatorService;
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
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 任务人员分配管理控制器。
 */
@RestController
@RequestMapping("/api/mfg/task-operator")
public class MfgTaskOperatorController {

    @Autowired
    private MfgTaskOperatorService mfgTaskOperatorService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询任务人员分配列表，支持任务、员工和任务角色筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param taskId 任务ID，可选
     * @param employeeId 员工ID，可选
     * @param roleInTask 任务角色，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long taskId,
                           @RequestParam(required = false) Long employeeId,
                           @RequestParam(required = false) String roleInTask) {
        Page<MfgTaskOperator> result = mfgTaskOperatorService.pageTaskOperators(current, size, taskId, employeeId, roleInTask);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据任务ID查询任务人员分配列表。
     * @param taskId 任务ID
     * @return 任务人员分配列表
     */
    @GetMapping("/task/{taskId}")
    public ResultData listByTask(@PathVariable("taskId") Long taskId) {
        List<MfgTaskOperator> operators = mfgTaskOperatorService.listTaskOperatorsByTaskId(taskId);
        return ResultData.success(ResultCode.SUCCESS, operators);
    }

    /**
     * 根据任务人员分配ID查询详情。
     * @param id 任务人员分配ID
     * @return 任务人员分配实体信息
     */
    @GetMapping("/{id}")
    public ResultData getTaskOperator(@PathVariable("id") Long id) {
        MfgTaskOperator taskOperator = mfgTaskOperatorService.getTaskOperatorById(id);
        return ResultData.success(ResultCode.SUCCESS, taskOperator);
    }

    /**
     * 新增任务人员分配。
     * @param taskOperator 任务人员分配实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody MfgTaskOperator taskOperator,
                           @RequestAttribute("userId") Long userId) {
        boolean flag = mfgTaskOperatorService.saveTaskOperator(taskOperator, userId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新任务人员分配信息。
     * @param taskOperator 任务人员分配实体（JSON格式，必须包含id）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgTaskOperator taskOperator,
                             @RequestAttribute("userId") Long userId) {
        boolean flag = mfgTaskOperatorService.updateTaskOperator(taskOperator, userId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除任务人员分配（软删除，将删除标志设为1）。
     * @param id 任务人员分配ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id,
                             @RequestAttribute("userId") Long userId) {
        boolean flag = mfgTaskOperatorService.removeTaskOperatorById(id, userId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出任务人员分配列表为Excel文件。
     * @param response HTTP响应对象
     * @param taskId 任务ID，可选
     * @param employeeId 员工ID，可选
     * @param roleInTask 任务角色，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) Long taskId,
                      @RequestParam(required = false) Long employeeId,
                      @RequestParam(required = false) String roleInTask) {
        List<MfgTaskOperator> taskOperators = mfgTaskOperatorService.listTaskOperators(taskId, employeeId, roleInTask);
        excelExportUtil.exportExcel(response, taskOperators, MfgTaskOperator.class, "任务人员分配列表", "任务人员分配列表");
    }
}
