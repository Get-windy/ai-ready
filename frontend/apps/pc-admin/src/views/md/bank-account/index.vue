<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="false"
      >
        <!-- ═══ 工具栏：新增银行（左） / 树展开收起 + 刷新 + 打印(F8) + 导出（右） ═══ -->
        <template #toolbar-left>
          <a-button
            type="primary"
            class="btn-add"
            @click="openCreate"
          >
            <PlusOutlined /> 新增银行
          </a-button>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip
              title="全部展开"
              placement="bottom"
            >
              <a-button
                size="small"
                :disabled="!hasTreeRows"
                @click="expandAll"
              >
                <DownSquareOutlined />
              </a-button>
            </a-tooltip>
            <a-tooltip
              title="全部收起"
              placement="bottom"
            >
              <a-button
                size="small"
                :disabled="!hasTreeRows"
                @click="collapseAll"
              >
                <RightSquareOutlined />
              </a-button>
            </a-tooltip>
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
          </a-space>
        </template>

        <!-- ═══ 查询区：筛选条件 + 查询 + 显示停用 + 显示层次结构（对标 ql361 页面固定项） ═══ -->
        <template #search-fields>
          <div class="search-row">
            <span class="search-label">筛选条件</span>
            <a-input
              v-model:value="searchForm.keyword"
              placeholder="请输入科目编号/科目名称"
              size="small"
              style="width: 220px"
              allow-clear
              @press-enter="handleSearch"
            />
            <a-button
              type="primary"
              size="small"
              class="btn-search"
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
            <a-checkbox
              v-model:checked="searchForm.showTree"
              @change="handleSearch"
            >
              显示层次结构
            </a-checkbox>
          </div>
        </template>

        <!-- ═══ 数据表格：列配置走表头齿轮（个人配置 / 全局配置，对齐 ql361 列配置弹窗） ═══ -->
        <template #table>
          <BillDetailTable
            :columns="columns"
            :data-source="displayRows"
            :loading="loading"
            :view-mode="true"
            :storage-key="storageKey"
            :global-config-key="storageKey"
            :show-pagination="true"
            v-model:current="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            @page-change="handlePageChange"
          >
            <!-- 树形列：展开箭头 + 文件夹/叶子图标（对标 ql361 行首图标列） -->
            <template #treeCell="{ record }">
              <span
                v-if="!record.__ghost"
                class="tree-cell"
              >
                <a
                  v-if="record.hasChildren"
                  class="tree-toggle"
                  @click="toggleExpand(record)"
                >{{ expandedIds.has(record.id) ? '▾' : '▸' }}</a>
                <FolderOpenOutlined
                  v-if="record.hasChildren"
                  class="tree-icon folder"
                />
                <FileOutlined
                  v-else
                  class="tree-icon leaf"
                />
              </span>
            </template>

            <!-- 科目名称：按层级缩进 + 点击查看 -->
            <template #nameCell="{ record }">
              <span
                v-if="!record.__ghost"
                class="name-cell"
                :style="{ paddingLeft: `${nameIndent(record)}px` }"
              >
                <a
                  class="cell-link"
                  @click="openView(record)"
                >{{ record.accountName }}</a>
              </span>
            </template>

            <!-- 账户类型 -->
            <template #accountTypeCell="{ record }">
              <a-tag
                v-if="!record.__ghost && record.accountType"
                :color="ACCOUNT_TYPE_MAP[record.accountType]?.color"
              >
                {{ ACCOUNT_TYPE_MAP[record.accountType]?.label || record.accountType }}
              </a-tag>
              <span v-else>-</span>
            </template>

            <!-- 是否用于商城线下转账收款 -->
            <template #mallCell="{ record }">
              <span v-if="!record.__ghost">{{ record.mallTransferEnabled === 1 ? '是' : '否' }}</span>
            </template>

            <!-- 状态 -->
            <template #statusCell="{ record }">
              <a-tag
                v-if="!record.__ghost"
                :color="record.status === 1 ? 'green' : 'default'"
              >
                {{ record.status === 1 ? '启用' : '停用' }}
              </a-tag>
            </template>

            <!-- 操作列：修改 / 删除 / 更多（对标 ql361 行级操作） -->
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
                      <a-menu-item @click="openView(record)">
                        查看详情
                      </a-menu-item>
                      <a-menu-item @click="handleToggleStatus(record)">
                        {{ record.status === 1 ? '停用' : '启用' }}
                      </a-menu-item>
                      <a-menu-item @click="handleCopyCode(record)">
                        复制科目编号
                      </a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </template>
          </BillDetailTable>

          <!-- 底部：当前路径（对标 ql361 表尾左侧） -->
          <div class="path-strip">
            <span class="breadcrumb-text">当前路径：</span>
            <span class="breadcrumb-path">{{ currentPath }}</span>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 新增 / 修改 / 查看：银行信息弹窗（对标 ql361「银行信息」表单） ═══ -->
    <a-modal
      v-model:open="formVisible"
      :title="formTitle"
      :width="720"
      :ok-text="formMode === 'view' ? '关闭' : '保存(Enter)'"
      :cancel-text="'关闭(Esc)'"
      :cancel-button-props="cancelBtnProps"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
        :disabled="formMode === 'view'"
      >
        <a-row :gutter="0">
          <a-col :span="12">
            <a-form-item
              label="银行编号"
              name="subjectCode"
            >
              <a-input
                v-model:value="form.subjectCode"
                placeholder="自动生成，可修改"
                :disabled="formMode === 'view' || form.isSystem === 1"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="银行全称"
              name="accountName"
            >
              <a-input
                v-model:value="form.accountName"
                placeholder="请输入银行全称"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="账户类型"
              name="accountType"
            >
              <a-select
                v-model:value="form.accountType"
                placeholder="请选择账户类型"
                :options="accountTypeOptions"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="上级账户"
              name="parentId"
            >
              <a-tree-select
                v-model:value="form.parentId"
                :tree-data="parentTreeData"
                :field-names="{ children: 'children', label: 'accountName', value: 'id' }"
                placeholder="不选则为顶级账户"
                allow-clear
                tree-default-expand-all
                :dropdown-style="{ maxHeight: '260px', overflow: 'auto' }"
                @change="handleParentChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="助记码"
              name="easyCode"
            >
              <a-input
                v-model:value="form.easyCode"
                placeholder="请输入助记码"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="银行简称"
              name="briefName"
            >
              <a-input
                v-model:value="form.briefName"
                placeholder="请输入银行简称"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="开户行"
              name="bankName"
            >
              <a-input
                v-model:value="form.bankName"
                placeholder="请输入开户行"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="户主名"
              name="accountHolder"
            >
              <a-input
                v-model:value="form.accountHolder"
                placeholder="请输入户主名"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="银行账号"
              name="bankAccount"
            >
              <a-input
                v-model:value="form.bankAccount"
                placeholder="请输入银行账号"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="币种"
              name="currency"
            >
              <a-input
                v-model:value="form.currency"
                placeholder="默认 CNY"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="二维码"
              :label-col="{ span: 3 }"
              :wrapper-col="{ span: 20 }"
            >
              <div class="qrcode-row">
                <a-upload
                  :show-upload-list="false"
                  :before-upload="beforeQrcodeUpload"
                  accept="image/*"
                >
                  <a-button size="small">
                    <UploadOutlined /> 上传收款码
                  </a-button>
                </a-upload>
                <a-input
                  v-model:value="form.qrcodeUrl"
                  placeholder="收款码地址（上传后自动回填）"
                  style="flex: 1"
                />
                <img
                  v-if="form.qrcodeUrl"
                  :src="form.qrcodeUrl"
                  class="qrcode-preview"
                  alt="收款码"
                >
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="是否用于商城线下转账收款"
              name="mallTransferEnabled"
              :label-col="{ span: 14 }"
              :wrapper-col="{ span: 9 }"
            >
              <a-switch
                :checked="form.mallTransferEnabled === 1"
                checked-children="是"
                un-checked-children="否"
                @change="(v: any) => (form.mallTransferEnabled = v ? 1 : 0)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="状态"
              name="status"
            >
              <a-switch
                :checked="form.status === 1"
                checked-children="启用"
                un-checked-children="停用"
                @change="(v: any) => (form.status = v ? 1 : 0)"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="排序"
              name="sortNo"
            >
              <a-input-number
                v-model:value="form.sortNo"
                :min="0"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="备注"
              :label-col="{ span: 3 }"
              :wrapper-col="{ span: 20 }"
            >
              <a-textarea
                v-model:value="form.remark"
                :rows="2"
                placeholder="请输入备注"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PlusOutlined,
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  UploadOutlined,
  FolderOpenOutlined,
  FileOutlined,
  DownSquareOutlined,
  RightSquareOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { bankAccountApi, type BankAccountInfo } from '@/api/md'
import request from '@/utils/request'

defineOptions({ name: 'MdBankAccount' })

const storageKey = 'md-bank-account-columns'
const handleError = (e: any) => console.warn('[银行账户] ErrorBoundary:', e)

// ═══ 账户类型字典（与后端 FinanceAccount.accountType 注释一致） ═══
const ACCOUNT_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '银行账户', color: 'blue' },
  2: { label: '现金账户', color: 'green' },
  3: { label: '内部账户', color: 'purple' },
  4: { label: '外部账户', color: 'orange' },
}
const accountTypeOptions = Object.entries(ACCOUNT_TYPE_MAP).map(([value, v]) => ({
  label: v.label,
  value: Number(value),
}))

// ═══ 查询区（对标 ql361：筛选条件 + 显示停用 + 显示层次结构，层次结构默认勾选） ═══
const searchForm = reactive({
  keyword: '',
  showDisabled: false,
  showTree: true,
})

// ═══ 表格列：对标 ql361 列配置弹窗 4 列（默认显示全部）
//     另按本系统 finance_account 字段补充可选列（默认隐藏，可在表头齿轮「个人/全局配置」勾选） ═══
// 不传 :min-rows —— 使用 BillDetailTable 组件默认 20 行：数据不足 20 条时补 __ghost 空行，
// 与对标 ql361「每页固定 20 行」及《会计科目》《费用类型》等资料模块列表页口径一致；
// 本页所有 slot（treeCell/nameCell/accountTypeCell/mallCell/statusCell/actionCell）均已做 !record.__ghost 保护。
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 50, fixed: 'left' },
  { key: 'treeFlag', title: '', type: 'slot', slotName: 'treeCell', width: 46, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 150, fixed: 'left' },
  { key: 'subjectCode', title: '科目编号', width: 140 },
  { key: 'accountName', title: '科目名称', type: 'slot', slotName: 'nameCell', width: 240 },
  { key: 'accountType', title: '账户类型', type: 'slot', slotName: 'accountTypeCell', width: 120 },
  { key: 'mallTransferEnabled', title: '是否用于商城线下转账收款', type: 'slot', slotName: 'mallCell', width: 200 },
  // 本系统补充列（状态为「显示停用」勾选项的可视化依据，默认显示；其余默认隐藏）
  { key: 'status', title: '状态', type: 'slot', slotName: 'statusCell', width: 90 },
  { key: 'bankName', title: '开户行', width: 150, defaultHidden: true },
  { key: 'bankAccount', title: '银行账号', width: 180, defaultHidden: true },
  { key: 'accountHolder', title: '户主名', width: 120, defaultHidden: true },
  { key: 'easyCode', title: '助记码', width: 110, defaultHidden: true },
  { key: 'briefName', title: '银行简称', width: 130, defaultHidden: true },
  { key: 'currency', title: '币种', width: 80, defaultHidden: true },
  { key: 'balance', title: '账户余额', width: 120, align: 'right', defaultHidden: true, formatter: (v: any) => formatMoney(v) },
  { key: 'sortNo', title: '排序', width: 80, align: 'right', defaultHidden: true },
  { key: 'remark', title: '备注', width: 180, defaultHidden: true },
]

// ═══ 数据状态 ═══
const loading = ref(false)
const exporting = ref(false)
const rawRows = ref<BankAccountInfo[]>([])
const expandedIds = ref<Set<number>>(new Set())
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

/** 显示行：显示层次结构时按展开状态过滤（收起的节点隐藏其后代） */
const displayRows = computed(() => {
  if (!searchForm.showTree) return rawRows.value
  const byId = new Map<number, BankAccountInfo>()
  rawRows.value.forEach(r => byId.set(r.id, r))
  const hidden = new Set<number>()
  for (const row of rawRows.value) {
    let pid = row.parentId ?? null
    let guard = 0
    while (pid != null && byId.has(pid) && guard++ < 50) {
      if (!expandedIds.value.has(pid)) {
        hidden.add(row.id)
        break
      }
      pid = byId.get(pid)?.parentId ?? null
    }
  }
  return rawRows.value.filter(r => !hidden.has(r.id))
})

const hasTreeRows = computed(() => rawRows.value.some(r => r.hasChildren))

/** 当前路径（对标 ql361 表尾「当前路径」；本页无左侧分类树，始终为筛选范围） */
const currentPath = computed(() => (searchForm.keyword ? `全部 / 筛选：${searchForm.keyword}` : '全部'))

function nameIndent(record: BankAccountInfo): number {
  if (!searchForm.showTree) return 0
  const level = record.level || 1
  return Math.max(0, level - 1) * 16
}

function formatMoney(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据加载 ═══
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
    showDisabled: searchForm.showDisabled ? 1 : 0,
    showTree: searchForm.showTree ? 1 : 0,
  }
  if (searchForm.keyword) params.keyword = searchForm.keyword
  return params
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await bankAccountApi.page(buildParams())
    const page: any = res?.records ? res : { records: [], total: 0 }
    rawRows.value = (page.records || []) as BankAccountInfo[]
    pagination.total = Number(page.total) || 0
    // 默认全部展开（对标「显示层次结构」默认勾选）
    expandedIds.value = new Set(rawRows.value.filter(r => r.hasChildren).map(r => r.id))
  } catch (e) {
    console.warn('[银行账户] 加载失败', e)
    rawRows.value = []
    pagination.total = 0
    message.error('查询失败，请检查网络后重试')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(p: { page: number; pageSize: number }) {
  pagination.current = p.page
  pagination.pageSize = p.pageSize
  fetchData()
}

function refreshAll() {
  fetchData()
}

// ═══ 树展开/收起 ═══
function toggleExpand(record: BankAccountInfo) {
  const next = new Set(expandedIds.value)
  if (next.has(record.id)) next.delete(record.id)
  else next.add(record.id)
  expandedIds.value = next
}

function expandAll() {
  expandedIds.value = new Set(rawRows.value.filter(r => r.hasChildren).map(r => r.id))
}

function collapseAll() {
  expandedIds.value = new Set()
}

// ═══ 新增 / 修改 / 查看弹窗 ═══
const formVisible = ref(false)
const saving = ref(false)
const formMode = ref<'create' | 'edit' | 'view'>('create')
const formRef = ref<any>(null)
const parentTreeData = ref<any[]>([])

const emptyForm = () => ({
  id: undefined as number | undefined,
  subjectCode: '',
  accountName: '',
  accountType: 1,
  parentId: undefined as number | undefined,
  easyCode: '',
  briefName: '',
  bankName: '',
  accountHolder: '',
  bankAccount: '',
  currency: 'CNY',
  qrcodeUrl: '',
  mallTransferEnabled: 0,
  status: 1,
  isSystem: 0,
  sortNo: 0,
  remark: '',
})
const form = reactive(emptyForm())

const formRules: Record<string, any> = {
  subjectCode: [{ required: true, message: '请输入银行编号', trigger: 'blur' }],
  accountName: [{ required: true, message: '请输入银行全称', trigger: 'blur' }],
  accountType: [{ required: true, message: '请选择账户类型', trigger: 'change' }],
}

const formTitle = computed(() => (formMode.value === 'create' ? '银行信息' : formMode.value === 'edit' ? '银行信息--编辑' : '银行信息--查看'))

/** 查看态隐藏「关闭(Esc)」按钮（只保留一个关闭动作，避免文案重复） */
const cancelBtnProps = computed<any>(() => (formMode.value === 'view' ? { style: { display: 'none' } } : {}))

/** 上级账户下拉树（排除自身，避免形成环） */
function buildParentTree(list: BankAccountInfo[], excludeId?: number): any[] {
  const usable = list.filter(a => a.id !== excludeId)
  const byParent = new Map<any, BankAccountInfo[]>()
  usable.forEach(a => {
    const key = a.parentId ?? null
    if (!byParent.has(key)) byParent.set(key, [])
    byParent.get(key)!.push(a)
  })
  const build = (key: any): any[] =>
    (byParent.get(key) || []).map(a => ({ id: a.id, accountName: `${a.subjectCode ? a.subjectCode + ' ' : ''}${a.accountName}`, children: build(a.id) }))
  return build(null)
}

async function loadParentOptions(excludeId?: number) {
  try {
    const res: any = await bankAccountApi.options()
    const list: BankAccountInfo[] = Array.isArray(res) ? res : res?.data || []
    parentTreeData.value = buildParentTree(list, excludeId)
  } catch (e) {
    console.warn('[银行账户] 上级账户下拉加载失败', e)
    parentTreeData.value = []
  }
}

async function openCreate() {
  formMode.value = 'create'
  Object.assign(form, emptyForm())
  formVisible.value = true
  await loadParentOptions()
  await refreshNextCode()
}

/** 生成下一个银行编号（随上级账户变化：子账户编号 = 父编号 + 两位序号） */
async function refreshNextCode() {
  try {
    const res: any = await bankAccountApi.nextCode(form.parentId)
    form.subjectCode = typeof res === 'string' ? res : (res?.data || '')
  } catch (e) {
    console.warn('[银行账户] 生成银行编号失败', e)
  }
}

async function handleParentChange() {
  if (formMode.value !== 'create') return
  await refreshNextCode()
}

async function openEdit(record: BankAccountInfo) {
  formMode.value = 'edit'
  Object.assign(form, emptyForm(), {
    id: record.id,
    subjectCode: record.subjectCode || '',
    accountName: record.accountName || '',
    accountType: record.accountType ?? 1,
    parentId: record.parentId ?? undefined,
    easyCode: record.easyCode || '',
    briefName: record.briefName || '',
    bankName: record.bankName || '',
    accountHolder: record.accountHolder || '',
    bankAccount: record.bankAccount || '',
    currency: record.currency || 'CNY',
    qrcodeUrl: record.qrcodeUrl || '',
    mallTransferEnabled: record.mallTransferEnabled ?? 0,
    status: record.status ?? 1,
    isSystem: record.isSystem ?? 0,
    sortNo: record.sortNo ?? 0,
    remark: record.remark || '',
  })
  formVisible.value = true
  await loadParentOptions(record.id)
}

async function openView(record: BankAccountInfo) {
  await openEdit(record)
  formMode.value = 'view'
}

async function handleSave() {
  if (formMode.value === 'view') {
    formVisible.value = false
    return
  }
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload: Partial<BankAccountInfo> = {
      subjectCode: form.subjectCode,
      accountName: form.accountName,
      accountType: form.accountType,
      parentId: form.parentId ?? null,
      easyCode: form.easyCode,
      briefName: form.briefName,
      bankName: form.bankName,
      accountHolder: form.accountHolder,
      bankAccount: form.bankAccount,
      currency: form.currency,
      qrcodeUrl: form.qrcodeUrl,
      mallTransferEnabled: form.mallTransferEnabled,
      status: form.status,
      sortNo: form.sortNo,
      remark: form.remark,
    }
    if (formMode.value === 'edit' && form.id) {
      await bankAccountApi.update(form.id, payload)
      message.success('修改成功')
    } else {
      await bankAccountApi.create(payload)
      message.success('新增成功')
    }
    formVisible.value = false
    fetchData()
  } catch (e: any) {
    console.warn('[银行账户] 保存失败', e)
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function beforeQrcodeUpload(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res: any = await request.post('/file/upload', formData)
    const url = typeof res === 'string' ? res : res?.url || res?.data?.url
    if (!url) {
      message.error('收款码上传失败')
      return false
    }
    form.qrcodeUrl = url
    message.success('收款码上传成功')
  } catch {
    message.error('收款码上传失败')
  }
  return false
}

// ═══ 行操作 ═══
function handleDelete(record: BankAccountInfo) {
  Modal.confirm({
    title: '删除银行账户',
    content: `确认删除「${record.subjectCode || ''} ${record.accountName}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await bankAccountApi.remove(record.id)
        message.success('删除成功')
        fetchData()
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}

async function handleToggleStatus(record: BankAccountInfo) {
  const target = record.status === 1 ? 0 : 1
  try {
    await bankAccountApi.updateStatus(record.id, target)
    message.success(target === 1 ? '已启用' : '已停用')
    fetchData()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

async function handleCopyCode(record: BankAccountInfo) {
  const code = record.subjectCode || ''
  if (!code) {
    message.warning('该账户无科目编号')
    return
  }
  try {
    await navigator.clipboard.writeText(code)
    message.success(`已复制：${code}`)
  } catch {
    message.warning('浏览器不支持剪贴板，请手动复制')
  }
}

// ═══ 导出（后端真实 xlsx） ═══
async function handleExport() {
  exporting.value = true
  try {
    const blob: any = await bankAccountApi.export({
      keyword: searchForm.keyword || undefined,
      showDisabled: searchForm.showDisabled ? 1 : 0,
      showTree: 0,
    })
    if (!(blob instanceof Blob) || blob.size === 0) {
      message.warning('暂无可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `银行账户_${dayjs().format('YYYYMMDD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (e) {
    console.warn('[银行账户] 导出失败', e)
    message.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// ═══ 打印(F8)：按当前查询结果渲染列表后打印 ═══
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  if (!displayRows.value.length) {
    message.warning('没有可打印的数据')
    return
  }
  const rows = displayRows.value.map((r, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.subjectCode)}</td>
      <td>${escapeHtml(r.accountName)}</td>
      <td>${escapeHtml(ACCOUNT_TYPE_MAP[r.accountType || 0]?.label || '')}</td>
      <td>${r.mallTransferEnabled === 1 ? '是' : '否'}</td>
      <td>${escapeHtml(r.bankName)}</td>
      <td>${escapeHtml(r.bankAccount)}</td>
      <td class="num">${formatMoney(r.balance)}</td>
      <td>${r.status === 1 ? '启用' : '停用'}</td>
    </tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" />
    <title>银行账户</title>
    <style>
      body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px;color:#000}
      h2{text-align:center;margin:0 0 12px;font-size:18px}
      .meta{display:flex;flex-wrap:wrap;gap:4px 24px;font-size:12px;margin-bottom:8px}
      table{width:100%;border-collapse:collapse;font-size:12px}
      th,td{border:1px solid #999;padding:4px 6px;text-align:left}
      th{background:#f2f2f2}
      .num{text-align:right}
    </style></head><body>
    <h2>银行账户</h2>
    <div class="meta">
      <span>筛选条件：${escapeHtml(searchForm.keyword || '全部')}</span>
      <span>打印时间：${dayjs().format('YYYY-MM-DD HH:mm')}</span>
      <span>记录数：${displayRows.value.length}</span>
    </div>
    <table>
      <thead><tr>
        <th>#</th><th>科目编号</th><th>科目名称</th><th>账户类型</th>
        <th>是否用于商城线下转账收款</th><th>开户行</th><th>银行账号</th>
        <th>账户余额</th><th>状态</th>
      </tr></thead>
      <tbody>${rows}</tbody>
    </table></body></html>`
  const win = window.open('', '_blank', 'width=1000,height=700')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

// ═══ F8 快捷键 ═══
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

onMounted(() => {
  fetchData()
  window.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.btn-search {
  margin-left: 4px;
}
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
  color: #fff !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}
.tree-cell {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.tree-toggle {
  color: #666;
  cursor: pointer;
  font-size: 12px;
  width: 14px;
  display: inline-block;
  text-align: center;
}
.tree-icon {
  font-size: 14px;
}
.tree-icon.folder {
  color: #faad14;
}
.tree-icon.leaf {
  color: #bfbfbf;
}
.name-cell {
  display: inline-block;
}
.cell-link {
  color: #1890ff;
  cursor: pointer;
}
.cell-link:hover {
  text-decoration: underline;
}
.qrcode-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.qrcode-preview {
  width: 48px;
  height: 48px;
  object-fit: contain;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}
.path-strip {
  padding: 6px 12px;
  font-size: 12px;
  color: #888;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}
.breadcrumb-text {
  color: #888;
}
.breadcrumb-path {
  color: #409eff;
}
</style>
