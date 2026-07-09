package com.neuedu.ems.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.OrgDepartment;
import com.neuedu.ems.service.OrgDepartmentService;
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
@RequestMapping("/api/org/department")
public class OrgDepartmentController {

    @Autowired
    private OrgDepartmentService orgDepartmentService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询部门列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword) {
        Page<OrgDepartment> result = orgDepartmentService.pageDepartments(current, size, keyword);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 查询部门树。
     */
    @GetMapping("/tree")
    public ResultData tree() {
        List<OrgDepartment> result = orgDepartmentService.treeDepartments();
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询部门。
     */
    @GetMapping("/{departmentId}")
    public ResultData getDepartment(@PathVariable("departmentId") Long id) {
        OrgDepartment department = orgDepartmentService.getDepartmentById(id);
        return ResultData.success(ResultCode.SUCCESS, department);
    }

    /**
     * 新增部门。
     */
    @PostMapping
    public ResultData save(@RequestBody OrgDepartment department) {
        boolean flag = orgDepartmentService.saveDepartment(department);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新部门。
     */
    @PutMapping
    public ResultData update(@RequestBody OrgDepartment department) {
        boolean flag = orgDepartmentService.updateDepartment(department);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除部门，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = orgDepartmentService.removeDepartmentById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出部门 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword) {
        List<OrgDepartment> departments = orgDepartmentService.listDepartments(keyword);
        excelExportUtil.exportExcel(response, departments, OrgDepartment.class, "部门列表", "部门列表");
    }
}
