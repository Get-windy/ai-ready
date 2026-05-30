<template>
  <div class="skeleton-dashboard" aria-busy="true" aria-label="仪表盘数据加载中">
    <!-- ===== 欢迎栏 ===== -->
    <div class="skeleton-dashboard__header">
      <div class="skeleton-dashboard__header-left">
        <div class="skeleton-bar" style="width: 120px; height: 28px; margin-bottom: 8px" />
        <div class="skeleton-bar" style="width: 220px; height: 16px" />
      </div>
      <div class="skeleton-dashboard__header-right">
        <div class="skeleton-bar" style="width: 220px; height: 24px" />
        <div class="skeleton-bar" style="width: 80px; height: 24px" />
      </div>
    </div>

    <!-- ===== KPI 卡片行 ===== -->
    <div class="skeleton-dashboard__kpis">
      <SkeletonCard v-for="i in 4" :key="'kpi-' + i" show-suffix />
    </div>

    <!-- ===== 图表 + 待办行 ===== -->
    <div class="skeleton-dashboard__content-row">
      <!-- 图表卡片 -->
      <div class="skeleton-dashboard__chart-card">
        <SkeletonCard has-chart show-extra />
      </div>
      <!-- 待办列表卡片 -->
      <div class="skeleton-dashboard__todo-card">
        <div class="skeleton-dashboard__card">
          <div class="skeleton-dashboard__card-header">
            <div class="skeleton-bar" style="width: 80px; height: 18px" />
            <div class="skeleton-bar" style="width: 32px; height: 14px" />
          </div>
          <div class="skeleton-dashboard__card-body">
            <div
              v-for="i in 5"
              :key="'todo-' + i"
              class="skeleton-dashboard__todo-item"
            >
              <div class="skeleton-bar" style="width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0" />
              <div class="skeleton-dashboard__todo-content">
                <div class="skeleton-bar" :style="{ width: (70 - i * 5) + '%', height: '14px' }" />
                <div class="skeleton-bar" style="width: 30%; height: 12px; margin-top: 4px" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 快速入口 + 库存预警行 ===== -->
    <div class="skeleton-dashboard__bottom-row">
      <!-- 快速入口 -->
      <div class="skeleton-dashboard__quick-card">
        <div class="skeleton-dashboard__card">
          <div class="skeleton-dashboard__card-header">
            <div class="skeleton-bar" style="width: 80px; height: 18px" />
          </div>
          <div class="skeleton-dashboard__quick-grid">
            <div v-for="i in 6" :key="'quick-' + i" class="skeleton-dashboard__quick-item">
              <div class="skeleton-bar" style="width: 32px; height: 32px; border-radius: 50%" />
              <div class="skeleton-bar" style="width: 48px; height: 12px; margin-top: 8px" />
            </div>
          </div>
        </div>
      </div>

      <!-- 库存预警表 -->
      <div class="skeleton-dashboard__alert-card">
        <div class="skeleton-dashboard__card">
          <div class="skeleton-dashboard__card-header">
            <div class="skeleton-bar" style="width: 80px; height: 18px" />
            <div class="skeleton-bar" style="width: 28px; height: 20px; border-radius: 10px" />
          </div>
          <div class="skeleton-dashboard__card-body">
            <!-- 表头 -->
            <div class="skeleton-dashboard__alert-header">
              <div class="skeleton-bar" style="width: 80px; height: 14px" />
              <div class="skeleton-bar" style="width: 100px; height: 14px" />
              <div class="skeleton-bar" style="width: 50px; height: 14px" />
              <div class="skeleton-bar" style="width: 50px; height: 14px" />
              <div class="skeleton-bar" style="width: 40px; height: 14px" />
            </div>
            <!-- 数据行 -->
            <div
              v-for="i in 4"
              :key="'alert-' + i"
              class="skeleton-dashboard__alert-row"
            >
              <div class="skeleton-bar" style="width: 70px; height: 14px" />
              <div class="skeleton-bar" style="width: 90px; height: 14px" />
              <div class="skeleton-bar" style="width: 36px; height: 14px" />
              <div class="skeleton-bar" style="width: 36px; height: 14px" />
              <div class="skeleton-bar" style="width: 36px; height: 20px; border-radius: 2px" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import SkeletonCard from './SkeletonCard.vue'
</script>

<style scoped>
.skeleton-dashboard {
  padding: 24px;
  background-color: var(--color-bg-layout, #f0f2f5);
  min-height: 100%;
}

/* ===== 欢迎栏 ===== */
.skeleton-dashboard__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.skeleton-dashboard__header-left {
  display: flex;
  flex-direction: column;
}

.skeleton-dashboard__header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

/* ===== KPI 行 ===== */
.skeleton-dashboard__kpis {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

/* ===== 图表 + 待办行 ===== */
.skeleton-dashboard__content-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

/* ===== 底部行：快速入口 + 库存预警 ===== */
.skeleton-dashboard__bottom-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

/* ===== 通用卡片容器 ===== */
.skeleton-dashboard__card {
  background: var(--color-bg-container, #fff);
  border-radius: var(--border-radius-lg, 8px);
  border: 1px solid var(--color-border-light, #f0f0f0);
  overflow: hidden;
}

.skeleton-dashboard__card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--color-border-light, #f0f0f0);
}

.skeleton-dashboard__card-body {
  padding: 16px 24px;
}

/* ===== 待办项 ===== */
.skeleton-dashboard__todo-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border-light, #f0f0f0);
}

.skeleton-dashboard__todo-item:last-child {
  border-bottom: none;
}

.skeleton-dashboard__todo-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* ===== 快速入口 ===== */
.skeleton-dashboard__quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding-top: 0;
}

.skeleton-dashboard__quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  background: var(--color-bg-layout, #fafafa);
  border-radius: var(--border-radius-lg, 8px);
}

/* ===== 库存预警表 ===== */
.skeleton-dashboard__alert-header {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border-light, #f0f0f0);
}

.skeleton-dashboard__alert-row {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border-light, #f0f0f0);
  align-items: center;
}

.skeleton-dashboard__alert-row:last-child {
  border-bottom: none;
}

/* ===== 骨架条基础样式 + 微光动画 ===== */
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

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .skeleton-dashboard__content-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .skeleton-dashboard {
    padding: 16px;
  }

  .skeleton-dashboard__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .skeleton-dashboard__kpis {
    grid-template-columns: repeat(2, 1fr);
  }

  .skeleton-dashboard__content-row {
    grid-template-columns: 1fr;
  }

  .skeleton-dashboard__bottom-row {
    grid-template-columns: 1fr;
  }

  .skeleton-dashboard__quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .skeleton-dashboard__alert-header,
  .skeleton-dashboard__alert-row {
    gap: 6px;
  }
}

@media (max-width: 480px) {
  .skeleton-dashboard__kpis {
    grid-template-columns: 1fr;
  }

  .skeleton-dashboard__quick-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
