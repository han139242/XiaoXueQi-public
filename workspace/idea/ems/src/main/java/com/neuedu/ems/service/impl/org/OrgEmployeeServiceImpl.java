package com.neuedu.ems.service.impl.org;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neuedu.ems.entity.org.OrgEmployee;
import com.neuedu.ems.mapper.org.OrgEmployeeMapper;
import com.neuedu.ems.security.phone.PhoneWriteValue;
import com.neuedu.ems.security.phone.SensitivePhoneField;
import com.neuedu.ems.security.phone.SensitivePhoneService;
import com.neuedu.ems.service.org.OrgEmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 员工 Service 实现类。
 */
@Service
public class OrgEmployeeServiceImpl extends ServiceImpl<OrgEmployeeMapper, OrgEmployee> implements OrgEmployeeService {

    @Override
    public Page<OrgEmployee> pageEmployees(Integer current, Integer size, String keyword, Long departmentId, String status) {
        Page<OrgEmployee> page = new Page<>(current, size);
        return baseMapper.selectPage(page, buildEmployeeQuery(keyword, departmentId, status));
    }

    @Override
    public OrgEmployee getEmployeeById(Long employeeId) {
        QueryWrapper<OrgEmployee> wrapper = new QueryWrapper<>();
        wrapper.eq("employee_id", employeeId);
        wrapper.eq("deleted_flag", 0);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public boolean saveEmployee(OrgEmployee employee, String rawPhone) {
        if (employee.getDeletedFlag() == null) {
            employee.setDeletedFlag(0);
        }
        applyPhone(employee, sensitivePhoneService.prepareCreate(SensitivePhoneField.EMPLOYEE, rawPhone));
        return baseMapper.insert(employee) > 0;
    }

    @Override
    public boolean updateEmployee(OrgEmployee employee, String rawPhone) {
        if (employee.getEmployeeId() == null) {
            return false;
        }
        PhoneWriteValue phoneWrite = sensitivePhoneService.prepareUpdate(SensitivePhoneField.EMPLOYEE, rawPhone);
        applyPhone(employee, phoneWrite);
        employee.setDeletedFlag(null);
        if (phoneWrite.clear()) {
            UpdateWrapper<OrgEmployee> wrapper = new UpdateWrapper<OrgEmployee>()
                    .eq("employee_id", employee.getEmployeeId())
                    .eq("deleted_flag", 0)
                    .set("phone", null)
                    .set("phone_lookup_hash", null);
            return baseMapper.update(employee, wrapper) > 0;
        }
        return baseMapper.updateById(employee) > 0;
    }

    @Override
    public boolean removeEmployeeById(Long employeeId) {
        OrgEmployee employee = new OrgEmployee();
        employee.setEmployeeId(employeeId);
        employee.setDeletedFlag(1);
        return baseMapper.updateById(employee) > 0;
    }

    @Override
    public List<OrgEmployee> listEmployees(String keyword, Long departmentId, String status) {
        return baseMapper.selectList(buildEmployeeQuery(keyword, departmentId, status));
    }

    private QueryWrapper<OrgEmployee> buildEmployeeQuery(String keyword, Long departmentId, String status) {
        QueryWrapper<OrgEmployee> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted_flag", 0);
        if (StringUtils.hasText(keyword)) {
            String phoneLookupHash = sensitivePhoneService.lookupHash(keyword).orElse(null);
            wrapper.and(item -> item.like("employee_no", keyword)
                    .or()
                    .like("employee_name", keyword)
                    .or(phoneLookupHash != null)
                    .eq(phoneLookupHash != null, "phone_lookup_hash", phoneLookupHash));
        }
        if (departmentId != null) {
            wrapper.eq("department_id", departmentId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("employee_id");
        return wrapper;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private SensitivePhoneService sensitivePhoneService;

    private void applyPhone(OrgEmployee employee, PhoneWriteValue phoneWrite) {
        if (!phoneWrite.changed() || phoneWrite.clear()) {
            return;
        }
        employee.setPhone(phoneWrite.encryptedValue());
        employee.setPhoneLookupHash(phoneWrite.lookupHash());
    }
}
