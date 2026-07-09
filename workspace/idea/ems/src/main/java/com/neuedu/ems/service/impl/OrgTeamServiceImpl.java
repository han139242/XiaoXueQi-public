package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.OrgTeam;
import com.neuedu.ems.mapper.OrgTeamMapper;
import com.neuedu.ems.service.OrgTeamService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 班组 Service 实现类。
 */
@Service
public class OrgTeamServiceImpl extends ServiceImpl<OrgTeamMapper, OrgTeam> implements OrgTeamService {

    @Override
    public Page<OrgTeam> pageTeams(Integer current, Integer size, String keyword, Long departmentId, Long leaderId) {
        Page<OrgTeam> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildTeamQuery(keyword, departmentId, leaderId));
    }

    @Override
    public OrgTeam getTeamById(Long teamId) {
        QueryWrapper<OrgTeam> wrapper = new QueryWrapper<>();
        wrapper.eq("team_id", teamId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveTeam(OrgTeam team) {
        if (team.getDeletedFlag() == null) {
            team.setDeletedFlag(0);
        }
        return baseMapper.insert(team) > 0;
    }

    @Override
    public boolean updateTeam(OrgTeam team) {
        if (team.getTeamId() == null) {
            return false;
        }
        team.setDeletedFlag(null);
        return baseMapper.updateById(team) > 0;
    }

    @Override
    public boolean removeTeamById(Long teamId) {
        OrgTeam team = new OrgTeam();
        team.setTeamId(teamId);
        team.setDeletedFlag(1);
        return baseMapper.updateById(team) > 0;
    }

    @Override
    public List<OrgTeam> listTeams(String keyword, Long departmentId, Long leaderId) {
        return baseMapper.selectList(buildTeamQuery(keyword, departmentId, leaderId));
    }

    private QueryWrapper<OrgTeam> buildTeamQuery(String keyword, Long departmentId, Long leaderId) {
        QueryWrapper<OrgTeam> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("team_code", keyword).or().like("team_name", keyword));
        }
        if (departmentId != null) {
            wrapper.eq("department_id", departmentId);
        }
        if (leaderId != null) {
            wrapper.eq("leader_id", leaderId);
        }
        wrapper.orderByDesc("team_id");
        return wrapper;
    }
}
