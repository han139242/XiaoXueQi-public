package com.neuedu.ems.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.neuedu.ems.entity.OrgDepartment;

import java.util.List;

/**
 * 部门 Service 接口。
 */
public interface OrgDepartmentService extends IService<OrgDepartment> {

    Page<OrgDepartment> pageDepartments(Integer current, Integer size, String keyword);

    OrgDepartment getDepartmentById(Long departmentId);

    boolean saveDepartment(OrgDepartment department);

    boolean updateDepartment(OrgDepartment department);

    boolean removeDepartmentById(Long departmentId);

    List<OrgDepartment> listDepartments(String keyword);

    List<OrgDepartment> treeDepartments();
}
