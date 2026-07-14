package com.neuedu.ems.service.sys;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neuedu.ems.common.AuthorizationContext;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.exception.DataScopeAccessDeniedException;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 对生产执行链路应用 ALL/DEPARTMENT/TEAM/SELF 数据范围。
 */
@Service
public class DataScopeService {

    private final PermissionService permissionService;
    private final MfgTaskMapper mfgTaskMapper;
    private final MfgDispatchOrderMapper mfgDispatchOrderMapper;

    public DataScopeService(PermissionService permissionService,
                            MfgTaskMapper mfgTaskMapper,
                            MfgDispatchOrderMapper mfgDispatchOrderMapper) {
        this.permissionService = permissionService;
        this.mfgTaskMapper = mfgTaskMapper;
        this.mfgDispatchOrderMapper = mfgDispatchOrderMapper;
    }

    public void applyTaskScope(LambdaQueryWrapper<MfgTask> wrapper) {
        AuthorizationContext context = currentContext();
        if (context == null || "ALL".equals(context.dataScope())) {
            return;
        }
        if ("DEPARTMENT".equals(context.dataScope())) {
            requireDepartment(context);
            applyDepartmentTaskScope(wrapper, context.departmentId());
        } else if ("TEAM".equals(context.dataScope())) {
            requireEmployee(context);
            wrapper.apply("""
                    EXISTS (
                      SELECT 1
                      FROM mfg_dispatch_order scope_dispatch
                      JOIN org_team_member scope_member
                        ON scope_member.team_id = scope_dispatch.team_id
                       AND scope_member.deleted_flag = 0
                       AND scope_member.status = 'ENABLE'
                      WHERE scope_dispatch.dispatch_id = mfg_task.dispatch_id
                        AND scope_dispatch.deleted_flag = 0
                        AND scope_member.employee_id = {0}
                    )
                    """, context.employeeId());
        } else if ("SELF".equals(context.dataScope())) {
            requireEmployee(context);
            wrapper.apply("""
                    EXISTS (
                      SELECT 1
                      FROM mfg_task_operator scope_operator
                      WHERE scope_operator.task_id = mfg_task.task_id
                        AND scope_operator.employee_id = {0}
                        AND scope_operator.deleted_flag = 0
                    )
                    """, context.employeeId());
        }
    }

    public void applyWorkReportScope(LambdaQueryWrapper<MfgWorkReport> wrapper) {
        AuthorizationContext context = currentContext();
        if (context == null || "ALL".equals(context.dataScope())) {
            return;
        }
        if ("DEPARTMENT".equals(context.dataScope())) {
            requireDepartment(context);
            wrapper.apply("""
                    EXISTS (
                      SELECT 1
                      FROM org_employee scope_employee
                      WHERE scope_employee.employee_id = mfg_work_report.operator_id
                        AND scope_employee.department_id = {0}
                        AND scope_employee.deleted_flag = 0
                    )
                    """, context.departmentId());
        } else if ("TEAM".equals(context.dataScope())) {
            requireEmployee(context);
            wrapper.apply("""
                    EXISTS (
                      SELECT 1
                      FROM mfg_task scope_task
                      JOIN mfg_dispatch_order scope_dispatch
                        ON scope_dispatch.dispatch_id = scope_task.dispatch_id
                       AND scope_dispatch.deleted_flag = 0
                      JOIN org_team_member scope_member
                        ON scope_member.team_id = scope_dispatch.team_id
                       AND scope_member.deleted_flag = 0
                       AND scope_member.status = 'ENABLE'
                      WHERE scope_task.task_id = mfg_work_report.task_id
                        AND scope_task.deleted_flag = 0
                        AND scope_member.employee_id = {0}
                    )
                    """, context.employeeId());
        } else if ("SELF".equals(context.dataScope())) {
            requireEmployee(context);
            wrapper.apply("mfg_work_report.operator_id = {0}", context.employeeId());
        }
    }

    public void assertCanCreateTask(MfgTask task) {
        AuthorizationContext context = currentContext();
        if (context == null || "ALL".equals(context.dataScope())) {
            return;
        }
        if (task == null || task.getDispatchId() == null
                || !canAccessDispatch(task.getDispatchId(), context)) {
            throw new DataScopeAccessDeniedException();
        }
    }

    public void assertCanWriteWorkReport(MfgWorkReport workReport) {
        AuthorizationContext context = currentContext();
        if (context == null || "ALL".equals(context.dataScope())) {
            return;
        }
        requireEmployee(context);
        if (workReport == null || workReport.getTaskId() == null || !canAccessTask(workReport.getTaskId(), context)) {
            throw new DataScopeAccessDeniedException();
        }
        if ("SELF".equals(context.dataScope())) {
            if (workReport.getOperatorId() != null && !context.employeeId().equals(workReport.getOperatorId())) {
                throw new DataScopeAccessDeniedException();
            }
            workReport.setOperatorId(context.employeeId());
        }
    }

    public boolean isRestricted() {
        AuthorizationContext context = currentContext();
        return context != null && !"ALL".equals(context.dataScope());
    }

    public Long currentUserId() {
        AuthorizationContext context = currentContext();
        return context == null ? null : context.userId();
    }

    private boolean canAccessTask(Long taskId, AuthorizationContext context) {
        LambdaQueryWrapper<MfgTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MfgTask::getTaskId, taskId).eq(MfgTask::getDeletedFlag, 0);
        applyTaskScope(wrapper, context);
        return mfgTaskMapper.selectCount(wrapper) > 0;
    }

    private boolean canAccessDispatch(Long dispatchId, AuthorizationContext context) {
        LambdaQueryWrapper<MfgDispatchOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MfgDispatchOrder::getDispatchId, dispatchId)
                .eq(MfgDispatchOrder::getDeletedFlag, 0);
        if ("DEPARTMENT".equals(context.dataScope())) {
            requireDepartment(context);
            wrapper.apply("""
                        EXISTS (
                          SELECT 1 FROM org_team_member scope_member
                          JOIN org_employee scope_employee
                            ON scope_employee.employee_id = scope_member.employee_id
                           AND scope_employee.deleted_flag = 0
                          WHERE scope_member.team_id = mfg_dispatch_order.team_id
                            AND scope_employee.department_id = {0}
                            AND scope_member.status = 'ENABLE'
                            AND scope_member.deleted_flag = 0
                        )
                    """, context.departmentId());
        } else if ("TEAM".equals(context.dataScope())) {
            requireEmployee(context);
            wrapper.apply("""
                        EXISTS (
                          SELECT 1 FROM org_team_member scope_member
                          WHERE scope_member.team_id = mfg_dispatch_order.team_id
                            AND scope_member.employee_id = {0}
                            AND scope_member.status = 'ENABLE'
                            AND scope_member.deleted_flag = 0
                        )
                    """, context.employeeId());
        } else {
            return false;
        }
        return mfgDispatchOrderMapper.selectCount(wrapper) > 0;
    }

    private void applyTaskScope(LambdaQueryWrapper<MfgTask> wrapper, AuthorizationContext context) {
        if ("DEPARTMENT".equals(context.dataScope())) {
            requireDepartment(context);
            applyDepartmentTaskScope(wrapper, context.departmentId());
        } else if ("TEAM".equals(context.dataScope())) {
            wrapper.apply("""
                    EXISTS (
                      SELECT 1 FROM mfg_dispatch_order scope_dispatch
                      JOIN org_team_member scope_member
                        ON scope_member.team_id = scope_dispatch.team_id
                       AND scope_member.status = 'ENABLE' AND scope_member.deleted_flag = 0
                      WHERE scope_dispatch.dispatch_id = mfg_task.dispatch_id
                        AND scope_dispatch.deleted_flag = 0
                        AND scope_member.employee_id = {0}
                    )
                    """, context.employeeId());
        } else if ("SELF".equals(context.dataScope())) {
            wrapper.apply("""
                    EXISTS (
                      SELECT 1 FROM mfg_task_operator scope_operator
                      WHERE scope_operator.task_id = mfg_task.task_id
                        AND scope_operator.employee_id = {0}
                        AND scope_operator.deleted_flag = 0
                    )
                    """, context.employeeId());
        }
    }

    private void applyDepartmentTaskScope(LambdaQueryWrapper<MfgTask> wrapper, Long departmentId) {
        wrapper.apply("""
                (
                  EXISTS (
                    SELECT 1 FROM mfg_task_operator scope_operator
                    JOIN org_employee scope_employee
                      ON scope_employee.employee_id = scope_operator.employee_id
                     AND scope_employee.deleted_flag = 0
                    WHERE scope_operator.task_id = mfg_task.task_id
                      AND scope_operator.deleted_flag = 0
                      AND scope_employee.department_id = {0}
                  )
                  OR EXISTS (
                    SELECT 1 FROM mfg_dispatch_order scope_dispatch
                    JOIN org_team_member scope_member
                      ON scope_member.team_id = scope_dispatch.team_id
                     AND scope_member.status = 'ENABLE'
                     AND scope_member.deleted_flag = 0
                    JOIN org_employee scope_employee
                      ON scope_employee.employee_id = scope_member.employee_id
                     AND scope_employee.deleted_flag = 0
                    WHERE scope_dispatch.dispatch_id = mfg_task.dispatch_id
                      AND scope_dispatch.deleted_flag = 0
                      AND scope_employee.department_id = {0}
                  )
                )
                """, departmentId);
    }

    private AuthorizationContext currentContext() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object context = request.getAttribute("authorizationContext");
        if (context instanceof AuthorizationContext authorizationContext) {
            return authorizationContext;
        }
        Object userId = request.getAttribute("userId");
        return userId instanceof Number number ? permissionService.getContext(number.longValue()) : null;
    }

    private void requireEmployee(AuthorizationContext context) {
        if (context.employeeId() == null) {
            throw new DataScopeAccessDeniedException();
        }
    }

    private void requireDepartment(AuthorizationContext context) {
        if (context.departmentId() == null) {
            throw new DataScopeAccessDeniedException();
        }
    }
}
