package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.ProcessParameter;
import com.neuedu.ems.mapper.ProcessParameterMapper;
import com.neuedu.ems.service.ProcessParameterService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工艺参数 Service 实现类。
 */
@Service
public class ProcessParameterServiceImpl
        extends ServiceImpl<ProcessParameterMapper, ProcessParameter>
        implements ProcessParameterService {

    @Override
    public Page<ProcessParameter> pageParameters(Integer current, Integer size, Long operationId, String keyword) {
        Page<ProcessParameter> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildParameterQuery(operationId, keyword));
    }

    @Override
    public ProcessParameter getParameterById(Long parameterId) {
        QueryWrapper<ProcessParameter> wrapper = new QueryWrapper<>();
        wrapper.eq("parameter_id", parameterId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveParameter(ProcessParameter parameter) {
        if (parameter.getDeletedFlag() == null) {
            parameter.setDeletedFlag(0);
        }
        return baseMapper.insert(parameter) > 0;
    }

    @Override
    public boolean updateParameter(ProcessParameter parameter) {
        if (parameter.getParameterId() == null) {
            return false;
        }
        parameter.setDeletedFlag(null);
        return baseMapper.updateById(parameter) > 0;
    }

    @Override
    public boolean removeParameterById(Long parameterId) {
        ProcessParameter parameter = new ProcessParameter();
        parameter.setParameterId(parameterId);
        parameter.setDeletedFlag(1);
        return baseMapper.updateById(parameter) > 0;
    }

    @Override
    public List<ProcessParameter> listParameters(Long operationId, String keyword) {
        return baseMapper.selectList(buildParameterQuery(operationId, keyword));
    }

    private QueryWrapper<ProcessParameter> buildParameterQuery(Long operationId, String keyword) {
        QueryWrapper<ProcessParameter> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (operationId != null) {
            wrapper.eq("operation_id", operationId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like("parameter_name", keyword);
        }
        wrapper.orderByDesc("parameter_id");
        return wrapper;
    }
}
