package com.neuedu.ems.service.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.common.AiResponse;
import com.neuedu.ems.entity.mfg.MfgScheduleItem;
import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import com.neuedu.ems.vo.mfg.ManualScheduleCandidateLine;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能排程方案计算服务。
 */
public interface MfgScheduleScenarioService extends IService<MfgScheduleScenario> {

    Page<MfgScheduleScenario> pageScenarios(Integer current, Integer size, String keyword, String status);

    MfgScheduleScenario getScenarioById(Long scenarioId);

    List<MfgScheduleItem> listScenarioItems(Long scenarioId);

    MfgScheduleScenario openManualDraft(Long operatorId);

    List<ManualScheduleCandidateLine> listEligibleLines(Long scenarioId, Long productionOrderId);

    MfgScheduleItem addManualItem(Long scenarioId, Integer scenarioVersion, Long productionOrderId,
                                  Long lineId, Long operatorId);

    MfgScheduleItem updateManualPlacement(Long scenarioId, Long scheduleItemId, Integer scenarioVersion,
                                          Integer itemVersion, Long lineId, LocalDateTime plannedStartTime,
                                          LocalDateTime plannedEndTime, String remark, Long operatorId);

    MfgScheduleScenario removeManualItem(Long scenarioId, Long scheduleItemId, Integer scenarioVersion,
                                         Integer itemVersion, Long operatorId);

    MfgScheduleScenario validateManualDraft(Long scenarioId, Integer scenarioVersion, Long operatorId);

    AiResponse generateOrderAiAdvice(Long scenarioId, Long productionOrderId, Integer scenarioVersion);

    MfgScheduleScenario calculateScenario(MfgScheduleScenario request);

    MfgScheduleScenario confirmScenario(Long scenarioId, Integer version, Long operatorId, String remark);

    AiResponse generateAiAdvice(Long scenarioId, Integer version);
}
