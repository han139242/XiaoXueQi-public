package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * mfg_andon_alert
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_andon_alert")
public class MfgAndonAlert {

    @TableId(value = "alert_id", type = IdType.AUTO)
    private Long alertId;
    private String alertNo;
    private Long lineId;
    private Long stationId;
    private Long triggeredBy;
    private String alertType;
    private LocalDateTime triggerTime;
    private LocalDateTime responseTime;
    private LocalDateTime resolvedTime;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
