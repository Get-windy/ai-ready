<template>
  <div class="form-page-wrapper">
    <!-- ═══ 页头 ═══ -->
    <div class="page-header">
      <a-space :size="8">
        <a-button
          type="text"
          size="small"
          @click="handleBack"
        >
          <template #icon>
            <ArrowLeftOutlined />
          </template>
          返回
        </a-button>
        <span class="page-title">{{ isEdit ? '配送员 - 修改' : '配送员 - 新增' }}</span>
        <a-tag
          v-if="form.riderNo"
          color="blue"
        >
          {{ form.riderNo }}
        </a-tag>
        <a-tag
          v-if="isEdit && form.verifyStatus !== undefined"
          :color="RIDER_VERIFY_MAP[form.verifyStatus]?.color || 'default'"
        >
          {{ RIDER_VERIFY_MAP[form.verifyStatus]?.text || '-' }}
        </a-tag>
        <a-tag :color="RIDER_TYPE_MAP[form.riderType]?.color || 'default'">
          {{ RIDER_TYPE_MAP[form.riderType]?.text || '未选择类型' }}
        </a-tag>
      </a-space>
    </div>

    <div class="form-scroll-area">
      <!--
        分区栅格表单（对齐同模块《车辆管理》views/dms/vehicle/form.vue，源头是《资料模块》金标准 views/md/logistics/form.vue）
        · 配送员是「档案（主数据）」而非「单据」：字段多、语义分组明确、编辑频率低
          → 五分区两列栅格 + 行内校验，不再用 BillFormPage 的 inline flow（那是单据表头快速录单用的单行内联布局）。
        · 字段与后端 DmsRider 一一对应，必填与 RiderService.validateByType 保持一致（按类型差异化）。
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
        <FormSection title="基本信息" tip="姓名与手机号为必填；编号留空由系统按 PSY 号段生成">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item
                label="配送员编号"
                name="riderNo"
              >
                <a-input
                  v-model:value="form.riderNo"
                  placeholder="由系统自动生成（PSY0001）"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="姓名"
                name="realName"
              >
                <a-input
                  v-model:value="form.realName"
                  placeholder="请输入配送员姓名"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item
                label="手机号"
                name="phone"
              >
                <a-input
                  v-model:value="form.phone"
                  placeholder="11 位手机号"
                  size="small"
                  :maxlength="11"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                label="配送员类型"
                name="riderType"
              >
                <a-select
                  v-model:value="form.riderType"
                  placeholder="请选择配送员类型"
                  size="small"
                  :options="RIDER_TYPE_OPTIONS"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="接单状态">
                <a-select
                  v-model:value="form.status"
                  size="small"
                  :options="RIDER_STATUS_OPTIONS"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="头像地址">
                <a-input
                  v-model:value="form.avatarUrl"
                  placeholder="选填，头像图片 URL"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>

        <!-- ═══ ② 归属与组织（按类型差异化） ═══ -->
        <FormSection title="归属与组织" :tip="belongTip">

          <!-- 企业员工：系统账号 / 部门 / 入职日期 -->
          <template v-if="form.riderType === 1">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="关联系统账号"
                  name="userId"
                >
                  <a-select
                    v-model:value="form.userId"
                    placeholder="选择系统用户"
                    size="small"
                    show-search
                    option-filter-prop="label"
                    :options="userOptions"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="所属部门">
                  <a-select
                    v-model:value="form.deptName"
                    placeholder="选择部门"
                    size="small"
                    show-search
                    option-filter-prop="label"
                    :options="deptOptions"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="入职日期">
                  <a-date-picker
                    v-model:value="form.entryDate"
                    value-format="YYYY-MM-DD"
                    placeholder="请选择入职日期"
                    size="small"
                    style="width: 100%"
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </template>

          <!-- 外部平台配送员：归属渠道 / 平台骑手ID -->
          <template v-else-if="form.riderType === 3">
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item
                  label="归属渠道"
                  name="channelId"
                >
                  <a-select
                    v-model:value="form.channelId"
                    placeholder="选择运力渠道（在《渠道管理》维护）"
                    size="small"
                    show-search
                    option-filter-prop="label"
                    :options="channelOptions"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item
                  label="平台骑手ID"
                  name="platformRiderId"
                >
                  <a-input
                    v-model:value="form.platformRiderId"
                    placeholder="第三方平台的骑手编号"
                    size="small"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </template>

          <!-- 众包兼职 / 社会车辆司机：结算方式（社会车辆另需车牌） -->
          <template v-else>
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="结算方式">
                  <a-select
                    v-model:value="form.settleMethod"
                    placeholder="请选择结算方式"
                    size="small"
                    :options="settleOptions"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
              <a-col
                v-if="form.riderType === 4"
                :span="12"
              >
                <a-form-item
                  label="自带车牌号"
                  name="vehicleNo"
                >
                  <a-input
                    v-model:value="form.vehicleNo"
                    placeholder="社会车辆司机必填，如 苏A12345"
                    size="small"
                    allow-clear
                  />
                </a-form-item>
              </a-col>
            </a-row>
          </template>
        </FormSection>

        <!-- ═══ ③ 资格与证件 ═══ -->
        <FormSection title="资格与证件" tip="审核通过且证照未过期才可接单（资质门控）">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="身份证号">
                <a-input
                  v-model:value="form.idCard"
                  placeholder="18 位身份证号，列表展示自动脱敏"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="form.riderType === 3 ? '渠道背书有效期' : '资质到期日期'">
                <div class="date-with-hint">
                  <a-date-picker
                    v-model:value="form.qualificationExpireDate"
                    value-format="YYYY-MM-DD"
                    :placeholder="form.riderType === 3 ? '渠道背书有效期' : '证照有效期'"
                    size="small"
                    style="flex: 1"
                  />
                  <span
                    v-if="expireHint"
                    :class="['cert-hint', expireHintClass]"
                  >{{ expireHint }}</span>
                </div>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="驾驶证号">
                <a-input
                  v-model:value="form.driverLicense"
                  placeholder="选填"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="健康证号">
                <a-input
                  v-model:value="form.healthCertNo"
                  placeholder="选填"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>

        <!-- ═══ ④ 运力与排班 ═══ -->
        <FormSection title="运力与排班" tip="供调度按评分 / 负载 / 服务半径选人">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="车辆类型">
                <a-select
                  v-model:value="form.vehicleType"
                  placeholder="请选择"
                  size="small"
                  :options="vehicleTypeOptions"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item
                v-if="form.riderType !== 4"
                label="车牌号"
              >
                <a-input
                  v-model:value="form.vehicleNo"
                  placeholder="驾驶车辆车牌（选填）"
                  size="small"
                  allow-clear
                />
              </a-form-item>
              <a-form-item
                v-else
                label="最大并行单数"
              >
                <a-input-number
                  v-model:value="form.maxConcurrent"
                  :min="1"
                  :max="99"
                  placeholder="同时可接单量"
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="服务半径(km)">
                <a-input-number
                  v-model:value="form.serviceRadius"
                  :min="0"
                  :precision="1"
                  placeholder="如 5"
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col
              v-if="form.riderType !== 4"
              :span="12"
            >
              <a-form-item label="最大并行单数">
                <a-input-number
                  v-model:value="form.maxConcurrent"
                  :min="1"
                  :max="99"
                  placeholder="同时可接单量"
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="上班时间">
                <a-input
                  v-model:value="form.workHoursStart"
                  placeholder="HH:mm，如 09:00"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="下班时间">
                <a-input
                  v-model:value="form.workHoursEnd"
                  placeholder="HH:mm，如 18:00"
                  size="small"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="保证金(元)">
                <a-input-number
                  v-model:value="form.depositAmount"
                  :min="0"
                  :precision="2"
                  placeholder="选填"
                  size="small"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </FormSection>

        <!-- ═══ ⑤ 备注 ═══ -->
        <FormSection title="备注">
          <a-form-item
            label="备注"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 20 }"
          >
            <a-textarea
              v-model:value="form.remark"
              :rows="3"
              :maxlength="500"
              show-count
              placeholder="补充说明（排班偏好、特殊约定等）"
            />
          </a-form-item>
        </FormSection>
      </a-form>
    </div>

    <!-- ═══ 底部固定操作条 ═══ -->
    <div class="form-footer">
      <a-button
        size="large"
        @click="handleBack"
      >
        返回
      </a-button>
      <a-button
        type="primary"
        size="large"
        :loading="saving"
        @click="handleSave"
      >
        保存<span class="shortcut-hint">Ctrl+S</span>
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import FormSection from '@/components/FormSection/index.vue'
import {
  riderApi, RIDER_TYPE_MAP, RIDER_TYPE_OPTIONS, RIDER_STATUS_OPTIONS, RIDER_VERIFY_MAP,
} from '@/api/dms/rider'
import { channelApi } from '@/api/dms/channel'
import { userApi } from '@/api/user'
import { departmentApi } from '@/api/department'

defineOptions({ name: 'DmsRiderForm' })

const route = useRoute()
const router = useRouter()

const editId = computed(() => {
  const id = route.params.id || route.query.id
  return id ? Number(id) : null
})
const isEdit = computed(() => editId.value != null)

const saving = ref(false)
const formRef = ref<FormInstance>()

const form = ref<Record<string, any>>({
  riderNo: '',
  realName: '',
  phone: '',
  riderType: 1,
  status: 0,
  avatarUrl: '',
  userId: undefined,
  deptName: undefined,
  entryDate: undefined,
  channelId: undefined,
  platformRiderId: '',
  idCard: '',
  driverLicense: '',
  healthCertNo: '',
  qualificationExpireDate: undefined,
  settleMethod: undefined,
  vehicleType: undefined,
  vehicleNo: '',
  serviceRadius: undefined,
  maxConcurrent: undefined,
  workHoursStart: '',
  workHoursEnd: '',
  depositAmount: undefined,
  remark: '',
  verifyStatus: undefined,
})

// ═══ 选择器数据源 ═══
const userOptions = ref<{ label: string; value: string | number }[]>([])
const deptOptions = ref<{ label: string; value: string }[]>([])
const deptIdMap = ref<Record<string, number>>({})
const channelOptions = ref<{ label: string; value: number }[]>([])

const settleOptions = [
  { label: '按单结算', value: 1 },
  { label: '月结', value: 2 },
  { label: '时段结算', value: 3 },
]
const vehicleTypeOptions = [
  { label: '电动车', value: '电动车' },
  { label: '摩托车', value: '摩托车' },
  { label: '汽车', value: '汽车' },
  { label: '货车', value: '货车' },
]

async function loadOptions() {
  try {
    const res: any = await userApi.getList({ pageSize: 500 })
    const rows = Array.isArray(res) ? res : (res?.records || [])
    userOptions.value = rows.map((u: any) => ({
      label: u.realName || u.nickname || u.username,
      value: String(u.id),
    }))
  } catch (error) {
    console.warn('[配送员] 系统账号加载失败', error)
  }
  try {
    const res: any = await departmentApi.getList()
    const rows = Array.isArray(res) ? res : (res?.records || [])
    deptOptions.value = rows.map((d: any) => ({
      label: d.departmentName || d.deptName,
      value: d.departmentName || d.deptName,
    }))
    const map: Record<string, number> = {}
    rows.forEach((d: any) => {
      const name = d.departmentName || d.deptName
      if (name) map[name] = d.id
    })
    deptIdMap.value = map
  } catch (error) {
    console.warn('[配送员] 部门加载失败', error)
  }
  try {
    const res: any = await channelApi.page({ pageNum: 1, pageSize: 200 })
    const rows = res?.records || []
    channelOptions.value = rows.map((c: any) => ({ label: c.channelName, value: c.id }))
  } catch (error) {
    console.warn('[配送员] 渠道加载失败', error)
  }
}

// ═══ 分区提示语（按类型） ═══
const belongTip = computed(() => {
  if (form.value.riderType === 1) return '企业员工：绑定系统账号与部门，走内部考勤/绩效'
  if (form.value.riderType === 3) return '外部平台配送员：归属渠道由渠道方背书资质'
  if (form.value.riderType === 4) return '社会车辆司机：自带车辆，按结算方式计费'
  return '众包兼职：按单结算的社会运力'
})

// ═══ 资质到期提示（对齐车辆表单的到期告警） ═══
const expireHint = computed(() => {
  const d = form.value.qualificationExpireDate
  if (!d) return ''
  const target = new Date(`${d}T00:00:00`)
  if (Number.isNaN(target.getTime())) return ''
  const days = Math.floor((target.getTime() - Date.now()) / 86400000)
  if (days < 0) return `已过期 ${Math.abs(days)} 天，不可接单`
  if (days <= 30) return `剩 ${days} 天到期`
  return ''
})
const expireHintClass = computed(() => {
  const d = form.value.qualificationExpireDate
  if (!d) return ''
  const target = new Date(`${d}T00:00:00`)
  if (Number.isNaN(target.getTime())) return ''
  const days = Math.floor((target.getTime() - Date.now()) / 86400000)
  if (days < 0) return 'cert-expired'
  if (days <= 30) return 'cert-warning'
  return 'cert-normal'
})

// ═══ 校验规则（按类型差异化，与后端 RiderService.validateByType 对齐） ═══
const rules = computed<Record<string, Rule[]>>(() => {
  const base: Record<string, Rule[]> = {
    realName: [
      { required: true, message: '请输入配送员姓名', trigger: 'blur' },
      { min: 2, max: 20, message: '姓名长度 2-20 个字符', trigger: 'blur' },
    ],
    phone: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
    ],
    riderType: [{ required: true, message: '请选择配送员类型', trigger: 'change' }],
  }
  if (form.value.riderType === 1) {
    base.userId = [{ required: true, message: '企业员工必须关联系统账号', trigger: 'change' }]
  }
  if (form.value.riderType === 3) {
    base.channelId = [{ required: true, message: '外部平台配送员必须选择归属渠道', trigger: 'change' }]
    base.platformRiderId = [{ required: true, message: '外部平台配送员必须填写平台骑手ID', trigger: 'blur' }]
  }
  if (form.value.riderType === 4) {
    base.vehicleNo = [{ required: true, message: '社会车辆司机必须填写车牌号', trigger: 'blur' }]
  }
  return base
})

// ═══ 保存 ═══
async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    message.warning('请先补全标红的必填项')
    return
  }
  saving.value = true
  try {
    // 按当前类型裁剪 payload，避免把其它类型的专属字段写进库
    const payload: Record<string, any> = { ...form.value }
    if (payload.riderType !== 1) {
      payload.userId = null
      payload.deptName = null
      payload.deptId = null
      payload.entryDate = null
    } else {
      payload.deptId = payload.deptName ? deptIdMap.value[payload.deptName] : null
    }
    if (payload.riderType !== 3) {
      payload.channelId = null
      payload.platformRiderId = null
    }
    if (payload.riderType !== 2 && payload.riderType !== 4) {
      payload.settleMethod = null
    }
    if (payload.riderType !== 4) {
      // 非社会车辆司机的车牌在「运力与排班」区，保留（自有车辆描述）
    }
    delete payload.verifyStatus

    if (isEdit.value && editId.value) {
      await riderApi.update(editId.value, payload)
      message.success('修改成功')
    } else {
      await riderApi.create(payload)
      message.success('新增成功')
    }
    router.push('/dms/rider')
  } catch (error: any) {
    message.error(error?.response?.data?.message || error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleBack() {
  router.push('/dms/rider')
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 's') {
    e.preventDefault()
    handleSave()
  }
}

onMounted(async () => {
  document.addEventListener('keydown', handleKeydown)
  await loadOptions()
  if (editId.value) {
    try {
      const detail: any = await riderApi.getById(editId.value)
      if (detail) {
        Object.assign(form.value, {
          riderNo: detail.riderNo,
          realName: detail.realName,
          phone: detail.phone,
          riderType: detail.riderType,
          status: detail.status,
          avatarUrl: detail.avatarUrl,
          userId: detail.userId != null ? String(detail.userId) : undefined,
          deptName: detail.deptName,
          entryDate: detail.entryDate,
          channelId: detail.channelId,
          platformRiderId: detail.platformRiderId,
          idCard: detail.idCard,
          driverLicense: detail.driverLicense,
          healthCertNo: detail.healthCertNo,
          qualificationExpireDate: detail.qualificationExpireDate,
          settleMethod: detail.settleMethod,
          vehicleType: detail.vehicleType,
          vehicleNo: detail.vehicleNo,
          serviceRadius: detail.serviceRadius,
          maxConcurrent: detail.maxConcurrent,
          workHoursStart: detail.workHoursStart,
          workHoursEnd: detail.workHoursEnd,
          depositAmount: detail.depositAmount,
          remark: detail.remark,
          verifyStatus: detail.verifyStatus,
        })
      }
    } catch (error: any) {
      message.error(error?.response?.data?.message || '加载详情失败')
    }
  } else {
    try {
      const code: any = await riderApi.nextCode()
      form.value.riderNo = code || ''
    } catch (error) {
      console.warn('[配送员] 生成编号失败', error)
    }
  }
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* 与同模块《车辆管理》/《资料模块》金标准一致的表单外壳 */
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
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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
