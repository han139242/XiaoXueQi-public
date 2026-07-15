package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.common.NotificationEventPayload;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.org.OrgTeam;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.org.OrgTeamMapper;
import com.neuedu.ems.mapper.sys.SysEventOutboxMapper;
import com.neuedu.ems.mapper.sys.SysNotificationMapper;
import com.neuedu.ems.mapper.sys.SysUserMapper;
import com.neuedu.ems.service.sys.NotificationOutboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgResponsibilityNotificationServiceTest {

    private final NotificationOutboxService outboxService = mock(NotificationOutboxService.class);
    private final MfgTaskMapper taskMapper = mock(MfgTaskMapper.class);
    private final MfgDispatchOrderMapper dispatchMapper = mock(MfgDispatchOrderMapper.class);
    private final OrgTeamMapper teamMapper = mock(OrgTeamMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final MfgResponsibilityNotificationService service = new MfgResponsibilityNotificationService(
            outboxService, taskMapper, dispatchMapper, teamMapper, userMapper,
            mock(SysNotificationMapper.class), mock(SysEventOutboxMapper.class));

    @Test
    void taskAssignmentTargetsOnlyAssignedEmployeeWithTaskViewPermission() {
        MfgTask task = new MfgTask();
        task.setTaskId(1L);
        task.setTaskNo("TASK-1");
        task.setDeletedFlag(0);
        when(taskMapper.selectById(1L)).thenReturn(task);

        service.publishTaskAssigned(1L, 11L, null);

        ArgumentCaptor<NotificationEventPayload> payload = ArgumentCaptor.forClass(NotificationEventPayload.class);
        verify(outboxService).enqueue(eq("MFG_TASK:1:ASSIGNED:11"), eq("MFG_TASK"), eq(1L),
                eq("MFG_TASK_ASSIGNED"), payload.capture());
        assertEquals(1, payload.getValue().recipients().size());
        assertEquals(11L, payload.getValue().recipients().get(0).employeeId());
        assertEquals("mfg:task:view", payload.getValue().recipients().get(0).requiredPermission());
    }

    @Test
    void teamLeaderResolutionReturnsLeaderEmployeeInsteadOfTeamId() {
        MfgTask task = new MfgTask();
        task.setTaskId(1L);
        task.setDispatchId(2L);
        task.setDeletedFlag(0);
        MfgDispatchOrder dispatch = new MfgDispatchOrder();
        dispatch.setDispatchId(2L);
        dispatch.setTeamId(3L);
        dispatch.setDeletedFlag(0);
        OrgTeam team = new OrgTeam();
        team.setTeamId(3L);
        team.setLeaderId(99L);
        team.setDeletedFlag(0);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(dispatchMapper.selectById(2L)).thenReturn(dispatch);
        when(teamMapper.selectById(3L)).thenReturn(team);

        assertEquals(99L, service.resolveTeamLeader(1L));
    }
}
