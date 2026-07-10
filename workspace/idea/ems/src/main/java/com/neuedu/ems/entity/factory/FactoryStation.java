package com.neuedu.ems.entity.factory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * factory_station
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("factory_station")
public class FactoryStation {

    @TableId(value = "station_id", type = IdType.AUTO)
    private Long stationId;
    private String stationCode;
    private String stationName;
    private Long lineId;
    private String stationType;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
