package com.neuedu.ems.mapper.wms;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.wms.WmsReturnDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 退料明细管理 Mapper 接口。
 */
@Mapper
public interface WmsReturnDetailMapper extends BaseMapper<WmsReturnDetail> {

    @Select("""
            SELECT COALESCE(SUM(detail.return_quantity), 0)
            FROM wms_return_detail detail
            JOIN wms_return_order orders ON orders.return_id = detail.return_id
            WHERE detail.outbound_detail_id = #{outboundDetailId}
              AND detail.deleted_flag = 0
              AND orders.deleted_flag = 0
              AND orders.audit_status IN ('SUBMITTED', 'APPROVED')
              AND (#{excludedReturnId} IS NULL OR detail.return_id <> #{excludedReturnId})
            """)
    BigDecimal sumActiveReturnedQuantity(@Param("outboundDetailId") Long outboundDetailId,
                                         @Param("excludedReturnId") Long excludedReturnId);

    @Select("""
            SELECT COALESCE(SUM(detail.return_quantity), 0)
            FROM wms_return_detail detail
            JOIN wms_return_order orders ON orders.return_id = detail.return_id
            WHERE orders.work_order_id = #{workOrderId}
              AND detail.material_id = #{materialId}
              AND detail.batch_id = #{batchId}
              AND detail.deleted_flag = 0
              AND orders.deleted_flag = 0
              AND orders.audit_status IN ('SUBMITTED', 'APPROVED')
              AND (#{excludedReturnId} IS NULL OR detail.return_id <> #{excludedReturnId})
            """)
    BigDecimal sumActiveReturnedQuantityByBatch(@Param("workOrderId") Long workOrderId,
                                                @Param("materialId") Long materialId,
                                                @Param("batchId") Long batchId,
                                                @Param("excludedReturnId") Long excludedReturnId);
}
