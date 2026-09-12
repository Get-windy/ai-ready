<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        物流公司列表页（资料模块布局，对标 ql361「资料 → 物流公司」）
        · 无左侧分类树、无页面配置弹窗；列配置齿轮在数据表表头（个人配置 / 全局配置）
        · 工具栏：新增 / 导入 ｜ 刷新 / 打印(F8) / 导出 / 更多
        · 查询区：筛选条件（名称/编号/备注）+ 显示停用
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：新增 + 导入 ═══ -->
        <template #toolbar-left>
          <a-space :size="8">
            <a-button
              type="primary"
              size="small"
              class="btn-add"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              size="small"
              @click="showImportModal"
            >
              <UploadOutlined /> 导入
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) + 导出 + 更多 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <!-- 对标实测：物流公司工具栏「更多」= 停用 / 启用（对选中行批量生效）。
                 注意：#overlay 插槽内首个子节点必须是 a-menu，注释节点会让 antd 取到注释而渲染空浮层 -->
            <a-dropdown>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item
                    :disabled="selectedRows.length === 0"
                    @click="handleBatchStatus('DISABLED')"
                  >
                    停用
                  </a-menu-item>
                  <a-menu-item
                    :disabled="selectedRows.length === 0"
                    @click="handleBatchStatus('ENABLED')"
                  >
                    启用
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 查询区（固定项，对标无页面配置弹窗） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <div class="search-item">
                <span class="search-label">筛选条件</span>
                <a-input
                  v-model:value="searchForm.keyword"
                  placeholder="请输入物流公司名称/编号/备注"
                  size="small"
                  style="width: 260px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
              <a-checkbox
                v-model:checked="searchForm.showDisabled"
                @change="handleSearch"
              >
                显示停用
              </a-checkbox>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表（列配置齿轮在表头 rowNo 列右上角） ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="columns"
              :data-source="tableData"
              :loading="loading"
              :pagination="false"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              storage-key="md-logistics-columns"
              row-key="id"
              @selection-change="handleSelectionChange"
            >
              <template #nameCell="{ record }">
                <a
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record.partnerName }}</a>
              </template>

              <template #actionCell="{ record }">
                <a-space :size="0">
                  <a-button
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                  <a-dropdown>
                    <a-button
                      type="link"
                      size="small"
                    >
                      更多
                    </a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item @click="handleToggleStatus(record)">
                          {{ record.status === 'ENABLED' ? '停用' : '启用' }}
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>

        <!-- ═══ 底部：经典分页栏（对标 ql361：首页/上页/第(x/y)页/下页/尾页/跳转/共N条记录/每页显示N行） ═══ -->
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

      <!-- ═══ 导入向导（对标「基本信息导入」三步：下载模板 / 导入Excel / 完成） ═══ -->
      <BaseDataImportWizard
        v-model:open="importModalVisible"
        title="基本信息导入"
        template-url="/erp/md/customer/import-template"
        :template-params="{ partnerType: 'logistics' }"
        template-file-name="物流公司导入模板"
        import-url="/erp/md/customer/import-excel"
        :import-params="{ partnerType: 'logistics' }"
        @success="handleImportSuccess"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  UploadOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  DownOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import BaseDataImportWizard from '@/components/business/BaseDataImportWizard/index.vue'
import { partnerApi } from '@/api/erp/partner'
import type { Partner } from '@/api/erp/partner'
import request from '@/utils/request'

const router = useRouter()

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<Partner[]>([])
const selectedRows = ref<Partner[]>([])

// ═══ 查询条件（对标固定项：筛选条件 + 显示停用） ═══
const searchForm = reactive({
  keyword: '',
  showDisabled: false,
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 数据表列（对标默认 6 列，全部默认显示；列配置齿轮在 rowNo 表头） ═══
// 序号 / 勾选 / 操作为固定列（对标物流公司列表左侧：齿轮 + 勾选框 + 操作）
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '', key: 'rowCheck', type: 'checkbox', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 160, fixed: 'left' },
  { title: '物流公司编号', key: 'partnerCode', width: 140 },
  { title: '物流公司名称', key: 'partnerName', type: 'slot', slotName: 'nameCell', width: 240 },
  { title: '联系人', key: 'contactPerson', width: 110 },
  { title: '联系电话', key: 'contactPhone', width: 150 },
  { title: '物流公司地址', key: 'address', width: 280 },
  { title: '备注', key: 'remark', width: 200 },
]

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      partnerType: 'LOGISTICS',
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    // 「显示停用」未勾选 → 只看启用；勾选 → 显示全部（含停用）
    if (!searchForm.showDisabled) params.status = 'ENABLED'

    const res: any = await partnerApi.page(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[物流公司] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleSearch() {
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

function handleSelectionChange(rows: Partner[]) {
  selectedRows.value = rows || []
}

// ═══ 新增 / 修改 ═══
function handleAdd() {
  router.push('/md/logistics/form')
}

function handleEdit(record: Partner) {
  router.push(`/md/logistics/form/${record.id}`)
}

// ═══ 删除 ═══
function handleDelete(record: Partner) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除物流公司「${record.partnerName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await partnerApi.delete(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 启用 / 停用 ═══
async function toggleStatus(record: Partner) {
  const next = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await partnerApi.updateStatus(record.id, next)
    record.status = next
    message.success(next === 'ENABLED' ? '已启用' : '已停用')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '操作失败')
  }
}

function handleToggleStatus(record: Partner) {
  const actionText = record.status === 'ENABLED' ? '停用' : '启用'
  Modal.confirm({
    title: '确认',
    content: `确定要${actionText}物流公司「${record.partnerName}」吗？`,
    onOk: () => toggleStatus(record),
  })
}

/** 工具栏「更多 → 停用 / 启用」：对勾选行批量生效（对标口径） */
function handleBatchStatus(status: 'ENABLED' | 'DISABLED') {
  const actionText = status === 'ENABLED' ? '启用' : '停用'
  Modal.confirm({
    title: '批量修改',
    content: `确定要${actionText}选中的 ${selectedRows.value.length} 条物流公司吗？`,
    onOk: async () => {
      try {
        await partnerApi.batchStatus(selectedRows.value.map(r => r.id), status)
        message.success(`批量${actionText}成功`)
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || `批量${actionText}失败`)
      }
    },
  })
}

// ═══ 打印(F8) ═══
function handlePrint() {
  if (tableData.value.length === 0) {
    message.warning('没有可打印的数据')
    return
  }
  window.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（真实 Excel：后端 /erp/md/customer/export） ═══
async function handleExport() {
  try {
    const params: Record<string, any> = { partnerType: 'LOGISTICS', title: '物流公司' }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (!searchForm.showDisabled) params.status = 'ENABLED'

    const blob: any = await request.get('/erp/md/customer/export', { responseType: 'blob', params })
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `物流公司_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

// ═══ 导入（三步向导：下载模板 → 导入Excel → 完成） ═══
const importModalVisible = ref(false)

function showImportModal() {
  importModalVisible.value = true
}

function handleImportSuccess(result: any) {
  if (!result) return
  if (result.failure > 0) {
    message.warning(`导入完成：成功 ${result.success} 条，失败 ${result.failure} 条`)
  } else {
    message.success(`导入成功 ${result.success} 条`)
  }
  fetchList()
}

function handleError(error: Error) {
  console.error('[物流公司] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }

/* 橙色新增按钮（资料模块统一） */
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
