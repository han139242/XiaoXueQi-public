package com.neuedu.ems.service.org;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.org.OrgEmployee;

import java.util.List;

/**
 * 员工 Service 接口。
 */
public interface OrgEmployeeService extends IService<OrgEmployee> {

    Page<OrgEmployee> pageEmployees(Integer current, Integer size, String keyword, Long departmentId, String status);

    OrgEmployee getEmployeeById(Long employeeId);

    boolean saveEmployee(OrgEmployee employee);

    boolean updateEmployee(OrgEmployee employee);

    boolean removeEmployeeById(Long employeeId);

    List<OrgEmployee> listEmployees(String keyword, Long departmentId, String status);
}
