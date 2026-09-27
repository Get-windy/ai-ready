已完成调研。以下是审计报告。

---

# AI-Ready 系统模块（菜单 60013 子树）后端"真接通"审计报告

**审计范围**：`backend/core/api/core-api/.../cn/aiedge/{module,monitor,platform,datasource,devtool,export,tenant,permission,audit,scheduler,cache,dict,config,storage}` + `core/base/core-base`、`backend/agreement`。
**方法**：逐 Service 实现类通读 + 全仓 grep 反查"谁在调用"（判断是否有采集方/触发方）+ 前端 `api/admin.ts` 端点与后端 `@RequestMapping` 对账。只读，未修改任何文件。

**总体结论（先看这句）**：历史清单里**大部分已被真修**——备份（pg_dump/pg_restore 真进程）、清理规则（真 DELETE）、缓存管理（真 Redis INFO/SCAN）、服务商连通测试（真 TCP/SMTP）、模板管理（真落库 dev_template）、权限模拟（真接进 StpInterfaceImpl）都已核实为真实实现。**仍在的"假"集中在 3 个新发现的断链**：① 导入功能全线不落库却报"成功 N 条"；② 告警规则配好了但**没有任何代码触发**；③ 导出 Excel/CSV 主体为空。另有依赖健康探测硬编码等 5 处半桩。

---

## 一、服务状态 / 健康监控（菜单 monitor/health）

### 1.1 依赖服务探测 —— 硬编码常量【半桩，与历史一致，未修】
`core/api/core-api/src/main/java/cn/aiedge/monitor/controller/HealthMonitorController.java:245-274`

```java
dependencies.put("database", checkDependency("Database", dataSource != null));   // :253 唯一真实探测
dependencies.put("redis", checkDependency("Redis", false));           // :256 「需要实际检查」
dependencies.put("messageQueue", checkDependency("Message Queue", false)); // :259
dependencies.put("externalApi", checkDependency("External API", true));    // :262 「假设可用」
```
Redis / MQ 恒报 `DOWN`（写死 false），外部 API 恒报 `UP`（写死 true）。**故障与正常都无法反映**。注意方向与历史描述略有差异：不是"兜底为正常"，而是三个常量里两个恒 DOWN、一个恒 UP。`checkDependency` 本身只是把 boolean 包成 map（`:374-380`）。

对比：同一功能的另一实现 `core/base/core-base/src/main/java/cn/aiedge/trade/monitor/service/impl/ApiMonitorServiceImpl.java:146-271`（接口监控页在用）的 `deps()` 是**真探测**（DB `SELECT 1`、Redis `PING/PONG`、MQ TCP 建连、地图 Key、渠道近 24h 成败），且未配置时如实返回 `NOT_CONFIGURED`。**结论：同一个系统里存在"真探测"和"假探测"两套依赖健康接口，`/api/monitor/health/dependencies` 是假的那套。**

### 1.2 综合健康状态 components 为空【半桩】
`HealthMonitorController.java:59-63`：`if (health instanceof Health healthDetails) { ... // 简化实现，不获取组件详情; result.put("components", componentStatus); }` —— `healthDetails` 拿到后完全未使用，components 永远是空 map，健康分数由 `status` 字符串反推（`:347-359`）。

### 1.3 网络统计【半桩】
`monitor/controller/InfrastructureMonitorController.java:230-256`：只有 `collectTime` / `note`（"需要 OSHI 等外部库"）/ 非回环网卡计数，无任何吞吐/错误包/连接数指标。

### 1.4 真实的部分【真实】
- `SystemMonitorServiceImpl.java`：CPU/内存/JVM/线程/GC/磁盘全部走 `ManagementFactory`/`Runtime` 真采集（`:228-307`）。
- `HealthMonitorController` 的 `/database`（真 `connection.isValid`，`:90-118`）、`/jvm`、`/disk`、`/ready`、`/live` 真实。
- `InfrastructureMonitorController` 的 server/cpu/disk/network-interfaces/process/environment 真实。
- `AlertRuleServiceImpl.createRule/updateRule/deleteRule/enable/disable`、`AlertManagementController` 规则与历史 CRUD 真落库（`sys_alert_rule` / `sys_alert_history`）。

### 1.5 性能监控 —— 历史数据只在内存【半桩】
`monitor/service/impl/SystemMonitorServiceImpl.java:28-30, 70-74`
```java
private final LinkedList<SystemMetrics> metricsHistory = new LinkedList<>();  // 进程内，重启即丢
public List<SystemMetrics> getHistoryMetrics(int hours) {
    return new ArrayList<>(metricsHistory);   // hours 参数被完全忽略
}
```
`monitor/controller/PerformanceMetricsController.java:31-32` 同样是 `Collections.synchronizedList` 实例字段：`/aggregate`、`/trend`、`/compare`、`/predict` 都只消费这份内存历史，而**历史只在有人访问 `/realtime` 或 `/bottleneck` 时才写入**（`:64`）。冷启动下 `/aggregate` 直接返回 `{"message":"No data available for the specified time window"}`（`:92-96`），`/predict` 需 ≥10 个样本（`:197-201`）。多实例部署时各实例历史互不可见。指标本身采集是真实的，缺陷在"历史"这一个维度。

### 1.6 告警触发链路缺失【半桩 / 实质真桩】
`monitor/service/impl/AlertRuleServiceImpl.java:91` 定义了 `checkAndAlert(metricName, value, tenantId)`（含落 `sys_alert_history` + 通知），但**全仓 grep 无任何调用方**——只有接口声明与实现自身（`AlertRuleService.java:64`、`AlertRuleServiceImpl.java:91`）。`AlertManagementController` 只调用 create/update/delete/get/enable/disable/getEnabledRules（`:41-109, :294`）。
**后果**：告警规则能配、能启用，但**不会因为任何指标越界而触发**，`sys_alert_history` 除手工无来源，`/history` 与 `/statistics` 长期为空。

### 1.7 邮件/短信告警通知是日志【半桩】
`AlertRuleServiceImpl.java:195-209`
```java
case "email": log.info("[EMAIL] Sending alert to {}: {}", rule.getNotifyTargets(), message); break;
case "sms":   log.info("[SMS] Sending alert to {}: {}", rule.getNotifyTargets(), message); break;
case "webhook": sendWebhookNotification(rule, alert); break;   // 真实（有 WebhookService 时）
```
`sendNotification` 调用点前一行注释即"模拟发送通知"（`:130`）。email / sms 只打日志不发信；仅 webhook 分支在 `WebhookService` 存在且规则填了 URL 时真实外发（`:217-255`）。

### 1.8 告警规则列表筛选失效【缺陷】
`AlertManagementController.java:82-92`：`:82-86` 构造了带 `enabled` 条件的 `wrapper` 后**从未使用**；实际数据来自 `alertRuleService.getEnabledRules(null)`，而该方法内部固定 `.eq(AlertRule::getEnabled, true)`（`AlertRuleServiceImpl.java:82-88`）。
**后果**：列表中永远看不到"已禁用"的规则，且 `?enabled=false` 永远返回空。

### 1.9 通知配置与"测试通知"【真桩】
`AlertManagementController.java:33` `private final Map<String, NotificationConfig> notificationConfigs = new ConcurrentHashMap<>();` —— 通知渠道配置**只在内存**，重启丢失，多实例不共享。`:240-254` 的 `/notification/test`：查到配置后只 `log.info`，然后 `return Map.of(..., "message", "测试通知已发送")` —— 未向任何渠道发送，却回执"已发送"。

---

## 二、性能监控页补充 / 日志聚合

### 2.1 `LogAggregationService` 无任何调用方【真桩（死代码）】
`monitor/service/LogAggregationService.java`（全文 205 行，读写 Redis `log:agg:*`）。全仓 grep `LogAggregationService` 除本文件外 **0 命中**：`recordApiRequest` / `recordError` 从未被任何过滤器或切面调用，`getApiStats` / `getRequestTrend` / `getSlowRequests` 也没有控制器暴露。该服务写入的数据源不存在，读取入口也不存在。

---

## 三、缓存管理（菜单 62206）【真实】

`cache/controller/CacheManageController.java` 2026-09-18 已重写：`/status` 取 Redis `INFO`（used_memory / keyspace_hits / expired_keys / connected_clients / evicted_keys，`:71-123`），区域按**键前缀真实聚合**（SCAN 采样，`:85-110`），键列表用 SCAN 不用 KEYS（`:227-239`），`/all`、`/region/{name}`、`/region/{r}/key/{k}` 都是真删（`:146-222`）。Redis 不可用时返回 `code:500` + 明确 message，**不再返回假数据**（`:62-68`）。区域级 memory/hitRate 如实返回 `null`（Redis 不提供该粒度）而非编数（`:105-107`）。
`cache/service/CacheService.java`、`CacheEvictService.java` 均为真实 Redis 操作（`CacheService` 对 `redisTemplate` 做了 `@Autowired(required=false)` + null 判空，属降级不是造假）。
（附注：`cache/stats/CacheStatistics.java`、`cache/config/*`、`cache/policy/*` 存在但未在本次重点范围内逐行核对。）

---

## 四、数据源 / 备份 / 同步 / 清理（菜单 62301-62305）

### 4.1 连接管理【真实】
`datasource/service/impl/DataSourceServiceImpl.java:110-132`：`testConnection` 用 `DriverManager.getConnection` 真连并按 `isValid(5)` 落 `status`；CRUD 落 `sys_data_source`。

### 4.2 慢查询【真桩：无采集方，页面恒空】
`datasource/service/impl/SlowQueryServiceImpl.java:22-33` 只有 `list` / `export` 两个查询。全仓 grep `sys_slow_query`/`SlowQueryMapper`：仅 model、mapper、service、controller 四处，**没有任何 INSERT 写入方**（也无采集定时任务）。**该页永远返回空列表**——查询实现是真的，但根本没有数据来源。

### 4.3 备份管理【真实，历史问题已修】
`datasource/service/impl/BackupServiceImpl.java` + `service/BackupTaskRunner.java` + `support/PgProcessRunner.java`：
- 创建：解析连接（非 PG/不存在直接失败，`:79-84`）→ 校验 pg_dump 可执行（`:87-91`）→ 校验磁盘空间（`:92-94`）→ 幂等闸门（同数据源 pending/running 拒重复，`:96-100`）→ 插台账 `pending` → **异步**执行 `pg_dump --format=custom --no-owner --no-privileges`（`PgProcessRunner.java:171-191`，`ProcessBuilder` 不走 shell，口令走 `PGPASSWORD` 环境变量，独立线程读 stdout 防管道假死，超时 `destroyForcibly`）。
- 状态机 `pending→running→success|failed` 每步落库，异常路径 `catch` 后仍写终态（`BackupTaskRunner.java:57-97, 124-133`）；退出码 0 还要回读文件存在且非空才算成功（`:77-94`）。
- 恢复：`confirm=true` 闸门 + 台账必须 `success` + 文件三重校验后执行 `pg_restore --clean --if-exists`（同步），失败**不谎报**（`BackupServiceImpl.java:156-214`）。
- 删除：真删记录 + 尽力删文件（`:236-250`）。
- 例外：`DataMaintenanceProperties` 里 `pg-bin-dir` 未配置且 PATH 无 pg_dump 时，创建备份会**明确失败**（不是静默假成功）。

### 4.4 同步任务【真实（投递语义），历史问题已修】
`datasource/service/impl/SyncTaskServiceImpl.java:117-189`：先在 sync-engine 启用配置中按租户**唯一匹配**（0 条/多条都是明确失败，绝不猜，`:131-146`）→ 置 `last_run_status=running` 在途标记 → 复用 `SyncConfigService.triggerSync` 真投递 → 回执明确写「已投递 ≠ 已同步，本系统无法回读引擎侧搬运结果」（`:174-183`）。**不再把"执行一次"当成"打开任务开关"**（`:52` 注释所述旧 bug 已除）。`sys_sync_task` 与引擎表无映射列这一事实被如实暴露在错误文案里，属诚实边界而非造假。

### 4.5 清理规则【真实，历史问题已修】
`datasource/service/impl/CleanupRuleServiceImpl.java:104-173` + `support/CleanupExecutor.java:49-118`：`confirm` 闸门 → `CleanupGuard` 四重校验（白名单/标识符正则/存在性/条件列为时间类型）→ 可选 dry-run 只 `count(*)` → 真执行分批 `DELETE ... WHERE ctid IN (SELECT ... LIMIT n)`，带 `maxRowsPerRun` 截断保护，回传预统计/实删/剩余/耗时。执行结论写 `last_run_*`，**不污染启停开关 status**（`:222-232`）。
已登记的口径取舍（非缺陷）：当前库无分区表，故未走 `DETACH PARTITION`；单次执行是同步的（有行数上限，工作量有界）。

---

## 五、导入导出（菜单 62402 模板管理 + 全站导入导出）

### 5.1 `/api/export/excel/export`、`/api/export/csv/export`【真桩】
`export/controller/DataExportController.java:36-63`
```java
response.setContentType("application/vnd.openxmlformats-...sheet");
response.setHeader("Content-Disposition", ... + ".xlsx");
// 导出（实际应用中从数据库查询）
// TODO: 实现真实Excel导出逻辑        ← 方法体到此结束，:48
// TODO: 实现真实CSV导出逻辑          ← :62
```
两个端点只设了响应头，**不查数据、不写字节**。下游 `DataExportServiceImpl.exportExcel/exportCsv/exportExcelBatch`（`:38-161`）本身是真实的 POI/OpenCSV 实现——**但这两个入口没调用它**。

### 5.2 `/api/export/batch/excel`、`/api/export/pdf/export`【真桩（数据侧）】
`export/controller/DataExportControllerExt.java:110-114`
```java
private List<?> fetchData(String dataType, Map<String, Object> filters, int page, int size) {
    // 实际应用中应调用对应的Service进行分页查询
    // 这里返回空列表作为示例
    return List.of();
}
```
`exportExcelBatch` 的数据提供者是它（`:48-53`），`exportPdf` 取数也是它（`:68`）。**结果：下载得到的 xlsx/pdf 只有表头，无数据行**，且不报错。（`/pdf/report` 只把请求体里的 `content` 排版成 PDF，路径不同。）

### 5.3 `/api/import/v2/{excel|csv}/{dataType}`【真桩（写入侧）】
`export/service/impl/DataImportServiceImpl.java:82-158`（Excel）/`:161-...`（CSV）：完整做了解析、表头映射、必填校验、字典项校验，但**全文件没有任何 insert/update/save**（grep 无命中）。`:151-152` 直接 `return ImportResult.success(totalRows, successCount);`。
**后果：导入接口回执"成功 N 条"，数据库一行未写。**

### 5.4 `/api/import/upload`（批量导入）【半桩】
`export/service/impl/BatchImportServiceImpl.java:91-144`：读流 → 校验 → 逐行 `updateProgress(...)` 刷进度，**同样不落库**（只有进度回写 Redis）。附带两个缺陷：
- `:107-114` 对**同一个 `InputStream` 读了两次**（`importExcelWithValidation` 与 `importExcel`），第二次读时流已被消费，实际会走 `catch` 落到 `failed`（如实报错，不算谎报，但功能不可用）。
- `:275-281` `generateErrorFile` 只返回字符串 `"error_" + now + ".xlsx"`，**不生成文件**；随后该文件名会被写进进度的 `errorFile` 字段（`:132-134`），前端拿到的下载路径是假的。

### 5.5 `/api/export/excel/import`、`/api/export/csv/import`【半桩】
`DataExportController.java:66-96`
```java
List<?> data = dataExportService.importExcel(in, headers, rowClass);
return new DataExportService.ImportResult(data.size(), data.size(), 0, List.of());
```
解析是真解析，但**全部计为成功、零失败、不落库**；`rowClass` 恒为 `Map.class`（`:131-134`），`headers` 只有 user/product/customer 三类硬编码（`:121-129`）。

### 5.6 模板管理（菜单 62402）【真实】
`export/template/ImportTemplateStore.java` 2026-09-19 已从"内存 ConcurrentHashMap"改为**真实读写 `dev_template`**：`getAllTemplates/getTemplate/getTemplateByDataType` 走 `template_kind='import' AND enabled=true` 查询（`:202-207`），`register/updateTemplate` 真 insert/updateById，`removeTemplate` 真物理删（`:187-195`），操作人取登录账号（`:288-295`），content 列 JSON 序列化/反序列化失败时记 error 不伪造字段（`:262-280`）。`ImportTemplateController` 的 13 个端点与前端 `/api/import-templates` 对齐。
**注意**：它**刻意绕开** `cn.aiedge.dev.service.DevTemplateService` —— 因为 `cn.aiedge.dev` **不在 `scanBasePackages`**（`AiReadyApplication.java:21-108` 无该包），注释已在 `ImportTemplateStore.java:40-42` 说明；`DevTemplateMapper` 靠通配 `@MapperScan` 注册。`cn.aiedge.dev` 下只有 mapper/model/service、无 controller，故该包未装配目前无功能损失。

### 5.7 模板下载【真实】
`DataExportController.java:99-113`：调用 `dataExportService.exportExcel(List.of(), headers, out)` 生成只含表头的 xlsx —— **这是模板下载的预期语义**，属真实。

---

## 六、模块管理（菜单 62101-62104）【真实】

`module/service/impl/ModuleServiceImpl.java`：模块 CRUD + 逻辑删 + 状态切换（`:39-109`）、版本列表/分页（`:112-130`）、回滚（校验目标版本存在再改模块版本号，`:133-149`）、发布（真实插入 `sys_module_version` 并同步模块当前版本，发布人取真实登录用户而非硬编码 `admin`，`:161-191`）全部真落库。
`getUsageStats`（`:194-300`）：真实聚合 —— 模块清单来自 `sys_module`，调用次数/活跃用户来自 `sys_audit_log` 窗口统计，**"已授权租户数"改为按 `sys_tenant_module` 去重计数**（`:211-222`，注释登记此前硬编码为 1）；汇总卡 4 个数字均由数据算出（`:272-291`）。唯一口径依赖：`callCount`/`monthlyActive` 依赖 `audit_log.module` 与 `module_code` 对齐，脏数据会导致 0，但**不是编数**。
`ModuleEntitlementService` + `ModuleEntitlementInterceptor`（注册于 `config/WebMvcConfig.java:24,45`）把"模块授权"接进请求拦截，**"平台关掉模块"真的会 403**（`:1-60` 类注释记录此前 `hasModuleAccess` 只有读接口调用的空转状态）。

---

## 七、租户管理（tenant/list、package、quota、module-auth、approval）

### 7.1 租户配置【真桩】
`tenant/controller/TenantController.java:228-260`
```java
public Result<Map<String, Object>> getConfig(@PathVariable Long id) {
    ... Map.of("id", 0, "tenantId", id, "logo", "", "themeColor", "#1890ff",
               "features", "", "maxUsers", 100, "expireDate", "");   // :239-247 硬编码占位
}
public Result<Boolean> updateConfig(...) {
    // 配置存储暂未实现，返回成功占位        // :258
    return Result.ok(true);                  // :259
}
```
读返回硬编码 map（`maxUsers` 恒 100、`themeColor` 恒蓝），写**直接返回成功不落库**。前端 `api/tenant.ts:88,93` 定义了 `getConfig/updateConfig`，但全仓 grep 这两个方法的调用方 **0 命中**（无 .vue 使用）——属未接线的残留桩。

### 7.2 租户配额【半桩：能存不能用】
`tenant/service/impl/TenantQuotaServiceImpl.java:21-57` 是真实 CRUD（`sys_tenant_quota`）。但全仓 grep `SysTenantQuota`：只有 `TenantQuotaController`（CRUD）与 `SetAppCenterController.java:125-129`（**只读展示**）。**没有任何地方拿配额去校验**（建用户不查 maxUsers、不查存储上限），配置项纯展示。

### 7.3 租户套餐【真实（CRUD）】
`tenant/service/impl/TenantPackageServiceImpl.java` 真实 CRUD + 逻辑删。是否被登录/注册链路消费未在本次范围内核实（**未验证，不作结论**）。

### 7.4 租户档案 / 注册 / 重建【真实】
- `tenant/service/TenantProfileService.java`：真实双表事务写（`sys_tenant` + `sys_tenant_profile`），逐列显式 `set` 支持清空（`:35-80`）。
- `tenant/rebuild/SystemRebuildService.java`：范围清单是**数据驱动的真实 SQL 片段**（`delTextTenant(...)` 等），预检要求每个目标都能在 SQL 上限定租户否则整请求拒绝（`:382-390` 类注释）；未见桩（`:366-376` 的 `return null` 是 `scope(key)` 查不到的正常返回）。
- `tenant/scheduler/TenantExpiryScheduler.java`：`@Scheduled` 真实扫描并冻结过期租户（`:37-88`）。

---

## 八、邮件 / 短信 / 存储配置 / 安全策略（菜单 62502-62505）

### 8.1 邮件配置【真实】
`platform/service/impl/MailConfigServiceImpl.java:82-134`：`Transport.connect` 真连 SMTP 并认证，5 秒连接/读/写超时，SSL/STARTTLS 按配置映射，区分 `AuthenticationFailedException`（凭据错）与网络错误的文案。边界如实声明："只做连接+认证，不发信"（`:77`）。CRUD 落库真实（`:41-62`）。

### 8.2 短信配置【真实（能力如实收窄）】
`SmsConfigServiceImpl.java:95-135`：先校验 provider/accessKey/accessSecret/signName 是否齐备，再对内置服务商端点做 TCP 建连。**明确声明"未集成短信 SDK、未校验 AccessKey 有效性"**（`:83-92`）；未知 provider 直接失败并列出支持值（`:114-118`）。这是配置完整性 + 网络连通性测试，**不是短信发送能力**——文案已如实区分，算真实实现但其能力边界需知晓。

### 8.3 存储配置【真实（同上边界）】
`StorageConfigServiceImpl.java:85-177`：`local` 类型做**真实写入探测**（createDirectories + 写随机探针文件 + 删，`:123-138`），对象存储类型做 endpoint TCP 建连并声明"未集成 SDK、未校验 Key/bucket"（`:167-170`）。

### 8.4 安全策略【半桩：5 个字段无消费方】
`platform/service/impl/SecurityPolicyServiceImpl.java:20-61` 的读写真实落库。
消费链路：`core/base/.../config/SecurityPolicyBridge.java:32-42` 在 `ApplicationReadyEvent` 把 provider 注入 `PasswordPolicy` 静态字段 → `PasswordPolicy.validate` 真实使用 `passwordMinLength` 等。
但 `core/base/core-base/src/main/java/cn/aiedge/base/util/PasswordPolicy.java:23-29` 明确写道：
```java
* <li>不实现 {@code lockThreshold}/{@code lockDuration}/{@code sessionTimeout}/{@code ipWhitelist}
*     / {@code singleDevice} —— 这些字段目前**仍无消费方**，见开发文档「剩余缺口」。</li>
```
即：密码长度/字符类别（+ `passwordExpireDays` 由 `SysUserServiceImpl` 消费）真实生效；**登录失败锁定阈值、锁定时长、会话超时、IP 白名单、单设备登录这 5 项是"配了但没用"**。

### 8.5 平台参数（platform/params）【真实】
`config/service/impl/SystemConfigServiceImpl.java` 2026-09-18 已由"JVM 内存假实现"改造为真实读写 `sys_config`：读路径全走 mapper、save 真 INSERT/UPDATE 且返回回读行、真分页（LIMIT/OFFSET + 独立 COUNT）、批量删除单条 SQL、`refreshCache` 真清理并返回条数、内置配置拒绝删除是"真拒绝"（`:22-45` 逐条登记）；`getConfig(id)` 曾是 `return null; // 简化实现`，现为 `selectByIdAnyTenant`（`:102-103`）。端点 `/api/config/*` 与前端 `platformConfigApi`（`api/admin.ts:432-452`）对齐。

---

## 九、权限模拟（permission）【真实，历史空壳已修】

`permission/service/PermissionSimulationService.java:49-95`：模拟态写入 **Sa-Token Session**（`SESSION_KEY`），单次模拟走请求头 `X-Simulate-User-Id`；启动校验 `system:simulate` 权限、禁止模拟自己。
消费方确实存在：`core/base/core-base/src/main/java/cn/aiedge/base/security/StpInterfaceImpl.java:41,53,66-75` 的 `resolveEffectiveUserId()` 读取 `PermissionSimulationHolder`（单次）与 Session（持久），`@SaCheckPermission` 判定按被模拟用户计算。
`PermissionSimulationController.java:52-62` 用 `HashMap` 而非 `Map.of` 规避 null 值 NPE（注释即本仓实踩记录）。
**结论：真实可用。** `permission/service/impl/PermissionServiceImpl.java` 的权限/角色缓存查询在异常时回退 DB 或降级为空集（`:60-105, 186-197`），属缓存降级策略，非造假。

---

## 十、调度任务（dev/scheduler，菜单 62405）【真实】

`scheduler/task/TaskExecutor.java`：`ThreadPoolTaskScheduler` 真调度（CRON / FIXED_DELAY / FIXED_RATE，`:77-95`），执行前写 `scheduled_task_log`（RUNNING）→ 按 `job_key` 从**白名单注册表**解析处理器执行 → 写终态 + 重试 + 统计（`:157-227`）。执行逻辑 2026-09-14 已从"反射 `Class.forName` + 无参构造"改为 `JobHandlerRegistry`（`:229-246`），彻底解决"无法注入 Spring Bean"和"任意类名下发"两个问题。
`JobHandlerRegistry.java:38-60`：启动时收集容器内全部 `JobHandler`；**已注册实现实际只有 4 个**——`dms/dispatch/job/DmsDispatchEscalateJob`、`erp-marketing` 的 `AutoCampaignRunJob`/`MemberPointsExpireJob`/`StoredCardExpireJob`（全仓 `implements JobHandler` grep 结果）。注册表为空时会 `log.warn` 并让执行以"未注册的任务处理器"失败（不是静默成功）。
`scheduler/service/impl/ScheduledTaskServiceImpl.java`：CRUD/启停/立即执行/重试全部真实，`validateJobKey` 在配置期即拦截无效处理器（`:101-111`），发起人由调用线程解析后透传（避免切线程丢会话，`:157-160, 274-287`）。

---

## 十一、API 测试台（dev/api-test，菜单 62404）【真实】

`devtool/service/ApiTestOutboundService.java`：真实 Apache HttpClient 出站，方法白名单（`:82`）、逐跳头/平台头黑名单（`:95-102`）、响应头脱敏（`:104-106`），类注释逐条登记 8 道闸门，并**自建口径标注为 D 级、不声称业界标准**（`:58-60`）。配合 `devtool/security/ApiTestTargetGuard`（协议白名单 / allowlist / 内网网段 / 钉死 DNS 防 rebinding）。`cn.aiedge.devtool` 已在 `scanBasePackages`（`AiReadyApplication.java:107`）。

---

## 十二、接口监控（monitor/api）与 API 文档（dev/api-doc）【真实，历史桩已修】

- 接口监控页调 `/api/trade/api-monitor/*`：`core/base/core-base/.../trade/monitor/controller/ApiMonitorController.java` 15 个端点齐全；`ApiMonitorServiceImpl.java` 的呼叫做真分页/真聚合（`api_access_log`）、依赖健康真探测（§1.1）、阈值读配置中心（`:599-626`）、沙箱调用、告警列表、同步重试（`:687-715`）。
- 采集链路存在：`core/base/.../trade/monitor/ApiCallLogInterceptor.java`、`ApiCallLogRecorder.java`、`ApiMonitorLogRetentionJob.java`（保留期清理）。
- 页面曾写死的 6 条模块清单与不存在的 `/api/monitor/info` 已删除（`views/admin/monitor/api/index.vue:8-16`、`views/admin/dev/api-doc/index.vue:10-16` 页内注释记录）。
- API 文档页消费**真实 OpenAPI 3 spec**：`GET {backendOrigin}/v3/api-docs`（`api/admin.ts:748-757`），该路径在 Sa-Token 放行清单内（页面注释引 `SaTokenConfig.java`）。

---

## 十三、系统管理基础页（菜单/用户/角色/权限/岗位/字典/日志/配置）

- 这些页面后端在 `core/base/core-base/.../base/controller/**`（`SysMenuController`/`SysUserController`/`SysRoleController`/`SysPermissionController`/`PermissionTemplateController`/`SysTenantMenuController`/`UserPageConfigController` 等）。对 `controller/` 与 `SysMenuServiceImpl`/`SysUserServiceImpl`/`SysRoleServiceImpl` 做过 `TODO|FIXME|模拟|简化|占位` 专项 grep，**0 命中**。
- 字典：`dict/service/impl/DictItemServiceImpl.java` 真实 CRUD + 父子校验 + 缓存刷新（`:82-178`）；`batchCreate` 逐条 try/catch 跳过失败项并 `log.warn`（`:104-106`，属批量容错不是静默吞异常——返回值是真实成功条数）。
- 日志：`audit/controller/SysLogStubController.java`（**类名带 Stub 但实现是真的**，`/api/log` 分页/详情/模块/操作类型/清理预览/清理/导出/登录日志，`:99-266`）；`audit/service/impl/AuditLogServiceImpl.java` 真实落 `sys_audit_log`、查询/统计/清理/导出齐全。另一份平行实现 `base/controller/LogManageController`（`/api/system/log`）前端 0 引用，类注释已裁定保留但不接线。
- 本次**未逐行核对**该目录全部数十个控制器（超出"系统模块 30 页"重点），如需可单独补一轮。

---

## 十四、文件存储（cn.aiedge.storage）【未装配，符合背景描述】

`AiReadyApplication.java:21-108` 的 `scanBasePackages` **确实不含 `cn.aiedge.storage`**（对比同批注释 :92-108 只补了 module/monitor/platform/datasource/export/devtool/config）。
未装配的端点：`storage/controller/FileStorageController.java:35` `@RequestMapping("/api/storage")`、`storage/chunk/ChunkUploadController.java:26` `/api/storage/chunk` —— 运行时 **404**。
影响面**有限**：前端"企业 LOGO 上传"走的是 `cn.aiedge.common.file.FileUploadController`（`@RequestMapping("/api/file")`，`core-api/.../common/file/FileUploadController.java:74-104`，独立实现、直接落盘 `storage.local.base-path`），**不依赖 storage 包**；`api/admin.ts:613-621` 的"存储配置"走 `/api/storage-config/**`（`cn.aiedge.platform`，已装配）。
`storage/` 内部另有 `LocalFileStorageService.java:134` "简化实现，实际应从数据库查询"、`:305` "获取当前用户ID（简化实现）"、`ChunkUploadServiceImpl.java:241` "这里简化实现" —— 因未装配，**当前不影响运行**，属于"死目录"（装配后才暴露）。

---

## 十五、协议模块（backend/agreement）快速扫描

`agreement/src/main/java` 全目录 grep `TODO|FIXME|暂未实现|模拟|mock|stub|假数据|空实现` → **0 命中**。该包已在 `scanBasePackages`（`AiReadyApplication.java:66`，注释记录曾漏配导致端点 404，已修）。本次未逐类通读（不在"系统模块 30 页"清单内）。

---

## 附：本次**未找到实现**的搜索位置（供复核）

| 声称能力 | 搜索范围 | 结果 |
|---|---|---|
| 慢查询数据采集 | 全 `backend/**/*.java` grep `SlowQueryMapper` / `sys_slow_query` | 仅 model/mapper/service/controller 读取侧，**无写入方** |
| 日志聚合服务调用方 | 全 `backend/**/*.java` grep `LogAggregationService` | **0 命中**（除自身） |
| 告警触发调用方 | 全 `backend/**/*.java` grep `checkAndAlert` / `AlertRuleService` | 仅自身声明/实现 + `AlertManagementController` 的 CRUD 方法，**无 `checkAndAlert` 调用** |
| 配额限额校验 | 全 `backend/**/*.java` grep `SysTenantQuota` | 仅 CRUD + `SetAppCenterController` 只读展示 |
| 安全策略 5 字段消费方 | `PasswordPolicy.java:23-29` 自述 + grep `lockThreshold`/`ipWhitelist`/`sessionTimeout`/`singleDevice` | 仅实体 getter/setter 与日志，**无业务消费** |

---

# 真桩 / 半桩清单

## 真桩（功能完全不产生实际效果）

| # | 功能 | 文件:行 | 一句话 |
|---|---|---|---|
| 1 | 导出 Excel | `core/api/core-api/.../export/controller/DataExportController.java:36-49` | 只设响应头，方法体空，`// TODO: 实现真实Excel导出逻辑` |
| 2 | 导出 CSV | `.../export/controller/DataExportController.java:52-63` | 同上，`// TODO: 实现真实CSV导出逻辑` |
| 3 | 批量导出 Excel / PDF | `.../export/controller/DataExportControllerExt.java:110-114` | `fetchData()` 恒 `return List.of()`，产出只有表头 |
| 4 | 导入 Excel/CSV（v2 主链路） | `.../export/service/impl/DataImportServiceImpl.java:82-158` | 解析+校验齐全但**零 insert**，却 `return ImportResult.success(total, success)` |
| 5 | 导入 Excel/CSV（旧链路） | `.../export/controller/DataExportController.java:76-79` | 解析后 `new ImportResult(data.size(), data.size(), 0, List.of())`，全计成功、不落库 |
| 6 | 批量导入错误文件 | `.../export/service/impl/BatchImportServiceImpl.java:275-281` | 返回 `"error_"+ts+".xlsx"` 字符串，从不生成文件 |
| 7 | 依赖服务健康探测 | `.../monitor/controller/HealthMonitorController.java:253-262` | Redis/MQ 恒 false→DOWN，外部API 恒 true→UP，写死常量 |
| 8 | 测试通知渠道 | `.../monitor/controller/AlertManagementController.java:240-254` | 只打日志，回执"测试通知已发送" |
| 9 | 慢查询页 | `.../datasource/service/impl/SlowQueryServiceImpl.java:22-33` | 查询实现真实，但**全仓无任何写入方**，页面恒空 |
| 10 | 日志聚合服务 | `.../monitor/service/LogAggregationService.java`（全文） | 全仓 0 调用方，读写入口都不存在（死代码） |
| 11 | 租户配置读 | `.../tenant/controller/TenantController.java:239-247` | 返回硬编码 map（maxUsers 恒 100、themeColor 恒 #1890ff） |
| 12 | 租户配置写 | `.../tenant/controller/TenantController.java:257-259` | `// 配置存储暂未实现，返回成功占位` → `Result.ok(true)` |
| 13 | 告警自动触发 | `.../monitor/service/impl/AlertRuleServiceImpl.java:91` | `checkAndAlert` 无任何调用方，规则配了永不触发 |

## 半桩（部分真实）

| # | 功能 | 文件:行 | 一句话 |
|---|---|---|---|
| 1 | 告警邮件/短信通知 | `.../monitor/service/impl/AlertRuleServiceImpl.java:195-203` | email/sms 分支只 `log.info`，只有 webhook 真发 |
| 2 | 通知渠道配置 | `.../monitor/controller/AlertManagementController.java:33` | `ConcurrentHashMap` 内存态，重启即丢 |
| 3 | 告警规则列表筛选 | `.../monitor/controller/AlertManagementController.java:82-92` | `wrapper` 构造后未使用 + `getEnabledRules` 固定 enabled=true → 禁用规则不可见 |
| 4 | 性能监控历史/趋势/预测 | `.../monitor/service/impl/SystemMonitorServiceImpl.java:28-30,70-74`；`.../controller/PerformanceMetricsController.java:31-32,92-96` | 历史仅进程内且只在访问 `/realtime` 时写入，重启清零、多实例不共享，`hours` 参数被忽略 |
| 5 | 综合健康 components | `.../monitor/controller/HealthMonitorController.java:59-63` | `healthDetails` 取到后未使用，components 恒空 map |
| 6 | 网络统计 | `.../monitor/controller/InfrastructureMonitorController.java:230-256` | 只有采集时间 + "需外部库" 说明 + 网卡计数，无真实流量指标 |
| 7 | 安全策略 - 5 个字段 | `core/base/core-base/.../util/PasswordPolicy.java:23-29` | `lockThreshold/lockDuration/sessionTimeout/ipWhitelist/singleDevice` 自述"仍无消费方" |
| 8 | 租户配额 | `.../tenant/service/impl/TenantQuotaServiceImpl.java:21-57` | CRUD 真落库，但无任何建用户/存储校验消费，纯展示 |
| 9 | 批量导入进度 | `.../export/service/impl/BatchImportServiceImpl.java:91-144` | 进度真实写 Redis，但从未写业务表；且对同一 InputStream 读两次 |
| 10 | 文件存储包 | `AiReadyApplication.java:21-108`（无 `cn.aiedge.storage`） | `/api/storage/**` 与 `/api/storage/chunk/**` 运行期 404；`/api/file/**` 走独立实现不受影响 |

## 已确认修复（历史清单中已不成立的项）

备份管理（真 pg_dump/pg_restore）、同步任务（真投递+如实回执）、清理规则（真分批 DELETE）、缓存管理读写（真 Redis INFO/SCAN）、接口监控（真 `api_access_log` + 真依赖探测）、API 文档（真 `/v3/api-docs`）、权限模拟（真接 `StpInterfaceImpl`）、平台参数（真 `sys_config`）、模块管理（真落库 + entitlement 拦截）、邮件/短信/存储连通测试（真 SMTP / TCP / 写盘探测）、调度任务（真线程池 + 白名单处理器）、操作日志（真分页/导出/清理）、模板管理（真 `dev_template` 落库）。

**风险排序（若只修 3 个）**：① 导入"报成功不落库"（用户会以为数据已进系统，最危险）→ ② 导出"下载空文件"（交付物缺失）→ ③ 告警规则永不触发 + 依赖探测硬编码（故障不可见）。