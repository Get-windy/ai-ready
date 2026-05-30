<template>
  <div class="skeleton-form" aria-busy="true" aria-label="表单加载中">
    <div v-for="i in fields" :key="i" class="skeleton-form__field">
      <!-- Label 占位 -->
      <div class="skeleton-bar skeleton-form__label" />
      <!-- Input 占位 -->
      <div class="skeleton-bar skeleton-form__input" />
    </div>

    <!-- 底部按钮占位 -->
    <div class="skeleton-form__actions">
      <div class="skeleton-bar skeleton-form__btn" />
      <div class="skeleton-bar skeleton-form__btn" />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  /** 表单字段数量，默认 4 */
  fields?: number
  /** 是否显示底部按钮占位 */
  showActions?: boolean
}

withDefaults(defineProps<Props>(), {
  fields: 4,
  showActions: true
})
</script>

<style scoped>
.skeleton-form {
  padding: 8px 0;
}

/* 单个字段 */
.skeleton-form__field {
  margin-bottom: 24px;
}

.skeleton-form__label {
  height: 14px;
  width: 20%;
  margin-bottom: 8px;
}

.skeleton-form__input {
  height: 40px;
  width: 100%;
}

/* 底部按钮区域 */
.skeleton-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-light, #f0f0f0);
}

.skeleton-form__btn {
  height: 36px;
  width: 80px;
}

/* 骨架条基础样式 + 微光动画 */
.skeleton-bar {
  border-radius: var(--border-radius-base, 4px);
  background: linear-gradient(
    90deg,
    var(--color-bg-layout, #f5f5f5) 25%,
    var(--color-border-light, #e8e8e8) 37%,
    var(--color-bg-layout, #f5f5f5) 63%
  );
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

@keyframes shimmer {
  0% {
    background-position: -468px 0;
  }
  100% {
    background-position: 468px 0;
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .skeleton-form__field {
    margin-bottom: 18px;
  }

  .skeleton-form__input {
    height: 36px;
  }

  .skeleton-form__actions {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .skeleton-form__btn {
    width: 100%;
  }
}
</style>
