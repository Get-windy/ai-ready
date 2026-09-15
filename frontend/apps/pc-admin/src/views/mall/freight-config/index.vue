<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      full-height
    >
      <!--
        页面定位：商城 → 商城设置 → 运费设置（对标 ql361「商城 → 商城设置 → 运费设置」）
        对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/运费设置开发文档.md
        结构说明：左侧 2 子项导航（物流 / 到店自提）
                 + 右侧配置表单（物流：开关分组 + 统一运费分组 + 物流方式/运费模板分组；到店自提：开关 + 提货地址列表）
                 + 底部统一「保存」（按子项分别提交）
        金标准：ErrorBoundary > PageContainer(full-height) > 左子项导航 + 右分组表单
        API：shopConfigApi（GET/PUT /erp/mall/admin/config，单行 ShopConfig 实体）
        保存口径：「统一运费（免运费金额/固定运费）」复用既有列 free_shipping_amount / freight_amount；
                 物流开关/物流方式/运费模板/到店自提/提货地址落 Flyway V11.361.7 新增列
        存储口径：布尔开关库内 INTEGER 0/1；物流方式、运费模板、提货地址落 TEXT 列（JSON），
                 由前端 JSON.stringify / JSON.parse（try-catch 兜底）转换
      -->
      <div class="config-layout">
        <!-- ═══ 左侧：2 子项导航 ═══ -->
        <div class="config-layout__side">
          <a-menu
            :selected-keys="[currentMenuKey]"
            mode="inline"
            class="config-menu"
            @click="onMenuClick"
          >
            <a-menu-item
              v-for="m in MENU_ITEMS"
              :key="m.key"
            >
              <span class="config-menu__text">{{ m.label }}</span>
            </a-menu-item>
          </a-menu>
        </div>

        <!-- ═══ 右侧：当前子项配置表单 ═══ -->
        <div
          class="config-layout__main"
          data-testid="freight-config-form"
        >
          <a-spin :spinning="loading">
            <a-form
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 16 }"
              size="small"
            >
              <!-- ══════ 子项一：物流 ══════ -->
              <template v-if="currentMenuKey === 'logistics'">
                <a-card
                  title="物流设置"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（self_delivery） -->
                  <a-form-item label="自有配送">
                    <a-switch
                      v-model:checked="logistics.selfDelivery"
                      checked-children="开"
                      un-checked-children="关"
                    />
                    <div class="form-tip">
                      启用后商城下单可选择商家配送，且不计算运费
                    </div>
                  </a-form-item>
                  <!-- 已落库（enable_logistics） -->
                  <a-form-item label="启用物流">
                    <a-switch
                      v-model:checked="logistics.enableLogistics"
                      checked-children="开"
                      un-checked-children="关"
                    />
                    <div class="form-tip">
                      客户商城下单配送方式可以选择邮寄，且按订单和按重量只能同时开启一个
                    </div>
                  </a-form-item>
                </a-card>

                <!-- ══════ 运费计算方式 + 三张地区运费表（对标实测重建 2026-09-14） ══════
                     对标结构：两个固定开关（已在上方卡片）+「运费计算方式」下拉（3 项）
                     + 三张随选择切换的地区运费表（列头逐字对齐 HTML <th>）+ 红字默认运费提示。
                     核心维度 = 地区（省/市/区）与首重/续重；**无「启用」列**。 -->
                <a-card
                  title="地区运费"
                  :bordered="false"
                  class="group-card"
                >
                  <a-form-item label="运费计算方式">
                    <a-select
                      v-model:value="freight.wftype"
                      style="width: 200px"
                      :options="WFTYPE_OPTIONS"
                    />
                    <span class="form-tip" style="margin-left: 8px">
                      {{ wftypeTooltip }}
                    </span>
                  </a-form-item>

                  <div class="default-freight-row">
                    <span class="default-freight-row__label">未设置运费地区的默认运费</span>
                    <a-input-number
                      v-model:value="freight.defaultFreight"
                      :min="0"
                      :precision="2"
                      size="small"
                      style="width: 140px"
                    />
                    <span class="default-freight-row__unit">元</span>
                  </div>
                  <div class="freight-hint">
                    未设置运费的地区，默认运费为{{ defaultFreightDisplay }}元
                  </div>

                  <!-- 该列仅在「按订单金额」表出现，故置于表格卡片外（不进表头，避免非该表时表头多一列） -->
                  <div
                    v-if="freight.wftype === FREIGHT_WFTYPE.AMOUNT"
                    class="batch-bar"
                  >
                    <a-button
                      size="small"
                      :disabled="!freight.regions.length"
                      @click="openBatchTier"
                    >
                      批量设置金额阶梯
                    </a-button>
                    <span class="form-tip" style="margin-left: 8px">
                      将当前表格所有地区的金额阶梯统一设为同一套（避免逐行点「设置」）
                    </span>
                  </div>

                  <a-table
                    :columns="regionColumns"
                    :data-source="freight.regions"
                    :pagination="false"
                    size="small"
                    row-key="rowKey"
                    :scroll="{ x: 'max-content' }"
                  >
                    <template #bodyCell="{ column, record }">
                      <!-- 操作：增 / 删行（对标为 ⊕ / ⊖ 图标），列在最左（逐字对齐对标 <th> 顺序） -->
                      <template v-if="column.key === 'action'">
                        <a-space size="small">
                          <a-tooltip title="增加一行">
                            <PlusCircleOutlined
                              class="row-op row-op--add"
                              @click="addRegionRow()"
                            />
                          </a-tooltip>
                          <a-tooltip title="删除本行">
                            <MinusCircleOutlined
                              class="row-op row-op--del"
                              @click="removeRegionRow(asRegion(record).rowKey)"
                            />
                          </a-tooltip>
                        </a-space>
                      </template>
                      <!-- 区域（省/市/区） -->
                      <template v-else-if="column.key === 'region'">
                        <a-tree-select
                          v-model:value="record.code"
                          class="region-select"
                          :tree-data="regionTree"
                          :field-names="{ label: 'name', value: 'code', children: 'children' }"
                          :dropdown-style="{ maxHeight: '320px', overflow: 'auto' }"
                          tree-default-expand-all
                          show-search
                          tree-node-filter-prop="name"
                          placeholder="请选择省 / 市 / 区"
                          allow-clear
                          @change="onRegionChange(asRegion(record))"
                        />
                      </template>
                      <!-- 按重量：首重(KG) / 续重(KG) -->
                      <template v-else-if="column.key === 'firstWeight'">
                        <a-input-number
                          v-model:value="record.firstWeight"
                          :min="0"
                          :precision="3"
                          size="small"
                          style="width: 100%"
                        />
                      </template>
                      <template v-else-if="column.key === 'addWeight'">
                        <a-input-number
                          v-model:value="record.addWeight"
                          :min="0"
                          :precision="3"
                          size="small"
                          style="width: 100%"
                        />
                      </template>
                      <!-- 按订单数量：起算数量 / 增加数量 -->
                      <template v-else-if="column.key === 'startCount'">
                        <a-input-number
                          v-model:value="record.startCount"
                          :min="0"
                          :precision="2"
                          size="small"
                          style="width: 100%"
                        />
                      </template>
                      <template v-else-if="column.key === 'addCount'">
                        <a-input-number
                          v-model:value="record.addCount"
                          :min="0"
                          :precision="2"
                          size="small"
                          style="width: 100%"
                        />
                      </template>
                      <!-- 运费(元)：按订单金额表的单元格是行内「设置」按钮（配金额阶梯） -->
                      <template v-else-if="column.key === 'freight'">
                        <template v-if="freight.wftype === FREIGHT_WFTYPE.AMOUNT">
                          <a-button
                            type="link"
                            size="small"
                            @click="openTierModal(asRegion(record))"
                          >
                            设置
                          </a-button>
                          <span class="form-tip" style="margin: 0">
                            {{ tierSummary(asRegion(record)) }}
                          </span>
                        </template>
                        <a-input-number
                          v-else
                          v-model:value="record.freight"
                          :min="0"
                          :precision="2"
                          size="small"
                          style="width: 100%"
                        />
                      </template>
                      <!-- 续费(元)：按重量 / 按订单数量 -->
                      <template v-else-if="column.key === 'addFreight'">
                        <a-input-number
                          v-model:value="record.addFreight"
                          :min="0"
                          :precision="2"
                          size="small"
                          style="width: 100%"
                        />
                      </template>
                      <!-- 满额包邮(元)：每行一个金额（对标无模板级包邮开关 + 阈值） -->
                      <template v-else-if="column.key === 'freeFreight'">
                        <a-input-number
                          v-model:value="record.freeFreight"
                          :min="0"
                          :precision="2"
                          size="small"
                          style="width: 100%"
                          placeholder="0"
                        />
                      </template>
                      <!-- 批量：对标该表头存在，点击后为「批量设置」弹窗（本轮未抓到弹窗内容，
                           仅按列头逐字对齐实现表头与入口；详见开发文档「剩余缺口」#9） -->
                      <template v-else-if="column.key === 'batch'">
                        <a-button
                          v-if="freight.wftype === FREIGHT_WFTYPE.AMOUNT"
                          type="link"
                          size="small"
                          @click="openBatchTier"
                        >
                          批量
                        </a-button>
                        <span
                          v-else
                          class="form-tip"
                          style="margin: 0"
                        >—</span>
                      </template>
                    </template>
                  </a-table>

                  <div class="table-foot">
                    <a-button
                      size="small"
                      @click="addRegionRow()"
                    >
                      <PlusCircleOutlined /> 新增地区
                    </a-button>
                    <span
                      v-if="!freight.regions.length"
                      class="form-tip"
                      style="margin-left: 8px"
                    >
                      暂无地区行；未设置运费的地区按上方默认运费计算
                    </span>
                  </div>
                </a-card>
              </template>

              <!-- ══════ 子项二：到店自提 ══════ -->
              <template v-else-if="currentMenuKey === 'pickup'">
                <a-card
                  title="到店自提设置"
                  :bordered="false"
                  class="group-card"
                >
                  <!-- 已落库（enable_pickup） -->
                  <a-form-item label="到店自提">
                    <a-switch
                      v-model:checked="pickup.enabled"
                      checked-children="开"
                      un-checked-children="关"
                    />
                  </a-form-item>
                  <a-form-item label="提货地址">
                    <!-- 已落库（pickup_addresses JSON 数组：[{contact,phone,address}]）；可增删改，保存后回显 -->
                    <div
                      v-if="!pickup.addresses.length"
                      class="pickup-empty"
                    >
                      请添加至少1条提货地址，以免影响买家商城下单
                    </div>
                    <a-table
                      v-else
                      :columns="pickupColumns"
                      :data-source="pickup.addresses"
                      :pagination="false"
                      size="small"
                      row-key="rowKey"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'contact'">
                          <a-input
                            v-model:value="record.contact"
                            size="small"
                            placeholder="联系人"
                          />
                        </template>
                        <template v-else-if="column.key === 'phone'">
                          <a-input
                            v-model:value="record.phone"
                            size="small"
                            placeholder="联系电话"
                          />
                        </template>
                        <template v-else-if="column.key === 'address'">
                          <a-input
                            v-model:value="record.address"
                            size="small"
                            placeholder="提货地址"
                          />
                        </template>
                        <template v-else-if="column.key === 'action'">
                          <a-button
                            type="link"
                            size="small"
                            danger
                            @click="removePickupAddress(record.rowKey)"
                          >
                            删除
                          </a-button>
                        </template>
                      </template>
                    </a-table>
                    <div style="margin-top: 12px">
                      <a-button
                        size="small"
                        @click="addPickupAddress"
                      >
                        <PlusCircleOutlined /> 添加提货地址
                      </a-button>
                      <span class="form-tip" style="margin-left: 8px">
                        至少 1 条地址才能开启「到店自提」并保存
                      </span>
                    </div>
                  </a-form-item>
                </a-card>
              </template>

              <!-- ═══ 金额阶梯弹窗（仅「按订单金额」表行内「设置」；对标后端 prilist:[{min,max,freight,caninput}]） ═══ -->
              <a-modal
                v-model:open="tierModal.open"
                :title="tierModal.title"
                :width="720"
                :mask-closable="false"
                @ok="confirmTiers"
              >
                <div class="group-tip">
                  按订单金额区间设置运费：区间为 [最小金额, 最大金额]，最大金额留空表示「不限」；
                  同一地区区间不得重叠。
                </div>
                <a-table
                  :columns="tierColumns"
                  :data-source="tierModal.tiers"
                  :pagination="false"
                  size="small"
                  row-key="rowKey"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'min'">
                      <a-input-number
                        v-model:value="record.min"
                        :min="0"
                        :precision="2"
                        size="small"
                        style="width: 100%"
                      />
                    </template>
                    <template v-else-if="column.key === 'max'">
                      <a-input-number
                        v-model:value="record.max"
                        :min="0"
                        :precision="2"
                        size="small"
                        style="width: 100%"
                        placeholder="不限"
                      />
                    </template>
                    <template v-else-if="column.key === 'tierFreight'">
                      <a-input-number
                        v-model:value="record.freight"
                        :min="0"
                        :precision="2"
                        size="small"
                        style="width: 100%"
                      />
                    </template>
                    <template v-else-if="column.key === 'tierAction'">
                      <a-button
                        type="link"
                        size="small"
                        danger
                        @click="removeTierRow(record.rowKey)"
                      >
                        删除
                      </a-button>
                    </template>
                  </template>
                </a-table>
                <div class="table-foot">
                  <a-button
                    size="small"
                    @click="addTierRow"
                  >
                    <PlusCircleOutlined /> 新增阶梯
                  </a-button>
                </div>
              </a-modal>

              <!-- ═══ 底部：按子项保存 ═══ -->
              <div class="config-footer">
                <a-space>
                  <a-button
                    type="primary"
                    :loading="saving"
                    @click="handleSave"
                  >
                    保存
                  </a-button>
                  <a-button
                    :disabled="loading"
                    @click="loadConfig"
                  >
                    重置
                  </a-button>
                </a-space>
              </div>
            </a-form>
          </a-spin>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
/**
 * 运费设置（商城 → 商城设置 → 运费设置）
 * 对标文档：docs/Yh-Spec/手动整理对标开发文档/交易模块/运费设置开发文档.md
 * 结构：左侧 2 子项导航（物流/到店自提）+ 右侧分组表单
 *   · 物流：两个固定开关（自有配送 / 启用物流）+「运费计算方式」下拉 + **三张随选择切换的
 *          地区运费表**（按重量 / 按订单金额 / 按订单数量，列头逐字对齐对标 HTML <th>）
 *   · 到店自提：开关 + 提货地址列表
 *
 * 存储口径（Flyway V11.361.7 补齐本页列；V11.364.0 按对标实测重建 freight_template 形状）：
 *   · 布尔开关：库内 INTEGER 0/1，经 boolToBit / bitToBool 显式转换
 *       self_delivery（对标 `selfmention` 自有配送）/ enable_logistics（对标 `isexpress` 启用物流）
 *   · JSON 结构化字段（TEXT 列，前端 JSON.stringify/JSON.parse，try-catch 兜底）：
 *       freight_template   {wftype:1|2|3, defaultFreight, regions:[{code,province,city,area,
 *                          firstWeight,freight,addWeight,addFreight,startCount,addCount,
 *                          freeFreight,amountTiers?:[{min,max,freight,caninput}]}]}
 *                          —— 对标实测重建（2026-09-14）；`wftype` = 对标后端字段名，
 *                          取值 1=按重量 / 2=按订单金额 / 3=按订单数量
 *       pickup_addresses   [{contact,phone,address}]
 *   · ⚠️ `logistics_methods`（物流方式列表）**已废弃**（对标经实测根本没有「物流方式」列表）：
 *       本页**不再读写**该列；列与既有数据仅为兼容保留，见 V11.364.0 迁移注释。
 *   · ⚠️ 统一运费卡片（free_shipping_amount / freight_amount）已随本次重建移除：
 *       对标「物流」子项无此结构。
 *
 * 保存安全机制：三页共用单行实体 + 同一对端点；后端 updateConfig 走 updateById，
 *   MyBatis-Plus 默认忽略 null 字段 → 本页只提交自己维护的字段时不会覆盖另两页字段。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusCircleOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { sysRegionApi, type SysRegion } from '@/api/sys-region'
import {
  shopConfigApi,
  boolToBit,
  bitToBool,
  parseJsonField,
  stringifyJsonField,
  FREIGHT_WFTYPE,
  type ShopConfig,
  type ShopFreightRegionRow,
  type ShopFreightTier,
} from '@/api/erp/mall'

defineOptions({ name: 'MallFreightConfig' })

// ═══ 左侧 2 子项导航（对标实测） ═══
const MENU_ITEMS = [
  { key: 'logistics', label: '物流' },
  { key: 'pickup', label: '到店自提' },
] as const
const currentMenuKey = ref<string>('logistics')
const currentMenuLabel = computed(() => MENU_ITEMS.find(m => m.key === currentMenuKey.value)?.label || '')

function onMenuClick({ key }: { key: string | number }) {
  currentMenuKey.value = String(key)
}

// ═══ 子项一 · 物流（对标实测：两个固定开关 + 运费计算方式 + 三张地区运费表） ═══
const WFTYPE_OPTIONS = [
  { label: '按重量', value: FREIGHT_WFTYPE.WEIGHT },
  { label: '按订单金额', value: FREIGHT_WFTYPE.AMOUNT },
  { label: '按订单数量', value: FREIGHT_WFTYPE.COUNT },
]

/** 地区行（含前端专用 rowKey，不落库） */
interface RegionRow extends ShopFreightRegionRow { rowKey: string }
interface TierRow extends ShopFreightTier { rowKey: string }

/**
 * 物流设置 —— 两个**固定开关**（对标实测，不是动态行列表）
 * 对标内部字段名：自有配送 `selfmention` / 启用物流 `isexpress`（语义一一对应，命名属本系统自有命名）
 */
const logistics = reactive({
  selfDelivery: false,
  enableLogistics: false,
})

const freight = reactive({
  wftype: FREIGHT_WFTYPE.WEIGHT as number,
  /** 未设置运费的地区的默认运费（对标红字提示 N，实测默认 0） */
  defaultFreight: 0 as number,
  regions: [] as RegionRow[],
})

/** 新建一条地区行 */
function newRegionRow(seq: number): RegionRow {
  return {
    rowKey: `r${seq}`,
    code: undefined,
    province: '',
    city: '',
    area: '',
    firstWeight: 0,
    freight: 0,
    addWeight: 0,
    addFreight: 0,
    startCount: 0,
    addCount: 0,
    freeFreight: 0,
    amountTiers: [],
  }
}
let regionSeq = 0
/** 表格 slot 的 record 由 antdv 推断为 Record<string, any>，此处收窄回 RegionRow（运行时即同一对象） */
function asRegion(record: Record<string, any>): RegionRow {
  return record as RegionRow
}
function addRegionRow() {
  regionSeq += 1
  freight.regions.push(newRegionRow(regionSeq))
}
function removeRegionRow(rowKey: string) {
  const idx = freight.regions.findIndex(r => r.rowKey === rowKey)
  if (idx >= 0) freight.regions.splice(idx, 1)
}

/** 「按重量」表（6 列 + 批量），列头逐字对齐对标 HTML <th> */
const weightColumns = [
  { title: '操作', key: 'action', width: 80 },
  { title: '区域', key: 'region', width: 200 },
  { title: '首重(KG)', key: 'firstWeight', width: 120 },
  { title: '运费(元)', key: 'freight', width: 120 },
  { title: '续重(KG)', key: 'addWeight', width: 120 },
  { title: '续费(元)', key: 'addFreight', width: 120 },
  { title: '满额包邮(元)', key: 'freeFreight', width: 130 },
  { title: '批量', key: 'batch', width: 80 },
]
/** 「按订单金额」表（运费单元格为行内「设置」按钮，配金额阶梯 prilist） */
const amountColumns = [
  { title: '操作', key: 'action', width: 80 },
  { title: '区域', key: 'region', width: 200 },
  { title: '运费(元)', key: 'freight', width: 240 },
  { title: '满额包邮(元)', key: 'freeFreight', width: 130 },
  { title: '批量', key: 'batch', width: 80 },
]
/** 「按订单数量」表（起算数量 / 增加数量） */
const countColumns = [
  { title: '操作', key: 'action', width: 80 },
  { title: '区域', key: 'region', width: 200 },
  { title: '起算数量', key: 'startCount', width: 120 },
  { title: '运费(元)', key: 'freight', width: 120 },
  { title: '增加数量', key: 'addCount', width: 120 },
  { title: '续费(元)', key: 'addFreight', width: 120 },
  { title: '满额包邮(元)', key: 'freeFreight', width: 130 },
  { title: '批量', key: 'batch', width: 80 },
]
/** 当前「运费计算方式」对应的表列（三张表随选择切换） */
const regionColumns = computed(() => {
  if (freight.wftype === FREIGHT_WFTYPE.AMOUNT) return amountColumns
  if (freight.wftype === FREIGHT_WFTYPE.COUNT) return countColumns
  return weightColumns
})

/** 金额阶梯弹窗的列（对标行内「设置」→ 配 min/max/freight） */
const tierColumns = [
  { title: '最小金额(元)', key: 'min', width: 180 },
  { title: '最大金额(元)', key: 'max', width: 180 },
  { title: '运费(元)', key: 'tierFreight', width: 160 },
  { title: '操作', key: 'tierAction', width: 80 },
]

const wftypeTooltip = computed(() => {
  if (freight.wftype === FREIGHT_WFTYPE.AMOUNT) return '按订单金额：每个地区可分别设置金额区间运费（行内「设置」）'
  if (freight.wftype === FREIGHT_WFTYPE.COUNT) return '按订单数量：按起算数量 + 增加数量计价'
  return '按重量：按首重 + 续重计价'
})

/** 红字提示中的默认运费（取配置值，默认 0；对标原文「未设置运费的地区，默认运费为0元」） */
const defaultFreightDisplay = computed(() => {
  const n = Number(freight.defaultFreight)
  return Number.isFinite(n) ? n : 0
})

// ── 行政区划（省/市/区）数据源：sysRegionApi 自带会话内缓存 ──
const regionTree = ref<SysRegion[]>([])
async function loadRegionTree() {
  try {
    regionTree.value = await sysRegionApi.tree()
  } catch (e) {
    regionTree.value = []
    console.warn('[运费设置] 行政区划加载失败', e)
  }
}
/** 选择地区后把省/市/区名称落到行上（对标后端 region:[{code,province,city,area}]） */
function onRegionChange(record: RegionRow) {
  const resolved = resolveRegionPath(regionTree.value, record.code)
  record.province = resolved[0] || ''
  record.city = resolved[1] || ''
  record.area = resolved[2] || ''
  // code 与树对不上（脏数据 / 行政区划变更）时清空，避免保存出无名称的空地区
  if (!resolved.length) record.code = undefined
}
/** 返回 [省, 市, 区] 名称路径（按层级顺序） */
function resolveRegionPath(nodes: SysRegion[], code?: string, trail: string[] = []): string[] {
  if (!code) return []
  for (const n of nodes) {
    const next = [...trail, n.name]
    if (n.code === code) return next
    const hit = resolveRegionPath(n.children || [], code, next)
    if (hit.length) return hit
  }
  return []
}

// ── 金额阶梯弹窗（仅「按订单金额」表；对标行内「设置」） ──
let tierSeq = 0
function newTierRow(seq: number): TierRow {
  return { rowKey: `k${seq}`, min: 0, max: null, freight: 0, caninput: true }
}
const tierModal = reactive({
  open: false,
  title: '',
  regionKey: '' as string,
  tiers: [] as TierRow[],
})
function openTierModal(record: RegionRow) {
  tierSeq = 0
  tierModal.regionKey = record.rowKey
  const regionLabel = [record.province, record.city, record.area].filter(Boolean).join(' / ') || '未选择地区'
  tierModal.title = `金额阶梯设置 · ${regionLabel}`
  const existing = Array.isArray(record.amountTiers) ? record.amountTiers : []
  tierModal.tiers = existing.map((t) => {
    tierSeq += 1
    return {
      rowKey: `k${tierSeq}`,
      min: Number(t.min) || 0,
      max: t.max === null || t.max === undefined || (t.max as unknown as string) === '' ? null : Number(t.max),
      freight: Number(t.freight) || 0,
      caninput: t.caninput !== false,
    }
  })
  if (!tierModal.tiers.length) tierModal.tiers = [newTierRow(++tierSeq)]
  tierModal.open = true
}
function addTierRow() {
  tierSeq += 1
  tierModal.tiers.push(newTierRow(tierSeq))
}
function removeTierRow(rowKey: string) {
  const idx = tierModal.tiers.findIndex(t => t.rowKey === rowKey)
  if (idx >= 0) tierModal.tiers.splice(idx, 1)
}
/** 保存阶梯：区间不得重叠（max=null 视为 +∞） */
function confirmTiers() {
  const sorted = [...tierModal.tiers].sort((a, b) => (Number(a.min) || 0) - (Number(b.min) || 0))
  for (let i = 1; i < sorted.length; i += 1) {
    const prevMax = sorted[i - 1].max
    if (prevMax === null || prevMax === undefined) {
      message.warning('金额阶梯中存在「不限」区间，其后再有区间会重叠，请调整')
      return
    }
    if ((Number(sorted[i].min) || 0) < Number(prevMax)) {
      message.warning('金额阶梯区间不得重叠，请调整最小/最大金额')
      return
    }
  }
  const row = freight.regions.find(r => r.rowKey === tierModal.regionKey)
  if (row) {
    row.amountTiers = tierModal.tiers.map(t => ({
      min: Number(t.min) || 0,
      max: t.max === null || t.max === undefined ? null : Number(t.max),
      freight: Number(t.freight) || 0,
      caninput: t.caninput !== false,
    }))
  }
  tierModal.open = false
}
/** 表格内联摘要：展示该地区已配的金额阶梯条数 */
function tierSummary(record: RegionRow): string {
  const n = Array.isArray(record.amountTiers) ? record.amountTiers.length : 0
  return n ? `已设 ${n} 段` : '未设置'
}
/** 批量设置入口（对标「批量」表头入口；本轮未抓到弹窗内容，暂与单行「设置」等价） */
function openBatchTier() {
  if (!freight.regions.length) {
    message.info('请先新增至少一个地区')
    return
  }
  openTierModal(freight.regions[0])
}

// ═══ 子项二 · 到店自提 ═══
interface PickupAddress { rowKey: string; contact: string; phone: string; address: string }
const pickup = reactive({
  enabled: false,
  addresses: [] as PickupAddress[],
})
/** 提货地址列表列（对齐文档：联系人 / 联系电话 / 提货地址；行内可编辑 + 删除） */
const pickupColumns = [
  { title: '联系人', key: 'contact', width: 180 },
  { title: '联系电话', key: 'phone', width: 180 },
  { title: '提货地址', key: 'address' },
  { title: '操作', key: 'action', width: 80 },
]
let pickupSeq = 0
function addPickupAddress() {
  pickupSeq += 1
  pickup.addresses.push({ rowKey: `p${pickupSeq}`, contact: '', phone: '', address: '' })
}
function removePickupAddress(rowKey: string) {
  const idx = pickup.addresses.findIndex(p => p.rowKey === rowKey)
  if (idx >= 0) pickup.addresses.splice(idx, 1)
}

// ═══ 配置读写（单行 ShopConfig） ═══
const loading = ref(false)
const saving = ref(false)
const configId = ref<number | undefined>(undefined)
/** 完整配置缓存：保存时合并，避免覆盖其它设置页维护的字段 */
let fullConfig: ShopConfig | null = null

async function loadConfig() {
  loading.value = true
  try {
    const res: any = await shopConfigApi.get()
    const cfg: ShopConfig | null = res?.data ?? res ?? null
    fullConfig = cfg
    configId.value = cfg?.id
    // 已落库字段回填（V11.361.7 起本页全部字段真实回填；V11.364.0 起 freight_template 为新形状）
    logistics.selfDelivery = bitToBool(cfg?.selfDelivery, false)
    logistics.enableLogistics = bitToBool(cfg?.enableLogistics, false)
    // 运费计算方式 + 地区运费表（对标实测重建）
    const tpl = parseJsonField<Record<string, any> | null>(cfg?.freightTemplate, null)
    if (tpl && typeof tpl === 'object') {
      const wt = Number(tpl.wftype)
      const validWftypes: number[] = [FREIGHT_WFTYPE.WEIGHT, FREIGHT_WFTYPE.AMOUNT, FREIGHT_WFTYPE.COUNT]
      freight.wftype = validWftypes.includes(wt) ? wt : FREIGHT_WFTYPE.WEIGHT
      freight.defaultFreight = Number(tpl.defaultFreight) || 0
      const regions = Array.isArray(tpl.regions) ? tpl.regions : []
      freight.regions = regions
        .filter((r: any) => r && typeof r === 'object')
        .map((r: any, i: number) => ({
          rowKey: `r${i + 1}`,
          code: r.code === null || r.code === undefined || r.code === '' ? undefined : String(r.code),
          province: String(r.province || ''),
          city: String(r.city || ''),
          area: String(r.area || ''),
          firstWeight: Number(r.firstWeight) || 0,
          freight: Number(r.freight) || 0,
          addWeight: Number(r.addWeight) || 0,
          addFreight: Number(r.addFreight) || 0,
          startCount: Number(r.startCount) || 0,
          addCount: Number(r.addCount) || 0,
          freeFreight: Number(r.freeFreight) || 0,
          amountTiers: (Array.isArray(r.amountTiers) ? r.amountTiers : [])
            .filter((t: any) => t && typeof t === 'object')
            .map((t: any) => ({
              min: Number(t.min) || 0,
              max: t.max === null || t.max === undefined || t.max === '' ? null : Number(t.max),
              freight: Number(t.freight) || 0,
              caninput: t.caninput !== false,
            })),
        }))
      regionSeq = freight.regions.length
    } else {
      // 首次进入即空表；空表时给一行空地区，便于直接填
      freight.wftype = FREIGHT_WFTYPE.WEIGHT
      freight.defaultFreight = 0
      freight.regions = [newRegionRow(++regionSeq)]
    }
    // ⚠️ 不再解析 logistics_methods（该列已废弃，对标不存在「物流方式」列表）
    const addresses = parseJsonField<Array<Record<string, any>>>(cfg?.pickupAddresses, [])
    pickup.addresses = (Array.isArray(addresses) ? addresses : [])
      .filter(a => a && typeof a === 'object')
      .map((a, i) => ({
        rowKey: `p${i + 1}`,
        contact: String(a.contact || ''),
        phone: String(a.phone || ''),
        address: String(a.address || ''),
      }))
    pickupSeq = pickup.addresses.length
    pickup.enabled = bitToBool(cfg?.enablePickup, false)
  } catch (e) {
    console.warn('[运费设置] 商城配置获取失败', e)
  } finally {
    loading.value = false
  }
}

function handleError(err: any) {
  console.warn('[运费设置] ErrorBoundary:', err)
}

/** 按子项校验 */
function validateCurrentItem(): boolean {
  if (currentMenuKey.value === 'logistics') {
    if (Number(freight.defaultFreight) < 0) {
      message.warning('默认运费不能为负数')
      return false
    }
    // 地区行不得重复（同一地区重复配置会导致运费计算歧义）
    const codes = freight.regions.map(r => r.code).filter(Boolean) as string[]
    if (new Set(codes).size !== codes.length) {
      message.warning('存在重复的地区行，请删除重复项')
      return false
    }
    // 未选择地区的行不允许保存 —— 但仅有「启用物流」时才对数据区强校验
    // （对标实测：数据区在「启用物流 = 关」时本就不渲染，此时不应拦住状态开关的保存）
    if (logistics.enableLogistics && freight.regions.some(r => !r.code && !r.province)) {
      message.warning('存在未选择「区域」的地区行，请选择地区或删除该行')
      return false
    }
    const negative = freight.regions.some(r => [r.firstWeight, r.freight, r.addWeight, r.addFreight,
      r.startCount, r.addCount, r.freeFreight].some(v => Number(v) < 0))
    if (negative) {
      message.warning('运费 / 重量 / 数量 / 包邮金额不能为负数')
      return false
    }
  }
  if (currentMenuKey.value === 'pickup' && pickup.enabled && !pickup.addresses.length) {
    message.warning('请添加至少1条提货地址，以免影响买家商城下单')
    return false
  }
  return true
}

/** 按子项分别保存：仅提交当前子项维护的字段 + 完整配置合并
 *  （后端 updateById 忽略 null 字段，故未提交字段不会被清空；三页共用一行因此安全） */
async function handleSave() {
  if (!validateCurrentItem()) return
  saving.value = true
  try {
    const payload: ShopConfig = {
      ...(fullConfig || {}),
      id: configId.value,
      shopName: fullConfig?.shopName || '订货商城',
    }
    if (currentMenuKey.value === 'logistics') {
      // 布尔 → 库内 0/1（self_delivery ↔ 对标 selfmention；enable_logistics ↔ 对标 isexpress）
      payload.selfDelivery = boolToBit(logistics.selfDelivery)
      payload.enableLogistics = boolToBit(logistics.enableLogistics)
      // 地区运费表 JSON（对标实测重建，见 ShopFreightTemplate）；rowKey 不落库，
      // 空白行（既无 code 也无省名）不落库，避免脏数据堆积
      payload.freightTemplate = stringifyJsonField({
        wftype: Number(freight.wftype) || FREIGHT_WFTYPE.WEIGHT,
        defaultFreight: Number(freight.defaultFreight) || 0,
        regions: freight.regions
          .filter(r => r.code || r.province)
          .map(r => ({
          code: r.code || '',
          province: r.province || '',
          city: r.city || '',
          area: r.area || '',
          firstWeight: Number(r.firstWeight) || 0,
          freight: Number(r.freight) || 0,
          addWeight: Number(r.addWeight) || 0,
          addFreight: Number(r.addFreight) || 0,
          startCount: Number(r.startCount) || 0,
          addCount: Number(r.addCount) || 0,
          freeFreight: Number(r.freeFreight) || 0,
          amountTiers: (Array.isArray(r.amountTiers) ? r.amountTiers : []).map(t => ({
            min: Number(t.min) || 0,
            max: t.max === null || t.max === undefined ? null : Number(t.max),
            freight: Number(t.freight) || 0,
            caninput: t.caninput !== false,
          })),
        })),
      })
      // ⚠️ 不再提交 logisticsMethods（列已废弃，对标不存在「物流方式」列表）
    }
    if (currentMenuKey.value === 'pickup') {
      payload.enablePickup = boolToBit(pickup.enabled)
      payload.pickupAddresses = stringifyJsonField(
        pickup.addresses.map(p => ({ contact: p.contact, phone: p.phone, address: p.address })),
      )
    }
    await shopConfigApi.update(payload)
    message.success(`${currentMenuLabel.value}已保存`)
    await loadConfig()
  } catch (e) {
    console.warn('[运费设置] 保存失败', e)
    message.error(`${currentMenuLabel.value}保存失败`)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  void loadRegionTree()
  void loadConfig()
})
</script>

<style scoped>
/* 左子项导航 + 右配置表单（全高布局，右区独立滚动） */
.config-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  gap: 16px;
  overflow: hidden;
  padding: 12px 16px;
  box-sizing: border-box;
}
.config-layout__side {
  width: 180px;
  flex-shrink: 0;
  border-right: 1px solid #f0f0f0;
  padding-right: 8px;
  overflow: auto;
}
.config-menu {
  border-inline-end: none !important;
}
.config-menu__text {
  font-size: 14px;
}
.config-layout__main {
  flex: 1;
  min-width: 0;
  overflow: auto;
  padding-right: 8px;
}
.group-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 16px;
}
.group-card :deep(.ant-card-head) {
  min-height: 40px;
  padding: 0 16px;
  border-bottom: 1px solid #f5f5f5;
}
.group-card :deep(.ant-card-head-title) {
  font-size: 14px;
  font-weight: 600;
  padding: 10px 0;
}
.group-card :deep(.ant-card-body) {
  padding: 16px 16px 4px;
}
.group-tip {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
  margin: -4px 0 12px;
}
.form-tip {
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.6;
  margin-top: 2px;
}
/* ── 地区运费表（对标实测重建） ── */
.default-freight-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 8px 24px;
}
.default-freight-row__label {
  color: #595959;
  font-size: 13px;
}
.default-freight-row__unit {
  color: #606266;
  font-size: 12px;
}
.freight-hint {
  margin: -4px 0 12px 24px;
  color: #ff4d4f;
  font-size: 12px;
  line-height: 1.6;
}
.batch-bar {
  margin: 0 0 8px 24px;
}
.region-select {
  width: 100%;
  min-width: 190px;
}
/* 操作列 ⊕ / ⊖ 图标（对标为图标按钮，非文字按钮） */
.row-op {
  font-size: 15px;
  cursor: pointer;
}
.row-op--add {
  color: #1890ff;
}
.row-op--del {
  color: #ff4d4f;
}
.table-foot {
  margin-top: 12px;
}
.pickup-empty {
  padding: 16px;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  color: #fa8c16;
  font-size: 13px;
  background: #fffbe6;
}
.config-footer {
  position: sticky;
  bottom: 0;
  padding: 12px 0 16px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
</style>
