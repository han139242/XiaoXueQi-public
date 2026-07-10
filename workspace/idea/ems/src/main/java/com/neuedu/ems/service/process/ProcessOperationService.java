package com.neuedu.ems.service.process;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.process.ProcessOperation;

import java.util.List;

/**
 * 工序 Service 接口。
 */
public interface ProcessOperationService extends IService<ProcessOperation> {

    Page<ProcessOperation> pageOperations(Integer current, Integer size, String keyword, String status);

    ProcessOperation getOperationById(Long operationId);

    boolean saveOperation(ProcessOperation operation);

    boolean updateOperation(ProcessOperation operation);

    boolean removeOperationById(Long operationId);

    List<ProcessOperation> listOperations(String keyword, String status);
}
