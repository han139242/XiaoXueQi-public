package com.neuedu.ems.service.impl.mfg;

import com.neuedu.ems.entity.factory.FactoryLine;
import com.neuedu.ems.entity.mfg.MfgLineCalendarRule;
import com.neuedu.ems.entity.org.OrgShift;
import com.neuedu.ems.exception.BusinessException;
import com.neuedu.ems.mapper.factory.FactoryLineMapper;
import com.neuedu.ems.mapper.mfg.MfgLineCalendarRuleMapper;
import com.neuedu.ems.mapper.org.OrgShiftMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MfgLineCalendarRuleServiceImplTest {

    private MfgLineCalendarRuleServiceImpl service;
    private MfgLineCalendarRuleMapper calendarRuleMapper;
    private FactoryLineMapper lineMapper;
    private OrgShiftMapper shiftMapper;

    @BeforeEach
    void setUp() {
        service = new MfgLineCalendarRuleServiceImpl();
        calendarRuleMapper = mock(MfgLineCalendarRuleMapper.class);
        lineMapper = mock(FactoryLineMapper.class);
        shiftMapper = mock(OrgShiftMapper.class);

        ReflectionTestUtils.setField(service, "baseMapper", calendarRuleMapper);
        ReflectionTestUtils.setField(service, "factoryLineMapper", lineMapper);
        ReflectionTestUtils.setField(service, "orgShiftMapper", shiftMapper);
    }

    @Test
    void saveAcceptsValidCalendarRule() {
        MfgLineCalendarRule calendarRule = calendarRule();
        when(lineMapper.selectOne(any())).thenReturn(new FactoryLine());
        when(shiftMapper.selectOne(any())).thenReturn(new OrgShift());
        when(calendarRuleMapper.selectCount(any())).thenReturn(0L);
        when(calendarRuleMapper.insert(any(MfgLineCalendarRule.class))).thenReturn(1);

        assertTrue(service.saveCalendarRule(calendarRule));

        ArgumentCaptor<MfgLineCalendarRule> insert = ArgumentCaptor.forClass(MfgLineCalendarRule.class);
        verify(calendarRuleMapper).insert(insert.capture());
        assertEquals("ENABLE", insert.getValue().getStatus());
        assertEquals(0, insert.getValue().getDeletedFlag());
    }

    @Test
    void saveRejectsEndDateBeforeStartDate() {
        MfgLineCalendarRule calendarRule = calendarRule();
        calendarRule.setEffectiveTo(calendarRule.getEffectiveFrom().minusDays(1));

        assertThrows(BusinessException.class, () -> service.saveCalendarRule(calendarRule));
    }

    @Test
    void saveRejectsInvalidDayOfWeekBeforeDataAccess() {
        MfgLineCalendarRule calendarRule = calendarRule();
        calendarRule.setDayOfWeek(8);

        assertThrows(BusinessException.class, () -> service.saveCalendarRule(calendarRule));
    }

    @Test
    void disableUpdatesStatusInsteadOfDeletingCalendarRule() {
        MfgLineCalendarRule existing = calendarRule();
        existing.setCalendarRuleId(5L);
        existing.setStatus("ENABLE");
        existing.setVersion(3);
        when(calendarRuleMapper.selectOne(any())).thenReturn(existing);
        when(calendarRuleMapper.updateById(any(MfgLineCalendarRule.class))).thenReturn(1);

        assertTrue(service.disableCalendarRule(5L));

        ArgumentCaptor<MfgLineCalendarRule> update = ArgumentCaptor.forClass(MfgLineCalendarRule.class);
        verify(calendarRuleMapper).updateById(update.capture());
        assertEquals("DISABLE", update.getValue().getStatus());
        assertEquals(3, update.getValue().getVersion());
    }

    private MfgLineCalendarRule calendarRule() {
        MfgLineCalendarRule calendarRule = new MfgLineCalendarRule();
        calendarRule.setLineId(1L);
        calendarRule.setDayOfWeek(1);
        calendarRule.setShiftId(2L);
        calendarRule.setEffectiveFrom(LocalDate.of(2026, 7, 13));
        calendarRule.setCapacityFactor(BigDecimal.ONE);
        return calendarRule;
    }
}
