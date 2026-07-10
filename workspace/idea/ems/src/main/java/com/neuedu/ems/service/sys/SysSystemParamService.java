package com.neuedu.ems.service.sys;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.sys.SysSystemParam;

import java.util.List;

/**
 * 系统参数 Service 接口。
 */
public interface SysSystemParamService extends IService<SysSystemParam> {

    Page<SysSystemParam> pageSystemParams(Integer current, Integer size, String keyword, String moduleCode);

    SysSystemParam getSystemParamById(Long paramId);

    boolean saveSystemParam(SysSystemParam systemParam);

    boolean updateSystemParam(SysSystemParam systemParam);

    boolean removeSystemParamById(Long paramId);

    List<SysSystemParam> listSystemParams(String keyword, String moduleCode);
}
