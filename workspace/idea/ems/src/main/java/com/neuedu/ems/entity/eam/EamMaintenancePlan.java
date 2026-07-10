package com.neuedu.ems.entity.eam;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * eam_maintenance_plan
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("eam_maintenance_plan")
public class EamMaintenancePlan {

    @TableId(value = "plan_id", type = IdType.AUTO)
    private Long planId;
    private Long equipmentId;
    private String maintenanceType;
    private String cycleType;
    private BigDecimal cycleValue;
    private String maintenanceContent;
    private Long responsibleId;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
