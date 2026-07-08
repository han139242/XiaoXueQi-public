package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * mfg_exception
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mfg_exception")
public class MfgException {

    @TableId(value = "exception_id", type = IdType.AUTO)
    private Long exceptionId;
    private String exceptionNo;
    private String exceptionType;
    private Long sourceAlertId;
    private Long taskId;
    private Long stationId;
    private Long reportedBy;
    private String description;
    private String handleResult;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
