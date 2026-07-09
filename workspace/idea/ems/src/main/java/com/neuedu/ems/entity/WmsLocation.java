package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * wms_location 库位表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_location")
public class WmsLocation {

    @TableId(value = "location_id", type = IdType.AUTO)
    private Long locationId;
    private String locationCode;
    private String locationName;
    private Long warehouseId;
    private String locationType;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
