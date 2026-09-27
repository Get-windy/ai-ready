<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 复用资料模块通用骨架（与客户 / 供应商 / 商品等页面同一版式）：
           Tab 条 → 工具栏 → 查询行 → 数据表 → 分页；本页无分类树故关闭左面板 -->
      <CategoryListLayout
        :show-category-panel="false"
        :tabs="tabs"
        :active-tab="activeTab"
        :show-table-footer="true"
        @tab-change="onTabChange"
      >
        <!-- ── 工具栏（左：新增按钮组；右：刷新/打印(F8)/导出） ── -->
        <template #toolbar-left>
          <a-button
            type="primary"
            @click="handleAdd"
          >
            <PlusOutlined /> 新增{{ addLabel }}
          </a-button>
          <a-button
            v-if="activeTab === 'unit'"
            type="primary"
            @click="openUnitGroupManager"
          >
            <ApartmentOutlined /> 单位组管理
          </a-button>
        </template>

        <template #toolbar-right>
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
            <ExportOutlined /> 导出
          </a-button>
        </template>

        <!-- ── 查询行（筛选条件 + 查询） ── -->
        <template #search-fields>
          <div class="search-row">
            <div class="search-item">
              <span class="search-label">筛选条件</span>
              <a-input
                v-model:value="searchKeyword"
                placeholder="筛选条件"
                size="small"
                style="width: 220px"
                allow-clear
                @press-enter="handleSearch"
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

        <!-- ── 数据表（表头齿轮 = 列配置） ── -->
        <template #table>
          <BillDetailTable
            ref="tableRef"
            :key="activeTab"
            :columns="currentColumns"
            v-model:data-source="tableData"
            :loading="loading"
            :view-mode="true"
            :fill-mode="true"
            :storage-key="currentStorageKey"
          >
            <!-- 操作列 -->
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
                  v-if="activeTab === 'tag'"
                  type="link"
                  size="small"
                  :danger="record.status !== 0"
                  @click="handleToggleTagStatus(record)"
                >
                  {{ record.status === 0 ? '启用' : '停用' }}
                </a-button>
                <a-button
                  v-else
                  type="link"
                  size="small"
                  danger
                  @click="handleDelete(record)"
                >
                  删除
                </a-button>
              </a-space>
            </template>

            <!-- 是否默认列（对标 GoodsUnitList：#{isdefault}===1?'√':'X'） -->
            <template #isDefaultCell="{ record }">
              <span :class="record.isDefault === 1 ? 'aux-flag-on' : 'aux-flag-off'">
                {{ record.isDefault === 1 ? '√' : 'X' }}
              </span>
            </template>

            <!-- 对应商品列（商品名聚合串，超长省略） -->
            <template #productNamesCell="{ record }">
              <a-tooltip
                :title="record.productNames || '暂无关联商品'"
                placement="bottom"
              >
                <span class="aux-ellipsis">{{ record.productNames || '-' }}</span>
              </a-tooltip>
            </template>
          </BillDetailTable>
        </template>

        <!-- ── 底部分页（资料模块经典形态，对齐对标分页栏） ── -->
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

      <!-- ══════════ 品牌 / 单位 编辑弹窗 ══════════ -->
      <a-modal
        v-model:open="mainModalVisible"
        :title="modalTitle"
        :confirm-loading="modalLoading"
        width="520px"
        ok-text="保存(Enter)"
        cancel-text="取消(Esc)"
        @ok="handleModalSubmit"
        @cancel="closeMainModal"
      >
        <a-form
          ref="mainFormRef"
          :model="mainForm"
          :rules="mainFormRules"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          style="margin-top: 16px"
        >
          <a-form-item
            :label="activeTab === 'brand' ? '品牌名称' : activeTab === 'unit' ? '单位名称' : '标签名称'"
            name="name"
          >
            <a-input
              v-model:value="mainForm.name"
              :placeholder="activeTab === 'brand' ? '请输入品牌名称' : activeTab === 'unit' ? '请输入单位名称' : '请输入标签名称'"
              :maxlength="activeTab === 'tag' ? 50 : 30"
              @press-enter="handleModalSubmit"
            />
          </a-form-item>

          <template v-if="activeTab !== 'tag'">
            <a-form-item label="助记码">
              <a-input
                v-model:value="mainForm.mnemonicCode"
                placeholder="请输入助记码(拼音首字母)"
                :maxlength="50"
                @press-enter="handleModalSubmit"
              />
            </a-form-item>
            <a-form-item :label="activeTab === 'unit' ? '计量单位备注' : '备注'">
              <a-input
                v-model:value="mainForm.remark"
                placeholder="请输入备注,限30字"
                :maxlength="30"
                show-count
                @press-enter="handleModalSubmit"
              />
            </a-form-item>
          </template>

          <a-form-item
            v-if="activeTab === 'unit'"
            label="是否默认"
          >
            <a-switch
              v-model:checked="mainForm.isDefault"
              checked-children="是"
              un-checked-children="否"
            />
          </a-form-item>

          <a-form-item
            v-if="activeTab === 'tag'"
            label="排序"
          >
            <a-input-number
              v-model:value="mainForm.sortOrder"
              :min="0"
              style="width: 100%"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ══════════ 单位组管理弹窗（对标：工具栏 → 查询 → 单位/单位关系表 → 分页） ══════════ -->
      <a-modal
        v-model:open="unitGroupVisible"
        title="单位组管理"
        width="900px"
        :footer="null"
        @cancel="unitGroupVisible = false"
      >
        <div class="ug-toolbar">
          <a-button
            type="primary"
            @click="handleAddUnitGroup"
          >
            <PlusOutlined /> 新增单位组
          </a-button>
          <div class="ug-toolbar-right">
            <a-button
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              size="small"
              :loading="unitGroupExporting"
              @click="handleExportUnitGroup"
            >
              <ExportOutlined /> 导出
            </a-button>
            <a-button
              size="small"
              @click="fetchUnitGroups"
            >
              <ReloadOutlined /> 刷新
            </a-button>
          </div>
        </div>

        <div class="ug-query">
          <a-input
            v-model:value="unitGroupKeyword"
            placeholder="单位"
            size="small"
            style="width: 220px"
            allow-clear
            @press-enter="handleUnitGroupSearch"
          />
          <a-select
            v-model:value="unitGroupStatus"
            placeholder="显示状态"
            size="small"
            style="width: 120px"
            allow-clear
            :options="unitGroupStatusOptions"
          />
          <a-button
            type="primary"
            size="small"
            @click="handleUnitGroupSearch"
          >
            查询
          </a-button>
        </div>

        <BillDetailTable
          ref="unitGroupTableRef"
          :columns="unitGroupColumns"
          v-model:data-source="unitGroupData"
          :loading="unitGroupLoading"
          :view-mode="true"
          :fill-mode="false"
          :max-height="360"
          :min-rows="20"
          storage-key="md-product-supplement-unit-group-columns"
        >
          <template #ugActionCell="{ record }">
            <a-space :size="0">
              <a-button
                type="link"
                size="small"
                @click="handleEditUnitGroup(record)"
              >
                修改
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleToggleUnitGroupStatus(record)"
              >
                {{ record.status === 0 ? '启用' : '停用' }}
              </a-button>
              <a-button
                type="link"
                size="small"
                danger
                @click="handleDeleteUnitGroup(record)"
              >
                删除
              </a-button>
            </a-space>
          </template>
        </BillDetailTable>

        <StandardPagination
          variant="classic"
          :current="unitGroupPagination.current"
          :page-size="unitGroupPagination.pageSize"
          :total="unitGroupPagination.total"
          :page-size-options="[20, 50]"
          @change="handleUnitGroupPageChange"
        />
      </a-modal>

      <!-- ══════════ 单位组新增编辑（对标：固定 3 行 类型/单位名称/换算关系） ══════════ -->
      <a-modal
        v-model:open="unitGroupFormVisible"
        title="单位组新增编辑"
        width="600px"
        :footer="null"
        @cancel="unitGroupFormVisible = false"
      >
        <table class="ug-edit-grid">
          <thead>
            <tr>
              <th style="width: 48px">
                #
              </th>
              <th style="width: 110px">
                类型
              </th>
              <th>单位名称</th>
              <th style="width: 150px">
                换算关系
                <a-tooltip
                  title="相对小单位的倍数，小单位固定为 1"
                  placement="bottom"
                >
                  <QuestionCircleOutlined class="ug-help" />
                </a-tooltip>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, idx) in unitGroupRows"
              :key="row.unitType"
            >
              <td class="ug-idx">
                {{ idx + 1 }}
              </td>
              <td>{{ unitTypeLabel(row.unitType) }}</td>
              <td>
                <a-select
                  v-model:value="row.unitName"
                  :options="unitDictOptions"
                  :loading="unitDictLoading"
                  placeholder="请选择单位"
                  size="small"
                  show-search
                  allow-clear
                  style="width: 100%"
                  @change="(val: any) => onUnitNameChange(row, val)"
                />
              </td>
              <td>
                <a-input-number
                  v-model:value="row.conversionRate"
                  :min="1"
                  :precision="0"
                  :disabled="row.unitType === 'SMALL'"
                  size="small"
                  style="width: 100%"
                />
              </td>
            </tr>
          </tbody>
        </table>

        <div class="ug-edit-footer">
          <a-button
            type="primary"
            :loading="unitGroupSaving"
            @click="handleUnitGroupSubmit"
          >
            保存(Enter)
          </a-button>
          <a-button @click="unitGroupFormVisible = false">
            关闭(Esc)
          </a-button>
        </div>
      </a-modal>
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="md-product-supplement"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  ExportOutlined,
  ApartmentOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { toMnemonicCode } from '@/composables/useMnemonicCode'
import {
  productBrandApi,
  productUnitDictApi,
  mallTagApi,
  productUnitGroupApi,
} from '@/api/erp/product'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

/**
 * 商品辅助资料（资料 → 商品管理 → 商品辅助资料）
 *
 * 对标 ql361 实测（2026-09-11，22stable.ql361.com）为 3 个子标签组合页：
 *   1. 商品品牌：工具栏「新增品牌 | 刷新 | 打印(F8) | 导出」+ 查询「筛选条件 + 查询」
 *   2. 商品单位：工具栏「新增单位 | 单位组管理 | 刷新 | 打印(F8) | 导出」+ 查询
 *   3. 商品标签：行内「修改 / 停用」
 *
 * 裁决：对标 3 Tab（无「商品分类」）；商品分类在 ql361 由「商品」页的分类树承载，
 *      本系统 `views/erp/product/index.vue` 已完整承载分类增删改，故本页移除分类 Tab。
 */

// ── Tab 配置（对标 3 子标签） ──
type AuxTabKey = 'brand' | 'unit' | 'tag'
const tabs: { key: AuxTabKey; label: string }[] = [
  { key: 'brand', label: '商品品牌' },
  { key: 'unit', label: '商品单位' },
  { key: 'tag', label: '商品标签' },
]
const activeTab = ref<AuxTabKey>('brand')

const addLabel = computed(() => {
  switch (activeTab.value) {
    case 'brand': return '品牌'
    case 'unit': return '单位'
    default: return '标签'
  }
})

/** 各 Tab 独立列配置存储键（切 Tab 时 BillDetailTable 会按 storage-key 重载） */
const currentStorageKey = computed(() => `md-product-supplement-${activeTab.value}-columns`)

// ── 列定义（对标数据表列） ──
const brandColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50 },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110 },
  { key: 'brandName', title: '品牌名称', width: 220 },
  { key: 'mnemonicCode', title: '助记码', width: 140 },
  { key: 'remark', title: '备注' },
]

const unitColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50 },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110 },
  { key: 'unitName', title: '商品单位', width: 180 },
  { key: 'mnemonicCode', title: '助记码', width: 140 },
  { key: 'remark', title: '计量单位备注' },
  { key: 'isDefault', title: '是否默认', type: 'slot', slotName: 'isDefaultCell', width: 100, align: 'center' },
]

// 列序对齐对标源码（GoodsTags.columnMap：序号 | 操作 | 标签名称 fullname | 对应商品 goodsname）；
// 标签为标准槽位 TAG_N + 用户自定义昵称
const tagColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50 },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110 },
  { key: 'tagName', title: '标签名称', width: 200 },
  { key: 'productNames', title: '对应商品', type: 'slot', slotName: 'productNamesCell' },
]

const currentColumns = computed<DetailColumnConfig[]>(() => {
  switch (activeTab.value) {
    case 'unit': return unitColumns
    case 'tag': return tagColumns
    default: return brandColumns
  }
})

// ── 列表数据 ──
const tableRef = ref()
const loading = ref(false)
const tableData = ref<any[]>([])
const searchKeyword = ref('')
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

async function fetchData() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...(searchKeyword.value ? { keyword: searchKeyword.value } : {}),
    }
    const res: any = activeTab.value === 'brand'
      ? await productBrandApi.page(params)
      : activeTab.value === 'unit'
        ? await productUnitDictApi.page(params)
        : await mallTagApi.page(params)
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (e) {
    console.error('[商品辅助资料] 加载失败', e)
    message.error('加载数据失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function switchTab(key: 'brand' | 'unit' | 'tag') {
  if (activeTab.value === key) return
  activeTab.value = key
  searchKeyword.value = ''
  pagination.current = 1
  fetchData()
}

/** CategoryListLayout 的 tab-change 回调（其 key 为 string） */
function onTabChange(key: string) {
  switchTab(key as 'brand' | 'unit' | 'tag')
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleRefresh() {
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ── 新增 / 修改（品牌 / 单位 / 标签） ──
const mainModalVisible = ref(false)
const modalLoading = ref(false)
const mainFormRef = ref<FormInstance>()
const editingId = ref<string | null>(null)

const mainForm = reactive({
  name: '',
  mnemonicCode: '',
  remark: '',
  isDefault: false,
  sortOrder: 0,
})

const modalTitle = computed(() => {
  const label = addLabel.value
  return (editingId.value ? '修改' : '新增') + label
})

const mainFormRules = computed<Record<string, Rule[]>>(() => ({
  name: [{
    required: true,
    message: `请输入${activeTab.value === 'brand' ? '品牌名称' : activeTab.value === 'unit' ? '单位名称' : '标签名称'}`,
    trigger: 'blur',
  }],
}))

/** 助记码自动跟随：仅当用户未手工改动过时才用名称拼音覆盖 */
let lastAutoMnemonic = ''
watch(() => mainForm.name, (val) => {
  if (activeTab.value === 'tag') return
  const current = mainForm.mnemonicCode || ''
  if (current === '' || current === lastAutoMnemonic) {
    const auto = toMnemonicCode(val || '')
    mainForm.mnemonicCode = auto
    lastAutoMnemonic = auto
  }
})

function resetMainForm() {
  mainForm.name = ''
  mainForm.mnemonicCode = ''
  mainForm.remark = ''
  mainForm.isDefault = false
  mainForm.sortOrder = 0
  lastAutoMnemonic = ''
}

function handleAdd() {
  editingId.value = null
  resetMainForm()
  mainModalVisible.value = true
}

function handleEdit(record: any) {
  editingId.value = String(record.id)
  if (activeTab.value === 'brand') {
    mainForm.name = record.brandName || ''
    mainForm.mnemonicCode = record.mnemonicCode || ''
    mainForm.remark = record.remark || ''
  } else if (activeTab.value === 'unit') {
    mainForm.name = record.unitName || ''
    mainForm.mnemonicCode = record.mnemonicCode || ''
    mainForm.remark = record.remark || ''
    mainForm.isDefault = record.isDefault === 1
  } else {
    mainForm.name = record.tagName || ''
    mainForm.sortOrder = record.sortOrder ?? 0
  }
  lastAutoMnemonic = mainForm.mnemonicCode
  mainModalVisible.value = true
}

function closeMainModal() {
  mainModalVisible.value = false
}

async function handleModalSubmit() {
  try {
    await mainFormRef.value?.validate()
  } catch {
    return
  }
  modalLoading.value = true
  try {
    if (activeTab.value === 'brand') {
      const payload = {
        brandName: mainForm.name.trim(),
        mnemonicCode: mainForm.mnemonicCode,
        remark: mainForm.remark,
      }
      editingId.value
        ? await productBrandApi.update(editingId.value, payload)
        : await productBrandApi.create(payload)
    } else if (activeTab.value === 'unit') {
      const payload = {
        unitName: mainForm.name.trim(),
        mnemonicCode: mainForm.mnemonicCode,
        remark: mainForm.remark,
        isDefault: mainForm.isDefault ? 1 : 0,
      }
      editingId.value
        ? await productUnitDictApi.update(editingId.value, payload)
        : await productUnitDictApi.create(payload)
    } else {
      const payload = {
        tagName: mainForm.name.trim(),
        sortOrder: mainForm.sortOrder,
      }
      editingId.value
        ? await mallTagApi.update(editingId.value, payload)
        : await mallTagApi.create(payload)
    }
    message.success(editingId.value ? '修改成功' : '新增成功')
    mainModalVisible.value = false
    fetchData()
    if (unitGroupVisible.value) fetchUnitGroups()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    modalLoading.value = false
  }
}

// ── 删除 / 启停 ──
function handleDelete(record: any) {
  const name = activeTab.value === 'brand' ? record.brandName : record.unitName
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除「${name}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        activeTab.value === 'brand'
          ? await productBrandApi.delete(String(record.id))
          : await productUnitDictApi.delete(String(record.id))
        message.success('删除成功')
        fetchData()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}

function handleToggleTagStatus(record: any) {
  const nextStatus = record.status === 0 ? 1 : 0
  const action = nextStatus === 0 ? '停用' : '启用'
  Modal.confirm({
    title: `确认${action}`,
    content: `确定要${action}标签「${record.tagName}」吗？`,
    onOk: async () => {
      try {
        await mallTagApi.updateStatus(String(record.id), nextStatus)
        message.success(`${action}成功`)
        fetchData()
      } catch (e: any) {
        message.error(e?.message || `${action}失败`)
      }
    },
  })
}

// ── 打印（F8） / 导出 ──
// ═══ 打印（结果集打印） ═══
// 原先是 window.print() —— 打出来是整个后台界面（菜单/工具栏/翻页都跟着上纸）。
// 列取页面自己的列定义，模板按数据里的列画表头（改列不用改模板）。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'md-product-supplement',
  title: '商品补充资料',
  columns: () => brandColumns,
  rows: () => tableData.value,
  emptyTip: '没有可打印的数据',
})

async function handleExport() {
  try {
    const params = searchKeyword.value ? { keyword: searchKeyword.value } : {}
    const blob: any = activeTab.value === 'brand'
      ? await productBrandApi.exportFile(params)
      : activeTab.value === 'unit'
        ? await productUnitDictApi.exportFile(params)
        : await mallTagApi.exportFile(params)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = `${addLabel.value}_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) {
    console.error('[商品辅助资料] 导出失败', e)
    message.error('导出失败')
  }
}

// ── 单位组管理（对标：单位组管理弹窗 + 单位组新增编辑弹窗） ──
const unitGroupVisible = ref(false)
const unitGroupTableRef = ref()
const unitGroupLoading = ref(false)
const unitGroupExporting = ref(false)
const unitGroupData = ref<any[]>([])
const unitGroupKeyword = ref('')
/** 对标「显示状态」字典（ConstData.showstopstatus：全部 -1 / 已启用 2 / 已停用 1），默认「已启用」 */
const unitGroupStatusOptions = [
  { label: '全部', value: -1 },
  { label: '已启用', value: 2 },
  { label: '已停用', value: 1 },
]
const unitGroupStatus = ref<number>(2)

/** 对标状态值 → 本系统 status（1启用 / 0停用）；-1 表示全部不过滤 */
function toLocalUnitGroupStatus(v: number | undefined): number | undefined {
  if (v === 2) return 1
  if (v === 1) return 0
  return undefined
}
const unitGroupPagination = reactive({ current: 1, pageSize: 20, total: 0 })

/** 对标列：操作 | 单位 | 单位关系（仅此 3 列，均默认显示） */
const unitGroupColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50 },
  { key: 'action', title: '操作', type: 'action', slotName: 'ugActionCell', width: 160 },
  { key: 'unitNames', title: '单位', width: 260 },
  { key: 'unitRates', title: '单位关系' },
]

const unitGroupFormVisible = ref(false)
const unitGroupSaving = ref(false)
const unitGroupEditingId = ref<string | null>(null)

/** 对标「单位组新增编辑」固定 3 行：小单位 / 中单位 / 大单位 */
interface UnitGroupRow {
  unitType: 'SMALL' | 'MEDIUM' | 'LARGE'
  unitName: string
  unitId?: string
  conversionRate: number
}
const UNIT_GROUP_TYPES: Array<{ type: UnitGroupRow['unitType']; label: string }> = [
  { type: 'SMALL', label: '小单位' },
  { type: 'MEDIUM', label: '中单位' },
  { type: 'LARGE', label: '大单位' },
]
const unitGroupRows = ref<UnitGroupRow[]>([])

function emptyUnitGroupRows(): UnitGroupRow[] {
  return UNIT_GROUP_TYPES.map(t => ({
    unitType: t.type,
    unitName: '',
    unitId: undefined,
    conversionRate: 1,
  }))
}

function unitTypeLabel(type: string) {
  return UNIT_GROUP_TYPES.find(t => t.type === type)?.label || type
}

/** 选中单位名后回填单位字典 ID（明细引用字典，不复制字典） */
function onUnitNameChange(row: UnitGroupRow, value: string) {
  row.unitId = value ? unitDictIdMap.value[value] : undefined
}

// 单位字典下拉（单位组内成员来源，红线：不另建重复字典）
const unitDictOptions = ref<{ label: string; value: string }[]>([])
/** 单位名称 → 单位字典ID（提交 items 时回填 unitId 引用） */
const unitDictIdMap = ref<Record<string, string>>({})
const unitDictLoading = ref(false)
let unitDictLoaded = false

async function loadUnitDictOptions() {
  if (unitDictLoaded) return
  unitDictLoading.value = true
  try {
    const list = await productUnitDictApi.list()
    unitDictOptions.value = (list || []).map((u: any) => ({
      label: u.unitName,
      value: u.unitName,
    }))
    const map: Record<string, string> = {}
    for (const u of list || []) {
      if (u.unitName && u.id != null) map[u.unitName] = String(u.id)
    }
    unitDictIdMap.value = map
    unitDictLoaded = true
  } catch (e) {
    console.error('[商品辅助资料] 加载单位字典失败', e)
  } finally {
    unitDictLoading.value = false
  }
}

function openUnitGroupManager() {
  unitGroupVisible.value = true
  unitGroupKeyword.value = ''
  unitGroupStatus.value = 2
  unitGroupPagination.current = 1
  loadUnitDictOptions()
  fetchUnitGroups()
}

function handleUnitGroupSearch() {
  unitGroupPagination.current = 1
  fetchUnitGroups()
}

async function fetchUnitGroups() {
  unitGroupLoading.value = true
  try {
    const localStatus = toLocalUnitGroupStatus(unitGroupStatus.value)
    const res: any = await productUnitGroupApi.page({
      pageNum: unitGroupPagination.current,
      pageSize: unitGroupPagination.pageSize,
      ...(unitGroupKeyword.value ? { keyword: unitGroupKeyword.value } : {}),
      ...(localStatus === undefined ? {} : { status: localStatus }),
    })
    unitGroupData.value = res?.records || []
    unitGroupPagination.total = res?.total || 0
  } catch (e) {
    console.error('[商品辅助资料] 加载单位组失败', e)
    message.error('加载单位组失败')
    unitGroupData.value = []
    unitGroupPagination.total = 0
  } finally {
    unitGroupLoading.value = false
  }
}

/** 单位组导出（后端真实 xlsx，与查询条件同口径） */
async function handleExportUnitGroup() {
  unitGroupExporting.value = true
  try {
    const params: { keyword?: string; status?: number } = {}
    if (unitGroupKeyword.value) params.keyword = unitGroupKeyword.value
    const localStatus = toLocalUnitGroupStatus(unitGroupStatus.value)
    if (localStatus !== undefined) params.status = localStatus
    const blob: any = await productUnitGroupApi.exportFile(params)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = `商品单位组_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e: any) {
    console.error('[商品辅助资料] 单位组导出失败', e)
    message.error('导出失败')
  } finally {
    unitGroupExporting.value = false
  }
}

function handleUnitGroupPageChange(page: number, pageSize: number) {
  unitGroupPagination.current = page
  unitGroupPagination.pageSize = pageSize
  fetchUnitGroups()
}

function handleAddUnitGroup() {
  unitGroupEditingId.value = null
  unitGroupRows.value = emptyUnitGroupRows()
  loadUnitDictOptions()
  unitGroupFormVisible.value = true
}

async function handleEditUnitGroup(record: any) {
  unitGroupEditingId.value = String(record.id)
  unitGroupRows.value = emptyUnitGroupRows()
  loadUnitDictOptions()
  unitGroupFormVisible.value = true
  try {
    const detail: any = await productUnitGroupApi.detail(String(record.id))
    for (const item of detail?.items || []) {
      const row = unitGroupRows.value.find(r => r.unitType === item.unitType)
      if (row) {
        row.unitName = item.unitName || ''
        row.unitId = item.unitId ? String(item.unitId) : undefined
        row.conversionRate = Number(item.conversionRate ?? 1)
      }
    }
  } catch (e: any) {
    message.error(e?.message || '加载单位组明细失败')
  }
}

async function handleUnitGroupSubmit() {
  const small = unitGroupRows.value.find(r => r.unitType === 'SMALL')
  if (!small?.unitName) {
    message.warning('请选择小单位')
    return
  }
  const items = unitGroupRows.value
    .filter(r => !!r.unitName)
    .map((r, idx) => ({
      unitType: r.unitType,
      unitName: r.unitName,
      unitId: unitDictIdMap.value[r.unitName],
      conversionRate: r.unitType === 'SMALL' ? 1 : r.conversionRate,
      sortOrder: idx + 1,
    }))
  for (const item of items) {
    if (item.unitType !== 'SMALL' && (!item.conversionRate || item.conversionRate <= 1)) {
      message.warning('中单位/大单位的换算关系必须大于 1')
      return
    }
  }
  unitGroupSaving.value = true
  try {
    const payload = { status: 1, items }
    unitGroupEditingId.value
      ? await productUnitGroupApi.update(unitGroupEditingId.value, payload)
      : await productUnitGroupApi.create(payload)
    message.success(unitGroupEditingId.value ? '修改成功' : '新增成功')
    unitGroupFormVisible.value = false
    fetchUnitGroups()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    unitGroupSaving.value = false
  }
}

/** 行内「停用 / 启用」（对标 GoodsUnitTemplateList 操作列第二项） */
function handleToggleUnitGroupStatus(record: any) {
  const nextStatus = record.status === 1 ? 0 : 1
  const action = nextStatus === 0 ? '停用' : '启用'
  Modal.confirm({
    title: `确认${action}`,
    content: `确定要${action}单位组「${record.unitNames || ''}」吗？`,
    onOk: async () => {
      try {
        await productUnitGroupApi.updateStatus(String(record.id), nextStatus)
        message.success(`${action}成功`)
        fetchUnitGroups()
      } catch (e: any) {
        message.error(e?.message || `${action}失败`)
      }
    },
  })
}

function handleDeleteUnitGroup(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除单位组「${record.unitNames || ''}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await productUnitGroupApi.delete(String(record.id))
        message.success('删除成功')
        fetchUnitGroups()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}

// ── 快捷键：F8 打印 ──
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})

function handleError(err: any) {
  console.warn('[商品辅助资料] ErrorBoundary:', err)
}
</script>

<style scoped>
/* ── 查询行样式（必须页面自备）：骨架 CategoryListLayout 的 scoped 样式不作用于
       页面注入到 #search-fields 插槽的内容，不写则「筛选条件/输入框/查询」会纵向塌陷 ── */
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
  margin-left: 8px;
}

/* ── 数据表填充剩余高度（骨架 #table 插槽内） ── */
:deep(.bill-detail-table) {
  flex: 1;
  min-height: 0;
}

/* ── 是否默认标记 ── */
.aux-flag-on {
  color: #52c41a;
  font-weight: 600;
}
.aux-flag-off {
  color: #bfbfbf;
}

/* ── 长文本省略 ── */
.aux-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

/* ── 单位组弹窗工具栏 / 查询行（对标：左新增，右打印·导出·刷新） ── */
.ug-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}
.ug-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ug-query {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

/* ── 单位组新增编辑：固定 3 行编辑网格 ── */
.ug-edit-grid {
  width: 100%;
  border-collapse: collapse;
  margin-top: 8px;
}
.ug-edit-grid th,
.ug-edit-grid td {
  border: 1px solid #e8e8e8;
  padding: 6px 8px;
  font-size: 13px;
  text-align: left;
}
.ug-edit-grid th {
  background: #fafafa;
  font-weight: 600;
  color: #333;
}
.ug-edit-grid .ug-idx {
  color: #999;
}
.ug-help {
  color: #999;
  margin-left: 2px;
}
.ug-edit-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}
</style>
