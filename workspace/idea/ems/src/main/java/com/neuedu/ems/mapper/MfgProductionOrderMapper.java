package com.neuedu.ems.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.MfgProductionOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 生产订单管理 Mapper 接口。
 */
@Mapper
public interface MfgProductionOrderMapper extends BaseMapper<MfgProductionOrder> {
}
