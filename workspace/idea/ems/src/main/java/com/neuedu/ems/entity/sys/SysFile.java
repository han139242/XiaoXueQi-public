package com.neuedu.ems.entity.sys;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * sys_file
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_file")
public class SysFile {

    @TableId(value = "file_id", type = IdType.AUTO)
    private Long fileId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private String bizType;
    private Long bizId;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
