package com.neuedu.ems.config;

import com.baomidou.mybatisplus.annotation.Version;
import com.neuedu.ems.exception.OptimisticLockConflictException;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 在 MyBatis 生成 SQL 前统一填充实体审计字段，并将版本更新失败转为明确冲突。
 */
@Component
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class AuditFieldInterceptor implements Interceptor {

    private final AuditFieldFiller auditFieldFiller;

    public AuditFieldInterceptor(AuditFieldFiller auditFieldFiller) {
        this.auditFieldFiller = auditFieldFiller;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        SqlCommandType commandType = statement.getSqlCommandType();
        List<Object> entities = findEntities(invocation.getArgs()[1]);
        entities.forEach(entity -> auditFieldFiller.fill(entity, commandType));
        boolean versionedUpdate = commandType == SqlCommandType.UPDATE
                && entities.stream().anyMatch(this::hasVersionValue);

        Object result = invocation.proceed();
        if (versionedUpdate && result instanceof Number number && number.intValue() == 0) {
            throw new OptimisticLockConflictException();
        }
        return result;
    }

    private List<Object> findEntities(Object parameter) {
        List<Object> entities = new ArrayList<>();
        Set<Object> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        collect(parameter, entities, visited);
        return entities;
    }

    private void collect(Object value, List<Object> entities, Set<Object> visited) {
        if (value == null || !visited.add(value)) {
            return;
        }
        if (isEntity(value)) {
            entities.add(value);
        } else if (value instanceof Map<?, ?> map) {
            map.values().forEach(item -> collect(item, entities, visited));
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collect(item, entities, visited));
        } else if (value.getClass().isArray()) {
            for (Object item : (Object[]) value) {
                collect(item, entities, visited);
            }
        }
    }

    private boolean isEntity(Object value) {
        Package entityPackage = value.getClass().getPackage();
        return entityPackage != null && entityPackage.getName().startsWith("com.neuedu.ems.entity");
    }

    private boolean hasVersionValue(Object entity) {
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Version.class)) {
                    try {
                        field.setAccessible(true);
                        return field.get(entity) != null;
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("无法读取乐观锁版本字段", e);
                    }
                }
            }
        }
        return false;
    }
}
