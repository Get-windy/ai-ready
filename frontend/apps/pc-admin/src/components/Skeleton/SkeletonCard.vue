<template>
  <div
    class="skeleton-card"
    :class="{ 'skeleton-card--chart': hasChart }"
    aria-busy="true"
    aria-label="卡片内容加载中"
  >
    <!-- 标题区域 -->
    <div class="skeleton-card__header">
      <div class="skeleton-bar skeleton-card__title" />
      <div v-if="showExtra" class="skeleton-bar skeleton-card__extra" />
    </div>

    <!-- 数值/内容区域 -->
    <div class="skeleton-card__body">
      <div
        class="skeleton-bar skeleton-card__value"
        :style="{ height: hasChart ? '32px' : '36px' }"
      />
      <div v-if="showSuffix" class="skeleton-bar skeleton-card__suffix" />
    </div>

    <!-- 图表占位区域 -->
    <div v-if="hasChart" class="skeleton-card__chart">
      <div class="skeleton-card__chart-area">
        <!-- 模拟折线图 -->
        <svg class="skeleton-card__chart-svg" viewBox="0 0 400 120" preserveAspectRatio="none">
          <polyline
            points="0,80 60,60 120,90 180,30 240,50 300,20 360,45 400,35"
            fill="none"
            stroke="var(--color-border-light, #e8e8e8)"
            stroke-width="2"
          />
        </svg>
      </div>
      <!-- X 轴标签占位 -->
      <div class="skeleton-card__chart-labels">
        <div v-for="i in 5" :key="i" class="skeleton-bar" style="width: 28px; height: 10px" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  /** 是否显示图表占位区域 */
  hasChart?: boolean
  /** 是否显示右上角 extra 占位 */
  showExtra?: boolean
  /** 是否显示后缀（趋势箭头等）占位 */
  showSuffix?: boolean
}

withDefaults(defineProps<Props>(), {
  hasChart: false,
  showExtra: false,
  showSuffix: false
})
</script>

<style scoped>
.skeleton-card {
  padding: 20px 24px;
  background: var(--color-bg-container, #fff);
  border-radius: var(--border-radius-lg, 8px);
  border: 1px solid var(--color-border-light, #f0f0f0);
  transition: box-shadow 0.3s;
}

.skeleton-card--chart {
  min-height: 320px;
}

/* 标题区域 */
.skeleton-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.skeleton-card__title {
  height: 16px;
  width: 45%;
}

.skeleton-card__extra {
  height: 14px;
  width: 60px;
}

/* 数值区域 */
.skeleton-card__body {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 8px;
}

.skeleton-card__value {
  width: 50%;
  border-radius: var(--border-radius-base, 4px);
}

.skeleton-card__suffix {
  height: 18px;
  width: 40px;
}

/* 图表区域 */
.skeleton-card__chart {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-light, #f0f0f0);
}

.skeleton-card__chart-area {
  height: 140px;
  margin-bottom: 12px;
  background: linear-gradient(
    180deg,
    var(--color-bg-layout, #f5f5f5) 0%,
    var(--color-bg-container, #fff) 100%
  );
  border-radius: var(--border-radius-base, 4px);
  position: relative;
  overflow: hidden;
}

.skeleton-card__chart-svg {
  width: 100%;
  height: 100%;
}

.skeleton-card__chart-labels {
  display: flex;
  justify-content: space-between;
  padding: 0 4px;
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
  .skeleton-card {
    padding: 16px;
  }

  .skeleton-card__chart-area {
    height: 100px;
  }
}
</style>
