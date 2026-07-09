package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.ProcessWorkInstruction;

import java.util.List;

/**
 * 作业指导书 Service 接口。
 */
public interface ProcessWorkInstructionService extends IService<ProcessWorkInstruction> {

    Page<ProcessWorkInstruction> pageInstructions(Integer current, Integer size, String keyword,
                                                  Long operationId, Long productId, String status);

    ProcessWorkInstruction getInstructionById(Long instructionId);

    boolean saveInstruction(ProcessWorkInstruction instruction);

    boolean updateInstruction(ProcessWorkInstruction instruction);

    boolean removeInstructionById(Long instructionId);

    List<ProcessWorkInstruction> listInstructions(String keyword, Long operationId, Long productId, String status);
}
