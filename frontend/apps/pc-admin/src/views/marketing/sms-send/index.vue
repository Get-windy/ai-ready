<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        发短信（营销 → 营销活动 → 发短信，菜单 80310）
        对标 ql361「营销 → 营销活动 → 发短信」：**三 Tab 复合页**
          · Tab1「发短信」：短信填写与发送表单（选择客户 / 短信内容 / 短信模板 / 公司签名 / 短信类型 /
            短信协议 / 手机预览 / 确认发送），右侧手机气泡预览
          · Tab2「短信历史」7 列：接收人 / 手机号 / 发送时间 / 经手人 / 短信内容 / 发送状态 / 失败原因
          · Tab3「短信模板管理」4 列：模版标题 / 模版内容 / 短信类型 / 最后修改时间；行级 修改 / 删除
        对标文档：docs/Yh-Spec/手动整理对标开发文档/营销模块/发短信开发文档.md
        计费口径：67 字/条，超出按新一条计；账户剩余短信条数为配额（对标实测 100 条）
        投递链路口径：发送统一入平台消息底座（sys_message），投递状态/失败原因从 sys_message 实时读取
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="activeTab !== 'send'"
        @tab-change="handleTabChange"
      >
        <template #toolbar-left>
          <a-button
            v-if="activeTab === 'template'"
            type="primary"
            size="small"
            class="btn-add"
            @click="openTemplateCreate"
          >
            <PlusOutlined /> 新增模板
          </a-button>
          <a-button
            v-if="activeTab === 'optout'"
            type="primary"
            size="small"
            class="btn-add"
            @click="openOptOutCreate"
          >
            <PlusOutlined /> 登记退订
          </a-button>
          <a-button
            v-if="activeTab === 'send'"
            size="small"
            @click="openComplianceSetting"
          >
            <SettingOutlined /> 合规设置
          </a-button>
        </template>

        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="activeTab !== 'send'"
              size="small"
              :loading="loading"
              @click="fetchList"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="activeTab !== 'send'"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
          </a-space>
        </template>

        <!-- ═══ Tab2/Tab3 查询区 ═══ -->
        <template #search-fields>
          <!-- ⚠️ 外层 div 必须常驻：只写 v-if 时插槽内容退化为注释节点，
               CategoryListLayout 会回落到内置「查询」按钮（发送表单页不该有） -->
          <div class="search-area">
            <div
              v-if="activeTab !== 'send'"
              class="search-row"
            >
              <template v-if="activeTab === 'history'">
                <span class="search-label">接收人</span>
                <a-input
                  v-model:value="historySearch.receiver"
                  placeholder="请输入接收人"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">手机号</span>
                <a-input
                  v-model:value="historySearch.mobile"
                  placeholder="请输入手机号"
                  size="small"
                  style="width: 150px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">发送状态</span>
                <a-select
                  v-model:value="historySearch.sendStatus"
                  placeholder="全部状态"
                  size="small"
                  style="width: 130px"
                  allow-clear
                  :options="SEND_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <template v-else-if="activeTab === 'optout'">
                <span class="search-label">手机号</span>
                <a-input
                  v-model:value="optOutSearch.mobile"
                  placeholder="请输入手机号"
                  size="small"
                  style="width: 180px"
                  allow-clear
                  @press-enter="handleSearch"
                />
              </template>
              <template v-else>
                <span class="search-label">模版标题</span>
                <a-input
                  v-model:value="templateSearch.keyword"
                  placeholder="请输入模版标题/内容"
                  size="small"
                  style="width: 220px"
                  allow-clear
                  @press-enter="handleSearch"
                />
                <span class="search-label">短信类型</span>
                <a-select
                  v-model:value="templateSearch.smsType"
                  placeholder="全部类型"
                  size="small"
                  style="width: 140px"
                  allow-clear
                  :options="SMS_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </template>
              <a-button
                type="primary"
                size="small"
                @click="searchOwner"
              >
                查询
              </a-button>
            </div>
          </div>
        </template>

        <template #table>
          <div class="table-area">
            <!-- ═══ Tab1 发短信（表单 + 手机预览） ═══ -->
            <div
              v-if="activeTab === 'send'"
              class="send-panel"
            >
              <div class="send-form">
                <a-alert
                  type="warning"
                  show-icon
                  :message="`温馨提示：目前剩余短信${setting.quotaRemain ?? 0}条，如需购买短信，请联系客服！`"
                  class="quota-alert"
                />
                <a-form
                  :label-col="{ span: 4 }"
                  :wrapper-col="{ span: 19 }"
                  size="small"
                >
                  <a-form-item label="选择客户">
                    <a-space>
                      <span>已选择 <b>{{ selectedCustomers.length }}</b> 人</span>
                      <a-button
                        type="primary"
                        size="small"
                        class="btn-add"
                        @click="customerPickerOpen = true"
                      >
                        选择
                      </a-button>
                      <a-button
                        v-if="selectedCustomers.length"
                        size="small"
                        type="link"
                        @click="selectedCustomers = []"
                      >
                        清空
                      </a-button>
                    </a-space>
                  </a-form-item>
                  <a-form-item label="短信内容">
                    <a-textarea
                      v-model:value="sendForm.content"
                      :rows="5"
                      :maxlength="320"
                      placeholder="请填写短信内容~"
                    />
                    <div class="char-counter">
                      <span class="tip">提示: 每超出67字将按照新一条短信发送</span>
                      <span>{{ sendForm.content.length }}/67</span>
                    </div>
                  </a-form-item>
                  <a-form-item label="选择短信模板">
                    <a-space>
                      <a-button
                        size="small"
                        @click="templatePickerOpen = true"
                      >
                        选择短信模板
                      </a-button>
                      <span
                        v-if="pickedTemplate"
                        class="selected-tip"
                      >{{ pickedTemplate.templateTitle }}</span>
                    </a-space>
                  </a-form-item>
                  <a-form-item label="公司签名">
                    <a-input
                      v-model:value="sendForm.signName"
                      style="width: 260px"
                      placeholder="请输入公司签名"
                    />
                  </a-form-item>
                  <a-form-item label="短信类型">
                    <a-radio-group v-model:value="sendForm.smsType">
                      <a-radio value="NOTICE">
                        通知短信 <QuestionCircleOutlined class="field-help" />
                      </a-radio>
                      <a-radio value="MARKETING">
                        营销短信 <QuestionCircleOutlined class="field-help" />
                      </a-radio>
                    </a-radio-group>
                  </a-form-item>
                  <a-form-item label="短信协议">
                    <a-checkbox v-model:checked="sendForm.agreed">
                      是否同意短信协议
                    </a-checkbox>
                    <a
                      class="protocol-link"
                      @click="protocolOpen = true"
                    >点击查看《短信协议》</a>
                  </a-form-item>
                  <a-form-item :wrapper-col="{ offset: 20, span: 4 }">
                    <a-button
                      type="primary"
                      :loading="sending"
                      :disabled="!canSend"
                      @click="handleSend"
                    >
                      确认发送
                    </a-button>
                  </a-form-item>
                </a-form>
              </div>

              <!-- 手机预览 -->
              <div class="phone-preview">
                <div class="phone-body">
                  <div class="phone-bubble">
                    {{ previewText }}
                  </div>
                </div>
              </div>
            </div>

            <!-- ═══ Tab2 短信历史 ═══ -->
            <BillDetailTable
              v-else-if="activeTab === 'history'"
              v-model:data-source="historyData"
              :columns="historyColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-sms-history-table-columns"
              global-config-key="marketing-sms-history-table-columns"
            >
              <template #sendTimeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtTime(record.sendTime) }}</span>
              </template>
              <template #statusCell="{ record }">
                <a-tag
                  v-if="!record.__ghost"
                  :color="SEND_STATUS_COLOR[record.sendStatus] || 'default'"
                >
                  {{ record.sendStatus }}
                </a-tag>
              </template>
            </BillDetailTable>

            <!-- ═══ Tab4 退订名单（合规） ═══ -->
            <BillDetailTable
              v-else-if="activeTab === 'optout'"
              v-model:data-source="optOutData"
              :columns="optOutColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-sms-optout-table-columns"
              global-config-key="marketing-sms-optout-table-columns"
            >
              <template #mobileCell="{ record }">
                <span v-if="!record.__ghost">{{ maskMobile(record.mobile) }}</span>
              </template>
              <template #sourceCell="{ record }">
                <span v-if="!record.__ghost">{{ OPT_OUT_SOURCE_MAP[record.source] || record.source || '-' }}</span>
              </template>
              <template #optOutTimeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtTime(record.optOutTime) }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleRemoveOptOut(record)"
                  >
                    移出名单
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>

            <!-- ═══ Tab3 短信模板管理 ═══ -->
            <BillDetailTable
              v-else
              v-model:data-source="templateData"
              :columns="templateColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="id"
              storage-key="marketing-sms-template-table-columns"
              global-config-key="marketing-sms-template-table-columns"
            >
              <template #titleCell="{ record }">
                <a
                  v-if="!record.__ghost"
                  class="cell-link"
                  @click="openTemplateEdit(record)"
                >{{ record.templateTitle }}</a>
              </template>
              <template #typeCell="{ record }">
                <span v-if="!record.__ghost">{{ SMS_TYPE_MAP[record.smsType] || record.smsType || '-' }}</span>
              </template>
              <template #updateTimeCell="{ record }">
                <span v-if="!record.__ghost">{{ fmtTime(record.updateTime) }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space
                  v-if="!record.__ghost"
                  :size="0"
                >
                  <a-button
                    type="link"
                    size="small"
                    @click="openTemplateEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    danger
                    @click="handleTemplateDelete(record)"
                  >
                    删除
                  </a-button>
                </a-space>
              </template>
            </BillDetailTable>
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 选择客户 ═══ -->
      <PartnerSelectModal
        v-model:open="customerPickerOpen"
        default-tab="customer"
        @select="handleCustomerPicked"
      />

      <!-- ═══ 选择短信模板 ═══ -->
      <a-modal
        v-model:open="templatePickerOpen"
        title="选择短信模板"
        width="760px"
        :footer="null"
      >
        <a-table
          :columns="PICK_COLUMNS"
          :data-source="templateOptions"
          :loading="templateLoading"
          :pagination="false"
          row-key="id"
          size="small"
          :scroll="{ y: 360 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'action'">
              <a-button
                type="link"
                size="small"
                @click="pickTemplate(record)"
              >
                选择
              </a-button>
            </template>
          </template>
        </a-table>
      </a-modal>

      <!-- ═══ 短信协议 ═══ -->
      <a-modal
        v-model:open="protocolOpen"
        title="短信协议"
        :footer="null"
        width="640px"
      >
        <div class="protocol-body">
          <p>1. 短信发送须遵守《通信短信息服务管理规定》，营销短信须提供退订方式（拒收请回复R）。</p>
          <p>2. 短信内容每 67 字计一条，超出部分按新一条计费。</p>
          <p>3. 不得发送违法违规、虚假宣传、骚扰性内容；因内容违规产生的责任由发送方承担。</p>
          <p>4. 短信发送时间应避开夜间休息时段，营销短信受更严格的时段与内容管制。</p>
        </div>
      </a-modal>

      <!-- ═══ 合规设置（发送时段 / 频控 / 签名）——四件套之三 ═══ -->
      <a-modal
        v-model:open="complianceOpen"
        title="短信合规设置"
        :confirm-loading="complianceSaving"
        width="560px"
        @ok="handleSaveCompliance"
      >
        <a-form
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 15 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="公司签名">
            <a-input v-model:value="complianceForm.signName" />
          </a-form-item>
          <a-form-item label="允许发送时段">
            <a-space>
              <a-input-number
                v-model:value="complianceForm.sendStartHour"
                :min="0"
                :max="23"
                :precision="0"
                style="width: 90px"
              />
              <span>时 至</span>
              <a-input-number
                v-model:value="complianceForm.sendEndHour"
                :min="1"
                :max="24"
                :precision="0"
                style="width: 90px"
              />
              <span>时</span>
            </a-space>
          </a-form-item>
          <a-form-item label="发送频控">
            <a-space>
              <a-input-number
                v-model:value="complianceForm.freqLimitDays"
                :min="0"
                :precision="0"
                style="width: 90px"
              />
              <span>天内同一号码最多</span>
              <a-input-number
                v-model:value="complianceForm.freqLimitCount"
                :min="0"
                :precision="0"
                style="width: 90px"
              />
              <span>条</span>
            </a-space>
          </a-form-item>
          <a-form-item label="剩余短信">
            配额 {{ setting.quotaTotal ?? 0 }} 条 / 已用 {{ setting.quotaUsed ?? 0 }} 条 / 剩余
            <b>{{ setting.quotaRemain ?? 0 }}</b> 条
          </a-form-item>
          <a-alert
            type="info"
            show-icon
            message="合规提示"
            description="发送时段默认 8:00–21:00（深夜发送属加重责任情形）；退订名单中的号码在任何情况下都不会被发送，且不得换名义再发。"
          />
        </a-form>
      </a-modal>

      <!-- ═══ 登记退订（回复R / 人工登记 / 客户主动要求） ═══ -->
      <a-modal
        v-model:open="optOutFormOpen"
        title="登记退订"
        :confirm-loading="optOutSaving"
        width="520px"
        @ok="handleSaveOptOut"
      >
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="手机号">
            <a-input
              v-model:value="optOutForm.mobile"
              placeholder="请输入退订手机号"
              :maxlength="20"
            />
          </a-form-item>
          <a-form-item label="客户名称">
            <a-input
              v-model:value="optOutForm.receiverName"
              placeholder="请输入客户名称"
            />
          </a-form-item>
          <a-form-item label="退订来源">
            <a-select
              v-model:value="optOutForm.source"
              :options="OPT_OUT_SOURCE_OPTIONS"
            />
          </a-form-item>
          <a-form-item label="备注">
            <a-input
              v-model:value="optOutForm.remark"
              placeholder="请输入备注"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- ═══ 新增/修改短信模板 ═══ -->
      <a-modal
        v-model:open="templateFormOpen"
        :title="editingTemplateId ? '修改短信模板' : '新增短信模板'"
        :confirm-loading="templateSaving"
        width="620px"
        @ok="handleTemplateSave"
      >
        <a-form
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 18 }"
          size="small"
          style="margin-top: 12px"
        >
          <a-form-item label="模版标题">
            <a-input
              v-model:value="templateForm.templateTitle"
              placeholder="请输入模版标题"
            />
          </a-form-item>
          <a-form-item label="短信类型">
            <a-select
              v-model:value="templateForm.smsType"
              :options="SMS_TYPE_OPTIONS"
            />
          </a-form-item>
          <a-form-item label="模版内容">
            <a-textarea
              v-model:value="templateForm.templateContent"
              :rows="4"
              :maxlength="500"
              placeholder="请输入模版内容，可使用 {联系人名称}{联系人性别} 占位符"
            />
          </a-form-item>
        </a-form>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import { PlusOutlined, ReloadOutlined, PrinterOutlined, QuestionCircleOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PartnerSelectModal from '@/components/PartnerSelectModal/index.vue'
import { smsMarketingApi, type SmsSetting, type SmsTemplate } from '@/api/marketing'

defineOptions({ name: 'MarketingSmsSend' })

const TABS = [
  { key: 'send', label: '发短信' },
  { key: 'history', label: '短信历史' },
  { key: 'template', label: '短信模板管理' },
  // 本系统新增（对标三 Tab 之外的合规页签）：退订名单是《网络交易监督管理办法》第 16 条的刚性要求
  { key: 'optout', label: '退订名单' },
]
const activeTab = ref('send')

const SMS_TYPE_MAP: Record<string, string> = { NOTICE: '通知短信', MARKETING: '营销短信' }
const SMS_TYPE_OPTIONS = [
  { value: 'NOTICE', label: '通知短信' },
  { value: 'MARKETING', label: '营销短信' },
]
const SEND_STATUS_OPTIONS = [
  { value: 'PENDING', label: '待发送' },
  { value: 'SENT', label: '发送成功' },
  { value: 'FAILED', label: '发送失败' },
]
const SEND_STATUS_COLOR: Record<string, string> = { 待发送: 'default', 发送成功: 'green', 发送失败: 'red' }

const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ Tab1 发短信 ═══
const setting = ref<SmsSetting>({})
const selectedCustomers = ref<any[]>([])
const customerPickerOpen = ref(false)
const templatePickerOpen = ref(false)
const templateLoading = ref(false)
const templateOptions = ref<SmsTemplate[]>([])
const pickedTemplate = ref<SmsTemplate | null>(null)
const protocolOpen = ref(false)
const sending = ref(false)

const sendForm = reactive({
  content: '',
  signName: '',
  smsType: 'NOTICE',
  agreed: false,
})

const previewText = computed(() => {
  const sign = sendForm.signName ? `【${sendForm.signName}】` : ''
  const suffix = sendForm.smsType === 'MARKETING' ? '拒收请回复R' : ''
  return `${sign}${sendForm.content}${suffix}` || '（短信预览）'
})
const canSend = computed(() => !!sendForm.content.trim() && selectedCustomers.value.length > 0 && sendForm.agreed)

function handleCustomerPicked(record: any) {
  if (!record?.id) return
  if (!selectedCustomers.value.some(c => String(c.id) === String(record.id))) {
    selectedCustomers.value.push({ id: record.id, name: record.partyName || record.partnerName })
  }
}

async function loadSetting() {
  try {
    setting.value = await smsMarketingApi.getSetting()
    if (!sendForm.signName) sendForm.signName = setting.value?.signName || ''
  } catch (e) {
    console.error('[发短信] 加载短信设置失败', e)
  }
}

async function loadTemplateOptions() {
  templateLoading.value = true
  try {
    templateOptions.value = await smsMarketingApi.templateList()
  } catch (e) {
    console.error('[发短信] 加载短信模板失败', e)
    templateOptions.value = []
  } finally {
    templateLoading.value = false
  }
}

function pickTemplate(tpl: SmsTemplate) {
  pickedTemplate.value = tpl
  sendForm.content = tpl.templateContent || ''
  sendForm.smsType = tpl.smsType || 'NOTICE'
  if (tpl.updateTime) sendForm.agreed = sendForm.agreed
  templatePickerOpen.value = false
}

async function handleSend() {
  if (!sendForm.agreed) {
    message.warning('请先勾选同意短信协议')
    return
  }
  sending.value = true
  try {
    const res = await smsMarketingApi.send({
      partnerIds: selectedCustomers.value.map(c => c.id),
      content: sendForm.content,
      signName: sendForm.signName,
      smsType: sendForm.smsType,
      agreed: sendForm.agreed,
    })
    message.success(`已提交发送 ${res?.sentCount ?? 0} 条（批次 ${res?.batchNo || ''}）`)
    await loadSetting()
    if (activeTab.value === 'history') fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

const PICK_COLUMNS = [
  { title: '模版标题', dataIndex: 'templateTitle', key: 'templateTitle', width: 160 },
  { title: '模版内容', dataIndex: 'templateContent', key: 'templateContent' },
  { title: '短信类型', dataIndex: 'smsType', key: 'smsType', width: 110 },
  { title: '操作', key: 'action', width: 80 },
]

// ═══ Tab2 短信历史 ═══
const historyData = ref<any[]>([])
const historySearch = reactive({ receiver: '', mobile: '', sendStatus: undefined as string | undefined })
const historyColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'receiverName', title: '接收人', type: 'input', width: 160 },
  { key: 'mobile', title: '手机号', type: 'input', width: 140 },
  { key: 'sendTime', title: '发送时间', type: 'slot', slotName: 'sendTimeCell', width: 170 },
  { key: 'handlerName', title: '经手人', type: 'input', width: 120 },
  { key: 'content', title: '短信内容', type: 'input', width: 420 },
  { key: 'sendStatus', title: '发送状态', type: 'slot', slotName: 'statusCell', width: 110 },
  { key: 'failReason', title: '失败原因', type: 'input', width: 240 },
]

// ═══ Tab4 退订名单（合规四件套之一） ═══
const OPT_OUT_SOURCE_MAP: Record<string, string> = {
  REPLY_R: '回复R退订',
  MANUAL: '人工登记',
  CUSTOMER: '客户主动要求',
}
const OPT_OUT_SOURCE_OPTIONS = Object.entries(OPT_OUT_SOURCE_MAP).map(([value, label]) => ({ value, label }))

const optOutData = ref<any[]>([])
const optOutSearch = reactive({ mobile: '' })
const optOutColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 120, fixed: 'left' },
  { key: 'mobile', title: '手机号', type: 'slot', slotName: 'mobileCell', width: 150 },
  { key: 'receiverName', title: '客户名称', type: 'input', width: 200 },
  { key: 'source', title: '退订来源', type: 'slot', slotName: 'sourceCell', width: 130 },
  { key: 'optOutTime', title: '退订时间', type: 'slot', slotName: 'optOutTimeCell', width: 170 },
  { key: 'remark', title: '备注', type: 'input', width: 200 },
]

/** 手机号掩码（个人信息最小化展示） */
function maskMobile(v: any): string {
  const s = String(v ?? '')
  if (s.length < 7) return s
  return s.slice(0, 3) + '****' + s.slice(-4)
}

const optOutFormOpen = ref(false)
const optOutSaving = ref(false)
const optOutForm = reactive({ mobile: '', receiverName: '', source: 'REPLY_R', remark: '' })

function openOptOutCreate() {
  Object.assign(optOutForm, { mobile: '', receiverName: '', source: 'REPLY_R', remark: '' })
  optOutFormOpen.value = true
}

async function handleSaveOptOut() {
  if (!optOutForm.mobile.trim()) {
    message.warning('请输入手机号')
    return
  }
  optOutSaving.value = true
  try {
    await smsMarketingApi.addOptOut({ ...optOutForm })
    message.success('已加入退订名单，该号码不会再被发送')
    optOutFormOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '登记失败')
  } finally {
    optOutSaving.value = false
  }
}

function handleRemoveOptOut(record: any) {
  Modal.confirm({
    title: '移出退订名单',
    content: `确定要把 ${maskMobile(record.mobile)} 移出退订名单吗？仅在客户重新明确同意后方可移出。`,
    okText: '确认移出',
    okType: 'danger',
    onOk: async () => {
      try {
        await smsMarketingApi.removeOptOut(record.id)
        message.success('已移出退订名单')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '移出失败')
      }
    },
  })
}

// ═══ 合规设置（时段 / 频控 / 签名） ═══
const complianceOpen = ref(false)
const complianceSaving = ref(false)
const complianceForm = reactive({
  signName: '',
  sendStartHour: 8,
  sendEndHour: 21,
  freqLimitDays: 7,
  freqLimitCount: 3,
})

function openComplianceSetting() {
  complianceForm.signName = setting.value?.signName || ''
  complianceForm.sendStartHour = setting.value?.sendStartHour ?? 8
  complianceForm.sendEndHour = setting.value?.sendEndHour ?? 21
  complianceForm.freqLimitDays = setting.value?.freqLimitDays ?? 7
  complianceForm.freqLimitCount = setting.value?.freqLimitCount ?? 3
  complianceOpen.value = true
}

async function handleSaveCompliance() {
  complianceSaving.value = true
  try {
    const saved = await smsMarketingApi.saveSetting({ ...complianceForm })
    setting.value = saved
    sendForm.signName = saved?.signName || sendForm.signName
    message.success('合规设置已保存')
    complianceOpen.value = false
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    complianceSaving.value = false
  }
}

// ═══ Tab3 短信模板管理 ═══
const templateData = ref<SmsTemplate[]>([])
const templateSearch = reactive({ keyword: '', smsType: undefined as string | undefined })
const templateColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 130, fixed: 'left' },
  { key: 'templateTitle', title: '模版标题', type: 'slot', slotName: 'titleCell', width: 180 },
  { key: 'templateContent', title: '模版内容', type: 'input', width: 520 },
  { key: 'smsType', title: '短信类型', type: 'slot', slotName: 'typeCell', width: 120 },
  { key: 'updateTime', title: '最后修改时间', type: 'slot', slotName: 'updateTimeCell', width: 170 },
]

// ═══ 数据加载 ═══
async function fetchList() {
  loading.value = true
  try {
    if (activeTab.value === 'history') {
      const res: any = await smsMarketingApi.historyPage({
        receiver: historySearch.receiver || undefined,
        mobile: historySearch.mobile || undefined,
        sendStatus: historySearch.sendStatus || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      historyData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else if (activeTab.value === 'template') {
      const res: any = await smsMarketingApi.templatePage({
        keyword: templateSearch.keyword || undefined,
        smsType: templateSearch.smsType || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      templateData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    } else if (activeTab.value === 'optout') {
      const res: any = await smsMarketingApi.optOutPage({
        mobile: optOutSearch.mobile || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize,
      })
      optOutData.value = res?.records || []
      pagination.total = Number(res?.total) || 0
    }
  } catch (error: any) {
    console.error('[发短信] 加载列表失败', error)
    message.error(error?.response?.data?.message || '加载列表失败')
    if (activeTab.value === 'history') historyData.value = []
    else if (activeTab.value === 'optout') optOutData.value = []
    else templateData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  if (key !== 'send') fetchList()
}
function handleSearch() {
  pagination.current = 1
  fetchList()
}

/** 查询按钮按当前 Tab 分派（退订名单用 mobile 过滤，其余用 keyword） */
function searchOwner() {
  handleSearch()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchList()
}

// ═══ 模板增删改 ═══
const templateFormOpen = ref(false)
const templateSaving = ref(false)
const editingTemplateId = ref<any>(null)
const templateForm = reactive({ templateTitle: '', templateContent: '', smsType: 'NOTICE' })

function openTemplateCreate() {
  editingTemplateId.value = null
  Object.assign(templateForm, { templateTitle: '', templateContent: '', smsType: 'NOTICE' })
  templateFormOpen.value = true
}
function openTemplateEdit(record: any) {
  editingTemplateId.value = record.id
  Object.assign(templateForm, {
    templateTitle: record.templateTitle || '',
    templateContent: record.templateContent || '',
    smsType: record.smsType || 'NOTICE',
  })
  templateFormOpen.value = true
}
async function handleTemplateSave() {
  if (!templateForm.templateTitle.trim()) {
    message.warning('请输入模版标题')
    return
  }
  if (!templateForm.templateContent.trim()) {
    message.warning('请输入模版内容')
    return
  }
  templateSaving.value = true
  try {
    if (editingTemplateId.value) {
      await smsMarketingApi.updateTemplate(editingTemplateId.value, { ...templateForm })
      message.success('模板已更新')
    } else {
      await smsMarketingApi.createTemplate({ ...templateForm })
      message.success('模板已新增')
    }
    templateFormOpen.value = false
    fetchList()
  } catch (error: any) {
    message.error(error?.response?.data?.message || '保存失败')
  } finally {
    templateSaving.value = false
  }
}
function handleTemplateDelete(record: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除短信模板「${record.templateTitle}」吗？`,
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try {
        await smsMarketingApi.removeTemplate(record.id)
        message.success('删除成功')
        fetchList()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '删除失败')
      }
    },
  })
}

// ═══ 打印 ═══
function fmtTime(v: any): string {
  return v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'
}
function escapeHtml(v: any): string {
  return String(v ?? '').replace(/[&<>"']/g, c => (
    { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string
  ))
}

function handlePrint() {
  const isHistory = activeTab.value === 'history'
  const isOptOut = activeTab.value === 'optout'
  const src = isHistory ? historyData.value : (isOptOut ? optOutData.value : templateData.value)
  const rows = src.filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可打印的数据')
    return
  }
  const title = isHistory ? '短信历史' : (isOptOut ? '短信退订名单' : '短信模板管理')
  const cols = isHistory
    ? ['接收人', '手机号', '发送时间', '经手人', '短信内容', '发送状态', '失败原因']
    : ['模版标题', '模版内容', '短信类型', '最后修改时间']
  const cell = (r: any, i: number) => {
    if (isHistory) {
      return [r.receiverName, r.mobile, fmtTime(r.sendTime), r.handlerName, r.content, r.sendStatus, r.failReason]
    }
    return [r.templateTitle, r.templateContent, SMS_TYPE_MAP[r.smsType] || r.smsType, fmtTime(r.updateTime)]
  }
  const body = rows.map((r: any, i: number) => `<tr><td>${i + 1}</td>${
    cell(r, i).map(v => `<td>${escapeHtml(v)}</td>`).join('')}</tr>`).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><title>${title}</title>
    <style>body{font-family:"Microsoft YaHei",Arial,sans-serif;margin:0;padding:16px}
    h2{text-align:center;margin:0 0 12px;font-size:18px}
    table{width:100%;border-collapse:collapse;font-size:12px}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left}th{background:#f2f2f2}</style></head><body>
    <h2>${title}</h2>
    <table><thead><tr><th>#</th>${cols.map(c => `<th>${c}</th>`).join('')}</tr></thead>
    <tbody>${body}</tbody></table></body></html>`
  const win = window.open('', '_blank', 'width=1200,height=800')
  if (!win) {
    message.warning('浏览器阻止了打印窗口，请允许弹出窗口后重试')
    return
  }
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    if (activeTab.value !== 'send') handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[发短信] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(() => {
  loadSetting()
  loadTemplateOptions()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 0; }
.search-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-link { color: #1890ff; cursor: pointer; }
.cell-link:hover { text-decoration: underline; }
.btn-add {
  background: #ff6b35 !important;
  border-color: #ff6b35 !important;
}
.btn-add:hover {
  background: #e55a2b !important;
  border-color: #e55a2b !important;
}

/* ═══ Tab1 发短信 ═══ */
.send-panel {
  display: flex;
  gap: 24px;
  padding: 12px 16px;
  overflow-y: auto;
  height: 100%;
}
.send-form { flex: 1; min-width: 0; }
.quota-alert { margin-bottom: 12px; }
.char-counter {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.field-help { margin-left: 4px; color: #b0b0b0; font-size: 12px; }
.protocol-link { color: #ff6b35; margin-left: 8px; }
.phone-preview {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  justify-content: center;
}
.phone-body {
  width: 250px;
  min-height: 420px;
  border: 10px solid #333;
  border-radius: 22px;
  background: #f5f7fa;
  padding: 16px 12px;
  display: flex;
  align-items: flex-start;
}
.phone-bubble {
  max-width: 100%;
  padding: 8px 10px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #e0e0e0;
  font-size: 13px;
  line-height: 20px;
  word-break: break-all;
  color: #333;
}
.selected-tip { font-size: 12px; color: #666; }
.protocol-body { font-size: 13px; line-height: 24px; color: #333; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
