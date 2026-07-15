package com.neuedu.ems.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_notification_recipient")
public class SysNotificationRecipient {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long notificationId;
    private Long recipientUserId;
    private Long recipientEmployeeId;
    private String recipientReason;
    private String readStatus;
    private LocalDateTime readTime;
    private String ackStatus;
    private LocalDateTime ackTime;
    private String deliveryStatus;
    private LocalDateTime deliveredTime;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
