package com.neuedu.ems.dto.mdm;

import lombok.Data;

import java.time.LocalDateTime;

/** 供应商写入 DTO。 */
@Data
public class MdmSupplierRequest {
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
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
