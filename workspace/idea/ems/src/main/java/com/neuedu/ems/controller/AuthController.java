package com.neuedu.ems.controller;

import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.SysUser;
import com.neuedu.ems.service.AuthService;
import com.neuedu.ems.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RequestMapping("/api/auth")
@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResultData login(@RequestBody LoginRequest request) {
        SysUser user = authService.login(request.getUsername(), request.getPassword());
        if (user != null) {
            // 登录成功，生成一个令牌
            String token = jwtUtil.generateToken(user.getUserId(), user.getUsername());
            ResultData resultData = ResultData.success(ResultCode.SUCCESS, user);
            resultData.put("token", token);
            return resultData;
        }
        return ResultData.fail(ResultCode.USER_AND_PASSWORD_ERROR);
    }

    /**
     * 登录请求VO
     */
    @lombok.Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
