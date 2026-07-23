<template>
  <ErrorBoundary>
    <PageContainer title="商品辅助资料">
      <div class="search-area">
        <div class="search-row">
          <div class="search-item">
            <span class="search-label">资料类型</span>
            <a-select
              v-model:value="searchParams.auxType"
              placeholder="请选择资料类型"
              allow-clear
              style="width: 180px"
            >
              <a-select-option value="品牌">品牌</a-select-option>
              <a-select-option value="产地">产地</a-select-option>
              <a-select-option value="材质">材质</a-select-option>
              <a-select-option value="颜色">颜色</a-select-option>
              <a-select-option value="规格">规格</a-select-option>
              <a-select-option value="单位">单位</a-select-option>
            </a-select>
          </div>
          <div class="search-item">
            <span class="search-label">编码</span>
            <a-input
              v-model:value="searchParams.code"
              placeholder="请输入编码"
              allow-clear
              style="width: 160px"
            />
          </div>
          <div class="search-item">
            <span class="search-label">名称</span>
            <a-input
              v-model:value="searchParams.name"
              placeholder="请输入名称"
              allow-clear
              style="width: 180px"
            />
          </div>
          <div class="search-item">
            <a-space>
              <a-button type="primary" @click="handleSearch">
                <template #icon><SearchOutlined /></template>
                查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon><ClearOutlined /></template>
                重置
              </a-button>
            </a-space>
          </div>
        </div>
      </div>
      <div class="toolbar">
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </div>
      <BillTableList
        ref="tableRef"
        :columns="columns"
        :api-url="apiUrl"
        :params="searchParams"
      />
    </PageContainer>

    <a-modal
      v-model:open="editVisible"
      :title="editingId ? '编辑辅助资料' : '新增辅助资料'"
      width="600px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form :model="editForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="资料类型" required>
              <a-select v-model:value="editForm.auxType" placeholder="请选择">
                <a-select-option value="品牌">品牌</a-select-option>
                <a-select-option value="产地">产地</a-select-option>
                <a-select-option value="材质">材质</a-select-option>
                <a-select-option value="颜色">颜色</a-select-option>
                <a-select-option value="规格">规格</a-select-option>
                <a-select-option value="单位">单位</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="编码" required>
              <a-input v-model:value="editForm.code" placeholder="请输入编码" :disabled="!!editingId" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="名称" required>
          <a-input v-model:value="editForm.name" placeholder="请输入名称" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="editForm.sort" :min="0" style="width:100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-switch v-model:checked="editForm.status" checked-children="启用" un-checked-children="停用" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="备注">
          <a-textarea v-model:value="editForm.remark" :rows="2" placeholder="请输入备注" />
        </a-form-item>
      </a-form>
    </a-modal>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ClearOutlined, PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import request from '@/utils/request'

const tableRef = ref()
const apiUrl = '/md/product-aux/page'
const editVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const searchParams = reactive({
  auxType: undefined as string | undefined,
  code: '',
  name: '',
})

const columns = [
  { title: '资料类型', dataIndex: 'auxType', width: 100 },
  { title: '编码', dataIndex: 'code', width: 120 },
  { title: '名称', dataIndex: 'name', width: 200 },
  { title: '排序', dataIndex: 'sort', width: 80 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', width: 200, ellipsis: true },
]

const editForm = reactive({
  auxType: undefined as string | undefined,
  code: '',
  name: '',
  sort: 0,
  status: true,
  remark: '',
})

function handleSearch() {
  tableRef.value?.reload()
}

function handleReset() {
  searchParams.auxType = undefined
  searchParams.code = ''
  searchParams.name = ''
  tableRef.value?.reload()
}

function handleAdd() {
  editingId.value = null
  editForm.auxType = undefined
  editForm.code = ''
  editForm.name = ''
  editForm.sort = 0
  editForm.status = true
  editForm.remark = ''
  editVisible.value = true
}

async function handleSave() {
  if (!editForm.auxType || !editForm.name) {
    message.warning('请填写资料类型和名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/md/product-aux/${editingId.value}`, editForm)
      message.success('更新成功')
    } else {
      await request.post('/md/product-aux', editForm)
      message.success('创建成功')
    }
    editVisible.value = false
    tableRef.value?.reload()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.search-area {
  padding: 16px 16px 0;
  background: #fff;
  border-radius: 4px;
  margin-bottom: 16px;
}
.search-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-label {
  white-space: nowrap;
  font-size: 14px;
}
.toolbar {
  margin-bottom: 12px;
}
</style>
