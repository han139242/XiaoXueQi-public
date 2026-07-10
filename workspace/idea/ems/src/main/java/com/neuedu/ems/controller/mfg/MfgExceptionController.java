package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgException;
import com.neuedu.ems.service.mfg.MfgExceptionService;
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
import java.util.Map;

/**
 * 生产异常管理控制器。
 */
@RestController
@RequestMapping("/api/mfg/exception")
public class MfgExceptionController {

    @Autowired
    private MfgExceptionService mfgExceptionService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询生产异常列表，支持异常编号、类型、状态、任务和工位筛选。
     * @param current 当前页码，默认为1
     * @param size 每页记录数，默认为10
     * @param keyword 关键字（匹配异常编号），可选
     * @param exceptionType 异常类型，可选
     * @param status 异常状态，可选
     * @param taskId 任务ID，可选
     * @param stationId 工位ID，可选
     * @return 分页结果
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String exceptionType,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) Long taskId,
                           @RequestParam(required = false) Long stationId) {
        Page<MfgException> result = mfgExceptionService.pageExceptions(current, size, keyword, exceptionType, status, taskId, stationId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据生产异常ID查询详情。
     * @param exceptionId 生产异常ID
     * @return 生产异常实体信息
     */
    @GetMapping("/{exceptionId}")
    public ResultData getException(@PathVariable("exceptionId") Long exceptionId) {
        MfgException exception = mfgExceptionService.getExceptionById(exceptionId);
        return ResultData.success(ResultCode.SUCCESS, exception);
    }

    /**
     * 新增生产异常。
     * @param exception 生产异常实体（JSON格式）
     * @return 操作结果
     */
    @PostMapping
    public ResultData save(@RequestBody MfgException exception) {
        boolean flag = mfgExceptionService.saveException(exception);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新生产异常信息。
     * @param exception 生产异常实体（JSON格式，必须包含exceptionId）
     * @return 操作结果
     */
    @PutMapping
    public ResultData update(@RequestBody MfgException exception) {
        boolean flag = mfgExceptionService.updateException(exception);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 处理生产异常，记录处理结果并将状态改为CLOSED。
     * @param exceptionId 生产异常ID
     * @param request 请求体，支持handleResult和remark参数
     * @return 操作结果
     */
    @PutMapping("/{exceptionId}/handle")
    public ResultData handle(@PathVariable("exceptionId") Long exceptionId,
                             @RequestBody(required = false) Map<String, Object> request) {
        boolean flag = mfgExceptionService.handleException(exceptionId, getString(request, "handleResult"), getString(request, "remark"));
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除生产异常（软删除，将删除标志设为1）。
     * @param exceptionId 生产异常ID
     * @return 操作结果
     */
    @DeleteMapping("/{exceptionId}")
    public ResultData delete(@PathVariable("exceptionId") Long exceptionId) {
        boolean flag = mfgExceptionService.removeExceptionById(exceptionId);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出生产异常列表为Excel文件。
     * @param response HTTP响应对象
     * @param keyword 关键字（匹配异常编号），可选
     * @param exceptionType 异常类型，可选
     * @param status 异常状态，可选
     * @param taskId 任务ID，可选
     * @param stationId 工位ID，可选
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) String exceptionType,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) Long taskId,
                      @RequestParam(required = false) Long stationId) {
        List<MfgException> exceptions = mfgExceptionService.listExceptions(keyword, exceptionType, status, taskId, stationId);
        excelExportUtil.exportExcel(response, exceptions, MfgException.class, "生产异常列表", "生产异常列表");
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
