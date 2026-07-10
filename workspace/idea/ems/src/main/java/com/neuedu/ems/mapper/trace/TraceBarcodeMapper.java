package com.neuedu.ems.mapper.trace;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.trace.TraceBarcode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 条码追溯 Mapper 接口。
 */
@Mapper
public interface TraceBarcodeMapper extends BaseMapper<TraceBarcode> {
}
