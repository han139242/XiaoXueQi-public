package com.neuedu.ems.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper 接口。
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
