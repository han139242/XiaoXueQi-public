package com.neuedu.ems.service.process;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.process.ProcessParameter;

import java.util.List;

/**
 * 工艺参数 Service 接口。
 */
public interface ProcessParameterService extends IService<ProcessParameter> {

    Page<ProcessParameter> pageParameters(Integer current, Integer size, Long operationId, String keyword);

    ProcessParameter getParameterById(Long parameterId);

    boolean saveParameter(ProcessParameter parameter);

    boolean updateParameter(ProcessParameter parameter);

    boolean removeParameterById(Long parameterId);

    List<ProcessParameter> listParameters(Long operationId, String keyword);
}
