package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.ProcessWorkInstruction;
import com.neuedu.ems.mapper.ProcessWorkInstructionMapper;
import com.neuedu.ems.service.ProcessWorkInstructionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 作业指导书 Service 实现类。
 */
@Service
public class ProcessWorkInstructionServiceImpl
        extends ServiceImpl<ProcessWorkInstructionMapper, ProcessWorkInstruction>
        implements ProcessWorkInstructionService {

    @Override
    public Page<ProcessWorkInstruction> pageInstructions(Integer current, Integer size, String keyword,
                                                         Long operationId, Long productId, String status) {
        Page<ProcessWorkInstruction> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildInstructionQuery(keyword, operationId, productId, status));
    }

    @Override
    public ProcessWorkInstruction getInstructionById(Long instructionId) {
        QueryWrapper<ProcessWorkInstruction> wrapper = new QueryWrapper<>();
        wrapper.eq("instruction_id", instructionId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveInstruction(ProcessWorkInstruction instruction) {
        if (instruction.getDeletedFlag() == null) {
            instruction.setDeletedFlag(0);
        }
        return baseMapper.insert(instruction) > 0;
    }

    @Override
    public boolean updateInstruction(ProcessWorkInstruction instruction) {
        if (instruction.getInstructionId() == null) {
            return false;
        }
        instruction.setDeletedFlag(null);
        return baseMapper.updateById(instruction) > 0;
    }

    @Override
    public boolean removeInstructionById(Long instructionId) {
        ProcessWorkInstruction instruction = new ProcessWorkInstruction();
        instruction.setInstructionId(instructionId);
        instruction.setDeletedFlag(1);
        return baseMapper.updateById(instruction) > 0;
    }

    @Override
    public List<ProcessWorkInstruction> listInstructions(String keyword, Long operationId,
                                                         Long productId, String status) {
        return baseMapper.selectList(buildInstructionQuery(keyword, operationId, productId, status));
    }

    private QueryWrapper<ProcessWorkInstruction> buildInstructionQuery(String keyword, Long operationId,
                                                                       Long productId, String status) {
        QueryWrapper<ProcessWorkInstruction> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.like("title", keyword);
        }
        if (operationId != null) {
            wrapper.eq("operation_id", operationId);
        }
        if (productId != null) {
            wrapper.eq("product_id", productId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("instruction_id");
        return wrapper;
    }
}
