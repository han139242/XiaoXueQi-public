package com.neuedu.ems.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.WmsInboundOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库单管理 Mapper 接口。
 */
@Mapper
public interface WmsInboundOrderMapper extends BaseMapper<WmsInboundOrder> {
}
