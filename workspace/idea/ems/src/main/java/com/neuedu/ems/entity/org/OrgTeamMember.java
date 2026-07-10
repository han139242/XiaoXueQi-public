package com.neuedu.ems.entity.org;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * org_team_member
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("org_team_member")
public class OrgTeamMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long teamId;
    private Long employeeId;
    private LocalDate joinDate;
    private LocalDate leaveDate;
    private String status;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Integer deletedFlag;
}
