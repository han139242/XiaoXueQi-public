package com.neuedu.ems.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.neuedu.ems.entity.SysUser;
import org.apache.ibatis.annotations.*;

/**
 * 系统用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    // MyBatis-Plus已提供基础CRUD方法
    // 自定义查询方法可以写在这里

    /**
     * 根据用户名查询用户
     */
    @Select("select * from sys_user where username = #{username} and deleted_flag = 0")
    SysUser getByUserName(@Param("username") String username);

    /**
     * 根据用户名和密码查询用户（登录）
     */
    @Select("select * from sys_user where username = #{username} and password_hash = #{password} and deleted_flag = 0")
    SysUser getByUserNameAndPassword(@Param("username") String username, @Param("password") String password);
}