<template>
  <div class="form-page-wrapper">
    <!-- ═══ 页头 ═══ -->
    <div class="page-header">
      <a-space :size="8">
        <a-button
          type="text"
          size="small"
          @click="handleCancel"
        >
          <template #icon>
            <ArrowLeftOutlined />
          </template>
          返回
        </a-button>
        <span class="page-title">{{ isEdit ? '车辆 - 修改' : '车辆 - 新增' }}</span>
        <a-tag
          v-if="form.vehicleCode"
          color="blue"
        >
          {{ form.vehicleCode }}
        </a-tag>
      </a-space>
    </div>

    <div class="form-scroll-area">
      <!--
        分区栅格表单（对齐《资料模块》金标准：views/md/logistics/form.vue）
        · 车辆是「档案（主数据）」而非「单据」：字段多、语义分组明确、编辑频率低 → 四分区两列栅格 + 行内校验，
          不再用 BillFormPage 的 inline flow（那是单据表头快速录单用的单行内联布局）。
        · 字段与后端 VehicleDTO 一一对应（22 字段），必填与后端 @NotBlank/@NotNull 保持一致。
      -->
      <a-form
        ref="formRef"
        layout="horizontal"
        :model="form"
        :rules="rules"
        :label-col="{ span: 8 }"
        :wrapper-col="{ span: 16 }"
        :colon="false"
        size="small"
      >
        <!-- ═══ ① 基本信息 ═══ -->
        <FormSection title="基本信息">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="车辆编码">
                <a-input
                  v-model:value="form.vehicleCode"
                  placeholder="由系统自动生成（VH 号段）"
                  size="small"
                  disabled
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="车牌号"
                name="plateNo"
              >
                <a-input
                  v-model:value="form.plateNo"
                  placeholder="如 京A12345"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item
                label="车辆类型"
                name="vehicleType"
              >
                <a-select
                  v-model:value="form.vehicleType"
                  placeholder="请选择"
                  size="small"
                  :options="VEHICLE_TYPE_OPTIONS"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="归属类型"
                name="ownershipType"
              >
                <a-select
                  v-model:value="form.ownershipType"
                  placeholder="请选择"
                  size="small"
                  :options="OWNERSHIP_OPTIONS"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item
                label="品牌"
                name="brand"
              >
                <a-input
                  v-model:value="form.brand"
                  placeholder="如 五菱"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="型号"
                name="model"
              >
                <a-input
                  v-model:value="form.model"
                  placeholder="如 荣光V"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item
                label="颜色"
                name="color"
              >
                <a-input
                  v-model:value="form.color"
                  placeholder="如 白色"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="发动机号"
                name="engineNo"
              >
                <a-input
                  v-model:value="form.engineNo"
                  placeholder="请输入发动机号"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item
                label="VIN（车架号）"
                name="vin"
              >
                <a-input
                  v-model:value="form.vin"
                  placeholder="17 位大写字母数字，非必填"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="所属部门">
                <a-input
                  v-model:value="form.department"
                  placeholder="请输入所属部门"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>

        <!-- ═══ ② 规格与载重（参与智能调度派单约束） ═══ -->
        <FormSection title="规格与载重" tip="核定载重 / 货厢容积将作为派单负载与超限校验依据">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="核定载重(kg)">
                <a-input-number
                  v-model:value="form.ratedLoad"
                  :min="0"
                  :max="99999"
                  :precision="2"
                  placeholder="0 - 99999"
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="核定载客(人)">
                <a-input-number
                  v-model:value="form.ratedPassenger"
                  :min="0"
                  :max="99"
                  :precision="0"
                  placeholder="0 - 99"
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="货厢容积(m³)">
                <a-input-number
                  v-model:value="form.cargoVolume"
                  :min="0"
                  :precision="2"
                  placeholder="请输入货厢容积"
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="保养间隔(km)">
                <a-input-number
                  v-model:value="form.maintenanceIntervalKm"
                  :min="0"
                  :precision="0"
                  placeholder="如 5000"
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>

        <!-- ═══ ③ 证件与期限（三证到期，编辑时即给剩余天数） ═══ -->
        <FormSection title="证件与期限" tip="保险 / 年检 / 营运证到期前 30 天在列表与到期提醒中告警">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item
                label="注册日期"
                name="registerDate"
              >
                <a-date-picker
                  v-model:value="form.registerDate"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择注册日期"
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="营运证号">
                <a-input
                  v-model:value="form.operatingPermitNo"
                  placeholder="非营运车辆可留空"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="营运证到期">
                <div class="date-with-hint">
                  <a-date-picker
                    v-model:value="form.operatingPermitExpireDate"
                    value-format="YYYY-MM-DD"
                    placeholder="请选择营运证到期日"
                    size="small"
                    style="flex: 1"
                  />
                  <span
                    v-if="certHint(form.operatingPermitExpireDate)"
                    :class="['cert-hint', certHintClass(form.operatingPermitExpireDate)]"
                  >{{ certHint(form.operatingPermitExpireDate) }}</span>
                </div>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="保险到期">
                <div class="date-with-hint">
                  <a-date-picker
                    v-model:value="form.insuranceExpireDate"
                    value-format="YYYY-MM-DD"
                    placeholder="请选择保险到期日"
                    size="small"
                    style="flex: 1"
                  />
                  <span
                    v-if="certHint(form.insuranceExpireDate)"
                    :class="['cert-hint', certHintClass(form.insuranceExpireDate)]"
                  >{{ certHint(form.insuranceExpireDate) }}</span>
                </div>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="年检到期">
                <div class="date-with-hint">
                  <a-date-picker
                    v-model:value="form.inspectionExpireDate"
                    value-format="YYYY-MM-DD"
                    placeholder="请选择年检到期日"
                    size="small"
                    style="flex: 1"
                  />
                  <span
                    v-if="certHint(form.inspectionExpireDate)"
                    :class="['cert-hint', certHintClass(form.inspectionExpireDate)]"
                  >{{ certHint(form.inspectionExpireDate) }}</span>
                </div>
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>

        <!-- ═══ ④ 归属与车主（个人自带/租赁时车主可与配送员不同） ═══ -->
        <FormSection title="车主信息" :tip="form.ownershipType === 1 ? '公司自有车辆可留空' : '个人自带 / 租赁车辆必填车主'">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="车主姓名">
                <a-input
                  v-model:value="form.ownerName"
                  placeholder="个人自带 / 租赁车辆填写"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="车主电话"
                name="ownerPhone"
              >
                <a-input
                  v-model:value="form.ownerPhone"
                  placeholder="请输入车主联系电话"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="24">
              <a-form-item
                label="备注"
                :label-col="{ span: 4 }"
                :wrapper-col="{ span: 20 }"
              >
                <a-textarea
                  v-model:value="form.remark"
                  placeholder="请输入备注"
                  :rows="2"
                  :maxlength="500"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>
      </a-form>
    </div>

    <!-- ═══ 底部按钮栏 ═══ -->
    <div class="form-footer">
      <a-space :size="12">
        <a-button
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          保存<span class="shortcut-hint">Ctrl+S</span>
        </a-button>
        <a-button @click="handleCancel">
          返回<span class="shortcut-hint">Esc</span>
        </a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 车辆新增 / 编辑表单页（配送 → 人车管理 → 车辆管理 → 添加，菜单 80770 双入口）
 *
 * 布局口径（2026-09-13 重构）：车辆是**档案（主数据）**，22 字段按语义分四区 + 两列栅格
 *   （对标仓库内资料模块金标准 `views/md/logistics/form.vue`，以及 Odoo form view 的 group 两列布局）。
 *   原实现用 `BillFormPage` 的 inline flow（单据表头快速录单用的单行内联布局），22 个字段挤成一条横带、
 *   无分组、报错只能弹 toast，属"把档案当单据录"，已废弃。
 *
 * 字段与后端 `VehicleDTO` 一一对应（必填与 @NotBlank/@NotNull 一致），见《车辆管理开发文档》§3.6.1。
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import FormSection from '@/components/FormSection/index.vue'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { vehicleApi } from '@/api/dms/vehicle'

const route = useRoute()
const router = useRouter()

const saving = ref(false)
const formRef = ref<FormInstance>()

const VEHICLE_TYPE_OPTIONS = [
  { label: '电动车', value: 1 }, { label: '小货车', value: 2 }, { label: '面包车', value: 3 },
  { label: '厢式货车', value: 4 }, { label: '冷藏车', value: 5 }, { label: '三轮车', value: 6 },
]

const OWNERSHIP_OPTIONS = [
  { label: '公司自有', value: 1 }, { label: '个人自带', value: 2 }, { label: '租赁', value: 3 },
]

const form = reactive<Record<string, any>>({
  id: undefined,
  vehicleCode: '',
  plateNo: '',
  vehicleType: undefined,
  ownershipType: undefined,
  brand: '',
  model: '',
  color: '',
  engineNo: '',
  vin: '',
  department: '',
  ratedLoad: undefined,
  ratedPassenger: undefined,
  cargoVolume: undefined,
  maintenanceIntervalKm: undefined,
  registerDate: undefined,
  operatingPermitNo: '',
  operatingPermitExpireDate: undefined,
  insuranceExpireDate: undefined,
  inspectionExpireDate: undefined,
  ownerName: '',
  ownerPhone: '',
  remark: '',
})

const rules: Record<string, any[]> = {
  plateNo: [
    { required: true, message: '请输入车牌号', trigger: 'blur' },
    { max: 20, message: '车牌号最长 20 位', trigger: 'blur' },
  ],
  vehicleType: [{ required: true, message: '请选择车辆类型', trigger: 'change' }],
  ownershipType: [{ required: true, message: '请选择归属类型', trigger: 'change' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  model: [{ required: true, message: '请输入型号', trigger: 'blur' }],
  color: [{ required: true, message: '请输入颜色', trigger: 'blur' }],
  engineNo: [{ required: true, message: '请输入发动机号', trigger: 'blur' }],
  registerDate: [{ required: true, message: '请选择注册日期', trigger: 'change' }],
  vin: [{ pattern: /^[A-HJ-NP-Z0-9]{17}$/, message: 'VIN 须为 17 位大写字母数字（不含 I/O/Q）', trigger: 'blur' }],
  ownerPhone: [{ pattern: /^[0-9-+() ]{6,20}$/, message: '车主电话格式不正确', trigger: 'blur' }],
}

const isEdit = computed(() => !!route.params.id || !!route.query.id)

/** 证件剩余天数提示（与列表页告警色一致：已过期红 / ≤30 天橙 / 正常绿） */
function certDaysLeft(dateStr?: string): number | null {
  if (!dateStr) return null
  const target = new Date(`${String(dateStr).slice(0, 10)}T00:00:00`)
  if (Number.isNaN(target.getTime())) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((target.getTime() - today.getTime()) / 86400000)
}

function certHint(dateStr?: string): string {
  const days = certDaysLeft(dateStr)
  if (days == null) return ''
  if (days < 0) return `已过期 ${-days} 天`
  if (days === 0) return '今日到期'
  return `剩余 ${days} 天`
}

function certHintClass(dateStr?: string): string {
  const days = certDaysLeft(dateStr)
  if (days == null) return ''
  if (days < 0) return 'cert-expired'
  if (days <= 30) return 'cert-warning'
  return 'cert-normal'
}

/** 去掉空串/undefined，避免把「清空」写成脏值；日期字段保持 YYYY-MM-DD 字符串 */
function buildPayload(): Record<string, any> {
  const payload: Record<string, any> = { ...form }
  delete payload.id
  delete payload.vehicleCode
  Object.keys(payload).forEach((k) => {
    if (payload[k] === undefined || payload[k] === '') delete payload[k]
  })
  return payload
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    message.warning('请先补全标红的必填项')
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (isEdit.value && form.id) {
      await vehicleApi.update(form.id, payload)
      message.success('保存成功')
    } else {
      await vehicleApi.create(payload)
      message.success('新增成功')
    }
    router.push('/dms/vehicle')
  } catch (error: any) {
    message.error(error?.response?.data?.message || (isEdit.value ? '保存失败' : '新增失败'))
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  router.push('/dms/vehicle')
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
    e.preventDefault()
    handleSave()
  }
  if (e.key === 'Escape') {
    handleCancel()
  }
}

async function loadDetail(id: number | string) {
  try {
    const detail: any = await vehicleApi.getById(Number(id))
    if (!detail) {
      message.error('车辆不存在')
      return
    }
    Object.keys(form).forEach((k) => {
      if (k in detail) form[k] = detail[k]
    })
  } catch (error: any) {
    message.error(error?.response?.data?.message || '加载车辆详情失败')
  }
}

onMounted(async () => {
  document.addEventListener('keydown', handleKeydown)
  const id = route.params.id || route.query.id
  if (id) {
    await loadDetail(id as string)
  } else {
    try {
      const code: any = await vehicleApi.nextCode()
      form.vehicleCode = code || ''
    } catch (error) {
      console.warn('[车辆管理] 生成车辆编码失败', error)
    }
  }
})

onBeforeUnmount(() => document.removeEventListener('keydown', handleKeydown))
</script>

<style scoped>
/* 与资料模块金标准（views/md/logistics/form.vue）一致的表单外壳 */
.form-page-wrapper { display: flex; flex-direction: column; height: 100%; overflow: hidden; background: #f5f7fa; }
.page-header { padding: 10px 16px; flex-shrink: 0; background: #fff; border-bottom: 1px solid #e8e8e8; }
.page-title { font-size: 15px; font-weight: 600; color: #262626; }

.form-scroll-area { flex: 1; overflow-y: auto; padding: 12px 16px; min-height: 0; }

.date-with-hint { display: flex; align-items: center; gap: 8px; }
.cert-hint { font-size: 12px; white-space: nowrap; }
.cert-expired { color: #cf1322; font-weight: 600; }
.cert-warning { color: #d46b08; }
.cert-normal { color: #389e0d; }

.form-footer {
  background: #fff;
  padding: 12px 24px;
  text-align: right;
  flex-shrink: 0;
  border-top: 1px solid #e8e8e8;
}

:deep(.ant-form-item) { margin-bottom: 12px; }
:deep(.ant-form-item-label > label) { font-size: 13px; color: #595959; }
:deep(.ant-form-item-required::before) { margin-right: 2px; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
