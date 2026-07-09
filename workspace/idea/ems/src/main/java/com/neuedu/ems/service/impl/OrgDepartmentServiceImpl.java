package com.neuedu.ems.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.OrgDepartment;
import com.neuedu.ems.mapper.OrgDepartmentMapper;
import com.neuedu.ems.service.OrgDepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 部门 Service 实现类。
 */
@Service
public class OrgDepartmentServiceImpl extends ServiceImpl<OrgDepartmentMapper, OrgDepartment> implements OrgDepartmentService {

    @Override
    public Page<OrgDepartment> pageDepartments(Integer current, Integer size, String keyword) {
        Page<OrgDepartment> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildDepartmentQuery(keyword));
    }

    @Override
    public OrgDepartment getDepartmentById(Long departmentId) {
        QueryWrapper<OrgDepartment> wrapper = new QueryWrapper<>();
        wrapper.eq("department_id", departmentId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveDepartment(OrgDepartment department) {
        if (department.getDeletedFlag() == null) {
            department.setDeletedFlag(0);
        }
        return baseMapper.insert(department) > 0;
    }

    @Override
    public boolean updateDepartment(OrgDepartment department) {
        if (department.getDepartmentId() == null) {
            return false;
        }
        department.setDeletedFlag(null);
        return baseMapper.updateById(department) > 0;
    }

    @Override
    public boolean removeDepartmentById(Long departmentId) {
        OrgDepartment department = new OrgDepartment();
        department.setDepartmentId(departmentId);
        department.setDeletedFlag(1);
        return baseMapper.updateById(department) > 0;
    }

    @Override
    public List<OrgDepartment> listDepartments(String keyword) {
        return baseMapper.selectList(buildDepartmentQuery(keyword));
    }

    @Override
    public List<OrgDepartment> treeDepartments() {
        QueryWrapper<OrgDepartment> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        wrapper.orderByAsc("parent_id").orderByAsc("department_id");
        return baseMapper.selectList(wrapper);
    }

    private QueryWrapper<OrgDepartment> buildDepartmentQuery(String keyword) {
        QueryWrapper<OrgDepartment> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(item -> item.like("department_code", keyword).or().like("department_name", keyword));
        }
        wrapper.orderByDesc("department_id");
        return wrapper;
    }
}
