package com.neuedu.ems.mapper.trace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.trace.TraceMaterialUsage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物料使用追溯 Mapper 接口。
 */
@Mapper
public interface TraceMaterialUsageMapper extends BaseMapper<TraceMaterialUsage> {
}
