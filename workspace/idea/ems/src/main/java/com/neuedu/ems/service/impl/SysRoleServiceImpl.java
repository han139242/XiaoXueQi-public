package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.SysRole;
import com.neuedu.ems.mapper.SysRoleMapper;
import com.neuedu.ems.service.SysRoleService;
import org.springframework.stereotype.Service;

/**
 * 系统角色 Service 实现类。
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
}
