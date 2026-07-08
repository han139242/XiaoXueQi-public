package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * factory_workshop
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("factory_workshop")
public class FactoryWorkshop {

    @TableId(value = "workshop_id", type = IdType.AUTO)
    private Long workshopId;
    private String workshopCode;
    private String workshopName;
    private Long managerId;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
