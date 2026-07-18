package com.neuedu.ems.security.phone;

import com.neuedu.ems.dto.mdm.MdmCustomerRequest;
import com.neuedu.ems.dto.mdm.MdmCustomerView;
import com.neuedu.ems.dto.mdm.MdmSupplierRequest;
import com.neuedu.ems.dto.mdm.MdmSupplierView;
import com.neuedu.ems.dto.org.OrgEmployeeRequest;
import com.neuedu.ems.dto.org.OrgEmployeeView;
import com.neuedu.ems.entity.mdm.MdmCustomer;
import com.neuedu.ems.entity.mdm.MdmSupplier;
import com.neuedu.ems.entity.org.OrgEmployee;
import com.neuedu.ems.exception.SensitivePhoneCryptoException;
import org.springframework.stereotype.Component;

/**
 * 敏感字段资源的 HTTP DTO 与持久化实体转换器，禁止实体穿透 Controller 边界。
 */
@Component
public class SensitivePhoneDtoMapper {

    private final SensitivePhoneService phoneService;

    public SensitivePhoneDtoMapper(SensitivePhoneService phoneService) {
        this.phoneService = phoneService;
    }

    public OrgEmployee toEntity(OrgEmployeeRequest request) {
        OrgEmployee entity = new OrgEmployee();
        entity.setEmployeeId(request.getEmployeeId());
        entity.setEmployeeNo(request.getEmployeeNo());
        entity.setEmployeeName(request.getEmployeeName());
        entity.setDepartmentId(request.getDepartmentId());
        entity.setPositionName(request.getPositionName());
        entity.setSkillLevel(request.getSkillLevel());
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        entity.setVersion(request.getVersion());
        entity.setCreatedBy(request.getCreatedBy());
        entity.setCreatedTime(request.getCreatedTime());
        entity.setUpdatedBy(request.getUpdatedBy());
        entity.setUpdatedTime(request.getUpdatedTime());
        return entity;
    }

    public MdmCustomer toEntity(MdmCustomerRequest request) {
        MdmCustomer entity = new MdmCustomer();
        entity.setCustomerId(request.getCustomerId());
        entity.setCustomerCode(request.getCustomerCode());
        entity.setCustomerName(request.getCustomerName());
        entity.setContactName(request.getContactName());
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        entity.setVersion(request.getVersion());
        entity.setCreatedBy(request.getCreatedBy());
        entity.setCreatedTime(request.getCreatedTime());
        entity.setUpdatedBy(request.getUpdatedBy());
        entity.setUpdatedTime(request.getUpdatedTime());
        return entity;
    }

    public MdmSupplier toEntity(MdmSupplierRequest request) {
        MdmSupplier entity = new MdmSupplier();
        entity.setSupplierId(request.getSupplierId());
        entity.setSupplierCode(request.getSupplierCode());
        entity.setSupplierName(request.getSupplierName());
        entity.setContactName(request.getContactName());
        entity.setStatus(request.getStatus());
        entity.setRemark(request.getRemark());
        entity.setVersion(request.getVersion());
        entity.setCreatedBy(request.getCreatedBy());
        entity.setCreatedTime(request.getCreatedTime());
        entity.setUpdatedBy(request.getUpdatedBy());
        entity.setUpdatedTime(request.getUpdatedTime());
        return entity;
    }

    public OrgEmployeeView toView(OrgEmployee entity) {
        if (entity == null) return null;
        OrgEmployeeView view = new OrgEmployeeView();
        view.setEmployeeId(entity.getEmployeeId());
        view.setEmployeeNo(entity.getEmployeeNo());
        view.setEmployeeName(entity.getEmployeeName());
        view.setDepartmentId(entity.getDepartmentId());
        view.setPositionName(entity.getPositionName());
        view.setSkillLevel(entity.getSkillLevel());
        view.setPhone(safeDisplay(SensitivePhoneField.EMPLOYEE, entity.getPhone()));
        view.setStatus(entity.getStatus());
        view.setRemark(entity.getRemark());
        copyAudit(entity, view);
        return view;
    }

    public MdmCustomerView toView(MdmCustomer entity) {
        if (entity == null) return null;
        MdmCustomerView view = new MdmCustomerView();
        view.setCustomerId(entity.getCustomerId());
        view.setCustomerCode(entity.getCustomerCode());
        view.setCustomerName(entity.getCustomerName());
        view.setContactName(entity.getContactName());
        view.setContactPhone(safeDisplay(SensitivePhoneField.CUSTOMER, entity.getContactPhone()));
        view.setStatus(entity.getStatus());
        view.setRemark(entity.getRemark());
        view.setVersion(entity.getVersion());
        view.setCreatedBy(entity.getCreatedBy());
        view.setCreatedTime(entity.getCreatedTime());
        view.setUpdatedBy(entity.getUpdatedBy());
        view.setUpdatedTime(entity.getUpdatedTime());
        view.setDeletedFlag(entity.getDeletedFlag());
        return view;
    }

    public MdmSupplierView toView(MdmSupplier entity) {
        if (entity == null) return null;
        MdmSupplierView view = new MdmSupplierView();
        view.setSupplierId(entity.getSupplierId());
        view.setSupplierCode(entity.getSupplierCode());
        view.setSupplierName(entity.getSupplierName());
        view.setContactName(entity.getContactName());
        view.setContactPhone(safeDisplay(SensitivePhoneField.SUPPLIER, entity.getContactPhone()));
        view.setStatus(entity.getStatus());
        view.setRemark(entity.getRemark());
        view.setVersion(entity.getVersion());
        view.setCreatedBy(entity.getCreatedBy());
        view.setCreatedTime(entity.getCreatedTime());
        view.setUpdatedBy(entity.getUpdatedBy());
        view.setUpdatedTime(entity.getUpdatedTime());
        view.setDeletedFlag(entity.getDeletedFlag());
        return view;
    }

    private void copyAudit(OrgEmployee source, OrgEmployeeView target) {
        target.setVersion(source.getVersion());
        target.setCreatedBy(source.getCreatedBy());
        target.setCreatedTime(source.getCreatedTime());
        target.setUpdatedBy(source.getUpdatedBy());
        target.setUpdatedTime(source.getUpdatedTime());
        target.setDeletedFlag(source.getDeletedFlag());
    }

    private String safeDisplay(SensitivePhoneField field, String storedValue) {
        try {
            return phoneService.display(field, storedValue);
        } catch (SensitivePhoneCryptoException e) {
            return null;
        }
    }
}
