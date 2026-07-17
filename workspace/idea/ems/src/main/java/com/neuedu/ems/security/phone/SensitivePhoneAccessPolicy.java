package com.neuedu.ems.security.phone;

import com.neuedu.ems.common.AuthorizationContext;
import com.neuedu.ems.exception.SensitivePhoneAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从 JWT 过滤器写入的请求权限快照判断敏感字段权限，缺失上下文时失败关闭。
 */
@Component
public class SensitivePhoneAccessPolicy {

    public boolean canViewFull(SensitivePhoneField field) {
        return currentContext().hasPermission(field.getFullViewPermission());
    }

    public boolean canWrite(SensitivePhoneField field) {
        return currentContext().hasPermission(field.getWritePermission());
    }

    public void requireWrite(SensitivePhoneField field) {
        if (!canWrite(field)) {
            throw new SensitivePhoneAccessDeniedException();
        }
    }

    private AuthorizationContext currentContext() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw new SensitivePhoneAccessDeniedException();
        }
        HttpServletRequest request = attributes.getRequest();
        Object context = request.getAttribute("authorizationContext");
        if (context instanceof AuthorizationContext authorizationContext) {
            return authorizationContext;
        }
        throw new SensitivePhoneAccessDeniedException();
    }
}
