package com.neuedu.ems.service.sys;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neuedu.ems.common.AuthorizationContext;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.exception.DataScopeAccessDeniedException;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class DataScopeServiceTest {

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void appliesSelfTaskAndWorkReportRestrictions() {
        DataScopeService service = serviceWithContext("SELF", 21L);
        LambdaQueryWrapper<MfgTask> taskQuery = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<MfgWorkReport> reportQuery = new LambdaQueryWrapper<>();

        service.applyTaskScope(taskQuery);
        service.applyWorkReportScope(reportQuery);

        assertTrue(taskQuery.getCustomSqlSegment().contains("mfg_task_operator"));
        assertTrue(reportQuery.getCustomSqlSegment().contains("operator_id"));
    }

    @Test
    void appliesTeamMembershipRestriction() {
        DataScopeService service = serviceWithContext("TEAM", 31L);
        LambdaQueryWrapper<MfgTask> query = new LambdaQueryWrapper<>();

        service.applyTaskScope(query);

        assertTrue(query.getCustomSqlSegment().contains("org_team_member"));
        assertTrue(query.getCustomSqlSegment().contains("mfg_dispatch_order"));
    }

    @Test
    void appliesDepartmentRestrictionInsteadOfTreatingItAsAllData() {
        DataScopeService service = serviceWithContext("DEPARTMENT", 31L);
        LambdaQueryWrapper<MfgTask> taskQuery = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<MfgWorkReport> reportQuery = new LambdaQueryWrapper<>();

        service.applyTaskScope(taskQuery);
        service.applyWorkReportScope(reportQuery);

        assertTrue(taskQuery.getCustomSqlSegment().contains("department_id"));
        assertTrue(reportQuery.getCustomSqlSegment().contains("department_id"));
    }

    @Test
    void crossDepartmentOperationalRolesCanReadAllTasksAndReports() {
        for (String role : Set.of("QUALITY_ENGINEER", "WAREHOUSE_MANAGER", "EQUIPMENT_ENGINEER")) {
            DataScopeService service = serviceWithContext("DEPARTMENT", 31L, Set.of(role));
            LambdaQueryWrapper<MfgTask> taskQuery = new LambdaQueryWrapper<>();
            LambdaQueryWrapper<MfgWorkReport> reportQuery = new LambdaQueryWrapper<>();

            service.applyTaskScope(taskQuery);
            service.applyWorkReportScope(reportQuery);

            assertTrue(taskQuery.getCustomSqlSegment().isBlank());
            assertTrue(reportQuery.getCustomSqlSegment().isBlank());
        }
    }

    @Test
    void selfReporterCannotImpersonateAnotherEmployee() {
        DataScopeService service = serviceWithContext("SELF", 21L);
        MfgWorkReport report = new MfgWorkReport();
        report.setTaskId(9L);
        report.setOperatorId(22L);

        assertThrows(DataScopeAccessDeniedException.class, () -> service.assertCanWriteWorkReport(report));
    }

    @Test
    void operableTaskIdsStillUseSelfScopeForAFullReadView() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authorizationContext", new AuthorizationContext(
                7L, 21L, 3L, "SELF", Set.of("PRODUCTION_OPERATOR"), Set.of(), Set.of()));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MfgTaskMapper taskMapper = mock(MfgTaskMapper.class);
        MfgTask assigned = new MfgTask();
        assigned.setTaskId(2L);
        when(taskMapper.selectList(any())).thenReturn(List.of(assigned));
        DataScopeService service = new DataScopeService(
                mock(PermissionService.class), taskMapper, mock(MfgDispatchOrderMapper.class));

        Set<Long> result = service.findOperableTaskIds(Set.of(1L, 2L));

        assertEquals(Set.of(2L), result);
    }

    private DataScopeService serviceWithContext(String scope, Long employeeId) {
        return serviceWithContext(scope, employeeId, Set.of());
    }

    private DataScopeService serviceWithContext(String scope, Long employeeId, Set<String> roles) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authorizationContext", new AuthorizationContext(
                7L, employeeId, 3L, scope, roles, Set.of(), Set.of(1L)));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return new DataScopeService(
                mock(PermissionService.class), mock(MfgTaskMapper.class), mock(MfgDispatchOrderMapper.class));
    }
}
