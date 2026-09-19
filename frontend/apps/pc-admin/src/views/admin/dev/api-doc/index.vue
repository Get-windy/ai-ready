<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        API文档（系统 → 开发工具 → API文档，菜单 62403）
        · 平台控制台页面（client_type='system-admin'）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/API文档开发文档.md

        ═══ 去桩记录（本轮只改前端，对照文档 §12-P0）═══
        ① 原页表格 100% 前端写死 6 条模块清单（`modules.value = [...字面量]`，basePath 如 /api/auth、
          /api/user、/api/role、/api/base 在本系统后端**根本不存在**，文档 §12-P0①⑤）→ 已整段删除；
        ② 原页「真实分支」依赖 `GET /api/monitor/info`，而 SystemMonitorController 17 个端点中无 /info
          → 必然 404（文档 §12-P0②）→ 死分支删除；
        ③ 原页 `catch {}` 吞掉 404 后**立即回填 6 条假数据**，用户无法区分真假（文档 §12-P0③）
          → 改为 console.error + message.error + 清空 + 页面级错误横幅（**禁止假数据兜底**）；
        ④ 本页数据改为消费**真实 OpenAPI 3 spec**：`GET {后端源}/v3/api-docs`
          （实测 200，4.1MB，2997 个路径 / 3431 个操作 / 369 个 tag；该端点在 Sa-Token 放行清单内）。
          paths 摊平为「路径 / 方法 / 分组 tag / 摘要 / operationId」并支持按分组、方法、路径筛选。

        ═══ 文档跳转按钮（curl 实测 2026-09-18）═══
        · Knife4j  /doc.html                 → 200，可用（绝对地址新窗口打开）；
        · SwaggerUI /swagger-ui/index.html   → 401「请先登录」—— 该路径**不在** SaTokenConfig 放行清单
          （SaTokenConfig.java:60-64/100-105 只有 /doc.html、/webjars/**、/v3/api-docs/**）
          → 按钮置灰并注明原因，待后端放行后去掉 disabled 即可。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：接口总数（来自真实 spec） ═══ -->
        <template #toolbar-left>
          <span class="toolbar-hint">
            <template v-if="specError">OpenAPI spec 不可用，接口列表为空（原因见下方提示）</template>
            <template v-else>
              接口 {{ filteredRows.length }} / {{ allRows.length }} 个 · OpenAPI {{ specVersion || '-' }}
            </template>
          </span>
        </template>

        <!-- ═══ 工具栏右侧：文档跳转 + 刷新 + 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('knife4j')"
              size="small"
              @click="openKnife4j"
            >
              <FileTextOutlined /> Knife4j 文档
            </a-button>
            <!-- Swagger UI 后端未放行（实测 401）→ 置灰并注明，避免用户点击后打开一个 401 页 -->
            <a-tooltip
              title="后端 Sa-Token 放行清单未包含 /swagger-ui/**，直连返回 401，当前不可用；本系统请用 Knife4j"
              placement="bottom"
            >
              <span>
                <a-button
                  size="small"
                  disabled
                >
                  <FileTextOutlined /> Swagger UI
                </a-button>
              </span>
            </a-tooltip>
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

        <!-- ═══ 查询区（接口路径 / 请求方法 / 分组） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('path')">
                <span class="search-label">接口路径</span>
                <a-input
                  v-model:value="searchForm.path"
                  placeholder="请输入接口路径关键字"
                  size="small"
                  style="width: 220px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('method')">
                <span class="search-label">请求方法</span>
                <a-select
                  v-model:value="searchForm.method"
                  placeholder="全部方法"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="METHOD_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('tag')">
                <span class="search-label">分组</span>
                <a-select
                  v-model:value="searchForm.tag"
                  placeholder="全部分组"
                  size="small"
                  style="width: 220px"
                  allow-clear
                  show-search
                  :filter-option="filterTagOption"
                  :options="tagOptions"
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

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列） ═══ -->
        <template #table>
          <div class="table-area">
            <!-- 接口失败必须可见：如实说明原因并留空，不编数据 -->
            <a-alert
              v-if="specError"
              type="error"
              show-icon
              class="spec-error"
            >
              <template #message>
                OpenAPI spec 拉取失败：{{ specError }}
              </template>
              <template #description>
                尝试地址：{{ specUrl }}。请确认后端 core-api（5655）已启动且 `/v3/api-docs` 可访问
                （该端点需在 Sa-Token 放行清单内）。页面不会用占位数据兜底，接口列表保持为空。
              </template>
            </a-alert>

            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-dev-api-doc-table-columns"
              global-config-key="system-dev-api-doc-table-columns"
            >
              <!-- 请求方法 -->
              <template #methodCell="{ record }">
                <a-tag :color="METHOD_COLORS[record.method] || 'default'">
                  {{ record.method }}
                </a-tag>
              </template>

              <!-- 废弃标记（OpenAPI Operation.deprecated） -->
              <template #deprecatedCell="{ record }">
                <a-tag :color="record.deprecated ? 'red' : 'default'">
                  {{ record.deprecated ? '已废弃' : '正常' }}
                </a-tag>
              </template>

              <!-- 操作列 -->
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

      <!-- ═══ 接口详情弹窗（内容全部来自 spec 的 Operation 对象，无前端补字段） ═══ -->
      <a-modal
        v-model:open="detailVisible"
        :title="detailRow ? `${detailRow.method} ${detailRow.path}` : '接口详情'"
        width="860px"
        :footer="null"
      >
        <a-descriptions
          :column="2"
          size="small"
          bordered
          style="margin-bottom:16px"
        >
          <a-descriptions-item label="分组">
            {{ detailRow?.tag || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="operationId">
            {{ detailRow?.operation?.operationId || '-' }}
          </a-descriptions-item>
          <a-descriptions-item
            label="摘要"
            :span="2"
          >
            {{ detailRow?.operation?.summary || '-' }}
          </a-descriptions-item>
          <a-descriptions-item
            label="说明"
            :span="2"
          >
            {{ detailRow?.operation?.description || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="是否废弃">
            {{ detailRow?.operation?.deprecated ? '是' : '否' }}
          </a-descriptions-item>
          <a-descriptions-item label="请求体">
            {{ requestBodyText }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="detail-block-title">
          参数（{{ detailParameters.length }}）
        </div>
        <a-table
          :data-source="detailParameters"
          :columns="parameterColumns"
          row-key="__key"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'required'">
              {{ record.required ? '是' : '否' }}
            </template>
          </template>
        </a-table>

        <div class="detail-block-title">
          响应（{{ detailResponses.length }}）
        </div>
        <a-table
          :data-source="detailResponses"
          :columns="responseColumns"
          row-key="__key"
          :pagination="false"
          size="small"
        />
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SettingOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { apiDocApi, backendOrigin, type OpenApiOperation } from '@/api/admin'

defineOptions({ name: 'AdminDevApiDoc' })

const PAGE_CONFIG_STORAGE_KEY = 'system-dev-api-doc-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

/** 摊平后的一行 = OpenAPI 的一个操作（path × method） */
interface ApiRow {
  id: string
  path: string
  method: string
  tag: string
  summary: string
  operationId: string
  deprecated: boolean
  operation: OpenApiOperation
}

// ═══ 枚举 ═══
const METHOD_OPTIONS = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'PATCH', value: 'PATCH' },
]
/** OpenAPI 操作颜色（沿用 swagger-ui 的惯例配色：读蓝 / 写绿 / 改橙 / 删红） */
const METHOD_COLORS: Record<string, string> = {
  GET: 'blue',
  POST: 'green',
  PUT: 'orange',
  DELETE: 'red',
  PATCH: 'purple',
}
/** 无 tags 的操作归入此分组（OpenAPI 允许 Operation.tags 缺省） */
const UNGROUPED = '未分组'

// ═══ 状态 ═══
const loading = ref(false)
const allRows = ref<ApiRow[]>([])
const tableData = ref<ApiRow[]>([])
const specVersion = ref('')
/** 拉取失败原因（非空即页面级错误态；页面留空，不编数据） */
const specError = ref('')
const specUrl = `${backendOrigin()}/v3/api-docs`

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  path: '' as string,
  method: undefined as string | undefined,
  tag: undefined as string | undefined,
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'path', label: '接口路径', visible: true },
  { key: 'method', label: '请求方法', visible: true },
  { key: 'tag', label: '分组', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'knife4j', label: 'Knife4j 文档', enabled: true },
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

// ═══ 分组选项（由真实 spec 的操作去重得到，不硬编码） ═══
const tagOptions = computed(() =>
  [...new Set(allRows.value.map(r => r.tag))]
    .sort((a, b) => a.localeCompare(b, 'zh-CN'))
    .map(v => ({ label: v, value: v })),
)

/** a-select 的本地过滤（分组数量可达数百，必须可搜索） */
function filterTagOption(input: string, option: any) {
  return String(option?.value ?? '').toLowerCase().includes(String(input ?? '').toLowerCase())
}

// 序号列承载表头「列配置」齿轮；操作列为固定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 80, fixed: 'left' },
  { key: 'path', title: '接口路径', type: 'input', width: 360 },
  { key: 'method', title: '请求方法', type: 'slot', slotName: 'methodCell', width: 100, align: 'center' },
  { key: 'tag', title: '分组', type: 'input', width: 170 },
  { key: 'summary', title: '摘要', type: 'input', width: 300 },
  { key: 'operationId', title: 'operationId', type: 'input', width: 220 },
  { key: 'deprecated', title: '状态', type: 'slot', slotName: 'deprecatedCell', width: 90 },
]

// ═══ 详情弹窗 ═══
const detailVisible = ref(false)
const detailRow = ref<ApiRow | null>(null)

const parameterColumns = [
  { title: '参数名', dataIndex: 'name', key: 'name', width: 170 },
  { title: '位置', dataIndex: 'in', key: 'in', width: 90 },
  { title: '必填', key: 'required', width: 70 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 150 },
  { title: '说明', dataIndex: 'description', key: 'description', ellipsis: true },
]
const responseColumns = [
  { title: '状态码', dataIndex: 'code', key: 'code', width: 100 },
  { title: '说明', dataIndex: 'description', key: 'description', ellipsis: true },
]

/** $ref 简写：'#/components/schemas/XxxYyy' → 'XxxYyy' */
function shortenRef(ref: string): string {
  const parts = String(ref).split('/')
  return parts[parts.length - 1] || ref
}

/** schema → 可读类型串（含 $ref / 数组 / 基础类型；取不到显示「-」） */
function schemaType(schema: any): string {
  if (!schema) return '-'
  if (schema.$ref) return shortenRef(schema.$ref)
  if (schema.type === 'array') return `${schemaType(schema.items)}[]`
  if (Array.isArray(schema.type)) return schema.type.join(' | ')
  if (schema.type) return String(schema.type)
  if (schema.allOf) return schema.allOf.map(schemaType).join(' + ')
  return '-'
}

const detailParameters = computed(() => {
  const list = detailRow.value?.operation?.parameters || []
  return list.map((p, i) => ({
    __key: `${p?.in || 'x'}-${p?.name || i}`,
    name: p?.name || '-',
    in: p?.in || '-',
    required: !!p?.required,
    type: schemaType(p?.schema),
    description: p?.description || '-',
  }))
})

const detailResponses = computed(() => {
  const map = detailRow.value?.operation?.responses || {}
  return Object.entries(map).map(([code, val]: [string, any]) => ({
    __key: code,
    code,
    description: val?.description || '-',
  }))
})

const requestBodyText = computed(() => {
  const body = detailRow.value?.operation?.requestBody
  if (!body) return '无'
  const contents = Object.keys(body.content || {})
  return `${body.required ? '必填' : '可选'}（${contents.join(' / ') || '-'}）`
})

// ═══ 数据加载 ═══
/** 前端筛选（spec 一次性拉全量，筛选/分页均在前端完成） */
const filteredRows = computed(() => {
  const path = searchForm.path.trim().toLowerCase()
  return allRows.value.filter((r) => {
    if (path && !r.path.toLowerCase().includes(path)) return false
    if (searchForm.method && r.method !== searchForm.method) return false
    if (searchForm.tag && r.tag !== searchForm.tag) return false
    return true
  })
})

/** 按当前筛选结果重算当前页切片与总数 */
function applyPagination() {
  pagination.total = filteredRows.value.length
  const maxPage = Math.max(1, Math.ceil(pagination.total / pagination.pageSize))
  if (pagination.current > maxPage) pagination.current = maxPage
  const start = (pagination.current - 1) * pagination.pageSize
  tableData.value = filteredRows.value.slice(start, start + pagination.pageSize)
}

/** 把 spec.paths 摊平成操作行（按路径 + 方法稳定排序） */
function flattenSpec(paths: Record<string, Record<string, OpenApiOperation>>): ApiRow[] {
  const rows: ApiRow[] = []
  for (const [p, ops] of Object.entries(paths || {})) {
    for (const [m, op] of Object.entries(ops || {})) {
      if (!op || typeof op !== 'object') continue
      rows.push({
        id: `${m.toUpperCase()} ${p}`,
        path: p,
        method: m.toUpperCase(),
        tag: (op.tags && op.tags[0]) || UNGROUPED,
        summary: op.summary || '',
        operationId: op.operationId || '',
        deprecated: !!op.deprecated,
        operation: op,
      })
    }
  }
  return rows.sort((a, b) => (a.path === b.path ? a.method.localeCompare(b.method) : a.path.localeCompare(b.path)))
}

async function fetchSpec() {
  loading.value = true
  try {
    const spec = await apiDocApi.spec()
    const rows = flattenSpec(spec?.paths || {})
    allRows.value = rows
    specVersion.value = spec?.openapi || ''
    specError.value = ''
    pagination.current = 1
    applyPagination()
    if (!rows.length) message.warning('OpenAPI spec 已返回，但 paths 为空')
  } catch (error: any) {
    // 失败必须可见：日志 + 提示 + 清空（禁止假数据兜底）
    console.error('[API文档] 拉取 OpenAPI spec 失败', specUrl, error)
    message.error(error?.message || '拉取 OpenAPI spec 失败')
    allRows.value = []
    tableData.value = []
    pagination.total = 0
    specVersion.value = ''
    specError.value = error?.message || '未知错误'
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  applyPagination()
}
function handleReset() {
  searchForm.path = ''
  searchForm.method = undefined
  searchForm.tag = undefined
  pagination.current = 1
  applyPagination()
}
function handleRefresh() {
  fetchSpec()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  applyPagination()
}

// ═══ 行操作 / 文档跳转 ═══
function openDetail(record: ApiRow) {
  detailRow.value = record
  detailVisible.value = true
}

/** Knife4j 文档页：绝对地址新窗口打开（vite 不代理 /doc.html，相对路径会打开 SPA 自身） */
function openKnife4j() {
  window.open(`${backendOrigin()}/doc.html`, '_blank')
}

function handleError(error: Error) {
  console.error('[API文档] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(fetchSpec)
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.toolbar-hint { font-size: 12px; color: #8c8c8c; }
.spec-error { margin: 8px 16px 0; flex-shrink: 0; }
.detail-block-title { font-size: 13px; color: #666; margin: 0 0 8px; }
</style>
