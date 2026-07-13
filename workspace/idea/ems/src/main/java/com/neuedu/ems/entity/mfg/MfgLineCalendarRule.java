package com.neuedu.ems.entity.mfg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_line_calendar_rule")
public class MfgLineCalendarRule {

    @TableId(value = "calendar_rule_id", type = IdType.AUTO)
    private Long calendarRuleId;
    private Long lineId;
    private Integer dayOfWeek;
    private Long shiftId;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal capacityFactor;
    private String status;
    private String remark;
    @Version
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
