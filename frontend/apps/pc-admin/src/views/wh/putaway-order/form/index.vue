<template>
  <ErrorBoundary>
    <PageContainer title="上架单">
      <a-alert
        type="info"
        show-icon
        class="gap-alert"
        message="上架明细可由收货完成、PDA 扫码作业生成，也可在 PC 端直接录入；仅待处理状态的明细可编辑保存（整体替换）。"
      />

      <!-- ═══ 单据头 ═══ -->
      <div class="panel">
        <div class="panel-title">
          单据头
          <a-tag
            v-if="form.id"
            :color="statusColor(form.status)"
            class="panel-tag"
          >{{ statusText(form.status) }}</a-tag>
        </div>
        <a-form layout="inline" class="header-form">
          <a-form-item label="单号">
            <a-input v-model:value="form.taskNo" style="width: 190px" placeholder="自动生成，可修改" />
          </a-form-item>
          <a-form-item label="上架仓库" required>
            <a-select
              v-model:value="form.warehouseId"
              style="width: 180px"
              placeholder="请选择仓库"
              :options="warehouseOptions"
              :loading="warehouseLoading"
              @change="handleWarehouseChange"
            />
          </a-form-item>
          <a-form-item label="来源类型">
            <a-select v-model:value="form.sourceType" style="width: 130px" :options="sourceTypeOptions" />
          </a-form-item>
          <a-form-item label="来源单据ID">
            <a-input-number v-model:value="form.sourceId" style="width: 150px" :min="0" placeholder="关联单据ID" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="form.remark" style="width: 260px" placeholder="备注" />
          </a-form-item>
        </a-form>
        <div class="btn-row">
          <a-space>
            <a-button type="primary" :loading="saving" :disabled="!editable" @click="handleSave">
              {{ form.id ? '保存修改' : '保存单据' }}
            </a-button>
            <a-button @click="handleNew">新建</a-button>
            <template v-if="form.id">
              <a-button
                v-if="form.status === 0"
                type="primary"
                ghost
                :loading="acting"
                @click="handleStart"
              >开始上架</a-button>
              <a-popconfirm v-if="form.status === 1" title="确认完成该上架单？" @confirm="handleConfirm">
                <a-button type="primary" ghost :loading="acting">确认上架</a-button>
              </a-popconfirm>
              <a-popconfirm v-if="form.status === 0 || form.status === 1" title="确认取消该上架单？" @confirm="handleCancel">
                <a-button danger :loading="acting">取消</a-button>
              </a-popconfirm>
            </template>
          </a-space>
        </div>
      </div>

      <!-- ═══ 作业明细（待处理可编辑） ═══ -->
      <div class="panel">
        <div class="panel-title">
          上架明细
          <template v-if="detailEditable">
            <a-button size="small" type="link" @click="addDetailRow">+ 添加明细</a-button>
            <a-button size="small" type="primary" :loading="detailSaving" @click="handleSaveDetails">保存明细</a-button>
          </template>
          <a-tag v-else color="default">仅待处理状态可编辑</a-tag>
        </div>
        <a-table
          :columns="detailColumns"
          :data-source="details"
          :loading="detailLoading"
          :pagination="false"
          row-key="_key"
          size="small"
          :locale="{ emptyText: form.id ? (detailEditable ? '点击「添加明细」录入商品' : '暂无明细（由收货作业或 PDA 生成）') : '请先保存或载入上架单' }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.dataIndex === 'lineNo'">
              {{ index + 1 }}
            </template>
            <template v-else-if="column.dataIndex === 'productId'">
              <a-select
                v-model:value="record.productId"
                style="width: 100%"
                placeholder="选择商品"
                :options="productOptions"
                :loading="productLoading"
                show-search
                option-filter-prop="label"
                @change="(val: any) => handleProductChange(record, val)"
              />
            </template>
            <template v-else-if="column.dataIndex === 'quantity'">
              <a-input-number
                v-if="detailEditable"
                v-model:value="record.quantity"
                :min="0"
                style="width: 100%"
                placeholder="上架数量"
              />
              <span v-else>{{ formatQty(record.quantity) }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'fromLocationId'">
              <a-select
                v-model:value="record.fromLocationId"
                style="width: 100%"
                placeholder="来源货位"
                :options="locationOptions"
                :loading="locationLoading"
                show-search
                option-filter-prop="label"
                allow-clear
                @change="(val: any) => handleFromLocationChange(record, val)"
              />
            </template>
            <template v-else-if="column.dataIndex === 'toLocationId'">
              <a-select
                v-model:value="record.toLocationId"
                style="width: 100%"
                placeholder="目标货位"
                :options="locationOptions"
                :loading="locationLoading"
                show-search
                option-filter-prop="label"
                allow-clear
                @change="(val: any) => handleToLocationChange(record, val)"
              />
            </template>
            <template v-else-if="column.dataIndex === 'batchNo'">
              <a-input v-if="detailEditable" v-model:value="record.batchNo" placeholder="批次号" />
              <span v-else>{{ record.batchNo || '-' }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <a-tag :color="detailStatusColor(record.status)">{{ detailStatusText(record.status) }}</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-popconfirm title="删除该行？" @confirm="removeDetailRow(record._key)">
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </div>

      <!-- ═══ 最近上架单 ═══ -->
      <div class="panel">
        <div class="panel-title">
          最近上架单
          <a-button size="small" type="link" @click="loadList">刷新</a-button>
        </div>
        <a-table
          :columns="listColumns"
          :data-source="list"
          :loading="listLoading"
          :pagination="pagination"
          row-key="id"
          size="small"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'sourceType'">
              {{ sourceTypeText(record.sourceType) }}
            </template>
            <template v-else-if="column.dataIndex === 'progress'">
              {{ formatQty(record.putawayQuantity) }} / {{ formatQty(record.totalQuantity) }}
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-space :size="4">
                <a-button size="small" type="link" @click="loadRecord(record)">载入</a-button>
                <a-button v-if="record.status === 0" size="small" type="link" @click="handleStartRow(record)">开始</a-button>
                <a-button v-if="record.status === 1" size="small" type="link" @click="handleConfirmRow(record)">确认</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { putawayApi, type WmsPutawayTask, type WmsPutawayDetail } from '@/api/wms/putaway'
import { warehouseApi, locationApi, type WmsLocation } from '@/api/wms/warehouse'
import { productApi, type Product } from '@/api/erp/product'
import { useUserStore } from '@/stores/user'
import { WMS_STATUS_MAP, DETAIL_STATUS_MAP, formatQty, formatTime, genTaskNo } from '../../whTask'

defineOptions({ name: 'WhPutawayOrderForm' })

const userStore = useUserStore()

// ═══ 字典 ═══
const sourceTypeOptions = [
  { label: '收货上架', value: 0 }, { label: '退货上架', value: 1 }, { label: '调拨上架', value: 2 }, { label: '其他', value: 3 },
]
function sourceTypeText(t: number) { return sourceTypeOptions.find(o => o.value === t)?.label || '其他' }
function statusText(s: number) { return WMS_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return WMS_STATUS_MAP[s]?.color || 'default' }
function detailStatusText(s: number) { return DETAIL_STATUS_MAP[s]?.text || '未知' }
function detailStatusColor(s: number) { return DETAIL_STATUS_MAP[s]?.color || 'default' }

// ═══ 仓库/货位/商品选项 ═══
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const warehouseLoading = ref(false)
const locations = ref<WmsLocation[]>([])
const locationLoading = ref(false)
const locationOptions = computed(() =>
  locations.value.map(l => ({ label: `${l.locationCode}${l.locationName ? ' ' + l.locationName : ''}`, value: l.id }))
)
const products = ref<Product[]>([])
const productLoading = ref(false)
const productOptions = computed(() =>
  products.value.map(p => ({ label: `${p.productCode} ${p.productName}`, value: p.id }))
)

async function loadWarehouses() {
  warehouseLoading.value = true
  try {
    const list: any = await warehouseApi.listAll()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({ label: w.warehouseName, value: w.id }))
  } catch (e) { console.warn('[上架单] 仓库列表获取失败', e) }
  finally { warehouseLoading.value = false }
}
async function loadLocations(warehouseId: number) {
  locationLoading.value = true
  try {
    const list: any = await locationApi.listByWarehouse(warehouseId)
    locations.value = Array.isArray(list) ? list : []
  } catch (e) { locations.value = []; console.warn('[上架单] 货位列表获取失败', e) }
  finally { locationLoading.value = false }
}
async function loadProducts() {
  productLoading.value = true
  try {
    const res = await productApi.page({ pageNum: 1, pageSize: 500 })
    products.value = res?.records || []
  } catch (e) { products.value = []; console.warn('[上架单] 商品列表获取失败', e) }
  finally { productLoading.value = false }
}
function handleWarehouseChange(val: number) {
  const w = warehouseOptions.value.find(o => o.value === val)
  form.warehouseName = w?.label || ''
  locations.value = []
  if (val) loadLocations(val)
}

// ═══ 单据头表单 ═══
const emptyForm = () => ({
  id: 0, taskNo: genTaskNo('PA'), sourceType: 0, sourceId: undefined as number | undefined,
  warehouseId: undefined as number | undefined, warehouseName: '', remark: '', status: 0,
})
const form = reactive(emptyForm())
const editable = computed(() => !form.id || form.status === 0)
const saving = ref(false)
const acting = ref(false)

function currentUser() {
  return { userId: userStore.userId || 0, userName: userStore.userInfo?.nickname || userStore.userInfo?.username || '系统' }
}

async function handleSave() {
  if (!form.warehouseId) { message.warning('请选择上架仓库'); return }
  if (!form.taskNo) { message.warning('请填写单号'); return }
  saving.value = true
  try {
    const payload: Partial<WmsPutawayTask> = {
      taskNo: form.taskNo, sourceType: form.sourceType, sourceId: form.sourceId,
      warehouseId: form.warehouseId, warehouseName: form.warehouseName, remark: form.remark,
    }
    if (form.id) {
      await putawayApi.update({ ...payload, id: form.id })
      message.success('上架单已更新')
    } else {
      const res: any = await putawayApi.save({ ...payload, status: 0 })
      form.id = res?.id || 0
      message.success('上架单已创建，可录入明细')
    }
    loadList()
  } catch (e: any) { message.error(e?.message || '保存失败') }
  finally { saving.value = false }
}

function handleNew() {
  Object.assign(form, emptyForm())
  details.value = []
}

// ═══ 工作流动作 ═══
async function doAction(fn: () => Promise<any>, ok: string) {
  acting.value = true
  try { await fn(); message.success(ok); await refreshCurrent(); loadList() }
  catch (e: any) { message.error(e?.message || '操作失败') }
  finally { acting.value = false }
}
function handleStart() { const u = currentUser(); doAction(() => putawayApi.startPutaway(form.id, u.userId, u.userName), '已开始上架') }
function handleConfirm() { const u = currentUser(); doAction(() => putawayApi.confirmPutaway(form.id, u.userId, u.userName), '上架已确认') }
function handleCancel() { doAction(() => putawayApi.cancelPutaway(form.id, 'PC端取消上架'), '上架单已取消') }
function handleStartRow(r: any) { const u = currentUser(); doAction(() => putawayApi.startPutaway(r.id, u.userId, u.userName), '已开始上架') }
function handleConfirmRow(r: any) { const u = currentUser(); doAction(() => putawayApi.confirmPutaway(r.id, u.userId, u.userName), '上架已确认') }

async function refreshCurrent() {
  if (!form.id) return
  try {
    const t: any = await putawayApi.getById(form.id)
    if (t) fillForm(t)
  } catch (e) { console.warn('[上架单] 刷新单据失败', e) }
}

// ═══ 明细（待处理可编辑） ═══
type EditableDetail = Partial<WmsPutawayDetail> & { _key: string }
let rowSeq = 0
const details = ref<EditableDetail[]>([])
const detailLoading = ref(false)
const detailSaving = ref(false)
const detailEditable = computed(() => !!form.id && form.status === 0)

const detailColumns = computed<any[]>(() => {
  if (detailEditable.value) {
    return [
      { title: '行号', dataIndex: 'lineNo', width: 56 },
      { title: '商品', dataIndex: 'productId', width: 220 },
      { title: '规格', dataIndex: 'productSpec', width: 100 },
      { title: '单位', dataIndex: 'productUnit', width: 64 },
      { title: '上架数量', dataIndex: 'quantity', width: 110, align: 'right' },
      { title: '来源货位', dataIndex: 'fromLocationId', width: 150 },
      { title: '目标货位', dataIndex: 'toLocationId', width: 150 },
      { title: '批次号', dataIndex: 'batchNo', width: 110 },
      { title: '操作', dataIndex: 'action', width: 70, fixed: 'right' },
    ]
  }
  return [
    { title: '行号', dataIndex: 'lineNo', width: 60 },
    { title: '商品编码', dataIndex: 'productCode', width: 120 },
    { title: '商品名称', dataIndex: 'productName', width: 180, ellipsis: true },
    { title: '规格', dataIndex: 'productSpec', width: 100 },
    { title: '单位', dataIndex: 'productUnit', width: 70 },
    { title: '数量', dataIndex: 'quantity', width: 90, align: 'right' },
    { title: '来源货位', dataIndex: 'fromLocationCode', width: 110 },
    { title: '目标货位', dataIndex: 'toLocationCode', width: 110 },
    { title: '批次号', dataIndex: 'batchNo', width: 110 },
    { title: '状态', dataIndex: 'status', width: 90 },
  ]
})

function addDetailRow() {
  details.value.push({ _key: `n${++rowSeq}`, productId: undefined, quantity: undefined, fromLocationId: undefined, toLocationId: undefined, batchNo: '' })
}
function removeDetailRow(key: string) {
  details.value = details.value.filter(r => r._key !== key)
}
function handleProductChange(row: any, val: number) {
  const p = products.value.find(x => x.id === val)
  if (!p) return
  row.productId = p.id
  row.productCode = p.productCode
  row.productName = p.productName
  row.productSpec = p.spec || ''
  row.productUnit = p.unit || ''
}
function handleFromLocationChange(row: any, val: number | undefined) {
  const l = locations.value.find(x => x.id === val)
  row.fromLocationCode = l?.locationCode || ''
}
function handleToLocationChange(row: any, val: number | undefined) {
  const l = locations.value.find(x => x.id === val)
  row.toLocationCode = l?.locationCode || ''
}

async function handleSaveDetails() {
  if (!form.id) return
  if (!details.value.length) { message.warning('请先添加明细行'); return }
  if (details.value.some(r => !r.productId)) { message.warning('明细行请选择商品'); return }
  if (details.value.some(r => !(Number(r.quantity) > 0))) { message.warning('明细行请填写大于0的上架数量'); return }
  detailSaving.value = true
  try {
    const payload: Partial<WmsPutawayDetail>[] = details.value.map(r => ({
      productId: r.productId, productCode: r.productCode, productName: r.productName,
      productSpec: r.productSpec, productUnit: r.productUnit,
      quantity: Number(r.quantity),
      fromLocationId: r.fromLocationId, fromLocationCode: r.fromLocationCode,
      toLocationId: r.toLocationId, toLocationCode: r.toLocationCode,
      batchNo: r.batchNo || '',
    }))
    await putawayApi.saveDetails(form.id, payload)
    message.success('上架明细已保存')
    await loadDetails(form.id)
    await refreshCurrent()
    loadList()
  } catch (e: any) { message.error(e?.message || '明细保存失败') }
  finally { detailSaving.value = false }
}

async function loadDetails(taskId: number) {
  detailLoading.value = true
  try {
    const list: any = await putawayApi.getDetails(taskId)
    details.value = (Array.isArray(list) ? list : []).map((d: any) => ({ ...d, _key: `s${d.id}` }))
  } catch (e) { details.value = []; console.warn('[上架单] 明细获取失败', e) }
  finally { detailLoading.value = false }
}

// ═══ 最近上架单列表 ═══
const list = ref<WmsPutawayTask[]>([])
const listLoading = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })
const listColumns: any[] = [
  { title: '单号', dataIndex: 'taskNo', width: 170 },
  { title: '来源类型', dataIndex: 'sourceType', width: 100 },
  { title: '仓库', dataIndex: 'warehouseName', width: 110 },
  { title: '进度(上架/总)', dataIndex: 'progress', width: 120, align: 'right' },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '操作人', dataIndex: 'assigneeName', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 130 },
  { title: '操作', dataIndex: 'action', width: 140, fixed: 'right' },
]
async function loadList() {
  listLoading.value = true
  try {
    const res: any = await putawayApi.page({ current: pagination.current, size: pagination.pageSize })
    list.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e) { list.value = []; pagination.total = 0; console.warn('[上架单] 列表获取失败', e) }
  finally { listLoading.value = false }
}
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadList() }

function fillForm(t: WmsPutawayTask) {
  Object.assign(form, {
    id: t.id, taskNo: t.taskNo, sourceType: t.sourceType ?? 0, sourceId: t.sourceId ?? undefined,
    warehouseId: t.warehouseId ?? undefined, warehouseName: t.warehouseName || '', remark: t.remark || '', status: t.status ?? 0,
  })
  if (t.warehouseId) loadLocations(t.warehouseId)
}
function loadRecord(r: any) {
  fillForm(r)
  loadDetails(r.id)
}

onMounted(() => { loadWarehouses(); loadProducts(); loadList() })
</script>

<style scoped>
.gap-alert { margin-bottom: 16px; }
.panel { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.panel-title { font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
.panel-tag { margin-left: 4px; }
.header-form { row-gap: 8px; }
.btn-row { margin-top: 12px; padding-top: 12px; border-top: 1px dashed #f0f0f0; }
</style>
