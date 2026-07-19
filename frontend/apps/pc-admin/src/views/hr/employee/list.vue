<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header__left">
        <h2 class="page-title">
          员工管理
        </h2>
      </div>
      <div class="page-header__right">
        <a-button
          type="primary"
          @click="showCreateModal"
        >
          <template #icon>
            <PlusOutlined />
          </template>
          新增员工
        </a-button>
      </div>
    </div>

    <div class="page-container__body">
      <a-card
        :bordered="false"
        class="search-card"
      >
        <a-form layout="inline">
          <a-form-item label="员工姓名">
            <a-input
              v-model:value="searchForm.employeeName"
              placeholder="请输入员工姓名"
              allow-clear
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="searchForm.status"
              placeholder="请选择状态"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="1">
                在职
              </a-select-option>
              <a-select-option :value="2">
                试用
              </a-select-option>
              <a-select-option :value="0">
                离职
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button
              type="primary"
              @click="handleSearch"
            >
              查询
            </a-button>
            <a-button
              style="margin-left: 8px"
              @click="handleReset"
            >
              重置
            </a-button>
          </a-form-item>
        </a-form>
      </a-card>

      <a-card
        :bordered="false"
        class="table-card"
      >
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
              <span>{{ EDUCATION_MAP[record.education] || '本科' }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="EMPLOYEE_STATUS_MAP[record.status]?.color">
                {{ EMPLOYEE_STATUS_MAP[record.status]?.text }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button
                  type="link"
                  size="small"
                  @click="showEditModal(record)"
                >
                  编辑
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  @click="showContracts(record)"
                >
                  合同
                </a-button>
                <a-button
                  v-if="record.status === 2"
                  type="link"
                  size="small"
                  @click="handleRegularize(record)"
                >
                  转正
                </a-button>
                <a-button
                  v-if="record.status === 1"
                  type="link"
                  size="small"
                  danger
                  @click="handleResign(record)"
                >
                  离职
                </a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingId ? '编辑员工' : '新增员工'"
      width="700px"
      @ok="handleSave"
    >
      <a-form
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="员工编号">
              <a-input
                v-model:value="form.employeeNo"
                placeholder="请输入员工编号"
                :disabled="!!editingId"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="员工姓名"
              required
            >
              <a-input
                v-model:value="form.employeeName"
                placeholder="请输入员工姓名"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="性别">
              <a-select
                v-model:value="form.gender"
                placeholder="请选择性别"
              >
                <a-select-option :value="1">
                  男
                </a-select-option>
                <a-select-option :value="2">
                  女
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="出生日期">
              <a-date-picker
                v-model:value="form.birthDate"
                placeholder="请选择出生日期"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="手机号">
              <a-input
                v-model:value="form.phone"
                placeholder="请输入手机号"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="邮箱">
              <a-input
                v-model:value="form.email"
                placeholder="请输入邮箱"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="身份证号">
              <a-input
                v-model:value="form.idCard"
                placeholder="请输入身份证号"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="学历">
              <a-select
                v-model:value="form.education"
                placeholder="请选择学历"
              >
                <a-select-option :value="4">
                  大专
                </a-select-option>
                <a-select-option :value="5">
                  本科
                </a-select-option>
                <a-select-option :value="6">
                  硕士
                </a-select-option>
                <a-select-option :value="7">
                  博士
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="入职日期"
              required
            >
              <a-date-picker
                v-model:value="form.hireDate"
                placeholder="请选择入职日期"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="员工类型">
              <a-select
                v-model:value="form.employeeType"
                placeholder="请选择员工类型"
              >
                <a-select-option :value="1">
                  全职
                </a-select-option>
                <a-select-option :value="2">
                  兼职
                </a-select-option>
                <a-select-option :value="3">
                  实习
                </a-select-option>
                <a-select-option :value="4">
                  外包
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-textarea
            v-model:value="form.remark"
            placeholder="请输入备注"
            :rows="2"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 合同列表弹窗 -->
    <a-modal
      v-model:open="contractsModalVisible"
      title="合同列表"
      width="720px"
      :footer="null"
      :destroy-on-close="true"
    >
      <a-table
        :data-source="contractsList"
        :loading="contractsLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <a-table-column
          title="合同编号"
          data-index="contractNo"
          :width="140"
        />
        <a-table-column
          title="合同名称"
          data-index="contractName"
        />
        <a-table-column
          title="合同类型"
          :width="100"
        >
          <template #default="{ record }">
            {{ CONTRACT_TYPE_MAP[record.contractType] || record.contractType }}
          </template>
        </a-table-column>
        <a-table-column
          title="开始日期"
          data-index="startDate"
          :width="100"
        />
        <a-table-column
          title="结束日期"
          data-index="endDate"
          :width="100"
        />
        <a-table-column
          title="约定薪资"
          data-index="salaryAmount"
          :width="100"
        >
          <template #default="{ record }">
            ¥{{ record.salaryAmount?.toFixed(2) ?? '-' }}
          </template>
        </a-table-column>
        <a-table-column
          title="状态"
          :width="70"
        >
          <template #default="{ record }">
            {{ CONTRACT_STATUS_MAP[record.status] || record.status }}
          </template>
        </a-table-column>
      </a-table>
      <div
        v-if="!contractsList.length"
        style="text-align:center;padding:24px;color:#999;"
      >
        暂无合同记录
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  hrEmployeeApi,
  hrContractApi,
  type HrEmployee,
  type HrContract,
  CONTRACT_TYPE_MAP,
  CONTRACT_STATUS_MAP,
  GENDER_MAP,
  EDUCATION_MAP,
  EMPLOYEE_STATUS_MAP
} from '@/api/hr'

const loading = ref(false)
const tableData = ref<HrEmployee[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total: number) => `共 ${total} 条` })

const searchForm = reactive({ employeeName: '', status: undefined as number | undefined })
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  employeeNo: '',
  employeeName: '',
  gender: 1,
  birthDate: null as any,
  phone: '',
  email: '',
  idCard: '',
  education: 5,
  hireDate: null as any,
  employeeType: 1,
  remark: ''
})

const columns: any[] = [
  { title: '员工编号', dataIndex: 'employeeNo', key: 'employeeNo', width: 120 },
  { title: '员工姓名', dataIndex: 'employeeName', key: 'employeeName', width: 120 },
  { title: '性别', dataIndex: 'gender', key: 'gender', width: 80 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '学历', dataIndex: 'education', key: 'education', width: 100 },
  { title: '入职日期', dataIndex: 'hireDate', key: 'hireDate', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 180, fixed: 'right' }
]

async function loadData() {
  loading.value = true
  try {
    const result = await hrEmployeeApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    tableData.value = result.records
    pagination.total = result.total
  } catch (e) {
    message.error('查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.current = 1; loadData() }
function handleReset() { searchForm.employeeName = ''; searchForm.status = undefined; handleSearch() }
function handleTableChange(p: any) { pagination.current = p.current; pagination.pageSize = p.pageSize; loadData() }

function showCreateModal() {
  editingId.value = null
  Object.assign(form, { employeeNo: '', employeeName: '', gender: 1, birthDate: null, phone: '', email: '', idCard: '', education: 5, hireDate: null, employeeType: 1, remark: '' })
  modalVisible.value = true
}

function showEditModal(record: any) {
  editingId.value = record.id
  Object.assign(form, {
    employeeNo: record.employeeNo,
    employeeName: record.employeeName,
    gender: record.gender,
    birthDate: record.birthDate,
    phone: record.phone || '',
    email: record.email || '',
    idCard: record.idCard || '',
    education: record.education,
    hireDate: record.hireDate,
    employeeType: record.employeeType,
    remark: record.remark || ''
  })
  modalVisible.value = true
}

async function handleSave() {
  if (!form.employeeName) {
    message.warning('请填写员工姓名')
    return
  }
  try {
    if (editingId.value) {
      await hrEmployeeApi.update(editingId.value, form)
      message.success('更新成功')
    } else {
      await hrEmployeeApi.create(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadData()
  } catch (e) {
    message.error('保存失败')
  }
}

async function handleRegularize(record: any) {
  Modal.confirm({
    title: '确认转正',
    content: `确定要将员工 "${record.employeeName}" 转为正式员工吗？`,
    onOk: async () => {
      await hrEmployeeApi.updateStatus(record.id, 1)
      message.success('转正成功')
      loadData()
    }
  })
}

async function handleResign(record: any) {
  Modal.confirm({
    title: '确认离职',
    content: `确定要将员工 "${record.employeeName}" 办理离职吗？`,
    okType: 'danger',
    onOk: async () => {
      await hrEmployeeApi.updateStatus(record.id, 0)
      message.success('离职成功')
      loadData()
    }
  })
}

const contractsModalVisible = ref(false)
const contractsList = ref<HrContract[]>([])
const contractsLoading = ref(false)

async function showContracts(record: any) {
  contractsLoading.value = true
  try {
    contractsList.value = await hrContractApi.listByEmployee(record.id) || []
  } catch {
    contractsList.value = []
  } finally {
    contractsLoading.value = false
  }
  contractsModalVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.page-container { height: 100%; display: flex; flex-direction: column; }
.page-header { padding: 16px 24px; background: #fff; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
.page-title { margin: 0; font-size: 18px; font-weight: 600; }
.page-container__body { flex: 1; padding: 16px; overflow: auto; }
.search-card { margin-bottom: 16px; }
.table-card { background: #fff; }
</style>