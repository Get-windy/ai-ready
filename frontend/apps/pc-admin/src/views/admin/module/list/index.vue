<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        模块列表（系统 → 模块管理 → 模块列表，菜单 62101）
        · 平台控制台页面：ql361 无对标 → 按 Odoo ir.module.module（Settings → Apps）建模
        · 单入口单视图列表页；模块编码为「技术名」，只读不可改（对齐 Odoo name readonly=True + UNIQUE(module_code)）
        · 数据表列配置齿轮在表头 rowNo 列（个人/全局），storage-key 持久化
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/模块列表开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增模块 ═══ -->
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('add')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增模块
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

        <!-- ═══ 查询区（模块名称/编码 + 状态） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('keyword')">
                <span class="search-label">模块名称/编码</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="请输入模块名称或编码"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
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
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-module-list-table-columns"
              global-config-key="system-module-list-table-columns"
            >
              <!-- 模块名称 -->
              <template #moduleNameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openEdit(record)"
                >{{ record.moduleName }}</a>
              </template>

              <!-- 状态 -->
              <template #statusCell="{ record }">
                <a-tag :color="record.status === 1 ? 'green' : 'red'">
                  {{ record.status === 1 ? '启用' : '停用' }}
                </a-tag>
              </template>

              <!-- 更新时间 -->
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
                    @click="openEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    @click="openVersions(record)"
                  >
                    版本
                  </a-button>
                  <a-popconfirm
                    :title="record.status === 1 ? '确定停用此模块?' : '确定启用此模块?'"
                    @confirm="toggleStatus(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      :danger="record.status === 1"
                      :loading="togglingId === record.id"
                    >
                      {{ record.status === 1 ? '停用' : '启用' }}
                    </a-button>
                  </a-popconfirm>
                  <a-popconfirm
                    title="确定删除此模块?"
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

      <!-- ═══ 新增/编辑模块弹窗 ═══ -->
      <a-modal
        v-model:open="editVisible"
        :title="editingId ? '编辑模块' : '新增模块'"
        width="520px"
        :confirm-loading="saving"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="editForm"
          :rules="formRules"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 17 }"
          style="margin-top:16px"
        >
          <a-form-item
            label="模块编码"
            name="moduleCode"
          >
            <!-- 技术名：编辑时只读（对齐 Odoo name readonly=True）；新增时必须填写 -->
            <a-input
              v-if="!editingId"
              v-model:value="editForm.moduleCode"
              placeholder="请输入模块编码（唯一，创建后不可修改）"
            />
            <a-input
              v-else
              :value="editingRecord?.moduleCode"
              disabled
            />
          </a-form-item>
          <a-form-item
            label="模块名称"
            name="moduleName"
          >
            <a-input
              v-model:value="editForm.moduleName"
              placeholder="请输入模块名称"
            />
          </a-form-item>
          <a-form-item
            label="版本号"
            name="version"
          >
            <a-input
              v-model:value="editForm.version"
              placeholder="如: 1.0.0"
            />
          </a-form-item>
          <a-form-item
            label="排序号"
            name="sortOrder"
          >
            <a-input-number
              v-model:value="editForm.sortOrder"
              :min="0"
              style="width:100%"
            />
          </a-form-item>
          <a-form-item
            label="描述"
            name="description"
          >
            <a-textarea
              v-model:value="editForm.description"
              :rows="3"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 版本记录弹窗（只读） ═══ -->
      <a-modal
        v-model:open="versionsVisible"
        :title="`版本记录 - ${versionsRecord?.moduleName || ''}`"
        width="760px"
        :footer="null"
      >
        <a-table
          :data-source="versions"
          :columns="versionColumns"
          :loading="versionsLoading"
          row-key="id"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'releaseStatus'">
              <a-tag :color="RELEASE_STATUS_MAP[record.releaseStatus]?.color || 'default'">
                {{ RELEASE_STATUS_MAP[record.releaseStatus]?.label || record.releaseStatus || '-' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'releaseTime'">
              {{ fmtTime(record.releaseTime) }}
            </template>
          </template>
        </a-table>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
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
import { moduleApi, type ModuleItem } from '@/api/admin'

defineOptions({ name: 'AdminModuleList' })

const route = useRoute()
const PAGE_CONFIG_STORAGE_KEY = 'system-module-list-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

// ═══ 枚举 ═══
const STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]
/** 发布状态中文映射（与「模块版本」页 62102 保持同一套观感） */
const RELEASE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  released: { label: '已发布', color: 'green' },
  beta: { label: '测试版', color: 'blue' },
  draft: { label: '草稿', color: 'default' },
}

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const togglingId = ref<number | null>(null)
const tableData = ref<ModuleItem[]>([])

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  keyword: '' as string,
  status: undefined as number | undefined,
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '模块名称/编码', visible: true },
  { key: 'status', label: '状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增模块', enabled: true },
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
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 210, fixed: 'left' },
  { key: 'moduleName', title: '模块名称', type: 'slot', slotName: 'moduleNameCell', width: 160 },
  { key: 'moduleCode', title: '模块编码', type: 'input', width: 150 },
  { key: 'version', title: '版本号', type: 'input', width: 110 },
  { key: 'sortOrder', title: '排序', type: 'input', width: 70 },
  { key: 'description', title: '描述', type: 'input', width: 220 },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'updateTime', title: '更新时间', type: 'slot', slotName: 'updateTimeCell', width: 160 },
]

const versionColumns = [
  { title: '版本号', dataIndex: 'version', key: 'version', width: 110 },
  { title: '更新日志', dataIndex: 'changelog', key: 'changelog', ellipsis: true },
  { title: '发布状态', dataIndex: 'releaseStatus', key: 'releaseStatus', width: 100 },
  { title: '发布人', dataIndex: 'publisher', key: 'publisher', width: 110 },
  { title: '发布时间', dataIndex: 'releaseTime', key: 'releaseTime', width: 160 },
]

// ═══ 编辑弹窗 ═══
const editVisible = ref(false)
const editingId = ref<number | null>(null)
const editingRecord = ref<ModuleItem | null>(null)
const formRef = ref()
const emptyForm = () => ({
  moduleCode: '' as string,
  moduleName: '' as string,
  version: '' as string,
  sortOrder: 0 as number,
  description: '' as string,
})
const editForm = reactive(emptyForm())
const formRules = computed<Record<string, any>>(() => ({
  moduleCode: editingId.value ? [] : [{ required: true, message: '请输入模块编码', trigger: 'blur' }],
  moduleName: [{ required: true, message: '请输入模块名称', trigger: 'blur' }],
}))

// ═══ 版本记录弹窗 ═══
const versionsVisible = ref(false)
const versionsLoading = ref(false)
const versions = ref<any[]>([])
const versionsRecord = ref<ModuleItem | null>(null)

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await moduleApi.list({
      keyword: searchForm.keyword || undefined,
      status: searchForm.status,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[模块列表] 加载列表失败', error)
    message.error(error?.message || '加载模块列表失败')
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
  editingId.value = null
  editingRecord.value = null
  Object.assign(editForm, emptyForm())
  editVisible.value = true
}

function openEdit(record: ModuleItem) {
  editingId.value = record.id
  editingRecord.value = record
  Object.assign(editForm, emptyForm(), {
    moduleCode: record.moduleCode,
    moduleName: record.moduleName,
    version: record.version || '',
    sortOrder: record.sortOrder ?? 0,
    description: record.description || '',
  })
  editVisible.value = true
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = {
      moduleName: editForm.moduleName,
      version: editForm.version,
      sortOrder: editForm.sortOrder,
      description: editForm.description,
    }
    if (editingId.value) {
      await moduleApi.update(editingId.value, payload)
      message.success('模块已更新')
    } else {
      await moduleApi.create({ ...payload, moduleCode: editForm.moduleCode, status: 1 })
      message.success('模块已创建')
    }
    editVisible.value = false
    editingId.value = null
    editingRecord.value = null
    fetchList()
  } catch (e: any) {
    message.error(e?.message || (editingId.value ? '更新失败' : '创建失败'))
  } finally {
    saving.value = false
  }
}

async function handleDelete(record: ModuleItem) {
  try {
    await moduleApi.remove(record.id)
    message.success('模块已删除')
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

/** 停用/启用：失败时不改动本地行，避免 UI 与后端不一致 */
async function toggleStatus(record: ModuleItem) {
  togglingId.value = record.id
  try {
    await moduleApi.toggleStatus(record.id)
    message.success(record.status === 1 ? '模块已停用' : '模块已启用')
    await fetchList()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    togglingId.value = null
  }
}

async function openVersions(record: ModuleItem) {
  versionsRecord.value = record
  versionsVisible.value = true
  versionsLoading.value = true
  try {
    const res: any = await moduleApi.versions(record.id)
    versions.value = res?.records || []
  } catch (e: any) {
    console.error('[模块列表] 加载版本记录失败', e)
    message.error(e?.message || '加载版本记录失败')
    versions.value = []
  } finally {
    versionsLoading.value = false
  }
}

function handleError(error: Error) {
  console.error('[模块列表] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  // 支持从「使用统计」页下钻带 keyword（模块编码）进入
  const q = route.query.keyword
  if (typeof q === 'string' && q) searchForm.keyword = q
  fetchList()
})
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
</style>
