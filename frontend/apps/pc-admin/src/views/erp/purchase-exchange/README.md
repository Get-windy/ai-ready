# 采购换货管理模块

## 功能概述

采购换货管理模块是 ERP 系统采购流程的重要补充，用于处理采购退货和换货业务场景。

## 功能特性

### 1. 换货单列表页面
- 支持按换货单号、原采购订单、供应商、状态、换货日期筛选
- 表格展示换货单基本信息
- 分页功能
- 导出功能

### 2. 换货单创建/编辑页面
- 基于原采购订单创建换货单
- 支持选择换货类型（质量问题、规格不符、数量错误、其他）
- 填写换货原因和备注
- 编辑换货商品明细（数量、单价）
- 自动计算换货总金额

### 3. 换货审批流程页面
- 查看换货单基本信息
- 审批通过/拒绝操作
- 填写审批意见

### 4. 换货单详情查看页面
- 展示换货单完整信息
- 显示换货明细列表
- 显示审批记录时间线

### 5. 换货单跟踪查询页面
- 流程跟踪时间线
- 换货明细展示
- 审批记录表格

## 技术实现

### 技术栈
- Vue 3 + TypeScript
- Ant Design Vue
- Pinia 状态管理
- Vitest 单元测试

### 文件结构
```
purchase-exchange/
├── index.vue                    # 换货单列表页面
├── components/
│   ├── ExchangeFormModal.vue    # 创建/编辑弹窗
│   ├── ExchangeApproveModal.vue # 审批弹窗
│   ├── ExchangeDetailModal.vue  # 详情弹窗
│   └── ExchangeTrackModal.vue   # 跟踪弹窗
├── __tests__/
│   ├── index.spec.ts            # 列表页面测试
│   ├── ExchangeFormModal.spec.ts
│   ├── ExchangeApproveModal.spec.ts
│   └── ExchangeDetailModal.spec.ts
└── README.md                    # 本文档
```

### API 接口

#### 换货单管理
- `GET /erp/purchase/exchange/page` - 分页查询换货单
- `GET /erp/purchase/exchange/{id}` - 获取换货单详情
- `POST /erp/purchase/exchange` - 创建换货单
- `PUT /erp/purchase/exchange/{id}` - 更新换货单
- `DELETE /erp/purchase/exchange/{id}` - 删除换货单

#### 审批流程
- `POST /erp/purchase/exchange/{id}/submit` - 提交审批
- `POST /erp/purchase/exchange/{id}/approve` - 审批通过
- `POST /erp/purchase/exchange/{id}/reject` - 审批拒绝
- `POST /erp/purchase/exchange/{id}/cancel` - 取消换货单
- `POST /erp/purchase/exchange/{id}/complete` - 完成换货单

#### 查询接口
- `GET /erp/purchase/exchange/{id}/items` - 获取换货单明细
- `GET /erp/purchase/exchange/{id}/approval-records` - 获取审批记录
- `GET /erp/purchase/exchange/{id}/tracking` - 获取跟踪信息
- `GET /erp/purchase/exchange/export` - 导出换货单

## 数据结构

### 换货单状态
```typescript
enum ExchangeStatus {
  DRAFT = 0,           // 草稿
  PENDING_APPROVAL = 1, // 待审批
  APPROVED = 2,        // 已审批
  EXCHANGING = 3,      // 换货中
  COMPLETED = 4,       // 已完成
  REJECTED = 5,        // 已拒绝
  CANCELLED = 6        // 已取消
}
```

### 换货类型
- 1: 质量问题
- 2: 规格不符
- 3: 数量错误
- 4: 其他

## 使用说明

### 创建换货单
1. 点击"新建换货单"按钮
2. 选择原采购订单
3. 填写换货日期、换货类型、换货原因
4. 编辑换货商品明细（修改换货数量和单价）
5. 点击确定提交

### 审批换货单
1. 在列表中找到状态为"待审批"的换货单
2. 点击"审批"按钮
3. 选择审批结果（通过/拒绝）
4. 填写审批意见
5. 点击确定提交

### 查看换货单详情
1. 点击换货单行的"查看"按钮
2. 查看换货单完整信息和明细
3. 查看审批记录时间线

### 跟踪换货单
1. 点击换货单行的"跟踪"按钮
2. 查看流程跟踪时间线
3. 查看换货明细和审批记录

## 测试

### 运行单元测试
```bash
npm run test -- src/views/erp/purchase-exchange/__tests__
```

### 测试覆盖率
- 列表页面功能测试
- 表单弹窗功能测试
- 审批弹窗功能测试
- 详情弹窗功能测试

## 注意事项

1. 只有草稿状态的换货单可以编辑和删除
2. 只有待审批状态的换货单可以审批
3. 换货数量不能超过原采购数量
4. 换货单创建后需要提交审批才能进入后续流程

## 后续优化

1. 集成库存模块，实现库存回滚
2. 集成财务模块，实现财务冲销
3. 添加更多筛选条件
4. 支持批量操作
5. 添加数据权限控制
