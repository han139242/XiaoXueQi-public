package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * org_team
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("org_team")
public class OrgTeam {

    @TableId(value = "team_id", type = IdType.AUTO)
    private Long teamId;
    private String teamCode;
    private String teamName;
    private Long leaderId;
    private Long departmentId;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
