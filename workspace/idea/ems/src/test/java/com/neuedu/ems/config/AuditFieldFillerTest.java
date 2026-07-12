package com.neuedu.ems.config;

import com.neuedu.ems.entity.sys.SysUser;
import org.apache.ibatis.mapping.SqlCommandType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuditFieldFillerTest {

    private final AuditFieldFiller filler = new AuditFieldFiller();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void fillsInsertAuditFieldsFromJwtRequestUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 7L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SysUser user = new SysUser();
        user.setCreatedBy(99L);

        filler.fill(user, SqlCommandType.INSERT);

        assertEquals(7L, user.getCreatedBy());
        assertEquals(7L, user.getUpdatedBy());
        assertNotNull(user.getCreatedTime());
        assertNotNull(user.getUpdatedTime());
    }

    @Test
    void fillsUpdateTimeOutsideWebRequest() {
        SysUser user = new SysUser();

        filler.fill(user, SqlCommandType.UPDATE);

        assertNotNull(user.getUpdatedTime());
    }
}
