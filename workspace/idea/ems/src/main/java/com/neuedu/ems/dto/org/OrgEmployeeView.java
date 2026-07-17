package com.neuedu.ems.dto.org;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工读取 DTO。phone 仅为后端解密后的完整值或脱敏值。
 */
@Data
public class OrgEmployeeView {
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private Long departmentId;
    private String positionName;
    private String skillLevel;
    private String phone;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
