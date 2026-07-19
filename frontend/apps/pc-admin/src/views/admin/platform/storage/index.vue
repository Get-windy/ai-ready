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
            <a-breadcrumb-item>存储配置</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            存储配置
          </h2>
        </div>
      </div>
    </template>

    <a-card
      :bordered="false"
      title="文件存储配置"
    >
      <a-form
        layout="vertical"
        style="max-width:600px"
      >
        <a-form-item label="存储方式">
          <a-radio-group v-model:value="config.storageType">
            <a-radio value="local">
              本地存储
            </a-radio>
            <a-radio value="aliyun">
              阿里云OSS
            </a-radio>
            <a-radio value="tencent">
              腾讯云COS
            </a-radio>
            <a-radio value="qiniu">
              七牛云Kodo
            </a-radio>
            <a-radio value="minio">
              MinIO
            </a-radio>
          </a-radio-group>
        </a-form-item>

        <template v-if="config.storageType === 'local'">
          <a-form-item label="存储路径">
            <a-input
              v-model:value="config.localPath"
              placeholder="/data/files"
            />
          </a-form-item>
          <a-form-item label="访问URL前缀">
            <a-input
              v-model:value="config.localUrlPrefix"
              placeholder="/uploads"
            />
          </a-form-item>
        </template>

        <template v-if="config.storageType !== 'local'">
          <a-form-item label="Endpoint">
            <a-input
              v-model:value="config.endpoint"
              :placeholder="config.storageType === 'aliyun' ? 'oss-cn-hangzhou.aliyuncs.com' : ''"
            />
          </a-form-item>
          <a-form-item label="Bucket">
            <a-input v-model:value="config.bucket" />
          </a-form-item>
          <a-form-item label="AccessKey">
            <a-input v-model:value="config.accessKey" />
          </a-form-item>
          <a-form-item label="AccessSecret">
            <a-input-password v-model:value="config.accessSecret" />
          </a-form-item>
        </template>

        <a-form-item>
          <a-button
            type="primary"
            @click="saveConfig"
          >
            保存配置
          </a-button>
          <a-button
            style="margin-left:8px"
            @click="testStorage"
          >
            测试连接
          </a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card
      :bordered="false"
      title="存储概览"
      style="margin-top:16px"
    >
      <a-row :gutter="16">
        <a-col :span="8">
          <a-statistic
            title="已用空间"
            :value="storageUsed"
          />
        </a-col>
        <a-col :span="8">
          <a-statistic
            title="文件总数"
            :value="fileCount"
          />
        </a-col>
        <a-col :span="8">
          <a-statistic
            title="今日上传"
            :value="todayUploads"
          />
        </a-col>
      </a-row>
    </a-card>
  </PageContainer>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import request from '@/utils/request'

const config = reactive({
  storageType: 'local',
  localPath: '/data/files',
  localUrlPrefix: '/uploads',
  endpoint: '',
  bucket: '',
  accessKey: '',
  accessSecret: '',
})

const storageUsed = '12.5 GB'
const fileCount = 45230
const todayUploads = 156

function saveConfig() {
  message.loading('正在保存...', 0)
  request.post('/storage/config', config).then(() => {
    message.success('存储配置已保存')
  }).catch(() => {
    message.error('保存失败')
  })
}

async function testStorage() {
  message.loading('正在测试存储连接...', 0)
  try {
    await request.post('/storage/test', config)
    message.success('存储连接测试成功')
  } catch {
    message.error('存储连接测试失败')
  }
}

async function fetchStorageConfig() {
  try {
    const res = await request.get('/storage/config')
    if (res) {
      config.storageType = res.storageType || 'local'
      config.localPath = res.localPath || '/data/files'
      config.localUrlPrefix = res.localUrlPrefix || '/uploads'
      config.endpoint = res.endpoint || ''
      config.bucket = res.bucket || ''
      config.accessKey = res.accessKey || ''
      config.accessSecret = res.accessSecret || ''
    }
  } catch {
    // 使用默认值
  }
}

onMounted(fetchStorageConfig)
</script>
