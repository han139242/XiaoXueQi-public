package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.SysUser;
import com.neuedu.ems.mapper.SysUserMapper;
import com.neuedu.ems.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户Service实现类
 */
@Service
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    @Override
    public SysUser getByUserName(String username) {
        return baseMapper.getByUserName(username);
    }

    @Override
    public SysUser getByUserNameAndPassword(String username, String password) {
        return baseMapper.getByUserNameAndPassword(username, password);
    }
}
