# ERP 系统 UI 组件库设计规范

## 项目概述
基于 ERP 系统的业务需求，设计并实现用户界面组件库，包括通用组件和业务组件。

## 技术规范
- **技术栈**: Vue 3.4 + TypeScript + Vite
- **UI 框架**: Element Plus 2.4
- **图表库**: ECharts 5.4
- **设计原则**: 模块化、可扩展、主题定制、国际化支持

## 组件架构

### 1. 通用基础组件
#### 1.1 已实现组件
- ✅ `ARButton` - 增强型按钮组件
- ⏳ `ARInput` - 增强型输入框组件
- ⏳ `ARSelect` - 选择器组件

#### 1.2 待实现通用组件
- ⬜ `ARForm` - 表单组件（封装 Element Plus Form）
- ⬜ `ARTable` - 表格组件（封装 Element Plus Table）
- ⬜ `ARDialog` - 对话框组件
- ⬜ `ARModal` - 模态框组件
- ⬜ `ARCard` - 卡片组件
- ⬜ `ARTabs` - 标签页组件
- ⬜ `ARBadge` - 徽章组件
- ⬜ `ARAvatar` - 头像组件
- ⬜ `ARTooltip` - 工具提示组件
- ⬜ `ARPopover` - 弹出框组件
- ⬜ `ARDropdown` - 下拉菜单组件
- ⬜ `ARMention` - 提及组件
- ⬜ `ARTag` - 标签组件
- ⬜ `ARProgress` - 进度条组件
- ⬜ `ARSlider` - 滑块组件
- ⬜ `ARSwitch` - 开关组件
- ⬜ `ARCheckbox` - 复选框组件
- ⬜ `ARRadio` - 单选框组件
- ⬜ `ARInputNumber` - 数字输入框
- ⬜ `ARDatePicker` - 日期选择器
- ⬜ `ARTree` - 树形组件
- ⬜ `ARTimeline` - 时间轴组件
- ⬜ `ARCascader` - 级联选择器
- ⬜ `ARUpload` - 上传组件

### 2. ERP 业务组件
#### 2.1 采购管理组件
- ⬜ `PurchaseOrderForm` - 采购订单表单
- ⬜ `PurchaseOrderTable` - 采购订单表格
- ⬜ `PurchaseInvoiceView` - 采购发票视图
- ⬜ `SupplierManagement` - 供应商管理

#### 2.2 库存管理组件
- ⬜ `InventoryDashboard` - 库存仪表板
- ⬜ `InventoryTable` - 库存表格
- ⬜ `StockMovementChart` - 库存移动图表
- ⬜ `WarehouseManagement` - 仓库管理
- ⬜ `InventoryAlert` - 库存预警组件

#### 2.3 财务管理组件
- ⬜ `FinancialChart` - 财务图表
- ⬜ `FinancialReportTable` - 财务报表表格
- ⬜ `BalanceSheetView` - 资产负债表视图
- ⬜ `IncomeStatementView` - 损益表视图

#### 2.4 销售管理组件
- ⬜ `SalesOrderForm` - 销售订单表单
- ⬜ `SalesDashboard` - 销售仪表板
- ⬜ `CustomerManagement` - 客户管理

## 主题系统设计
### CSS 变量体系
```
:root {
  /* 主色调 */
  --ar-color-primary: #409eff;
  --ar-color-success: #67c23a;
  --ar-color-warning: #e6a23c;
  --ar-color-danger: #f56c6c;
  --ar-color-info: #909399;
  
  /* 中性色 */
  --ar-bg-color: #f0f2f5;
  --ar-text-color-primary: #303133;
  --ar-text-color-regular: #606266;
  --ar-text-color-secondary: #909399;
  --ar-border-color-base: #dcdfe6;
  
  /* 字体 */
  --ar-font-family: 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  --ar-font-size-base: 14px;
  --ar-font-size-small: 13px;
  --ar-font-size-large: 16px;
  
  /* 圆角 */
  --ar-border-radius-base: 4px;
  --ar-border-radius-small: 2px;
  --ar-border-radius-round: 20px;
  
  /* 间距 */
  --ar-spacing-xs: 4px;
  --ar-spacing-sm: 8px;
  --ar-spacing-md: 12px;
  --ar-spacing-lg: 16px;
  --ar-spacing-xl: 24px;
}
```

### 深色主题变量
```
[data-theme="dark"] {
  --ar-bg-color: #141414;
  --ar-text-color-primary: #e5e5e5;
  --ar-text-color-regular: #a9a9a9;
  --ar-text-color-secondary: #8c8c8c;
  --ar-border-color-base: #434343;
  
  --ar-color-primary: #1668dc;
  --ar-color-success: #49aa19;
  --ar-color-warning: #d89614;
  --ar-color-danger: #dc4446;
  --ar-color-info: #595959;
}
```

## 国际化支持
### 语言包结构
```
locales/
├── zh-CN/
│   ├── common.json
│   ├── erp.json
│   ├── purchase.json
│   └── inventory.json
├── en-US/
│   ├── common.json
│   ├── erp.json
│   └── ...
└── index.ts
```

### 组件国际化示例
```typescript
// 在组件中使用国际化
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

// 组件标签国际化
<ARButton>{{ t('common.submit') }}</ARButton>
```

## 实现优先级
### 第一阶段（本任务完成）
1. ✅ 完成基础组件架构设计
2. ⬜ 实现剩余基础通用组件（表单、表格、弹窗）
3. ⬜ 实现核心ERP业务组件（采购、库存、财务）
4. ⬜ 完善主题定制系统
5. ⬜ 添加国际化支持

### 第二阶段（后续迭代）
1. 优化组件性能
2. 添加单元测试
3. 完善组件文档
4. 创建组件示例页面
5. 添加无障碍支持

## 组件开发规范
1. **命名规范**: 组件使用 PascalCase，变量使用 camelCase
2. **类型定义**: 所有组件必须有完整的 TypeScript 接口定义
3. **Props 设计**: 遵循 Element Plus 的 Props 命名规范
4. **样式隔离**: 使用 scoped CSS 或 CSS Modules
5. **文档要求**: 每个组件必须包含 README.md 和使用示例
6. **测试要求**: 组件必须有基本的单元测试
7. **国际化**: 所有文本内容必须支持国际化

## 验收标准
1. ✅ 设计完整的组件库架构
2. ⬜ 实现通用UI组件（按钮、表单、表格、弹窗等）
3. ⬜ 开发业务专用组件（采购、库存、财务等）
4. ⬜ 支持主题定制和国际化
5. ⬜ 提供组件使用文档和示例