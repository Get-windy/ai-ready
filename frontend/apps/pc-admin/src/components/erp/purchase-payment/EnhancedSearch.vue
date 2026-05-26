<template>
  <div class="enhanced-search">
    <a-form
      layout="inline"
      :model="formState"
      ref="searchFormRef"
    >
      <!-- 基础搜索 -->
      <a-form-item label="订单号">
        <a-input
          v-model:value="formState.orderNo"
          placeholder="请输入订单号"
          allow-clear
          @press-enter="handleSearch"
        />
      </a-form-item>
      
      <a-form-item label="供应商">
        <a-select
          v-model:value="formState.supplierId"
          placeholder="请选择供应商"
          allow-clear
          show-search
          option-filter-prop="label"
          :options="supplierOptions"
          style="width: 180px"
        />
      </a-form-item>
      
      <a-form-item label="状态">
        <a-select
          v-model:value="formState.status"
          placeholder="请选择状态"
          allow-clear
          style="width: 120px"
          :options="statusOptions"
        />
      </a-form-item>
      
      <!-- 高级搜索切换 -->
      <a-form-item>
        <a-button
          type="link"
          @click="toggleAdvancedSearch"
          size="small"
        >
          {{ showAdvanced ? '收起高级搜索' : '展开高级搜索' }}
        </a-button>
      </a-form-item>
      
      <!-- 操作按钮 -->
      <a-form-item>
        <a-space>
          <a-button
            type="primary"
            @click="handleSearch"
            :loading="loading"
          >
            查询
          </a-button>
          <a-button @click="handleReset">
            重置
          </a-button>
        </a-space>
      </a-form-item>
    </a-form>
    
    <!-- 高级搜索区域 -->
    <a-collapse
      v-model:activeKey="collapseActiveKeys"
      :bordered="false"
      class="advanced-search-collapse"
      v-if="showAdvanced"
    >
      <a-collapse-panel key="advanced" header="高级搜索">
        <a-form
          layout="vertical"
          :model="formState"
        >
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="金额范围">
                <a-input-number-group compact>
                  <a-input-number
                    v-model:value="formState.minAmount"
                    placeholder="最小金额"
                    :min="0"
                    :precision="2"
                    :formatter="(value) => `¥${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                    style="width: 45%"
                  />
                  <a-input
                    placeholder="~"
                    disabled
                    style="width: 10%; text-align: center; background: #f5f5f5; border-color: #d9d9d9"
                  />
                  <a-input-number
                    v-model:value="formState.maxAmount"
                    placeholder="最大金额"
                    :min="0"
                    :precision="2"
                    :formatter="(value) => `¥${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')"
                    style="width: 45%"
                  />
                </a-input-number-group>
              </a-form-item>
            </a-col>
            
            <a-col :span="8">
              <a-form-item label="日期范围">
                <a-range-picker
                  v-model:value="formState.dateRange"
                  style="width: 100%"
                  :placeholder="['开始日期', '结束日期']"
                  format="YYYY-MM-DD"
                />
              </a-form-item>
            </a-col>
            
            <a-col :span="8">
              <a-form-item label="采购员">
                <a-select
                  v-model:value="formState.purchaserId"
                  placeholder="请选择采购员"
                  allow-clear
                  show-search
                  option-filter-prop="label"
                  :options="purchaserOptions"
                />
              </a-form-item>
            </a-col>
          </a-row>
          
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="创建人">
                <a-input
                  v-model:value="formState.creator"
                  placeholder="请输入创建人"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            
            <a-col :span="8">
              <a-form-item label="关键词">
                <a-input
                  v-model:value="formState.keyword"
                  placeholder="供应商名称/订单备注等"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            
            <a-col :span="8">
              <a-form-item label=" ">
                <a-space>
                  <a-button
                    type="primary"
                    @click="handleSaveSearch"
                    size="small"
                  >
                    保存搜索条件
                  </a-button>
                  <a-button
                    @click="handleLoadSearch"
                    size="small"
                  >
                    加载已保存
                  </a-button>
                </a-space>
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>
      </a-collapse-panel>
    </a-collapse>
    
    <!-- 搜索历史 -->
    <div class="search-history" v-if="searchHistory.length > 0">
      <a-divider orientation="left">最近搜索</a-divider>
      <a-space wrap>
        <a-tag
          v-for="(item, index) in searchHistory"
          :key="index"
          color="blue"
          @click="handleHistoryClick(item)"
          class="history-tag"
        >
          {{ item.name }}
        </a-tag>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

interface SearchParams {
  orderNo: string
  supplierId?: number
  status?: number
  minAmount?: number
  maxAmount?: number
  dateRange?: [dayjs.Dayjs, dayjs.Dayjs]
  purchaserId?: number
  creator: string
  keyword: string
}

interface SearchHistoryItem {
  name: string
  params: Partial<SearchParams>
  timestamp: number
}

interface Props {
  loading?: boolean
  initialParams?: Partial<SearchParams>
}

interface Emits {
  (e: 'search', params: Partial<SearchParams>): void
  (e: 'reset'): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<Emits>()

// 搜索表单状态
const formState = reactive<SearchParams>({
  orderNo: '',
  supplierId: undefined,
  status: undefined,
  minAmount: undefined,
  maxAmount: undefined,
  dateRange: undefined,
  purchaserId: undefined,
  creator: '',
  keyword: ''
})

// 组件状态
const searchFormRef = ref<FormInstance>()
const showAdvanced = ref(false)
const collapseActiveKeys = ref<string[]>(['advanced'])
const searchHistory = ref<SearchHistoryItem[]>([])
const STORAGE_KEY = 'erp-purchase-search-history'

// 选项数据
const supplierOptions = [
  { value: 1, label: '供应商A' },
  { value: 2, label: '供应商B' },
  { value: 3, label: '供应商C' },
  { value: 4, label: '供应商D' }
]

const statusOptions = [
  { value: 0, label: '草稿' },
  { value: 1, label: '待审批' },
  { value: 2, label: '已审批' },
  { value: 3, label: '部分入库' },
  { value: 4, label: '完成' },
  { value: 5, label: '已取消' }
]

const purchaserOptions = [
  { value: 1, label: '张三' },
  { value: 2, label: '李四' },
  { value: 3, label: '王五' }
]

// 切换高级搜索
const toggleAdvancedSearch = () => {
  showAdvanced.value = !showAdvanced.value
  if (showAdvanced.value) {
    collapseActiveKeys.value = ['advanced']
  } else {
    collapseActiveKeys.value = []
  }
}

// 搜索处理
const handleSearch = () => {
  const params = { ...formState }
  // 处理日期范围
  if (params.dateRange && params.dateRange.length === 2) {
    params['startDate'] = params.dateRange[0].format('YYYY-MM-DD')
    params['endDate'] = params.dateRange[1].format('YYYY-MM-DD')
    delete params.dateRange
  }
  
  // 添加到搜索历史
  addToSearchHistory(params)
  
  emit('search', params)
}

// 重置表单
const handleReset = () => {
  Object.assign(formState, {
    orderNo: '',
    supplierId: undefined,
    status: undefined,
    minAmount: undefined,
    maxAmount: undefined,
    dateRange: undefined,
    purchaserId: undefined,
    creator: '',
    keyword: ''
  })
  emit('reset')
}

// 保存搜索条件
const handleSaveSearch = () => {
  const name = prompt('请输入保存的名称：')
  if (name) {
    const item: SearchHistoryItem = {
      name,
      params: { ...formState },
      timestamp: Date.now()
    }
    addToSearchHistory(item.params, name)
    message.success('搜索条件已保存')
  }
}

// 加载搜索历史
const handleLoadSearch = () => {
  const items = searchHistory.value
  if (items.length === 0) {
    message.info('暂无保存的搜索条件')
    return
  }
  
  Modal.confirm({
    title: '选择搜索条件',
    content: (
      <div>
        {items.map(item => (
          <div key={item.timestamp} class="history-item">
            <a-radio value={item.timestamp}>{item.name}</a-radio>
          </div>
        ))}
      </div>
    ),
    onOk(selectedTimestamp: number) {
      const item = items.find(i => i.timestamp === selectedTimestamp)
      if (item) {
        Object.assign(formState, item.params)
        handleSearch()
      }
    }
  })
}

// 点击历史搜索标签
const handleHistoryClick = (item: SearchHistoryItem) => {
  Object.assign(formState, item.params)
  handleSearch()
}

// 添加到搜索历史
const addToSearchHistory = (params: Partial<SearchParams>, customName?: string) => {
  const existingIndex = searchHistory.value.findIndex(item => 
    JSON.stringify(item.params) === JSON.stringify(params)
  )
  
  if (existingIndex > -1) {
    // 更新已有的记录
    searchHistory.value.splice(existingIndex, 1)
  }
  
  const name = customName || generateSearchName(params)
  const newItem: SearchHistoryItem = {
    name,
    params: { ...params },
    timestamp: Date.now()
  }
  
  searchHistory.value.unshift(newItem)
  
  // 只保留最近的10条记录
  if (searchHistory.value.length > 10) {
    searchHistory.value = searchHistory.value.slice(0, 10)
  }
  
  // 保存到本地存储
  localStorage.setItem(STORAGE_KEY, JSON.stringify(searchHistory.value))
}

// 生成搜索名称
const generateSearchName = (params: Partial<SearchParams>): string => {
  const parts: string[] = []
  
  if (params.orderNo) {
    parts.push(`订单:${params.orderNo}`)
  }
  
  if (params.supplierId) {
    const supplier = supplierOptions.find(s => s.value === params.supplierId)
    if (supplier) parts.push(`供应商:${supplier.label}`)
  }
  
  if (params.status !== undefined) {
    const status = statusOptions.find(s => s.value === params.status)
    if (status) parts.push(`状态:${status.label}`)
  }
  
  if (params.creator) {
    parts.push(`创建人:${params.creator}`)
  }
  
  if (params.keyword) {
    parts.push(`关键词:${params.keyword}`)
  }
  
  return parts.length > 0 ? parts.join(';') : '全部'
}

// 初始化
onMounted(() => {
  // 从本地存储加载搜索历史
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved) {
    try {
      searchHistory.value = JSON.parse(saved)
    } catch (e) {
      console.error('Failed to parse search history:', e)
    }
  }
  
  // 如果有初始参数，设置到表单
  if (props.initialParams) {
    Object.assign(formState, props.initialParams)
  }
})

// 监听加载状态变化
watch(() => props.loading, (newVal) => {
  if (!newVal) {
    // 搜索完成，可以处理其他逻辑
  }
})

// 暴露方法给父组件
defineExpose({
  resetForm: handleReset,
  getFormState: () => ({ ...formState })
})
</script>

<style scoped>
.enhanced-search {
  background: #fff;
  padding: 16px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.advanced-search-collapse {
  margin-top: 16px;
  background: #fafafa;
}

.search-history {
  margin-top: 16px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.history-tag:hover {
  background-color: #e6f7ff;
  transform: translateY(-1px);
}

.history-item {
  padding: 4px 0;
}
</style>