package com.neuedu.ems.security.phone;

/**
 * 受保护电话字段的表列上下文和字段级权限。
 */
public enum SensitivePhoneField {
    EMPLOYEE("org_employee", "phone", "org:employee:phone:view-full", "org:employee:phone:write"),
    CUSTOMER("mdm_customer", "contact_phone", "mdm:customer:contact-phone:view-full", "mdm:customer:contact-phone:write"),
    SUPPLIER("mdm_supplier", "contact_phone", "mdm:supplier:contact-phone:view-full", "mdm:supplier:contact-phone:write");

    private final String tableName;
    private final String columnName;
    private final String fullViewPermission;
    private final String writePermission;

    SensitivePhoneField(String tableName, String columnName, String fullViewPermission, String writePermission) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.fullViewPermission = fullViewPermission;
        this.writePermission = writePermission;
    }

    public String aad() {
        return "mes:" + tableName + ":" + columnName;
    }

    public String getFullViewPermission() {
        return fullViewPermission;
    }

    public String getWritePermission() {
        return writePermission;
    }
}
