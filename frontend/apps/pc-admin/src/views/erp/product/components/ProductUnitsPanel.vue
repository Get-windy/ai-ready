<template>
  <div>
    <div class="panel-toolbar">
      <a-button v-permission="'erp:product:unit-edit'" size="small" type="primary" @click="debounceClick('add', showAddModal)">
        <PlusOutlined /> 添加单位
      </a-button>
    </div>
    <vxe-table :data="list" border size="small" max-height="300" align="center">
      <vxe-column type="seq" title="#" width="50" />
      <vxe-column field="unitName" title="单位名称" />
      <vxe-column field="conversionRate" title="换算率">
        <template #default="{ row }">{{ row.isBaseUnit ? '基本单位' : row.conversionRate }}</template>
      </vxe-column>
      <vxe-column field="isBaseUnit" title="是否默认">
        <template #default="{ row }">
          <a-tag v-if="row.isBaseUnit" color="green">默认</a-tag>
        </template>
      </vxe-column>
      <vxe-column field="barcode" title="条码" />
      <vxe-column title="操作" width="120">
        <template #default="{ row }">
          <a-button v-permission="'erp:product:unit-edit'" type="link" size="small" @click="debounceClick('edit_' + row.id, () => editRow(row))">编辑</a-button>
          <a-button v-permission="'erp:product:unit-edit'" type="link" size="small" danger @click="debounceClick('del_' + row.id, () => handleDelete(row.id))">删除</a-button>
        </template>
      </vxe-column>
    </vxe-table>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑单位' : '添加单位'" width="500px" @ok="debounceClick('modalOk', handleSave)">
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="单位名称" required>
          <a-input v-model:value="form.unitName" placeholder="箱/包/盒" size="small" />
        </a-form-item>
        <a-form-item label="换算率">
          <a-input-number v-model:value="form.conversionRate" :precision="6" :min="0.000001" style="width:100%" size="small" />
        </a-form-item>
        <a-form-item label="是否基本单位">
          <a-switch v-model:checked="form.isBaseUnit" />
        </a-form-item>
        <a-form-item label="条码">
          <a-input v-model:value="form.barcode" size="small" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { productUnitApi, type ProductUnit } from '@/api/erp/product'

const props = defineProps<{ productId: number }>()

// ── 防抖 ──
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const list = ref<ProductUnit[]>([])
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ unitName: '', conversionRate: 1, isBaseUnit: false, barcode: '' })

async function load() {
  list.value = await productUnitApi.getByProduct(props.productId)
}

function showAddModal() {
  editingId.value = null
  Object.assign(form, { unitName: '', conversionRate: 1, isBaseUnit: false, barcode: '' })
  modalVisible.value = true
}

function editRow(row: ProductUnit) {
  editingId.value = row.id
  Object.assign(form, {
    unitName: row.unitName,
    conversionRate: row.conversionRate,
    isBaseUnit: !!row.isBaseUnit,
    barcode: row.barcode || ''
  })
  modalVisible.value = true
}

async function handleSave() {
  if (!form.unitName) { message.warning('请输入单位名称'); return }
  const data = { ...form, productId: props.productId, isBaseUnit: form.isBaseUnit ? 1 : 0 }
  try {
    if (editingId.value) {
      await productUnitApi.update(editingId.value, data)
    } else {
      await productUnitApi.create(data)
    }
    message.success('保存成功')
    modalVisible.value = false
    await load()
  } catch { message.error('操作失败') }
}

async function handleDelete(id: number) {
  try {
    await productUnitApi.delete(id)
    message.success('删除成功')
    await load()
  } catch { message.error('删除失败') }
}

onMounted(load)
onUnmounted(() => { debounceMap.clear() })
</script>

<style scoped>
.panel-toolbar { margin-bottom: 8px; }
</style>
