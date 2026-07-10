package com.neuedu.ems.mapper.qms;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.qms.QmsNonconformingOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 不合格品处理单 Mapper 接口。
 */
@Mapper
public interface QmsNonconformingOrderMapper extends BaseMapper<QmsNonconformingOrder> {
}
