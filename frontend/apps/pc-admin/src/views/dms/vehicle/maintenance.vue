<template>
  <div v-if="!embedded">
    <!-- 作为独立页面：含完整布局 -->
    <ErrorBoundary @error="handleError"><PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <a-breadcrumb>
              <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
              <a-breadcrumb-item><router-link to="/dms/vehicle">车辆管理</router-link></a-breadcrumb-item>
              <a-breadcrumb-item>维保记录</a-breadcrumb-item>
            </a-breadcrumb>
            <h2>维保记录</h2>
          </div>
          <div class="page-header__right">
            <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
            <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增记录</a-button>
          </div>
        </div>
      </template>
      <template #filter>
        <SearchBar :fields="searchFields" :loading="loading" @search="handleSearch" @reset="handleReset" />
      </template>
      <template #default>
        <div class="page-body">
          <a-table
            :dataSource="dataList"
            :columns="columns"
            :loading="loading"
            :pagination="pagination"
            rowKey="id"
            size="small"
            bordered
            @change="handleTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'maintType'">
                <a-tag :color="maintTypeMap[record.maintType]?.color || 'default'">
                  {{ maintTypeMap[record.maintType]?.text || record.maintType }}
                </a-tag>
              </template>
              <template v-if="column.dataIndex === 'action'">
                <a-popconfirm title="确定删除该维保记录？" @confirm="handleDelete(record)">
                  <a-button type="link" size="small" danger><DeleteOutlined /></a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </div>
      </template>
    </PageContainer></ErrorBoundary>

    <!-- 新增弹窗 -->
    <a-modal v-model:open="modalVisible" title="新增维保记录" width="640px" :confirm-loading="modalLoading" @ok="handleModalOk" @cancel="handleModalCancel">
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="维保类型" name="maintType">
          <a-select v-model:value="formState.maintType" size="small" placeholder="请选择维保类型">
            <a-select-option :value="1">保养</a-select-option>
            <a-select-option :value="2">维修</a-select-option>
            <a-select-option :value="3">年检</a-select-option>
            <a-select-option :value="4">保险</a-select-option>
            <a-select-option :value="5">事故</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="维保日期" name="maintDate">
          <a-date-picker v-model:value="formState.maintDate" size="small" style="width:100%" placeholder="选择日期" />
        </a-form-item>
        <a-form-item label="维保内容" name="maintContent">
          <a-textarea v-model:value="formState.maintContent" :rows="2" size="small" placeholder="请输入维保内容" />
        </a-form-item>
        <a-form-item label="维保费用" name="maintCost">
          <a-input-number v-model:value="formState.maintCost" :min="0" size="small" style="width:100%" placeholder="请输入费用" />
        </a-form-item>
        <a-form-item label="维保厂商" name="maintVendor">
          <a-input v-model:value="formState.maintVendor" size="small" placeholder="请输入维保厂商" />
        </a-form-item>
        <a-form-item label="维保后里程" name="afterMaintMileage">
          <a-input-number v-model:value="formState.afterMaintMileage" :min="0" size="small" style="width:100%" placeholder="请输入维保后里程" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>

  <!-- 作为嵌入组件：仅表格 -->
  <div v-else>
    <div class="embedded-header">
      <h4>维保记录</h4>
      <a-button type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增</a-button>
    </div>
    <a-table
      :dataSource="dataList"
      :columns="embeddedColumns"
      :loading="loading"
      rowKey="id"
      size="small"
      bordered
      :pagination="false as any"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'maintType'">
          <a-tag :color="maintTypeMap[record.maintType]?.color || 'default'">
            {{ maintTypeMap[record.maintType]?.text || record.maintType }}
          </a-tag>
        </template>
        <template v-if="column.dataIndex === 'action'">
          <a-popconfirm title="确定删除该维保记录？" @confirm="handleDelete(record)">
            <a-button type="link" size="small" danger><DeleteOutlined /></a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <!-- 新增弹窗（嵌入模式共用） -->
    <a-modal v-model:open="modalVisible" title="新增维保记录" width="640px" :confirm-loading="modalLoading" @ok="handleModalOk" @cancel="handleModalCancel">
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="维保类型" name="maintType">
          <a-select v-model:value="formState.maintType" size="small" placeholder="请选择维保类型">
            <a-select-option :value="1">保养</a-select-option>
            <a-select-option :value="2">维修</a-select-option>
            <a-select-option :value="3">年检</a-select-option>
            <a-select-option :value="4">保险</a-select-option>
            <a-select-option :value="5">事故</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="维保日期" name="maintDate">
          <a-date-picker v-model:value="formState.maintDate" size="small" style="width:100%" placeholder="选择日期" />
        </a-form-item>
        <a-form-item label="维保内容" name="maintContent">
          <a-textarea v-model:value="formState.maintContent" :rows="2" size="small" placeholder="请输入维保内容" />
        </a-form-item>
        <a-form-item label="维保费用" name="maintCost">
          <a-input-number v-model:value="formState.maintCost" :min="0" size="small" style="width:100%" placeholder="请输入费用" />
        </a-form-item>
        <a-form-item label="维保厂商" name="maintVendor">
          <a-input v-model:value="formState.maintVendor" size="small" placeholder="请输入维保厂商" />
        </a-form-item>
        <a-form-item label="维保后里程" name="afterMaintMileage">
          <a-input-number v-model:value="formState.afterMaintMileage" :min="0" size="small" style="width:100%" placeholder="请输入维保后里程" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { vehicleApi } from '@/api/dms/vehicle'
import { useWmsTable } from '@/composables/useWmsTable'
import { Modal } from 'ant-design-vue'
import {
  PlusOutlined, ReloadOutlined, DeleteOutlined
} from '@ant-design/icons-vue'

function handleError(err: any) { console.warn('[DMS维保]', err) }

// ── Props ──────────────────────────────────────────
const props = withDefaults(defineProps<{
  vehicleId?: number | string
  embedded?: boolean
}>(), {
  vehicleId: undefined,
  embedded: false,
})

const maintTypeMap: Record<number, { text: string; color: string }> = {
  1: { text: '保养', color: 'blue' },
  2: { text: '维修', color: 'orange' },
  3: { text: '年检', color: 'purple' },
  4: { text: '保险', color: 'cyan' },
  5: { text: '事故', color: 'red' },
}

const columns = [
  { title: '车辆ID', dataIndex: 'vehicleId', width: 80 },
  { title: '维保类型', dataIndex: 'maintType', width: 100 },
  { title: '维保日期', dataIndex: 'maintDate', width: 110 },
  { title: '维保内容', dataIndex: 'maintContent', ellipsis: true },
  { title: '费用', dataIndex: 'maintCost', width: 90 },
  { title: '维保厂商', dataIndex: 'maintVendor', width: 130 },
  { title: '维保后里程', dataIndex: 'afterMaintMileage', width: 110 },
  { title: '操作', dataIndex: 'action', width: 60, fixed: 'right' },
] as any

const embeddedColumns = [
  ...columns.filter(c => c.dataIndex !== 'vehicleId'),
]

const searchFields: SearchField[] = [
  { name: 'vehicleId', label: '车辆ID', type: 'input', placeholder: '请输入车辆ID' },
  { name: 'maintType', label: '维保类型', type: 'select', placeholder: '请选择类型', options: [
    { label: '保养', value: 1 }, { label: '维修', value: 2 },
    { label: '年检', value: 3 }, { label: '保险', value: 4 }, { label: '事故', value: 5 },
  ]},
]

const wms = useWmsTable({
  fetchFn: vehicleApi.maintenancePage as any,
  deleteFn: vehicleApi.deleteMaintenance,
  defaultPageSize: 10,
  refreshInterval: 0,
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, handleSearch, handleReset } = wms

// ── 表单 ──────────────────────────────────────────
const modalVisible = ref(false)
const modalLoading = ref(false)
const formRef = ref<FormInstance>()
const formState = reactive<Record<string, any>>({
  vehicleId: 0, maintType: undefined, maintDate: undefined,
  maintContent: '', maintCost: undefined, maintVendor: '',
  afterMaintMileage: undefined,
})
const formRules: Record<string, any[]> = {
  maintType: [{ required: true, message: '请选择维保类型', trigger: 'change' }],
}

// 当 vehicleId prop 变化时自动设置
watch(() => props.vehicleId, (val) => {
  if (val) {
    searchParams.vehicleId = val
    formState.vehicleId = Number(val)
    handleSearch()
  }
}, { immediate: true })

function handleAdd() {
  Object.assign(formState, {
    vehicleId: Number(props.vehicleId || searchParams.vehicleId || 0),
    maintType: undefined, maintDate: undefined,
    maintContent: '', maintCost: undefined, maintVendor: '',
    afterMaintMileage: undefined,
  })
  modalVisible.value = true
}

function handleDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该维保记录吗？',
    okButtonProps: { danger: true },
    onOk: async () => {
      try {
        await vehicleApi.deleteMaintenance(record.id)
        message.success('删除成功')
        wms.fetchData()
      } catch (err: any) {
        message.error(err?.message || '删除失败')
      }
    },
  })
}

async function handleModalOk() {
  try { await formRef.value?.validate() } catch { return }
  modalLoading.value = true
  try {
    await vehicleApi.createMaintenance({ ...formState })
    message.success('创建成功')
    modalVisible.value = false
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || '创建失败')
  } finally {
    modalLoading.value = false
  }
}

function handleModalCancel() {
  modalVisible.value = false
}

function handleTableChange(pag: any) {
  wms.handlePageChange(pag.current, pag.pageSize)
}

// 独立页面模式下的搜索/重置
function onSearch(formData: Record<string, any>) {
  Object.assign(searchParams, formData)
  handleSearch()
}

function onReset() {
  Object.keys(searchParams).forEach(k => { searchParams[k] = undefined })
  wms.handleReset()
}

</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.page-body { padding: 0; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-picker-small), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }

.embedded-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.embedded-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
</style>
