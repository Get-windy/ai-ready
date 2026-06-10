<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import Card from '@/components/common/Card.vue'
import Button from '@/components/common/Button.vue'
import Field from '@/components/common/Field.vue'
import Select from '@/components/common/Select.vue'

const router = useRouter()

const settings = ref({
  serverUrl: '',
  defaultPrinter: '',
  autoPrint: false,
  retryCount: 3,
  retryInterval: 5,
  logLevel: 'info',
  notifyOnComplete: true,
  notifyOnFail: true
})

const authState = ref({
  isLoggedIn: false,
  username: '',
  tenantName: '',
  clientName: ''
})

const printers = ref<any[]>([])
const connectionStatus = ref<'connected' | 'disconnected' | 'connecting'>('disconnected')
const saving = ref(false)
const autoStartEnabled = ref(false)

onMounted(async () => {
  const savedSettings = await window.electronAPI.settings.get()
  settings.value = { ...settings.value, ...savedSettings }

  printers.value = await window.electronAPI.printers.getPrinters()

  connectionStatus.value = await window.electronAPI.connection.getStatus()

  // 加载认证状态
  const state = await window.electronAPI.auth.getAuthState()
  authState.value = {
    isLoggedIn: state.isLoggedIn,
    username: state.username || '',
    tenantName: state.tenantName || '',
    clientName: state.clientName || ''
  }

  // 加载开机自启设置
  autoStartEnabled.value = await window.electronAPI.app.getAutoStart()
})

const handleSave = async () => {
  saving.value = true
  try {
    await window.electronAPI.settings.save(settings.value)

    if (settings.value.serverUrl) {
      await window.electronAPI.connection.connect(settings.value.serverUrl)
    }

    message.success('设置已保存')
  } finally {
    saving.value = false
  }
}

const handleToggleAutoStart = async () => {
  autoStartEnabled.value = !autoStartEnabled.value
  await window.electronAPI.app.setAutoStart(autoStartEnabled.value)
  message.success(autoStartEnabled.value ? '已开启开机自启' : '已关闭开机自启')
}

const handleLogout = async () => {
  const serverUrl = await window.electronAPI.settings.getSetting('serverUrl') as string
  await window.electronAPI.auth.logout(serverUrl || '')
  message.success('已退出登录')
  router.push('/login')
}

const handleTestConnection = async () => {
  if (!settings.value.serverUrl) {
    message.warning('请输入服务器地址')
    return
  }
  
  connectionStatus.value = 'connecting'
  
  try {
    await window.electronAPI.connection.connect(settings.value.serverUrl)
    
    const status = await window.electronAPI.connection.getStatus()
    connectionStatus.value = status
    
    if (status === 'connected') {
      message.success('连接成功')
    } else {
      message.error('连接失败')
    }
  } catch (error) {
    connectionStatus.value = 'disconnected'
    message.error('连接失败: ' + (error?.message || error))
  }
}

const handleTestPrint = async () => {
  if (!settings.value.defaultPrinter) {
    message.warning('请选择默认打印机')
    return
  }
  
  try {
    await window.electronAPI.printers.testPrint(settings.value.defaultPrinter)
    message.success('测试打印已发送')
  } catch (error) {
    message.error('测试打印失败: ' + (error?.message || error))
  }
}

const handleDisconnect = async () => {
  await window.electronAPI.connection.disconnect()
  connectionStatus.value = 'disconnected'
}
</script>

<template>
  <div class="settings-page">
    <div class="page-header">
      <h1>设置</h1>
    </div>
    
    <div class="settings-content">
      <Card title="服务器连接" class="settings-card">
        <div class="connection-status">
          <span :class="['status-badge', connectionStatus]">
            {{ connectionStatus === 'connected' ? '已连接' : 
               connectionStatus === 'connecting' ? '连接中' : '未连接' }}
          </span>
        </div>
        
        <Field
          label="服务器地址"
          :value="settings.serverUrl"
          placeholder="例如: http://192.168.1.100:8080"
          @change="(val: string) => settings.serverUrl = val"
        />
        
        <div class="button-group">
          <Button type="primary" @click="handleTestConnection">测试连接</Button>
          <Button v-if="connectionStatus === 'connected'" @click="handleDisconnect">断开连接</Button>
        </div>
      </Card>
      
      <Card title="账号信息" class="settings-card">
        <div class="account-info">
          <div class="info-row">
            <span class="label">登录账号</span>
            <span class="value">{{ authState.username || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="label">所属租户</span>
            <span class="value">{{ authState.tenantName || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="label">客户端名称</span>
            <span class="value">{{ authState.clientName || '未配置' }}</span>
          </div>
          <div class="button-group">
            <Button @click="handleLogout">退出登录</Button>
          </div>
        </div>
      </Card>

      <Card title="启动设置" class="settings-card">
        <div class="toggle-row">
          <span class="label">开机自动启动</span>
          <label class="toggle-switch">
            <input
              type="checkbox"
              :checked="autoStartEnabled"
              @change="handleToggleAutoStart"
            />
            <span class="toggle-slider"></span>
          </label>
        </div>
        <div class="toggle-row">
          <span class="label">自动登录</span>
          <span class="value-hint">{{ authState.isLoggedIn ? '已启用（记住密码）' : '未启用' }}</span>
        </div>
        <p class="settings-hint">开启后，电脑开机时打印客户端将自动启动并连接服务器</p>
      </Card>

      <Card title="打印机设置" class="settings-card">
        <Select
          label="默认打印机"
          :value="settings.defaultPrinter"
          :options="printers.map(p => ({ value: p.name, label: p.name }))"
          placeholder="选择默认打印机"
          @change="(val: string) => settings.defaultPrinter = val"
        />
        
        <div class="button-group">
          <Button @click="handleTestPrint">测试打印</Button>
          <Button @click="router.push('/printers')">管理打印机</Button>
        </div>
      </Card>
      
      <Card title="打印选项" class="settings-card">
        <div class="toggle-row">
          <span class="label">自动打印</span>
          <input 
            type="checkbox"
            :checked="settings.autoPrint"
            @change="(e: any) => settings.autoPrint = e.target.checked"
          />
        </div>
        
        <Field
          label="重试次数"
          type="number"
          :value="settings.retryCount"
          @change="(val: number) => settings.retryCount = val"
        />
        
        <Field
          label="重试间隔(秒)"
          type="number"
          :value="settings.retryInterval"
          @change="(val: number) => settings.retryInterval = val"
        />
      </Card>
      
      <Card title="通知设置" class="settings-card">
        <div class="toggle-row">
          <span class="label">打印完成通知</span>
          <input 
            type="checkbox"
            :checked="settings.notifyOnComplete"
            @change="(e: any) => settings.notifyOnComplete = e.target.checked"
          />
        </div>
        
        <div class="toggle-row">
          <span class="label">打印失败通知</span>
          <input 
            type="checkbox"
            :checked="settings.notifyOnFail"
            @change="(e: any) => settings.notifyOnFail = e.target.checked"
          />
        </div>
      </Card>
      
      <Card title="日志设置" class="settings-card">
        <Select
          label="日志级别"
          :value="settings.logLevel"
          :options="[
            { value: 'error', label: '错误' },
            { value: 'warn', label: '警告' },
            { value: 'info', label: '信息' },
            { value: 'debug', label: '调试' }
          ]"
          @change="(val: string) => settings.logLevel = val"
        />
        
        <div class="button-group">
          <Button @click="router.push('/logs')">查看日志</Button>
        </div>
      </Card>
      
      <div class="save-section">
        <Button type="primary" size="large" :loading="saving" @click="handleSave">
          保存设置
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.settings-page {
  .page-header {
    margin-bottom: 24px;
    
    h1 {
      font-size: 24px;
      font-weight: 600;
      color: #333;
    }
  }
}

.settings-content {
  max-width: 600px;
  
  .settings-card {
    margin-bottom: 16px;
  }
}

.connection-status {
  padding: 12px 0;
  
  .status-badge {
    display: inline-flex;
    padding: 4px 12px;
    border-radius: 4px;
    font-size: 14px;
    
    &.connected {
      background: #e6f7ff;
      color: #1988fa;
    }
    
    &.connecting {
      background: #fff1e6;
      color: #ff976a;
    }
    
    &.disconnected {
      background: #ffe6e6;
      color: #f44;
    }
  }
}

.button-group {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

.toggle-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  
  .label {
    font-size: 14px;
    color: #333;
  }
  
  input[type="checkbox"] {
    width: 20px;
    height: 20px;
  }
}

.save-section {
  margin-top: 24px;
}

/* 账号信息 */
.account-info {
  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;

    .label {
      font-size: 14px;
      color: #969799;
    }

    .value {
      font-size: 14px;
      color: #333;
      font-weight: 500;
    }
  }
}

/* 开关样式 */
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;

  input {
    opacity: 0;
    width: 0;
    height: 0;
  }

  .toggle-slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: #dcdfe6;
    border-radius: 24px;
    transition: 0.3s;

    &::before {
      content: '';
      position: absolute;
      height: 18px;
      width: 18px;
      left: 3px;
      bottom: 3px;
      background-color: white;
      border-radius: 50%;
      transition: 0.3s;
    }
  }

  input:checked + .toggle-slider {
    background-color: #07c160;
  }

  input:checked + .toggle-slider::before {
    transform: translateX(20px);
  }
}

.value-hint {
  font-size: 13px;
  color: #969799;
}

.settings-hint {
  font-size: 12px;
  color: #c0c4cc;
  margin: 4px 0 0;
  line-height: 1.5;
}
</style>