package com.neuedu.ems.common;

import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.exception.AiServiceException;
import com.neuedu.ems.exception.AccountStatusException;
import com.neuedu.ems.exception.OptimisticLockConflictException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一捕获并处理所有控制器层抛出的异常
 */
//@Slf4j
@RestControllerAdvice  // 等同于 @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 返回明确的账号停用、锁定或状态异常提示。
     */
    @ExceptionHandler(AccountStatusException.class)
    public ResultData handleAccountStatusException(AccountStatusException e) {
        log.info("账号登录受限：{}", e.getMessage());
        return ResultData.fail(e.getResultCode());
    }

    @ExceptionHandler(OptimisticLockConflictException.class)
    public ResultData handleOptimisticLockConflict(OptimisticLockConflictException e) {
        log.info("乐观锁冲突：{}", e.getMessage());
        return ResultData.fail(ResultCode.DATA_CONFLICT);
    }

    @ExceptionHandler(MyBatisSystemException.class)
    public ResultData handleMyBatisSystemException(MyBatisSystemException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockConflictException conflict) {
                return handleOptimisticLockConflict(conflict);
            }
        }
        log.error("数据库访问异常", e);
        return ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 处理自定义业务异常（优先级最高）
     */
    @ExceptionHandler(BusinessException.class)
    public ResultData handleBusinessException(BusinessException e) {
        // 业务异常只打印info级别日志，不打印堆栈（避免日志冗余）
        //log.info("业务异常：{}", e.getMessage());
        return ResultData.fail(ResultCode.BUSINESS_ERROR);
    }

    /**
     * 处理参数校验异常（@Valid/@Validated 校验失败）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResultData handleValidationException(Exception e) {
        String errorMsg;

        if (e instanceof MethodArgumentNotValidException) {
            // 请求体参数校验失败
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;

            errorMsg = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("；"));
        } else {
            // 请求参数绑定校验失败（如路径参数、请求头参数）
            BindException ex = (BindException) e;
            errorMsg = ex.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("；"));
        }
        log.warn("参数校验异常：{}", errorMsg);
        return ResultData.fail(ResultCode.PARAM_ERROR);
    }

    /**
     * 处理404异常（需要在配置中开启：spring.mvc.throw-exception-if-no-handler-found=true）
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultData handle404Exception(NoHandlerFoundException e) {
        log.warn("404异常：请求路径不存在 - {}", e.getRequestURL());
        return ResultData.fail(ResultCode.NOT_FOUND);
    }

    /**
     * AI 服务错误只返回可公开的简短原因，不暴露第三方响应、密钥或调用栈。
     */
    @ExceptionHandler(AiServiceException.class)
    public ResultData handleAiServiceException(AiServiceException e) {
        log.warn("AI服务调用失败：{}", e.getMessage());
        return ResultData.fail(ResultCode.BUSINESS_ERROR, Map.of("message", e.getMessage()));
    }

    /**
     * 处理所有未捕获的系统异常（兜底处理，优先级最低）
     */
    @ExceptionHandler(Exception.class)
    public ResultData handleSystemException(Exception e) {
        // 系统异常打印error级别日志，包含堆栈信息，方便排查问题
        log.error("系统异常", e);
        // 给前端返回通用错误信息，避免暴露系统内部细节
        return ResultData.fail(ResultCode.FAILED);
    }
}
