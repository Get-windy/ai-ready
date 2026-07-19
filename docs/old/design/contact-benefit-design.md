# 联系人受益（回扣/提点）功能设计文档

> 最后更新：2026-07-04
> 状态：后端已实现，前端页面待设计

---

## 一、业务背景

在 B2B 销售场景中，某单位的联系人（采购员、决策人等）可能需要获得个人收益，通常有两种模式：

### 模式1：折扣+提点

- 公司在**基准价格**基础上给客户打折
- 联系人从交易金额中获得一定比例的"提点"（回扣）
- 提点由公司利润中支出

```
基准价: ¥100
给客户折扣: 10%  → 客户付 ¥90
个人提点: 3%     → 个人得 ¥100 × 3% = ¥3
公司实收: ¥90 - ¥3 = ¥87
```

### 模式2：基准价+加点

- 公司给出**基准价格**不变
- 在基准价格基础上**加价**给联系人，客户实际支付加价后的价格
- 加价部分归联系人所有

```
基准价: ¥100
公司定价: ¥100（不变）
加价给个人: 5%  → 客户付 ¥105
个人得: ¥100 × 5% = ¥5
公司实收: ¥100（不变）
```

---

## 二、数据库设计

### 迁移脚本：`V9.30.0__Grade_Price_Contact_Benefit.sql`

在 `erp_product_grade_price` 表中新增 4 个字段：

```sql
ALTER TABLE erp_product_grade_price
    ADD COLUMN IF NOT EXISTS contact_partner_id BIGINT,
    ADD COLUMN IF NOT EXISTS benefit_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS benefit_rate NUMERIC(10, 4),
    ADD COLUMN IF NOT EXISTS benefit_fixed NUMERIC(12, 2);
```

### 字段说明

| 字段 | 类型 | 含义 | 示例 |
|------|------|------|------|
| `contact_partner_id` | BIGINT | 受益联系人ID，关联往来单位联系人表，`NULL`表示无受益 | `123456789` |
| `benefit_type` | VARCHAR(20) | 受益模式：`DISCOUNT`=折扣+提点，`MARKUP`=基准价+加点，`NULL`=无受益 | `DISCOUNT` |
| `benefit_rate` | NUMERIC(10,4) | 提点/加点比例（%），如 `3.0000` 表示 3% | `3.0000` |
| `benefit_fixed` | NUMERIC(12,2) | 固定金额，不为 NULL 时覆盖 `benefit_rate` 计算 | `50.00` |

### 索引

```sql
CREATE INDEX IF NOT EXISTS idx_grade_price_contact ON erp_product_grade_price(contact_partner_id);
```

### 设计说明

- 受益字段跟随等级价格记录（`erp_product_grade_price`），支持**按单位×按等级**的细粒度配置
- `benefit_fixed` 优先级高于 `benefit_rate`：当 `benefit_fixed` 不为 NULL 时，个人所得直接使用固定金额
- 一个产品的不同单位、不同等级可以配置不同的受益方案

---

## 三、后端实现

### 3.1 实体类：`ProductGradePrice.java`

位置：`erp/erp-stock/src/main/java/cn/aiedge/erp/stock/entity/ProductGradePrice.java`

```java
// ── 联系人受益（回扣/提点）──
/** 受益联系人ID（关联往来单位联系人），NULL=无回扣 */
private Long contactPartnerId;
/** 受益模式: DISCOUNT=折扣+提点, MARKUP=基准价+加点, NULL=无 */
private String benefitType;
/** 提点/加点比例(%)，如3.00表示3% */
private BigDecimal benefitRate;
/** 固定金额，不为NULL时覆盖benefitRate计算 */
private BigDecimal benefitFixed;
```

### 3.2 批量创建：`ProductController.batchCreate()`

在单位数据中从 payload 提取受益字段，写入等级价格记录：

```java
// 提取联系人受益字段（回扣/提点），从单位数据中剥离
Long contactPartnerId = null;
String benefitType = null;
BigDecimal benefitRate = null;
BigDecimal benefitFixed = null;
{
    Object v = u.remove("contactPartnerId");
    if (v != null) try { contactPartnerId = Long.parseLong(v.toString()); } catch (Exception ignored) {}
    Object bt = u.remove("benefitType");
    if (bt != null) benefitType = bt.toString();
    Object br = u.remove("benefitRate");
    if (br != null) try { benefitRate = new BigDecimal(br.toString()); } catch (Exception ignored) {}
    Object bf = u.remove("benefitFixed");
    if (bf != null) try { benefitFixed = new BigDecimal(bf.toString()); } catch (Exception ignored) {}
}

// 创建 ProductGradePrice 时设置受益字段
gp.setContactPartnerId(contactPartnerId);
gp.setBenefitType(benefitType);
gp.setBenefitRate(benefitRate);
gp.setBenefitFixed(benefitFixed);
```

### 3.3 批量更新：`ProductController.batchUpdate()`

与 batch-create 相同的逻辑，先删除旧的等级价格（含受益字段），再重新写入。

### 3.4 加载表单：`getFormById()`

`ProductFormDTO` 中已有 `gradePrices` 字段，新增的受益字段通过 `IProductGradePriceService.getByProductId()` 自动返回，无需额外改动。

### 3.5 计算公式

计算公式未在后端硬编码，由前端/UI 展示时按需计算：

```
DISCOUNT 模式（折扣+提点）:
  客户单价 = gradePrice × (1 - discount/100)        [已有折扣逻辑]
  个人所得 = benefitFixed ?? (gradePrice × benefitRate / 100)
  公司实收 = 客户单价 - 个人所得

MARKUP 模式（基准价+加点）:
  客户单价 = gradePrice × (1 + benefitRate / 100)
  个人所得 = benefitFixed ?? (gradePrice × benefitRate / 100)
  公司实收 = gradePrice（不变）

无受益（benefitType = NULL）:
  客户单价 = gradePrice × (1 - discount/100)
  个人所得 = 0
  公司实收 = 客户单价
```

---

## 四、前端现状

### 4.1 类型定义：`product.ts`

`ProductGradePrice` 接口已新增受益字段：

```typescript
export interface ProductGradePrice {
  id?: number
  productId?: number
  unitId?: number
  gradeCode: string
  gradeName?: string
  gradePrice?: number
  basePrice?: number
  isActive?: number
  // ── 联系人受益（回扣/提点）──
  /** 受益联系人ID，undefined=无回扣 */
  contactPartnerId?: number
  /** 受益模式: DISCOUNT / MARKUP / undefined */
  benefitType?: 'DISCOUNT' | 'MARKUP' | string
  /** 提点/加点比例(%) */
  benefitRate?: number
  /** 固定金额，不为undefined时覆盖benefitRate */
  benefitFixed?: number
}
```

### 4.2 数据加载：`form.vue`

编辑商品时，从 `gradePrices` 列表按 `unitId` 分组，提取 `gradePrice1~8` 的同时也提取受益字段，合并到单位行数据：

```typescript
if (gp.contactPartnerId || gp.benefitType) {
  const row = gpMap[gp.unitId]
  if (!row.contactPartnerId) {
    row.contactPartnerId = gp.contactPartnerId
    row.benefitType = gp.benefitType
    row.benefitRate = gp.benefitRate
    row.benefitFixed = gp.benefitFixed
  }
}
```

### 4.3 保存 Payload：`form.vue`

保存时，`unitsPayload` 中每个单位带上受益字段：

```typescript
const unitsPayload = unitList.value.map((u: any) => ({
  // ... 单位基础字段
  gradePrice1: u.gradePrice1,
  // ... gradePrice2~8
  contactPartnerId: u.contactPartnerId,
  benefitType: u.benefitType,
  benefitRate: u.benefitRate,
  benefitFixed: u.benefitFixed,
  _key: u._key
}))
```

### 4.4 UI 状态

当前**无前端页面**提供受益字段的输入界面。数据流已打通，等待设计专门的管理页面。

---

## 五、API 请求/响应格式

### 创建/更新商品 Payload（units 数组中每个单位）

```json
{
  "unitName": "箱",
  "unitType": "LARGE",
  "isBaseUnit": 0,
  "conversionRate": 10,
  "gradePrice1": 95.00,
  "gradePrice2": 90.00,
  "gradePrice3": 85.00,
  "contactPartnerId": 123456789,
  "benefitType": "DISCOUNT",
  "benefitRate": 3.00,
  "benefitFixed": null,
  "_key": "_u1"
}
```

### 加载商品表单响应（gradePrices 数组）

```json
{
  "product": { "..." : "..." },
  "units": [ "..." ],
  "recommends": [ "..." ],
  "gradePrices": [
    {
      "id": 1001,
      "productId": 100,
      "unitId": 2001,
      "gradeCode": "GRADE_1",
      "gradePrice": 95.00,
      "contactPartnerId": 123456789,
      "benefitType": "DISCOUNT",
      "benefitRate": 3.00,
      "benefitFixed": null,
      "isActive": 1
    }
  ]
}
```

---

## 六、待实现事项

### 后端
- [ ] 联系人下拉列表 API（从往来单位联系人表查询）
- [ ] 受益金额计算 API（可选，根据 gradePrice + benefit 字段计算实际金额）
- [ ] 受益汇总表/报表（按联系人统计累计提点金额）

### 前端
- [ ] 专门的管理页面设计（待讨论位置：可能在商品辅助资料或独立的"价格政策"模块中）
- [ ] 提点模式选择器（`DISCOUNT` / `MARKUP` 下拉）
- [ ] 提点比例输入框（%）
- [ ] 联系人搜索选择器（SearchSelect，搜索往来单位联系人）
- [ ] 实时预览计算结果（客户付多少、个人得多少、公司收多少）

---

## 七、关键文件路径

| 文件 | 路径 |
|------|------|
| 迁移脚本 | `backend/core/api/core-api/src/main/resources/db/migration/V9.30.0__Grade_Price_Contact_Benefit.sql` |
| 等级价格实体 | `backend/erp/erp-stock/src/main/java/cn/aiedge/erp/stock/entity/ProductGradePrice.java` |
| 产品Controller | `backend/erp/erp-stock/src/main/java/cn/aiedge/erp/stock/controller/ProductController.java` |
| 产品表单DTO | `backend/erp/erp-stock/src/main/java/cn/aiedge/erp/stock/dto/ProductFormDTO.java` |
| 前端 API 类型 | `frontend/apps/pc-admin/src/api/erp/product.ts` |
| 前端商品表单 | `frontend/apps/pc-admin/src/views/erp/product/form.vue` |
