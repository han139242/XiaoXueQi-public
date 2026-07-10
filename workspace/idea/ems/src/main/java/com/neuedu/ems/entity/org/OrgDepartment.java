package com.neuedu.ems.entity.org;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * org_department
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("org_department")
public class OrgDepartment {

    @TableId(value = "department_id", type = IdType.AUTO)
    private Long departmentId;
    private Long parentId;
    private String departmentCode;
    private String departmentName;
    private Long managerId;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
