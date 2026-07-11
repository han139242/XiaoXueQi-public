package com.neuedu.ems.service.impl.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.neuedu.ems.common.AiQuestion;
import com.neuedu.ems.common.AiResponse;
import com.neuedu.ems.config.AiProperties;
import com.neuedu.ems.exception.AiServiceException;
import com.neuedu.ems.service.dashboard.DashboardService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        assertEquals("high", status.get("reasoningEffort"));
        assertFalse(status.containsKey("apiKey"));
        assertFalse(status.containsValue(properties.getApiKey()));
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
        assertTrue(requestBody.get().contains("\"reasoning_effort\":\"high\""));
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
        properties.setApiUrl("https://blackaicoding.com/");
        AiServiceImpl service = createService(properties, client, mock(DashboardService.class));

        service.chat(question("系统怎么使用？", "general", null));

        assertEquals("https://blackaicoding.com/v1/chat/completions", requestUrl.get());
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

    private AiServiceImpl createService(AiProperties properties, OkHttpClient client,
                                        DashboardService dashboardService) {
        return new AiServiceImpl(properties, client, objectMapper, dashboardService);
    }

    private AiProperties enabledProperties() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiUrl("https://example.test/chat/completions");
        properties.setApiKey("test-api-key");
        properties.setModel("test-model");
        properties.setReasoningEffort("high");
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
