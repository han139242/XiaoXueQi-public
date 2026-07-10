package com.neuedu.ems.entity.eam;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * eam_spot_check_plan
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("eam_spot_check_plan")
public class EamSpotCheckPlan {

    @TableId(value = "plan_id", type = IdType.AUTO)
    private Long planId;
    private Long equipmentId;
    private String checkCycle;
    private String checkItems;
    private String responsibleRole;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
