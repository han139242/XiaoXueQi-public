package com.neuedu.ems.service.impl.mfg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.entity.factory.FactoryLine;
import com.neuedu.ems.entity.mfg.MfgLineCalendarRule;
import com.neuedu.ems.entity.org.OrgShift;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.exception.OptimisticLockConflictException;
import com.neuedu.ems.mapper.factory.FactoryLineMapper;
import com.neuedu.ems.mapper.mfg.MfgLineCalendarRuleMapper;
import com.neuedu.ems.mapper.org.OrgShiftMapper;
import com.neuedu.ems.service.mfg.MfgLineCalendarRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * 产线周历规则配置服务实现。
 */
@Service
public class MfgLineCalendarRuleServiceImpl
        extends ServiceImpl<MfgLineCalendarRuleMapper, MfgLineCalendarRule>
        implements MfgLineCalendarRuleService {

    private static final String STATUS_ENABLE = "ENABLE";
    private static final String STATUS_DISABLE = "DISABLE";
    private static final BigDecimal MAX_CAPACITY_FACTOR = new BigDecimal("2");

    @Autowired
    private FactoryLineMapper factoryLineMapper;

    @Autowired
    private OrgShiftMapper orgShiftMapper;

    @Override
    public Page<MfgLineCalendarRule> pageCalendarRules(Integer current, Integer size, Long lineId,
                                                        Integer dayOfWeek, Long shiftId, String status) {
        Page<MfgLineCalendarRule> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildCalendarRuleQuery(lineId, dayOfWeek, shiftId, status));
    }

    @Override
    public MfgLineCalendarRule getCalendarRuleById(Long calendarRuleId) {
        if (calendarRuleId == null) {
            return null;
        }
        QueryWrapper<MfgLineCalendarRule> wrapper = new QueryWrapper<>();
        wrapper.eq("calendar_rule_id", calendarRuleId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveCalendarRule(MfgLineCalendarRule calendarRule) {
        requireCalendarRule(calendarRule);
        calendarRule.setStatus(normalizeStatus(calendarRule.getStatus()));
        validateCalendarRule(calendarRule, null);
        clearInsertManagedFields(calendarRule);
        return baseMapper.insert(calendarRule) > 0;
    }

    @Override
    public boolean updateCalendarRule(MfgLineCalendarRule calendarRule) {
        requireCalendarRule(calendarRule);
        if (calendarRule.getCalendarRuleId() == null) {
            throw invalid("产线周历规则主键不能为空");
        }
        MfgLineCalendarRule existing = getCalendarRuleById(calendarRule.getCalendarRuleId());
        if (existing == null) {
            throw notFound("产线周历规则不存在或已删除");
        }
        calendarRule.setStatus(normalizeStatus(calendarRule.getStatus()));
        validateCalendarRule(calendarRule, calendarRule.getCalendarRuleId());
        if (calendarRule.getVersion() == null) {
            calendarRule.setVersion(existing.getVersion());
        }
        clearUpdateManagedFields(calendarRule);
        if (baseMapper.updateById(calendarRule) == 0) {
            throw new OptimisticLockConflictException();
        }
        return true;
    }

    @Override
    public boolean disableCalendarRule(Long calendarRuleId) {
        MfgLineCalendarRule existing = getCalendarRuleById(calendarRuleId);
        if (existing == null) {
            throw notFound("产线周历规则不存在或已删除");
        }
        if (STATUS_DISABLE.equals(existing.getStatus())) {
            return true;
        }

        MfgLineCalendarRule update = new MfgLineCalendarRule();
        update.setCalendarRuleId(existing.getCalendarRuleId());
        update.setStatus(STATUS_DISABLE);
        update.setVersion(existing.getVersion());
        if (baseMapper.updateById(update) == 0) {
            throw new OptimisticLockConflictException();
        }
        return true;
    }

    @Override
    public List<MfgLineCalendarRule> listCalendarRules(Long lineId, Integer dayOfWeek, Long shiftId, String status) {
        return baseMapper.selectList(buildCalendarRuleQuery(lineId, dayOfWeek, shiftId, status));
    }

    private void validateCalendarRule(MfgLineCalendarRule calendarRule, Long excludedCalendarRuleId) {
        if (calendarRule.getDayOfWeek() == null
                || calendarRule.getDayOfWeek() < 1
                || calendarRule.getDayOfWeek() > 7) {
            throw invalid("星期必须为 1 至 7");
        }
        if (calendarRule.getEffectiveFrom() == null) {
            throw invalid("生效日期不能为空");
        }
        if (calendarRule.getEffectiveTo() != null
                && calendarRule.getEffectiveTo().isBefore(calendarRule.getEffectiveFrom())) {
            throw invalid("失效日期不能早于生效日期");
        }
        validateCapacityFactor(calendarRule.getCapacityFactor());

        requireActiveLine(calendarRule.getLineId());
        requireActiveShift(calendarRule.getShiftId());
        ensureNoDuplicate(calendarRule.getLineId(), calendarRule.getDayOfWeek(), calendarRule.getShiftId(),
                calendarRule.getEffectiveFrom(), excludedCalendarRuleId);
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

    private void requireActiveShift(Long shiftId) {
        if (shiftId == null) {
            throw invalid("班次不能为空");
        }
        QueryWrapper<OrgShift> wrapper = new QueryWrapper<>();
        wrapper.eq("shift_id", shiftId);
        wrapper.eq("deleted_flag", 0);
        if (orgShiftMapper.selectOne(wrapper) == null) {
            throw invalid("班次不存在或已删除");
        }
    }

    private void ensureNoDuplicate(Long lineId, Integer dayOfWeek, Long shiftId,
                                   LocalDate effectiveFrom, Long excludedCalendarRuleId) {
        QueryWrapper<MfgLineCalendarRule> wrapper = new QueryWrapper<>();
        wrapper.eq("line_id", lineId);
        wrapper.eq("day_of_week", dayOfWeek);
        wrapper.eq("shift_id", shiftId);
        wrapper.eq("effective_from", effectiveFrom);
        wrapper.eq("deleted_flag", 0);
        if (excludedCalendarRuleId != null) {
            wrapper.ne("calendar_rule_id", excludedCalendarRuleId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw invalid("同一产线、星期、班次和生效日期只能配置一次");
        }
    }

    private QueryWrapper<MfgLineCalendarRule> buildCalendarRuleQuery(Long lineId, Integer dayOfWeek,
                                                                       Long shiftId, String status) {
        QueryWrapper<MfgLineCalendarRule> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (lineId != null) {
            wrapper.eq("line_id", lineId);
        }
        if (dayOfWeek != null) {
            wrapper.eq("day_of_week", dayOfWeek);
        }
        if (shiftId != null) {
            wrapper.eq("shift_id", shiftId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", normalizeStatus(status));
        }
        wrapper.orderByAsc("line_id");
        wrapper.orderByAsc("day_of_week");
        wrapper.orderByAsc("effective_from");
        wrapper.orderByAsc("calendar_rule_id");
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

    private void clearInsertManagedFields(MfgLineCalendarRule calendarRule) {
        calendarRule.setCalendarRuleId(null);
        calendarRule.setVersion(null);
        calendarRule.setDeletedFlag(0);
        calendarRule.setCreatedBy(null);
        calendarRule.setCreatedTime(null);
        calendarRule.setUpdatedBy(null);
        calendarRule.setUpdatedTime(null);
    }

    private void clearUpdateManagedFields(MfgLineCalendarRule calendarRule) {
        calendarRule.setDeletedFlag(null);
        calendarRule.setCreatedBy(null);
        calendarRule.setCreatedTime(null);
        calendarRule.setUpdatedBy(null);
        calendarRule.setUpdatedTime(null);
    }

    private void requireCalendarRule(MfgLineCalendarRule calendarRule) {
        if (calendarRule == null) {
            throw invalid("产线周历规则不能为空");
        }
    }

    private BusinessException invalid(String message) {
        return new BusinessException(ResultCode.PARAM_ERROR.getCode(), message);
    }

    private BusinessException notFound(String message) {
        return new BusinessException(ResultCode.NOT_FOUND.getCode(), message);
    }
}
