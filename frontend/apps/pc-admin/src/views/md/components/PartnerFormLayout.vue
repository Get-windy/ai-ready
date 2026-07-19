<template>
  <div class="partner-form-layout">
    <PartnerTypeSidebar :active-key="activeKey" />
    <div class="partner-form-content">
      <!-- 顶部标题栏 -->
      <div class="content-header">
        <div class="header-left">
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">
                首页
              </router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item>{{ pageTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-title">
            {{ pageTitle }}
          </h2>
        </div>
        <div class="header-right">
          <a-dropdown>
            <a-button
              type="primary"
              size="small"
            >
              新增
              <DownOutlined />
            </a-button>
            <template #overlay>
              <a-menu @click="handleAddMenuClick">
                <a-menu-item key="customer">
                  新增客户
                </a-menu-item>
                <a-menu-item key="supplier">
                  新增供应商
                </a-menu-item>
                <a-menu-item key="logistics">
                  新增物流公司
                </a-menu-item>
                <a-menu-item key="partner">
                  新增其他往来单位
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>

      <!-- 表单内容区 -->
      <div class="content-body">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { DownOutlined } from '@ant-design/icons-vue'
import PartnerTypeSidebar from './PartnerTypeSidebar.vue'

defineProps<{
  activeKey: string
  pageTitle: string
}>()

const router = useRouter()

const pathMap: Record<string, string> = {
  customer: '/md/customer',
  supplier: '/md/supplier',
  logistics: '/md/logistics',
  partner: '/md/partner',
}

function handleAddMenuClick(info: { key: string | number }) {
  router.push(pathMap[String(info.key)] || '/md/partner')
}
</script>

<style scoped>
.partner-form-layout {
  display: flex;
  height: 100%;
  background: #f5f7fa;
}

.partner-form-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.page-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}
</style>
