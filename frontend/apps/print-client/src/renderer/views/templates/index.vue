<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import Card from '@/components/common/Card.vue'

const router = useRouter()

interface PrintTemplate {
  id: string
  name: string
  type: 'order' | 'invoice' | 'contract' | 'label' | 'report'
  description: string
  createTime: string
  updateTime: string
  isDefault: boolean
  previewUrl: string
}

const templates = ref<PrintTemplate[]>([])
const loading = ref(false)

const typeMap = {
  order: { label: '订单模板', color: '#1988fa' },
  invoice: { label: '发票模板', color: '#07c160' },
  contract: { label: '合同模板', color: '#ff976a' },
  label: { label: '标签模板', color: '#7232dd' },
  report: { label: '报表模板', color: '#969799' }
}

onMounted(async () => {
  loadTemplates()
})

const loadTemplates = async () => {
  loading.value = true
  
  try {
    const data = await window.electronAPI.templates.getTemplates()
    templates.value = data || [
      {
        id: '1',
        name: '销售订单模板',
        type: 'order',
        description: '用于打印销售订单',
        createTime: '2024-01-10',
        updateTime: '2024-01-15',
        isDefault: true,
        previewUrl: ''
      },
      {
        id: '2',
        name: '增值税发票模板',
        type: 'invoice',
        description: '用于打印增值税发票',
        createTime: '2024-01-10',
        updateTime: '2024-01-12',
        isDefault: false,
        previewUrl: ''
      },
      {
        id: '3',
        name: '销售合同模板',
        type: 'contract',
        description: '用于打印销售合同',
        createTime: '2024-01-10',
        updateTime: '2024-01-14',
        isDefault: false,
        previewUrl: ''
      },
      {
        id: '4',
        name: '商品标签模板',
        type: 'label',
        description: '用于打印商品标签',
        createTime: '2024-01-10',
        updateTime: '2024-01-13',
        isDefault: false,
        previewUrl: ''
      },
      {
        id: '5',
        name: '月度报表模板',
        type: 'report',
        description: '用于打印月度报表',
        createTime: '2024-01-10',
        updateTime: '2024-01-11',
        isDefault: false,
        previewUrl: ''
      }
    ]
  } finally {
    loading.value = false
  }
}

const handleEdit = (template: PrintTemplate) => {
  router.push(`/templates/${template.id}/edit`)
}

const handleDelete = async (template: PrintTemplate) => {
  if (confirm(`确定删除模板 "${template.name}"？`)) {
    try {
      await window.electronAPI.templates.deleteTemplate(template.id)
      templates.value = templates.value.filter(t => t.id !== template.id)
    } catch (err) {
      message.error('删除失败: ' + (err?.message || err))
    }
  }
}

const handleSetDefault = async (template: PrintTemplate) => {
  try {
    await window.electronAPI.templates.setDefault(template.id)
    templates.value.forEach(t => {
      t.isDefault = t.id === template.id
    })
  } catch (err) {
    message.error('设置失败: ' + (err?.message || err))
  }
}

const handleAddTemplate = () => {
  router.push('/templates/new')
}
</script>

<template>
  <div class="templates-page">
    <div class="page-header">
      <h1>打印模板</h1>
      <button class="add-btn" @click="handleAddTemplate">
        + 新增模板
      </button>
    </div>
    
    <div class="templates-list">
      <Card 
        v-for="template in templates"
        :key="template.id"
        class="template-card"
      >
        <template #header>
          <div class="card-header">
            <span class="template-name">{{ template.name }}</span>
            <span class="template-type" :style="{ color: typeMap[template.type].color }">
              {{ typeMap[template.type].label }}
            </span>
            <span v-if="template.isDefault" class="default-tag">
              默认
            </span>
          </div>
        </template>
        
        <div class="card-content">
          <div class="template-desc">{{ template.description }}</div>
          <div class="template-time">
            <span>创建: {{ template.createTime }}</span>
            <span>更新: {{ template.updateTime }}</span>
          </div>
        </div>
        
        <div class="card-actions">
          <button class="btn edit" @click="handleEdit(template)">
            编辑
          </button>
          <button 
            v-if="!template.isDefault"
            class="btn default"
            @click="handleSetDefault(template)"
          >
            设为默认
          </button>
          <button 
            v-if="!template.isDefault"
            class="btn delete"
            @click="handleDelete(template)"
          >
            删除
          </button>
        </div>
      </Card>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.templates-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    h1 {
      font-size: 20px;
      font-weight: 600;
      color: #333;
    }
    
    .add-btn {
      padding: 8px 16px;
      background: #1988fa;
      color: #fff;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      
      &:hover {
        background: #0e7cd3;
      }
    }
  }
}

.templates-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.template-card {
  .card-header {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .template-name {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
    
    .template-type {
      font-size: 12px;
    }
    
    .default-tag {
      padding: 2px 8px;
      background: #07c160;
      color: #fff;
      font-size: 12px;
      border-radius: 4px;
    }
  }
  
  .card-content {
    padding: 12px 0;
    
    .template-desc {
      color: #969799;
      margin-bottom: 8px;
    }
    
    .template-time {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: #969799;
    }
  }
  
  .card-actions {
    display: flex;
    gap: 8px;
    
    .btn {
      padding: 6px 12px;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;
      
      &.edit {
        background: #1988fa;
        color: #fff;
        
        &:hover {
          background: #0e7cd3;
        }
      }
      
      &.default {
        background: #07c160;
        color: #fff;
        
        &:hover {
          background: #06ad56;
        }
      }
      
      &.delete {
        background: #f44;
        color: #fff;
        
        &:hover {
          background: #d63030;
        }
      }
    }
  }
}
</style>