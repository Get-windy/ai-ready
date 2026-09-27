<template>
  <a-card
    title="账号绑定"
    :bordered="false"
    class="social-binding-card"
  >
    <a-alert
      type="info"
      show-icon
      class="binding-tip"
    >
      <template #message>
        绑定三方账号后，可直接用钉钉 / 企业微信 / 飞书扫码登录，并提升账号安全性。
      </template>
    </a-alert>

    <a-spin :spinning="loading">
      <div class="binding-list">
        <div
          v-for="item in bindings"
          :key="item.platform"
          class="binding-item"
        >
          <div class="binding-info">
            <span class="binding-name">{{ item.name }}</span>
            <span
              v-if="item.bound"
              class="binding-status is-bound"
            >已绑定{{ item.nickname ? '：' + item.nickname : '' }}</span>
            <span
              v-else-if="!item.configured"
              class="binding-status"
            >管理员未配置</span>
            <span
              v-else
              class="binding-status"
            >未绑定</span>
          </div>

          <a-button
            v-if="item.bound"
            size="small"
            danger
            :loading="acting === item.platform"
            @click="handleUnbind(item)"
          >
            解绑
          </a-button>
          <a-button
            v-else
            size="small"
            type="primary"
            :disabled="!item.configured"
            :loading="acting === item.platform"
            @click="handleBind(item)"
          >
            绑定
          </a-button>
        </div>

        <a-empty
          v-if="!loading && !bindings.length"
          description="暂无三方平台可用"
        />
      </div>
    </a-spin>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { socialApi, type SocialBindingStatus } from '@/api/social'

const loading = ref(false)
const acting = ref('')
const bindings = ref<SocialBindingStatus[]>([])

const load = async () => {
  loading.value = true
  try {
    const res: any = await socialApi.getBindings()
    bindings.value = res || []
  } catch (error: any) {
    message.error(error?.message || '加载绑定状态失败')
  } finally {
    loading.value = false
  }
}

/**
 * 绑定：整页跳转到三方授权页。
 * 授权完成后由后端 302 回到个人中心（bind-callback-url），因此此处无需处理返回值。
 */
const handleBind = async (item: SocialBindingStatus) => {
  acting.value = item.platform
  try {
    const url: any = await socialApi.getAuthorizeUrl(item.platform, 'bind')
    if (!url) {
      message.error('未取得授权地址')
      return
    }
    window.location.href = url
  } catch (error: any) {
    message.error(error?.message || `发起${item.name}绑定失败`)
  } finally {
    acting.value = ''
  }
}

const handleUnbind = (item: SocialBindingStatus) => {
  Modal.confirm({
    title: `解绑${item.name}`,
    content: `解绑后将无法再用${item.name}扫码登录，确定继续吗？`,
    okText: '确定解绑',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      acting.value = item.platform
      try {
        await socialApi.unbind(item.platform)
        message.success(`已解绑${item.name}`)
        await load()
      } catch (error: any) {
        message.error(error?.message || '解绑失败')
      } finally {
        acting.value = ''
      }
    }
  })
}

onMounted(() => {
  // 绑定成功时后端 302 回到本页并带 socialBind=ok
  const params = new URLSearchParams(window.location.search)
  if (params.get('socialBind') === 'ok') {
    message.success('绑定成功')
    params.delete('socialBind')
    const query = params.toString()
    window.history.replaceState({}, '', window.location.pathname + (query ? `?${query}` : ''))
  }
  load()
})
</script>

<style scoped>
.binding-tip {
  margin-bottom: 16px;
}

.binding-list {
  display: flex;
  flex-direction: column;
}

.binding-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.binding-item:last-child {
  border-bottom: none;
}

.binding-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.binding-name {
  font-size: 14px;
  color: #1a1a1a;
}

.binding-status {
  font-size: 12px;
  color: #8c8c8c;
}

.binding-status.is-bound {
  color: #52c41a;
}
</style>
