<template>
  <!-- 权限预览横幅：只在模拟开启时出现，且必须一眼可辨 —— 否则管理员会误以为
       「自己看不到某菜单」是系统故障，而实际上是在以他人身份预览 -->
  <div
    v-if="status.simulating"
    class="simulation-banner"
  >
    <div class="simulation-banner__inner">
      <EyeOutlined class="simulation-banner__icon" />
      <span class="simulation-banner__text">
        正在以 <b>{{ targetName }}</b> 的权限视角查看
        <span
          v-if="status.reason"
          class="simulation-banner__reason"
        >（{{ status.reason }}）</span>
      </span>
      <a-button
        size="small"
        type="primary"
        ghost
        :loading="loading"
        @click="handleStop"
      >
        结束预览
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 权限预览横幅（全局，挂在主布局内容区上方）
 *
 * 状态来自 useSimulation 的模块级单例，任何页面调用 start() 后本组件自动出现。
 */
import { computed, onMounted } from 'vue'
import { EyeOutlined } from '@ant-design/icons-vue'
import { useSimulation } from '@/composables/useSimulation'

const { status, loading, targetLabel, refresh, stop } = useSimulation()

/**
 * 横幅上显示「在模拟谁」。
 * 拿到用户名就显示「张三（#12）」；刷新页面后只剩服务端会话里的 ID，
 * 就退回显示 ID —— 不猜、不编造姓名。
 */
const targetName = computed(() => {
  const id = status.value.targetUserId
  if (targetLabel.value) return id == null ? targetLabel.value : `${targetLabel.value}（#${id}）`
  return id == null ? '未知用户' : `用户 #${id}`
})

// 刷新页面后模拟态仍在服务端会话里，这里补一次查询以恢复横幅
onMounted(() => {
  void refresh()
})

async function handleStop() {
  try {
    await stop()
  } catch {
    // 失败已在 composable 内提示；保留横幅以便重试
  }
}
</script>

<style scoped>
.simulation-banner {
  margin-bottom: 8px;
  padding: 8px 16px;
  color: #ad4e00;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 6px;
}

.simulation-banner__inner {
  display: flex;
  align-items: center;
  gap: 8px;
}

.simulation-banner__icon {
  font-size: 16px;
  color: #fa8c16;
}

.simulation-banner__text {
  flex: 1;
  font-size: 13px;
}

.simulation-banner__reason {
  color: #8c8c8c;
}
</style>
