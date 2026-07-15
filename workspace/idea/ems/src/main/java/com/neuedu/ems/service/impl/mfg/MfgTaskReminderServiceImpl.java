package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.neuedu.ems.service.mfg.MfgTaskReminderService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MfgTaskReminderServiceImpl implements MfgTaskReminderService {

    private static final String ROLE_OPERATOR = "PRODUCTION_OPERATOR";
    private static final String ROLE_SUPERVISOR = "PRODUCTION_SUPERVISOR";
    private static final String AUDIENCE_OPERATOR = "OPERATOR";
    private static final String AUDIENCE_TEAM = "TEAM";
    private static final int MAX_PREVIEW_TASKS = 20;

    private final MfgTaskMapper taskMapper;
    private final MfgTaskOperatorMapper taskOperatorMapper;
    private final MfgDispatchOrderMapper dispatchOrderMapper;
    private final MfgWorkReportMapper workReportMapper;

    public MfgTaskReminderServiceImpl(MfgTaskMapper taskMapper,
                                      MfgTaskOperatorMapper taskOperatorMapper,
                                      MfgDispatchOrderMapper dispatchOrderMapper,
                                      MfgWorkReportMapper workReportMapper) {
        this.taskMapper = taskMapper;
        this.taskOperatorMapper = taskOperatorMapper;
        this.dispatchOrderMapper = dispatchOrderMapper;
        this.workReportMapper = workReportMapper;
    }

    @Override
    public TaskReminderSummary getCurrentUserSummary(AuthorizationContext context) {
        if (context == null || context.employeeId() == null || context.isSuperAdmin()) {
            return TaskReminderSummary.disabled();
        }
        if (context.roles().contains(ROLE_SUPERVISOR)) {
            return supervisorSummary(context);
        }
        if (context.roles().contains(ROLE_OPERATOR)) {
            return operatorSummary(context.employeeId());
        }
        return TaskReminderSummary.disabled();
    }

    private TaskReminderSummary operatorSummary(Long employeeId) {
        LambdaQueryWrapper<MfgTask> query = activeTaskQuery();
        query.in(MfgTask::getStatus, BusinessStatus.Production.WAIT_START, BusinessStatus.Production.PAUSED)
                .apply("""
                        EXISTS (
                          SELECT 1 FROM mfg_task_operator reminder_operator
                          WHERE reminder_operator.task_id = mfg_task.task_id
                            AND reminder_operator.employee_id = {0}
                            AND reminder_operator.deleted_flag = 0
                        )
                        """, employeeId)
                .orderByDesc(MfgTask::getUpdatedTime)
                .orderByDesc(MfgTask::getTaskId);
        List<MfgTask> tasks = taskMapper.selectList(query);
        int waiting = countStatus(tasks, BusinessStatus.Production.WAIT_START);
        int paused = countStatus(tasks, BusinessStatus.Production.PAUSED);
        List<TaskReminderSummary.TaskReminderItem> items = tasks.stream()
                .map(task -> reminderItem(task,
                        List.of(BusinessStatus.Production.PAUSED.equals(task.getStatus()) ? "PAUSED" : "WAITING"), null))
                .toList();
        return summary(AUDIENCE_OPERATOR, tasks.size(), waiting, 0, paused, 0, List.of(), items);
    }

    private TaskReminderSummary supervisorSummary(AuthorizationContext context) {
        if (context.teamIds().isEmpty()) {
            return emptyEnabled(AUDIENCE_TEAM);
        }
        String teamIds = context.teamIds().stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
        String teamDispatchSql = "SELECT dispatch_id FROM mfg_dispatch_order WHERE deleted_flag = 0 AND team_id IN ("
                + teamIds + ")";
        LambdaQueryWrapper<MfgTask> query = activeTaskQuery();
        query.inSql(MfgTask::getDispatchId, teamDispatchSql)
                .and(scope -> scope
                        .and(unassigned -> unassigned.eq(MfgTask::getStatus, BusinessStatus.Production.WAIT_START)
                                .notExists("SELECT 1 FROM mfg_task_operator reminder_operator "
                                        + "WHERE reminder_operator.task_id = mfg_task.task_id "
                                        + "AND reminder_operator.deleted_flag = 0"))
                        .or(paused -> paused.eq(MfgTask::getStatus, BusinessStatus.Production.PAUSED))
                        .or(overdue -> overdue
                                .in(MfgTask::getStatus, BusinessStatus.Production.WAIT_START,
                                        BusinessStatus.Production.RUNNING, BusinessStatus.Production.PAUSED)
                                .inSql(MfgTask::getDispatchId,
                                        "SELECT dispatch_id FROM mfg_dispatch_order WHERE deleted_flag = 0 "
                                                + "AND due_time IS NOT NULL AND due_time < NOW() AND team_id IN ("
                                                + teamIds + ")")))
                .orderByDesc(MfgTask::getUpdatedTime)
                .orderByDesc(MfgTask::getTaskId);
        List<MfgTask> tasks = taskMapper.selectList(query);

        Set<Long> taskIds = tasks.stream().map(MfgTask::getTaskId).collect(Collectors.toSet());
        Set<Long> assignedTaskIds = assignedTaskIds(taskIds);
        Map<Long, MfgDispatchOrder> dispatches = dispatchMap(tasks);
        LocalDateTime now = LocalDateTime.now();
        int unassigned = 0;
        int paused = 0;
        int overdue = 0;
        List<TaskReminderSummary.TaskReminderItem> items = new ArrayList<>();
        for (MfgTask task : tasks) {
            List<String> categories = new ArrayList<>();
            if (BusinessStatus.Production.WAIT_START.equals(task.getStatus())
                    && !assignedTaskIds.contains(task.getTaskId())) {
                categories.add("UNASSIGNED");
                unassigned++;
            }
            if (BusinessStatus.Production.PAUSED.equals(task.getStatus())) {
                categories.add("PAUSED");
                paused++;
            }
            MfgDispatchOrder dispatch = dispatches.get(task.getDispatchId());
            if (dispatch != null && dispatch.getDueTime() != null && dispatch.getDueTime().isBefore(now)) {
                categories.add("OVERDUE");
                overdue++;
            }
            if (!categories.isEmpty()) {
                items.add(reminderItem(task, categories, dispatch == null ? null : dispatch.getDueTime()));
            }
        }

        List<MfgWorkReport> reports = context.hasPermission("mfg:work-report:audit")
                ? submittedTeamReports(teamDispatchSql)
                : List.of();
        return summary(AUDIENCE_TEAM, items.size(), 0, unassigned, paused, overdue, reports, items);
    }

    private List<MfgWorkReport> submittedTeamReports(String teamDispatchSql) {
        LambdaQueryWrapper<MfgWorkReport> query = new LambdaQueryWrapper<>();
        query.eq(MfgWorkReport::getDeletedFlag, 0)
                .eq(MfgWorkReport::getAuditStatus, BusinessStatus.Document.SUBMITTED)
                .inSql(MfgWorkReport::getTaskId,
                        "SELECT task_id FROM mfg_task WHERE deleted_flag = 0 AND dispatch_id IN (" + teamDispatchSql + ")")
                .orderByDesc(MfgWorkReport::getUpdatedTime)
                .orderByDesc(MfgWorkReport::getReportId);
        return workReportMapper.selectList(query);
    }

    private Set<Long> assignedTaskIds(Set<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Set.of();
        }
        LambdaQueryWrapper<MfgTaskOperator> query = new LambdaQueryWrapper<>();
        query.select(MfgTaskOperator::getTaskId)
                .in(MfgTaskOperator::getTaskId, taskIds)
                .eq(MfgTaskOperator::getDeletedFlag, 0);
        return taskOperatorMapper.selectList(query).stream()
                .map(MfgTaskOperator::getTaskId)
                .collect(Collectors.toSet());
    }

    private Map<Long, MfgDispatchOrder> dispatchMap(List<MfgTask> tasks) {
        Set<Long> dispatchIds = tasks.stream().map(MfgTask::getDispatchId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (dispatchIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, MfgDispatchOrder> result = new HashMap<>();
        for (MfgDispatchOrder dispatch : dispatchOrderMapper.selectBatchIds(dispatchIds)) {
            if (dispatch.getDeletedFlag() == null || dispatch.getDeletedFlag() == 0) {
                result.put(dispatch.getDispatchId(), dispatch);
            }
        }
        return result;
    }

    private LambdaQueryWrapper<MfgTask> activeTaskQuery() {
        return new LambdaQueryWrapper<MfgTask>().eq(MfgTask::getDeletedFlag, 0);
    }

    private int countStatus(List<MfgTask> tasks, String status) {
        return (int) tasks.stream().filter(task -> status.equals(task.getStatus())).count();
    }

    private TaskReminderSummary.TaskReminderItem reminderItem(MfgTask task, List<String> categories,
                                                               LocalDateTime dueTime) {
        return new TaskReminderSummary.TaskReminderItem(task.getTaskId(), task.getTaskNo(), task.getStatus(),
                List.copyOf(categories), dueTime, task.getUpdatedTime());
    }

    private TaskReminderSummary emptyEnabled(String audience) {
        return summary(audience, 0, 0, 0, 0, 0, List.of(), List.of());
    }

    private TaskReminderSummary summary(String audience, int taskTotal, int waiting, int unassigned,
                                        int paused, int overdue, List<MfgWorkReport> reports,
                                        List<TaskReminderSummary.TaskReminderItem> items) {
        List<TaskReminderSummary.TaskReminderItem> sortedItems = items.stream()
                .sorted(Comparator.comparing(TaskReminderSummary.TaskReminderItem::taskId))
                .toList();
        List<MfgWorkReport> sortedReports = reports.stream()
                .sorted(Comparator.comparing(MfgWorkReport::getReportId))
                .toList();
        String signature = signature(audience, sortedItems, sortedReports);
        int auditPending = reports.size();
        List<TaskReminderSummary.TaskReminderItem> preview = sortedItems.stream().limit(MAX_PREVIEW_TASKS).toList();
        return new TaskReminderSummary(true, audience, taskTotal + auditPending, taskTotal, waiting,
                unassigned, paused, overdue, auditPending, signature, preview);
    }

    private String signature(String audience, List<TaskReminderSummary.TaskReminderItem> tasks,
                             List<MfgWorkReport> reports) {
        StringBuilder source = new StringBuilder(audience);
        tasks.forEach(task -> source.append("|T:").append(task.taskId()).append(':').append(task.status())
                .append(':').append(task.categories()).append(':').append(task.updatedTime()));
        reports.forEach(report -> source.append("|R:").append(report.getReportId()).append(':')
                .append(report.getAuditStatus()).append(':').append(report.getUpdatedTime()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", e);
        }
    }
}
