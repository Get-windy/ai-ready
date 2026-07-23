<template>
  <ErrorBoundary>
    <PageContainer title="盘点单">
      <a-alert
        type="info"
        show-icon
        class="gap-alert"
        message="盘点结果（账面数/实盘数）可由 PDA 扫码盘点生成，也可在 PC 端直接录入；仅待盘点状态的明细可编辑保存（整体替换）。"
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
          <a-form-item label="盘点仓库" required>
            <a-select
              v-model:value="form.warehouseId"
              style="width: 180px"
              placeholder="请选择仓库"
              :options="warehouseOptions"
              :loading="warehouseLoading"
              @change="handleWarehouseChange"
            />
          </a-form-item>
          <a-form-item label="盘点类型">
            <a-select v-model:value="form.checkType" style="width: 120px" :options="checkTypeOptions" />
          </a-form-item>
          <a-form-item label="盘点范围">
            <a-select v-model:value="form.scopeType" style="width: 130px" :options="scopeTypeOptions" />
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
              >开始盘点</a-button>
              <a-popconfirm v-if="form.status === 1" title="确认提交盘点结果？" @confirm="handleSubmitResult">
                <a-button type="primary" ghost :loading="acting">提交盘点结果</a-button>
              </a-popconfirm>
              <a-popconfirm v-if="form.status === 2" title="确认审核通过该盘点单？" @confirm="handleApprove">
                <a-button type="primary" ghost :loading="acting">审核盘点</a-button>
              </a-popconfirm>
              <a-popconfirm v-if="form.status === 0 || form.status === 1" title="确认取消该盘点单？" @confirm="handleCancel">
                <a-button danger :loading="acting">取消</a-button>
              </a-popconfirm>
            </template>
          </a-space>
        </div>
      </div>

      <!-- ═══ 盘点结果（待盘点可编辑） ═══ -->
      <div class="panel">
        <div class="panel-title">
          盘点结果
          <template v-if="detailEditable">
            <a-button size="small" type="link" @click="addDetailRow">+ 添加明细</a-button>
            <a-button size="small" type="primary" :loading="detailSaving" @click="handleSaveDetails">保存明细</a-button>
          </template>
          <a-tag v-else color="default">仅待盘点状态可编辑</a-tag>
        </div>
        <a-table
          :columns="resultColumns"
          :data-source="results"
          :loading="resultLoading"
          :pagination="false"
          row-key="_key"
          size="small"
          :locale="{ emptyText: form.id ? (detailEditable ? '点击「添加明细」录入盘点商品' : '暂无盘点结果（由 PDA 盘点生成）') : '请先保存或载入盘点单' }"
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
            <template v-else-if="column.dataIndex === 'locationId'">
              <a-select
                v-model:value="record.locationId"
                style="width: 100%"
                placeholder="选择货位"
                :options="locationOptions"
                :loading="locationLoading"
                show-search
                option-filter-prop="label"
                allow-clear
                @change="(val: any) => handleLocationChange(record, val)"
              />
            </template>
            <template v-else-if="column.dataIndex === 'batchNo'">
              <a-input v-if="detailEditable" v-model:value="record.batchNo" placeholder="批次号" />
              <span v-else>{{ record.batchNo || '-' }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'bookQuantity'">
              <a-input-number
                v-if="detailEditable"
                v-model:value="record.bookQuantity"
                :min="0"
                style="width: 100%"
                placeholder="账面数量"
              />
              <span v-else>{{ formatQty(record.bookQuantity) }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'actualQuantity'">
              <a-input-number
                v-if="detailEditable"
                v-model:value="record.actualQuantity"
                :min="0"
                style="width: 100%"
                placeholder="实盘数量"
              />
              <span v-else>{{ formatQty(record.actualQuantity) }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'diffQuantity'">
              <span :style="{ color: (record.diffQuantity || 0) !== 0 ? '#ff4d4f' : '#52c41a', fontWeight: 600 }">
                {{ formatQty(record.diffQuantity) }}
              </span>
            </template>
            <template v-else-if="column.dataIndex === 'diffAmount'">
              {{ formatQty(record.diffAmount) }}
            </template>
            <template v-else-if="column.dataIndex === 'diffType'">
              <a-tag :color="diffTypeColor(record.diffType)">{{ diffTypeText(record.diffType) }}</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-popconfirm title="删除该行？" @confirm="removeDetailRow(record._key)">
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </div>

      <!-- ═══ 最近盘点单 ═══ -->
      <div class="panel">
        <div class="panel-title">
          最近盘点单
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
            <template v-if="column.dataIndex === 'checkType'">
              {{ checkTypeText(record.checkType) }}
            </template>
            <template v-else-if="column.dataIndex === 'progress'">
              {{ record.checkedItems ?? 0 }} / {{ record.totalItems ?? 0 }}
            </template>
            <template v-else-if="column.dataIndex === 'diffItems'">
              <span :style="{ color: (record.diffItems || 0) > 0 ? '#ff4d4f' : 'inherit' }">{{ record.diffItems ?? 0 }}</span>
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
                <a-button v-if="record.status === 1" size="small" type="link" @click="handleSubmitRow(record)">提交</a-button>
                <a-button v-if="record.status === 2" size="small" type="link" @click="handleApproveRow(record)">审核</a-button>
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
import { checkApi, type WmsCheckTask, type WmsCheckResult } from '@/api/wms/check'
import { warehouseApi, locationApi, type WmsLocation } from '@/api/wms/warehouse'
import { productApi, type Product } from '@/api/erp/product'
import { useUserStore } from '@/stores/user'
import { CHECK_STATUS_MAP, formatQty, formatTime, genTaskNo } from '../../whTask'

defineOptions({ name: 'WhInventoryOrderForm' })

const userStore = useUserStore()

// ═══ 字典 ═══
const checkTypeOptions = [
  { label: '全盘', value: 0 }, { label: '抽盘', value: 1 }, { label: '循环盘点', value: 2 },
]
const scopeTypeOptions = [
  { label: '全部商品', value: 0 }, { label: '按货位', value: 1 }, { label: '按商品', value: 2 },
]
function checkTypeText(t: number) { return checkTypeOptions.find(o => o.value === t)?.label || '全盘' }
function statusText(s: number) { return CHECK_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return CHECK_STATUS_MAP[s]?.color || 'default' }
function diffTypeText(t: number) {
  if (t === 1) return '盘盈'
  if (t === 2) return '盘亏'
  return '无差异'
}
function diffTypeColor(t: number) {
  if (t === 1) return 'green'
  if (t === 2) return 'red'
  return 'default'
}

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
  } catch (e) { console.warn('[盘点单] 仓库列表获取失败', e) }
  finally { warehouseLoading.value = false }
}
async function loadLocations(warehouseId: number) {
  locationLoading.value = true
  try {
    const list: any = await locationApi.listByWarehouse(warehouseId)
    locations.value = Array.isArray(list) ? list : []
  } catch (e) { locations.value = []; console.warn('[盘点单] 货位列表获取失败', e) }
  finally { locationLoading.value = false }
}
async function loadProducts() {
  productLoading.value = true
  try {
    const res = await productApi.page({ pageNum: 1, pageSize: 500 })
    products.value = res?.records || []
  } catch (e) { products.value = []; console.warn('[盘点单] 商品列表获取失败', e) }
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
  id: 0, taskNo: genTaskNo('CH'), warehouseId: undefined as number | undefined, warehouseName: '',
  checkType: 0, scopeType: 0, remark: '', status: 0,
})
const form = reactive(emptyForm())
const editable = computed(() => !form.id || form.status === 0)
const saving = ref(false)
const acting = ref(false)

function currentUser() {
  return { userId: userStore.userId || 0, userName: userStore.userInfo?.nickname || userStore.userInfo?.username || '系统' }
}

async function handleSave() {
  if (!form.warehouseId) { message.warning('请选择盘点仓库'); return }
  if (!form.taskNo) { message.warning('请填写单号'); return }
  saving.value = true
  try {
    const payload: Partial<WmsCheckTask> = {
      taskNo: form.taskNo, warehouseId: form.warehouseId, warehouseName: form.warehouseName,
      checkType: form.checkType, scopeType: form.scopeType, remark: form.remark,
    }
    if (form.id) {
      await checkApi.update({ ...payload, id: form.id })
      message.success('盘点单已更新')
    } else {
      const res: any = await checkApi.save({ ...payload, status: 0 })
      form.id = res?.id || 0
      message.success('盘点单已创建，可录入明细')
    }
    loadList()
  } catch (e: any) { message.error(e?.message || '保存失败') }
  finally { saving.value = false }
}

function handleNew() {
  Object.assign(form, emptyForm())
  results.value = []
}

// ═══ 工作流动作 ═══
async function doAction(fn: () => Promise<any>, ok: string) {
  acting.value = true
  try { await fn(); message.success(ok); await refreshCurrent(); loadList() }
  catch (e: any) { message.error(e?.message || '操作失败') }
  finally { acting.value = false }
}
function handleStart() { const u = currentUser(); doAction(() => checkApi.startCheck(form.id, u.userId, u.userName), '已开始盘点') }
function handleSubmitResult() { const u = currentUser(); doAction(() => checkApi.submitResult(form.id, u.userId, u.userName), '盘点结果已提交') }
function handleApprove() { const u = currentUser(); doAction(() => checkApi.approveCheck(form.id, u.userId), '盘点已审核') }
function handleCancel() { doAction(() => checkApi.cancelCheck(form.id, 'PC端取消盘点'), '盘点单已取消') }
function handleStartRow(r: any) { const u = currentUser(); doAction(() => checkApi.startCheck(r.id, u.userId, u.userName), '已开始盘点') }
function handleSubmitRow(r: any) { const u = currentUser(); doAction(() => checkApi.submitResult(r.id, u.userId, u.userName), '盘点结果已提交') }
function handleApproveRow(r: any) { const u = currentUser(); doAction(() => checkApi.approveCheck(r.id, u.userId), '盘点已审核') }

async function refreshCurrent() {
  if (!form.id) return
  try {
    const t: any = await checkApi.getById(form.id)
    if (t) fillForm(t)
  } catch (e) { console.warn('[盘点单] 刷新单据失败', e) }
}

// ═══ 盘点结果（待盘点可编辑） ═══
type EditableDetail = Partial<WmsCheckResult> & { _key: string }
let rowSeq = 0
const results = ref<EditableDetail[]>([])
const resultLoading = ref(false)
const detailSaving = ref(false)
const detailEditable = computed(() => !!form.id && form.status === 0)

const resultColumns = computed<any[]>(() => {
  if (detailEditable.value) {
    return [
      { title: '行号', dataIndex: 'lineNo', width: 56 },
      { title: '商品', dataIndex: 'productId', width: 220 },
      { title: '规格', dataIndex: 'productSpec', width: 100 },
      { title: '单位', dataIndex: 'productUnit', width: 64 },
      { title: '货位', dataIndex: 'locationId', width: 150 },
      { title: '批次号', dataIndex: 'batchNo', width: 110 },
      { title: '账面数量', dataIndex: 'bookQuantity', width: 110, align: 'right' },
      { title: '实盘数量', dataIndex: 'actualQuantity', width: 110, align: 'right' },
      { title: '操作', dataIndex: 'action', width: 70, fixed: 'right' },
    ]
  }
  return [
    { title: '行号', dataIndex: 'lineNo', width: 60 },
    { title: '商品编码', dataIndex: 'productCode', width: 120 },
    { title: '商品名称', dataIndex: 'productName', width: 180, ellipsis: true },
    { title: '规格', dataIndex: 'productSpec', width: 100 },
    { title: '单位', dataIndex: 'productUnit', width: 70 },
    { title: '货位', dataIndex: 'locationCode', width: 110 },
    { title: '批次号', dataIndex: 'batchNo', width: 110 },
    { title: '账面数量', dataIndex: 'bookQuantity', width: 100, align: 'right' },
    { title: '实盘数量', dataIndex: 'actualQuantity', width: 100, align: 'right' },
    { title: '差异数量', dataIndex: 'diffQuantity', width: 100, align: 'right' },
    { title: '差异类型', dataIndex: 'diffType', width: 90 },
    { title: '差异金额', dataIndex: 'diffAmount', width: 110, align: 'right' },
  ]
})

function addDetailRow() {
  results.value.push({ _key: `n${++rowSeq}`, productId: undefined, locationId: undefined, batchNo: '', bookQuantity: undefined, actualQuantity: undefined })
}
function removeDetailRow(key: string) {
  results.value = results.value.filter(r => r._key !== key)
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
function handleLocationChange(row: any, val: number | undefined) {
  const l = locations.value.find(x => x.id === val)
  row.locationCode = l?.locationCode || ''
}

async function handleSaveDetails() {
  if (!form.id) return
  if (!results.value.length) { message.warning('请先添加明细行'); return }
  if (results.value.some(r => !r.productId)) { message.warning('明细行请选择商品'); return }
  if (results.value.some(r => !(Number(r.bookQuantity) >= 0))) { message.warning('明细行请填写账面数量'); return }
  detailSaving.value = true
  try {
    const payload: Partial<WmsCheckResult>[] = results.value.map(r => ({
      productId: r.productId, productCode: r.productCode, productName: r.productName,
      productSpec: r.productSpec, productUnit: r.productUnit,
      locationId: r.locationId, locationCode: r.locationCode, batchNo: r.batchNo || '',
      bookQuantity: Number(r.bookQuantity) || 0,
      actualQuantity: r.actualQuantity === undefined || r.actualQuantity === null ? undefined : Number(r.actualQuantity),
    }))
    await checkApi.saveDetails(form.id, payload)
    message.success('盘点明细已保存')
    await loadResults(form.id)
    await refreshCurrent()
    loadList()
  } catch (e: any) { message.error(e?.message || '明细保存失败') }
  finally { detailSaving.value = false }
}

async function loadResults(taskId: number) {
  resultLoading.value = true
  try {
    const list: any = await checkApi.getResults(taskId)
    results.value = (Array.isArray(list) ? list : []).map((d: any) => ({ ...d, _key: `s${d.id}` }))
  } catch (e) { results.value = []; console.warn('[盘点单] 结果获取失败', e) }
  finally { resultLoading.value = false }
}

// ═══ 最近盘点单列表 ═══
const list = ref<WmsCheckTask[]>([])
const listLoading = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })
const listColumns: any[] = [
  { title: '单号', dataIndex: 'taskNo', width: 170 },
  { title: '盘点类型', dataIndex: 'checkType', width: 100 },
  { title: '仓库', dataIndex: 'warehouseName', width: 110 },
  { title: '进度(项)', dataIndex: 'progress', width: 100, align: 'right' },
  { title: '差异项', dataIndex: 'diffItems', width: 80, align: 'right' },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '盘点人', dataIndex: 'checkerName', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 130 },
  { title: '操作', dataIndex: 'action', width: 160, fixed: 'right' },
]
async function loadList() {
  listLoading.value = true
  try {
    const res: any = await checkApi.page({ current: pagination.current, size: pagination.pageSize })
    list.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e) { list.value = []; pagination.total = 0; console.warn('[盘点单] 列表获取失败', e) }
  finally { listLoading.value = false }
}
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadList() }

function fillForm(t: WmsCheckTask) {
  Object.assign(form, {
    id: t.id, taskNo: t.taskNo, warehouseId: t.warehouseId ?? undefined, warehouseName: t.warehouseName || '',
    checkType: t.checkType ?? 0, scopeType: t.scopeType ?? 0, remark: t.remark || '', status: t.status ?? 0,
  })
  if (t.warehouseId) loadLocations(t.warehouseId)
}
function loadRecord(r: any) {
  fillForm(r)
  loadResults(r.id)
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
