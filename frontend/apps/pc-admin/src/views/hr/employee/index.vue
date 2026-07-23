<template>
  <ErrorBoundary>
    <PageContainer title="员工管理">
      <template #extra>
        <a-button type="primary" @click="showCreateModal">
          <template #icon><PlusOutlined /></template>
          新增员工
        </a-button>
      </template>

      <!-- 搜索区 -->
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="员工姓名">
            <a-input v-model:value="searchForm.employeeName" placeholder="请输入" allow-clear style="width: 160px" />
          </a-form-item>
          <a-form-item label="员工编号">
            <a-input v-model:value="searchForm.employeeNo" placeholder="请输入" allow-clear style="width: 160px" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="searchForm.status" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option :value="1">在职</a-select-option>
              <a-select-option :value="2">试用</a-select-option>
              <a-select-option :value="0">离职</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="员工类型">
            <a-select v-model:value="searchForm.employeeType" placeholder="全部" allow-clear style="width: 120px">
              <a-select-option :value="1">全职</a-select-option>
              <a-select-option :value="2">兼职</a-select-option>
              <a-select-option :value="3">实习</a-select-option>
              <a-select-option :value="4">外包</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch"><template #icon><SearchOutlined /></template>查询</a-button>
              <a-button @click="handleReset"><template #icon><ClearOutlined /></template>重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 表格 -->
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
            <template v-if="column.key === 'gender'">
              <span>{{ GENDER_MAP[record.gender] || '未知' }}</span>
            </template>
            <template v-if="column.key === 'education'">
              <span>{{ EDUCATION_MAP[record.education] || '-' }}</span>
            </template>
            <template v-if="column.key === 'employeeType'">
              <span>{{ EMPLOYEE_TYPE_MAP[record.employeeType] || '-' }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="EMPLOYEE_STATUS_MAP[record.status]?.color">
                {{ EMPLOYEE_STATUS_MAP[record.status]?.text }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="showEditModal(record)">编辑</a-button>
                <a-button type="link" size="small" @click="showContracts(record)">合同</a-button>
                <a-button v-if="record.status === 2" type="link" size="small" @click="handleRegularize(record)">转正</a-button>
                <a-button v-if="record.status === 1" type="link" size="small" danger @click="handleResign(record)">离职</a-button>
                <a-popconfirm title="确认删除？" @confirm="handleDelete(record)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑员工' : '新增员工'"
      width="720px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form ref="formRef" :model="form" :rules="rules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="员工编号" name="employeeNo">
              <a-input v-model:value="form.employeeNo" placeholder="自动生成或手动输入" :disabled="!!editingId" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="员工姓名" name="employeeName" required>
              <a-input v-model:value="form.employeeName" placeholder="请输入" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="性别">
              <a-select v-model:value="form.gender">
                <a-select-option :value="1">男</a-select-option>
                <a-select-option :value="2">女</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="出生日期">
              <a-date-picker v-model:value="form.birthDate" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="手机号">
              <a-input v-model:value="form.phone" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input v-model:value="form.email" placeholder="请输入" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="身份证号">
              <a-input v-model:value="form.idCard" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="学历">
              <a-select v-model:value="form.education">
                <a-select-option :value="4">大专</a-select-option>
                <a-select-option :value="5">本科</a-select-option>
                <a-select-option :value="6">硕士</a-select-option>
                <a-select-option :value="7">博士</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="毕业院校">
              <a-input v-model:value="form.school" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="专业">
              <a-input v-model:value="form.major" placeholder="请输入" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="员工类型">
              <a-select v-model:value="form.employeeType">
                <a-select-option :value="1">全职</a-select-option>
                <a-select-option :value="2">兼职</a-select-option>
                <a-select-option :value="3">实习</a-select-option>
                <a-select-option :value="4">外包</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="入职日期" name="hireDate" required>
              <a-date-picker v-model:value="form.hireDate" style="width:100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="紧急联系人">
              <a-input v-model:value="form.emergencyContact" placeholder="请输入" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="紧急电话">
              <a-input v-model:value="form.emergencyPhone" placeholder="请输入" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="2" placeholder="请输入" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 合同列表弹窗 -->
    <a-modal v-model:open="contractsModalVisible" title="合同列表" width="720px" :footer="null" :destroy-on-close="true">
      <a-table :data-source="contractsList" :loading="contractsLoading" :pagination="false" row-key="id" size="small">
        <a-table-column title="合同编号" data-index="contractNo" :width="140" />
        <a-table-column title="合同名称" data-index="contractName" />
        <a-table-column title="合同类型" :width="100">
          <template #default="{ record }">{{ CONTRACT_TYPE_MAP[record.contractType] || record.contractType }}</template>
        </a-table-column>
        <a-table-column title="开始日期" data-index="startDate" :width="100" />
        <a-table-column title="结束日期" data-index="endDate" :width="100" />
        <a-table-column title="约定薪资" data-index="salaryAmount" :width="100">
          <template #default="{ record }">¥{{ record.salaryAmount?.toFixed(2) ?? '-' }}</template>
        </a-table-column>
        <a-table-column title="状态" :width="70">
          <template #default="{ record }">{{ CONTRACT_STATUS_MAP[record.status] || record.status }}</template>
        </a-table-column>
      </a-table>
      <div v-if="!contractsList.length" style="text-align:center;padding:24px;color:#999;">暂无合同记录</div>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import {
  hrEmployeeApi, hrContractApi, type HrEmployee, type HrContract,
  GENDER_MAP, EDUCATION_MAP, EMPLOYEE_TYPE_MAP, EMPLOYEE_STATUS_MAP,
  CONTRACT_TYPE_MAP, CONTRACT_STATUS_MAP
} from '@/api/hr'

const loading = ref(false)
const tableData = ref<HrEmployee[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` })
const searchForm = reactive({ employeeName: '', employeeNo: '', status: undefined as number | undefined, employeeType: undefined as number | undefined })

const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const saving = ref(false)
const form = reactive({
  employeeNo: '', employeeName: '', gender: 1, birthDate: null as any,
  phone: '', email: '', idCard: '', education: 5,
  school: '', major: '', hireDate: null as any, employeeType: 1,
  emergencyContact: '', emergencyPhone: '', remark: '',
})
const rules: Record<string, any> = {
  employeeName: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  hireDate: [{ required: true, message: '请选择入职日期', trigger: 'change' }],
}

const columns: any[] = [
  { title: '员工编号', dataIndex: 'employeeNo', key: 'employeeNo', width: 120 },
  { title: '员工姓名', dataIndex: 'employeeName', key: 'employeeName', width: 120 },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 70 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '学历', dataIndex: 'education', key: 'education', width: 80 },
  { title: '员工类型', dataIndex: 'employeeType', key: 'employeeType', width: 80 },
  { title: '入职日期', dataIndex: 'hireDate', key: 'hireDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' },
]

async function loadData() {
  loading.value = true
  try {
    const result = await hrEmployeeApi.page({ pageNum: pagination.current, pageSize: pagination.pageSize, ...searchForm })
    tableData.value = result.records
    pagination.total = result.total
  } catch { message.error('查询失败') }
  finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.employeeName = ''; searchForm.employeeNo = ''; searchForm.status = undefined; searchForm.employeeType = undefined; handleSearch() }
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showCreateModal() {
  editingId.value = null
  Object.assign(form, { employeeNo: '', employeeName: '', gender: 1, birthDate: null, phone: '', email: '', idCard: '', education: 5, school: '', major: '', hireDate: null, employeeType: 1, emergencyContact: '', emergencyPhone: '', remark: '' })
  modalVisible.value = true
}

function showEditModal(record: any) {
  editingId.value = record.id
  Object.assign(form, {
    employeeNo: record.employeeNo, employeeName: record.employeeName, gender: record.gender,
    birthDate: record.birthDate, phone: record.phone || '', email: record.email || '',
    idCard: record.idCard || '', education: record.education, school: record.school || '',
    major: record.major || '', hireDate: record.hireDate, employeeType: record.employeeType,
    emergencyContact: record.emergencyContact || '', emergencyPhone: record.emergencyPhone || '',
    remark: record.remark || '',
  })
  modalVisible.value = true
}

async function handleSave() {
  try { await formRef.value?.validate() } catch { return }
  saving.value = true
  try {
    if (editingId.value) {
      await hrEmployeeApi.update(editingId.value, form as any)
      message.success('更新成功')
    } else {
      await hrEmployeeApi.create(form as any)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch { message.error('保存失败') }
  finally { saving.value = false }
}

async function handleRegularize(record: any) {
  Modal.confirm({ title: '确认转正', content: `确定将 "${record.employeeName}" 转正吗？`, onOk: async () => {
    await hrEmployeeApi.updateStatus(record.id, 1)
    message.success('转正成功'); loadData()
  }})
}

async function handleResign(record: any) {
  Modal.confirm({ title: '确认离职', content: `确定将 "${record.employeeName}" 办理离职吗？`, okType: 'danger', onOk: async () => {
    await hrEmployeeApi.updateStatus(record.id, 0)
    message.success('离职成功'); loadData()
  }})
}

async function handleDelete(record: any) {
  try {
    await hrEmployeeApi.delete(record.id)
    message.success('删除成功'); loadData()
  } catch { message.error('删除失败') }
}

const contractsModalVisible = ref(false)
const contractsList = ref<HrContract[]>([])
const contractsLoading = ref(false)

async function showContracts(record: any) {
  contractsLoading.value = true
  try { contractsList.value = await hrContractApi.listByEmployee(record.id) || [] }
  catch { contractsList.value = [] }
  finally { contractsLoading.value = false }
  contractsModalVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.search-area { background: #fff; padding: 16px 20px; border-radius: 8px; margin-bottom: 16px; }
.table-area { background: #fff; padding: 16px; border-radius: 8px; }
</style>
