package com.neuedu.ems.common;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

/**
 * 前端传输的问答实体
 */

public class AiQuestion {
    // 用户提问（非空校验+长度限制）
    @NotBlank(message = "问题不能为空")
    @Length(max = 500, message = "问题长度不能超过500字")
    private String question;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}