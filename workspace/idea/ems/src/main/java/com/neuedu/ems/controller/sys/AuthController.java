package com.neuedu.ems.controller.sys;

import com.neuedu.ems.common.AuthUserInfo;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.service.sys.AuthService;
import com.neuedu.ems.service.impl.sys.TokenRevocationService;
import com.neuedu.ems.util.JwtUtil;
import com.neuedu.ems.util.JwtConstants;
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

    @Autowired
    private TokenRevocationService tokenRevocationService;

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
     * 获取当前登录用户、角色和权限。
     */
    @GetMapping("/info")
    public ResultData info(@RequestAttribute("userId") Long userId) {
        AuthUserInfo userInfo = authService.getUserInfo(userId);
        return userInfo == null
                ? ResultData.fail(ResultCode.NOT_FOUND)
                : ResultData.success(ResultCode.SUCCESS, userInfo);
    }

    /**
     * 注销当前令牌并清理登录缓存。
     */
    @PostMapping("/logout")
    public ResultData logout(@RequestAttribute("userId") Long userId,
                             @RequestHeader(JwtConstants.JWT_HEADER) String authorization) throws Exception {
        String token = jwtUtil.extractTokenFromHeader(authorization);
        tokenRevocationService.revoke(token, jwtUtil.getExpirationFromToken(token));
        authService.logout(userId);
        return ResultData.success(ResultCode.SUCCESS);
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
