<template>
  <div>
    <div class="panel-toolbar">
      <a-button v-permission="'erp:product:attr-edit'" size="small" type="primary" @click="debounceClick('add', showAddModal)">
        <PlusOutlined /> 添加属性
      </a-button>
    </div>
    <vxe-table :data="list" border size="small" max-height="300" align="center">
      <vxe-column type="seq" title="#" width="50" />
      <vxe-column field="attrName" title="属性名称" />
      <vxe-column field="attrType" title="类型">
        <template #default="{ row }">
          <a-tag>{{ row.attrType === 'SELECT' ? '选择' : row.attrType === 'COLOR' ? '颜色' : '文本' }}</a-tag>
        </template>
      </vxe-column>
      <vxe-column field="attrValue" title="属性值" />
      <vxe-column title="操作" width="80">
        <template #default="{ row }">
          <a-button v-permission="'erp:product:attr-edit'" type="link" size="small" danger @click="debounceClick('del_' + row.attrDefId, () => removeValue(row))">删除</a-button>
        </template>
      </vxe-column>
    </vxe-table>

    <a-modal v-model:open="modalVisible" title="添加属性" width="500px" @ok="debounceClick('modalOk', handleAdd)">
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="属性定义" required>
          <a-select v-model:value="newValue.attrDefId" placeholder="请选择属性" size="small" @change="onDefChange">
            <a-select-option v-for="d in defs" :key="d.id" :value="d.id">{{ d.attrName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="属性值" required>
          <a-select v-if="selectedDef?.attrType === 'SELECT'" v-model:value="newValue.attrValue" size="small">
            <a-select-option v-for="o in options" :key="o.id" :value="o.optionValue">{{ o.optionLabel || o.optionValue }}</a-select-option>
          </a-select>
          <a-input v-else v-model:value="newValue.attrValue" placeholder="请输入属性值" size="small" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { productAttributeApi, type ProductAttributeDef, type ProductAttributeOption, type ProductAttributeValue } from '@/api/erp/product'

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

const list = ref<ProductAttributeValue[]>([])
const defs = ref<ProductAttributeDef[]>([])
const options = ref<ProductAttributeOption[]>([])
const selectedDef = ref<ProductAttributeDef | null>(null)
const modalVisible = ref(false)
const newValue = ref({ attrDefId: undefined as number | undefined, attrValue: '' })

async function load() {
  const [vals, attrDefs] = await Promise.all([
    productAttributeApi.getValues(props.productId),
    productAttributeApi.getDefs()
  ])
  list.value = vals
  defs.value = attrDefs
}

function showAddModal() {
  newValue.value = { attrDefId: undefined, attrValue: '' }
  selectedDef.value = null
  options.value = []
  modalVisible.value = true
}

async function onDefChange(defId: number) {
  selectedDef.value = defs.value.find(d => d.id === defId) || null
  if (selectedDef.value?.attrType === 'SELECT') {
    options.value = await productAttributeApi.getOptions(defId)
  }
}

async function handleAdd() {
  if (!newValue.value.attrDefId || !newValue.value.attrValue) {
    message.warning('请选择属性和输入值')
    return
  }
  const newRecord: ProductAttributeValue = {
    attrDefId: newValue.value.attrDefId!,
    attrValue: newValue.value.attrValue,
    attrName: selectedDef.value?.attrName || '',
    attrType: selectedDef.value?.attrType || 'TEXT',
    sortOrder: 0,
    productId: props.productId,
    id: 0
  }
  list.value.push(newRecord)
  modalVisible.value = false
  await save()
}

async function removeValue(row: ProductAttributeValue) {
  list.value = list.value.filter(v => v !== row)
  await save()
}

async function save() {
  try {
    await productAttributeApi.saveValues(props.productId, list.value)
    message.success('保存成功')
  } catch {
    message.error('保存失败')
  }
}

onMounted(load)
onUnmounted(() => { debounceMap.clear() })
</script>

<style scoped>
.panel-toolbar {
  margin-bottom: 8px;
}
</style>
