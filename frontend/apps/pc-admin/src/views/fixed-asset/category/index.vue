<template>
  <div class="category-list">
    <a-row :gutter="16">
      <a-col :span="10">
        <a-card title="分类树">
          <template #extra>
            <a-button type="primary" size="small" @click="showAddRootModal">添加根分类</a-button>
          </template>
          <a-tree
            v-if="treeData.length > 0"
            :tree-data="treeData"
            :default-expand-all="true"
            @select="onSelect"
          />
          <a-empty v-else description="暂无分类数据" />
        </a-card>
      </a-col>
      <a-col :span="14">
        <a-card :title="selectedCategory ? '分类详情' : '选择分类'">
          <template v-if="selectedCategory">
            <a-descriptions :column="1" bordered :label-style="{ fontWeight: 'bold' }">
              <a-descriptions-item label="分类编码">{{ selectedCategory.categoryCode }}</a-descriptions-item>
              <a-descriptions-item label="分类名称">{{ selectedCategory.categoryName }}</a-descriptions-item>
              <a-descriptions-item label="排序">{{ selectedCategory.sortOrder }}</a-descriptions-item>
              <a-descriptions-item label="默认折旧方法">{{ methodMap[selectedCategory.defaultDepreciationMethod] }}</a-descriptions-item>
              <a-descriptions-item label="默认使用年限(月)">{{ selectedCategory.defaultUsefulLife }}</a-descriptions-item>
              <a-descriptions-item label="描述">{{ selectedCategory.description }}</a-descriptions-item>
            </a-descriptions>
            <a-space style="margin-top: 16px">
              <a-button type="primary" @click="showEditModal">编辑</a-button>
              <a-button @click="showAddChildModal">添加子分类</a-button>
              <a-popconfirm title="确认删除?" @confirm="handleDelete">
                <a-button danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
          <a-empty v-else description="请在左侧选择一个分类" />
        </a-card>
      </a-col>
    </a-row>

    <!-- Category Form Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
      :confirmLoading="modalLoading"
    >
      <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="分类编码">
          <a-input v-model:value="formData.categoryCode" placeholder="分类编码" />
        </a-form-item>
        <a-form-item label="分类名称" required>
          <a-input v-model:value="formData.categoryName" placeholder="分类名称" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="formData.sortOrder" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="默认折旧方法">
          <a-select v-model:value="formData.defaultDepreciationMethod" placeholder="选择折旧方法">
            <a-select-option value="straight_line">直线法</a-select-option>
            <a-select-option value="double_declining">双倍余额递减法</a-select-option>
            <a-select-option value="sum_of_years">年数总和法</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="默认使用年限(月)">
          <a-input-number v-model:value="formData.defaultUsefulLife" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="formData.description" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { fixedAssetCategoryApi } from '@/api/fixed-asset'
import { message } from 'ant-design-vue'

interface Category {
  id: number
  categoryCode: string
  categoryName: string
  parentId: number
  sortOrder: number
  defaultDepreciationMethod: string
  defaultUsefulLife: number
  description: string
}

const treeData = ref<any[]>([])
const selectedCategory = ref<Category | null>(null)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const isAddChild = ref(false)
const editId = ref<number | null>(null)

const formData = reactive({
  categoryCode: '',
  categoryName: '',
  parentId: undefined as number | undefined,
  sortOrder: 0,
  defaultDepreciationMethod: 'straight_line',
  defaultUsefulLife: 60,
  description: '',
})

const methodMap: Record<string, string> = {
  straight_line: '直线法', double_declining: '双倍余额递减法', sum_of_years: '年数总和法',
}

const modalTitle = computed(() => {
  if (isEdit.value) return '编辑分类'
  if (isAddChild.value) return '添加子分类'
  return '添加根分类'
})

onMounted(() => {
  fetchTree()
})

function fetchTree() {
  fixedAssetCategoryApi.getTree().then((res: any) => {
    treeData.value = res.data || []
  })
}

function onSelect(keys: any[], info: any) {
  if (keys.length > 0) {
    const id = Number(keys[0])
    fixedAssetCategoryApi.getById(id).then((res: any) => {
      selectedCategory.value = res.data
    })
  }
}

function showAddRootModal() {
  isEdit.value = false
  isAddChild.value = false
  editId.value = null
  Object.assign(formData, {
    categoryCode: '',
    categoryName: '',
    parentId: undefined,
    sortOrder: 0,
    defaultDepreciationMethod: 'straight_line',
    defaultUsefulLife: 60,
    description: '',
  })
  modalVisible.value = true
}

function showAddChildModal() {
  if (!selectedCategory.value) return
  isEdit.value = false
  isAddChild.value = true
  editId.value = null
  Object.assign(formData, {
    categoryCode: '',
    categoryName: '',
    parentId: selectedCategory.value.id,
    sortOrder: 0,
    defaultDepreciationMethod: selectedCategory.value.defaultDepreciationMethod || 'straight_line',
    defaultUsefulLife: selectedCategory.value.defaultUsefulLife || 60,
    description: '',
  })
  modalVisible.value = true
}

function showEditModal() {
  if (!selectedCategory.value) return
  isEdit.value = true
  isAddChild.value = false
  editId.value = selectedCategory.value.id
  Object.assign(formData, selectedCategory.value)
  modalVisible.value = true
}

function handleModalOk() {
  modalLoading.value = true
  const apiCall = isEdit.value && editId.value
    ? fixedAssetCategoryApi.update(editId.value, formData)
    : fixedAssetCategoryApi.create(formData)

  apiCall.then(() => {
    message.success(isEdit.value ? '更新成功' : '创建成功')
    modalVisible.value = false
    fetchTree()
    selectedCategory.value = null
  }).catch((err: any) => {
    message.error(err.message || '操作失败')
  }).finally(() => {
    modalLoading.value = false
  })
}

function handleDelete() {
  if (!selectedCategory.value) return
  fixedAssetCategoryApi.delete(selectedCategory.value.id).then(() => {
    message.success('删除成功')
    selectedCategory.value = null
    fetchTree()
  }).catch((err: any) => {
    message.error(err.message || '删除失败')
  })
}
</script>
