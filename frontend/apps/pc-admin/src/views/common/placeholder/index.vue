<template>
  <PageContainer full-height>
    <div class="placeholder-page">
      <div class="placeholder-icon">
        <BuildOutlined />
      </div>
      <h3>{{ pageTitle }}</h3>
      <p>功能开发中，敬请期待...</p>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { BuildOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'

const route = useRoute()
const pageTitle = computed(() => (route.meta?.title as string) || '开发中')
</script>

<style scoped>
.placeholder-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #c0c4cc;
}

.placeholder-page h3 {
  font-size: 18px;
  color: #606266;
  margin: 0 0 8px 0;
}

.placeholder-page p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}
</style>
