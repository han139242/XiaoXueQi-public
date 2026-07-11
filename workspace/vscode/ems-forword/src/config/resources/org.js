import { defineResource, optionSets, values } from './helpers'

export default [
  defineResource({
    key: 'org-department', domain: 'org', route: 'org/departments', title: '部门管理', icon: 'factory', baseUrl: '/api/org/department', idField: 'departmentId', treeEndpoint: '/api/org/department/tree',
    fields: ['parentId', 'departmentCode', 'departmentName', 'managerId', 'remark', 'createdTime'], filters: ['keyword'],
    overrides: { parentId: { type: 'reference', reference: 'org-department' }, departmentCode: { required: true }, departmentName: { required: true }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'org-employee', domain: 'org', route: 'org/employees', title: '员工管理', icon: 'user', baseUrl: '/api/org/employee', idField: 'employeeId',
    fields: ['employeeNo', 'employeeName', 'departmentId', 'positionName', 'skillLevel', 'phone', 'status', 'remark', 'createdTime'], filters: ['keyword', 'departmentId', 'status'], defaults: { status: 'ENABLE' },
    overrides: {
      employeeNo: { required: true }, employeeName: { required: true }, departmentId: { required: true },
      skillLevel: { type: 'select', options: values([['BEGINNER', '初级', 'info'], ['INTERMEDIATE', '中级', 'primary'], ['ADVANCED', '高级', 'success'], ['EXPERT', '专家', 'warning']]) },
      status: { type: 'select', options: optionSets.employeeStatus }, createdTime: { form: false, width: 170 }
    }
  }),
  defineResource({
    key: 'org-team', domain: 'org', route: 'org/teams', title: '班组管理', icon: 'users', baseUrl: '/api/org/team', idField: 'teamId',
    fields: ['teamCode', 'teamName', 'departmentId', 'leaderId', 'remark', 'createdTime'], filters: ['keyword', 'departmentId', 'leaderId'],
    overrides: { teamCode: { required: true }, teamName: { required: true }, departmentId: { required: true }, createdTime: { form: false, width: 170 } },
    relations: [{ key: 'members', title: '班组成员', resource: 'org-team-member', parentField: 'teamId', endpoint: (row) => `/api/org/team-member/team/${row.teamId}` }]
  }),
  defineResource({
    key: 'org-shift', domain: 'org', route: 'org/shifts', title: '班次管理', icon: 'calendar', baseUrl: '/api/org/shift', idField: 'shiftId',
    fields: ['shiftCode', 'shiftName', 'startTime', 'endTime', 'remark', 'createdTime'], filters: ['keyword'],
    overrides: { shiftCode: { required: true }, shiftName: { required: true }, startTime: { required: true, type: 'datetime' }, endTime: { required: true, type: 'datetime' }, createdTime: { form: false, width: 170 } }
  }),
  defineResource({
    key: 'org-team-member', domain: 'org', route: 'org/team-members', title: '班组成员', baseUrl: '/api/org/team-member', idField: 'id', permissions: { export: false }, menu: false,
    fields: ['teamId', 'employeeId', 'joinDate', 'leaveDate', 'status'], filters: ['teamId', 'employeeId', 'status'], defaults: { status: 'ENABLE' },
    overrides: { teamId: { required: true }, employeeId: { required: true }, joinDate: { required: true }, status: { type: 'select', options: optionSets.enable } }
  })
]
