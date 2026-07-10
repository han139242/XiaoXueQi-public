package com.neuedu.ems.mapper.mfg;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.mfg.MfgWorkOrderMaterial;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单物料需求持久层接口。
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作。
 */
@Mapper
public interface MfgWorkOrderMaterialMapper extends BaseMapper<MfgWorkOrderMaterial> {
}
