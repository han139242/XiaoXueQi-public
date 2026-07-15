package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.entity.mfg.MfgDispatchOrder;
import com.neuedu.ems.entity.mfg.MfgTask;
import com.neuedu.ems.entity.mfg.MfgTaskOperator;
import com.neuedu.ems.entity.mfg.MfgWorkReport;
import com.neuedu.ems.entity.org.OrgTeam;
import com.neuedu.ems.mapper.mfg.MfgDispatchOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskMapper;
import com.neuedu.ems.mapper.mfg.MfgTaskOperatorMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkReportMapper;
import com.neuedu.ems.mapper.org.OrgTeamMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "mes.notification.scheduling-enabled", havingValue = "true", matchIfMissing = true)
public class MfgResponsibilityNotificationScanner {

    private final MfgTaskMapper taskMapper;
    private final MfgTaskOperatorMapper taskOperatorMapper;
    private final MfgDispatchOrderMapper dispatchMapper;
    private final MfgWorkReportMapper workReportMapper;
    private final OrgTeamMapper teamMapper;
    private final MfgResponsibilityNotificationService notificationService;

    public MfgResponsibilityNotificationScanner(MfgTaskMapper taskMapper,
                                                MfgTaskOperatorMapper taskOperatorMapper,
                                                MfgDispatchOrderMapper dispatchMapper,
                                                MfgWorkReportMapper workReportMapper,
                                                OrgTeamMapper teamMapper,
                                                MfgResponsibilityNotificationService notificationService) {
        this.taskMapper = taskMapper;
        this.taskOperatorMapper = taskOperatorMapper;
        this.dispatchMapper = dispatchMapper;
        this.workReportMapper = workReportMapper;
        this.teamMapper = teamMapper;
        this.notificationService = notificationService;
    }

    @Scheduled(initialDelayString = "${mes.notification.mfg-scan-initial-delay-ms:15000}",
            fixedDelayString = "${mes.notification.mfg-scan-fixed-delay-ms:60000}")
    public void synchronizeOpenResponsibilities() {
        List<MfgTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<MfgTask>()
                .eq(MfgTask::getDeletedFlag, 0)
                .in(MfgTask::getStatus, BusinessStatus.Production.WAIT_START,
                        BusinessStatus.Production.RUNNING, BusinessStatus.Production.PAUSED)
                .orderByAsc(MfgTask::getTaskId)
                .last("LIMIT 500"));
        if (tasks.isEmpty()) {
            synchronizeSubmittedReports(Map.of());
            return;
        }

        Set<Long> taskIds = tasks.stream().map(MfgTask::getTaskId).collect(Collectors.toSet());
        Map<Long, List<Long>> operators = taskOperatorMapper.selectList(new LambdaQueryWrapper<MfgTaskOperator>()
                        .in(MfgTaskOperator::getTaskId, taskIds)
                        .eq(MfgTaskOperator::getDeletedFlag, 0))
                .stream().collect(Collectors.groupingBy(MfgTaskOperator::getTaskId,
                        Collectors.mapping(MfgTaskOperator::getEmployeeId, Collectors.toList())));

        Set<Long> dispatchIds = tasks.stream().map(MfgTask::getDispatchId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MfgDispatchOrder> dispatches = dispatchIds.isEmpty() ? Map.of()
                : activeById(dispatchMapper.selectBatchIds(dispatchIds),
                MfgDispatchOrder::getDispatchId, MfgDispatchOrder::getDeletedFlag);
        Set<Long> teamIds = dispatches.values().stream().map(MfgDispatchOrder::getTeamId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, OrgTeam> teams = teamIds.isEmpty() ? Map.of()
                : activeById(teamMapper.selectBatchIds(teamIds), OrgTeam::getTeamId, OrgTeam::getDeletedFlag);

        LocalDateTime now = LocalDateTime.now();
        for (MfgTask task : tasks) {
            List<Long> assignedEmployees = operators.getOrDefault(task.getTaskId(), List.of());
            MfgDispatchOrder dispatch = dispatches.get(task.getDispatchId());
            OrgTeam team = dispatch == null ? null : teams.get(dispatch.getTeamId());
            Long leaderId = team == null ? null : team.getLeaderId();
            if (BusinessStatus.Production.WAIT_START.equals(task.getStatus())) {
                if (assignedEmployees.isEmpty()) {
                    notificationService.publishTaskUnassigned(task,
                            dispatch == null ? null : dispatch.getTeamId(), leaderId);
                } else {
                    assignedEmployees.forEach(employeeId ->
                            notificationService.publishTaskAssigned(task.getTaskId(), employeeId, null));
                }
            }
            if (BusinessStatus.Production.PAUSED.equals(task.getStatus())) {
                notificationService.publishTaskPaused(task, leaderId, assignedEmployees);
            }
            if (dispatch != null && dispatch.getDueTime() != null && dispatch.getDueTime().isBefore(now)) {
                notificationService.publishTaskOverdue(task, dispatch, leaderId);
            }
        }
        synchronizeSubmittedReports(tasks.stream().collect(Collectors.toMap(MfgTask::getTaskId, Function.identity())));
    }

    private void synchronizeSubmittedReports(Map<Long, MfgTask> knownTasks) {
        List<MfgWorkReport> reports = workReportMapper.selectList(new LambdaQueryWrapper<MfgWorkReport>()
                .eq(MfgWorkReport::getDeletedFlag, 0)
                .eq(MfgWorkReport::getAuditStatus, BusinessStatus.Document.SUBMITTED)
                .orderByAsc(MfgWorkReport::getReportId)
                .last("LIMIT 500"));
        for (MfgWorkReport report : reports) {
            MfgTask task = knownTasks.get(report.getTaskId());
            if (task == null && report.getTaskId() != null) {
                task = taskMapper.selectById(report.getTaskId());
            }
            if (task != null && task.getDeletedFlag() != null && task.getDeletedFlag() != 0) {
                task = null;
            }
            MfgDispatchOrder dispatch = task == null || task.getDispatchId() == null
                    ? null : dispatchMapper.selectById(task.getDispatchId());
            if (dispatch != null && dispatch.getDeletedFlag() != null && dispatch.getDeletedFlag() != 0) {
                dispatch = null;
            }
            OrgTeam team = dispatch == null || dispatch.getTeamId() == null
                    ? null : teamMapper.selectById(dispatch.getTeamId());
            if (team != null && team.getDeletedFlag() != null && team.getDeletedFlag() != 0) {
                team = null;
            }
            notificationService.publishWorkReportSubmitted(report, null, team == null ? null : team.getLeaderId());
        }
    }

    private <T> Map<Long, T> activeById(Collection<T> rows, Function<T, Long> id,
                                         Function<T, Integer> deletedFlag) {
        if (rows == null) {
            return Map.of();
        }
        return rows.stream().filter(row -> row != null && (deletedFlag.apply(row) == null || deletedFlag.apply(row) == 0))
                .collect(Collectors.toMap(id, Function.identity(), (left, right) -> left));
    }
}
