package com.neuedu.ems.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.WmsStockMovement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存流水管理 Mapper 接口。
 */
@Mapper
public interface WmsStockMovementMapper extends BaseMapper<WmsStockMovement> {
}
