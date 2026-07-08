package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * eam_spot_check_record
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("eam_spot_check_record")
public class EamSpotCheckRecord {

    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;
    private Long planId;
    private Long equipmentId;
    private Long checkerId;
    private LocalDateTime checkTime;
    private String checkResult;
    private String abnormalDescription;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
