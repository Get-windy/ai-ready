<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        租户列表（系统 → 租户管理 → 租户列表，菜单 62001）
        · 平台控制台核心页：ql361 无对标 → 按 SAP BTP 全局账户/子账户 + jeecg 租户管理建模
        · 字段名与后端实体 sys_tenant 逐字对齐（此前 contactName/expireDate 不存在 → 两列恒空）
        · 开发文档：docs/Yh-Spec/手动整理对标开发文档/系统模块/租户列表开发文档.md
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <template #toolbar-left>
          <a-button
            v-if="isButtonEnabled('create')"
            type="primary"
            size="small"
            class="btn-add"
            @click="handleCreate"
          >
            <PlusOutlined /> 新增租户
          </a-button>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="fetchData"
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
              <template v-if="isQueryFieldVisible('tenantCode')">
                <span class="search-label">租户编码</span>
                <a-input
                  v-model:value="query.tenantCode"
                  placeholder="请输入租户编码"
                  size="small"
                  style="width: 170px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('tenantName')">
                <span class="search-label">租户名称</span>
                <a-input
                  v-model:value="query.tenantName"
                  placeholder="请输入租户名称"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-if="isQueryFieldVisible('status')">
                <span class="search-label">状态</span>
                <a-select
                  v-model:value="query.status"
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

        <template #table>
          <div class="table-area">
            <BillDetailTable
              v-model:data-source="list"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              storage-key="system-tenant-list-table-columns"
              global-config-key="system-tenant-list-table-columns"
            >
              <template #tenantNameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record.tenantName }}</a>
              </template>

              <template #statusCell="{ record }">
                <a-tag :color="record.status === 1 ? 'green' : 'red'">
                  {{ record.status === 1 ? '正常' : '禁用' }}
                </a-tag>
              </template>

              <template #expireTimeCell="{ record }">
                {{ fmtTime(record.expireTime) }}
              </template>

              <template #createTimeCell="{ record }">
                {{ fmtTime(record.createTime) }}
              </template>

              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    编辑
                  </a-button>
                  <a-popconfirm
                    :title="record.status === 1 ? '确定禁用此租户?' : '确定启用此租户?'"
                    @confirm="handleToggleStatus(record)"
                  >
                    <a-button
                      type="link"
                      size="small"
                      :danger="record.status === 1"
                      :loading="togglingId === record.id"
                    >
                      {{ record.status === 1 ? '禁用' : '启用' }}
                    </a-button>
                  </a-popconfirm>
                  <a-popconfirm
                    title="确定删除此租户?（逻辑删除）"
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

      <a-modal
        v-model:open="modalVisible"
        :title="editingTenant ? '编辑租户' : '新增租户'"
        width="600px"
        :confirm-loading="modalLoading"
        @ok="handleModalOk"
        @cancel="handleModalCancel"
      >
        <a-form
          ref="modalFormRef"
          :model="modalForm"
          :rules="modalRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          style="margin-top:16px"
        >
          <a-form-item
            label="租户编码"
            name="tenantCode"
          >
            <a-input
              v-model:value="modalForm.tenantCode"
              placeholder="请输入租户编码（唯一）"
            />
          </a-form-item>
          <a-form-item
            label="租户名称"
            name="tenantName"
          >
            <a-input
              v-model:value="modalForm.tenantName"
              placeholder="请输入租户名称"
            />
          </a-form-item>
          <a-form-item
            label="联系人"
            name="contactPerson"
          >
            <a-input
              v-model:value="modalForm.contactPerson"
              placeholder="请输入联系人"
            />
          </a-form-item>
          <a-form-item
            label="联系电话"
            name="contactPhone"
          >
            <a-input
              v-model:value="modalForm.contactPhone"
              placeholder="请输入联系电话"
            />
          </a-form-item>
          <a-form-item
            label="联系邮箱"
            name="contactEmail"
          >
            <a-input
              v-model:value="modalForm.contactEmail"
              placeholder="请输入联系邮箱"
            />
          </a-form-item>
          <a-form-item
            label="租户等级"
            name="level"
          >
            <a-select
              v-model:value="modalForm.level"
              placeholder="请选择租户等级"
              :options="LEVEL_OPTIONS"
            />
          </a-form-item>
          <a-form-item
            label="到期时间"
            name="expireTime"
          >
            <a-date-picker
              v-model:value="modalForm.expireTime"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width:100%"
              placeholder="请选择到期时间"
            />
          </a-form-item>
          <a-form-item
            label="状态"
            name="status"
          >
            <a-select
              v-model:value="modalForm.status"
              :options="STATUS_OPTIONS"
            />
          </a-form-item>
          <a-form-item
            label="备注"
            name="remark"
          >
            <a-textarea
              v-model:value="modalForm.remark"
              :rows="3"
              placeholder="请输入备注"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { tenantApi, type TenantInfo } from '@/api/tenant'

defineOptions({ name: 'AdminTenantList' })

const PAGE_CONFIG_STORAGE_KEY = 'system-tenant-list-page-config'

interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfig { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const STATUS_OPTIONS = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
]
const LEVEL_OPTIONS = [
  { label: '基础版', value: 'basic' },
  { label: '专业版', value: 'professional' },
  { label: '企业版', value: 'enterprise' },
]

const list = ref<TenantInfo[]>([])
const loading = ref(false)
const togglingId = ref<number | null>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const query = reactive({
  tenantCode: '' as string,
  tenantName: '' as string,
  status: undefined as number | undefined,
})

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'tenantCode', label: '租户编码', visible: true },
  { key: 'tenantName', label: '租户名称', visible: true },
  { key: 'status', label: '状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'create', label: '新增租户', enabled: true },
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
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { key: 'id', title: 'ID', type: 'input', width: 80 },
  { key: 'tenantCode', title: '租户编码', type: 'input', width: 150 },
  { key: 'tenantName', title: '租户名称', type: 'slot', slotName: 'tenantNameCell', width: 180 },
  { key: 'contactPerson', title: '联系人', type: 'input', width: 120 },
  { key: 'contactPhone', title: '联系电话', type: 'input', width: 140 },
  { key: 'contactEmail', title: '联系邮箱', type: 'input', width: 180, defaultHidden: true },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'expireTime', title: '到期时间', type: 'slot', slotName: 'expireTimeCell', width: 160 },
  { key: 'createTime', title: '创建时间', type: 'slot', slotName: 'createTimeCell', width: 160, defaultHidden: true },
  { key: 'remark', title: '备注', type: 'input', width: 180, defaultHidden: true },
]

function fmtTime(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await tenantApi.getPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      tenantCode: query.tenantCode || undefined,
      tenantName: query.tenantName || undefined,
      status: query.status,
    })
    // 响应拦截器已拆掉 Result 外层；兼容两种形态
    const page = res?.data?.records ? res.data : (res || {})
    list.value = page.records || []
    pagination.total = Number(page.total) || 0
  } catch (error: any) {
    console.error('[租户列表] 加载失败', error)
    message.error(error?.message || '加载租户列表失败')
    list.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}
function handleReset() {
  query.tenantCode = ''
  query.tenantName = ''
  query.status = undefined
  pagination.current = 1
  fetchData()
}
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 弹窗 ═══
const modalVisible = ref(false)
const modalLoading = ref(false)
const modalFormRef = ref()
const editingTenant = ref<TenantInfo | null>(null)
const emptyForm = () => ({
  tenantCode: '',
  tenantName: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  level: 'basic',
  expireTime: undefined as string | undefined,
  status: 1,
  remark: '',
})
const modalForm = reactive(emptyForm())
const modalRules = {
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
}

function handleCreate() {
  editingTenant.value = null
  Object.assign(modalForm, emptyForm())
  modalVisible.value = true
}

function handleEdit(record: TenantInfo) {
  editingTenant.value = record
  Object.assign(modalForm, emptyForm(), {
    tenantCode: record.tenantCode,
    tenantName: record.tenantName,
    contactPerson: record.contactPerson || '',
    contactPhone: record.contactPhone || '',
    contactEmail: record.contactEmail || '',
    level: record.level || 'basic',
    expireTime: record.expireTime || undefined,
    status: record.status ?? 1,
    remark: record.remark || '',
  })
  modalVisible.value = true
}

async function handleModalOk() {
  try {
    await modalFormRef.value?.validate()
  } catch {
    return
  }
  modalLoading.value = true
  try {
    if (editingTenant.value) {
      await tenantApi.update(editingTenant.value.id, modalForm)
      message.success('更新成功')
    } else {
      await tenantApi.create(modalForm)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    modalLoading.value = false
  }
}

function handleModalCancel() {
  editingTenant.value = null
}

async function handleToggleStatus(record: TenantInfo) {
  togglingId.value = record.id
  try {
    await tenantApi.updateStatus(record.id, record.status === 1 ? 0 : 1)
    message.success(record.status === 1 ? '租户已禁用' : '租户已启用')
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    togglingId.value = null
  }
}

async function handleDelete(record: TenantInfo) {
  try {
    await tenantApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

function handleError(error: Error) {
  console.error('[租户列表] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(fetchData)
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.btn-add { background: #fa8c16; border-color: #fa8c16; }
</style>
