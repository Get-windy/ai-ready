<template>
  <ErrorBoundary>
    <PageContainer title="应用中心">
      <div class="content-card">
        <a-spin :spinning="loading">
          <a-row :gutter="[16, 16]">
            <a-col v-for="app in appList" :key="app.id" :xs="24" :sm="12" :md="8" :lg="6">
              <a-card hoverable class="app-card" @click="handleOpen(app)">
                <template #cover>
                  <div class="app-icon" :style="{ background: app.color || '#1890ff' }">
                    <component :is="app.iconComputed" v-if="app.iconComputed" />
                    <span v-else class="app-icon-text">{{ app.name?.charAt(0) }}</span>
                  </div>
                </template>
                <a-card-meta :title="app.name">
                  <template #description>
                    <span class="app-desc">{{ app.description || '暂无描述' }}</span>
                  </template>
                </a-card-meta>
                <template #actions>
                  <a-tag :color="app.enabled ? 'success' : 'default'">{{ app.enabled ? '已启用' : '未启用' }}</a-tag>
                  <a-switch v-model:checked="app.enabled" size="small" @click.stop @change="(checked: boolean) => handleToggle(app, checked)" />
                </template>
              </a-card>
            </a-col>
          </a-row>
          <a-empty v-if="!loading && appList.length === 0" description="暂无可用应用" />
        </a-spin>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { AppstoreOutlined, ShoppingCartOutlined, TeamOutlined, FileTextOutlined, SettingOutlined, ToolOutlined, WalletOutlined, BarChartOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

const loading = ref(false)
const appList = ref<any[]>([])

const iconMap: Record<string, any> = {
  AppstoreOutlined, ShoppingCartOutlined, TeamOutlined,
  FileTextOutlined, SettingOutlined, ToolOutlined,
  WalletOutlined, BarChartOutlined
}

async function loadApps() {
  loading.value = true
  try {
    const result = await request.get('/module/list')
    if (Array.isArray(result)) {
      appList.value = result.map((app: any) => ({
        ...app,
        iconComputed: iconMap[app.icon as string] || null,
        color: app.color || ['#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2', '#eb2f96', '#fa8c16'][Math.floor(Math.random() * 8)]
      }))
    }
  } catch (e) {
    // 静默失败，使用默认数据
    appList.value = getDefaultApps()
  } finally {
    loading.value = false
  }
}

function getDefaultApps() {
  return [
    { id: 1, name: 'ERP管理', description: '进销存、财务、生产一体化管理', iconComputed: AppstoreOutlined, color: '#1890ff', enabled: true },
    { id: 2, name: 'CRM客户', description: '客户关系管理与销售跟进', iconComputed: TeamOutlined, color: '#52c41a', enabled: true },
    { id: 3, name: 'WMS仓储', description: '智能仓储与库存管理', iconComputed: ShoppingCartOutlined, color: '#faad14', enabled: true },
    { id: 4, name: 'DMS配送', description: '配送路线规划与调度', iconComputed: ToolOutlined, color: '#f5222d', enabled: true },
    { id: 5, name: '财务管理', description: '应收应付、账务核算', iconComputed: WalletOutlined, color: '#722ed1', enabled: true },
    { id: 6, name: '人力资源', description: '人事考勤薪资绩效管理', iconComputed: TeamOutlined, color: '#13c2c2', enabled: true },
    { id: 7, name: '数据分析', description: '经营分析与数据报表', iconComputed: BarChartOutlined, color: '#eb2f96', enabled: true },
    { id: 8, name: '系统设置', description: '系统参数、权限配置', iconComputed: SettingOutlined, color: '#fa8c16', enabled: true }
  ]
}

function handleOpen(app: any) {
  if (app.url) {
    window.open(app.url, '_blank')
  }
}

async function handleToggle(app: any, checked: boolean) {
  try {
    await request.put(`/module/${app.id}`, { enabled: checked })
    message.success(`${app.name} ${checked ? '已启用' : '已停用'}`)
  } catch (e) {
    app.enabled = !checked
    message.error('操作失败')
  }
}

onMounted(loadApps)
</script>

<style scoped>
.content-card { background: #fff; padding: 24px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.app-card { cursor: pointer; transition: all .3s; }
.app-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(0,0,0,.12); }
.app-icon { height: 120px; display: flex; align-items: center; justify-content: center; font-size: 48px; color: #fff; }
.app-icon-text { font-size: 48px; font-weight: bold; color: #fff; }
.app-desc { display: block; min-height: 36px; color: #666; font-size: 13px; }
</style>
