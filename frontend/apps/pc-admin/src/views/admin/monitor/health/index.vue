<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        服务状态（系统 → 系统监控 → 服务状态，菜单 62201）
        · 平台控制台页面：ql361 无对标 → 按 Spring Boot Actuator + K8s 探针（readiness / liveness）能力模型建模
        · 仪表盘形态（卡片 + 小表格），不是台账列表 → 不套 CategoryListLayout；页内表格用 BillDetailTable 以获得表头列配置齿轮
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/服务状态开发文档.md

        ═══ 数据真实性（与后端源码逐一对应，页面如实标注）═══
        真实探测：
          /monitor/health/status  Actuator HealthEndpoint#health() 聚合健康
          /monitor/health/jvm     ManagementFactory MXBean（堆内存 / 线程数 / 死锁）
          /monitor/health/disk    File.listRoots()
          /monitor/health/database DataSource 实连 + connection.isValid(5)
          /monitor/health/ready   就绪探针（DB 实连）
          /monitor/health/live    存活探针（堆占用 + 死锁数）
        🔴 常量（非真实探测，页面必须标注，不得看起来像探测结果）：
          HealthMonitorController.java:247  database    → dataSource != null（装配存在性判定，非连通性探测）
          HealthMonitorController.java:250  redis       → 常量 false，恒 DOWN
          HealthMonitorController.java:253  messageQueue→ 常量 false，恒 DOWN
          HealthMonitorController.java:256  externalApi → 常量 true，恒 UP
        🔴 /monitor/health/status 的 components 后端恒为空（HealthMonitorController.java:58-60
          自述「简化实现，不获取组件详情」）→ 页面显示「未返回组件明细」而非空白。

        ═══ 本轮修复（只改前端）═══
        ① 外壳补 ErrorBoundary；
        ② 删除 `res.status || 'UP'` / `res.healthScore || 100` 兜底 —— 后端 DOWN 时 healthScore 合法值为 0，
           `0 || 100` 会把「宕机」显示成「100 分正常」；初值一律改 null，取不到显示「—」；
        ③ 4 处 `catch { /* ignore */ }` 静默吞错 → 改为 console.error + message.error + 对应区块置「—」，
          并在页顶汇总成错误横幅（接口失败必须可见）；
        ④ 磁盘表换 BillDetailTable（表头齿轮：个人 + 全局列配置）；
        ⑤ 磁盘 status 补齐后端三值 UP/WARNING/CRITICAL（原实现只认 UP，把 WARNING 显示成「异常」）；
        ⑥ 「运行状态」不再写死「运行中」、「服务名」取后端字段、「服务时间」标签纠正为「最后刷新」
          （原标签叫服务时间，值其实是浏览器本地时钟，后端 RuntimeMXBean#getUptime 未透出）；
        ⑦ 新增 /database、/ready、/live 三块真实探针视图（文档 §12 P1-⑥ 登记的缺口）。
      -->
      <template #header>
        <div class="page-header">
          <div class="page-header-left">
            <a-breadcrumb>
              <a-breadcrumb-item>
                <router-link to="/">
                  首页
                </router-link>
              </a-breadcrumb-item>
              <a-breadcrumb-item>系统管理</a-breadcrumb-item>
              <a-breadcrumb-item>服务状态</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header-title">
              服务状态
            </h2>
          </div>
          <div class="page-header-right">
            <a-space :size="8">
              <span class="auto-refresh-hint">每 30 秒自动刷新</span>
              <a-button
                size="small"
                :loading="loading"
                @click="refreshAll"
              >
                <template #icon>
                  <ReloadOutlined />
                </template>
                刷新
              </a-button>
            </a-space>
          </div>
        </div>
      </template>

      <div class="dashboard-scroll">
        <a-spin :spinning="loading">
          <!-- 加载失败汇总：接口异常必须可见，绝不静默 -->
          <a-alert
            v-if="hasError"
            class="section-alert"
            type="error"
            show-icon
            message="部分监控数据加载失败，对应区块已置为「—」"
            :description="`失败区块：${errorSectionNames}。请检查 core-api（5655）是否可用或稍后重试。`"
          />

          <a-alert
            class="section-alert"
            type="warning"
            show-icon
            banner
            message="依赖服务状态中的 Redis / Message Queue / External API 为后端硬编码常量，并非真实探测结果，请勿据此判断线上依赖健康度。"
          />

          <!-- ═══ 综合健康状态 ═══ -->
          <a-row
            :gutter="16"
            class="block-row"
          >
            <a-col :span="24">
              <a-card :bordered="false">
                <template #title>
                  综合健康状态
                  <a-tooltip title="来自 Spring Boot Actuator HealthEndpoint#health() 聚合结果（HealthMonitorController.java:50）">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                </template>
                <template #extra>
                  <a-tag :color="STATUS_MAP[healthStatus ?? '']?.color || 'default'">
                    {{ STATUS_MAP[healthStatus ?? '']?.label || '未获取' }}
                  </a-tag>
                </template>
                <a-row :gutter="[16,16]">
                  <a-col :span="6">
                    <a-statistic
                      title="健康分数"
                      :value="healthScore ?? 0"
                      :formatter="() => statText(healthScore, 0, ' 分')"
                      :value-style="{ color: scoreColor }"
                    >
                      <template #prefix>
                        <CheckCircleOutlined
                          v-if="healthScore !== null && healthScore >= 90"
                          style="color:#52c41a"
                        />
                      </template>
                      <template #suffix>
                        <a-tooltip title="后端按「单一 Actuator 状态」线性映射：UP→100 / DEGRADED→70 / DOWN→0 / 其它→50（HealthMonitorController.java:339-351），不是多指标加权评分。">
                          <QuestionCircleOutlined class="title-hint" />
                        </a-tooltip>
                      </template>
                    </a-statistic>
                  </a-col>
                  <a-col :span="6">
                    <!-- 纯文本指标：:value 固定传数字 0，展示串一律走 :formatter（否则「—」会被解析成 -0） -->
                    <a-statistic
                      title="服务名称"
                      :value="0"
                      :formatter="() => serviceName || '—'"
                    />
                  </a-col>
                  <a-col :span="6">
                    <a-statistic
                      title="运行状态"
                      :value="0"
                      :formatter="() => runStateLabel"
                    >
                      <template #prefix>
                        <!-- 仅在真实 UP 时显示旋转图标；故障时不再假装「运行中」 -->
                        <SyncOutlined
                          v-if="healthStatus === 'UP'"
                          spin
                          style="color:#1890ff"
                        />
                      </template>
                    </a-statistic>
                  </a-col>
                  <a-col :span="6">
                    <a-statistic
                      title="最后刷新"
                      :value="0"
                      :formatter="() => lastRefresh || '—'"
                    />
                  </a-col>
                </a-row>

                <!-- Actuator 组件树：后端 components 恒空，如实提示 -->
                <div class="components-block">
                  <div class="components-title">
                    健康组件明细（components）
                  </div>
                  <template v-if="componentList.length">
                    <a-space
                      v-for="c in componentList"
                      :key="c.name"
                      :size="6"
                      class="component-item"
                    >
                      <span class="component-name">{{ c.name }}</span>
                      <a-tag :color="STATUS_MAP[c.status ?? '']?.color || 'default'">
                        {{ STATUS_MAP[c.status ?? '']?.label || c.status || '未知' }}
                      </a-tag>
                    </a-space>
                  </template>
                  <span
                    v-else
                    class="muted"
                  >后端 /status 未返回组件明细（components 为空，HealthMonitorController.java:58-60「简化实现」），
                    此处无数据可展示；各组件的真实状态请参考下方各专项卡片。</span>
                </div>
              </a-card>
            </a-col>
          </a-row>

          <!-- ═══ 依赖服务状态 ═══ -->
          <a-row
            :gutter="16"
            class="block-row"
          >
            <a-col :span="24">
              <a-card :bordered="false">
                <template #title>
                  依赖服务状态
                  <a-tooltip title="来自 GET /api/monitor/health/dependencies。除 Database 外均为后端硬编码常量，详见每张子卡的说明。">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                </template>
                <template #extra>
                  <a-tag :color="dependencySummary === 'UP' ? 'green' : dependencySummary === 'DEGRADED' ? 'orange' : 'default'">
                    汇总：{{ dependencySummary === 'UP' ? '全部可用' : dependencySummary === 'DEGRADED' ? '降级' : (dependencySummary || '未获取') }}
                  </a-tag>
                </template>

                <a-empty
                  v-if="!dependencies.length"
                  description="依赖服务数据不可用"
                />
                <a-row
                  v-else
                  :gutter="[16,16]"
                >
                  <a-col
                    v-for="dep in dependencies"
                    :key="dep.key"
                    :span="8"
                  >
                    <a-card
                      size="small"
                      :style="{ borderLeft: `3px solid ${depBorderColor(dep)}` }"
                    >
                      <a-row align="middle">
                        <a-col flex="auto">
                          <div style="font-weight:500">
                            {{ dep.name }}
                          </div>
                          <div class="dep-note">
                            {{ depNote(dep) }}
                          </div>
                        </a-col>
                        <a-col>
                          <a-tag :color="depTagColor(dep)">
                            {{ depTagLabel(dep) }}
                          </a-tag>
                        </a-col>
                      </a-row>
                    </a-card>
                  </a-col>
                </a-row>

                <div
                  v-if="dependencySummary === 'DEGRADED'"
                  class="muted summary-note"
                >
                  汇总状态 DEGRADED 由上述常量项（Redis / Message Queue 恒 DOWN）直接导致，不代表真实依赖故障。
                </div>
              </a-card>
            </a-col>
          </a-row>

          <!-- ═══ JVM 运行状态 ═══ -->
          <a-row
            :gutter="16"
            class="block-row"
          >
            <a-col :span="24">
              <a-card :bordered="false">
                <template #title>
                  JVM 运行状态
                  <a-tooltip title="来自 ManagementFactory MXBean 实算值（GET /api/monitor/health/jvm）。阈值：堆占用 >80% 警告、>90% 或有死锁为严重。">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                </template>
                <template #extra>
                  <a-space :size="8">
                    <a-tag :color="STATUS_MAP[jvmStatus ?? '']?.color || 'default'">
                      {{ STATUS_MAP[jvmStatus ?? '']?.label || '未获取' }}
                    </a-tag>
                    <span class="muted">死锁线程：{{ jvmDeadlockedCount ?? '—' }}</span>
                  </a-space>
                </template>
                <a-row :gutter="[16,16]">
                  <a-col :span="6">
                    <a-statistic
                      title="堆内存使用"
                      :value="jvmHeapUsage ?? 0"
                      :formatter="() => statText(jvmHeapUsage, 1, '%')"
                      :value-style="{ color: jvmHeapUsage !== null && jvmHeapUsage > 80 ? '#ff4d4f' : jvmHeapUsage === null ? '#bfbfbf' : '#52c41a' }"
                    />
                  </a-col>
                  <a-col :span="6">
                    <a-statistic
                      title="已用堆内存"
                      :value="jvmHeapUsed ?? 0"
                      :formatter="() => formatBytes(jvmHeapUsed)"
                    />
                  </a-col>
                  <a-col :span="6">
                    <a-statistic
                      title="最大堆内存"
                      :value="jvmHeapMax ?? 0"
                      :formatter="() => formatBytes(jvmHeapMax)"
                    />
                  </a-col>
                  <a-col :span="6">
                    <a-statistic
                      title="线程数"
                      :value="jvmThreadCount ?? 0"
                      :formatter="() => statText(jvmThreadCount, 0, '')"
                    />
                  </a-col>
                </a-row>
              </a-card>
            </a-col>
          </a-row>

          <!-- ═══ 磁盘状态 ═══ -->
          <a-row
            :gutter="16"
            class="block-row"
          >
            <a-col :span="24">
              <a-card :bordered="false">
                <template #title>
                  磁盘状态
                  <a-tooltip title="来自 File.listRoots() 实测（GET /api/monitor/health/disk）。阈值：使用率 >90% 警告、>95% 严重。">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                </template>
                <template #extra>
                  <a-tag :color="STATUS_MAP[diskStatus ?? '']?.color || 'default'">
                    汇总：{{ STATUS_MAP[diskStatus ?? '']?.label || '未获取' }}
                  </a-tag>
                </template>
                <!--
                  表格容器必须有确定高度并是 flex 纵向容器：
                  BillDetailTable 根元素为 flex:1，父级非 flex / 无高度时表格会塌陷为 0。
                  :min-rows="0" —— 本表是只读快照，不要 20 行 ghost 占位行（否则空数据时显示 20 行空行）。
                -->
                <div class="dash-table">
                  <BillDetailTable
                    v-model:data-source="disks"
                    :columns="diskColumns"
                    :view-mode="true"
                    :min-rows="0"
                    empty-text="磁盘数据不可用"
                    row-key="path"
                    storage-key="system-monitor-health-table-columns"
                    global-config-key="system-monitor-health-table-columns"
                  >
                    <template #usageCell="{ record }">
                      <a-progress
                        :percent="Number(record.usagePercent || 0)"
                        size="small"
                        :status="record.usagePercent > 90 ? 'exception' : 'active'"
                        :format="progressText"
                      />
                    </template>
                    <template #statusCell="{ record }">
                      <a-tag :color="STATUS_MAP[record.status || '']?.color || 'default'">
                        {{ STATUS_MAP[record.status || '']?.label || '未知' }}
                      </a-tag>
                    </template>
                  </BillDetailTable>
                </div>
              </a-card>
            </a-col>
          </a-row>

          <!-- ═══ 数据库与探针（/database · /ready · /live，均为真实探测） ═══ -->
          <a-row :gutter="16">
            <a-col :span="24">
              <a-card :bordered="false">
                <template #title>
                  数据库与探针
                  <a-tooltip title="来自 GET /api/monitor/health/database（DataSource 实连 + isValid(5)）、/ready、/live。">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                </template>
                <a-row :gutter="[16,16]">
                  <a-col :span="8">
                    <a-card
                      size="small"
                      title="数据库连接"
                    >
                      <template #extra>
                        <a-tag :color="STATUS_MAP[dbHealth?.status ?? '']?.color || 'default'">
                          {{ STATUS_MAP[dbHealth?.status ?? '']?.label || (dbHealth ? '未知' : '未获取') }}
                        </a-tag>
                      </template>
                      <div class="probe-line">
                        产品：{{ dbHealth?.databaseProductName ? `${dbHealth.databaseProductName} ${dbHealth.databaseProductVersion || ''}` : '—' }}
                      </div>
                      <div class="probe-line">
                        连通响应耗时：{{ dbHealth?.responseTimeMs ?? '—' }} ms
                      </div>
                      <div
                        v-if="dbHealth?.error"
                        class="probe-line probe-error"
                      >
                        错误：{{ dbHealth.error }}
                      </div>
                    </a-card>
                  </a-col>
                  <a-col :span="8">
                    <a-card
                      size="small"
                      title="就绪探针（readiness）"
                    >
                      <template #extra>
                        <a-tag :color="readyProbe?.ready ? 'green' : readyProbe ? 'red' : 'default'">
                          {{ readyProbe ? (readyProbe.ready ? '已就绪' : '未就绪') : '未获取' }}
                        </a-tag>
                      </template>
                      <div class="probe-line">
                        检查项：
                      </div>
                      <div
                        v-for="(c, i) in readyProbe?.checks || []"
                        :key="i"
                        class="probe-line"
                      >
                        {{ c }}
                      </div>
                      <div
                        v-if="!readyProbe?.checks?.length"
                        class="probe-line"
                      >
                        —
                      </div>
                    </a-card>
                  </a-col>
                  <a-col :span="8">
                    <a-card
                      size="small"
                      title="存活探针（liveness）"
                    >
                      <template #extra>
                        <a-tag :color="liveProbe?.alive ? 'green' : liveProbe ? 'red' : 'default'">
                          {{ liveProbe ? (liveProbe.alive ? '存活' : '异常') : '未获取' }}
                        </a-tag>
                      </template>
                      <div class="probe-line">
                        判定口径：堆占用 &gt;99% 或死锁线程 &gt;10
                      </div>
                      <div class="probe-line">
                        状态码：{{ liveProbe?.status || '—' }}
                      </div>
                    </a-card>
                  </a-col>
                </a-row>
              </a-card>
            </a-col>
          </a-row>
        </a-spin>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, CheckCircleOutlined, SyncOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import request from '@/utils/request'

defineOptions({ name: 'AdminMonitorHealth' })

// ═══ 状态值域映射（对齐后端三值 UP / WARNING / CRITICAL 与 Actuator 的 DOWN / UNKNOWN）═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  UP: { label: '正常', color: 'green' },
  WARNING: { label: '警告', color: 'orange' },
  DEGRADED: { label: '降级', color: 'orange' },
  CRITICAL: { label: '严重', color: 'red' },
  DOWN: { label: '异常', color: 'red' },
  OUT_OF_SERVICE: { label: '已停服务', color: 'red' },
  UNKNOWN: { label: '未知', color: 'default' },
}

/**
 * 🔴 后端 /dependencies 中「不是真实探测」的项，逐字对应 HealthMonitorController 硬编码行号。
 * 这些项必须在页面上标注清楚，否则会让人误以为 Redis / MQ 真的挂了、外部 API 真的通。
 */
const CONSTANT_DEPENDENCIES: Record<string, { tag: string; color: string; note: string }> = {
  redis: {
    tag: '未接入探针',
    color: 'default',
    note: '后端硬编码为 DOWN（HealthMonitorController.java:250「需要实际检查」），非真实探测',
  },
  messageQueue: {
    tag: '未接入探针',
    color: 'default',
    note: '后端硬编码为 DOWN（HealthMonitorController.java:253「需要实际检查」），非真实探测',
  },
  externalApi: {
    tag: '未接入探针',
    color: 'default',
    note: '后端硬编码为 UP（HealthMonitorController.java:256「假设可用」），非真实探测',
  },
  database: {
    tag: '装配判定',
    color: 'default',
    note: '按 DataSource Bean 是否存在判定（HealthMonitorController.java:247），非连通性探测；真实连通性见下方「数据库连接」卡',
  },
}

// ═══ 数据状态（一律 null = 未取到 → 页面显示「—」，绝不用 0/UP/100 等假值兜底）═══
const loading = ref(false)
const lastRefresh = ref('')

const healthStatus = ref<string | null>(null)
const healthScore = ref<number | null>(null)
const serviceName = ref<string | null>(null)
const components = ref<Record<string, any> | null>(null)

const jvmStatus = ref<string | null>(null)
const jvmHeapUsage = ref<number | null>(null)
const jvmHeapUsed = ref<number | null>(null)
const jvmHeapMax = ref<number | null>(null)
const jvmThreadCount = ref<number | null>(null)
const jvmDeadlockedCount = ref<number | null>(null)

const diskStatus = ref<string | null>(null)
const disks = ref<any[]>([])

const dependencies = ref<Array<{ key: string; name: string; status: string | null }>>([])
const dependencySummary = ref<string | null>(null)

interface DbHealth {
  status: string
  responseTimeMs?: number | null
  databaseProductName?: string
  databaseProductVersion?: string
  error?: string
}
const dbHealth = ref<DbHealth | null>(null)
const readyProbe = ref<{ ready: boolean; status: string; checks?: string[] } | null>(null)
const liveProbe = ref<{ alive: boolean; status: string } | null>(null)

/** 各区块加载失败信息（key → 区块中文名）；非空即展示顶部错误横幅，并让对应卡片处于「—」态 */
const sectionErrors = reactive<Record<string, string>>({})
const hasError = computed(() => Object.keys(sectionErrors).length > 0)
const errorSectionNames = computed(() => Object.values(sectionErrors).join('、'))

const componentList = computed(() => {
  const c = components.value
  if (!c) return []
  return Object.entries(c).map(([name, val]: [string, any]) => ({
    name,
    status: typeof val === 'string' ? val : val?.status ?? null,
  }))
})

/** 运行状态：不再写死「运行中」，绑定真实 Actuator 状态 */
const runStateLabel = computed(() => {
  if (healthStatus.value === null) return '—'
  if (healthStatus.value === 'UP') return '运行中'
  if (healthStatus.value === 'DOWN') return '已停止'
  return STATUS_MAP[healthStatus.value]?.label || healthStatus.value
})

const scoreColor = computed(() => {
  if (healthScore.value === null) return '#bfbfbf'
  if (healthScore.value >= 90) return '#52c41a'
  if (healthScore.value >= 50) return '#faad14'
  return '#ff4d4f'
})

// ═══ 磁盘表列（rowNo 列承载表头「列配置」齿轮；插槽列必须 type:'slot'）═══
const diskColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'path', title: '路径', type: 'input', width: 140 },
  { key: 'totalSpace', title: '总空间', type: 'input', width: 110 },
  { key: 'usedSpace', title: '已用空间', type: 'input', width: 110 },
  { key: 'freeSpace', title: '空闲空间', type: 'input', width: 110 },
  { key: 'usagePercent', title: '使用率', type: 'slot', slotName: 'usageCell', width: 200 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 110 },
]

let timer: ReturnType<typeof setInterval> | null = null

// ═══ 工具函数 ═══
function numOrNull(v: any): number | null {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

/**
 * a-statistic 文本展示：null → 「—」。
 * ⚠️ 必须走 :formatter —— 直接给 :value 传「—」这类非数字串会被 a-statistic 解析成 -0。
 */
function statText(v: number | null | undefined, digits = 1, suffix = ''): string {
  if (v === null || v === undefined || Number.isNaN(v)) return '—'
  return v.toFixed(digits) + suffix
}

/** a-progress 内置百分比文案：后端 usagePercent 是全精度 double，需收敛到 1 位小数 */
function progressText(p: number): string {
  return `${Number(p).toFixed(1)}%`
}

function formatBytes(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined || !Number.isFinite(Number(bytes))) return '—'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = Number(bytes)
  let i = 0
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(1) + ' ' + units[i]
}

function depTagLabel(dep: { key: string; status: string | null }): string {
  const constant = CONSTANT_DEPENDENCIES[dep.key]
  if (constant) return constant.tag
  if (dep.status === null) return '未获取'
  return dep.status === 'UP' ? '正常运行' : '服务异常'
}

function depTagColor(dep: { key: string; status: string | null }): string {
  const constant = CONSTANT_DEPENDENCIES[dep.key]
  if (constant) return constant.color
  if (dep.status === null) return 'default'
  return dep.status === 'UP' ? 'green' : 'red'
}

function depBorderColor(dep: { key: string; status: string | null }): string {
  if (CONSTANT_DEPENDENCIES[dep.key]) return '#d9d9d9'
  if (dep.status === null) return '#d9d9d9'
  return dep.status === 'UP' ? '#52c41a' : '#ff4d4f'
}

function depNote(dep: { key: string; status: string | null }): string {
  const constant = CONSTANT_DEPENDENCIES[dep.key]
  if (constant) return constant.note
  return dep.status === 'UP' ? '正常响应' : '探测失败'
}

/** 统一失败处理：console.error + message.error（同 key 去重，避免 30 秒轮询刷屏）+ 区块错误态 */
function markError(section: string, label: string, error: any) {
  console.error(`[服务状态] ${label}加载失败`, error)
  sectionErrors[section] = label
  message.error({
    content: `${label}加载失败：${error?.message || '请求异常'}`,
    key: `monitor-health-${section}`,
    duration: 4,
  })
}

function clearError(section: string) {
  delete sectionErrors[section]
}

// ═══ 数据加载 ═══
async function fetchHealthStatus() {
  try {
    const res: any = await request.get('/monitor/health/status')
    // ⚠️ 不做 `|| 'UP'` / `|| 100` 兜底：后端 DOWN 时 healthScore=0 是合法值，兜底会把宕机显示成健康
    healthStatus.value = res?.status ?? null
    healthScore.value = numOrNull(res?.healthScore)
    serviceName.value = res?.serviceName ?? null
    const comps = res?.components
    components.value = comps && typeof comps === 'object' && Object.keys(comps).length ? comps : null
    clearError('status')
  } catch (error: any) {
    healthStatus.value = null
    healthScore.value = null
    serviceName.value = null
    components.value = null
    markError('status', '综合健康状态', error)
  }
}

async function fetchJvmHealth() {
  try {
    const res: any = await request.get('/monitor/health/jvm')
    jvmStatus.value = res?.status ?? null
    jvmHeapUsed.value = numOrNull(res?.heapMemory?.used)
    jvmHeapMax.value = numOrNull(res?.heapMemory?.max)
    jvmHeapUsage.value = numOrNull(res?.heapMemory?.usagePercent)
    jvmThreadCount.value = numOrNull(res?.threads?.threadCount)
    jvmDeadlockedCount.value = numOrNull(res?.threads?.deadlockedThreadCount)
    clearError('jvm')
  } catch (error: any) {
    jvmStatus.value = null
    jvmHeapUsed.value = null
    jvmHeapMax.value = null
    jvmHeapUsage.value = null
    jvmThreadCount.value = null
    jvmDeadlockedCount.value = null
    markError('jvm', 'JVM 运行状态', error)
  }
}

async function fetchDiskHealth() {
  try {
    const res: any = await request.get('/monitor/health/disk')
    const list = Array.isArray(res?.disks) ? res.disks : []
    disks.value = list.map((d: any) => ({
      ...d,
      totalSpace: formatBytes(numOrNull(d?.totalSpace)),
      usedSpace: formatBytes(numOrNull(d?.usedSpace)),
      freeSpace: formatBytes(numOrNull(d?.freeSpace)),
      // 后端 status 为三值 UP / WARNING / CRITICAL，原实现只认 UP 会把 WARNING 显示成「异常」
      status: d?.status ?? null,
      usagePercent: numOrNull(d?.usagePercent) ?? 0,
    }))
    diskStatus.value = res?.status ?? null
    clearError('disk')
  } catch (error: any) {
    disks.value = []
    diskStatus.value = null
    markError('disk', '磁盘状态', error)
  }
}

async function fetchDependencies() {
  try {
    const res: any = await request.get('/monitor/health/dependencies')
    const raw = res?.dependencies
    dependencies.value = raw && typeof raw === 'object'
      ? Object.entries(raw).map(([key, val]: [string, any]) => ({
        key,
        name: val?.name || key,
        status: val?.status ?? null,
      }))
      : []
    dependencySummary.value = res?.status ?? null
    clearError('dependencies')
  } catch (error: any) {
    dependencies.value = []
    dependencySummary.value = null
    markError('dependencies', '依赖服务状态', error)
  }
}

async function fetchDatabaseHealth() {
  try {
    const res: any = await request.get('/monitor/health/database')
    dbHealth.value = {
      status: res?.status ?? 'UNKNOWN',
      responseTimeMs: numOrNull(res?.responseTimeMs),
      databaseProductName: res?.databaseProductName,
      databaseProductVersion: res?.databaseProductVersion,
      error: res?.error,
    }
    clearError('database')
  } catch (error: any) {
    dbHealth.value = null
    markError('database', '数据库探针', error)
  }
}

async function fetchReadyProbe() {
  try {
    const res: any = await request.get('/monitor/health/ready')
    readyProbe.value = {
      ready: !!res?.ready,
      status: res?.status ?? 'UNKNOWN',
      checks: Array.isArray(res?.checks) ? res.checks : [],
    }
    clearError('ready')
  } catch (error: any) {
    readyProbe.value = null
    markError('ready', '就绪探针', error)
  }
}

async function fetchLiveProbe() {
  try {
    const res: any = await request.get('/monitor/health/live')
    liveProbe.value = { alive: !!res?.alive, status: res?.status ?? 'UNKNOWN' }
    clearError('live')
  } catch (error: any) {
    liveProbe.value = null
    markError('live', '存活探针', error)
  }
}

async function refreshAll() {
  loading.value = true
  // 各 fetch 自带 catch，单个失败不影响其余区块
  await Promise.all([
    fetchHealthStatus(),
    fetchJvmHealth(),
    fetchDiskHealth(),
    fetchDependencies(),
    fetchDatabaseHealth(),
    fetchReadyProbe(),
    fetchLiveProbe(),
  ])
  lastRefresh.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  loading.value = false
}

function handleError(error: Error) {
  console.error('[服务状态] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  refreshAll()
  // 30 秒自动刷新（与后端无交互参数，纯前端轮询周期）
  timer = setInterval(refreshAll, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  timer = null
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; }
.page-header-left { display: flex; align-items: center; gap: 12px; }
.page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header-right { display: flex; align-items: center; }
.auto-refresh-hint { font-size: 12px; color: #999; }

/* 仪表盘内容区：PageContainer 的 body 是 overflow:hidden，滚动必须由这一层自己承担 */
.dashboard-scroll { flex: 1; min-height: 0; overflow-y: auto; padding: 12px 16px 16px; background: #f5f5f5; }
.section-alert { margin-bottom: 12px; }
.block-row { margin-bottom: 16px; }
.title-hint { color: #bfbfbf; margin-left: 4px; }
.muted { color: #999; font-size: 12px; line-height: 1.6; }
.dep-note { font-size: 12px; color: #999; line-height: 1.5; margin-top: 2px; }
.summary-note { display: block; margin-top: 12px; }

.components-block { margin-top: 16px; padding-top: 12px; border-top: 1px dashed #f0f0f0; }
.components-title { font-size: 13px; color: #666; margin-bottom: 8px; }
.component-item { margin-right: 16px; }
.component-name { font-size: 12px; color: #666; }

/* BillDetailTable 根元素 flex:1，父级必须是有确定高度的 flex 纵向容器 */
.dash-table { height: 280px; display: flex; flex-direction: column; }

.probe-line { font-size: 12px; color: #666; line-height: 1.8; word-break: break-all; }
.probe-error { color: #ff4d4f; }
</style>
