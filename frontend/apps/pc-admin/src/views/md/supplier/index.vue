<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        category-title="供应商分类"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :current-path="currentPath"
        :show-table-footer="true"
        @category-select="handleCategorySelect"
        @category-expand="(keys: any[]) => (expandedKeys = keys)"
        @category-add="openCategoryModal(null)"
        @category-retry="loadCategoryTree"
      >
        <!-- 分类节点行内操作（编辑 / 删除分类） -->
        <template #tree-title="node">
          <span class="tree-node">
            <span class="tree-node-name">{{ node.categoryName }}</span>
            <span
              v-if="node.id !== ROOT_CATEGORY_ID"
              class="tree-node-actions"
            >
              <EditOutlined
                title="编辑分类"
                @click.stop="openCategoryModal(node)"
              />
              <DeleteOutlined
                title="删除分类"
                @click.stop="handleCategoryDelete(node)"
              />
            </span>
          </span>
        </template>

        <!-- ═══ 工具栏 ═══ -->
        <template #toolbar-left>
          <a-button
            type="primary"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增
          </a-button>
          <a-button
            size="small"
            @click="importModalVisible = true"
          >
            <UploadOutlined /> 导入
          </a-button>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              :loading="loading"
              @click="refreshAll"
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
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <a-dropdown>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item
                    :disabled="selectedIds.length === 0"
                    @click="openMoveModal"
                  >
                    <DragOutlined /> 批量搬移
                  </a-menu-item>
                  <a-menu-item
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchDelete"
                  >
                    <DeleteOutlined /> 批量删除
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchStatus('DISABLED')"
                  >
                    停用
                  </a-menu-item>
                  <a-menu-item
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchStatus('ENABLED')"
                  >
                    启用
                  </a-menu-item>
                  <a-menu-item
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchPriceTrack(false)"
                  >
                    取消价格跟踪
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标：筛选条件 / 显示状态 / 查询 / 显示层次结构 / 显示客户中的供应商 同一行） ═══ -->
        <template #search-fields>
          <div class="search-row">
            <div class="search-item">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="query.keyword"
                placeholder="请输入供应商编号/名称/联系人"
                size="small"
                style="width: 200px"
                allow-clear
                @press-enter="handleSearch"
              />
            </div>
            <div class="search-item">
              <span class="search-label">显示状态</span>
              <a-select
                v-model:value="query.status"
                size="small"
                style="width: 120px"
                :options="statusOptions"
                @change="handleSearch"
              />
            </div>
            <a-button
              type="primary"
              size="small"
              class="btn-search"
              @click="handleSearch"
            >
              查询
            </a-button>
            <a-checkbox
              v-model:checked="query.showHierarchy"
              @change="handleSearch"
            >
              显示层次结构
            </a-checkbox>
            <a-checkbox
              v-model:checked="query.showAsCustomer"
              @change="handleSearch"
            >
              显示客户中的供应商
            </a-checkbox>
          </div>
        </template>

        <!-- ═══ 数据表格（列配置走表头齿轮：个人 / 全局） ═══ -->
        <template #table>
          <BillDetailTable
            ref="tableRef"
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :view-mode="true"
            :min-rows="1"
            :show-pagination="false"
            :storage-key="storageKey"
            :global-config-key="storageKey"
            @checkbox-change="handleRowCheck"
            @checkbox-all="handleRowCheckAll"
            @sort-change="handleSortChange"
          >
            <!-- 操作列：订货 / 修改 / 更多（对标行级操作） -->
            <template #actionCell="{ record }">
              <a-space :size="0">
                <a-button
                  type="link"
                  size="small"
                  @click="handleOrder(record)"
                >
                  订货
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="handleEdit(record)"
                >
                  修改
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
                      <a-menu-item
                        danger
                        @click="handleDelete(record)"
                      >
                        删除
                      </a-menu-item>
                      <a-menu-item @click="handleToggleStatus(record)">
                        停用
                      </a-menu-item>
                      <a-menu-item @click="handleView(record)">
                        详情
                      </a-menu-item>
                      <a-menu-item
                        :disabled="!canMergePartner(record)"
                        :title="canMergePartner(record) ? '' : '该往来单位未同时登记为客户，无可合并目标'"
                        @click="openMergeModal(record)"
                      >
                        客商合并
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </template>

            <!-- 供应商名称：可点击进入详情 -->
            <template #nameCell="{ record }">
              <a
                class="cell-link"
                @click="handleView(record)"
              >{{ record.partnerName }}</a>
            </template>

            <!-- 附件数量列 -->
            <template #attachmentCell="{ record }">
              <span v-if="record.attachmentCount">
                <PaperClipOutlined /> {{ record.attachmentCount }}
              </span>
              <span v-else>-</span>
            </template>

            <!-- 固定列：附件快捷入口（有附件时高亮可用） -->
            <template #attachIconCell="{ record }">
              <a-button
                type="text"
                size="small"
                :disabled="!record.attachmentCount"
                :title="record.attachmentCount ? `查看附件（${record.attachmentCount}）` : '暂无附件'"
                @click="showAttachments(record)"
              >
                <PaperClipOutlined :style="{ color: record.attachmentCount ? '#faad14' : '#d9d9d9' }" />
              </a-button>
            </template>
          </BillDetailTable>
        </template>

        <!-- ═══ 底部：分页 ═══ -->
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
    </PageContainer>

    <!-- ═══ 分类新增/编辑 ═══ -->
    <a-modal
      v-model:open="categoryModalVisible"
      :title="editingCategory ? '编辑分类' : '新增分类'"
      :confirm-loading="categorySaving"
      width="420px"
      @ok="handleCategorySave"
    >
      <a-form
        ref="categoryFormRef"
        :model="categoryForm"
        :rules="categoryRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="分类名称"
          name="categoryName"
        >
          <a-input
            v-model:value="categoryForm.categoryName"
            placeholder="请输入分类名称"
            size="small"
          />
        </a-form-item>
        <a-form-item label="分类编码">
          <a-input
            v-model:value="categoryForm.categoryCode"
            placeholder="留空则系统自动生成"
            size="small"
          />
        </a-form-item>
        <a-form-item label="上级分类">
          <a-tree-select
            v-model:value="categoryForm.parentId"
            :tree-data="categoryTreeData"
            :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
            placeholder="无（根节点）"
            allow-clear
            size="small"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number
            v-model:value="categoryForm.sortOrder"
            :min="0"
            size="small"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 批量搬移 ═══ -->
    <a-modal
      v-model:open="moveModalVisible"
      title="批量搬移"
      :confirm-loading="moveSaving"
      width="420px"
      @ok="handleBatchMove"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="目标分类">
          <a-tree-select
            v-model:value="moveCategoryId"
            :tree-data="categoryTreeData"
            :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
            placeholder="请选择目标分类"
            allow-clear
            size="small"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
      <a-alert
        :message="`将把选中的 ${selectedIds.length} 个供应商搬移到目标分类`"
        type="info"
        show-icon
      />
    </a-modal>

    <!-- ═══ 客商合并（对标行级「更多 → 客商合并」） ═══ -->
    <a-modal
      v-model:open="mergeModalVisible"
      title="客商合并"
      :confirm-loading="mergeSaving"
      width="520px"
      ok-text="确定合并"
      @ok="handleMerge"
    >
      <a-alert
        :message="`将把「${mergeSource?.partnerName || ''}」彻底并入目标往来单位：全部业务引用（订单/出入库/收付款等）迁移到目标，当前记录被永久删除。`"
        type="warning"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-form
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="目标单位">
          <a-select
            v-model:value="mergeTargetId"
            show-search
            allow-clear
            size="small"
            placeholder="请输入名称搜索客户"
            :filter-option="false"
            :not-found-content="mergeSearching ? undefined : '未找到匹配的客户'"
            :options="mergeTargetOptions"
            style="width: 100%"
            @search="searchMergeTargets"
            @focus="searchMergeTargets('')"
          />
        </a-form-item>
        <a-form-item label="合并内容">
          <span class="merge-scope">全部业务引用（联系人 · 附件 · 订单 · 出入库 · 收付款 · 应收应付 · 发票）· 期初应付/预付 · 所属分类（目标为空时）· 多重身份</span>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 导入向导（对标「基本信息导入」三步：下载模板 / 导入Excel / 完成） ═══ -->
    <BaseDataImportWizard
      v-model:open="importModalVisible"
      title="基本信息导入"
      template-url="/erp/md/customer/import-template"
      :template-params="{ partnerType: 'supplier' }"
      template-file-name="供应商导入模板"
      import-url="/erp/md/customer/import-excel"
      :import-params="{ partnerType: 'supplier' }"
      @success="handleImportSuccess"
    />

    <!-- ═══ 附件查看 ═══ -->
    <a-modal
      v-model:open="attachModalVisible"
      :title="`附件 - ${attachPartnerName}`"
      width="560px"
      :footer="null"
    >
      <a-spin :spinning="attachLoading">
        <a-empty v-if="attachList.length === 0" description="暂无附件" />
        <div
          v-for="f in attachList"
          :key="f.id"
          class="attach-row"
        >
          <FileOutlined />
          <a
            :href="f.fileUrl"
            target="_blank"
            class="attach-name"
          >{{ f.fileName }}</a>
          <span class="attach-size">{{ formatSize(f.fileSize) }}</span>
        </div>
      </a-spin>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  DeleteOutlined,
  DownOutlined,
  DownloadOutlined,
  DragOutlined,
  EditOutlined,
  FileOutlined,
  PaperClipOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import BaseDataImportWizard from '@/components/business/BaseDataImportWizard/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { partnerApi, partnerAttachmentApi, partnerCategoryApi, type PartnerAttachment, type PartnerCategory } from '@/api/erp/partner'
import dayjs from 'dayjs'

defineOptions({ name: 'MdSupplier' })

const router = useRouter()
const handleError = (e: any) => console.warn('[供应商] ErrorBoundary:', e)

/** 虚拟根节点：全部供应商 */
const ROOT_CATEGORY_ID = '0'
const storageKey = 'md-supplier-columns'

// ═══ 查询条件（对标：筛选条件 / 显示状态 / 显示层次结构 / 显示客户中的供应商） ═══
const statusOptions = [
  { label: '全部', value: '' },
  { label: '已启用', value: 'ENABLED' },
  { label: '已停用', value: 'DISABLED' },
]
const query = reactive({
  keyword: '',
  status: 'ENABLED' as string,
  showHierarchy: false,
  showAsCustomer: false,
})

// ═══ 表格列（对标《供应商开发文档》12 列；序号/勾选/附件/操作 为固定列） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'attachIcon', title: '', type: 'slot', slotName: 'attachIconCell', width: 52, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 145, fixed: 'left' },
  { key: 'partnerCode', title: '供应商编号', width: 135, ellipsis: true, sortable: true },
  { key: 'partnerName', title: '供应商名称', type: 'slot', slotName: 'nameCell', width: 207, ellipsis: true, sortable: true },
  { key: 'contactPerson', title: '联系人', width: 100 },
  { key: 'contactPhone', title: '联系电话', width: 100 },
  {
    key: 'addTime',
    title: '新增时间',
    width: 120,
    sortable: true,
    formatter: (v: any) => formatDate(v),
  },
  { key: 'attachmentCount', title: '附件', type: 'slot', slotName: 'attachmentCell', width: 100 },
  { key: 'remark', title: '备注', width: 160, ellipsis: true },
  { key: 'operatingSeries', title: '经营系列', width: 120, ellipsis: true },
  { key: 'operatingArea', title: '经营面积', width: 120 },
  { key: 'taxNumber', title: '税号', width: 120, ellipsis: true },
  { key: 'bankName', title: '开户行', width: 120, ellipsis: true },
  { key: 'bankAccount', title: '银行账号', width: 120, ellipsis: true },
]

// ═══ 数据状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<any[]>([])
const selectedIds = ref<any[]>([])
const tableRef = ref<any>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
/** 列头排序状态（对标：供应商编号 / 供应商名称 / 新增时间 可点列头排序） */
const sortState = reactive({ field: '', order: 'asc' as 'asc' | 'desc' })

function formatDate(v: any): string {
  if (!v) return ''
  const s = String(v).replace('T', ' ')
  return s.length >= 10 ? s.slice(0, 10) : s
}
function formatSize(bytes: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

// ═══ 分类树 ═══
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryList = ref<PartnerCategory[]>([])
const selectedCategoryId = ref<string | number>(ROOT_CATEGORY_ID)
const expandedKeys = ref<(string | number)[]>([ROOT_CATEGORY_ID])

const categoryTreeData = computed<any[]>(() => [
  {
    id: ROOT_CATEGORY_ID,
    categoryName: '全部供应商',
    children: categoryList.value,
  },
])

const currentPath = computed(() => {
  if (String(selectedCategoryId.value) === ROOT_CATEGORY_ID) return '全部供应商'
  const path: string[] = []
  const walk = (nodes: any[], target: string): boolean => {
    for (const n of nodes) {
      path.push(n.categoryName)
      if (String(n.id) === target) return true
      if (n.children?.length && walk(n.children, target)) return true
      path.pop()
    }
    return false
  }
  walk(categoryList.value, String(selectedCategoryId.value))
  return path.length ? path.join(' / ') : '全部供应商'
})

async function loadCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const tree = await partnerCategoryApi.getTree('SUPPLIER')
    categoryList.value = Array.isArray(tree) ? tree : []
  } catch (e) {
    console.warn('[供应商] 分类树加载失败', e)
    categoryError.value = true
    categoryList.value = []
  } finally {
    categoryLoading.value = false
  }
}

function handleCategorySelect(keys: any[]) {
  selectedCategoryId.value = keys && keys.length ? keys[0] : ROOT_CATEGORY_ID
  handleSearch()
}

// ═══ 分类新增 / 编辑 / 删除 ═══
const categoryModalVisible = ref(false)
const categorySaving = ref(false)
const editingCategory = ref<any>(null)
const categoryFormRef = ref<any>(null)
const categoryForm = reactive({
  categoryName: '',
  categoryCode: '',
  parentId: undefined as any,
  sortOrder: 0,
})
const categoryRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

function openCategoryModal(node: any) {
  editingCategory.value = node || null
  Object.assign(categoryForm, {
    categoryName: node?.categoryName || '',
    categoryCode: node?.categoryCode || '',
    parentId: node ? (node.parentId && node.parentId !== 0 ? node.parentId : undefined) : undefined,
    sortOrder: node?.sortOrder ?? 0,
  })
  categoryModalVisible.value = true
}

async function handleCategorySave() {
  try {
    await categoryFormRef.value?.validate()
  } catch {
    return
  }
  categorySaving.value = true
  try {
    const payload: Partial<PartnerCategory> = {
      categoryName: categoryForm.categoryName,
      categoryCode: categoryForm.categoryCode,
      parentId: categoryForm.parentId ? Number(categoryForm.parentId) : undefined,
      sortOrder: categoryForm.sortOrder,
      categoryType: 'SUPPLIER',
    } as any
    if (editingCategory.value && String(editingCategory.value.id) !== ROOT_CATEGORY_ID) {
      await partnerCategoryApi.update(editingCategory.value.id, payload)
      message.success('分类更新成功')
    } else {
      await partnerCategoryApi.create(payload)
      message.success('分类新增成功')
    }
    categoryModalVisible.value = false
    await loadCategoryTree()
  } catch (e: any) {
    message.error(e?.message || '分类保存失败')
  } finally {
    categorySaving.value = false
  }
}

function handleCategoryDelete(node: any) {
  if (!node || String(node.id) === ROOT_CATEGORY_ID) return
  Modal.confirm({
    title: '删除分类',
    content: `确定删除分类「${node.categoryName}」吗？`,
    okType: 'danger',
    onOk: async () => {
      try {
        const msg: any = await partnerCategoryApi.delete(node.id)
        if (typeof msg === 'string' && msg) {
          message.warning(msg)
          return
        }
        message.success('分类删除成功')
        if (String(selectedCategoryId.value) === String(node.id)) {
          selectedCategoryId.value = ROOT_CATEGORY_ID
        }
        await loadCategoryTree()
        await fetchList()
      } catch {
        message.error('分类删除失败')
      }
    },
  })
}

// ═══ 列表查询 ═══
/** 列 key → 后端排序字段（后端只认白名单列名） */
const SORT_FIELD_MAP: Record<string, string> = {
  partnerCode: 'partyCode',
  partnerName: 'partyName',
  addTime: 'createTime',
}

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      partnerType: 'supplier',
      status: query.status || undefined,
      keyword: query.keyword || undefined,
      showHierarchy: query.showHierarchy || undefined,
      showAsCustomer: query.showAsCustomer || undefined,
    }
    if (sortState.field && SORT_FIELD_MAP[sortState.field]) {
      params.sortField = SORT_FIELD_MAP[sortState.field]
      params.sortOrder = sortState.order
    }
    if (String(selectedCategoryId.value) !== ROOT_CATEGORY_ID) {
      // 分类筛选：层次结构视图按父分类下钻，普通视图按所属分类过滤
      params.categoryId = Number(selectedCategoryId.value)
    }
    const res: any = await partnerApi.page(params as any)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    selectedIds.value = []
  } catch (e) {
    console.warn('[供应商] 列表加载失败', e)
    tableData.value = []
    pagination.total = 0
    message.error('查询失败，请检查网络后重试')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}
/** 列头排序：切换字段/方向后回到第 1 页重查 */
function handleSortChange(field: string, order: string) {
  if (!field || !order) {
    sortState.field = ''
    sortState.order = 'asc'
  } else {
    sortState.field = field
    sortState.order = order === 'desc' ? 'desc' : 'asc'
  }
  pagination.current = 1
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}
function refreshAll() {
  loadCategoryTree()
  fetchList()
}

// ═══ 行选择 ═══
function handleRowCheck(record: any, _index: number, checked: boolean) {
  const id = record.id
  if (checked) {
    if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
  } else {
    selectedIds.value = selectedIds.value.filter((k: any) => k !== id)
  }
}
function handleRowCheckAll(checked: boolean, records: any[]) {
  selectedIds.value = checked ? records.map((r: any) => r.id) : []
}

// ═══ 行操作 ═══
function handleAdd() {
  router.push('/md/supplier/form')
}
function handleEdit(record: any) {
  router.push({ path: '/md/supplier/form', query: { id: record.id } })
}
function handleView(record: any) {
  handleEdit(record)
}
/** 订货：跳转采购订单新增，带入供应商（/erp/purchase/form 读取 query.supplierId） */
function handleOrder(record: any) {
  router.push({ path: '/erp/purchase/form', query: { supplierId: record.id, supplierName: record.partnerName } })
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除供应商「${record.partnerName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await partnerApi.delete(record.id)
        message.success('删除成功')
        fetchList()
      } catch {
        message.error('删除失败')
      }
    },
  })
}

// ═══ 行级「更多」：停用 / 客商合并 ═══

/** 行级停用（对标行级「更多 → 停用」） */
async function handleToggleStatus(record: any) {
  try {
    await partnerApi.updateStatus(record.id, 'DISABLED')
    message.success(`已停用「${record.partnerName}」`)
    fetchList()
  } catch {
    message.error('停用失败')
  }
}

const mergeModalVisible = ref(false)
const mergeSaving = ref(false)
const mergeSource = ref<any>(null)
const mergeTargetId = ref<any>(null)
const mergeTargetOptions = ref<any[]>([])
const mergeSearching = ref(false)

/** 仅当该往来单位同时登记为客户（roles 含 CUSTOMER）时才存在可合并目标，与对标菜单禁用态一致 */
function canMergePartner(record: any): boolean {
  return String(record?.roles || '').split(',').includes('CUSTOMER')
}

function openMergeModal(record: any) {
  mergeSource.value = record
  mergeTargetId.value = null
  mergeTargetOptions.value = []
  mergeModalVisible.value = true
  searchMergeTargets(record.partnerName || '')
}

let mergeSearchTimer: any = null
function searchMergeTargets(keyword: string) {
  if (mergeSearchTimer) clearTimeout(mergeSearchTimer)
  mergeSearchTimer = setTimeout(async () => {
    mergeSearching.value = true
    try {
      const res: any = await partnerApi.page({
        partnerType: 'customer', keyword: keyword || undefined, pageNum: 1, pageSize: 20,
      } as any)
      mergeTargetOptions.value = (res?.records || [])
        .filter((r: any) => String(r.id) !== String(mergeSource.value?.id))
        .map((r: any) => ({ label: `${r.partnerName}（${r.partnerCode}）`, value: r.id }))
    } catch {
      mergeTargetOptions.value = []
    } finally {
      mergeSearching.value = false
    }
  }, 300)
}

function handleMerge() {
  if (!mergeTargetId.value) {
    message.warning('请选择要合并到的目标单位')
    return
  }
  Modal.confirm({
    title: '确认客商合并',
    content: `合并后「${mergeSource.value?.partnerName}」被永久删除，其全部业务引用（订单/出入库/收付款等）迁移到目标单位。`,
    okText: '确定合并',
    okType: 'danger',
    onOk: async () => {
      mergeSaving.value = true
      try {
        const res = await partnerApi.mergePartner(mergeSource.value.id, mergeTargetId.value)
        if (res?.merged) {
          message.success(`已并入「${res.targetName}」（迁移引用 ${res.movedReferences} 条 · 联系人 ${res.movedContacts} · 附件 ${res.movedAttachments}）`)
          mergeModalVisible.value = false
          fetchList()
        } else {
          message.error(res?.reason || '合并失败')
        }
      } catch {
        message.error('合并失败')
      } finally {
        mergeSaving.value = false
      }
    },
  })
}

// ═══ 批量操作（工具栏「更多」） ═══
function requireSelection(): boolean {
  if (selectedIds.value.length === 0) {
    message.warning('请先选择供应商')
    return false
  }
  return true
}
async function handleBatchStatus(status: string) {
  if (!requireSelection()) return
  const label = status === 'ENABLED' ? '启用' : '停用'
  try {
    await partnerApi.batchStatus(selectedIds.value, status)
    message.success(`已${label} ${selectedIds.value.length} 条`)
    fetchList()
  } catch {
    message.error(`${label}失败`)
  }
}
async function handleBatchPriceTrack(enabled: boolean) {
  if (!requireSelection()) return
  try {
    await partnerApi.batchPriceTrack(selectedIds.value, enabled)
    message.success(enabled ? '已启用价格跟踪' : '已取消价格跟踪')
    fetchList()
  } catch {
    message.error('操作失败')
  }
}
function handleBatchDelete() {
  if (!requireSelection()) return
  Modal.confirm({
    title: '批量删除',
    content: `确定删除选中的 ${selectedIds.value.length} 个供应商吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await partnerApi.batchDelete(selectedIds.value)
        message.success('批量删除成功')
        fetchList()
      } catch {
        message.error('批量删除失败')
      }
    },
  })
}

const moveModalVisible = ref(false)
const moveSaving = ref(false)
const moveCategoryId = ref<any>(undefined)
function openMoveModal() {
  if (!requireSelection()) return
  moveCategoryId.value = undefined
  moveModalVisible.value = true
}
async function handleBatchMove() {
  if (!moveCategoryId.value) {
    message.warning('请选择目标分类')
    return
  }
  moveSaving.value = true
  try {
    await partnerApi.batchMove(selectedIds.value, moveCategoryId.value)
    message.success('搬移成功')
    moveModalVisible.value = false
    fetchList()
  } catch {
    message.error('搬移失败')
  } finally {
    moveSaving.value = false
  }
}

// ═══ 导出（真实 xlsx 流，与分页查询同口径） ═══
async function handleExport() {
  exporting.value = true
  try {
    const params: Record<string, any> = {
      partnerType: 'supplier',
      title: '供应商',
      status: query.status || undefined,
      keyword: query.keyword || undefined,
      showAsCustomer: query.showAsCustomer || undefined,
    }
    if (String(selectedCategoryId.value) !== ROOT_CATEGORY_ID) {
      params.categoryId = Number(selectedCategoryId.value)
    }
    const blob: any = await partnerApi.export(params)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = `供应商_${dayjs().format('YYYYMMDD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    console.warn('[供应商] 导出失败', e)
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 打印(F8)：按当前查询结果渲染后打印 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}
function handlePrint() {
  if (!tableData.value.length) {
    message.warning('没有可打印的数据')
    return
  }
  const rows = tableData.value.map((r, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.partnerCode)}</td>
      <td>${escapeHtml(r.partnerName)}</td>
      <td>${escapeHtml(r.contactPerson)}</td>
      <td>${escapeHtml(r.contactPhone)}</td>
      <td>${formatDate(r.addTime)}</td>
      <td>${escapeHtml(r.remark)}</td>
      <td>${escapeHtml(r.operatingSeries)}</td>
      <td>${escapeHtml(r.operatingArea)}</td>
      <td>${escapeHtml(r.taxNumber)}</td>
      <td>${escapeHtml(r.bankName)}</td>
      <td>${escapeHtml(r.bankAccount)}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>供应商</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;gap:24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
    </style></head><body>
    <h2>供应商</h2>
    <div class="meta">
      <span>当前路径：${escapeHtml(currentPath.value)}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${tableData.value.length}</span>
    </div>
    <table>
      <thead><tr>
        <th>序号</th><th>供应商编号</th><th>供应商名称</th><th>联系人</th><th>联系电话</th>
        <th>新增时间</th><th>备注</th><th>经营系列</th><th>经营面积</th><th>税号</th>
        <th>开户行</th><th>银行账号</th>
      </tr></thead>
      <tbody>${rows}</tbody>
    </table>
  </body></html>`
  const w = window.open('', '_blank', 'width=1200,height=800')
  if (!w) {
    message.warning('打印窗口被浏览器拦截，请允许弹出窗口')
    return
  }
  w.document.write(html)
  w.document.close()
  w.focus()
  setTimeout(() => w.print(), 300)
}

// ═══ 导入（三步向导：下载模板 → 导入Excel → 完成） ═══
const importModalVisible = ref(false)

function handleImportSuccess(result: any) {
  if (!result) return
  if (result.failure > 0) {
    message.warning(`导入完成：成功 ${result.success} 条，失败 ${result.failure} 条`)
  } else {
    message.success(`导入成功 ${result.success} 条`)
  }
  fetchList()
}

// ═══ 附件查看 ═══
const attachModalVisible = ref(false)
const attachLoading = ref(false)
const attachList = ref<PartnerAttachment[]>([])
const attachPartnerName = ref('')
async function showAttachments(record: any) {
  if (!record.attachmentCount) return
  attachPartnerName.value = record.partnerName
  attachModalVisible.value = true
  attachLoading.value = true
  try {
    attachList.value = await partnerAttachmentApi.getByPartner(record.id)
  } catch {
    attachList.value = []
  } finally {
    attachLoading.value = false
  }
}

// ═══ 快捷键：F5 刷新 / F8 打印 / Ctrl+N 新增 ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
    refreshAll()
  }
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    handleAdd()
  }
}

onMounted(() => {
  loadCategoryTree()
  fetchList()
  document.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.btn-search {
  margin-left: 4px;
}
.cell-link {
  color: #1890ff;
  cursor: pointer;
}
.cell-link:hover {
  text-decoration: underline;
}
.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
}
.tree-node-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tree-node-actions {
  display: none;
  gap: 6px;
  color: #8c8c8c;
  font-size: 12px;
}
.tree-node:hover .tree-node-actions {
  display: inline-flex;
}
.tree-node-actions :deep(.anticon):hover {
  color: #1890ff;
}
.attach-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}
.attach-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.attach-size {
  color: #999;
  font-size: 12px;
}
.merge-scope {
  color: #666;
  font-size: 13px;
}
</style>
