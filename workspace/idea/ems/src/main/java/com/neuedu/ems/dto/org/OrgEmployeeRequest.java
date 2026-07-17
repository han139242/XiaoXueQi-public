package com.neuedu.ems.dto.org;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工写入 DTO。电话明文只在请求边界短暂存在。
 */
@Data
public class OrgEmployeeRequest {
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
}
