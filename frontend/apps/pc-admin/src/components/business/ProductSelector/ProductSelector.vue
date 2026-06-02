<template>
  <div class="product-selector">
    <!-- 触发按钮 -->
    <a-button
      v-if="mode === 'button'"
      size="small"
      type="dashed"
      @click="visible = true"
    >
      <template #icon><PlusOutlined /></template>
      {{ buttonText || '选择产品' }}
    </a-button>

    <!-- 内联选择 -->
    <a-select
      v-else
      v-model:value="selectedProduct"
      :placeholder="placeholder || '搜索并选择产品...'"
      :loading="searchLoading"
      show-search
      allow-clear
      :filter-option="false"
      style="width: 100%"
      :value-key="valueKey"
      @search="handleSearch"
      @change="handleSelectChange"
      @focus="handleFocus"
    >
      <a-select-option
        v-for="item in searchResults"
        :key="item[valueKey]"
        :value="item[valueKey]"
        :label="item.productName || item.name"
      >
        <div class="product-option">
          <div class="product-option-main">
            <span class="product-name">{{ item.productName || item.name }}</span>
            <span v-if="item.productCode" class="product-code">{{ item.productCode }}</span>
          </div>
          <div class="product-option-meta">
            <span v-if="item.spec" class="product-spec">{{ item.spec }}</span>
            <span v-if="showStock" class="product-stock">库存: {{ item.stock ?? '-' }}</span>
            <span v-if="showPrice && item.salePrice" class="product-price">¥{{ item.salePrice }}</span>
          </div>
        </div>
      </a-select-option>
    </a-select>

    <!-- 弹出选择器 Modal -->
    <a-modal
      v-model:open="visible"
      title="选择产品"
      :width="720"
      :footer="null"
      :destroy-on-close="true"
    >
      <div class="product-selector-modal">
        <!-- 搜索和筛选 -->
        <div class="selector-toolbar">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索产品名称/编码..."
            style="width: 300px"
            @search="handleSearchProducts"
          />
          <a-select
            v-model:value="categoryFilter"
            placeholder="产品分类"
            style="width: 160px"
            allow-clear
            :options="categoryOptions"
          />
        </div>

        <!-- 产品列表 -->
        <a-table
          :data-source="productList"
          :columns="productColumns"
          :pagination="{ pageSize: 8, showSizeChanger: false }"
          :row-selection="productSelection"
          row-key="id"
          size="small"
          :loading="productLoading"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'price'">
              ¥{{ record.salePrice?.toFixed(2) || '0.00' }}
            </template>
            <template v-if="column.key === 'stock'">
              <span :class="record.stock !== undefined && record.stock <= record.minStock ? 'stock-warning' : ''">
                {{ record.stock ?? '-' }}
              </span>
            </template>
          </template>
        </a-table>

        <!-- 已选产品 -->
        <div v-if="selectedProducts.length > 0" class="selected-products">
          <div class="selected-products-header">
            <span>已选 {{ selectedProducts.length }} 项</span>
            <a-button type="link" size="small" @click="selectedProducts = []">清空</a-button>
          </div>
          <a-table
            :data-source="selectedProducts"
            :columns="selectedColumns"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'quantity'">
                <a-input-number
                  v-model:value="record.quantity"
                  :min="1"
                  size="small"
                  style="width: 80px"
                />
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" danger size="small" @click="selectedProducts.splice(index, 1)">
                  移除
                </a-button>
              </template>
            </template>
          </a-table>
        </div>

        <!-- 操作按钮 -->
        <div class="selector-footer">
          <a-button @click="visible = false">取消</a-button>
          <a-button type="primary" :disabled="selectedProducts.length === 0" @click="handleConfirm">
            确认选择 ({{ selectedProducts.length }})
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'

interface ProductItem {
  id: number
  productName?: string
  name?: string
  productCode?: string
  spec?: string
  unit?: string
  salePrice?: number
  costPrice?: number
  stock?: number
  minStock?: number
  categoryName?: string
  [key: string]: any
}

const props = withDefaults(defineProps<{
  mode?: 'button' | 'select'
  buttonText?: string
  placeholder?: string
  multiple?: boolean
  showStock?: boolean
  showPrice?: boolean
  valueKey?: string
  selectedKeys?: (string | number)[]
  categoryOptions?: { label: string; value: any }[]
}>(), {
  mode: 'select',
  multiple: false,
  showStock: true,
  showPrice: true,
  valueKey: 'id'
})

const emit = defineEmits<{
  'select': [products: ProductItem[]]
  'change': [product: ProductItem | null]
}>()

const visible = ref(false)
const searchKeyword = ref('')
const categoryFilter = ref(undefined)
const searchLoading = ref(false)
const productLoading = ref(false)
const productList = ref<ProductItem[]>([])
const selectedProducts = ref<ProductItem[]>([])
const searchResults = ref<ProductItem[]>([])
const selectedProduct = ref<any>(undefined)

const productColumns = [
  { title: '编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '名称', dataIndex: 'productName', key: 'productName' },
  { title: '规格', dataIndex: 'spec', key: 'spec', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '售价', key: 'price', width: 100, align: 'right' },
  { title: '库存', key: 'stock', width: 80, align: 'right' }
]

const selectedColumns = [
  { title: '编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '名称', dataIndex: 'productName', key: 'productName' },
  { title: '数量', key: 'quantity', width: 100 },
  { title: '操作', key: 'action', width: 60 }
]

const productSelection = {
  selectedRowKeys: computed(() => selectedProducts.value.map(p => p.id)),
  onChange: (keys: any[], rows: any[]) => {
    // 合并已选，避免重复
    const existingIds = new Set(selectedProducts.value.map(p => p.id))
    const newRows = rows.filter(r => !existingIds.has(r.id)).map(r => ({ ...r, quantity: 1 }))
    selectedProducts.value = [...selectedProducts.value, ...newRows]
  }
}

function handleSearch(value: string) {
  if (!value) {
    searchResults.value = []
    return
  }
  searchLoading.value = true
  // TODO: 调用产品搜索 API
  // productApi.search({ keyword: value }).then(res => {
  //   searchResults.value = res.data || []
  // }).finally(() => {
  //   searchLoading.value = false
  // })
  setTimeout(() => {
    searchLoading.value = false
  }, 300)
}

function handleSelectChange(value: any) {
  const product = searchResults.value.find(p => p[props.valueKey] === value)
  if (product) {
    emit('change', product)
    if (props.multiple) {
      if (!selectedProducts.value.some(p => p.id === product.id)) {
        selectedProducts.value.push({ ...product, quantity: 1 })
        emit('select', [...selectedProducts.value])
      }
    } else {
      emit('select', [product])
    }
  }
}

function handleFocus() {
  if (searchResults.value.length === 0) {
    handleSearch('')
  }
}

function handleSearchProducts() {
  productLoading.value = true
  // TODO: 调用产品列表 API
  // productApi.list({ keyword: searchKeyword.value, category: categoryFilter.value }).then(res => {
  //   productList.value = res.data?.records || []
  // }).finally(() => {
  //   productLoading.value = false
  // })
  setTimeout(() => {
    productLoading.value = false
  }, 500)
}

function handleConfirm() {
  emit('select', [...selectedProducts.value])
  visible.value = false
}

defineExpose({
  open: () => { visible.value = true },
  close: () => { visible.value = false },
  selectedProducts
})
</script>

<style scoped>
.product-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 2px 0;
}

.product-option-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-name {
  font-weight: 500;
}

.product-code {
  font-size: 12px;
  color: var(--color-text-tertiary, #999);
}

.product-option-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--color-text-tertiary, #999);
}

.product-spec {
  color: var(--color-text-secondary, #666);
}

.product-stock {
  color: var(--color-text-tertiary, #999);
}

.product-price {
  color: var(--color-primary, #1890ff);
  font-weight: 500;
}

/* Modal 样式 */
.product-selector-modal {
  max-height: 560px;
  overflow-y: auto;
}

.selector-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.selected-products {
  margin-top: 12px;
  border: 1px solid var(--color-border-secondary, #f0f0f0);
  border-radius: 6px;
  padding: 8px;
}

.selected-products-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
}

.selector-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border-secondary, #f0f0f0);
}

.stock-warning {
  color: var(--color-danger, #ff4d4f);
  font-weight: 500;
}
</style>
