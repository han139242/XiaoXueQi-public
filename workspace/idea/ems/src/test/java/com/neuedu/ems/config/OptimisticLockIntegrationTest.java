package com.neuedu.ems.config;

import com.neuedu.ems.entity.sys.SysUser;
import com.neuedu.ems.exception.OptimisticLockConflictException;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.mybatis.spring.MyBatisSystemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class OptimisticLockIntegrationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    void fillsAuditTimeAndRejectsStaleVersion() {
        SysUser user = new SysUser();
        user.setUsername("lock-test-" + System.nanoTime());
        user.setPasswordHash("test-only");
        user.setStatus("ENABLE");
        user.setDeletedFlag(0);
        sysUserMapper.insert(user);

        SysUser stored = sysUserMapper.selectById(user.getUserId());
        assertNotNull(stored.getCreatedTime());
        assertEquals(0, stored.getVersion());

        SysUser firstUpdate = new SysUser();
        firstUpdate.setUserId(stored.getUserId());
        firstUpdate.setVersion(stored.getVersion());
        firstUpdate.setRemark("first update");
        assertEquals(1, sysUserMapper.updateById(firstUpdate));

        SysUser staleUpdate = new SysUser();
        staleUpdate.setUserId(stored.getUserId());
        staleUpdate.setVersion(stored.getVersion());
        staleUpdate.setRemark("stale update");
        MyBatisSystemException exception = assertThrows(
                MyBatisSystemException.class,
                () -> sysUserMapper.updateById(staleUpdate));
        Throwable cause = exception;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertEquals(OptimisticLockConflictException.class, cause.getClass());
    }
}
