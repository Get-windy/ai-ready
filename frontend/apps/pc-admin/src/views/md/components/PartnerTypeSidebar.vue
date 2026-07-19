<template>
  <div class="partner-sidebar-panel">
    <div class="sidebar-menu">
      <div
        v-for="item in menuItems"
        :key="item.key"
        class="sidebar-menu-item"
        :class="{ active: activeKey === item.key }"
        @click="handleSelect(item.key)"
      >
        <component
          :is="item.icon"
          class="menu-icon"
        />
        <span class="menu-text">{{ item.label }}</span>
      </div>
    </div>
    <div class="sidebar-footer">
      <a-button
        type="link"
        block
        size="small"
        @click="handleAdd"
      >
        <template #icon>
          <PlusOutlined />
        </template>
        添加
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { PlusOutlined, TeamOutlined, ShopOutlined, CarOutlined, AppstoreOutlined, FileTextOutlined } from '@ant-design/icons-vue'

const props = withDefaults(defineProps<{
  activeKey?: string
}>(), {
  activeKey: ''
})

const router = useRouter()
const route = useRoute()

const menuItems = [
  { key: 'template', label: '模板', icon: FileTextOutlined, path: '/md/partner' },
  { key: 'customer', label: '客户', icon: TeamOutlined, path: '/md/customer' },
  { key: 'supplier', label: '供应商', icon: ShopOutlined, path: '/md/supplier' },
  { key: 'logistics', label: '物流公司', icon: CarOutlined, path: '/md/logistics' },
  { key: 'partner', label: '其他往来单位', icon: AppstoreOutlined, path: '/md/partner' },
]

const currentKey = computed(() => {
  if (props.activeKey) return props.activeKey
  const path = route.path
  if (path.includes('/md/customer')) return 'customer'
  if (path.includes('/md/supplier')) return 'supplier'
  if (path.includes('/md/logistics')) return 'logistics'
  if (path.includes('/md/partner')) return 'partner'
  return 'template'
})

function handleSelect(key: string) {
  const item = menuItems.find(m => m.key === key)
  if (item && item.path) {
    router.push(item.path)
  }
}

function handleAdd() {
  // 根据当前选中的类型，打开对应的新增表单
  const key = currentKey.value
  const pathMap: Record<string, string> = {
    customer: '/md/customer',
    supplier: '/md/supplier',
    logistics: '/md/logistics',
    partner: '/md/partner',
    template: '/md/partner',
  }
  router.push(pathMap[key] || '/md/partner')
}
</script>

<style scoped>
.partner-sidebar-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-right: 1px solid #f0f0f0;
  width: 160px;
  min-width: 160px;
}

.sidebar-menu {
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
}

.sidebar-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  color: #595959;
  border-left: 3px solid transparent;
}

.sidebar-menu-item:hover {
  background: #f5f5f5;
  color: #262626;
}

.sidebar-menu-item.active {
  background: #e6f7ff;
  color: #1890ff;
  border-left-color: #1890ff;
  font-weight: 500;
}

.menu-icon {
  font-size: 14px;
}

.menu-text {
  white-space: nowrap;
}

.sidebar-footer {
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
}
</style>
