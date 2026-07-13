package com.neuedu.ems.entity.process;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * process_route_item
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("process_route_item")
public class ProcessRouteItem {

    @TableId(value = "route_item_id", type = IdType.AUTO)
    private Long routeItemId;
    private Long routeId;
    private Long operationId;
    private Integer sequenceNo;
    private BigDecimal standardTime;
    private Integer qcRequired;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
