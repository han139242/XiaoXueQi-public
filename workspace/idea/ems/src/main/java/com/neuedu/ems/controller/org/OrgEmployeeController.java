package com.neuedu.ems.controller.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.common.ResultData;
import com.neuedu.ems.entity.org.OrgEmployee;
import com.neuedu.ems.service.org.OrgEmployeeService;
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
@RequestMapping("/api/org/employee")
public class OrgEmployeeController {

    @Autowired
    private OrgEmployeeService orgEmployeeService;

    @Autowired
    private ExcelExportUtil excelExportUtil;

    /**
     * 分页查询员工列表。
     */
    @GetMapping("/page")
    public ResultData list(@RequestParam(defaultValue = "1") Integer current,
                           @RequestParam(defaultValue = "10") Integer size,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long departmentId,
                           @RequestParam(required = false) String status) {
        Page<OrgEmployee> result = orgEmployeeService.pageEmployees(current, size, keyword, departmentId, status);
        return ResultData.success(ResultCode.SUCCESS, result);
    }

    /**
     * 根据 ID 查询员工。
     */
    @GetMapping("/{employeeId}")
    public ResultData getEmployee(@PathVariable("employeeId") Long id) {
        OrgEmployee employee = orgEmployeeService.getEmployeeById(id);
        return ResultData.success(ResultCode.SUCCESS, employee);
    }

    /**
     * 新增员工。
     */
    @PostMapping
    public ResultData save(@RequestBody OrgEmployee employee) {
        boolean flag = orgEmployeeService.saveEmployee(employee);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 更新员工。
     */
    @PutMapping
    public ResultData update(@RequestBody OrgEmployee employee) {
        boolean flag = orgEmployeeService.updateEmployee(employee);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 删除员工，统一做软删除。
     */
    @DeleteMapping("/{id}")
    public ResultData delete(@PathVariable("id") Long id) {
        boolean flag = orgEmployeeService.removeEmployeeById(id);
        return flag ? ResultData.success(ResultCode.SUCCESS) : ResultData.fail(ResultCode.FAILED);
    }

    /**
     * 导出员工 Excel。
     */
    @GetMapping("/excel")
    public void excel(HttpServletResponse response,
                      @RequestParam(required = false) String keyword,
                      @RequestParam(required = false) Long departmentId,
                      @RequestParam(required = false) String status) {
        List<OrgEmployee> employees = orgEmployeeService.listEmployees(keyword, departmentId, status);
        excelExportUtil.exportExcel(response, employees, OrgEmployee.class, "员工列表", "员工列表");
    }
}
