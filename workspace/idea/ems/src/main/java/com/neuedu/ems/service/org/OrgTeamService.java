package com.neuedu.ems.service.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.org.OrgTeam;

import java.util.List;

/**
 * 班组 Service 接口。
 */
public interface OrgTeamService extends IService<OrgTeam> {

    Page<OrgTeam> pageTeams(Integer current, Integer size, String keyword, Long departmentId, Long leaderId);

    OrgTeam getTeamById(Long teamId);

    boolean saveTeam(OrgTeam team);

    boolean updateTeam(OrgTeam team);

    boolean removeTeamById(Long teamId);

    List<OrgTeam> listTeams(String keyword, Long departmentId, Long leaderId);
}
