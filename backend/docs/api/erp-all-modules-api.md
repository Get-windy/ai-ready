# ERP 全模块 RESTful API 清单

> 本文档涵盖: 产品资料完善 | 往来单位管理 | 客户等级价格关联 | 营销管理(11种子类型) | 销售/采购取价逻辑

---

## 模块一：产品资料完善

### API 前缀: `/api/erp/product`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/attributes/defs` | 查询产品属性定义列表 |
| POST | `/attributes/defs` | 新增属性定义 |
| PUT | `/attributes/defs/{id}` | 更新属性定义 |
| DELETE | `/attributes/defs/{id}` | 删除属性定义 |
| GET | `/attributes/defs/{id}/options` | 查询属性选项 |
| POST | `/attributes/options` | 新增属性选项 |
| PUT | `/attributes/options/{id}` | 更新属性选项 |
| DELETE | `/attributes/options/{id}` | 删除属性选项 |
| GET | `/{productId}/attributes` | 查询产品属性值 |
| PUT | `/{productId}/attributes` | 批量保存产品属性值 |
| GET | `/{productId}/units` | 查询产品多单位列表 |
| POST | `/units` | 新增产品单位 |
| PUT | `/units/{id}` | 更新产品单位 |
| DELETE | `/units/{id}` | 删除产品单位 |
| GET | `/{productId}/barcodes` | 查询产品条形码列表 |
| POST | `/barcodes` | 新增条形码 |
| PUT | `/barcodes/{id}` | 更新条形码 |
| DELETE | `/barcodes/{id}` | 删除条形码 |
| GET | `/{productId}/attachments` | 查询产品附件列表 |
| POST | `/attachments/upload` | 上传附件 |
| DELETE | `/attachments/{id}` | 删除附件 |
| GET | `/{productId}/related` | 查询关联产品列表 |
| POST | `/related` | 新增产品关联 |
| DELETE | `/related/{id}` | 删除产品关联 |
| GET | `/sku-rules` | 查询SKU生成规则 |
| POST | `/sku-rules` | 新增SKU生成规则 |
| PUT | `/sku-rules/{id}` | 更新SKU生成规则 |
| PUT | `/sku-rules/{id}/set-default` | 设置默认规则 |
| POST | `/generate-sku` | 根据规则生成SKU |

### erp_product 扩展字段

| 方法 | 路径 | 说明 |
|------|------|------|
| PUT | `/{id}/approval` | 产品审批(提交/通过/驳回) |

---

## 模块二：往来单位统一管理

### API 前缀: `/api/erp/partner`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/categories/tree` | 获取分类树 |
| POST | `/categories` | 新增分类 |
| PUT | `/categories/{id}` | 更新分类 |
| DELETE | `/categories/{id}` | 删除分类 |
| GET | `/grades` | 查询等级列表(按type筛选) |
| POST | `/grades` | 新增等级 |
| PUT | `/grades/{id}` | 更新等级 |
| DELETE | `/grades/{id}` | 删除等级 |
| GET | `/page` | 分页查询往来单位 |
| GET | `/{id}` | 查询往来单位详情 |
| POST | `` | 新增往来单位 |
| PUT | `/{id}` | 更新往来单位 |
| PUT | `/{id}/status` | 启用/停用 |
| DELETE | `/{id}` | 删除往来单位 |
| GET | `/search` | 搜索往来单位(下拉选择) |
| GET | `/{partnerId}/contacts` | 查询联系人列表 |
| POST | `/contacts` | 新增联系人 |
| PUT | `/contacts/{id}` | 更新联系人 |
| DELETE | `/contacts/{id}` | 删除联系人 |
| GET | `/{partnerId}/addresses` | 查询地址列表 |
| POST | `/addresses` | 新增地址 |
| PUT | `/addresses/{id}` | 更新地址 |
| DELETE | `/addresses/{id}` | 删除地址 |
| PUT | `/addresses/{id}/set-default` | 设为默认地址 |
| GET | `/{partnerId}/bank-accounts` | 查询银行账户列表 |
| POST | `/bank-accounts` | 新增银行账户 |
| PUT | `/bank-accounts/{id}` | 更新银行账户 |
| DELETE | `/bank-accounts/{id}` | 删除银行账户 |
| GET | `/tags` | 查询标签列表 |
| POST | `/tags` | 新增标签 |
| PUT | `/tags/{id}` | 更新标签 |
| DELETE | `/tags/{id}` | 删除标签 |
| POST | `/tags/attach` | 批量设置标签 |
| GET | `/{partnerId}/balance` | 查询余额 |
| GET | `/{partnerId}/balance-logs` | 查询余额流水 |
| GET | `/{partnerId}/statistics` | 查询客户统计(订单/金额) |

---

## 模块三：客户等级与产品价格关联

### API 前缀: `/api/erp/pricing`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/grade-prices` | 查询等级价格列表 |
| POST | `/grade-prices` | 新增等级价格 |
| PUT | `/grade-prices/{id}` | 更新等级价格 |
| DELETE | `/grade-prices/{id}` | 删除等级价格 |
| PUT | `/grade-prices/batch` | 批量更新等级价格 |
| GET | `/partner-grade-prices` | 查询客户等级-产品价格关联 |
| POST | `/partner-grade-prices` | 新增关联 |
| PUT | `/partner-grade-prices/batch` | 批量设置 |
| GET | `/customer-prices` | 查询客户特定价格 |
| POST | `/customer-prices` | 新增客户特定价格 |
| PUT | `/customer-prices/{id}` | 更新 |
| DELETE | `/customer-prices/{id}` | 删除 |
| PUT | `/customer-prices/batch` | 批量导入客户价格 |
| GET | `/calculate` | 根据取价规则计算价格 |
| GET | `/price-history/{productId}/{partnerId}` | 查询交易价格历史 |

---

## 模块四：营销管理

### API 前缀: `/api/erp/marketing`

#### 4.1 营销规则通用

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/rules/page` | 分页查询规则 |
| GET | `/rules/{id}` | 查询规则详情 |
| POST | `/rules` | 新增规则 |
| PUT | `/rules/{id}` | 更新规则 |
| DELETE | `/rules/{id}` | 删除规则 |
| PUT | `/rules/{id}/status` | 更新规则状态(发布/暂停/结束) |
| GET | `/rules/{id}/products` | 查询规则适用产品 |
| POST | `/rules/{id}/products` | 设置规则适用产品 |
| GET | `/rules/{id}/partners` | 查询规则适用客户 |
| POST | `/rules/{id}/partners` | 设置规则适用客户 |

#### 4.2 折扣规则 (DISCOUNT)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/discounts/{ruleId}` | 查询折扣配置 |
| PUT | `/discounts/{ruleId}` | 保存折扣配置 |

#### 4.3 满减规则 (THRESHOLD)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/thresholds/{ruleId}` | 查询满减规则列表(多级阶梯) |
| PUT | `/thresholds/{ruleId}` | 保存满减规则 |
| POST | `/thresholds/{ruleId}/levels` | 新增满减阶梯级 |

#### 4.4 免邮规则 (FREIGHT_DISCOUNT)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/freight/{ruleId}` | 查询免邮配置 |
| PUT | `/freight/{ruleId}` | 保存免邮配置 |

#### 4.5 赠品规则 (GIFT)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/gifts/{ruleId}` | 查询赠品配置 |
| PUT | `/gifts/{ruleId}` | 保存赠品配置 |

#### 4.6 阶梯价规则 (TIERED)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/tiered/{ruleId}` | 查询阶梯价配置 |
| PUT | `/tiered/{ruleId}` | 批量保存阶梯价 |

#### 4.7 团购 (GROUP_BUY)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/group-buy/page` | 分页查询团购活动 |
| GET | `/group-buy/{id}` | 查询团购详情 |
| POST | `/group-buy` | 新建团购活动 |
| PUT | `/group-buy/{id}` | 更新团购活动 |
| PUT | `/group-buy/{id}/status` | 更新活动状态 |
| GET | `/group-buy/{id}/participants` | 查询参与记录 |
| GET | `/group-buy/groups/{groupId}` | 查询团详情 |

#### 4.8 佣金规则 (COMMISSION)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/commission-rules/page` | 分页查询佣金规则 |
| POST | `/commission-rules` | 新增佣金规则 |
| PUT | `/commission-rules/{id}` | 更新 |
| DELETE | `/commission-rules/{id}` | 删除 |
| GET | `/settlement-config` | 查询结算配置 |
| PUT | `/settlement-config` | 更新结算配置 |

#### 4.9 分享佣金 (SHARE)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/share-activities/page` | 分页查询分享活动 |
| POST | `/share-activities` | 新建分享活动 |
| PUT | `/share-activities/{id}` | 更新 |
| GET | `/share-activities/{id}/relations` | 查询分享关系链 |
| GET | `/share-activities/{id}/commission-details` | 查询分享佣金明细 |
| PUT | `/share-commission/{id}/settle` | 结算分享佣金 |

#### 4.10 推广佣金 (PROMOTION)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/channels/page` | 分页查询推广渠道 |
| POST | `/channels` | 新增渠道 |
| PUT | `/channels/{id}` | 更新 |
| DELETE | `/channels/{id}` | 删除 |
| GET | `/promotions/page` | 分页查询推广活动 |
| POST | `/promotions` | 新建推广活动 |
| PUT | `/promotions/{id}` | 更新 |
| PUT | `/promotions/{id}/status` | 更新活动状态 |
| GET | `/promotions/{id}/logs` | 查询推广记录 |
| GET | `/settlements/page` | 分页查询结算记录 |
| POST | `/settlements` | 创建结算单 |
| PUT | `/settlements/{id}/approve` | 审核结算单 |
| PUT | `/settlements/{id}/pay` | 确认付款 |

#### 4.11 余额/提现

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/balances/{partnerId}` | 查询用户余额 |
| POST | `/balances/recharge` | 充值(后台) |
| POST | `/balances/adjust` | 余额调整(后台) |
| GET | `/balance-logs/page` | 分页查询余额流水 |
| GET | `/withdraws/page` | 分页查询提现申请 |
| POST | `/withdraws` | 提交提现申请 |
| PUT | `/withdraws/{id}/approve` | 审核提现 |
| PUT | `/withdraws/{id}/pay` | 确认打款 |
| PUT | `/withdraws/{id}/reject` | 驳回提现 |

---

## 模块五：取价逻辑引擎

### API 前缀: `/api/erp/pricing`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/configs` | 查询取价配置 |
| PUT | `/configs/{id}` | 更新取价配置 |
| GET | `/resolve` | 解析最终价格 |
| GET | `/price-memory/page` | 分页查询价格记忆 |
| POST | `/price-memory` | 记录交易价格 |
| GET | `/price-memory/latest/{productId}/{partnerId}` | 查询最新交易价格 |

> **resolve 参数**: `?productId=1&partnerId=2&bizType=SALE&quantity=10`
> 返回: `{source, price, priceList: [{source, label, price}]}`

---

## 通用开发约定

### 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 分页请求
```json
// 请求: ?pageNum=1&pageSize=20&keyword=&status=
// 响应:
{
  "records": [...],
  "total": 100,
  "pageNum": 1,
  "pageSize": 20
}
```

### 软删除
- 所有表使用 `deleted INTEGER DEFAULT 0` 逻辑删除
- DELETE 请求统一标记 `deleted=1`
- 所有 GET 查询自动过滤 `deleted=0`

### 租户隔离
- 所有业务表包含 `tenant_id` 字段
- 服务层自动从 SecurityContext 获取当前租户ID
