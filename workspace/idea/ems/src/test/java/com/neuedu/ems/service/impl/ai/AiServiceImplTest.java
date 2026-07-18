package com.neuedu.ems.service.impl.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neuedu.ems.common.AiQuestion;
import com.neuedu.ems.common.AiResponse;
import com.neuedu.ems.config.AiProperties;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.exception.AiServiceException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.service.dashboard.DashboardService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiServiceImplTest {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void statusDoesNotExposeApiKey() {
        AiProperties properties = enabledProperties();
        AiServiceImpl service = createService(properties, responseClient(200, upstreamResponse("测试"), null),
                mock(DashboardService.class));

        Map<String, Object> status = service.getStatus();

        assertEquals(true, status.get("available"));
        assertEquals("READY", status.get("state"));
        assertEquals("DEEPSEEK", status.get("provider"));
        assertFalse(status.containsKey("reasoningEffort"));
        assertFalse(status.containsKey("apiKey"));
        assertFalse(status.containsValue(properties.getApiKey()));
    }

    @Test
    void defaultPropertiesRequireApiKeyFromEnvironment() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        AiServiceImpl service = createService(properties, new OkHttpClient(), mock(DashboardService.class));

        Map<String, Object> status = service.getStatus();

        assertEquals(false, status.get("configured"));
        assertEquals(false, status.get("available"));
        assertEquals("MISCONFIGURED", status.get("state"));
    }

    @Test
    void disabledServiceRejectsChatWithoutCallingUpstream() {
        AiProperties properties = enabledProperties();
        properties.setEnabled(false);
        AiServiceImpl service = createService(properties, new OkHttpClient(), mock(DashboardService.class));

        AiServiceException exception = assertThrows(AiServiceException.class,
                () -> service.chat(question("系统怎么使用？", "general", null)));

        assertEquals("AI服务尚未启用", exception.getMessage());
    }

    @Test
    void chatParsesJsonAndFiltersSensitiveContext() throws Exception {
        String modelContent = "{\"answer\":\"库存预警需要关注\",\"suggestions\":[\"核对安全库存\",\"检查补货建议\"]}";
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        OkHttpClient client = responseClient(200, upstreamResponse(modelContent), request -> {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            requestBody.set(buffer.readUtf8());
            authorization.set(request.header("Authorization"));
        });
        AiServiceImpl service = createService(enabledProperties(), client, mock(DashboardService.class));
        AiQuestion request = question("库存情况如何？", "general", Map.of(
                "workOrderId", 12,
                "token", "never-send-token-123",
                "nested", Map.of("password", "never-send-password-456", "batchId", 8)));

        AiResponse result = service.chat(request);

        assertEquals("库存预警需要关注", result.getAnswer());
        assertEquals(List.of("核对安全库存", "检查补货建议"), result.getSuggestions());
        assertEquals("test-model", result.getModel());
        assertNotNull(result.getGeneratedAt());
        assertTrue(requestBody.get().contains("workOrderId"));
        assertTrue(requestBody.get().contains("batchId"));
        assertTrue(requestBody.get().contains("\"model\":\"test-model\""));
        assertFalse(requestBody.get().contains("reasoning_effort"));
        assertFalse(requestBody.get().contains("temperature"));
        assertFalse(requestBody.get().contains("max_tokens"));
        assertFalse(requestBody.get().contains("never-send-token-123"));
        assertFalse(requestBody.get().contains("never-send-password-456"));
        assertEquals("Bearer test-api-key", authorization.get());
    }

    @Test
    void baseApiUrlIsExpandedToChatCompletionsEndpoint() {
        AtomicReference<String> requestUrl = new AtomicReference<>();
        OkHttpClient client = responseClient(200, upstreamResponse("测试"), request ->
                requestUrl.set(request.url().toString()));
        AiProperties properties = enabledProperties();
        properties.setApiUrl("https://api.deepseek.com");
        AiServiceImpl service = createService(properties, client, mock(DashboardService.class));

        service.chat(question("系统怎么使用？", "general", null));

        assertEquals("https://api.deepseek.com/v1/chat/completions", requestUrl.get());
    }

    @Test
    void generalChatIncludesPendingProductionOrderSummary() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        MfgProductionOrderMapper orderMapper = mock(MfgProductionOrderMapper.class);
        MfgProductionPlanMapper planMapper = mock(MfgProductionPlanMapper.class);
        MfgProductionOrder pendingOrder = new MfgProductionOrder();
        pendingOrder.setProductionOrderId(101L);
        pendingOrder.setProductionOrderNo("PO-DRAFT-001");
        pendingOrder.setOrderQuantity(new BigDecimal("300"));
        pendingOrder.setDeliveryDate(LocalDate.of(2026, 7, 20));
        pendingOrder.setStatus("DRAFT");
        when(orderMapper.selectList(any())).thenReturn(List.of(pendingOrder));
        when(planMapper.selectList(any())).thenReturn(List.of());
        AiServiceImpl service = createService(enabledProperties(), responseClient(200,
                upstreamResponse("{\"answer\":\"当前有1个待排订单\",\"suggestions\":[]}"), request -> {
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);
                    requestBody.set(buffer.readUtf8());
                }), mock(DashboardService.class), orderMapper, planMapper);

        AiResponse result = service.chat(question("现在有多少待排订单？", "general", null));

        assertEquals("当前有1个待排订单", result.getAnswer());
        assertTrue(requestBody.get().contains("pendingProductionOrders"));
        assertTrue(requestBody.get().contains("orderCount"));
        assertTrue(requestBody.get().contains("PO-DRAFT-001"));
    }

    @Test
    void plainTextModelResponseFallsBackToAnswer() {
        AiServiceImpl service = createService(enabledProperties(), responseClient(200,
                upstreamResponse("请先检查设备状态。"), null), mock(DashboardService.class));

        AiResponse result = service.chat(question("设备为什么停机？", "general", null));

        assertEquals("请先检查设备状态。", result.getAnswer());
        assertTrue(result.getSuggestions().isEmpty());
    }

    @Test
    void upstreamFailureReturnsSafeMessage() {
        AiServiceImpl service = createService(enabledProperties(), responseClient(500,
                "{\"error\":\"internal provider detail\"}", null), mock(DashboardService.class));

        AiServiceException exception = assertThrows(AiServiceException.class,
                () -> service.chat(question("分析生产情况", "general", null)));

        assertEquals("AI服务暂时不可用，请稍后重试", exception.getMessage());
        assertFalse(exception.getMessage().contains("provider"));
    }

    @Test
    void dashboardInsightLoadsRealDashboardAggregates() {
        DashboardService dashboardService = mock(DashboardService.class);
        LocalDate beginDate = LocalDate.of(2026, 7, 1);
        LocalDate endDate = LocalDate.of(2026, 7, 7);
        when(dashboardService.getSummary(beginDate, endDate)).thenReturn(Map.of("runningTaskCount", 2));
        when(dashboardService.getProductionTrend(beginDate, endDate)).thenReturn(List.of(Map.of("date", "2026-07-01")));
        when(dashboardService.getQualityStatistics(beginDate, endDate)).thenReturn(Map.of("qualifiedRate", 96));
        when(dashboardService.getEquipmentStatus()).thenReturn(Map.of("fault", 1));
        AiServiceImpl service = createService(enabledProperties(), responseClient(200,
                upstreamResponse("{\"answer\":\"整体稳定\",\"suggestions\":[]}"), null), dashboardService);
        AiQuestion request = question("解读本周看板", "dashboard",
                Map.of("beginDate", "2026-07-01", "endDate", "2026-07-07"));

        AiResponse result = service.getDashboardInsight(request);

        assertEquals("整体稳定", result.getAnswer());
        verify(dashboardService).getSummary(beginDate, endDate);
        verify(dashboardService).getProductionTrend(beginDate, endDate);
        verify(dashboardService).getQualityStatistics(beginDate, endDate);
        verify(dashboardService).getEquipmentStatus();
    }

    @Test
    void scheduleScenarioAdviceUsesOnlyProvidedSummary() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        DashboardService dashboardService = mock(DashboardService.class);
        AiServiceImpl service = createService(enabledProperties(), responseClient(200,
                upstreamResponse("{\"answer\":\"产线存在瓶颈\",\"suggestions\":[\"人工复核冲突订单\"]}"), request -> {
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);
                    requestBody.set(buffer.readUtf8());
                }), dashboardService);

        AiResponse result = service.getScheduleScenarioAdvice(Map.of(
                "scenario", Map.of("orderCount", 2, "conflictOrderCount", 1),
                "conflictReasons", Map.of("排程窗口有效产能不足", 1)));

        assertEquals("产线存在瓶颈", result.getAnswer());
        assertEquals(List.of("人工复核冲突订单"), result.getSuggestions());
        assertTrue(requestBody.get().contains("scheduleScenario"));
        assertTrue(requestBody.get().contains("排程窗口有效产能不足"));
        verifyNoInteractions(dashboardService);
    }

    @Test
    void manualBoardAdviceRequiresCurrentStateAndPendingPlacementSections() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AiServiceImpl service = createService(enabledProperties(), responseClient(200,
                upstreamResponse("{\"answer\":\"建议先安排 P1\",\"suggestions\":[]}"), request -> {
                    Buffer buffer = new Buffer();
                    request.body().writeTo(buffer);
                    requestBody.set(buffer.readUtf8());
                }), mock(DashboardService.class));

        service.getScheduleScenarioAdvice(Map.of(
                "analysisType", "MANUAL_SCHEDULE_BOARD_ADVICE",
                "pendingPlacementOptions", List.of(Map.of(
                        "reference", "P1",
                        "candidateLineOptions", "LINE-A：最早可排 07-18 08:00，预计完成 07-18 14:00，建议预留 360 分钟产线工时，优先级 1"))));

        assertTrue(requestBody.get().contains("当前排产状态"));
        assertTrue(requestBody.get().contains("待排订单安排建议"));
        assertTrue(requestBody.get().contains("P1"));
        assertTrue(requestBody.get().contains("LINE-A：最早可排 07-18 08:00"));
        assertTrue(requestBody.get().contains("每个待排订单及风险与优化建议必须各自独立成段，并以空行分隔"));
        assertTrue(requestBody.get().contains("不得描述字段、脱敏、上下文、接口、系统错误或内部处理"));
        assertFalse(requestBody.get().contains("[内容过深，已省略]"));
        assertTrue(requestBody.get().contains("不得声称已创建、调整或确认任何计划"));
    }

    private AiServiceImpl createService(AiProperties properties, OkHttpClient client,
                                        DashboardService dashboardService) {
        return createService(properties, client, dashboardService,
                mock(MfgProductionOrderMapper.class), mock(MfgProductionPlanMapper.class));
    }

    private AiServiceImpl createService(AiProperties properties, OkHttpClient client,
                                        DashboardService dashboardService, MfgProductionOrderMapper orderMapper,
                                        MfgProductionPlanMapper planMapper) {
        return new AiServiceImpl(properties, client, objectMapper, dashboardService, orderMapper, planMapper);
    }

    private AiProperties enabledProperties() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiUrl("https://example.test/chat/completions");
        properties.setApiKey("test-api-key");
        properties.setModel("test-model");
        return properties;
    }

    private AiQuestion question(String text, String module, Map<String, Object> context) {
        AiQuestion question = new AiQuestion();
        question.setQuestion(text);
        question.setModule(module);
        question.setContext(context);
        return question;
    }

    private String upstreamResponse(String content) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", "test-model");
        root.putArray("choices").addObject().putObject("message").put("content", content);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private OkHttpClient responseClient(int code, String body, RequestInspector inspector) {
        return new OkHttpClient.Builder().addInterceptor(chain -> {
            if (inspector != null) {
                inspector.inspect(chain.request());
            }
            return new Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("test")
                    .body(ResponseBody.create(body, JSON))
                    .build();
        }).build();
    }

    @FunctionalInterface
    private interface RequestInspector {
        void inspect(okhttp3.Request request) throws IOException;
    }
}
