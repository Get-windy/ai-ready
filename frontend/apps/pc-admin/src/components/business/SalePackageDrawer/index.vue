<template>
  <!--
    包裹/运单管理（一单多包）—— 落地《物流发货-业界做法调研》P1/P2-5
    一条记录 = 一个包裹；支持承运商档案引用、运单号唯一、电子面单取号（未开通则提示手工录入）。
  -->
  <a-drawer
    :open="open"
    :title="`包裹/运单 · ${orderNo || ''}`"
    width="980px"
    @close="handleClose"
  >
    <div class="pkg-toolbar">
      <a-space>
        <a-button type="primary" @click="openForm()">新增包裹</a-button>
        <a-alert
          v-if="waybillEnabled === false"
          type="info"
          show-icon
          style="display: inline-block; padding: 2px 10px"
          message="电子面单未开通（未配置 erp.logistics.waybill.*），请手工录入运单号"
        />
      </a-space>
      <span class="pkg-hint">
        共 {{ packages.length }} 个包裹 · 运费按承运商规则自动试算，可手工覆盖
      </span>
    </div>

    <a-table
      :data-source="packages"
      :columns="columns"
      :pagination="false"
      row-key="id"
      size="small"
      bordered
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'packageStatus'">
          <a-tag :color="PKG_STATUS[record.packageStatus]?.color || 'default'">
            {{ PKG_STATUS[record.packageStatus]?.label || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'waybillNo'">
          <span v-if="record.waybillNo">{{ record.waybillNo }}</span>
          <span v-else class="pkg-empty">未取号</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space :size="4">
            <a-button type="link" size="small" @click="openForm(record)">编辑</a-button>
            <a-button type="link" size="small" @click="handleAcquire(record)">取号</a-button>
            <a-button type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 包裹表单 -->
    <a-modal
      v-model:open="formVisible"
      :title="form.id ? '编辑包裹' : '新增包裹'"
      width="720px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form :label-col="{ span: 7 }" :wrapper-col="{ span: 17 }">
        <a-row :gutter="12">
          <a-col :span="12"><a-form-item label="包裹号"><a-input v-model:value="form.packageNo" placeholder="留空自动生成 P1/P2…" /></a-form-item></a-col>
          <a-col :span="12">
            <a-form-item label="承运商">
              <a-auto-complete
                v-model:value="form.logisticsCompany"
                :options="carrierOptions"
                placeholder="选择《资料→物流公司》或手工输入"
                allow-clear
                style="width: 100%"
                @select="onCarrierSelect"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12"><a-form-item label="运单号"><a-input v-model:value="form.waybillNo" placeholder="手工录入或点「取号」" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="件数"><a-input-number v-model:value="form.packageCount" :min="1" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="重量(kg)"><a-input-number v-model:value="form.packageWeight" :min="0" :precision="3" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="体积(m³)"><a-input-number v-model:value="form.packageVolume" :min="0" :precision="4" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="配送方式"><a-input v-model:value="form.deliveryMethod" placeholder="如：物流 / 快递 / 自提" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="司机"><a-input v-model:value="form.driverName" /></a-form-item></a-col>
          <a-col :span="12">
            <a-form-item label="运费付款方">
              <a-select v-model:value="form.freightPayer" allow-clear :options="FREIGHT_PAYER_OPTIONS" placeholder="寄付/到付/月结" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="运费(元)">
              <a-input-number v-model:value="form.shippingFee" :min="0" :precision="2" style="width: 100%" placeholder="留空按规则试算" />
            </a-form-item>
          </a-col>
          <a-col :span="12"><a-form-item label="代收货款(元)"><a-input-number v-model:value="form.codAmount" :min="0" :precision="2" style="width: 100%" /></a-form-item></a-col>
          <a-col :span="24"><a-form-item label="备注" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }"><a-input v-model:value="form.remark" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-modal>
  </a-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { saleLogisticsApi, type SalePackage } from '@/api/erp'
import { partnerApi } from '@/api/erp/partner'

const props = defineProps<{ open: boolean; orderId?: number | string; orderNo?: string }>()
const emit = defineEmits<{ (e: 'update:open', v: boolean): void; (e: 'changed'): void }>()

const PKG_STATUS: Record<number, { label: string; color: string }> = {
  0: { label: '待发货', color: 'default' },
  1: { label: '已发货', color: 'blue' },
  2: { label: '已签收', color: 'green' },
  9: { label: '异常', color: 'red' },
}
const FREIGHT_PAYER_OPTIONS = [
  { label: '寄付', value: '寄付' },
  { label: '到付', value: '到付' },
  { label: '月结', value: '月结' },
]

const columns = [
  { title: '包裹号', dataIndex: 'packageNo', key: 'packageNo', width: 80 },
  { title: '承运商', dataIndex: 'logisticsCompany', key: 'logisticsCompany', width: 130 },
  { title: '运单号', key: 'waybillNo', width: 160 },
  { title: '件数', dataIndex: 'packageCount', key: 'packageCount', width: 60 },
  { title: '重量(kg)', dataIndex: 'packageWeight', key: 'packageWeight', width: 90 },
  { title: '运费(元)', dataIndex: 'shippingFee', key: 'shippingFee', width: 90 },
  { title: '状态', key: 'packageStatus', width: 90 },
  { title: '操作', key: 'action', width: 170, fixed: 'right' as const },
]

const packages = ref<SalePackage[]>([])
const carrierOptions = ref<{ label: string; value: string }[]>([])
const carrierMap = ref<Record<string, number>>({})
const waybillEnabled = ref<boolean | null>(null)
const formVisible = ref(false)
const saving = ref(false)

const emptyForm = (): SalePackage => ({
  packageStatus: 0, packageCount: 1, freightPayer: '寄付',
} as SalePackage)
const form = reactive<SalePackage>(emptyForm())

const pick = (res: any) => res?.data ?? res ?? null

async function loadPackages() {
  if (!props.orderId) return
  const res: any = await saleLogisticsApi.packages(props.orderId).catch(() => null)
  packages.value = pick(res) || []
}

async function loadCarriers() {
  if (carrierOptions.value.length) return
  const res: any = await partnerApi.list('LOGISTICS', '1', 500).catch(() => null)
  const list = pick(res) || []
  carrierOptions.value = list.map((p: any) => ({ label: p.partyName || p.name, value: p.partyName || p.name }))
  carrierMap.value = list.reduce((acc: Record<string, number>, p: any) => {
    const name = p.partyName || p.name
    if (name) acc[name] = Number(p.id)
    return acc
  }, {})
}

async function loadWaybillStatus() {
  const res: any = await saleLogisticsApi.waybillStatus().catch(() => null)
  waybillEnabled.value = pick(res)?.enabled ?? false
}

watch(() => props.open, async (v) => {
  if (v) {
    await Promise.all([loadPackages(), loadCarriers(), loadWaybillStatus()])
  }
})

const onCarrierSelect = (value: string) => {
  form.logisticsCompanyId = carrierMap.value[value]
}

function openForm(record?: SalePackage) {
  Object.assign(form, emptyForm(), record || {})
  if (record?.logisticsCompany) {
    form.logisticsCompanyId = carrierMap.value[record.logisticsCompany] ?? record.logisticsCompanyId
  }
  formVisible.value = true
}

async function handleSave() {
  if (!props.orderId) return
  saving.value = true
  try {
    const payload: SalePackage = { ...form }
    if (payload.logisticsCompany && carrierMap.value[payload.logisticsCompany]) {
      payload.logisticsCompanyId = carrierMap.value[payload.logisticsCompany]
    }
    await saleLogisticsApi.savePackage(props.orderId, payload)
    message.success('已保存')
    formVisible.value = false
    await loadPackages()
    emit('changed')
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleAcquire(record: SalePackage) {
  try {
    await saleLogisticsApi.acquireWaybill(record.id!)
    message.success('取号成功')
    await loadPackages()
    emit('changed')
  } catch (e: any) {
    message.error(e?.message || '取号失败')
  }
}

function handleDelete(record: SalePackage) {
  Modal.confirm({
    title: '删除包裹',
    content: `确认删除包裹 ${record.packageNo || ''}？`,
    onOk: async () => {
      try {
        await saleLogisticsApi.deletePackage(props.orderId!, record.id!)
        message.success('已删除')
        await loadPackages()
        emit('changed')
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}

const handleClose = () => emit('update:open', false)
</script>

<style scoped>
.pkg-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.pkg-hint {
  color: #8c8c8c;
  font-size: 12px;
}
.pkg-empty {
  color: #bfbfbf;
}
</style>
