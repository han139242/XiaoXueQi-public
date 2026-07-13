package com.neuedu.ems.entity.mfg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_schedule_scenario")
public class MfgScheduleScenario {

    @TableId(value = "scenario_id", type = IdType.AUTO)
    private Long scenarioId;
    private String scenarioNo;
    private String scenarioName;
    private LocalDateTime horizonStart;
    private LocalDateTime horizonEnd;
    private String algorithmCode;
    private String status;
    private Integer orderCount;
    private Integer scheduledOrderCount;
    private Integer conflictOrderCount;
    private Integer lateOrderCount;
    private BigDecimal totalLoadMinutes;
    private String aiAdvice;
    private String aiModel;
    private LocalDateTime aiGeneratedTime;
    private Long confirmedBy;
    private LocalDateTime confirmedTime;
    private String remark;
    @Version
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
