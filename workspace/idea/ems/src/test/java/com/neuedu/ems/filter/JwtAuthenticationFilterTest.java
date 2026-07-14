package com.neuedu.ems.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuedu.ems.common.AuthorizationContext;
import com.neuedu.ems.security.PermissionCodeResolver;
import com.neuedu.ems.service.impl.sys.TokenRevocationService;
import com.neuedu.ems.service.sys.PermissionService;
import com.neuedu.ems.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private JwtUtil jwtUtil;
    private PermissionService permissionService;
    private TokenRevocationService tokenRevocationService;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        jwtUtil = mock(JwtUtil.class);
        permissionService = mock(PermissionService.class);
        tokenRevocationService = mock(TokenRevocationService.class);
        ReflectionTestUtils.setField(filter, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(filter, "permissionService", permissionService);
        ReflectionTestUtils.setField(filter, "permissionCodeResolver", new PermissionCodeResolver());
        ReflectionTestUtils.setField(filter, "tokenRevocationService", tokenRevocationService);
        ReflectionTestUtils.setField(filter, "objectMapper", new ObjectMapper());
    }

    @Test
    void returns403WhenAuthenticatedUserLacksPermission() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/sys/user/page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(permissionService.getContext(7L)).thenReturn(context(Set.of("dashboard:summary:view")));

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
        assertEquals(403, new ObjectMapper().readTree(response.getContentAsString()).get("code").asInt());
    }

    @Test
    void letsAuthorizedRequestReachTheChain() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/sys/user/page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(permissionService.getContext(7L)).thenReturn(context(Set.of("sys:user:view")));

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(7L, chain.getRequest().getAttribute("userId"));
    }

    private MockHttpServletRequest authenticatedRequest(String method, String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("Authorization", "Bearer token");
        when(jwtUtil.extractTokenFromHeader("Bearer token")).thenReturn("token");
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("token")).thenReturn(7L);
        when(tokenRevocationService.isRevoked("token")).thenReturn(false);
        return request;
    }

    private AuthorizationContext context(Set<String> permissions) {
        return new AuthorizationContext(7L, 21L, 3L, "ALL", Set.of("ROLE"), permissions, Set.of());
    }
}
