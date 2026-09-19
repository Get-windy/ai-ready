<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        模板管理（系统 → 开发工具 → 模板管理，菜单 62402）
        · 平台控制台页面（client_type='system-admin'），ql361 无对标
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/模板管理开发文档.md

        ═══ 数据源真相（2026-09-19 落库改造后，页面必须如实呈现）═══
        后端 `cn.aiedge.export.controller.ImportTemplateController`（前缀 /api/import-templates，
        13 个端点、类级 @SaCheckLogin、无端点级权限码）读的是**库表 `dev_template`**
        （经 `ImportTemplateStore`，迁移 V11.415.0）：模板定义真实持久化，
        新增/删除**重启后不复原**、多实例读同一份数据。
          · 只取 `template_kind='import'` 的行 → 同表内 7 行 `codegen`（代码生成模板，遗留种子，
            文档 §5.5）**不出现在本页**；
          · 只取 `enabled=true` 的行（`enabled` 是 DB 侧停用开关；接口无启用/停用语义、不下发该字段，
            故页面**没有**启用/停用入口）；
          · `content` 列存 JSON，仅承载表列无法表达的部分
            （fields / sampleRowCount / maxImportRows / strictValidation）；
            name/code/type/description/version 由表列承载，不重复存储。
        改造前（2026-09-18）本节写的是「后端内存态、重启恢复默认 4 个、多实例不一致」，现已被取代。

        ═══ 前一轮（2026-09-18，金标准路线 A，只改前端）═══
        ① 外壳补 ErrorBoundary + CategoryListLayout（五插槽）；
        ② 裸 a-table → BillDetailTable（表头齿轮：个人/全局列配置，storage-key 见下）；
        ③ 补查询区「模板名称 / 模板类型」——后端 `GET /import-templates` **不接受任何查询参数**
          （文档 §3.3），故筛选与分页**均在前端完成**，并在代码处注明；数据量恒为 4 条，
          前端分页为形态对齐（文档 §12-P2⑭），不假装是后端分页；
        ④ 补 StandardPagination 经典分页栏 + PageConfigPanel（查询条件显隐 / 功能按钮启用）；
        ⑤ 修文档 §12-P1④：`fetchData` 原 `catch {}` 静默吞错并清空列表 → 改为
          console.error + message.error + 清空（**禁止假数据兜底**）；
        ⑥ 预览弹窗补「下拉选项」列（文档 §12-P2⑫：后端 FieldPreview 含 8 个字段，原页只渲染 7 个）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：模板计数（数据来自库表 dev_template，如实标注） ═══ -->
        <template #toolbar-left>
          <span class="toolbar-hint">
            导入模板 {{ filteredList.length }} 个（已落库 dev_template）
          </span>
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

        <!-- ═══ 查询区（模板名称 / 模板类型） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('templateName')">
                <span class="search-label">模板名称</span>
                <a-input
                  v-model:value="searchForm.templateName"
                  placeholder="请输入模板名称"
                  size="small"
                  style="width: 200px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('dataType')">
                <span class="search-label">模板类型</span>
                <a-select
                  v-model:value="searchForm.dataType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  :options="dataTypeOptions"
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
              row-key="templateId"
              storage-key="system-dev-template-table-columns"
              global-config-key="system-dev-template-table-columns"
            >
              <!-- 数据类型（后端取值为 product/user/customer/order，未知值原样展示） -->
              <template #dataTypeCell="{ record }">
                <a-tag color="blue">
                  {{ dataTypeLabel(record.dataType) }}
                </a-tag>
              </template>

              <!-- 字段数（前端派生：后端未下发该字段，由 fields.length 现算，文档 §3.5） -->
              <template #fieldCountCell="{ record }">
                {{ record.fields?.length ?? 0 }}
              </template>

              <!-- 最大导入行数（千分位；空值显示「-」） -->
              <template #maxImportRowsCell="{ record }">
                {{ formatNumber(record.maxImportRows) }}
              </template>

              <!-- 校验模式（后端 4 个模板均为 true，见文档 §5.1 说明） -->
              <template #strictValidationCell="{ record }">
                <a-tag :color="record.strictValidation ? 'orange' : 'default'">
                  {{ record.strictValidation ? '严格校验' : '常规校验' }}
                </a-tag>
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
                    @click="openPreview(record)"
                  >
                    预览
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    :loading="downloadingId === record.templateId"
                    @click="downloadTemplate(record)"
                  >
                    下载
                  </a-button>
                  <a-popconfirm
                    title="确定删除此模板?"
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

      <!-- ═══ 模板结构预览弹窗（只读，数据来自 GET /import-templates/{id}/preview） ═══ -->
      <a-modal
        v-model:open="previewVisible"
        title="模板结构预览"
        width="900px"
        :footer="null"
      >
        <a-spin :spinning="previewLoading">
          <a-descriptions
            :column="2"
            size="small"
            bordered
            style="margin-bottom:16px"
          >
            <a-descriptions-item label="模板ID">
              {{ preview?.templateId || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="模板名称">
              {{ preview?.templateName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="最大导入行数">
              {{ formatNumber(preview?.maxImportRows) }}
            </a-descriptions-item>
            <a-descriptions-item label="描述">
              {{ preview?.description || '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <a-table
            :data-source="preview?.fields || []"
            :columns="fieldColumns"
            row-key="fieldName"
            :pagination="false"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'required'">
                <a-tag :color="record.required ? 'red' : 'default'">
                  {{ record.required ? '必填' : '选填' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'dropdownOptions'">
                {{ formatDropdownOptions(record.dropdownOptions) }}
              </template>
            </template>
          </a-table>
        </a-spin>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { importTemplateApi, type ImportTemplateItem, type TemplatePreview } from '@/api/admin'

defineOptions({ name: 'AdminDevTemplate' })

const PAGE_CONFIG_STORAGE_KEY = 'system-dev-template-page-config'

// 页面配置的查询条件 / 功能按钮项（与 PageConfigPanel 的 prop 结构一致）
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

/**
 * dataType 中文名（dev_template 中 template_kind='import' 的 4 行 type 值，实测 2026-09-19）
 * 未命中的值原样展示（不吞、不编），便于后续新增模板时仍可读。
 */
const DATA_TYPE_LABELS: Record<string, string> = {
  product: '产品',
  user: '用户',
  customer: '客户',
  order: '订单',
}

// ═══ 状态 ═══
const loading = ref(false)
const downloadingId = ref<string>('')
/** 当前页数据（前端筛选 + 前端分页后的切片） */
const tableData = ref<ImportTemplateItem[]>([])
/** 全量数据（后端一次性返回，见文件头「数据源真相」） */
const allList = ref<ImportTemplateItem[]>([])

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  templateName: '' as string,
  dataType: undefined as string | undefined,
})

// ═══ 查询条件 / 功能按钮（页面配置） ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'templateName', label: '模板名称', visible: true },
  { key: 'dataType', label: '模板类型', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
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

// ═══ 类型选项（由已加载数据的前端去重得到，不硬编码后端值域） ═══
const dataTypeOptions = computed(() =>
  [...new Set(allList.value.map(t => t.dataType).filter(Boolean))]
    .map(v => ({ label: dataTypeLabel(v), value: v })),
)

// 序号列承载表头「列配置」齿轮；操作列为固定列
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { key: 'templateName', title: '模板名称', type: 'input', width: 180 },
  { key: 'templateId', title: '模板ID', type: 'input', width: 180 },
  { key: 'dataType', title: '模板类型', type: 'slot', slotName: 'dataTypeCell', width: 110 },
  { key: 'version', title: '版本', type: 'input', width: 80 },
  { key: 'fieldCount', title: '字段数', type: 'slot', slotName: 'fieldCountCell', width: 80, align: 'right' },
  { key: 'maxImportRows', title: '最大导入行数', type: 'slot', slotName: 'maxImportRowsCell', width: 120, align: 'right' },
  { key: 'strictValidation', title: '校验模式', type: 'slot', slotName: 'strictValidationCell', width: 100 },
  { key: 'description', title: '描述', type: 'input', width: 220 },
]

// 预览弹窗字段结构表（8 列，含后端 FieldPreview 的 dropdownOptions，文档 §12-P2⑫）
const fieldColumns = [
  { title: '字段名', dataIndex: 'fieldName', key: 'fieldName', width: 150 },
  { title: '字段标题', dataIndex: 'fieldTitle', key: 'fieldTitle', width: 130 },
  { title: '类型', dataIndex: 'fieldType', key: 'fieldType', width: 90 },
  { title: '是否必填', key: 'required', width: 90 },
  { title: '最大长度', dataIndex: 'maxLength', key: 'maxLength', width: 90, align: 'right' as const },
  { title: '下拉选项', key: 'dropdownOptions', width: 160 },
  { title: '示例值', dataIndex: 'sampleValue', key: 'sampleValue', width: 120, ellipsis: true },
  { title: '说明', dataIndex: 'description', key: 'description', ellipsis: true },
]

// ═══ 预览弹窗 ═══
const previewVisible = ref(false)
const previewLoading = ref(false)
const preview = ref<TemplatePreview | null>(null)

// ═══ 工具函数 ═══
function dataTypeLabel(val: string | undefined): string {
  if (!val) return '-'
  return DATA_TYPE_LABELS[val] || val
}

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

/** 下拉选项 {food:'食品'} → 「食品 / 电子产品」；无选项显示「-」 */
function formatDropdownOptions(options: Record<string, string> | null | undefined): string {
  if (!options) return '-'
  const labels = Object.values(options)
  return labels.length ? labels.join(' / ') : '-'
}

// ═══ 数据加载 ═══
/** 前端筛选（后端端点无查询参数，见文件头说明） */
const filteredList = computed(() => {
  const name = searchForm.templateName.trim().toLowerCase()
  return allList.value.filter((t) => {
    if (name && !String(t.templateName || '').toLowerCase().includes(name)) return false
    if (searchForm.dataType && t.dataType !== searchForm.dataType) return false
    return true
  })
})

/** 按当前筛选结果重算当前页切片与总数 */
function applyPagination() {
  pagination.total = filteredList.value.length
  const maxPage = Math.max(1, Math.ceil(pagination.total / pagination.pageSize))
  if (pagination.current > maxPage) pagination.current = maxPage
  const start = (pagination.current - 1) * pagination.pageSize
  tableData.value = filteredList.value.slice(start, start + pagination.pageSize)
}

async function fetchList() {
  loading.value = true
  try {
    const res = await importTemplateApi.list()
    allList.value = Array.isArray(res) ? res : []
    applyPagination()
  } catch (error: any) {
    // 接口失败必须可见：日志 + 提示 + 清空（禁止假数据兜底）
    console.error('[模板管理] 加载模板列表失败', error)
    message.error(error?.message || '加载模板列表失败')
    allList.value = []
    tableData.value = []
    pagination.total = 0
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
  searchForm.templateName = ''
  searchForm.dataType = undefined
  pagination.current = 1
  applyPagination()
}
function handleRefresh() {
  fetchList()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  applyPagination()
}

// ═══ 行操作 ═══
async function openPreview(record: ImportTemplateItem) {
  previewVisible.value = true
  previewLoading.value = true
  preview.value = null
  try {
    preview.value = await importTemplateApi.preview(record.templateId)
  } catch (error: any) {
    console.error('[模板管理] 加载模板预览失败', record.templateId, error)
    message.error(error?.message || '预览加载失败')
  } finally {
    previewLoading.value = false
  }
}

async function downloadTemplate(record: ImportTemplateItem) {
  downloadingId.value = record.templateId
  try {
    const blob = await importTemplateApi.download(record.templateId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${record.templateName || record.templateId}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    message.success('模板下载成功')
  } catch (error: any) {
    console.error('[模板管理] 下载模板失败', record.templateId, error)
    message.error(error?.message || '模板下载失败')
  } finally {
    downloadingId.value = ''
  }
}

/**
 * 删除模板
 * 后端对「不存在的模板」返回 HTTP 200 + {success:false}（ImportTemplateController.java:113-116），
 * admin.ts 的 mutate 已按 success 字段抛错 → 此处在失败时不提示成功（修文档 §12-P1⑤）。
 * 2026-09-19 起删除作用于库表 dev_template（物理删），**重启后不再复活**。
 */
async function handleDelete(record: ImportTemplateItem) {
  try {
    await importTemplateApi.remove(record.templateId)
    message.success('模板已删除')
    fetchList()
  } catch (error: any) {
    console.error('[模板管理] 删除模板失败', record.templateId, error)
    message.error(error?.message || '删除失败')
  }
}

function handleError(error: Error) {
  console.error('[模板管理] 页面错误', error)
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
.toolbar-hint { font-size: 12px; color: #8c8c8c; }
</style>
