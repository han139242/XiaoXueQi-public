package com.neuedu.ems.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.WmsOutboundOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库单管理 Mapper 接口。
 */
@Mapper
public interface WmsOutboundOrderMapper extends BaseMapper<WmsOutboundOrder> {
}
