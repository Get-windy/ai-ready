<template>
  <ErrorBoundary>
    <PageContainer title="应用中心">
      <div class="content-card">
        <a-spin :spinning="loading">
          <!-- 套餐信息 -->
          <template v-if="packageList.length > 0">
            <h4 class="section-title">
              租户套餐
            </h4>
            <a-row :gutter="[16, 16]">
              <a-col
                v-for="pkg in packageList"
                :key="pkg.id"
                :xs="24"
                :sm="12"
                :md="8"
              >
                <a-card
                  size="small"
                  class="pkg-card"
                >
                  <div class="pkg-header">
                    <span class="pkg-name">{{ pkg.packageName }}</span>
                    <a-tag :color="pkg.status === 1 ? 'success' : 'default'">
                      {{ pkg.status === 1 ? '生效中' : '已停用' }}
                    </a-tag>
                  </div>
                  <div class="pkg-meta">
                    <span>套餐编码：{{ pkg.packageCode }}</span>
                    <span v-if="pkg.maxUsers">最大用户数：{{ formatNumber(pkg.maxUsers) }}</span>
                    <span v-if="pkg.price !== undefined && pkg.price !== null">价格：¥{{ formatMoney(pkg.price) }}</span>
                  </div>
                  <div
                    v-if="pkg.description"
                    class="pkg-desc"
                  >
                    {{ pkg.description }}
                  </div>
                </a-card>
              </a-col>
            </a-row>
            <a-divider />
          </template>

          <!-- 已开通模块 -->
          <div class="module-header">
            <h4 class="section-title">
              已开通模块（{{ filteredModules.length }}）
            </h4>
            <a-input-search
              v-model:value="keyword"
              placeholder="搜索模块名称 / 编码"
              style="width: 240px"
              allow-clear
            />
          </div>
          <a-row :gutter="[16, 16]">
            <a-col
              v-for="mod in filteredModules"
              :key="mod.id"
              :xs="24"
              :sm="12"
              :md="8"
              :lg="6"
            >
              <a-card
                hoverable
                class="app-card"
              >
                <div class="app-header">
                  <div
                    class="app-icon"
                    :style="{ background: moduleColor(mod.moduleCode) }"
                  >
                    {{ mod.moduleName?.charAt(0) || '?' }}
                  </div>
                  <div class="app-title">
                    <div class="app-name">
                      {{ mod.moduleName }}
                    </div>
                    <a-tag class="app-code">
                      {{ mod.moduleCode }}
                    </a-tag>
                  </div>
                </div>
                <div class="app-meta">
                  <div class="meta-row">
                    <span class="meta-label">开通类型</span>
                    <a-tag :color="purchaseTypeTag(mod.purchaseType).color">
                      {{ purchaseTypeTag(mod.purchaseType).label }}
                    </a-tag>
                  </div>
                  <div class="meta-row">
                    <span class="meta-label">到期时间</span>
                    <span>{{ formatDate(mod.expireTime) }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="meta-label">状态</span>
                    <a-tag :color="moduleStatus(mod).color">
                      {{ moduleStatus(mod).label }}
                    </a-tag>
                  </div>
                </div>
              </a-card>
            </a-col>
          </a-row>
          <a-empty
            v-if="!loading && filteredModules.length === 0"
            description="暂无已开通模块"
          />
        </a-spin>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { tenantModuleApi, type TenantModule } from '@/api/tenantModule'
import { tenantPackageApi, type TenantPackageInfo } from '@/api/tenant'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const moduleList = ref<TenantModule[]>([])
const packageList = ref<TenantPackageInfo[]>([])
const keyword = ref('')

const PURCHASE_TYPE_MAP: Record<string, { label: string; color: string }> = {
  trial: { label: '试用', color: 'orange' },
  purchase: { label: '购买', color: 'green' },
  gift: { label: '赠送', color: 'blue' },
  free: { label: '免费', color: 'default' }
}

// 固定色板，按模块编码哈希取色（不使用随机色，保证同一模块颜色稳定）
const COLOR_PALETTE = ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16']

const filteredModules = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return moduleList.value
  return moduleList.value.filter(m =>
    (m.moduleName || '').toLowerCase().includes(kw) ||
    (m.moduleCode || '').toLowerCase().includes(kw)
  )
})

function moduleColor(code: string | undefined): string {
  const str = code || ''
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = (hash * 31 + str.charCodeAt(i)) >>> 0
  }
  return COLOR_PALETTE[hash % COLOR_PALETTE.length]
}

function purchaseTypeTag(type: string | undefined): { label: string; color: string } {
  if (!type) return { label: '-', color: 'default' }
  return PURCHASE_TYPE_MAP[type.toLowerCase()] || { label: type, color: 'default' }
}

function moduleStatus(mod: TenantModule): { label: string; color: string } {
  if (mod.status !== 1) return { label: '已停用', color: 'default' }
  if (mod.expireTime && new Date(mod.expireTime).getTime() < Date.now()) {
    return { label: '已过期', color: 'error' }
  }
  return { label: '有效', color: 'success' }
}

function formatDate(val: string | null | undefined): string {
  if (!val) return '长期有效'
  return String(val).slice(0, 10)
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

async function loadModules() {
  loading.value = true
  try {
    const res: any = await tenantModuleApi.getList(userStore.tenantId || 1)
    moduleList.value = Array.isArray(res) ? res : res?.data || []
  } catch (e) {
    console.warn('[应用中心] 租户模块获取失败', e)
  } finally {
    loading.value = false
  }
}

async function loadPackages() {
  try {
    const res: any = await tenantPackageApi.getList()
    packageList.value = res?.records || []
  } catch (e) {
    // 无平台套餐权限（platform:tenant-package:list）时仅展示模块区
    console.warn('[应用中心] 套餐列表获取失败', e)
  }
}

onMounted(() => {
  loadModules()
  loadPackages()
})
</script>

<style scoped>
.tip-alert { margin-bottom: 12px; }
.content-card { background: #fff; padding: 24px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.section-title { margin: 0 0 12px; font-weight: 600; }
.module-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 8px; }
.module-header .section-title { margin: 0; }
.pkg-card { height: 100%; }
.pkg-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.pkg-name { font-size: 15px; font-weight: 600; }
.pkg-meta { display: flex; flex-direction: column; gap: 4px; color: #666; font-size: 13px; }
.pkg-desc { margin-top: 8px; color: #999; font-size: 12px; }
.app-card { height: 100%; }
.app-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.app-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: bold; color: #fff; flex-shrink: 0; }
.app-title { min-width: 0; }
.app-name { font-size: 15px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.app-code { margin-top: 4px; }
.app-meta { display: flex; flex-direction: column; gap: 6px; }
.meta-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.meta-label { color: #999; width: 60px; flex-shrink: 0; }
</style>
