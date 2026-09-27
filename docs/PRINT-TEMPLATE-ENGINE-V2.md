# 打印模板引擎 v2 与「单据打印」系统级契约

> 落盘时间 2026-09-26。本文只写**已实现**的东西，与代码同步；改代码务必回来改本文。
> 相关代码：`backend/erp/erp-printing`（引擎 / 服务 / 控制器）、各业务模块的 `PrintDataProvider`。

## 一、为什么要做

打印原先有三处断点（2026-09-26 实测，均已修）：

| 断点 | 现象 |
|---|---|
| 状态口径不一致 | 前端查模板发 `status: 1`（数字），库里是 varchar 存 `'PUBLISHED'` → 查询恒不命中，**打印弹窗的模板下拉永远是空的** |
| 权限码写错前缀 | 前端用 `printing:*`（库里 0 条），后端是 `print:*` → 按钮**对所有人隐藏**（管理员也看不见） |
| 取数/渲染逻辑重复 | 4 处页面各自重抄「挑模板 → 取数 → 渲染」，连带复制了上面的缺陷 |

根因是：**前后端各修各的，中间没有契约也没有检查**。本文定义的契约 + `tools/check-print-wiring.py` 就是来补这个的。

## 二、两条渲染路径

引擎入口 `FormatEngineImpl.renderToHtml(templateJson, dataJson)` 按模板形态分流：

| | v1 绝对坐标 | v2 分区流式 |
|---|---|---|
| 模板键 | `components: [...]` | `sections: {...}` |
| 坐标 | 模板给死 `x/y/w/h`（mm） | 引擎按 12 栅格 + 分页算 |
| 适用 | 套打、标签、固定版面 | 单据（订单/出库/退货…） |
| 合计 / 分页 / 大写 | 不支持 | 支持 |

渲染一律用 `table` + 内联样式，**不用 Flexbox/Grid** —— 最终经浏览器打印或 wkhtmltopdf 这类老 WebKit，`<thead>` 每页重印是原生行为，最稳。

### v2 模板结构

```jsonc
{
  "version": 2, "freeLayout": false,
  "paperSize": "A4", "pageWidth": 210, "pageHeight": 297,   // mm
  "margin": { "top": 10, "right": 10, "bottom": 12, "left": 10 },
  "sections": {
    "pageHeader": { "repeat": true, "height": 16,
      "items": [ { "type": "label", "content": "销售订单",
                   "style": { "textAlign": "center", "fontSize": "18px", "fontWeight": "bold" } } ] },

    "docHeader": { "columns": 12,                     // 字段按栅格自动排布，不用算坐标
      "items": [ { "type": "field", "label": "单号", "field": "orderNo", "span": 4,
                   "formatConfig": { "type": "date", "pattern": "yyyy-MM-dd" } } ] },

    "items": { "field": "items",                      // 明细数组的键名
      "repeatHeader": true, "rowsPerPage": 0,         // 0=按纸高自动分页；>0=固定行数（套打）
      "rowHeight": 7, "fillBlankRows": false,         // 套打时补空行
      "emptyWhenNoData": true, "totalLabel": "合计",
      "columns": [ { "header": "数量", "field": "quantity", "width": "70px",
                     "align": "right", "agg": "sum", "digits": 2 } ] },

    "summary": { "items": [ { "type": "field", "label": "金额大写", "field": "billAmount",
                              "span": 6, "formatConfig": { "type": "rmbUpper" } } ] },

    "pageFooter": { "repeat": true, "position": "bottom",
      "items": [ { "type": "label", "content": "制单：____ 审核：____ 签收：____" },
                 { "type": "pageNumber", "format": "第{0}页 / 共{1}页" } ] }
  }
}
```

支持的 `formatConfig.type`：`date`（pattern，`LocalDate`/`LocalDateTime` 都认）、
`number`（decimals/thousands）、`enum`（mapping）、`expression`（SpEL，用 `#value` 引用字段值）、
`rmbUpper`（人民币大写）。

### 列由数据给：`items.columnsFrom`（结果集打印用）

单据类模板的列写死在 `items.columns`；**结果集打印**（列表页打"我当前看到的这批数据"）
的列是运行时才定的（含用户当前列配置），模板里写不死。这时改用：

```jsonc
"items": { "field": "rows", "columnsFrom": "columns", ... }
```

引擎会把数据里 `columns` 数组的每一项转成列，两种写法都认：

```jsonc
{ "key": "productName", "title": "商品名称" }
{ "field": "productName", "header": "商品名称", "width": 110, "align": "right", "digits": 2 }
```

- 数字宽度会**自动补 px**（前端列配置里 `width: 110` 直接当 CSS 用是无效的）；
- 数据里没给列（或给了空数组）时**回落到模板写死的 `columns`**，不让整张表凭空消失；
- 于是**一份通用模板**（见 `tool-results/print-template-v2/_result-set.json`）就能服务于
  所有 `{ title, columns, rows }` 形状的列表页 —— 列变了不用改模板。

配套约定（页面侧）：结果集页的 `print-data` 统一成

```jsonc
{ "title": "商品列表", "columns": [{ "key": "…", "title": "…" }], "rows": [ … ], "printTime": "…" }
```

`columns` 的 `key` 必须与 `rows` 里的字段名一一对应，否则那一列空白。

每页一个 `.print-page` 块（纸高固定 + `overflow:hidden`），页间 `page-break-after`；
每页自带一份 `<thead>`；合计在**末页**的 `<tfoot>`；`docHeader` 只在首页。

## 三、系统级契约：页面只给 pageCode + 单据主键

```
GET  /api/v2/print/documents/{pageCode}/templates        已发布模板（「已发布」由服务端定）
GET  /api/v2/print/documents/page-codes                  已注册装配器的页面编码
POST /api/v2/print/documents/{pageCode}/{documentId}/render[?templateId=]
                                                         → { html, templateId, templateName,
                                                             paperSize, defaultTemplateId, documentNo }
```

数据由后端装配，页面**不必懂模板、不必准备数据**：

```java
public interface PrintDataProvider {          // cn.aiedge.erp.printing.spi
    String pageCode();                        // 与 page_code、前端 page-code 三处一致
    Map<String, Object> load(Long documentId);// 装配标准数据包；单据不存在返回 null
    default String documentNo(Long id) { return null; }
}
```

Spring 按 `pageCode` 收进 `PrintDataProviderRegistry`；**同一 pageCode 注册两次直接启动失败**
（宁可起不来，也不要静默用错装配器）。

> ⚠️ **多人/多会话并行开发时的硬约束**：因为上面这条是启动期 fail-fast，
> 两个人同时给同一个页面加装配器 ⇒ **整个应用起不来**（报错里会点出两个类名）。
> 动某个 pageCode 之前，先跑 `python tools/check-print-wiring.py --codes` 看它有没有人注册；
> 那个命令会把重复注册直接报成失败项。同理，库里同一个 `(tenant, page_code, template_name)`
> 也被唯一约束挡着 —— 模板名要写清楚是哪张单，别都叫「标准模板」。

**数据包契约**：单据头平铺（键名 = 实体属性名，便于对回列）、明细固定键 `items`、
跨行汇总放顶层。各单据的明细金额字段名**跟着自己的实体走**（销售订单是 `amount`，
退货单是 `lineAmount`）——装配器负责翻译，模板照着对应单据的实体写。

**打印设置**（`set_print_config` 的小数位数 / 打印内容）在**服务端唯一实现**
（`PrintBehaviorApplier`，含「金额类字段永不格式化」的边界）：数据是服务端装配的，
前端那份 `printBehavior.ts` 只服务于尚未迁移的兼容路径。

### 先判断：这页该不该有装配器

**不是所有打印页都该走后端装配。** 判据只看一件事：**打的是"某一张单据"，还是"我正在看的这批数据"？**

| | 单据打印 | 结果集打印 |
|---|---|---|
| 例 | 销售订单 / 出库单 / 采购订单 / 配送任务单 | 商品列表、货位设置、价格管理、实名认证、各类查询报表 |
| 数据的来源 | 库里一张单据 + 它的明细 | 当前筛选结果 + **用户当前列配置** |
| page-code 对应 | 有 `documentId` | 没有 |
| 该有装配器吗 | **该** | **不该** —— 硬搬后端装配会丢掉列配置与筛选语义 |

已知属结果集打印（保持兼容路径，不要再试图补装配器）：
`md:product` / `md:barcode` / `md:location` / `md:product-price` / `dms:verification` /
`erp-mall-product` / `erp-sales-report`。
它们的 `print-data` 刻意围绕"我正在看的这批数据"构造，统一形状是 `{title, columns, rows, printTime}`
（`ship-query` / `dispatch-query` 不在此列 —— 它们打的是销售出库单 / 配送任务单本体，
有委托实现的后端装配器，见下面「同一个单据被多个入口打印」）。

### page-code 是路径段，不能含 `/`

三个业务级端点都把 page-code 放在**路径**里（`/documents/{pageCode}/templates`），
所以 page-code 必须能安全当路径段用：

- 前端会对它做 `encodeURIComponent`，`/` 变成 `%2F`；
- Tomcat 的 `ALLOW_ENCODED_SLASH` 默认关，见到 `%2F` 直接回 **400**（不是 404，也不是「模板不存在」）；
- 于是「取模板 → 渲染」在第一个请求就断，而静态看代码一切正常。

实测 `erp/sales-report`、`erp/mall/product` 曾恒 400（2026-09-27 已改成 `erp-sales-report` /
`erp-mall-product`）。`tools/check-print-wiring.py` 有这条守卫，新页面别再起带 `/` 的编码。

### 接入一个新页面（4 步）

1. 业务模块 pom 加 `erp-printing` 依赖（依赖方向不成环：erp-printing 只依赖 core-base）
2. 写一个 `@Component implements PrintDataProvider`，`pageCode()` 与前端一致；
   实体上只有 ID 的字段（供应商/仓库/经手人）要在装配器里补名称，**查不到就留空，不要打印 ID**
3. 建 v2 模板落库：`python tools/ql361-print-template-seed.py <模板.json> <pageCode> --name "…" --apply`
4. 跑 `python tools/check-print-wiring.py` 确认三方对齐，
   再跑 `python tools/verify-print-document.cjs <pageCode> <documentId>` 做端到端验证

同一个单据被多个入口打印时（如采购收货入口打的就是采购订单），**新入口的装配器应委托已有实现**，
不要复制一份装配逻辑（见 `PurchaseReceivePrintDataProvider`）。

前端 `<PrintDialog page-code="…" :document-id="formData.id" :print-data="…" />` ——
给了 `document-id` 且该页有装配器就走后端装配，否则回落「页面自己给数据」的兼容路径
（新建/复制单据 id 为空时正需要这条回落）。

## 四、已知边界

- **一个模板只支持一个明细表**（`sections.items` 单例）。换货单的「换出/换入各一张表」暂时
  表达不了，改成一张表加「类型」列（`SaleExchangePrintDataProvider` 已派生 `warehouseTypeName`）。
- **金额大写**是外币无关的中文大写；`≥ 1e15 元` 直接退回数字串（不硬算）。
- **自动分页是按纸高估算**（`rowHeight` 默认 7mm），行内换行多会溢出；要求精确就用
  `rowsPerPage` 固定行数。
- v1 的 `components` 与 v2 的 `sections` **不要同时存在**于一个模板。
- `PrintDialog` 的草稿拦截（`allowDraftPrint`）在后端装配模式下**只能靠调用方显式传
  `:is-draft`**，否则这一关不生效。

## 五、命令行工具

| 工具 | 用途 |
|---|---|
| `tools/check-print-wiring.py` | 接入检查：pageCode 三方对齐 / 权限码 / 接口路径。三类漂移一次抓出 |
| `tools/verify-print-document.cjs <pageCode> <documentId> [templateId]` | 端到端验证：登录 → 取模板 → 渲染 → 断言内容 |
| `tools/e2e-sale-pre-order-print.cjs` | 预订货单**造单 + 打印**端到端。dev 库里该表本来是空的，没有 documentId 就验不了这条链 —— 页面数据为空的单据照这个写一个 |
| `tools/ql361-print-template-seed.py <json> <pageCode> --name … [--apply] [--template-id N]` | 模板落库；`--template-id` 精确改写某行（按名字匹配会被空格拆词坑到） |
| `tools/ql361-print-template-convert.py` | ql361 模板 → v1 格式（**仅供研究对标**，转换产物缺单号/合计/页码，不能当生产模板） |

## 六、踩过的坑（别再踩）

- **「假打印」长什么样**：销售出库单原来的 `handlePrint()` 只调了
  `outboundApi.print(id)` —— 那个端点**只回写打印次数**，不渲染也不打印，却提示「打印成功」。
  判断一处打印是不是假打印，就看它有没有真的产出 HTML 并交给浏览器/打印链；
  只调了个「打印次数 +1」之类的接口就报成功，就是假的。
  正确姿势：`PrintDialog` 负责渲染与打印，次数回写放 `@print-success` 回调里。
- **动态绑定的 page-code 必查**：`sales/doc-query` 原来用
  `SALE_ORDER: 'sale-order'` 映射，而销售订单页用的是 `'sale'` —— 那个编码全站只存在于这一行，
  既没模板也没装配器，点打印必然报「还没有已发布的打印模板」。
  凡 `:page-code="变量"` 的写法，`tools/check-print-wiring.py` 判不了（会提示人工确认），
  变量映射表里的每个值都要跟对应页面的字面量逐个核对。
- **写验证脚本时雪花 ID 只能按字符串传**：本库单据/商品 ID 是 `990000000000000001` 这个量级，
  超过 JS 安全整数 `2^53` —— 写成字面量数字会被静默取整成 `...000`，后端收到的是**另一个 ID**
  （表现为 `商品不存在：…000`）。读回来没事：后端把 Long 序列化成了字符串。
- `v-permission` 校验不到的码会**移除元素**（不是置灰）→ 权限码写错就是按钮消失，且不报错。
- `ExpressionEvaluator` 走 SpEL，字段值必须写 `#value`（裸写 `value` 会被当根对象属性 → 求值失败）。
- `PrintDialog` 的预览必须用 `srcdoc` iframe：用 `v-html` 会把模板的 `<style>` 提到当前页面里生效，
  污染整个后台界面。
- 打印文档不要在 `<body>` 里再套一层 `<html>` —— 内层 `<head>/<style>` 会被丢弃，模板的字体与
  纸张设置随之失效（所见与所打不一致）。

## 七、结果集页的统一接法（`useListPrint`）

列表 / 查询 / 报表页**不要各写一遍**打印逻辑 —— 用 `@/composables/useListPrint`：

```ts
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'purchase-doc-query',   // 与库里的模板一致，且不含 `/`
  title: '采购单据查询',
  columns: () => columns,           // 页面自己的列定义，原样给
  rows: () => tableData.value,
  selectedRows: () => selectedRows.value,   // 可选：勾选了就打勾选的
})
```
```vue
<PrintDialog ref="printDialogRef" page-code="purchase-doc-query" :print-data="printData" />
```

它替页面做三件事：筛列（行号 / 勾选 / 操作 / 图片 / `defaultHidden` 不打）、
按列自带的 `formatter(value, record)` 格式化单元格（与表格同格式）、组装 `{title, columns, rows}`。
列由数据给（`items.columnsFrom`），所以页面的列变了不用去改模板。

**宽表要横向**：`table-layout:fixed` + 表头 `nowrap`，A4 纵向放不下多少列。
约定 **可打印列 > 14 就用横向模板**（`tool-results/print-template-v2/_result-set-landscape.json`，
`pageWidth 297 / pageHeight 210`；模板名写「结果集模板（横向·v2）」以便一眼分辨）。
实测 `purchase-doc-query`（37 列）、`purchase-sales-driven`（42 列）用横向，
其余结果集页（≤14 列）用纵向的 `_result-set.json`。
