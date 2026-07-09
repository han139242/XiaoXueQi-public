# 羽毛球 MES 系统数据库表结构分析

> 本文按照老师参考文档的写法整理：先从系统功能和用例出发，再说明每个实体为什么需要保存数据。  
> 参考文档仅作为格式和 MES 拆表思路参考，本文不照抄参考案例，而是面向羽毛球生产 MES 重新规范设计。

## 1. 设计依据

### 1.1 业务主线

羽毛球 MES 的核心业务链路如下：

```text
客户/ERP生产需求
  -> 生产订单
  -> 生产计划
  -> 生产工单
  -> 派工单
  -> 生产任务
  -> 生产领料
  -> 工序执行
  -> 生产报工
  -> 质量检验
  -> 成品入库
  -> 条码追溯
```

横向支撑数据包括：

```text
人员组织、权限、物料、产品、产品物料清单、工艺路线、作业指导书、车间产线工位、仓库库位、设备台账、异常记录、审批和日志
```

### 1.2 对原用例的合理化处理

1. “创建生产工单”用例在报告中有不同视角。数据库中统一拆为：`mfg_production_order`、`mfg_production_plan`、`mfg_work_order`、`mfg_dispatch_order`、`mfg_task`，避免订单、工单、任务概念混用。
2. “处理设备异常”和“安灯告警”相关功能统一联动。现场快速呼叫保存在 `mfg_andon_alert`，形成业务异常保存在 `mfg_exception`，需要维修时进入 `eam_repair_order`。
3. “报工生产数据”不只保存合格数和不良数，还要保存任务、工序、工位、人员、实际工时、审核状态和物料消耗，因此拆为 `mfg_work_report` 和 `mfg_report_material`。
4. “检验生产产品”统一进入质量模块。质检主单保存在 `qms_inspection_order`，检验项实测结果保存在 `qms_inspection_result`，不合格处理保存在 `qms_nonconforming_order` 和 `qms_nonconforming_detail`。
5. “生成条码”和“产品追溯”不是单独重复保存所有业务数据，而是通过 `trace_barcode`、`trace_product_lot`、`trace_material_usage` 关联生产、仓储和质量记录。其中 `trace_material_usage.used_quantity` 属于面向追溯查询的**合理冗余**（与 `mfg_report_material` 存在字段重叠），目的是让扫码追溯一次命中，不必回溯多张报工表；冗余字段以报工消耗为准，不作为库存扣减依据。
6. “发起补货建议”本质属于库存和生产需求联动，设计为 `wms_replenishment_suggestion`，由库存预警、产品物料清单需求和生产计划共同触发。
7. **两类“批次”概念区分**：`wms_material_batch` 是采购/入库批次（面向原料来料与库存追溯），`trace_product_lot` 是生产批次（面向成品/半成品产出与质检）。成品完工入库时，由 `trace_product_lot` 生成成品的 `wms_material_batch`，二者通过物料 ID 与批次号建立对应关系，不混用。

### 1.3 表数量控制

最终核心版控制为 **78 张表**：

| 模块 | 表数量 |
|---|---:|
| 系统与通用支撑 | 12 |
| 组织人员 | 5 |
| 基础档案 | 8 |
| 工艺管理 | 5 |
| 工厂建模 | 4 |
| 生产计划与执行 | 11 |
| 仓储库存 | 14 |
| 质量管理 | 8 |
| 设备管理 | 8 |
| 条码追溯 | 3 |
| 合计 | 78 |

计件工资、安全培训、复杂 ERP/财务同步先不进入核心表，后续可在此基础上扩展。

## 2. 统一规范

> 本章为企业级落地规范，所有建表脚本和字段设计都必须遵循，用于保证数据一致性、可追溯性和并发安全。

### 2.1 命名规范

- 表名统一使用英文小写下划线，单词全拼、见名知义，避免缩写歧义。
- 表名按模块加统一前缀：
  - 系统表使用 `sys_` 前缀。
  - 组织人员表使用 `org_` 前缀。
  - 主数据表使用 `mdm_` 前缀。
  - 工艺表使用 `process_` 前缀。
  - 工厂资源表使用 `factory_` 前缀。
  - 生产表使用 `mfg_` 前缀。
  - 仓储表使用 `wms_` 前缀。
  - 质量表使用 `qms_` 前缀。
  - 设备表使用 `eam_` 前缀。
  - 追溯表使用 `trace_` 前缀。
- 主从结构统一用 `_order`（主表）与 `_detail`/`_item`（明细表）区分，如 `wms_inbound_order` 与 `wms_inbound_detail`。
- 索引与约束命名统一前缀，便于运维识别：
  - 主键：`pk_表名`。
  - 唯一索引：`uk_表名_字段`。
  - 普通索引：`idx_表名_字段`。
  - 外键（如启用物理外键）：`fk_从表_主表`。

### 2.2 主键与通用字段规范

主键规范：

- 每张表必须有单一主键，类型统一为 `BIGINT UNSIGNED`，采用自增或雪花算法生成，禁止使用业务编号作主键。
- 主表主键使用业务语义命名（如 `work_order_id`、`task_id`），关联表和明细表主键统一命名为 `id`，两种写法在本文档中含义一致，均指该表主键。
- 业务编号（如 `order_no`、`work_order_no`、`task_no`）是给人看和跨系统对账用的，必须建唯一索引，但不承担主键职责。

通用字段规范：除纯关联表（如 `sys_user_role`）和纯日志表可酌情精简外，所有业务表都在各自“核心属性”之外**统一自动附加**下列字段，无需在每张表逐条重复列出。这些字段按用途分为四类：

**（1）业务字段**

- `code` 或业务编号字段：如 `order_no`、`work_order_no`、`task_no`，唯一。
- `status`：状态字段，取值统一引用 `sys_dict_data` 的字典值，不直接存中文。
- `remark`：备注，`VARCHAR(500)`。

**（2）审计字段——记录“谁在什么时候创建和修改了这条数据”**

用于生产和质量的责任追溯，任何单据出问题都能查到经手人和时间。

- `created_by`：创建人（用户 ID），`BIGINT UNSIGNED`。
- `created_time`：创建时间，`DATETIME`。
- `updated_by`：最后更新人（用户 ID），`BIGINT UNSIGNED`。
- `updated_time`：最后更新时间，`DATETIME`。

**（3）软删除字段——逻辑删除，不物理删除数据**

生产数据不允许真正从库里删掉（否则追溯链断裂、审计无据），所谓删除只是把标记置 1，查询时默认过滤，数据仍保留可追溯。

- `deleted_flag`：软删除标记，`TINYINT` 默认 0（0 未删、1 已删）。

**（4）乐观锁字段——防止并发更新互相覆盖**

多人同时更新同一条记录（如同时扣同一批库存）时，靠版本号在提交时比对，版本不一致则拒绝并重试，避免超扣或数量被覆盖。库存、工单、任务等高并发表必须启用。

- `version`：乐观锁版本号，`INT` 默认 0，每次更新自动加 1。

> 建表提示：AI 生成 DDL 时，对每张业务表都要在其核心字段之后自动补齐上述审计、软删除、乐观锁字段；`status`/`code`/`remark` 若已在核心属性中出现则不重复添加。纯关联表（多对多中间表）保留文档中列出的全部核心字段，通用字段只追加 `created_by`、`created_time`、`deleted_flag`，不追加 `version`、`remark`、`updated_by`、`updated_time`，详见第 16.3 节。

### 2.3 数据类型规范

为避免各表字段类型不统一导致的精度丢失和查询问题，统一约定：

| 数据类别 | 统一类型 | 说明 |
|---|---|---|
| 主键、外键 ID | `BIGINT UNSIGNED` | 全库统一，禁止用 `INT` 存主键 |
| 业务编号 code/no | `VARCHAR(64)` | 唯一索引 |
| 名称、标题 | `VARCHAR(128)` | |
| 备注、描述 | `VARCHAR(500)` / `TEXT` | 长文本用 `TEXT` |
| 状态、类型枚举 | `VARCHAR(32)` | 存 `sys_dict_data` 字典值，不存中文文本 |
| 数量、工时、用量 | `DECIMAL(18,4)` | 避免浮点误差 |
| 金额、成本、费用 | `DECIMAL(18,2)` | |
| 比率（损耗率、预警比例） | `DECIMAL(9,4)` | |
| 是否类布尔标记 | `TINYINT` | 0 否 / 1 是 |
| 日期 | `DATE` | 仅日期 |
| 时间戳 | `DATETIME` | 精确到秒，必要时 `DATETIME(3)` |
| 附件 | 存 `file_id` 关联 `sys_file` | 业务表不直接存文件二进制或长 URL |

### 2.4 约束与索引规范

- 所有业务编号（`_no`、`_code`）建唯一索引。
- 高频关联字段（如 `work_order_id`、`task_id`、`material_id`、`batch_id`）建普通索引。
- 涉及“唯一存在性”的表必须建复合唯一约束，例如：
  - `wms_inventory`：`(warehouse_id, location_id, material_id, batch_id)` 唯一，保证同一库位同一批次库存只有一条余额记录。
  - `sys_user_role`：`(user_id, role_id)` 唯一。
  - `factory_station_operation`：`(station_id, operation_id)` 唯一。
- 外键策略：MES 现场并发高、单据量大，采用**逻辑外键**（关系由应用层和本文档约束保证），不默认创建物理外键，避免高并发下的锁竞争；关键主从单据可按需启用物理外键。
- 字符集与存储引擎统一：字符集 `utf8mb4`、排序规则 `utf8mb4_general_ci`、存储引擎 `InnoDB`，保证事务、行锁和中文/emoji 兼容。

### 2.5 数据完整性与并发规范

- 数量、重量、工时等字段使用 `DECIMAL`，避免浮点误差。
- 余额表和流水表必须同时存在，例如 `wms_inventory`（当前有多少）与 `wms_stock_movement`（怎么变动的），余额不允许脱离流水直接修改。
- 库存扣减、工单/任务数量回写等并发更新，统一使用 `version` 乐观锁，防止超扣和数量覆盖。
- 状态变化要进入 `sys_status_change_log`，不能只覆盖当前状态。
- 审批过程要进入 `sys_approval_record`，不能只在业务表上放一个审核人字段。
- 附件统一进入 `sys_file`，业务表只保存文件 ID 或关联关系。
- 关键操作（下达、报工、出入库、审批）必须写 `sys_operation_log`，保证审计可追溯。

## 3. 系统与通用支撑

### 1. sys_user 用户表

实体作用：保存系统登录账号信息，包括管理员、生产计划员、车间主任、操作工、仓库管理员、质检员、设备管理员等。

为什么要有它：PC 端和微信小程序都需要登录认证，系统必须知道当前操作人是谁，并把操作行为追溯到具体账号。

核心属性及业务含义：
- `user_id`：用户主键。
- `username`：登录账号，唯一。
- `password_hash`：加密后的密码。
- `employee_id`：关联员工档案。
- `status`：账号状态，如正常、停用、锁定。
- `last_login_time`：最后登录时间。

对应功能/用例：登录认证、权限控制、操作留痕、移动端身份识别。

### 2. sys_role 角色表

实体作用：保存系统角色，例如系统管理员、生产计划员、车间主任、操作工、仓库管理员、质检员、设备管理员、管理层。

为什么要有它：不同岗位能操作的菜单和数据范围不同，角色用于统一授权，避免逐个用户配置权限。

核心属性及业务含义：
- `role_id`：角色主键。
- `role_code`：角色编码，唯一。
- `role_name`：角色名称。
- `data_scope`：数据范围，如全部、部门、本班组、本人。
- `status`：角色状态。

对应功能/用例：权限管理、菜单控制、数据范围控制。

### 3. sys_menu 菜单权限表

实体作用：保存 PC 端菜单、小程序页面、接口权限和按钮权限。

为什么要有它：系统需要控制不同角色能看到哪些页面、能点击哪些按钮，例如审核、下达、报工、质检、入库等。

核心属性及业务含义：
- `menu_id`：菜单主键。
- `parent_id`：上级菜单。
- `menu_name`：菜单名称。
- `menu_type`：目录、菜单、按钮、接口。
- `permission_code`：权限标识。
- `route_path`：前端路由。

对应功能/用例：PC 管理端菜单、小程序功能入口、按钮级权限。

### 4. sys_user_role 用户角色关联表

实体作用：保存用户和角色之间的多对多关系。

为什么要有它：一个员工可能既是设备管理员又是维修人员，一个角色也会分配给多个用户。

核心属性及业务含义：
- `id`：主键。
- `user_id`：用户 ID。
- `role_id`：角色 ID。
- `assigned_time`：授权时间。

对应功能/用例：用户授权、权限变更。

### 5. sys_role_menu 角色菜单关联表

实体作用：保存角色和菜单权限之间的多对多关系。

为什么要有它：系统通过角色控制菜单和按钮访问范围，例如质检员能录入检验结果，但不能审批生产计划。

核心属性及业务含义：
- `id`：主键。
- `role_id`：角色 ID。
- `menu_id`：菜单或按钮 ID。

对应功能/用例：角色授权、菜单权限配置。

### 6. sys_dict_type 字典类型表

实体作用：保存字典分类，例如工单状态、任务状态、库存状态、检验结果、设备状态、异常类型。

为什么要有它：MES 中状态很多，如果各表直接写死状态文本，后期统计和维护会混乱。

核心属性及业务含义：
- `dict_type_id`：字典类型主键。
- `dict_type_code`：字典类型编码，唯一。
- `dict_type_name`：字典类型名称。
- `status`：是否启用。

对应功能/用例：状态统一、下拉选项、流程判断。

### 7. sys_dict_data 字典数据表

实体作用：保存具体字典项，例如工单状态的新建、已下达、生产中、已完成、已关闭。

为什么要有它：各业务表中的状态字段都应使用统一字典值，保证查询、统计和前端显示一致。

核心属性及业务含义：
- `dict_data_id`：字典数据主键。
- `dict_type_code`：所属字典类型。
- `dict_value`：字典值。
- `dict_label`：显示文本。
- `sort_no`：排序号。

对应功能/用例：状态枚举、检验结果、异常类型、库存类型。

### 8. sys_system_param 系统参数表

实体作用：保存系统级配置，例如默认仓库、库存预警比例、条码生成规则开关、报工审核方式。

为什么要有它：企业规则会调整，不能把参数写死在代码里。

核心属性及业务含义：
- `param_id`：参数主键。
- `param_key`：参数键，唯一。
- `param_value`：参数值。
- `param_name`：参数名称。
- `module_code`：所属模块。

对应功能/用例：系统配置、规则配置、运行参数维护。

### 9. sys_file 附件文件表

实体作用：统一保存系统上传文件的元数据，例如作业指导书、质检图片、维修照片、异常附件。

为什么要有它：不同业务都会上传附件，如果每张表都单独设计文件字段，后续维护会混乱。

核心属性及业务含义：
- `file_id`：文件主键。
- `file_name`：原始文件名。
- `file_url`：文件访问地址。
- `file_type`：文件类型。
- `biz_type`：业务类型。
- `biz_id`：关联业务 ID。

对应功能/用例：作业指导书上传、质检拍照、维修附件、异常附件。

### 10. sys_operation_log 操作日志表

实体作用：记录用户关键操作，例如新增、修改、删除、审核、下达、报工、入库、出库。

为什么要有它：MES 数据关系到生产过程和质量追溯，必须知道谁在什么时候做了什么操作。

核心属性及业务含义：
- `log_id`：日志主键。
- `user_id`：操作用户。
- `operation_type`：操作类型。
- `module_code`：模块编码。
- `biz_id`：业务 ID。
- `request_ip`：请求 IP。
- `operation_time`：操作时间。

对应功能/用例：审计追踪、问题排查、责任追溯。

### 11. sys_status_change_log 状态变更日志表

实体作用：记录业务对象状态变化，例如工单从已下达到生产中，质检单从待检到已完成。

为什么要有它：只在业务表保存当前状态无法还原历史过程，状态流转必须留痕。

核心属性及业务含义：
- `log_id`：日志主键。
- `biz_type`：业务类型。
- `biz_id`：业务 ID。
- `old_status`：原状态。
- `new_status`：新状态。
- `change_reason`：变更原因。
- `changed_by`：变更人。
- `changed_time`：变更时间。

对应功能/用例：工单状态流转、任务状态流转、库存状态、设备状态、质检状态。

### 12. sys_approval_record 审批记录表

实体作用：记录各类业务审批过程，例如工单审批、报工审核、出库审核、不合格品处理审批。

为什么要有它：审批不能只保留最后结果，需要记录每一步审批意见和审批人。

核心属性及业务含义：
- `approval_id`：审批记录主键。
- `biz_type`：业务类型。
- `biz_id`：业务 ID。
- `approval_node`：审批节点。
- `approver_id`：审批人。
- `approval_result`：同意、驳回、退回。
- `approval_comment`：审批意见。
- `approval_time`：审批时间。

对应功能/用例：工单审批、报工审核、出入库审核、不合格处理审批。

## 4. 组织人员

### 13. org_department 部门表

实体作用：保存企业组织部门，例如生产部、质量部、仓储部、设备部、工艺部。

为什么要有它：人员、权限、数据范围和报表统计都需要按部门归属管理。

核心属性及业务含义：
- `department_id`：部门主键。
- `parent_id`：上级部门。
- `department_code`：部门编码。
- `department_name`：部门名称。
- `manager_id`：负责人。

对应功能/用例：组织管理、人员管理、数据权限。

### 14. org_employee 员工表

实体作用：保存所有业务人员档案，包括操作工、质检员、仓库管理员、维修人员、班组长等。

为什么要有它：生产任务、报工、质检、维修、审批都需要落到具体员工。

核心属性及业务含义：
- `employee_id`：员工主键。
- `employee_no`：工号，唯一。
- `employee_name`：姓名。
- `department_id`：所属部门。
- `position_name`：岗位名称。
- `skill_level`：技能等级。
- `phone`：联系方式。
- `status`：在职、离职、停用。

对应功能/用例：报工、质检、设备维修、审批、人员档案。

### 15. org_team 班组表

实体作用：保存生产班组信息，例如植毛一班、包装二班、质检班组。

为什么要有它：派工单通常下达到班组，班组长负责接收任务和审核报工。

核心属性及业务含义：
- `team_id`：班组主键。
- `team_code`：班组编码。
- `team_name`：班组名称。
- `leader_id`：班组长。
- `department_id`：所属部门。

对应功能/用例：派工、任务分配、班组统计。

### 16. org_team_member 班组成员表

实体作用：保存员工与班组之间的关系。

为什么要有它：一个班组有多个成员，员工也可能因排班调整进入不同班组。

核心属性及业务含义：
- `id`：主键。
- `team_id`：班组 ID。
- `employee_id`：员工 ID。
- `join_date`：加入日期。
- `leave_date`：离开日期。
- `status`：是否有效。

对应功能/用例：任务人员分配、班组报表。

### 17. org_shift 班次表

实体作用：保存班次定义，例如白班、夜班、早班、中班。

为什么要有它：生产任务、报工、设备点检和产量统计需要按班次归集。

核心属性及业务含义：
- `shift_id`：班次主键。
- `shift_code`：班次编码。
- `shift_name`：班次名称。
- `start_time`：开始时间。
- `end_time`：结束时间。

对应功能/用例：生产排班、报工统计、设备点检统计。

## 5. 基础档案

### 18. mdm_unit 计量单位表

实体作用：保存数量单位，例如个、根、克、瓶、箱、筒。

为什么要有它：物料、库存、产品物料清单、报工和质检都需要统一单位，避免数量含义不一致。

核心属性及业务含义：
- `unit_id`：单位主键。
- `unit_code`：单位编码。
- `unit_name`：单位名称。
- `precision_scale`：小数精度。
- `status`：是否启用。

对应功能/用例：物料档案、产品物料清单、库存、报工、质检。

### 19. mdm_supplier 供应商表

实体作用：保存原材料供应商档案，例如毛片供应商、球头供应商、胶水供应商。

为什么要有它：原料批次追溯需要知道物料来自哪家供应商，质量问题也需要反查供应来源。

核心属性及业务含义：
- `supplier_id`：供应商主键。
- `supplier_code`：供应商编码。
- `supplier_name`：供应商名称。
- `contact_name`：联系人。
- `contact_phone`：联系电话。
- `status`：合作状态。

对应功能/用例：原材料入库、供应商追溯、质量追溯。

### 20. mdm_customer 客户表

实体作用：保存客户档案。

为什么要有它：生产订单可能来自客户需求或 ERP 销售订单，成品追溯和订单交付需要客户信息。

核心属性及业务含义：
- `customer_id`：客户主键。
- `customer_code`：客户编码。
- `customer_name`：客户名称。
- `contact_name`：联系人。
- `contact_phone`：联系电话。
- `status`：客户状态。

对应功能/用例：生产订单、交付管理、客户追溯。

### 21. mdm_material_category 物料分类表

实体作用：保存物料分类，例如毛片、球头、胶水、线圈、包装材料、半成品、成品。

为什么要有它：物料数量多，需要按分类管理、查询、统计和设置库存策略。

核心属性及业务含义：
- `category_id`：分类主键。
- `parent_id`：上级分类。
- `category_code`：分类编码。
- `category_name`：分类名称。
- `status`：是否启用。

对应功能/用例：物料档案、库存查询、分类统计。

### 22. mdm_material 物料档案表

实体作用：保存所有原材料、辅料、半成品和成品的基础档案。

为什么要有它：仓储库存、生产领料、产品物料清单、质量追溯都必须通过统一物料编码识别。

核心属性及业务含义：
- `material_id`：物料主键。
- `material_code`：物料编码，唯一。
- `material_name`：物料名称。
- `category_id`：物料分类。
- `spec_model`：规格型号。
- `unit_id`：计量单位。
- `material_type`：原料、半成品、成品、辅料。
- `status`：正常、停用。

对应功能/用例：物料管理、入库、领料、产品物料清单、追溯。

### 23. mdm_product 产品档案表

实体作用：保存羽毛球产品型号档案，例如训练球、比赛球、耐打型羽毛球等。

为什么要有它：生产订单、工艺路线、产品物料清单和质检标准都需要关联具体产品。

核心属性及业务含义：
- `product_id`：产品主键。
- `product_code`：产品编码。
- `product_name`：产品名称。
- `product_model`：产品型号。
- `material_id`：对应成品物料 ID。
- `status`：产品状态。

对应功能/用例：生产订单、产品物料清单、工艺路线、成品质检。

### 24. mdm_bom 产品物料清单主表

实体作用：保存产品物料清单的主信息。

为什么要有它：生产某款羽毛球需要哪些材料、用量多少，必须由产品物料清单提供标准依据。

核心属性及业务含义：
- `bom_id`：产品物料清单主键。
- `bom_code`：产品物料清单编码。
- `product_id`：关联产品。
- `bom_version`：版本号。
- `effective_date`：生效日期。
- `status`：草稿、已发布、停用。

对应功能/用例：创建工单、物料需求计算、齐套检查、生产领料。

### 25. mdm_bom_item 产品物料清单明细表

实体作用：保存产品物料清单中每种物料的标准用量。

为什么要有它：产品物料清单主表只说明是哪款产品的清单，明细表才说明生产一个产品具体需要哪些物料和数量。

核心属性及业务含义：
- `bom_item_id`：产品物料清单明细主键。
- `bom_id`：产品物料清单主表 ID。
- `material_id`：所需物料。
- `quantity`：标准用量。
- `loss_rate`：损耗率。
- `sequence_no`：顺序号。

对应功能/用例：工单用料需求、库存预警、生产领料、成本核算。

## 6. 工艺管理

### 26. process_operation 工序表

实体作用：保存羽毛球生产的标准工序，例如毛片分选、球头处理、植毛、粘合、校正、飞行测试、包装。

为什么要有它：生产任务、报工、质检、计件都需要落到具体工序。

核心属性及业务含义：
- `operation_id`：工序主键。
- `operation_code`：工序编码。
- `operation_name`：工序名称。
- `operation_type`：生产、检验、包装。
- `standard_time`：标准工时。
- `status`：是否启用。

对应功能/用例：工艺路线、任务下达、报工、质检。

### 27. process_route 工艺路线表

实体作用：保存某个产品的工艺路线主信息。

为什么要有它：不同羽毛球型号的工序顺序和质量要求不同，需要提前定义生产路径。

核心属性及业务含义：
- `route_id`：工艺路线主键。
- `route_code`：路线编码。
- `route_name`：路线名称。
- `product_id`：关联产品。
- `version`：版本号。
- `status`：草稿、已发布、停用。

对应功能/用例：工单创建、任务拆分、作业指导书查询。

### 28. process_route_item 工艺路线明细表

实体作用：保存工艺路线中的工序顺序。

为什么要有它：路线主表只定义产品路线，明细表定义第几步做什么、在哪类工位做、是否需要质检。

核心属性及业务含义：
- `route_item_id`：明细主键。
- `route_id`：工艺路线 ID。
- `operation_id`：工序 ID。
- `sequence_no`：工序顺序。
- `standard_time`：标准工时。
- `qc_required`：是否需要质检。

对应功能/用例：任务自动拆分、工序流转、工序质检。

### 29. process_parameter 工艺参数表

实体作用：保存工序参数标准，例如胶水用量、固化时间、温度、重量范围、飞行测试标准。

为什么要有它：羽毛球质量高度依赖工艺参数，系统需要记录标准值和执行依据。

核心属性及业务含义：
- `parameter_id`：参数主键。
- `operation_id`：工序 ID。
- `parameter_name`：参数名称。
- `standard_value`：标准值。
- `upper_limit`：上限。
- `lower_limit`：下限。
- `unit_id`：单位。

对应功能/用例：工艺执行检查、质检标准、工艺参数优化。

### 30. process_work_instruction 作业指导书表

实体作用：保存工序对应的作业指导书信息。

为什么要有它：操作工需要在现场查看当前有效的工艺说明、图片、视频或文件，保证按标准生产。

核心属性及业务含义：
- `instruction_id`：指导书主键。
- `operation_id`：工序 ID。
- `product_id`：产品 ID。
- `version`：版本号。
- `title`：指导书标题。
- `file_id`：附件文件 ID。
- `effective_time`：生效时间。
- `status`：草稿、已发布、停用。

对应功能/用例：作业指导书查询、工艺文件发布、现场操作指导。

## 7. 工厂建模

### 31. factory_workshop 车间表

实体作用：保存车间档案，例如成型车间、包装车间、质检车间。

为什么要有它：生产计划、设备、产线和人员都需要归属到具体车间。

核心属性及业务含义：
- `workshop_id`：车间主键。
- `workshop_code`：车间编码。
- `workshop_name`：车间名称。
- `manager_id`：负责人。
- `status`：运行、停用。

对应功能/用例：生产计划、车间看板、设备管理。

### 32. factory_line 产线表

实体作用：保存产线档案。

为什么要有它：生产计划通常安排到产线，产线产能会影响排产结果。

核心属性及业务含义：
- `line_id`：产线主键。
- `line_code`：产线编码。
- `line_name`：产线名称。
- `workshop_id`：所属车间。
- `daily_capacity`：日产能。
- `status`：运行、停线、维护。

对应功能/用例：生产计划、排产、任务下达、看板统计。

### 33. factory_station 工位表

实体作用：保存工位档案，例如植毛工位、粘合工位、飞行测试工位、包装工位。

为什么要有它：任务和报工需要落到具体工位，设备和工序也需要绑定工位。

核心属性及业务含义：
- `station_id`：工位主键。
- `station_code`：工位编码。
- `station_name`：工位名称。
- `line_id`：所属产线。
- `station_type`：生产、检验、包装。
- `status`：运行、停用、维护。

对应功能/用例：任务执行、报工、设备绑定、安灯告警。

### 34. factory_station_operation 工位工序关联表

实体作用：保存工位可以执行哪些工序。

为什么要有它：不是所有工位都能做所有工序，任务派发时需要校验工位能力。

核心属性及业务含义：
- `id`：主键。
- `station_id`：工位 ID。
- `operation_id`：工序 ID。
- `is_default`：是否默认工位。
- `status`：是否有效。

对应功能/用例：任务派发、工位能力校验、工艺路线执行。

## 8. 生产计划与执行

### 35. mfg_production_order 生产订单表

实体作用：保存生产需求来源，说明要生产什么产品、生产多少、什么时候交付。

为什么要有它：生产必须有来源，生产订单是后续计划、工单、任务和追溯的起点。

核心属性及业务含义：
- `production_order_id`：生产订单主键。
- `production_order_no`：生产订单编号。
- `customer_id`：客户 ID。
- `product_id`：产品 ID。
- `order_quantity`：订单数量。
- `delivery_date`：交付日期。
- `source_type`：ERP 同步、手工创建。
- `status`：新建、已排产、生产中、已完成、已关闭。

对应功能/用例：订单转工单、生产计划、订单进度跟踪。

### 36. mfg_production_plan 生产计划表

实体作用：保存生产主管根据订单、产能和库存制定的生产安排。

为什么要有它：订单说明要做多少，计划说明怎么安排做、在哪条产线做、什么时候做。

核心属性及业务含义：
- `plan_id`：计划主键。
- `plan_no`：计划编号。
- `production_order_id`：关联生产订单。
- `line_id`：计划产线。
- `planned_start_time`：计划开始时间。
- `planned_end_time`：计划结束时间。
- `planned_quantity`：计划数量。
- `status`：待生效、已生效、已完成、已取消。

对应功能/用例：生产排程、计划管理、工单生成。

### 37. mfg_work_order 生产工单表

实体作用：保存下达到车间执行的生产工单。

为什么要有它：工单是连接计划和现场执行的核心单据，后续任务、报工、质检和入库都要关联工单。

核心属性及业务含义：
- `work_order_id`：工单主键。
- `work_order_no`：工单编号，唯一。
- `plan_id`：生产计划 ID。
- `product_id`：产品 ID。
- `bom_id`：使用的产品物料清单。
- `route_id`：使用的工艺路线。
- `planned_quantity`：计划生产数量。
- `completed_quantity`：完成数量。
- `status`：新建、已审核、已下达、生产中、已完成、已关闭。

对应功能/用例：创建生产工单、工单审批下达、生产任务拆分。

### 38. mfg_work_order_material 工单用料需求表

实体作用：保存工单根据产品物料清单计算出的物料需求。

为什么要有它：生产前必须知道需要哪些物料、需要多少、库存是否齐套。

核心属性及业务含义：
- `requirement_id`：需求主键。
- `work_order_id`：工单 ID。
- `material_id`：物料 ID。
- `required_quantity`：需求数量。
- `issued_quantity`：已发料数量。
- `shortage_quantity`：缺料数量。
- `status`：待齐套、部分齐套、已齐套。

对应功能/用例：物料齐套检查、生产领料、补货建议。

### 39. mfg_dispatch_order 派工单表

实体作用：保存从工单拆分并下达到班组的派工信息。

为什么要有它：工单通常是车间级或批次级，班组实际执行需要更细的派工单。

核心属性及业务含义：
- `dispatch_id`：派工单主键。
- `dispatch_no`：派工单编号。
- `work_order_id`：工单 ID。
- `team_id`：派工班组。
- `planned_quantity`：派工数量。
- `due_time`：要求完成时间。
- `material_ready_status`：物料齐套状态。
- `status`：待派发、已派发、已接受、生产中、已完成、已驳回。

对应功能/用例：生产任务下达、班组接单、车间管理。

### 40. mfg_task 生产任务单表

实体作用：保存具体工序和工位上的生产任务。

为什么要有它：操作工不是直接执行工单，而是执行某工序、某工位上的任务单。

核心属性及业务含义：
- `task_id`：任务主键。
- `task_no`：任务编号。
- `dispatch_id`：派工单 ID。
- `work_order_id`：工单 ID。
- `operation_id`：工序 ID。
- `station_id`：工位 ID。
- `planned_quantity`：计划数量。
- `completed_quantity`：完成数量。
- `defect_quantity`：不良数量。
- `status`：待开工、生产中、暂停、已完工、已关闭。

对应功能/用例：现场任务查看、生产报工、工序质检。

### 41. mfg_task_operator 任务人员分配表

实体作用：保存任务分配给哪些操作工。

为什么要有它：一个任务可能由多人协作完成，后续报工和计件需要知道人员分配关系。

核心属性及业务含义：
- `id`：主键。
- `task_id`：任务 ID。
- `employee_id`：员工 ID。
- `role_in_task`：任务角色，如主操作、协助。
- `assigned_time`：分配时间。

对应功能/用例：任务派发、操作工任务列表、报工人员确认。

### 42. mfg_work_report 生产报工表

实体作用：保存操作工完成任务后的报工数据。

为什么要有它：生产进度、合格数量、不良数量、实际工时和报工审核都依赖报工记录。

核心属性及业务含义：
- `report_id`：报工主键。
- `report_no`：报工编号。
- `task_id`：任务 ID。
- `operator_id`：操作工 ID。
- `station_id`：工位 ID。
- `qualified_quantity`：合格数量。
- `defect_quantity`：不良数量。
- `actual_work_hours`：实际工时。
- `report_time`：报工时间。
- `audit_status`：待审核、通过、驳回。

对应功能/用例：报工生产数据、生产进度同步、班组长审核。

### 43. mfg_report_material 报工用料消耗表

实体作用：保存报工过程中实际消耗的物料批次和数量。

为什么要有它：实际生产消耗可能与产品物料清单理论用量不同，必须记录真实用料，支撑成本和追溯。

核心属性及业务含义：
- `id`：主键。
- `report_id`：报工 ID。
- `material_id`：物料 ID。
- `batch_id`：物料批次 ID。
- `used_quantity`：消耗数量。
- `scrap_quantity`：损耗数量。

对应功能/用例：生产报工、物料追溯、成本核算、库存扣减。

### 44. mfg_exception 生产异常表

实体作用：保存生产过程中出现的业务异常，例如缺料、质量异常、工艺异常、设备异常。

为什么要有它：异常会影响任务进度、质量和交期，需要记录发现、处理和关闭全过程。

核心属性及业务含义：
- `exception_id`：异常主键。
- `exception_no`：异常编号。
- `exception_type`：异常类型。
- `source_alert_id`：来源安灯告警 ID（现场安灯升级为业务异常时关联，可空）。
- `task_id`：关联任务。
- `station_id`：关联工位。
- `reported_by`：上报人。
- `description`：异常描述。
- `handle_result`：处理结果。
- `status`：待处理、处理中、已解决、已关闭。

联动说明：现场即时呼叫先进 `mfg_andon_alert`，升级为需跟踪处理的业务异常时进 `mfg_exception`（通过 `source_alert_id` 回溯安灯来源），若需维修则进 `eam_repair_order`（通过 `exception_id` 关联），三表联动但职责分离。

对应功能/用例：处理设备异常、报工异常说明、现场异常发起。

### 45. mfg_andon_alert 安灯告警表

实体作用：保存现场通过安灯发起的即时告警。

为什么要有它：安灯用于现场快速响应，强调触发时间、响应时间和解除时间，适合统计异常响应效率。

核心属性及业务含义：
- `alert_id`：告警主键。
- `alert_no`：告警编号。
- `line_id`：产线 ID。
- `station_id`：工位 ID。
- `triggered_by`：触发人。
- `alert_type`：缺料、设备、质量、安全、其他。
- `trigger_time`：触发时间。
- `response_time`：响应时间。
- `resolved_time`：解除时间。
- `status`：告警中、处理中、已解除。

对应功能/用例：现场异常发起、安灯响应、异常统计。

## 9. 仓储库存

### 46. wms_warehouse 仓库表

实体作用：保存仓库档案，例如原料仓、半成品仓、成品仓、不良品仓。

为什么要有它：库存必须归属到仓库，入库、出库、盘点和追溯都需要仓库维度。

核心属性及业务含义：
- `warehouse_id`：仓库主键。
- `warehouse_code`：仓库编码。
- `warehouse_name`：仓库名称。
- `warehouse_type`：原料、半成品、成品、不良品。
- `manager_id`：负责人。
- `status`：启用、停用。

对应功能/用例：入库、出库、库存查询、盘点。

### 47. wms_location 库位表

实体作用：保存仓库中的具体库位，例如 A 区 01 架 02 层。

为什么要有它：库存不只要知道在哪个仓库，还要知道具体放在哪个库位。

核心属性及业务含义：
- `location_id`：库位主键。
- `location_code`：库位编码。
- `location_name`：库位名称。
- `warehouse_id`：所属仓库。
- `location_type`：正常、待检、不良、冻结。
- `status`：空闲、占用、锁定、维护。

对应功能/用例：上架、拣货、库存查询、盘点。

### 48. wms_material_batch 物料批次表

实体作用：保存原材料、半成品和成品的批次信息。

为什么要有它：羽毛球质量追溯必须能查到每批毛片、球头、胶水、包装材料的来源和质量状态。

核心属性及业务含义：
- `batch_id`：批次主键。
- `batch_no`：批次号，唯一。
- `material_id`：物料 ID。
- `supplier_id`：供应商 ID。
- `supplier_batch_no`：供应商批次号。
- `production_date`：生产日期。
- `received_date`：入库日期。
- `quality_status`：待检、合格、不合格、让步接收。
- `status`：可用、冻结、耗尽、报废。

对应功能/用例：原料入库、批次追溯、质检、生产领料。

### 49. wms_inventory 库存余额表

实体作用：保存某物料、某批次、某库位当前有多少库存。

为什么要有它：领料、齐套检查、库存预警和盘点都需要快速查询当前库存余额。

核心属性及业务含义：
- `inventory_id`：库存主键。
- `warehouse_id`：仓库 ID。
- `location_id`：库位 ID。
- `material_id`：物料 ID。
- `batch_id`：批次 ID。
- `available_quantity`：可用数量。
- `locked_quantity`：锁定数量。
- `inspection_quantity`：待检数量。

对应功能/用例：库存查询、齐套检查、出库扣减、盘点。

### 50. wms_stock_movement 库存流水表

实体作用：保存每一次库存变动记录。

为什么要有它：库存余额只能表示现在有多少，流水才能说明库存怎么来的、怎么走的。

核心属性及业务含义：
- `movement_id`：流水主键。
- `movement_no`：流水编号。
- `movement_type`：入库、出库、退料、盘点、调整。
- `source_order_type`：来源单据类型。
- `source_order_id`：来源单据 ID。
- `material_id`：物料 ID。
- `batch_id`：批次 ID。
- `quantity`：变动数量。
- `direction`：入、出。
- `operation_time`：操作时间。

对应功能/用例：入库、出库、退料、盘点、库存追溯。

### 51. wms_inbound_order 入库单主表

实体作用：保存入库业务的主单信息。

为什么要有它：原材料采购入库、生产完工入库、退料入库都需要单据留痕。

核心属性及业务含义：
- `inbound_id`：入库单主键。
- `inbound_no`：入库单号。
- `inbound_type`：采购入库、生产入库、退料入库。
- `warehouse_id`：入库仓库。
- `source_order_type`：来源单据类型。
- `source_order_id`：来源单据 ID。
- `audit_status`：审核状态。
- `inbound_time`：入库时间。

对应功能/用例：原材料入库、成品入库、退料入库。

### 52. wms_inbound_detail 入库单明细表

实体作用：保存入库单中的具体物料和数量。

为什么要有它：一张入库单可能包含多种物料或多个批次，必须用明细表表达。

核心属性及业务含义：
- `detail_id`：明细主键。
- `inbound_id`：入库单 ID。
- `material_id`：物料 ID。
- `batch_id`：批次 ID。
- `location_id`：库位 ID。
- `inbound_quantity`：入库数量。

对应功能/用例：入库上架、库存增加、批次生成。

### 53. wms_outbound_order 出库单主表

实体作用：保存出库业务主单信息。

为什么要有它：生产领料、销售出库、调拨出库都需要审批和单据留痕。

核心属性及业务含义：
- `outbound_id`：出库单主键。
- `outbound_no`：出库单号。
- `outbound_type`：生产领料、销售出库、调拨出库。
- `warehouse_id`：出库仓库。
- `work_order_id`：关联工单。
- `applicant_id`：申请人。
- `audit_status`：审核状态。
- `outbound_time`：出库时间。

对应功能/用例：生产领料、出库审核、库存扣减。

### 54. wms_outbound_detail 出库单明细表

实体作用：保存出库单中的具体物料、批次和数量。

为什么要有它：生产领料需要明确领了哪种物料、哪个批次、多少数量。

核心属性及业务含义：
- `detail_id`：明细主键。
- `outbound_id`：出库单 ID。
- `material_id`：物料 ID。
- `batch_id`：批次 ID。
- `location_id`：库位 ID。
- `outbound_quantity`：出库数量。

对应功能/用例：生产领料、批次追溯、库存扣减。

### 55. wms_return_order 退料/退库单主表

实体作用：保存生产退料或不良退库主单。

为什么要有它：生产中多领、错领、坏料和不良品不能直接回库，必须有退料单记录原因和审批。

核心属性及业务含义：
- `return_id`：退料单主键。
- `return_no`：退料单号。
- `return_type`：良品退料、不良退料、成品退库。
- `work_order_id`：关联工单。
- `applicant_id`：申请人。
- `audit_status`：审核状态。
- `return_time`：退库时间。

对应功能/用例：生产退料、不良品隔离、库存回写。

### 56. wms_return_detail 退料/退库明细表

实体作用：保存退料单中的具体物料、批次、数量和原因。

为什么要有它：退料主表只描述一次退料业务，明细表说明退回了哪些具体物料。

核心属性及业务含义：
- `detail_id`：明细主键。
- `return_id`：退料单 ID。
- `material_id`：物料 ID。
- `batch_id`：批次 ID。
- `return_quantity`：退回数量。
- `return_reason`：退回原因。

对应功能/用例：退料入库、不良隔离、批次追溯。

### 57. wms_stocktake_order 盘点单主表

实体作用：保存库存盘点任务主信息。

为什么要有它：工业仓储必须定期盘点，系统账面库存需要与实物库存核对。

核心属性及业务含义：
- `stocktake_id`：盘点单主键。
- `stocktake_no`：盘点单号。
- `warehouse_id`：盘点仓库。
- `stocktake_type`：全盘、抽盘、循环盘点。
- `stocktake_status`：待盘、盘点中、已完成、已关闭。
- `started_time`：开始时间。
- `finished_time`：完成时间。

对应功能/用例：库存盘点、库存调整、仓库管理。

### 58. wms_stocktake_detail 盘点单明细表

实体作用：保存盘点时每个物料批次的账面数和实盘数。

为什么要有它：盘点差异必须明细到物料、批次和库位，才能形成库存调整依据。

核心属性及业务含义：
- `detail_id`：明细主键。
- `stocktake_id`：盘点单 ID。
- `material_id`：物料 ID。
- `batch_id`：批次 ID。
- `location_id`：库位 ID。
- `book_quantity`：账面数量。
- `actual_quantity`：实盘数量。
- `difference_quantity`：差异数量。

对应功能/用例：库存盘点、差异调整、库存审计。

### 59. wms_replenishment_suggestion 补货建议表

实体作用：保存根据库存预警和生产需求生成的补货建议。

为什么要有它：生产计划和工单用料会触发缺料风险，系统需要形成补货建议，提醒仓库或采购处理。

核心属性及业务含义：
- `suggestion_id`：建议主键。
- `suggestion_no`：建议编号。
- `material_id`：物料 ID。
- `required_quantity`：需求数量。
- `available_quantity`：当前可用库存。
- `suggested_quantity`：建议补货数量。
- `source_type`：库存预警、工单需求、人工发起。
- `status`：待处理、已确认、已关闭。

对应功能/用例：发起补货建议、库存预警、工单齐套检查。

## 10. 质量管理

### 60. qms_inspection_item 检验项目表

实体作用：保存质量检验项目，例如重量、球头直径、羽毛排列、胶水粘合度、飞行稳定性、外观缺陷。

为什么要有它：质检必须按标准项目执行，不能凭质检员主观判断。

核心属性及业务含义：
- `item_id`：检验项目主键。
- `item_code`：项目编码。
- `item_name`：项目名称。
- `item_type`：尺寸、性能、外观、重量。
- `inspection_method`：人工、仪器、目检。
- `unit_id`：单位。

对应功能/用例：检验标准维护、检验方案、质检录入。

### 61. qms_inspection_standard 检验标准表

实体作用：保存检验项目的合格标准。

为什么要有它：同一检验项目对不同产品或工序可能有不同标准，需要独立配置。

核心属性及业务含义：
- `standard_id`：标准主键。
- `item_id`：检验项目 ID。
- `product_id`：产品 ID。
- `operation_id`：工序 ID。
- `standard_value`：标准值。
- `upper_limit`：上限。
- `lower_limit`：下限。
- `judgement_rule`：判定规则。

对应功能/用例：生产过程检验、成品检验、质量判定。

### 62. qms_inspection_scheme 检验方案表

实体作用：保存某产品或工序的一组检验要求。

为什么要有它：实际质检不是单个项目，而是一套检验方案，例如植毛工序检验方案、成品终检方案。

核心属性及业务含义：
- `scheme_id`：方案主键。
- `scheme_code`：方案编码。
- `scheme_name`：方案名称。
- `product_id`：产品 ID。
- `operation_id`：工序 ID。
- `inspection_type`：来料检、过程检、终检。
- `status`：草稿、已发布、停用。

对应功能/用例：检验方案制定、过程检验、成品终检。

### 63. qms_inspection_scheme_item 检验方案明细表

实体作用：保存检验方案包含哪些检验项目。

为什么要有它：一个方案由多个项目组成，且每个项目可能有不同抽检规则和判定规则。

核心属性及业务含义：
- `id`：主键。
- `scheme_id`：方案 ID。
- `item_id`：检验项目 ID。
- `standard_id`：检验标准 ID。
- `sample_quantity`：抽样数量。
- `sequence_no`：检验顺序。

对应功能/用例：质检录入、检验结果判定。

### 64. qms_inspection_order 检验单主表

实体作用：保存一次质量检验的主单信息。

为什么要有它：来料检、过程检、成品终检都需要形成检验单，作为质量追溯依据。

核心属性及业务含义：
- `inspection_id`：检验单主键。
- `inspection_no`：检验单号。
- `inspection_type`：来料检、过程检、终检。
- `scheme_id`：检验方案 ID。
- `work_order_id`：工单 ID。
- `task_id`：任务 ID。
- `report_id`：报工 ID。
- `batch_id`：批次 ID。
- `inspector_id`：质检员。
- `inspection_result`：合格、不合格、让步接收。
- `status`：待检、检验中、已完成。

字段可空说明：`work_order_id`、`task_id`、`report_id`、`batch_id` 按检验类型部分可空——来料检只填 `batch_id`（无工单、任务、报工），过程检填工单、任务、报工，成品终检填工单和产品批次，由 `inspection_type` 决定必填组合。

对应功能/用例：检验生产产品、工序质检、成品终检。

### 65. qms_inspection_result 检验结果明细表

实体作用：保存检验单中每个检验项目的实测结果。

为什么要有它：检验主单只保存总体结果，具体哪个项目合格或不合格需要明细记录。

核心属性及业务含义：
- `result_id`：结果主键。
- `inspection_id`：检验单 ID。
- `item_id`：检验项目 ID。
- `measured_value`：实测值。
- `result`：合格、不合格。
- `defect_description`：缺陷描述。
- `photo_file_id`：质检图片附件。

对应功能/用例：质检录入、缺陷记录、质量追溯。

### 66. qms_nonconforming_order 不合格品处理单表

实体作用：保存不合格品处理主单。

为什么要有它：检验不合格后不能直接报废或返工，必须形成处理流程和审批记录。

核心属性及业务含义：
- `ncr_id`：处理单主键。
- `ncr_no`：处理单号。
- `inspection_id`：检验单 ID。
- `product_lot_id`：产品批次 ID。
- `defect_reason`：不合格原因。
- `disposition_type`：返工、报废、降级、让步接收。
- `approval_status`：审批状态。
- `status`：待处理、处理中、已关闭。

对应功能/用例：不合格处理、返工、报废审批。

### 67. qms_nonconforming_detail 不合格品处理明细表

实体作用：保存不合格品处理的明细结果。

为什么要有它：一个处理单可能涉及多个产品条码、多个缺陷项或多种处理结果。

核心属性及业务含义：
- `detail_id`：明细主键。
- `ncr_id`：处理单 ID。
- `barcode_id`：产品条码 ID。
- `inspection_result_id`：检验结果 ID。
- `defect_quantity`：不合格数量。
- `final_disposition`：最终处理方式。
- `recheck_result`：复检结果。

对应功能/用例：缺陷处理、返工复检、不良追溯。

## 11. 设备管理

### 68. eam_equipment_category 设备分类表

实体作用：保存设备分类，例如生产设备、检测设备、辅助工具、仓储设备。

为什么要有它：设备数量多，需要按类别进行维护、统计和保养策略配置。

核心属性及业务含义：
- `category_id`：设备分类主键。
- `parent_id`：上级分类。
- `category_code`：分类编码。
- `category_name`：分类名称。
- `status`：是否启用。

对应功能/用例：设备台账、设备查询、设备统计。

### 69. eam_equipment 设备台账表

实体作用：保存每台设备的基础档案。

为什么要有它：设备状态监控、维修、保养、点检都必须基于设备台账。

核心属性及业务含义：
- `equipment_id`：设备主键。
- `equipment_code`：设备编码。
- `equipment_name`：设备名称。
- `category_id`：设备分类。
- `model`：规格型号。
- `manufacturer`：厂家。
- `purchase_date`：购入日期。
- `station_id`：所在工位。
- `status`：运行、停机、故障、维修中、报废。

对应功能/用例：查看设备信息、设备维修、设备状态监控。

### 70. eam_equipment_status_log 设备状态记录表

实体作用：保存设备状态变化和采集记录。

为什么要有它：设备不能只保存当前状态，还要知道何时故障、何时恢复、停机多久。

核心属性及业务含义：
- `status_log_id`：状态记录主键。
- `equipment_id`：设备 ID。
- `running_status`：运行状态。
- `downtime_type`：停机类型（计划停机、非计划停机、故障停机、换型停机），支撑 OEE 停机分类统计。
- `alert_level`：报警等级。
- `station_id`：所在工位。
- `collected_time`：采集时间。
- `description`：状态说明。

对应功能/用例：设备状态监控、OEE 分析、维修触发。

### 71. eam_maintenance_plan 保养计划表

实体作用：保存设备定期保养计划。

为什么要有它：设备管理不能只坏了再修，需要按周期预防性维护，降低停机风险。

核心属性及业务含义：
- `plan_id`：保养计划主键。
- `equipment_id`：设备 ID。
- `maintenance_type`：日常、一级、二级。
- `cycle_type`：按天、按小时、按次数。
- `cycle_value`：周期值。
- `maintenance_content`：保养内容。
- `responsible_id`：负责人。
- `status`：启用、停用。

对应功能/用例：设备保养计划、设备管理。

### 72. eam_maintenance_record 保养记录表

实体作用：保存设备实际保养执行记录。

为什么要有它：计划只是要求，实际是否执行、执行结果如何，需要单独记录。

核心属性及业务含义：
- `record_id`：保养记录主键。
- `plan_id`：保养计划 ID。
- `equipment_id`：设备 ID。
- `maintainer_id`：保养人。
- `maintenance_time`：保养时间。
- `maintenance_result`：保养结果。
- `next_maintenance_time`：下次保养时间。

对应功能/用例：设备保养记录、设备生命周期管理。

### 73. eam_spot_check_plan 点检计划表

实体作用：保存设备点检计划。

为什么要有它：现场设备通常每班或每天开机前都要点检，点检项目需要标准化。

核心属性及业务含义：
- `plan_id`：点检计划主键。
- `equipment_id`：设备 ID。
- `check_cycle`：点检周期。
- `check_items`：点检项目说明。
- `responsible_role`：负责岗位。
- `status`：启用、停用。

对应功能/用例：设备点检、开机检查、隐患预防。

### 74. eam_spot_check_record 点检记录表

实体作用：保存每次设备点检结果。

为什么要有它：设备点检发现的小问题可以提前处理，避免生产过程中突然停机。

核心属性及业务含义：
- `record_id`：点检记录主键。
- `plan_id`：点检计划 ID。
- `equipment_id`：设备 ID。
- `checker_id`：点检人。
- `check_time`：点检时间。
- `check_result`：正常、异常。
- `abnormal_description`：异常说明。

对应功能/用例：设备点检、异常上报、维修触发。

### 75. eam_repair_order 设备报修/维修单表

实体作用：保存设备故障报修和维修过程。

为什么要有它：设备异常必须形成报修、派工、维修、验收、关闭的闭环。

核心属性及业务含义：
- `repair_id`：维修单主键。
- `repair_no`：维修单号。
- `equipment_id`：设备 ID。
- `exception_id`：关联生产异常。
- `reported_by`：报修人。
- `reported_time`：报修时间。
- `fault_description`：故障描述。
- `repairer_id`：维修人。
- `repair_result`：维修结果。
- `repair_cost`：维修费用。
- `status`：待派工、维修中、待验收、已关闭。

对应功能/用例：处理设备异常、设备维修、维修验收。

## 12. 条码追溯

### 76. trace_barcode 条码表

实体作用：保存系统生成或绑定的条码信息。

为什么要有它：扫码是 MES 现场操作和追溯的入口，物料、批次、产品、单据都可能绑定条码。

核心属性及业务含义：
- `barcode_id`：条码主键。
- `barcode_value`：条码值，唯一。
- `barcode_type`：物料、批次、产品、单据。
- `biz_type`：关联业务类型。
- `biz_id`：关联业务 ID。
- `generated_rule`：生成规则。
- `status`：启用、停用、作废。

对应功能/用例：生成条码、扫码报工、扫码追溯、仓储扫码。

### 77. trace_product_lot 产品批次表

实体作用：保存成品或半成品的生产批次。

为什么要有它：羽毛球通常按批次生产和检验，追溯时需要从产品批次反查工单、任务、物料和质检记录。

核心属性及业务含义：
- `product_lot_id`：产品批次主键。
- `lot_no`：产品批次号。
- `product_id`：产品 ID。
- `work_order_id`：工单 ID。
- `batch_quantity`：批次数量。
- `qualified_quantity`：合格数量。
- `defect_quantity`：不良数量。
- `status`：生产中、待检、合格、入库、关闭。

对应功能/用例：产品追溯、成品入库、成品质检。

### 78. trace_material_usage 物料使用追溯表

实体作用：保存产品批次或产品条码与原材料批次之间的使用关系。

为什么要有它：质量问题发生时，需要快速查出某批羽毛球使用了哪批毛片、球头、胶水和包装材料。

核心属性及业务含义：
- `usage_id`：追溯记录主键。
- `product_lot_id`：产品批次 ID。
- `barcode_id`：产品条码 ID。
- `work_order_id`：工单 ID。
- `report_id`：报工 ID。
- `material_id`：物料 ID。
- `batch_id`：原料批次 ID。
- `used_quantity`：使用数量。

对应功能/用例：产品质量追溯、物料批次追溯、客户扫码追溯。

## 13. 规范性检查结论

1. 表结构从功能出发，不照搬参考文档案例。
2. 生产订单、生产计划、生产工单、派工单、生产任务单边界清楚，避免概念混用。
3. 物料档案、库存余额、库存流水、物料批次职责分离，避免库存数据只存当前数量而无法追溯。
4. 质量模块以检验单和检验结果为数据源，追溯模块不重复保存质检结果。
5. 安灯告警、生产异常、设备维修三者联动但不混成一张表，既能快速响应现场，也能形成维修闭环。
6. 通用文件、审批、状态流转、操作日志单独建表，满足企业级系统审计要求。
7. 工艺路线、作业指导书、产品物料清单都有版本概念，能够支持历史生产记录回溯。
8. 核心业务表都应包含业务编号、状态、审计字段、乐观锁版本号和软删除字段。
9. 数据类型、约束、索引、字符集、存储引擎、外键策略均已在第 2 章统一约定，可直接作为建表脚本依据。
10. 两类批次（采购批次 `wms_material_batch` 与生产批次 `trace_product_lot`）、追溯冗余字段、检验单按类型可空字段均已说明，避免落地时产生歧义。

## 14. 后续建表建议

下一步生成 MySQL 脚本时，建议按以下顺序建表：

1. 系统字典、权限、组织人员。
2. 物料、产品、产品物料清单、供应商、客户。
3. 工艺路线、工序、作业指导书。
4. 车间、产线、工位。
5. 生产订单、计划、工单、任务。
6. 仓库、库位、批次、库存、出入库。
7. 报工、质检、不合格处理。
8. 设备台账、点检、保养、维修。
9. 条码和追溯。
10. 日志、审批、状态流转补充索引。

## 15. 可选扩展表（不计入 78 张核心表）

以下表在核心版之外，属于企业级完整系统常见但非最小闭环所必需的部分，可在核心表稳定后按需扩展，列出以说明设计边界，避免评审时被认为遗漏：

| 建议表名 | 作用 | 说明 |
|---|---|---|
| `org_schedule` | 排班表 | 将“员工/班组—日期—班次”关联，支撑排产与报工按班次归集，弥补现有只有 `org_shift` 班次定义而无排班的缺口。 |
| `sys_notification` | 消息通知表 | 安灯告警、异常、审批待办、库存预警统一推送到 PC 和小程序，落地待办与消息中心。 |
| `sys_login_log` | 登录日志表 | 从 `sys_operation_log` 中独立出登录/登出/失败记录，满足安全审计。 |
| 计件工资相关表 | 计件核算 | 报工数据已保存工时和人员，计件规则与工资核算可另建模块扩展。 |
| 安全管理相关表 | 安全巡检、隐患整改、安全培训 | 参考文档提及的安全管理模块，属独立子系统，可后续扩展。 |

设计边界说明：核心 78 张表已覆盖“订单→计划→工单→派工→任务→领料→报工→质检→入库→追溯”的完整生产闭环及其支撑数据；上述扩展表不影响主链路完整性，按项目实际需要纳入。

## 16. AI 建表执行指令（生成 DDL 时必须严格遵守）

> 本节是给 AI 生成 MySQL 建表脚本的精确约束，目的是让生成结果确定、无歧义、可直接执行。生成脚本时必须逐条遵守。

### 16.1 全局约定

1. 数据库：MySQL 8.0；每张表统一 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci`。
2. 只生成核心 78 张表（第 3–12 章）；第 15 章扩展表不生成。
3. 严格按第 14 章顺序建表，被引用表先建，保证脚本可一次性顺序执行。
4. 采用逻辑外键，默认**不**生成 `FOREIGN KEY` 物理约束，但要为所有外键字段建普通索引。
5. 每张表每个字段都要写 `COMMENT`，表也要写 `COMMENT`，注释用中文，取自本文档字段含义。
6. 主键统一 `BIGINT UNSIGNED NOT NULL AUTO_INCREMENT`。

### 16.2 字段类型映射（与 2.3 一致，不得偏离）

| 文档中的字段语义 | MySQL 类型 |
|---|---|
| 主键、外键 ID（`_id` 结尾） | `BIGINT UNSIGNED` |
| 业务编号（`_no`、`_code` 结尾） | `VARCHAR(64)` |
| 名称、标题（`_name`、`title`） | `VARCHAR(128)` |
| 描述、备注（`description`、`remark`） | `VARCHAR(500)` |
| 状态、类型枚举（`status`、`_type`、`_status`） | `VARCHAR(32)` |
| 数量、工时、用量（`_quantity`、`_hours`、`quantity`） | `DECIMAL(18,4)` |
| 金额、成本、费用（`_cost`、`_amount`） | `DECIMAL(18,2)` |
| 比率（`_rate`、`loss_rate`） | `DECIMAL(9,4)` |
| 是否标记（`is_`、`_flag`） | `TINYINT`（0/1） |
| 日期（`_date`） | `DATE` |
| 时间（`_time`） | `DATETIME` |
| 密码（`password_hash`） | `VARCHAR(128)` |
| IP（`request_ip`） | `VARCHAR(64)` |
| URL（`file_url`、`route_path`） | `VARCHAR(255)` |

**命名规则例外字段（优先级高于上表，必须按此建类型，防止套错）**：

下列字段虽然名字符合某条通用规则，但语义不同，必须按指定类型建，不得套用上表：

| 字段 | 正确类型 | 说明（为什么是例外） |
|---|---|---|
| `sort_no`、`sequence_no` | `INT` | 是排序号/顺序号，虽以 `_no` 结尾但不是业务编号，不能建成 `VARCHAR(64)` |
| `supplier_batch_no`、`bom_version` | `VARCHAR(64)` | 是字符串编号/版本号，正常按编号处理 |
| `qc_required`、`is_default` | `TINYINT`（0/1） | 是否类布尔，虽未以 `is_`/`_flag` 结尾 |
| `precision_scale` | `INT` | 小数精度位数 |
| `cycle_value` | `DECIMAL(18,4)` | 保养/点检周期数值 |
| `result`、`direction`、`data_scope`、`alert_level`、`role_in_task`、`recheck_result`、`final_disposition`、`repair_result`、`maintenance_result`、`check_result` | `VARCHAR(32)` | 是枚举值，虽未以 `_type`/`_status` 结尾，统一按枚举存字典值 |
| `username`、`phone`、`contact_phone`、`contact_name`、`manufacturer`、`model`、`spec_model`、`product_model`、`position_name` | `VARCHAR(64)` | 普通短文本 |
| `generated_rule`、`judgement_rule`、`inspection_method`、`check_cycle`、`check_items`、`maintenance_content`、`maintenance_type`、`abnormal_description`、`fault_description`、`defect_description`、`handle_result`、`repair_result` | `VARCHAR(255)` | 规则/方式/说明类中长文本；`check_items`、`maintenance_content` 可用 `TEXT` |
| `barcode_value` | `VARCHAR(128)` | 条码值，建唯一索引 |
| `password_hash` | `VARCHAR(128)` | 已单列 |

其余未列出的字段一律按上表通用规则映射。

### 16.3 每张业务表统一追加的通用字段（放在核心字段之后）

按 2.2 节，若 `status`/`remark`/业务编号已在该表核心属性中出现，则不重复添加；其余一律追加：

```sql
remark        VARCHAR(500)     NULL      COMMENT '备注',
version       INT              NOT NULL DEFAULT 0   COMMENT '乐观锁版本号',
created_by    BIGINT UNSIGNED  NULL      COMMENT '创建人',
created_time  DATETIME         NULL      COMMENT '创建时间',
updated_by    BIGINT UNSIGNED  NULL      COMMENT '更新人',
updated_time  DATETIME         NULL      COMMENT '更新时间',
deleted_flag  TINYINT          NOT NULL DEFAULT 0   COMMENT '软删除标记 0未删 1已删'
```

纯关联表（如 `sys_user_role`、`sys_role_menu`、`org_team_member`、`mfg_task_operator`、`factory_station_operation`）**保留文档中列出的全部核心字段**（含 `assigned_time`、`role_in_task`、`is_default`、`status` 等），仅在通用字段上精简：只追加 `created_by`、`created_time`、`deleted_flag`，不追加 `version`、`remark`、`updated_by`、`updated_time`。

### 16.4 索引与约束生成规则

1. 主键：`PRIMARY KEY (主键字段)`。
2. 业务编号：凡文档标注“唯一”的 `_no`/`_code` 字段，建 `UNIQUE KEY uk_表名_字段 (字段)`。
3. 外键字段：所有 `_id` 结尾的关联字段建 `KEY idx_表名_字段 (字段)`。
4. 复合唯一约束（必须生成）：
   - `wms_inventory`：`UNIQUE KEY uk_wms_inventory_whlmb (warehouse_id, location_id, material_id, batch_id)`。
   - `sys_user_role`：`UNIQUE KEY uk_sys_user_role (user_id, role_id)`。
   - `sys_role_menu`：`UNIQUE KEY uk_sys_role_menu (role_id, menu_id)`。
   - `factory_station_operation`：`UNIQUE KEY uk_factory_station_operation (station_id, operation_id)`。
5. 索引命名统一用 `uk_`（唯一）、`idx_`（普通）前缀。

### 16.5 生成后自检清单

生成 DDL 后 AI 需自查并确认：

1. 表数量正好 78 张，表名与本文档完全一致。
2. 每张表都有主键，且类型为 `BIGINT UNSIGNED`。
3. 每张业务表都含审计、软删除、乐观锁字段；纯关联表按 16.3 精简。
4. 所有 `_no`/`_code` 唯一字段有唯一索引，所有 `_id` 字段有普通索引。
5. 16.4 第 4 条的四个复合唯一约束都已生成。
6. 每字段、每表都有中文 `COMMENT`。
7. 脚本可按第 14 章顺序一次性执行，无先引用后建表的报错。
