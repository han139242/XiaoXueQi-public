package com.neuedu.ems.service.impl.process;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.process.ProcessOperation;
import com.neuedu.ems.mapper.process.ProcessOperationMapper;
import com.neuedu.ems.service.process.ProcessOperationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工序 Service 实现类。
 */
@Service
public class ProcessOperationServiceImpl
        extends ServiceImpl<ProcessOperationMapper, ProcessOperation>
        implements ProcessOperationService {

    @Override
    public Page<ProcessOperation> pageOperations(Integer current, Integer size, String keyword, String status) {
        Page<ProcessOperation> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildOperationQuery(keyword, status));
    }

    @Override
    public ProcessOperation getOperationById(Long operationId) {
        QueryWrapper<ProcessOperation> wrapper = new QueryWrapper<>();
        wrapper.eq("operation_id", operationId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveOperation(ProcessOperation operation) {
        if (operation.getDeletedFlag() == null) {
            operation.setDeletedFlag(0);
        }
        return baseMapper.insert(operation) > 0;
    }

    @Override
    public boolean updateOperation(ProcessOperation operation) {
        if (operation.getOperationId() == null) {
            return false;
        }
        operation.setDeletedFlag(null);
        return baseMapper.updateById(operation) > 0;
    }

    @Override
    public boolean removeOperationById(Long operationId) {
        ProcessOperation operation = new ProcessOperation();
        operation.setOperationId(operationId);
        operation.setDeletedFlag(1);
        return baseMapper.updateById(operation) > 0;
    }

    @Override
    public List<ProcessOperation> listOperations(String keyword, String status) {
        return baseMapper.selectList(buildOperationQuery(keyword, status));
    }

    private QueryWrapper<ProcessOperation> buildOperationQuery(String keyword, String status) {
        QueryWrapper<ProcessOperation> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("operation_code", keyword)
                    .or()
                    .like("operation_name", keyword)
                    .or()
                    .like("operation_type", keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("operation_id");
        return wrapper;
    }
}
