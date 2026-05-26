<template>
  <div class="user-page">
    <van-nav-bar title="用户管理">
      <template #right>
        <van-icon name="plus" size="18" @click="goAdd" />
      </template>
    </van-nav-bar>

    <van-search
      v-model="searchText"
      placeholder="搜索用户名/姓名/手机号"
      show-action
      @search="onSearch"
      @cancel="onCancel"
    />

    <van-dropdown-menu>
      <van-dropdown-item v-model="filter.role" :options="roleOptions" title="角色" />
      <van-dropdown-item v-model="filter.status" :options="statusOptions" title="状态" />
      <van-dropdown-item v-model="filter.department" :options="departmentOptions" title="部门" />
    </van-dropdown-menu>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <van-list
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="onLoad"
      >
        <van-cell-group inset>
          <van-cell
            v-for="user in userList"
            :key="user.id"
            is-link
            @click="goDetail(user)"
          >
            <template #icon>
              <van-image
                round
                width="40"
                height="40"
                :src="user.avatar || defaultAvatar"
              />
            </template>
            <template #title>
              <div class="user-title">
                <span class="user-name">{{ user.name }}</span>
                <van-tag :type="getStatusType(user.status)" size="small">
                  {{ user.statusLabel }}
                </van-tag>
              </div>
            </template>
            <template #label>
              <div class="user-info">
                <div class="info-row">
                  <van-icon name="user-o" />
                  <span>{{ user.username }}</span>
                </div>
                <div class="info-row">
                  <van-icon name="phone-o" />
                  <span>{{ user.phone }}</span>
                </div>
              </div>
            </template>
            <template #value>
              <div class="user-role">{{ user.roleName }}</div>
            </template>
          </van-cell>
        </van-cell-group>
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
const searchText = ref('')
const refreshing = ref(false)
const loading = ref(false)
const finished = ref(false)

const filter = reactive({
  role: 0,
  status: 0,
  department: 0
})

const roleOptions = [
  { text: '全部角色', value: 0 },
  { text: '管理员', value: 1 },
  { text: '销售', value: 2 },
  { text: '采购', value: 3 },
  { text: '财务', value: 4 }
]

const statusOptions = [
  { text: '全部状态', value: 0 },
  { text: '在职', value: 1 },
  { text: '离职', value: 2 },
  { text: '禁用', value: 3 }
]

const departmentOptions = [
  { text: '全部部门', value: 0 },
  { text: '销售部', value: 1 },
  { text: '采购部', value: 2 },
  { text: '财务部', value: 3 },
  { text: '行政部', value: 4 }
]

const userList = ref<any[]>([])

onMounted(() => {
  loadData()
})

const loadData = () => {
  userList.value = [
    { id: 1, name: '张三', username: 'zhangsan', phone: '138****1234', role: 1, roleName: '管理员', status: 1, statusLabel: '在职', department: '销售部', avatar: '' },
    { id: 2, name: '李四', username: 'lisi', phone: '139****5678', role: 2, roleName: '销售', status: 1, statusLabel: '在职', department: '销售部', avatar: '' },
    { id: 3, name: '王五', username: 'wangwu', phone: '137****9012', role: 3, roleName: '采购', status: 1, statusLabel: '在职', department: '采购部', avatar: '' },
    { id: 4, name: '赵六', username: 'zhaoliu', phone: '136****3456', role: 4, roleName: '财务', status: 1, statusLabel: '在职', department: '财务部', avatar: '' },
    { id: 5, name: '钱七', username: 'qianqi', phone: '135****7890', role: 2, roleName: '销售', status: 2, statusLabel: '离职', department: '销售部', avatar: '' }
  ]
}

const onRefresh = async () => {
  await loadData()
  refreshing.value = false
}

const onLoad = () => {
  loading.value = false
  finished.value = true
}

const onSearch = () => {
  loadData()
}

const onCancel = () => {
  searchText.value = ''
  loadData()
}

const getStatusType = (status: number) => {
  const types: Record<number, string> = {
    1: 'success',
    2: 'default',
    3: 'danger'
  }
  return types[status] || 'default'
}

const goDetail = (user: any) => {
  router.push(`/user/${user.id}`)
}

const goAdd = () => {
  router.push('/user/add')
}
</script>

<style scoped lang="scss">
.user-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.user-title {
  display: flex;
  align-items: center;
  gap: 8px;

  .user-name {
    font-size: 15px;
    font-weight: 500;
  }
}

.user-info {
  margin-top: 4px;

  .info-row {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #999;
    margin-top: 2px;
  }
}

.user-role {
  font-size: 12px;
  color: #666;
}
</style>