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
            <a-breadcrumb-item>邮件配置</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            邮件配置
          </h2>
        </div>
      </div>
    </template>

    <a-row :gutter="16">
      <a-col :span="12">
        <a-card
          :bordered="false"
          title="SMTP 配置"
        >
          <a-form layout="vertical">
            <a-form-item label="SMTP服务器">
              <a-input
                v-model:value="smtp.host"
                placeholder="smtp.example.com"
              />
            </a-form-item>
            <a-form-item label="端口">
              <a-input-number
                v-model:value="smtp.port"
                :min="1"
                :max="65535"
                style="width:100%"
              />
            </a-form-item>
            <a-form-item label="加密方式">
              <a-select v-model:value="smtp.encryption">
                <a-select-option value="none">
                  无
                </a-select-option>
                <a-select-option value="ssl">
                  SSL
                </a-select-option>
                <a-select-option value="tls">
                  TLS
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="邮箱地址">
              <a-input
                v-model:value="smtp.username"
                placeholder="noreply@example.com"
              />
            </a-form-item>
            <a-form-item label="密码/授权码">
              <a-input-password v-model:value="smtp.password" />
            </a-form-item>
            <a-form-item>
              <a-button
                type="primary"
                @click="saveSmtp"
              >
                保存配置
              </a-button>
              <a-button
                style="margin-left:8px"
                @click="testConnection"
              >
                测试连接
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card
          :bordered="false"
          title="邮件模板"
        >
          <a-table
            :data-source="templates"
            :columns="tmplColumns"
            row-key="id"
            :pagination="false"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a @click="editTemplate(record)">编辑</a>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const smtp = reactive({
  host: '',
  port: 465,
  encryption: 'ssl',
  username: '',
  password: '',
})

const templates = ref<any[]>([])
const tmplColumns = [
  { title: '模板名称', dataIndex: 'name', key: 'name' },
  { title: '模板编码', dataIndex: 'code', key: 'code', width: 150 },
  { title: '操作', key: 'action', width: 60 },
]

function saveSmtp() {
  message.loading('正在保存...', 0)
  request.post('/mail/config', smtp).then(() => {
    message.success('SMTP配置已保存')
  }).catch(() => {
    message.error('保存失败')
  })
}

async function testConnection() {
  message.loading('正在测试邮件连接...', 0)
  try {
    await request.post('/mail/test', smtp)
    message.success('邮件连接测试成功')
  } catch {
    message.error('邮件连接测试失败')
  }
}

async function fetchData() {
  try {
    const res = await request.get('/mail/config')
    if (res) {
      smtp.host = res.host || ''
      smtp.port = res.port || 465
      smtp.encryption = res.encryption || 'ssl'
      smtp.username = res.username || ''
      smtp.password = res.password || ''
    }
  } catch {
    message.error('获取配置失败')
  }

  try {
    const res = await request.get('/mail/templates')
    templates.value = res?.records || []
  } catch {
    templates.value = []
  }
}

function editTemplate(record: any) {
  message.info('编辑模板: ' + record.name)
}

onMounted(fetchData)
</script>
