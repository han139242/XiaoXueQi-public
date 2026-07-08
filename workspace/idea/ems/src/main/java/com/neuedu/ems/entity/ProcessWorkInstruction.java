package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * process_work_instruction
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("process_work_instruction")
public class ProcessWorkInstruction {

    @TableId(value = "instruction_id", type = IdType.AUTO)
    private Long instructionId;
    private Long operationId;
    private Long productId;
    private String version;
    private String title;
    private Long fileId;
    private LocalDateTime effectiveTime;
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
