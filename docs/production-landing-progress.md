# 生产级落地攻坚 — 进度文档

> 最后更新：2026-07-22（B 阶段完成）
> 重启会话后请先读本文件，再从"下一步行动"继续。

## 0. 环境与操作要点

- **psql**：`"/c/Program Files/PostgreSQL/18/bin/psql.exe"`；`localhost:5432 devdb`，用户 `devuser`，**密码 `devuser123`**；`export PGPASSWORD='devuser123'`
- **登录调接口**：`GET /api/auth/captcha` 拿 SVG（正则 `>([^<]{1,2})</text>` 拼字符）→ `POST /api/auth/login` `{username:admin,password:admin123,tenantName:系统租户,captcha,captchaKey}`（中文必须写 UTF-8 文件 `--data-binary @file`）→ 头 `Authorization: Bearer <token>`
- **跑后端**：`cd I:/AI-Ready/backend && java -Dfile.encoding=UTF-8 -jar core/api/core-api/target/core-api-0.3.7-exec.jar --spring.profiles.active=dev`（5655）
  - 多模块改动后必须 `mvn -o -q -Dmaven.test.skip=true -pl <改动模块>,core/api/core-api install` 全部重装再重启（spring-boot:run 只编译 core-api 且模块多了会触发 Windows 命令行过长 error=206，一律用 exec.jar）
  - 杀端口：`netstat -ano | grep :5655` 找 PID → `powershell -c "Stop-Process -Id <PID> -Force"`
- **前端类型检查**：`cd frontend/apps/pc-admin && ./node_modules/.bin/vue-tsc --noEmit`（**当前全仓 0 错误，保持不破**）
- **Flyway**：迁移目录唯一 `core/api/core-api/src/main/resources/db/migration`；dev 有 out-of-order+repair-on-migrate；新迁移交给启动自动应用即可（等价手动执行+标记）

## 1. 已完成总览

### A 阶段：后端接线与地基（全部完成）
- **10 个域接线**：quality/trade/erp.metrics/erp.invoice/erp.budget/hr/dms/erp.marketing/core-payment/core-platform（scanBasePackages+pom），全部端点实测 200
- **排障**：HR Mapper 移到 cn.aiedge.hr.mapper；DmsApplication 删自带 @MapperScan；PermissionAuditAspect 切点修通配；删 4 个 Stub/冗余 Controller（Budget/FixedAsset/Dms/PermissionControllerExt）
- **Schema 修复**（Flyway V9.10.2-3 重排、V11.1 残表重建(并行会话)、V11.2 索引、V11.3 invoice_payment_record 拆表、V11.4 枚举大小写、V11.5 erp_metric_def 定义/值分表）
- **metrics 生产化**：计算 SQL 全部改真实表、删 15 处造假默认值、NOT_SUPPORTED 只读隔离修 25P02、dashboard 图表改真实 SQL
- **测试**：全 reactor 测试编译绿；erp-purchase 103 测试通过；修了 PurchaseQuoteController.submitQuote 强转 bug

### B 阶段：前端骨架页全量真实化（~150 页，vue-tsc 0 错误）
- **基建**：`ARReportPage`（查询区+表格+分页+导出）、`ARStatCards`、`ARReportChart`（echarts）三组件；`src/api/analytics.ts` 等域封装
- **B1 分析域 33 页**：全部真实对接；新建 12 个后端报表端点（docquery 综合单据×3、stock 进销存/流水/采购准备×3、finance 往来余额/回款统计×2、sale 客户活跃/促销分析×2、mall 交易分析、marketing 员工提成汇总）
- **B2 十一域**：wh 9（WMS 作业单）、dms 10、crm 10+拜访3（降级）、finance 17（账簿报表）、purchase 9、md 7、admin 8、set 3、占位表单 4（采购入库/费用分摊/其他出库/BOM）、marketing 17、mall 13
- **类型清零**：vue-tsc 109→0；顺手修了 useColumnConfig 拖拽签名（隐藏运行时 bug）
- 原则遵守：全部真实端点对接（先 grep 后端核实）；真缺后端的降级为只读视图+a-alert 标注，无 mock

## 2. 后端缺口清单（C 阶段输入，按域汇总）

**真缺失（需建后端）**：
1. 会计期间管理 + 月结执行/状态端点（finance/month-closing 已降级只读）
2. WMS 明细保存端点（8 个作业单表单明细只读；明细现仅 ERP 推送/PDA 产生）；借进借出整套 CRUD（borrow-in/out 降级为库存流水）
3. 秒杀场次、预售、弹窗广告、加价购规则、热搜词管理写端点、短信群发/记录、积分兑换流水（marketing 6+ 页降级）
4. POS 收银后端（trade/pos 现复用商城接口的极简页）；商城公告 notice、关键词库 keyword、装修设计师存取（mall 3 页降级）
5. 外勤拜访全套（visit-plan/exec/review 降级为跟进记录视图）
6. 促销效果归因需 erp_sale_order 加 promotion_id 或活动-订单关联表
7. 慢查询统计、备份策略/下载、同步启停与日志、清理日志、codegen 模板（admin 域部分降级）
8. 支付方式/渠道主数据 CRUD、岗位-权限关联、总账/明细账 Controller（LedgerService 已有无 REST）、在线支付对账
9. 商城订单状态口径雷：MallOrderServiceImpl 把取消写成 erp_sale_order.status=5（与 erp-sales 的 5=交易完成冲突）
10. 流程设计器前端页（菜单 801/80610 现指向流程监控页）

**双轨/基建债（后续清理）**：
- 工作流两套（core-base /workflow/** vs core-api /api/workflow/**）；财务两套（core-api cn.aiedge.finance vs erp-finance）；前端 saleOrderApi 两套（@/api/erp vs @/api/sale-order.ts）
- 响应包装不统一：expense/budget 的 ApiResponse.code 是字符串 "200"，receipt/payment 统计等裸 Map，都会被前端拦截器误判（建议统一 base.vo.Result 或拦截器兼容）
- 租户头不一致：全局拦截器发 tenantId，core-api 部分控制器读 X-Tenant-Id
- 3 张表缺 tenant_id 列（erp_purchase_order_item、erp_stock_damage_item、erp_stock_overflow_item、biz_party_follow 4 张）会触发 TenantLine 注入报错，目前靠 @InterceptorIgnore 规避，建议加 IGNORE_TENANT_TABLES 或补列
- 根目录 db/migration 3 个孤儿脚本；mall_order 0 行遗留表
- 采购入库不回写 erp_stock（PurchaseInboundServiceImpl 无 StockService 调用）——业务链路缺陷，影响库存准确性

## 3. 下一步行动（按序）

1. **C1 后端真缺失补齐**（按缺口清单分组派工，每组：实体+表(Flyway 新迁移)+Controller/Service+端点验证）：
   - 会计期间+月结（erp-finance，注意 voucher 已有 period 字段）
   - WMS 明细保存 + 借进借出（wms）
   - 秒杀场次（erp-marketing）+ 公告/关键词（erp-mall）
   - POS 收银后端（交易闭环：扫码商品→购物车→结算→零售单/支付）
   - 外勤拜访（crm 新建 visit 系列）
2. **C2 前端补齐**：流程设计器（画布已有 src/components/Workflow/Canvas 基础）；各降级页在后端就绪后转正
3. **C3 双轨清理**：工作流/财务/saleOrderApi 合并选型；孤儿 SQL 归位；tenant_id 缺列根治
4. 全程保持 vue-tsc 0 错误、mvn 主代码编译绿、新 SQL 走 Flyway 迁移

## 4. 关键文件索引

- 菜单树：`tool-results/menu-analysis/menu_tree.txt`
- 启动类（扫描白名单）：`backend/core/api/core-api/src/main/java/cn/aiedge/AiReadyApplication.java`
- 前端报表组件：`frontend/apps/pc-admin/src/components/ARReportPage|ARStatCards|ARReportChart`
- 分析域 API：`frontend/apps/pc-admin/src/api/analytics.ts`
- 新后端端点：erp-stock `StockReportController`、erp-finance `PartnerBalanceController/CollectionStatsController`、erp-sales `SaleAnalysisController`、erp-mall `MallTradeAnalysisController`、erp-marketing `CommissionStaffSummaryController`、core-api `docquery/DocQueryController`

---

## 5. C1 阶段增量（2026-07-22 第二批，全部完成并验证）

**后端真缺失补齐（6 组迁移 V11.6-V11.11 已应用）**：
- V11.6.0 会计期间+月结：`fin_accounting_period`(预置24期间)+`fin_month_closing_log`；端点 /api/erp/finance/period/*、/month-closing/execute(真实未过账凭证校验)/reopen/status/logs
- V11.7.0 借进借出：`wms_borrow_order(+item/return/return_item)`；/api/wms/borrow 全套（审批库存增减、归还回冲，复用 InventoryService 悲观锁）；另修复 InventoryServiceImpl 的 locationId=null 匹配 bug；6 个 WMS 作业单补 detail/save 端点
- V11.8.0 外勤拜访：`crm_visit_plan`+`crm_visit_record` 重建（旧 3 张空表废弃）；/api/crm/visit/plan|record|review|stats
- V11.9-11.11.0 秒杀场次/商城公告/关键词：`mkt_flash_sale(+order)`、`mall_notice`、`mall_keyword`（含示例数据）；/api/erp/marketing/flash-sale、/api/erp/mall/admin/notice|keyword

**前端转正（18 页）**：visit-plan/exec/review 3（完整拜访闭环）、mall-flash/notice-config/keyword-bank 3（完整 CRUD）、wh borrow 3（借进借出全流程+归还弹窗）、wh 作业单 6（明细可编辑+detail/save）、month-closing（月结检查/执行/反月结/日志）、set/accounting-period（期间管理）
**验证**：vue-tsc 全仓 0 错误；8 组新端点 curl 全 200

## 6. 剩余待办（C2/C3，按优先级）

1. **POS 收银后端**（trade/pos 极简页 → 生产级：扫码/结算/支付/交班，接零售单+core-payment）
2. **流程设计器页面**（菜单 801/80610 现指向流程监控页；src/components/Workflow/Canvas 有基础）
3. **业务链路缺陷**：采购入库不回写 erp_stock（PurchaseInboundServiceImpl 无库存调用，影响库存准确性，优先级高）
4. **促销归因**：erp_sale_order 加 promotion_id/活动-订单关联
5. **降级页剩余缺口**：慢查询统计/备份策略与下载/同步启停日志/清理日志/codegen（admin）；支付方式与渠道主数据 CRUD（md）；总账/明细账 Controller（LedgerService 无 REST）；在线支付对账
6. **双轨/基建债**：工作流两套、财务两套、前端 saleOrderApi 两套、响应包装不统一（expense/budget 字符串 code、裸 Map）、租户头不一致（tenantId vs X-Tenant-Id）、4 张表缺 tenant_id 列（erp_purchase_order_item/erp_stock_damage_item/erp_stock_overflow_item/biz_party_follow）、根目录 3 个孤儿 SQL、mall_order 遗留表、商城取消订单 status=5 口径雷
7. **小修**：execute/reopen 操作人落 mock_operator（header 未传 userId）；月结权限项 finance:month-closing:* 未配菜单权限

---

## 7. C2 阶段增量（2026-07-22 第三批，全部 E2E 验证）

**采购入库回写库存修复（业务链路硬伤）**：
- `PurchaseInboundServiceImpl.updateStock` 原为只打日志的空实现 → 注入 erp-stock StockService 真实回写（confirmWarehouse 时 +库存）；cancel() 对 WAREHOUSE_CONFIRMED 状态加 reverseStock 回冲；erp-purchase pom 加 erp-stock 依赖
- 连带补齐缺列（V11.12.0）：erp_stock.unit_price、erp_purchase_inbound 19 个流程/物流列
- **E2E 验证通过**：create→submit→approve→receive→receiveItem→quality-check→warehouse-confirm（库存 0→10）→cancel（库存 10→0）

**POS 收银闭环**：
- settle 原来不扣库存 → RetailOrderServiceImpl.settle 注入 StockService.decreaseStock（事务内，失败回滚）
- saveWithItems 原来不生成单号 → 新增 generateRetailNo（LS+yyyyMMdd+4位流水）
- 交班管理新建（V11.13.0 erp_retail_shift）：/api/sales/retail/shift open/close/current/page/{id}，交班自动汇总班次时段内已结算单（单数/总额/分支付方式/应收现金/长短款）
- 前端 POS 收银台完整重写（880行+5个子组件：扫码即查即加/购物车/多支付结算/找零/挂单取单/开班交班/小票预览，F1/F2/F9 快捷键）
- **E2E 验证通过**：开班→零售单 LS202607220001→结算（库存 10→7，SETTLED）→再开班次→结算 51→交班（orderCount=1, cash=51, expected=151, difference=0）

**流程设计器**：`views/workflow/designer/index.vue`（912行，画布可视化+节点属性抽屉+版本化保存）；菜单 801/80610 已 UPDATE 指向设计器（fix_workflow_designer_menu.sql 已执行）

**Schema 补丁（V11.12-V11.15）**：erp_stock.unit_price、erp_purchase_inbound 19 列、wms_borrow 3 表 version、wms_inventory_log 审计列+version、wms 6 张明细/结果表 version

## 8. 新发现的重要架构债（C3 输入，优先级从高到低）

1. **双库存轨（最严重）**：`erp_stock`（ERP 域：销售/采购/POS 出入库）vs `wms_inventory`+`wms_inventory_log`（WMS 域：作业/借进借出）。借进审批 +20 只写 wms_inventory，erp_stock 不动 → 同一商品两本账。需要统一或建同步机制（建议方向：WMS 作业回写时同步 erp_stock，或长期合并为单库存模型）
2. **双工作流引擎**：core-base /workflow/**（引擎完整但前端不可达）vs core-api /api/workflow/**（在线但定义存内存缓存重启丢失、仅 list/detail/create 端点、审批人/审批模式口径与前端不一致）。需要选型合并+落库持久化
3. **财务双轨**：core-api cn.aiedge.finance vs erp-finance（应收应付/报表各两套）
4. **前端 saleOrderApi 两套**：@/api/erp vs @/api/sale-order.ts
5. **响应包装不统一**：expense/budget ApiResponse.code 是字符串"200"、多处裸 Map/裸实体，前端拦截器误判（各域已用原生 axios 自解包规避，建议后端统一 base.vo.Result 或加 ResponseBodyAdvice）
6. **租户头不一致**：前端拦截器发 tenantId，core-api 部分控制器读 X-Tenant-Id（admin 域租户隔离实际未生效）
7. 4 张表缺 tenant_id 列（erp_purchase_order_item、erp_stock_damage_item、erp_stock_overflow_item、biz_party_follow），现靠 @InterceptorIgnore 规避
8. 月结操作人落 mock_operator（execute/reopen 从 header 取 userId，前端未传）

## 9. 当前系统状态

- 后端 5655 运行最新构建（0.3.7），Flyway 已应用到 V11.13.0（V11.14/V11.15 已手工执行+文件就位，下次启动自动登记）
- 前端 vue-tsc 全仓 0 错误；后端 mvn 主代码编译绿；erp-purchase 103 测试通过
- 全部 252 个菜单页面：~150 个本轮真实化 + 原有的完整页，骨架页基本清零（除少数后端真缺的降级页已标注）

---

## 10. C3 阶段增量（2026-07-22 第四批）

**双库存轨治理（E2E 验证）**：WMS InventoryServiceImpl.increase/decrease 同事务镜像增量到 erp_stock（increase 失败回滚两轨；decrease 遇历史漂移记 error 不阻断作业）；wms pom 加 erp-stock 依赖。实测：借进审批 +5 → wms_inventory 20→25 且 erp_stock 5→10。存量漂移 1 行（借进首测 +20 只写 WMS 轨）留作对账线索。

**工作流引擎合并 Step 1-3（保留 core-api 套）**：
- 持久层整体下沉 PostgreSQL（新建 4 个 entity/mapper + WorkflowConverter + WorkflowSeedRunner 启动 seed 3 内置流程），CacheService 依赖完全移除，无 Redis 全功能可用
- 状态机接线完成（approve/reject/cancel/withdraw/intervene 全走 StateTransitionManager.transition）
- 补齐前端在调的 9 个 404 端点（instance/detail、task/detail|approve|transfer、analysis/refresh|report、definitions PUT/publish/disable/DELETE）
- userId 加 StpUtil 兜底（前端不发 X-User-Id 头导致待办恒空的问题修复）
- core-base 侧唯一改动：MyBatisPlusConfig 忽略表清单 +4 张工作流表（规避"无会话线程注入 tenant_id=null 永不匹配"的平台既有问题）
- E2E 全链路验证：create→start→待办→审批→完成→转办→挂起/恢复/终止→版本化更新→启停→删除保护

**C3.3 基建清理**：
- V11.16.0：4 张表补 tenant_id（erp_purchase_order_item/erp_stock_damage_item/erp_stock_overflow_item/biz_party_follow），报表 Mapper 的 4 处 @InterceptorIgnore 已移除
- 孤儿 SQL 归档（db/migration/archive/）：3 个脚本判定（2 个 MySQL 语法早被覆盖、HR 建表与 V8.9.0 重复）；差额收编 V11.17.0（erp_sale_order_item 补 47 列！实体引用全部缺失列，销售订单操作曾有隐患）+ V11.18.0（item_code/pricing_unit）
- V11.19.0：sys_user 补 version（/api/v2/user/{id} 从 500 恢复 200）
- Redis 序列化器注册 JavaTimeModule + 保留多态类型（RedisConfig + CacheConfig 两处），修 User 缓存 LocalDateTime 序列化异常
- 前端 request.ts 同时发送 tenantId 与 X-Tenant-Id 两种头
- 版本号升到 0.3.8（全 reactor 已重装）

## 11. 剩余待办

1. **工作流 Step 4-6**：core-base 套退役（cn.aiedge.base.workflow 整包 + controller + service + mapper，已 grep 包外无引用）；V9.1.0 死列清理（form_data 除外已启用）；approverType=role 已实现（sys_role.role_code→sys_user_role），leader/dept_leader 解析待 sys_dept.leader 语义明确；业务单据（采购/销售订单）接引擎走门面+回调影子模式（方案见调研报告）
2. **响应包装统一**：建议后端加 ResponseBodyAdvice 或统一 base.vo.Result，消除前端各域原生 axios 自解包的临时方案
3. **双 SaleOutboundServiceImpl**（cn.aiedge.erp.sale.outbound vs cn.aiedge.erp.sale.service）+ 双财务轨 + 双 saleOrderApi（前端）
4. **无会话线程租户注入 tenant_id=null** 的平台级问题（影响 Runner/调度器，工作流表已规避，其他表待治理）
5. 存量漂移对账 1 行；商城取消订单 status=5 口径；月结操作人 header；retail 退货库存回补；促销归因 promotion_id

---

## 12. C3.4 阶段增量（2026-07-22 第五批，全部验证）

**core-base 工作流套物理退役**：31 个文件删除（内存引擎整包/Controller/Service/4 mapper+XML/4 entity/2 测试类），删除前全量 grep 无包外引用；启动验证通过（首次因 target/classes 残留 WorkflowNodeMapper.xml 启动失败，clean 重建后正常——target 残留是删资源文件后必须 clean 的老坑）。

**V11.20.0 死列清理**：workflow_definition/instance/task 三表 V9.1.0 第二套死列全 drop（form_data 保留，已启用存业务数据）。

**leader/dept_leader 审批人解析**：实现（sys_user.dept_id 找发起者部门→sys_dept.leader 按 数字ID→username→real_name 解析→parent 链上溯；全空走占位任务）；SysUser 实体补 realName 字段。

**单据接引擎（门面+影子模式，E2E 验证）**：
- core-base 新增 `cn.aiedge.base.workflow.facade`（ApprovalFacade/ApprovalCallback SPI）；core-api 实现 WorkflowApprovalFacadeImpl
- 采购/销售订单 submitForApproval 后自动 startShadowApproval（businessData 含单号/金额/供应商/客户供 SpEL；instanceId 记审核流水子表；异常不阻断）
- 实测：采购单提交 → audit_trail 记 `影子工作流实例已发起: instanceId=...` → workflow_instance(business_type=purchase_order, current=部门经理审批) + 占位任务(角色:manager，devdb 无该角色属预期)

**ApiResponse 同包双类合并（危险隐患排除）**：
- cn.aiedge.common.result.ApiResponse 曾有 core-api(A)/core-base(B) 两个实现，A 遮蔽 B 导致 erp 模块 NoSuchMethodError（采购订单创建 500 实测）
- B 充实（Serializable+success 字段+ok()/okWithId/error×3），A 删除，core-api 4 处 data-first 调用换序 + 1 处 success(null) 强转
- 全 backend 扫描确认唯一重复 FQCN 已消除（其余同名 ApiResponse 均不同包不冲突）
- 全量 0.3.8 构建绿，采购订单创建/提交恢复 200

**Schema 补丁**：V11.21.0（erp_purchase_order settled_amount/expected_receive_time）、V11.22.0（采购 6 子表 tenant_id+回填+索引）

**进程治理**：清理了 10+ 个陈旧后端实例（旧 jar/spring-boot:run 残留），现仅 5655 单实例运行 0.3.8

## 13. 当前状态与剩余

- 后端 5655 运行 0.3.8（Flyway V11.22.0），前端 vue-tsc 0 错误
- 剩余（C4+）：响应包装深度统一（expense/budget 字符串 code、裸 Map 端点，建议 ResponseBodyAdvice 收口）；双 SaleOutboundServiceImpl 合并；财务双轨（core-api cn.aiedge.finance vs erp-finance）；前端双 saleOrderApi；工作流第二期（ApprovalCallback 回写单据 status、在途实例防重、REQUIRES_NEW）；存量漂移对账 1 行；无会话线程租户注入 null 治理

---

## 14. C4 阶段增量（2026-07-22 第六批，全部验证）

**响应包装治理（第一步）**：前端 request.ts 拦截器成功判定扩展为 `code===200 || code==='200' || success===true`，Map 型响应（无 data 字段）整体返回——一处修改消除 expense/budget 字符串 code、admin/datax Map 响应的"误判失败"问题（各域原生 axios 自解包可逐步退役，后端深度统一留作后续）。

**工作流二期：审批回调回写单据（E2E 闭环验证）**：
- 引擎 approve/reject 达终态 → 发布 ApprovalCompletedEvent → @TransactionalEventListener(AFTER_COMMIT, fallbackExecution=true) + 专用线程池异步分发 → ApprovalCallback 广播
- 回调签名扩展 operator（onApproved/onRejected 带 operatorId/operatorName，core-base facade 同步）
- 防重：startApproval 前取消在途旧实例（驳回重提不会叠影子实例）；防循环：单据 approve/reject 不经 submitForApproval 路径互斥；幂等：双轨操作后执行方抛"不在待审批状态"回调捕获忽略
- **回调线程租户上下文根因修复**：ApprovalCompletedEvent 带 tenantId，分发器 setTempTenantId/clearTempTenantId——首次回调失败（"订单不存在"）的根因是平台级问题：无会话线程 TenantLineInnerInterceptor 注入字面量 tenant_id=null（永不匹配）。此问题影响所有 Runner/调度器/异步线程，其他场景待治理
- **E2E 实测**：采购单提交→影子实例→待办→工作流审批通过→采购单 status 1→2，audit_trail 记"审批通过"，回调日志 operator=超级管理员 ✓

**双 SaleOutboundServiceImpl 合并**：活套（sale.outbound，1623行全功能）保留；死套（sale 根包，14行空壳、映射不存在的 t_sale_outbound 表、还把 /api/sales/doc-query/page 的 OUTBOUND 分支拖成必 500）删除 4 类，doc-query 三处引用重定向到活套（顺带修复该 500）。

**前端双 saleOrderApi 合并**：以 erp.ts 为正源（方法 31 个最全、id:number 风格），迁入被弃套独有方法（productSummary/batchLogisticsRemark/salesPriceTrackApi），sale-order.ts 删除，vue-tsc 全仓 0 错误。

## 15. 遗留（下轮候选）

1. **平台级租户注入 null 治理**：TenantLineInnerInterceptor 在无会话线程注入字面量 tenant_id=null（永不匹配），影响全部调度器/Runner/异步线程的数据查询——建议改为可跳过或使用默认租户或逐一设置临时上下文（本轮仅修了回调链路）
2. 响应包装深度统一：后端 ResponseBodyAdvice 收口 + expense/budget 字符串 code 改数字 + 前端各域 axios 自解包退役
3. 财务双轨（core-api cn.aiedge.finance vs erp-finance）合并选型
4. 工作流三期：回写失败补偿/重试、intervene(terminate) 联动、单据端点改为触发引擎审批（影子→正式）、存量影子实例清理
5. DatabaseInitializer 建表列不全（erp_sale_outbound 30列 vs 实际110列）全新环境问题
6. 存量漂移对账 1 行；mall 取消订单 status=5 口径；retail 退货库存回补；月结操作人 header

---

## 16. C5 阶段增量（2026-07-22 第七批，全部验证）

**平台级租户注入 null 根治**：
- 根因：原生 TenantLineInnerInterceptor 在 getTenantId() 返回 null 时注入字面量 `tenant_id = null`（PG 中永不匹配），无会话线程（调度器/Runner/@Async）查租户表全部静默返回空
- 修复：新增 `AiReadyTenantLineInnerInterceptor`（core-base config），租户不可解析时整体跳过租户处理；MyBatisPlusConfig 换用
- 验证：启动日志 null 注入从 109 处降为 **0**；会话线程租户过滤正常（definitions/inv-summary 实测）

**财务双轨合并（保留 erp-finance 为唯一财务轨）**：
- 搬迁：reconciliation（对账）、other-income-doc（其他收入单）18 个文件迁入 erp-finance（路径 `/api/erp/finance/reconciliation|other-income-doc`，权限注解改 @PreAuthorize 模式，表名不变）——三个端点实测 200
- 删除：core-api `cn.aiedge.finance` 整包 112 文件（含 mock 资产负债表、空壳 profit/cost、简化应收应付）+ scanBasePackages 移除
- V11.23.0：drop 7 张空死表（fin_receivable/payable/payment/receipt + profit/cost 三壳表，逐表确认 0 行）
- 前端：reconciliationApi/other-income-doc 路径适配；payment-doc/receipt-doc 死链页改接 `/erp/payment|/erp/receipt`（字段 paymentAmount/receiptAmount + 整数状态机对齐）；删除 accounts-receivable/accounts-payable 旧目录（含路由键清理）+ receivable.ts/accounting.ts 死 API + 3 个 @deprecated 封装；vue-tsc 全仓 0 错误

## 17. 累计成果总账（截至本批）

- 后端：10 域接线、20+ 新后端端点、Schema 迁移 V9.10.2→V11.23.0（23 个）、核心链路修复（采购入库回写/POS扣库存/单号）、工作流引擎统一（持久化+状态机+回调驱动单据）、双库存镜像、财务单轨、ApiResponse 唯一化、租户注入根治
- 前端：~150 骨架页真实化、vue-tsc 109→0 且持续保持、报表组件三件套、双 saleOrderApi 合并、财务旧页清理
- 验证方式：全部关键改动经 5655 实例 curl E2E（登录→业务操作→DB 核对）

## 18. 剩余待办（递减清单）

1. 响应包装后端深度统一（ResponseBodyAdvice 收口 + expense/budget code 字符串改数字；前端容错已生效，优先级低）
2. 工作流三期：回写失败补偿/重试、intervene(terminate) 联动、影子转正式（单据端点改触发引擎）
3. DatabaseInitializer 残留死逻辑清理（fin_payable 加列段）+ 建表列不全（erp_sale_outbound 30 vs 110）
4. 业财集成 HTTP 自调改 Bean 直调；打印种子 pageCodes 更新
5. 存量漂移对账 1 行；mall 取消单 status=5 口径；retail 退货回补；月结操作人 header；dept 全体成员审批类型
6. 权限菜单种子：finance:reconciliation:* / finance:other-income-doc:* / finance:month-closing:* 配置

---

## 19. C6 阶段增量（2026-07-22 第八批，全部验证）

**DatabaseInitializer 清理与建表补全**：
- 删除 fin_payable 死逻辑段（:1082-1164，表已 drop）
- 新增 3 个 safeAddColumn 兜底方法：erp_sale_outbound 94 列、erp_sale_order 109 列、erp_stock 4 列——启动后 devdb 实表已对齐实体（outbound 123 列/sale_order 143 列/stock 29 列），全新环境首启不再缺列

**权限菜单种子（V11.24.0，已执行）**：
- sys_permission 16 行（finance:reconciliation×5 / other-income-doc×5 / month-closing×3(view/execute/reopen) / period×3(view/create/update)）
- sys_menu 补「对账」菜单（80122，挂账务处理下）；角色分配需在权限界面操作（超管不受影响）

**字符串 code 统一**：expense/budget 两处模块级 ApiResponse 的 code 由 String "200" 改 int 200（JSON 契约不变，使用点仅本模块 11 个 Controller 全部覆盖），实测返回数字 code

**业财集成 HTTP 自调 → Bean 直调**：
- 5 个调用方（sales/purchase/fixed-asset 的 *AccountingService + erp-finance 内 expense/payment 两个）重写为注入 BusinessAccountingService 直调；callFinanceApi/RestTemplate/FINANCE_BASE_URL 全部删除
- 事务语义：保持"记账独立提交"——网关三方法 REQUIRES_NEW（调用方全部 catch-and-continue，并入外层事务反而会 UnexpectedRollbackException 破坏主流程）；PaymentAccountingService 核销用 TransactionTemplate(REQUIRES_NEW)
- erp-purchase/erp-fixed-asset pom 加 erp-finance 依赖；HTTP 端点保留（外部系统可能用）

**expense 表重建（V11.25.0）**：6 张手工残表（expense_application 连 id 主键都没有）按 JPA 实体 DROP+重建（52/20/23/39/18/22 列，实体-表终态零差异确认），4 个 expense 端点实测 200

**技术决策记录**：响应包装**不做**全局 ResponseBodyAdvice 收口——2731 端点包装形态混杂（Result/ApiResponse/裸Page/裸Map/裸实体/二进制），全局切面双包装与二进制穿透风险大于收益；前端拦截器三态容错（C4）已统一消费侧，新端点沿用 base.vo.Result 多数派，遗留裸端点随触随改

## 20. 剩余待办（最终递减清单）

1. 工作流三期：回写失败补偿/重试队列、intervene(terminate) 联动、影子转正式（单据 approve/reject 端点改触发引擎审批）
2. SalesAccountingService/PurchaseAccountingService/FixedAssetAccountingService 三个直调服务当前无调用方（死代码），需确认业务触发点（发货记账/收货记账/折旧记账）接入时机
3. 角色权限分配（新 finance 权限码需在权限界面分配给角色）
4. 小项：存量漂移对账、mall 取消单 status=5、retail 退货回补、月结操作人 header、dept 全体成员审批、erp.finance.api-base-url yml 残留清理、打印种子 pageCodes

---

## 21. C7 工作流三期（2026-07-22 第九批，全部 E2E 验证）

**回调失败补偿（V11.26.0）**：
- `workflow_callback_log` 补偿表（instance/biz/result/operator/status(pending/success/failed/final-failed)/error/retry_count/max_retry=5/next_retry_time）
- 分发前落 pending、失败指数退避（1m/5m/15m/30m/1h）、@Scheduled 每分钟捞单重发（按行内 tenant_id 设临时上下文）、5 次仍败 final-failed
- 端点：`GET /api/workflow/callback-log/page`、`POST /callback-log/{id}/retry`（人工立即重试）

**intervene(terminate) 联动**：终止实例发布 result=terminated 事件，dispatcher 视同驳回回调（单据退回草稿可改后重提，旧实例已终态不触发防重）——E2E：terminate → 采购单 status 2→0 ✓

**影子转正式（核心行为切换）**：
- 采购/销售 approve/reject 端点：有在途引擎实例→走引擎 approveTask 驱动（不再直接 setStatus，回调最终回写）；无实例/引擎不可用→回退原直接逻辑
- 销售 approve 走引擎前先 checkStockAvailability（不冻结）前置校验，回调内 checkAndFreezeStock 为最终权威（冻结失败进补偿重试，补货可自愈）
- 防循环/防重确认：回调到达时实例已终态→findInFlight 返回 null→直接翻转路径不再发实例
- **E2E：单据端点 approve → 引擎驱动 → 回调回写 status=2，callback_log(success, retry=0) ✓**

## 22. 系统当前形态（全战役收敛）

**审批中台**：流程定义(DB持久化+版本化+设计器)→实例(状态机校验)→任务(user/role/leader 解析)→回调(AFTER_COMMIT+租户上下文+补偿重试)→单据回写(影子+正式双模)。采购/销售已全链接入。
**单轨化**：库存(ERP主+WMS镜像)、财务(erp-finance唯一)、工作流(core-api唯一)、ApiResponse(唯一FQCN)、saleOrderApi(前端唯一)。
**数据层**：25 个 Flyway 迁移修复全部 schema-实体错位；租户注入 null 根治；expense/invoice/metrics 等撞表全部拆分。
**遗留小项**（不阻塞）：pending 死记录兜底扫描、auditorName 异步线程记"系统"、batchApprove 未走引擎、三个业财直调服务待业务触发点接入、角色权限界面分配、漂移对账 1 行。

---

## 23. 收尾递减项清零（2026-07-22 第十批，全部验证）

**工作流收尾**：pending 死记录 10 分钟兜底扫描（崩溃自愈）；auditorName 修复（approve 加 operatorName 重载，采购侧补审核流水 operator 字段写入，回调传终审人）；approverType=dept 实现（本部门启用成员，status=0 语义以代码证据为准，不递归子部门防越权）。

**业财触发点接入（业财一体化正式打通，E2E 全链验证）**：
- 销售 complete()→应收+收入凭证、采购 confirmWarehouse()→应付+库存凭证、折旧 batchCalculate→折旧凭证；防重：existsBySource(sourceType,sourceId) + 折旧按期间判重
- 排障四连：①MetaObjectHandler tenantId 按字段类型赋值（String/Long 兼容）②finance 12 表 id 补 IDENTITY（V11.27.0，DO 块幂等）③基础会计科目植入（V11.28.0，14 个一级科目，覆盖集成服务全部编码）④erp-finance BaseEntity.tenantId String→Long 对齐 bigint 列
- **终验：入库确认 → finance_payable 应付 255.00 → finance_voucher 凭证 202607-0001 posted 借贷平衡 ✓**

**其余小项**：商城取消单 status 5→6（含回退映射/管理端/分析 SQL 兼容，历史 0 数据）；retail 退货 increaseStock 回补（abs 口径）；月结操作人改 StpUtil 优先（不再 mock_operator）；打印种子 pageCodes 更新（删 payment-record 模板）；yml 无 api-base-url 残留（零改动确认）；16 个财务权限分配至 SUPER/SYSTEM/DEPT_ADMIN 三角色；库存漂移对账 erp_stock 10→25 与 wms 对齐

**遗留记录**（不阻塞，按优先级）：①销售 complete() 旧 generateAccountingVoucher 与新网关路径对同一出库单会各产生一张收入凭证（旧为 draft 不入总账），建议后续禁用旧路径 ②应收/应付 source 唯一索引（判重非原子）③凭证级判重（部分失败缺口人工补录）④ dept 占位任务需人工转交机制

## 24. 全战役最终状态

- 后端 5655 运行 0.3.8，Flyway V11.28.0（28 个迁移），全量编译绿，erp-purchase 103 测试通过
- 前端 vue-tsc 全仓 0 错误（持续保持），~150 页全真实化
- 业财一体（单据→库存→应收应付→凭证→总账）、审批中台（定义→实例→任务→回调→单据）、单轨化（库存/财务/工作流/ApiResponse/前端API）三大主线全部闭环
