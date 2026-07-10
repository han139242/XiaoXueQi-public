package com.neuedu.ems.mapper.wms;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.wms.WmsInboundOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库单管理 Mapper 接口。
 */
@Mapper
public interface WmsInboundOrderMapper extends BaseMapper<WmsInboundOrder> {
}
