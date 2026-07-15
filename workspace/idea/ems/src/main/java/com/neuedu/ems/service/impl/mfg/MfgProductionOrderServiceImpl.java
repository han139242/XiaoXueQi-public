package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.mfg.MfgProductionOrder;
import com.neuedu.ems.common.BusinessStatus;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.sys.SysOperationLog;
import com.neuedu.ems.entity.sys.SysStatusChangeLog;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.mfg.MfgProductionOrderMapper;
import com.neuedu.ems.mapper.sys.SysOperationLogMapper;
import com.neuedu.ems.mapper.sys.SysStatusChangeLogMapper;
import com.neuedu.ems.service.mfg.MfgProductionOrderService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产订单管理 Service 实现类。
 */
@Service
public class MfgProductionOrderServiceImpl extends ServiceImpl<MfgProductionOrderMapper, MfgProductionOrder> implements MfgProductionOrderService {

    @Autowired
    private SysStatusChangeLogMapper sysStatusChangeLogMapper;

    @Autowired
    private SysOperationLogMapper sysOperationLogMapper;

    /**
     * 分页查询生产订单列表，支持关键字搜索、状态筛选、客户筛选和产品筛选。
     * @param current 当前页码
     * @param size 每页记录数
     * @param keyword 关键字（匹配订单编号）
     * @param status 订单状态
     * @param customerId 客户ID
     * @param productId 产品ID
     * @return 分页结果
     */
    @Override
    public Page<MfgProductionOrder> pageOrders(Integer current, Integer size, String keyword, String status, Long customerId, Long productId) {
        Page<MfgProductionOrder> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildOrderQuery(keyword, status, customerId, productId));
    }

    /**
     * 根据生产订单ID查询生产订单信息。
     * @param productionOrderId 生产订单ID
     * @return 生产订单实体
     */
    @Override
    public MfgProductionOrder getOrderById(Long productionOrderId) {
        QueryWrapper<MfgProductionOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("production_order_id", productionOrderId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 新增生产订单，自动设置删除标志为0。
     * @param order 生产订单实体
     * @return 是否成功
     */
    @Override
    public boolean saveOrder(MfgProductionOrder order) {
        if (order == null || !StringUtils.hasText(order.getProductionOrderNo()) || order.getProductId() == null
                || order.getOrderQuantity() == null || order.getOrderQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        order.setStatus(BusinessStatus.Document.DRAFT);
        if (order.getDeletedFlag() == null) {
            order.setDeletedFlag(0);
        }
        return baseMapper.insert(order) > 0;
    }

    /**
     * 更新生产订单信息，不允许修改删除标志。
     * @param order 生产订单实体
     * @return 是否成功
     */
    @Override
    public boolean updateOrder(MfgProductionOrder order) {
        if (order.getProductionOrderId() == null) {
            return false;
        }
        MfgProductionOrder existing = getOrderById(order.getProductionOrderId());
        if (existing == null || !BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            return false;
        }
        order.setDeletedFlag(null);
        order.setStatus(null);
        if (order.getVersion() == null) {
            order.setVersion(existing.getVersion());
        }
        return baseMapper.updateById(order) > 0;
    }

    /**
     * 根据ID删除生产订单（软删除，将删除标志设为1）。
     * @param productionOrderId 生产订单ID
     * @return 是否成功
     */
    @Override
    public boolean removeOrderById(Long productionOrderId) {
        MfgProductionOrder existing = getOrderById(productionOrderId);
        if (existing == null || !BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            return false;
        }
        MfgProductionOrder order = new MfgProductionOrder();
        order.setProductionOrderId(productionOrderId);
        order.setVersion(existing.getVersion());
        order.setDeletedFlag(1);
        return baseMapper.updateById(order) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfgProductionOrder releaseOrder(Long productionOrderId, Long operatorId, String remark) {
        MfgProductionOrder existing = getOrderById(productionOrderId);
        if (existing == null) {
            throw business(ResultCode.NOT_FOUND, "生产订单不存在或已删除");
        }
        if (!BusinessStatus.Document.DRAFT.equals(existing.getStatus())) {
            throw business(ResultCode.BUSINESS_ERROR, "只有 DRAFT 状态的生产订单可以下达");
        }
        if (operatorId == null) {
            throw business(ResultCode.PARAM_ERROR, "操作人不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        MfgProductionOrder update = new MfgProductionOrder();
        update.setProductionOrderId(productionOrderId);
        update.setStatus(BusinessStatus.Document.RELEASED);
        update.setVersion(existing.getVersion());
        update.setUpdatedBy(operatorId);
        update.setUpdatedTime(now);
        update.setRemark(StringUtils.hasText(remark) ? remark : existing.getRemark());
        if (baseMapper.updateById(update) == 0) {
            throw business(ResultCode.DATA_CONFLICT, "生产订单已被其他用户修改，请刷新后重试");
        }
        insertStatusLog(productionOrderId, existing.getStatus(), BusinessStatus.Document.RELEASED,
                "RELEASE_PRODUCTION_ORDER", operatorId, remark, now);
        insertOperationLog(productionOrderId, "RELEASE_PRODUCTION_ORDER", operatorId, remark, now);
        return getOrderById(productionOrderId);
    }

    /**
     * 查询生产订单列表，支持关键字搜索、状态筛选、客户筛选和产品筛选。
     * @param keyword 关键字（匹配订单编号）
     * @param status 订单状态
     * @param customerId 客户ID
     * @param productId 产品ID
     * @return 生产订单列表
     */
    @Override
    public List<MfgProductionOrder> listOrders(String keyword, String status, Long customerId, Long productId) {
        return baseMapper.selectList(buildOrderQuery(keyword, status, customerId, productId));
    }

    /**
     * 构建生产订单查询条件，过滤已删除记录。
     * @param keyword 关键字
     * @param status 状态
     * @param customerId 客户ID
     * @param productId 产品ID
     * @return 查询条件包装器
     */
    private QueryWrapper<MfgProductionOrder> buildOrderQuery(String keyword, String status, Long customerId, Long productId) {
        QueryWrapper<MfgProductionOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.like("production_order_no", keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        if (customerId != null) {
            wrapper.eq("customer_id", customerId);
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        wrapper.orderByDesc("production_order_id");
        return wrapper;
    }

    private void insertStatusLog(Long bizId, String oldStatus, String newStatus, String reason,
                                 Long operatorId, String remark, LocalDateTime now) {
        SysStatusChangeLog log = new SysStatusChangeLog();
        log.setBizType("MFG_PRODUCTION_ORDER");
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
        log.setModuleCode("mfg_production_order");
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
