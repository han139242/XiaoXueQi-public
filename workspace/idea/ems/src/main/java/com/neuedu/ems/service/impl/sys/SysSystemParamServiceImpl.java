package com.neuedu.ems.service.impl.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.sys.SysSystemParam;
import com.neuedu.ems.mapper.sys.SysSystemParamMapper;
import com.neuedu.ems.service.sys.SysSystemParamService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统参数 Service 实现类。
 */
@Service
public class SysSystemParamServiceImpl extends ServiceImpl<SysSystemParamMapper, SysSystemParam> implements SysSystemParamService {

    @Override
    public Page<SysSystemParam> pageSystemParams(Integer current, Integer size, String keyword, String moduleCode) {
        Page<SysSystemParam> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildSystemParamQuery(keyword, moduleCode));
    }

    @Override
    public SysSystemParam getSystemParamById(Long paramId) {
        QueryWrapper<SysSystemParam> wrapper = new QueryWrapper<>();
        wrapper.eq("param_id", paramId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveSystemParam(SysSystemParam systemParam) {
        if (systemParam.getDeletedFlag() == null) {
            systemParam.setDeletedFlag(0);
        }
        return baseMapper.insert(systemParam) > 0;
    }

    @Override
    public boolean updateSystemParam(SysSystemParam systemParam) {
        if (systemParam.getParamId() == null) {
            return false;
        }
        systemParam.setDeletedFlag(null);
        return baseMapper.updateById(systemParam) > 0;
    }

    @Override
    public boolean removeSystemParamById(Long paramId) {
        SysSystemParam systemParam = new SysSystemParam();
        systemParam.setParamId(paramId);
        systemParam.setDeletedFlag(1);
        return baseMapper.updateById(systemParam) > 0;
    }

    @Override
    public List<SysSystemParam> listSystemParams(String keyword, String moduleCode) {
        return baseMapper.selectList(buildSystemParamQuery(keyword, moduleCode));
    }

    private QueryWrapper<SysSystemParam> buildSystemParamQuery(String keyword, String moduleCode) {
        QueryWrapper<SysSystemParam> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(moduleCode)) {
            wrapper.eq("module_code", moduleCode);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("param_key", keyword).or().like("param_name", keyword));
        }
        wrapper.orderByDesc("param_id");
        return wrapper;
    }
}
