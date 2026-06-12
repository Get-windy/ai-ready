<template>
  <ErrorBoundary @error="handleError">
  <div class="product-detail-fullscreen">
    <!-- 固定顶栏 -->
    <div class="detail-header">
      <div class="detail-header__left">
        <a-button type="text" @click="goBack">
          <LeftOutlined /> 返回
        </a-button>
        <span class="detail-title">{{ isNew ? '新增产品' : '编辑产品' }}</span>
        <span v-if="!isNew && productId" class="detail-code">({{ form.productCode }})</span>
      </div>
      <div class="detail-header__right">
        <a-space>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>Ctrl+S</kbd> 保存</span>
          </span>
          <span v-if="lastUpdateTime" class="update-time">数据更新: {{ lastUpdateTime }}</span>
          <PrintButton page-code="erp/product" button-size="small" button-type="default" />
          <a-button @click="goBack">取消</a-button>
          <a-button v-permission="'erp:product:save'" type="primary" :loading="saving" @click="debounceClick('save', handleSave)">
            <SaveOutlined /> 保存
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 可滚动内容区 -->
    <div class="detail-body">
      <a-spin :spinning="loading">
        <!-- ===== 基本信息 ===== -->
        <a-card title="基本信息" class="detail-card">
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="产品编码" required>
                <a-input v-model:value="form.productCode" placeholder="自动生成或手动输入" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="产品名称" required>
                <a-input v-model:value="form.productName" placeholder="请输入产品名称" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="规格型号">
                <a-input v-model:value="form.spec" placeholder="如: 27寸 4K" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="基本单位" required>
                <a-input v-model:value="form.unit" placeholder="个/件/箱/公斤" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="产品分类">
                <a-tree-select
                  v-model:value="form.categoryId"
                  :tree-data="categoryTree"
                  :replace-fields="{ children: 'children', label: 'categoryName', value: 'id' }"
                  placeholder="请选择分类"
                  allow-clear
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="条形码">
                <a-input v-model:value="form.barcode" placeholder="扫描条码" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="SKU">
                <a-input v-model:value="form.sku" placeholder="库存单位编码" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="产品类型">
                <a-select v-model:value="form.productType" size="small">
                  <a-select-option value="SINGLE">单品</a-select-option>
                  <a-select-option value="KIT">套件</a-select-option>
                  <a-select-option value="SERVICE">服务</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="状态">
                <a-switch
                  :checked="form.status === 'ENABLED'"
                  checked-children="启用"
                  un-checked-children="停用"
                  @change="(v: boolean) => form.status = v ? 'ENABLED' : 'DISABLED'"
                />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item label="备注">
                <a-textarea v-model:value="form.remark" :rows="2" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <!-- 展开/折叠高级字段 -->
          <a-collapse v-model:active-key="collapseKeys" ghost>
            <a-collapse-panel key="advanced" header="更多信息">
              <a-row :gutter="24">
                <a-col :span="8">
                  <a-form-item label="品牌">
                    <a-input v-model:value="form.brand" placeholder="品牌名称" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="产地">
                    <a-input v-model:value="form.origin" placeholder="产地" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="重量(kg)">
                    <a-input-number v-model:value="form.weight" :precision="4" :min="0" style="width:100%" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="体积(m³)">
                    <a-input-number v-model:value="form.volume" :precision="4" :min="0" style="width:100%" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="保质期(天)">
                    <a-input-number v-model:value="form.shelfLifeDays" :min="0" style="width:100%" size="small" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="税率(%)">
                    <a-input-number v-model:value="form.taxRate" :precision="2" :min="0" :max="100" style="width:100%" size="small" />
                  </a-form-item>
                </a-col>
              </a-row>
            </a-collapse-panel>
          </a-collapse>
        </a-card>

        <!-- ===== 价格信息 ===== -->
        <a-card title="价格信息" class="detail-card">
          <a-row :gutter="24">
            <a-col :span="8">
              <a-form-item label="成本价">
                <a-input-number v-model:value="form.costPrice" :precision="2" :min="0" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="标准售价">
                <a-input-number v-model:value="form.standardPrice" :precision="2" :min="0" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="批发价">
                <a-input-number v-model:value="form.wholesalePrice" :precision="2" :min="0" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="默认产品等级">
                <a-select v-model:value="form.productGradeId" placeholder="请选择等级" allow-clear size="small">
                  <a-select-option v-for="g in grades" :key="g.id" :value="g.id">{{ g.gradeName }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="采购价">
                <a-input-number v-model:value="form.purchasePrice" :precision="2" :min="0" style="width:100%" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="零售价">
                <a-input-number v-model:value="form.retailPrice" :precision="2" :min="0" style="width:100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>

        <!-- ===== 等级价格表(仅编辑时显示) ===== -->
        <a-card v-if="!isNew" title="等级价格配置" class="detail-card">
          <template #extra>
            <a-button type="primary" size="small" @click="addGradePriceRow">
              <PlusOutlined /> 添加行
            </a-button>
          </template>
          <vxe-table
            ref="gradePriceTableRef"
            :data="gradePriceList"
            :edit-config="{ trigger: 'click', mode: 'row' }"
            border
            size="small"
            align="center"
            max-height="300"
          >
            <vxe-column type="seq" title="#" width="50" />
            <vxe-column field="productGradeId" title="产品等级" width="140" :edit-render="({ name: 'select', options: gradeSelectOptions } as any)" />
            <vxe-column field="price" title="价格(元)" width="160" :edit-render="({ name: 'input', type: 'number', props: { precision: 2, min: 0 } } as any)" />
            <vxe-column field="minOrderQty" title="起订量" width="100" :edit-render="({ name: 'input', type: 'number', props: { min: 0 } } as any)" />
            <vxe-column field="_action" title="操作" width="80">
              <template #default="{ row, rowIndex }">
                <a-button type="link" size="small" danger @click="removeGradePriceRow(rowIndex)">删除</a-button>
              </template>
            </vxe-column>
          </vxe-table>
        </a-card>

        <!-- ===== 库存管理 ===== -->
        <a-card v-if="!isNew" title="库存管理" class="detail-card">
          <template #extra>
            <a-tag color="blue">{{ inventoryModeLabel }}</a-tag>
          </template>
          <div class="inventory-summary">
            <a-row :gutter="24">
              <a-col :span="6">
                <div class="inv-stat-item">
                  <div class="inv-stat-label">当前库存</div>
                  <div class="inv-stat-value">{{ stockInfo.quantity ?? '-' }}</div>
                  <div class="inv-stat-unit">{{ form.unit || '件' }}</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="inv-stat-item">
                  <div class="inv-stat-label">可用库存</div>
                  <a-tooltip title="可用数量 = 总库存 - 冻结数量">
                    <div class="inv-stat-value inv-stat-value--green">{{ stockInfo.availableQuantity ?? '-' }}</div>
                  </a-tooltip>
                  <div class="inv-stat-unit">{{ form.unit || '件' }}</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="inv-stat-item">
                  <div class="inv-stat-label">安全库存</div>
                  <div class="inv-stat-value">{{ stockInfo.safetyStock ?? '-' }}</div>
                  <div class="inv-stat-unit">{{ form.unit || '件' }}</div>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="inv-stat-item">
                  <div class="inv-stat-label">库存预警</div>
                  <div class="inv-stat-value">
                    <a-tag v-if="stockInfo.quantity != null && stockInfo.safetyStock != null && stockInfo.quantity <= stockInfo.safetyStock" color="red">偏低</a-tag>
                    <a-tag v-else color="green">正常</a-tag>
                  </div>
                </div>
              </a-col>
            </a-row>
            <a-divider style="margin: 12px 0;" />
            <a-space>
              <a-button size="small" @click="router.push(`/erp/stock?productId=${productId}`)">
                <ContainerOutlined /> 查看库存明细
              </a-button>
            </a-space>
          </div>
        </a-card>
        <!-- ===== 扩展信息(辅助属性/单位/条码/附件/关联) ===== -->
        <a-card v-if="!isNew" title="扩展信息" class="detail-card">
          <a-tabs v-model:active-key="extTabKey" size="small">
            <a-tab-pane key="attributes" tab="辅助属性">
              <ProductAttributesPanel :product-id="productId" />
            </a-tab-pane>
            <a-tab-pane key="units" tab="多单位">
              <ProductUnitsPanel :product-id="productId" />
            </a-tab-pane>
            <a-tab-pane key="barcodes" tab="条形码">
              <ProductBarcodesPanel :product-id="productId" />
            </a-tab-pane>
            <a-tab-pane key="attachments" tab="附件">
              <ProductAttachmentsPanel :product-id="productId" />
            </a-tab-pane>
            <a-tab-pane key="related" tab="关联产品">
              <ProductRelatedPanel :product-id="productId" />
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </a-spin>
    </div>
  </div>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed, watch, defineExpose } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { LeftOutlined, SaveOutlined, PlusOutlined, ContainerOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import { productApi, productCategoryApi, productGradeApi, productGradePriceApi, inventoryModeApi } from '@/api/erp/product'
import type { ProductGrade, ProductCategory, InventoryMode } from '@/api/erp/product'
import request from '@/utils/request'
import type { VxeTableInstance } from 'vxe-table'
import ProductAttributesPanel from './components/ProductAttributesPanel.vue'
import ProductUnitsPanel from './components/ProductUnitsPanel.vue'
import ProductBarcodesPanel from './components/ProductBarcodesPanel.vue'
import ProductAttachmentsPanel from './components/ProductAttachmentsPanel.vue'
import ProductRelatedPanel from './components/ProductRelatedPanel.vue'

const route = useRoute()
const router = useRouter()

// ── 防抖工具 ──────────────────────────────────────────
function handleError(err: any) { console.warn('[产品详情] ErrorBoundary 捕获异常:', err) }

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const productId = computed(() => route.params.id ? Number(route.params.id) : 0)
const isNew = computed(() => !productId.value || route.path.includes('/create'))

const loading = ref(false)
const saving = ref(false)
const lastUpdateTime = ref('')

// ── 表单脏检测 ──
const isDirty = ref(false)
const originalFormSnapshot = ref('')

function takeFormSnapshot() {
  originalFormSnapshot.value = JSON.stringify(form)
  isDirty.value = false
}

function markDirty() {
  if (!originalFormSnapshot.value) return
  isDirty.value = JSON.stringify(form) !== originalFormSnapshot.value
}

// 为所有表单字段添加自动脏检测（通过 Proxy 或显式调用）
// 保存/取消按钮点击时重置脏状态
function resetDirty() {
  isDirty.value = false
  takeFormSnapshot()
}

// ── 折叠面板与扩展标签状态 ──
const collapseKeys = ref<string[]>([])
const extTabKey = ref('attributes')

// ── 表单数据 ──
const form = reactive({
  productCode: '',
  productName: '',
  spec: '',
  unit: '',
  categoryId: undefined as number | undefined,
  productGradeId: undefined as number | undefined,
  barcode: '',
  sku: '',
  productType: 'SINGLE',
  costPrice: undefined as number | undefined,
  standardPrice: undefined as number | undefined,
  wholesalePrice: undefined as number | undefined,
  imageUrl: '',
  status: 'ENABLED',
  remark: '',
  // 扩展字段
  weight: undefined as number | undefined,
  volume: undefined as number | undefined,
  origin: '',
  brand: '',
  taxRate: 13,
  purchasePrice: undefined as number | undefined,
  retailPrice: undefined as number | undefined,
  shelfLifeDays: undefined as number | undefined
})

// ── 库存管理模式 ──
const inventoryMode = ref<InventoryMode>('BATCH')
const inventoryModeLabel = computed(() => {
  const map: Record<InventoryMode, string> = { BATCH: '批次管理', SERIAL: '序列号管理', SKU: 'SKU管理' }
  return map[inventoryMode.value]
})

// ── 库存信息 ──
const stockInfo = ref({
  quantity: null as number | null,
  availableQuantity: null as number | null,
  safetyStock: null as number | null,
  warehouseName: ''
})

// ── 下拉数据 ──
const categoryTree = ref<ProductCategory[]>([])
const grades = ref<ProductGrade[]>([])

// ── 等级价格表 ──
const gradePriceTableRef = ref<VxeTableInstance>()
interface GradePriceRow {
  id?: number
  productGradeId: number | undefined
  price: number
  minOrderQty: number
  _isNew?: boolean
}
const gradePriceList = ref<GradePriceRow[]>([])

// 深度监听表单变化
watch(form, () => { markDirty() }, { deep: true })

// 路由离开守卫 — 捕获 SPA 内导航
onBeforeRouteLeave((_to, _from, next) => {
  if (isDirty.value) {
    Modal.confirm({
      title: '未保存的更改',
      content: '当前有未保存的修改，确定要离开吗？',
      okText: '离开',
      cancelText: '继续编辑',
      onOk: () => next(),
      onCancel: () => next(false)
    })
  } else {
    next()
  }
})

const gradeSelectOptions = computed(() =>
  grades.value.map(g => ({ label: g.gradeName, value: g.id }))
)

// ── 初始化 ──
async function init() {
  loading.value = true
  try {
    // 并行加载
    const [cats, gds] = await Promise.all([
      productCategoryApi.getTree(),
      productGradeApi.list()
    ])
    categoryTree.value = cats
    grades.value = gds

    if (!isNew.value && productId.value) {
      await loadProduct(productId.value)
    } else {
      takeFormSnapshot()
    }
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (e) {
    console.error('[产品详情] 初始化失败', e)
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

async function loadProduct(id: number) {
  try {
    const prod = await productApi.getById(id)
    Object.assign(form, {
      productCode: prod.productCode,
      productName: prod.productName,
      spec: prod.spec,
      unit: prod.unit,
      categoryId: prod.categoryId,
      productGradeId: prod.productGradeId,
      barcode: prod.barcode,
      sku: prod.sku || '',
      productType: prod.productType || 'SINGLE',
      costPrice: prod.costPrice,
      standardPrice: prod.standardPrice,
      wholesalePrice: prod.wholesalePrice,
      imageUrl: prod.imageUrl || '',
      status: prod.status || 'ENABLED',
      remark: prod.remark || '',
      // 扩展字段
      weight: prod.weight,
      volume: prod.volume,
      origin: prod.origin || '',
      brand: prod.brand || '',
      taxRate: prod.taxRate ?? 13,
      purchasePrice: prod.purchasePrice,
      retailPrice: prod.retailPrice,
      shelfLifeDays: prod.shelfLifeDays
    })
    // 并行加载: 等级价格 + 库存模式 + 库存信息
    const [prices] = await Promise.all([
      productGradePriceApi.getByProduct(id),
      inventoryModeApi.get().then(mode => { inventoryMode.value = mode }).catch(() => {}),
      request.get(`/erp/stock/by-product/${id}`).then((res: any) => {
        if ((res as any)?.id) stockInfo.value = res
      }).catch(() => {})
    ])
    gradePriceList.value = (prices || []).map(p => ({
      id: p.id,
      productGradeId: p.productGradeId,
      price: p.price,
      minOrderQty: p.minOrderQty || 0
    }))
    takeFormSnapshot()
  } catch (e) {
    console.error('[产品详情] 加载产品失败', e)
    message.error('加载产品信息失败')
  }
}

function addGradePriceRow() {
  gradePriceList.value.push({
    productGradeId: undefined,
    price: 0,
    minOrderQty: 0,
    _isNew: true
  })
}

function removeGradePriceRow(index: number) {
  gradePriceList.value.splice(index, 1)
}

// ── 保存 ──
async function handleSave() {
  // 基本校验
  if (!form.productCode) { message.warning('请输入产品编码'); return }
  if (!form.productName) { message.warning('请输入产品名称'); return }
  if (!form.unit) { message.warning('请输入基本单位'); return }

  saving.value = true
  try {
    const payload: Partial<import('@/api/erp/product').Product> = {
      productCode: form.productCode,
      productName: form.productName,
      spec: form.spec,
      unit: form.unit,
      categoryId: form.categoryId,
      productGradeId: form.productGradeId,
      barcode: form.barcode,
      sku: form.sku,
      productType: form.productType,
      costPrice: form.costPrice,
      standardPrice: form.standardPrice,
      wholesalePrice: form.wholesalePrice,
      imageUrl: form.imageUrl,
      status: form.status,
      remark: form.remark,
      weight: form.weight,
      volume: form.volume,
      origin: form.origin || undefined,
      brand: form.brand || undefined,
      taxRate: form.taxRate,
      purchasePrice: form.purchasePrice,
      retailPrice: form.retailPrice,
      shelfLifeDays: form.shelfLifeDays
    }
    if (isNew.value) {
      await productApi.create(payload)
      message.success('创建成功')
    } else {
      await productApi.update(productId.value, payload)
      // 保存等级价格
      if (gradePriceList.value.length > 0) {
        await productGradePriceApi.batchSave(productId.value, gradePriceList.value.map(r => ({
          id: r.id,
          productGradeId: r.productGradeId,
          price: r.price,
          minOrderQty: r.minOrderQty,
          isActive: 1
        })))
      }
      message.success('保存成功')
    }
    resetDirty()
    goBack()
  } catch (e: unknown) {
    console.error('[产品详情] 保存失败', e)
    const msg = e instanceof Error ? e.message : '保存失败'
    message.error(msg)
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/erp/product')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
  }
  // Ctrl+S 保存
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    if (!saving.value) handleSave()
  }
}

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (isDirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(() => {
  init()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

defineExpose({ fetchData: () => init() })
</script>

<style scoped>
.product-detail-fullscreen {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
  overflow: hidden;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  z-index: 10;
}

.detail-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.detail-code {
  font-size: 13px;
  color: #999;
}

.detail-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

/* ── 快捷键提示 ── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}

.detail-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

:deep(.ant-card-head) {
  padding: 0 16px;
  min-height: 40px;
}

:deep(.ant-card-head-title) {
  font-size: 14px;
  font-weight: 600;
  padding: 8px 0;
}

:deep(.ant-card-body) {
  padding: 16px;
}

:deep(.ant-form-item) {
  margin-bottom: 12px;
}

:deep(.ant-form-item-label) {
  padding-bottom: 2px;
}

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  height: 28px;
  line-height: 28px;
}

:deep(.ant-btn-sm) {
  height: 28px;
  font-size: 13px;
}
</style>
