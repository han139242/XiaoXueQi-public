import { defineResource, field, optionSets, values } from './helpers'

const enable = { type: 'select', options: optionSets.enable, defaultValue: 'ENABLE' }

export default [
  defineResource({
    key: 'sys-user', domain: 'sys', route: 'sys/users', title: '用户管理', icon: 'users', baseUrl: '/api/sys/user', idField: 'userId',
    description: '维护系统账号、员工绑定、账号状态和角色授权',
    fields: ['username', 'passwordHash', 'employeeId', 'status', 'lastLoginTime', 'remark', 'createdTime'],
    filters: ['keyword', 'status'], defaults: { status: 'ENABLE' },
    overrides: {
      username: { required: true, width: 150 },
      passwordHash: { label: '初始密码', type: 'password', requiredOnCreate: true, table: false, detail: false, edit: false },
      status: { type: 'select', options: optionSets.userStatus },
      lastLoginTime: { form: false, width: 170 }, createdTime: { form: false, width: 170 }
    },
    actions: [
      {
        key: 'reset-password', label: '重置密码', icon: 'key', method: 'put', type: 'warning',
        path: (row) => `/api/sys/user/${row.userId}/reset-password`,
        fields: [field('password', { label: '新密码', type: 'password', required: true, minLength: 6 })],
        payload: (values) => ({ password: values.password })
      },
      {
        key: 'assign-roles', label: '分配角色', icon: 'users', method: 'put', type: 'primary',
        path: (row) => `/api/sys/user/${row.userId}/roles`,
        fields: [field('roleIds', { label: '角色', type: 'reference', reference: 'sys-role', multiple: true, required: true })],
        payload: (values) => ({ roleIds: values.roleIds || [] })
      }
    ]
  }),
  defineResource({
    key: 'sys-role', domain: 'sys', route: 'sys/roles', title: '角色管理', icon: 'shield-check', baseUrl: '/api/sys/role', idField: 'roleId',
    description: '维护角色编码、数据权限范围和启停状态',
    fields: ['roleCode', 'roleName', 'dataScope', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'ENABLE', dataScope: 'ALL' },
    overrides: {
      roleCode: { required: true }, roleName: { required: true },
      dataScope: { type: 'select', required: true, options: values([['ALL', '全部数据', 'primary'], ['DEPARTMENT', '本部门', 'success'], ['TEAM', '本班组', 'warning'], ['SELF', '仅本人', 'info']]) },
      status: enable, createdTime: { form: false, width: 170 }
    },
    actions: [{
      key: 'assign-menus', label: '分配权限', icon: 'shield-check', method: 'put', type: 'primary',
      path: (row) => `/api/sys/role/${row.roleId}/menus`,
      fields: [field('menuIds', { label: '菜单与操作权限', type: 'reference', reference: 'sys-menu', multiple: true, required: true })],
      preload: {
        path: (row) => `/api/sys/role-menu/role/${row.roleId}`,
        map: (relations) => ({ menuIds: (Array.isArray(relations) ? relations : []).map((item) => item.menuId) })
      },
      payload: (values) => ({ menuIds: values.menuIds || [] })
    }]
  }),
  defineResource({
    key: 'sys-menu', domain: 'sys', route: 'sys/menus', title: '菜单管理', icon: 'menu', baseUrl: '/api/sys/menu', idField: 'menuId', treeEndpoint: '/api/sys/menu/tree',
    description: '以树形结构维护目录、菜单、按钮和权限标识',
    fields: ['parentId', 'menuName', 'menuType', 'permissionCode', 'routePath', 'remark', 'createdTime'], filters: ['keyword', 'menuType'],
    overrides: {
      parentId: { type: 'reference', reference: 'sys-menu' }, menuName: { required: true },
      menuType: { type: 'select', required: true, options: values([['DIRECTORY', '目录', 'info'], ['MENU', '菜单', 'primary'], ['BUTTON', '按钮', 'warning'], ['API', '接口', 'success']]) },
      createdTime: { form: false, width: 170 }
    }
  }),
  defineResource({
    key: 'sys-dict-type', domain: 'sys', route: 'sys/dict-types', title: '字典类型', icon: 'database', baseUrl: '/api/sys/dict-type', idField: 'dictTypeId',
    fields: ['dictTypeCode', 'dictTypeName', 'status', 'remark', 'createdTime'], filters: ['keyword', 'status'], defaults: { status: 'ENABLE' },
    overrides: { dictTypeCode: { required: true }, dictTypeName: { required: true }, status: enable, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'sys-dict-data', domain: 'sys', route: 'sys/dict-data', title: '字典数据', icon: 'database', baseUrl: '/api/sys/dict-data', idField: 'dictDataId',
    fields: ['dictTypeCode', 'dictValue', 'dictLabel', 'sortNo', 'remark', 'createdTime'], filters: ['keyword', 'dictTypeCode'],
    overrides: { dictTypeCode: { required: true }, dictValue: { required: true }, dictLabel: { required: true }, sortNo: { type: 'number', min: 0 }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'sys-system-param', domain: 'sys', route: 'sys/parameters', title: '系统参数', icon: 'settings', baseUrl: '/api/sys/system-param', idField: 'paramId',
    fields: ['paramKey', 'paramName', 'paramValue', 'moduleCode', 'remark', 'updatedTime'], filters: ['keyword', 'moduleCode'],
    overrides: { paramKey: { required: true }, paramName: { required: true }, paramValue: { required: true, type: 'textarea' }, updatedTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'sys-file', domain: 'sys', route: 'sys/files', title: '附件文件', icon: 'package', baseUrl: '/api/sys/file', idField: 'fileId',
    fields: ['fileName', 'fileUrl', 'fileType', 'bizType', 'bizId', 'remark', 'createdTime'], filters: ['keyword', 'bizType', 'bizId', 'fileType'],
    overrides: { fileName: { required: true }, fileUrl: { required: true }, createdTime: { form: false, width: 170 } },
    globalActions: [{ key: 'upload', label: '上传文件', icon: 'upload', type: 'primary', kind: 'upload', url: '/api/sys/file/upload' }],
    actions: [{ key: 'download', label: '下载', icon: 'download', type: 'primary', kind: 'download', path: (row) => `/api/sys/file/download/${row.fileId}` }]
  }),
  defineResource({
    key: 'sys-operation-log', domain: 'sys', route: 'sys/operation-logs', title: '操作日志', icon: 'activity', baseUrl: '/api/sys/operation-log', idField: 'logId',
    fields: ['userId', 'operationType', 'moduleCode', 'bizId', 'requestIp', 'operationTime', 'remark'], filters: ['userId', 'operationType', 'moduleCode', 'bizId'],
    permissions: { create: false, edit: false, delete: false, export: true },
    overrides: { operationTime: { form: false, width: 170 }, remark: { form: false } }
  }),
  defineResource({
    key: 'sys-status-change-log', domain: 'sys', route: 'sys/status-logs', title: '状态变更日志', icon: 'route', baseUrl: '/api/sys/status-change-log', idField: 'logId',
    fields: ['bizType', 'bizId', 'oldStatus', 'newStatus', 'changeReason', 'changedBy', 'changedTime', 'remark'], filters: ['bizType', 'bizId', 'oldStatus', 'newStatus', 'changedBy'],
    permissions: { create: false, edit: false, delete: false, export: true },
    overrides: { changedTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'sys-approval-record', domain: 'sys', route: 'sys/approvals', title: '审批记录', icon: 'check', baseUrl: '/api/sys/approval-record', idField: 'approvalId',
    fields: ['bizType', 'bizId', 'approvalNode', 'approverId', 'approvalResult', 'approvalComment', 'approvalTime', 'remark'], filters: ['bizType', 'bizId', 'approvalNode', 'approverId', 'approvalResult'],
    overrides: { approvalResult: { type: 'select', options: optionSets.approval }, approvalTime: { width: 170 } }
  }),
  defineResource({ key: 'sys-user-role', domain: 'sys', route: 'sys/user-roles', title: '用户角色关联', baseUrl: '/api/sys/user-role', idField: 'id', fields: ['userId', 'roleId', 'assignedTime'], filters: ['userId', 'roleId'], permissions: { export: false }, menu: false }),
  defineResource({ key: 'sys-role-menu', domain: 'sys', route: 'sys/role-menus', title: '角色菜单关联', baseUrl: '/api/sys/role-menu', idField: 'id', fields: ['roleId', 'menuId'], filters: ['roleId', 'menuId'], permissions: { export: false }, menu: false })
]
