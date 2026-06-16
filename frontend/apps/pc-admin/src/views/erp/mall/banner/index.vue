<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <a-breadcrumb>
          <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
          <a-breadcrumb-item><router-link to="/mall">商城管理</router-link></a-breadcrumb-item>
          <a-breadcrumb-item>轮播图管理</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="page-header__update-time">更新于: {{ lastUpdateTime }}</span>
          <a-space :size="8">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>N</kbd> 新增</span>
            </span>
            <a-tooltip title="F5: 刷新 | Ctrl+N: 新增">
              <a-button size="small" @click="debounceClick('refresh', fetchData)">
                <template #icon><ReloadOutlined /></template>刷新
              </a-button>
            </a-tooltip>
          </a-space>
        </div>
      </div>
    </template>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="false as any"
      :row-key="'id'"
      :show-search="false"
      add-text="新增轮播图"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @refresh="debounceClick('refresh', fetchData)"
    >
      <template #empty>
        <a-empty v-if="!hasError" description="暂无轮播图" />
        <a-result v-else status="error" title="数据加载失败">
          <template #extra>
            <a-button type="primary" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>重新加载
            </a-button>
          </template>
        </a-result>
      </template>

      <template #imageCell="{ record }">
        <a-image
          v-if="record.imageUrl"
          :src="record.imageUrl"
          :width="80"
          :height="45"
          style="object-fit: cover; border-radius: 4px; cursor: pointer;"
          fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
        />
        <span v-else style="color: #ccc">无图片</span>
      </template>

      <template #statusCell="{ record }">
        <a-switch
          :checked="record.status === 1"
          size="small"
          @change="handleToggleStatus(record)"
        />
      </template>

      <template #action="{ record }">
        <a-space>
          <a-button v-permission="'erp:mall:banner:edit'" type="link" size="small" @click="handleEdit(record)">编辑</a-button>
          <a-button v-permission="'erp:mall:banner:delete'" type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 轮播图表单弹窗 -->
    <FullScreenDetail
      :visible="modalVisible"
      :title="modalTitle"
      :save-loading="modalLoading"
      @save="handleModalOk"
      @close="handleFormClose"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 18 }"
        :rules="formRules"
      >
        <a-form-item label="图片URL" name="imageUrl">
          <a-input v-model:value="formState.imageUrl" placeholder="请输入图片URL" />
        </a-form-item>
        <a-form-item label="标题" name="title">
          <a-input v-model:value="formState.title" placeholder="请输入标题" :maxlength="200" />
        </a-form-item>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="链接类型" name="linkType">
              <a-select v-model:value="formState.linkType" placeholder="选择链接类型">
                <a-select-option value="none">无链接</a-select-option>
                <a-select-option value="product">商品</a-select-option>
                <a-select-option value="category">分类</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="链接目标" name="linkValue">
              <a-input v-model:value="formState.linkValue" placeholder="商品ID/分类ID" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="跳转链接" name="linkUrl">
          <a-input v-model:value="formState.linkUrl" placeholder="自定义跳转URL（可选）" />
        </a-form-item>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="排序" name="sortOrder">
              <a-input-number v-model:value="formState.sortOrder" :min="0" :style="{ width: '100%' }" placeholder="数字越小越靠前" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-switch v-model:checked="formState.status" :checked-value="1" :un-checked-value="0" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </FullScreenDetail>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'MallBannerList' })

import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { ReloadOutlined, SyncOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { shopBannerApi, type ShopBanner } from '@/api/erp/mall'

const loading = ref(false)
const hasError = ref(false)
const tableData = ref<ShopBanner[]>([])
const tableDataSource = tableData

const vxeColumns: any = computed(() => [
  { field: 'sortOrder', title: '排序', width: 70, align: 'center' },
  { field: 'imageUrl', title: '图片', width: 120, slotName: 'imageCell' },
  { field: 'title', title: '标题', width: 200, showOverflow: 'tooltip' },
  { field: 'linkType', title: '链接类型', width: 100 },
  { field: 'linkValue', title: '链接目标', width: 120 },
  { field: 'status', title: '状态', width: 70, slotName: 'statusCell' },
  { type: 'action', title: '操作', width: 140, fixed: 'right' }
])

// ── 防抖 ──
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await shopBannerApi.list()
    if (res) {
      tableData.value = Array.isArray(res) ? res : []
    }
  } catch (err) {
    hasError.value = true
    tableData.value = []
    console.warn('[轮播图] 加载列表失败', err)
  } finally {
    loading.value = false
  }
}

// ── 弹窗 ──
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const modalTitle = computed(() => isEdit.value ? '编辑轮播图' : '新增轮播图')

const formState = reactive<ShopBanner>({
  title: '',
  imageUrl: '',
  linkUrl: '',
  linkType: 'none',
  linkValue: '',
  sortOrder: 0,
  status: 1
})

const formRules: Record<string, any> = {
  imageUrl: { required: true, message: '请输入图片URL', trigger: 'blur' }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, { title: '', imageUrl: '', linkUrl: '', linkType: 'none', linkValue: '', sortOrder: 0, status: 1 })
  modalVisible.value = true
}

const handleEdit = (record: ShopBanner) => {
  isEdit.value = true
  Object.assign(formState, {
    id: record.id,
    title: record.title,
    imageUrl: record.imageUrl,
    linkUrl: record.linkUrl,
    linkType: record.linkType || 'none',
    linkValue: record.linkValue,
    sortOrder: record.sortOrder ?? 0,
    status: record.status ?? 1
  })
  modalVisible.value = true
}

const handleDelete = (record: ShopBanner) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除轮播图 "${record.title || '未命名'}" 吗？`,
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      try {
        await shopBannerApi.delete(record.id!)
        message.success('删除成功')
        fetchData()
      } catch (err) {
        console.warn('[轮播图] 删除失败', err)
      }
    }
  })
}

const handleToggleStatus = async (record: ShopBanner) => {
  const newStatus = record.status === 1 ? 0 : 1
  try {
    await shopBannerApi.update(record.id!, { ...record, status: newStatus })
    message.success(newStatus === 1 ? '已启用' : '已禁用')
    fetchData()
  } catch (err) {
    console.warn('[轮播图] 切换状态失败', err)
  }
}

const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true
    if (isEdit.value) {
      await shopBannerApi.update(formState.id!, formState as ShopBanner)
      message.success('更新成功')
    } else {
      await shopBannerApi.create(formState as ShopBanner)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[轮播图] 保存失败', err)
  } finally {
    modalLoading.value = false
  }
}

const handleFormClose = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// ── 键盘快捷键 & 自动刷新 ──
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function handleKeydown(e: KeyboardEvent) {
  if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return
  if (e.key === 'F5') {
    e.preventDefault(); debounceClick('refresh', fetchData)
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault(); handleAdd()
  }
}

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 300
  }, 300000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  document.removeEventListener('keydown', handleKeydown)
})

function handleError(err: any) { console.warn('[MallBannerList]', err) }

defineExpose({ fetchData })
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
