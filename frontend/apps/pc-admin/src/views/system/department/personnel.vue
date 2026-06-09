<template>
  <div class="department-personnel">
    <!-- 缁熻鍗＄墖 -->
    <div class="stat-cards">
      <div class="stat-card stat-total">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ pagination.total }}</div>
          <div class="stat-card-label">浜哄憳鎬绘暟</div>
        </div>
        <TeamOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-active">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ activeCount }}</div>
          <div class="stat-card-label">姝ｅ父浜哄憳</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-disabled">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ disabledCount }}</div>
          <div class="stat-card-label">鍋滅敤浜哄憳</div>
        </div>
        <StopOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-dept">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ currentDepartment?.departmentName || '-' }}</div>
          <div class="stat-card-label">褰撳墠閮ㄩ棬</div>
        </div>
        <ApartmentOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :show-search="false"
      :show-add="false"
      :show-edit="false"
      :show-delete="false"
      :show-batch-delete="false"
      :selectable="true"
      add-text="娣诲姞浜哄憳"
      @refresh="fetchData"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="(keys: any) => { selectedRowKeys.value = keys as number[] }"
    >
      <template #toolbar-actions>
        <a-space>
          <a-tag v-if="currentDepartment" color="blue">
            {{ currentDepartment.departmentName }}
          </a-tag>
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            娣诲姞浜哄憳
          </a-button>
          <a-button @click="handleTransfer">
            <template #icon><SwapOutlined /></template>
            浜哄憳璋冨姩
          </a-button>
          <a-button @click="handleBack">
            <template #icon><ArrowLeftOutlined /></template>
            杩斿洖
          </a-button>
        </a-space>
      </template>

      <template #userInfoCell="{ record }">
        <a-space>
          <a-avatar
            :src="record.avatar"
            :size="32"
          >
            {{ record.nickname?.charAt(0) || record.username?.charAt(0) }}
          </a-avatar>
          <div>
            <div class="user-name">
              {{ record.username }}
            </div>
            <div class="user-nickname">
              {{ record.nickname }}
            </div>
          </div>
        </a-space>
      </template>
      <template #statusCell="{ record }">
        <a-tag :color="record.status === 0 ? 'success' : 'error'">
          {{ record.status === 0 ? '姝ｅ父' : '鍋滅敤' }}
        </a-tag>
      </template>
      <template #positionCell="{ record }">
        {{ record.positionName || '-' }}
      </template>
      <template #action="{ record }">
        <a-space>
          <a-button
            type="link"
            size="small"
            @click="handleTransferSingle(record as UserInfo)"
          >
            璋冨姩
          </a-button>
          <a-button
            type="link"
            size="small"
            danger
            @click="handleRemove(record as UserInfo)"
          >
            绉婚櫎
          </a-button>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 娣诲姞浜哄憳寮圭獥 -->
    <a-modal
      v-model:open="addModalVisible"
      title="娣诲姞閮ㄩ棬浜哄憳"
      :confirm-loading="addModalLoading"
      width="600px"
      @ok="handleAddModalOk"
      @cancel="handleAddModalCancel"
    >
      <a-form
        ref="addFormRef"
        :model="addFormState"
        :rules="addFormRules as any"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="閫夋嫨浜哄憳"
          name="userIds"
        >
          <a-select
            v-model:value="addFormState.userIds"
            mode="multiple"
            placeholder="璇烽€夋嫨瑕佹坊鍔犵殑浜哄憳"
            show-search
            :filter-option="filterUserOption"
          >
            <a-select-option
              v-for="user in availableUsers"
              :key="user.id"
              :value="user.id"
            >
              {{ user.nickname || user.username }} - {{ user.phone || '-' }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          label="宀椾綅"
          name="positionId"
        >
          <a-select
            v-model:value="addFormState.positionId"
            placeholder="璇烽€夋嫨宀椾綅"
            allow-clear
          >
            <a-select-option
              v-for="pos in positionList"
              :key="pos.id"
              :value="pos.id"
            >
              {{ pos.positionName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 浜哄憳璋冨姩寮圭獥 -->
    <a-modal
      v-model:open="transferModalVisible"
      title="浜哄憳璋冨姩"
      :confirm-loading="transferModalLoading"
      width="600px"
      @ok="handleTransferModalOk"
      @cancel="handleTransferModalCancel"
    >
      <div class="transfer-modal-content">
        <a-form
          ref="transferFormRef"
          :model="transferFormState"
          :rules="transferFormRules as any"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="鐩爣閮ㄩ棬"
            name="targetDepartmentId"
          >
            <a-tree-select
              v-model:value="transferFormState.targetDepartmentId"
              :tree-data="departmentTreeData"
              :field-names="{ children: 'children', label: 'departmentName', value: 'id' }"
              placeholder="璇烽€夋嫨鐩爣閮ㄩ棬"
              allow-clear
              show-search
              tree-node-filter-prop="departmentName"
            />
          </a-form-item>
          <a-form-item label="璋冨矖浜哄憳">
            <a-list
              size="small"
              :data-source="selectedUsers"
              :grid="{ gutter: 16, column: 2 }"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #title>
                      {{ item.nickname || item.username }}
                    </template>
                    <template #description>
                      {{ item.positionName || '-' }}
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
          </a-form-item>
          <a-form-item
            label="鏂板矖浣�"
            name="newPositionId"
          >
            <a-select
              v-model:value="transferFormState.newPositionId"
              placeholder="璇烽€夋嫨鏂板矖浣嶏紙鍙€夛級"
              allow-clear
            >
              <a-select-option
                v-for="pos in positionList"
                :key="pos.id"
                :value="pos.id"
              >
                {{ pos.positionName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- 鍗曚汉璋冨姩寮圭獥 -->
    <a-modal
      v-model:open="singleTransferModalVisible"
      title="浜哄憳璋冨姩"
      :confirm-loading="singleTransferModalLoading"
      width="600px"
      @ok="handleSingleTransferModalOk"
      @cancel="handleSingleTransferModalCancel"
    >
      <a-form
        ref="singleTransferFormRef"
        :model="singleTransferFormState"
        :rules="singleTransferFormRules as any"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="褰撳墠浜哄憳">
          <a-input
            :value="selectedUser?.nickname || selectedUser?.username"
            disabled
          />
        </a-form-item>
        <a-form-item
          label="鐩爣閮ㄩ棬"
          name="targetDepartmentId"
        >
          <a-tree-select
            v-model:value="singleTransferFormState.targetDepartmentId"
            :tree-data="departmentTreeData"
            :field-names="{ children: 'children', label: 'departmentName', value: 'id' }"
            placeholder="璇烽€夋嫨鐩爣閮ㄩ棬"
            allow-clear
            show-search
            tree-node-filter-prop="departmentName"
          />
        </a-form-item>
        <a-form-item
          label="鏂板矖浣�"
          name="newPositionId"
        >
          <a-select
            v-model:value="singleTransferFormState.newPositionId"
            placeholder="璇烽€夋嫨鏂板矖浣嶏紙鍙€夛級"
            allow-clear
          >
            <a-select-option
              v-for="pos in positionList"
              :key="pos.id"
              :value="pos.id"
            >
              {{ pos.positionName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined,
  SwapOutlined,
  ArrowLeftOutlined,
  TeamOutlined,
  CheckCircleOutlined,
  StopOutlined,
  ApartmentOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import request from '@/utils/request'
import { userApi, type UserInfo } from '@/api/user'
import { departmentApi, type DepartmentInfo } from '@/api/department'
import { positionApi, type PositionInfo } from '@/api/position'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

// 褰撳墠閮ㄩ棬
const currentDepartment = ref<DepartmentInfo | null>(null)

// 鎼滅储琛ㄥ崟
const searchForm = reactive({
  username: '',
  phone: ''
})

// 琛ㄦ牸鏁版嵁
const tableData = ref<UserInfo[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])

// ── 统计数据 ────────────────────────────────────────────
const activeCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const disabledCount = computed(() => tableData.value.filter(r => r.status === 1).length)

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

// 鍒嗛〉閰嶇疆
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 琛ㄦ牸鍒楀畾涔�
const vxeColumns: any[] = [
  { title: '鐢ㄦ埛淇℃伅', key: 'userInfo', width: 200, slotName: 'userInfoCell' },
  { title: '鎵嬫満鍙�', dataIndex: 'phone', width: 120 },
  { title: '閭', dataIndex: 'email', width: 180, ellipsis: true },
  { title: '宀椾綅', key: 'position', width: 150, slotName: 'positionCell' },
  { title: '鐘舵€�', key: 'status', width: 80, slotName: 'statusCell' },
  { title: '鍒涘缓鏃堕棿', dataIndex: 'createTime', width: 160 },
  { type: 'action', title: '鎿嶄綔', width: 150, fixed: 'right' }
]

// 绛涢€夊瓧娈�
const filterFields: FilterField[] = [
  { key: 'username', label: '鐢ㄦ埛鍚�', type: 'input', placeholder: '璇疯緭鍏ョ敤鎴峰悕' },
  { key: 'phone', label: '鎵嬫満鍙�', type: 'input', placeholder: '璇疯緭鍏ユ墜鏈哄彿' },
]

// 娣诲姞浜哄憳寮圭獥
const addModalVisible = ref(false)
const addModalLoading = ref(false)
const addFormRef = ref<FormInstance>()

const addFormState = reactive({
  userIds: [] as number[],
  positionId: undefined as number | undefined
})

const addFormRules = {
  userIds: [{ required: true, message: '璇烽€夋嫨浜哄憳', trigger: 'change' }]
}

// 鍙敤鐢ㄦ埛鍒楄〃锛堜笉鍦ㄥ綋鍓嶉儴闂ㄧ殑鐢ㄦ埛锛�
const availableUsers = ref<UserInfo[]>([])

// 宀椾綅鍒楄〃
const positionList = ref<PositionInfo[]>([])

// 鎵归噺璋冨姩寮圭獥
const transferModalVisible = ref(false)
const transferModalLoading = ref(false)
const transferFormRef = ref<FormInstance>()

const transferFormState = reactive({
  targetDepartmentId: undefined as number | undefined,
  newPositionId: undefined as number | undefined
})

const transferFormRules = {
  targetDepartmentId: [{ required: true, message: '璇烽€夋嫨鐩爣閮ㄩ棬', trigger: 'change' }]
}

// 鍗曚汉璋冨姩寮圭獥
const singleTransferModalVisible = ref(false)
const singleTransferModalLoading = ref(false)
const singleTransferFormRef = ref<FormInstance>()
const selectedUser = ref<UserInfo | null>(null)

const singleTransferFormState = reactive({
  targetDepartmentId: undefined as number | undefined,
  newPositionId: undefined as number | undefined
})

const singleTransferFormRules = {
  targetDepartmentId: [{ required: true, message: '璇烽€夋嫨鐩爣閮ㄩ棬', trigger: 'change' }]
}

// 閮ㄩ棬鏍戞暟鎹�
const departmentTreeData = ref<DepartmentInfo[]>([])

// 鏁版嵁鍔犺浇
const fetchData = async () => {
  loading.value = true
  try {
    const res = await userApi.getPage({
      tenantId: userStore.tenantId,
      deptId: currentDepartment.value?.id,
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    message.error('鍔犺浇鏁版嵁澶辫触')
  } finally {
    loading.value = false
  }
}

// 鍔犺浇鍙敤鐢ㄦ埛鍒楄〃
const fetchAvailableUsers = async () => {
  try {
    const res = await userApi.getList({
      tenantId: userStore.tenantId,
      status: 0,
      pageSize: 1000
    })
    if (res.data) {
      // 杩囨护鎺夊凡鍦ㄥ綋鍓嶉儴闂ㄧ殑鐢ㄦ埛
      const currentDeptUserIds = tableData.value.map(u => u.id)
      availableUsers.value = res.data.filter(u => !currentDeptUserIds.includes(u.id))
    }
  } catch (error) {
    console.warn('[系统管理] 加载可用用户失败', error)
  }
}

// 鍔犺浇宀椾綅鍒楄〃
const fetchPositionList = async () => {
  try {
    const res = await positionApi.getList({ tenantId: userStore.tenantId, status: 0 })
    if (res.data) {
      positionList.value = res.data
    }
  } catch (error) {
    console.warn('[系统管理] 加载岗位列表失败', error)
  }
}

// 鍔犺浇閮ㄩ棬鏍�
const fetchDepartmentTree = async () => {
  try {
    const res = await departmentApi.getTree({ tenantId: userStore.tenantId, status: 0 })
    if (res.data) {
      departmentTreeData.value = res.data
    }
  } catch (error) {
    console.warn('[系统管理] 加载部门树失败', error)
  }
}

// 鎼滅储
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, { username: '', phone: '' })
  handleSearch()
}

// 绛涢€夊彉鍖�
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { username: '', phone: '' })
  } else {
    Object.assign(searchForm, filters)
  }
  pagination.current = 1
  fetchData()
}

// 鍒嗛〉鍙樺寲
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// 鑾峰彇閫変腑鐢ㄦ埛
const selectedUsers = computed(() => {
  return tableData.value.filter(u => selectedRowKeys.value.includes(u.id))
})

// 娣诲姞浜哄憳
const handleAdd = () => {
  addFormState.userIds = []
  addFormState.positionId = undefined
  fetchAvailableUsers()
  addModalVisible.value = true
}

const handleAddModalOk = async () => {
  try {
    await addFormRef.value?.validate()
    addModalLoading.value = true

    await request.post('/department/personnel/add', {
      deptId: currentDepartment.value?.id,
      userIds: addFormState.userIds,
      positionId: addFormState.positionId
    })
    message.success('娣诲姞鎴愬姛')
    addModalVisible.value = false
    fetchData()
  } catch (error) {
    message.error('鎿嶄綔澶辫触')
  } finally {
    addModalLoading.value = false
  }
}

const handleAddModalCancel = () => {
  addModalVisible.value = false
  addFormRef.value?.resetFields()
}

// 鎵归噺璋冨姩
const handleTransfer = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('璇峰厛閫夋嫨瑕佽皟鍔ㄧ殑浜哄憳')
    return
  }
  transferFormState.targetDepartmentId = undefined
  transferFormState.newPositionId = undefined
  transferModalVisible.value = true
}

const handleTransferModalOk = async () => {
  try {
    await transferFormRef.value?.validate()
    transferModalLoading.value = true

    await request.post('/department/personnel/transfer', {
      deptId: currentDepartment.value?.id,
      targetDeptId: transferFormState.targetDepartmentId,
      userIds: selectedRowKeys.value,
      positionId: transferFormState.newPositionId
    })
    message.success('璋冨姩鎴愬姛')
    transferModalVisible.value = false
    selectedRowKeys.value = []
    fetchData()
  } catch (error) {
    message.error('鎿嶄綔澶辫触')
  } finally {
    transferModalLoading.value = false
  }
}

const handleTransferModalCancel = () => {
  transferModalVisible.value = false
  transferFormRef.value?.resetFields()
}

// 鍗曚汉璋冨姩
const handleTransferSingle = (user: UserInfo) => {
  selectedUser.value = user
  singleTransferFormState.targetDepartmentId = undefined
  singleTransferFormState.newPositionId = undefined
  singleTransferModalVisible.value = true
}

const handleSingleTransferModalOk = async () => {
  if (!selectedUser.value) return

  try {
    await singleTransferFormRef.value?.validate()
    singleTransferModalLoading.value = true

    await request.post('/department/personnel/transfer', {
      deptId: currentDepartment.value?.id,
      targetDeptId: singleTransferFormState.targetDepartmentId,
      userIds: [selectedUser.value!.id],
      positionId: singleTransferFormState.newPositionId
    })
    message.success('璋冨姩鎴愬姛')
    singleTransferModalVisible.value = false
    fetchData()
  } catch (error) {
    message.error('鎿嶄綔澶辫触')
  } finally {
    singleTransferModalLoading.value = false
  }
}

const handleSingleTransferModalCancel = () => {
  singleTransferModalVisible.value = false
  singleTransferFormRef.value?.resetFields()
}

// 绉婚櫎浜哄憳
const handleRemove = (user: UserInfo) => {
  Modal.confirm({
    title: '确认移除',
    content: `确定要将用户 "${user.nickname || user.username}" 从当前部门移除吗？`,
    async onOk() {
      await request.delete('/department/personnel/remove', {
        params: { deptId: currentDepartment.value?.id, userId: user.id }
      })
      message.success('移除成功')
      fetchData()
    }
  })
}

// 杩斿洖
const handleBack = () => {
  router.push('/system/department')
}

// 杩囨护鐢ㄦ埛
const filterUserOption = (input: string, option: any) => {
  const user = availableUsers.value.find(u => u.id === option.value)
  if (!user) return false
  const name = user.nickname || user.username || ''
  return name.toLowerCase().includes(input.toLowerCase())
}

onMounted(() => {
  // 浠庤矾鐢卞弬鏁拌幏鍙栭儴闂↖D
  const deptId = route.query.deptId as string
  if (deptId) {
    departmentApi.getById(Number(deptId)).then(res => {
      if (res.data) {
        currentDepartment.value = res.data
        fetchData()
      }
    })
  }
  fetchPositionList()
  fetchDepartmentTree()
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.department-personnel {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-active { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-disabled { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-dept { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}


.user-name {
  font-weight: 500;
}

.user-nickname {
  font-size: 12px;
  color: #999;
}

.transfer-modal-content {
  padding: 16px 0;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
