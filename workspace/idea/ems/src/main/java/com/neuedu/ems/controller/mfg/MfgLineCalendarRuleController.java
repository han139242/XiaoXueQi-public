package com.neuedu.ems.controller.mfg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.mfg.MfgLineCalendarRule;
import com.neuedu.ems.service.mfg.MfgLineCalendarRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产线周历规则配置控制器。
 */
@RestController
@RequestMapping("/api/mfg/line-calendar-rule")
public class MfgLineCalendarRuleController {

    @Autowired
    private MfgLineCalendarRuleService mfgLineCalendarRuleService;

    @GetMapping("/page")
    public ResultData page(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long lineId,
                           @RequestParam(required = false) Integer dayOfWeek,
                           @RequestParam(required = false) Long shiftId,
                           @RequestParam(required = false) String status) {
        Page<MfgLineCalendarRule> result = mfgLineCalendarRuleService
                .pageCalendarRules(current, size, lineId, dayOfWeek, shiftId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    @GetMapping("/{calendarRuleId}")
    public ResultData detail(@PathVariable Long calendarRuleId) {
        return ResultData.success(ResultCode.SUCCESS,
                mfgLineCalendarRuleService.getCalendarRuleById(calendarRuleId));
    }

    @PostMapping
    public ResultData create(@RequestBody MfgLineCalendarRule calendarRule) {
        return mfgLineCalendarRuleService.saveCalendarRule(calendarRule)
                ? ResultData.success(ResultCode.SUCCESS)
                : ResultData.fail(ResultCode.FAILED);
    }

    @PutMapping
    public ResultData update(@RequestBody MfgLineCalendarRule calendarRule) {
        return mfgLineCalendarRuleService.updateCalendarRule(calendarRule)
                ? ResultData.success(ResultCode.SUCCESS)
                : ResultData.fail(ResultCode.FAILED);
    }

    @PutMapping("/{calendarRuleId}/disable")
    public ResultData disable(@PathVariable Long calendarRuleId) {
        return mfgLineCalendarRuleService.disableCalendarRule(calendarRuleId)
                ? ResultData.success(ResultCode.SUCCESS)
                : ResultData.fail(ResultCode.FAILED);
    }
}
