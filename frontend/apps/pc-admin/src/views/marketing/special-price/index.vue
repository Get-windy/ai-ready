<template>
  <ErrorBoundary>
    <PageContainer title="特价管理">
      <!-- ═══ 商品选择区 ═══ -->
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="特价商品">
            <a-select
              v-model:value="selectedProductId"
              :options="productOptions"
              placeholder="请选择商品查看/维护等级特价"
              :filter-option="filterOption"
              show-search
              style="width: 320px"
              @change="loadPrices"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                :disabled="!selectedProductId"
                @click="openCreate"
              >
                <template #icon>
                  <PlusOutlined />
                </template>新增特价
              </a-button>
              <a-button
                :disabled="!selectedProductId"
                @click="loadPrices"
              >
                <template #icon>
                  <ReloadOutlined />
                </template>刷新
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 特价列表 ═══ -->
      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="priceList"
          :loading="loading"
          row-key="id"
          size="small"
          :pagination="false"
          :locale="{ emptyText: selectedProductId ? '该商品暂无等级特价' : '请先选择商品' }"
        >
          <template #bodyCell="{ column, record, text }">
            <template v-if="['basePrice', 'gradePrice', 'discountAmount'].includes(column.dataIndex as string)">
              {{ formatMoney(text) }}
            </template>
            <template v-else-if="column.dataIndex === 'discountRate'">
              {{ text != null ? `${(Number(text) * 10).toFixed(1)} 折` : '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'qtyRange'">
              {{ record.minOrderQty ?? 1 }} ~ {{ record.maxOrderQty ?? '不限' }}
            </template>
            <template v-else-if="column.dataIndex === 'period'">
              {{ fmtDate(record.effectiveDate) }} ~ {{ fmtDate(record.expireDate) }}
            </template>
            <template v-else-if="column.dataIndex === 'isActive'">
              <a-switch
                :checked="record.isActive === 1"
                checked-children="启用"
                un-checked-children="停用"
                size="small"
                @change="(checked: boolean) => toggleActive(record, checked)"
              />
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a @click="openEdit(record)">编辑</a>
                <a-divider type="vertical" />
                <a-popconfirm
                  title="确认删除该特价？"
                  @confirm="handleDelete(record)"
                >
                  <a class="text-danger">删除</a>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <!-- 新增/编辑特价弹窗 -->
      <a-modal
        v-model:open="modalVisible"
        :title="editingPrice ? '编辑等级特价' : '新增等级特价'"
        :confirm-loading="modalLoading"
        :width="560"
        @ok="handleModalOk"
        @cancel="modalVisible = false"
      >
        <a-form
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
          style="margin-top: 16px"
        >
          <a-form-item
            label="客户等级"
            required
          >
            <a-input
              v-model:value="modalForm.gradeName"
              placeholder="如：VIP客户 / 一级经销商"
            />
          </a-form-item>
          <a-form-item label="等级编码">
            <a-input
              v-model:value="modalForm.gradeCode"
              placeholder="如：VIP1"
            />
          </a-form-item>
          <a-form-item label="基准价">
            <a-input-number
              v-model:value="modalForm.basePrice"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item
            label="特价"
            required
          >
            <a-input-number
              v-model:value="modalForm.gradePrice"
              :min="0"
              :precision="2"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="数量区间">
            <a-space>
              <a-input-number
                v-model:value="modalForm.minOrderQty"
                :min="1"
                placeholder="最少"
              />
              <span>~</span>
              <a-input-number
                v-model:value="modalForm.maxOrderQty"
                :min="1"
                placeholder="最多"
              />
            </a-space>
          </a-form-item>
          <a-form-item label="有效期">
            <a-range-picker
              v-model:value="modalForm.dateRange"
              style="width: 100%"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea
              v-model:value="modalForm.remark"
              :rows="2"
              placeholder="选填"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { gradePriceApi, type ProductGradePrice } from '@/api/marketing'
import { optionsApi, type OptionItem } from '@/api/options'

// ═══ 商品选择 ═══
const productOptions = ref<{ label: string; value: number }[]>([])
const selectedProductId = ref<number>()

function filterOption(input: string, option: any) {
  return (option?.label || '').toLowerCase().includes(input.toLowerCase())
}

// ═══ 特价列表 ═══
const loading = ref(false)
const priceList = ref<ProductGradePrice[]>([])

const columns: any[] = [
  { title: '客户等级', dataIndex: 'gradeName', key: 'gradeName', width: 140 },
  { title: '等级编码', dataIndex: 'gradeCode', key: 'gradeCode', width: 110 },
  { title: '基准价', dataIndex: 'basePrice', key: 'basePrice', width: 110, align: 'right' },
  { title: '特价', dataIndex: 'gradePrice', key: 'gradePrice', width: 110, align: 'right' },
  { title: '数量区间', dataIndex: 'qtyRange', key: 'qtyRange', width: 120 },
  { title: '有效期', dataIndex: 'period', key: 'period', width: 210 },
  { title: '状态', dataIndex: 'isActive', key: 'isActive', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 140, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fmtDate(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD') : '不限'
}

async function loadPrices() {
  if (!selectedProductId.value) return
  loading.value = true
  try {
    priceList.value = await gradePriceApi.byProduct(selectedProductId.value)
  } catch (e) {
    priceList.value = []
    console.warn('[特价管理] 等级特价获取失败', e)
  } finally {
    loading.value = false
  }
}

// ═══ 新增/编辑弹窗 ═══
const modalVisible = ref(false)
const modalLoading = ref(false)
const editingPrice = ref<ProductGradePrice | null>(null)
const modalForm = reactive<{
  gradeName?: string
  gradeCode?: string
  basePrice?: number
  gradePrice?: number
  minOrderQty?: number
  maxOrderQty?: number
  dateRange?: [Dayjs, Dayjs]
  remark?: string
}>({})

function openCreate() {
  editingPrice.value = null
  Object.assign(modalForm, { gradeName: undefined, gradeCode: undefined, basePrice: undefined, gradePrice: undefined, minOrderQty: 1, maxOrderQty: undefined, dateRange: undefined, remark: undefined })
  modalVisible.value = true
}

function openEdit(record: any) {
  editingPrice.value = record
  Object.assign(modalForm, {
    gradeName: record.gradeName,
    gradeCode: record.gradeCode,
    basePrice: record.basePrice,
    gradePrice: record.gradePrice,
    minOrderQty: record.minOrderQty,
    maxOrderQty: record.maxOrderQty,
    dateRange: record.effectiveDate && record.expireDate ? [dayjs(record.effectiveDate), dayjs(record.expireDate)] : undefined,
    remark: record.remark
  })
  modalVisible.value = true
}

async function handleModalOk() {
  if (!modalForm.gradeName) {
    message.warning('请输入客户等级')
    return
  }
  if (modalForm.gradePrice === undefined || modalForm.gradePrice === null) {
    message.warning('请输入特价')
    return
  }
  modalLoading.value = true
  try {
    const payload: Partial<ProductGradePrice> = {
      productId: selectedProductId.value,
      gradeName: modalForm.gradeName,
      gradeCode: modalForm.gradeCode,
      basePrice: modalForm.basePrice,
      gradePrice: modalForm.gradePrice,
      minOrderQty: modalForm.minOrderQty,
      maxOrderQty: modalForm.maxOrderQty,
      remark: modalForm.remark,
      effectiveDate: modalForm.dateRange?.[0] ? modalForm.dateRange[0].format('YYYY-MM-DDT00:00:00') : undefined,
      expireDate: modalForm.dateRange?.[1] ? modalForm.dateRange[1].format('YYYY-MM-DDT23:59:59') : undefined
    }
    if (editingPrice.value) {
      await gradePriceApi.update(editingPrice.value.id, payload)
      message.success('更新成功')
    } else {
      await gradePriceApi.create({ ...payload, isActive: 1 })
      message.success('创建成功')
    }
    modalVisible.value = false
    loadPrices()
  } catch (e) {
    console.warn('[特价管理] 保存失败', e)
  } finally {
    modalLoading.value = false
  }
}

// ═══ 启用/停用 / 删除 ═══
async function toggleActive(record: any, checked: boolean) {
  try {
    await gradePriceApi.update(record.id, { isActive: checked ? 1 : 0 })
    message.success(checked ? '已启用' : '已停用')
    loadPrices()
  } catch (e) {
    console.warn('[特价管理] 状态更新失败', e)
  }
}

async function handleDelete(record: any) {
  try {
    await gradePriceApi.remove(record.id)
    message.success('删除成功')
    loadPrices()
  } catch (e) {
    console.warn('[特价管理] 删除失败', e)
  }
}

onMounted(async () => {
  try {
    const list: OptionItem[] = await optionsApi.getProducts()
    productOptions.value = list.map(p => ({ label: `${p.name}(${p.code})`, value: p.id }))
  } catch (e) {
    console.warn('[特价管理] 商品列表获取失败', e)
  }
})
</script>

<style scoped>
.search-area {
  background: #fff;
  padding: 16px 20px 0;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.text-danger {
  color: #ff4d4f;
}
</style>
