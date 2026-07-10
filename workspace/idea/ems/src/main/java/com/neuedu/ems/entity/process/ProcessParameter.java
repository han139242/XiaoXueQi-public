package com.neuedu.ems.entity.process;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * process_parameter
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("process_parameter")
public class ProcessParameter {

    @TableId(value = "parameter_id", type = IdType.AUTO)
    private Long parameterId;
    private Long operationId;
    private String parameterName;
    private String standardValue;
    private String upperLimit;
    private String lowerLimit;
    private Long unitId;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
