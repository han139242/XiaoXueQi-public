package com.neuedu.ems.entity.process;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * process_route
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("process_route")
public class ProcessRoute {

    @TableId(value = "route_id", type = IdType.AUTO)
    private Long routeId;
    private String routeCode;
    private String routeName;
    private Long productId;
    private String version;
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
