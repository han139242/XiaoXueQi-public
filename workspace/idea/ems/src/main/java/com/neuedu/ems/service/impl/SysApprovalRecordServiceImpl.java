package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.SysApprovalRecord;
import com.neuedu.ems.mapper.SysApprovalRecordMapper;
import com.neuedu.ems.service.SysApprovalRecordService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批记录 Service 实现类。
 */
@Service
public class SysApprovalRecordServiceImpl extends ServiceImpl<SysApprovalRecordMapper, SysApprovalRecord> implements SysApprovalRecordService {

    @Override
    public Page<SysApprovalRecord> pageApprovalRecords(Integer current, Integer size, String bizType, Long bizId,
                                                       String approvalNode, Long approverId, String approvalResult,
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        Page<SysApprovalRecord> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildApprovalRecordQuery(
                bizType, bizId, approvalNode, approverId, approvalResult, startTime, endTime));
    }

    @Override
    public SysApprovalRecord getApprovalRecordById(Long approvalId) {
        QueryWrapper<SysApprovalRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("approval_id", approvalId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveApprovalRecord(SysApprovalRecord approvalRecord) {
        if (approvalRecord.getDeletedFlag() == null) {
            approvalRecord.setDeletedFlag(0);
        }
        return baseMapper.insert(approvalRecord) > 0;
    }

    @Override
    public boolean updateApprovalRecord(SysApprovalRecord approvalRecord) {
        if (approvalRecord.getApprovalId() == null) {
            return false;
        }
        approvalRecord.setDeletedFlag(null);
        return baseMapper.updateById(approvalRecord) > 0;
    }

    @Override
    public boolean removeApprovalRecordById(Long approvalId) {
        SysApprovalRecord approvalRecord = new SysApprovalRecord();
        approvalRecord.setApprovalId(approvalId);
        approvalRecord.setDeletedFlag(1);
        return baseMapper.updateById(approvalRecord) > 0;
    }

    @Override
    public List<SysApprovalRecord> listApprovalRecords(String bizType, Long bizId, String approvalNode,
                                                       Long approverId, String approvalResult,
                                                       LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.selectList(buildApprovalRecordQuery(
                bizType, bizId, approvalNode, approverId, approvalResult, startTime, endTime));
    }

    private QueryWrapper<SysApprovalRecord> buildApprovalRecordQuery(String bizType, Long bizId, String approvalNode,
                                                                     Long approverId, String approvalResult,
                                                                     LocalDateTime startTime,
                                                                     LocalDateTime endTime) {
        QueryWrapper<SysApprovalRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(bizType)) {
            wrapper.eq("biz_type", bizType);
        }
        if (bizId != null) {
            wrapper.eq("biz_id", bizId);
        }
        if (StringUtils.hasText(approvalNode)) {
            wrapper.eq("approval_node", approvalNode);
        }
        if (approverId != null) {
            wrapper.eq("approver_id", approverId);
        }
        if (StringUtils.hasText(approvalResult)) {
            wrapper.eq("approval_result", approvalResult);
        }
        if (startTime != null) {
            wrapper.ge("approval_time", startTime);
        }
        if (endTime != null) {
            wrapper.le("approval_time", endTime);
        }
        wrapper.orderByDesc("approval_time", "approval_id");
        return wrapper;
    }
}
