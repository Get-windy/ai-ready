<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header-left">
            <a-breadcrumb>
              <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
              <a-breadcrumb-item>设置</a-breadcrumb-item>
              <a-breadcrumb-item>外链同步</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="page-title">外链同步</h2>
            <p class="page-desc">配置外部系统连接，管理字段映射规则</p>
          </div>
          <div class="page-header-right">
            <a-button size="small" :loading="refreshLoading" @click="handleRefresh">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </div>
        </div>
      </template>

      <!-- 主Tab切换 -->
      <a-tabs v-model:activeKey="mainTab" class="main-tabs">
        <a-tab-pane key="config" tab="同步配置">
          <div class="master-detail-layout">
            <!-- 左侧平台列表 -->
            <div class="sidebar">
              <div class="sidebar-header">
                <span class="sidebar-title">已配置平台</span>
                <a-button type="primary" size="small" @click="showCreateConfig">
                  <template #icon><PlusOutlined /></template>
                </a-button>
              </div>
              <a-menu
                v-model:selectedKeys="selectedConfigKeys"
                mode="inline"
                @click="onConfigMenuClick"
              >
                <a-menu-item v-for="cfg in configs" :key="cfg.id">
                  <div class="menu-item-content">
                    <CloudOutlined />
                    <span class="menu-item-text">{{ cfg.displayName || getSystemName(cfg.sourceType) }}</span>
                    <a-badge :status="cfg.status === 1 ? 'success' : 'default'" />
                  </div>
                </a-menu-item>
              </a-menu>
              <a-empty v-if="configs.length === 0" description="暂无配置" />
            </div>

            <!-- 右侧配置详情 -->
            <div class="detail-panel">
              <a-empty v-if="!selectedConfig" description="请在左侧选择一个平台" />
              <div v-else class="config-detail">
                <div class="detail-header">
                  <h3>{{ selectedConfig.displayName || getSystemName(selectedConfig.sourceType) }}</h3>
                  <a-space>
                    <a-button size="small" @click="editConfig(selectedConfig)">
                      <template #icon><EditOutlined /></template>
                      编辑
                    </a-button>
                    <a-button size="small" @click="testConnection(selectedConfig.id)">
                      <template #icon><ApiOutlined /></template>
                      测试连接
                    </a-button>
                    <a-popconfirm title="确定删除此配置？" @confirm="deleteConfig(selectedConfig.id)">
                      <a-button size="small" danger>
                        <template #icon><DeleteOutlined /></template>
                        删除
                      </a-button>
                    </a-popconfirm>
                  </a-space>
                </div>
                <a-descriptions :column="2" bordered size="small">
                  <a-descriptions-item label="系统类型">{{ selectedConfig.sourceType }}</a-descriptions-item>
                  <a-descriptions-item label="状态">
                    <a-tag :color="selectedConfig.status === 1 ? 'green' : 'default'">
                      {{ selectedConfig.status === 1 ? '已启用' : '已禁用' }}
                    </a-tag>
                  </a-descriptions-item>
                  <a-descriptions-item label="登录账号">{{ selectedConfig.sourceUsername }}</a-descriptions-item>
                  <a-descriptions-item label="API地址">{{ selectedConfig.baseUrl }}</a-descriptions-item>
                  <a-descriptions-item label="同步方向">
                    <a-tag :color="directionColor(selectedConfig.syncDirection)">
                      {{ directionLabel(selectedConfig.syncDirection) }}
                    </a-tag>
                  </a-descriptions-item>
                  <a-descriptions-item label="同步方式">
                    <a-tag :color="selectedConfig.syncMode === 'incremental' ? 'blue' : 'orange'">
                      {{ selectedConfig.syncMode === 'incremental' ? '增量同步' : '全量同步' }}
                    </a-tag>
                  </a-descriptions-item>
                  <a-descriptions-item label="同步频率">{{ selectedConfig.syncCron || '未设置' }}</a-descriptions-item>
                  <a-descriptions-item label="心跳间隔">{{ selectedConfig.heartbeatInterval ? selectedConfig.heartbeatInterval + '秒' : '未设置' }}</a-descriptions-item>
                  <a-descriptions-item label="同步单据" :span="2">
                    {{ formatBillTypes(selectedConfig.billTypes) }}
                  </a-descriptions-item>
                  <a-descriptions-item label="最后同步" :span="2">
                    {{ selectedConfig.lastSyncTime || '从未同步' }}
                  </a-descriptions-item>
                  <a-descriptions-item label="备注" :span="2">{{ selectedConfig.remark || '无' }}</a-descriptions-item>
                </a-descriptions>
              </div>
            </div>
          </div>
        </a-tab-pane>

        <a-tab-pane key="mapping" tab="字段映射">
          <div class="master-detail-layout">
            <!-- 左侧平台列表 -->
            <div class="sidebar">
              <div class="sidebar-header">
                <span class="sidebar-title">已配置平台</span>
              </div>
              <a-menu
                v-model:selectedKeys="selectedMappingConfigKeys"
                mode="inline"
                @click="onMappingConfigMenuClick"
              >
                <a-menu-item v-for="cfg in configs" :key="cfg.id">
                  <div class="menu-item-content">
                    <CloudOutlined />
                    <span class="menu-item-text">{{ cfg.displayName || getSystemName(cfg.sourceType) }}</span>
                  </div>
                </a-menu-item>
              </a-menu>
              <a-empty v-if="configs.length === 0" description="暂无配置" />
            </div>

            <!-- 右侧字段映射 -->
            <div class="detail-panel">
              <a-empty v-if="!selectedMappingConfig" description="请在左侧选择一个平台" />
              <div v-else class="mapping-detail">
                <!-- 数据集合分类选择 -->
                <div class="domain-selector">
                  <span class="domain-label">数据集合：</span>
                  <a-radio-group v-model:value="selectedDomain" button-style="solid" size="small" @change="onDomainChange">
                    <a-radio-button v-for="d in domainCategories" :key="d.key" :value="d.key">
                      {{ d.label }}
                    </a-radio-button>
                  </a-radio-group>
                </div>

                <!-- 单据类型子Tab -->
                <a-tabs v-if="filteredBillTypes.length > 0" v-model:activeKey="selectedBillType" @change="loadFieldMappings" size="small">
                  <a-tab-pane v-for="bt in filteredBillTypes" :key="bt" :tab="getBillTypeLabel(bt)" />
                </a-tabs>
                <a-empty v-else description="该平台未配置此数据集合的单据类型" style="padding: 40px 0;">
                  <template #extra>
                    <a-button size="small" @click="editConfig(selectedMappingConfig)">去配置单据类型</a-button>
                  </template>
                </a-empty>

                <template v-if="filteredBillTypes.length > 0">

                <div class="mapping-toolbar">
                  <a-space>
                    <a-button size="small" @click="initFromTemplate" :loading="templateLoading">
                      <template #icon><ThunderboltOutlined /></template>
                      从模板初始化
                    </a-button>
                    <a-button size="small" type="primary" @click="showCreateMapping">
                      <template #icon><PlusOutlined /></template>
                      添加映射
                    </a-button>
                    <a-button size="small" :loading="mappingSaving" @click="batchSaveMappings">
                      <template #icon><SaveOutlined /></template>
                      保存
                    </a-button>
                  </a-space>
                </div>

                <a-table
                  :columns="mappingColumns"
                  :data-source="mappingRows"
                  :pagination="false"
                  :scroll="{ y: 500 }"
                  size="small"
                  row-key="id"
                  :row-selection="{ selectedRowKeys: selectedMappingRows, onChange: onMappingRowSelect }"
                >
                  <template #bodyCell="{ column, record, index }">
                    <template v-if="column.key === 'index'">
                      {{ index + 1 }}
                    </template>
                    <template v-if="column.key === 'localField'">
                      <a-select
                        v-model:value="record.targetField"
                        size="small"
                        style="width: 150px"
                        show-search
                        :filter-option="filterOption"
                        @change="onLocalFieldChange(record)"
                      >
                        <a-select-option v-for="f in localFieldOptions" :key="f.value" :value="f.value">
                          {{ f.label }} ({{ f.value }})
                        </a-select-option>
                      </a-select>
                    </template>
                    <template v-if="column.key === 'localLabel'">
                      {{ record.targetLabel }}
                    </template>
                    <template v-if="column.key === 'externalField'">
                      <a-select
                        v-model:value="record.sourceField"
                        size="small"
                        style="width: 150px"
                        show-search
                        :filter-option="filterOption"
                        @change="onExternalFieldChange(record)"
                      >
                        <a-select-option v-for="f in externalFieldOptions" :key="f.value" :value="f.value">
                          {{ f.label }} ({{ f.value }})
                        </a-select-option>
                      </a-select>
                    </template>
                    <template v-if="column.key === 'externalLabel'">
                      {{ record.sourceLabel }}
                    </template>
                    <template v-if="column.key === 'transformType'">
                      <a-select v-model:value="record.transformType" size="small" style="width: 100px">
                        <a-select-option value="direct">直接映射</a-select-option>
                        <a-select-option value="enum">枚举转换</a-select-option>
                        <a-select-option value="formula">公式</a-select-option>
                        <a-select-option value="default">默认值</a-select-option>
                      </a-select>
                    </template>
                    <template v-if="column.key === 'transformRule'">
                      <a-input v-model:value="record.transformRule" size="small" placeholder="JSON" style="width: 120px" />
                    </template>
                    <template v-if="column.key === 'defaultValue'">
                      <a-input v-model:value="record.defaultValue" size="small" style="width: 80px" />
                    </template>
                    <template v-if="column.key === 'required'">
                      <a-checkbox v-model:checked="record.required" />
                    </template>
                    <template v-if="column.key === 'status'">
                      <a-switch
                        :checked="record.status === 1"
                        @change="(checked: any) => toggleMappingStatus(record, checked as boolean)"
                        size="small"
                      />
                    </template>
                    <template v-if="column.key === 'action'">
                      <a-space>
                        <a-button type="link" size="small" @click="editMapping(record)">编辑</a-button>
                        <a-popconfirm title="确定删除？" @confirm="deleteMapping(record, index)">
                          <a-button type="link" danger size="small">删除</a-button>
                        </a-popconfirm>
                      </a-space>
                    </template>
                  </template>
                </a-table>
                </template>
              </div>
            </div>
          </div>
        </a-tab-pane>

        <a-tab-pane key="history" tab="同步历史">
          <div class="master-detail-layout">
            <!-- 左侧平台列表 -->
            <div class="sidebar">
              <div class="sidebar-header">
                <span class="sidebar-title">已配置平台</span>
              </div>
              <a-menu
                v-model:selectedKeys="selectedHistoryConfigKeys"
                mode="inline"
                @click="onHistoryConfigMenuClick"
              >
                <a-menu-item v-for="cfg in configs" :key="cfg.id">
                  <div class="menu-item-content">
                    <CloudOutlined />
                    <span class="menu-item-text">{{ cfg.displayName || getSystemName(cfg.sourceType) }}</span>
                  </div>
                </a-menu-item>
              </a-menu>
              <a-empty v-if="configs.length === 0" description="暂无配置" />
            </div>

            <!-- 右侧同步历史 -->
            <div class="detail-panel">
              <a-empty v-if="!selectedHistoryConfig" description="请在左侧选择一个平台查看同步历史" />
              <div v-else class="history-detail">
                <div class="detail-header">
                  <h3>{{ selectedHistoryConfig.displayName || getSystemName(selectedHistoryConfig.sourceType) }} — 同步历史</h3>
                  <a-button size="small" :loading="historyLoading" @click="loadSyncHistory">
                    <template #icon><ReloadOutlined /></template>
                    刷新
                  </a-button>
                </div>
                <a-table
                  :columns="historyColumns"
                  :data-source="historyRows"
                  :pagination="false"
                  :scroll="{ y: 500 }"
                  size="small"
                  row-key="id"
                  :loading="historyLoading"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'status'">
                      <a-tag :color="historyStatusColor(record.status)">
                        {{ historyStatusLabel(record.status) }}
                      </a-tag>
                    </template>
                    <template v-if="column.key === 'syncType'">
                      <a-tag>{{ record.syncType === 'full' ? '全量' : '增量' }}</a-tag>
                    </template>
                    <template v-if="column.key === 'billType'">
                      {{ getBillTypeLabel(record.billType) }}
                    </template>
                    <template v-if="column.key === 'triggerType'">
                      {{ record.triggerType === 'cron' ? '定时' : '手动' }}
                    </template>
                    <template v-if="column.key === 'duration'">
                      {{ record.durationMs ? (record.durationMs / 1000).toFixed(1) + 's' : '-' }}
                    </template>
                    <template v-if="column.key === 'records'">
                      <span v-if="record.status === 'running'">执行中...</span>
                      <span v-else>
                        共{{ record.recordsTotal || 0 }}条，
                        成功<span style="color: #52c41a">{{ record.recordsSynced || 0 }}</span>，
                        失败<span style="color: #f5222d">{{ record.recordsFailed || 0 }}</span>
                      </span>
                    </template>
                    <template v-if="column.key === 'errorMessage'">
                      <a-tooltip v-if="record.errorMessage" :title="record.errorMessage">
                        <span class="error-text">{{ record.errorMessage }}</span>
                      </a-tooltip>
                      <span v-else>-</span>
                    </template>
                  </template>
                </a-table>
              </div>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>

      <!-- 新建/编辑配置抽屉 -->
      <a-drawer
        :open="configDrawerVisible"
        :title="editingConfigId ? '编辑同步配置' : '新建同步配置'"
        width="600px"
        @close="closeConfigDrawer"
      >
        <a-form ref="configFormRef" :model="configForm" :rules="configFormRules" layout="vertical">
          <a-form-item label="导入系统" name="sourceType" required>
            <a-select
              v-model:value="configForm.sourceType"
              placeholder="请选择外部系统"
              :options="sourceOptions"
              @change="onSourceChange"
            />
          </a-form-item>
          <a-form-item label="显示名称" name="displayName">
            <a-input v-model:value="configForm.displayName" placeholder="自定义显示名称" />
          </a-form-item>
          <a-divider>账号绑定</a-divider>
          <a-form-item label="登录账号" name="sourceUsername" required>
            <a-input v-model:value="configForm.sourceUsername" />
          </a-form-item>
          <a-form-item label="登录密码" name="sourcePassword" required>
            <a-input-password v-model:value="configForm.sourcePassword" />
          </a-form-item>
          <a-form-item label="API地址">
            <a-input v-model:value="configForm.baseUrl" />
          </a-form-item>
          <a-divider>同步设置</a-divider>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="同步方向" name="syncDirection">
                <a-radio-group v-model:value="configForm.syncDirection">
                  <a-radio value="inbound">外部→系统</a-radio>
                  <a-radio value="outbound">系统→外部</a-radio>
                  <a-radio value="bidirectional">双向</a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="同步方式">
                <a-radio-group v-model:value="configForm.syncMode">
                  <a-radio value="incremental">增量</a-radio>
                  <a-radio value="full">全量</a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="同步频率(Cron)">
            <a-input v-model:value="configForm.syncCron" placeholder="*/30 * * * *" />
          </a-form-item>
          <a-form-item label="心跳间隔(秒)">
            <a-input-number v-model:value="configForm.heartbeatInterval" :min="60" style="width: 100%" />
          </a-form-item>
          <a-form-item label="同步单据类型">
            <a-checkbox-group v-model:value="selectedBillTypes">
              <a-checkbox v-for="item in billTypeItems" :key="item.itemValue" :value="item.itemValue">
                {{ item.itemText }}
              </a-checkbox>
            </a-checkbox-group>
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea v-model:value="configForm.remark" :rows="2" />
          </a-form-item>
        </a-form>
        <template #footer>
          <a-space>
            <a-button @click="closeConfigDrawer">取消</a-button>
            <a-button type="primary" :loading="configSaving" @click="saveConfig">保存</a-button>
          </a-space>
        </template>
      </a-drawer>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined,
  ApiOutlined, CloudOutlined, ThunderboltOutlined, SaveOutlined,
} from '@ant-design/icons-vue'
import request from '@/utils/request'
import { dictItemApi, type DictItem } from '@/api/dict'

// ── 主Tab ──
const mainTab = ref('config')

// ── 平台列表 ──
const configs = ref<any[]>([])
const refreshLoading = ref(false)
const selectedConfigKeys = ref<number[]>([])
const selectedConfig = ref<any>(null)

const selectedMappingConfigKeys = ref<number[]>([])
const selectedMappingConfig = ref<any>(null)

// ── 配置表单 ──
const configDrawerVisible = ref(false)
const editingConfigId = ref<number | null>(null)
const configSaving = ref(false)
const configFormRef = ref()
const sourceOptions = ref<any[]>([])
const billTypeItems = ref<DictItem[]>([])
const selectedBillTypes = ref<string[]>(['601', '604', '504', '801'])

const defaultConfigForm = {
  sourceType: '',
  displayName: '',
  sourceUsername: '',
  sourcePassword: '',
  baseUrl: 'https://www.ql361.com',
  syncMode: 'incremental',
  syncCron: '*/30 * * * *',
  heartbeatInterval: 300,
  syncDirection: 'inbound',
  remark: '',
}
const configForm = reactive({ ...defaultConfigForm })
const configFormRules = {
  sourceType: [{ required: true, message: '请选择导入系统' }],
  sourceUsername: [{ required: true, message: '请输入登录账号' }],
  sourcePassword: [{ required: true, message: '请输入登录密码' }],
}

// ── 字段映射 ──
const selectedBillType = ref('')
const mappingRows = ref<any[]>([])
const selectedMappingRows = ref<number[]>([])
const mappingSaving = ref(false)
const templateLoading = ref(false)
const selectedDomain = ref('master')

// 数据集合分类（按业务域分组）
const domainCategories = [
  { key: 'master', label: '基础数据', billTypes: ['601', '604'] },         // 客户、供应商
  { key: 'product', label: '商品数据', billTypes: ['504'] },               // 商品
  { key: 'sales', label: '销售数据', billTypes: ['101', '102', '103'] },   // 销售订单、出库、退货
  { key: 'purchase', label: '采购数据', billTypes: ['201', '202', '203'] },// 采购订单、入库、退货
  { key: 'inventory', label: '库存数据', billTypes: ['801', '802', '803'] },// 库存、入库、出库
  { key: 'finance', label: '财务数据', billTypes: ['301', '302', '303', '304'] },// 应收、应付、收款、付款
]

// 根据选中的数据集合过滤可用的单据类型
const filteredBillTypes = computed(() => {
  if (!selectedMappingConfig.value) return []
  const domain = domainCategories.find(d => d.key === selectedDomain.value)
  if (!domain) return []
  try {
    const configBillTypes = JSON.parse(selectedMappingConfig.value.billTypes || '[]')
    // 只显示配置中启用的、且属于当前数据集合的单据类型
    return domain.billTypes.filter(bt => configBillTypes.includes(bt))
  } catch { return [] }
})

function onDomainChange() {
  // 切换数据集合时，自动选中第一个可用的单据类型
  if (filteredBillTypes.value.length > 0) {
    selectedBillType.value = filteredBillTypes.value[0]
    loadFieldMappings()
  } else {
    selectedBillType.value = ''
    mappingRows.value = []
  }
}

// 本地字段选项（根据单据类型动态生成）
const localFieldOptions = computed(() => {
  const bt = selectedBillType.value
  if (bt === '601' || bt === '604') {
    return [
      { value: 'partyCode', label: '往来单位编码' },
      { value: 'partyName', label: '往来单位名称' },
      { value: 'shortName', label: '简称' },
      { value: 'contact', label: '联系人' },
      { value: 'phone', label: '联系电话' },
      { value: 'address', label: '地址' },
      { value: 'settlementType', label: '结算方式' },
      { value: 'partyType', label: '往来类型' },
      { value: 'remark', label: '备注' },
    ]
  } else if (bt === '504') {
    return [
      { value: 'productCode', label: '商品编码' },
      { value: 'productName', label: '商品名称' },
      { value: 'barcode', label: '条码' },
      { value: 'unit', label: '单位' },
      { value: 'categoryName', label: '商品分类' },
      { value: 'costPrice', label: '成本价' },
      { value: 'salePrice', label: '零售价' },
      { value: 'spec', label: '规格' },
    ]
  } else if (bt === '801' || bt === '802' || bt === '803') {
    return [
      { value: 'productCode', label: '商品编码' },
      { value: 'warehouseCode', label: '仓库编码' },
      { value: 'quantity', label: '库存数量' },
      { value: 'batchNo', label: '批次号' },
      { value: 'productionDate', label: '生产日期' },
    ]
  } else if (bt.startsWith('1')) {
    // 销售类单据
    return [
      { value: 'billNo', label: '单据编号' },
      { value: 'billDate', label: '单据日期' },
      { value: 'customerCode', label: '客户编码' },
      { value: 'customerName', label: '客户名称' },
      { value: 'productCode', label: '商品编码' },
      { value: 'productName', label: '商品名称' },
      { value: 'quantity', label: '数量' },
      { value: 'unitPrice', label: '单价' },
      { value: 'amount', label: '金额' },
      { value: 'warehouseCode', label: '仓库编码' },
      { value: 'remark', label: '备注' },
    ]
  } else if (bt.startsWith('2')) {
    // 采购类单据
    return [
      { value: 'billNo', label: '单据编号' },
      { value: 'billDate', label: '单据日期' },
      { value: 'supplierCode', label: '供应商编码' },
      { value: 'supplierName', label: '供应商名称' },
      { value: 'productCode', label: '商品编码' },
      { value: 'productName', label: '商品名称' },
      { value: 'quantity', label: '数量' },
      { value: 'unitPrice', label: '单价' },
      { value: 'amount', label: '金额' },
      { value: 'warehouseCode', label: '仓库编码' },
      { value: 'remark', label: '备注' },
    ]
  } else if (bt.startsWith('3')) {
    // 财务类单据
    return [
      { value: 'billNo', label: '单据编号' },
      { value: 'billDate', label: '单据日期' },
      { value: 'partyCode', label: '往来单位编码' },
      { value: 'partyName', label: '往来单位名称' },
      { value: 'amount', label: '金额' },
      { value: 'balance', label: '余额' },
      { value: 'settlementType', label: '结算方式' },
      { value: 'remark', label: '备注' },
    ]
  }
  return []
})

// 外链字段选项（示例，实际应从API获取）
const externalFieldOptions = computed(() => {
  const bt = selectedBillType.value
  if (bt === '601') {
    return [
      { value: 'customer_code', label: '客户编码' },
      { value: 'customer_name', label: '客户名称' },
      { value: 'short_name', label: '简称' },
      { value: 'contact_person', label: '联系人' },
      { value: 'phone', label: '电话' },
      { value: 'address', label: '地址' },
      { value: 'settlement_type', label: '结算方式' },
      { value: 'remark', label: '备注' },
    ]
  } else if (bt === '604') {
    return [
      { value: 'supplier_code', label: '供应商编码' },
      { value: 'supplier_name', label: '供应商名称' },
      { value: 'short_name', label: '简称' },
      { value: 'contact_person', label: '联系人' },
      { value: 'phone', label: '电话' },
      { value: 'address', label: '地址' },
      { value: 'remark', label: '备注' },
    ]
  } else if (bt === '504') {
    return [
      { value: 'product_code', label: '商品编码' },
      { value: 'product_name', label: '商品名称' },
      { value: 'barcode', label: '条码' },
      { value: 'unit', label: '单位' },
      { value: 'category', label: '分类' },
      { value: 'cost_price', label: '成本价' },
      { value: 'sale_price', label: '零售价' },
      { value: 'spec', label: '规格' },
    ]
  } else if (bt === '801' || bt === '802' || bt === '803') {
    return [
      { value: 'product_code', label: '商品编码' },
      { value: 'warehouse_code', label: '仓库编码' },
      { value: 'quantity', label: '库存数量' },
      { value: 'batch_no', label: '批次号' },
      { value: 'production_date', label: '生产日期' },
    ]
  } else if (bt.startsWith('1')) {
    return [
      { value: 'bill_no', label: '单据编号' },
      { value: 'bill_date', label: '单据日期' },
      { value: 'customer_code', label: '客户编码' },
      { value: 'customer_name', label: '客户名称' },
      { value: 'product_code', label: '商品编码' },
      { value: 'product_name', label: '商品名称' },
      { value: 'quantity', label: '数量' },
      { value: 'unit_price', label: '单价' },
      { value: 'amount', label: '金额' },
      { value: 'warehouse_code', label: '仓库编码' },
      { value: 'remark', label: '备注' },
    ]
  } else if (bt.startsWith('2')) {
    return [
      { value: 'bill_no', label: '单据编号' },
      { value: 'bill_date', label: '单据日期' },
      { value: 'supplier_code', label: '供应商编码' },
      { value: 'supplier_name', label: '供应商名称' },
      { value: 'product_code', label: '商品编码' },
      { value: 'product_name', label: '商品名称' },
      { value: 'quantity', label: '数量' },
      { value: 'unit_price', label: '单价' },
      { value: 'amount', label: '金额' },
      { value: 'warehouse_code', label: '仓库编码' },
      { value: 'remark', label: '备注' },
    ]
  } else if (bt.startsWith('3')) {
    return [
      { value: 'bill_no', label: '单据编号' },
      { value: 'bill_date', label: '单据日期' },
      { value: 'party_code', label: '往来单位编码' },
      { value: 'party_name', label: '往来单位名称' },
      { value: 'amount', label: '金额' },
      { value: 'balance', label: '余额' },
      { value: 'settlement_type', label: '结算方式' },
      { value: 'remark', label: '备注' },
    ]
  }
  return []
})

const mappingColumns = [
  { title: '序号', key: 'index', width: 60, align: 'center' as const },
  { title: '本地字段', key: 'localField', width: 180 },
  { title: '本地字段名', key: 'localLabel', width: 120 },
  { title: '外链字段', key: 'externalField', width: 180 },
  { title: '外链字段名', key: 'externalLabel', width: 120 },
  { title: '转换类型', key: 'transformType', width: 120 },
  { title: '转换规则', key: 'transformRule', width: 140 },
  { title: '默认值', key: 'defaultValue', width: 100 },
  { title: '必填', key: 'required', width: 60, align: 'center' as const },
  { title: '状态', key: 'status', width: 80, align: 'center' as const },
  { title: '操作', key: 'action', width: 120, fixed: 'right' as const },
]

// ── 方法 ──
async function loadConfigs() {
  try {
    const res = await request.get('/v1/sync-config')
    configs.value = Array.isArray(res) ? res : []
    if (configs.value.length > 0 && !selectedConfig.value) {
      selectedConfigKeys.value = [configs.value[0].id]
      selectedConfig.value = configs.value[0]
    }
  } catch (e: any) {
    message.error('加载配置失败')
  }
}

async function loadSources() {
  try {
    const res = await request.get('/v1/sync-config/sources')
    sourceOptions.value = (Array.isArray(res) ? res : []).map((s: any) => ({
      label: s.systemName,
      value: s.systemCode,
    }))
  } catch (e) { console.error(e) }
}

async function loadBillTypes() {
  try {
    const res = await dictItemApi.getByDictCode('BILL_TYPE')
    billTypeItems.value = Array.isArray(res) ? res : []
  } catch (e) { console.error(e) }
}

function getSystemName(sourceType: string): string {
  const found = sourceOptions.value.find(s => s.value === sourceType)
  return found ? found.label : sourceType
}

function formatBillTypes(billTypes: string): string {
  try {
    const types = JSON.parse(billTypes || '[]')
    const map: Record<string, string> = {}
    billTypeItems.value.forEach(item => { map[item.itemValue] = item.itemText })
    return types.map((t: string) => map[t] || t).join('、')
  } catch { return billTypes }
}

function directionLabel(dir: string): string {
  return dir === 'outbound' ? '系统→外部' : dir === 'bidirectional' ? '双向同步' : '外部→系统'
}

function directionColor(dir: string): string {
  return dir === 'outbound' ? 'orange' : dir === 'bidirectional' ? 'purple' : 'green'
}

function getBillTypeLabel(bt: string): string {
  const found = billTypeItems.value.find(item => item.itemValue === bt)
  return found ? found.itemText : bt
}

function handleRefresh() {
  refreshLoading.value = true
  loadConfigs().finally(() => { refreshLoading.value = false })
}

function onConfigMenuClick({ key }: any) {
  const cfg = configs.value.find(c => c.id === key)
  selectedConfig.value = cfg
}

function onMappingConfigMenuClick({ key }: any) {
  const cfg = configs.value.find(c => c.id === key)
  selectedMappingConfig.value = cfg
  selectedDomain.value = 'master'
  onDomainChange()
}

function showCreateConfig() {
  editingConfigId.value = null
  Object.assign(configForm, defaultConfigForm)
  selectedBillTypes.value = ['601', '604', '504', '801']
  configDrawerVisible.value = true
}

function editConfig(cfg: any) {
  editingConfigId.value = cfg.id
  configForm.sourceType = cfg.sourceType
  configForm.displayName = cfg.displayName
  configForm.sourceUsername = cfg.sourceUsername
  configForm.sourcePassword = ''
  configForm.baseUrl = cfg.baseUrl
  configForm.syncMode = cfg.syncMode
  configForm.syncCron = cfg.syncCron
  configForm.heartbeatInterval = cfg.heartbeatInterval
  configForm.syncDirection = cfg.syncDirection || 'inbound'
  configForm.remark = cfg.remark
  try { selectedBillTypes.value = JSON.parse(cfg.billTypes || '[]') }
  catch { selectedBillTypes.value = [] }
  configDrawerVisible.value = true
}

function closeConfigDrawer() {
  configDrawerVisible.value = false
  configFormRef.value?.resetFields()
}

function onSourceChange(value: string) {
  if (!configForm.displayName) configForm.displayName = getSystemName(value)
}

async function saveConfig() {
  try { await configFormRef.value.validate() } catch { return }
  configSaving.value = true
  try {
    const payload = { ...configForm, billTypes: JSON.stringify(selectedBillTypes.value) }
    if (editingConfigId.value) {
      await request.put(`/v1/sync-config/${editingConfigId.value}`, payload)
      message.success('更新成功')
    } else {
      await request.post('/v1/sync-config', payload)
      message.success('创建成功')
    }
    closeConfigDrawer()
    await loadConfigs()
  } catch (e: any) { message.error(e?.message || '保存失败') }
  finally { configSaving.value = false }
}

async function deleteConfig(id: number) {
  try {
    await request.delete(`/v1/sync-config/${id}`)
    message.success('删除成功')
    selectedConfig.value = null
    selectedConfigKeys.value = []
    await loadConfigs()
  } catch (e: any) { message.error(e?.message || '删除失败') }
}

async function testConnection(id: number) {
  try {
    const res = await request.post(`/v1/sync-config/${id}/test`)
    if (res?.connected) message.success(`连接成功 (${res?.latency}ms)`)
    else message.error(res?.message || '连接失败')
  } catch (e: any) { message.error(e?.message || '测试失败') }
}

async function loadFieldMappings() {
  if (!selectedMappingConfig.value || !selectedBillType.value) return
  try {
    const res = await request.get(
      `/v1/sync-config/${selectedMappingConfig.value.id}/field-mappings/${selectedBillType.value}`
    )
    mappingRows.value = (Array.isArray(res) ? res : []).map((m: any) => ({
      id: m.id,
      sourceField: m.sourceField || '',
      sourceLabel: m.sourceLabel || '',
      targetField: m.targetField || '',
      targetLabel: m.targetLabel || '',
      transformType: m.transformType || 'direct',
      transformRule: m.transformRule || '',
      defaultValue: m.defaultValue || '',
      required: !!m.required,
      status: m.status ?? 1,
    }))
  } catch (e) {
    mappingRows.value = []
  }
}

function filterOption(input: string, option: any) {
  return option.label.toLowerCase().includes(input.toLowerCase())
}

function onLocalFieldChange(record: any) {
  const opt = localFieldOptions.value.find(f => f.value === record.targetField)
  if (opt) record.targetLabel = opt.label
}

function onExternalFieldChange(record: any) {
  const opt = externalFieldOptions.value.find(f => f.value === record.sourceField)
  if (opt) record.sourceLabel = opt.label
}

let mappingSeq = 0
function showCreateMapping() {
  mappingRows.value.push({
    id: `new_${++mappingSeq}`,
    sourceField: '',
    sourceLabel: '',
    targetField: '',
    targetLabel: '',
    transformType: 'direct',
    transformRule: '',
    defaultValue: '',
    required: false,
    status: 1,
  })
}

function editMapping(record: any) {
  // 行内编辑，无需弹窗
}

function deleteMapping(record: any, index: number) {
  mappingRows.value.splice(index, 1)
}

function toggleMappingStatus(record: any, checked: boolean) {
  record.status = checked ? 1 : 0
}

function onMappingRowSelect(keys: number[]) {
  selectedMappingRows.value = keys
}

async function batchSaveMappings() {
  if (!selectedMappingConfig.value || !selectedBillType.value) return
  mappingSaving.value = true
  try {
    const payload = mappingRows.value.map((r, idx) => ({
      id: typeof r.id === 'number' ? r.id : undefined,
      sourceConfigId: selectedMappingConfig.value.id,
      billType: selectedBillType.value,
      sourceField: r.sourceField,
      sourceLabel: r.sourceLabel,
      targetField: r.targetField,
      targetLabel: r.targetLabel,
      transformType: r.transformType,
      transformRule: r.transformRule,
      defaultValue: r.defaultValue,
      required: r.required,
      status: r.status,
      sortOrder: idx,
    }))
    const res = await request.post(
      `/v1/sync-config/${selectedMappingConfig.value.id}/field-mappings/batch`,
      payload
    )
    message.success('保存成功')
    mappingRows.value = (Array.isArray(res) ? res : []).map((m: any) => ({
      id: m.id,
      sourceField: m.sourceField || '',
      sourceLabel: m.sourceLabel || '',
      targetField: m.targetField || '',
      targetLabel: m.targetLabel || '',
      transformType: m.transformType || 'direct',
      transformRule: m.transformRule || '',
      defaultValue: m.defaultValue || '',
      required: !!m.required,
      status: m.status ?? 1,
    }))
  } catch (e: any) { message.error(e?.message || '保存失败') }
  finally { mappingSaving.value = false }
}

async function initFromTemplate() {
  if (!selectedMappingConfig.value || !selectedBillType.value) return
  templateLoading.value = true
  try {
    const res = await request.post(
      `/v1/sync-config/${selectedMappingConfig.value.id}/field-mappings/init-template`,
      null,
      { params: { billType: selectedBillType.value } }
    )
    message.success('已从模板初始化')
    mappingRows.value = (Array.isArray(res) ? res : []).map((m: any) => ({
      id: m.id,
      sourceField: m.sourceField || '',
      sourceLabel: m.sourceLabel || '',
      targetField: m.targetField || '',
      targetLabel: m.targetLabel || '',
      transformType: m.transformType || 'direct',
      transformRule: m.transformRule || '',
      defaultValue: m.defaultValue || '',
      required: !!m.required,
      status: m.status ?? 1,
    }))
  } catch (e: any) { message.error(e?.message || '初始化失败') }
  finally { templateLoading.value = false }
}

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }

// ── 同步历史 ──
const selectedHistoryConfigKeys = ref<string[]>([])
const selectedHistoryConfig = ref<any>(null)
const historyRows = ref<any[]>([])
const historyLoading = ref(false)

const historyColumns = [
  { title: '同步时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
  { title: '同步类型', key: 'syncType', width: 80, align: 'center' as const },
  { title: '数据类别', key: 'billType', width: 100 },
  { title: '触发方式', key: 'triggerType', width: 80, align: 'center' as const },
  { title: '状态', key: 'status', width: 90, align: 'center' as const },
  { title: '耗时', key: 'duration', width: 80, align: 'center' as const },
  { title: '同步结果', key: 'records', width: 200 },
  { title: '错误信息', key: 'errorMessage', width: 200, ellipsis: true },
]

function onHistoryConfigMenuClick({ key }: any) {
  const cfg = configs.value.find(c => c.id === key)
  selectedHistoryConfig.value = cfg
  loadSyncHistory()
}

async function loadSyncHistory() {
  if (!selectedHistoryConfig.value) return
  historyLoading.value = true
  try {
    const res = await request.get(`/v1/sync-history/config/${selectedHistoryConfig.value.id}`)
    historyRows.value = Array.isArray(res) ? res : []
  } catch (e) {
    historyRows.value = []
  } finally {
    historyLoading.value = false
  }
}

function historyStatusLabel(status: string): string {
  return status === 'success' ? '成功' : status === 'failed' ? '失败' : status === 'partial' ? '部分成功' : '执行中'
}

function historyStatusColor(status: string): string {
  return status === 'success' ? 'green' : status === 'failed' ? 'red' : status === 'partial' ? 'orange' : 'blue'
}

onMounted(async () => {
  await loadSources()
  await loadBillTypes()
  await loadConfigs()
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: flex-start; }
.page-header-left { flex: 1; }
.page-title { margin: 8px 0 4px; font-size: 20px; font-weight: 600; }
.page-desc { margin: 0; color: #8c8c8c; font-size: 13px; }

.main-tabs { margin-top: 16px; }

.master-detail-layout {
  display: flex;
  gap: 16px;
  min-height: 600px;
}

.sidebar {
  width: 240px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  background: #fafafa;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.sidebar-title { font-weight: 600; font-size: 14px; }

.menu-item-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-item-text { flex: 1; }

.detail-panel {
  flex: 1;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 20px;
  background: #fff;
}

.config-detail .detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.config-detail .detail-header h3 { margin: 0; font-size: 18px; }

.mapping-detail { min-height: 500px; }

.mapping-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}

.domain-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0 12px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 4px;
}

.domain-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}

.error-text {
  color: #f5222d;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
  display: inline-block;
}

.history-detail .detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.history-detail .detail-header h3 { margin: 0; font-size: 18px; }
</style>
