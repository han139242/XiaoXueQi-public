package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.AiResponse;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.factory.FactoryLine;
import com.neuedu.ems.entity.mfg.MfgLineCalendarRule;
import com.neuedu.ems.entity.mfg.MfgLineProductCapability;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgScheduleItem;
import com.neuedu.ems.entity.mfg.MfgScheduleScenario;
import com.neuedu.ems.entity.org.OrgShift;
import com.neuedu.ems.entity.process.ProcessRoute;
import com.neuedu.ems.entity.process.ProcessRouteItem;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.factory.FactoryLineMapper;
import com.neuedu.ems.mapper.mfg.MfgLineCalendarRuleMapper;
import com.neuedu.ems.mapper.mfg.MfgLineProductCapabilityMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleItemMapper;
import com.neuedu.ems.mapper.mfg.MfgScheduleScenarioMapper;
import com.neuedu.ems.mapper.org.OrgShiftMapper;
import com.neuedu.ems.mapper.process.ProcessRouteItemMapper;
import com.neuedu.ems.mapper.process.ProcessRouteMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.ai.AiService;
import com.neuedu.ems.service.mfg.MfgScheduleScenarioService;
import com.neuedu.ems.vo.mfg.ManualScheduleBoard;
import com.neuedu.ems.vo.mfg.ManualScheduleBoardItem;
import com.neuedu.ems.vo.mfg.ManualScheduleCandidateLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 第一版可解释的确定性排程服务。
 *
 * <p>产线窗口使用日产能作为基准容量；产品能力系数将订单数量换算为基准容量消耗，
 * 周历能力系数则折算每日窗口容量。路线明细标准工时之和仅用于计算和展示订单负荷分钟数。</p>
 */
@Service
public class MfgScheduleScenarioServiceImpl
        extends ServiceImpl<MfgScheduleScenarioMapper, MfgScheduleScenario>
        implements MfgScheduleScenarioService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CALCULATED = "CALCULATED";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_RELEASED = "RELEASED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_ENABLE = "ENABLE";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String ITEM_STATUS_SCHEDULED = "SCHEDULED";
    private static final String ITEM_STATUS_CONFLICT = "CONFLICT";
    private static final String DECISION_SOURCE_RULE_ENGINE = "RULE_ENGINE";
    private static final String DECISION_SOURCE_MANUAL = "MANUAL";
    private static final String ALGORITHM_CODE = "RULE_V1";
    private static final String ALGORITHM_CODE_MANUAL_BOARD = "MANUAL_BOARD";
    private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int CAPACITY_SCALE = 8;
    private static final long MANUAL_CANDIDATE_CACHE_TTL_NANOS = Duration.ofSeconds(30).toNanos();
    private static final int MANUAL_CANDIDATE_CACHE_MAX_SIZE = 2048;
    private static final int MANUAL_BOARD_AI_ADVICE_ORDER_LIMIT = 12;
    private static final int MANUAL_BOARD_AI_ADVICE_LINE_LIMIT = 3;
    private static final DateTimeFormatter MANUAL_BOARD_AI_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final Map<ManualCandidateGroupKey, ManualCandidateListCacheEntry> manualCandidateListCache =
            new ConcurrentHashMap<>();

    @Autowired
    private MfgScheduleItemMapper mfgScheduleItemMapper;

    @Autowired
    private MfgProductionOrderMapper mfgProductionOrderMapper;

    @Autowired
    private MfgProductionPlanMapper mfgProductionPlanMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    @Autowired
    private AiService aiService;

    @Autowired
    private MfgLineProductCapabilityMapper mfgLineProductCapabilityMapper;

    @Autowired
    private MfgLineCalendarRuleMapper mfgLineCalendarRuleMapper;

    @Autowired
    private FactoryLineMapper factoryLineMapper;

    @Autowired
    private ProcessRouteMapper processRouteMapper;

    @Autowired
    private ProcessRouteItemMapper processRouteItemMapper;

    @Autowired
    private OrgShiftMapper orgShiftMapper;

    @Override
    public Page<MfgScheduleScenario> pageScenarios(Integer current, Integer size, String keyword, String status) {
        Page<MfgScheduleScenario> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildScenarioQuery(keyword, status));
    }

    @Override
    public MfgScheduleScenario getScenarioById(Long scenarioId) {
        if (scenarioId == null) {
            return null;
        }
        QueryWrapper<MfgScheduleScenario> wrapper = new QueryWrapper<>();
        wrapper.eq("scenario_id", scenarioId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public List<MfgScheduleItem> listScenarioItems(Long scenarioId) {
        if (scenarioId == null || getScenarioById(scenarioId) == null) {
            throw notFound("排程方案不存在或已删除");
        }
        QueryWrapper<MfgScheduleItem> wrapper = new QueryWrapper<>();
        wrapper.eq("scenario_id", scenarioId);
        wrapper.eq("deleted_flag", 0);
        wrapper.orderByAsc("sequence_no");
        wrapper.orderByAsc("schedule_item_id");
        return mfgScheduleItemMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgScheduleScenario openManualDraft(Long operatorId) {
        if (operatorId == null) {
            throw invalid("当前登录用户不能为空");
        }
        QueryWrapper<MfgScheduleScenario> query = new QueryWrapper<>();
        query.eq("deleted_flag", 0)
                .eq("algorithm_code", ALGORITHM_CODE_MANUAL_BOARD)
                .eq("created_by", operatorId)
                .in("status", STATUS_DRAFT, STATUS_CALCULATED)
                .orderByDesc("order_count")
                .orderByDesc("updated_time")
                .orderByDesc("scenario_id");
        List<MfgScheduleScenario> existingDrafts = baseMapper.selectList(query);
        if (!existingDrafts.isEmpty()) {
            return existingDrafts.getFirst();
        }

        LocalDateTime now = LocalDateTime.now(SCHEDULE_ZONE);
        MfgScheduleScenario scenario = new MfgScheduleScenario();
        scenario.setScenarioNo("MANUAL-" + operatorId + "-" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        scenario.setScenarioName("手工编排草案");
        scenario.setHorizonStart(now.toLocalDate().atStartOfDay());
        scenario.setHorizonEnd(now.toLocalDate().plusDays(30).atTime(23, 59, 59));
        scenario.setAlgorithmCode(ALGORITHM_CODE_MANUAL_BOARD);
        scenario.setStatus(STATUS_DRAFT);
        scenario.setOrderCount(0);
        scenario.setScheduledOrderCount(0);
        scenario.setConflictOrderCount(0);
        scenario.setLateOrderCount(0);
        scenario.setTotalLoadMinutes(BigDecimal.ZERO);
        scenario.setVersion(0);
        scenario.setCreatedBy(operatorId);
        scenario.setCreatedTime(now);
        scenario.setUpdatedBy(operatorId);
        scenario.setUpdatedTime(now);
        scenario.setDeletedFlag(0);
        if (baseMapper.insert(scenario) == 0 || scenario.getScenarioId() == null) {
            throw new BusinessException(ResultCode.FAILED);
        }
        insertOperationLog(scenario.getScenarioId(), "OPEN_MANUAL_SCHEDULE_DRAFT", "mfg_schedule_scenario",
                operatorId, "创建手工编排草案", now);
        return scenario;
    }

    @Override
    public ManualScheduleBoard getManualScheduleBoard(Long operatorId) {
        if (operatorId == null) {
            throw invalid("当前登录用户不能为空");
        }

        List<MfgScheduleScenario> scenarios = listEditableManualScenarios();
        Map<Long, MfgScheduleScenario> scenarioMap = scenarios.stream()
                .collect(Collectors.toMap(MfgScheduleScenario::getScenarioId, item -> item));
        MfgScheduleScenario workspaceDraft = scenarios.stream()
                .filter(item -> Objects.equals(item.getCreatedBy(), operatorId))
                .findFirst()
                .orElse(null);

        List<MfgScheduleItem> manualItems = listEditableManualItems(scenarioMap.keySet());
        QueryWrapper<MfgProductionPlan> planQuery = new QueryWrapper<>();
        planQuery.eq("deleted_flag", 0);
        List<MfgProductionPlan> allPlans = mfgProductionPlanMapper.selectList(planQuery);
        List<MfgProductionPlan> visiblePlans = allPlans.stream()
                .filter(item -> STATUS_DRAFT.equals(item.getStatus()) || STATUS_RELEASED.equals(item.getStatus()))
                .toList();
        Set<Long> visiblePlanOrderIds = visiblePlans.stream().map(MfgProductionPlan::getProductionOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        // A formal production plan is authoritative if legacy or stale manual data still exists for the same order.
        List<MfgScheduleItem> visibleManualItems = manualItems.stream()
                .filter(item -> !visiblePlanOrderIds.contains(item.getProductionOrderId()))
                .toList();

        Set<Long> orderIds = new HashSet<>();
        visibleManualItems.stream().map(MfgScheduleItem::getProductionOrderId).filter(Objects::nonNull).forEach(orderIds::add);
        allPlans.stream().map(MfgProductionPlan::getProductionOrderId).filter(Objects::nonNull).forEach(orderIds::add);
        Map<Long, MfgProductionOrder> orderMap = loadProductionOrders(orderIds);

        List<ManualScheduleBoardItem> timelineItems = new ArrayList<>();
        for (MfgScheduleItem item : visibleManualItems) {
            MfgScheduleScenario scenario = scenarioMap.get(item.getScenarioId());
            timelineItems.add(toManualBoardItem(item, scenario, orderMap.get(item.getProductionOrderId())));
        }
        for (MfgProductionPlan plan : visiblePlans) {
            timelineItems.add(toProductionPlanBoardItem(plan, orderMap.get(plan.getProductionOrderId())));
        }
        timelineItems.sort(Comparator
                .comparing(ManualScheduleBoardItem::getPlannedStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ManualScheduleBoardItem::getSourceType)
                .thenComparing(item -> item.getScheduleItemId() == null ? Long.MAX_VALUE : item.getScheduleItemId()));

        Set<Long> plannedOrderIds = allPlans.stream().map(MfgProductionPlan::getProductionOrderId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<MfgProductionOrder> pendingOrders = listDraftOrders().stream()
                .filter(order -> !plannedOrderIds.contains(order.getProductionOrderId()))
                .toList();

        LocalDateTime horizonStart = timelineItems.stream().map(ManualScheduleBoardItem::getPlannedStartTime)
                .filter(Objects::nonNull).min(LocalDateTime::compareTo)
                .orElse(workspaceDraft == null ? LocalDate.now(SCHEDULE_ZONE).atStartOfDay()
                        : workspaceDraft.getHorizonStart());
        LocalDateTime horizonEnd = timelineItems.stream().map(ManualScheduleBoardItem::getPlannedEndTime)
                .filter(Objects::nonNull).max(LocalDateTime::compareTo)
                .orElse(workspaceDraft == null ? horizonStart.plusDays(30)
                        : workspaceDraft.getHorizonEnd());

        ManualScheduleBoard board = new ManualScheduleBoard();
        board.setWorkspaceDraft(workspaceDraft);
        board.setTimelineItems(timelineItems);
        board.setPendingOrders(pendingOrders);
        board.setHorizonStart(horizonStart);
        board.setHorizonEnd(horizonEnd);
        board.setManualItemCount(visibleManualItems.size());
        board.setProductionPlanCount(visiblePlans.size());
        board.setConflictCount((int) visibleManualItems.stream()
                .filter(item -> ITEM_STATUS_CONFLICT.equals(item.getStatus())).count());
        board.setLateCount((int) timelineItems.stream()
                .filter(item -> item.getTardinessMinutes() != null && item.getTardinessMinutes() > 0).count());
        return board;
    }

    @Override
    public AiResponse generateManualScheduleBoardAdvice(Long operatorId) {
        ManualScheduleBoard board = getManualScheduleBoard(operatorId);
        return aiService.getScheduleScenarioAdvice(buildManualBoardAiSummary(board));
    }

    @Override
    public List<ManualScheduleCandidateLine> listEligibleLines(Long scenarioId, Long productionOrderId) {
        MfgScheduleScenario scenario = requireManualScenario(scenarioId, false);
        MfgProductionOrder order = requireDraftOrderWithoutPlan(productionOrderId);
        List<ManualScheduleCandidateLine> cached = findCachedEligibleLines(scenario, order);
        if (cached != null) {
            return cached;
        }
        List<ManualScheduleCandidateLine> candidates = buildEligibleLines(scenario, order);
        cacheEligibleLines(scenario, order, candidates);
        return candidates;
    }

    @Override
    public List<ManualScheduleCandidateLine> listEligibleLinesForOrder(Long productionOrderId) {
        MfgProductionOrder order = requireDraftOrderWithoutPlan(productionOrderId);
        LocalDateTime now = LocalDateTime.now(SCHEDULE_ZONE);
        MfgScheduleScenario transientScenario = new MfgScheduleScenario();
        transientScenario.setHorizonStart(now.toLocalDate().atStartOfDay());
        transientScenario.setHorizonEnd(now.toLocalDate().plusDays(30).atTime(23, 59, 59));
        return buildEligibleLines(transientScenario, order);
    }

    @Override
    public ManualScheduleCandidateLine validatePlanningPlacement(Long productionOrderId, Long lineId,
                                                                  LocalDateTime plannedStartTime,
                                                                  LocalDateTime plannedEndTime) {
        MfgProductionOrder order = requireDraftOrderWithoutPlan(productionOrderId);
        MfgScheduleScenario transientScenario = new MfgScheduleScenario();
        transientScenario.setHorizonStart(plannedStartTime);
        transientScenario.setHorizonEnd(plannedEndTime);
        MfgScheduleItem transientItem = new MfgScheduleItem();
        transientItem.setProductionOrderId(productionOrderId);
        transientItem.setLineId(lineId);
        transientItem.setPlannedStartTime(plannedStartTime);
        transientItem.setPlannedEndTime(plannedEndTime);
        String reason = validateManualPlacement(transientScenario, transientItem, order, lineId,
                plannedStartTime, plannedEndTime, List.of());
        if (reason != null) {
            throw invalid(reason);
        }
        return resolveManualCapability(order, lineId, plannedStartTime, plannedEndTime);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgScheduleItem addManualItem(Long scenarioId, Integer scenarioVersion, Long productionOrderId,
                                         Long lineId, Long operatorId) {
        MfgScheduleScenario scenario = requireManualScenario(scenarioId, false);
        assertScenarioVersion(scenario, scenarioVersion);
        MfgProductionOrder order = requireDraftOrderWithoutPlan(productionOrderId);
        if (lineId == null) {
            throw invalid("请选择生产产线");
        }
        ensureOrderNotInEditableScenario(productionOrderId, scenarioId);
        List<MfgScheduleItem> activeItems = listActiveScenarioItems(scenarioId);
        ManualScheduleCandidateLine candidate = buildEligibleLines(scenario, order).stream()
                .filter(item -> Objects.equals(item.getLineId(), lineId))
                .findFirst()
                .orElseThrow(() -> invalid("所选产线不具备当前订单的有效生产能力或可用时段"));

        LocalDateTime now = LocalDateTime.now(SCHEDULE_ZONE);
        MfgScheduleItem item = new MfgScheduleItem();
        item.setScenarioId(scenarioId);
        item.setProductionOrderId(order.getProductionOrderId());
        item.setProductId(order.getProductId());
        item.setRouteId(candidate.getRouteId());
        item.setLineId(candidate.getLineId());
        item.setSequenceNo(nextSequenceNo(activeItems));
        item.setPlannedQuantity(order.getOrderQuantity());
        item.setPlannedStartTime(candidate.getEarliestStartTime());
        item.setPlannedEndTime(candidate.getEarliestEndTime());
        item.setEstimatedMinutes(candidate.getEstimatedMinutes());
        item.setPriorityScore(BigDecimal.ZERO);
        item.setTardinessMinutes(calculateTardiness(order.getDeliveryDate(), candidate.getEarliestEndTime()));
        item.setDecisionSource(DECISION_SOURCE_MANUAL);
        item.setStatus(ITEM_STATUS_SCHEDULED);
        item.setExplanation("人工选择产线后按最早可用时段加入草案");
        item.setVersion(0);
        item.setCreatedBy(operatorId);
        item.setCreatedTime(now);
        item.setUpdatedBy(operatorId);
        item.setUpdatedTime(now);
        item.setDeletedFlag(0);
        MfgScheduleItem removedItem = findRemovedScenarioOrderItem(scenarioId, productionOrderId);
        if (removedItem == null) {
            if (mfgScheduleItemMapper.insert(item) == 0 || item.getScheduleItemId() == null) {
                throw new BusinessException(ResultCode.FAILED);
            }
        } else {
            restoreRemovedManualItem(item, removedItem);
        }
        List<MfgScheduleItem> itemsForSummary = new ArrayList<>(activeItems);
        itemsForSummary.add(item);
        refreshManualScenarioSummary(scenario, STATUS_DRAFT, operatorId, now, itemsForSummary);
        invalidateCandidateCache(scenarioId);
        insertOperationLog(item.getScheduleItemId(), "ADD_MANUAL_SCHEDULE_ITEM", "mfg_schedule_item",
                operatorId, "订单加入手工排程草案", now);
        return item;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgScheduleItem updateManualPlacement(Long scenarioId, Long scheduleItemId, Integer scenarioVersion,
                                                  Integer itemVersion, Long lineId, LocalDateTime plannedStartTime,
                                                  LocalDateTime plannedEndTime, String remark, Long operatorId) {
        MfgScheduleScenario scenario = requireManualScenario(scenarioId, false);
        assertScenarioVersion(scenario, scenarioVersion);
        MfgScheduleItem item = requireManualItem(scenarioId, scheduleItemId);
        if (!Objects.equals(item.getVersion(), itemVersion)) {
            throw conflict("排程已更新，请刷新排程总览后重新操作");
        }
        MfgProductionOrder order = requireDraftOrderWithoutPlan(item.getProductionOrderId());
        String reason = validateManualPlacement(scenario, item, order, lineId, plannedStartTime, plannedEndTime,
                listActiveScenarioItems(scenarioId));
        if (reason != null) {
            throw invalid(reason);
        }
        ManualScheduleCandidateLine capability = resolveManualCapability(order, lineId, plannedStartTime, plannedEndTime);
        LocalDateTime now = LocalDateTime.now(SCHEDULE_ZONE);
        MfgScheduleItem update = new MfgScheduleItem();
        update.setScheduleItemId(scheduleItemId);
        update.setVersion(itemVersion);
        update.setLineId(lineId);
        update.setRouteId(capability.getRouteId());
        update.setPlannedStartTime(plannedStartTime);
        update.setPlannedEndTime(plannedEndTime);
        update.setEstimatedMinutes(capability.getEstimatedMinutes());
        update.setTardinessMinutes(calculateTardiness(order.getDeliveryDate(), plannedEndTime));
        update.setDecisionSource(DECISION_SOURCE_MANUAL);
        update.setStatus(ITEM_STATUS_SCHEDULED);
        update.setConflictReason(null);
        update.setExplanation("人工编辑产线与计划时间");
        update.setRemark(trimToNull(remark));
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (mfgScheduleItemMapper.updateById(update) == 0) {
            throw conflict("排程已更新，请刷新排程总览后重新操作");
        }
        refreshManualScenarioSummary(scenario, STATUS_DRAFT, operatorId, now);
        invalidateCandidateCache(scenarioId);
        insertOperationLog(scheduleItemId, "UPDATE_MANUAL_SCHEDULE_PLACEMENT", "mfg_schedule_item",
                operatorId, "编辑手工排程时间块", now);
        return requireManualItem(scenarioId, scheduleItemId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgScheduleScenario removeManualItem(Long scenarioId, Long scheduleItemId, Integer scenarioVersion,
                                                Integer itemVersion, Long operatorId) {
        MfgScheduleScenario scenario = requireManualScenario(scenarioId, false);
        assertScenarioVersion(scenario, scenarioVersion);
        MfgScheduleItem item = requireManualItem(scenarioId, scheduleItemId);
        if (!Objects.equals(item.getVersion(), itemVersion)) {
            throw conflict("排程已更新，请刷新排程总览后重新操作");
        }
        LocalDateTime now = LocalDateTime.now(SCHEDULE_ZONE);
        MfgScheduleItem update = new MfgScheduleItem();
        update.setScheduleItemId(scheduleItemId);
        update.setVersion(itemVersion);
        update.setDeletedFlag(1);
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (mfgScheduleItemMapper.updateById(update) == 0) {
            throw conflict("排程已更新，请刷新排程总览后重新操作");
        }
        refreshManualScenarioSummary(scenario, STATUS_DRAFT, operatorId, now);
        invalidateCandidateCache(scenarioId);
        insertOperationLog(scheduleItemId, "REMOVE_MANUAL_SCHEDULE_ITEM", "mfg_schedule_item",
                operatorId, "订单移出手工排程草案", now);
        return getActiveScenario(scenarioId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgProductionPlan confirmManualItem(Long scenarioId, Long scheduleItemId, Integer scenarioVersion,
                                               Integer itemVersion, String remark, Long operatorId) {
        if (scenarioId == null || scheduleItemId == null || scenarioVersion == null
                || itemVersion == null || operatorId == null) {
            throw invalid("方案、排程明细、版本和确认人不能为空");
        }
        MfgScheduleScenario scenario = requireManualScenario(scenarioId, false);
        assertScenarioVersion(scenario, scenarioVersion);
        MfgScheduleItem item = requireManualItem(scenarioId, scheduleItemId);
        if (!Objects.equals(item.getVersion(), itemVersion)) {
            throw conflict("排程已更新，请刷新排程总览后重新操作");
        }
        validateScheduledItem(item, new HashSet<>());
        if (hasActivePlan(item.getProductionOrderId())) {
            throw invalid("该订单已生成生产计划，请刷新排程总览查看");
        }
        MfgProductionOrder order = getActiveProductionOrder(item.getProductionOrderId());
        if (order == null) {
            throw notFound("排程明细关联的生产订单不存在或已删除");
        }
        if (!STATUS_DRAFT.equals(order.getStatus()) || order.getVersion() == null) {
            throw invalid("该订单状态已变化，不能再生成生产计划，请刷新后重新选择");
        }

        List<MfgScheduleItem> scenarioItems = listActiveScenarioItems(scenarioId);
        String validationReason = validateManualItem(scenario, item, scenarioItems);
        if (validationReason != null) {
            throw invalid("当前计划不可确认：" + validationReason);
        }

        LocalDateTime now = LocalDateTime.now(SCHEDULE_ZONE);
        MfgProductionPlan plan = createProductionPlan(scenario, item, operatorId, now);
        String normalizedRemark = trimToNull(remark);
        if (normalizedRemark != null) {
            plan.setRemark(normalizedRemark);
        }
        if (mfgProductionPlanMapper.insert(plan) == 0 || plan.getPlanId() == null) {
            throw new BusinessException(ResultCode.FAILED);
        }

        MfgProductionOrder orderUpdate = new MfgProductionOrder();
        orderUpdate.setProductionOrderId(order.getProductionOrderId());
        orderUpdate.setStatus(STATUS_RELEASED);
        orderUpdate.setVersion(order.getVersion());
        orderUpdate.setUpdatedBy(operatorId);
        orderUpdate.setUpdatedTime(now);
        if (mfgProductionOrderMapper.updateById(orderUpdate) == 0) {
            throw conflict("订单状态已更新，请刷新排程总览后重新操作");
        }

        MfgScheduleItem itemUpdate = new MfgScheduleItem();
        itemUpdate.setScheduleItemId(scheduleItemId);
        itemUpdate.setPlanId(plan.getPlanId());
        itemUpdate.setStatus(STATUS_CONFIRMED);
        itemUpdate.setVersion(itemVersion);
        itemUpdate.setUpdatedBy(operatorId);
        itemUpdate.setUpdatedTime(now);
        if (mfgScheduleItemMapper.updateById(itemUpdate) == 0) {
            throw conflict("排程已更新，请刷新排程总览后重新操作");
        }

        List<MfgScheduleItem> remainingItems = scenarioItems.stream()
                .filter(existing -> !Objects.equals(existing.getScheduleItemId(), scheduleItemId))
                .filter(existing -> existing.getPlanId() == null)
                .filter(existing -> !STATUS_CONFIRMED.equals(existing.getStatus()))
                .toList();
        refreshManualScenarioSummary(scenario, STATUS_DRAFT, operatorId, now, remainingItems);
        insertStatusChangeLog(plan.getPlanId(), null, STATUS_RELEASED, "MFG_PRODUCTION_PLAN",
                "CONFIRM_MANUAL_SCHEDULE_ITEM", operatorId, normalizedRemark, now);
        insertStatusChangeLog(order.getProductionOrderId(), STATUS_DRAFT, STATUS_RELEASED, "MFG_PRODUCTION_ORDER",
                "CONFIRM_MANUAL_SCHEDULE_ITEM", operatorId, normalizedRemark, now);
        insertStatusChangeLog(item.getScheduleItemId(), item.getStatus(), STATUS_CONFIRMED, "MFG_SCHEDULE_ITEM",
                "CONFIRM_MANUAL_SCHEDULE_ITEM", operatorId, normalizedRemark, now);
        insertOperationLog(scheduleItemId, "CONFIRM_MANUAL_SCHEDULE_ITEM", "mfg_schedule_item",
                operatorId, normalizedRemark, now);
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgScheduleScenario validateManualDraft(Long scenarioId, Integer scenarioVersion, Long operatorId) {
        MfgScheduleScenario scenario = requireManualScenario(scenarioId, false);
        assertScenarioVersion(scenario, scenarioVersion);
        List<MfgScheduleItem> items = listActiveScenarioItems(scenarioId);
        if (items.isEmpty()) {
            throw invalid("手工草案没有可校验的订单");
        }
        LocalDateTime now = LocalDateTime.now(SCHEDULE_ZONE);
        for (MfgScheduleItem item : items) {
            String reason = validateManualItem(scenario, item, items);
            String expectedStatus = reason == null ? ITEM_STATUS_SCHEDULED : ITEM_STATUS_CONFLICT;
            if (!Objects.equals(item.getStatus(), expectedStatus) || !Objects.equals(item.getConflictReason(), reason)) {
                MfgScheduleItem update = new MfgScheduleItem();
                update.setScheduleItemId(item.getScheduleItemId());
                update.setVersion(item.getVersion());
                update.setStatus(expectedStatus);
                update.setConflictReason(reason);
                update.setTardinessMinutes(item.getPlannedEndTime() == null ? 0L
                        : calculateTardiness(requireDraftOrderWithoutPlan(item.getProductionOrderId()).getDeliveryDate(),
                        item.getPlannedEndTime()));
                update.setUpdatedBy(operatorId);
                update.setUpdatedTime(now);
                if (mfgScheduleItemMapper.updateById(update) == 0) {
                    throw conflict("排程明细已被其他用户修改，请刷新后重试");
                }
            }
        }
        refreshManualScenarioSummary(getActiveScenario(scenarioId), STATUS_CALCULATED, operatorId, now);
        invalidateCandidateCache(scenarioId);
        insertOperationLog(scenarioId, "VALIDATE_MANUAL_SCHEDULE_DRAFT", "mfg_schedule_scenario",
                operatorId, "校验手工排程草案", now);
        return getActiveScenario(scenarioId);
    }

    @Override
    public AiResponse generateOrderAiAdvice(Long scenarioId, Long productionOrderId, Integer scenarioVersion) {
        MfgScheduleScenario scenario = requireManualScenario(scenarioId, false);
        assertScenarioVersion(scenario, scenarioVersion);
        MfgProductionOrder order = requireDraftOrderWithoutPlan(productionOrderId);
        List<ManualScheduleCandidateLine> candidates = buildEligibleLines(scenario, order);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scenarioStatus", scenario.getStatus());
        summary.put("orderProductId", order.getProductId());
        summary.put("orderQuantity", order.getOrderQuantity());
        summary.put("deliveryDate", order.getDeliveryDate());
        summary.put("candidateLines", candidates.stream().map(candidate -> Map.of(
                "lineId", candidate.getLineId(), "workshopId", candidate.getWorkshopId(),
                "priorityLevel", candidate.getPriorityLevel(), "estimatedMinutes", candidate.getEstimatedMinutes(),
                "earliestStartTime", candidate.getEarliestStartTime(), "earliestEndTime", candidate.getEarliestEndTime()))
                .toList());
        summary.put("draftItemCount", listActiveScenarioItems(scenarioId).size());
        return aiService.getScheduleScenarioAdvice(summary);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgScheduleScenario calculateScenario(MfgScheduleScenario request) {
        validateCalculationRequest(request);
        String scenarioNo = request.getScenarioNo().trim();
        ensureScenarioNoAvailable(scenarioNo);
        List<MfgProductionOrder> orders = listDraftOrders();
        validateDraftOrders(orders);

        MfgScheduleScenario scenario = createDraftScenario(request, scenarioNo);
        if (baseMapper.insert(scenario) == 0 || scenario.getScenarioId() == null) {
            throw new BusinessException(ResultCode.FAILED);
        }

        SchedulingContext context = loadSchedulingContext(scenario.getHorizonStart(), scenario.getHorizonEnd());
        Map<Long, LineScheduleState> lineStates = new HashMap<>();
        int scheduledCount = 0;
        int conflictCount = 0;
        int lateCount = 0;
        BigDecimal totalLoadMinutes = BigDecimal.ZERO;

        for (int index = 0; index < orders.size(); index++) {
            MfgProductionOrder order = orders.get(index);
            MfgScheduleItem item = scheduleOrder(scenario, order, index + 1, orders.size(), context, lineStates);
            if (mfgScheduleItemMapper.insert(item) == 0) {
                throw new BusinessException(ResultCode.FAILED);
            }
            if (ITEM_STATUS_SCHEDULED.equals(item.getStatus())) {
                scheduledCount++;
                totalLoadMinutes = totalLoadMinutes.add(item.getEstimatedMinutes());
                if (item.getTardinessMinutes() != null && item.getTardinessMinutes() > 0) {
                    lateCount++;
                }
            } else {
                conflictCount++;
            }
        }

        scenario.setStatus(STATUS_CALCULATED);
        scenario.setOrderCount(orders.size());
        scenario.setScheduledOrderCount(scheduledCount);
        scenario.setConflictOrderCount(conflictCount);
        scenario.setLateOrderCount(lateCount);
        scenario.setTotalLoadMinutes(totalLoadMinutes.setScale(2, RoundingMode.HALF_UP));
        if (baseMapper.updateById(scenario) == 0) {
            throw new BusinessException(ResultCode.FAILED);
        }
        return scenario;
    }

    /**
     * 确认排程方案，将所有已排程明细原子转换为正式生产计划。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgScheduleScenario confirmScenario(Long scenarioId, Integer version, Long operatorId, String remark) {
        if (scenarioId == null || version == null || operatorId == null) {
            throw invalid("方案、版本和确认人不能为空");
        }

        MfgScheduleScenario scenario = getActiveScenario(scenarioId);
        if (scenario == null) {
            throw notFound("排程方案不存在或已删除");
        }
        if (!STATUS_CALCULATED.equals(scenario.getStatus())) {
            throw invalid("只有 CALCULATED 状态的排程方案可以确认");
        }
        if (!Objects.equals(version, scenario.getVersion())) {
            throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "排程方案已被其他用户修改，请刷新后重试");
        }

        List<MfgScheduleItem> items = listActiveScenarioItems(scenarioId);
        List<ConfirmationEntry> entries = validateAndLoadConfirmationEntries(items);
        LocalDateTime now = LocalDateTime.now();

        for (ConfirmationEntry entry : entries) {
            MfgScheduleItem item = entry.item();
            MfgProductionOrder order = entry.order();

            MfgProductionPlan plan = createProductionPlan(scenario, item, operatorId, now);
            if (mfgProductionPlanMapper.insert(plan) == 0 || plan.getPlanId() == null) {
                throw new BusinessException(ResultCode.FAILED);
            }

            MfgProductionOrder orderUpdate = new MfgProductionOrder();
            orderUpdate.setProductionOrderId(order.getProductionOrderId());
            orderUpdate.setStatus(STATUS_RELEASED);
            orderUpdate.setVersion(order.getVersion());
            orderUpdate.setUpdatedBy(operatorId);
            orderUpdate.setUpdatedTime(now);
            if (mfgProductionOrderMapper.updateById(orderUpdate) == 0) {
                throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "生产订单已被其他用户修改，请重新计算方案");
            }

            MfgScheduleItem itemUpdate = new MfgScheduleItem();
            itemUpdate.setScheduleItemId(item.getScheduleItemId());
            itemUpdate.setPlanId(plan.getPlanId());
            itemUpdate.setStatus(STATUS_CONFIRMED);
            itemUpdate.setVersion(item.getVersion());
            itemUpdate.setUpdatedBy(operatorId);
            itemUpdate.setUpdatedTime(now);
            if (mfgScheduleItemMapper.updateById(itemUpdate) == 0) {
                throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "排程明细已被其他用户修改，请重新计算方案");
            }

            insertStatusChangeLog(plan.getPlanId(), null, STATUS_RELEASED, "MFG_PRODUCTION_PLAN",
                    "GENERATE_FROM_SCHEDULE_SCENARIO", operatorId, remark, now);
            insertStatusChangeLog(order.getProductionOrderId(), STATUS_DRAFT, STATUS_RELEASED, "MFG_PRODUCTION_ORDER",
                    "CONFIRM_SCHEDULE_SCENARIO", operatorId, remark, now);
            insertStatusChangeLog(item.getScheduleItemId(), ITEM_STATUS_SCHEDULED, STATUS_CONFIRMED, "MFG_SCHEDULE_ITEM",
                    "CONFIRM_SCHEDULE_SCENARIO", operatorId, remark, now);
        }

        scenario.setStatus(STATUS_CONFIRMED);
        scenario.setConfirmedBy(operatorId);
        scenario.setConfirmedTime(now);
        scenario.setUpdatedBy(operatorId);
        scenario.setUpdatedTime(now);
        if (baseMapper.updateById(scenario) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "排程方案已被其他用户修改，请刷新后重试");
        }
        insertStatusChangeLog(scenarioId, STATUS_CALCULATED, STATUS_CONFIRMED, "MFG_SCHEDULE_SCENARIO",
                "CONFIRM_SCHEDULE_SCENARIO", operatorId, remark, now);
        insertOperationLog(scenarioId, "CONFIRM_SCHEDULE_SCENARIO", "mfg_schedule_scenario", operatorId, remark, now);
        return scenario;
    }

    /**
     * 读取方案脱敏汇总并生成 AI 风险建议。AI 调用完成后才写回建议字段，避免模型不可用时产生半成品数据。
     */
    @Override
    public AiResponse generateAiAdvice(Long scenarioId, Integer version) {
        if (scenarioId == null || version == null) {
            throw invalid("方案和版本不能为空");
        }
        MfgScheduleScenario scenario = getActiveScenario(scenarioId);
        if (scenario == null) {
            throw notFound("排程方案不存在或已删除");
        }
        if (!STATUS_CALCULATED.equals(scenario.getStatus())) {
            throw invalid("只有 CALCULATED 状态的排程方案可以生成 AI 风险建议");
        }
        if (!Objects.equals(version, scenario.getVersion())) {
            throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "排程方案已被其他用户修改，请刷新后重试");
        }

        List<MfgScheduleItem> items = listActiveScenarioItems(scenarioId);
        AiResponse response = aiService.getScheduleScenarioAdvice(buildAiAdviceSummary(scenario, items));
        if (response == null || !StringUtils.hasText(response.getAnswer())) {
            throw new BusinessException(ResultCode.FAILED);
        }

        scenario.setAiAdvice(formatAiAdvice(response));
        scenario.setAiModel(response.getModel());
        scenario.setAiGeneratedTime(response.getGeneratedAt() == null ? LocalDateTime.now() : response.getGeneratedAt());
        if (baseMapper.updateById(scenario) == 0) {
            throw new BusinessException(ResultCode.DATA_CONFLICT.getCode(), "排程方案已被其他用户修改，请刷新后重试");
        }
        return response;
    }

    private Map<String, Object> buildAiAdviceSummary(MfgScheduleScenario scenario, List<MfgScheduleItem> items) {
        Map<String, Object> scenarioSummary = new LinkedHashMap<>();
        scenarioSummary.put("algorithmCode", scenario.getAlgorithmCode());
        scenarioSummary.put("horizonStart", scenario.getHorizonStart());
        scenarioSummary.put("horizonEnd", scenario.getHorizonEnd());
        scenarioSummary.put("orderCount", scenario.getOrderCount());
        scenarioSummary.put("scheduledOrderCount", scenario.getScheduledOrderCount());
        scenarioSummary.put("conflictOrderCount", scenario.getConflictOrderCount());
        scenarioSummary.put("lateOrderCount", scenario.getLateOrderCount());
        scenarioSummary.put("totalLoadMinutes", scenario.getTotalLoadMinutes());

        Map<Long, LineAiLoad> loadsByLine = new TreeMap<>();
        Map<String, Integer> conflictReasons = new TreeMap<>();
        for (MfgScheduleItem item : items == null ? List.<MfgScheduleItem>of() : items) {
            if (ITEM_STATUS_CONFLICT.equals(item.getStatus())) {
                String reason = StringUtils.hasText(item.getConflictReason())
                        ? item.getConflictReason().trim() : "未说明冲突原因";
                conflictReasons.merge(reason, 1, Integer::sum);
                continue;
            }
            if (!ITEM_STATUS_SCHEDULED.equals(item.getStatus()) || item.getLineId() == null) {
                continue;
            }
            LineAiLoad load = loadsByLine.computeIfAbsent(item.getLineId(), LineAiLoad::new);
            load.add(item);
        }

        List<Map<String, Object>> lineLoads = new ArrayList<>();
        for (LineAiLoad load : loadsByLine.values()) {
            lineLoads.add(load.toSummary());
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scenario", scenarioSummary);
        summary.put("lineLoads", lineLoads);
        summary.put("conflictReasons", conflictReasons);
        return summary;
    }

    private String formatAiAdvice(AiResponse response) {
        StringBuilder advice = new StringBuilder(response.getAnswer().trim());
        if (response.getSuggestions() != null) {
            for (String suggestion : response.getSuggestions()) {
                if (StringUtils.hasText(suggestion)) {
                    advice.append(System.lineSeparator()).append("- ").append(suggestion.trim());
                }
            }
        }
        return advice.toString();
    }

    private MfgScheduleScenario getActiveScenario(Long scenarioId) {
        QueryWrapper<MfgScheduleScenario> wrapper = new QueryWrapper<>();
        wrapper.eq("scenario_id", scenarioId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    private List<MfgScheduleItem> listActiveScenarioItems(Long scenarioId) {
        QueryWrapper<MfgScheduleItem> wrapper = new QueryWrapper<>();
        wrapper.eq("scenario_id", scenarioId);
        wrapper.eq("deleted_flag", 0);
        wrapper.orderByAsc("sequence_no");
        wrapper.orderByAsc("schedule_item_id");
        return mfgScheduleItemMapper.selectList(wrapper);
    }

    private List<MfgScheduleScenario> listEditableManualScenarios() {
        QueryWrapper<MfgScheduleScenario> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0)
                .eq("algorithm_code", ALGORITHM_CODE_MANUAL_BOARD)
                .in("status", STATUS_DRAFT, STATUS_CALCULATED)
                .orderByDesc("order_count")
                .orderByDesc("updated_time")
                .orderByDesc("scenario_id");
        return baseMapper.selectList(wrapper);
    }

    private List<MfgScheduleItem> listEditableManualItems(Set<Long> scenarioIds) {
        if (scenarioIds == null || scenarioIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper<MfgScheduleItem> wrapper = new QueryWrapper<>();
        wrapper.in("scenario_id", scenarioIds)
                .eq("deleted_flag", 0)
                .in("status", ITEM_STATUS_SCHEDULED, ITEM_STATUS_CONFLICT)
                .orderByAsc("planned_start_time")
                .orderByAsc("schedule_item_id");
        return mfgScheduleItemMapper.selectList(wrapper);
    }

    private List<MfgScheduleItem> listGlobalEditableManualItems() {
        Set<Long> scenarioIds = listEditableManualScenarios().stream()
                .map(MfgScheduleScenario::getScenarioId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return listEditableManualItems(scenarioIds);
    }

    private List<MfgProductionPlan> listActiveProductionPlans() {
        QueryWrapper<MfgProductionPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0).in("status", STATUS_DRAFT, STATUS_RELEASED);
        return mfgProductionPlanMapper.selectList(wrapper);
    }

    private Map<Long, MfgProductionOrder> loadProductionOrders(Set<Long> productionOrderIds) {
        if (productionOrderIds == null || productionOrderIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<MfgProductionOrder> wrapper = new QueryWrapper<>();
        wrapper.in("production_order_id", productionOrderIds).eq("deleted_flag", 0);
        return mfgProductionOrderMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(MfgProductionOrder::getProductionOrderId, item -> item));
    }

    private ManualScheduleBoardItem toManualBoardItem(MfgScheduleItem item, MfgScheduleScenario scenario,
                                                       MfgProductionOrder order) {
        ManualScheduleBoardItem result = new ManualScheduleBoardItem();
        result.setSourceType("MANUAL");
        result.setScenarioId(item.getScenarioId());
        result.setScenarioNo(scenario == null ? null : scenario.getScenarioNo());
        result.setScenarioVersion(scenario == null ? null : scenario.getVersion());
        result.setScheduleItemId(item.getScheduleItemId());
        result.setItemVersion(item.getVersion());
        result.setProductionOrderId(item.getProductionOrderId());
        result.setProductionOrderNo(order == null ? null : order.getProductionOrderNo());
        result.setProductId(item.getProductId() != null ? item.getProductId()
                : order == null ? null : order.getProductId());
        result.setLineId(item.getLineId());
        result.setPlannedQuantity(item.getPlannedQuantity());
        result.setPlannedStartTime(item.getPlannedStartTime());
        result.setPlannedEndTime(item.getPlannedEndTime());
        result.setTardinessMinutes(item.getTardinessMinutes());
        result.setStatus(item.getStatus());
        result.setConflictReason(item.getConflictReason());
        result.setRemark(item.getRemark());
        result.setEditable(true);
        return result;
    }

    private ManualScheduleBoardItem toProductionPlanBoardItem(MfgProductionPlan plan, MfgProductionOrder order) {
        ManualScheduleBoardItem result = new ManualScheduleBoardItem();
        result.setSourceType("PLAN");
        result.setPlanId(plan.getPlanId());
        result.setPlanNo(plan.getPlanNo());
        result.setProductionOrderId(plan.getProductionOrderId());
        result.setProductionOrderNo(order == null ? null : order.getProductionOrderNo());
        result.setProductId(order == null ? null : order.getProductId());
        result.setLineId(plan.getLineId());
        result.setPlannedQuantity(plan.getPlannedQuantity());
        result.setPlannedStartTime(plan.getPlannedStartTime());
        result.setPlannedEndTime(plan.getPlannedEndTime());
        result.setTardinessMinutes(order == null ? 0L
                : calculateTardiness(order.getDeliveryDate(), plan.getPlannedEndTime()));
        result.setStatus(plan.getStatus());
        result.setRemark(plan.getRemark());
        result.setEditable(false);
        return result;
    }

    private Map<String, Object> buildManualBoardAiSummary(ManualScheduleBoard board) {
        Map<Long, BoardLineAiLoad> loadsByLine = new TreeMap<>();
        Map<String, Integer> conflictReasons = new TreeMap<>();
        for (ManualScheduleBoardItem item : board.getTimelineItems()) {
            if (item.getLineId() != null) {
                loadsByLine.computeIfAbsent(item.getLineId(), BoardLineAiLoad::new).add(item);
            }
            if (StringUtils.hasText(item.getConflictReason())) {
                conflictReasons.merge(item.getConflictReason().trim(), 1, Integer::sum);
            }
        }

        Set<Long> manualOrderIds = board.getTimelineItems().stream()
                .filter(item -> "MANUAL".equals(item.getSourceType()))
                .map(ManualScheduleBoardItem::getProductionOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<MfgProductionOrder> unassignedPendingOrders = board.getPendingOrders().stream()
                .filter(order -> !manualOrderIds.contains(order.getProductionOrderId()))
                .toList();
        BigDecimal pendingQuantity = unassignedPendingOrders.stream()
                .map(MfgProductionOrder::getOrderQuantity).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate earliestDeliveryDate = unassignedPendingOrders.stream()
                .map(MfgProductionOrder::getDeliveryDate).filter(Objects::nonNull)
                .min(LocalDate::compareTo).orElse(null);
        long pendingProductCount = unassignedPendingOrders.stream()
                .map(MfgProductionOrder::getProductId).filter(Objects::nonNull).distinct().count();

        Map<String, Object> scope = new LinkedHashMap<>();
        scope.put("horizonStart", board.getHorizonStart());
        scope.put("horizonEnd", board.getHorizonEnd());
        scope.put("manualItemCount", board.getManualItemCount());
        scope.put("productionPlanCount", board.getProductionPlanCount());
        scope.put("pendingOrderCount", unassignedPendingOrders.size());
        scope.put("conflictCount", board.getConflictCount());
        scope.put("lateCount", board.getLateCount());

        Map<String, Object> pending = new LinkedHashMap<>();
        pending.put("orderCount", unassignedPendingOrders.size());
        pending.put("productCount", pendingProductCount);
        pending.put("totalQuantity", pendingQuantity);
        pending.put("earliestDeliveryDate", earliestDeliveryDate);

        List<Map<String, Object>> pendingPlacementOptions = buildManualBoardPendingPlacementOptions(board,
                unassignedPendingOrders);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("analysisType", "MANUAL_SCHEDULE_BOARD_ADVICE");
        summary.put("scope", scope);
        summary.put("lineLoads", loadsByLine.values().stream().map(BoardLineAiLoad::toSummary).toList());
        summary.put("pendingOrders", pending);
        summary.put("pendingPlacementOptions", pendingPlacementOptions);
        summary.put("conflictReasons", conflictReasons);
        return summary;
    }

    private List<Map<String, Object>> buildManualBoardPendingPlacementOptions(ManualScheduleBoard board,
                                                                                List<MfgProductionOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        MfgScheduleScenario adviceScenario = board.getWorkspaceDraft();
        if (adviceScenario == null) {
            adviceScenario = new MfgScheduleScenario();
            adviceScenario.setHorizonStart(board.getHorizonStart());
            adviceScenario.setHorizonEnd(board.getHorizonEnd());
        }
        List<MfgScheduleItem> globalManualItems = listGlobalEditableManualItems();
        List<MfgProductionOrder> prioritizedOrders = orders.stream()
                .sorted(Comparator.comparing(MfgProductionOrder::getDeliveryDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MfgProductionOrder::getProductionOrderNo,
                                Comparator.nullsLast(String::compareTo)))
                .limit(MANUAL_BOARD_AI_ADVICE_ORDER_LIMIT)
                .toList();
        List<Map<String, Object>> options = new ArrayList<>();
        for (int index = 0; index < prioritizedOrders.size(); index++) {
            MfgProductionOrder order = prioritizedOrders.get(index);
            List<ManualScheduleCandidateLine> candidates = buildEligibleLines(adviceScenario, order, globalManualItems).stream()
                    .limit(MANUAL_BOARD_AI_ADVICE_LINE_LIMIT)
                    .toList();
            Map<String, Object> option = new LinkedHashMap<>();
            option.put("reference", "P" + (index + 1));
            option.put("productId", order.getProductId());
            option.put("orderQuantity", order.getOrderQuantity());
            option.put("deliveryDate", order.getDeliveryDate());
            option.put("candidateLineOptions", formatManualBoardAiCandidateOptions(candidates));
            options.add(option);
        }
        return options;
    }

    private String formatManualBoardAiCandidateOptions(List<ManualScheduleCandidateLine> candidates) {
        if (candidates.isEmpty()) {
            return "当前没有可直接安排的产线窗口；请优先释放最早可用产线，或协调可替代产线后再安排。";
        }
        return candidates.stream()
                .map(candidate -> {
                    String line = StringUtils.hasText(candidate.getLineCode())
                            ? candidate.getLineCode()
                            : "产线" + candidate.getLineId();
                    String startTime = formatManualBoardAiTime(candidate.getEarliestStartTime());
                    String endTime = formatManualBoardAiTime(candidate.getEarliestEndTime());
                    String workingMinutes = candidate.getMinimumWorkingMinutes() == null
                            ? "--"
                            : candidate.getMinimumWorkingMinutes().toString();
                    String priority = candidate.getPriorityLevel() == null
                            ? "--"
                            : candidate.getPriorityLevel().toString();
                    return line + "：最早可排 " + startTime
                            + "，预计完成 " + endTime
                            + "，建议预留 " + workingMinutes + " 分钟产线工时"
                            + "，优先级 " + priority;
                })
                .collect(Collectors.joining("；"));
    }

    private String formatManualBoardAiTime(LocalDateTime time) {
        return time == null ? "--" : time.format(MANUAL_BOARD_AI_TIME_FORMATTER);
    }

    private List<ConfirmationEntry> validateAndLoadConfirmationEntries(List<MfgScheduleItem> items) {
        if (items == null || items.isEmpty()) {
            throw invalid("排程方案没有可确认的排程明细");
        }
        List<ConfirmationEntry> entries = new ArrayList<>();
        Set<Long> productionOrderIds = new HashSet<>();
        for (MfgScheduleItem item : items) {
            validateScheduledItem(item, productionOrderIds);
            if (hasActivePlan(item.getProductionOrderId())) {
                throw invalid("生产订单已存在有效生产计划，不能重复确认");
            }
            MfgProductionOrder order = getActiveProductionOrder(item.getProductionOrderId());
            if (order == null) {
                throw notFound("排程明细关联的生产订单不存在或已删除");
            }
            if (!STATUS_DRAFT.equals(order.getStatus())) {
                throw invalid("生产订单不是 DRAFT 状态，不能确认排程方案");
            }
            if (order.getVersion() == null) {
                throw invalid("生产订单缺少版本号，不能确认排程方案");
            }
            entries.add(new ConfirmationEntry(item, order));
        }
        return entries;
    }

    private void validateScheduledItem(MfgScheduleItem item, Set<Long> productionOrderIds) {
        if (item == null || item.getScheduleItemId() == null) {
            throw invalid("排程明细数据不完整");
        }
        if (ITEM_STATUS_CONFLICT.equals(item.getStatus())) {
            throw invalid("方案存在 CONFLICT 排程明细，不能确认");
        }
        if (!ITEM_STATUS_SCHEDULED.equals(item.getStatus())) {
            throw invalid("排程明细不是 SCHEDULED 状态，不能确认");
        }
        if (item.getPlanId() != null) {
            throw invalid("排程明细已关联生产计划，不能重复确认");
        }
        if (item.getProductionOrderId() == null || !productionOrderIds.add(item.getProductionOrderId())) {
            throw invalid("排程方案存在重复或缺失的生产订单");
        }
        if (item.getLineId() == null || item.getSequenceNo() == null || item.getSequenceNo() <= 0
                || item.getPlannedQuantity() == null || item.getPlannedQuantity().compareTo(BigDecimal.ZERO) <= 0
                || item.getPlannedStartTime() == null || item.getPlannedEndTime() == null
                || !item.getPlannedEndTime().isAfter(item.getPlannedStartTime())) {
            throw invalid("已排程明细缺少有效的计划产线、数量或时间");
        }
        if (item.getVersion() == null) {
            throw invalid("排程明细缺少版本号，不能确认");
        }
    }

    private boolean hasActivePlan(Long productionOrderId) {
        QueryWrapper<MfgProductionPlan> wrapper = new QueryWrapper<>();
        wrapper.eq("production_order_id", productionOrderId);
        wrapper.eq("deleted_flag", 0);
        wrapper.ne("status", STATUS_CLOSED);
        return mfgProductionPlanMapper.selectCount(wrapper) > 0;
    }

    private MfgProductionOrder getActiveProductionOrder(Long productionOrderId) {
        QueryWrapper<MfgProductionOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("production_order_id", productionOrderId);
        wrapper.eq("deleted_flag", 0);
        return mfgProductionOrderMapper.selectOne(wrapper);
    }

    private MfgProductionPlan createProductionPlan(MfgScheduleScenario scenario, MfgScheduleItem item,
                                                    Long operatorId, LocalDateTime now) {
        MfgProductionPlan plan = new MfgProductionPlan();
        plan.setPlanNo("SP-" + scenario.getScenarioId() + "-" + item.getSequenceNo());
        plan.setProductionOrderId(item.getProductionOrderId());
        plan.setLineId(item.getLineId());
        plan.setPlannedStartTime(item.getPlannedStartTime());
        plan.setPlannedEndTime(item.getPlannedEndTime());
        plan.setPlannedQuantity(item.getPlannedQuantity());
        plan.setStatus(STATUS_RELEASED);
        plan.setRemark("由排程方案 " + scenario.getScenarioNo() + " 确认生成");
        plan.setVersion(0);
        plan.setCreatedBy(operatorId);
        plan.setCreatedTime(now);
        plan.setUpdatedBy(operatorId);
        plan.setUpdatedTime(now);
        plan.setDeletedFlag(0);
        return plan;
    }

    private void insertStatusChangeLog(Long bizId, String oldStatus, String newStatus, String bizType,
                                       String reason, Long operatorId, String remark, LocalDateTime changedTime) {
        SysStatusChangeLog statusLog = new SysStatusChangeLog();
        statusLog.setBizType(bizType);
        statusLog.setBizId(bizId);
        statusLog.setOldStatus(oldStatus);
        statusLog.setNewStatus(newStatus);
        statusLog.setChangeReason(reason);
        statusLog.setChangedBy(operatorId);
        statusLog.setChangedTime(changedTime);
        statusLog.setCreatedBy(operatorId);
        statusLog.setCreatedTime(changedTime);
        statusLog.setUpdatedBy(operatorId);
        statusLog.setUpdatedTime(changedTime);
        statusLog.setRemark(remark);
        statusLog.setDeletedFlag(0);
        if (sysStatusChangeLogMapper.insert(statusLog) == 0) {
            throw new BusinessException(ResultCode.FAILED);
        }
    }

    private void insertOperationLog(Long bizId, String operationType, String moduleCode,
                                    Long operatorId, String remark, LocalDateTime operationTime) {
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setUserId(operatorId);
        operationLog.setOperationType(operationType);
        operationLog.setModuleCode(moduleCode);
        operationLog.setBizId(bizId);
        operationLog.setOperationTime(operationTime);
        operationLog.setCreatedBy(operatorId);
        operationLog.setCreatedTime(operationTime);
        operationLog.setUpdatedBy(operatorId);
        operationLog.setUpdatedTime(operationTime);
        operationLog.setRemark(remark);
        operationLog.setDeletedFlag(0);
        if (sysOperationLogMapper.insert(operationLog) == 0) {
            throw new BusinessException(ResultCode.FAILED);
        }
    }

    private MfgScheduleScenario requireManualScenario(Long scenarioId, boolean requireDraft) {
        MfgScheduleScenario scenario = getActiveScenario(scenarioId);
        if (scenario == null || !ALGORITHM_CODE_MANUAL_BOARD.equals(scenario.getAlgorithmCode())) {
            throw notFound("手工排程草案不存在或已删除");
        }
        if (requireDraft && !STATUS_DRAFT.equals(scenario.getStatus())) {
            throw invalid("当前临时安排已不能继续编辑，请刷新排程总览后重新操作");
        }
        if (!STATUS_DRAFT.equals(scenario.getStatus()) && !STATUS_CALCULATED.equals(scenario.getStatus())) {
            throw invalid("当前临时安排已不能继续操作，请刷新排程总览后重新选择");
        }
        return scenario;
    }

    private void assertScenarioVersion(MfgScheduleScenario scenario, Integer version) {
        if (version == null || !Objects.equals(version, scenario.getVersion())) {
            throw conflict("排程已更新，请刷新排程总览后重新操作");
        }
    }

    private MfgProductionOrder requireDraftOrderWithoutPlan(Long productionOrderId) {
        if (productionOrderId == null) {
            throw invalid("生产订单不能为空");
        }
        MfgProductionOrder order = getActiveProductionOrder(productionOrderId);
        if (order == null) {
            throw notFound("生产订单不存在或已删除");
        }
        if (hasActivePlan(productionOrderId)) {
            throw invalid("该订单已生成生产计划，请在生产计划中查看或调整");
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            throw invalid("该订单已下达或已进入执行，不能再调整临时安排");
        }
        if (order.getProductId() == null || order.getOrderQuantity() == null
                || order.getOrderQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw invalid("生产订单缺少有效产品或数量");
        }
        return order;
    }

    private MfgScheduleItem requireManualItem(Long scenarioId, Long scheduleItemId) {
        if (scheduleItemId == null) {
            throw invalid("排程明细不能为空");
        }
        QueryWrapper<MfgScheduleItem> query = new QueryWrapper<>();
        query.eq("schedule_item_id", scheduleItemId).eq("scenario_id", scenarioId).eq("deleted_flag", 0);
        MfgScheduleItem item = mfgScheduleItemMapper.selectOne(query);
        if (item == null) {
            throw notFound("排程明细不存在或已删除");
        }
        if (!DECISION_SOURCE_MANUAL.equals(item.getDecisionSource())) {
            throw invalid("当前明细不是手工编排记录");
        }
        return item;
    }

    private MfgScheduleItem findRemovedScenarioOrderItem(Long scenarioId, Long productionOrderId) {
        QueryWrapper<MfgScheduleItem> query = new QueryWrapper<>();
        query.eq("scenario_id", scenarioId)
                .eq("production_order_id", productionOrderId)
                .eq("deleted_flag", 1);
        return mfgScheduleItemMapper.selectOne(query);
    }

    private void restoreRemovedManualItem(MfgScheduleItem item, MfgScheduleItem removedItem) {
        if (!DECISION_SOURCE_MANUAL.equals(removedItem.getDecisionSource())) {
            throw invalid("当前草案中的历史排程明细不是人工编排记录，不能重新加入");
        }
        int currentVersion = removedItem.getVersion() == null ? 0 : removedItem.getVersion();
        UpdateWrapper<MfgScheduleItem> update = new UpdateWrapper<>();
        update.eq("schedule_item_id", removedItem.getScheduleItemId())
                .eq("version", currentVersion)
                .set("product_id", item.getProductId())
                .set("route_id", item.getRouteId())
                .set("line_id", item.getLineId())
                .set("plan_id", null)
                .set("sequence_no", item.getSequenceNo())
                .set("planned_quantity", item.getPlannedQuantity())
                .set("planned_start_time", item.getPlannedStartTime())
                .set("planned_end_time", item.getPlannedEndTime())
                .set("estimated_minutes", item.getEstimatedMinutes())
                .set("priority_score", item.getPriorityScore())
                .set("tardiness_minutes", item.getTardinessMinutes())
                .set("decision_source", item.getDecisionSource())
                .set("status", item.getStatus())
                .set("conflict_reason", null)
                .set("explanation", item.getExplanation())
                .set("remark", null)
                .set("deleted_flag", 0)
                .set("version", currentVersion + 1)
                .set("updated_by", item.getUpdatedBy())
                .set("updated_time", item.getUpdatedTime());
        if (mfgScheduleItemMapper.update(null, update) == 0) {
            throw conflict("排程明细已被其他用户修改，请刷新后重试");
        }
        item.setScheduleItemId(removedItem.getScheduleItemId());
        item.setVersion(currentVersion + 1);
        item.setCreatedBy(removedItem.getCreatedBy());
        item.setCreatedTime(removedItem.getCreatedTime());
    }

    private void ensureOrderNotInEditableScenario(Long productionOrderId, Long currentScenarioId) {
        QueryWrapper<MfgScheduleItem> itemQuery = new QueryWrapper<>();
        itemQuery.eq("production_order_id", productionOrderId).eq("deleted_flag", 0);
        for (MfgScheduleItem item : mfgScheduleItemMapper.selectList(itemQuery)) {
            if (Objects.equals(item.getScenarioId(), currentScenarioId)) {
                throw invalid("该订单已在当前排程中，无需重复添加");
            }
            MfgScheduleScenario scenario = getActiveScenario(item.getScenarioId());
            if (scenario != null && ALGORITHM_CODE_MANUAL_BOARD.equals(scenario.getAlgorithmCode())
                    && (STATUS_DRAFT.equals(scenario.getStatus()) || STATUS_CALCULATED.equals(scenario.getStatus()))) {
                throw invalid("该订单已在排程总览中，无需重复添加");
            }
        }
    }

    private int nextSequenceNo(List<MfgScheduleItem> items) {
        return items.stream().map(MfgScheduleItem::getSequenceNo)
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(0) + 1;
    }

    private List<ManualScheduleCandidateLine> buildEligibleLines(MfgScheduleScenario scenario, MfgProductionOrder order) {
        return buildEligibleLines(scenario, order, listGlobalEditableManualItems());
    }

    private List<ManualScheduleCandidateLine> buildEligibleLines(MfgScheduleScenario scenario, MfgProductionOrder order,
                                                                  List<MfgScheduleItem> items) {
        LocalDateTime now = LocalDateTime.now(SCHEDULE_ZONE);
        LocalDateTime horizonStart = maxTime(scenario.getHorizonStart(), now.toLocalDate().atStartOfDay());
        LocalDateTime horizonEnd = scenario.getHorizonEnd() == null || scenario.getHorizonEnd().isBefore(horizonStart.plusDays(30))
                ? horizonStart.plusDays(30).withHour(23).withMinute(59).withSecond(59) : scenario.getHorizonEnd();
        SchedulingContext context = loadSchedulingContext(horizonStart, horizonEnd);
        List<MfgProductionPlan> activePlans = listActiveProductionPlans();
        List<ManualScheduleCandidateLine> result = new ArrayList<>();
        for (MfgLineProductCapability capability : context.capabilitiesByProduct()
                .getOrDefault(order.getProductId(), List.of())) {
            FactoryLine line = context.activeLines().get(capability.getLineId());
            ProcessRoute route = context.releasedRoutes().get(capability.getRouteId());
            BigDecimal unitMinutes = context.routeMinutesByRoute().get(capability.getRouteId());
            if (line == null || !STATUS_RUNNING.equals(line.getStatus()) || route == null
                    || !Objects.equals(route.getProductId(), order.getProductId()) || unitMinutes == null
                    || capability.getCapacityFactor() == null || capability.getCapacityFactor().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            LocalDateTime lineAvailableAt = horizonStart;
            for (MfgScheduleItem item : items) {
                if (Objects.equals(item.getLineId(), line.getLineId()) && item.getPlannedEndTime() != null) {
                    lineAvailableAt = maxTime(lineAvailableAt, item.getPlannedEndTime());
                }
            }
            for (MfgProductionPlan plan : activePlans) {
                if (Objects.equals(plan.getLineId(), line.getLineId()) && plan.getPlannedEndTime() != null) {
                    lineAvailableAt = maxTime(lineAvailableAt, plan.getPlannedEndTime());
                }
            }
            List<CapacityWindow> lineWindows = buildLineWindows(line,
                    context.calendarRulesByLine().getOrDefault(line.getLineId(), List.of()), context.shiftsById(),
                    lineAvailableAt, horizonEnd);
            LineScheduleState state = new LineScheduleState(lineWindows);
            BigDecimal requiredCapacity = order.getOrderQuantity().divide(capability.getCapacityFactor(),
                    CAPACITY_SCALE, RoundingMode.HALF_UP);
            Allocation allocation = state.allocate(requiredCapacity);
            if (allocation == null) {
                continue;
            }
            ManualScheduleCandidateLine candidate = new ManualScheduleCandidateLine();
            candidate.setLineId(line.getLineId());
            candidate.setLineCode(line.getLineCode());
            candidate.setLineName(line.getLineName());
            candidate.setWorkshopId(line.getWorkshopId());
            candidate.setRouteId(route.getRouteId());
            candidate.setPriorityLevel(capability.getPriorityLevel());
            candidate.setCapacityFactor(capability.getCapacityFactor());
            candidate.setEstimatedMinutes(unitMinutes.multiply(order.getOrderQuantity()).setScale(2, RoundingMode.HALF_UP));
            candidate.setEarliestStartTime(allocation.startTime());
            candidate.setEarliestEndTime(allocation.endTime());
            candidate.setMinimumWorkingMinutes(calculateWorkingMinutes(lineWindows,
                    allocation.startTime(), allocation.endTime()));
            result.add(candidate);
        }
        result.sort(Comparator.comparing(ManualScheduleCandidateLine::getEarliestEndTime)
                .thenComparing(ManualScheduleCandidateLine::getPriorityLevel, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ManualScheduleCandidateLine::getLineId));
        return result;
    }

    private void cacheEligibleLines(MfgScheduleScenario scenario, MfgProductionOrder order,
                                    List<ManualScheduleCandidateLine> candidates) {
        long now = System.nanoTime();
        if (manualCandidateListCache.size() > MANUAL_CANDIDATE_CACHE_MAX_SIZE) {
            manualCandidateListCache.entrySet().removeIf(entry -> entry.getValue().expiresAtNanos() <= now);
            if (manualCandidateListCache.size() > MANUAL_CANDIDATE_CACHE_MAX_SIZE) {
                manualCandidateListCache.clear();
            }
        }
        long expiresAt = now + MANUAL_CANDIDATE_CACHE_TTL_NANOS;
        ManualCandidateGroupKey groupKey = new ManualCandidateGroupKey(scenario.getScenarioId(), scenario.getVersion(),
                order.getProductionOrderId());
        manualCandidateListCache.put(groupKey,
                new ManualCandidateListCacheEntry(List.copyOf(candidates), expiresAt));
    }

    private List<ManualScheduleCandidateLine> findCachedEligibleLines(MfgScheduleScenario scenario,
                                                                       MfgProductionOrder order) {
        ManualCandidateGroupKey key = new ManualCandidateGroupKey(scenario.getScenarioId(), scenario.getVersion(),
                order.getProductionOrderId());
        ManualCandidateListCacheEntry entry = manualCandidateListCache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtNanos() <= System.nanoTime()) {
            manualCandidateListCache.remove(key, entry);
            return null;
        }
        return entry.candidates();
    }

    private void invalidateCandidateCache(Long scenarioId) {
        manualCandidateListCache.clear();
    }

    private ManualScheduleCandidateLine resolveManualCapability(MfgProductionOrder order, Long lineId,
                                                                LocalDateTime startTime, LocalDateTime endTime) {
        if (lineId == null || startTime == null || endTime == null) {
            throw invalid("产线和计划时间不能为空");
        }
        SchedulingContext context = loadSchedulingContext(startTime.toLocalDate().atStartOfDay(),
                endTime.toLocalDate().plusDays(1).atStartOfDay());
        FactoryLine line = context.activeLines().get(lineId);
        if (line == null || !STATUS_RUNNING.equals(line.getStatus())) {
            throw invalid("产线不存在、已删除或未处于 RUNNING 状态");
        }
        for (MfgLineProductCapability capability : context.capabilitiesByProduct()
                .getOrDefault(order.getProductId(), List.of())) {
            if (!Objects.equals(capability.getLineId(), lineId) || capability.getCapacityFactor() == null
                    || capability.getCapacityFactor().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ProcessRoute route = context.releasedRoutes().get(capability.getRouteId());
            BigDecimal unitMinutes = context.routeMinutesByRoute().get(capability.getRouteId());
            if (route == null || !Objects.equals(route.getProductId(), order.getProductId()) || unitMinutes == null) {
                continue;
            }
            ManualScheduleCandidateLine result = new ManualScheduleCandidateLine();
            result.setLineId(lineId);
            result.setLineCode(line.getLineCode());
            result.setLineName(line.getLineName());
            result.setWorkshopId(line.getWorkshopId());
            result.setRouteId(route.getRouteId());
            result.setPriorityLevel(capability.getPriorityLevel());
            result.setCapacityFactor(capability.getCapacityFactor());
            result.setEstimatedMinutes(unitMinutes.multiply(order.getOrderQuantity()).setScale(2, RoundingMode.HALF_UP));
            return result;
        }
        throw invalid("所选产线没有该产品的启用能力或已发布路线");
    }

    private String validateManualPlacement(MfgScheduleScenario scenario, MfgScheduleItem currentItem,
                                           MfgProductionOrder order, Long lineId, LocalDateTime startTime,
                                           LocalDateTime endTime, List<MfgScheduleItem> allItems) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return "计划开始时间必须早于结束时间";
        }
        ManualScheduleCandidateLine capability;
        try {
            capability = resolveManualCapability(order, lineId, startTime, endTime);
        } catch (BusinessException e) {
            return e.getMessage();
        }
        SchedulingContext context = loadSchedulingContext(startTime.toLocalDate().atStartOfDay(),
                endTime.toLocalDate().plusDays(1).atStartOfDay());
        FactoryLine line = context.activeLines().get(lineId);
        List<CapacityWindow> calendarWindows = buildLineWindows(line,
                context.calendarRulesByLine().getOrDefault(lineId, List.of()), context.shiftsById(),
                startTime.toLocalDate().atStartOfDay(), endTime.toLocalDate().plusDays(1).atStartOfDay());
        boolean startInCalendar = calendarWindows.stream().anyMatch(window ->
                !startTime.isBefore(window.startTime()) && startTime.isBefore(window.endTime()));
        boolean endInCalendar = calendarWindows.stream().anyMatch(window ->
                endTime.isAfter(window.startTime()) && !endTime.isAfter(window.endTime()));
        if (!startInCalendar || !endInCalendar) {
            return "计划时间未完全落在产线有效周历和班次内";
        }
        BigDecimal availableCapacity = buildLineWindows(line,
                context.calendarRulesByLine().getOrDefault(lineId, List.of()), context.shiftsById(), startTime, endTime)
                .stream().map(CapacityWindow::capacity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal requiredCapacity = order.getOrderQuantity().divide(capability.getCapacityFactor(),
                CAPACITY_SCALE, RoundingMode.HALF_UP);
        if (availableCapacity.compareTo(requiredCapacity) < 0) {
            return "计划时段内的产线有效产能不足";
        }
        List<MfgScheduleItem> occupiedItems = new ArrayList<>(allItems);
        occupiedItems.addAll(listGlobalEditableManualItems());
        for (MfgScheduleItem other : occupiedItems) {
            if (other.getScheduleItemId() != null && currentItem.getScheduleItemId() != null
                    && Objects.equals(other.getScheduleItemId(), currentItem.getScheduleItemId())) {
                continue;
            }
            if (Objects.equals(other.getLineId(), lineId) && overlaps(startTime, endTime,
                    other.getPlannedStartTime(), other.getPlannedEndTime())) {
                return "计划时间与当前草案中的其他订单重叠";
            }
        }
        QueryWrapper<MfgProductionPlan> planQuery = new QueryWrapper<>();
        planQuery.eq("line_id", lineId).eq("deleted_flag", 0).in("status", STATUS_DRAFT, STATUS_RELEASED);
        for (MfgProductionPlan plan : mfgProductionPlanMapper.selectList(planQuery)) {
            if (overlaps(startTime, endTime, plan.getPlannedStartTime(), plan.getPlannedEndTime())) {
                return "计划时间与已发布生产计划重叠";
            }
        }
        return null;
    }

    private long calculateWorkingMinutes(List<CapacityWindow> windows, LocalDateTime startTime,
                                         LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            return 0L;
        }
        long seconds = 0L;
        for (CapacityWindow window : windows) {
            LocalDateTime overlapStart = startTime.isAfter(window.startTime()) ? startTime : window.startTime();
            LocalDateTime overlapEnd = endTime.isBefore(window.endTime()) ? endTime : window.endTime();
            if (overlapEnd.isAfter(overlapStart)) {
                seconds += Duration.between(overlapStart, overlapEnd).toSeconds();
            }
        }
        return (seconds + 59L) / 60L;
    }

    private String validateManualItem(MfgScheduleScenario scenario, MfgScheduleItem item,
                                      List<MfgScheduleItem> allItems) {
        try {
            MfgProductionOrder order = requireDraftOrderWithoutPlan(item.getProductionOrderId());
            return validateManualPlacement(scenario, item, order, item.getLineId(), item.getPlannedStartTime(),
                    item.getPlannedEndTime(), allItems);
        } catch (BusinessException e) {
            return e.getMessage();
        }
    }

    private boolean overlaps(LocalDateTime start, LocalDateTime end, LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start != null && end != null && otherStart != null && otherEnd != null
                && start.isBefore(otherEnd) && end.isAfter(otherStart);
    }

    private void refreshManualScenarioSummary(MfgScheduleScenario scenario, String targetStatus,
                                              Long operatorId, LocalDateTime now) {
        refreshManualScenarioSummary(scenario, targetStatus, operatorId, now,
                listActiveScenarioItems(scenario.getScenarioId()));
    }

    private void refreshManualScenarioSummary(MfgScheduleScenario scenario, String targetStatus,
                                              Long operatorId, LocalDateTime now, List<MfgScheduleItem> items) {
        int scheduled = 0;
        int conflicts = 0;
        int late = 0;
        BigDecimal load = BigDecimal.ZERO;
        LocalDateTime earliest = null;
        LocalDateTime latest = null;
        for (MfgScheduleItem item : items) {
            if (ITEM_STATUS_CONFLICT.equals(item.getStatus())) {
                conflicts++;
                continue;
            }
            scheduled++;
            if (item.getEstimatedMinutes() != null) {
                load = load.add(item.getEstimatedMinutes());
            }
            if (item.getTardinessMinutes() != null && item.getTardinessMinutes() > 0) {
                late++;
            }
            if (item.getPlannedStartTime() != null && (earliest == null || item.getPlannedStartTime().isBefore(earliest))) {
                earliest = item.getPlannedStartTime();
            }
            if (item.getPlannedEndTime() != null && (latest == null || item.getPlannedEndTime().isAfter(latest))) {
                latest = item.getPlannedEndTime();
            }
        }
        MfgScheduleScenario update = new MfgScheduleScenario();
        update.setScenarioId(scenario.getScenarioId());
        update.setVersion(scenario.getVersion());
        update.setStatus(targetStatus);
        update.setOrderCount(items.size());
        update.setScheduledOrderCount(scheduled);
        update.setConflictOrderCount(conflicts);
        update.setLateOrderCount(late);
        update.setTotalLoadMinutes(load.setScale(2, RoundingMode.HALF_UP));
        update.setHorizonStart(earliest == null ? scenario.getHorizonStart() : earliest);
        update.setHorizonEnd(latest == null ? scenario.getHorizonEnd() : latest);
        update.setAiAdvice(null);
        update.setAiModel(null);
        update.setAiGeneratedTime(null);
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (baseMapper.updateById(update) == 0) {
            throw conflict("手工排程草案已被其他用户修改，请刷新后重试");
        }
    }

    private LocalDateTime maxTime(LocalDateTime left, LocalDateTime right) {
        if (left == null) {
            return right;
        }
        return left.isAfter(right) ? left : right;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private MfgScheduleItem scheduleOrder(MfgScheduleScenario scenario, MfgProductionOrder order, int sequenceNo,
                                          int totalOrderCount, SchedulingContext context,
                                          Map<Long, LineScheduleState> lineStates) {
        MfgScheduleItem item = new MfgScheduleItem();
        item.setScenarioId(scenario.getScenarioId());
        item.setProductionOrderId(order.getProductionOrderId());
        item.setProductId(order.getProductId());
        item.setSequenceNo(sequenceNo);
        item.setPlannedQuantity(order.getOrderQuantity());
        item.setPriorityScore(BigDecimal.valueOf(totalOrderCount - sequenceNo + 1L));
        item.setDecisionSource(DECISION_SOURCE_RULE_ENGINE);
        item.setTardinessMinutes(0L);
        item.setDeletedFlag(0);

        String inputProblem = validateOrderInput(order);
        if (inputProblem != null) {
            return conflict(item, inputProblem);
        }

        List<MfgLineProductCapability> capabilities = context.capabilitiesByProduct()
                .getOrDefault(order.getProductId(), List.of());
        if (capabilities.isEmpty()) {
            return conflict(item, "未配置启用的产线产品能力");
        }

        boolean hasRunningLine = false;
        boolean hasReleasedRoute = false;
        boolean hasRouteWorkTime = false;
        boolean hasCalendar = false;
        CandidateAllocation winner = null;

        for (MfgLineProductCapability capability : capabilities) {
            FactoryLine line = context.activeLines().get(capability.getLineId());
            if (line == null || !STATUS_RUNNING.equals(line.getStatus())) {
                continue;
            }
            hasRunningLine = true;

            ProcessRoute route = context.releasedRoutes().get(capability.getRouteId());
            if (route == null || !Objects.equals(route.getProductId(), order.getProductId())) {
                continue;
            }
            hasReleasedRoute = true;

            BigDecimal unitRouteMinutes = context.routeMinutesByRoute().get(route.getRouteId());
            if (unitRouteMinutes == null) {
                continue;
            }
            hasRouteWorkTime = true;

            LineScheduleState state = lineStates.computeIfAbsent(line.getLineId(), lineId ->
                    new LineScheduleState(buildLineWindows(line, context.calendarRulesByLine().getOrDefault(lineId, List.of()),
                            context.shiftsById(), scenario.getHorizonStart(), scenario.getHorizonEnd())));
            if (state.isEmpty()) {
                continue;
            }
            hasCalendar = true;

            if (capability.getCapacityFactor() == null || capability.getCapacityFactor().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal requiredBaseCapacity = order.getOrderQuantity()
                    .divide(capability.getCapacityFactor(), CAPACITY_SCALE, RoundingMode.HALF_UP);
            LineScheduleState simulation = state.copy();
            Allocation allocation = simulation.allocate(requiredBaseCapacity);
            if (allocation == null) {
                continue;
            }

            BigDecimal estimatedMinutes = unitRouteMinutes.multiply(order.getOrderQuantity())
                    .setScale(2, RoundingMode.HALF_UP);
            CandidateAllocation candidate = new CandidateAllocation(capability, route, simulation, allocation, estimatedMinutes);
            if (isPreferred(candidate, winner)) {
                winner = candidate;
            }
        }

        if (winner == null) {
            if (!hasRunningLine) {
                return conflict(item, "候选产线不存在、已删除或未处于 RUNNING 状态");
            }
            if (!hasReleasedRoute) {
                return conflict(item, "候选产线未配置已发布且匹配产品的工艺路线");
            }
            if (!hasRouteWorkTime) {
                return conflict(item, "工艺路线缺少有效标准工时");
            }
            if (!hasCalendar) {
                return conflict(item, "候选产线在排程窗口内没有有效周历");
            }
            return conflict(item, "排程窗口有效产能不足");
        }

        lineStates.put(winner.capability().getLineId(), winner.stateAfterAllocation());
        item.setRouteId(winner.route().getRouteId());
        item.setLineId(winner.capability().getLineId());
        item.setPlannedStartTime(winner.allocation().startTime());
        item.setPlannedEndTime(winner.allocation().endTime());
        item.setEstimatedMinutes(winner.estimatedMinutes());
        item.setTardinessMinutes(calculateTardiness(order.getDeliveryDate(), winner.allocation().endTime()));
        item.setStatus(ITEM_STATUS_SCHEDULED);
        item.setExplanation("按交期顺序计算；选择最早完成的候选产线，适配优先级="
                + winner.capability().getPriorityLevel() + "，产品能力系数=" + winner.capability().getCapacityFactor());
        return item;
    }

    private boolean isPreferred(CandidateAllocation candidate, CandidateAllocation currentWinner) {
        if (currentWinner == null) {
            return true;
        }
        int endComparison = candidate.allocation().endTime().compareTo(currentWinner.allocation().endTime());
        if (endComparison != 0) {
            return endComparison < 0;
        }
        int priorityComparison = Integer.compare(candidate.capability().getPriorityLevel(), currentWinner.capability().getPriorityLevel());
        if (priorityComparison != 0) {
            return priorityComparison < 0;
        }
        int lineComparison = Long.compare(candidate.capability().getLineId(), currentWinner.capability().getLineId());
        if (lineComparison != 0) {
            return lineComparison < 0;
        }
        return Long.compare(candidate.capability().getCapabilityId(), currentWinner.capability().getCapabilityId()) < 0;
    }

    private MfgScheduleItem conflict(MfgScheduleItem item, String reason) {
        item.setStatus(ITEM_STATUS_CONFLICT);
        item.setConflictReason(reason);
        item.setExplanation("未生成部分排程：" + reason);
        item.setLineId(null);
        item.setRouteId(null);
        item.setPlannedStartTime(null);
        item.setPlannedEndTime(null);
        item.setEstimatedMinutes(null);
        item.setTardinessMinutes(0L);
        return item;
    }

    private String validateOrderInput(MfgProductionOrder order) {
        if (order.getProductionOrderId() == null) {
            return "生产订单主键不能为空";
        }
        if (order.getProductId() == null) {
            return "生产订单缺少产品";
        }
        if (order.getOrderQuantity() == null || order.getOrderQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return "生产订单数量必须大于 0";
        }
        if (order.getDeliveryDate() == null) {
            return "生产订单缺少交付日期";
        }
        return null;
    }

    private void validateDraftOrders(List<MfgProductionOrder> orders) {
        for (MfgProductionOrder order : orders) {
            String problem = validateOrderInput(order);
            if (problem != null) {
                throw invalid("生产订单 " + (order.getProductionOrderNo() == null ? "<未编号>" : order.getProductionOrderNo())
                        + " 数据不完整：" + problem);
            }
        }
    }

    private long calculateTardiness(LocalDate deliveryDate, LocalDateTime plannedEndTime) {
        LocalDateTime deadline = deliveryDate.atTime(LocalTime.MAX);
        if (!plannedEndTime.isAfter(deadline)) {
            return 0L;
        }
        return Duration.between(deadline, plannedEndTime).toMinutes();
    }

    private MfgScheduleScenario createDraftScenario(MfgScheduleScenario request, String scenarioNo) {
        MfgScheduleScenario scenario = new MfgScheduleScenario();
        scenario.setScenarioNo(scenarioNo);
        scenario.setScenarioName(request.getScenarioName().trim());
        scenario.setHorizonStart(request.getHorizonStart());
        scenario.setHorizonEnd(request.getHorizonEnd());
        scenario.setAlgorithmCode(ALGORITHM_CODE);
        scenario.setStatus(STATUS_DRAFT);
        scenario.setOrderCount(0);
        scenario.setScheduledOrderCount(0);
        scenario.setConflictOrderCount(0);
        scenario.setLateOrderCount(0);
        scenario.setTotalLoadMinutes(BigDecimal.ZERO);
        scenario.setRemark(request.getRemark());
        scenario.setDeletedFlag(0);
        return scenario;
    }

    private void validateCalculationRequest(MfgScheduleScenario request) {
        if (request == null || !StringUtils.hasText(request.getScenarioNo()) || !StringUtils.hasText(request.getScenarioName())) {
            throw invalid("方案编号和方案名称不能为空");
        }
        if (request.getHorizonStart() == null || request.getHorizonEnd() == null
                || !request.getHorizonEnd().isAfter(request.getHorizonStart())) {
            throw invalid("排程窗口结束时间必须晚于开始时间");
        }
    }

    private void ensureScenarioNoAvailable(String scenarioNo) {
        QueryWrapper<MfgScheduleScenario> wrapper = new QueryWrapper<>();
        wrapper.eq("scenario_no", scenarioNo);
        wrapper.eq("deleted_flag", 0);
        if (baseMapper.selectCount(wrapper) > 0) {
            throw invalid("排程方案编号已存在");
        }
    }

    private List<MfgProductionOrder> listDraftOrders() {
        QueryWrapper<MfgProductionOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        wrapper.eq("status", STATUS_DRAFT);
        wrapper.orderByAsc("delivery_date");
        wrapper.orderByAsc("production_order_no");
        wrapper.orderByAsc("production_order_id");
        return mfgProductionOrderMapper.selectList(wrapper);
    }

    private SchedulingContext loadSchedulingContext(LocalDateTime horizonStart, LocalDateTime horizonEnd) {
        List<MfgLineProductCapability> capabilities = mfgLineProductCapabilityMapper.selectList(activeCapabilityQuery());
        Map<Long, List<MfgLineProductCapability>> capabilitiesByProduct = capabilities.stream()
                .collect(Collectors.groupingBy(MfgLineProductCapability::getProductId));

        Map<Long, FactoryLine> activeLines = factoryLineMapper.selectList(activeLineQuery()).stream()
                .collect(Collectors.toMap(FactoryLine::getLineId, line -> line));
        Map<Long, ProcessRoute> releasedRoutes = processRouteMapper.selectList(releasedRouteQuery()).stream()
                .collect(Collectors.toMap(ProcessRoute::getRouteId, route -> route));
        Map<Long, BigDecimal> routeMinutesByRoute = buildRouteMinutesByRoute();
        Map<Long, List<MfgLineCalendarRule>> calendarRulesByLine = mfgLineCalendarRuleMapper
                .selectList(activeCalendarRuleQuery()).stream()
                .collect(Collectors.groupingBy(MfgLineCalendarRule::getLineId));
        Map<Long, OrgShift> shiftsById = orgShiftMapper.selectList(activeShiftQuery()).stream()
                .collect(Collectors.toMap(OrgShift::getShiftId, shift -> shift));

        return new SchedulingContext(capabilitiesByProduct, activeLines, releasedRoutes, routeMinutesByRoute,
                calendarRulesByLine, shiftsById, horizonStart, horizonEnd);
    }

    private Map<Long, BigDecimal> buildRouteMinutesByRoute() {
        Map<Long, List<ProcessRouteItem>> itemsByRoute = processRouteItemMapper.selectList(activeRouteItemQuery()).stream()
                .filter(item -> item.getRouteId() != null)
                .collect(Collectors.groupingBy(ProcessRouteItem::getRouteId));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map.Entry<Long, List<ProcessRouteItem>> entry : itemsByRoute.entrySet()) {
            List<ProcessRouteItem> items = entry.getValue();
            boolean valid = !items.isEmpty() && items.stream().allMatch(item ->
                    item.getStandardTime() != null && item.getStandardTime().compareTo(BigDecimal.ZERO) > 0);
            if (valid) {
                result.put(entry.getKey(), items.stream().map(ProcessRouteItem::getStandardTime)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
            }
        }
        return result;
    }

    private List<CapacityWindow> buildLineWindows(FactoryLine line, List<MfgLineCalendarRule> rules,
                                                   Map<Long, OrgShift> shiftsById, LocalDateTime horizonStart,
                                                   LocalDateTime horizonEnd) {
        if (line.getDailyCapacity() == null || line.getDailyCapacity().compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        List<CapacityWindow> windows = new ArrayList<>();
        LocalDate date = horizonStart.toLocalDate();
        LocalDate endDate = horizonEnd.toLocalDate();
        while (!date.isAfter(endDate)) {
            Map<Long, MfgLineCalendarRule> rulesByShift = latestRulesForDate(rules, date);
            List<ShiftSegment> daySegments = new ArrayList<>();
            long fullDaySeconds = 0L;
            for (MfgLineCalendarRule rule : rulesByShift.values()) {
                OrgShift shift = shiftsById.get(rule.getShiftId());
                if (shift == null || shift.getStartTime() == null || shift.getEndTime() == null
                        || rule.getCapacityFactor() == null || rule.getCapacityFactor().compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                LocalDateTime fullStart = inScheduleZone(date, shift.getStartTime());
                LocalDateTime fullEnd = inScheduleZone(date, shift.getEndTime());
                if (shift.getEndTime().isBefore(shift.getStartTime())) {
                    fullEnd = fullEnd.plusDays(1);
                }
                if (!fullEnd.isAfter(fullStart)) {
                    continue;
                }
                long fullShiftSeconds = Duration.between(fullStart, fullEnd).toSeconds();
                fullDaySeconds += fullShiftSeconds;
                LocalDateTime start = fullStart.isBefore(horizonStart) ? horizonStart : fullStart;
                LocalDateTime end = fullEnd.isAfter(horizonEnd) ? horizonEnd : fullEnd;
                if (end.isAfter(start)) {
                    daySegments.add(new ShiftSegment(start, end, fullShiftSeconds, rule.getCapacityFactor()));
                }
            }

            if (fullDaySeconds > 0) {
                for (ShiftSegment segment : daySegments) {
                    long clippedSeconds = Duration.between(segment.startTime(), segment.endTime()).toSeconds();
                    BigDecimal baseCapacity = line.getDailyCapacity()
                            .multiply(segment.capacityFactor())
                            .multiply(BigDecimal.valueOf(clippedSeconds))
                            .divide(BigDecimal.valueOf(fullDaySeconds), CAPACITY_SCALE, RoundingMode.HALF_UP);
                    if (baseCapacity.compareTo(BigDecimal.ZERO) > 0) {
                        windows.add(new CapacityWindow(segment.startTime(), segment.endTime(), baseCapacity, BigDecimal.ZERO));
                    }
                }
            }
            date = date.plusDays(1);
        }
        windows.sort(Comparator.comparing(CapacityWindow::startTime));
        return windows;
    }

    private Map<Long, MfgLineCalendarRule> latestRulesForDate(List<MfgLineCalendarRule> rules, LocalDate date) {
        Map<Long, MfgLineCalendarRule> result = new HashMap<>();
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        for (MfgLineCalendarRule rule : rules) {
            if (!Objects.equals(rule.getDayOfWeek(), dayOfWeek.getValue()) || rule.getEffectiveFrom() == null
                    || rule.getEffectiveFrom().isAfter(date)
                    || (rule.getEffectiveTo() != null && rule.getEffectiveTo().isBefore(date))) {
                continue;
            }
            MfgLineCalendarRule existing = result.get(rule.getShiftId());
            if (existing == null || rule.getEffectiveFrom().isAfter(existing.getEffectiveFrom())) {
                result.put(rule.getShiftId(), rule);
            }
        }
        return result;
    }

    private LocalDateTime inScheduleZone(LocalDate date, LocalTime time) {
        return ZonedDateTime.of(date, time, SCHEDULE_ZONE).toLocalDateTime();
    }

    private QueryWrapper<MfgScheduleScenario> buildScenarioQuery(String keyword, String status) {
        QueryWrapper<MfgScheduleScenario> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("scenario_no", keyword).or().like("scenario_name", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim().toUpperCase());
        }
        wrapper.orderByDesc("scenario_id");
        return wrapper;
    }

    private QueryWrapper<MfgLineProductCapability> activeCapabilityQuery() {
        QueryWrapper<MfgLineProductCapability> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        wrapper.eq("status", STATUS_ENABLE);
        wrapper.orderByAsc("priority_level");
        wrapper.orderByAsc("capability_id");
        return wrapper;
    }

    private QueryWrapper<FactoryLine> activeLineQuery() {
        QueryWrapper<FactoryLine> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        return wrapper;
    }

    private QueryWrapper<ProcessRoute> releasedRouteQuery() {
        QueryWrapper<ProcessRoute> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        wrapper.eq("status", STATUS_RELEASED);
        return wrapper;
    }

    private QueryWrapper<ProcessRouteItem> activeRouteItemQuery() {
        QueryWrapper<ProcessRouteItem> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        wrapper.orderByAsc("sequence_no");
        wrapper.orderByAsc("route_item_id");
        return wrapper;
    }

    private QueryWrapper<MfgLineCalendarRule> activeCalendarRuleQuery() {
        QueryWrapper<MfgLineCalendarRule> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        wrapper.eq("status", STATUS_ENABLE);
        return wrapper;
    }

    private QueryWrapper<OrgShift> activeShiftQuery() {
        QueryWrapper<OrgShift> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        return wrapper;
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ResultCode.DATA_CONFLICT.getCode(), message);
    }

    private BusinessException notFound(String message) {
        return new BusinessException(ResultCode.NOT_FOUND.getCode(), message);
    }

    private record ManualCandidateGroupKey(Long scenarioId, Integer scenarioVersion, Long productionOrderId) {
    }

    private record ManualCandidateListCacheEntry(List<ManualScheduleCandidateLine> candidates, long expiresAtNanos) {
    }

    private record SchedulingContext(Map<Long, List<MfgLineProductCapability>> capabilitiesByProduct,
                                     Map<Long, FactoryLine> activeLines,
                                     Map<Long, ProcessRoute> releasedRoutes,
                                     Map<Long, BigDecimal> routeMinutesByRoute,
                                     Map<Long, List<MfgLineCalendarRule>> calendarRulesByLine,
                                     Map<Long, OrgShift> shiftsById,
                                     LocalDateTime horizonStart,
                                     LocalDateTime horizonEnd) {
    }

    private record CandidateAllocation(MfgLineProductCapability capability, ProcessRoute route,
                                       LineScheduleState stateAfterAllocation, Allocation allocation,
                                       BigDecimal estimatedMinutes) {
    }

    private record ConfirmationEntry(MfgScheduleItem item, MfgProductionOrder order) {
    }

    private static final class BoardLineAiLoad {
        private final Long lineId;
        private int manualItemCount;
        private int productionPlanCount;
        private int lateCount;
        private BigDecimal totalPlannedQuantity = BigDecimal.ZERO;
        private long totalPlannedMinutes;
        private LocalDateTime earliestStartTime;
        private LocalDateTime latestEndTime;

        private BoardLineAiLoad(Long lineId) {
            this.lineId = lineId;
        }

        private void add(ManualScheduleBoardItem item) {
            if ("PLAN".equals(item.getSourceType())) {
                productionPlanCount++;
            } else {
                manualItemCount++;
            }
            if (item.getPlannedQuantity() != null) {
                totalPlannedQuantity = totalPlannedQuantity.add(item.getPlannedQuantity());
            }
            if (item.getPlannedStartTime() != null && item.getPlannedEndTime() != null
                    && item.getPlannedEndTime().isAfter(item.getPlannedStartTime())) {
                totalPlannedMinutes += Duration.between(item.getPlannedStartTime(), item.getPlannedEndTime()).toMinutes();
            }
            if (item.getTardinessMinutes() != null && item.getTardinessMinutes() > 0) {
                lateCount++;
            }
            if (item.getPlannedStartTime() != null
                    && (earliestStartTime == null || item.getPlannedStartTime().isBefore(earliestStartTime))) {
                earliestStartTime = item.getPlannedStartTime();
            }
            if (item.getPlannedEndTime() != null
                    && (latestEndTime == null || item.getPlannedEndTime().isAfter(latestEndTime))) {
                latestEndTime = item.getPlannedEndTime();
            }
        }

        private Map<String, Object> toSummary() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("lineId", lineId);
            summary.put("manualItemCount", manualItemCount);
            summary.put("productionPlanCount", productionPlanCount);
            summary.put("lateCount", lateCount);
            summary.put("totalPlannedQuantity", totalPlannedQuantity);
            summary.put("totalPlannedMinutes", totalPlannedMinutes);
            summary.put("earliestStartTime", earliestStartTime);
            summary.put("latestEndTime", latestEndTime);
            return summary;
        }
    }

    private static final class LineAiLoad {
        private final Long lineId;
        private int scheduledOrderCount;
        private int lateOrderCount;
        private BigDecimal totalPlannedQuantity = BigDecimal.ZERO;
        private BigDecimal totalEstimatedMinutes = BigDecimal.ZERO;
        private LocalDateTime earliestStartTime;
        private LocalDateTime latestEndTime;

        private LineAiLoad(Long lineId) {
            this.lineId = lineId;
        }

        private void add(MfgScheduleItem item) {
            scheduledOrderCount++;
            if (item.getPlannedQuantity() != null) {
                totalPlannedQuantity = totalPlannedQuantity.add(item.getPlannedQuantity());
            }
            if (item.getEstimatedMinutes() != null) {
                totalEstimatedMinutes = totalEstimatedMinutes.add(item.getEstimatedMinutes());
            }
            if (item.getTardinessMinutes() != null && item.getTardinessMinutes() > 0) {
                lateOrderCount++;
            }
            if (item.getPlannedStartTime() != null
                    && (earliestStartTime == null || item.getPlannedStartTime().isBefore(earliestStartTime))) {
                earliestStartTime = item.getPlannedStartTime();
            }
            if (item.getPlannedEndTime() != null
                    && (latestEndTime == null || item.getPlannedEndTime().isAfter(latestEndTime))) {
                latestEndTime = item.getPlannedEndTime();
            }
        }

        private Map<String, Object> toSummary() {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("lineId", lineId);
            summary.put("scheduledOrderCount", scheduledOrderCount);
            summary.put("lateOrderCount", lateOrderCount);
            summary.put("totalPlannedQuantity", totalPlannedQuantity);
            summary.put("totalEstimatedMinutes", totalEstimatedMinutes);
            summary.put("earliestStartTime", earliestStartTime);
            summary.put("latestEndTime", latestEndTime);
            return summary;
        }
    }

    private record Allocation(LocalDateTime startTime, LocalDateTime endTime) {
    }

    private record ShiftSegment(LocalDateTime startTime, LocalDateTime endTime, long fullSeconds,
                                BigDecimal capacityFactor) {
    }

    private static final class LineScheduleState {
        private final List<CapacityWindow> windows;

        private LineScheduleState(List<CapacityWindow> windows) {
            this.windows = windows;
        }

        private boolean isEmpty() {
            return windows.isEmpty();
        }

        private LineScheduleState copy() {
            return new LineScheduleState(windows.stream().map(CapacityWindow::copy).toList());
        }

        private Allocation allocate(BigDecimal requiredBaseCapacity) {
            BigDecimal remaining = requiredBaseCapacity;
            LocalDateTime start = null;
            LocalDateTime end = null;
            for (CapacityWindow window : windows) {
                BigDecimal available = window.capacity().subtract(window.usedCapacity());
                if (available.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal consumed = remaining.min(available);
                if (start == null) {
                    start = window.timeAt(window.usedCapacity());
                }
                window.consume(consumed);
                end = window.timeAt(window.usedCapacity());
                remaining = remaining.subtract(consumed);
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    return new Allocation(start, end);
                }
            }
            return null;
        }
    }

    private static final class CapacityWindow {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final BigDecimal capacity;
        private BigDecimal usedCapacity;

        private CapacityWindow(LocalDateTime startTime, LocalDateTime endTime, BigDecimal capacity, BigDecimal usedCapacity) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.capacity = capacity;
            this.usedCapacity = usedCapacity;
        }

        private LocalDateTime startTime() {
            return startTime;
        }

        private LocalDateTime endTime() {
            return endTime;
        }

        private BigDecimal capacity() {
            return capacity;
        }

        private BigDecimal usedCapacity() {
            return usedCapacity;
        }

        private void consume(BigDecimal amount) {
            usedCapacity = usedCapacity.add(amount);
        }

        private LocalDateTime timeAt(BigDecimal used) {
            long durationSeconds = Duration.between(startTime, endTime).toSeconds();
            if (durationSeconds <= 0 || capacity.compareTo(BigDecimal.ZERO) <= 0) {
                return startTime;
            }
            long offsetSeconds = used.divide(capacity, CAPACITY_SCALE, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(durationSeconds))
                    .setScale(0, RoundingMode.CEILING)
                    .longValue();
            return startTime.plusSeconds(Math.min(Math.max(offsetSeconds, 0L), durationSeconds));
        }

        private CapacityWindow copy() {
            return new CapacityWindow(startTime, endTime, capacity, usedCapacity);
        }
    }
}
