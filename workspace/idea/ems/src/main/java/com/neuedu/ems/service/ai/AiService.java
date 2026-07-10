package com.neuedu.ems.service.ai;

import com.neuedu.ems.common.AiQuestion;
import com.neuedu.ems.common.AiResponse;

import java.util.Map;

/**
 * MES 智能助手业务接口。
 */
public interface AiService {

    Map<String, Object> getStatus();

    AiResponse chat(AiQuestion request);

    AiResponse getDashboardInsight(AiQuestion request);

    AiResponse getModuleSuggestion(AiQuestion request);
}
