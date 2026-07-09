package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.SysApprovalRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批记录 Service 接口。
 */
public interface SysApprovalRecordService extends IService<SysApprovalRecord> {

    Page<SysApprovalRecord> pageApprovalRecords(Integer current, Integer size, String bizType, Long bizId,
                                                String approvalNode, Long approverId, String approvalResult,
                                                LocalDateTime startTime, LocalDateTime endTime);

    SysApprovalRecord getApprovalRecordById(Long approvalId);

    boolean saveApprovalRecord(SysApprovalRecord approvalRecord);

    boolean updateApprovalRecord(SysApprovalRecord approvalRecord);

    boolean removeApprovalRecordById(Long approvalId);

    List<SysApprovalRecord> listApprovalRecords(String bizType, Long bizId, String approvalNode,
                                                Long approverId, String approvalResult,
                                                LocalDateTime startTime, LocalDateTime endTime);
}
