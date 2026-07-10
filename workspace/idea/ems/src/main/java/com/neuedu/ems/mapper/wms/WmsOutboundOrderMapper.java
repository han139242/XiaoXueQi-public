package com.neuedu.ems.mapper.wms;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.wms.WmsOutboundOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库单管理 Mapper 接口。
 */
@Mapper
public interface WmsOutboundOrderMapper extends BaseMapper<WmsOutboundOrder> {
}
