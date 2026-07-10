package com.neuedu.ems.controller.ai;

import com.neuedu.ems.common.AiQuestion;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.service.ai.AiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MES 智能助手控制器。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 查询 AI 是否启用和配置完整，不会向第三方发起请求。
     */
    @GetMapping("/status")
    public ResultData status() {
        return ResultData.success(ResultCode.SUCCESS, aiService.getStatus());
    }

    /**
     * 普通 MES 问答。
     */
    @PostMapping("/chat")
    public ResultData chat(@Valid @RequestBody AiQuestion request) {
        return ResultData.success(ResultCode.SUCCESS, aiService.chat(request));
    }

    /**
     * 使用后端真实看板汇总生成数据解读。
     */
    @PostMapping("/dashboard-insight")
    public ResultData dashboardInsight(@Valid @RequestBody AiQuestion request) {
        return ResultData.success(ResultCode.SUCCESS, aiService.getDashboardInsight(request));
    }

    /**
     * 根据模块和后端汇总数据生成只读业务建议。
     */
    @PostMapping("/module-suggestion")
    public ResultData moduleSuggestion(@Valid @RequestBody AiQuestion request) {
        return ResultData.success(ResultCode.SUCCESS, aiService.getModuleSuggestion(request));
    }
}
