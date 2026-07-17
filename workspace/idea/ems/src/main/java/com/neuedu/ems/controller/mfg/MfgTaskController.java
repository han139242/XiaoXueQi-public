package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.common.AuthorizationContext;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.service.mfg.MfgTaskService;
import com.neuedu.ems.service.mfg.MfgTaskReminderService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 生产任务管理控制器。
 */
@RestController
@RequestMapping("/api/mfg/task")
public class MfgTaskController {

    @Autowired
    private MfgTaskService mfgTaskService;

    @Autowired
    private MfgTaskReminderService mfgTaskReminderService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询生产任务列表，支持任务编号、状态、派工单、工单、工序和工位筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配任务编号），可选
     * @param status 任务状态，可选
     * @param dispatchId 派工单ID，可选
     * @param workOrderId 工单ID，可选
     * @param operationId 工序ID，可选
     * @param stationId 工位ID，可选
     * @param employeeId 已分配员工ID，可选
     * @param sort 创建时间排序，可选值newest、oldest
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long dispatchId,
                           @RequestParam(required = false) Long workOrderId,
                           @RequestParam(required = false) Long operationId,
                           @RequestParam(required = false) Long stationId,
                           @RequestParam(required = false) Long employeeId,
                           @RequestParam(required = false) String sort,
                           @RequestParam(defaultValue = "false") boolean fullView) {
        Page<MfgTask> result = fullView
                ? mfgTaskService.pageTasksForView(current, size, keyword, status, dispatchId, workOrderId,
                operationId, stationId, employeeId, sort)
                : mfgTaskService.pageTasks(current, size, keyword, status, dispatchId, workOrderId,
                operationId, stationId, employeeId, sort);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    @GetMapping("/reminder-summary")
    public ResultData reminderSummary(@RequestAttribute("authorizationContext") AuthorizationContext context) {
        return ResultData.success(ResultCode.SUCCESS, mfgTaskReminderService.getCurrentUserSummary(context));
    }

    /**
     * 根据生产任务ID查询生产任务详情。
     * @param taskId 生产任务ID
     * @return 生产任务实体信息
     */
    @GetMapping("/{taskId}")
    public ResultData getTask(@PathVariable("taskId") Long taskId,
                              @RequestParam(defaultValue = "false") boolean fullView) {
        MfgTask task = fullView ? mfgTaskService.getTaskForView(taskId) : mfgTaskService.getTaskById(taskId);
        return ResultData.success(ResultCode.SUCCESS, task);
    }

    /**
     * 新增生产任务。
     * @param task 生产任务实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody MfgTask task) {
        boolean flag = mfgTaskService.saveTask(task);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新生产任务信息。
     * @param task 生产任务实体（JSON格式，必须包含taskId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgTask task) {
        boolean flag = mfgTaskService.updateTask(task);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 开始生产任务，将任务状态改为RUNNING。
     * @param taskId 生产任务ID
     * @param request 请求体，支持stationId参数
     * @return 操作结果
     */
    @PutMapping("/{taskId}/start")
    public ResultData start(@PathVariable("taskId") Long taskId,
                            @RequestAttribute("userId") Long userId,
                            @RequestBody(required = false) Map<String, Object> request) {
        boolean flag = mfgTaskService.startTask(taskId, getLong(request, "stationId"), userId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 完成生产任务，记录完成数量和不良数量并将任务状态改为FINISHED。
     * @param taskId 生产任务ID
     * @param request 请求体，支持completedQuantity、defectQuantity和remark参数
     * @return 操作结果
     */
    @PutMapping("/{taskId}/finish")
    public ResultData finish(@PathVariable("taskId") Long taskId,
                             @RequestAttribute("userId") Long userId,
                             @RequestBody(required = false) Map<String, Object> request) {
        boolean flag = mfgTaskService.finishTask(
                taskId,
                getBigDecimal(request, "completedQuantity"),
                getBigDecimal(request, "defectQuantity"),
                getString(request, "remark"),
                userId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    @PutMapping("/{taskId}/controlled-close")
    public ResultData controlledClose(@PathVariable("taskId") Long taskId,
                                      @RequestAttribute("userId") Long userId,
                                      @RequestBody Map<String, Object> request) {
        MfgTask compensationTask = mfgTaskService.controlledCloseTask(
                taskId, userId, getString(request, "reason"));
        return ResultData.success(ResultCode.SUCCESS,
                Map.of("compensationTaskId", compensationTask.getTaskId()));
    }

    @PutMapping("/{taskId}/withdraw")
    public ResultData withdraw(@PathVariable Long taskId,
                               @RequestAttribute("userId") Long userId,
                               @RequestBody(required = false) Map<String, Object> request) {
        mfgTaskService.withdrawTaskStart(taskId, userId, getString(request, "remark"));
        return ResultData.success(ResultCode.SUCCESS);
    }

    /**
     * 删除生产任务（软删除，将删除标志设为1）。
     * @param taskId 生产任务ID
     * @return 操作结果
     */
    @DeleteMapping("/{taskId}")
    public ResultData delete(@PathVariable("taskId") Long taskId) {
        boolean flag = mfgTaskService.removeTaskById(taskId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出生产任务列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配任务编号），可选
     * @param status 任务状态，可选
     * @param dispatchId 派工单ID，可选
     * @param workOrderId 工单ID，可选
     * @param operationId 工序ID，可选
     * @param stationId 工位ID，可选
     * @param employeeId 已分配员工ID，可选
     * @param sort 创建时间排序，可选值newest、oldest
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) Long dispatchId,
                      @RequestParam(required = false) Long workOrderId,
                      @RequestParam(required = false) Long operationId,
                      @RequestParam(required = false) Long stationId,
                      @RequestParam(required = false) Long employeeId,
                      @RequestParam(required = false) String sort) {
        List<MfgTask> tasks = mfgTaskService.listTasks(keyword, status, dispatchId, workOrderId, operationId, stationId, employeeId, sort);
        excelExportUtil.exportExcel(response, tasks, MfgTask.class, "生产任务列表", "生产任务列表");
    }

    /**
     * 从请求体中读取Long类型参数。
     * @param request 请求体Map
     * @param key 参数名
     * @return Long类型参数值
     */
    private Long getLong(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : Long.valueOf(value.toString());
    }

    /**
     * 从请求体中读取BigDecimal类型参数。
     * @param request 请求体Map
     * @param key 参数名
     * @return BigDecimal类型参数值
     */
    private BigDecimal getBigDecimal(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : new BigDecimal(value.toString());
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
}
