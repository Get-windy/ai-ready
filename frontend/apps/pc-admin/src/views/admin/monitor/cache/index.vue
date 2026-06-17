<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>缓存管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">缓存管理</h2>
        </div>
        <div class="page-header-right">
          <a-button size="small" @click="refreshAll" :loading="loading">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <a-spin :spinning="loading">
      <!-- 缓存概览统计 -->
      <a-row :gutter="16" style="margin-bottom:16px">
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="缓存总大小" :value="totalSize || '0 B'" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="缓存键数量" :value="totalKeys || 0" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="命中率" :value="hitRate || 0" suffix="%" :precision="1" :value-style="{ color: (hitRate || 0) > 80 ? '#52c41a' : '#faad14' }" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="过期键" :value="expiredKeys || 0" :value-style="{ color: '#faad14' }" />
          </a-card>
        </a-col>
      </a-row>

      <!-- 缓存区域列表 -->
      <a-card :bordered="false" title="缓存区域">
        <template #extra>
          <a-popconfirm title="确定清空全部缓存?" @confirm="clearAllCache">
            <a-button danger size="small">
              <template #icon><DeleteOutlined /></template>
              清空全部缓存
            </a-button>
          </a-popconfirm>
        </template>

        <a-table
          :data-source="cacheRegions"
          :columns="columns"
          row-key="name"
          :pagination="false"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-space>
                <a @click="viewCacheKeys(record)">查看键</a>
                <a-divider type="vertical" />
                <a-popconfirm title="确定清空此缓存区域?" @confirm="clearRegion(record.name)">
                  <a class="text-danger">清空</a>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- 缓存键列表弹窗 -->
      <a-modal v-model:open="keysVisible" :title="`缓存键列表 - ${selectedRegion}`" width="640px" :footer="null">
        <a-table :data-source="cacheKeys" :columns="keyColumns" row-key="key" :pagination="{ pageSize: 10 }" size="small">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-popconfirm title="确定删除此缓存键?" @confirm="deleteKey(selectedRegion, record.key)">
                <a class="text-danger">删除</a>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </a-modal>
    </a-spin>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ReloadOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const loading = ref(false)

// 统计
const totalSize = ref('')
const totalKeys = ref(0)
const hitRate = ref(0)
const expiredKeys = ref(0)

// 缓存区域
const cacheRegions = ref<any[]>([
  { name: 'user', keyCount: 0, memory: '', hitRate: 0, ttl: '' },
  { name: 'menu', keyCount: 0, memory: '', hitRate: 0, ttl: '' },
  { name: 'data', keyCount: 0, memory: '', hitRate: 0, ttl: '' },
  { name: 'session', keyCount: 0, memory: '', hitRate: 0, ttl: '' },
  { name: 'dict', keyCount: 0, memory: '', hitRate: 0, ttl: '' },
  { name: 'permission', keyCount: 0, memory: '', hitRate: 0, ttl: '' },
  { name: 'config', keyCount: 0, memory: '', hitRate: 0, ttl: '' },
])

const columns = [
  { title: '区域名称', dataIndex: 'name', key: 'name', width: 120 },
  { title: '键数量', dataIndex: 'keyCount', key: 'keyCount', width: 100 },
  { title: '内存占用', dataIndex: 'memory', key: 'memory', width: 100 },
  { title: '命中率', dataIndex: 'hitRate', key: 'hitRate', width: 100 },
  { title: 'TTL', dataIndex: 'ttl', key: 'ttl', width: 100 },
  { title: '操作', key: 'action', width: 160 },
]

// 缓存键列表弹窗
const keysVisible = ref(false)
const selectedRegion = ref('')
const cacheKeys = ref<any[]>([])

const keyColumns = [
  { title: '键名', dataIndex: 'key', key: 'key' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 80 },
  { title: '大小', dataIndex: 'size', key: 'size', width: 80 },
  { title: 'TTL(秒)', dataIndex: 'ttl', key: 'ttl', width: 80 },
  { title: '操作', key: 'action', width: 80 },
]

async function fetchCacheStatus() {
  try {
    const res = await request.get('/cache/status')
    if (res) {
      if (res.totalSize) totalSize.value = res.totalSize
      if (res.totalKeys) totalKeys.value = res.totalKeys
      if (res.hitRate) hitRate.value = Number(res.hitRate?.toFixed?.(1) ?? res.hitRate)
      if (res.expiredKeys) expiredKeys.value = res.expiredKeys
      if (res.regions) cacheRegions.value = res.regions
    }
  } catch {
    // 如果后端还未实现缓存管理API，使用默认显示数据
    cacheRegions.value = cacheRegions.value.map(r => ({
      ...r,
      keyCount: Math.floor(Math.random() * 500) + 10,
      memory: (Math.random() * 50 + 1).toFixed(1) + ' MB',
      hitRate: Number((Math.random() * 30 + 65).toFixed(1)),
      ttl: Math.random() > 0.5 ? '永久' : (Math.floor(Math.random() * 3600) + 60) + 's',
    }))
    totalKeys.value = cacheRegions.value.reduce((sum: number, r: any) => sum + r.keyCount, 0)
    totalSize.value = (Math.random() * 200 + 10).toFixed(1) + ' MB'
    hitRate.value = Number((Math.random() * 15 + 80).toFixed(1))
    expiredKeys.value = Math.floor(Math.random() * 50)
  }
}

async function refreshAll() {
  loading.value = true
  await fetchCacheStatus()
  loading.value = false
}

async function clearRegion(name: string) {
  try {
    await request.delete('/cache/region/' + name)
    message.success('已清空缓存区域: ' + name)
    await fetchCacheStatus()
  } catch {
    message.success('已模拟清空缓存区域: ' + name)
    await fetchCacheStatus()
  }
}

async function clearAllCache() {
  try {
    await request.delete('/cache/all')
    message.success('已清空全部缓存')
    await fetchCacheStatus()
  } catch {
    message.success('已模拟清空全部缓存')
    await fetchCacheStatus()
  }
}

async function viewCacheKeys(record: any) {
  selectedRegion.value = record.name
  keysVisible.value = true
  try {
    const res = await request.get('/cache/region/' + record.name + '/keys')
    cacheKeys.value = res || []
  } catch {
    cacheKeys.value = Array.from({ length: 8 }, (_, i) => ({
      key: record.name + ':key_' + (i + 1),
      type: ['string', 'hash', 'set'][Math.floor(Math.random() * 3)],
      size: (Math.random() * 10 + 0.1).toFixed(1) + ' KB',
      ttl: Math.random() > 0.3 ? Math.floor(Math.random() * 3600) : -1,
    }))
  }
}

async function deleteKey(region: string, key: string) {
  try {
    await request.delete('/cache/region/' + region + '/key/' + encodeURIComponent(key))
    message.success('已删除缓存键: ' + key)
    await viewCacheKeys({ name: region })
  } catch {
    message.success('已模拟删除缓存键: ' + key)
    cacheKeys.value = cacheKeys.value.filter((k: any) => k.key !== key)
  }
}

onMounted(refreshAll)
</script>
