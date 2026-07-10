package com.neuedu.ems.service.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.org.OrgTeamMember;

import java.util.List;

/**
 * 班组成员 Service 接口。
 */
public interface OrgTeamMemberService extends IService<OrgTeamMember> {

    Page<OrgTeamMember> pageTeamMembers(Integer current, Integer size, Long teamId, Long employeeId, String status);

    OrgTeamMember getTeamMemberById(Long id);

    boolean saveTeamMember(OrgTeamMember teamMember);

    boolean updateTeamMember(OrgTeamMember teamMember);

    boolean removeTeamMemberById(Long id);

    List<OrgTeamMember> listByTeamId(Long teamId);
}
