<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        互联账号列表页（资料模块布局，对标 ql361「资料 → 往来单位 → 互联账号」）
        · 实抓口径（2026-09-11 22stable.ql361.com）：无分类树、无页面配置弹窗、无「新增」按钮
        · 工具栏：刷新 / 打印(F8) / 导出 / 更多（更多 = 批量解绑 / 批量删除）
        · 查询区：往来单位（放大镜 → 往来单位选择弹窗）+ 互联账号（请输入互联手机号码）+ 查询
        · 数据列（列配置齿轮：个人配置 / 全局配置）：往来单位 / 互联用户名 / 手机号
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="false"
      >
        <!-- ═══ 温馨提示（对标：与工具栏同排左侧） ═══ -->
        <template #toolbar-left>
          <div class="page-tip">
            温馨提示：此页面用于管理互联平台账号与对应往来单位的绑定关系。
          </div>
        </template>

        <!-- ═══ 工具栏右侧：刷新 / 打印(F8) / 导出 / 更多 ═══ -->
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
            <a-dropdown>
              <a-button size="small">
                更多 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="openBindModal">
                    绑定互联账号
                  </a-menu-item>
                  <a-menu-item
                    :disabled="!hasSelection"
                    @click="handleBatchUnbind"
                  >
                    批量解绑
                  </a-menu-item>
                  <a-menu-item
                    danger
                    :disabled="!hasSelection"
                    @click="handleBatchDelete"
                  >
                    批量删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标固定项，无页面配置弹窗） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-row">
              <!-- 往来单位：放大镜 → 往来单位选择弹窗（客户信息 / 供应商信息） -->
              <div
                class="field-box"
                :title="searchForm.partnerName || '点击选择往来单位'"
                @click="openPartnerPicker"
              >
                <span class="field-box-label">往来单位</span>
                <span
                  v-if="searchForm.partnerName"
                  class="field-box-value"
                >{{ searchForm.partnerName }}</span>
                <CloseCircleOutlined
                  v-if="searchForm.partnerId"
                  class="field-box-clear"
                  @click.stop="clearPartner"
                />
                <SearchOutlined class="field-box-icon" />
              </div>

              <!-- 互联账号：按手机号 / 互联用户名查询 -->
              <div class="field-box field-box-input">
                <span class="field-box-label">互联账号</span>
                <input
                  v-model="searchForm.keyword"
                  class="field-box-raw"
                  placeholder="请输入互联手机号码"
                  @keyup.enter="handleSearch"
                >
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
              :pagination="pagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              storage-key="md-linked-account-columns"
              row-key="id"
              @page-change="handlePageChange"
              @selection-change="handleSelectionChange"
            >
              <template #actionCell="{ record }">
                <!-- 占位空行（__ghost）不渲染行内操作，与对标空表一致 -->
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="handleUnbind(record)"
                  >
                    解绑
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>

      <!-- ═══ 往来单位选择弹窗（通用组件） ═══ -->
      <PartnerSelectModal
        v-model:open="partnerPickerVisible"
        title="往来单位选择"
        @select="handlePartnerSelected"
      />

      <!-- ═══ 绑定互联账号 ═══ -->
      <a-modal
        v-model:open="bindVisible"
        title="绑定互联账号"
        :width="640"
        :confirm-loading="bindLoading"
        ok-text="确定"
        cancel-text="取消"
        @ok="handleBindSubmit"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          class="bind-form"
        >
          <a-form-item
            label="往来单位"
            required
          >
            <div
              class="field-box bind-field"
              @click="bindPickerVisible = true"
            >
              <span
                v-if="bindForm.partyName"
                class="field-box-value"
              >{{ bindForm.partyName }}</span>
              <span
                v-else
                class="field-box-placeholder"
              >请选择往来单位</span>
              <SearchOutlined class="field-box-icon" />
            </div>
          </a-form-item>
          <a-form-item label="互联平台">
            <a-select
              v-model:value="bindForm.platform"
              :options="platformOptions"
              placeholder="请选择互联平台"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="关联类型">
            <a-select
              v-model:value="bindForm.linkType"
              :options="linkTypeOptions"
              placeholder="请选择关联类型"
              allow-clear
            />
          </a-form-item>
          <a-form-item
            label="互联用户名"
            required
          >
            <a-input
              v-model:value="bindForm.linkedUserName"
              placeholder="请输入互联用户名"
              :maxlength="128"
              allow-clear
            />
          </a-form-item>
          <a-form-item
            label="手机号"
            required
          >
            <a-input
              v-model:value="bindForm.phone"
              placeholder="请输入手机号"
              :maxlength="32"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="bindForm.remark"
              :rows="2"
              :maxlength="500"
              placeholder="请输入备注"
              allow-clear
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 绑定弹窗内的往来单位选择 -->
      <PartnerSelectModal
        v-model:open="bindPickerVisible"
        title="往来单位选择"
        @select="handleBindSelected"
      />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  DownOutlined,
  SearchOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PartnerSelectModal from '@/components/PartnerSelectModal/index.vue'
import { linkedAccountApi } from '@/api/erp/linkedAccount'
import type { LinkedAccountVO } from '@/api/erp/linkedAccount'
import request from '@/utils/request'

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<LinkedAccountVO[]>([])
const selectedRows = ref<LinkedAccountVO[]>([])
const hasSelection = computed(() => selectedRows.value.length > 0)

// ═══ 查询条件（对标固定项：往来单位 + 互联账号） ═══
const searchForm = reactive({
  partnerId: undefined as string | number | undefined,
  partnerName: '',
  keyword: '',
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 数据表列（对标全量 3 列，全部默认显示） ═══
const columns: any[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', slotName: 'actionCell', width: 130, fixed: 'left' },
  { title: '往来单位', key: 'partyName', width: 300 },
  { title: '互联用户名', key: 'linkedUserName', width: 220 },
  { title: '手机号', key: 'phone', width: 180 },
]

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    const res: any = await linkedAccountApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      partnerId: searchForm.partnerId,
      keyword: searchForm.keyword || undefined,
      // 默认只看「已绑定」；解绑后的记录（status=0）不再出现在列表，供营销/会员/商城按状态引用
      status: 1,
    })
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.error('[互联账号] 加载列表失败', error)
    message.error(error?.response?.data?.message || error?.message || '加载列表失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件 ═══
function handleSearch() {
  pagination.current = 1
  selectedRows.value = []
  fetchList()
}

function handleRefresh() {
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  // 翻页后清空勾选：否则「批量解绑/批量删除」会作用到已不在当前页的旧行
  selectedRows.value = []
  fetchList()
}

function handleSelectionChange(rows: LinkedAccountVO[]) {
  selectedRows.value = rows || []
}

// ═══ 往来单位选择（查询条件） ═══
const partnerPickerVisible = ref(false)

function openPartnerPicker() {
  partnerPickerVisible.value = true
}

function handlePartnerSelected(record: any) {
  searchForm.partnerId = record?.id
  searchForm.partnerName = record?.partnerName || record?.partyName || ''
  handleSearch()
}

function clearPartner() {
  searchForm.partnerId = undefined
  searchForm.partnerName = ''
  handleSearch()
}

// ═══ 解绑（状态切换：1 已绑定 → 0 已解绑） ═══
function handleUnbind(record: LinkedAccountVO) {
  Modal.confirm({
    title: '确认解绑',
    content: `确定要解绑「${record.partyName || ''}」的互联账号「${record.linkedUserName || ''}」吗？`,
    onOk: async () => {
      try {
        await linkedAccountApi.updateStatus(record.id, 0)
        message.success('解绑成功')
        selectedRows.value = []
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || error?.message || '解绑失败')
      }
    },
  })
}

// ═══ 删除 ═══
function handleDelete(record: LinkedAccountVO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除互联账号「${record.linkedUserName || ''}」吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await linkedAccountApi.delete(record.id)
        message.success('删除成功')
        selectedRows.value = []
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || error?.message || '删除失败')
      }
    },
  })
}

// ═══ 批量解绑 / 批量删除（更多） ═══
function handleBatchUnbind() {
  if (!hasSelection.value) {
    message.warning('请先选择要解绑的记录')
    return
  }
  Modal.confirm({
    title: '批量解绑',
    content: `确定要解绑选中的 ${selectedRows.value.length} 条互联账号吗？`,
    onOk: async () => {
      try {
        await linkedAccountApi.batchStatus(selectedRows.value.map(r => r.id), 0)
        message.success('批量解绑成功')
        selectedRows.value = []
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || error?.message || '批量解绑失败')
      }
    },
  })
}

function handleBatchDelete() {
  if (!hasSelection.value) {
    message.warning('请先选择要删除的记录')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRows.value.length} 条互联账号吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await linkedAccountApi.batchDelete(selectedRows.value.map(r => r.id))
        message.success('批量删除成功')
        selectedRows.value = []
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || error?.message || '批量删除失败')
      }
    },
  })
}

// ═══ 绑定互联账号 ═══
const bindVisible = ref(false)
const bindLoading = ref(false)
const bindPickerVisible = ref(false)
const bindForm = reactive({
  partyId: undefined as string | number | undefined,
  partyCode: '',
  partyName: '',
  platform: undefined as string | undefined,
  linkType: undefined as string | undefined,
  linkedUserName: '',
  phone: '',
  remark: '',
})

/** 字典（互联平台 / 关联类型）——全局唯一，由后端 /dict 单点提供 */
const platformOptions = ref<Array<{ label: string; value: string }>>([])
const linkTypeOptions = ref<Array<{ label: string; value: string }>>([])

async function loadDict() {
  try {
    const dict: any = await linkedAccountApi.dict()
    platformOptions.value = dict?.platforms || []
    linkTypeOptions.value = dict?.linkTypes || []
  } catch (error) {
    console.error('[互联账号] 加载字典失败', error)
  }
}

function openBindModal() {
  bindForm.partyId = undefined
  bindForm.partyCode = ''
  bindForm.partyName = ''
  bindForm.platform = undefined
  bindForm.linkType = undefined
  bindForm.linkedUserName = ''
  bindForm.phone = ''
  bindForm.remark = ''
  bindVisible.value = true
}

function handleBindSelected(record: any) {
  bindForm.partyId = record?.id
  bindForm.partyCode = record?.partnerCode || ''
  bindForm.partyName = record?.partnerName || ''
}

async function handleBindSubmit() {
  if (!bindForm.partyId) {
    message.warning('请选择往来单位')
    return
  }
  if (!bindForm.linkedUserName) {
    message.warning('请输入互联用户名')
    return
  }
  if (!bindForm.phone) {
    message.warning('请输入手机号')
    return
  }
  bindLoading.value = true
  try {
    const created: any = await linkedAccountApi.create({
      partyId: bindForm.partyId,
      partyCode: bindForm.partyCode,
      partyName: bindForm.partyName,
      platform: bindForm.platform || 'OTHER',
      linkType: bindForm.linkType || 'MEMBER',
      linkedUserName: bindForm.linkedUserName,
      phone: bindForm.phone,
      remark: bindForm.remark,
    })
    if (!created) {
      message.error('绑定失败（请确认该平台下互联用户名未被占用）')
      return
    }
    message.success('绑定成功')
    bindVisible.value = false
    handleSearch()
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '绑定失败')
  } finally {
    bindLoading.value = false
  }
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

// ═══ 导出（真实 Excel：后端 /erp/md/linked-account/export） ═══
async function handleExport() {
  try {
    const params: Record<string, any> = { title: '互联账号', status: 1 }
    if (searchForm.partnerId) params.partyId = searchForm.partnerId
    if (searchForm.keyword) params.keyword = searchForm.keyword

    const blob: any = await request.get('/erp/md/linked-account/export', { responseType: 'blob', params })
    if (blob?.type && String(blob.type).includes('application/json')) {
      message.warning('没有可导出的数据')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    // 用本机日期（toISOString 是 UTC，东八区晚间会差一天）
    const d = new Date()
    const today = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    a.download = `互联账号_${today}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '导出失败')
  }
}

function handleError(error: Error) {
  console.error('[互联账号] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadDict()
  fetchList()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
/* ── 温馨提示（对标：浅黄底 + 橙棕字，与工具栏同排） ── */
.page-tip {
  display: inline-block;
  padding: 3px 10px;
  background: #fdf6e3;
  border: 1px solid #f0e2b8;
  border-radius: 2px;
  font-size: 13px;
  color: #b8860b;
  white-space: nowrap;
}

/* ── 查询区 ── */
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

/* 对标字段框：标签在框内左侧 + 右侧放大镜 */
.field-box {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  min-width: 200px;
  padding: 0 8px;
  border: 1px solid #d9d9d9;
  border-radius: 2px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
}
.field-box:hover { border-color: #2f5aa8; }
.field-box-label { color: #333; white-space: nowrap; }
.field-box-value { color: #2f5aa8; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.field-box-placeholder { color: #bfbfbf; }
.field-box-icon { margin-left: auto; color: #999; }
.field-box-clear { color: #bfbfbf; }
.field-box-clear:hover { color: #ff4d4f; }
.field-box-input { min-width: 220px; cursor: text; }
.field-box-raw {
  flex: 1;
  min-width: 110px;
  border: none;
  outline: none;
  font-size: 13px;
  background: transparent;
}
.field-box-raw::placeholder { color: #bfbfbf; }

.bind-field { width: 100%; min-width: 0; }

.table-area { flex: 1; min-height: 0; overflow: hidden; }

/* 紧凑尺寸（资料模块统一） */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
