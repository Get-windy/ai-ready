<template>
  <ErrorBoundary>
    <PageContainer title="借进单">
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
            <a-input v-model:value="form.orderNo" style="width: 180px" placeholder="保存后自动生成" disabled />
          </a-form-item>
          <a-form-item label="往来单位" required>
            <a-select
              v-model:value="form.partnerId"
              style="width: 220px"
              placeholder="请选择往来单位"
              :options="partnerOptions"
              :loading="partnerLoading"
              show-search
              option-filter-prop="label"
              :disabled="!editable"
              @change="handlePartnerChange"
            />
          </a-form-item>
          <a-form-item label="仓库" required>
            <a-select
              v-model:value="form.warehouseId"
              style="width: 170px"
              placeholder="请选择仓库"
              :options="warehouseOptions"
              :loading="warehouseLoading"
              :disabled="!editable"
              @change="handleWarehouseChange"
            />
          </a-form-item>
          <a-form-item label="借进日期">
            <a-date-picker
              v-model:value="form.borrowDate"
              value-format="YYYY-MM-DD"
              style="width: 150px"
              :disabled="!editable"
            />
          </a-form-item>
          <a-form-item label="预计归还日">
            <a-date-picker
              v-model:value="form.expectedReturnDate"
              value-format="YYYY-MM-DD"
              style="width: 150px"
              :disabled="!editable"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="form.remark" style="width: 220px" placeholder="备注" :disabled="!editable" />
          </a-form-item>
        </a-form>
        <div class="btn-row">
          <a-space>
            <a-button type="primary" :loading="saving" :disabled="!editable" @click="handleSave">
              保存草稿
            </a-button>
            <a-button @click="handleNew">新建</a-button>
            <template v-if="form.id">
              <a-popconfirm v-if="form.status === 0" title="确认提交该借进单审批？" @confirm="handleSubmit">
                <a-button type="primary" ghost :loading="acting">提交审批</a-button>
              </a-popconfirm>
              <a-popconfirm v-if="form.status === 1" title="审批通过后借进商品库存将增加，确认审批通过？" @confirm="handleApprove">
                <a-button type="primary" ghost :loading="acting">审批通过</a-button>
              </a-popconfirm>
              <a-button
                v-if="form.status === 2 || form.status === 3"
                type="primary"
                ghost
                @click="openReturnModal(form.id, form.orderNo)"
              >归还</a-button>
              <a-popconfirm v-if="form.status === 0 || form.status === 1" title="确认取消该借进单？" @confirm="handleCancel">
                <a-button danger :loading="acting">取消单据</a-button>
              </a-popconfirm>
              <a-popconfirm v-if="form.status === 0 || form.status === 5" title="确认删除该借进单？删除后不可恢复" @confirm="handleRemove">
                <a-button danger :loading="acting">删除</a-button>
              </a-popconfirm>
            </template>
          </a-space>
        </div>
      </div>

      <!-- ═══ 借进明细（草稿可编辑） ═══ -->
      <div class="panel">
        <div class="panel-title">
          借进明细
          <a-tag v-if="!detailEditable" color="default">仅草稿状态可编辑</a-tag>
          <a-button v-if="detailEditable" size="small" type="link" @click="addDetailRow">+ 添加明细</a-button>
        </div>
        <a-table
          :columns="detailColumns"
          :data-source="details"
          :pagination="false"
          row-key="_key"
          size="small"
          :locale="{ emptyText: detailEditable ? '点击「添加明细」录入借进商品' : '暂无明细' }"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.dataIndex === 'lineNo'">
              {{ index + 1 }}
            </template>
            <template v-else-if="column.dataIndex === 'productId'">
              <a-select
                v-if="detailEditable"
                v-model:value="record.productId"
                style="width: 100%"
                placeholder="选择商品"
                :options="productOptions"
                :loading="productLoading"
                show-search
                option-filter-prop="label"
                @change="(val: any) => handleProductChange(record, val)"
              />
              <span v-else>{{ record.productCode }} {{ record.productName }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'quantity'">
              <a-input-number
                v-if="detailEditable"
                v-model:value="record.quantity"
                :min="0"
                style="width: 100%"
                placeholder="数量"
              />
              <span v-else>{{ formatQty(record.quantity) }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'price'">
              <a-input-number
                v-if="detailEditable"
                v-model:value="record.price"
                :min="0"
                :precision="2"
                style="width: 100%"
                placeholder="单价"
              />
              <span v-else>{{ formatQty(record.price) }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'amount'">
              {{ formatQty((Number(record.quantity) || 0) * (Number(record.price) || 0)) }}
            </template>
            <template v-else-if="column.dataIndex === 'returnedQuantity'">
              {{ formatQty(record.returnedQuantity) }}
            </template>
            <template v-else-if="column.dataIndex === 'remark'">
              <a-input v-if="detailEditable" v-model:value="record.remark" placeholder="行备注" />
              <span v-else>{{ record.remark || '-' }}</span>
            </template>
            <template v-else-if="column.dataIndex === 'action'">
              <a-popconfirm v-if="detailEditable" title="删除该行？" @confirm="removeDetailRow(record._key)">
                <a-button size="small" type="link" danger>删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </div>

      <!-- ═══ 最近借进单 ═══ -->
      <div class="panel">
        <div class="panel-title">
          最近借进单
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
            <template v-if="column.dataIndex === 'totalQuantity'">
              {{ formatQty(record.totalQuantity) }}
            </template>
            <template v-else-if="column.dataIndex === 'returnProgress'">
              <a-progress
                :percent="returnPercent(record)"
                size="small"
                style="width: 110px"
                :format="() => `${formatQty(record.returnedQuantity)} / ${formatQty(record.totalQuantity)}`"
              />
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
                <a-button v-if="record.status === 0" size="small" type="link" @click="handleSubmitRow(record)">提交</a-button>
                <a-button v-if="record.status === 1" size="small" type="link" @click="handleApproveRow(record)">审批</a-button>
                <a-button
                  v-if="record.status === 2 || record.status === 3"
                  size="small"
                  type="link"
                  @click="openReturnModal(record.id, record.orderNo)"
                >归还</a-button>
                <a-popconfirm v-if="record.status === 0 || record.status === 1" title="确认取消该单？" @confirm="handleCancelRow(record)">
                  <a-button size="small" type="link" danger>取消</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <!-- ═══ 归还登记弹窗 ═══ -->
      <a-modal
        v-model:open="returnModalOpen"
        :title="`归还登记 - ${returnOrderNo}`"
        width="760px"
        :confirm-loading="returning"
        @ok="handleReturnSubmit"
      >
        <a-form layout="inline" class="return-form">
          <a-form-item label="归还日期">
            <a-date-picker v-model:value="returnForm.returnDate" value-format="YYYY-MM-DD" style="width: 150px" />
          </a-form-item>
          <a-form-item label="备注">
            <a-input v-model:value="returnForm.remark" style="width: 320px" placeholder="归还备注" />
          </a-form-item>
        </a-form>
        <a-table
          :columns="returnColumns"
          :data-source="returnItems"
          :loading="returnLoading"
          :pagination="false"
          row-key="orderItemId"
          size="small"
          :locale="{ emptyText: '该单暂无明细' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="['quantity', 'returnedQuantity', 'remaining'].includes(String(column.dataIndex))">
              {{ formatQty(record[String(column.dataIndex)]) }}
            </template>
            <template v-else-if="column.dataIndex === 'returnQuantity'">
              <a-input-number
                v-model:value="record.returnQuantity"
                :min="0"
                :max="record.remaining"
                style="width: 120px"
                :disabled="record.remaining <= 0"
                placeholder="0"
              />
            </template>
          </template>
        </a-table>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { borrowApi, type WmsBorrowOrder, type WmsBorrowOrderItem, type WmsBorrowOrderVO } from '@/api/wms/borrow'
import { warehouseApi } from '@/api/wms/warehouse'
import { partnerApi } from '@/api/erp/partner'
import { productApi, type Product } from '@/api/erp/product'
import { useUserStore } from '@/stores/user'
import { formatQty, formatTime } from '../../whTask'

defineOptions({ name: 'WhBorrowInForm' })

/** 方向：1-借进（与后端 WmsBorrowOrder.direction 对齐） */
const DIRECTION = 1

const userStore = useUserStore()

// ═══ 字典 ═══
const BORROW_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '部分归还', color: 'gold' },
  4: { text: '已归还', color: 'green' },
  5: { text: '已取消', color: 'red' },
}
function statusText(s: number) { return BORROW_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return BORROW_STATUS_MAP[s]?.color || 'default' }
function returnPercent(r: any) {
  const total = Number(r.totalQuantity) || 0
  if (total <= 0) return 0
  return Math.min(100, Math.round(((Number(r.returnedQuantity) || 0) / total) * 100))
}

// ═══ 下拉选项 ═══
const warehouseOptions = ref<{ label: string; value: number }[]>([])
const warehouseLoading = ref(false)
const partnerOptions = ref<{ label: string; value: number }[]>([])
const partnerLoading = ref(false)
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
  } catch (e) { console.warn('[借进单] 仓库列表获取失败', e) }
  finally { warehouseLoading.value = false }
}
async function loadPartners() {
  partnerLoading.value = true
  try {
    const list = await partnerApi.list(undefined, undefined, 500)
    partnerOptions.value = (Array.isArray(list) ? list : []).map(p => ({ label: p.partnerName, value: p.id }))
  } catch (e) { console.warn('[借进单] 往来单位获取失败', e) }
  finally { partnerLoading.value = false }
}
async function loadProducts() {
  productLoading.value = true
  try {
    const res = await productApi.page({ pageNum: 1, pageSize: 500 })
    products.value = res?.records || []
  } catch (e) { products.value = []; console.warn('[借进单] 商品列表获取失败', e) }
  finally { productLoading.value = false }
}

// ═══ 单据头表单 ═══
function todayStr() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
const emptyForm = () => ({
  id: 0, orderNo: '', partnerId: undefined as number | undefined, partnerName: '',
  warehouseId: undefined as number | undefined, warehouseName: '',
  borrowDate: todayStr(), expectedReturnDate: undefined as string | undefined, remark: '', status: 0,
})
const form = reactive(emptyForm())
const editable = computed(() => !form.id || form.status === 0)
const detailEditable = computed(() => editable.value)
const saving = ref(false)
const acting = ref(false)

function currentUser() {
  return { userId: userStore.userId || 0, userName: userStore.userInfo?.nickname || userStore.userInfo?.username || '系统' }
}

function handlePartnerChange(val: number) {
  const p = partnerOptions.value.find(o => o.value === val)
  form.partnerName = p?.label || ''
}
function handleWarehouseChange(val: number) {
  const w = warehouseOptions.value.find(o => o.value === val)
  form.warehouseName = w?.label || ''
}

// ═══ 明细行 ═══
interface EditableBorrowItem extends Partial<WmsBorrowOrderItem> { _key: string }
let rowSeq = 0
const details = ref<EditableBorrowItem[]>([])

function addDetailRow() {
  details.value.push({ _key: `n${++rowSeq}`, productId: undefined, quantity: undefined, price: undefined, remark: '' })
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
  row.unit = p.unit || ''
  if (row.price === undefined || row.price === null) {
    row.price = p.costPrice || p.standardPrice || 0
  }
}

const detailColumns = computed<any[]>(() => {
  const cols: any[] = [
    { title: '行号', dataIndex: 'lineNo', width: 56 },
    { title: '商品', dataIndex: 'productId', width: 240 },
    { title: '规格', dataIndex: 'productSpec', width: 100 },
    { title: '单位', dataIndex: 'unit', width: 64 },
    { title: '数量', dataIndex: 'quantity', width: 110, align: 'right' },
    { title: '单价', dataIndex: 'price', width: 110, align: 'right' },
    { title: '金额', dataIndex: 'amount', width: 110, align: 'right' },
  ]
  if (!detailEditable.value) cols.push({ title: '已归还', dataIndex: 'returnedQuantity', width: 90, align: 'right' })
  cols.push({ title: '备注', dataIndex: 'remark', width: 140 })
  if (detailEditable.value) cols.push({ title: '操作', dataIndex: 'action', width: 70, fixed: 'right' })
  return cols
})

// ═══ 保存草稿 ═══
async function handleSave() {
  if (!form.partnerId) { message.warning('请选择往来单位'); return }
  if (!form.warehouseId) { message.warning('请选择仓库'); return }
  const rows = details.value.filter(r => r.productId)
  if (rows.some(r => !(Number(r.quantity) > 0))) { message.warning('明细行请填写大于0的数量'); return }
  const items: Partial<WmsBorrowOrderItem>[] = rows.map((r, i) => ({
    lineNo: i + 1,
    productId: r.productId, productCode: r.productCode, productName: r.productName,
    productSpec: r.productSpec, unit: r.unit,
    quantity: Number(r.quantity), price: Number(r.price) || 0,
    amount: (Number(r.quantity) || 0) * (Number(r.price) || 0),
    remark: r.remark || '',
  }))
  saving.value = true
  try {
    const payload: Partial<WmsBorrowOrderVO> = {
      direction: DIRECTION,
      partnerId: form.partnerId, partnerName: form.partnerName,
      warehouseId: form.warehouseId, warehouseName: form.warehouseName,
      borrowDate: form.borrowDate, expectedReturnDate: form.expectedReturnDate,
      remark: form.remark, items: items as WmsBorrowOrderItem[],
    }
    if (form.id) {
      await borrowApi.update({ ...payload, id: form.id })
      message.success('借进单草稿已更新')
    } else {
      const res: any = await borrowApi.create(payload)
      form.id = res?.id || 0
      message.success(`借进单已保存草稿：${res?.orderNo || ''}`)
    }
    await refreshCurrent()
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
function handleSubmit() { doAction(() => borrowApi.submit(form.id), '已提交审批') }
function handleApprove() { const u = currentUser(); doAction(() => borrowApi.approve(form.id, u.userId, u.userName), '审批通过，库存已增加') }
function handleCancel() { doAction(() => borrowApi.cancel(form.id), '单据已取消') }
function handleSubmitRow(r: any) { doAction(() => borrowApi.submit(r.id), '已提交审批') }
function handleApproveRow(r: any) { const u = currentUser(); doAction(() => borrowApi.approve(r.id, u.userId, u.userName), '审批通过，库存已增加') }
function handleCancelRow(r: any) { doAction(() => borrowApi.cancel(r.id), '单据已取消') }
async function handleRemove() {
  acting.value = true
  try {
    await borrowApi.remove(form.id)
    message.success('单据已删除')
    handleNew()
    loadList()
  } catch (e: any) { message.error(e?.message || '删除失败') }
  finally { acting.value = false }
}

async function refreshCurrent() {
  if (!form.id) return
  try {
    const vo: any = await borrowApi.getById(form.id)
    if (vo) fillForm(vo)
  } catch (e) { console.warn('[借进单] 刷新单据失败', e) }
}

// ═══ 最近借进单列表 ═══
const list = ref<WmsBorrowOrder[]>([])
const listLoading = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })
const listColumns: any[] = [
  { title: '单号', dataIndex: 'orderNo', width: 170 },
  { title: '往来单位', dataIndex: 'partnerName', width: 160, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', width: 110 },
  { title: '借进日期', dataIndex: 'borrowDate', width: 110 },
  { title: '预计归还日', dataIndex: 'expectedReturnDate', width: 110 },
  { title: '总数量', dataIndex: 'totalQuantity', width: 90, align: 'right' },
  { title: '归还进度(还/总)', dataIndex: 'returnProgress', width: 190 },
  { title: '状态', dataIndex: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', width: 130 },
  { title: '操作', dataIndex: 'action', width: 190, fixed: 'right' },
]
async function loadList() {
  listLoading.value = true
  try {
    const res: any = await borrowApi.page({ current: pagination.current, size: pagination.pageSize, direction: DIRECTION })
    list.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (e) { list.value = []; pagination.total = 0; console.warn('[借进单] 列表获取失败', e) }
  finally { listLoading.value = false }
}
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadList() }

function fillForm(vo: WmsBorrowOrderVO) {
  Object.assign(form, {
    id: vo.id, orderNo: vo.orderNo || '', partnerId: vo.partnerId ?? undefined, partnerName: vo.partnerName || '',
    warehouseId: vo.warehouseId ?? undefined, warehouseName: vo.warehouseName || '',
    borrowDate: vo.borrowDate || todayStr(), expectedReturnDate: vo.expectedReturnDate || undefined,
    remark: vo.remark || '', status: vo.status ?? 0,
  })
  details.value = (vo.items || []).map(it => ({
    _key: `s${it.id}`, id: it.id, orderId: it.orderId, productId: it.productId,
    productCode: it.productCode, productName: it.productName, productSpec: it.productSpec, unit: it.unit,
    quantity: it.quantity, returnedQuantity: it.returnedQuantity, price: it.price, amount: it.amount, remark: it.remark,
  }))
}
function loadRecord(r: any) {
  // 列表行不含明细，载入完整单据（含明细）
  form.id = r.id
  refreshCurrent()
}

// ═══ 归还登记 ═══
interface ReturnRow {
  orderItemId: number
  productCode: string
  productName: string
  quantity: number
  returnedQuantity: number
  remaining: number
  returnQuantity: number
}
const returnModalOpen = ref(false)
const returnOrderId = ref(0)
const returnOrderNo = ref('')
const returnItems = ref<ReturnRow[]>([])
const returnLoading = ref(false)
const returning = ref(false)
const returnForm = reactive({ returnDate: todayStr(), remark: '' })
const returnColumns: any[] = [
  { title: '商品编码', dataIndex: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', width: 180, ellipsis: true },
  { title: '借进数量', dataIndex: 'quantity', width: 100, align: 'right' },
  { title: '已还', dataIndex: 'returnedQuantity', width: 90, align: 'right' },
  { title: '未还', dataIndex: 'remaining', width: 90, align: 'right' },
  { title: '本次归还', dataIndex: 'returnQuantity', width: 140 },
]

async function openReturnModal(orderId: number, orderNo: string) {
  returnOrderId.value = orderId
  returnOrderNo.value = orderNo || ''
  returnForm.returnDate = todayStr()
  returnForm.remark = ''
  returnItems.value = []
  returnModalOpen.value = true
  returnLoading.value = true
  try {
    const vo: any = await borrowApi.getById(orderId)
    returnItems.value = (vo?.items || []).map((it: any) => {
      const qty = Number(it.quantity) || 0
      const returned = Number(it.returnedQuantity) || 0
      return {
        orderItemId: it.id, productCode: it.productCode, productName: it.productName,
        quantity: qty, returnedQuantity: returned, remaining: Math.max(0, qty - returned), returnQuantity: 0,
      }
    })
  } catch (e) { message.error('归还明细加载失败'); console.warn('[借进单] 归还明细获取失败', e) }
  finally { returnLoading.value = false }
}

async function handleReturnSubmit() {
  const items = returnItems.value
    .filter(r => Number(r.returnQuantity) > 0)
    .map(r => ({ orderItemId: r.orderItemId, quantity: Number(r.returnQuantity) }))
  if (!items.length) { message.warning('请填写至少一行的本次归还数量'); return }
  returning.value = true
  try {
    const u = currentUser()
    await borrowApi.returnOrder({
      orderId: returnOrderId.value,
      returnDate: returnForm.returnDate,
      operatorId: u.userId, operatorName: u.userName,
      remark: returnForm.remark, items,
    })
    message.success('归还登记成功，库存已回冲')
    returnModalOpen.value = false
    await refreshCurrent()
    loadList()
  } catch (e: any) { message.error(e?.message || '归还登记失败') }
  finally { returning.value = false }
}

onMounted(() => { loadWarehouses(); loadPartners(); loadProducts(); loadList() })
</script>

<style scoped>
.panel { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.panel-title { font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
.panel-tag { margin-left: 4px; }
.header-form { row-gap: 8px; }
.btn-row { margin-top: 12px; padding-top: 12px; border-top: 1px dashed #f0f0f0; }
.return-form { margin-bottom: 12px; }
</style>
