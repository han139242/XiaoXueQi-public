package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * eam_equipment 设备台账表
 * 用于管理设备的基本信息，包括设备编码、名称、分类、规格型号、厂家、购入日期、所在工位和运行状态等
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("eam_equipment")
public class EamEquipment {

    @TableId(value = "equipment_id", type = IdType.AUTO)
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long categoryId;
    private String model;
    private String manufacturer;
    private LocalDate purchaseDate;
    private Long stationId;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
