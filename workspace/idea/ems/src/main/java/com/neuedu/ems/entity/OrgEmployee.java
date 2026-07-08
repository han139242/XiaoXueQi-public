package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * org_employee
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("org_employee")
public class OrgEmployee {

    @TableId(value = "employee_id", type = IdType.AUTO)
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
