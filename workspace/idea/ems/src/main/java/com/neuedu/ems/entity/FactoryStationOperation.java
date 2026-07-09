package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * factory_station_operation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("factory_station_operation")
public class FactoryStationOperation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long stationId;
    private Long operationId;
    private Integer isDefault;
    private String status;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Integer deletedFlag;
}
