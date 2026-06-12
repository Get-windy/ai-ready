<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统设置</a-breadcrumb-item>
            <a-breadcrumb-item>数据导入</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-title">数据导入</h2>
          <p class="page-desc">绑定外部系统账号，将数据自动同步到当前租户</p>
        </div>
        <div class="page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" v-permission="'system:dataimport:query'" @click="handleRefresh">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
          <a-button type="primary" v-permission="'system:dataimport:create'" @click="showCreateDrawer">
            <template #icon><PlusOutlined /></template>
            新建导入配置
          </a-button>
        </div>
      </div>
    </template>

    <!-- 配置列表 -->
    <a-skeleton v-if="loading && configs.length === 0" active :paragraph="{ rows: 6 }" style="padding: 20px;" />
    <div class="config-list" v-else>
      <!-- 无数据 -->
      <a-empty v-if="configs.length === 0 && !hasError" description="暂无导入配置">
        <template #extra>
          <a-button type="primary" @click="showCreateDrawer">新建配置</a-button>
        </template>
      </a-empty>

      <a-result v-else-if="configs.length === 0 && hasError" status="error" title="数据加载失败">
        <template #extra>
          <a-button type="primary" @click="debounceClick('refresh', loadConfigs)()">
            <template #icon><ReloadOutlined /></template>
            重新加载
          </a-button>
        </template>
      </a-result>

      <!-- 配置卡片 -->
      <div v-else class="config-cards">
        <a-card
          v-for="item in configs"
          :key="item.id"
          class="config-card"
          :class="{ disabled: item.status === 0 }"
        >
          <div class="card-header">
            <div class="card-source">
              <span class="source-icon" :class="'icon-' + item.sourceType">
                <CloudOutlined />
              </span>
              <div>
                <div class="source-name">{{ item.displayName || getSystemName(item.sourceType) }}</div>
                <div class="source-type">{{ item.sourceType }}</div>
              </div>
            </div>
            <div class="card-status">
              <a-switch
                :checked="item.status === 1"
                @change="(checked: any) => toggleStatus(item.id, checked as boolean)"
              />
            </div>
          </div>

          <a-divider style="margin: 12px 0" />

          <div class="card-body">
            <a-descriptions :column="2" size="small">
              <a-descriptions-item label="登录账号">
                {{ item.sourceUsername }}
              </a-descriptions-item>
              <a-descriptions-item label="同步方式">
                <a-tag :color="item.syncMode === 'incremental' ? 'blue' : 'orange'">
                  {{ item.syncMode === 'incremental' ? '增量同步' : '全量同步' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="同步频率">
                {{ item.syncCron || '未设置' }}
              </a-descriptions-item>
              <a-descriptions-item label="心跳间隔">
                {{ item.heartbeatInterval ? item.heartbeatInterval + 's' : '未设置' }}
              </a-descriptions-item>
              <a-descriptions-item label="同步单据">
                <span class="bill-types">{{ formatBillTypes(item.billTypes) }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="最后同步">
                {{ item.lastSyncTime || '从未同步' }}
              </a-descriptions-item>
            </a-descriptions>
          </div>

          <div class="card-footer">
            <a-space>
              <a-button size="small" v-permission="'system:dataimport:update'" @click="editConfig(item)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-button size="small" v-permission="'system:dataimport:test'" @click="testConnection(item.id)">
                <template #icon><ApiOutlined /></template>
                测试连接
              </a-button>
              <a-button size="small" type="primary" ghost v-permission="'system:dataimport:sync'" @click="triggerSync(item.id, 'incremental')">
                <template #icon><SyncOutlined /></template>
                立即同步
              </a-button>
              <a-popconfirm title="确定删除此配置？" @confirm="deleteConfig(item.id)">
                <a-button size="small" danger v-permission="'system:dataimport:delete'">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </div>
        </a-card>
      </div>
    </div>

    <!-- 新建/编辑抽屉 -->
    <a-drawer
      :open="drawerVisible"
      :title="editingId ? '编辑导入配置' : '新建导入配置'"
      width="600px"
      @close="closeDrawer"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        layout="vertical"
      >
        <!-- 导入系统 -->
        <a-form-item label="导入系统" name="sourceType" required>
          <a-select
            v-model:value="formData.sourceType"
            placeholder="请选择要绑定的外部系统"
            size="small"
            :options="sourceOptions"
            :loading="sourcesLoading"
            @change="onSourceChange"
          >
            <template #option="{ label, description }">
              <div>
                <div>{{ label }}</div>
                <div class="option-desc">{{ description }}</div>
              </div>
            </template>
          </a-select>
        </a-form-item>

        <!-- 显示名称 -->
        <a-form-item label="显示名称" name="displayName">
          <a-input v-model:value="formData.displayName" placeholder="自定义显示名称（选填）" />
        </a-form-item>

        <!-- 登录信息 -->
        <a-divider>账号绑定</a-divider>

        <a-form-item label="登录账号" name="sourceUsername" required>
          <a-input v-model:value="formData.sourceUsername" placeholder="外部系统的登录账号" />
        </a-form-item>

        <a-form-item label="登录密码" name="sourcePassword" required>
          <a-input-password v-model:value="formData.sourcePassword" placeholder="外部系统的登录密码" />
        </a-form-item>

        <a-form-item label="API 地址">
          <a-input v-model:value="formData.baseUrl" placeholder="API 基础地址" />
        </a-form-item>

        <!-- 同步配置 -->
        <a-divider>同步设置</a-divider>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="同步方式" name="syncMode">
              <a-radio-group v-model:value="formData.syncMode">
                <a-radio value="incremental">增量同步</a-radio>
                <a-radio value="full">全量同步</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="心跳间隔（秒）" name="heartbeatInterval">
              <a-input-number v-model:value="formData.heartbeatInterval" :min="60" :max="86400" :step="60" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="同步频率（Cron）" name="syncCron">
          <a-input v-model:value="formData.syncCron" placeholder="*/30 * * * *" />
          <div class="form-help">
            默认每30分钟一次。格式: 秒 分 时 日 月 周
          </div>
        </a-form-item>

        <a-form-item label="同步单据类型">
          <a-checkbox-group v-model:value="selectedBillTypes">
            <a-checkbox
              v-for="item in billTypeItems"
              :key="item.itemValue"
              :value="item.itemValue"
            >{{ item.itemText }}</a-checkbox>
          </a-checkbox-group>
        </a-form-item>

        <a-form-item label="备注">
          <a-textarea v-model:value="formData.remark" :rows="2" />
        </a-form-item>
      </a-form>

      <template #footer>
        <a-space>
          <a-button @click="closeDrawer">取消</a-button>
          <a-button type="primary" :loading="saving" @click="saveConfig">保存</a-button>
        </a-space>
      </template>
    </a-drawer>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, EditOutlined, DeleteOutlined, SyncOutlined, ApiOutlined, CloudOutlined, WarningOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'
import { dictItemApi, type DictItem } from '@/api/dict'

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

interface SyncConfig {
  id: number
  tenantId: number
  sourceType: string
  displayName: string
  sourceUsername: string
  sourcePassword: string
  baseUrl: string
  syncMode: string
  syncCron: string
  heartbeatInterval: number
  billTypes: string
  status: number
  lastSyncTime: string
  remark: string
}

interface SourceOption {
  systemCode: string
  systemName: string
  description: string
}

const loading = ref(false)
const hasError = ref(false)
const configs = ref<SyncConfig[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const drawerVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const sourcesLoading = ref(false)
const sourceOptions = ref<SourceOption[]>([])
const billTypeItems = ref<DictItem[]>([])

const billTypeLabelMap = computed(() => {
  const map: Record<string, string> = {}
  billTypeItems.value.forEach(item => {
    map[item.itemValue] = item.itemText
  })
  return map
})

const formRef = ref()
const selectedBillTypes = ref<string[]>(['601', '604', '504', '801'])

const defaultForm = {
  sourceType: '',
  displayName: '',
  sourceUsername: '',
  sourcePassword: '',
  baseUrl: 'https://www.ql361.com',
  syncMode: 'incremental',
  syncCron: '*/30 * * * *',
  heartbeatInterval: 300,
  billTypes: '["601","604","504","801"]',
  remark: '',
}

const formData = reactive({ ...defaultForm })

const formRules: any = {
  sourceType: [{ required: true, message: '请选择导入系统' }],
  sourceUsername: [{ required: true, message: '请输入登录账号' }],
  sourcePassword: [{ required: true, message: '请输入登录密码' }],
}

// 获取系统名称
function getSystemName(sourceType: string): string {
  const found = sourceOptions.value.find(s => s.systemCode === sourceType)
  return found ? found.systemName : sourceType
}

// 格式化单据类型
function formatBillTypes(billTypes: string): string {
  try {
    const types = JSON.parse(billTypes || '[]')
    return types.map((t: string) => billTypeLabelMap.value[t] || t).join('、')
  } catch {
    return billTypes
  }
}

// 加载单据类型列表
async function loadBillTypes() {
  try {
    const res = await dictItemApi.getByDictCode('BILL_TYPE')
    billTypeItems.value = res.data || []
  } catch (e) {
    console.error('加载单据类型失败:', e)
  }
}

// 加载支持的导入系统列表
async function loadSources() {
  sourcesLoading.value = true
  try {
    const res = await request.get('/v1/sync-config/sources')
    sourceOptions.value = (res.data || []).map((s: any) => ({
      label: s.systemName,
      value: s.systemCode,
      ...s,
    }))
  } catch (e) {
    console.error('加载导入系统列表失败:', e)
  } finally {
    sourcesLoading.value = false
  }
}

// ── 刷新 ──────────────────────────────────────────────
const handleRefresh = () => {
  refreshLoading.value = true
  loadConfigs()
}

// 加载配置列表
async function loadConfigs() {
  loading.value = true
  hasError.value = false
  try {
    const res = await request.get('/v1/sync-config')
    configs.value = res.data || []
  } catch (e) {
    hasError.value = true
    console.error('加载配置列表失败:', e)
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// 新建
function showCreateDrawer() {
  editingId.value = null
  Object.assign(formData, defaultForm)
  selectedBillTypes.value = ['601', '604', '504', '801']
  drawerVisible.value = true
}

// 编辑
function editConfig(item: SyncConfig) {
  editingId.value = item.id
  formData.sourceType = item.sourceType
  formData.displayName = item.displayName
  formData.sourceUsername = item.sourceUsername
  formData.sourcePassword = ''
  formData.baseUrl = item.baseUrl
  formData.syncMode = item.syncMode
  formData.syncCron = item.syncCron
  formData.heartbeatInterval = item.heartbeatInterval
  formData.remark = item.remark
  try {
    selectedBillTypes.value = JSON.parse(item.billTypes || '[]')
  } catch {
    selectedBillTypes.value = []
  }
  drawerVisible.value = true
}

function closeDrawer() {
  drawerVisible.value = false
  formRef.value?.resetFields()
}

// 来源类型切换
function onSourceChange(value: string) {
  if (!formData.displayName) {
    formData.displayName = getSystemName(value)
  }
}

// 保存
async function saveConfig() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    formData.billTypes = JSON.stringify(selectedBillTypes.value)

    if (editingId.value) {
      await request.put(`/v1/sync-config/${editingId.value}`, formData)
      message.success('更新成功')
    } else {
      await request.post('/v1/sync-config', formData)
      message.success('创建成功')
    }

    closeDrawer()
    await loadConfigs()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 删除
async function deleteConfig(id: number) {
  try {
    await request.delete(`/v1/sync-config/${id}`)
    message.success('删除成功')
    await loadConfigs()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

// 启用/禁用
async function toggleStatus(id: number, enabled: boolean) {
  try {
    await request.post(`/v1/sync-config/${id}/toggle`, null, {
      params: { enabled },
    })
    message.success(enabled ? '已启用' : '已禁用')
    await loadConfigs()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

// 测试连接
async function testConnection(id: number) {
  try {
    const res = await request.post(`/v1/sync-config/${id}/test`)
    if (res?.connected) {
      message.success(`连接成功 (${res?.latency}ms)`)
    } else {
      message.error(res?.message || '连接失败')
    }
  } catch (e: any) {
    message.error(e?.message || '连接测试失败')
  }
}

// 触发同步
async function triggerSync(id: number, syncType: string) {
  try {
    const res = await request.post(`/v1/sync-config/${id}/sync`, null, {
      params: { syncType },
    })
    if (res.data?.success) {
      message.success('同步任务已提交')
    }
  } catch (e: any) {
    message.error(e?.message || '触发同步失败')
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', loadConfigs)()
  }
}

onMounted(async () => {
  await loadSources()
  await loadBillTypes()
  await loadConfigs()
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadConfigs()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-header-left {
  flex: 1;
}

.page-title {
  margin: 8px 0 4px;
  font-size: 20px;
  font-weight: 600;
}

.page-desc {
  margin: 0;
  color: #8c8c8c;
  font-size: 13px;
}

.config-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(500px, 1fr));
  gap: 16px;
}

.config-card {
  border-radius: 8px;
  transition: all 0.3s;
}

.config-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.config-card.disabled {
  opacity: 0.6;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-source {
  display: flex;
  align-items: center;
  gap: 12px;
}

.source-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: #e6f7ff;
  color: #1890ff;
}

.source-name {
  font-weight: 600;
  font-size: 15px;
}

.source-type {
  color: #8c8c8c;
  font-size: 12px;
}

.card-footer {
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.bill-types {
  font-size: 12px;
  color: #595959;
}

.form-help {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.option-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
.config-list :deep(.ant-input-sm),
.config-list :deep(.ant-input-number-sm),
.config-list :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.config-list :deep(.ant-picker-small),
.config-list :deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
.config-list :deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
.config-list :deep(.ant-input-number-sm input) { height: 26px; }

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

</style>
