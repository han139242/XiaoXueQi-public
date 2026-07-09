package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.OrgTeamMember;
import com.neuedu.ems.mapper.OrgTeamMemberMapper;
import com.neuedu.ems.service.OrgTeamMemberService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 班组成员 Service 实现类。
 */
@Service
public class OrgTeamMemberServiceImpl extends ServiceImpl<OrgTeamMemberMapper, OrgTeamMember> implements OrgTeamMemberService {

    @Override
    public Page<OrgTeamMember> pageTeamMembers(Integer current, Integer size, Long teamId, Long employeeId, String status) {
        Page<OrgTeamMember> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildTeamMemberQuery(teamId, employeeId, status));
    }

    @Override
    public OrgTeamMember getTeamMemberById(Long id) {
        QueryWrapper<OrgTeamMember> wrapper = new QueryWrapper<>();
        wrapper.eq("id", id);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveTeamMember(OrgTeamMember teamMember) {
        if (teamMember.getCreatedTime() == null) {
            teamMember.setCreatedTime(LocalDateTime.now());
        }
        if (teamMember.getDeletedFlag() == null) {
            teamMember.setDeletedFlag(0);
        }
        return baseMapper.insert(teamMember) > 0;
    }

    @Override
    public boolean updateTeamMember(OrgTeamMember teamMember) {
        if (teamMember.getId() == null) {
            return false;
        }
        teamMember.setDeletedFlag(null);
        return baseMapper.updateById(teamMember) > 0;
    }

    @Override
    public boolean removeTeamMemberById(Long id) {
        OrgTeamMember teamMember = new OrgTeamMember();
        teamMember.setId(id);
        teamMember.setDeletedFlag(1);
        return baseMapper.updateById(teamMember) > 0;
    }

    @Override
    public List<OrgTeamMember> listByTeamId(Long teamId) {
        QueryWrapper<OrgTeamMember> wrapper = new QueryWrapper<>();
        wrapper.eq("team_id", teamId);
        wrapper.eq("deleted_flag", 0);
        wrapper.orderByDesc("join_date").orderByDesc("id");
        return baseMapper.selectList(wrapper);
    }

    private QueryWrapper<OrgTeamMember> buildTeamMemberQuery(Long teamId, Long employeeId, String status) {
        QueryWrapper<OrgTeamMember> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (teamId != null) {
            wrapper.eq("team_id", teamId);
        }
        if (employeeId != null) {
            wrapper.eq("employee_id", employeeId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("id");
        return wrapper;
    }
}
