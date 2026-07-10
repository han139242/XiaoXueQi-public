package com.neuedu.ems.entity.wms;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * wms_warehouse
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("wms_warehouse")
public class WmsWarehouse {

    @TableId(value = "warehouse_id", type = IdType.AUTO)
    private Long warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseType;
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
