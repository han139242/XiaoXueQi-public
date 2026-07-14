package com.neuedu.ems.service.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.mfg.MfgLineCalendarRule;

import java.util.List;

/**
 * 产线周历规则配置服务。
 */
public interface MfgLineCalendarRuleService extends IService<MfgLineCalendarRule> {

    Page<MfgLineCalendarRule> pageCalendarRules(Integer current, Integer size, Long lineId,
                                                 Integer dayOfWeek, Long shiftId, String status);

    MfgLineCalendarRule getCalendarRuleById(Long calendarRuleId);

    boolean saveCalendarRule(MfgLineCalendarRule calendarRule);

    boolean updateCalendarRule(MfgLineCalendarRule calendarRule);

    boolean disableCalendarRule(Long calendarRuleId);

    List<MfgLineCalendarRule> listCalendarRules(Long lineId, Integer dayOfWeek, Long shiftId, String status);
}
