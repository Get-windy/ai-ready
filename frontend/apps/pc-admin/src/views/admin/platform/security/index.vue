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
            <a-breadcrumb-item>安全策略</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">
            安全策略
          </h2>
        </div>
        <div class="page-header-right">
          <a-button
            type="primary"
            size="small"
            @click="saveAll"
          >
            <template #icon>
              <SaveOutlined />
            </template>
            保存全部
          </a-button>
        </div>
      </div>
    </template>

    <a-row :gutter="16">
      <a-col :span="12">
        <a-card
          :bordered="false"
          title="登录安全"
        >
          <a-form layout="vertical">
            <a-form-item label="登录失败锁定阈值">
              <a-input-number
                v-model:value="loginPolicy.lockThreshold"
                :min="1"
                :max="20"
                style="width:120px"
              /> 次
            </a-form-item>
            <a-form-item label="锁定时间">
              <a-input-number
                v-model:value="loginPolicy.lockDuration"
                :min="1"
                :max="1440"
                style="width:120px"
              /> 分钟
            </a-form-item>
            <a-form-item label="验证码">
              <a-switch v-model:checked="loginPolicy.captchaEnabled" /> 启用登录验证码
            </a-form-item>
            <a-form-item label="双因素认证">
              <a-switch v-model:checked="loginPolicy.twoFactorEnabled" /> 启用双因素认证
            </a-form-item>
          </a-form>
        </a-card>

        <a-card
          :bordered="false"
          title="密码策略"
          style="margin-top:16px"
        >
          <a-form layout="vertical">
            <a-form-item label="最小长度">
              <a-input-number
                v-model:value="passwordPolicy.minLength"
                :min="6"
                :max="32"
                style="width:120px"
              /> 位
            </a-form-item>
            <a-form-item label="复杂度要求">
              <a-checkbox v-model:checked="passwordPolicy.requireUpper">
                包含大写字母
              </a-checkbox><br>
              <a-checkbox v-model:checked="passwordPolicy.requireLower">
                包含小写字母
              </a-checkbox><br>
              <a-checkbox v-model:checked="passwordPolicy.requireDigit">
                包含数字
              </a-checkbox><br>
              <a-checkbox v-model:checked="passwordPolicy.requireSpecial">
                包含特殊字符
              </a-checkbox>
            </a-form-item>
            <a-form-item label="密码有效期">
              <a-input-number
                v-model:value="passwordPolicy.expireDays"
                :min="0"
                :max="365"
                style="width:120px"
              /> 天（0=永不过期）
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <a-col :span="12">
        <a-card
          :bordered="false"
          title="访问控制"
        >
          <a-form layout="vertical">
            <a-form-item label="会话超时时间">
              <a-input-number
                v-model:value="accessPolicy.sessionTimeout"
                :min="5"
                :max="1440"
                style="width:120px"
              /> 分钟
            </a-form-item>
            <a-form-item label="单设备登录">
              <a-switch v-model:checked="accessPolicy.singleDevice" /> 限制单设备登录
            </a-form-item>
            <a-form-item label="IP白名单">
              <a-textarea
                v-model:value="accessPolicy.ipWhitelist"
                :rows="3"
                placeholder="每行一个IP地址或CIDR"
              />
            </a-form-item>
            <a-form-item label="接口访问频率限制">
              <a-input-number
                v-model:value="accessPolicy.rateLimit"
                :min="10"
                :max="10000"
                style="width:120px"
              /> 次/分钟
            </a-form-item>
          </a-form>
        </a-card>

        <a-card
          :bordered="false"
          title="审计日志"
          style="margin-top:16px"
        >
          <a-form layout="vertical">
            <a-form-item label="日志保留天数">
              <a-input-number
                v-model:value="auditPolicy.retentionDays"
                :min="7"
                :max="730"
                style="width:120px"
              /> 天
            </a-form-item>
            <a-form-item label="敏感操作审计">
              <a-switch v-model:checked="auditPolicy.logSensitiveOps" /> 记录敏感操作日志
            </a-form-item>
            <a-form-item label="登录日志">
              <a-switch v-model:checked="auditPolicy.logLogin" /> 记录登录日志
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </PageContainer>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SaveOutlined } from '@ant-design/icons-vue'
import request from '@/utils/request'

const loginPolicy = reactive({
  lockThreshold: 5,
  lockDuration: 30,
  captchaEnabled: true,
  twoFactorEnabled: false,
})

const passwordPolicy = reactive({
  minLength: 8,
  requireUpper: true,
  requireLower: true,
  requireDigit: true,
  requireSpecial: false,
  expireDays: 90,
})

const accessPolicy = reactive({
  sessionTimeout: 60,
  singleDevice: true,
  ipWhitelist: '',
  rateLimit: 1000,
})

const auditPolicy = reactive({
  retentionDays: 180,
  logSensitiveOps: true,
  logLogin: true,
})

async function fetchPolicy() {
  try {
    const res = await request.get('/system/security/policy')
    if (res) {
      if (res.lockThreshold !== undefined) loginPolicy.lockThreshold = res.lockThreshold
      if (res.lockDuration !== undefined) loginPolicy.lockDuration = res.lockDuration
      if (res.captchaEnabled !== undefined) loginPolicy.captchaEnabled = res.captchaEnabled
      if (res.twoFactorEnabled !== undefined) loginPolicy.twoFactorEnabled = res.twoFactorEnabled
      if (res.passwordMinLength !== undefined) passwordPolicy.minLength = res.passwordMinLength
      if (res.requireUpper !== undefined) passwordPolicy.requireUpper = res.requireUpper
      if (res.requireLower !== undefined) passwordPolicy.requireLower = res.requireLower
      if (res.requireDigit !== undefined) passwordPolicy.requireDigit = res.requireDigit
      if (res.requireSpecial !== undefined) passwordPolicy.requireSpecial = res.requireSpecial
      if (res.passwordExpireDays !== undefined) passwordPolicy.expireDays = res.passwordExpireDays
      if (res.sessionTimeout !== undefined) accessPolicy.sessionTimeout = res.sessionTimeout
      if (res.singleDevice !== undefined) accessPolicy.singleDevice = res.singleDevice
      if (res.ipWhitelist !== undefined) accessPolicy.ipWhitelist = res.ipWhitelist || ''
      if (res.rateLimit !== undefined) accessPolicy.rateLimit = res.rateLimit
      if (res.auditRetentionDays !== undefined) auditPolicy.retentionDays = res.auditRetentionDays
      if (res.logSensitiveOps !== undefined) auditPolicy.logSensitiveOps = res.logSensitiveOps
      if (res.logLogin !== undefined) auditPolicy.logLogin = res.logLogin
    }
  } catch {
    // 使用默认值
  }
}

async function saveAll() {
  message.loading('正在保存安全策略...', 0)
  try {
    const policy = {
      lockThreshold: loginPolicy.lockThreshold,
      lockDuration: loginPolicy.lockDuration,
      captchaEnabled: loginPolicy.captchaEnabled,
      twoFactorEnabled: loginPolicy.twoFactorEnabled,
      passwordMinLength: passwordPolicy.minLength,
      requireUpper: passwordPolicy.requireUpper,
      requireLower: passwordPolicy.requireLower,
      requireDigit: passwordPolicy.requireDigit,
      requireSpecial: passwordPolicy.requireSpecial,
      passwordExpireDays: passwordPolicy.expireDays,
      sessionTimeout: accessPolicy.sessionTimeout,
      singleDevice: accessPolicy.singleDevice,
      ipWhitelist: accessPolicy.ipWhitelist,
      rateLimit: accessPolicy.rateLimit,
      auditRetentionDays: auditPolicy.retentionDays,
      logSensitiveOps: auditPolicy.logSensitiveOps,
      logLogin: auditPolicy.logLogin,
    }
    await request.post('/system/security/policy/save', policy)
    message.success('安全策略已保存')
  } catch {
    message.error('保存失败')
  }
}

onMounted(fetchPolicy)
</script>
