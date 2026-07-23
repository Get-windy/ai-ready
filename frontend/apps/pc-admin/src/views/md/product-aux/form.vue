<template>
  <ErrorBoundary>
    <PageContainer title="商品辅助资料详情">
      <template #extra>
        <a-button type="text" @click="handleBack">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
      </template>

      <div class="form-content">
        <a-form ref="formRef" :model="form" layout="vertical">
          <!-- 基本信息 -->
          <div class="section-card">
            <div class="section-title">基本信息</div>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="资料类型" required>
                  <a-select v-model:value="form.auxType" placeholder="请选择">
                    <a-select-option value="品牌">品牌</a-select-option>
                    <a-select-option value="产地">产地</a-select-option>
                    <a-select-option value="材质">材质</a-select-option>
                    <a-select-option value="颜色">颜色</a-select-option>
                    <a-select-option value="规格">规格</a-select-option>
                    <a-select-option value="单位">单位</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="编码" required>
                  <a-input v-model:value="form.code" placeholder="自动生成或手动输入" :disabled="!!editingId" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="名称" required>
                  <a-input v-model:value="form.name" placeholder="请输入名称" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="排序">
                  <a-input-number v-model:value="form.sort" :min="0" style="width:100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="状态">
                  <a-switch v-model:checked="form.status" checked-children="启用" un-checked-children="停用" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="助记码">
                  <a-input v-model:value="form.mnemonic" placeholder="拼音首字母" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="备注">
              <a-textarea v-model:value="form.remark" :rows="2" placeholder="请输入备注" />
            </a-form-item>
          </div>

          <!-- 多单位 -->
          <div class="section-card">
            <div class="section-title">多单位</div>
            <div class="multi-unit-grid">
              <div class="unit-row header">
                <span>单位名称</span>
                <span>换算系数</span>
                <span>是否主单位</span>
                <span>操作</span>
              </div>
              <div v-for="(unit, idx) in form.units" :key="idx" class="unit-row">
                <a-input v-model:value="unit.unitName" placeholder="如：箱" size="small" />
                <a-input-number v-model:value="unit.conversionRate" :min="1" size="small" style="width:100px" />
                <a-radio v-model:value="form.masterUnitIdx" :name="'masterUnit'" :checked="form.masterUnitIdx === idx" @change="form.masterUnitIdx = idx" />
                <a-button type="link" danger size="small" @click="form.units.splice(idx, 1)">移除</a-button>
              </div>
              <a-button type="dashed" block size="small" @click="form.units.push({ unitName: '', conversionRate: 1 })">
                <template #icon><PlusOutlined /></template>
                添加单位
              </a-button>
            </div>
          </div>

          <!-- 条码管理 -->
          <div class="section-card">
            <div class="section-title">条码管理</div>
            <div class="barcode-grid">
              <div class="barcode-row header">
                <span>条码</span>
                <span>条码类型</span>
                <span>对应单位</span>
                <span>是否默认</span>
                <span>操作</span>
              </div>
              <div v-for="(bc, idx) in form.barcodes" :key="idx" class="barcode-row">
                <a-input v-model:value="bc.barcode" placeholder="条码" size="small" />
                <a-select v-model:value="bc.barcodeType" size="small" style="width:120px">
                  <a-select-option value="EAN13">EAN-13</a-select-option>
                  <a-select-option value="CODE128">Code128</a-select-option>
                  <a-select-option value="QR">二维码</a-select-option>
                  <a-select-option value="OTHER">其他</a-select-option>
                </a-select>
                <a-input v-model:value="bc.unitName" placeholder="对应单位" size="small" style="width:100px" />
                <a-radio :checked="bc.isDefault" @change="setDefaultBarcode(idx)" />
                <a-button type="link" danger size="small" @click="form.barcodes.splice(idx, 1)">移除</a-button>
              </div>
              <a-button type="dashed" block size="small" @click="form.barcodes.push({ barcode: '', barcodeType: 'EAN13', unitName: '', isDefault: false })">
                <template #icon><PlusOutlined /></template>
                添加条码
              </a-button>
            </div>
          </div>

          <!-- 价格体系 -->
          <div class="section-card">
            <div class="section-title">价格体系</div>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="参考进价">
                  <a-input-number v-model:value="form.refPurchasePrice" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="参考售价">
                  <a-input-number v-model:value="form.refSalePrice" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="会员价">
                  <a-input-number v-model:value="form.memberPrice" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="最低售价">
                  <a-input-number v-model:value="form.minSalePrice" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="批发价">
                  <a-input-number v-model:value="form.wholesalePrice" :precision="2" :min="0" style="width:100%" placeholder="0.00" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="税率(%)">
                  <a-input-number v-model:value="form.taxRate" :precision="2" :min="0" :max="100" style="width:100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <div class="section-subtitle">等级价格</div>
            <div class="grade-price-grid">
              <div class="gp-row header">
                <span>等级名称</span>
                <span>价格</span>
              </div>
              <div v-for="(gp, idx) in form.gradePrices" :key="idx" class="gp-row">
                <a-input v-model:value="gp.gradeName" placeholder="等级" size="small" />
                <a-input-number v-model:value="gp.price" :precision="2" :min="0" size="small" style="width:120px" />
                <a-button type="link" danger size="small" @click="form.gradePrices.splice(idx, 1)">移除</a-button>
              </div>
              <a-button type="dashed" block size="small" @click="form.gradePrices.push({ gradeName: '', price: 0 })">
                <template #icon><PlusOutlined /></template>
                添加等级价格
              </a-button>
            </div>
          </div>

          <!-- 批次/保质期属性 -->
          <div class="section-card">
            <div class="section-title">批次/保质期属性</div>
            <a-row :gutter="24">
              <a-col :span="8">
                <a-form-item label="启用批次管理">
                  <a-switch v-model:checked="form.enableBatch" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="启用保质期管理">
                  <a-switch v-model:checked="form.enableExpiry" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="启用序列号管理">
                  <a-switch v-model:checked="form.enableSerial" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row v-if="form.enableExpiry" :gutter="24">
              <a-col :span="8">
                <a-form-item label="保质期（天）">
                  <a-input-number v-model:value="form.expiryDays" :min="1" style="width:100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="预警提前（天）">
                  <a-input-number v-model:value="form.warningDays" :min="1" style="width:100%" />
                </a-form-item>
              </a-col>
            </a-row>
          </div>
        </a-form>

        <!-- 固定底部 -->
        <div class="form-footer">
          <a-space>
            <a-button @click="handleBack">取消</a-button>
            <a-button :loading="saving" type="primary" @click="handleSubmit">保存</a-button>
          </a-space>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, PlusOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  auxType: undefined as string | undefined,
  code: '',
  name: '',
  sort: 0,
  status: true,
  mnemonic: '',
  remark: '',
  // 多单位
  masterUnitIdx: 0,
  units: [{ unitName: '个', conversionRate: 1 }] as { unitName: string; conversionRate: number }[],
  // 条码
  barcodes: [] as { barcode: string; barcodeType: string; unitName: string; isDefault: boolean }[],
  // 价格体系
  refPurchasePrice: 0,
  refSalePrice: 0,
  memberPrice: 0,
  minSalePrice: 0,
  wholesalePrice: 0,
  taxRate: 13,
  gradePrices: [] as { gradeName: string; price: number }[],
  // 批次/保质期
  enableBatch: false,
  enableExpiry: false,
  enableSerial: false,
  expiryDays: 365,
  warningDays: 30,
})

function handleBack() {
  router.push('/md/product-aux/index')
}

function setDefaultBarcode(idx: number) {
  form.barcodes.forEach((bc, i) => { bc.isDefault = i === idx })
}

async function loadData(id: number) {
  try {
    const res = await request.get(`/md/product-aux/${id}`)
    Object.assign(form, res)
  } catch {
    message.error('加载失败')
  }
}

async function handleSubmit() {
  if (!form.auxType || !form.name) {
    message.warning('请填写资料类型和名称')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/md/product-aux/${editingId.value}`, form)
      message.success('更新成功')
    } else {
      await request.post('/md/product-aux', form)
      message.success('创建成功')
    }
    router.push('/md/product-aux/index')
  } catch (e: any) {
    message.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  const id = route.params.id
  if (id) {
    editingId.value = Number(id)
    loadData(Number(id))
  }
})
</script>

<style scoped>
.form-content { flex: 1; overflow-y: auto; padding: 0 16px 16px; }
.section-card { background: #fff; border-radius: 6px; padding: 20px 24px 12px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.05); }
.section-title { font-size: 14px; font-weight: 600; color: #262626; margin-bottom: 16px; padding-bottom: 10px; border-bottom: 1px solid #f0f0f0; }
.section-subtitle { font-size: 13px; font-weight: 600; color: #595959; margin: 12px 0 8px; }
.form-footer { background: #fff; border-radius: 6px; padding: 16px 24px; text-align: right; box-shadow: 0 -1px 4px rgba(0,0,0,0.05); margin-top: 12px; }

.multi-unit-grid, .barcode-grid, .grade-price-grid { display: flex; flex-direction: column; gap: 8px; }
.unit-row, .barcode-row, .gp-row { display: flex; gap: 12px; align-items: center; }
.unit-row.header, .barcode-row.header, .gp-row.header { font-size: 12px; color: #999; font-weight: 600; }
</style>
