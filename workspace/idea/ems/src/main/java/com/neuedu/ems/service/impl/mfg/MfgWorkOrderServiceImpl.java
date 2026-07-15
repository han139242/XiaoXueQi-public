package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgWorkOrder;
import com.neuedu.ems.entity.mfg.MfgWorkOrderMaterial;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.entity.mfg.MfgProductionPlan;
import com.neuedu.ems.entity.mfg.MfgLineProductCapability;
import com.neuedu.ems.entity.mdm.MdmBom;
import com.neuedu.ems.entity.mdm.MdmBomItem;
import com.neuedu.ems.entity.process.ProcessRoute;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.entity.wms.WmsInventory;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgWorkOrderMaterialMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.mfg.MfgProductionPlanMapper;
import com.neuedu.ems.mapper.mfg.MfgLineProductCapabilityMapper;
import com.neuedu.ems.mapper.mdm.MdmBomMapper;
import com.neuedu.ems.mapper.mdm.MdmBomItemMapper;
import com.neuedu.ems.mapper.process.ProcessRouteMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.mapper.wms.WmsInventoryMapper;
import com.neuedu.ems.service.mfg.MfgWorkOrderService;
import com.neuedu.ems.service.mfg.MfgProductionOrderService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 生产工单管理 Service 实现类。
 */
@Service
public class MfgWorkOrderServiceImpl extends ServiceImpl<MfgWorkOrderMapper, MfgWorkOrder> implements MfgWorkOrderService {

    @Autowired
    private MfgProductionOrderService productionOrderService;

    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Autowired
    private MfgProductionPlanMapper productionPlanMapper;

    @Autowired
    private MfgProductionOrderMapper productionOrderMapper;

    @Autowired
    private MdmBomMapper bomMapper;

    @Autowired
    private MdmBomItemMapper bomItemMapper;

    @Autowired
    private MfgWorkOrderMaterialMapper workOrderMaterialMapper;

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Autowired
    private MfgLineProductCapabilityMapper lineProductCapabilityMapper;

    @Autowired
    private ProcessRouteMapper routeMapper;

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 分页查询生产工单列表，支持关键字搜索、状态筛选、生产计划筛选和产品筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配工单编号）
     * @param status 工单状态
     * @param planId 生产计划ID
     * @param productId 产品ID
     * @return 分页结果
     */
    @Override
    public Page<MfgWorkOrder> pageWorkOrders(Integer current, Integer size, String keyword, String status, Long planId, Long productId) {
        Page<MfgWorkOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildWorkOrderQuery(keyword, status, planId, productId));
    }

    /**
     * 根据生产工单ID查询生产工单信息。
     * @param workOrderId 生产工单ID
     * @return 生产工单实体
     */
    @Override
    public MfgWorkOrder getWorkOrderById(Long workOrderId) {
        QueryWrapper<MfgWorkOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("work_order_id", workOrderId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增生产工单，自动设置删除标志为0。
     * @param workOrder 生产工单实体
     * @return 是否成功
     */
    @Override
    public boolean saveWorkOrder(MfgWorkOrder workOrder) {
        throw business(ResultCode.BUSINESS_ERROR, "生产工单必须从已生效生产计划生成，不能脱离来源手工新增");
    }

    /**
     * 更新生产工单信息，不允许修改删除标志。
     * @param workOrder 生产工单实体
     * @return 是否成功
     */
    @Override
    public boolean updateWorkOrder(MfgWorkOrder workOrder) {
        if (workOrder.getWorkOrderId() == null) {
            return false;
        }
        MfgWorkOrder existing = getWorkOrderById(workOrder.getWorkOrderId());
        if (existing == null || !BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            return false;
        }
        workOrder.setDeletedFlag(null);
        workOrder.setStatus(null);
        workOrder.setCompletedQuantity(null);
        workOrder.setPlanId(null);
        workOrder.setProductId(null);
        workOrder.setBomId(null);
        workOrder.setRouteId(null);
        workOrder.setPlannedQuantity(null);
        if (workOrder.getVersion() == null) {
            workOrder.setVersion(existing.getVersion());
        }
        return baseMapper.updateById(workOrder) > 0;
    }

    /**
     * 根据ID删除生产工单（软删除，将删除标志设为1）。
     * @param workOrderId 生产工单ID
     * @return 是否成功
     */
    @Override
    public boolean removeWorkOrderById(Long workOrderId) {
        MfgWorkOrder existing = getWorkOrderById(workOrderId);
        if (existing == null || !BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            return false;
        }
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderId(workOrderId);
        workOrder.setVersion(existing.getVersion());
        workOrder.setDeletedFlag(1);
        return baseMapper.updateById(workOrder) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgWorkOrder generateWorkOrder(Long planId, MfgWorkOrder request, Long operatorId) {
        if (planId == null || operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "生产计划和操作人不能为空");
        }
        MfgWorkOrder command = request == null ? new MfgWorkOrder() : request;
        MfgProductionPlan plan = activePlan(planId);
        if (plan == null) {
            throw business(ResultCode.NOT_FOUND, "生产计划不存在或已删除");
        }
        if (!BusinessStatus.Document.RELEASED.equals(plan.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有 RELEASED 状态的生产计划可以生成工单");
        }
        if (hasActiveWorkOrder(planId)) {
            throw business(ResultCode.BUSINESS_ERROR, "该生产计划已生成工单，不能重复生成");
        }
        MfgProductionOrder order = activeOrder(plan.getProductionOrderId());
        if (order == null || order.getProductId() == null) {
            throw business(ResultCode.BUSINESS_ERROR, "生产计划未关联有效产品订单");
        }
        if (plan.getLineId() == null || plan.getPlannedQuantity() == null
                || plan.getPlannedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw business(ResultCode.BUSINESS_ERROR, "生产计划缺少有效产线或计划数量");
        }
        MdmBom bom = resolveReleasedBom(order.getProductId(), plan);
        ProcessRoute route = resolveReleasedRoute(order.getProductId(), plan.getLineId());
        List<MfgWorkOrderMaterial> materialRequirements = buildMaterialRequirements(
                bom, plan.getPlannedQuantity());
        LocalDateTime now = LocalDateTime.now();
        MfgWorkOrder workOrder = new MfgWorkOrder();
        workOrder.setWorkOrderNo("WO-" + planId + "-" + now.format(NUMBER_TIME));
        workOrder.setPlanId(planId);
        workOrder.setProductId(order.getProductId());
        workOrder.setBomId(bom.getBomId());
        workOrder.setRouteId(route.getRouteId());
        workOrder.setPlannedQuantity(plan.getPlannedQuantity());
        workOrder.setCompletedQuantity(BigDecimal.ZERO);
        workOrder.setStatus(BusinessStatus.Document.DRAFT);
        workOrder.setRemark(command.getRemark());
        workOrder.setVersion(0);
        workOrder.setCreatedBy(operatorId);
        workOrder.setCreatedTime(now);
        workOrder.setUpdatedBy(operatorId);
        workOrder.setUpdatedTime(now);
        workOrder.setDeletedFlag(0);
        if (baseMapper.insert(workOrder) == 0 || workOrder.getWorkOrderId() == null) {
            throw new BusinessException(ResultCode.FAILED);
        }
        saveMaterialRequirements(workOrder, bom, materialRequirements, operatorId, now);
        insertStatusLog(workOrder.getWorkOrderId(), null, BusinessStatus.Document.DRAFT,
                "GENERATE_WORK_ORDER", operatorId, command.getRemark(), now);
        insertOperationLog(workOrder.getWorkOrderId(), "GENERATE_WORK_ORDER", operatorId, command.getRemark(), now);
        return workOrder;
    }

    private List<MfgWorkOrderMaterial> buildMaterialRequirements(MdmBom bom, BigDecimal plannedQuantity) {
        List<MdmBomItem> items = bomItemMapper.selectList(new LambdaQueryWrapper<MdmBomItem>()
                .eq(MdmBomItem::getBomId, bom.getBomId())
                .eq(MdmBomItem::getDeletedFlag, 0)
                .orderByAsc(MdmBomItem::getSequenceNo, MdmBomItem::getBomItemId));
        if (items == null || items.isEmpty()) {
            throw business(ResultCode.BUSINESS_ERROR, "已发布物料清单没有有效明细，不能生成生产工单");
        }

        Map<Long, BigDecimal> requiredByMaterial = new LinkedHashMap<>();
        for (MdmBomItem item : items) {
            if (item.getMaterialId() == null || item.getQuantity() == null
                    || item.getQuantity().compareTo(BigDecimal.ZERO) <= 0
                    || (item.getLossRate() != null && item.getLossRate().compareTo(BigDecimal.ZERO) < 0)) {
                throw business(ResultCode.BUSINESS_ERROR, "物料清单存在无效物料、用量或损耗率，不能生成生产工单");
            }
            BigDecimal lossRate = item.getLossRate() == null ? BigDecimal.ZERO : item.getLossRate();
            BigDecimal required = plannedQuantity
                    .multiply(item.getQuantity())
                    .multiply(BigDecimal.ONE.add(lossRate))
                    .setScale(4, RoundingMode.HALF_UP);
            requiredByMaterial.merge(item.getMaterialId(), required, BigDecimal::add);
        }

        List<WmsInventory> inventories = inventoryMapper.selectList(new LambdaQueryWrapper<WmsInventory>()
                .in(WmsInventory::getMaterialId, requiredByMaterial.keySet())
                .eq(WmsInventory::getDeletedFlag, 0));
        Map<Long, BigDecimal> availableByMaterial = new LinkedHashMap<>();
        if (inventories != null) {
            for (WmsInventory inventory : inventories) {
                if (inventory.getMaterialId() != null && inventory.getAvailableQuantity() != null) {
                    availableByMaterial.merge(inventory.getMaterialId(), inventory.getAvailableQuantity(), BigDecimal::add);
                }
            }
        }

        List<MfgWorkOrderMaterial> requirements = new java.util.ArrayList<>(requiredByMaterial.size());
        for (Map.Entry<Long, BigDecimal> entry : requiredByMaterial.entrySet()) {
            BigDecimal required = entry.getValue().setScale(4, RoundingMode.HALF_UP);
            BigDecimal available = availableByMaterial.getOrDefault(entry.getKey(), BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);
            BigDecimal shortage = required.subtract(available).max(BigDecimal.ZERO)
                    .setScale(4, RoundingMode.HALF_UP);
            String readiness = shortage.signum() == 0
                    ? "READY" : available.signum() > 0 ? "PARTIAL_READY" : "WAIT_READY";

            MfgWorkOrderMaterial requirement = new MfgWorkOrderMaterial();
            requirement.setMaterialId(entry.getKey());
            requirement.setRequiredQuantity(required);
            requirement.setIssuedQuantity(BigDecimal.ZERO.setScale(4));
            requirement.setShortageQuantity(shortage);
            requirement.setStatus(readiness);
            requirements.add(requirement);
        }
        return requirements;
    }

    private void saveMaterialRequirements(MfgWorkOrder workOrder, MdmBom bom,
                                          List<MfgWorkOrderMaterial> requirements,
                                          Long operatorId, LocalDateTime now) {
        for (MfgWorkOrderMaterial requirement : requirements) {
            requirement.setWorkOrderId(workOrder.getWorkOrderId());
            requirement.setRemark("由物料清单 " + bom.getBomId() + " 自动生成");
            requirement.setVersion(0);
            requirement.setCreatedBy(operatorId);
            requirement.setCreatedTime(now);
            requirement.setUpdatedBy(operatorId);
            requirement.setUpdatedTime(now);
            requirement.setDeletedFlag(0);
            if (workOrderMaterialMapper.insert(requirement) == 0) {
                throw business(ResultCode.FAILED, "工单用料需求生成失败");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgWorkOrder releaseWorkOrder(Long workOrderId, Long operatorId, String remark) {
        MfgWorkOrder existing = getWorkOrderById(workOrderId);
        if (existing == null) {
            throw business(ResultCode.NOT_FOUND, "生产工单不存在或已删除");
        }
        if (!List.of(BusinessStatus.Document.DRAFT, BusinessStatus.Document.APPROVED).contains(existing.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有 DRAFT 或 APPROVED 状态的生产工单可以下达");
        }
        if (operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "操作人不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        MfgWorkOrder update = new MfgWorkOrder();
        update.setWorkOrderId(workOrderId);
        update.setStatus(BusinessStatus.Document.RELEASED);
        update.setVersion(existing.getVersion());
        update.setRemark(StringUtils.hasText(remark) ? remark : existing.getRemark());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产工单已被其他用户修改，请刷新后重试");
        }
        insertStatusLog(workOrderId, existing.getStatus(), BusinessStatus.Document.RELEASED,
                "RELEASE_WORK_ORDER", operatorId, remark, now);
        insertOperationLog(workOrderId, "RELEASE_WORK_ORDER", operatorId, remark, now);
        return getWorkOrderById(workOrderId);
    }

    @Override
    public void withdrawWorkOrder(Long workOrderId, Long operatorId, String remark) {
        productionOrderService.withdrawUnexecutedWorkOrderChain(workOrderId, operatorId, remark);
    }

    /**
     * 查询生产工单列表，支持关键字搜索、状态筛选、生产计划筛选和产品筛选。
     * @param keyword 关键字（匹配工单编号）
     * @param status 工单状态
     * @param planId 生产计划ID
     * @param productId 产品ID
     * @return 生产工单列表
     */
    @Override
    public List<MfgWorkOrder> listWorkOrders(String keyword, String status, Long planId, Long productId) {
        return baseMapper.selectList(buildWorkOrderQuery(keyword, status, planId, productId));
    }

    /**
     * 构建生产工单查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @param planId 生产计划ID
     * @param productId 产品ID
     * @return 查询条件包装器
     */
    private QueryWrapper<MfgWorkOrder> buildWorkOrderQuery(String keyword, String status, Long planId, Long productId) {
        QueryWrapper<MfgWorkOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.like("work_order_no", keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (planId != null) {
            wrapper.eq("plan_id", planId);
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        wrapper.orderByDesc("work_order_id");
        return wrapper;
    }

    private MfgProductionPlan activePlan(Long planId) {
        QueryWrapper<MfgProductionPlan> query = new QueryWrapper<>();
        query.eq("plan_id", planId)
                .eq("deleted_flag", 0)
                .last("FOR UPDATE");
        return productionPlanMapper.selectOne(query);
    }

    private MfgProductionOrder activeOrder(Long productionOrderId) {
        QueryWrapper<MfgProductionOrder> query = new QueryWrapper<>();
        query.eq("production_order_id", productionOrderId).eq("deleted_flag", 0);
        return productionOrderMapper.selectOne(query);
    }

    private boolean hasActiveWorkOrder(Long planId) {
        QueryWrapper<MfgWorkOrder> query = new QueryWrapper<>();
        query.eq("plan_id", planId).eq("deleted_flag", 0).ne("status", BusinessStatus.Document.CLOSED);
        return baseMapper.selectCount(query) > 0;
    }

    private MdmBom resolveReleasedBom(Long productId, MfgProductionPlan plan) {
        LambdaQueryWrapper<MdmBom> query = new LambdaQueryWrapper<MdmBom>()
                .eq(MdmBom::getProductId, productId)
                .eq(MdmBom::getStatus, BusinessStatus.Document.RELEASED)
                .eq(MdmBom::getDeletedFlag, 0)
                .isNotNull(MdmBom::getEffectiveDate)
                .orderByDesc(MdmBom::getEffectiveDate)
                .orderByDesc(MdmBom::getBomId);
        if (plan.getPlannedStartTime() != null) {
            query.le(MdmBom::getEffectiveDate, plan.getPlannedStartTime().toLocalDate());
        }
        List<MdmBom> candidates = bomMapper.selectList(query);
        if (candidates.isEmpty()) {
            throw business(ResultCode.BUSINESS_ERROR, "订单产品在计划日期没有已发布的有效物料清单");
        }
        if (candidates.size() > 1
                && Objects.equals(candidates.get(0).getEffectiveDate(), candidates.get(1).getEffectiveDate())) {
            throw business(ResultCode.BUSINESS_ERROR,
                    "订单产品存在多个同日生效的已发布物料清单，请先清理版本有效性");
        }
        return candidates.get(0);
    }

    private ProcessRoute resolveReleasedRoute(Long productId, Long lineId) {
        List<MfgLineProductCapability> capabilities = lineProductCapabilityMapper.selectList(
                new LambdaQueryWrapper<MfgLineProductCapability>()
                        .eq(MfgLineProductCapability::getLineId, lineId)
                        .eq(MfgLineProductCapability::getProductId, productId)
                        .eq(MfgLineProductCapability::getStatus, BusinessStatus.Common.ENABLE)
                        .eq(MfgLineProductCapability::getDeletedFlag, 0)
                        .orderByAsc(MfgLineProductCapability::getPriorityLevel)
                        .orderByAsc(MfgLineProductCapability::getCapabilityId));
        ProcessRoute selectedRoute = null;
        Integer selectedPriority = null;
        for (MfgLineProductCapability capability : capabilities) {
            ProcessRoute route = routeMapper.selectOne(new LambdaQueryWrapper<ProcessRoute>()
                    .eq(ProcessRoute::getRouteId, capability.getRouteId())
                    .eq(ProcessRoute::getProductId, productId)
                    .eq(ProcessRoute::getStatus, BusinessStatus.Document.RELEASED)
                    .eq(ProcessRoute::getDeletedFlag, 0));
            if (route != null) {
                if (selectedRoute == null) {
                    selectedRoute = route;
                    selectedPriority = capability.getPriorityLevel();
                } else if (Objects.equals(selectedPriority, capability.getPriorityLevel())
                        && !Objects.equals(selectedRoute.getRouteId(), route.getRouteId())) {
                    throw business(ResultCode.BUSINESS_ERROR,
                            "计划产线存在多个同优先级产品路线，请先明确产线产品能力优先级");
                } else {
                    break;
                }
            }
        }
        if (selectedRoute != null) {
            return selectedRoute;
        }
        List<ProcessRoute> releasedRoutes = routeMapper.selectList(new LambdaQueryWrapper<ProcessRoute>()
                .eq(ProcessRoute::getProductId, productId)
                .eq(ProcessRoute::getStatus, BusinessStatus.Document.RELEASED)
                .eq(ProcessRoute::getDeletedFlag, 0)
                .orderByAsc(ProcessRoute::getRouteId));
        if (releasedRoutes.size() == 1) {
            return releasedRoutes.get(0);
        }
        if (releasedRoutes.isEmpty()) {
            throw business(ResultCode.BUSINESS_ERROR, "订单产品没有已发布的工艺路线");
        }
        throw business(ResultCode.BUSINESS_ERROR,
                "计划产线未配置产品路线且产品存在多个已发布路线，请先维护产线产品能力");
    }

    private void insertStatusLog(Long bizId, String oldStatus, String newStatus, String reason,
                                 Long operatorId, String remark, LocalDateTime now) {
        SysStatusChangeLog log = new SysStatusChangeLog();
        log.setBizType("MFG_WORK_ORDER");
        log.setBizId(bizId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setChangeReason(reason);
        log.setChangedBy(operatorId);
        log.setChangedTime(now);
        log.setRemark(remark);
        log.setCreatedBy(operatorId);
        log.setCreatedTime(now);
        log.setUpdatedBy(operatorId);
        log.setUpdatedTime(now);
        log.setDeletedFlag(0);
        if (sysStatusChangeLogMapper.insert(log) == 0) throw new BusinessException(ResultCode.FAILED);
    }

    private void insertOperationLog(Long bizId, String type, Long operatorId, String remark, LocalDateTime now) {
        SysOperationLog log = new SysOperationLog();
        log.setUserId(operatorId);
        log.setOperationType(type);
        log.setModuleCode("mfg_work_order");
        log.setBizId(bizId);
        log.setOperationTime(now);
        log.setRemark(remark);
        log.setCreatedBy(operatorId);
        log.setCreatedTime(now);
        log.setUpdatedBy(operatorId);
        log.setUpdatedTime(now);
        log.setDeletedFlag(0);
        if (sysOperationLogMapper.insert(log) == 0) throw new BusinessException(ResultCode.FAILED);
    }

    private BusinessException business(ResultCode code, String message) {
        return new BusinessException(code.getCode(), message);
    }
}
