package com.neuedu.ems.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * sys_dict_data
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_dict_data")
public class SysDictData {

    @TableId(value = "dict_data_id", type = IdType.AUTO)
    private Long dictDataId;
    private String dictTypeCode;
    private String dictValue;
    private String dictLabel;
    private Integer sortNo;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
