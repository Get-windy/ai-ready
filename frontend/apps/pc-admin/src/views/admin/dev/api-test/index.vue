<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>API测试</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">API测试</h2>
        </div>
      </div>
    </template>

    <a-row :gutter="16">
      <a-col :span="8">
        <a-card :bordered="false" title="请求配置">
          <a-form layout="vertical">
            <a-form-item label="请求方法">
              <a-select v-model:value="method">
                <a-select-option value="GET">GET</a-select-option>
                <a-select-option value="POST">POST</a-select-option>
                <a-select-option value="PUT">PUT</a-select-option>
                <a-select-option value="DELETE">DELETE</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="请求URL">
              <a-input v-model:value="url" placeholder="/api/..." />
            </a-form-item>
            <a-form-item label="请求头">
              <a-textarea v-model:value="headers" :rows="3" placeholder="Content-Type: application/json&#10;Authorization: Bearer ..." />
            </a-form-item>
            <a-form-item label="请求体">
              <a-textarea v-model:value="body" :rows="6" placeholder='{"key": "value"}' />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="sendRequest" :loading="sending" block>
                <template #icon><SendOutlined /></template>
                发送请求
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :span="16">
        <a-card :bordered="false" title="响应结果">
          <template #extra>
            <a-tag v-if="responseTime">{{ responseTime }}ms</a-tag>
            <a-tag v-if="statusCode" :color="statusCode < 400 ? 'green' : 'red'">{{ statusCode }}</a-tag>
          </template>
          <pre class="response-body">{{ response }}</pre>
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { SendOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const method = ref('GET')
const url = ref('/api/monitor/info')
const headers = ref('')
const body = ref('')
const sending = ref(false)
const response = ref('点击"发送请求"查看响应')
const statusCode = ref(0)
const responseTime = ref(0)

async function sendRequest() {
  if (!url.value) return
  sending.value = true
  const startTime = Date.now()

  try {
    const config: any = {
      method: method.value,
      url: url.value,
    }

    if (headers.value) {
      const headerObj: Record<string, string> = {}
      headers.value.split('\n').forEach(line => {
        const idx = line.indexOf(':')
        if (idx > 0) {
          headerObj[line.slice(0, idx).trim()] = line.slice(idx + 1).trim()
        }
      })
      config.headers = headerObj
    }

    if ((method.value === 'POST' || method.value === 'PUT') && body.value) {
      config.data = JSON.parse(body.value)
    }

    const res = await request(config)
    statusCode.value = 200
    response.value = JSON.stringify(res, null, 2)
  } catch (e: any) {
    statusCode.value = e?.response?.status || 500
    response.value = JSON.stringify(e?.response?.data || e.message || '请求失败', null, 2)
  } finally {
    responseTime.value = Date.now() - startTime
    sending.value = false
  }
}
</script>

<style scoped>
.response-body {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 4px;
  min-height: 400px;
  max-height: 600px;
  overflow: auto;
  font-size: 13px;
  line-height: 1.5;
  margin: 0;
}
</style>
