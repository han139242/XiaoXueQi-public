package com.neuedu.ems.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_event_outbox")
public class SysEventOutbox {

    @TableId(value = "outbox_id", type = IdType.AUTO)
    private Long outboxId;
    private String eventKey;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String payloadJson;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryTime;
    private String lastError;
    private LocalDateTime occurredTime;
    private LocalDateTime processedTime;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
