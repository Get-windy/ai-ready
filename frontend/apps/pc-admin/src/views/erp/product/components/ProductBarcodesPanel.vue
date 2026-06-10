<template>
  <div>
    <div class="panel-toolbar">
      <a-button v-permission="'erp:product:barcode-edit'" size="small" type="primary" @click="debounceClick('add', showAddModal)">
        <PlusOutlined /> 添加条形码
      </a-button>
    </div>
    <vxe-table :data="list" border size="small" max-height="300" align="center">
      <vxe-column type="seq" title="#" width="50" />
      <vxe-column field="barcode" title="条形码" />
      <vxe-column field="barcodeType" title="类型" />
      <vxe-column field="isDefault" title="默认">
        <template #default="{ row }">
          <a-tag v-if="row.isDefault" color="green">默认</a-tag>
        </template>
      </vxe-column>
      <vxe-column title="操作" width="120">
        <template #default="{ row }">
          <a-button v-permission="'erp:product:barcode-edit'" type="link" size="small" @click="debounceClick('edit_' + row.id, () => editRow(row))">编辑</a-button>
          <a-button v-permission="'erp:product:barcode-edit'" type="link" size="small" danger @click="debounceClick('del_' + row.id, () => handleDelete(row.id))">删除</a-button>
        </template>
      </vxe-column>
    </vxe-table>

    <a-modal v-model:open="modalVisible" :title="editingId ? '编辑条形码' : '添加条形码'" width="500px" @ok="debounceClick('modalOk', handleSave)">
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="条形码" required>
          <a-input v-model:value="form.barcode" placeholder="扫描或输入条码" size="small" />
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="form.barcodeType" size="small">
            <a-select-option value="EAN13">EAN-13</a-select-option>
            <a-select-option value="CODE128">CODE128</a-select-option>
            <a-select-option value="QR">QR码</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="设为默认">
          <a-switch v-model:checked="form.isDefault" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { productBarcodeApi, type ProductBarcode } from '@/api/erp/product'

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

const list = ref<ProductBarcode[]>([])
const modalVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ barcode: '', barcodeType: 'EAN13', isDefault: false })

async function load() {
  list.value = await productBarcodeApi.getByProduct(props.productId)
}

function showAddModal() {
  editingId.value = null
  Object.assign(form, { barcode: '', barcodeType: 'EAN13', isDefault: false })
  modalVisible.value = true
}

function editRow(row: ProductBarcode) {
  editingId.value = row.id
  Object.assign(form, { barcode: row.barcode, barcodeType: row.barcodeType, isDefault: !!row.isDefault })
  modalVisible.value = true
}

async function handleSave() {
  if (!form.barcode) { message.warning('请输入条形码'); return }
  try {
    const data = { ...form, productId: props.productId, isDefault: form.isDefault ? 1 : 0 }
    if (editingId.value) {
      await productBarcodeApi.update(editingId.value, data)
    } else {
      await productBarcodeApi.create(data)
    }
    message.success('保存成功')
    modalVisible.value = false
    await load()
  } catch { message.error('操作失败') }
}

async function handleDelete(id: number) {
  try { await productBarcodeApi.delete(id); message.success('删除成功'); await load() }
  catch { message.error('删除失败') }
}

onMounted(load)
onUnmounted(() => { debounceMap.clear() })
</script>

<style scoped>
.panel-toolbar { margin-bottom: 8px; }
</style>
