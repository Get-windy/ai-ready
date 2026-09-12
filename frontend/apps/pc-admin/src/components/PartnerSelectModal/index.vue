<template>
  <a-modal
    :open="open"
    :title="title"
    :width="width"
    :footer="null"
    :destroy-on-close="true"
    :mask-closable="false"
    @cancel="handleCancel"
  >
    <!-- 双 Tab：客户信息 / 供应商信息（对标「往来单位选择」弹窗） -->
    <div class="psm-tabs">
      <div
        v-for="tab in TABS"
        :key="tab.key"
        :class="['psm-tab', { active: activeTab === tab.key }]"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </div>
    </div>

    <!-- 查询区 -->
    <div class="psm-search">
      <a-input
        v-model:value="keyword"
        :placeholder="placeholder"
        size="small"
        allow-clear
        style="width: 260px"
        @press-enter="handleSearch"
      />
      <a-checkbox
        v-model:checked="showHierarchy"
        @change="handleSearch"
      >
        显示层次结构
      </a-checkbox>
      <a-button
        type="primary"
        size="small"
        @click="handleSearch"
      >
        查询
      </a-button>
    </div>

    <!-- 候选数据表 -->
    <a-table
      :columns="tableColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="false"
      :row-key="rowKey"
      :custom-row="customRow"
      :row-class-name="rowClassName"
      :table-layout="'fixed'"
      size="small"
      bordered
    />

    <!-- 分页 + 确定 / 取消 -->
    <div class="psm-footer">
      <StandardPagination
        :current="pagination.current"
        :page-size="pagination.pageSize"
        :total="pagination.total"
        :page-size-options="[10, 11, 20, 50, 100]"
        @change="handlePageChange"
      />
      <div class="psm-footer-btns">
        <a-button
          type="primary"
          :disabled="!selectedRecord"
          @click="handleConfirm"
        >
          确定(Enter)
        </a-button>
        <a-button @click="handleCancel">
          取消(Esc)
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
/**
 * PartnerSelectModal — 通用「往来单位选择」弹窗
 *
 * 对标 ql361 往来单位选择弹窗（资料/单据类页面的往来单位字段「放大镜」触发器）：
 * 双 Tab（客户信息 / 供应商信息）+ 关键字查询 + 显示层次结构 + 编号/名称/联系人/
 * 联系电话/联系地址/备注 六列 + 分页 + 确定(Enter)/取消(Esc)。
 *
 * 数据源统一走 /erp/md/customer/page（往来单位单一口径），禁止各页面另建候选接口。
 */
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { Modal } from 'ant-design-vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import { partnerApi } from '@/api/erp/partner'

const TABS = [
  { key: 'customer', label: '客户信息' },
  { key: 'supplier', label: '供应商信息' },
] as const

const props = withDefaults(defineProps<{
  open: boolean
  title?: string
  /** 初始 Tab：客户信息 / 供应商信息 */
  defaultTab?: 'customer' | 'supplier'
  placeholder?: string
  width?: number
  rowKey?: string
}>(), {
  title: '往来单位选择',
  defaultTab: 'customer',
  placeholder: '请输入名称/编号/备注',
  width: 940,
  rowKey: 'id',
})

const emit = defineEmits<{
  'update:open': [value: boolean]
  'select': [record: any]
}>()

const activeTab = ref<'customer' | 'supplier'>(props.defaultTab)
const keyword = ref('')
const showHierarchy = ref(false)
const loading = ref(false)
const dataSource = ref<any[]>([])
const selectedRecord = ref<any>(null)
const pagination = reactive({ current: 1, pageSize: 11, total: 0 })

/** 列：序号 / 编号 / 名称 / 联系人 / 联系电话 / 联系地址 / 备注（对标实测） */
const tableColumns = computed(() => [
  { title: '', key: '__rowNo', width: 46, customRender: ({ index }: any) => index + 1 },
  // 编号 / 名称 支持排序（对标「往来单位选择」表头带 ↕ 排序标识）
  {
    title: '编号', dataIndex: 'partnerCode', key: 'partnerCode', width: 110,
    sorter: (a: any, b: any) => String(a?.partnerCode ?? '').localeCompare(String(b?.partnerCode ?? '')),
  },
  {
    title: '名称', dataIndex: 'partnerName', key: 'partnerName', width: 170,
    sorter: (a: any, b: any) => String(a?.partnerName ?? '').localeCompare(String(b?.partnerName ?? ''), 'zh-CN'),
  },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 100 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 130 },
  { title: '联系地址', dataIndex: 'address', key: 'address', width: 180 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 120 },
])

async function fetchList() {
  loading.value = true
  try {
    const res: any = await partnerApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      partnerType: activeTab.value,
      keyword: keyword.value || undefined,
      // 显示层次结构 = 以分类节点为行（对标实测口径）
      showHierarchy: showHierarchy.value || undefined,
    })
    dataSource.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e) {
    console.error('[往来单位选择] 加载失败', e)
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function switchTab(key: 'customer' | 'supplier') {
  if (activeTab.value === key) return
  activeTab.value = key
  selectedRecord.value = null
  pagination.current = 1
  fetchList()
}

function handleSearch() {
  pagination.current = 1
  selectedRecord.value = null
  fetchList()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

function customRow(record: any) {
  return {
    onClick: () => { selectedRecord.value = record },
    onDblclick: () => { selectedRecord.value = record; handleConfirm() },
    style: { cursor: 'pointer' },
  }
}

function rowClassName(record: any) {
  return selectedRecord.value && record?.[props.rowKey] === selectedRecord.value?.[props.rowKey]
    ? 'psm-row-active'
    : ''
}

function handleConfirm() {
  if (!selectedRecord.value) {
    Modal.warning({ title: '提示', content: '请先选择一条往来单位记录' })
    return
  }
  if (selectedRecord.value.partnerType === 'category') {
    Modal.warning({ title: '提示', content: '分类节点不能作为往来单位绑定，请选择具体单位' })
    return
  }
  emit('select', selectedRecord.value)
  emit('update:open', false)
}

function handleCancel() {
  emit('update:open', false)
}

/** Enter = 确定 / Esc = 取消（对标按钮文案） */
function handleKeydown(e: KeyboardEvent) {
  if (!props.open) return
  if (e.key === 'Enter') {
    e.preventDefault()
    handleConfirm()
  } else if (e.key === 'Escape') {
    e.preventDefault()
    handleCancel()
  }
}

watch(() => props.open, (val) => {
  if (val) {
    activeTab.value = props.defaultTab
    keyword.value = ''
    showHierarchy.value = false
    selectedRecord.value = null
    pagination.current = 1
    fetchList()
    window.addEventListener('keydown', handleKeydown, true)
  } else {
    window.removeEventListener('keydown', handleKeydown, true)
  }
})

onMounted(() => {
  if (props.open) {
    fetchList()
    window.addEventListener('keydown', handleKeydown, true)
  }
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown, true))
</script>

<style scoped>
.psm-tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid #e8e8e8;
  margin-bottom: 10px;
}
.psm-tab {
  padding: 6px 20px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border: 1px solid transparent;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  user-select: none;
}
.psm-tab:hover { color: #2f5aa8; }
.psm-tab.active {
  color: #fff;
  background: #2f5aa8;
  border-color: #2f5aa8;
}
.psm-search {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.psm-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}
.psm-footer-btns {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
:deep(.psm-row-active) > td {
  background-color: #fff3e0 !important;
}
:deep(.standard-pagination) {
  display: flex;
  align-items: center;
}
</style>
