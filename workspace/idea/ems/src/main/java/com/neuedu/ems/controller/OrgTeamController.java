package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.OrgTeam;
import com.neuedu.ems.service.OrgTeamService;
import com.neuedu.ems.util.ExcelExportUtil;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping("/api/org/team")
public class OrgTeamController {

    @Autowired
    private OrgTeamService orgTeamService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询班组列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long departmentId,
                           @RequestParam(required = false) Long leaderId) {
        Page<OrgTeam> result = orgTeamService.pageTeams(current, size, keyword, departmentId, leaderId);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询班组。
     */
    @GetMapping("/{teamId}")
    public ResultData getTeam(@PathVariable("teamId") Long id) {
        OrgTeam team = orgTeamService.getTeamById(id);
        return ResultData.success(ResultCode.SUCCESS, team);
    }

    /**
     * 新增班组。
     */
    @PostMapping
    public ResultData save(@RequestBody OrgTeam team) {
        boolean flag = orgTeamService.saveTeam(team);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新班组。
     */
    @PutMapping
    public ResultData update(@RequestBody OrgTeam team) {
        boolean flag = orgTeamService.updateTeam(team);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除班组，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = orgTeamService.removeTeamById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出班组 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long departmentId,
                      @RequestParam(required = false) Long leaderId) {
        List<OrgTeam> teams = orgTeamService.listTeams(keyword, departmentId, leaderId);
        excelExportUtil.exportExcel(response, teams, OrgTeam.class, "班组列表", "班组列表");
    }
}
