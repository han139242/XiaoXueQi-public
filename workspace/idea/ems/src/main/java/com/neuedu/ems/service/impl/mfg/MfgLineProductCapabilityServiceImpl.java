package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.factory.FactoryLine;
import com.neuedu.ems.entity.mdm.MdmProduct;
import com.neuedu.ems.entity.mfg.MfgLineProductCapability;
import com.neuedu.ems.entity.process.ProcessRoute;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.exception.OptimisticLockConflictException;
import com.neuedu.ems.mapper.factory.FactoryLineMapper;
import com.neuedu.ems.mapper.mdm.MdmProductMapper;
import com.neuedu.ems.mapper.mfg.MfgLineProductCapabilityMapper;
import com.neuedu.ems.mapper.process.ProcessRouteMapper;
import com.neuedu.ems.service.mfg.MfgLineProductCapabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * 产线产品能力配置服务实现。
 */
@Service
public class MfgLineProductCapabilityServiceImpl
        extends ServiceImpl<MfgLineProductCapabilityMapper, MfgLineProductCapability>
        implements MfgLineProductCapabilityService {

    private static final String STATUS_ENABLE = "ENABLE";
    private static final String STATUS_DISABLE = "DISABLE";
    private static final String ROUTE_STATUS_RELEASED = "RELEASED";
    private static final BigDecimal MAX_CAPACITY_FACTOR = new BigDecimal("2");

    @Autowired
    private FactoryLineMapper factoryLineMapper;

    @Autowired
    private MdmProductMapper mdmProductMapper;

    @Autowired
    private ProcessRouteMapper processRouteMapper;

    @Override
    public Page<MfgLineProductCapability> pageCapabilities(Integer current, Integer size, Long lineId,
                                                            Long productId, Long routeId, String status) {
        Page<MfgLineProductCapability> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildCapabilityQuery(lineId, productId, routeId, status));
    }

    @Override
    public MfgLineProductCapability getCapabilityById(Long capabilityId) {
        if (capabilityId == null) {
            return null;
        }
        QueryWrapper<MfgLineProductCapability> wrapper = new QueryWrapper<>();
        wrapper.eq("capability_id", capabilityId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveCapability(MfgLineProductCapability capability) {
        requireCapability(capability);
        capability.setStatus(normalizeStatus(capability.getStatus()));
        validateCapability(capability, null);
        clearInsertManagedFields(capability);
        return baseMapper.insert(capability) > 0;
    }

    @Override
    public boolean updateCapability(MfgLineProductCapability capability) {
        requireCapability(capability);
        if (capability.getCapabilityId() == null) {
            throw invalid("产线产品能力主键不能为空");
        }
        MfgLineProductCapability existing = getCapabilityById(capability.getCapabilityId());
        if (existing == null) {
            throw notFound("产线产品能力不存在或已删除");
        }
        capability.setStatus(normalizeStatus(capability.getStatus()));
        validateCapability(capability, capability.getCapabilityId());
        if (capability.getVersion() == null) {
            capability.setVersion(existing.getVersion());
        }
        clearUpdateManagedFields(capability);
        if (baseMapper.updateById(capability) == 0) {
            throw new OptimisticLockConflictException();
        }
        return true;
    }

    @Override
    public boolean disableCapability(Long capabilityId) {
        MfgLineProductCapability existing = getCapabilityById(capabilityId);
        if (existing == null) {
            throw notFound("产线产品能力不存在或已删除");
        }
        if (STATUS_DISABLE.equals(existing.getStatus())) {
            return true;
        }

        MfgLineProductCapability update = new MfgLineProductCapability();
        update.setCapabilityId(existing.getCapabilityId());
        update.setStatus(STATUS_DISABLE);
        update.setVersion(existing.getVersion());
        if (baseMapper.updateById(update) == 0) {
            throw new OptimisticLockConflictException();
        }
        return true;
    }

    @Override
    public List<MfgLineProductCapability> listCapabilities(Long lineId, Long productId, Long routeId, String status) {
        return baseMapper.selectList(buildCapabilityQuery(lineId, productId, routeId, status));
    }

    private void validateCapability(MfgLineProductCapability capability, Long excludedCapabilityId) {
        if (capability.getPriorityLevel() == null || capability.getPriorityLevel() <= 0) {
            throw invalid("适配优先级必须为正整数");
        }
        validateCapacityFactor(capability.getCapacityFactor());

        requireActiveLine(capability.getLineId());
        requireActiveProduct(capability.getProductId());
        ProcessRoute route = requireActiveRoute(capability.getRouteId());
        if (!capability.getProductId().equals(route.getProductId())) {
            throw invalid("工艺路线不属于所选产品");
        }
        if (!ROUTE_STATUS_RELEASED.equals(route.getStatus())) {
            throw invalid("工艺路线必须处于 RELEASED 状态");
        }
        ensureNoDuplicate(capability.getLineId(), capability.getProductId(), capability.getRouteId(), excludedCapabilityId);
    }

    private void validateCapacityFactor(BigDecimal capacityFactor) {
        if (capacityFactor == null
                || capacityFactor.compareTo(BigDecimal.ZERO) <= 0
                || capacityFactor.compareTo(MAX_CAPACITY_FACTOR) > 0) {
            throw invalid("产能系数必须大于 0 且不超过 2");
        }
    }

    private void requireActiveLine(Long lineId) {
        if (lineId == null) {
            throw invalid("产线不能为空");
        }
        QueryWrapper<FactoryLine> wrapper = new QueryWrapper<>();
        wrapper.eq("line_id", lineId);
        wrapper.eq("deleted_flag", 0);
        if (factoryLineMapper.selectOne(wrapper) == null) {
            throw invalid("产线不存在或已删除");
        }
    }

    private void requireActiveProduct(Long productId) {
        if (productId == null) {
            throw invalid("产品不能为空");
        }
        QueryWrapper<MdmProduct> wrapper = new QueryWrapper<>();
        wrapper.eq("product_id", productId);
        wrapper.eq("deleted_flag", 0);
        if (mdmProductMapper.selectOne(wrapper) == null) {
            throw invalid("产品不存在或已删除");
        }
    }

    private ProcessRoute requireActiveRoute(Long routeId) {
        if (routeId == null) {
            throw invalid("工艺路线不能为空");
        }
        QueryWrapper<ProcessRoute> wrapper = new QueryWrapper<>();
        wrapper.eq("route_id", routeId);
        wrapper.eq("deleted_flag", 0);
        ProcessRoute route = processRouteMapper.selectOne(wrapper);
        if (route == null) {
            throw invalid("工艺路线不存在或已删除");
        }
        return route;
    }

    private void ensureNoDuplicate(Long lineId, Long productId, Long routeId, Long excludedCapabilityId) {
        QueryWrapper<MfgLineProductCapability> wrapper = new QueryWrapper<>();
        wrapper.eq("line_id", lineId);
        wrapper.eq("product_id", productId);
        wrapper.eq("route_id", routeId);
        wrapper.eq("deleted_flag", 0);
        if (excludedCapabilityId != null) {
            wrapper.ne("capability_id", excludedCapabilityId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw invalid("同一产线、产品和工艺路线只能配置一次");
        }
    }

    private QueryWrapper<MfgLineProductCapability> buildCapabilityQuery(Long lineId, Long productId, Long routeId,
                                                                          String status) {
        QueryWrapper<MfgLineProductCapability> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (lineId != null) {
            wrapper.eq("line_id", lineId);
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        if (routeId != null) {
            wrapper.eq("route_id", routeId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", normalizeStatus(status));
        }
        wrapper.orderByAsc("priority_level");
        wrapper.orderByDesc("capability_id");
        return wrapper;
    }

    private String normalizeStatus(String status) {
        String normalized = StringUtils.hasText(status)
                ? status.trim().toUpperCase(Locale.ROOT)
                : STATUS_ENABLE;
        if (!STATUS_ENABLE.equals(normalized) && !STATUS_DISABLE.equals(normalized)) {
            throw invalid("状态只能为 ENABLE 或 DISABLE");
        }
        return normalized;
    }

    private void clearInsertManagedFields(MfgLineProductCapability capability) {
        capability.setCapabilityId(null);
        capability.setVersion(null);
        capability.setDeletedFlag(0);
        capability.setCreatedBy(null);
        capability.setCreatedTime(null);
        capability.setUpdatedBy(null);
        capability.setUpdatedTime(null);
    }

    private void clearUpdateManagedFields(MfgLineProductCapability capability) {
        capability.setDeletedFlag(null);
        capability.setCreatedBy(null);
        capability.setCreatedTime(null);
        capability.setUpdatedBy(null);
        capability.setUpdatedTime(null);
    }

    private void requireCapability(MfgLineProductCapability capability) {
        if (capability == null) {
            throw invalid("产线产品能力不能为空");
        }
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
    }

    private BusinessException notFound(String message) {
        return new BusinessException(ResultCode.NOT_FOUND.getCode(), message);
    }
}
