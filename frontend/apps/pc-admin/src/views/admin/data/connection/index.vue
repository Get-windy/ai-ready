<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        连接管理（系统管理 → 数据管理 → 连接管理，菜单 62301）
        · 平台控制台页面：ql361 无对标 → 按 DBeaver/Navicat「Database Connections」建模
        · 数据表列配置齿轮在表头 rowNo 列（个人/全局），storage-key 持久化
        · 后端：cn.aiedge.datasource.controller.DataSourceController（前缀 /api/data-source）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/连接管理开发文档.md
      -->
      <!--
        ═══ 当前生效连接 ═══
        本系统**自身**此刻连的库（运行时 DataSource 的 JDBC 元数据）。
        与下方表格（sys_data_source 里用户登记的外部数据源）是两回事——
        此前页面只有后者，这张表为空时整页空白，最该看到的「我连的是哪个库」反而没有。
      -->
      <a-alert
        v-if="currentConn"
        type="info"
        show-icon
        class="current-conn-bar"
      >
        <template #message>
          <span class="cc-label">当前连接</span>
          <a-tag
            v-if="currentConn.dbType"
            color="blue"
          >
            {{ currentConn.dbType }}
          </a-tag>
          <span
            v-if="currentConn.host"
            class="cc-item"
          >
            {{ currentConn.host }}:{{ currentConn.port }}/{{ currentConn.databaseName }}
          </span>
          <span
            v-if="currentConn.username"
            class="cc-item"
          >
            用户 {{ currentConn.username }}
          </span>
          <span
            v-if="currentConn.poolTotal != null"
            class="cc-item"
          >
            连接池 {{ currentConn.poolActive }}/{{ currentConn.poolTotal }}（上限 {{ currentConn.poolMax }}）
          </span>
          <span
            v-if="currentConn.error"
            class="cc-item cc-error"
          >
            元数据读取失败：{{ currentConn.error }}
          </span>
        </template>
      </a-alert>

      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增连接 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增连接
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

        <!-- ═══ 查询区（连接名称走后端 / 类型·状态为前端本地过滤） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('keyword')">
                <span class="search-label">连接名称</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="连接名称/数据库类型/库名"
                  size="small"
                  style="width: 220px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('dbType')">
                <span class="search-label">数据库类型</span>
                <a-select
                  v-model:value="searchForm.dbType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  :options="DB_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('status')">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  placeholder="全部状态"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="STATUS_OPTIONS"
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
            <BillDetailTable
              v-model:data-source="displayData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-data-connection-table-columns"
              global-config-key="system-data-connection-table-columns"
            >
              <!-- 连接名称：点击直达编辑 -->
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.name }}</a>
              </template>

              <!-- 状态：与后端 DataSource.status 值域一致（connected/disconnected/error） -->
              <template #statusCell="{ record }">
                <a-badge
                  v-if="!record.__ghost"
                  :status="STATUS_MAP[record.status]?.badge || 'default'"
                  :text="STATUS_MAP[record.status]?.label || record.status || '未知'"
                />
              </template>

              <!-- 更新时间：后端为 LocalDateTime，统一格式化展示 -->
              <template #updateTimeCell="{ record }">
                {{ fmtTime(record.updateTime) }}
              </template>

              <!-- 操作列 -->
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    :loading="testingId === record.id"
                    @click="testConnection(record)"
                  >
                    测试
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="openEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-popconfirm
                    title="确定删除此连接?"
                    @confirm="handleDelete(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      danger
                    >
                      删除
                    </a-button>
                  </a-popconfirm>
                </a-space>
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

      <!-- ═══ 新增/编辑连接弹窗 ═══ -->
      <a-modal
        v-model:open="showForm"
        :title="editRecord ? '编辑连接' : '新增连接'"
        width="560px"
        :confirm-loading="saving"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="form"
          :rules="formRules"
          layout="vertical"
        >
          <a-form-item
            label="连接名称"
            name="name"
          >
            <a-input
              v-model:value="form.name"
              placeholder="输入连接名称"
            />
          </a-form-item>
          <a-form-item
            label="数据库类型"
            name="dbType"
          >
            <a-select
              v-model:value="form.dbType"
              :options="DB_TYPE_OPTIONS"
            />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item
                label="主机地址"
                name="host"
              >
                <a-input
                  v-model:value="form.host"
                  placeholder="localhost"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="端口"
                name="port"
              >
                <a-input-number
                  v-model:value="form.port"
                  style="width:100%"
                  :min="1"
                  :max="65535"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item
            label="数据库名"
            name="databaseName"
          >
            <a-input
              v-model:value="form.databaseName"
              placeholder="输入数据库名"
            />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item
                label="用户名"
                name="username"
              >
                <a-input v-model:value="form.username" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                :label="editRecord ? '密码（留空则不修改）' : '密码'"
                name="password"
              >
                <a-input-password v-model:value="form.password" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item
            label="描述"
            name="description"
          >
            <a-textarea
              v-model:value="form.description"
              :rows="2"
              placeholder="连接用途说明"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
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
import { dataSourceApi, type DataSourceItem } from '@/api/admin'

defineOptions({ name: 'AdminDataConnection' })

const PAGE_CONFIG_STORAGE_KEY = 'system-data-connection-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 枚举 ═══
/** 数据库类型值域（提交值 SQLServer 与后端 buildJdbcUrl 的 switch 分支一致） */
const DB_TYPE_OPTIONS = [
  { label: 'MySQL', value: 'MySQL' },
  { label: 'PostgreSQL', value: 'PostgreSQL' },
  { label: 'Oracle', value: 'Oracle' },
  { label: 'SQL Server', value: 'SQLServer' },
]
/** 连接状态（与后端 DataSource.status 一致：connected/disconnected/error） */
const STATUS_MAP: Record<string, { label: string; badge: 'default' | 'error' | 'warning' | 'success' | 'processing' }> = {
  connected: { label: '已连接', badge: 'success' },
  disconnected: { label: '未连接', badge: 'default' },
  error: { label: '异常', badge: 'error' },
}
const STATUS_OPTIONS = [
  { label: '已连接', value: 'connected' },
  { label: '未连接', value: 'disconnected' },
  { label: '异常', value: 'error' },
]

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const testingId = ref<number | null>(null)
const tableData = ref<DataSourceItem[]>([])

/** 后端 /list 为内存分页（page/pageSize + records/total/current/size/pages），真分页参数 */
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/**
 * 当前**生效**的数据库连接（本系统自身连的库），来自 /data-source/current。
 * 与下面 tableData（`sys_data_source` 里用户登记的外部数据源）是两回事。
 */
const currentConn = ref<Record<string, any> | null>(null)
const searchForm = reactive({
  keyword: '' as string,
  dbType: undefined as string | undefined,
  status: undefined as string | undefined,
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '连接名称', visible: true },
  { key: 'dbType', label: '数据库类型', visible: true },
  { key: 'status', label: '状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增连接', enabled: true },
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

// 序号列承载表头「列配置」齿轮；操作列为固定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 190, fixed: 'left' },
  { key: 'name', title: '连接名称', type: 'slot', slotName: 'nameCell', width: 180 },
  { key: 'dbType', title: '数据库类型', type: 'input', width: 120 },
  { key: 'host', title: '主机地址', type: 'input', width: 150 },
  { key: 'port', title: '端口', type: 'input', width: 80 },
  { key: 'databaseName', title: '数据库名', type: 'input', width: 150 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 100 },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'updateTimeCell', width: 160 },
]

/**
 * 展示行 = 后端当页数据 + 类型/状态本地过滤。
 *
 * ⚠️ 后端 GET /api/data-source/list 只接收 keyword / page / pageSize 三个入参
 * （DataSourceController.list），没有 dbType / status 筛选参数；
 * 且 keyword 的 like 只匹配 name/dbType/databaseName，**不匹配 host**（故查询框文案已改准）。
 * 因此「数据库类型 / 状态」两项为**前端本地过滤**，作用范围是后端返回的当前页；
 * 切换这两项会回到第 1 页重新拉取（handleSearch），避免在第 N 页上出现「过滤后为空」的错觉。
 * 后端补上筛选参数后，这两项应改为服务端条件。
 */
const displayData = computed(() => {
  return tableData.value.filter((r: any) => {
    if (searchForm.dbType && r.dbType !== searchForm.dbType) return false
    if (searchForm.status && r.status !== searchForm.status) return false
    return true
  })
})

// ═══ 编辑弹窗 ═══
const showForm = ref(false)
const editRecord = ref<DataSourceItem | null>(null)
const formRef = ref()
const emptyForm = () => ({
  name: '',
  dbType: 'PostgreSQL',
  host: '',
  port: 5432 as number | undefined,
  databaseName: '',
  username: '',
  password: '',
  description: '',
})
const form = reactive(emptyForm())
/** 与后端 DDL 的 NOT NULL 列对齐（此前 required 只是视觉星号，端口/类型无校验） */
const formRules = {
  name: [{ required: true, message: '请输入连接名称', trigger: 'blur' }],
  dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'change' }],
  databaseName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
}

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// ═══ 数据加载 ═══

/**
 * 当前**生效**的数据库连接（本系统自身）。
 *
 * 背景：本页原先只展示 `sys_data_source` 里**用户登记的外部数据源**。该表为空时页面一片空白，
 * 而管理员打开「连接管理」最想知道的「本系统此刻连的是哪个库」反而看不到。
 * 后端 /data-source/current 从运行时 DataSource 读 JDBC 元数据（不读任何表；密码不返回、用户名已脱敏）。
 *
 * 定义放在 fetchList 之前：虽然函数声明本就会提升，但放在前面可避免
 * Vite HMR 部分更新时出现「调用方已更新、被调方未注入」的假 ReferenceError。
 */
async function fetchCurrent() {
  try {
    currentConn.value = await dataSourceApi.current()
  } catch (error: any) {
    console.warn('[连接管理] 获取当前生效连接失败', error)
    currentConn.value = null
  }
}

async function fetchList() {
  loading.value = true
  // 并行刷新「当前生效连接」：它是本系统自身连的库，与下方登记的外部数据源无关，
  // 失败只告警、不阻塞列表（两个数据源相互独立）
  fetchCurrent()
  try {
    const res: any = await dataSourceApi.page({
      keyword: searchForm.keyword || undefined,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    // 禁止假数据兜底：失败即清空并明示原因（此前此处静默吞异常，404/500 时页面只显示空表）
    console.error('[连接管理] 加载连接列表失败', error)
    message.error(error?.message || '加载连接列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handleReset() {
  searchForm.keyword = ''
  searchForm.dbType = undefined
  searchForm.status = undefined
  pagination.current = 1
  fetchList()
}
function handleRefresh() {
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 新增 / 编辑 ═══
function openCreate() {
  editRecord.value = null
  Object.assign(form, emptyForm())
  showForm.value = true
}

function openEdit(record: DataSourceItem) {
  editRecord.value = record
  Object.assign(form, emptyForm(), {
    name: record.name,
    dbType: record.dbType,
    host: record.host,
    port: record.port,
    databaseName: record.databaseName,
    username: record.username,
    // 密码不回填；后端为 null-safe 部分更新，留空即不覆盖原密码
    password: '',
    description: record.description || '',
  })
  showForm.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload: Partial<DataSourceItem> = {
      name: form.name,
      dbType: form.dbType,
      host: form.host,
      port: form.port,
      databaseName: form.databaseName,
      username: form.username,
      description: form.description,
      ...(form.password ? { password: form.password } : {}),
    }
    if (editRecord.value) {
      await dataSourceApi.update(editRecord.value.id, payload)
      message.success('连接已更新')
    } else {
      await dataSourceApi.create(payload)
      message.success('连接已创建')
    }
    showForm.value = false
    editRecord.value = null
    Object.assign(form, emptyForm())
    fetchList()
  } catch (e: any) {
    // 失败时保留弹窗与已填内容，仅提示后端原因
    console.error('[连接管理] 保存连接失败', e)
    message.error(e?.message || (editRecord.value ? '更新失败' : '创建失败'))
  } finally {
    saving.value = false
  }
}

/**
 * 测试连接：后端会向目标库发起真实 JDBC 连接，并把结果写回 status（connected/error）。
 * 失败按 message.error 提示（不弹成功）；结束后一律回读列表以展示后端真实状态。
 */
async function testConnection(record: DataSourceItem) {
  if (testingId.value) return
  testingId.value = record.id
  try {
    await dataSourceApi.test(record.id)
    message.success('连接测试成功')
  } catch (e: any) {
    console.error('[连接管理] 测试连接失败', e)
    message.error(e?.message || '连接测试失败')
  } finally {
    testingId.value = null
    fetchList()
  }
}

async function handleDelete(record: DataSourceItem) {
  try {
    await dataSourceApi.remove(record.id)
    message.success('连接已删除')
    fetchList()
  } catch (e: any) {
    console.error('[连接管理] 删除连接失败', e)
    message.error(e?.message || '删除失败')
  }
}

function handleError(error: Error) {
  console.error('[连接管理] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(fetchList)
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.btn-add { background: #fa8c16; border-color: #fa8c16; }
/* ═══ 当前生效连接提示条 ═══ */
.current-conn-bar { margin: 0 0 8px 0; }
.current-conn-bar .cc-label { font-weight: 600; margin-right: 8px; }
.current-conn-bar .cc-item { margin-left: 16px; }
.current-conn-bar .cc-error { color: #cf1322; }
</style>
