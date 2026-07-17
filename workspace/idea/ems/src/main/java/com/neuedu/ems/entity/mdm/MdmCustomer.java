package com.neuedu.ems.entity.mdm;

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
    /** 数据库存储 AES-GCM 密文，HTTP 响应必须通过 MdmCustomerView 返回。 */
    @JsonIgnore
    private String contactPhone;
    @JsonIgnore
    @TableField("contact_phone_lookup_hash")
    private String contactPhoneLookupHash;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
