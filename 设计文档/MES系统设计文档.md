# MES制造执行系统 - 完整设计文档（基于实际数据库）

## 1. 项目概述

### 1.1 项目背景
本项目是一个**完整的制造执行系统(MES)**，包含78张数据库表，涵盖：
- 设备资产管理(EAM)
- 工厂车间管理(Factory)
- 主数据管理(MDM)
- 生产制造(MFG)
- 组织人事(ORG)
- 工艺路线(Process)
- 质量管理(QMS)
- 系统管理(SYS)
- 追溯管理(Trace)
- 仓储管理(WMS)

### 1.2 技术栈
- **后端**: Spring Boot 3.5.16 + Java 21
- **数据库**: MySQL 8.0 + MyBatis + MyBatis-Plus
- **前端**: Vue 3 + Element Plus (推荐)
- **安全认证**: JWT
- **API文档**: Swagger/OpenAPI
- **构建工具**: Gradle

---

## 2. 数据库模块说明（78张表）

### 模块1：设备资产管理 EAM (8张表)
```
eam_equipment                  设备表
eam_equipment_category         设备类别
eam_equipment_status_log       设备状态日志
eam_maintenance_plan           保养计划
eam_maintenance_record         保养记录
eam_repair_order               维修工单
eam_spot_check_plan            点检计划
eam_spot_check_record          点检记录
```

### 模块2：工厂车间管理 Factory (4张表)
```
factory_line                   产线表
factory_station                工位表
factory_station_operation      工位工序关联
factory_workshop               车间表
```

### 模块3：主数据管理 MDM (8张表)
```
mdm_bom                        物料清单(BOM)
mdm_bom_item                   BOM明细
mdm_customer                   客户表
mdm_material                   物料表
mdm_material_category          物料类别
mdm_product                    产品表
mdm_supplier                   供应商表
mdm_unit                       计量单位
```

### 模块4：生产制造 MFG (12张表)
```
mfg_andon_alert               安灯告警
mfg_dispatch_order            派工单
mfg_exception                 生产异常
mfg_production_order          生产订单
mfg_production_plan           生产计划
mfg_report_material           报工物料
mfg_task                      生产任务
mfg_task_operator             任务操作工
mfg_work_order                工单表
mfg_work_order_material       工单物料需求
mfg_work_report               工作报工
```

### 模块5：组织人事 ORG (6张表)
```
org_department                部门表
org_employee                  员工表
org_shift                     班次表
org_team                      班组表
org_team_member               班组成员
```

### 模块6：工艺路线 Process (6张表)
```
process_operation             工序表
process_parameter             工艺参数
process_route                 工艺路线
process_route_item            工艺路线明细
process_work_instruction      作业指导书
```

### 模块7：质量管理 QMS (8张表)
```
qms_inspection_item           质检项目
qms_inspection_order          质检单
qms_inspection_result         质检结果
qms_inspection_scheme         质检方案
qms_inspection_scheme_item    质检方案明细
qms_inspection_standard       质检标准
qms_nonconforming_detail      不合格品明细
qms_nonconforming_order       不合格品单
```

### 模块8：系统管理 SYS (12张表)
```
sys_approval_record           审批记录
sys_dict_data                 字典数据
sys_dict_type                 字典类型
sys_file                      文件表
sys_menu                      菜单表
sys_operation_log             操作日志
sys_role                      角色表
sys_role_menu                 角色菜单
sys_status_change_log         状态变更日志
sys_system_param              系统参数
sys_user                      用户表
sys_user_role                 用户角色
```

### 模块9：追溯管理 Trace (3张表)
```
trace_barcode                 条码表
trace_material_usage          物料使用记录
trace_product_lot             产品批次
```

### 模块10：仓储管理 WMS (11张表)
```
wms_inbound_detail            入库明细
wms_inbound_order             入库单
wms_inventory                 库存表
wms_location                  库位表
wms_material_batch            物料批次
wms_outbound_detail           出库明细
wms_outbound_order            出库单
wms_replenishment_suggestion  补货建议
wms_return_detail             退料明细
wms_return_order              退料单
wms_stock_movement            库存移动
wms_stocktake_detail          盘点明细
wms_stocktake_order           盘点单
wms_warehouse                 仓库表
```

---

## 3. 后端开发任务清单

### 3.1 实体类开发（78个Entity）

按模块创建实体类，**所有字段使用统一规范**：

**通用字段规范**：
```java
private Long xxxId;              // 主键，BIGINT UNSIGNED
private Integer version;         // 乐观锁版本号
private Long createdBy;          // 创建人ID
private LocalDateTime createdTime;  // 创建时间
private Long updatedBy;          // 更新人ID
private LocalDateTime updatedTime;  // 更新时间
private Integer deletedFlag;     // 删除标志 0-未删除 1-已删除
```

**实体类示例**（参考现有User.java修改）：
```java
@Data
@TableName("mdm_product")
public class Product {
    @TableId(value = "product_id", type = IdType.AUTO)
    private Long productId;
    
    private String productCode;
    private String productName;
    private String productModel;
    private Long materialId;
    private String status;
    private String remark;
    
    private Integer version;
    private Long createdBy;
    private LocalDateTime createdTime;
    private Long updatedBy;
    private LocalDateTime updatedTime;
    private Integer deletedFlag;
}
```

**需要创建的实体类（78个）**：

**EAM模块（8个）**：
- Equipment
- EquipmentCategory
- EquipmentStatusLog
- MaintenancePlan
- MaintenanceRecord
- RepairOrder
- SpotCheckPlan
- SpotCheckRecord

**Factory模块（4个）**：
- Line
- Station
- StationOperation
- Workshop

**MDM模块（8个）**：
- Bom
- BomItem
- Customer
- Material
- MaterialCategory
- Product
- Supplier
- Unit

**MFG模块（12个）**：
- AndonAlert
- DispatchOrder
- Exception
- ProductionOrder
- ProductionPlan
- ReportMaterial
- Task
- TaskOperator
- WorkOrder
- WorkOrderMaterial
- WorkReport

**ORG模块（6个）**：
- Department
- Employee
- Shift
- Team
- TeamMember

**Process模块（6个）**：
- Operation
- Parameter
- Route
- RouteItem
- WorkInstruction

**QMS模块（8个）**：
- InspectionItem
- InspectionOrder
- InspectionResult
- InspectionScheme
- InspectionSchemeItem
- InspectionStandard
- NonconformingDetail
- NonconformingOrder

**SYS模块（12个）**：
- ApprovalRecord
- DictData
- DictType
- SysFile
- Menu
- OperationLog
- Role
- RoleMenu
- StatusChangeLog
- SystemParam
- User
- UserRole

**Trace模块（3个）**：
- Barcode
- MaterialUsage
- ProductLot

**WMS模块（14个）**：
- InboundDetail
- InboundOrder
- Inventory
- Location
- MaterialBatch
- OutboundDetail
- OutboundOrder
- ReplenishmentSuggestion
- ReturnDetail
- ReturnOrder
- StockMovement
- StocktakeDetail
- StocktakeOrder
- Warehouse

### 3.2 Mapper接口开发（78个）

每个实体对应一个Mapper接口：
```java
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // MyBatis-Plus已提供基础CRUD
    // 自定义复杂查询写在这里
}
```

### 3.3 Service层开发

**核心业务Service（按优先级）**：

**第一优先级**（用户权限）：
- UserService - 用户管理
- RoleService - 角色管理
- MenuService - 菜单管理
- DepartmentService - 部门管理

**第二优先级**（主数据）：
- ProductService - 产品管理
- MaterialService - 物料管理
- SupplierService - 供应商管理
- CustomerService - 客户管理
- BomService - BOM管理

**第三优先级**（生产核心）：
- ProductionOrderService - 生产订单
- WorkOrderService - 工单管理
- TaskService - 生产任务
- WorkReportService - 工作报工

**第四优先级**（质量&仓储）：
- InspectionOrderService - 质检单管理
- InventoryService - 库存管理
- InboundOrderService - 入库管理
- OutboundOrderService - 出库管理

**第五优先级**（设备&追溯）：
- EquipmentService - 设备管理
- BarcodeService - 条码管理
- ProductLotService - 批次追溯

### 3.4 Controller层开发

**API分组规范**：
```
/api/sys/*      - 系统管理接口
/api/org/*      - 组织人事接口
/api/mdm/*      - 主数据管理接口
/api/mfg/*      - 生产制造接口
/api/qms/*      - 质量管理接口
/api/wms/*      - 仓储管理接口
/api/eam/*      - 设备管理接口
/api/process/*  - 工艺管理接口
/api/trace/*    - 追溯管理接口
/api/factory/*  - 车间管理接口
```

**Controller示例**：
```java
@RestController
@RequestMapping("/api/mdm/product")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @GetMapping("/page")
    public ResultData<IPage<Product>> page(
        @RequestParam(defaultValue = "1") Integer current,
        @RequestParam(defaultValue = "10") Integer size,
        ProductQueryVO query) {
        // 分页查询
    }
    
    @GetMapping("/{id}")
    public ResultData<Product> getById(@PathVariable Long id) {
        // 详情查询
    }
    
    @PostMapping
    public ResultData<Void> save(@RequestBody Product product) {
        // 新增
    }
    
    @PutMapping
    public ResultData<Void> update(@RequestBody Product product) {
        // 修改
    }
    
    @DeleteMapping("/{id}")
    public ResultData<Void> delete(@PathVariable Long id) {
        // 删除（软删除）
    }
}
```

---

## 4. 前端开发任务清单

### 4.1 前端页面规划（按模块）

#### 4.1.1 系统管理模块（/sys）
1. 登录页 `/login`
2. 用户管理 `/sys/user`
3. 角色管理 `/sys/role`
4. 菜单管理 `/sys/menu`
5. 字典管理 `/sys/dict`
6. 操作日志 `/sys/log`

#### 4.1.2 组织人事模块（/org）
7. 部门管理 `/org/department`
8. 员工管理 `/org/employee`
9. 班组管理 `/org/team`
10. 班次管理 `/org/shift`

#### 4.1.3 主数据模块（/mdm）
11. 产品管理 `/mdm/product`
12. 物料管理 `/mdm/material`
13. 物料类别 `/mdm/material-category`
14. BOM管理 `/mdm/bom`
15. 供应商管理 `/mdm/supplier`
16. 客户管理 `/mdm/customer`
17. 计量单位 `/mdm/unit`

#### 4.1.4 车间管理模块（/factory）
18. 车间管理 `/factory/workshop`
19. 产线管理 `/factory/line`
20. 工位管理 `/factory/station`

#### 4.1.5 工艺管理模块（/process）
21. 工序管理 `/process/operation`
22. 工艺路线 `/process/route`
23. 工艺参数 `/process/parameter`
24. 作业指导书 `/process/instruction`

#### 4.1.6 生产制造模块（/mfg）
25. **首页看板** `/dashboard` ⭐
26. 生产订单 `/mfg/production-order` ⭐
27. 生产计划 `/mfg/plan`
28. 工单管理 `/mfg/work-order` ⭐
29. 派工单 `/mfg/dispatch`
30. 生产任务 `/mfg/task` ⭐
31. 工作报工 `/mfg/report` ⭐
32. 安灯告警 `/mfg/andon`
33. 生产异常 `/mfg/exception`

#### 4.1.7 质量管理模块（/qms）
34. 质检项目 `/qms/item`
35. 质检方案 `/qms/scheme`
36. 质检单 `/qms/inspection` ⭐
37. 不合格品 `/qms/ncr`
38. 质量统计 `/qms/statistics`

#### 4.1.8 仓储管理模块（/wms）
39. 仓库管理 `/wms/warehouse`
40. 库位管理 `/wms/location`
41. 库存查询 `/wms/inventory` ⭐
42. 入库管理 `/wms/inbound` ⭐
43. 出库管理 `/wms/outbound` ⭐
44. 退料管理 `/wms/return`
45. 盘点管理 `/wms/stocktake`
46. 库存流水 `/wms/movement`

#### 4.1.9 设备管理模块（/eam）
47. 设备台账 `/eam/equipment`
48. 设备类别 `/eam/category`
49. 保养计划 `/eam/maintenance-plan`
50. 保养记录 `/eam/maintenance-record`
51. 维修工单 `/eam/repair`
52. 点检管理 `/eam/spot-check`
53. 设备状态 `/eam/status`

#### 4.1.10 追溯管理模块（/trace）
54. 条码管理 `/trace/barcode`
55. 批次追溯 `/trace/lot`
56. 物料追溯 `/trace/material`

**预计总页面数：约56个页面**

### 4.2 前端技术要点

**1. 路由配置（动态路由）**
```javascript
// router/index.js
const routes = [
  { path: '/login', component: Login },
  {
    path: '/',
    component: Layout,
    children: [
      { path: 'dashboard', component: Dashboard },
      // 根据用户权限动态加载
    ]
  }
]
```

**2. Axios封装**
```javascript
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token;
  }
  return config;
});
```

**3. 表格分页组件**
```vue
<el-table :data="tableData" v-loading="loading">
  <el-table-column prop="productCode" label="产品编码" />
  <el-table-column prop="productName" label="产品名称" />
</el-table>
<el-pagination
  @current-change="handlePageChange"
  :current-page="page.current"
  :page-size="page.size"
  :total="page.total"
/>
```

---

## 5. 核心业务流程

### 5.1 生产订单流程（完整流程）

```
1. 创建生产订单 (mfg_production_order)
   ↓
2. 生成生产计划 (mfg_production_plan)
   ↓
3. 根据BOM和工艺路线生成工单 (mfg_work_order)
   ↓
4. 计算物料需求 (mfg_work_order_material)
   ↓
5. 创建派工单 (mfg_dispatch_order)
   ↓
6. 生成生产任务 (mfg_task)
   ↓
7. 分配操作工 (mfg_task_operator)
   ↓
8. 操作工报工 (mfg_work_report)
   ↓
9. 质检 (qms_inspection_order)
   ↓
10. 合格品入库 (wms_inbound_order)
    ↓
11. 生成产品批次 (trace_product_lot)
    ↓
12. 订单完成
```

### 5.2 物料流转流程

**入库流程**：
```
供应商送货 → 创建入库单 → 质检 → 生成物料批次 → 入库上架 → 更新库存
```

**出库流程**：
```
创建派工单 → 生成物料需求 → 创建出库单 → 仓库拣货 → 出库 → 更新库存
```

### 5.3 质检流程

```
首件质检 → 过程巡检 → 完工终检 → 不合格品处理
```

---

## 6. 开发步骤建议（分7个阶段）

### 阶段1：基础框架（3-4天）
✅ 数据库已建好（78张表）
- [ ] 后端：生成78个Entity实体类
- [ ] 后端：生成78个Mapper接口
- [ ] 前端：项目初始化

### 阶段2：系统管理模块（4-5天）
- [ ] 用户登录（JWT）
- [ ] 用户管理CRUD
- [ ] 角色管理CRUD
- [ ] 菜单管理（动态路由）
- [ ] 字典管理

### 阶段3：主数据模块（5-6天）
- [ ] 产品管理
- [ ] 物料管理
- [ ] BOM管理
- [ ] 供应商管理
- [ ] 客户管理
- [ ] 车间/产线/工位管理

### 阶段4：生产制造核心（8-10天）⭐
- [ ] 生产订单管理
- [ ] 生产计划
- [ ] 工单管理
- [ ] 派工单
- [ ] 生产任务
- [ ] 工作报工
- [ ] 首页看板（统计图表）

### 阶段5：质量管理（4-5天）
- [ ] 质检项目/方案
- [ ] 质检单管理
- [ ] 不合格品处理
- [ ] 质量统计

### 阶段6：仓储管理（5-6天）
- [ ] 仓库/库位管理
- [ ] 库存查询
- [ ] 入库管理
- [ ] 出库管理
- [ ] 退料管理
- [ ] 盘点管理

### 阶段7：设备&追溯&测试（5-6天）
- [ ] 设备台账
- [ ] 保养/维修/点检
- [ ] 条码/批次追溯
- [ ] 全流程测试
- [ ] Bug修复

**预计总工期：35-45天**

---

## 7. 数据字典（关键字段）

### 7.1 通用状态
- `status`: 启用/禁用/维护中/停用
- `deleted_flag`: 0-未删除, 1-已删除

### 7.2 生产订单状态
- `待下达` → `已下达` → `生产中` → `已完成` → `已关闭`

### 7.3 工单状态
- `未开工` → `已开工` → `生产中` → `已完工` → `已入库`

### 7.4 质检类型
- `首件质检` / `过程巡检` / `完工终检` / `来料检验`

### 7.5 出入库类型
- 入库：`采购入库` / `生产入库` / `退货入库`
- 出库：`生产领料` / `销售出库` / `调拨出库`

---

## 8. 附录

### 8.1 快速启动命令

**查看所有表**：
```bash
java ShowTablesUTF8
cat database_tables.txt
```

**启动后端**：
```bash
./gradlew bootRun
```

**前端初始化**：
```bash
cd frontend
npm create vite@latest . -- --template vue
npm install
npm install vue-router pinia axios element-plus @element-plus/icons-vue
npm install echarts
npm run dev
```

---

**文档版本**: v2.0（基于实际数据库）  
**创建日期**: 2026-07-08  
**数据库表数**: 78张  
**适用项目**: MES制造执行系统
