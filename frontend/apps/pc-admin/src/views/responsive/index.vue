<template>
  <div class="responsive-test">
    <a-page-header
      title="响应式布局测试"
      sub-title="测试不同终端的显示效果"
    />

    <a-card
      title="当前屏幕信息"
      style="margin-bottom: 20px"
    >
      <a-descriptions
        :column="{ xs: 1, sm: 2, md: 3, lg: 4 }"
        bordered
      >
        <a-descriptions-item label="屏幕宽度">
          {{ windowWidth }}px
        </a-descriptions-item>
        <a-descriptions-item label="当前断点">
          <a-tag :color="breakpointColor">
            {{ currentBreakpoint }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="设备类型">
          <a-tag :color="deviceColor">
            {{ deviceType }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="建议布局">
          {{ recommendedLayout }}
        </a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card
      title="响应式网格测试"
      style="margin-bottom: 20px"
    >
      <div class="responsive-grid">
        <div
          v-for="i in 8"
          :key="i"
          class="grid-item"
        >
          <a-card
            size="small"
            :title="`项目 ${i}`"
          >
            响应式网格项目
          </a-card>
        </div>
      </div>
    </a-card>

    <a-card
      title="响应式Flex测试"
      style="margin-bottom: 20px"
    >
      <div class="responsive-flex">
        <div
          v-for="i in 4"
          :key="i"
          class="flex-item"
        >
          <a-card size="small">
            项目 {{ i }}
          </a-card>
        </div>
      </div>
    </a-card>

    <a-card
      title="响应式文本测试"
      style="margin-bottom: 20px"
    >
      <p class="responsive-text-xs">
        小号文本（移动端12px，平板/PC端14px）
      </p>
      <p class="responsive-text-sm">
        常规文本（移动端14px，平板/PC端16px）
      </p>
      <p class="responsive-text-md">
        中等文本（移动端16px，平板/PC端18px）
      </p>
    </a-card>

    <a-card
      title="响应式间距测试"
      style="margin-bottom: 20px"
    >
      <div
        class="responsive-padding"
        style="background: #f0f0f0"
      >
        响应式内边距（移动端12px，平板端16px，PC端24px）
      </div>
      <div
        class="responsive-margin"
        style="margin-top: 20px; background: #e0e0e0"
      >
        响应式外边距（移动端12px，平板端16px，PC端24px）
      </div>
    </a-card>

    <a-card
      title="显示/隐藏测试"
      style="margin-bottom: 20px"
    >
      <a-row :gutter="[16, 16]">
        <a-col
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <div class="test-box">
            <span class="hidden-xs">仅在移动端隐藏</span>
          </div>
        </a-col>
        <a-col
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <div class="test-box">
            <span class="visible-xs">仅在移动端显示</span>
          </div>
        </a-col>
        <a-col
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <div class="test-box">
            <span class="hidden-lg">仅在PC端隐藏</span>
          </div>
        </a-col>
        <a-col
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
        >
          <div class="test-box">
            <span class="visible-lg">仅在PC端显示</span>
          </div>
        </a-col>
      </a-row>
    </a-card>

    <a-card
      title="响应式表格测试"
      style="margin-bottom: 20px"
    >
      <div class="responsive-table-container">
        <VxeTableList
          :columns="vxeColumns"
          :data-source="tableData"
          :pagination="false"
          row-key="key"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        />
      </div>
    </a-card>

    <a-card title="布局建议">
      <a-alert
        :message="layoutRecommendation"
        type="info"
        show-icon
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { useResponsive } from '@/composables/useResponsiveState'

const { windowWidth, currentBreakpoint, isMobileView, isTabletView, isDesktopView } = useResponsive()

const breakpointColor = computed(() => {
  const colors: Record<string, string> = {
    xs: 'red',
    sm: 'orange',
    md: 'yellow',
    lg: 'green',
    xl: 'blue',
    xxl: 'purple'
  }
  return colors[currentBreakpoint.value] || 'default'
})

const deviceColor = computed(() => {
  if (isMobileView.value) return 'red'
  if (isTabletView.value) return 'orange'
  return 'green'
})

const deviceType = computed(() => {
  if (isMobileView.value) return '移动端'
  if (isTabletView.value) return '平板'
  return 'PC端'
})

const recommendedLayout = computed(() => {
  if (isMobileView.value) return '单列布局，隐藏侧边栏'
  if (isTabletView.value) return '双列布局，紧凑侧边栏'
  return '多列布局，完整侧边栏'
})

const layoutRecommendation = computed(() => {
  if (isMobileView.value) {
    return '移动端建议：使用单列布局，启用抽屉式导航，优化触摸操作，使用较大的按钮和文本'
  }
  if (isTabletView.value) {
    return '平板端建议：使用双列布局，保持侧边栏可见但缩小宽度，调整字体和间距'
  }
  return 'PC端建议：使用多列布局，完整侧边栏，利用大屏幕空间展示更多信息'
})

const vxeColumns = [
  { field: 'name', title: '姓名', width: 120 },
  { field: 'age', title: '年龄', width: 100 },
  { field: 'address', title: '地址', width: 200 },
  { field: 'email', title: '邮箱', width: 200 },
  { field: 'phone', title: '电话', width: 150 },
  { field: 'note', title: '备注', width: 200 },
]

const tableData = Array.from({ length: 5 }, (_, i) => ({
  key: i,
  name: `用户 ${i + 1}`,
  age: 20 + i * 2,
  address: `地址 ${i + 1}`,
  email: `user${i + 1}@example.com`,
  phone: `1380013800${i}`,
  note: `备注信息 ${i + 1}`
}))
</script>

<style scoped>
.responsive-test {
  padding: 20px;
}

.grid-item,
.flex-item {
  height: 100%;
}

.test-box {
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  border-radius: 4px;
  padding: 8px;
}
</style>

<style>
/* 导入响应式样式 */
@import '@/styles/responsive.css';
</style>