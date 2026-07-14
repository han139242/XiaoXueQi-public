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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
    void selfReporterCannotImpersonateAnotherEmployee() {
        DataScopeService service = serviceWithContext("SELF", 21L);
        MfgWorkReport report = new MfgWorkReport();
        report.setTaskId(9L);
        report.setOperatorId(22L);

        assertThrows(DataScopeAccessDeniedException.class, () -> service.assertCanWriteWorkReport(report));
    }

    private DataScopeService serviceWithContext(String scope, Long employeeId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("authorizationContext", new AuthorizationContext(
                7L, employeeId, 3L, scope, Set.of(), Set.of(), Set.of(1L)));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        return new DataScopeService(
                mock(PermissionService.class), mock(MfgTaskMapper.class), mock(MfgDispatchOrderMapper.class));
    }
}
