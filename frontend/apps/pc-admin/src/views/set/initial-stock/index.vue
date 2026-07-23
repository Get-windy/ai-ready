<template>
  <ErrorBoundary>
    <PageContainer title="库存期初">
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="产品编码">
            <a-input v-model:value="searchParams.productCode" placeholder="请输入" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="产品名称">
            <a-input v-model:value="searchParams.productName" placeholder="请输入" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="仓库">
            <a-select v-model:value="searchParams.warehouseId" placeholder="全部" allow-clear style="width: 160px" :options="warehouseOptions" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch"><template #icon><SearchOutlined /></template>查询</a-button>
              <a-button @click="handleReset"><template #icon><ClearOutlined /></template>重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>
      <div class="toolbar">
        <a-button type="primary" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>录入期初
        </a-button>
      </div>
      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
                <a-popconfirm title="确认删除？" @confirm="handleDelete(record)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>

    <!-- 编辑弹窗 -->
    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑期初' : '录入期初'" width="560px" :confirm-loading="saving" @ok="handleSave">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="产品" name="productId" required>
              <a-select v-model:value="form.productId" show-search :filter-option="filterProduct" placeholder="搜索选择产品" @change="handleProductChange">
                <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">{{ p.productName }} ({{ p.productCode }})</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="产品名称" name="productName">
              <a-input v-model:value="form.productName" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="仓库" name="warehouseId" required>
              <a-select v-model:value="form.warehouseId" placeholder="请选择" :options="warehouseOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="规格型号">
              <a-input v-model:value="form.specification" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="期初数量" name="quantity" required>
              <a-input-number v-model:value="form.quantity" :min="0" :precision="2" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="期初单价" name="unitPrice" required>
              <a-input-number v-model:value="form.unitPrice" :min="0" :precision="4" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="金额">
              <span style="line-height:32px;font-weight:bold">¥{{ (form.quantity * form.unitPrice).toFixed(2) }}</span>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="生产日期">
              <a-date-picker v-model:value="form.productionDate" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="有效期至">
              <a-date-picker v-model:value="form.expirationDate" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="2" placeholder="请输入" />
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'
import { warehouseApi } from '@/api/wms/warehouse'

const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const searchParams = reactive({ productCode: '', productName: '', warehouseId: undefined as number | undefined })

const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()

const productOptions = ref<any[]>([])
const warehouseOptions = ref<any[]>([])

const form = reactive({
  productId: undefined as number | undefined,
  productCode: '', productName: '', specification: '',
  warehouseId: undefined as number | undefined,
  quantity: 0, unitPrice: 0,
  productionDate: null as any,
  expirationDate: null as any,
  remark: '',
})
const rules: Record<string, any> = {
  productId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }],
}

const columns = [
  { title: '产品编码', dataIndex: 'productCode', width: 120 },
  { title: '产品名称', dataIndex: 'productName', width: 200 },
  { title: '规格型号', dataIndex: 'specification', width: 100 },
  { title: '仓库名称', dataIndex: 'warehouseName', width: 140 },
  { title: '期初数量', dataIndex: 'quantity', width: 100, align: 'right' as const },
  { title: '期初单价', dataIndex: 'unitPrice', width: 100, align: 'right' as const },
  { title: '金额', dataIndex: 'amount', width: 120, align: 'right' as const },
  { title: '录入时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

async function loadData() {
  loading.value = true
  try {
    const result = await request.get('/set/initial-stock/page', {
      params: { pageNum: pagination.current, pageSize: pagination.pageSize, ...searchParams }
    }) as any
    if (result?.records) {
      tableData.value = result.records
      pagination.total = result.total
    }
  } catch { message.error('查询失败') }
  finally { loading.value = false }
}

async function loadOptions() {
  try {
    const whRes = await warehouseApi.listAll()
    warehouseOptions.value = ((whRes as any)?.data || whRes || []).map((w: any) => ({ label: w.warehouseName, value: w.id }))
  } catch { warehouseOptions.value = [] }
}

function filterProduct(input: string, option: any) {
  return (option.children || '').toLowerCase().includes(input.toLowerCase())
}

async function handleProductChange(val: number) {
  const p = productOptions.value.find(x => x.id === val)
  if (p) {
    form.productCode = p.productCode
    form.productName = p.productName
    form.specification = p.specification || ''
  }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchParams.productCode = ''; searchParams.productName = ''; searchParams.warehouseId = undefined; handleSearch() }
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showCreateModal() {
  editingId.value = null
  Object.assign(form, { productId: undefined, productCode: '', productName: '', specification: '', warehouseId: undefined, quantity: 0, unitPrice: 0, productionDate: null, expirationDate: null, remark: '' })
  modalVisible.value = true
}

function showEditModal(record: any) {
  editingId.value = record.id
  Object.assign(form, record)
  modalVisible.value = true
}

async function handleSave() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/set/initial-stock/${editingId.value}`, form)
      message.success('更新成功')
    } else {
      await request.post('/set/initial-stock', form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e: any) { message.error(e?.response?.data?.message || '保存失败') }
  finally { saving.value = false }
}

async function handleDelete(record: any) {
  try {
    await request.delete(`/set/initial-stock/${record.id}`)
    message.success('删除成功')
    loadData()
  } catch { message.error('删除失败') }
}

onMounted(() => { loadOptions(); loadData() })
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; }
.toolbar { margin-bottom: 12px; }
.table-area { background: #fff; padding: 16px; border-radius: 8px; }
</style>
