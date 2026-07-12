package com.neuedu.ems.config;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 为实体写入统一审计字段，客户端传入的操作人不会覆盖 JWT 用户。
 */
@Component
public class AuditFieldFiller {

    public void fill(Object entity, SqlCommandType commandType) {
        if (entity == null || commandType == null) {
            return;
        }
        MetaObject metaObject = SystemMetaObject.forObject(entity);
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = currentUserId();

        if (commandType == SqlCommandType.INSERT) {
            setIfNull(metaObject, "createdTime", now);
            setIfNull(metaObject, "updatedTime", now);
            if (currentUserId != null) {
                set(metaObject, "createdBy", currentUserId);
                set(metaObject, "updatedBy", currentUserId);
            }
        } else if (commandType == SqlCommandType.UPDATE) {
            set(metaObject, "updatedTime", now);
            if (currentUserId != null) {
                set(metaObject, "updatedBy", currentUserId);
            }
        }
    }

    private Long currentUserId() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object value = request.getAttribute("userId");
        return value instanceof Number number ? number.longValue() : null;
    }

    private void setIfNull(MetaObject metaObject, String field, Object value) {
        if (metaObject.hasSetter(field) && metaObject.getValue(field) == null) {
            metaObject.setValue(field, value);
        }
    }

    private void set(MetaObject metaObject, String field, Object value) {
        if (metaObject.hasSetter(field)) {
            metaObject.setValue(field, value);
        }
    }
}
