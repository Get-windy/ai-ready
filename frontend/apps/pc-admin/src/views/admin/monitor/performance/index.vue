<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        性能监控（系统 → 系统监控 → 性能监控，菜单 62202）
        · 平台控制台页面：ql361 无对标 → 按 JDK MXBean + Spring Boot Actuator metrics 能力模型建模
        · 仪表盘形态（卡片 + 小表格），不是台账列表 → 不套 CategoryListLayout；页内表格用 BillDetailTable 以获得表头列配置齿轮
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/性能监控开发文档.md

        ═══ 数据真实性 ═══
        全部指标来自 JDK MXBean 实算（PerformanceMetricsController.java 的 collectXxxMetrics 系列），无 mock、无随机数：
          OperatingSystemMXBean → cpu{usage, systemUsage, processUsage, cores, loadAverage}
          MemoryMXBean          → memory{usage, heap, nonHeap}
          GarbageCollectorMXBean→ gc{collectors[], gcTimePercent}
          ThreadMXBean          → threads{count, deadlockedCount}
          ClassLoadingMXBean    → classLoading
          CompilationMXBean     → compilation
        ⚠️ 指标历史保存在后端进程内存（metricsHistory，MAX_HISTORY_SIZE=10000），**重启即丢、多实例不共享**
          → /aggregate、/trend、/predict 在服务刚重启时多半返回空/不足，属正常现象，页面如实提示。

        ═══ 本轮修复（只改前端）═══
        ① 外壳补 ErrorBoundary；
        ② 🔴 删除「系统CPU使用率 / 进程CPU使用率」的二次 ×100：
           后端 collectCpuMetrics 已执行 `getSystemCpuLoad() * 100` / `getProcessCpuLoad() * 100`
           （PerformanceMetricsController.java:371-373），前端原实现又乘一次 → 显示约 100 倍假值（12.5% 显示成 1250%）。
           现直接取后端交付值，与 cpu.usage 口径一致（:371 未再乘，可作对照）。
        ③ 🔴 所有指标初值由 0 改 null：原「全 0 初值 + 静默吞错」会让接口失败时显示成「零负载」假健康；
           现在失败/未取到一律显示「—」，并顶部汇总错误横幅 + message.error；
        ④ 4 处 `catch { /* ignore */ }` → console.error + message.error + 对应区块置「—」；
        ⑤ 接入后端 4 个已实现但前端未用的端点中的 3 个：
           /aggregate（窗口聚合均值/最大/最小）、/trend/{metricType}（折线图）、/predict/{metricType}（线性外推预测）；
           /compare（两时间段对比）未接 —— 其 window2 依赖进程内历史，服务重启后恒为空窗口，
           当前内存态下对比结果无参考意义，故不展示（避免用无意义数据充数）。
        ⑥ 页内新增「统计窗口 / 监控指标 / 刷新间隔」三项控制：
           windowMinutes → /aggregate?windowMinutes=（后端默认 60）
           metricType    → /trend/{metricType}、/predict/{metricType}（必须「分类.字段」点号格式，
                           见 PerformanceMetricsController.java:533-545 extractMetricValue）
           minutes       → /trend/{metricType}?minutes=（后端默认 60）
           刷新间隔为纯前端 setInterval 周期，后端无此参数，不要往接口上送。
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
              <a-breadcrumb-item>性能监控</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-header-title">
              性能监控
            </h2>
          </div>
          <div class="page-header-right">
            <a-space :size="8">
              <span class="control-label">统计窗口</span>
              <a-select
                v-model:value="windowMinutes"
                size="small"
                style="width: 110px"
                :options="WINDOW_OPTIONS"
              />
              <span class="control-label">监控指标</span>
              <a-select
                v-model:value="metricType"
                size="small"
                style="width: 170px"
                :options="METRIC_OPTIONS"
              />
              <span class="control-label">刷新间隔</span>
              <a-select
                v-model:value="refreshInterval"
                size="small"
                style="width: 120px"
                :options="REFRESH_OPTIONS"
              />
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
            message="部分性能指标加载失败，对应区块已置为「—」"
            :description="`失败区块：${errorSectionNames}。请检查 core-api（5655）是否可用或稍后重试。`"
          />

          <a-alert
            class="section-alert"
            type="info"
            show-icon
            message="指标历史保存在后端进程内存（MAX_HISTORY_SIZE=10000），服务重启即清空、多实例不共享 —— 趋势/聚合/预测在重启后需要累积采样点才有意义。"
          />

          <!-- ═══ 4 张核心指标卡（金标准统计卡：null → 「—」，不显示 0 假值） ═══ -->
          <ARStatCards :items="metricCards" />

          <!-- ═══ CPU 详情 + GC 详情 ═══ -->
          <a-row
            :gutter="16"
            class="block-row"
          >
            <a-col :span="12">
              <a-card :bordered="false">
                <template #title>
                  CPU 详情
                  <a-tooltip title="OperatingSystemMXBean 实算值。systemUsage / processUsage 由后端已乘 100 后返回（PerformanceMetricsController.java:372-373），前端不得再次放大。">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                </template>
                <a-descriptions
                  :column="1"
                  size="small"
                >
                  <a-descriptions-item label="CPU核心数">
                    {{ cpuCores ?? '—' }}
                  </a-descriptions-item>
                  <a-descriptions-item label="系统负载">
                    <!-- Windows 上 getSystemLoadAverage() 恒返回 -1（不支持），此时显示「—」而不是负数 -->
                    {{ cpuLoadAverageText }}
                  </a-descriptions-item>
                  <a-descriptions-item label="系统CPU使用率">
                    {{ cpuSystemUsageText }}
                  </a-descriptions-item>
                  <a-descriptions-item label="进程CPU使用率">
                    {{ cpuProcessUsageText }}
                  </a-descriptions-item>
                </a-descriptions>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card :bordered="false">
                <template #title>
                  GC 详情
                  <a-tooltip title="GarbageCollectorMXBean 累计值。GC 时间占比 = 累计 GC 耗时 / JVM 运行时长（后端简化口径，PerformanceMetricsController.java:433）。">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                </template>
                <!-- BillDetailTable 根元素 flex:1，父级必须是有确定高度的 flex 纵向容器 -->
                <div class="dash-table">
                  <BillDetailTable
                    v-model:data-source="gcCollectors"
                    :columns="gcColumns"
                    :view-mode="true"
                    :min-rows="0"
                    empty-text="GC 数据不可用"
                    row-key="name"
                    storage-key="system-monitor-performance-table-columns"
                    global-config-key="system-monitor-performance-table-columns"
                  />
                </div>
              </a-card>
            </a-col>
          </a-row>

          <!-- ═══ 性能趋势（真实接入 /trend/{metricType}） ═══ -->
          <a-row
            :gutter="16"
            class="block-row"
          >
            <a-col :span="24">
              <a-card :bordered="false">
                <template #title>
                  性能趋势
                  <a-tooltip :title="`GET /api/monitor/performance/trend/${metricType}?minutes=${windowMinutes}，采样点来自后端进程内存。数值单位与上方指标一致（百分比类指标为 0~100）。`">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                </template>
                <template #extra>
                  <a-space :size="8">
                    <a-tag v-if="trendSampleCount !== null">
                      采样 {{ trendSampleCount }} 点
                    </a-tag>
                    <a-tag :color="TREND_MAP[trendDirection ?? '']?.color || 'default'">
                      {{ TREND_MAP[trendDirection ?? '']?.label || '未获取' }}
                    </a-tag>
                  </a-space>
                </template>
                <ARReportChart
                  :option="trendOption"
                  :height="260"
                  :loading="loading"
                  :empty-text="trendEmptyText"
                />
              </a-card>
            </a-col>
          </a-row>

          <!-- ═══ 窗口聚合 + 预测 ═══ -->
          <a-row
            :gutter="16"
            class="block-row"
          >
            <a-col :span="24">
              <a-card :bordered="false">
                <template #title>
                  窗口聚合与预测（过去 {{ windowMinutes }} 分钟）
                  <a-tooltip :title="`GET /api/monitor/performance/aggregate?windowMinutes=${windowMinutes} 与 GET /api/monitor/performance/predict/${metricType}?predictMinutes=30`">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                </template>
                <template #extra>
                  <a-tag v-if="aggregateUnavailable">
                    该窗口暂无采样
                  </a-tag>
                </template>
                <a-descriptions
                  :column="4"
                  size="small"
                  bordered
                >
                  <a-descriptions-item label="采样点">
                    {{ aggregate?.sampleCount ?? '—' }}
                  </a-descriptions-item>
                  <a-descriptions-item label="CPU 使用率 均值/最大/最小">
                    {{ aggText(aggregate?.cpu, 'avgUsage') }} / {{ aggText(aggregate?.cpu, 'maxUsage') }} / {{ aggText(aggregate?.cpu, 'minUsage') }}
                  </a-descriptions-item>
                  <a-descriptions-item label="内存使用率 均值/最大/最小">
                    {{ aggText(aggregate?.memory, 'avgUsage') }} / {{ aggText(aggregate?.memory, 'maxUsage') }} / {{ aggText(aggregate?.memory, 'minUsage') }}
                  </a-descriptions-item>
                  <a-descriptions-item label="GC 时间占比 均值/最大">
                    {{ aggText(aggregate?.gc, 'avgGcTimePercent', 3) }} / {{ aggText(aggregate?.gc, 'maxGcTimePercent', 3) }}
                  </a-descriptions-item>
                </a-descriptions>
                <div class="predict-line">
                  <span class="control-label">线性外推预测（+30 分钟）：</span>
                  <span>{{ predictText }}</span>
                  <a-tooltip title="后端为简单线性回归（PerformanceMetricsController.java:198「简单线性回归预测」、:564-584），不是机器学习模型；采样点少于 10 个时后端直接返回「数据不足」，页面不臆造预测值。">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                  <a-tag
                    v-if="predict?.alert"
                    color="red"
                    class="predict-alert"
                  >
                    {{ predict.alert }}
                  </a-tag>
                </div>
              </a-card>
            </a-col>
          </a-row>

          <!-- ═══ 性能瓶颈分析 ═══ -->
          <a-row
            :gutter="16"
            class="block-row"
          >
            <a-col :span="24">
              <a-card :bordered="false">
                <template #title>
                  性能瓶颈分析
                  <a-tooltip title="GET /api/monitor/performance/bottleneck，现算 MXBean 与后端硬编码阈值比对，结论不落库（sys_alert_history 无新增）。">
                    <QuestionCircleOutlined class="title-hint" />
                  </a-tooltip>
                </template>
                <template #extra>
                  <a-space :size="8">
                    <span class="muted">死锁线程：{{ threadDeadlockedCount ?? '—' }}</span>
                    <a-tag :color="BOTTLENECK_MAP[bottleneckStatus ?? '']?.color || 'default'">
                      {{ BOTTLENECK_MAP[bottleneckStatus ?? '']?.label || '未获取' }}
                    </a-tag>
                  </a-space>
                </template>
                <a-empty
                  v-if="bottleneckStatus === null"
                  description="瓶颈分析数据不可用"
                />
                <a-empty
                  v-else-if="!bottlenecks.length"
                  description="未检测到性能瓶颈"
                />
                <a-list
                  v-else
                  :data-source="bottlenecks"
                  size="small"
                >
                  <template #renderItem="{ item }">
                    <a-list-item>
                      <a-list-item-meta>
                        <template #title>
                          <a-tag :color="item.severity === 'CRITICAL' ? 'red' : 'orange'">
                            {{ item.type }}
                          </a-tag>
                          {{ item.recommendation }}
                        </template>
                        <template #description>
                          当前值: {{ item.currentValue }} | 触发阈值: {{ item.threshold }}
                        </template>
                      </a-list-item-meta>
                    </a-list-item>
                  </template>
                </a-list>
                <!-- 阈值口径：后端只返回触发侧阈值，另一档在此如实补齐（两档都是后端硬编码常量，不可配置） -->
                <div class="threshold-note">
                  阈值口径（后端硬编码常量，不可配置，PerformanceMetricsController.java:237-284）：
                  警告档 CPU 80% / 内存 85% / GC 时间占比 10%；
                  严重档 CPU 95% / 内存 95% / GC 时间占比 20%；线程死锁数 &gt; 0 直接判定为严重。
                </div>
              </a-card>
            </a-col>
          </a-row>

          <!-- ═══ 类加载 & 编译 ═══ -->
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card :bordered="false">
                <template #title>
                  类加载信息
                </template>
                <a-descriptions
                  :column="1"
                  size="small"
                >
                  <a-descriptions-item label="已加载类数">
                    {{ classLoadedCount ?? '—' }}
                  </a-descriptions-item>
                  <a-descriptions-item label="累计加载类数">
                    {{ classTotalLoadedCount ?? '—' }}
                  </a-descriptions-item>
                  <a-descriptions-item label="已卸载类数">
                    {{ classUnloadedCount ?? '—' }}
                  </a-descriptions-item>
                </a-descriptions>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card :bordered="false">
                <template #title>
                  编译信息
                </template>
                <a-descriptions
                  :column="1"
                  size="small"
                >
                  <a-descriptions-item label="编译器">
                    {{ compilationName || '—' }}
                  </a-descriptions-item>
                  <a-descriptions-item label="编译总耗时">
                    {{ compilationTime !== null ? `${compilationTime} ms` : '—' }}
                  </a-descriptions-item>
                </a-descriptions>
              </a-card>
            </a-col>
          </a-row>
        </a-spin>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { StatCardItem } from '@/components/ARReportPage/types'
import request from '@/utils/request'

defineOptions({ name: 'AdminMonitorPerformance' })

// ═══ 枚举 / 选项 ═══
/** 统计窗口可选值（分钟）→ /aggregate?windowMinutes= 与 /trend?minutes=，均为后端已有参数，未发明 */
const WINDOW_OPTIONS = [
  { label: '15 分钟', value: 15 },
  { label: '30 分钟', value: 30 },
  { label: '60 分钟', value: 60 },
  { label: '180 分钟', value: 180 },
]

/**
 * 监控指标 → /trend/{metricType} 与 /predict/{metricType} 的路径参数。
 * ⚠️ 必须是「分类.字段」点号格式：后端 extractMetricValue 按 '.' 拆分两级取值
 * （PerformanceMetricsController.java:533-545），只传 'cpu' 会取不到值恒为 0。
 */
const METRIC_OPTIONS = [
  { label: 'CPU 使用率', value: 'cpu.usage' },
  { label: '系统 CPU 使用率', value: 'cpu.systemUsage' },
  { label: '进程 CPU 使用率', value: 'cpu.processUsage' },
  { label: '内存使用率', value: 'memory.usage' },
  { label: 'GC 时间占比', value: 'gc.gcTimePercent' },
]

/** 刷新间隔 = 纯前端轮询周期（后端无此参数）；0 = 不自动刷新 */
const REFRESH_OPTIONS = [
  { label: '不自动刷新', value: 0 },
  { label: '10 秒', value: 10000 },
  { label: '30 秒', value: 30000 },
  { label: '60 秒', value: 60000 },
]

const BOTTLENECK_MAP: Record<string, { label: string; color: string }> = {
  HEALTHY: { label: '健康', color: 'green' },
  WARNING: { label: '警告', color: 'orange' },
  CRITICAL: { label: '严重', color: 'red' },
}

const TREND_MAP: Record<string, { label: string; color: string }> = {
  increasing: { label: '趋势：上升', color: 'red' },
  decreasing: { label: '趋势：下降', color: 'green' },
  stable: { label: '趋势：平稳', color: 'blue' },
}

// ═══ 控制项 ═══
const windowMinutes = ref(60)
const metricType = ref('cpu.usage')
const refreshInterval = ref(30000)

// ═══ 数据状态（一律 null = 未取到 → 页面显示「—」，绝不用 0 兜底成「零负载」假象）═══
const loading = ref(false)

// CPU（后端已 ×100，前端直接使用，不得再乘）
const cpuUsage = ref<number | null>(null)
const cpuCores = ref<number | null>(null)
const cpuLoadAverage = ref<number | null>(null)
const cpuSystemUsage = ref<number | null>(null)
const cpuProcessUsage = ref<number | null>(null)

// 内存
const memUsage = ref<number | null>(null)

// GC
const gcTimePercent = ref<number | null>(null)
const gcCollectors = ref<any[]>([])

// 线程
const threadCount = ref<number | null>(null)
const threadDeadlockedCount = ref<number | null>(null)

// 瓶颈
const bottlenecks = ref<any[]>([])
const bottleneckStatus = ref<string | null>(null)

// 类加载 / 编译
const classLoadedCount = ref<number | null>(null)
const classTotalLoadedCount = ref<number | null>(null)
const classUnloadedCount = ref<number | null>(null)
const compilationName = ref<string | null>(null)
const compilationTime = ref<number | null>(null)

// 分析（趋势 / 聚合 / 预测）
const trend = ref<{ timestamps: string[]; values: number[]; trend?: string | null; min?: number | null; max?: number | null; avg?: number | null } | null>(null)
const aggregate = ref<any | null>(null)
const predict = ref<any | null>(null)

/** 各区块加载失败信息（key → 区块中文名）；非空即展示顶部错误横幅 */
const sectionErrors = reactive<Record<string, string>>({})
const hasError = computed(() => Object.keys(sectionErrors).length > 0)
const errorSectionNames = computed(() => Object.values(sectionErrors).join('、'))

// ═══ 表格列（rowNo 列承载表头「列配置」齿轮）═══
const gcColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'name', title: '名称', type: 'input', width: 220 },
  { key: 'count', title: 'GC次数', type: 'input', width: 110 },
  { key: 'time', title: '总耗时(ms)', type: 'input', width: 130 },
]

// ═══ 展示计算 ═══
function pct(v: number | null | undefined, digits = 1): string {
  if (v === null || v === undefined || Number.isNaN(v)) return '—'
  return v.toFixed(digits) + '%'
}

const cpuSystemUsageText = computed(() => pct(cpuSystemUsage.value))
const cpuProcessUsageText = computed(() => pct(cpuProcessUsage.value))
const cpuLoadAverageText = computed(() => {
  const v = cpuLoadAverage.value
  // Windows 平台 getSystemLoadAverage() 恒为 -1（不支持该指标），显示「—」而非负数
  if (v === null || v < 0) return '—'
  return v.toFixed(2)
})

const metricCards = computed<StatCardItem[]>(() => {
  const build = (label: string, value: number | null, threshold: number, digits = 1): StatCardItem => {
    if (value === null) {
      return { label, value: '—', suffix: '', valueStyle: { color: '#bfbfbf' } }
    }
    return {
      label,
      value,
      precision: digits,
      suffix: '%',
      valueStyle: { color: value > threshold ? '#ff4d4f' : '#52c41a' },
    }
  }
  return [
    build('CPU使用率', cpuUsage.value, 80),
    build('内存使用率', memUsage.value, 85),
    build('GC时间占比', gcTimePercent.value, 10, 2),
    {
      label: '活跃线程',
      value: threadCount.value === null ? '—' : threadCount.value,
      valueStyle: { color: threadCount.value !== null && threadCount.value > 200 ? '#ff4d4f' : threadCount.value === null ? '#bfbfbf' : '#52c41a' },
    },
  ]
})

const trendSampleCount = computed(() => (trend.value?.timestamps?.length ?? null))
const trendDirection = computed(() => trend.value?.trend ?? null)
const aggregateUnavailable = computed(() => !!aggregate.value && aggregate.value?.sampleCount === undefined)
const trendEmptyText = computed(() =>
  trendDirection.value === null
    ? '趋势数据不可用（接口未返回或该时间窗口无采样）'
    : '该时间窗口暂无采样数据，请等待后端累积采样点（历史保存在进程内存，重启即清空）',
)

function aggText(node: any, key: string, digits = 2): string {
  const v = node?.[key]
  if (v === null || v === undefined || Number.isNaN(Number(v))) return '—'
  return Number(v).toFixed(digits)
}

const predictText = computed(() => {
  const p = predict.value
  if (!p || p.predictedValue === undefined || p.predictedValue === null) {
    // 后端采样点少于 10 个时只返回 message，不返回预测值 → 如实说明，不臆造
    return p?.message ? '—（后端返回「数据不足」，至少需 10 个采样点）' : '—（预测数据不可用）'
  }
  const cur = Number(p.currentValue)
  const pre = Number(p.predictedValue)
  const conf = p.confidence === null || p.confidence === undefined ? null : Number(p.confidence) * 100
  return `${Number.isFinite(cur) ? cur.toFixed(2) : '—'} → ${Number.isFinite(pre) ? pre.toFixed(2) : '—'}`
    + `（置信度 ${conf === null ? '—' : conf.toFixed(0) + '%'}，线性外推，仅供容量规划参考）`
})

const trendOption = computed(() => {
  const t = trend.value
  if (!t?.timestamps?.length || !t.values?.length) return null
  const metricLabel = METRIC_OPTIONS.find(o => o.value === metricType.value)?.label || metricType.value
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 56, right: 20, top: 28, bottom: 40 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: t.timestamps.map(ts => formatClock(ts)),
    },
    yAxis: {
      type: 'value',
      name: metricLabel,
      nameTextStyle: { fontSize: 11, color: '#999' },
      scale: true,
    },
    series: [{
      name: metricLabel,
      type: 'line',
      smooth: true,
      showSymbol: t.values.length <= 30,
      data: t.values,
      areaStyle: { opacity: 0.12 },
    }],
  }
})

function formatClock(ts: string): string {
  // 后端时间为 LocalDateTime 字符串（如 2026-09-18T23:19:30.263323800）→ 只取时分秒
  const m = String(ts).match(/T(\d{2}:\d{2}:\d{2})/)
  return m ? m[1] : String(ts).slice(11, 19)
}

let timer: ReturnType<typeof setInterval> | null = null

// ═══ 工具函数 ═══
function numOrNull(v: any): number | null {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

/** 统一失败处理：console.error + message.error（同 key 去重，避免轮询刷屏）+ 区块错误态 */
function markError(section: string, label: string, error: any) {
  console.error(`[性能监控] ${label}加载失败`, error)
  sectionErrors[section] = label
  message.error({
    content: `${label}加载失败：${error?.message || '请求异常'}`,
    key: `monitor-performance-${section}`,
    duration: 4,
  })
}

function clearError(section: string) {
  delete sectionErrors[section]
}

// ═══ 数据加载 ═══
async function fetchRealtimeMetrics() {
  try {
    const res: any = await request.get('/monitor/performance/realtime')

    const cpu = res?.cpu
    if (cpu) {
      // ⚠️ 后端已 ×100（:371-373），此处直接使用，切勿再 * 100（否则显示放大 100 倍）
      cpuUsage.value = numOrNull(cpu.usage)
      cpuCores.value = numOrNull(cpu.cores)
      cpuLoadAverage.value = numOrNull(cpu.loadAverage)
      cpuSystemUsage.value = numOrNull(cpu.systemUsage)
      cpuProcessUsage.value = numOrNull(cpu.processUsage)
    } else {
      cpuUsage.value = null
      cpuCores.value = null
      cpuLoadAverage.value = null
      cpuSystemUsage.value = null
      cpuProcessUsage.value = null
    }

    memUsage.value = numOrNull(res?.memory?.usage)

    const gc = res?.gc
    gcTimePercent.value = numOrNull(gc?.gcTimePercent)
    gcCollectors.value = Array.isArray(gc?.collectors)
      ? gc.collectors.map((c: any) => ({
        name: c?.name ?? '—',
        count: numOrNull(c?.count),
        time: numOrNull(c?.time),
      }))
      : []

    threadCount.value = numOrNull(res?.threads?.count)
    threadDeadlockedCount.value = numOrNull(res?.threads?.deadlockedCount)

    classLoadedCount.value = numOrNull(res?.classLoading?.loadedClassCount)
    classTotalLoadedCount.value = numOrNull(res?.classLoading?.totalLoadedClassCount)
    classUnloadedCount.value = numOrNull(res?.classLoading?.unloadedClassCount)

    compilationName.value = res?.compilation?.name ?? null
    compilationTime.value = numOrNull(res?.compilation?.totalCompilationTime)

    clearError('realtime')
  } catch (error: any) {
    // 失败即置「—」，不保留旧值也不回落 0（旧值会让人误以为是最新数据）
    cpuUsage.value = null
    cpuCores.value = null
    cpuLoadAverage.value = null
    cpuSystemUsage.value = null
    cpuProcessUsage.value = null
    memUsage.value = null
    gcTimePercent.value = null
    gcCollectors.value = []
    threadCount.value = null
    threadDeadlockedCount.value = null
    classLoadedCount.value = null
    classTotalLoadedCount.value = null
    classUnloadedCount.value = null
    compilationName.value = null
    compilationTime.value = null
    markError('realtime', '实时性能指标', error)
  }
}

async function fetchBottleneck() {
  try {
    const res: any = await request.get('/monitor/performance/bottleneck')
    bottlenecks.value = Array.isArray(res?.bottlenecks) ? res.bottlenecks : []
    bottleneckStatus.value = res?.overallStatus ?? null
    clearError('bottleneck')
  } catch (error: any) {
    bottlenecks.value = []
    bottleneckStatus.value = null
    markError('bottleneck', '性能瓶颈分析', error)
  }
}

async function fetchAggregate() {
  try {
    // 参数名 windowMinutes 来自后端 @RequestParam(defaultValue = "60")（:70-74），未发明
    const res: any = await request.get('/monitor/performance/aggregate', {
      windowMinutes: windowMinutes.value,
    })
    aggregate.value = res && Object.keys(res).length ? res : null
    clearError('aggregate')
  } catch (error: any) {
    aggregate.value = null
    markError('aggregate', '窗口聚合', error)
  }
}

async function fetchTrend() {
  try {
    // minutes 来自后端 @RequestParam(defaultValue = "60")（:115-119），未发明
    const res: any = await request.get(`/monitor/performance/trend/${metricType.value}`, {
      minutes: windowMinutes.value,
    })
    const timestamps = Array.isArray(res?.timestamps) ? res.timestamps : []
    const values = Array.isArray(res?.values) ? res.values : []
    trend.value = timestamps.length
      ? {
        timestamps,
        values,
        trend: res?.trend ?? null,
        min: numOrNull(res?.min),
        max: numOrNull(res?.max),
        avg: numOrNull(res?.avg),
      }
      : null
    clearError('trend')
  } catch (error: any) {
    trend.value = null
    markError('trend', '性能趋势', error)
  }
}

async function fetchPredict() {
  try {
    // predictMinutes 来自后端 @RequestParam(defaultValue = "30")（:171-175），显式上送以与页面文案「+30 分钟」一致
    const res: any = await request.get(`/monitor/performance/predict/${metricType.value}`, {
      predictMinutes: 30,
    })
    predict.value = res && Object.keys(res).length ? res : null
    clearError('predict')
  } catch (error: any) {
    predict.value = null
    markError('predict', '性能预测', error)
  }
}

async function refreshAll() {
  loading.value = true
  // 各 fetch 自带 catch，单个失败不影响其余区块
  await Promise.all([
    fetchRealtimeMetrics(),
    fetchBottleneck(),
    fetchAggregate(),
    fetchTrend(),
    fetchPredict(),
  ])
  loading.value = false
}

function handleError(error: Error) {
  console.error('[性能监控] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 定时器（纯前端轮询周期）═══
function restartTimer() {
  if (timer) { clearInterval(timer); timer = null }
  if (refreshInterval.value > 0) {
    timer = setInterval(refreshAll, refreshInterval.value)
  }
}

watch(refreshInterval, restartTimer)

// 统计窗口同时影响 /aggregate 与 /trend；指标只影响 /trend、/predict
watch(windowMinutes, () => {
  void fetchAggregate()
  void fetchTrend()
  void fetchPredict()
})
watch(metricType, () => {
  void fetchTrend()
  void fetchPredict()
})

onMounted(() => {
  refreshAll()
  restartTimer()
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
.control-label { font-size: 12px; color: #666; }
.muted { font-size: 12px; color: #999; }

/* 仪表盘内容区：PageContainer 的 body 是 overflow:hidden，滚动必须由这一层自己承担 */
.dashboard-scroll { flex: 1; min-height: 0; overflow-y: auto; padding: 12px 16px 16px; background: #f5f5f5; }
.section-alert { margin-bottom: 12px; }
.block-row { margin-bottom: 16px; }
.title-hint { color: #bfbfbf; margin-left: 4px; }

/* BillDetailTable 根元素 flex:1，父级必须是有确定高度的 flex 纵向容器 */
.dash-table { height: 200px; display: flex; flex-direction: column; }

.predict-line { margin-top: 12px; font-size: 13px; color: #333; }
.predict-alert { margin-left: 8px; }
.threshold-note { margin-top: 12px; padding-top: 8px; border-top: 1px dashed #f0f0f0; font-size: 12px; color: #999; line-height: 1.7; }
</style>
