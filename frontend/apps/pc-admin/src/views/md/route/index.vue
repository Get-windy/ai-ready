<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        线路主数据（资料 → 配送管理 → 线路，对标 ql361）
        · 单入口；无左侧分类树、无页面配置弹窗；列配置齿轮在数据表表头（个人配置 / 全局配置）
        · 工具栏：新增 / 导入 ｜ 刷新 / 打印(F8) / 导出
        · 查询区（固定项）：线路编号·线路名称·配送区域 + 显示状态(默认已启用) + 线路类型(默认全部)
        · 列表 6 列：线路编号 / 线路名称 / 线路类型 / 物流公司 / 配送区域 / 备注
        · 新增弹窗标题「配送路线」：线路类型*(自配/物流) / 线路编号* / 线路名称* / 物流公司 / 配送区域子表 / 备注
        ⚠️ 本页是「线路档案」主数据，与《配送路线单》执行单据（配送 → 线路管理 → 线路列表）严格区分
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

        <!-- ═══ 工具栏右侧：刷新 + 打印(F8) + 导出 ═══ -->
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
                  placeholder="请输入线路编号/线路名称/配送区域"
                  size="small"
                  style="width: 260px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">显示状态</span>
                <a-select
                  v-model:value="searchForm.status"
                  size="small"
                  style="width: 110px"
                  @change="handleSearch"
                >
                  <a-select-option value="ENABLED">
                    已启用
                  </a-select-option>
                  <a-select-option value="DISABLED">
                    已停用
                  </a-select-option>
                  <a-select-option value="ALL">
                    全部
                  </a-select-option>
                </a-select>
              </div>
              <div class="search-item">
                <span class="search-label">线路类型</span>
                <a-select
                  v-model:value="searchForm.routeType"
                  size="small"
                  style="width: 110px"
                  @change="handleSearch"
                >
                  <a-select-option value="">
                    全部
                  </a-select-option>
                  <a-select-option value="SELF">
                    自配
                  </a-select-option>
                  <a-select-option value="LOGISTICS">
                    物流
                  </a-select-option>
                </a-select>
              </div>
              <a-button
                type="primary"
                size="small"
                @click="handleSearch"
              >
                查询
              </a-button>
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
              storage-key="md-route-columns"
              global-config-key="md-route-columns"
              row-key="id"
            >
              <template #nameCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="handleEdit(record)"
                >{{ record.routeName }}</a>
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
                  <a-button
                    type="link"
                    size="small"
                    @click="handleToggleStatus(record)"
                  >
                    {{ record.status === 'ENABLED' ? '停用' : '启用' }}
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
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

      <!-- ═══ 新增 / 修改弹窗（对标弹窗标题「配送路线」） ═══ -->
      <a-modal
        v-model:open="modalVisible"
        title="配送路线"
        :width="760"
        :mask-closable="false"
        :confirm-loading="saving"
        ok-text="保存(Enter)"
        cancel-text="关闭(Esc)"
        @ok="handleSave"
      >
        <a-form
          ref="formRef"
          :model="formState"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
        >
          <a-form-item
            label="线路类型"
            required
          >
            <!-- 对标实测（2026-09-12 二次复核）：线路类型为互斥单选，默认选中「自配」 -->
            <a-radio-group v-model:value="formState.routeType">
              <a-radio value="SELF">
                自配
              </a-radio>
              <a-radio value="LOGISTICS">
                物流
              </a-radio>
            </a-radio-group>
          </a-form-item>

          <a-form-item
            label="线路编号"
            required
          >
            <a-input
              v-model:value="formState.routeCode"
              placeholder="请输入线路编号"
              :maxlength="64"
            />
          </a-form-item>

          <a-form-item
            label="线路名称"
            required
          >
            <a-input
              v-model:value="formState.routeName"
              placeholder="请输入线路名称"
              :maxlength="128"
            />
          </a-form-item>

          <a-form-item
            v-if="formState.routeType === 'LOGISTICS'"
            label="物流公司"
          >
            <a-input
              v-model:value="formState.expressName"
              placeholder="请输入承运物流公司名称"
              :maxlength="128"
            />
          </a-form-item>

          <a-form-item label="配送区域">
            <!-- 对标实测（2026-09-12 二次复核）：行政区划多选（city-list），已选项以标签呈现可删除 -->
            <a-tree-select
              v-model:value="formState.areaCodes"
              class="area-select"
              :tree-data="regionTree"
              :field-names="{ label: 'name', value: 'code', children: 'children' }"
              :tree-checkable="true"
              :show-checked-strategy="SHOW_PARENT"
              :max-tag-count="8"
              :dropdown-style="{ maxHeight: '320px', overflow: 'auto' }"
              tree-default-expand-all
              show-search
              tree-node-filter-prop="name"
              placeholder="--无--"
              allow-clear
            />
          </a-form-item>

          <a-form-item label="备注">
            <a-textarea
              v-model:value="formState.remark"
              placeholder="请输入备注"
              :rows="2"
              :maxlength="500"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 导入向导（三步：下载模板 / 导入Excel / 完成） ═══ -->
      <BaseDataImportWizard
        v-model:open="importModalVisible"
        title="基本信息导入"
        template-url="/erp/md/route/import-template"
        template-file-name="线路导入模板"
        import-url="/erp/md/route/import-excel"
        @success="handleImportSuccess"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal, TreeSelect } from 'ant-design-vue'
import {
  PlusOutlined,
  UploadOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import BaseDataImportWizard from '@/components/business/BaseDataImportWizard/index.vue'
import { sysRegionApi, type SysRegion } from '@/api/sys-region'
import { mdRouteApi, type MdRouteInfo } from '@/api/md'

const SHOW_PARENT = TreeSelect.SHOW_PARENT

// ═══ 行政区划（配送区域多选数据源，会话内缓存由 sysRegionApi 提供） ═══
const regionTree = ref<SysRegion[]>([])

async function loadRegionTree() {
  try {
    regionTree.value = await sysRegionApi.tree()
  } catch (error) {
    console.warn('[线路] 行政区划加载失败', error)
    regionTree.value = []
  }
}

// ═══ 状态 ═══
const loading = ref(false)
const saving = ref(false)
const tableData = ref<MdRouteInfo[]>([])

// ═══ 查询条件（对标固定项） ═══
const searchForm = reactive({
  keyword: '',
  status: 'ENABLED',
  routeType: '',
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 数据表列（对标 6 列，全部默认显示） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 170, fixed: 'left' },
  { title: '线路编号', key: 'routeCode', width: 140 },
  { title: '线路名称', key: 'routeName', type: 'slot', slotName: 'nameCell', width: 220 },
  { title: '线路类型', key: 'routeTypeText', width: 120 },
  { title: '物流公司', key: 'expressName', width: 180 },
  { title: '配送区域', key: 'areaText', width: 260 },
  { title: '备注', key: 'remark', width: 200 },
]

// ═══ 查询参数拼装（显示状态「全部」→ showDisabled=1） ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
  }
  if (searchForm.keyword) params.keyword = searchForm.keyword
  if (searchForm.status === 'ALL') {
    params.showDisabled = 1
  } else {
    params.status = searchForm.status
  }
  if (searchForm.routeType) params.routeType = searchForm.routeType
  return params
}

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await mdRouteApi.page(buildParams())
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[线路] 加载列表失败', error)
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

// ═══ 新增 / 修改弹窗 ═══
const modalVisible = ref(false)
const formRef = ref()
const formState = reactive({
  id: null as number | null,
  routeCode: '',
  routeName: '',
  /** 线路类型（互斥单选）：SELF-自配 LOGISTICS-物流，默认自配（对标实测默认值） */
  routeType: 'SELF',
  expressName: '',
  remark: '',
  /** 配送区域（行政区划编码多选） */
  areaCodes: [] as string[],
})

function resetForm() {
  formState.id = null
  formState.routeCode = ''
  formState.routeName = ''
  formState.routeType = 'SELF'
  formState.expressName = ''
  formState.remark = ''
  formState.areaCodes = []
}

async function handleAdd() {
  resetForm()
  modalVisible.value = true
  try {
    const code: any = await mdRouteApi.nextCode()
    formState.routeCode = code || ''
  } catch (error) {
    console.warn('[线路] 生成线路编号失败', error)
  }
}

async function handleEdit(record: MdRouteInfo) {
  resetForm()
  modalVisible.value = true
  try {
    const detail: any = await mdRouteApi.detail(record.id)
    if (!detail) return
    formState.id = detail.id
    formState.routeCode = detail.routeCode || ''
    formState.routeName = detail.routeName || ''
    formState.routeType = detail.routeLogistics === 1 ? 'LOGISTICS' : 'SELF'
    formState.expressName = detail.expressName || ''
    formState.remark = detail.remark || ''
    formState.areaCodes = (detail.areas || []).map((a: any) => a.areaCode).filter(Boolean)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载线路详情失败')
  }
}

async function handleSave() {
  if (!formState.routeCode.trim()) {
    message.warning('请输入线路编号')
    return
  }
  if (!formState.routeName.trim()) {
    message.warning('请输入线路名称')
    return
  }
  // 区域名称/类型由后端按行政区划编码回填（sys_region 单一数据源）
  const areas = formState.areaCodes.map((code, i) => ({ areaCode: code, sortNo: i }))

  const payload = {
    routeCode: formState.routeCode.trim(),
    routeName: formState.routeName.trim(),
    routeSelf: formState.routeType === 'SELF' ? 1 : 0,
    routeLogistics: formState.routeType === 'LOGISTICS' ? 1 : 0,
    expressName: formState.routeType === 'LOGISTICS' ? formState.expressName : '',
    remark: formState.remark,
    areas,
  }

  saving.value = true
  try {
    if (formState.id) {
      await mdRouteApi.update(formState.id, payload)
      message.success('修改成功')
    } else {
      await mdRouteApi.create(payload)
      message.success('新增成功')
    }
    modalVisible.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ═══ 删除 ═══
function handleDelete(record: MdRouteInfo) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除线路「${record.routeName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await mdRouteApi.remove(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 启用 / 停用 ═══
function handleToggleStatus(record: MdRouteInfo) {
  const target = record.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const actionText = target === 'ENABLED' ? '启用' : '停用'
  Modal.confirm({
    title: '确认',
    content: `确定要${actionText}线路「${record.routeName}」吗？`,
    onOk: async () => {
      try {
        await mdRouteApi.updateStatus(record.id, target)
        message.success(`已${actionText}`)
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
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
  // 弹窗内 Enter 保存（对标「保存(Enter)」；多行文本域内不触发）
  if (e.key === 'Enter' && modalVisible.value && !e.ctrlKey && !e.altKey && !e.metaKey) {
    const target = e.target as HTMLElement | null
    if (target && target.tagName === 'TEXTAREA') return
    e.preventDefault()
    handleSave()
  }
}

// ═══ 导出（真实 Excel） ═══
async function handleExport() {
  try {
    const params: Record<string, any> = {}
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status === 'ALL') {
      params.showDisabled = 1
    } else {
      params.status = searchForm.status
    }
    if (searchForm.routeType) params.routeType = searchForm.routeType

    const blob: any = await mdRouteApi.export(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `线路_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

// ═══ 导入（三步向导） ═══
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
  console.error('[线路] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  fetchList()
  loadRegionTree()
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

/* 配送区域（行政区划多选） */
.area-select { width: 100%; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
