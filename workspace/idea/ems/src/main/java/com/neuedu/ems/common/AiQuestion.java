package com.neuedu.ems.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * AI 问答请求。
 */
public class AiQuestion {

    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题长度不能超过500字")
    private String question;

    @Size(max = 64, message = "模块编码长度不能超过64个字符")
    private String module;

    @Size(max = 20, message = "上下文字段不能超过20项")
    private Map<String, Object> context;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
