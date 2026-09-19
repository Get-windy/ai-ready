<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        模块版本（系统 → 模块管理 → 模块版本，菜单 62102）
        · 平台控制台页面：ql361 无对标 → 按 Odoo ir.module.module 的「版本记录」建模
        · 与「模块列表」行内「版本」弹窗同源（同一端点 GET /api/module/versions）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/模块版本开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('publish')"
            type="primary"
            size="small"
            class="btn-add"
            @click="openPublish(null)"
          >
            <PlusOutlined /> 发布新版本
          </a-button>
        </template>

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

        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <template v-if="isQueryFieldVisible('moduleId')">
                <span class="search-label">模块</span>
                <a-select
                  v-model:value="searchForm.moduleId"
                  placeholder="全部模块"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="moduleOptions"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('releaseStatus')">
                <span class="search-label">发布状态</span>
                <a-select
                  v-model:value="searchForm.releaseStatus"
                  placeholder="全部状态"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="RELEASE_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('version')">
                <span class="search-label">版本号</span>
                <a-input
                  v-model:value="searchForm.version"
                  placeholder="请输入版本号关键字"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
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

        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="tableData"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-module-version-table-columns"
              global-config-key="system-module-version-table-columns"
            >
              <template #changelogCell="{ record }">
                <span
                  v-if="!record.__ghost"
                  class="cell-ellipsis"
                  :title="record.changelog || ''"
                >{{ record.changelog || '-' }}</span>
              </template>

              <template #releaseStatusCell="{ record }">
                <a-tag :color="RELEASE_STATUS_MAP[record.releaseStatus]?.color || 'default'">
                  {{ RELEASE_STATUS_MAP[record.releaseStatus]?.label || record.releaseStatus || '-' }}
                </a-tag>
              </template>

              <template #releaseTimeCell="{ record }">
                {{ fmtTime(record.releaseTime) }}
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openDetail(record)"
                  >
                    详情
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    :disabled="record.releaseStatus === 'released'"
                    @click="openPublish(record)"
                  >
                    发布
                  </a-button>
                  <a-popconfirm
                    title="确定把模块当前版本回滚到此版本?"
                    @confirm="handleRollback(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      danger
                    >
                      回滚
                    </a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

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

      <!-- 详情（只读） -->
      <a-modal
        v-model:open="detailVisible"
        title="版本详情"
        width="560px"
        :footer="null"
      >
        <a-descriptions
          :column="1"
          bordered
          size="small"
          style="margin-top:12px"
        >
          <a-descriptions-item label="模块名称">
            {{ detailRecord?.moduleName || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="版本号">
            {{ detailRecord?.version || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="发布状态">
            <a-tag :color="RELEASE_STATUS_MAP[detailRecord?.releaseStatus]?.color || 'default'">
              {{ RELEASE_STATUS_MAP[detailRecord?.releaseStatus]?.label || detailRecord?.releaseStatus || '-' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="发布人">
            {{ detailRecord?.publisher || '-' }}
          </a-descriptions-item>
          <a-descriptions-item label="发布时间">
            {{ fmtTime(detailRecord?.releaseTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="创建时间">
            {{ fmtTime(detailRecord?.createTime) }}
          </a-descriptions-item>
          <a-descriptions-item label="更新日志">
            <div class="changelog-block">
              {{ detailRecord?.changelog || '-' }}
            </div>
          </a-descriptions-item>
        </a-descriptions>
      </a-modal>

      <!-- 发布新版本 -->
      <a-modal
        v-model:open="publishVisible"
        title="发布新版本"
        width="520px"
        :confirm-loading="publishing"
        @ok="handlePublish"
      >
        <a-form
          ref="publishFormRef"
          :model="publishForm"
          :rules="publishRules"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 17 }"
          style="margin-top:16px"
        >
          <a-form-item
            label="目标模块"
            name="moduleId"
          >
            <a-select
              v-model:value="publishForm.moduleId"
              placeholder="请选择模块"
              show-search
              option-filter-prop="label"
              :options="moduleOptions"
            />
          </a-form-item>
          <a-form-item
            label="版本号"
            name="version"
          >
            <a-input
              v-model:value="publishForm.version"
              placeholder="如: 1.0.0"
            />
          </a-form-item>
          <a-form-item
            label="更新日志"
            name="changelog"
          >
            <a-textarea
              v-model:value="publishForm.changelog"
              :rows="3"
              placeholder="请输入本次发布的更新内容"
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
import { moduleApi, type ModuleItem, type ModuleVersionItem } from '@/api/admin'

defineOptions({ name: 'AdminModuleVersion' })

const PAGE_CONFIG_STORAGE_KEY = 'system-module-version-page-config'

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const RELEASE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  released: { label: '已发布', color: 'green' },
  beta: { label: '测试版', color: 'blue' },
  draft: { label: '草稿', color: 'default' },
}
const RELEASE_STATUS_OPTIONS = Object.entries(RELEASE_STATUS_MAP).map(([value, v]) => ({ value, label: v.label }))

const loading = ref(false)
const tableData = ref<ModuleVersionItem[]>([])
const moduleList = ref<ModuleItem[]>([])
const moduleOptions = computed(() =>
  moduleList.value.map(m => ({ value: Number(m.id), label: `${m.moduleName}（${m.moduleCode}）` })))

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const searchForm = reactive({
  moduleId: undefined as number | undefined,
  releaseStatus: undefined as string | undefined,
  version: '' as string,
})

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'moduleId', label: '模块', visible: true },
  { key: 'releaseStatus', label: '发布状态', visible: true },
  { key: 'version', label: '版本号', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'publish', label: '发布新版本', enabled: true },
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

const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 160, fixed: 'left' },
  { key: 'moduleName', title: '模块名称', type: 'input', width: 150 },
  { key: 'version', title: '版本号', type: 'input', width: 130 },
  { key: 'changelog', title: '更新日志', type: 'slot', slotName: 'changelogCell', width: 280 },
  { key: 'releaseStatus', title: '发布状态', type: 'slot', slotName: 'releaseStatusCell', width: 100 },
  { key: 'publisher', title: '发布人', type: 'input', width: 110 },
  { key: 'releaseTime', title: '发布时间', type: 'slot', slotName: 'releaseTimeCell', width: 160 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'releaseTimeCell', width: 160, defaultHidden: true },
]

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

async function loadModules() {
  try {
    const res: any = await moduleApi.list({ pageNum: 1, pageSize: 200 })
    moduleList.value = res?.records || []
  } catch (e: any) {
    console.error('[模块版本] 加载模块下拉失败', e)
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await moduleApi.versions({
      moduleId: searchForm.moduleId,
      releaseStatus: searchForm.releaseStatus,
      version: searchForm.version || undefined,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[模块版本] 加载列表失败', error)
    message.error(error?.message || '加载版本列表失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}
function handleReset() {
  searchForm.moduleId = undefined
  searchForm.releaseStatus = undefined
  searchForm.version = ''
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

// ═══ 详情 ═══
const detailVisible = ref(false)
const detailRecord = ref<ModuleVersionItem | null>(null)
function openDetail(record: ModuleVersionItem) {
  detailRecord.value = record
  detailVisible.value = true
}

// ═══ 发布新版本 ═══
const publishVisible = ref(false)
const publishing = ref(false)
const publishFormRef = ref()
const publishForm = reactive({
  moduleId: undefined as number | undefined,
  version: '' as string,
  changelog: '' as string,
})
const publishRules = {
  moduleId: [{ required: true, message: '请选择模块', trigger: 'change' }],
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
}

function openPublish(record: ModuleVersionItem | null) {
  publishForm.moduleId = record ? Number(record.moduleId) : undefined
  publishForm.version = ''
  publishForm.changelog = ''
  publishVisible.value = true
}

async function handlePublish() {
  try {
    await publishFormRef.value?.validate()
  } catch {
    return
  }
  publishing.value = true
  try {
    await moduleApi.publish(publishForm.moduleId!, {
      version: publishForm.version,
      changelog: publishForm.changelog,
    })
    message.success('版本已发布')
    publishVisible.value = false
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

async function handleRollback(record: ModuleVersionItem) {
  try {
    await moduleApi.rollback(Number(record.moduleId), record.version)
    message.success(`已回滚到 ${record.version}`)
    fetchList()
  } catch (e: any) {
    message.error(e?.message || '回滚失败')
  }
}

function handleError(error: Error) {
  console.error('[模块版本] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadModules()
  fetchList()
})
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-ellipsis { display: inline-block; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; }
.changelog-block { white-space: pre-wrap; word-break: break-word; }
.btn-add { background: #fa8c16; border-color: #fa8c16; }
</style>
