package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * factory_line
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("factory_line")
public class FactoryLine {

    @TableId(value = "line_id", type = IdType.AUTO)
    private Long lineId;
    private String lineCode;
    private String lineName;
    private Long workshopId;
    private BigDecimal dailyCapacity;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
