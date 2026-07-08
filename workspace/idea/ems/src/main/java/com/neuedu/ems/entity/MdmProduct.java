package com.neuedu.ems.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * mdm_product
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("mdm_product")
public class MdmProduct {

    @TableId(value = "product_id", type = IdType.AUTO)
    private Long productId;
    private String productCode;
    private String productName;
    private String productModel;
    private Long materialId;
    private String status;
    private String remark;
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
