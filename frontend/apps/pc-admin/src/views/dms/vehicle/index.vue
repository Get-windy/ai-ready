<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>DMS / 车辆管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2>车辆管理</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge"><SyncOutlined /> {{ autoRefreshCountdown }}s</span>
          <a-button size="small" :loading="loading" @click="wms.debounce('refresh', wms.fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <a-button type="primary" size="small" @click="handleAdd"><PlusOutlined /> 新增车辆</a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          </span>
        </div>
      </div>
    </template>

    <template #filter>
      <SearchBar :fields="searchFields" :loading="loading" @search="handleSearch" @reset="handleReset" />
    </template>

    <template #default>
      <div class="page-body">
        <div v-if="selectedRowKeys.length > 0" class="batch-bar no-print">
          <span>已选择 {{ selectedRowKeys.length }} 项</span>
          <a-button size="small" danger @click="handleBatchDelete">批量删除</a-button>
          <a-button size="small" @click="selectedRowKeys = []">取消选择</a-button>
        </div>
        <SkeletonTable v-if="loading && dataList.length === 0" :columns="columns.length" :rows="8" />
        <a-table
          v-else
          :dataSource="dataList"
          :columns="columns"
          :loading="loading"
          :pagination="pagination"
          :rowSelection="rowSelection"
          rowKey="id"
          size="small"
          bordered
          @change="handleTableChange"
        >
          <template #emptyText>
            <a-empty description="暂无车辆数据">
              <a-space>
                <a-button size="small" type="primary" @click="handleAdd">新增车辆</a-button>
                <a-button size="small" @click="wms.fetchData">刷新</a-button>
              </a-space>
            </a-empty>
          </template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'vehicleType'">
              <a-tag :color="vehicleTypeMap[record.vehicleType]?.color || 'default'">
                {{ vehicleTypeMap[record.vehicleType]?.text || record.vehicleType }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="statusMap[record.status]?.color || 'default'">
                {{ statusMap[record.status]?.text || record.status }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'insuranceExpireDate'">
              <a-tag :color="getInsuranceExpireInfo(record.insuranceExpireDate)?.color || 'default'">
                {{ getInsuranceExpireInfo(record.insuranceExpireDate)?.text || record.insuranceExpireDate || '-' }}
              </a-tag>
            </template>
            <template v-if="column.dataIndex === 'action'">
              <a-space :size="2">
                <a-tooltip title="查看详情">
                  <a-button v-permission="'dms:vehicle:view'" type="link" size="small" @click="handleView(record as any)"><EyeOutlined /></a-button>
                </a-tooltip>
                <a-tooltip title="编辑">
                  <a-button v-permission="'dms:vehicle:edit'" type="link" size="small" @click="handleEdit(record as any)"><EditOutlined /></a-button>
                </a-tooltip>
                <a-dropdown>
                  <a-button type="link" size="small">
                    更多 <DownOutlined style="font-size:10px;" />
                  </a-button>
                  <template #overlay>
                    <a-menu @click="({ key }) => handleMoreAction(key as string, record as any)">
                      <a-menu-item v-permission="'dms:vehicle:status'" key="status"><SyncOutlined /> 状态变更</a-menu-item>
                      <a-menu-item v-permission="'dms:vehicle:bind-rider'" key="bindRider"><TeamOutlined /> 绑定骑手</a-menu-item>
                      <a-menu-item v-permission="'dms:vehicle:maintenance'" key="maintenance"><ToolOutlined /> 维保记录</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </template>
  </PageContainer></ErrorBoundary>

  <!-- 新增/编辑弹窗 -->
  <a-modal
    v-model:open="modalVisible"
    :title="modalTitle"
    width="800px"
    :confirm-loading="modalLoading"
    @ok="handleModalOk"
    @cancel="handleModalCancel"
  >
    <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="车牌号" name="plateNo">
            <a-input v-model:value="formState.plateNo" size="small" placeholder="请输入车牌号" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="车辆类型" name="vehicleType">
            <a-select v-model:value="formState.vehicleType" size="small" placeholder="请选择车辆类型">
              <a-select-option v-for="(text, val) in vehicleTypeOptions" :key="val" :value="Number(val)">{{ text }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="品牌" name="brand">
            <a-input v-model:value="formState.brand" size="small" placeholder="请输入品牌" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="型号" name="model">
            <a-input v-model:value="formState.model" size="small" placeholder="请输入型号" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="颜色" name="color">
            <a-input v-model:value="formState.color" size="small" placeholder="请输入颜色" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="VIN" name="vin">
            <a-input v-model:value="formState.vin" size="small" placeholder="请输入车架号" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="发动机号" name="engineNo">
            <a-input v-model:value="formState.engineNo" size="small" placeholder="请输入发动机号" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="所属性质" name="ownershipType">
            <a-select v-model:value="formState.ownershipType" size="small" placeholder="请选择所属性质">
              <a-select-option v-for="(text, val) in ownershipMap" :key="val" :value="Number(val)">{{ text }}</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="核定载重(kg)" name="ratedLoad">
            <a-input-number v-model:value="formState.ratedLoad" :min="0" size="small" style="width:100%" placeholder="请输入核定载重" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="核定载客" name="ratedPassenger">
            <a-input-number v-model:value="formState.ratedPassenger" :min="0" size="small" style="width:100%" placeholder="请输入核定载客" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="货箱容积(m³)" name="cargoVolume">
            <a-input-number v-model:value="formState.cargoVolume" :min="0" size="small" style="width:100%" placeholder="请输入货箱容积" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="所属部门" name="department">
            <a-input v-model:value="formState.department" size="small" placeholder="请输入所属部门" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="注册日期" name="registerDate">
            <a-date-picker v-model:value="formState.registerDate" size="small" style="width:100%" placeholder="选择注册日期" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="保养间隔(km)" name="maintenanceIntervalKm">
            <a-input-number v-model:value="formState.maintenanceIntervalKm" :min="0" size="small" style="width:100%" placeholder="请输入保养间隔" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="保险到期日" name="insuranceExpireDate">
            <a-date-picker v-model:value="formState.insuranceExpireDate" size="small" style="width:100%" placeholder="选择保险到期日" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="年检到期日" name="inspectionExpireDate">
            <a-date-picker v-model:value="formState.inspectionExpireDate" size="small" style="width:100%" placeholder="选择年检到期日" />
          </a-form-item>
        </a-col>
      </a-row>
      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
        <a-textarea v-model:value="formState.remark" :rows="2" size="small" placeholder="请输入备注" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 绑定骑手弹窗 -->
  <a-modal
    v-model:open="bindModalVisible"
    title="绑定骑手"
    width="480px"
    :confirm-loading="bindModalLoading"
    @ok="handleBindRiderOk"
    @cancel="handleBindRiderCancel"
  >
    <a-form ref="bindFormRef" :model="bindFormState" :rules="bindFormRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="骑手ID" name="riderId">
        <a-input-number v-model:value="bindFormState.riderId" :min="1" size="small" style="width:100%" placeholder="请输入骑手ID" />
      </a-form-item>
      <a-form-item label="骑手姓名" name="riderName">
        <a-input v-model:value="bindFormState.riderName" size="small" placeholder="请输入骑手姓名" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 状态变更弹窗 -->
  <a-modal
    v-model:open="statusModalVisible"
    title="状态变更"
    width="400px"
    :confirm-loading="statusModalLoading"
    @ok="handleStatusOk"
    @cancel="statusModalVisible = false"
  >
    <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="当前状态">
        <a-tag :color="statusMap[statusModalRecord?.status]?.color">{{ statusMap[statusModalRecord?.status]?.text }}</a-tag>
      </a-form-item>
      <a-form-item label="变更至">
        <a-select v-model:value="statusTargetValue" size="small">
          <a-select-option v-for="(item, key) in statusMap" :key="key" :value="Number(key)">{{ item.text }}</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import SkeletonTable from '@/components/Skeleton/SkeletonTable.vue'
import { vehicleApi, type DmsVehicle } from '@/api/dms/vehicle'
import { useWmsTable } from '@/composables/useWmsTable'
import {
  PlusOutlined, ReloadOutlined, EyeOutlined, EditOutlined,
  SyncOutlined, TeamOutlined, ToolOutlined, DownOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'

function handleError(err: any) { console.warn('[DMS车辆]', err) }

// 行选择
const selectedRowKeys = ref<(string | number)[]>([])
const rowSelection = {
  selectedRowKeys,
  onChange: (keys: (string | number)[]) => { selectedRowKeys.value = keys },
} as any
function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要删除的车辆')
    return
  }
  const ids = selectedRowKeys.value.join(',')
  wms.handleDelete({ id: ids as any }, `确定要删除选中的 ${selectedRowKeys.value.length} 个车辆吗？`)
}

const router = useRouter()

// ── 映射表 ──────────────────────────────────────────
const vehicleTypeMap: Record<number, { text: string; color: string }> = {
  1: { text: '电动车', color: 'green' },
  2: { text: '小货车', color: 'blue' },
  3: { text: '面包车', color: 'orange' },
  4: { text: '厢式货车', color: 'purple' },
  5: { text: '冷藏车', color: 'cyan' },
  6: { text: '三轮车', color: 'gold' },
}

const vehicleTypeOptions: Record<number, string> = {
  1: '电动车', 2: '小货车', 3: '面包车', 4: '厢式货车', 5: '冷藏车', 6: '三轮车',
}

const statusMap: Record<number, { text: string; color: string }> = {
  0: { text: '空闲', color: 'green' },
  1: { text: '使用中', color: 'blue' },
  2: { text: '维修中', color: 'orange' },
  3: { text: '已报废', color: 'red' },
}

const ownershipMap: Record<number, string> = {
  1: '公司自有', 2: '个人自带', 3: '租赁',
}

function getInsuranceExpireInfo(dateStr?: string): { text: string; color: string } | null {
  if (!dateStr) return null
  const now = new Date()
  const expireDate = new Date(dateStr)
  const diff = expireDate.getTime() - now.getTime()
  const daysLeft = Math.ceil(diff / (1000 * 60 * 60 * 24))
  if (daysLeft < 0) return { text: `已过期 (${dateStr.slice(0, 10)})`, color: 'red' }
  if (daysLeft <= 30) return { text: `即将到期 (${daysLeft}天)`, color: 'orange' }
  return { text: dateStr.slice(0, 10), color: 'green' }
}

// ── 表格列 ──────────────────────────────────────────
const columns = [
  { title: '车牌号', dataIndex: 'plateNo', width: 110 },
  { title: '品牌', dataIndex: 'brand', width: 100 },
  { title: '型号', dataIndex: 'model', width: 120 },
  { title: '颜色', dataIndex: 'color', width: 70 },
  { title: '车辆类型', dataIndex: 'vehicleType', width: 100 },
  { title: '状态', dataIndex: 'status', width: 80 },
  { title: '当前骑手', dataIndex: 'riderName', width: 100 },
  { title: '车队长', dataIndex: 'vehicleManagerName', width: 90 },
  { title: '保险到期', dataIndex: 'insuranceExpireDate', width: 120 },
  { title: '操作', dataIndex: 'action', width: 140, fixed: 'right' },
] as any

// ── 搜索字段 ──────────────────────────────────────────
const searchFields: SearchField[] = [
  { name: 'plateNo', label: '车牌号', type: 'input', placeholder: '请输入车牌号' },
  { name: 'brand', label: '品牌', type: 'input', placeholder: '请输入品牌' },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '空闲', value: 0 }, { label: '使用中', value: 1 },
    { label: '维修中', value: 2 }, { label: '已报废', value: 3 },
  ]},
  { name: 'vehicleType', label: '车辆类型', type: 'select', placeholder: '请选择类型', options: [
    { label: '电动车', value: 1 }, { label: '小货车', value: 2 },
    { label: '面包车', value: 3 }, { label: '厢式货车', value: 4 },
    { label: '冷藏车', value: 5 }, { label: '三轮车', value: 6 },
  ]},
]

// ── CRUD ──────────────────────────────────────────
const wms = useWmsTable({
  fetchFn: vehicleApi.page as any,
  deleteFn: vehicleApi.remove,
  defaultPageSize: 20,
  refreshInterval: 30,
  shortcuts: { f5: 'refresh', ctrlN: 'add' },
})

const { tableData: dataList, loading, pagination, searchParams, lastUpdateTime, autoRefreshCountdown } = wms

// ── 新增/编辑 ──────────────────────────────────────────
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const modalTitle = computed(() => (isEdit.value ? '编辑车辆' : '新增车辆'))
const formRef = ref<FormInstance>()
const formState = reactive<Record<string, any>>({
  id: 0, plateNo: '', brand: '', model: '', color: '', vehicleType: undefined,
  vin: '', engineNo: '', ratedLoad: 0, ratedPassenger: 0, cargoVolume: 0,
  registerDate: undefined, insuranceExpireDate: undefined, inspectionExpireDate: undefined,
  maintenanceIntervalKm: 0, ownershipType: undefined, department: '', remark: '',
})
const formRules: Record<string, any[]> = {
  plateNo: [
    { required: true, message: '请输入车牌号', trigger: 'blur' },
    { pattern: /^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤川青藏琼宁][A-Z][A-HJ-NP-Z0-9]{4,5}[A-HJ-NP-Z0-9挂学警]$/, message: '车牌号格式不正确', trigger: 'blur' },
  ],
  vehicleType: [{ required: true, message: '请选择车辆类型', trigger: 'change' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
  color: [{ required: true, message: '请输入颜色', trigger: 'blur' }],
  vin: [
    { pattern: /^[A-HJ-NP-Z0-9]{17}$/, message: '车架号格式不正确（17位大写字母数字）', trigger: 'blur' },
  ],
  engineNo: [{ required: true, message: '请输入发动机号', trigger: 'blur' }],
  ownershipType: [{ required: true, message: '请选择所属性质', trigger: 'change' }],
  ratedLoad: [{ type: 'number', min: 0, max: 99999, message: '核定载重范围 0-99999 kg', trigger: 'blur' }],
  ratedPassenger: [{ type: 'number', min: 0, max: 99, message: '核定载客范围 0-99 人', trigger: 'blur' }],
  registerDate: [{ required: true, message: '请选择注册日期', trigger: 'change' }],
}

function initFormState(data?: Record<string, any>) {
  const defaults = {
    id: 0, plateNo: '', brand: '', model: '', color: '', vehicleType: undefined,
    vin: '', engineNo: '', ratedLoad: undefined, ratedPassenger: undefined, cargoVolume: undefined,
    registerDate: undefined, insuranceExpireDate: undefined, inspectionExpireDate: undefined,
    maintenanceIntervalKm: undefined, ownershipType: undefined, department: '', remark: '',
  }
  Object.assign(formState, data ? { ...defaults, ...data } : defaults)
}

function handleAdd() {
  isEdit.value = false
  editId.value = null
  initFormState()
  modalVisible.value = true
}

function handleEdit(record: DmsVehicle) {
  isEdit.value = true
  editId.value = record.id
  initFormState(record)
  modalVisible.value = true
}

async function handleModalOk() {
  try { await formRef.value?.validate() } catch { return }
  modalLoading.value = true
  try {
    const payload = { ...formState }
    // 清理 undefined 值
    Object.keys(payload).forEach(k => {
      if (payload[k] === undefined) delete payload[k]
    })
    if (isEdit.value && editId.value) {
      await vehicleApi.update(editId.value, payload)
      message.success('更新成功')
    } else {
      await vehicleApi.create(payload)
      message.success('创建成功')
    }
    modalVisible.value = false
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || (isEdit.value ? '更新失败' : '创建失败'))
  } finally {
    modalLoading.value = false
  }
}

function handleModalCancel() {
  modalVisible.value = false
}

// ── 更多操作 ──────────────────────────────────────────
const statusModalVisible = ref(false)
const statusModalLoading = ref(false)
const statusModalRecord = ref<any>(null)
const statusTargetValue = ref<number>(0)

const bindModalVisible = ref(false)
const bindModalLoading = ref(false)
const bindVehicleId = ref<number>(0)
const bindFormRef = ref<FormInstance>()
const bindFormState = reactive({ riderId: 0, riderName: '' })
const bindFormRules: Record<string, any[]> = {
  riderId: [{ required: true, message: '请输入骑手ID', trigger: 'blur' }],
  riderName: [{ required: true, message: '请输入骑手姓名', trigger: 'blur' }],
}

function handleMoreAction(key: string, record: DmsVehicle) {
  switch (key) {
    case 'status':
      statusModalRecord.value = record
      statusTargetValue.value = record.status
      statusModalVisible.value = true
      break
    case 'bindRider':
      bindVehicleId.value = record.id
      bindFormState.riderId = 0
      bindFormState.riderName = ''
      bindModalVisible.value = true
      break
    case 'maintenance':
      router.push({ path: '/dms/vehicle/maintenance', query: { vehicleId: record.id } })
      break
  }
}

async function handleStatusOk() {
  if (!statusModalRecord.value) return
  statusModalLoading.value = true
  try {
    await vehicleApi.updateStatus(statusModalRecord.value.id, statusTargetValue.value)
    message.success('状态更新成功')
    statusModalVisible.value = false
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || '状态更新失败')
  } finally {
    statusModalLoading.value = false
  }
}

async function handleBindRiderOk() {
  try { await bindFormRef.value?.validate() } catch { return }
  bindModalLoading.value = true
  try {
    await vehicleApi.bindRiderWithName({
      vehicleId: bindVehicleId.value,
      riderId: bindFormState.riderId,
      riderName: bindFormState.riderName,
    })
    message.success('绑定成功')
    bindModalVisible.value = false
    wms.fetchData()
  } catch (err: any) {
    message.error(err?.message || '绑定失败')
  } finally {
    bindModalLoading.value = false
  }
}

function handleBindRiderCancel() {
  bindModalVisible.value = false
}

// ── 查看 ──────────────────────────────────────────
function handleView(record: DmsVehicle) {
  router.push({ path: `/dms/vehicle/${record.id}` })
}

// ── 搜索/分页 ──────────────────────────────────────────
function handleSearch(formData: Record<string, any>) {
  Object.assign(searchParams, formData)
  wms.handleSearch()
}

function handleReset() {
  Object.keys(searchParams).forEach(k => { searchParams[k] = undefined })
  wms.handleReset()
}

function handleTableChange(pag: any) {
  wms.handlePageChange(pag.current, pag.pageSize)
}
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; align-items: center; gap: 12px; }
.page-header__left h2 { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #909399; padding: 2px 8px; border-radius: 4px; background: #f5f7fa; user-select: none; }
.page-body { padding: 0; }
.batch-bar { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 4px; margin-bottom: 8px; font-size: 13px; }
.batch-bar span { flex: 1; color: #1890ff; }
:deep(.ant-input-sm), :deep(.ant-input-number-sm), :deep(.ant-select-single.ant-select-sm .ant-select-selector), :deep(.ant-picker-small), :deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }
@media print {
  .page-header__right .shortcut-hints,
  .auto-refresh-badge,
  .update-time { display: none !important; }
}
</style>
