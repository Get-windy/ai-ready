<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        其他往来单位（资料 → 往来单位 → 其他往来单位）
        · 本系统自建页面（ql361 无该菜单入口，无对标），按资料模块金标准实现
        · 布局：左「单位分类」树（可维护）+ 右数据表（列配置走表头齿轮）
        · 口径：biz_party.party_type=4，分类统一走 biz_party_category.party_type=4（categoryId）
      -->
      <CategoryListLayout
        category-title="单位分类"
        :category-tree-data="categoryTreeData"
        :category-loading="categoryLoading"
        :category-error="categoryError"
        :selected-category-id="selectedCategoryId"
        :category-expanded-keys="expandedKeys"
        :current-path="currentPath"
        :tabs="[]"
        :show-table-footer="true"
        @category-select="handleCategorySelect"
        @category-expand="(keys: any[]) => (expandedKeys = keys)"
        @category-add="openCategoryModal(null)"
        @category-retry="loadCategoryTree"
      >
        <!-- 分类树标题栏操作（作用于当前选中节点） -->
        <template #category-header-actions>
          <a-button
            type="link"
            size="small"
            title="修改"
            @click="handleCategoryHeaderEdit"
          >
            <EditOutlined />
          </a-button>
          <a-button
            type="link"
            size="small"
            title="删除"
            @click="handleCategoryHeaderDelete"
          >
            <DeleteOutlined />
          </a-button>
        </template>

        <template #tree-title="node">
          <span class="tree-node-name">{{ node.categoryName }}</span>
        </template>

        <!-- ═══ 工具栏 ═══ -->
        <template #toolbar-left>
          <a-button
            type="primary"
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
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              size="small"
              title="配置"
              @click="showPageConfig = true"
            >
              <SettingOutlined />
            </a-button>
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="refreshAll"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              :loading="exporting"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <a-dropdown v-if="hasMoreActions">
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item
                    v-if="isButtonEnabled('batchMove')"
                    :disabled="selectedIds.length === 0"
                    @click="openMoveModal"
                  >
                    <DragOutlined /> 批量搬移
                  </a-menu-item>
                  <a-menu-item
                    v-if="isButtonEnabled('batchDelete')"
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchDelete"
                  >
                    <DeleteOutlined /> 批量删除
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item
                    v-if="isButtonEnabled('disable')"
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchStatus('DISABLED')"
                  >
                    停用
                  </a-menu-item>
                  <a-menu-item
                    v-if="isButtonEnabled('enable')"
                    :disabled="selectedIds.length === 0"
                    @click="handleBatchStatus('ENABLED')"
                  >
                    启用
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 查询区 ═══ -->
        <template #search-fields>
          <div class="search-row">
            <div
              v-if="isQueryVisible('keyword')"
              class="search-item"
            >
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="query.keyword"
                placeholder="请输入单位名称/编号/联系人"
                size="small"
                style="width: 220px"
                allow-clear
                @press-enter="handleSearch"
              />
            </div>
            <div
              v-if="isQueryVisible('categoryId')"
              class="search-item"
            >
              <span class="search-label">单位类别</span>
              <a-tree-select
                v-model:value="selectedCategoryId"
                :tree-data="categoryTreeData"
                :field-names="{ children: 'children', label: 'categoryName', value: 'id' }"
                placeholder="全部"
                size="small"
                allow-clear
                tree-default-expand-all
                style="width: 180px"
                @change="handleSearch"
              />
            </div>
            <div
              v-if="isQueryVisible('status')"
              class="search-item"
            >
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
          </div>
        </template>

        <!-- ═══ 数据表 ═══ -->
        <template #table>
          <BillDetailTable
            ref="tableRef"
            :columns="columns"
            :data-source="tableData"
            :loading="loading"
            :view-mode="true"
            :show-pagination="false"
            :storage-key="storageKey"
            :global-config-key="`${storageKey}-global`"
            @checkbox-change="handleRowCheck"
            @checkbox-all="handleRowCheckAll"
          >
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
                <a-dropdown>
                  <a-button
                    type="link"
                    size="small"
                  >
                    更多
                  </a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="handleEdit(record)">
                        查看详情
                      </a-menu-item>
                      <a-menu-item
                        danger
                        @click="handleDelete(record)"
                      >
                        删除
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </template>

            <template #nameCell="{ record }">
              <a
                v-if="!record.__ghost"
                class="cell-link"
                @click="handleEdit(record)"
              >{{ record.partnerName }}</a>
            </template>

            <template #statusCell="{ record }">
              <a-tag
                v-if="!record.__ghost"
                :color="record.status === 'ENABLED' ? 'green' : 'red'"
              >
                {{ record.status === 'ENABLED' ? '启用' : '停用' }}
              </a-tag>
            </template>

            <template #attachmentCell="{ record }">
              <a-button
                v-if="!record.__ghost && record.attachmentCount"
                type="link"
                size="small"
                @click="showAttachments(record)"
              >
                <PaperClipOutlined /> {{ record.attachmentCount }}
              </a-button>
              <span v-else-if="!record.__ghost">-</span>
            </template>
          </BillDetailTable>
        </template>

        <template #table-footer>
          <StandardPagination
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 附件查看（列表「附件」列入口） ═══ -->
    <a-modal
      v-model:open="attachModalVisible"
      :title="`附件 - ${attachPartnerName}`"
      width="560px"
      :footer="null"
    >
      <a-spin :spinning="attachLoading">
        <a-empty
          v-if="attachList.length === 0"
          description="暂无附件"
        />
        <div
          v-for="f in attachList"
          :key="f.id"
          class="attach-row"
        >
          <PaperClipOutlined />
          <a
            :href="f.fileUrl"
            target="_blank"
            class="attach-name"
          >{{ f.fileName || '附件' }}</a>
          <span class="attach-size">{{ formatSize(f.fileSize) }}</span>
        </div>
      </a-spin>
    </a-modal>
    <!-- ═══ 页面配置（查询条件 / 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFields"
      :function-buttons-config="functionButtons"
      :default-query-fields-config="DEFAULT_QUERY_FIELDS"
      :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
      :storage-key="pageConfigStorageKey"
      hide-print-config
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

    <!-- ═══ 分类新增/编辑 ═══ -->
    <a-modal
      v-model:open="categoryModalVisible"
      :title="editingCategory ? '修改单位分类' : '新增单位分类'"
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
            placeholder="如：银行 / 政府机构 / 劳务公司"
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
            tree-default-expand-all
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
      <a-alert
        :message="`将把选中的 ${selectedIds.length} 个单位搬移到目标分类`"
        type="info"
        show-icon
      />
    </a-modal>

    <!-- ═══ 导入向导（基础资料导入：下载模板 / 导入Excel / 完成） ═══ -->
    <BaseDataImportWizard
      v-model:open="importModalVisible"
      title="基本信息导入"
      template-url="/erp/md/customer/import-template"
      :template-params="{ partnerType: 'other' }"
      template-file-name="其他往来单位导入模板"
      import-url="/erp/md/customer/import-excel"
      :import-params="{ partnerType: 'other' }"
      @success="handleImportSuccess"
    />
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="md-partner"
      :print-data="printData"
    />
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
  PaperClipOutlined,
  PlusOutlined,
  PrinterOutlined,
  ReloadOutlined,
  SettingOutlined,
  UploadOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import BaseDataImportWizard from '@/components/business/BaseDataImportWizard/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import {
  partnerApi,
  partnerAttachmentApi,
  partnerCategoryApi,
  type Partner,
  type PartnerAttachment,
  type PartnerCategory,
} from '@/api/erp/partner'
import dayjs from 'dayjs'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'MdPartner' })

const router = useRouter()
const handleError = (e: any) => console.warn('[其他往来单位] ErrorBoundary:', e)

/** 虚拟根节点：全部单位 */
const ROOT_CATEGORY_ID = '0'
/** 数据表列配置存储键（表头齿轮：个人 / 全局） */
const storageKey = 'md-partner-columns'
/** 页面配置（查询条件 / 功能按钮）存储键 */
const pageConfigStorageKey = 'md-partner-page-config'

// ═══ 查询条件 ═══
const statusOptions = [
  { label: '全部', value: '' },
  { label: '已启用', value: 'ENABLED' },
  { label: '已停用', value: 'DISABLED' },
]
const query = reactive({
  keyword: '',
  status: 'ENABLED' as string,
})

// ═══ 页面配置（查询条件显隐 / 功能按钮开关） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'keyword', label: '筛选条件', visible: true },
  { key: 'categoryId', label: '单位类别', visible: true },
  { key: 'status', label: '显示状态', visible: true },
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'batchMove', label: '批量搬移', enabled: true },
  { key: 'batchDelete', label: '批量删除', enabled: true },
  { key: 'disable', label: '停用', enabled: true },
  { key: 'enable', label: '启用', enabled: true },
]

const queryFields = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtons = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))
const showPageConfig = ref(false)

const hasMoreActions = computed(() =>
  ['batchMove', 'batchDelete', 'disable', 'enable'].some(k => isButtonEnabled(k)),
)

function isQueryVisible(key: string): boolean {
  const hit = queryFields.value.find(f => f.key === key)
  return hit ? hit.visible : false
}
function isButtonEnabled(key: string): boolean {
  const hit = functionButtons.value.find(b => b.key === key)
  return hit ? hit.enabled : false
}
function loadPageConfig() {
  try {
    const raw = localStorage.getItem(pageConfigStorageKey)
    if (!raw) {
      queryFields.value = DEFAULT_QUERY_FIELDS.map(f => ({ ...f }))
      functionButtons.value = DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f }))
      return
    }
    const parsed = JSON.parse(raw) as PageConfigData
    queryFields.value = DEFAULT_QUERY_FIELDS.map(df => {
      const saved = parsed.queryFields?.find(f => f.key === df.key)
      return saved ? { ...df, ...saved } : { ...df }
    })
    functionButtons.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
      const saved = parsed.functionButtons?.find(f => f.key === bf.key)
      return saved ? { ...bf, ...saved } : { ...bf }
    })
  } catch {
    // 配置损坏时保持默认
  }
}
function handlePageConfigChange(config: any) {
  localStorage.setItem(pageConfigStorageKey, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || [],
  }))
  loadPageConfig()
}

// ═══ 表格列（列配置走数据表表头齿轮，默认显示列见 defaultHidden） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'rowCheck', title: '', type: 'checkbox', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 130, fixed: 'left' },
  { key: 'partnerCode', title: '单位编号', width: 150, sortable: true, ellipsis: true },
  { key: 'partnerName', title: '单位名称', type: 'slot', slotName: 'nameCell', width: 220, sortable: true, ellipsis: true },
  { key: 'categoryName', title: '单位类别', width: 120, ellipsis: true },
  { key: 'contactPerson', title: '联系人', width: 100 },
  { key: 'contactPhone', title: '联系电话', width: 130 },
  { key: 'address', title: '联系地址', width: 220, ellipsis: true },
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 80 },
  { key: 'mnemonicCode', title: '助记码', width: 110, defaultHidden: true },
  { key: 'openingReceivable', title: '期初应收', width: 110, defaultHidden: true },
  { key: 'openingPayable', title: '期初应付', width: 110, defaultHidden: true },
  { key: 'bankName', title: '开户行', width: 140, ellipsis: true, defaultHidden: true },
  { key: 'bankAccount', title: '银行账号', width: 150, ellipsis: true, defaultHidden: true },
  { key: 'taxNumber', title: '税号', width: 150, ellipsis: true, defaultHidden: true },
  { key: 'addTime', title: '新增时间', width: 120, formatter: (v: any) => formatDate(v) },
  { key: 'remark', title: '备注', width: 160, ellipsis: true },
  { key: 'attachmentCount', title: '附件', type: 'slot', slotName: 'attachmentCell', width: 90 },
]

// ═══ 数据状态 ═══
const loading = ref(false)
const exporting = ref(false)
const tableData = ref<Partner[]>([])
const selectedRows = ref<Partner[]>([])
const tableRef = ref<any>(null)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedIds = computed(() =>
  selectedRows.value.filter((r: any) => !r.__ghost).map((r: any) => r.id),
)

function formatDate(v: any): string {
  if (!v) return ''
  const s = String(v).replace('T', ' ')
  return s.length >= 10 ? s.slice(0, 10) : s
}

// ═══ 分类树 ═══
const categoryLoading = ref(false)
const categoryError = ref(false)
const categoryList = ref<PartnerCategory[]>([])
const selectedCategoryId = ref<string | number>(ROOT_CATEGORY_ID)
const expandedKeys = ref<(string | number)[]>([ROOT_CATEGORY_ID])

const categoryTreeData = computed<any[]>(() => [
  { id: ROOT_CATEGORY_ID, categoryName: '全部单位', children: categoryList.value },
])

const currentPath = computed(() => {
  if (String(selectedCategoryId.value) === ROOT_CATEGORY_ID) return '全部单位'
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
  return path.length ? path.join(' / ') : '全部单位'
})

async function loadCategoryTree() {
  categoryLoading.value = true
  categoryError.value = false
  try {
    const tree = await partnerCategoryApi.getTree('OTHER')
    categoryList.value = Array.isArray(tree) ? tree : []
  } catch (e) {
    console.warn('[其他往来单位] 分类树加载失败', e)
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

// ═══ 分类新增 / 修改 / 删除 ═══
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

/** 当前选中的分类节点（分类标题栏操作对象；无选中时返回 null） */
const selectedCategoryNode = computed<any>(() => {
  if (String(selectedCategoryId.value) === ROOT_CATEGORY_ID) return null
  let found: any = null
  const walk = (nodes: any[]) => {
    for (const n of nodes) {
      if (String(n.id) === String(selectedCategoryId.value)) {
        found = n
        return
      }
      if (n.children?.length) walk(n.children)
    }
  }
  walk(categoryList.value)
  return found
})

function handleCategoryHeaderEdit() {
  const node = selectedCategoryNode.value
  if (!node) {
    message.warning('请先在左侧选择要修改的分类')
    return
  }
  openCategoryModal(node)
}

function handleCategoryHeaderDelete() {
  const node = selectedCategoryNode.value
  if (!node) {
    message.warning('请先在左侧选择要删除的分类')
    return
  }
  handleCategoryDelete(node)
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
      categoryName: categoryForm.categoryName.trim(),
      categoryCode: categoryForm.categoryCode,
      parentId: categoryForm.parentId ? Number(categoryForm.parentId) : undefined,
      sortOrder: categoryForm.sortOrder,
      categoryType: 'OTHER',
    } as any
    if (editingCategory.value) {
      await partnerCategoryApi.update(editingCategory.value.id, payload)
      message.success('分类修改成功')
    } else {
      await partnerCategoryApi.create(payload)
      message.success('分类新增成功')
    }
    categoryModalVisible.value = false
    await loadCategoryTree()
    await fetchList()
  } catch (e: any) {
    message.error(e?.message || '分类保存失败')
  } finally {
    categorySaving.value = false
  }
}

function handleCategoryDelete(node: any) {
  Modal.confirm({
    title: '删除分类',
    content: `确定删除分类「${node.categoryName}」吗？`,
    okText: '确认删除',
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
async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      partnerType: 'OTHER',
      status: query.status || undefined,
      keyword: query.keyword || undefined,
    }
    if (String(selectedCategoryId.value) !== ROOT_CATEGORY_ID) {
      params.categoryId = Number(selectedCategoryId.value)
    }
    const res: any = await partnerApi.page(params as any)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    selectedRows.value = []
  } catch (e) {
    console.warn('[其他往来单位] 列表加载失败', e)
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
  if (!record || record.__ghost) return
  if (checked) {
    if (!selectedRows.value.some((r: any) => r.id === record.id)) {
      selectedRows.value = [...selectedRows.value, record]
    }
  } else {
    selectedRows.value = selectedRows.value.filter((r: any) => r.id !== record.id)
  }
}
function handleRowCheckAll(checked: boolean, records: any[]) {
  const rows = (records || []).filter((r: any) => r && !r.__ghost)
  selectedRows.value = checked ? rows : []
}

// ═══ 行操作 ═══
function handleAdd() {
  router.push('/md/partner/form')
}
function handleEdit(record: any) {
  if (!record || record.__ghost) return
  router.push({ path: '/md/partner/form', query: { id: record.id } })
}
function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除「${record.partnerName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await partnerApi.delete(record.id)
        message.success('删除成功')
        fetchList()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 批量操作（工具栏「更多」） ═══
function requireSelection(): boolean {
  if (selectedIds.value.length === 0) {
    message.warning('请先选择单位')
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
function handleBatchDelete() {
  if (!requireSelection()) return
  Modal.confirm({
    title: '批量删除',
    content: `确定删除选中的 ${selectedIds.value.length} 个单位吗？`,
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
      partnerType: 'OTHER',
      title: '其他往来单位',
      status: query.status || undefined,
      keyword: query.keyword || undefined,
    }
    if (String(selectedCategoryId.value) !== ROOT_CATEGORY_ID) {
      params.categoryId = Number(selectedCategoryId.value)
    }
    const blob: any = await partnerApi.export(params)
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = `其他往来单位_${dayjs().format('YYYYMMDD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    console.warn('[其他往来单位] 导出失败', e)
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + 新开窗口打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列取自原打印表格的 <th>（原「序号」行号列由模板/引擎处理，不再由页面拼）。
const printColumns: any[] = [
  { title: '单位编号', key: 'partnerCode' },
  { title: '单位名称', key: 'partnerName' },
  { title: '单位类别', key: 'categoryName' },
  { title: '联系人', key: 'contactPerson' },
  { title: '联系电话', key: 'contactPhone' },
  { title: '联系地址', key: 'address' },
  { title: '状态', key: 'status', formatter: (v: any) => (v === 'ENABLED' ? '启用' : '停用') },
  { title: '备注', key: 'remark' },
]

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'md-partner',
  title: '其他往来单位',
  rows: () => tableData.value,
  columns: () => printColumns,
  // 原打印抬头的一行元信息（打印时间由模板 pageHeader 负责）
  totalText: () => `当前路径：${currentPath.value}，记录数：${tableData.value.length}`,
  emptyTip: '没有可打印的数据',
})

// ═══ 附件查看（列表「附件」列入口） ═══
const attachModalVisible = ref(false)
const attachLoading = ref(false)
const attachList = ref<PartnerAttachment[]>([])
const attachPartnerName = ref('')

function formatSize(bytes?: number | null) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

async function showAttachments(record: any) {
  if (!record?.attachmentCount) return
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

// ═══ 导入（复用基础资料导入向导；真实 Excel 落库） ═══
const importModalVisible = ref(false)

function showImportModal() {
  importModalVisible.value = true
}

function handleImportSuccess(result: any) {
  if (result && Number(result.success) > 0) {
    message.success(`导入成功 ${result.success} 条${Number(result.failure) ? `，失败 ${result.failure} 条` : ''}`)
  }
  refreshAll()
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
  loadPageConfig()
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
.tree-node-name {
  display: inline-block;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
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
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
</style>
