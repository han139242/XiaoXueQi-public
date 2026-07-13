package com.neuedu.ems.entity.org;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * org_shift
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("org_shift")
public class OrgShift {

    @TableId(value = "shift_id", type = IdType.AUTO)
    private Long shiftId;
    private String shiftCode;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
