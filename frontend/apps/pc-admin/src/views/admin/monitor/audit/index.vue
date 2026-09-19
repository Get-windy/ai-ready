<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        操作审计（系统 → 系统监控 → 操作审计，菜单 62205）
        · 平台控制台页面：ql361 无对标 → 按 SAP SAL「安全审计日志（Security Audit Log / SM20）」能力模型建模
        · 只读审计台账：查询区 3 项（审计类型/模块/时间范围）+ 顶部统计卡 + 全字段台账表
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/操作审计开发文档.md
        · 🔴 本轮修复（只改前端）：
          ① 4 处字段名与后端实体 cn.aiedge.audit.model.AuditLog 错配
             （userName→username / status→result / ipAddress→operIp / createTime→operTime），
             原实现导致有数据时「每一行都显示红色失败」且操作用户、IP 恒空；
          ② 统计卡读 failCount → 后端真实键为 failureCount（且数值是字符串，需 Number 归一）；
          ③ fetchData 无 catch → 补 catch（console.error + message.error + 清空，不做假数据兜底）；
          ④ 审计类型/模块下拉由硬编码大写改为「后端枚举 + 实测值域」，修复筛选恒 0 行、tag 恒灰；
          ⑤ 行内「详情」原为纯前端 → 改为真实调用 GET /api/audit/detail/{logId}，抽屉内展示全部 24 个字段。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：导出（真实调用 GET /api/audit/export） ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('export')"
            size="small"
            :loading="exporting"
            @click="handleExport"
          >
            <DownloadOutlined /> 导出
          </a-button>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              title="页面配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（审计类型 / 模块 / 时间范围，横向 flex 自适应） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('auditType')">
                <span class="search-label">审计类型</span>
                <a-select
                  v-model:value="searchForm.auditType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  :options="auditTypeOptions"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('module')">
                <span class="search-label">模块</span>
                <a-select
                  v-model:value="searchForm.module"
                  placeholder="全部模块"
                  size="small"
                  style="width: 160px"
                  allow-clear
                  show-search
                  :filter-option="filterModuleOption"
                  :options="moduleOptions"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('timeRange')">
                <span class="search-label">时间范围</span>
                <a-range-picker
                  v-model:value="searchForm.timeRange"
                  size="small"
                  show-time
                  style="width: 340px"
                  @change="handleSearch"
                />
              </template>
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-button
                size="small"
                @click="handleReset"
              >
                重置
              </a-button>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（统计卡 + 台账，列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <!-- ── 审计统计（口径与查询区时间范围一致；后端不支持按类型/模块筛选统计） ── -->
            <a-row
              :gutter="12"
              class="stat-row"
            >
              <a-col :span="8">
                <a-card
                  size="small"
                  :bordered="false"
                  class="stat-card"
                >
                  <a-statistic
                    title="审计日志总数"
                    :value="stats.total"
                    :value-style="{ color: '#1677ff' }"
                  />
                </a-card>
              </a-col>
              <a-col :span="8">
                <a-card
                  size="small"
                  :bordered="false"
                  class="stat-card"
                >
                  <a-statistic
                    title="成功操作"
                    :value="stats.successCount"
                    :value-style="{ color: '#52c41a' }"
                  />
                </a-card>
              </a-col>
              <a-col :span="8">
                <a-card
                  size="small"
                  :bordered="false"
                  class="stat-card"
                >
                  <a-statistic
                    title="失败操作"
                    :value="stats.failureCount"
                    :value-style="{ color: '#ff4d4f' }"
                  />
                </a-card>
              </a-col>
            </a-row>

            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-audit-log-table-columns"
              global-config-key="system-audit-log-table-columns"
            >
              <!-- 审计类型（小写 code 着色，⚠️ 必须 type:'slot' 才能自定义渲染） -->
              <template #auditTypeCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="auditTypeColor(record.auditType)"
                >
                  {{ auditTypeLabel(record.auditType) }}
                </a-tag>
              </template>

              <!-- 操作结果（真实字段是 result，值域 SUCCESS/FAILURE） -->
              <template #resultCell="{ record }">
                <a-badge
                  v-if="!record.__ghost"
                  :status="record.result === 'SUCCESS' ? 'success' : 'error'"
                  :text="record.result === 'SUCCESS' ? '成功' : '失败'"
                />
              </template>

              <!-- 操作时间（真实业务时间字段是 operTime，非 createTime） -->
              <template #operTimeCell="{ record }">
                {{ fmtTime(record.operTime) }}
              </template>

              <!-- 记录时间（入库时间，默认隐藏） -->
              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <!-- 操作列：真实调用详情接口 -->
              <template #actionCell="{ record }">
                <a-button
                  v-if="!record.__ghost"
                  type="link"
                  size="small"
                  @click="openDetail(record)"
                >
                  详情
                </a-button>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏 ═══ -->
        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="PAGE_CONFIG_STORAGE_KEY"
        :hide-print-config="true"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />

      <!-- ═══ 审计日志详情抽屉（真实加载 GET /api/audit/detail/{logId}） ═══ -->
      <a-drawer
        v-model:open="detailVisible"
        title="审计日志详情"
        width="760"
        destroy-on-close
      >
        <a-spin :spinning="detailLoading">
          <!-- 详情加载失败：如实提示，不做假数据兜底 -->
          <a-alert
            v-if="detailError"
            type="error"
            show-icon
            :message="detailError"
            class="detail-alert"
          />
          <a-descriptions
            v-else-if="detailItem"
            :column="2"
            size="small"
            bordered
          >
            <a-descriptions-item label="日志ID">
              {{ detailItem.id ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="租户ID">
              {{ detailItem.tenantId ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="审计类型">
              {{ auditTypeLabel(detailItem.auditType) }}
            </a-descriptions-item>
            <a-descriptions-item label="操作结果">
              <a-badge
                :status="detailItem.result === 'SUCCESS' ? 'success' : 'error'"
                :text="detailItem.result === 'SUCCESS' ? '成功' : '失败'"
              />
            </a-descriptions-item>
            <a-descriptions-item label="操作模块">
              {{ detailItem.module || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作动作">
              {{ detailItem.action || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="对象类型">
              {{ detailItem.targetType || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="对象ID">
              {{ detailItem.targetId || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="对象名称">
              {{ detailItem.targetName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作用户">
              {{ detailItem.username || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作用户ID">
              {{ detailItem.userId ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作IP">
              {{ detailItem.operIp || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作地点">
              {{ detailItem.operLocation || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="操作时间">
              {{ fmtTime(detailItem.operTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="记录时间">
              {{ fmtTime(detailItem.createTime) }}
            </a-descriptions-item>
            <a-descriptions-item label="耗时">
              {{ detailItem.duration != null ? `${detailItem.duration} ms` : '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="请求方法">
              {{ detailItem.requestMethod || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="请求URL">
              {{ detailItem.requestUrl || '-' }}
            </a-descriptions-item>
            <a-descriptions-item
              label="错误信息"
              :span="2"
            >
              {{ detailItem.errorMsg || '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <!-- 变更前后值 / 快照：审计页最核心的信息（原实现完全看不到） -->
          <template v-if="detailItem">
            <div
              v-for="blk in JSON_BLOCKS"
              :key="blk.key"
              class="json-block"
            >
              <div class="json-block__title">
                {{ blk.label }}
              </div>
              <pre
                v-if="detailItem[blk.key]"
                class="json-block__pre"
              >{{ prettyJson(detailItem[blk.key]) }}</pre>
              <div
                v-else
                class="json-block__empty"
              >
                无
              </div>
            </div>
          </template>
        </a-spin>
      </a-drawer>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  DownloadOutlined,
  ReloadOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useExport } from '@/composables/useExport'
import request from '@/utils/request'

defineOptions({ name: 'AdminMonitorAudit' })

const PAGE_CONFIG_STORAGE_KEY = 'system-audit-log-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 枚举 ═══
/**
 * 审计类型值域：后端实体 AuditLog.AuditType 的 code 是**小写**（实测 /query?auditType=login 命中、
 * 大写 LOGIN 恒 0 行）→ 下拉 value 必须用小写，否则筛选恒 0。
 */
const AUDIT_TYPE_MAP: Record<string, { label: string; color: string }> = {
  login: { label: '登录', color: 'blue' },
  logout: { label: '登出', color: 'cyan' },
  create: { label: '创建', color: 'green' },
  update: { label: '更新', color: 'orange' },
  delete: { label: '删除', color: 'red' },
  export: { label: '导出', color: 'purple' },
  import: { label: '导入', color: 'geekblue' },
  access: { label: '访问', color: 'default' },
  grant: { label: '授权', color: 'gold' },
  other: { label: '其他', color: 'default' },
}

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  auditType: undefined as string | undefined,
  module: undefined as string | undefined,
  timeRange: undefined as any,
})

/** 统计卡（真实键：total / successCount / failureCount；后端数值序列化为字符串，取用时 Number 归一） */
const stats = reactive({ total: 0, successCount: 0, failureCount: 0 })

/**
 * 下拉选项：始终从「内部字典 + 实测值域」合并而来 ——
 * 只增不减，避免统计口径随时间范围变化导致已选值从下拉里消失。
 */
const auditTypeSeen = ref<string[]>(Object.keys(AUDIT_TYPE_MAP))
const moduleSeen = ref<string[]>([])

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'auditType', label: '审计类型', visible: true },
  { key: 'module', label: '模块', visible: true },
  { key: 'timeRange', label: '时间范围', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'export', label: '导出', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
]
const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

function isQueryFieldVisible(key: string) {
  return queryFields.value.find(f => f.key === key)?.visible !== false
}
function isButtonEnabled(key: string) {
  return functionButtons.value.find(f => f.key === key)?.enabled !== false
}
function handlePageConfigChange(config: PageConfig) {
  if (config?.queryFields?.length) queryFields.value = config.queryFields
  if (config?.functionButtons?.length) functionButtons.value = config.functionButtons
}

/** 审计类型下拉：字典项在前，实测新增值（无中文名）兜底在后 */
const auditTypeOptions = ref<{ label: string; value: string }[]>([])
/** 模块下拉：module 是自由文本，选项完全来自实测值（无 /api/audit/modules 端点） */
const moduleOptions = ref<{ label: string; value: string }[]>([])

function rebuildOptions() {
  auditTypeOptions.value = auditTypeSeen.value
    .filter(Boolean)
    .map(code => ({ label: AUDIT_TYPE_MAP[code]?.label || code, value: code }))
  moduleOptions.value = moduleSeen.value.filter(Boolean).map(m => ({ label: m, value: m }))
}
/** 模块下拉搜索（模块名可自由输入，show-search 需自定义过滤） */
function filterModuleOption(input: string, option: any) {
  return String(option?.label || '').toLowerCase().includes(String(input || '').toLowerCase())
}

// 序号列承载表头「列配置」齿轮；操作列为固定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'rowAction', title: '操作', type: 'action', slotName: 'actionCell', width: 80, fixed: 'left' },
  { key: 'id', title: '日志ID', type: 'input', width: 190 },
  // ⚠️ 必须写 type:'slot'：只写 slotName 会静默直出原值（true/2 之类）
  { key: 'auditType', title: '审计类型', type: 'slot', slotName: 'auditTypeCell', width: 100 },
  { key: 'module', title: '操作模块', type: 'input', width: 160 },
  { key: 'username', title: '操作用户', type: 'input', width: 140 },
  { key: 'result', title: '操作结果', type: 'slot', slotName: 'resultCell', width: 100 },
  { key: 'operTime', title: '操作时间', type: 'slot', slotName: 'operTimeCell', width: 170 },
  { key: 'operIp', title: '操作IP', type: 'input', width: 140 },
  { key: 'action', title: '操作动作', type: 'input', width: 120, defaultHidden: true },
  { key: 'userId', title: '操作用户ID', type: 'input', width: 130, defaultHidden: true },
  { key: 'operLocation', title: '操作地点', type: 'input', width: 140, defaultHidden: true },
  { key: 'targetType', title: '对象类型', type: 'input', width: 130, defaultHidden: true },
  { key: 'targetId', title: '对象ID', type: 'input', width: 150, defaultHidden: true },
  { key: 'targetName', title: '对象名称', type: 'input', width: 160, defaultHidden: true },
  { key: 'requestMethod', title: '请求方法', type: 'input', width: 100, defaultHidden: true },
  { key: 'requestUrl', title: '请求URL', type: 'input', width: 240, defaultHidden: true },
  { key: 'duration', title: '耗时(ms)', type: 'input', width: 100, defaultHidden: true },
  { key: 'errorMsg', title: '错误信息', type: 'input', width: 200, defaultHidden: true },
  { key: 'tenantId', title: '租户ID', type: 'input', width: 100, defaultHidden: true },
  { key: 'createTime', title: '记录时间', type: 'slot', slotName: 'createTimeCell', width: 170, defaultHidden: true },
]

/** 详情抽屉里按 JSON 美化展示的字段（审计核心信息：改了什么） */
const JSON_BLOCKS = [
  { key: 'beforeData', label: '变更前数据（beforeData）' },
  { key: 'afterData', label: '变更后数据（afterData）' },
  { key: 'dataSnapshot', label: '业务数据快照（dataSnapshot）' },
  { key: 'requestParams', label: '请求参数（requestParams）' },
  { key: 'responseData', label: '响应结果（responseData）' },
]

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'
}
function auditTypeLabel(code: string | null | undefined): string {
  if (!code) return '-'
  return AUDIT_TYPE_MAP[code]?.label || code
}
function auditTypeColor(code: string | null | undefined): string {
  return (code && AUDIT_TYPE_MAP[code]?.color) || 'default'
}
/** JSON 串美化；不是合法 JSON 时原样返回 */
function prettyJson(val: any): string {
  if (typeof val !== 'string') return JSON.stringify(val, null, 2)
  try {
    return JSON.stringify(JSON.parse(val), null, 2)
  } catch {
    return val
  }
}

// ═══ 数据加载 ═══
/** 当前查询条件 → 请求参数（时间格式与后端 @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") 一致） */
function buildQueryParams() {
  const params: Record<string, any> = {}
  if (searchForm.auditType) params.auditType = searchForm.auditType
  if (searchForm.module) params.module = searchForm.module
  if (searchForm.timeRange?.[0]) params.startTime = searchForm.timeRange[0].format('YYYY-MM-DD HH:mm:ss')
  if (searchForm.timeRange?.[1]) params.endTime = searchForm.timeRange[1].format('YYYY-MM-DD HH:mm:ss')
  return params
}

/**
 * 统计接口只接受 tenantId / startTime / endTime
 * （不支持 auditType / module / userId）→ 单独构造，避免发无关参数造成"统计跟着筛选走"的误解。
 */
function buildStatsParams() {
  const params: Record<string, any> = {}
  if (searchForm.timeRange?.[0]) params.startTime = searchForm.timeRange[0].format('YYYY-MM-DD HH:mm:ss')
  if (searchForm.timeRange?.[1]) params.endTime = searchForm.timeRange[1].format('YYYY-MM-DD HH:mm:ss')
  return params
}

/** 列表 + 统计（统计口径跟随查询区的时间范围，与列表保持一致） */
async function fetchData() {
  loading.value = true
  try {
    const params = buildQueryParams()
    const res: any = await request.get('/audit/query', {
      params: { ...params, page: pagination.current, pageSize: pagination.pageSize },
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    collectOptions(tableData.value)

    const statsRes: any = await request.get('/audit/statistics', { params: buildStatsParams() })
    stats.total = Number(statsRes?.total) || 0
    stats.successCount = Number(statsRes?.successCount) || 0
    // 🔴 后端真实键是 failureCount（不是 failCount）
    stats.failureCount = Number(statsRes?.failureCount) || 0
    collectOptions([], statsRes)
  } catch (error: any) {
    console.error('[操作审计] 加载审计日志失败', error)
    message.error(error?.response?.data?.message || error?.message || '加载审计日志失败')
    // 失败即清空，不做假数据兜底
    tableData.value = []
    pagination.total = 0
    stats.total = 0
    stats.successCount = 0
    stats.failureCount = 0
  } finally {
    loading.value = false
  }
}

/** 从「本页记录 + 统计聚合」中收集真实出现过的审计类型 / 模块，供下拉使用 */
function collectOptions(rows: any[], statsRes?: any) {
  const types = new Set(auditTypeSeen.value)
  const mods = new Set(moduleSeen.value)
  for (const r of rows || []) {
    if (r?.auditType) types.add(r.auditType)
    if (r?.module) mods.add(r.module)
  }
  for (const k of Object.keys(statsRes?.typeStats || {})) if (k && k !== 'null') types.add(k)
  for (const k of Object.keys(statsRes?.moduleStats || {})) if (k && k !== 'unknown') mods.add(k)
  const nextTypes = [...types]
  const nextMods = [...mods]
  // 仅在真正变化时重建，避免无谓的渲染（按内容比对，不能只比长度）
  if (nextTypes.join('|') !== auditTypeSeen.value.join('|')) auditTypeSeen.value = nextTypes
  if (nextMods.join('|') !== moduleSeen.value.join('|')) moduleSeen.value = nextMods
  rebuildOptions()
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchData()
}
function handleReset() {
  searchForm.auditType = undefined
  searchForm.module = undefined
  searchForm.timeRange = undefined
  pagination.current = 1
  fetchData()
}
function handleRefresh() {
  fetchData()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 详情抽屉（真实调用详情接口，不再拿列表行当详情） ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailItem = ref<any>(null)

async function openDetail(record: any) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detailItem.value = null
  try {
    // 雪花 ID 在前后端均以字符串传递，避免 JS 大整数精度丢失
    const res: any = await request.get(`/audit/detail/${record.id}`)
    if (!res || !res.id) {
      detailError.value = '详情接口未返回该日志'
      return
    }
    detailItem.value = res
  } catch (error: any) {
    console.error('[操作审计] 加载日志详情失败', error)
    detailError.value = error?.response?.data?.message || error?.message || '加载审计日志详情失败'
    message.error(detailError.value)
  } finally {
    detailLoading.value = false
  }
}

// ═══ 导出（真实调用 GET /api/audit/export） ═══
const { execute: executeExport, exporting } = useExport()
const EXPORT_HEADERS = ['日志ID', '审计类型', '操作模块', '操作动作', '操作用户', '操作时间', '操作结果', '操作IP', '请求方法', '请求URL', '耗时(ms)', '错误信息']
const EXPORT_FIELDS = ['id', 'auditType', 'module', 'action', 'username', 'operTime', 'result', 'operIp', 'requestMethod', 'requestUrl', 'duration', 'errorMsg']

const toExportRows = (list: any[]) => list.map(r => EXPORT_FIELDS.map((k) => {
  if (k === 'auditType') return auditTypeLabel(r.auditType)
  if (k === 'operTime') return r.operTime ? fmtTime(r.operTime) : ''
  return r[k] ?? ''
}))

function handleExport() {
  executeExport({
    fileName: '操作审计',
    headers: EXPORT_HEADERS,
    total: pagination.total,
    fetchAll: () => request.get('/audit/export', { params: buildQueryParams() }),
    mapToRows: toExportRows,
    fallbackRows: () => toExportRows(tableData.value),
  })
}

function handleError(error: Error) {
  console.error('[操作审计] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  rebuildOptions()
  fetchData()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
/* 统计卡固定高度，不参与表格的伸缩 */
.stat-row { flex-shrink: 0; padding: 12px 16px 0; }
.stat-card { background: #fafafa; }
.detail-alert { margin-bottom: 8px; }
.json-block { margin-top: 12px; }
.json-block__title { font-size: 13px; color: #666; margin-bottom: 4px; }
.json-block__pre { max-height: 220px; overflow: auto; background: #f5f5f5; padding: 8px; border-radius: 4px; margin: 0; font-size: 12px; }
.json-block__empty { font-size: 12px; color: #bbb; }
</style>
