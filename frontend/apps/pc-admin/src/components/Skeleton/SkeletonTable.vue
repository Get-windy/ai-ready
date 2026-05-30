<template>
  <div class="skeleton-table" aria-busy="true" aria-label="表格数据加载中">
    <!-- 表头区域 -->
    <div class="skeleton-table__header">
      <div
        v-for="col in columns"
        :key="'h-' + col"
        class="skeleton-bar skeleton-bar--header"
        :style="{ width: colWidth(col) }"
      />
    </div>
    <!-- 数据行区域 -->
    <div class="skeleton-table__body">
      <div v-for="row in rows" :key="row" class="skeleton-table__row">
        <div
          v-for="col in columns"
          :key="'c-' + col"
          class="skeleton-bar"
          :style="{ width: colWidth(col) }"
        />
      </div>
    </div>
    <!-- 分页区域 -->
    <div class="skeleton-table__pagination">
      <div class="skeleton-bar" style="width: 120px; height: 14px" />
      <div class="skeleton-bar" style="width: 200px; height: 14px" />
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  /** 行数，默认 5 */
  rows?: number
  /** 列数，默认 4 */
  columns?: number
}

withDefaults(defineProps<Props>(), {
  rows: 5,
  columns: 4
})

/** 模拟表格列宽变化，使骨架更自然 */
function colWidth(index: number): string {
  const widths = ['28%', '18%', '22%', '14%', '12%', '16%', '20%', '10%']
  return widths[index % widths.length]
}
</script>

<style scoped>
.skeleton-table {
  width: 100%;
  background: var(--color-bg-container, #fff);
  border-radius: var(--border-radius-lg, 8px);
  overflow: hidden;
}

/* 表头 */
.skeleton-table__header {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  background: var(--color-bg-layout, #fafafa);
  border-bottom: 1px solid var(--color-border-light, #f0f0f0);
}

.skeleton-bar--header {
  height: 20px;
}

/* 数据行主体 */
.skeleton-table__body {
  padding: 0;
}

.skeleton-table__row {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border-light, #f0f0f0);
  transition: background-color 0.2s;
}

.skeleton-table__row:last-child {
  border-bottom: none;
}

/* 分页 */
.skeleton-table__pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border-light, #f0f0f0);
  background: var(--color-bg-layout, #fafafa);
}

/* 骨架条基础样式 + 微光动画 */
.skeleton-bar {
  height: 16px;
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
  .skeleton-table__header,
  .skeleton-table__row {
    padding: 10px 12px;
    gap: 8px;
  }
  .skeleton-table__pagination {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
}
</style>
