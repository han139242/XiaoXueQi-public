package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.OrgTeamMember;
import com.neuedu.ems.service.OrgTeamMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/org/team-member")
public class OrgTeamMemberController {

    @Autowired
    private OrgTeamMemberService orgTeamMemberService;

    /**
     * 分页查询班组成员列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) Long teamId,
                           @RequestParam(required = false) Long employeeId,
                           @RequestParam(required = false) String status) {
        Page<OrgTeamMember> result = orgTeamMemberService.pageTeamMembers(current, size, teamId, employeeId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据班组查询成员。
     */
    @GetMapping("/team/{teamId}")
    public ResultData listByTeam(@PathVariable("teamId") Long teamId) {
        List<OrgTeamMember> result = orgTeamMemberService.listByTeamId(teamId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询班组成员。
     */
    @GetMapping("/{id}")
    public ResultData getTeamMember(@PathVariable("id") Long id) {
        OrgTeamMember teamMember = orgTeamMemberService.getTeamMemberById(id);
        return ResultData.success(ResultCode.SUCCESS, teamMember);
    }

    /**
     * 新增班组成员。
     */
    @PostMapping
    public ResultData save(@RequestBody OrgTeamMember teamMember) {
        boolean flag = orgTeamMemberService.saveTeamMember(teamMember);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新班组成员。
     */
    @PutMapping
    public ResultData update(@RequestBody OrgTeamMember teamMember) {
        boolean flag = orgTeamMemberService.updateTeamMember(teamMember);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除班组成员，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = orgTeamMemberService.removeTeamMemberById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }
}
