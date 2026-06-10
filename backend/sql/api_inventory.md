# ERP 核心业务模块 API 清单

> 技术栈: Spring Boot + MyBatis-Plus  
> API 前缀: `/api/erp`  
> 统一响应: `ApiResponse<T>` | `PageResult<T>`  
> 分页参数: `current`, `size` (query params)  
> 审计字段由 MyBatis-Plus MetaObjectHandler 自动填充

---

## 模块1: 产品资料管理

### 产品分类 API (`/api/erp/product-category`)

| 方法   | 路径                    | 说明                      | 参数                              |
|--------|-------------------------|---------------------------|-----------------------------------|
| GET    | `/tree`                 | 获取分类树(全量)          | -                                 |
| GET    | `/{id}`                 | 获取分类详情              | Path: id                         |
| GET    | `/children/{parentId}`  | 获取子分类列表            | Path: parentId                   |
| POST   | `/`                     | 新增分类                  | Body: categoryCode, categoryName, parentId, sortOrder |
| PUT    | `/{id}`                 | 编辑分类                  | Body: 同上(可部分更新)            |
| PUT    | `/{id}/sort`            | 拖拽排序更新              | Body: sortOrder, parentId(如需移动) |
| DELETE | `/{id}`                 | 删除分类(检查是否有子节点和产品) | -                            |

### 产品等级 API (`/api/erp/product-grade`)

| 方法   | 路径       | 说明             | 参数                   |
|--------|------------|------------------|------------------------|
| GET    | `/list`    | 获取全部等级列表  | -                      |
| GET    | `/{id}`    | 等级详情          | Path: id               |
| POST   | `/`        | 新增等级          | Body: gradeCode, gradeName, gradeLevel |
| PUT    | `/{id}`    | 编辑等级          | Body: 同上(可部分更新) |
| DELETE | `/{id}`    | 删除等级(检查引用) | -                      |

### 产品管理 API (`/api/erp/product`)

| 方法   | 路径                    | 说明                       | 参数                           |
|--------|-------------------------|----------------------------|----------------------------------|
| GET    | `/page`                 | 分页查询产品列表(可分类筛选) | Query: categoryId, keyword, status, current, size |
| GET    | `/{id}`                 | 产品详情(含等级价格)        | Path: id                        |
| GET    | `/by-code/{productCode}`| 按编码查询                  | Path: productCode               |
| POST   | `/`                     | 新增产品                    | Body: productCode, productName, categoryId, baseUnit, ... |
| PUT    | `/{id}`                 | 编辑产品                    | Body: 产品字段                   |
| PUT    | `/{id}/status`          | 启/停用产品                 | Query: status(1/0)              |
| DELETE | `/{id}`                 | 删除产品(检查引用)          | -                                |
| GET    | `/batch`                | 批量查询(按ID列表)          | Query: ids(逗号分隔)             |

### 产品等级价格 API (`/api/erp/product-grade-price`)

| 方法   | 路径                         | 说明                  | 参数                       |
|--------|------------------------------|-----------------------|----------------------------|
| GET    | `/by-product/{productId}`    | 获取某产品的等级价格列表 | Path: productId            |
| POST   | `/batch-save`                | 批量保存等级价格        | Body: [{productId, productGradeId, price}, ...] |
| POST   | `/`                          | 新增单条等级价格        | Body: productId, productGradeId, price |
| PUT    | `/{id}`                      | 编辑等级价格            | Body: price, isActive      |
| DELETE | `/{id}`                      | 删除等级价格            | -                          |

---

## 模块2: 往来单位统一管理

### 往来单位分类 API (`/api/erp/party-category`)

> 利用现有 `biz_party_category` 表,扩展API

| 方法   | 路径                   | 说明                      | 参数                     |
|--------|------------------------|---------------------------|--------------------------|
| GET    | `/tree`                | 获取分类树(按单位类型筛选) | Query: partyType(可选)   |
| GET    | `/{id}`                | 分类详情                  | Path: id                 |
| GET    | `/children/{parentId}` | 获取子分类                | Path: parentId           |
| POST   | `/`                    | 新增分类                  | Body: categoryName, partyType, parentId, sortOrder |
| PUT    | `/{id}`                | 编辑分类                  | Body: 同上               |
| PUT    | `/{id}/sort`           | 排序更新                  | Body: sortOrder, parentId |
| DELETE | `/{id}`                | 删除分类(检查引用)        | -                        |

### 往来单位 API (`/api/erp/party`)

> 扩展现有 `PartyController`,补充接口

| 方法   | 路径                    | 说明                         | 参数                              |
|--------|-------------------------|------------------------------|-----------------------------------|
| GET    | `/page`                 | 分页查询(含类型/分类筛选)     | Query: partyType, categoryId, keyword, status, current, size |
| GET    | `/{id}`                 | 详情(含等级、地址、联系人)    | Path: id                          |
| GET    | `/by-code/{partyCode}`  | 按编码查询                   | Path: partyCode                   |
| POST   | `/`                     | 新增                         | Body: partyName, partyType, categoryId, partyLevel, ... |
| PUT    | `/{id}`                 | 编辑                         | Body: 单位字段                     |
| PUT    | `/{id}/status`          | 启/停用                      | Query: status(1/0)                |
| DELETE | `/{id}`                 | 删除(检查交易记录)           | -                                  |
| GET    | `/options`              | 下拉选项列表(简版)           | Query: partyType, keyword         |

### 往来单位地址 API (`/api/erp/party-address`)

| 方法   | 路径                          | 说明                 | 参数                  |
|--------|-------------------------------|----------------------|-----------------------|
| GET    | `/by-party/{partyId}`         | 获取单位地址列表      | Path: partyId         |
| POST   | `/`                           | 新增地址             | Body: partyId, ...    |
| PUT    | `/{id}`                       | 编辑地址             | Body: 地址字段         |
| PUT    | `/{id}/default`               | 设为默认地址          | -                     |
| DELETE | `/{id}`                       | 删除地址              | -                     |

### 往来单位联系人 API (`/api/erp/party-contact`)

| 方法   | 路径                          | 说明                 | 参数                  |
|--------|-------------------------------|----------------------|-----------------------|
| GET    | `/by-party/{partyId}`         | 获取联系人列表        | Path: partyId         |
| POST   | `/`                           | 新增联系人            | Body: partyId, ...    |
| PUT    | `/{id}`                       | 编辑联系人            | Body: 联系人字段       |
| PUT    | `/{id}/primary`               | 设为主要联系人        | -                     |
| DELETE | `/{id}`                       | 删除联系人            | -                     |

---

## 模块3: 客户等级与产品等级定价矩阵

### 定价矩阵 API (`/api/erp/pricing-matrix`)

| 方法   | 路径                               | 说明                        | 参数                               |
|--------|------------------------------------|-----------------------------|------------------------------------|
| GET    | `/page`                            | 分页查询矩阵                 | Query: customerGradeId, productGradeId, keyword |
| GET    | `/matrix`                          | 获取完整定价矩阵(二维表格)    | -                                  |
| GET    | `/{id}`                            | 详情                        | Path: id                           |
| POST   | `/batch-save`                      | 批量保存矩阵(全量覆盖)       | Body: [{customerGradeId, productGradeId, price}, ...] |
| POST   | `/`                                | 新增矩阵行                  | Body: customerGradeId, productGradeId, price |
| PUT    | `/{id}`                            | 编辑矩阵行                  | Body: price, discountRate          |
| DELETE | `/{id}`                            | 删除矩阵行                  | -                                   |
| GET    | `/preview`                         | 预览矩阵效果(根据当前配置)   | Query: customerGradeId(可选)        |

> **说明**: `/matrix` 返回二维结构便于前端渲染表格:  
> ```json
> {
>   "customerGrades": [{ "id": 1, "gradeName": "VIP" }, ...],
>   "productGrades": [{ "id": 1, "gradeName": "A级" }, ...],
>   "data": {
>     "1": { "1": 80, "2": 90 },
>     "2": { "1": 90, "2": 100 }
>   }
> }
> ```

---

## 模块4: 营销管理

### 营销规则 API (`/api/erp/marketing-rule`)

| 方法   | 路径                    | 说明                     | 参数                           |
|--------|-------------------------|--------------------------|--------------------------------|
| GET    | `/page`                 | 分页查询(按类型/状态筛选)  | Query: ruleType, status, keyword, current, size |
| GET    | `/{id}`                 | 详情(含优惠明细)          | Path: id                      |
| GET    | `/by-product/{productId}` | 获取适用于某产品的规则   | Query: partnerId(客户ID,用于等级筛选) |
| POST   | `/`                     | 新增规则(含明细)          | Body: ruleName, ruleType, priority, startTime, endTime, details[...] |
| PUT    | `/{id}`                 | 编辑规则                  | Body: 规则字段                  |
| PUT    | `/{id}/status`          | 更新状态(启用/停用)       | Query: status(1/0)             |
| DELETE | `/{id}`                 | 删除规则                  | -                               |

### 营销规则明细 API (`/api/erp/marketing-rule-detail`)

| 方法   | 路径                        | 说明             | 参数              |
|--------|-----------------------------|------------------|-------------------|
| GET    | `/by-rule/{ruleId}`         | 获取规则优惠明细   | Path: ruleId     |
| POST   | `/batch-save`               | 批量保存明细       | Body: ruleId, details[...] |
| POST   | `/`                         | 新增单条明细       | Body: ruleId, detailType, ... |
| PUT    | `/{id}`                     | 编辑明细           | Body: 字段         |
| DELETE | `/{id}`                     | 删除明细           | -                  |

### 满减运费规则 API (`/api/erp/freight-discount-rule`)

| 方法   | 路径                    | 说明             | 参数                   |
|--------|-------------------------|------------------|------------------------|
| GET    | `/page`                 | 分页查询          | Query: status, keyword |
| GET    | `/{id}`                 | 详情              | Path: id               |
| POST   | `/`                     | 新增              | Body: 规则字段          |
| PUT    | `/{id}`                 | 编辑              | Body: 规则字段          |
| DELETE | `/{id}`                 | 删除              | -                      |
| GET    | `/calculate`            | 计算运费优惠       | Query: orderAmount, quantity, weight, regionId, customerGradeId |

### 团购活动 API (`/api/erp/group-buy`)

| 方法   | 路径                    | 说明                     | 参数                          |
|--------|-------------------------|--------------------------|-------------------------------|
| GET    | `/page`                 | 分页查询                  | Query: status, keyword        |
| GET    | `/{id}`                 | 详情(含参团列表)          | Path: id                      |
| POST   | `/`                     | 新增团购活动              | Body: activityName, productId, groupPrice, groupMinCount, startTime, endTime |
| PUT    | `/{id}`                 | 编辑活动                  | Body: 字段                     |
| PUT    | `/{id}/status`          | 更新状态                  | Query: status                 |
| DELETE | `/{id}`                 | 删除活动                  | -                              |
| PUT    | `/{id}/simulate-group`  | 模拟成团                  | Query: simulateCount          |

### 团购参团 API (`/api/erp/group-buy-participant`)

| 方法   | 路径                          | 说明             | 参数                     |
|--------|-------------------------------|------------------|--------------------------|
| GET    | `/by-activity/{activityId}`   | 活动参团列表      | Path: activityId         |
| GET    | `/by-partner/{partnerId}`     | 用户参团记录      | Path: partnerId          |
| POST   | `/join`                       | 参团              | Body: activityId, partnerId, quantity |
| PUT    | `/{id}/cancel`                | 取消参团          | -                        |

### 一客一价 API (`/api/erp/customer-product-price`)

| 方法   | 路径                         | 说明                | 参数                          |
|--------|------------------------------|---------------------|-------------------------------|
| GET    | `/by-partner/{partnerId}`    | 获取客户专属价格列表  | Path: partnerId               |
| GET    | `/by-product/{productId}`    | 获取产品专属价格列表  | Path: productId               |
| POST   | `/`                          | 新增/覆盖专属价格    | Body: partnerId, productId, customPrice |
| POST   | `/batch-import`              | 批量导入(Excel)      | Multipart: file               |
| PUT    | `/{id}`                      | 编辑                | Body: customPrice, status     |
| DELETE | `/{id}`                      | 删除                | -                              |
| GET    | `/check`                     | 检查是否存在专属价格  | Query: partnerId, productId   |

### 分佣规则 API (`/api/erp/commission-rule`)

| 方法   | 路径                | 说明             | 参数                   |
|--------|---------------------|------------------|------------------------|
| GET    | `/page`             | 分页查询          | Query: status, keyword |
| GET    | `/{id}`             | 详情              | Path: id               |
| POST   | `/`                 | 新增规则          | Body: 分佣规则字段       |
| PUT    | `/{id}`             | 编辑规则          | Body: 字段              |
| DELETE | `/{id}`             | 删除规则          | -                       |
| GET    | `/calculate`        | 试算分佣金额       | Query: salesAmount, productId, salespersonId |

### 裂变活动 API (`/api/erp/share-activity`)

| 方法   | 路径                        | 说明                     | 参数                         |
|--------|-----------------------------|--------------------------|------------------------------|
| GET    | `/page`                     | 分页查询                  | Query: status, keyword       |
| GET    | `/{id}`                     | 详情(含佣金配置)          | Path: id                     |
| POST   | `/`                         | 新增活动                  | Body: activityName, commissionType, level123Rate, ... |
| PUT    | `/{id}`                     | 编辑活动                  | Body: 字段                    |
| PUT    | `/{id}/status`              | 更新状态                  | Query: status                 |
| DELETE | `/{id}`                     | 删除活动                  | -                              |
| GET    | `/{id}/statistics`          | 活动数据统计(邀请/注册/下单) | -                           |

### 分享关系 API (`/api/erp/share-relation`)

| 方法   | 路径                              | 说明                     | 参数                      |
|--------|-----------------------------------|--------------------------|---------------------------|
| GET    | `/by-inviter/{inviterId}`         | 某人的邀请列表(下级)      | Path: inviterId           |
| GET    | `/by-invitee/{inviteeId}`         | 被邀请记录               | Path: inviteeId           |
| GET    | `/tree/{inviterId}`               | 裂变关系树(多级)         | Path: inviterId           |
| POST   | `/generate-code`                   | 生成分享链接/二维码       | Body: activityId, inviterId |
| PUT    | `/{id}/bind-order`                 | 绑定首单订单             | Body: orderId, orderAmount |

### 裂变提成明细 API (`/api/erp/share-commission`)

| 方法   | 路径                          | 说明                | 参数                   |
|--------|-------------------------------|---------------------|------------------------|
| GET    | `/by-inviter/{inviterId}`     | 邀请人提成列表       | Path: inviterId        |
| GET    | `/by-activity/{activityId}`   | 活动提成汇总         | Path: activityId       |
| GET    | `/page`                       | 分页查询             | Query: status, partnerId |
| PUT    | `/{id}/settle`                | 单条结算             | -                      |
| POST   | `/batch-settle`               | 批量结算             | Body: ids[]            |

### 推广渠道 API (`/api/erp/promotion-channel`)

| 方法   | 路径                    | 说明             | 参数                   |
|--------|-------------------------|------------------|------------------------|
| GET    | `/page`                 | 分页查询          | Query: channelType, status |
| GET    | `/{id}`                 | 详情              | Path: id               |
| POST   | `/`                     | 新增渠道          | Body: channelCode, channelName, channelType, ... |
| PUT    | `/{id}`                 | 编辑渠道          | Body: 字段              |
| DELETE | `/{id}`                 | 删除渠道          | -                       |

### 推广活动 API (`/api/erp/promotion-activity`)

| 方法   | 路径                    | 说明             | 参数                   |
|--------|-------------------------|------------------|------------------------|
| GET    | `/page`                 | 分页查询          | Query: channelId, status |
| GET    | `/{id}`                 | 详情(含效果数据)   | Path: id               |
| POST   | `/`                     | 新增活动          | Body: activityName, channelId, commissionMode, ... |
| PUT    | `/{id}`                 | 编辑活动          | Body: 字段              |
| PUT    | `/{id}/status`          | 更新状态          | Query: status          |
| DELETE | `/{id}`                 | 删除活动          | -                       |

### 推广效果日志 API (`/api/erp/promotion-log`)

| 方法   | 路径                              | 说明               | 参数                       |
|--------|-----------------------------------|--------------------|----------------------------|
| GET    | `/by-activity/{activityId}`       | 活动效果日志        | Path: activityId           |
| GET    | `/stats/{activityId}`             | 活动效果统计        | Path: activityId           |
| POST   | `/track`                          | 记录推广事件(点击/展示/注册/下单) | Body: eventType, activityId, channelId, visitorId, ipAddress |
| GET    | `/dashboard`                      | 推广数据看板(概览)   | Query: startDate, endDate  |

### 推广佣金结算 API (`/api/erp/promotion-settlement`)

| 方法   | 路径                         | 说明              | 参数                      |
|--------|------------------------------|-------------------|---------------------------|
| GET    | `/page`                      | 分页查询           | Query: channelId, status  |
| GET    | `/{id}`                      | 详情              | Path: id                  |
| POST   | `/generate`                  | 生成结算单(按周期)  | Body: activityId, periodStart, periodEnd |
| PUT    | `/{id}/confirm`              | 确认结算           | -                          |
| PUT    | `/{id}/pay`                  | 标记已付款         | Body: withdrawRequestId(可选) |

### 提成结算配置 API (`/api/erp/commission-settlement-config`)

| 方法   | 路径                    | 说明                           | 参数                 |
|--------|-------------------------|--------------------------------|----------------------|
| GET    | `/list`                 | 获取所有类型配置                | -                    |
| GET    | `/by-type/{ruleType}`   | 按规则类型获取配置              | Path: ruleType       |
| POST   | `/`                     | 新增/保存配置                   | Body: ruleType, settlementType, withdrawablePercent, minWithdrawAmount, withdrawFeePercent, amountTierConfig |
| PUT    | `/{id}`                 | 编辑配置                        | Body: 字段            |

### 用户余额 API (`/api/erp/user-balance`)

| 方法   | 路径                          | 说明                   | 参数                    |
|--------|-------------------------------|------------------------|-------------------------|
| GET    | `/by-partner/{partnerId}`     | 获取用户所有类型余额     | Path: partnerId         |
| GET    | `/balance-summary/{partnerId}`| 余额概览(含明细汇总)     | Path: partnerId         |
| POST   | `/adjust`                     | 管理员手动调整余额       | Body: partnerId, balanceType, amount, remark |

### 余额明细 API (`/api/erp/balance-log`)

| 方法   | 路径                          | 说明               | 参数                        |
|--------|-------------------------------|--------------------|-----------------------------|
| GET    | `/by-partner/{partnerId}`     | 用户余额变动明细     | Path: partnerId, Query: logType, page |
| GET    | `/page`                       | 分页(管理员视角)    | Query: partnerId, logType, startDate, endDate |
| POST   | `/export`                     | 导出明细Excel       | Body: 筛选条件               |

### 提现申请 API (`/api/erp/withdraw-request`)

| 方法   | 路径                         | 说明                 | 参数                          |
|--------|------------------------------|----------------------|-------------------------------|
| GET    | `/page`                      | 分页(管理员审核列表)   | Query: status, partnerId      |
| GET    | `/my`                        | 我的提现申请列表       | Query: partnerId, status      |
| GET    | `/{id}`                      | 详情                  | Path: id                      |
| POST   | `/apply`                     | 提交提现申请(幂等)     | Body: partnerId, amount, accountType, accountNo, accountName, idempotentKey |
| PUT    | `/{id}/approve`              | 审核通过               | Body: auditorId, auditRemark(可选) |
| PUT    | `/{id}/reject`               | 审核驳回               | Body: auditRemark              |
| PUT    | `/{id}/pay`                  | 标记已打款             | Body: paidProof(可选)          |
| PUT    | `/{id}/cancel`               | 用户取消申请           | -                               |
| POST   | `/batch-approve`             | 批量审核               | Body: ids[], action(approve/reject), auditRemark |
| POST   | `/export`                    | 导出提现申请Excel      | Body: 筛选条件                  |

---

## 模块5: 交易记忆价与取价引擎

### 交易记忆价 API (`/api/erp/transaction-price-memory`)

| 方法   | 路径                                         | 说明                  | 参数                             |
|--------|----------------------------------------------|-----------------------|----------------------------------|
| GET    | `/by-partner-product`                        | 获取记忆价             | Query: partnerId, productId, businessType(SALE/PURCHASE) |
| GET    | `/by-partner/{partnerId}`                    | 获取某客户所有记忆价    | Path: partnerId                  |
| POST   | `/update`                                    | 更新/记录记忆价(系统自动)| Body: partnerId, productId, businessType, lastPrice, orderId, orderNo |
| DELETE | `/{id}`                                      | 清除记忆价             | -                                 |

### 取价引擎 API (`/api/erp/price-engine`)

> 此为销售/采购开单时调用的核心取价接口

| 方法   | 路径                    | 说明                       | 参数                                  |
|--------|-------------------------|----------------------------|---------------------------------------|
| POST   | `/calculate`            | 计算商品价格(单商品)        | Body: partnerId, productId, quantity, businessType=SALE |
| POST   | `/calculate-batch`      | 批量计算价格(订单所有行)    | Body: partnerId, items[{productId, quantity}], businessType |
| POST   | `/preview-rules`        | 预览适用于本次交易的规则    | Body: partnerId, productIds[], amount  |
| GET    | `/price-breakdown/{productId}` | 价格构成说明        | Query: partnerId, quantity            |

> **calculate 返回结构:**
> ```json
> {
>   "finalPrice": 80.00,
>   "priceSource": "CUSTOMER_GRADE_MATRIX",
>   "priceSourceLabel": "客户等级定价矩阵",
>   "breakdown": [
>     { "step": 1, "type": "一客一价", "price": null, "matched": false },
>     { "step": 2, "type": "团购价", "price": null, "matched": false },
>     { "step": 3, "type": "营销规则", "ruleName": "全场9折", "price": 90.00, "matched": true },
>     { "step": 4, "type": "等级矩阵", "price": 80.00, "matched": true, "final": true }
>   ],
>   "appliedRules": ["营销规则(全场9折)", "定价矩阵(VIP×A级)"],
>   "originalPrice": 100.00,
>   "discountAmount": 20.00,
>   "isManualModified": false
> }
> ```
>
> **价格来源枚举:**
> - `GROUP_BUY` - 团购价
> - `CUSTOM_PRICE` - 一客一价
> - `MARKETING_RULE` - 营销规则
> - `CUSTOMER_GRADE_MATRIX` - 客户等级定价矩阵
> - `PRODUCT_GRADE_PRICE` - 产品等级价格
> - `TRANSACTION_MEMORY` - 交易记忆价
> - `STANDARD_PRICE` - 标准售价
> - `MANUAL_INPUT` - 手动输入

### 订单结算抵扣 API

| 方法   | 路径                    | 说明                | 参数                                   |
|--------|-------------------------|---------------------|----------------------------------------|
| GET    | `/settlement/balance/{partnerId}` | 获取可用抵扣余额 | Path: partnerId                     |
| POST   | `/settlement/deduct`    | 提交抵扣(预占)       | Body: partnerId, orderId, deductAmount |
| POST   | `/settlement/confirm`   | 确认抵扣(支付成功后)  | Body: orderId                          |
| POST   | `/settlement/refund`    | 退款退回抵扣余额      | Body: orderId, refundAmount            |

---

## 数据字典与辅助 API

| 方法   | 路径                    | 说明                | 参数     |
|--------|-------------------------|---------------------|----------|
| GET    | `/dict/product-grade`   | 产品等级枚举         | -        |
| GET    | `/dict/rule-type`       | 营销规则类型枚举      | -        |
| GET    | `/dict/settlement-type` | 结算方式枚举         | -        |
| GET    | `/dict/account-type`    | 提现账户类型枚举      | -        |
| GET    | `/dict/commission-mode` | 推广佣金模式枚举      | -        |
| GET    | `/dict/party-type`      | 往来单位类型枚举      | -        |
| GET    | `/dict/promotion-channel-type` | 推广渠道类型枚举 | -        |

---

## ER 图(文本关系说明)

```
┌─────────────────────────────┐
│     erp_product_category     │──┐
└─────────────────────────────┘  │ 1:N
                                 │
┌─────────────────────────────┐  │
│        erp_product           │──┘
│  (product_grade_id ──────────│──┐
└─────────────────────────────┘  │
       │ 1:N                     │
       ▼                         │
┌─────────────────────────────┐  │
│  erp_product_grade_price     │  │
└─────────────────────────────┘  │
                                 │
┌─────────────────────────────┐  │
│     erp_product_grade        │◄─┘
└─────────────────────────────┘
       │ 1:N
       ▼
┌─────────────────────────────┐
│     erp_pricing_matrix       │
│  (customer_grade_id ─────────│──┐
└─────────────────────────────┘  │
                                  │
┌─────────────────────────────┐  │
│   biz_customer_grade         │◄─┘
└─────────────────────────────┘

┌─────────────────────────────┐
│    erp_marketing_rule        │──┐ 1:N
└─────────────────────────────┘  │
                                 ▼
                        ┌───────────────────────┐
                        │ erp_marketing_rule_detail│
                        └───────────────────────┘

┌─────────────────────────────┐
│  erp_group_buy_activity      │──┐ 1:N
└─────────────────────────────┘  │
                                 ▼
                        ┌───────────────────────┐
                        │erp_group_buy_participant│
                        └───────────────────────┘
                               │
                               │ N:1
                               ▼
                        ┌───────────────────────┐
                        │      biz_party         │◄──── 往来单位(核心)
                        └───────────────────────┘
                               ▲
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
┌────────────────┐  ┌──────────────────┐  ┌──────────────────────┐
│biz_party_address│  │ biz_party_contact │  │  erp_user_balance     │
└────────────────┘  └──────────────────┘  │  erp_balance_log       │
                                          │  erp_withdraw_request  │
                                          └──────────────────────┘

┌─────────────────────┐         ┌─────────────────────────────┐
│  erp_share_activity  │──┐ 1:N │  erp_promotion_channel       │──┐
└─────────────────────┘  │     └─────────────────────────────┘  │
                         ▼                                      ▼
                ┌────────────────────┐                ┌───────────────────────┐
                │ erp_share_relation  │                │ erp_promotion_activity │
                └────────────────────┘                └───────────────────────┘
                         │ 1:N                                 │ 1:N
                         ▼                                     ▼
                ┌────────────────────────┐         ┌───────────────────────┐
                │erp_share_commission_det│         │  erp_promotion_log     │
                └────────────────────────┘         │erp_promotion_settlement│
                                                   └───────────────────────┘
```
