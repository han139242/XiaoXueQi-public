package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * sys_dict_type
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_dict_type")
public class SysDictType {

    @TableId(value = "dict_type_id", type = IdType.AUTO)
    private Long dictTypeId;
    private String dictTypeCode;
    private String dictTypeName;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
