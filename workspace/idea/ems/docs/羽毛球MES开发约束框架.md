# 羽毛球 MES 开发约束框架

本文作为后续合作开发的统一约束。以后新增接口、服务、Mapper、业务流转、数据库变更，都先按本文检查，再编码。

## 1. 当前基线

项目当前处于后端接口开发前置阶段：

- 数据库设计已完成，核心表为 78 张。
- MySQL 数据库为 `mes`，当前实际表数量和 `schema.sql` 一致。
- 实体类已生成，并已完成实体数量、表名、字段、主键的基础一致性检查。
- 后端接口文档已有，但接口代码尚未全量落地。

设计源文档：

- `../../../../相关文档/md/database_table_analysis（数据库）.md`
- `../../../../相关文档/md/后端接口文档.md`

项目内落地文件：

- `src/main/resources/db/schema.sql`
- `src/main/java/com/neuedu/ems/entity`
- `src/main/resources/application.properties`
- `AGENTS.md`

## 2. 总体开发原则

1. 先对齐设计，再写代码。
2. 先完成最小业务闭环，再补齐外围管理功能。
3. 数据完整性优先于接口数量。
4. 业务状态必须可追踪，库存数量必须可追溯。
5. 不做无关重构，不引入无关依赖。
6. 每个模块一次只解决一个明确边界，避免把多个业务流混在一个接口里。

## 3. 模块边界

后端模块按数据库前缀和接口前缀统一：

| 模块 | 表前缀 | 接口前缀 | 说明 |
|---|---|---|---|
| 系统管理 | `sys_` | `/api/sys` | 用户、角色、菜单、字典、参数、文件、日志、审批 |
| 组织人事 | `org_` | `/api/org` | 部门、员工、班组、班次 |
| 主数据 | `mdm_` | `/api/mdm` | 单位、供应商、客户、物料、产品、BOM |
| 工艺管理 | `process_` | `/api/process` | 工序、工艺路线、参数、作业指导书 |
| 工厂建模 | `factory_` | `/api/factory` | 车间、产线、工位、工位工序 |
| 生产制造 | `mfg_` | `/api/mfg` | 订单、计划、工单、派工、任务、报工、异常、安灯 |
| 仓储库存 | `wms_` | `/api/wms` | 仓库、库位、批次、库存、出入库、盘点、补货 |
| 质量管理 | `qms_` | `/api/qms` | 检验项目、标准、方案、检验单、不合格处理 |
| 设备管理 | `eam_` | `/api/eam` | 设备、状态、保养、点检、维修 |
| 条码追溯 | `trace_` | `/api/trace` | 条码、产品批次、物料使用追溯 |
| 看板 | 无固定表 | `/api/dashboard` | 汇总查询，按需调用各业务 Mapper |

模块之间的调用必须遵守业务方向，不允许反向污染。例如库存模块可以被生产报工和入库确认调用，但普通库存编辑接口不能直接修改库存数量。

## 4. 包结构约束

每个资源模块按固定四层实现：

```text
controller: 接收 HTTP 请求，转换参数，返回 ResultData
service: 定义业务能力
service.impl: 实现业务规则、状态流转、事务
mapper: 数据访问，继承 BaseMapper
entity: 数据库表实体
```

命名规则：

```text
实体：SysUser
Mapper：SysUserMapper
Service：SysUserService
ServiceImpl：SysUserServiceImpl
Controller：SysUserController
```

当前已有 `UserController`、`UserService` 这类早期命名，后续系统管理模块建议逐步收敛到接口文档中的 `SysUserController`、`SysUserService` 命名，避免和业务用户概念混淆。

## 5. 接口统一规范

所有正式接口必须满足：

- 统一以 `/api` 开头。
- 模块路径和接口文档一致，例如 `/api/sys/user`、`/api/mfg/work-order`。
- 分页查询统一为 `GET /page`。
- 分页参数统一为 `current`、`size`。
- 详情查询统一为 `GET /{id}`。
- 新增统一为 `POST /`。
- 修改统一为 `PUT /`。
- 删除统一为 `DELETE /{id}`，只做软删除。
- Excel 导出统一为 `GET /excel`。
- 主从表一次提交统一使用 `/full`。

统一返回：

```json
{
  "msg": "操作成功",
  "code": 200,
  "data": {}
}
```

分页返回：

```json
{
  "msg": "操作成功",
  "code": 200,
  "data": {
    "records": [],
    "total": 0,
    "size": 10,
    "current": 1,
    "pages": 0
  }
}
```

## 6. Controller 约束

Controller 只能做：

- 接收请求参数。
- 基础参数校验。
- 调用 Service。
- 返回 `ResultData`。

Controller 不能做：

- 拼复杂 SQL。
- 直接操作多个 Mapper。
- 写状态流转规则。
- 写库存扣减或回滚逻辑。
- 捕获异常后吞掉错误。

## 7. Service 约束

Service 是业务边界，必须负责：

- 业务校验。
- 状态流转。
- 主从表事务。
- 库存流水一致性。
- 操作日志和状态日志。
- 调用其它领域服务。

以下操作必须加 `@Transactional`：

- 主从表 `/full` 新增和修改。
- 工单下达、任务开始、任务完成。
- 生产报工。
- 入库确认、出库确认、退料入库、盘点完成。
- 检验单提交、不合格品处理审批。
- 设备维修派工、关闭维修单。

## 8. Mapper 约束

- 每张表一个 Mapper，继承 `BaseMapper<Entity>`。
- 简单 CRUD 优先用 MyBatis-Plus。
- 复杂统计可以写注解 SQL 或 XML，但必须和实体字段命名保持一致。
- XML Mapper 不能保留示例 SQL，例如 `Blog` 示例。
- Mapper 方法名称必须表达业务语义，例如 `selectByWorkOrderId`、`selectActiveByCode`。

## 9. 数据与实体约束

实体和数据库必须保持一致：

- `@TableName` 对应真实表名。
- `@TableId` 必须标注真实主键。
- Java 字段使用驼峰，数据库字段使用下划线。
- `BIGINT UNSIGNED` 对应 `Long`。
- `DECIMAL` 对应 `BigDecimal`。
- `DATE` 对应 `LocalDate`。
- `DATETIME` 对应 `LocalDateTime`。
- `TINYINT` 当前统一用 `Integer`，除非明确改为 Boolean。

查询约束：

- 默认查询必须过滤 `deleted_flag = 0`。
- 业务编号、编码字段必须按唯一性处理。
- 不允许物理删除生产、质量、仓储、追溯数据。

## 10. 状态约束

状态字段统一存英文编码，不存中文。

通用启停：

```text
ENABLE
DISABLE
```

单据状态：

```text
DRAFT
SUBMITTED
APPROVED
REJECTED
RELEASED
CLOSED
```

生产状态：

```text
WAIT_START
RUNNING
PAUSED
FINISHED
CLOSED
```

质量结果：

```text
PASS
FAIL
CONCESSION
REWORK
SCRAP
```

仓储类型：

```text
PURCHASE
PRODUCTION
MATERIAL_ISSUE
SALE
RETURN
STOCKTAKE
```

设备状态：

```text
IDLE
RUNNING
MAINTAINING
REPAIRING
STOPPED
FAULT
SCRAPPED
```

告警等级：

```text
LOW
MEDIUM
HIGH
URGENT
```

新增状态值前必须先确认 `sys_dict_type`、`sys_dict_data` 设计，不能在业务代码里临时写死中文状态。

## 11. 库存约束

库存是高风险模块，必须遵守：

- `wms_inventory` 表示当前余额。
- `wms_stock_movement` 表示余额变化过程。
- 入库、出库、退料、盘点必须同时写余额和流水。
- 普通库存修改接口禁止直接改 `available_quantity`、`locked_quantity`、`inspection_quantity`。
- 扣减库存前必须校验可用数量。
- 并发扣减必须使用事务和版本控制，避免超扣。

## 12. 批次与追溯约束

- 原料、采购、仓储批次使用 `wms_material_batch`。
- 成品或半成品生产批次使用 `trace_product_lot`。
- 条码入口使用 `trace_barcode`。
- 扫码追溯通过 `trace_material_usage` 串联产品批次、工单、报工、物料批次。
- 追溯表可以保留必要冗余，但库存扣减以库存流水和库存余额为准。

## 13. 日志与审计约束

必须记录状态变更：

- 生产订单下达、关闭。
- 生产计划审批或下达。
- 工单下达、关闭。
- 任务开始、完成、暂停。
- 报工提交、审核。
- 检验单提交。
- 不合格品处理审批。
- 入库、出库、退料、盘点确认。
- 设备状态变更、维修派工、维修关闭。

关键操作必须写 `sys_operation_log`，状态变化写 `sys_status_change_log`，审批过程写 `sys_approval_record`。

## 14. 最小业务闭环顺序

第一阶段先打通主链路：

1. 登录认证、用户管理。
2. 计量单位、供应商、客户、物料分类、物料、产品、BOM。
3. 工序、工艺路线、车间、产线、工位。
4. 生产订单、生产计划、生产工单。
5. 工单用料需求、原材料出库、库存查询。
6. 派工单、生产任务、任务人员分配。
7. 生产报工、报工用料消耗。
8. 检验单、检验结果、不合格品处理。
9. 成品入库、产品批次、条码生成、扫码追溯。
10. 首页看板、设备台账、安灯告警、异常处理、设备维修。

每个阶段完成后必须能运行测试，不能堆到最后统一调。

## 15. 单模块落地清单

每开发一个资源模块，按下面清单检查：

```text
1. Entity 已存在且字段正确
2. Mapper 已创建并继承 BaseMapper
3. Service 已创建
4. ServiceImpl 已创建
5. Controller 已创建
6. 路径符合接口文档
7. 分页接口过滤 deleted_flag = 0
8. 删除接口只做软删除
9. 状态字段使用英文编码
10. 主从表接口加事务
11. 关键状态变更写状态日志
12. 关键操作写操作日志
13. Maven 测试通过
```

## 16. 验证门禁

每次代码修改后至少运行：

```powershell
C:\Users\USER\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd test
```

如果本机 Maven 路径不同，使用项目 Maven Wrapper 或本机 Maven，但必须说明实际执行的命令和结果。

涉及数据库或实体时，还必须检查：

```text
schema.sql 表数量 = 78
mes 实际表数量 = 78
实体数量 = 78
表名无差异
字段无差异
主键注解无差异
```

## 17. 禁止事项

- 禁止直接删除生产业务数据。
- 禁止绕过 Service 在 Controller 里操作多个 Mapper。
- 禁止库存余额无流水变更。
- 禁止后端和数据库存中文状态。
- 禁止把采购批次和生产批次混用。
- 禁止随意新增表、字段、状态值、依赖。
- 禁止保留示例 Mapper、示例 SQL、无业务意义接口。
- 禁止为了赶进度跳过测试。

## 18. 协作规则

- 每次开发前先确认本次目标属于哪个模块。
- 每次只完成一个清晰的业务边界。
- 如果文档和代码冲突，以数据库事实和业务闭环为准，同时更新文档或记录差异。
- 若需要改变既有设计，先说明影响范围：数据库、实体、接口、前端、测试。
- 开发完成后必须说明改了什么、验证了什么、还剩什么风险。
