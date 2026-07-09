package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.OrgEmployee;

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
