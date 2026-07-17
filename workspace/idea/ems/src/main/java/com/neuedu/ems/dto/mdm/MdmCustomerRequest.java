package com.neuedu.ems.dto.mdm;

import lombok.Data;

import java.time.LocalDateTime;

/** 客户写入 DTO。 */
@Data
public class MdmCustomerRequest {
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
}
