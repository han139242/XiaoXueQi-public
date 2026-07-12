package com.neuedu.ems.filter;


import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.exception.JwtException;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.util.JwtConstants;
import com.neuedu.ems.util.JwtUtil;
import com.neuedu.ems.service.impl.sys.TokenRevocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT过滤器：拦截请求，验证token有效性
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ObjectMapper objectMapper; // Spring默认注入的JSON工具
    @Autowired
    private TokenRevocationService tokenRevocationService;


    // 放行的路径（无需鉴权的接口，如登录、注册、静态资源）
    private static final String[] EXCLUDE_PATHS = {
            "/api/auth/login",
            "/register",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/error",
            "/swagger-ui/",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/swagger-ui/**",
            "/upload/**",
            "/captcha/**",
            "/temp/**",
            "/favicon.ico"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 判断哪些访问路径不需要验证token令牌，直接放行
        try {
            // 1. 放行OPTIONS预检请求
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.setStatus(HttpServletResponse.SC_OK);
                filterChain.doFilter(request, response);
                return;
            }

            if(isExcludePath(request.getRequestURI())) {
                filterChain.doFilter(request,response);
                return;
            }

            // 获取没有前缀的token
            String token = jwtUtil.extractTokenFromHeader(request.getHeader(JwtConstants.JWT_HEADER));
            // 当token为空时，执行
            if(!StringUtils.hasText(token)) {
                throw new JwtException("没有登录，请先登录");
            }

            // 判断token是否有效
            if(!jwtUtil.validateToken(token)) {
                throw new JwtException("token无效或者过期");
            }
            if (tokenRevocationService.isRevoked(token)) {
                throw new JwtException("token已注销");
            }

            // 先把UserId存储到request中
            // 1: 从token中获取userId
            Long userId = jwtUtil.getUserIdFromToken(token);
            // 2: 将uerId放入到request中
            request.setAttribute("userId", userId);

            filterChain.doFilter(request, response);
        }catch (JwtException e) {
            log.debug("JWT request rejected: method={}, uri={}, reason={}",
                    request.getMethod(), request.getRequestURI(), e.getMessage());
            // 当捕获到异常信息之后，说明，token是无效的，应该给前端返回错误信息
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            ResultData resultData = ResultData.fail(ResultCode.TOKEN_ERROR);
            // "{"code": 702, "msg": "token无效或者过期"}"
            String resutString = objectMapper.writeValueAsString(resultData);

            response.getWriter().write(resutString);
        }catch (Exception e) {
            log.error("JWT filter failed: method={}, uri={}", request.getMethod(), request.getRequestURI(), e);
            throw new ServletException("JWT filter processing failed", e);
        }


    }
    /**
     * 核心过滤逻辑
     */


    /**
     * 判断当前请求路径是否需要放行
     * /user
     */
    private boolean isExcludePath(String requestUri) {
        for (String excludePath : EXCLUDE_PATHS) {
            // 支持通配符（如/swagger-ui/**）
            String path = excludePath.replace("/**", ".*");
            if (requestUri.matches(path)) {
                return true;
            }
        }
        return false;
    }
}
