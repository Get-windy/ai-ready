# 配送模块 · 对标开发文档集

> **对标系统**：来肯企汇 ql361 v2.2（`22stable.ql361.com`，桌面式多标签，页面无独立路由）
> **数据来源**：Playwright 实测抓取（2026-09-12，账号 15095664266）——列配置弹窗（数据表内 `.icon-shezhi2`）、页面配置弹窗（顶部按钮栏 `iconButtonConfig` 齿轮）、页面结构（查询区/工具栏/表头）、截图。
> **通用规范**：见《ql361对标/对标开发技术参考文档》第 4、5 章（价格等级 / 客户来源 / 红线 / 验收标准），本文不重复。
> **抓取资产生成**：截图 `tool-results/ql361/pages/配发收__*.png`、`资料__配送管理__线路*.png`；字段清单 `抓取结果/<页名>_抓取.json`。
> **术语**：业务上「骑手」统一称**配送员**——配送员可以是**企业员工**（自有），也可以是**外部平台**的骑手/配送员（众包、第三方运力）。

---

## 1. 模块定位与边界

| 分块 | 对标来源 | 本系统菜单 | 内容 |
|------|---------|-----------|------|
| **配发收（配送执行）** | ql361 `配发收` 域（实测 5 页） | 侧边栏「配发收」 | 配送查询、物流发货、发货查询、物流退货收货、采购订货收货 |
| **配送 DMS（配送能力体系）** | ⚠️ ql361 无对应菜单（本系统新增） | 侧边栏「配送」 | 配送路线、配送业务、人车管理、物流配送、收货业务、调度管理、配送跟踪、配送配置、API监控（**9 分组 22 项**） |
| **配送主数据** | ql361 `资料 → 配送管理 → 线路`（实测） | 挂在「资料」菜单，业务归配送 | 线路（配送线路档案） |

> **边界铁律**：ql361 的「配发收 → 配送业务」下**只有「配送查询」，没有「配送单」**（2026-09-12 实测确认）。
> 本系统《配送单》属**新增单据**，无对标页，按本系统自主建模，**不要臆造 ql361 字段**。

**实测确认的 ql361 菜单结构**（配发收，鼠标悬停展开 `popupmenu`）：

```
配发收
├─ 配送业务 └─ 配送查询            menuId 3557,  billType 52445
├─ 发货业务 ├─ 物流发货            menuId 47001, billType 2341   → 打开「订单处理中心」并选中 2.拣货/发货
│           └─ 发货查询            menuId 3752,  billType 2357   → 销售出库单(XSCKD)台账
└─ 收货业务 ├─ 物流退货收货        menuId 300401,billType 2734   → 打开「销售退货申请-历史」
            └─ 采购订货收货        menuId 40001, billType 230101 → 打开「收货处理」
```
`资料` 域实测菜单：`商品管理 / 往来单位 / 仓库管理 / 配送管理(线路) / 职员权限 / 财务账户`。

### 1.1 本系统配送模块实际菜单（sys_menu 实测 · 10 分组 27 项）

> ⚠️ **2026-09-23 复审订正**：标题此前记为「25 项」，与本表实际行数（**27 行**）不符，已订正。
> 完整口径：配送子树 `60005` = 1 顶级 + 10 分组 + **27 叶子**，另加挂 `资料 → 配送管理` 的 `70530 线路`（业务属配送），合计 **28 个页面**。连通性复核 **0 缺陷**（`tools/audit-dms-menu.py`）。

> 2026-09-12 查询 `devdb.sys_menu`（顶级 `60005 配送`，menu_code `mega:delivery`）。**这是本模块文档的唯一页面口径**（此前按 mega-menu 设计稿写的"6 列 17 项"已作废）。菜单通过数据库动态管理，变更走 Flyway 迁移。

| 分组 | 菜单项 | 菜单ID | path | 组件 | 后端接口 | 现状 |
|------|--------|:-----:|------|------|---------|------|
| 配送业务 | 配送查询 | 70150 | `dispatch/query` | `views/dispatch/query/index.vue` | `/api/dms/task/page` + `/filter-options` | ✅ **金标准**（24 列/默认 20、13 条件 + 9 段快捷时间、操作列 4 项、合计行、经典分页） |
| | 配送单 `[历史]` | 80760 | `dispatch/dispatch-order/form`（`list_path=…/index`） | `views/dispatch/dispatch-order/{form,index}` | `taskApi`（`/api/dms/task/*`：next-no/page/page-detail/save/detail/audit/unaudit/print/export + 复用 `/filter-options`） | ✅ **金标准**（2026-09-12，E2E 194/194）：24 列(20 默认+4 隐藏)、16 条件 + 9 段快捷时间、页面配置 + 序号齿轮列配置、合计行、经典分页；表单 `BillFormPage` + 商品明细 10 列 + 配置 3 Tab；号段 `PSD-YYYYMMDD-序号`；迁移 `V11.199.0` |
| | 配送仪表盘 | 80820 | `dms/dashboard` | `views/dms/dashboard/index.vue` | ✅ `/api/dms/dashboard/*`（7 接口：stats/trend/task-summary/top-riders/distribution/active-bindings/pending-alerts） | ✅ **金标准**（2026-09-12/13，E2E **149/149** 独立复验）：14 KPI 卡（可下钻）+ 3 图表 + 待办两表 + 绩效 Top；口径固化 + 时间范围/渠道/订单类型筛选 + 自动刷新（间隔读配送参数）+ 真实 xlsx；复验修 **order_type 为 NULL 致分布接口 500** 与 **`AlarmOutlined` 图标不存在致调度任务页白屏（连带两处下钻失效）** |
| 配送路线 | 配送路线单 | 80700 | `dms/route-list` | `views/dms/route-list/index.vue` | `/api/delivery/route/*`（规划委托 `dms/route`） | ✅ **金标准 + 能力升级**（2026-09-13，E2E 217/217）：27 列(17 默认+7 隐藏)、8 查询项、8 功能按钮、多点签收、**围栏归集/手动补单/地图规划/催单提前/ETA**、真实 xlsx |
| | 路线规划 | 80830 | `dms/route` | `views/dms/route/index.vue` | ✅ `/api/dms/route/*` + `/api/dms/route/fence/*` | ✅ **金标准**（2026-09-13 P2 收口，E2E 186/186）：5 能力 + 矢量地图（拖拽标点/可选高德 JS 底图，无 Key 自动回退）+ 围栏档案 + VRP（容量/时间窗/多车型/多起点/硬窗口）+ 连通性自检 + 敏感配置脱敏 + 配置审计与回滚 + Redis 跨实例热生效 |
| 人车管理 | 车辆管理 | 80770 | `dms/vehicle`（`list_path=dms/vehicle/form`，`tag=添加`） | `views/dms/vehicle/{index,form}.vue` | `/api/dms/vehicle/*` | ✅ **金标准完成 2026-09-12**（E2E 121/121）：22 列(17 默认+6 隐藏)、8 查询项、7 功能按钮、页面配置 + 序号齿轮列配置、状态机、人车一对一绑定/解绑+流水、三证到期提醒、真实 xlsx；6 处接口不匹配全修；迁移 `V11.175.0`；见《车辆管理开发文档》§3.6 |
| | 车辆维护 | 80780 | `dms/vehicle/maintenance` | `views/dms/vehicle/maintenance.vue` | `/api/dms/vehicle/maintenance/*` | ✅ 金标准完成（2026-09-12，E2E 160/160）：列表+弹窗+到期提醒+四口径费用统计；**厂商=往来单位档案引用+名称快照**（V11.210.0）；见《车辆维护开发文档》§6/§7 |
| | 配送员管理 | 80790 | `dms/rider`（`list_path=dms/rider/form`，`tag=添加`） | `views/dms/rider/index.vue` | `/api/dms/rider/*` | ✅ **金标准完成 2026-09-12**（E2E 118/118）；双入口；类型枚举/假删除/接口 5 处不匹配已修 |
| | 用车管理 | 80840 | `dms/vehicle/usage` | `views/dms/vehicle/usage/index.vue` | `/api/dms/verification/*` + `/api/dms/vehicle/energy/*` | ✅ **金标准完成**：6 Tab（用车登记／车辆巡检／车辆补能／**补能卡**／**能耗报表**／核验预警），E2E 261/261（+司机端 18/18）|
| | 人员核验 | 80847 | `dms/verification` | `views/dms/verification/index.vue` | `/api/dms/verification/*` | ✅ **金标准完成**：3 Tab（实名认证／证照核验／**准入核验**）；人的合规，不含车辆检查 |
| 物流配送 | 物流发货 | 70155 | `dispatch/logistics-ship` | `views/dispatch/logistics-ship/index.vue` | 复用《订单处理中心》视图 + `/center/picking-shipping(-summary)`、`/{id}/pick-complete`、`/batch-pick-complete`、`/batch-logistics-remark`、`/api/erp/sale/logistics/*`（包裹/取号/运费/ASN） | ✅ **金标准完成**（2026-09-13，E2E **90/90**）：薄壳直达「2.拣货/发货」，37 列（默认 17 + 20 隐藏）、查询 16 项、按钮 6 个、行级 取消/拣完/发货/更多、服务端合计行（销售金额｜商品数量）；拣完=明细 `picked_quantity` 回写，发货后流转 status 4 并移出列表；**「物流备注」= ql361 `OrderRemarks` 15 字段弹窗 + 行内「更多 → 物流/备注」**；见《物流发货开发文档》§3.1/§5.6。**2026-09-14 生产级升级（P0/P1/P2，E2E 46/46）**：物流信息接 1:N 子表 + 承运商档案引用 + 一单多包 + 运费规则与对账 + 电子面单取号 + ASN 外发，见该文档 §6 |
| | 物流运费对账 | 70162 | `dispatch/freight-reconcile` | `views/dispatch/freight-reconcile/index.vue` | `/api/erp/sale/logistics/*`（freight/rules·calc·reconcile·bill·reconcile-mark、shipment-notify） | 🆕 **本系统新增**（2026-09-14，迁移 `V11.341.0`）：三 Tab = 运费规则（承运商×区域×重量区间 首重/续重）/ 运费对账（我方计费 vs 承运商账单 + 差异清单 + 标记已对账）/ 发货通知 ASN（台账 + 重试）；见《物流发货开发文档》§6.6 |
| | 发货查询 | 70156 | `dispatch/ship-query` | `views/dispatch/ship-query/index.vue` | `outboundApi.page` + `/api/dms/task/outbound-filter`（固定项） | ✅ **金标准**（2026-09-13 二次复核，E2E 126/126）：49 列(默认 14)+25 查询项+2 固定项(配送状态/配送线路，DMS 反查)+6 按钮+商品汇总+真实打印/导出；**本单金额口径已修正**（商品金额−优惠+运费+其他费用，列表/明细/打印三处统一）；见《发货查询开发文档》§6 |
| 收货业务 | 物流退货收货 | 70160 | `dispatch/return-receive` | `views/dispatch/return-receive/index.vue` | 复用《销售退货申请》视图 + `/api/erp/sale/return/*`、`/api/erp/sale/return-doc/*` | ✅ **金标准完成**（2026-09-13，E2E 90/90）：入口直达复用 `sales/return-apply`（双 Tab 默认 20/22 列、47/62 全量列配置、27/16 查询、6/5 按钮）+ **收货闭环**（退货单审核回写申请 已收/未收，部分收货/取消回退，消除双链路重复过账）；迁移 `V11.311.0`；二轮修「按明细行键重复（Vue Duplicate keys）」——明细查询补 `itemId` + 页面按 Tab 指定 `row-key`，见《物流退货收货开发文档》§6/§7（含 §7.1） |
| | 采购订货收货 | 70161 | `dispatch/purchase-receive` | `views/dispatch/purchase-receive/index.vue` | `/api/erp/purchase/order/doc-query/page` + `/detail-query/page`（复用）+ `/batch-print` | ✅ **金标准完成**（2026-09-13，二轮复验修复后 **E2E 104/104**）：双 Tab「按单据 26 列/默认 10 + 按明细 51 列/默认 16」+ 独立列/页面配置（13/11 查询项）+ 批量收货生成**带待收明细的《采购入库单》**+ F8 打印回写打印次数；二轮修 6 处真实缺陷（切 Tab 列显隐串用、F8 未绑定、打印次数死列、自定义数字字段查询失效、改查询条件替换功能按钮、收货后勾选残留），见《采购订货收货开发文档》§6/§7 |
| 调度管理 | 调度任务 | 80730 | `dms/dispatch-task` | `views/dms/dispatch-task/index.vue` | `taskApi.page`(+`/{id}/logs`、`/batch-assign`、`/batch-cancel`、`/batch-print`、`/{id}/assign`、`/{id}/reassign`) + `/api/dms/dispatch/*` | ✅ **金标准完成 2026-09-13，2026-09-14 复核**（E2E **175/175**）：31 列(序号/勾选/操作 + 28 业务列：14 默认/14 隐藏)、13 查询项、10 功能按钮、行级 5 操作；指派/改派**负载硬门控**（并接上限·载重·容积·休息中）+ **调度审计 `dms_task_log`** + 批量指派/取消/打印**逐单反馈** + 自动调度结果报告 + **超时升级（换人重派）** + 地图派单；修复**雪花 ID `Number()` 精度丢失致指派错人**；迁移 `V11.320.0`；见《调度任务开发文档》§8 |
| | 智能调度 | 80850 | `dms/dispatch` | `views/dms/dispatch/index.vue` | `/api/dms/dispatch/*`（strategy/auto·preview/auto/assign/reassign/batch-assign/escalate-overdue/candidates/fence-check/stat + **route-rider 区域分包绑定 6 个**）+ `/api/dms/task/page`（列表复用） | ✅ **金标准完成 2026-09-13**（E2E **92/92**）：12 端点齐全、空壳已消灭；按 §3.1 **重定位「调度策略与执行台」三 Tab**（策略配置 / 调度执行 / 效果复盘）——策略落配置中心即热生效、预览不落库 + 逐单命中规则与拒绝原因、候选评分与在途负载、自动占比/平均派单耗时/超时率；**区域分包（按线路档案绑定配送员，严格/偏好两档）+ 在线心跳口径统一 + 抢单·竞价独立计数**；迁移 `V11.218.0` / `V11.321.0`；见《智能调度开发文档》§7 |
| | 订单池 | 80860 | `dms/order-pool` | `views/dms/order-pool/{index,bid-detail}.vue` | `/api/dms/order-pool/*`（page/export/publish/offline/enable-bid/**disable-bid**/settle/force-assign/expire-scan/grab/bid/bid-list/cancel-bid） | ✅ **金标准 + 二轮独立复验**（2026-09-13，E2E **129/129**）：**21 列（18 默认 + 3 隐藏：配送线路/区域、下架原因、下架时间）** + 池状态机（**含「关闭竞价」竞价中→待抢单**）+ 抢单/竞价/结算/强制分配/下架闭环（**中标与任务指派同事务**）+ 发布到池·开启竞价·强制分配·代客出价弹窗 + 竞价详情页；二轮查出并修复 **8 处真实缺陷**（逻辑删除未过滤致撤销报价仍可中标、已接单池可重复发布致双接单、接单配送员/线路查询条件静默失效、下架原因不落库、结算不过资质门控等）+ 补 **关闭竞价**能力；迁移 `V11.331.0`；见《订单池开发文档》§5 |
| 配送跟踪 | 实时跟踪 | 80740 | `dms/realtime-tracking` | `views/dms/realtime-tracking/index.vue` | `/api/dms/tracking/*`（rider-page/stat/alerts/replay/stream/clean-expired/export…） | ✅ **金标准完成 2026-09-13**（E2E **86/86**，同日第二轮补齐遗留）：位置聚合一次分页（去 N+1）+ 后端统计卡 + 地图回放（倍速/抽稀）+ 双 Tab 两套列配置 + **四类异常预警（含偏航）** + **SSE 实时推送 + 30s 兜底** + **合规（采集时段/保留策略）**；迁移 `V11.217.0` / `V11.310.0`；见《实时跟踪开发文档》§7 |
| | 配送跟踪 | 80870 | `dms/tracking` | `views/dms/tracking/index.vue` | `/api/dms/tracking/*`（page/mileage/export/track-vo/task-vo/replay/report/latest） | ✅ **金标准完成 2026-09-13**（E2E **97/97**）：15 列(11 默认+4 隐藏)、6 查询项+快捷时间、左配送员过滤面板（**在线/离线=位置上报心跳口径**，与《配送员管理》《实时跟踪》统一）、段里程/里程聚合、地图查看+逐点回放（复用 RouteMapCanvas）、来源字典、真实 xlsx；**+查询审计留痕（`@OperationLog`→`sys_oper_log`）+异常联动预警（超速/异常停留推《人员核验》，带去重）**；迁移 `V11.215.0`（第二轮三项补齐**零迁移**）；见《配送跟踪开发文档》§6 |
| | 签收管理 | 80900 | `dms/sign` | `views/dms/sign/index.vue` | `/api/dms/sign/*`（page/detail/stat/export/submit/audit/batch-audit/delete） | ✅ **金标准完成 2026-09-13**（PC E2E 101/101 + **司机端 E2E 58/58**）：22 列(12 默认+10 隐藏)、8 查询项、统计条、审核流转（通过→任务已完成 / 驳回→退回配送中）、部分签收数量、偏差阈值配置化、事件外发、真实 xlsx；**司机端「配送 Tab→详情→签收/收款」由原型桩改真实契约**（文档 §7）；迁移 `V11.214.0`；见《签收管理开发文档》§6/§7 |
| 结算收款 | 配送结算 | 80910 | `dms/settlement` | `views/dms/settlement/index.vue` | `/api/dms/settlement/*`（rule/rule-page/fee/report/page/generate/{id}/items/confirm/push-erp/**reconcile**） | ✅ **金标准（二轮收口）**（2026-09-13，E2E **87/87**，4 Tab）：**计费规则表**（按 结算对象×渠道/线路×生效期×优先级 差异化，未命中回落配置化缺省费率）+ 结算单闭环（生成→确认→推送 ERP **真实记账**：凭证 6602/2202·2241 + 外部运力应付，双幂等）+ **渠道结算**（`target_type=2`）+ **部分签收折算**（拒收不计费）+ **对账**（凭证一致性/应付核销/配送费收款 + 差异清单）+ **防重复计费**；迁移 `V11.330.0`；见《配送结算开发文档》§3/§6 |
| | 收款管理 | 80920 | `dms/payment` | `views/dms/payment/index.vue` | `/api/dms/payment/*`（dict/qrcode/confirm/confirm-batch/callback/mark-unpaid/page/export/stat/handover/handover-summary/unpaid-*/urge/promise/write-off/collections/push-finance/flow-*/reconcile） | ✅ **金标准**（2026-09-13 两轮收口，E2E **104/104**）：**四 Tab**（收款台账/**未付管理**/交款稽核/**支付流水**）+ 假二维码→**配置化**（未开通不生成）+ 回调**幂等** + 交款稽核（应上交 vs 已上交 + **交款超时预警**）+ 挂账**催收/核销闭环** + 流水**逐笔对账（5 类结果）** + **推送财务幂等** + 现金**单笔/单日限额**；迁移 `V11.322.0` |
| 配送配置 | 配送参数 | 80750 | `dms/config-params` | `views/dms/config-params/index.vue` | `/api/dms/config/*`（page/meta/batch/{key}/reset/{key}/history/{key}/rollback/**export/import**） | ✅ **金标准完成 2026-09-13（二轮补齐）**（E2E **86/86**）：**元数据驱动**（47 键/9 分组，类型·默认值·范围·单位·生效方式）+ 左分组树 + 类型化控件（数字/开关/下拉）+ 批量保存（事务 + diff 预览）+ 恢复默认 + 变更历史/回滚 + 敏感键脱敏与「留空=不修改」+ **参数集导出/导入（JSON：预览差异/逐项失败原因/整体拒绝/幂等回灌/敏感键不导出明文）** + 查询补作用域·是否可改·更新时间；首轮修「参数中心接口 tenantId 默认 0 → 误写全局行」真实缺陷；见《配送参数开发文档》§7 |
| | 渠道管理 | 80880 | `dms/channel` | `views/dms/channel/index.vue` | ✅ `/api/dms/channel/*`（14 接口，含 push-order / orders / callback-logs / callback） | ✅ **金标准（两轮）**（2026-09-13 首轮 + 2026-09-14 二轮，E2E **102/102**）：13 列(11 默认+2 隐藏) + 表头齿轮列配置 + 页面配置(6 条件/7 按钮) + 4 Tab 表单；真删除+引用保护、批量启停/删除、连通性测试、运力同步、凭据脱敏+**AES-GCM 加密**+回写保护；**渠道派单（幂等·重试·台账）/ 回调验签+时间戳+防重放+状态回写 / 自动调度接线（渠道失败降级自有运力）/ 外部单台账+回调日志 / Stub 适配器去伪**；迁移 `V11.230.0` / `V11.350.0` |
| | 配送配置 | 80890 | `dms/config` | `views/dms/config/index.vue` | `configApi.list`（只读） | ✅ **业务域总览（只读，8 分组）**（2026-09-13）：与「配送参数」分工明确——本页只读总览、参数页唯一可写（编辑+变更历史），消除两套 KV 维护 |
| API监控 | API监控 | 90107 | `trade/api-monitor` | `views/trade/api-monitor/list.vue` | `/api/trade/api-monitor/*`（stat/deps/calls(page·stat·trend·endpoints·export)/sandbox·invoke/alerts/thresholds/sync(stat·retry)/clean-expired）+ `/api/trade/inventory-sync/(page·export)` | ✅ **金标准**（2026-09-14，E2E **119/119**）：8 KPI 卡全后端真实聚合（含 P95）+ **依赖健康逐项**（DB/Redis/MQ-TCP/地图/渠道）+ **联调沙箱**（5 组 12 接口，真实回环 + 请求号 + 历史）+ 三 Tab 台账（逐 Tab 列配置）+ 页面配置 + F5/自动刷新 + 真实 xlsx；调用日志**复用扩展 `api_access_log`**（IN/OUT/SANDBOX 三方向埋点）；同步记录**失败重试**（复用原记录）/6 类失败分类/导出；告警 5 类（配置化阈值 + Redis 静默期 + `dms_event_outbox` 事件外发）；迁移 `V11.360.0/1/2`；见《API监控开发文档》§6 |

**菜单变更记录**：
- `V11.168.0`（2026-09-12）：① `60501 线路管理`→**配送路线**、`80700 线路列表`→**配送路线单**；② `60502 人车管理`补齐 3 个子菜单（车辆管理/车辆维护/配送员管理）——此前该分组**无子菜单**、mega 面板不渲染、页面不可达。
- `V11.169.0`（2026-09-12）：补齐 8 个页面菜单（配送仪表盘/路线规划/实名认证/智能调度/订单池/配送跟踪/渠道管理/配送配置）。
- `V11.170.0`（2026-09-12）：补齐**缺失能力**菜单——新增分组「**结算收款**」(60507) + 3 项：`80900 签收管理`（挂配送跟踪组）、`80910 配送结算`、`80920 收款管理`；同步新建 3 个前端页面与 3 个 api 文件（`api/dms/{sign,settlement,payment}.ts`），并在 `dynamicRoutes.ts` componentMap 注册。
- `V11.300.0`（2026-09-13）：**修复消息底座** `sys_message.id` 无自增来源（`AUTO` 策略却无序列/identity 且 NOT NULL）——此前邮件/站内信/短信**全部写不进去**；补序列 + 默认值。配送侧配套：`erp_delivery_eta_notify.message_id`（`V11.216.0`）关联底座消息。
- `V11.310.0`（2026-09-13）：**实时跟踪补齐 §5/§6 遗留**——新增配置 `dms.tracking.deviation.meters`（偏航阈值）/`collect.hours`（位置采集时段）/`retention.days`（轨迹保留天数），全局 + 系统租户双行落库；配套后端 **SSE 推送通道**（`GET /api/dms/tracking/stream`）、**四类异常预警**（含偏航）与**合规清理**（`POST /tracking/clean-expired` + 每日按租户定时清理）。**不新增业务表/列**（位置页是纯聚合读模型），不改菜单。
- `V11.320.0`（2026-09-13）：**调度任务金标准**数据侧——新建 `dms_task_log`（调度审计：指派/改派/取消/异常/自动调度/超时升级/批量操作，含变更前后配送员与操作人快照、原因），支撑 `GET /api/dms/task/{id}/logs`。**只加表，不改菜单、不改 `dms_task` 语义**。
- `V11.322.0`（2026-09-13）：**收款管理金标准第二轮**数据侧——新建 `dms_payment_flow`（支付平台流水，`(tenant_id, channel_code, trade_no)` 部分唯一索引，支撑导入幂等与逐笔对账）、`dms_payment_collection`（挂账催收/承诺/核销流水）；`dms_payment` 补 `finance_push_status` / `finance_trace_id`（推财务幂等）；预置资金安全 3 键（现金单笔/单日限额、交款时限）。**只加表/列 + 预置配置，不改菜单**。
- `V11.350.0`（2026-09-14）：**渠道管理第二轮（对接能力落地）**——新建 `dms_channel_order`（外部单台账，幂等键 `taskNo:channelId` 部分唯一索引）与 `dms_channel_callback_log`（回调日志，`(tenant_id, channel_id, nonce)` 部分唯一索引防重放）；预置 5 个渠道对接配置键（`dms.channel.push.retry` / `.push.fallback` / `.callback.tolerance.seconds` / `.config.encrypt` / `.config.encrypt-key`，全局 + 系统租户双行）。支撑**派单接线**（`DispatchService` 读 `channelId` 走适配器下单 + 幂等重试 + 降级自有运力）、**回调验签与状态回写**、**凭据 AES-GCM 加密存储**。**只加表/键，不改菜单**；⚠️ 迁移号避让：当日并行会话已占用 11.34x 段，本页取 11.350.0。
- `V11.321.0`（2026-09-13）：**智能调度「区域分包」落地**——新建 `dms_route_rider`（线路档案 × 配送员绑定：快照编号/名称、优先级、启停 + `(tenant_id, route_id, rider_id) WHERE deleted=0` 部分唯一索引）+ 配置键 `dms.dispatch.area.strict`（严格模式，全局 + 系统租户双行）。线路主数据仍只读引用 `erp_route`（不新建线路表），供 `DispatchService` 的 AREA 策略消费。**只加表/键，不改菜单**。
- `V11.331.0`（2026-09-13）：**订单池二轮复验**数据侧——`dms_order_pool` 补 `offline_reason`/`offline_time`（下架原因留痕，此前接口收参但只写日志、页面填的原因静默丢失）+ `(tenant_id, task_id, pool_status)` 索引（入池判重）。仅补列/索引，不改菜单、不改状态机语义。⚠️ 迁移号避让：当日并行会话已占用 `11.321/11.322/11.330`。
- `V11.340.0`（2026-09-14）：**销售物流域生产级升级（P0/P1/P2）**数据侧——`erp_sale_order_logistics` 补 `logistics_company_id`（承运商档案引用）、包裹层 5 列（`package_no/count/weight/volume/status`）、`remark`、`update_time`、运费对账 3 列（`freight_bill_amount/freight_diff/freight_reconciled`）+ 2 索引；新建 `erp_freight_rule`（承运商×区域×重量区间 首重/续重）、`erp_shipment_notify`（ASN 台账，`(tenant_id,idempotent_key)` 唯一）。**该子表早已存在但前端从未接线（0 行）**，本次接线并补齐。
- `V11.341.0`（2026-09-14）：**新增菜单** `70162 物流运费对账`（挂 `60402 发货业务`，path `dispatch/freight-reconcile`）——本系统新增能力（ql361 无对标），承载 运费规则 / 运费对账 / 发货通知 ASN 三 Tab。
- `V11.330.0`（2026-09-13）：**配送结算二轮收口**数据侧——新建 `dms_settlement_rule`（计费规则表：结算对象/适用渠道/适用线路、计价方式、费率与加价、生效期、结算周期、优先级）；`dms_settlement` 补 ERP 记账回执列（`rule_id`/`erp_voucher_no`/`erp_payable_id`/`erp_payable_no`）；`dms_settlement_item` 补签收口径列（`sign_type`/`planned_quantity`/`actual_quantity`/`billing_ratio`/`weight_fee`）；预置 3 个记账科目配置键（`dms.settlement.account.*`）。仅建表/补列，不改菜单。
- `V11.211.0`（2026-09-13）：**配送路线单能力升级**——`erp_delivery_route` 补 `fence_id`/`fence_name`/`auto_collect`；`erp_route_point` 补 `eta_time`/`expedited`；`biz_party` 补配送坐标 `latitude`/`longitude`；新建 `erp_delivery_eta_notify`（ETA 通知台账，通道后接）。仅补列/建表，不改菜单。
- `V11.171.0`（2026-09-12）：**配送路线单金标准**数据侧改造——`erp_delivery_route` 补 11 列（route_id/route_name/route_type/vehicle_id/vehicle_no/plan_date/actual_duration/failed_points/cancel_time/cancel_reason/create_by_name）+ 4 个索引（含 `(tenant_id, route_code) WHERE deleted=0` 部分唯一索引）；`erp_route_point` 补 `tenant_id`（明细表租户列，缺失会导致多租户插件注入条件后 SQL 报错）+ `signee`/`sign_time`/`fail_reason`/`update_time`。仅补列与索引，不改菜单。
- `V11.180.0`（2026-09-12）：**路线规划金标准**数据侧——新建 `dms_geo_fence`（电子围栏档案：圆形/多边形、绑定线路档案/渠道/仓库区域、编码唯一部分索引），支撑 `POST /api/dms/route/fence-check` 与围栏 CRUD。仅加表，不改菜单。
  - ⚠️ **迁移号避让**：当日并行会话有 4 份重号的 `V11.171.0` 文件（Flyway 启动即失败），本页取 **11.180.0**；多会话并行时先 `ls db/migration | tail` 找空号。
- `V11.181.0`（2026-09-13）：**地图服务 Key 配置落位**——`dms_config` 预置全局 4 键（`map.default-provider` + `map.amap/tencent/baidu.api-key`）+ `(tenant_id, config_key) WHERE deleted=0` 部分唯一索引；配套在 **core-api `application.yml`** 显式声明 `dms.map.*`（⚠️ 模块自带的 `dms-application.yml` 位于嵌套 jar 内**不会被 core-api 加载**，这是「设了环境变量仍降级」的根因）。四种配置落位与排障见《路线规划开发文档》**§8**。
- 菜单相关表：`sys_menu`（已补全 32 条含分组）、`sys_role_menu`、`sys_tenant_menu` —— 后两者现有配送菜单亦无记录（超管全量下发），**无需补授权**。

### 1.2 ✅ 已修复：运行 jar 未包含 DMS 模块（2026-09-12）

2026-09-12 实测：`/api/dms/*` **全部接口 500**。定位过程：`sys_menu` 与前端路由均正常、`dms_vehicle`/`dms_rider` 表存在且 SQL 可查、`cn.aiedge.dms` 已在 core-api `scanBasePackages` 内；最终以 `unzip -l core-api-0.3.17-exec.jar | grep BOOT-INF/lib/dms` 确认——**运行中的 fat jar 里没有 `dms-delivery` 嵌套 jar**（313 个 `BOOT-INF/lib` 中无 DMS），即 DMS 的 Controller/Mapper 从未被加载。

- 产物链现状：`backend/dms/target/dms-delivery-0.3.17.jar` 存在、`classes` 131 个 class；但本地 m2 只有 `dms-delivery/0.2.0`（2026-06-19），**无 0.3.17/0.3.18**。
- 影响面：配送模块**全部后端能力不可用**（车辆/配送员/调度/跟踪/参数/任务），前端页面只能渲染空态。
- **修复状态**：✅ 已修复——`dms-delivery` 已 `install` 进 m2（含 0.3.18），`core-api-0.3.17-exec.jar` 现包含 `BOOT-INF/lib/dms-delivery-0.3.17.jar`，`/api/dms/*` 由 500 变为 401（Controller 已加载，仅需登录）。
- 后续新增 DMS 代码仍需：`mvn -pl dms clean install -DskipTests` → 重新 `package` core-api → **重启后端**（共享环境操作需协调并行会话；本次仪表盘验收采用**独立 fat jar 副本 + `jar u0f` 替换嵌套 jar + 独立端口**，不影响他人实例；详见 `AI_DEVELOPER_RULES.md` 7.3）。

**菜单归属补充**：`线路`（`md:route`，菜单ID 70530）挂在 `资料 → 配送管理`，业务属配送线路**档案**，见《线路开发文档》；与「配送路线单」（执行单）**不是同一对象**。

---

## 2. 文档索引（22 个菜单项，一一对应）

### 2.1 对标 ql361 实测（6 篇）

| 文档 | 菜单项 | 实测金标准 |
|------|--------|-----------|
| [配送查询开发文档](./配送查询开发文档.md) | 配送查询 | 24 列配置（默认 20 + 操作）/ 无页面配置弹窗 |
| [物流发货开发文档](./物流发货开发文档.md) | 物流发货 | 37 列（默认 17）/ 查询 16 / 按钮 6 |
| [发货查询开发文档](./发货查询开发文档.md) | 发货查询 | 49 列（默认 14）/ 查询 25 / 按钮 6 |
| [物流退货收货开发文档](./物流退货收货开发文档.md) | 物流退货收货 | 按单据 47（默认 20）/ 按明细 62（默认 22） |
| [采购订货收货开发文档](./采购订货收货开发文档.md) | 采购订货收货 | 按单据 28（默认 11）/ 按明细 51（默认 16） |
| [线路开发文档](./线路开发文档.md) | （主数据，菜单在资料域） | 6 列全默认 / 无页面配置弹窗 |

### 2.2 本系统新增 · 无对标（16 篇，按金标准 + 本系统实现 + 业界实践撰写）

| 文档 | 菜单项 | 路由 | 一句话 |
|------|--------|------|--------|
| [配送单开发文档](./配送单开发文档.md) | 配送单 `[历史]` | `dispatch/dispatch-order` | 双入口单据（列表 + 表单） |
| [配送仪表盘开发文档](./配送仪表盘开发文档.md) | 配送仪表盘 | `dms/dashboard` | 运营看板（✅ 金标准完成，E2E **149/149**） |
| [配送路线单开发文档](./配送路线单开发文档.md) | 配送路线单 | `dms/route-list` | 配送路线**执行单**（VRP/多点签收/围栏归集/催单/ETA）｜✅ **金标准 + 能力升级** E2E 217/217 |
| [路线规划开发文档](./路线规划开发文档.md) | 路线规划 | `dms/route` | LBS 工具（规划/编码/逆编码/坐标转换/围栏，✅ 金标准，E2E 129/129） |
| [车辆管理开发文档](./车辆管理开发文档.md) | 车辆管理 | `dms/vehicle` | 车辆档案 + 绑定配送员 |
| [车辆维护开发文档](./车辆维护开发文档.md) | 车辆维护 | `dms/vehicle/maintenance` | 维保记录 |
| [配送员管理开发文档](./配送员管理开发文档.md) | 配送员管理 | `dms/rider` | 配送员档案（企业员工/外部平台） |
| [实名认证开发文档](./实名认证开发文档.md) | 实名认证 | `dms/verification` | ✅ 4 Tab：实名认证 KYC + 人车核验（E2E 175/175 + 司机端 14/14） |
| [调度任务开发文档](./调度任务开发文档.md) | 调度任务 | `dms/dispatch-task` | 调度工作台（指派/改派/批量/超时升级/地图派单/审计）｜✅ **金标准** E2E 175/175 |
| [智能调度开发文档](./智能调度开发文档.md) | 智能调度 | `dms/dispatch` | 派单策略与执行台：四类策略（含**区域分包**按线路档案绑定配送员）+ 在线心跳口径 + 复盘（✅ 金标准，E2E 92/92） |
| [订单池开发文档](./订单池开发文档.md) | 订单池 | `dms/order-pool` | 抢单/竞价（✅ 金标准 + 二轮复验，E2E 129/129） |
| [实时跟踪开发文档](./实时跟踪开发文档.md) | 实时跟踪 | `dms/realtime-tracking` | 配送员位置 + 轨迹回放（未接地图/推送） |
| [配送跟踪开发文档](./配送跟踪开发文档.md) | 配送跟踪 | `dms/tracking` | 轨迹点台账（✅ 金标准） |
| [签收管理开发文档](./签收管理开发文档.md) | 签收管理 | `dms/sign` | 电子签收台账/审核 + **司机端签收/收款双端打通**（✅ PC 101/101 + 司机端 58/58） |
| [配送结算开发文档](./配送结算开发文档.md) | 配送结算 | `dms/settlement` | 计费规则/结算单/推 ERP 真实记账/对账（✅ 金标准二轮，E2E **87/87**） |
| [收款管理开发文档](./收款管理开发文档.md) | 收款管理 | `dms/payment` | 代收货款/配送费收款（✅ 金标准两轮，E2E **104/104**：四 Tab / 未付闭环 / 对账 / 推财务 / 资金限额） |
| [配送参数开发文档](./配送参数开发文档.md) | 配送参数 | `dms/config-params` | 参数中心（元数据驱动 + 类型化控件 + 审计回滚 + 导入导出）｜✅ 金标准 E2E 86/86 |
| [渠道管理开发文档](./渠道管理开发文档.md) | 渠道管理 | `dms/channel` | 运力渠道 + **派单接线/回调验签/凭据加密**（✅ 金标准两轮，E2E 102/102） |
| [配送配置开发文档](./配送配置开发文档.md) | 配送配置 | `dms/config` | 租户级配置（⚠️ 与参数页重叠） |
| [API监控开发文档](./API监控开发文档.md) | API监控 | `trade/api-monitor` | 接口监控 + 依赖健康 + 联调沙箱 + 同步重试/告警（✅ 金标准，E2E **119/119**） |
| [配送模块（DMS）开发文档](./配送模块DMS开发文档.md) | —（模块级总览） | `dms/*` | 数据模型 / 边界 / 公共能力 / 实施路线 |

---

## 3. 对标实测覆盖状态

| 页面 | 对标实测 | 列配置弹窗 | 页面配置弹窗 | 备注 |
|------|:-------:|:---------:|:-----------:|------|
| 配送查询 | ✅ 2026-09-12 | ✅ 24 列 | ❌ 无齿轮 | ✅ 金标准已完成（E2E `tools/e2e-dispatch-query.cjs`） |
| 配送单 | ❌ 无对标（ql361 无此页面） | ✅ 24 列（默认 20） | ✅ 查询 16 / 按钮 6 | ✅ 金标准已完成 2026-09-12（E2E `tools/e2e-dispatch-order.cjs`，194/194） |
| 物流发货 | ✅ 2026-09-12 | ✅ 37 列（默认 17） | ✅ 查询 16 / 按钮 6 | ✅ 金标准完成 2026-09-13（E2E `tools/e2e-logistics-ship.cjs`，**90/90**）；复用「订单处理中心」2.拣货/发货阶段视图 + 物流/备注 15 字段弹窗 |
| 发货查询 | ✅ | ✅ 49 列 | ✅ 查询 25 / 按钮 6 | 销售出库单 XSCKD 台账 |
| 物流退货收货 | ✅ | ✅ 47 + 62 列 | ✅ 27/16、6/5 | 复用「销售退货申请-历史」 |
| 采购订货收货 | ✅ | ✅ 28 + 51 列 | ✅ 13/11、5 | 复用「收货处理」 |
| 线路 | ✅ 09-07/09-12 | ✅ 6 列 | ❌ 无 | 配送区域=行政区划多选 |
| 配送路线单 | ❌ 无对标（ql361 无「配送」菜单） | ✅ 27 列（17 默认） | ✅ 查询 8 / 按钮 8 | ✅ 金标准 + 能力升级（E2E `tools/e2e-dms-route-list.cjs`，192/192；含围栏归集/规划/催单/ETA+短信底座/地图查看/通知台账/司机端） |
| 用车管理 | ❌ 无对标（ql361 无「配送」菜单） | ✅ 每 Tab 独立 | ✅ 每 Tab 独立 | ✅ 金标准已完成（E2E 261/261）：补能流水自动区间里程/每公里成本、补能卡「一卡一车一人」稽核、能耗报表（油电对比+CO₂）、司机端补能登记 |
| 人员核验 | ❌ 无对标（ql361 无「配送」菜单） | ✅ 每 Tab 独立 | ✅ 每 Tab 独立 | ✅ 金标准已完成（E2E `tools/e2e-verification.cjs`，261/261；司机端 `tools/e2e-driver-verification.cjs`，18/18） |
| 配送仪表盘 | ❌ 无对标 | — | — | ✅ 金标准已完成（E2E `tools/e2e-dms-dashboard.cjs`，**149/149** 独立复验；14 KPI 卡/3 图表/下钻闭环；复验修 order_type NULL 致 500 + 调度任务页图标白屏） |
| 配送员管理 | ❌ 无对标（本系统新增） | ✅ 19 列（默认 14） | ✅ 查询 8 / 按钮 9 | ✅ 金标准完成 2026-09-12（E2E 118/118） |
| 车辆管理 | ❌ 无对标（本系统新增） | ✅ 22 列（17 默认） | ✅ 查询 8 / 按钮 7 | ✅ 金标准完成 2026-09-12（E2E 121/121） |
| 签收管理 | ❌ 无对标（本系统新增） | ✅ 22 列（12 默认） | ✅ 查询 8 / 按钮 4 | ✅ 金标准完成 2026-09-13（PC E2E `tools/e2e-dms-sign.cjs` 101/101 + 司机端 `tools/e2e-driver-sign.cjs` 58/58） |
| 配送跟踪 | ❌ 无对标（本系统新增） | ✅ 15 列（11 默认） | ✅ 查询 6 / 按钮 3 | ✅ 金标准完成 2026-09-13（E2E **97/97**；含查询审计留痕 + 异常联动预警） |
| 实时跟踪 | ❌ 无对标（本系统新增） | ✅ 16 + 11 列（两套） | ✅ 查询 4 / 按钮 5 | ✅ 金标准完成 2026-09-13（E2E **86/86**；含 SSE 推送/偏航预警/合规采集与保留） |
| 渠道管理 | ❌ 无对标（本系统新增） | ✅ 13 列（11 默认） | ✅ 查询 6 / 按钮 7 | ✅ **金标准（两轮）**（E2E `tools/e2e-dms-channel.cjs`，**102/102**；真删除+引用保护/凭据脱敏+加密/派单接线/回调验签与防重放/外部单台账+回调日志） |
| 调度任务 | ❌ 无对标（ql361 无「配送」菜单） | ✅ 31 列（28 业务列：14 默认/14 隐藏） | ✅ 查询 13 / 按钮 10 | ✅ 金标准完成 2026-09-13（2026-09-14 复核 E2E **175/175**；负载硬门控 + 调度审计 + 批量逐单反馈 + 超时升级 + 地图派单） |
| 智能调度 | ❌ 无对标（ql361 无「配送」菜单） | ✅ 任务表 10 列 + **区域分包绑定 9 列** | ✅ 查询 2 / 按钮 5 | ✅ 金标准完成 2026-09-13（E2E `tools/e2e-dms-dispatch.cjs` **92/92**；三 Tab 策略台 + 区域分包绑定 + 在线心跳口径 + 抢单·竞价归口统计） |
| 订单池 | ❌ 无对标（ql361 无「配送」菜单） | ✅ 21 列（18 默认 + 3 隐藏） | ✅ 查询 10 / 按钮 7 | ✅ 金标准 + **二轮独立复验**（E2E `tools/e2e-order-pool.cjs` **129/129**；抢单/竞价/结算/关闭竞价 + 撤销报价不参与结算 + 下架留痕；见《订单池开发文档》§5） |
| API监控 | ❌ 无对标（ql361 无「配送」菜单） | ✅ 调用日志 14 列（默认 8）+ 同步记录 12 列（默认 10）+ 告警 9 列 | ✅ 查询 12 / 按钮 4 | ✅ **金标准完成 2026-09-14**（E2E `tools/e2e-api-monitor.cjs`，**119/119**；8 KPI 全真实含 P95 / 依赖健康逐项 / 联调沙箱 12 接口 / 同步失败重试 / 5 类告警 + 静默期 + 事件外发；调用日志复用扩展 `api_access_log`） |
| 其余 8 项 | ❌ 无对标（ql361 无「配送」菜单） | — | — | 按金标准 + 本系统 + 业界实践撰写 |

---

## 4. 域级说明

- **单据编号规则**（实测）：销售订单 `XSDD-`、销售出库单 `XSCKD-`，后缀 `YYYYMMDD-序号`；**配送路线单 `PSXL-YYYYMMDD-序号`（2026-09-12 起，`GET /api/delivery/route/next-no`）**；**配送单 `PSD-YYYYMMDD-序号`（2026-09-12 起，`GET /api/dms/task/next-no`，后端为权威）**。
- **入口直达现象**：3 个菜单为「直达型」——物流发货 → 订单处理中心 2.拣货/发货；物流退货收货 → 销售退货申请-历史；采购订货收货 → 收货处理。落地**必须复用目标视图组件**，不得重复造页面。
- **状态流转主线**：
  - 销售线：销售订单(XSDD) → 订单处理中心审核 → 拣货/发货 → 销售出库单(XSCKD) → 配送任务（待配送/配送中/已配送）
  - 退货线：销售退货申请 → 物流退货收货（已收/未收）→ 销售退货单入库
  - 采购线：采购订单 → 采购订货收货（待收货/部分收货）→ 采购入库单
- **配送员两类来源**：企业员工（`RiderTypeEnum.OWN_STAFF`，关联 `userId`）/ 外部平台配送员（`PLATFORM_RIDER`，关联 `channelId`）/ 众包兼职 / 社会车辆司机。
- **状态口径**：执行态 `TaskStatusEnum` 9 态只在执行侧展示；对外台账统一映射为 `待配送 / 配送中 / 已配送`。
- **围栏归集口径**（2026-09-13）：销售出库单（已发货）/ 销售订单（待发货·部分发货）的**客户配送坐标**落入路线单绑定的电子围栏 → 自动入线；围栏**档案**由《路线规划》维护，执行侧只绑定判定；围栏外单据支持**手动补进**。客户配送坐标单一口径 = `biz_party.latitude/longitude`。

---

## 5. 通用待复核项

- ~~配送查询「批量打印」打印模板配置弹窗~~ ✅ 2026-09-12 源码实测已补全（`BatchPsSelectPrintTemplate`：打印内容复选 + 模板下拉 + 「仅打印未打印过的单据」+ 本地/远程打印机；行内操作列 = 打印/补打 + 单据明细 + 取消，见《配送查询开发文档》§2、§5.5）。
- 三个直达页行内「更多」菜单明细（~~物流发货~~ ✅ 2026-09-13 二次实测：ql361 = `修改 / 终止 / 物流·备注`，本系统已接 `查看 / 物流·备注 / 打印`（修改·终止属销售订单域，待接）；物流退货收货 / 采购订货收货 仍待复核）。
- 配送任务编号、收货单据编号规则。
- 收货确认与采购入库单/销售退货单的生成先后关系。

---

## 6. 写作与开发原则

- **不重复通用规范**：统一指向《对标开发技术参考文档》。
- **不发明字段**：字段名出自实测弹窗原始列名；无对标页如实标注「本系统建模」。
- **两类配置严格分开**：查询条件/功能按钮 →「页面配置」；默认列 + 全量可配置列 →「数据表格列配置」。
- **页面组件优先复用**、**功能/模块不重复开发**、**阶段结束前回写开发文档**。
- **【金标准】模块级 E2E** 放 `tools/e2e-<模块>.cjs`；**【金标准】启动进程随手关**（见 `AI_DEVELOPER_RULES.md` 7.3）。

### 6.1 模块级巡检工具（2026-09-13 增补）

| 工具 | 作用 | 用法 |
|------|------|------|
| `tools/smoke-dms-pages.cjs` | **全模块页面冒烟**：逐个打开 25 个页面，断言不弹登录 / 不白屏 / 无 `pageerror`·`console.error`（只抓构建期查不出的运行期 P0，如路由模块 import 失败导致的白屏） | `ERP_PORT=5673 FE_URL=http://localhost:5656 node tools/smoke-dms-pages.cjs`（结果落 `tool-results/dms-smoke/result.json`） |
| `tools/audit-icons-vue.cjs` | **图标名静态闸门**：校验全前端 `@ant-design/icons-vue` 导入的图标在**已装版本**中存在（`AlarmOutlined` 在 v7.0.1 已移除 → 曾使《调度任务》整页白屏、仪表盘两处下钻失效） | `node tools/audit-icons-vue.cjs`（非 0 退出即存在白屏风险） |

> 冒烟为**页面级**回归（快、粗）；功能正确性仍以各页 `tools/e2e-*.cjs` 为准。改依赖版本、批量换图标、或新增路由页后务必先跑这两个脚本。


---

## 7. 本模块配置落位索引（2026-09-13 增补）

> 统一口径见《对标开发技术参考文档》**§5.6 外部服务与密钥「配置落位」**；本模块涉及项如下（新增配置项请同步登记）。

| 配置项 | 消费页/代码 | 落位 | 生效 | 缺失行为 |
|--------|------------|------|------|---------|
| `dms.map.{amap,tencent,baidu}.api-key`、`dms.map.default-provider` | 《路线规划》`dms/route`（规划/编码/逆编码/围栏） | 环境变量 `AMAP_API_KEY`/`TENCENT_MAP_API_KEY`/`BAIDU_MAP_API_KEY` → core-api `application.yml` → 《配送参数》配置中心 `map.*` | 重启 / **热生效** | 直线降级；编码不可用并提示（详见《路线规划开发文档》§8） |
| `amap.key` | 《配送路线单》路线优化/重规划/导航（`erp-delivery-route`） | 复用同一把 `AMAP_API_KEY`（core-api `application.yml` 的 `amap.key`） | 重启 | 优化/导航不可用（建单不受影响） |
| `VITE_AMAP_KEY` | 司机端 `driver-delivery` 地图导航页 | `frontend/apps/driver-delivery/.env.*`（⚠️ 当前缺该文件 → 司机端地图无 Key） | **构建期** | 地图不渲染/导航异常 |
| `dms_channel.config_json` | 《渠道管理》运力渠道适配器（AppKey/AppSecret/回调地址/网关/签名方式） | **业务档案表字段**（渠道弹窗「对接配置」）；**敏感值加密落库**（AES-256-GCM，`ENCv1:` 前缀），服务端解密后供适配器/回调验签使用 | 页面保存即生效 | 适配器未接通（下单/同步如实失败，不伪造成功） |
| `DMS_CHANNEL_ENCRYPT_KEY` / 配置中心 `dms.channel.config.encrypt-key` | 《渠道管理》凭据加密 | 环境变量 → core-api `application.yml`（`dms.channel.encrypt-key`）→ 配置中心（`CHANNEL` 组） | 保存即热生效 | 用**内置开发默认密钥**并打 WARN（生产必须显式配置） |
| `dms.channel.config.encrypt`（凭据加密开关，默认 true） | 《渠道管理》保存渠道 | 配置中心 `dms_config`（`CHANNEL` 组） | 保存即热生效 | true（加密） |
| `dms.channel.push.retry` / `dms.channel.push.fallback` | 《渠道管理》派单 / 《智能调度》自动调度 | 配置中心 `dms_config`（`CHANNEL` 组，租户可覆盖） | 保存即热生效 | 重试 2 次（指数退避 150→300→600ms）/ 渠道失败**降级自有运力** |
| `dms.channel.callback.tolerance.seconds` | 《渠道管理》回调验签 | 配置中心 `dms_config` | 保存即热生效 | 300 秒（超出判定过期并拒绝） |
| 运力渠道适配器开关（`dms.channel.own/dada/meituan/shunfeng/taxi.enabled`） | 适配器装配（`@ConditionalOnProperty`） | core-api `application.yml`（环境变量 `DMS_CHANNEL_*_ENABLED`，默认仅 `own=true`） | 重启 | 仅装配自有运力；外部平台适配器未对接（如实失败） |
| 配送费费率 / 结算规则 | 《配送结算》 | **配置中心**（`dms_config` 的 `dms.settlement.*`，全局缺省）+ **计费规则表** `dms_settlement_rule`（按 结算对象 × 渠道/线路 × 生效期 × 优先级 差异化） | 保存即热生效 / 规则命中即生效 | 内置缺省费率（起步 5 元 / 2 元每公里） |
| 配送费记账科目 | 《配送结算》推送 ERP | 配置中心 `dms.settlement.account.debit.subject`（借）/`credit.subject`（贷·外部运力）/`staff.subject`（贷·自有配送员） | 保存即热生效 | 6602 / 2202 / 2241 |
| `dms.payment.qrcode.base-url`（收款码服务地址） | 《收款管理》`dms/payment` | 配置中心《配送参数》`dms_config`（`SETTLEMENT` 组，全局 + 租户可覆盖） | 保存即热生效 | **未配置则不生成二维码**（不返回示例 URL，页面提示走线下） |
| `dms.payment.cash.limit.per.order` / `.daily`（现金单笔/单日限额） | 《收款管理》收款确认 | 同上 | 保存即热生效 | 0 = 不限 |
| `dms.payment.handover.deadline.hours`（交款时限） | 《收款管理》台账/稽核 | 同上 | 保存即热生效 | 24 小时 |
| 商户号 / 商户密钥 / API 证书 / 支付回调地址 | 《收款管理》 | **支付渠道档案**（《资料 → 支付渠道》，表 `md_payment_channel`） | 页面保存 | 支付不可用（回调地址由 core-api 对外基址提供） |
| 监控告警阈值（`monitor.threshold.*` 5 键：错误率 / P95 / 失败数 / 同步失败数 / 静默期） | 《API监控》`trade/api-monitor` | ✅ 配置中心 `dms_config`（配送参数页「API监控」分组，全局 + 租户覆盖） | 保存即热生效 | 代码默认值 5% / 2000ms / 20 / 0 / 30 分钟 |
| 监控页自动刷新间隔（`monitor.threshold.auto-refresh-seconds`）、调用日志保留天数（`monitor.log.retention-days`） | 《API监控》 | 同上 | 保存即热生效 | 30 秒（默认不自动开启） / 30 天（0=不清理） |
| 派单策略参数（`dms.dispatch.*`，10 键：策略/三项权重/并接上限/在线要求/载重·容积/超时升级/**区域分包严格模式**） | 《智能调度》`dms/dispatch`（`DispatchService`） | 配置中心 `dms_config`（配送参数页可改，租户可覆盖） | 保存即热生效 | 代码默认值：NEAREST / 1:1:1 / 5 单 / 需空闲 / 2000kg / 10m³ / 30 分钟 / 非严格分包 |
| **区域分包绑定**（线路档案 × 配送员） | 《智能调度》策略配置 Tab（`RouteRiderController`） | **业务表** `dms_route_rider`（线路来自 `erp_route` 只读引用，页面保存） | 页面保存即生效（派单时实时解析） | 无绑定 → 非严格档按综合评分派单；严格档无人可派 |
| 在线判定阈值（供自动派单判离线） | 《智能调度》+《实时跟踪》等 | 配置中心 `dms.tracking.online.minutes`（**同一把键，不另立**） | 保存即热生效 | 2 分钟 |
| 实时跟踪：在线阈值 / 超速 / 停留 / 偏航 / 采集时段 / 保留天数（`dms.tracking.*`） | 《实时跟踪》`dms/realtime-tracking` | 配置中心 `dms_config`（配送参数页可改，租户可覆盖） | 保存即热生效 | 代码默认值：2 分钟 / 60 / 15 / **1000 米** / 全天 / **0=不自动清理** |
| 实时跟踪：SSE 推送间隔 | 《实时跟踪》SSE（`GET /api/dms/tracking/stream`） | `dms.tracking.stream.interval-ms`（core-api `application.yml`，可选覆盖） | 重启 | 5000ms |
| **配送超时升级定时扫描**（`dms.dispatch.escalateOverdue`） | 「系统管理 → 开发工具 → 定时任务」（`scheduled_task` 表 + `DmsDispatchEscalateJob`） | **调度任务行**：Cron `0 */10 * * * ?` + `enabled` + `execute_params`（`{"dryRun": true}` 可先演练）；多实例由 Redis 锁 `dms:job:lock:dms.dispatch.escalateOverdue` 互斥 | 页面启用即生效（默认 **停用**）/ 参数保存即生效 | 不自动扫描，仅页面/接口手动触发 |

> 前端 `VITE_*` 一律是**构建期**注入；运行时可配的地图 Key 由后端下发（`GET /api/dms/route/config` 只回 `keySource/envVarName/configKey`，不回显明文）。

---

## 8. 通用陷阱（跨页，2026-09-14 复核补充）

### 8.1 列表勾选：按 rowIndex 持有 → 残留与漂移（组件层已修）

`BillDetailTable` 的勾选状态原为 `checkedRows: Set<rowIndex>`，且**没有对外的清空方法**；
`BillTableList.clearSelection()` 只清了自己的 `selectedRecords`。后果有两类，且第二类会**改错数据**：

1. **批量条消失、行上仍勾着**：父层认为没选中，用户再点该行反而是「取消勾选」，要连点两次才能选中；
2. **勾选漂移**：换查询条件/翻页后，同一 `rowIndex` 指向**另一行** —— 批量指派/取消/删除会把用户**没勾选**的单据一起提交。
   （实际案例：调度任务的批量取消把「上一屏勾的两条」取消掉了，而当前屏勾的两条没动。）

**修复（组件层，全站列表页受益）**：
- `BillDetailTable`：新增 `clearSelection()`（清 `checkedRows` + `checkedRecords`）并 `defineExpose`；
- `BillTableList.clearSelection()`：委托调用子表，父/子状态不再脱节；
- `BillDetailTable`：`watch(dataSource)` 比较**行标识序列**（`rowKey` 或 `id`），集合变了就自动清空 → 从根上杜绝漂移，同数据重渲染仍保留勾选；
- 页面侧：查询 / 重置 / 翻页 / 排序 / 批量操作完成后主动 `clearSelection()`。

### 8.2 共享 devdb 上的 E2E：种子配送员会被别的会话「抢走」

并行会话跑《智能调度》的自动调度/超时升级时会**扫描全库待分配任务**，把本页种子的配送员占成「并接上限」或（在线口径生效后）打成「离线」。
因此模块级 E2E 里**凡是断言「指派成功」的用例都不能写死配送员**，应从
`GET /api/dms/dispatch/candidates?taskId=xxx` 里**动态挑第一个 eligible**（优先自己的种子，便于断言身份）；
另外写接口在共享库下常需 3~6s、全局自动调度可达分钟级，断言要用**轮询**而不是固定 sleep。

### 8.3 配送员「忙碌」是单向门：任务终态必须释放运力（2026-09-14 已修）

**规则**：`dms_rider.status`（0 离线 / 1 空闲 / 2 忙碌 / 3 休息）里的「忙碌」是**占用**语义，
凡任务进入终态（已完成 / 已取消）或改派换人，**必须**调用 `TaskService.releaseRiderIfIdle(riderId)`：
「该配送员名下**再无在途任务（1/2/3/4）**」时才回置「空闲(1)」，休息中的不覆盖。

**Why**：自动派单候选池默认（`dms.dispatch.require.online=true`）只取「空闲(1)」，
而原先**只有改派/取消**会回置空闲 —— **完成**（签收审核通过 → 已完成）从不回置：

- 配送员跑完第一单就永久停在「忙碌(2)」→ 再也不会被自动派单选中 → **运力池随完成单量单调收缩**，
  最终全库「无人可派」（已完成的单越多，能派的人越少）；
- 与「单人在途上限 `dms.dispatch.max.concurrent`=5」「负载均衡策略」的配置契约直接矛盾。

写入点（单一出口，勿各页自造）：`SignService.audit`（审核通过 → 已完成）、
`ChannelOrderService.applyTaskStatus`（渠道回传已完成/已取消）、`TaskService.cancel`、
`DispatchService.reassign` / `escalateOverdue`（后者改为**改派完成后**再释放，
否则计数里还含本单会把自己挡住）。

**How to apply**：
- 完成/取消类新入口一律只调 `releaseRiderIfIdle`，不要直接 `set status=IDLE`；
- 改派要把释放放到**任务归属切换之后**，重新派回原人时不会误释放（在途含本单）；
- 断言口径：`status=1` 且心跳在 `dms.tracking.online.minutes` 内 = 已回到自动派单候选池。

### 8.4 定时任务（开发工具）：一整条链路曾是「未接线」的死功能（2026-09-14 已修）

排查「配送超时升级要不要定时化」时，把这条链路挖到底 —— 四个缺陷叠在一起，
现象都是「**页面能用、功能静默无效**」：

1. **包未装配（根因）**：`AiReadyApplication.scanBasePackages` 是**白名单**，漏了 `cn.aiedge.scheduler`
   → 控制器/执行器/Mapper 从未注册：接口 404（前端 `fetchData` 吞异常 → 页面显示空列表）、
   `TaskExecutor` 不存在 → **执行日志 0 条**、`scheduled_task` 里的任务根本没人调度。
2. **执行目标无法装配**：执行器用 `Class.forName(executeClass).getDeclaredConstructor().newInstance()`
   + `getMethod(name, String.class)` 反射调用 → 只能跑「无参构造 + 单 String 参数」的普通类，
   **注入不了任何 Spring Bean**；且类名/方法名来自请求体 → 任意登录用户可下发（越权）。
   现改为 **`job_key` 白名单处理器**（`JobHandler` SPI 放 core-base，业务模块各自实现，core-api 统一收集）。
3. **同一功能两套实现**：还有一套 Quartz + `sys_scheduled_task` 的调度栈（`JobSchedulerService` 等），
   **依赖的内部 `ScheduledTaskRepository` 没有实现** → 一旦被扫描就启动失败；零数据零引用 → 已删除。
4. **前端页面字段全错**：弹窗改的是 `taskClass`/`taskGroup`/`description`/`status:1`，
   与实体（`jobKey`/`cronExpression`/`executeParams`/`taskDesc`/`enabled`/`status:'RUNNING'`）**完全对不上**。

**踩到的两个连带坑**：
- **平台级表被多租户过滤**：`scheduled_task` 带 `tenant_id`，迁移/种子写入的行是 `tenant_id=0`，
  而会话租户是 1 → 台账查不到、`selectById` 返回 null（表现为「列表空白 / 执行报『参数[id]格式不正确』」）。
  已在 `MyBatisPlusConfig.IGNORE_TENANT_TABLES` 登记 `scheduled_task` / `scheduled_task_log`。
- **a-select 带搜索框会打乱 `input` 顺序**：E2E 里按 `modal.locator('input').nth(n)` 取表单项必错位
  （会把 Cron 填进下拉的搜索框 → 校验提示「请输入Cron表达式」而实际是没选到处理器）。
  **按 `a-form-item` 标签定位**（`item('Cron表达式').locator('input').first()`）才稳。

并行会话会往同一租户写记录，**不带范围的计数断言必然假失败**（实证：《签收管理》E2E 的
`signTypes=1` / `hasPhoto=false` 把《调度任务》用例产生的签收记录算了进去 → 期望 3 实得 5）。

- 断言一律加作用域（本页/本套种子，如 `keyword=E2ESIGN`）；
- 用例自己产生的**跨模块可见**数据（如为驱动「任务→已完成」而写的签收记录）应在收尾时清理，
  别指望别人下次跑之前会重置；
- 需要「名下恰好 N 单在途」这类精确场景时，挑一个**别的会话不可能派单给它的**配送员
  （如 `verify_status=0` 未实名 → 候选恒为不可派，在途数不会被抢占污染）。
