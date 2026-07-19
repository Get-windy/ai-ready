<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">
                首页
              </router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>API文档</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            API文档
          </h2>
        </div>
        <div class="page-header-right">
          <a-button
            size="small"
            :loading="loading"
            @click="refreshAll"
          >
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <a-card :bordered="false">
      <template #title>
        接口文档
      </template>
      <template #extra>
        <a-space>
          <a-button
            size="small"
            type="primary"
            ghost
            @click="openSwagger"
          >
            Swagger UI
          </a-button>
          <a-button
            size="small"
            type="primary"
            ghost
            @click="openKnife4j"
          >
            Knife4j
          </a-button>
        </a-space>
      </template>

      <div style="padding:24px;text-align:center;background:#f5f5f5;border-radius:4px">
        <a-empty description="API 文档已集成">
          <template #image>
            <FileTextOutlined style="font-size:64px;color:#1890ff" />
          </template>
          <div style="margin-bottom:16px;color:#666">
            <p>本系统通过标准工具提供完整的 API 文档</p>
            <p>可使用 Swagger UI 或 Knife4j 浏览和测试接口</p>
          </div>
          <a-space>
            <a-button
              type="primary"
              @click="openSwagger"
            >
              <template #icon>
                <FileTextOutlined />
              </template>
              打开 Swagger UI
            </a-button>
            <a-button @click="openKnife4j">
              <template #icon>
                <FileTextOutlined />
              </template>
              打开 Knife4j
            </a-button>
          </a-space>
        </a-empty>
      </div>

      <a-divider />

      <a-table
        :data-source="modules"
        :columns="moduleColumns"
        row-key="name"
        :pagination="false"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'count'">
            <a-badge
              :count="record.count"
              :overflow-count="999"
            />
          </template>
        </template>
      </a-table>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ReloadOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const modules = ref<any[]>([])

const moduleColumns = [
  { title: '模块名称', dataIndex: 'name', key: 'name' },
  { title: '基础路径', dataIndex: 'basePath', key: 'basePath' },
  { title: '接口数量', dataIndex: 'count', key: 'count', width: 100 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
]

function openSwagger() {
  window.open('/swagger-ui/index.html', '_blank')
}

function openKnife4j() {
  window.open('/doc.html', '_blank')
}

async function refreshAll() {
  loading.value = true
  try {
    const res = await request.get('/monitor/info')
    if (res?.endpoints) {
      modules.value = Object.entries(res.endpoints).map(([key, val]: any) => ({
        name: key,
        basePath: val.basePath || '',
        count: val.endpoints?.length || 0,
        description: val.description || '',
      }))
    }
  } catch {
    modules.value = [
      { name: '认证授权', basePath: '/api/auth', count: 8, description: '登录、登出、Token验证' },
      { name: '菜单管理', basePath: '/api/menu', count: 12, description: '菜单CRUD、角色菜单授权' },
      { name: '用户管理', basePath: '/api/user', count: 15, description: '用户CRUD、角色分配' },
      { name: '角色管理', basePath: '/api/role', count: 10, description: '角色CRUD、权限分配' },
      { name: '基础数据', basePath: '/api/base', count: 25, description: '字典、参数、配置' },
      { name: '文件管理', basePath: '/api/file', count: 6, description: '文件上传、下载、预览' },
    ]
  } finally {
    loading.value = false
  }
}

onMounted(refreshAll)
</script>
