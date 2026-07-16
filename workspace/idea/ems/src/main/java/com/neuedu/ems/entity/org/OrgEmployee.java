package com.neuedu.ems.entity.org;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    /** 数据库存储 AES-GCM 密文，HTTP 响应必须通过 OrgEmployeeView 返回。 */
    @JsonIgnore
    private String phone;
    @JsonIgnore
    @TableField("phone_lookup_hash")
    private String phoneLookupHash;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
