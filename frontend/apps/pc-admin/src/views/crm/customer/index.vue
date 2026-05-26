<template>
  <div class="customer-management">
    <!-- 搜索区域 -->
    <a-card
      class="search-card"
      :bordered="false"
    >
      <a-form
        layout="inline"
        :model="searchForm"
      >
        <a-row
          :gutter="16"
          style="width: 100%"
        >
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="客户名称">
              <a-input
                v-model:value="searchForm.name"
                placeholder="请输入客户名称"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="客户等级">
              <a-select
                v-model:value="searchForm.level"
                placeholder="请选择等级"
                allow-clear
                style="width: 100%"
              >
                <a-select-option :value="1">
                  VIP客户
                </a-select-option>
                <a-select-option :value="2">
                  重要客户
                </a-select-option>
                <a-select-option :value="3">
                  普通客户
                </a-select-option>
                <a-select-option :value="4">
                  潜在客户
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="状态">
              <a-select
                v-model:value="searchForm.status"
                placeholder="请选择状态"
                allow-clear
                style="width: 100%"
              >
                <a-select-option :value="0">
                  正常
                </a-select-option>
                <a-select-option :value="1">
                  停用
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item>
              <a-space>
                <a-button
                  type="primary"
                  @click="handleSearch"
                >
                  搜索
                </a-button>
                <a-button @click="handleReset">
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card
      class="table-card"
      :bordered="false"
    >
      <template #title>
        <div class="table-header">
          <span>客户列表</span>
          <a-space>
            <a-button
              type="primary"
              @click="handleAdd"
            >
              新增客户
            </a-button>
            <a-button @click="handleExport">
              导出
            </a-button>
          </a-space>
        </div>
      </template>

      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-space>
              <a-avatar :style="{ backgroundColor: getLevelColor(record.level) }">
                {{ record.name?.charAt(0) }}
              </a-avatar>
              <div>
                <div class="customer-name">
                  {{ record.name }}
                </div>
                <div class="customer-code">
                  {{ record.code }}
                </div>
              </div>
            </a-space>
          </template>
          
          <template v-else-if="column.key === 'level'">
            <a-tag :color="getLevelColor(record.level)">
              {{ getLevelName(record.level) }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 0 ? 'success' : 'error'">
              {{ record.status === 0 ? '正常' : '停用' }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="handleView(record)"
              >
                查看
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                type="link"
                size="small"
                @click="handleFollow(record)"
              >
                跟进
              </a-button>
              <a-dropdown>
                <a-button
                  type="link"
                  size="small"
                >
                  更多
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleViewFollows(record)">
                      跟进记录
                    </a-menu-item>
                    <a-menu-item @click="handleViewOrders(record)">
                      订单记录
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item
                      danger
                      @click="handleDelete(record)"
                    >
                      删除
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 客户表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      width="700px"
      @ok="handleModalOk"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item
              label="客户名称"
              name="name"
            >
              <a-input
                v-model:value="formState.name"
                placeholder="请输入客户名称"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="客户编码"
              name="code"
            >
              <a-input
                v-model:value="formState.code"
                placeholder="请输入客户编码"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="联系人"
              name="contactPerson"
            >
              <a-input
                v-model:value="formState.contactPerson"
                placeholder="请输入联系人"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="联系电话"
              name="phone"
            >
              <a-input
                v-model:value="formState.phone"
                placeholder="请输入联系电话"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="邮箱"
              name="email"
            >
              <a-input
                v-model:value="formState.email"
                placeholder="请输入邮箱"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="客户等级"
              name="level"
            >
              <a-select
                v-model:value="formState.level"
                placeholder="请选择等级"
              >
                <a-select-option :value="1">
                  VIP客户
                </a-select-option>
                <a-select-option :value="2">
                  重要客户
                </a-select-option>
                <a-select-option :value="3">
                  普通客户
                </a-select-option>
                <a-select-option :value="4">
                  潜在客户
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="行业"
              name="industry"
            >
              <a-select
                v-model:value="formState.industry"
                placeholder="请选择行业"
              >
                <a-select-option value="IT">
                  IT/互联网
                </a-select-option>
                <a-select-option value="制造业">
                  制造业
                </a-select-option>
                <a-select-option value="金融">
                  金融
                </a-select-option>
                <a-select-option value="零售">
                  零售
                </a-select-option>
                <a-select-option value="其他">
                  其他
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item
              label="状态"
              name="status"
            >
              <a-radio-group v-model:value="formState.status">
                <a-radio :value="0">
                  正常
                </a-radio>
                <a-radio :value="1">
                  停用
                </a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="地址"
              name="address"
              :label-col="{ span: 3 }"
              :wrapper-col="{ span: 20 }"
            >
              <a-input
                v-model:value="formState.address"
                placeholder="请输入地址"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item
              label="备注"
              name="description"
              :label-col="{ span: 3 }"
              :wrapper-col="{ span: 20 }"
            >
              <a-textarea
                v-model:value="formState.description"
                placeholder="请输入备注"
                :rows="3"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <!-- 跟进记录弹窗 -->
    <a-modal
      v-model:open="followModalVisible"
      title="添加跟进记录"
      :confirm-loading="followModalLoading"
      width="600px"
      @ok="handleFollowModalOk"
    >
      <a-form
        :model="followForm"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="跟进类型"
          required
        >
          <a-select
            v-model:value="followForm.followType"
            placeholder="请选择跟进类型"
          >
            <a-select-option :value="1">
              电话
            </a-select-option>
            <a-select-option :value="2">
              拜访
            </a-select-option>
            <a-select-option :value="3">
              邮件
            </a-select-option>
            <a-select-option :value="4">
              微信
            </a-select-option>
            <a-select-option :value="5">
              其他
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          label="跟进内容"
          required
        >
          <a-textarea
            v-model:value="followForm.content"
            placeholder="请输入跟进内容"
            :rows="4"
          />
        </a-form-item>
        <a-form-item label="跟进结果">
          <a-select
            v-model:value="followForm.result"
            placeholder="请选择跟进结果"
          >
            <a-select-option :value="1">
              有意向
            </a-select-option>
            <a-select-option :value="2">
              无意向
            </a-select-option>
            <a-select-option :value="3">
              待跟进
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="下次跟进时间">
          <a-date-picker
            v-model:value="followForm.nextFollowTime"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'
import { customerApi, type CustomerInfo } from '@/api/customer'

// 搜索表单
const searchForm = reactive({
  name: '',
  level: undefined as number | undefined,
  status: undefined as number | undefined
})

// 表格数据
const tableData = ref<CustomerInfo[]>([])
const loading = ref(false)

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 表格列定义
const columns: TableProps['columns'] = [
  { title: '客户信息', key: 'name', width: 200 },
  { title: '联系人', dataIndex: 'contactPerson', width: 100 },
  { title: '联系电话', dataIndex: 'phone', width: 120 },
  { title: '客户等级', key: 'level', width: 100 },
  { title: '行业', dataIndex: 'industry', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

// 弹窗相关
const modalVisible = ref(false)
const modalLoading = ref(false)
const modalTitle = computed(() => isEdit.value ? '编辑客户' : '新增客户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({
  id: 0,
  name: '',
  code: '',
  contactPerson: '',
  phone: '',
  email: '',
  address: '',
  level: 3,
  industry: '',
  status: 0,
  description: ''
})

const formRules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

// 跟进相关
const followModalVisible = ref(false)
const followModalLoading = ref(false)
const currentCustomerId = ref(0)
const followForm = reactive({
  followType: 1,
  content: '',
  result: 3,
  nextFollowTime: null as any
})

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const res = await customerApi.getPage({
      tenantId: 1,
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索相关
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, { name: '', level: undefined, status: undefined })
  handleSearch()
}

// 表格操作
const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  fetchData()
}

// 新增客户
const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, {
    id: 0, name: '', code: '', contactPerson: '', phone: '', email: '', address: '', level: 3, industry: '', status: 0, description: ''
  })
  modalVisible.value = true
}

// 编辑客户
const handleEdit = (record: CustomerInfo) => {
  isEdit.value = true
  Object.assign(formState, record)
  modalVisible.value = true
}

// 查看客户
const handleView = (record: CustomerInfo) => {
  message.info(`查看客户: ${record.name}`)
  // TODO: 打开客户详情
}

// 提交表单
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true
    
    if (isEdit.value) {
      await customerApi.update(formState.id, formState)
      message.success('更新成功')
    } else {
      await customerApi.create(formState)
      message.success('创建成功')
    }
    
    modalVisible.value = false
    fetchData()
  } catch (error) {
    message.error('操作失败')
  } finally {
    modalLoading.value = false
  }
}

// 删除客户
const handleDelete = (record: CustomerInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除客户"${record.name}"吗？`,
    async onOk() {
      try {
        await customerApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error) {
        message.error('删除失败')
      }
    }
  })
}

// 添加跟进
const handleFollow = (record: CustomerInfo) => {
  currentCustomerId.value = record.id
  Object.assign(followForm, { followType: 1, content: '', result: 3, nextFollowTime: null })
  followModalVisible.value = true
}

// 提交跟进
const handleFollowModalOk = async () => {
  if (!followForm.content) {
    message.error('请输入跟进内容')
    return
  }
  followModalLoading.value = true
  try {
    await customerApi.addFollowRecord(currentCustomerId.value, {
      customerId: currentCustomerId.value,
      followType: followForm.followType,
      content: followForm.content,
      result: followForm.result,
      nextFollowTime: followForm.nextFollowTime
    })
    message.success('跟进记录添加成功')
    followModalVisible.value = false
  } catch (error) {
    message.error('添加失败')
  } finally {
    followModalLoading.value = false
  }
}

// 查看跟进记录
const handleViewFollows = (record: CustomerInfo) => {
  message.info(`查看跟进记录: ${record.name}`)
  // TODO: 打开跟进记录列表
}

// 查看订单记录
const handleViewOrders = (record: CustomerInfo) => {
  message.info(`查看订单记录: ${record.name}`)
  // TODO: 打开订单记录列表
}

// 导出
const handleExport = () => {
  message.info('导出功能开发中')
  // TODO: 实现导出功能
}

// 辅助方法
const getLevelColor = (level: number) => {
  const colors: Record<number, string> = { 1: '#ff4d4f', 2: '#faad14', 3: '#1890ff', 4: '#52c41a' }
  return colors[level] || '#999'
}

const getLevelName = (level: number) => {
  const names: Record<number, string> = { 1: 'VIP客户', 2: '重要客户', 3: '普通客户', 4: '潜在客户' }
  return names[level] || '未知'
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.customer-management {
  padding: 0;
}

.search-card {
  margin-bottom: 16px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.customer-name {
  font-weight: 500;
}

.customer-code {
  font-size: 12px;
  color: #999;
}
</style>