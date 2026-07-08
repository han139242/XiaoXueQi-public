package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * mdm_customer
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mdm_customer")
public class MdmCustomer {

    @TableId(value = "customer_id", type = IdType.AUTO)
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String contactName;
    private String contactPhone;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
