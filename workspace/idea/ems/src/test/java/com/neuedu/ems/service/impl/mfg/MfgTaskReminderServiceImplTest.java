package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.neuedu.ems.common.AuthorizationContext;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.TaskReminderSummary;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgTaskOperator;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskOperatorMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgTaskReminderServiceImplTest {

    @BeforeAll
    static void initializeMybatisMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "task"), MfgTask.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "taskOperator"), MfgTaskOperator.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "workReport"), MfgWorkReport.class);
    }

    private final MfgTaskMapper taskMapper = mock(MfgTaskMapper.class);
    private final MfgTaskOperatorMapper taskOperatorMapper = mock(MfgTaskOperatorMapper.class);
    private final MfgDispatchOrderMapper dispatchOrderMapper = mock(MfgDispatchOrderMapper.class);
    private final MfgWorkReportMapper workReportMapper = mock(MfgWorkReportMapper.class);
    private final MfgTaskReminderServiceImpl service = new MfgTaskReminderServiceImpl(
            taskMapper, taskOperatorMapper, dispatchOrderMapper, workReportMapper);

    @Test
    void adminAndManagementRolesDoNotReceiveOrdinaryTaskReminder() {
        for (String role : List.of("SUPER_ADMIN", "MES_MANAGER", "PLAN_SCHEDULER", "QUALITY_ENGINEER")) {
            TaskReminderSummary summary = service.getCurrentUserSummary(context(role, Set.of(), Set.of()));
            assertFalse(summary.enabled());
            assertEquals(0, summary.total());
        }
        verify(taskMapper, never()).selectList(any());
    }

    @Test
    void operatorReceivesOnlyAssignedWaitingAndPausedTasks() {
        MfgTask waiting = task(1L, BusinessStatus.Production.WAIT_START, 10L);
        MfgTask paused = task(2L, BusinessStatus.Production.PAUSED, 10L);
        when(taskMapper.selectList(any())).thenReturn(List.of(waiting, paused));

        TaskReminderSummary summary = service.getCurrentUserSummary(
                context("PRODUCTION_OPERATOR", Set.of(), Set.of()));

        assertTrue(summary.enabled());
        assertEquals("OPERATOR", summary.audience());
        assertEquals(2, summary.total());
        assertEquals(1, summary.waiting());
        assertEquals(1, summary.paused());
        assertEquals(0, summary.unassigned());
    }

    @Test
    void supervisorReceivesTeamPublicPausedOverdueAndAuditWork() {
        LocalDateTime now = LocalDateTime.now();
        MfgTask unassigned = task(1L, BusinessStatus.Production.WAIT_START, 10L);
        MfgTask paused = task(2L, BusinessStatus.Production.PAUSED, 10L);
        when(taskMapper.selectList(any())).thenReturn(List.of(unassigned, paused));
        MfgTaskOperator assignment = new MfgTaskOperator();
        assignment.setTaskId(2L);
        when(taskOperatorMapper.selectList(any())).thenReturn(List.of(assignment));
        MfgDispatchOrder dispatch = new MfgDispatchOrder();
        dispatch.setDispatchId(10L);
        dispatch.setDueTime(now.minusHours(1));
        dispatch.setDeletedFlag(0);
        when(dispatchOrderMapper.selectBatchIds(any())).thenReturn(List.of(dispatch));
        MfgWorkReport report = new MfgWorkReport();
        report.setReportId(8L);
        report.setAuditStatus(BusinessStatus.Document.SUBMITTED);
        report.setUpdatedTime(now);
        when(workReportMapper.selectList(any())).thenReturn(List.of(report));

        TaskReminderSummary summary = service.getCurrentUserSummary(context(
                "PRODUCTION_SUPERVISOR", Set.of(9L), Set.of("mfg:work-report:audit")));

        assertEquals("TEAM", summary.audience());
        assertEquals(3, summary.total());
        assertEquals(2, summary.taskTotal());
        assertEquals(1, summary.unassigned());
        assertEquals(1, summary.paused());
        assertEquals(2, summary.overdue());
        assertEquals(1, summary.auditPending());
        assertNotEquals("NONE", summary.signature());
    }

    private AuthorizationContext context(String role, Set<Long> teamIds, Set<String> permissions) {
        return new AuthorizationContext(1L, 100L, 5L, "SELF", Set.of(role), permissions, teamIds);
    }

    private MfgTask task(Long id, String status, Long dispatchId) {
        MfgTask task = new MfgTask();
        task.setTaskId(id);
        task.setTaskNo("TASK-" + id);
        task.setDispatchId(dispatchId);
        task.setStatus(status);
        task.setUpdatedTime(LocalDateTime.now());
        task.setDeletedFlag(0);
        return task;
    }
}
