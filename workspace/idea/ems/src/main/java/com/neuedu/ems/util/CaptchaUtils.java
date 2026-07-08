package com.neuedu.ems.util;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 验证码工具类（基于Hutool实现）
 * @Component：标识为Spring组件，可注入使用
 */
@Component
public class CaptchaUtils {

    /**
     * 生成图形验证码（线干扰型）
     * @param width 验证码图片宽度
     * @param height 验证码图片高度
     * @param codeLength 验证码字符长度
     * @param interfereCount 干扰线数量
     * @return LineCaptcha 对象（包含验证码图片和验证码字符串）
     */
    public LineCaptcha generateCaptcha(int width, int height, int codeLength, int interfereCount) {
        // 使用Hutool的CaptchaUtil生成线干扰验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(width, height, codeLength, interfereCount);
        // 验证码字符为随机数字+字母（默认）
        return lineCaptcha;
    }

    /**
     * 将验证码图片写入HttpServletResponse（返回给前端）
     * @param captcha 验证码对象
     * @param response HttpServletResponse
     * @throws IOException IO异常（图片写入失败）
     */
    public void writeCaptchaToResponse(LineCaptcha captcha, HttpServletResponse response) throws IOException {
        // 设置响应头：返回图片类型
        response.setContentType("image/png");
        // 禁止缓存（避免验证码图片缓存导致刷新无效）
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        // 获取输出流，写入验证码图片
        ServletOutputStream outputStream = response.getOutputStream();
        captcha.write(outputStream);
        // 刷新并关闭输出流
        outputStream.flush();
        outputStream.close();
    }
}