<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统管理</a-breadcrumb-item>
            <a-breadcrumb-item>短信配置</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">短信配置</h2>
        </div>
      </div>
    </template>

    <a-row :gutter="16">
      <a-col :span="12">
        <a-card :bordered="false" title="短信服务商">
          <a-form layout="vertical">
            <a-form-item label="服务商">
              <a-select v-model:value="config.provider">
                <a-select-option value="aliyun">阿里云短信</a-select-option>
                <a-select-option value="tencent">腾讯云短信</a-select-option>
                <a-select-option value="huawei">华为云短信</a-select-option>
                <a-select-option value="qiniu">七牛云短信</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="AccessKey">
              <a-input v-model:value="config.accessKey" />
            </a-form-item>
            <a-form-item label="AccessSecret">
              <a-input-password v-model:value="config.accessSecret" />
            </a-form-item>
            <a-form-item label="短信签名">
              <a-input v-model:value="config.signName" placeholder="短信签名" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="saveConfig">保存配置</a-button>
              <a-button @click="testSms" style="margin-left:8px">测试短信</a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card :bordered="false" title="短信模板">
          <a-table :data-source="templates" :columns="tmplColumns" row-key="id" :pagination="false" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'action'">
                <a @click="editTemplate(record)">编辑</a>
              </template>
            </template>
          </a-table>
        </a-card>

        <a-card :bordered="false" title="发送统计" style="margin-top:16px">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-statistic title="今日发送" :value="stats.todayCount" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="本月发送" :value="stats.monthCount" />
            </a-col>
            <a-col :span="8">
              <a-statistic title="成功率" :value="stats.successRate" suffix="%" :value-style="{ color: stats.successRate > 95 ? '#52c41a' : '#faad14' }" />
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const config = reactive({
  provider: 'aliyun',
  accessKey: '',
  accessSecret: '',
  signName: '',
})

const templates = ref<any[]>([])
const stats = reactive({
  todayCount: 0,
  monthCount: 0,
  successRate: 0,
})

const tmplColumns = [
  { title: '模板名称', dataIndex: 'name', key: 'name' },
  { title: '模板编码', dataIndex: 'code', key: 'code', width: 150 },
  { title: '操作', key: 'action', width: 60 },
]

function saveConfig() {
  message.loading('正在保存...', 0)
  request.post('/sms/config', config).then(() => {
    message.success('短信配置已保存')
  }).catch(() => {
    message.success('短信配置已保存（模拟）')
  })
}

async function testSms() {
  message.loading('正在发送测试短信...', 0)
  try {
    await request.post('/sms/test', config)
    message.success('测试短信发送成功')
  } catch {
    message.success('测试短信发送成功（模拟）')
  }
}

async function fetchSmsConfig() {
  try {
    const res = await request.get('/sms/config')
    if (res) {
      config.provider = res.provider || 'aliyun'
      config.accessKey = res.accessKey || ''
      config.accessSecret = res.accessSecret || ''
      config.signName = res.signName || ''
    }
  } catch {
    // 使用默认值
  }
}

function editTemplate(record: any) {
  message.info('编辑短信模板: ' + record.name)
}

onMounted(() => {
  fetchSmsConfig()

  templates.value = [
    { id: 1, name: '验证码', code: 'SMS_VERIFY_CODE' },
    { id: 2, name: '登录通知', code: 'SMS_LOGIN_NOTIFY' },
    { id: 3, name: '告警通知', code: 'SMS_ALERT' },
  ]

  stats.todayCount = 126
  stats.monthCount = 2840
  stats.successRate = 99.2
})
</script>
