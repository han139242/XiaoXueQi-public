package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgScheduleItem;
import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import com.neuedu.ems.service.mfg.MfgScheduleScenarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能排程方案计算与查询控制器。
 */
@RestController
@RequestMapping("/api/mfg/schedule-scenario")
public class MfgScheduleScenarioController {

    @Autowired
    private MfgScheduleScenarioService mfgScheduleScenarioService;

    @GetMapping("/page")
    public ResultData page(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        Page<MfgScheduleScenario> result = mfgScheduleScenarioService.pageScenarios(current, size, keyword, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    @GetMapping("/{scenarioId}")
    public ResultData detail(@PathVariable Long scenarioId) {
        return ResultData.success(ResultCode.SUCCESS, mfgScheduleScenarioService.getScenarioById(scenarioId));
    }

    @GetMapping("/{scenarioId}/items")
    public ResultData items(@PathVariable Long scenarioId) {
        List<MfgScheduleItem> result = mfgScheduleScenarioService.listScenarioItems(scenarioId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    @PostMapping("/manual-draft/open")
    public ResultData openManualDraft(@RequestAttribute("userId") Long operatorId) {
        return ResultData.success(ResultCode.SUCCESS, mfgScheduleScenarioService.openManualDraft(operatorId));
    }

    @GetMapping("/manual-board")
    public ResultData manualBoard(@RequestAttribute("userId") Long operatorId) {
        return ResultData.success(ResultCode.SUCCESS,
                mfgScheduleScenarioService.getManualScheduleBoard(operatorId));
    }

    @PostMapping("/manual-board/feasibility")
    public ResultData analyzeManualBoard(@RequestAttribute("userId") Long operatorId) {
        return ResultData.success(ResultCode.SUCCESS,
                mfgScheduleScenarioService.analyzeManualScheduleBoard(operatorId));
    }

    @GetMapping("/{scenarioId}/orders/{productionOrderId}/eligible-lines")
    public ResultData eligibleLines(@PathVariable Long scenarioId, @PathVariable Long productionOrderId) {
        return ResultData.success(ResultCode.SUCCESS,
                mfgScheduleScenarioService.listEligibleLines(scenarioId, productionOrderId));
    }

    @PostMapping("/{scenarioId}/items")
    public ResultData addManualItem(@PathVariable Long scenarioId, @RequestAttribute("userId") Long operatorId,
                                    @RequestBody ManualScheduleAddRequest request) {
        return ResultData.success(ResultCode.SUCCESS, mfgScheduleScenarioService.addManualItem(scenarioId,
                request.getScenarioVersion(), request.getProductionOrderId(), request.getLineId(), operatorId));
    }

    @PutMapping("/{scenarioId}/items/{scheduleItemId}/placement")
    public ResultData updateManualPlacement(@PathVariable Long scenarioId, @PathVariable Long scheduleItemId,
                                            @RequestAttribute("userId") Long operatorId,
                                            @RequestBody ManualSchedulePlacementRequest request) {
        return ResultData.success(ResultCode.SUCCESS, mfgScheduleScenarioService.updateManualPlacement(scenarioId,
                scheduleItemId, request.getScenarioVersion(), request.getItemVersion(), request.getLineId(),
                request.getPlannedStartTime(), request.getPlannedEndTime(), request.getRemark(), operatorId));
    }

    @DeleteMapping("/{scenarioId}/items/{scheduleItemId}")
    public ResultData removeManualItem(@PathVariable Long scenarioId, @PathVariable Long scheduleItemId,
                                       @RequestAttribute("userId") Long operatorId,
                                       @RequestBody ManualScheduleRemoveRequest request) {
        return ResultData.success(ResultCode.SUCCESS, mfgScheduleScenarioService.removeManualItem(scenarioId,
                scheduleItemId, request.getScenarioVersion(), request.getItemVersion(), operatorId));
    }

    @PutMapping("/{scenarioId}/items/{scheduleItemId}/confirm")
    public ResultData confirmManualItem(@PathVariable Long scenarioId, @PathVariable Long scheduleItemId,
                                        @RequestAttribute("userId") Long operatorId,
                                        @RequestBody ManualScheduleConfirmRequest request) {
        return ResultData.success(ResultCode.SUCCESS, mfgScheduleScenarioService.confirmManualItem(
                scenarioId, scheduleItemId, request.getScenarioVersion(), request.getItemVersion(),
                request.getRemark(), operatorId));
    }

    @PostMapping("/{scenarioId}/validate")
    public ResultData validateManualDraft(@PathVariable Long scenarioId, @RequestAttribute("userId") Long operatorId,
                                          @RequestBody ManualScheduleVersionRequest request) {
        return ResultData.success(ResultCode.SUCCESS,
                mfgScheduleScenarioService.validateManualDraft(scenarioId, request.getScenarioVersion(), operatorId));
    }

    @PostMapping("/{scenarioId}/orders/{productionOrderId}/ai-advice")
    public ResultData orderAiAdvice(@PathVariable Long scenarioId, @PathVariable Long productionOrderId,
                                    @RequestBody ManualScheduleVersionRequest request) {
        return ResultData.success(ResultCode.SUCCESS, mfgScheduleScenarioService.generateOrderAiAdvice(scenarioId,
                productionOrderId, request.getScenarioVersion()));
    }

    /**
     * 按当前 DRAFT 生产订单计算一个新的排程方案。
     * 此接口不创建正式生产计划，也不改变生产订单状态。
     */
    @PostMapping("/calculate")
    public ResultData calculate(@RequestBody MfgScheduleScenario request) {
        return ResultData.success(ResultCode.SUCCESS, mfgScheduleScenarioService.calculateScenario(request));
    }

    /**
     * 为已计算方案生成只读 AI 风险建议，不会修改排程、计划或订单。
     */
    @PostMapping("/{scenarioId}/ai-advice")
    public ResultData generateAiAdvice(@PathVariable Long scenarioId,
                                       @RequestBody(required = false) ScheduleScenarioAiAdviceRequest request) {
        return ResultData.success(ResultCode.SUCCESS, mfgScheduleScenarioService.generateAiAdvice(
                scenarioId, request == null ? null : request.getVersion()));
    }

    /**
     * 人工确认已计算且无冲突的排程方案，原子创建生产计划并下达生产订单。
     */
    @PutMapping("/{scenarioId}/confirm")
    public ResultData confirm(@PathVariable Long scenarioId,
                              @RequestAttribute("userId") Long operatorId,
                              @RequestBody(required = false) ScheduleScenarioConfirmRequest request) {
        return ResultData.success(ResultCode.SUCCESS, mfgScheduleScenarioService.confirmScenario(
                scenarioId, request == null ? null : request.getVersion(),
                operatorId, request == null ? null : request.getRemark()));
    }

    @lombok.Data
    public static class ScheduleScenarioConfirmRequest {
        private Integer version;
        private String remark;
    }

    @lombok.Data
    public static class ScheduleScenarioAiAdviceRequest {
        private Integer version;
    }

    @lombok.Data
    public static class ManualScheduleVersionRequest {
        private Integer scenarioVersion;
    }

    @lombok.Data
    public static class ManualScheduleAddRequest {
        private Integer scenarioVersion;
        private Long productionOrderId;
        private Long lineId;
    }

    @lombok.Data
    public static class ManualSchedulePlacementRequest {
        private Integer scenarioVersion;
        private Integer itemVersion;
        private Long lineId;
        private java.time.LocalDateTime plannedStartTime;
        private java.time.LocalDateTime plannedEndTime;
        private String remark;
    }

    @lombok.Data
    public static class ManualScheduleRemoveRequest {
        private Integer scenarioVersion;
        private Integer itemVersion;
    }

    @lombok.Data
    public static class ManualScheduleConfirmRequest {
        private Integer scenarioVersion;
        private Integer itemVersion;
        private String remark;
    }
}
