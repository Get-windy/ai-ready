<template>
  <ErrorBoundary>
    <PageContainer title="财务期初">
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="科目代码">
            <a-input v-model:value="searchParams.subjectCode" placeholder="请输入" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="科目名称">
            <a-input v-model:value="searchParams.subjectName" placeholder="请输入" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="年度">
            <a-input-number v-model:value="searchParams.year" placeholder="年度" style="width: 120px" :min="2000" :max="2099" />
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
            <template v-if="column.key === 'direction'">
              <a-tag :color="record.direction === 'DEBIT' ? 'blue' : 'green'">{{ record.direction === 'DEBIT' ? '借' : '贷' }}</a-tag>
            </template>
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
            <a-form-item label="科目" name="subjectId" required>
              <a-select v-model:value="form.subjectId" show-search :filter-option="filterSubject" placeholder="搜索选择科目">
                <a-select-option v-for="s in subjectOptions" :key="s.id" :value="s.id">{{ s.subjectCode }} - {{ s.subjectName }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="科目名称">
              <a-input v-model:value="form.subjectName" disabled />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="年度" name="year" required>
              <a-input-number v-model:value="form.year" :min="2000" :max="2099" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="借贷方向" name="direction" required>
              <a-select v-model:value="form.direction">
                <a-select-option value="DEBIT">借方</a-select-option>
                <a-select-option value="CREDIT">贷方</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="期初余额" name="openBalance" required>
              <a-input-number v-model:value="form.openBalance" :precision="2" style="width:100%" />
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

const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true })
const searchParams = reactive({ subjectCode: '', subjectName: '', year: undefined as number | undefined })

const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref()

const subjectOptions = ref<any[]>([])

const form = reactive({
  subjectId: undefined as number | undefined,
  subjectCode: '', subjectName: '',
  year: new Date().getFullYear(),
  direction: 'DEBIT',
  openBalance: 0,
  remark: '',
})
const rules: Record<string, any> = {
  subjectId: [{ required: true, message: '请选择科目', trigger: 'change' }],
  year: [{ required: true, message: '请输入年度', trigger: 'blur' }],
  direction: [{ required: true, message: '请选择方向', trigger: 'change' }],
  openBalance: [{ required: true, message: '请输入期初余额', trigger: 'blur' }],
}

const columns = [
  { title: '科目代码', dataIndex: 'subjectCode', width: 100 },
  { title: '科目名称', dataIndex: 'subjectName', width: 200 },
  { title: '期初余额', dataIndex: 'openBalance', width: 140, align: 'right' as const },
  { title: '借贷方向', dataIndex: 'direction', width: 80 },
  { title: '录入年度', dataIndex: 'year', width: 80 },
  { title: '备注', dataIndex: 'remark', width: 150, ellipsis: true },
  { title: '录入时间', dataIndex: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
]

async function loadData() {
  loading.value = true
  try {
    const result = await request.get('/set/initial-finance/page', {
      params: { pageNum: pagination.current, pageSize: pagination.pageSize, ...searchParams }
    }) as any
    if (result?.records) {
      tableData.value = result.records
      pagination.total = result.total
    }
  } catch { message.error('查询失败') }
  finally { loading.value = false }
}

async function loadSubjectOptions() {
  try {
    const res = await request.get('/finance/subject/list') as any
    subjectOptions.value = Array.isArray(res) ? res : res?.data || []
  } catch { subjectOptions.value = [] }
}

function filterSubject(input: string, option: any) {
  return (option.children || '').toLowerCase().includes(input.toLowerCase())
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchParams.subjectCode = ''; searchParams.subjectName = ''; searchParams.year = undefined; handleSearch() }
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showCreateModal() {
  editingId.value = null
  Object.assign(form, { subjectId: undefined, subjectCode: '', subjectName: '', year: new Date().getFullYear(), direction: 'DEBIT', openBalance: 0, remark: '' })
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
      await request.put(`/set/initial-finance/${editingId.value}`, form)
      message.success('更新成功')
    } else {
      await request.post('/set/initial-finance', form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e: any) { message.error(e?.response?.data?.message || '保存失败') }
  finally { saving.value = false }
}

async function handleDelete(record: any) {
  try {
    await request.delete(`/set/initial-finance/${record.id}`)
    message.success('删除成功')
    loadData()
  } catch { message.error('删除失败') }
}

onMounted(() => { loadSubjectOptions(); loadData() })
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; }
.toolbar { margin-bottom: 12px; }
.table-area { background: #fff; padding: 16px; border-radius: 8px; }
</style>
