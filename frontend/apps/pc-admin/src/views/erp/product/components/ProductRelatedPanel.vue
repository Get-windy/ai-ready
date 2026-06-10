<template>
  <div>
    <div class="panel-toolbar">
      <a-button v-permission="'erp:product:related-edit'" size="small" type="primary" @click="debounceClick('add', showAddModal)">
        <PlusOutlined /> 添加关联
      </a-button>
    </div>
    <vxe-table :data="list" border size="small" max-height="300" align="center">
      <vxe-column type="seq" title="#" width="50" />
      <vxe-column field="relatedProductCode" title="产品编码" />
      <vxe-column field="relatedProductName" title="产品名称" />
      <vxe-column field="relatedProductSpec" title="规格" />
      <vxe-column field="relationType" title="关联类型">
        <template #default="{ row }">
          <a-tag>{{ relationTypeLabel(row.relationType) }}</a-tag>
        </template>
      </vxe-column>
      <vxe-column title="操作" width="80">
        <template #default="{ row }">
          <a-button v-permission="'erp:product:related-edit'" type="link" size="small" danger @click="debounceClick('del_' + row.id, () => handleDelete(row.id))">删除</a-button>
        </template>
      </vxe-column>
    </vxe-table>

    <a-modal v-model:open="modalVisible" title="添加关联产品" width="600px" @ok="debounceClick('modalOk', handleAdd)">
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="关联产品" required>
          <a-select v-model:value="form.relatedProductId" show-search :filter-option="filterOption" placeholder="搜索选择产品" size="small">
            <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">
              [{{ p.productCode }}] {{ p.productName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关联类型">
          <a-select v-model:value="form.relationType" size="small">
            <a-select-option value="UPSELL">向上销售</a-select-option>
            <a-select-option value="CROSSSELL">交叉销售</a-select-option>
            <a-select-option value="ALTERNATIVE">替代品</a-select-option>
            <a-select-option value="ACCESSORY">配件</a-select-option>
            <a-select-option value="BUNDLE">捆绑</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="备注">
          <a-input v-model:value="form.remark" size="small" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { productRelatedApi, productApi, type ProductRelated, type Product } from '@/api/erp/product'

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

const list = ref<ProductRelated[]>([])
const modalVisible = ref(false)
const productOptions = ref<Product[]>([])
const form = reactive({ relatedProductId: undefined as number | undefined, relationType: 'CROSSSELL', remark: '' })

async function load() {
  list.value = await productRelatedApi.getByProduct(props.productId)
}

async function showAddModal() {
  form.relatedProductId = undefined
  form.relationType = 'CROSSSELL'
  form.remark = ''
  // 加载产品下拉
  const res = await productApi.page({ pageNum: 1, pageSize: 200 })
  productOptions.value = res.records || []
  modalVisible.value = true
}

function filterOption(input: string, option: { children?: string }) {
  return option.children?.toLowerCase().includes(input.toLowerCase())
}

function relationTypeLabel(type: string) {
  const map: Record<string, string> = { UPSELL: '向上销售', CROSSSELL: '交叉销售', ALTERNATIVE: '替代品', ACCESSORY: '配件', BUNDLE: '捆绑' }
  return map[type] || type
}

async function handleAdd() {
  if (!form.relatedProductId) { message.warning('请选择关联产品'); return }
  try {
    const relatedData: Partial<ProductRelated> = {
      relatedProductId: form.relatedProductId,
      relationType: form.relationType,
      remark: form.remark || undefined,
      productId: props.productId
    }
    await productRelatedApi.create(relatedData)
    message.success('添加成功')
    modalVisible.value = false
    await load()
  } catch { message.error('添加失败') }
}

async function handleDelete(id: number) {
  try { await productRelatedApi.delete(id); message.success('删除成功'); await load() }
  catch { message.error('删除失败') }
}

onMounted(load)
onUnmounted(() => { debounceMap.clear() })
</script>

<style scoped>
.panel-toolbar { margin-bottom: 8px; }
</style>
