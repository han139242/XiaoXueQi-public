package com.neuedu.ems.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notification")
public class SysNotification {

    @TableId(value = "notification_id", type = IdType.AUTO)
    private Long notificationId;
    private String eventKey;
    private String eventType;
    private String category;
    private String severity;
    private String title;
    private String summary;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private String routePath;
    private Long sourceUserId;
    private Long sourceEmployeeId;
    private LocalDateTime occurredTime;
    private LocalDateTime expireTime;
    private String status;
    private String payloadJson;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
