package com.neuedu.ems.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.WmsInventory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存余额管理 Mapper 接口。
 */
@Mapper
public interface WmsInventoryMapper extends BaseMapper<WmsInventory> {
}
