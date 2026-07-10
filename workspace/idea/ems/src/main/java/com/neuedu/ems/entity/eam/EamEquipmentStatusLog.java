package com.neuedu.ems.entity.eam;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * eam_equipment_status_log
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("eam_equipment_status_log")
public class EamEquipmentStatusLog {

    @TableId(value = "status_log_id", type = IdType.AUTO)
    private Long statusLogId;
    private Long equipmentId;
    private String runningStatus;
    private String downtimeType;
    private String alertLevel;
    private Long stationId;
    private LocalDateTime collectedTime;
    private String description;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
